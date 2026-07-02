package br.com.aluguelequipamento.model.dao;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import br.com.aluguelequipamento.model.domain.Devolucao;

/*
    Alessandro
*/
public class DevolucaoDAO {
    private static final String SQL_BASE =
        "SELECT d.*, " +
        "       (c.nome || ' / ' || e.nome || ' (' || rt.data_retirada::text || ')') AS descricao_retirada " +
        "FROM devolucao d " +
        "JOIN retirada rt ON rt.id = d.retirada_id " +
        "JOIN cliente c ON c.id = rt.cliente_id " +
        "JOIN equipamento e ON e.id = rt.equipamento_id ";

    public List<Devolucao> listar() throws SQLException {
        List<Devolucao> lista = new ArrayList<>();
        try (Statement st = ConexaoDAO.getConexao().createStatement();
             ResultSet rs = st.executeQuery(SQL_BASE + "ORDER BY d.data_devolucao DESC")) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public Devolucao buscarPorId(int id) throws SQLException {
        String sql = SQL_BASE + "WHERE d.id = ?";
        try (PreparedStatement ps = ConexaoDAO.getConexao().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        }
        return null;
    }

    public boolean retiradaJaDevolvida(int retiradaId) throws SQLException {
        String sql = "SELECT id FROM devolucao WHERE retirada_id = ?";
        try (PreparedStatement ps = ConexaoDAO.getConexao().prepareStatement(sql)) {
            ps.setInt(1, retiradaId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void inserir(Devolucao d) throws SQLException {
        java.sql.Connection conn = ConexaoDAO.getConexao();
        boolean autoCommitOriginal = conn.getAutoCommit();
        try {
            conn.setAutoCommit(false);
            validarRetiradaParaDevolucao(conn, d);

            // Busca informações da retirada para calcular o novo valor total com base no tempo real
            LocalDate dataRetirada = null;
            java.math.BigDecimal valorDiaria = null;
            String sqlInfo = "SELECT r.data_retirada, e.valor_diaria FROM retirada r JOIN equipamento e ON e.id = r.equipamento_id WHERE r.id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlInfo)) {
                ps.setInt(1, d.getRetiradaId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Date dr = rs.getDate("data_retirada");
                        if (dr != null) dataRetirada = dr.toLocalDate();
                        valorDiaria = rs.getBigDecimal("valor_diaria");
                    }
                }
            }

            // 1) Insere a devolução
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO devolucao (retirada_id, data_devolucao, observacao, status) " +
                    "VALUES (?, ?, ?, ?)")) {
                ps.setInt(1, d.getRetiradaId());
                ps.setDate(2, Date.valueOf(d.getDataDevolucao()));
                ps.setString(3, d.getObservacao());
                ps.setString(4, d.getStatus());
                ps.executeUpdate();
            }

            // 2) Marca a retirada como "concluida"
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE retirada SET status = 'concluida' WHERE id = ?")) {
                ps.setInt(1, d.getRetiradaId());
                ps.executeUpdate();
            }

            // 3) Libera o equipamento para "disponivel"
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE equipamento SET status = 'disponivel' " +
                    "WHERE id = (SELECT equipamento_id FROM retirada WHERE id = ?) " +
                    "AND status = 'alugado'")) {
                ps.setInt(1, d.getRetiradaId());
                ps.executeUpdate();
            }

            // 4) Atualiza o valor total da retirada conforme a data de devolução real
            if (dataRetirada != null && valorDiaria != null) {
                long dias = java.time.temporal.ChronoUnit.DAYS.between(dataRetirada, d.getDataDevolucao());
                if (dias == 0) dias = 1;
                java.math.BigDecimal novoValorTotal = valorDiaria.multiply(java.math.BigDecimal.valueOf(dias));

                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE retirada SET valor_total = ? WHERE id = ?")) {
                    ps.setBigDecimal(1, novoValorTotal);
                    ps.setInt(2, d.getRetiradaId());
                    ps.executeUpdate();
                }
            }

            conn.commit();
        } catch (SQLException ex) {
            conn.rollback();
            throw ex;
        } finally {
            conn.setAutoCommit(autoCommitOriginal);
        }
    }

    public void alterar(Devolucao d) throws SQLException {
        java.sql.Connection conn = ConexaoDAO.getConexao();
        boolean autoCommitOriginal = conn.getAutoCommit();
        try {
            conn.setAutoCommit(false);

            // Busca informações da retirada para recalcular o valor com base na nova data de devolução
            LocalDate dataRetirada = null;
            java.math.BigDecimal valorDiaria = null;
            String sqlInfo = "SELECT r.data_retirada, e.valor_diaria FROM retirada r JOIN equipamento e ON e.id = r.equipamento_id WHERE r.id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlInfo)) {
                ps.setInt(1, d.getRetiradaId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Date dr = rs.getDate("data_retirada");
                        if (dr != null) dataRetirada = dr.toLocalDate();
                        valorDiaria = rs.getBigDecimal("valor_diaria");
                    }
                }
            }

            String sql = "UPDATE devolucao SET retirada_id=?, data_devolucao=?, " +
                         "observacao=?, status=? WHERE id=?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, d.getRetiradaId());
                ps.setDate(2, Date.valueOf(d.getDataDevolucao()));
                ps.setString(3, d.getObservacao());
                ps.setString(4, d.getStatus());
                ps.setInt(5, d.getId());
                ps.executeUpdate();
            }

            // Atualiza o valor total da retirada
            if (dataRetirada != null && valorDiaria != null) {
                long dias = java.time.temporal.ChronoUnit.DAYS.between(dataRetirada, d.getDataDevolucao());
                if (dias == 0) dias = 1;
                java.math.BigDecimal novoValorTotal = valorDiaria.multiply(java.math.BigDecimal.valueOf(dias));

                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE retirada SET valor_total = ? WHERE id = ?")) {
                    ps.setBigDecimal(1, novoValorTotal);
                    ps.setInt(2, d.getRetiradaId());
                    ps.executeUpdate();
                }
            }

            conn.commit();
        } catch (SQLException ex) {
            conn.rollback();
            throw ex;
        } finally {
            conn.setAutoCommit(autoCommitOriginal);
        }
    }

    public void excluir(int id) throws SQLException {
        java.sql.Connection conn = ConexaoDAO.getConexao();
        boolean autoCommitOriginal = conn.getAutoCommit();
        try {
            conn.setAutoCommit(false);
            int retiradaId = buscarRetiradaId(conn, id);

            // Busca informações para restaurar o valor original planejado da retirada
            LocalDate dataRetirada = null;
            LocalDate dataPrevDevolucao = null;
            java.math.BigDecimal valorDiaria = null;
            String sqlInfo = "SELECT r.data_retirada, r.data_prev_devolucao, e.valor_diaria " +
                             "FROM retirada r JOIN equipamento e ON e.id = r.equipamento_id WHERE r.id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlInfo)) {
                ps.setInt(1, retiradaId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Date dr = rs.getDate("data_retirada");
                        if (dr != null) dataRetirada = dr.toLocalDate();
                        Date dp = rs.getDate("data_prev_devolucao");
                        if (dp != null) dataPrevDevolucao = dp.toLocalDate();
                        valorDiaria = rs.getBigDecimal("valor_diaria");
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM devolucao WHERE id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE retirada SET status = 'ativa' WHERE id = ?")) {
                ps.setInt(1, retiradaId);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE equipamento SET status = 'alugado' " +
                    "WHERE id = (SELECT equipamento_id FROM retirada WHERE id = ?) " +
                    "AND status = 'disponivel'")) {
                ps.setInt(1, retiradaId);
                ps.executeUpdate();
            }

            // Restaura o valor original baseado no planejamento inicial
            if (dataRetirada != null && dataPrevDevolucao != null && valorDiaria != null) {
                long dias = java.time.temporal.ChronoUnit.DAYS.between(dataRetirada, dataPrevDevolucao);
                if (dias == 0) dias = 1;
                java.math.BigDecimal valorOriginal = valorDiaria.multiply(java.math.BigDecimal.valueOf(dias));

                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE retirada SET valor_total = ? WHERE id = ?")) {
                    ps.setBigDecimal(1, valorOriginal);
                    ps.setInt(2, retiradaId);
                    ps.executeUpdate();
                }
            }

            conn.commit();
        } catch (SQLException ex) {
            conn.rollback();
            throw ex;
        } finally {
            conn.setAutoCommit(autoCommitOriginal);
        }
    }

    private void validarRetiradaParaDevolucao(java.sql.Connection conn, Devolucao d) throws SQLException {
        if (retiradaJaDevolvida(d.getRetiradaId())) {
            throw new SQLException("Retirada ja possui devolucao registrada.");
        }

        String sql = "SELECT data_retirada, status FROM retirada WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, d.getRetiradaId());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Retirada nao encontrada.");
                }
                Date dataRetirada = rs.getDate("data_retirada");
                if (dataRetirada != null && d.getDataDevolucao().isBefore(dataRetirada.toLocalDate())) {
                    throw new SQLException("Data de devolucao nao pode ser anterior a data de retirada.");
                }
                if (!"ativa".equals(rs.getString("status"))) {
                    throw new SQLException("Apenas retiradas ativas podem ser devolvidas.");
                }
            }
        }
    }

    private int buscarRetiradaId(java.sql.Connection conn, int devolucaoId) throws SQLException {
        String sql = "SELECT retirada_id FROM devolucao WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, devolucaoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("retirada_id");
                }
            }
        }
        throw new SQLException("Devolucao nao encontrada.");
    }

    private Devolucao mapear(ResultSet rs) throws SQLException {
        Devolucao d = new Devolucao();
        d.setId(rs.getInt("id"));
        d.setRetiradaId(rs.getInt("retirada_id"));
        d.setDescricaoRetirada(rs.getString("descricao_retirada"));
        Date dd = rs.getDate("data_devolucao");
        if (dd != null) d.setDataDevolucao(dd.toLocalDate());
        d.setObservacao(rs.getString("observacao"));
        d.setStatus(rs.getString("status"));
        return d;
    }
}
