package br.com.aluguelequipamento.model.dao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BancoDadosDAO {

    private static final String TODAS = "todas";
    private static final int LIMITE_MANUTENCOES_EM_ANDAMENTO = 10;
    private static final String[] TABELAS = {
            "cliente", "equipamento", "reserva", "retirada", "devolucao", "manutencao"
    };

    private final Random random = new Random();

    public void apagarDados(String tabela) throws SQLException {
        Connection conn = ConexaoDAO.getConexao();
        boolean autoCommitOriginal = conn.getAutoCommit();
        try {
            conn.setAutoCommit(false);
            String sql = TODAS.equals(tabela)
                    ? "TRUNCATE TABLE devolucao, retirada, manutencao, reserva, equipamento, cliente RESTART IDENTITY CASCADE"
                    : "TRUNCATE TABLE " + validarTabela(tabela) + " RESTART IDENTITY CASCADE";
            try (Statement st = conn.createStatement()) {
                st.executeUpdate(sql);
            }
            conn.commit();
        } catch (SQLException ex) {
            conn.rollback();
            throw ex;
        } finally {
            conn.setAutoCommit(autoCommitOriginal);
        }
    }

    public void popularDados(String tabela, int quantidade) throws SQLException {
        Connection conn = ConexaoDAO.getConexao();
        boolean autoCommitOriginal = conn.getAutoCommit();
        try {
            conn.setAutoCommit(false);
            if (TODAS.equals(tabela)) {
                for (String tab : TABELAS) {
                    popularTabela(conn, tab, quantidade);
                }
            } else {
                popularTabela(conn, validarTabela(tabela), quantidade);
            }
            conn.commit();
        } catch (SQLException ex) {
            conn.rollback();
            throw ex;
        } finally {
            conn.setAutoCommit(autoCommitOriginal);
        }
    }

    private void popularTabela(Connection conn, String tabela, int quantidade) throws SQLException {
        switch (tabela) {
            case "cliente" -> inserirClientes(conn, quantidade);
            case "equipamento" -> inserirEquipamentos(conn, quantidade);
            case "reserva" -> inserirReservas(conn, quantidade);
            case "retirada" -> inserirRetiradas(conn, quantidade);
            case "devolucao" -> inserirDevolucoes(conn, quantidade);
            case "manutencao" -> inserirManutencoes(conn, quantidade);
            default -> throw new SQLException("Tabela invalida.");
        }
    }

    private String validarTabela(String tabela) throws SQLException {
        for (String permitida : TABELAS) {
            if (permitida.equals(tabela)) {
                return tabela;
            }
        }
        throw new SQLException("Tabela invalida.");
    }

    private void inserirClientes(Connection conn, int quantidade) throws SQLException {
        String[] nomes = {"Miguel", "Alessandro", "Laura", "Rafael", "Bianca", "Gustavo", "Camila", "Diego",
                "Fernanda", "Bruno", "Leticia", "Marcelo", "Patricia", "Renato", "Juliana", "Thiago"};
        String[] sobrenomes = {"Silva", "Pereira", "Costa", "Souza", "Oliveira", "Santos", "Almeida",
                "Rodrigues", "Martins", "Ferreira", "Gomes", "Barbosa"};
        String[] bairros = {"Centro", "Jardim Camburi", "Praia da Costa", "Campo Grande", "Laranjeiras",
                "Itaparica", "Maruipe", "Cobilandia"};
        String sql = "INSERT INTO cliente (nome, cpf, telefone, email, endereco, data_cadastro) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < quantidade; i++) {
                String nome = sortear(nomes) + " " + sortear(sobrenomes) + " " + sufixo();
                ps.setString(1, nome);
                ps.setString(2, gerarCpf());
                ps.setString(3, "(27) 9" + numero(8));
                ps.setString(4, normalizar(nome).toLowerCase() + "@email.com");
                ps.setString(5, "Rua " + (100 + random.nextInt(900)) + ", " + sortear(bairros));
                ps.setDate(6, Date.valueOf(LocalDate.now().minusDays(random.nextInt(180))));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void inserirEquipamentos(Connection conn, int quantidade) throws SQLException {
        String[] nomes = {"Betoneira 400L", "Betoneira 250L", "Compressor 100L", "Compressor 50L",
                "Gerador 5kVA", "Gerador 8kVA", "Martelete SDS Plus", "Martelete Demolidor",
                "Andaime Tubular 4m", "Andaime Fachadeiro", "Furadeira de Impacto", "Lixadeira Orbital",
                "Serra Marmore", "Serra Circular", "Vibrador de Concreto", "Lavadora de Alta Pressao"};
        String sql = "INSERT INTO equipamento (nome, descricao, categoria, valor_diaria, status, data_cadastro) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < quantidade; i++) {
                String nome = sortear(nomes) + " Modelo " + (char) ('A' + random.nextInt(6)) + random.nextInt(10);
                ps.setString(1, nome);
                ps.setString(2, "Equipamento para locacao - " + nome);
                ps.setString(3, categoriaPorNome(nome));
                ps.setBigDecimal(4, valorDiariaPorNome(nome));
                ps.setString(5, "disponivel");
                ps.setDate(6, Date.valueOf(LocalDate.now().minusDays(15 + random.nextInt(240))));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void inserirReservas(Connection conn, int quantidade) throws SQLException {
        garantirClientes(conn, quantidade);
        garantirEquipamentosDisponiveis(conn, Math.max(8, quantidade / 2));
        List<Integer> clientes = buscarIds(conn, "cliente");
        List<Integer> equipamentos = buscarIds(conn, "equipamento");
        List<Integer> equipamentosDisponiveis = buscarIdsPorStatus(conn, "disponivel");
        List<Integer> reservadosNestaCarga = new ArrayList<>();

        String sql = "INSERT INTO reserva (cliente_id, equipamento_id, data_inicio, data_fim, status, observacao) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < quantidade; i++) {
                String status = sortearStatusReserva();
                if ("ativa".equals(status) && semIds(equipamentosDisponiveis, reservadosNestaCarga).isEmpty()) {
                    status = random.nextBoolean() ? "concluida" : "cancelada";
                }
                int equipamentoId = escolherEquipamentoReserva(equipamentos, equipamentosDisponiveis,
                        reservadosNestaCarga, status);
                LocalDate inicio = dataReserva(status, i);
                int duracao = 1 + random.nextInt("ativa".equals(status) ? 6 : 9);
                ps.setInt(1, escolherPorPopularidade(clientes));
                ps.setInt(2, equipamentoId);
                ps.setDate(3, Date.valueOf(inicio));
                ps.setDate(4, Date.valueOf(inicio.plusDays(duracao)));
                ps.setString(5, status);
                ps.setString(6, observacaoReserva(status));
                ps.addBatch();
                if ("ativa".equals(status)) {
                    atualizarStatusEquipamento(conn, equipamentoId, "reservado");
                    reservadosNestaCarga.add(equipamentoId);
                }
            }
            ps.executeBatch();
        }
    }

    private void inserirRetiradas(Connection conn, int quantidade) throws SQLException {
        garantirClientes(conn, quantidade);
        garantirEquipamentosDisponiveis(conn, Math.max(8, quantidade / 2));
        List<Integer> clientes = buscarIds(conn, "cliente");
        List<Integer> equipamentos = buscarIds(conn, "equipamento");
        List<Integer> equipamentosDisponiveis = buscarIdsPorStatus(conn, "disponivel");
        List<Integer> alugadosNestaCarga = new ArrayList<>();

        String sql = "INSERT INTO retirada (reserva_id, cliente_id, equipamento_id, data_retirada, "
                + "data_prev_devolucao, valor_total, status, observacao) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < quantidade; i++) {
                String status = sortearStatusRetirada();
                if ("ativa".equals(status) && semIds(equipamentosDisponiveis, alugadosNestaCarga).isEmpty()) {
                    status = "concluida";
                }
                int equipamentoId = escolherEquipamentoRetirada(equipamentos, equipamentosDisponiveis,
                        alugadosNestaCarga, status);
                LocalDate retirada = dataRetirada(status, i);
                int dias = 1 + random.nextInt(8);
                ps.setNull(1, Types.INTEGER);
                ps.setInt(2, escolherPorPopularidade(clientes));
                ps.setInt(3, equipamentoId);
                ps.setDate(4, Date.valueOf(retirada));
                ps.setDate(5, Date.valueOf(retirada.plusDays(dias)));
                ps.setBigDecimal(6, calcularValorAluguel(conn, equipamentoId, dias));
                ps.setString(7, status);
                ps.setString(8, observacaoRetirada(status));
                ps.addBatch();
                if ("ativa".equals(status)) {
                    atualizarStatusEquipamento(conn, equipamentoId, "alugado");
                    alugadosNestaCarga.add(equipamentoId);
                }
            }
            ps.executeBatch();
        }
    }

    private int inserirRetiradaConcluida(Connection conn) throws SQLException {
        List<Integer> clientes = buscarIds(conn, "cliente");
        List<Integer> equipamentos = buscarIds(conn, "equipamento");
        List<Integer> equipamentosDisponiveis = buscarIdsPorStatus(conn, "disponivel");

        String sql = "INSERT INTO retirada (reserva_id, cliente_id, equipamento_id, data_retirada, "
                + "data_prev_devolucao, valor_total, status, observacao) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int equipamentoId = escolherEquipamentoRetirada(equipamentos, equipamentosDisponiveis,
                    new ArrayList<>(), "concluida");
            LocalDate retirada = dataRetirada("concluida", 0);
            int dias = 1 + random.nextInt(8);

            ps.setNull(1, java.sql.Types.INTEGER);
            ps.setInt(2, escolherPorPopularidade(clientes));
            ps.setInt(3, equipamentoId);
            ps.setDate(4, Date.valueOf(retirada));
            ps.setDate(5, Date.valueOf(retirada.plusDays(dias)));
            ps.setBigDecimal(6, calcularValorAluguel(conn, equipamentoId, dias));
            ps.setString(7, "concluida");
            ps.setString(8, observacaoRetirada("concluida"));
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    private void inserirDevolucoes(Connection conn, int quantidade) throws SQLException {
        for (int i = 0; i < quantidade; i++) {
            int retiradaId = buscarRetiradaSemDevolucao(conn);
            if (retiradaId == 0) {
                retiradaId = inserirRetiradaConcluida(conn);
            }
            if (retiradaId == 0) continue;
            LocalDate dataRetirada = buscarDataRetirada(conn, retiradaId);
            String sql = "INSERT INTO devolucao (retirada_id, data_devolucao, observacao, status) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, retiradaId);
                ps.setDate(2, Date.valueOf(dataRetirada.plusDays(1 + random.nextInt(9))));
                ps.setString(3, observacaoDevolucao());
                ps.setString(4, "concluida");
                ps.executeUpdate();
            }
            atualizarRetiradaConcluida(conn, retiradaId);
        }
    }

    private void inserirManutencoes(Connection conn, int quantidade) throws SQLException {
        garantirEquipamentosDisponiveis(conn, Math.max(8, quantidade / 2));
        List<Integer> equipamentos = buscarIds(conn, "equipamento");
        List<Integer> equipamentosDisponiveis = buscarIdsPorStatus(conn, "disponivel");
        List<Integer> emManutencaoNestaCarga = new ArrayList<>();
        int emAndamento = contarStatus(conn, "manutencao", "em_andamento");

        String sql = "INSERT INTO manutencao (equipamento_id, descricao, data_inicio, data_previsao, data_fim, status) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < quantidade; i++) {
                boolean criarEmAndamento = emAndamento < LIMITE_MANUTENCOES_EM_ANDAMENTO
                        && !semIds(equipamentosDisponiveis, emManutencaoNestaCarga).isEmpty()
                        && random.nextInt(100) < 28;
                String status = criarEmAndamento ? "em_andamento" : "concluida";
                int equipamentoId = escolherEquipamentoManutencao(equipamentos, equipamentosDisponiveis,
                        emManutencaoNestaCarga, status);
                LocalDate inicio = dataManutencao(status, i);
                int prazo = 2 + random.nextInt(8);
                ps.setInt(1, equipamentoId);
                ps.setString(2, observacaoManutencao(status));
                ps.setDate(3, Date.valueOf(inicio));
                ps.setDate(4, Date.valueOf(inicio.plusDays(prazo)));
                if ("concluida".equals(status)) {
                    ps.setDate(5, Date.valueOf(inicio.plusDays(1 + random.nextInt(prazo))));
                } else {
                    ps.setNull(5, Types.DATE);
                    atualizarStatusEquipamento(conn, equipamentoId, "em_manutencao");
                    emAndamento++;
                    emManutencaoNestaCarga.add(equipamentoId);
                }
                ps.setString(6, status);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void garantirClientes(Connection conn, int quantidade) throws SQLException {
        if (contar(conn, "cliente") < quantidade) {
            inserirClientes(conn, quantidade);
        }
    }

    private void garantirEquipamentosDisponiveis(Connection conn, int quantidade) throws SQLException {
        if (buscarIdsPorStatus(conn, "disponivel").size() < quantidade) {
            inserirEquipamentos(conn, quantidade);
        }
    }

    private int contar(Connection conn, String tabela) throws SQLException {
        try (Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + validarTabela(tabela))) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private int contarStatus(Connection conn, String tabela, String status) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + validarTabela(tabela) + " WHERE status = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private List<Integer> buscarIds(Connection conn, String tabela) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        try (Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery("SELECT id FROM " + validarTabela(tabela) + " ORDER BY id")) {
            while (rs.next()) {
                ids.add(rs.getInt("id"));
            }
        }
        return ids;
    }

    private List<Integer> buscarIdsPorStatus(Connection conn, String status) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT id FROM equipamento WHERE status = ? ORDER BY id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt("id"));
                }
            }
        }
        return ids;
    }

    private int buscarRetiradaSemDevolucao(Connection conn) throws SQLException {
        String sql = "SELECT r.id FROM retirada r LEFT JOIN devolucao d ON d.retirada_id = r.id "
                + "WHERE d.id IS NULL AND r.status = 'concluida' ORDER BY r.id LIMIT 1";
        try (Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt("id") : 0;
        }
    }

    private LocalDate buscarDataRetirada(Connection conn, int retiradaId) throws SQLException {
        String sql = "SELECT data_retirada FROM retirada WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, retiradaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDate("data_retirada").toLocalDate();
                }
            }
        }
        return LocalDate.now();
    }

    private BigDecimal buscarValorDiaria(Connection conn, int equipamentoId) throws SQLException {
        String sql = "SELECT valor_diaria FROM equipamento WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, equipamentoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("valor_diaria");
                }
            }
        }
        return BigDecimal.valueOf(100);
    }

    private BigDecimal calcularValorAluguel(Connection conn, int equipamentoId, int dias) throws SQLException {
        BigDecimal desconto = BigDecimal.valueOf(dias >= 5 ? 0.90 : 1.00);
        return buscarValorDiaria(conn, equipamentoId)
                .multiply(BigDecimal.valueOf(dias))
                .multiply(desconto)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void atualizarStatusEquipamento(Connection conn, int equipamentoId, String status) throws SQLException {
        String sql = "UPDATE equipamento SET status = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, equipamentoId);
            ps.executeUpdate();
        }
    }

    private void atualizarRetiradaConcluida(Connection conn, int retiradaId) throws SQLException {
        String sql = "UPDATE retirada SET status = 'concluida' WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, retiradaId);
            ps.executeUpdate();
        }
    }

    private int escolherEquipamentoReserva(List<Integer> equipamentos, List<Integer> disponiveis,
            List<Integer> reservadosNestaCarga, String status) {
        if (!"ativa".equals(status)) {
            return escolherPorPopularidade(equipamentos);
        }
        List<Integer> candidatos = semIds(disponiveis, reservadosNestaCarga);
        return escolherPorPopularidade(candidatos.isEmpty() ? disponiveis : candidatos);
    }

    private int escolherEquipamentoRetirada(List<Integer> equipamentos, List<Integer> disponiveis,
            List<Integer> alugadosNestaCarga, String status) {
        if (!"ativa".equals(status)) {
            return escolherPorPopularidade(equipamentos);
        }
        List<Integer> candidatos = semIds(disponiveis, alugadosNestaCarga);
        return escolherPorPopularidade(candidatos.isEmpty() ? disponiveis : candidatos);
    }

    private int escolherEquipamentoManutencao(List<Integer> equipamentos, List<Integer> disponiveis,
            List<Integer> emManutencaoNestaCarga, String status) {
        if (!"em_andamento".equals(status)) {
            return escolherPorPopularidade(equipamentos);
        }
        List<Integer> candidatos = semIds(disponiveis, emManutencaoNestaCarga);
        return escolherPorPopularidade(candidatos.isEmpty() ? disponiveis : candidatos);
    }

    private int escolherPorPopularidade(List<Integer> ids) {
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("Lista de ids vazia.");
        }
        int limiteTop = Math.max(1, ids.size() / 3);
        if (ids.size() > 2 && random.nextInt(100) < 65) {
            return ids.get(random.nextInt(limiteTop));
        }
        return ids.get(random.nextInt(ids.size()));
    }

    private List<Integer> semIds(List<Integer> origem, List<Integer> ignorar) {
        List<Integer> filtrados = new ArrayList<>();
        for (Integer id : origem) {
            if (!ignorar.contains(id)) {
                filtrados.add(id);
            }
        }
        return filtrados;
    }

    private String sortearStatusReserva() {
        int chance = random.nextInt(100);
        if (chance < 58) {
            return "ativa";
        }
        if (chance < 82) {
            return "concluida";
        }
        return "cancelada";
    }

    private String sortearStatusRetirada() {
        return random.nextInt(100) < 35 ? "ativa" : "concluida";
    }

    private LocalDate dataReserva(String status, int indice) {
        if ("ativa".equals(status)) {
            return LocalDate.now().plusDays(1 + random.nextInt(25));
        }
        if ("concluida".equals(status)) {
            return LocalDate.now().minusDays(7 + random.nextInt(110));
        }
        return LocalDate.now().plusDays(random.nextInt(35) - 10L - (indice % 3));
    }

    private LocalDate dataRetirada(String status, int indice) {
        if ("ativa".equals(status)) {
            return LocalDate.now().minusDays(random.nextInt(8));
        }
        return LocalDate.now().minusDays(5 + random.nextInt(120) + (indice % 4));
    }

    private LocalDate dataManutencao(String status, int indice) {
        if ("em_andamento".equals(status)) {
            return LocalDate.now().minusDays(random.nextInt(12));
        }
        return LocalDate.now().minusDays(10 + random.nextInt(160) + (indice % 5));
    }

    private String observacaoReserva(String status) {
        String[] ativas = {"Reserva confirmada para obra", "Cliente aguardando retirada", "Locacao programada"};
        String[] concluidas = {"Reserva atendida sem ocorrencias", "Periodo finalizado", "Reserva convertida em aluguel"};
        String[] canceladas = {"Cancelada por remarcacao da obra", "Cliente desistiu da agenda", "Cancelada por conflito de data"};
        if ("ativa".equals(status)) {
            return sortear(ativas);
        }
        if ("concluida".equals(status)) {
            return sortear(concluidas);
        }
        return sortear(canceladas);
    }

    private String observacaoRetirada(String status) {
        String[] ativas = {"Equipamento em uso pelo cliente", "Retirada recente", "Locacao em andamento"};
        String[] concluidas = {"Aluguel finalizado", "Equipamento retornou em boas condicoes", "Contrato encerrado"};
        return "ativa".equals(status) ? sortear(ativas) : sortear(concluidas);
    }

    private String observacaoDevolucao() {
        String[] observacoes = {"Devolucao sem avarias", "Conferido no balcao", "Limpeza simples solicitada",
                "Retorno dentro do prazo", "Retorno com acessorios conferidos"};
        return sortear(observacoes);
    }

    private String observacaoManutencao(String status) {
        String[] andamento = {"Revisao preventiva em andamento", "Troca de componentes em avaliacao",
                "Teste de funcionamento pendente"};
        String[] concluidas = {"Revisao preventiva concluida", "Ajuste mecanico finalizado",
                "Limpeza tecnica e teste final", "Substituicao de peca concluida"};
        return "em_andamento".equals(status) ? sortear(andamento) : sortear(concluidas);
    }

    private String categoriaPorNome(String nome) {
        if (nome.contains("Gerador")) {
            return "Energia";
        }
        if (nome.contains("Lavadora")) {
            return "Jardinagem";
        }
        if (nome.contains("Betoneira") || nome.contains("Andaime") || nome.contains("Vibrador")) {
            return "Construcao";
        }
        return "Ferramentas";
    }

    private BigDecimal valorDiariaPorNome(String nome) {
        int base;
        if (nome.contains("Gerador 8")) {
            base = 260;
        } else if (nome.contains("Gerador")) {
            base = 200;
        } else if (nome.contains("Betoneira")) {
            base = 140;
        } else if (nome.contains("Compressor")) {
            base = 110;
        } else if (nome.contains("Andaime")) {
            base = 85;
        } else if (nome.contains("Martelete")) {
            base = 75;
        } else {
            base = 55;
        }
        return BigDecimal.valueOf(base + random.nextInt(31) - 10).setScale(2, RoundingMode.HALF_UP);
    }

    private String gerarCpf() {
        return numero(3) + "." + numero(3) + "." + numero(3) + "-" + numero(2);
    }

    private String numero(int tamanho) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tamanho; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    private String sufixo() {
        return String.valueOf(System.nanoTime()).substring(8);
    }

    private String normalizar(String texto) {
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcento.replace(" ", ".");
    }

    private String sortear(String[] opcoes) {
        return opcoes[random.nextInt(opcoes.length)];
    }
}
