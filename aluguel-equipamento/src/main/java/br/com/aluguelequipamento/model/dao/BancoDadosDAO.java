package br.com.aluguelequipamento.model.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class BancoDadosDAO {

    private static final String TODAS = "todas";
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
        String[] nomes = {"Miguel", "Alessandro", "Laura", "Rafael", "Bianca", "Gustavo", "Camila", "Diego"};
        String[] sobrenomes = {"Silva", "Mion", "Poleto", "Costa", "Souza", "Oliveira", "Santos", "Almeida"};
        String sql = "INSERT INTO cliente (nome, cpf, telefone, email, endereco, data_cadastro) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < quantidade; i++) {
                String nome = nomes[random.nextInt(nomes.length)] + " "
                        + sobrenomes[random.nextInt(sobrenomes.length)] + " " + sufixo();
                String cpf = gerarCpf();
                ps.setString(1, nome);
                ps.setString(2, cpf);
                ps.setString(3, "(27) 9" + numero(8));
                ps.setString(4, normalizar(nome).toLowerCase() + "@email.com");
                ps.setString(5, "Rua " + (100 + random.nextInt(900)) + ", Centro");
                ps.setDate(6, Date.valueOf(LocalDate.now().minusDays(random.nextInt(60))));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void inserirEquipamentos(Connection conn, int quantidade) throws SQLException {
        String[] nomes = {"Betoneira", "Compressor", "Gerador", "Martelete", "Andaime", "Furadeira", "Lixadeira", "Serra"};
        String[] categorias = {"Construção", "Ferramentas", "Energia", "Jardinagem", "Eventos e Festas"};
        String sql = "INSERT INTO equipamento (nome, descricao, categoria, valor_diaria, status, data_cadastro) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < quantidade; i++) {
                String nome = nomes[random.nextInt(nomes.length)] + " Modelo " + sufixo();
                ps.setString(1, nome);
                ps.setString(2, "Equipamento ficticio para testes - " + nome);
                ps.setString(3, categorias[random.nextInt(categorias.length)]);
                ps.setBigDecimal(4, BigDecimal.valueOf(40 + random.nextInt(260)));
                ps.setString(5, "disponivel");
                ps.setDate(6, Date.valueOf(LocalDate.now().minusDays(random.nextInt(90))));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void inserirReservas(Connection conn, int quantidade) throws SQLException {
        garantirClientes(conn, quantidade);
        garantirEquipamentosDisponiveis(conn, quantidade);
        List<Integer> clientes = buscarIds(conn, "cliente");
        List<Integer> equipamentos = buscarIdsPorStatus(conn, "disponivel");

        String sql = "INSERT INTO reserva (cliente_id, equipamento_id, data_inicio, data_fim, status, observacao) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < quantidade; i++) {
                int equipamentoId = equipamentos.get(i % equipamentos.size());
                LocalDate inicio = LocalDate.now().plusDays(3L + (i * 5L));
                String status = i % 3 == 0 ? "cancelada" : "ativa";
                ps.setInt(1, clientes.get(i % clientes.size()));
                ps.setInt(2, equipamentoId);
                ps.setDate(3, Date.valueOf(inicio));
                ps.setDate(4, Date.valueOf(inicio.plusDays(2 + random.nextInt(5))));
                ps.setString(5, status);
                ps.setString(6, "Reserva ficticia " + sufixo());
                ps.addBatch();
                if ("ativa".equals(status)) {
                    atualizarStatusEquipamento(conn, equipamentoId, "reservado");
                }
            }
            ps.executeBatch();
        }
    }

    private void inserirRetiradas(Connection conn, int quantidade) throws SQLException {
        garantirClientes(conn, quantidade);
        garantirEquipamentosDisponiveis(conn, quantidade);
        List<Integer> clientes = buscarIds(conn, "cliente");
        List<Integer> equipamentos = buscarIdsPorStatus(conn, "disponivel");

        String sql = "INSERT INTO retirada (reserva_id, cliente_id, equipamento_id, data_retirada, "
                + "data_prev_devolucao, valor_total, status, observacao) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < quantidade; i++) {
                int equipamentoId = equipamentos.get(i % equipamentos.size());
                LocalDate retirada = LocalDate.now().minusDays(i + 1L);
                String status = i % 2 == 0 ? "ativa" : "concluida";
                ps.setNull(1, Types.INTEGER);
                ps.setInt(2, clientes.get(i % clientes.size()));
                ps.setInt(3, equipamentoId);
                ps.setDate(4, Date.valueOf(retirada));
                ps.setDate(5, Date.valueOf(retirada.plusDays(4 + random.nextInt(6))));
                ps.setBigDecimal(6, BigDecimal.valueOf(120 + random.nextInt(800)));
                ps.setString(7, status);
                ps.setString(8, "Retirada ficticia " + sufixo());
                ps.addBatch();
                if ("ativa".equals(status)) {
                    atualizarStatusEquipamento(conn, equipamentoId, "alugado");
                }
            }
            ps.executeBatch();
        }
    }

    private void inserirDevolucoes(Connection conn, int quantidade) throws SQLException {
        for (int i = 0; i < quantidade; i++) {
            int retiradaId = buscarRetiradaSemDevolucao(conn);
            if (retiradaId == 0) {
                inserirRetiradas(conn, 1);
                retiradaId = buscarRetiradaSemDevolucao(conn);
            }
            String sql = "INSERT INTO devolucao (retirada_id, data_devolucao, observacao, status) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, retiradaId);
                ps.setDate(2, Date.valueOf(LocalDate.now().minusDays(random.nextInt(10))));
                ps.setString(3, "Devolucao ficticia " + sufixo());
                ps.setString(4, "concluida");
                ps.executeUpdate();
            }
            atualizarRetiradaConcluida(conn, retiradaId);
        }
    }

    private void inserirManutencoes(Connection conn, int quantidade) throws SQLException {
        garantirEquipamentosDisponiveis(conn, quantidade);
        List<Integer> equipamentos = buscarIdsPorStatus(conn, "disponivel");
        int emAndamento = contarStatus(conn, "manutencao", "em_andamento");

        String sql = "INSERT INTO manutencao (equipamento_id, descricao, data_inicio, data_previsao, data_fim, status) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < quantidade; i++) {
                int equipamentoId = equipamentos.get(i % equipamentos.size());
                boolean criarEmAndamento = emAndamento < 10 && i % 2 == 0;
                String status = criarEmAndamento ? "em_andamento" : "concluida";
                LocalDate inicio = LocalDate.now().minusDays(2L + i);
                ps.setInt(1, equipamentoId);
                ps.setString(2, "Manutencao ficticia do equipamento " + equipamentoId + " - " + sufixo());
                ps.setDate(3, Date.valueOf(inicio));
                ps.setDate(4, Date.valueOf(inicio.plusDays(5)));
                if ("concluida".equals(status)) {
                    ps.setDate(5, Date.valueOf(inicio.plusDays(3)));
                } else {
                    ps.setNull(5, Types.DATE);
                    atualizarStatusEquipamento(conn, equipamentoId, "em_manutencao");
                    emAndamento++;
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
                + "WHERE d.id IS NULL ORDER BY r.id LIMIT 1";
        try (Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt("id") : 0;
        }
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
        String sql = "UPDATE retirada SET status = \'concluida\' WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, retiradaId);
            ps.executeUpdate();
        }
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
        return texto.replace(" ", ".")
                .replace("ç", "c")
                .replace("ã", "a")
                .replace("é", "e");
    }
}
