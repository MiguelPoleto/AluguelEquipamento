package br.com.aluguelequipamento.model.dao;

import java.sql.Date;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import br.com.aluguelequipamento.model.domain.Reserva;


/*
    Miguel
*/
public class ReservaDAO {
   private static final String SQL_BASE =
        "SELECT r.*, c.nome AS nome_cliente, e.nome AS nome_equipamento " +
        "FROM reserva r " +
        "JOIN cliente c ON c.id = r.cliente_id " +
        "JOIN equipamento e ON e.id = r.equipamento_id ";

    public List<Reserva> listar() throws SQLException {
        List<Reserva> lista = new ArrayList<>();
        try (Statement st = ConexaoDAO.getConexao().createStatement();
             ResultSet rs = st.executeQuery(SQL_BASE + "ORDER BY r.data_cadastro DESC")) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public List<Reserva> listarAtivas() throws SQLException {
        List<Reserva> lista = new ArrayList<>();
        try (Statement st = ConexaoDAO.getConexao().createStatement();
             ResultSet rs = st.executeQuery(SQL_BASE + "WHERE r.status = \'ativa\' ORDER BY r.data_inicio")) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public Reserva buscarPorId(int id) throws SQLException {
        String sql = SQL_BASE + "WHERE r.id = ?";
        try (PreparedStatement ps = ConexaoDAO.getConexao().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        }
        return null;
    }

    /**
     * Verifica conflito de reserva ativa para o equipamento no periodo.
     * RN: Um item nao pode ser alugado se ja existir reserva de outro cliente no periodo.
     */
    public boolean existeConflito(int equipamentoId, java.time.LocalDate inicio,
                                   java.time.LocalDate fim, int clienteId, int idIgnorar) throws SQLException {
        String sql = "SELECT id FROM reserva WHERE equipamento_id = ? AND cliente_id <> ? " +
                     "AND id <> ? AND status = \'ativa\' " +
                     "AND NOT (data_fim < ? OR data_inicio > ?)";
        try (PreparedStatement ps = ConexaoDAO.getConexao().prepareStatement(sql)) {
            ps.setInt(1, equipamentoId);
            ps.setInt(2, clienteId);
            ps.setInt(3, idIgnorar);
            ps.setDate(4, Date.valueOf(inicio));
            ps.setDate(5, Date.valueOf(fim));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void inserir(Reserva r) throws SQLException {
        Connection conn = ConexaoDAO.getConexao();
        boolean autoCommitOriginal = conn.getAutoCommit();
        try {
            conn.setAutoCommit(false);
            validarCliente(conn, r.getClienteId());
            validarEquipamento(conn, r.getEquipamentoId());
            if (existeConflito(conn, r.getEquipamentoId(), r.getDataInicio(), r.getDataFim(), r.getClienteId(), 0)) {
                throw new SQLException("Equipamento ja possui reserva ativa de outro cliente neste periodo.");
            }

            inserir(conn, r);
            if ("ativa".equals(r.getStatus())) {
                atualizarStatusEquipamento(conn, r.getEquipamentoId(), "reservado");
            }
            conn.commit();
        } catch (SQLException ex) {
            conn.rollback();
            throw ex;
        } finally {
            conn.setAutoCommit(autoCommitOriginal);
        }
    }

    private void inserir(Connection conn, Reserva r) throws SQLException {
        String sql = "INSERT INTO reserva (cliente_id, equipamento_id, data_inicio, data_fim, status, observacao) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, r.getClienteId());
            ps.setInt(2, r.getEquipamentoId());
            ps.setDate(3, Date.valueOf(r.getDataInicio()));
            ps.setDate(4, Date.valueOf(r.getDataFim()));
            ps.setString(5, r.getStatus());
            ps.setString(6, r.getObservacao());
            ps.executeUpdate();
        }
    }

    public void alterar(Reserva r) throws SQLException {
        Connection conn = ConexaoDAO.getConexao();
        boolean autoCommitOriginal = conn.getAutoCommit();
        try {
            conn.setAutoCommit(false);
            validarCliente(conn, r.getClienteId());
            validarEquipamento(conn, r.getEquipamentoId());
            if ("ativa".equals(r.getStatus())
                    && existeConflito(conn, r.getEquipamentoId(), r.getDataInicio(), r.getDataFim(), r.getClienteId(), r.getId())) {
                throw new SQLException("Equipamento ja possui reserva ativa de outro cliente neste periodo.");
            }

            int equipamentoAnteriorId = buscarEquipamentoId(conn, r.getId());
            alterar(conn, r);

            if ("ativa".equals(r.getStatus())) {
                atualizarStatusEquipamento(conn, r.getEquipamentoId(), "reservado");
            }
            if (equipamentoAnteriorId != r.getEquipamentoId()) {
                liberarEquipamentoSemReservaAtiva(conn, equipamentoAnteriorId);
            }
            if (!"ativa".equals(r.getStatus())) {
                liberarEquipamentoSemReservaAtiva(conn, r.getEquipamentoId());
            }
            conn.commit();
        } catch (SQLException ex) {
            conn.rollback();
            throw ex;
        } finally {
            conn.setAutoCommit(autoCommitOriginal);
        }
    }

    private void alterar(Connection conn, Reserva r) throws SQLException {
        String sql = "UPDATE reserva SET cliente_id=?, equipamento_id=?, data_inicio=?, " +
                     "data_fim=?, status=?, observacao=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, r.getClienteId());
            ps.setInt(2, r.getEquipamentoId());
            ps.setDate(3, Date.valueOf(r.getDataInicio()));
            ps.setDate(4, Date.valueOf(r.getDataFim()));
            ps.setString(5, r.getStatus());
            ps.setString(6, r.getObservacao());
            ps.setInt(7, r.getId());
            ps.executeUpdate();
        }
    }

    public void atualizarStatus(int id, String status) throws SQLException {
        String sql = "UPDATE reserva SET status = ? WHERE id = ?";
        try (PreparedStatement ps = ConexaoDAO.getConexao().prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void excluir(int id) throws SQLException {
        Connection conn = ConexaoDAO.getConexao();
        boolean autoCommitOriginal = conn.getAutoCommit();
        try {
            conn.setAutoCommit(false);
            int equipamentoId = buscarEquipamentoId(conn, id);
            String sql = "DELETE FROM reserva WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            liberarEquipamentoSemReservaAtiva(conn, equipamentoId);
            conn.commit();
        } catch (SQLException ex) {
            conn.rollback();
            throw ex;
        } finally {
            conn.setAutoCommit(autoCommitOriginal);
        }
    }

    private boolean existeConflito(Connection conn, int equipamentoId, java.time.LocalDate inicio,
                                   java.time.LocalDate fim, int clienteId, int idIgnorar) throws SQLException {
        String sql = "SELECT id FROM reserva WHERE equipamento_id = ? AND cliente_id <> ? " +
                     "AND id <> ? AND status = \'ativa\' AND NOT (data_fim < ? OR data_inicio > ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, equipamentoId);
            ps.setInt(2, clienteId);
            ps.setInt(3, idIgnorar);
            ps.setDate(4, Date.valueOf(inicio));
            ps.setDate(5, Date.valueOf(fim));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void validarCliente(Connection conn, int clienteId) throws SQLException {
        String sql = "SELECT id FROM cliente WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, clienteId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Cliente nao encontrado.");
                }
            }
        }
    }

    private void validarEquipamento(Connection conn, int equipamentoId) throws SQLException {
        String sql = "SELECT id FROM equipamento WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, equipamentoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Equipamento nao encontrado.");
                }
            }
        }
    }

    private int buscarEquipamentoId(Connection conn, int reservaId) throws SQLException {
        String sql = "SELECT equipamento_id FROM reserva WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reservaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("equipamento_id");
                }
            }
        }
        throw new SQLException("Reserva nao encontrada.");
    }

    private void atualizarStatusEquipamento(Connection conn, int equipamentoId, String status) throws SQLException {
        String sql = "UPDATE equipamento SET status = ? WHERE id = ? AND status = \'disponivel\'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, equipamentoId);
            ps.executeUpdate();
        }
    }

    private void liberarEquipamentoSemReservaAtiva(Connection conn, int equipamentoId) throws SQLException {
        String sqlReserva = "SELECT id FROM reserva WHERE equipamento_id = ? AND status = \'ativa\'";
        try (PreparedStatement ps = conn.prepareStatement(sqlReserva)) {
            ps.setInt(1, equipamentoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return;
                }
            }
        }
        String sqlStatus = "UPDATE equipamento SET status = \'disponivel\' WHERE id = ? AND status = \'reservado\'";
        try (PreparedStatement ps = conn.prepareStatement(sqlStatus)) {
            ps.setInt(1, equipamentoId);
            ps.executeUpdate();
        }
    }

    private Reserva mapear(ResultSet rs) throws SQLException {
        Reserva r = new Reserva();
        r.setId(rs.getInt("id"));
        r.setClienteId(rs.getInt("cliente_id"));
        r.setNomeCliente(rs.getString("nome_cliente"));
        r.setEquipamentoId(rs.getInt("equipamento_id"));
        r.setNomeEquipamento(rs.getString("nome_equipamento"));
        Date di = rs.getDate("data_inicio");
        if (di != null) r.setDataInicio(di.toLocalDate());
        Date df = rs.getDate("data_fim");
        if (df != null) r.setDataFim(df.toLocalDate());
        r.setStatus(rs.getString("status"));
        r.setObservacao(rs.getString("observacao"));
        Timestamp ts = rs.getTimestamp("data_cadastro");
        if (ts != null) r.setDataCadastro(ts.toLocalDateTime());
        return r;
    } 
}
