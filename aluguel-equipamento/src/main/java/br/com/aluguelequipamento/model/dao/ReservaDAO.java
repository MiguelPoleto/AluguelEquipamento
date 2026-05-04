package br.com.aluguelequipamento.model.dao;

import java.sql.Date;
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
                                   java.time.LocalDate fim, int idIgnorar) throws SQLException {
        String sql = "SELECT id FROM reserva WHERE equipamento_id = ? AND id <> ? AND status = \'ativa\' " +
                     "AND NOT (data_fim < ? OR data_inicio > ?)";
        try (PreparedStatement ps = ConexaoDAO.getConexao().prepareStatement(sql)) {
            ps.setInt(1, equipamentoId);
            ps.setInt(2, idIgnorar);
            ps.setDate(3, Date.valueOf(inicio));
            ps.setDate(4, Date.valueOf(fim));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void inserir(Reserva r) throws SQLException {
        String sql = "INSERT INTO reserva (cliente_id, equipamento_id, data_inicio, data_fim, status, observacao) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = ConexaoDAO.getConexao().prepareStatement(sql)) {
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
        String sql = "UPDATE reserva SET cliente_id=?, equipamento_id=?, data_inicio=?, " +
                     "data_fim=?, status=?, observacao=? WHERE id=?";
        try (PreparedStatement ps = ConexaoDAO.getConexao().prepareStatement(sql)) {
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
        String sql = "DELETE FROM reserva WHERE id = ?";
        try (PreparedStatement ps = ConexaoDAO.getConexao().prepareStatement(sql)) {
            ps.setInt(1, id);
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
