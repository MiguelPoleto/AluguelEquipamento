package br.com.aluguelequipamento.model.dao;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import br.com.aluguelequipamento.model.domain.Retirada;

/*
    Alessandro
*/
public class RetiradaDAO {
    private static final String SQL_BASE =
        "SELECT rt.*, c.nome AS nome_cliente, e.nome AS nome_equipamento " +
        "FROM retirada rt " +
        "JOIN cliente c ON c.id = rt.cliente_id " +
        "JOIN equipamento e ON e.id = rt.equipamento_id ";

    public List<Retirada> listar() throws SQLException {
        List<Retirada> lista = new ArrayList<>();
        try (Statement st = ConexaoDAO.getConexao().createStatement();
             ResultSet rs = st.executeQuery(SQL_BASE + "ORDER BY rt.data_retirada DESC")) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public List<Retirada> listarAtivas() throws SQLException {
        List<Retirada> lista = new ArrayList<>();
        try (Statement st = ConexaoDAO.getConexao().createStatement();
             ResultSet rs = st.executeQuery(SQL_BASE + "WHERE rt.status = \'ativa\' ORDER BY rt.data_retirada")) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public Retirada buscarPorId(int id) throws SQLException {
        String sql = SQL_BASE + "WHERE rt.id = ?";
        try (PreparedStatement ps = ConexaoDAO.getConexao().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        }
        return null;
    }

    public void inserir(Retirada r) throws SQLException {
        String sql = "INSERT INTO retirada (reserva_id, cliente_id, equipamento_id, " +
                     "data_retirada, data_prev_devolucao, valor_total, status, observacao) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = ConexaoDAO.getConexao().prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {
            if (r.getReservaId() != null) ps.setInt(1, r.getReservaId());
            else ps.setNull(1, Types.INTEGER);
            ps.setInt(2, r.getClienteId());
            ps.setInt(3, r.getEquipamentoId());
            ps.setDate(4, Date.valueOf(r.getDataRetirada()));
            ps.setDate(5, Date.valueOf(r.getDataPrevDevolucao()));
            ps.setBigDecimal(6, r.getValorTotal());
            ps.setString(7, r.getStatus());
            ps.setString(8, r.getObservacao());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) r.setId(keys.getInt(1));
            }
        }
    }

    public void alterar(Retirada r) throws SQLException {
        String sql = "UPDATE retirada SET reserva_id=?, cliente_id=?, equipamento_id=?, " +
                     "data_retirada=?, data_prev_devolucao=?, valor_total=?, status=?, observacao=? WHERE id=?";
        try (PreparedStatement ps = ConexaoDAO.getConexao().prepareStatement(sql)) {
            if (r.getReservaId() != null) ps.setInt(1, r.getReservaId());
            else ps.setNull(1, Types.INTEGER);
            ps.setInt(2, r.getClienteId());
            ps.setInt(3, r.getEquipamentoId());
            ps.setDate(4, Date.valueOf(r.getDataRetirada()));
            ps.setDate(5, Date.valueOf(r.getDataPrevDevolucao()));
            ps.setBigDecimal(6, r.getValorTotal());
            ps.setString(7, r.getStatus());
            ps.setString(8, r.getObservacao());
            ps.setInt(9, r.getId());
            ps.executeUpdate();
        }
    }

    public void atualizarStatus(int id, String status) throws SQLException {
        String sql = "UPDATE retirada SET status = ? WHERE id = ?";
        try (PreparedStatement ps = ConexaoDAO.getConexao().prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void excluir(int id) throws SQLException {
        String sql = "DELETE FROM retirada WHERE id = ?";
        try (PreparedStatement ps = ConexaoDAO.getConexao().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private Retirada mapear(ResultSet rs) throws SQLException {
        Retirada r = new Retirada();
        r.setId(rs.getInt("id"));
        int resId = rs.getInt("reserva_id");
        if (!rs.wasNull()) r.setReservaId(resId);
        r.setClienteId(rs.getInt("cliente_id"));
        r.setNomeCliente(rs.getString("nome_cliente"));
        r.setEquipamentoId(rs.getInt("equipamento_id"));
        r.setNomeEquipamento(rs.getString("nome_equipamento"));
        Date dr = rs.getDate("data_retirada");
        if (dr != null) r.setDataRetirada(dr.toLocalDate());
        Date dd = rs.getDate("data_prev_devolucao");
        if (dd != null) r.setDataPrevDevolucao(dd.toLocalDate());
        r.setValorTotal(rs.getBigDecimal("valor_total"));
        r.setStatus(rs.getString("status"));
        r.setObservacao(rs.getString("observacao"));
        return r;
    }
}
