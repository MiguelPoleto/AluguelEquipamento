package br.com.aluguelequipamento.model.dao;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import br.com.aluguelequipamento.model.domain.Devolucao;


public class DevolucaoDAO {
    private static final String SQL_BASE =
        "SELECT d.*, " +
        "       (c.nome || \' / \' || e.nome || \' (\' || rt.data_retirada::text || \')\') AS descricao_retirada " +
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
        String sql = "INSERT INTO devolucao (retirada_id, data_devolucao, valor_multa, observacao, status) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = ConexaoDAO.getConexao().prepareStatement(sql)) {
            ps.setInt(1, d.getRetiradaId());
            ps.setDate(2, Date.valueOf(d.getDataDevolucao()));
            ps.setBigDecimal(3, d.getValorMulta());
            ps.setString(4, d.getObservacao());
            ps.setString(5, d.getStatus());
            ps.executeUpdate();
        }
    }

    public void alterar(Devolucao d) throws SQLException {
        String sql = "UPDATE devolucao SET retirada_id=?, data_devolucao=?, " +
                     "valor_multa=?, observacao=?, status=? WHERE id=?";
        try (PreparedStatement ps = ConexaoDAO.getConexao().prepareStatement(sql)) {
            ps.setInt(1, d.getRetiradaId());
            ps.setDate(2, Date.valueOf(d.getDataDevolucao()));
            ps.setBigDecimal(3, d.getValorMulta());
            ps.setString(4, d.getObservacao());
            ps.setString(5, d.getStatus());
            ps.setInt(6, d.getId());
            ps.executeUpdate();
        }
    }

    public void excluir(int id) throws SQLException {
        String sql = "DELETE FROM devolucao WHERE id = ?";
        try (PreparedStatement ps = ConexaoDAO.getConexao().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private Devolucao mapear(ResultSet rs) throws SQLException {
        Devolucao d = new Devolucao();
        d.setId(rs.getInt("id"));
        d.setRetiradaId(rs.getInt("retirada_id"));
        d.setDescricaoRetirada(rs.getString("descricao_retirada"));
        Date dd = rs.getDate("data_devolucao");
        if (dd != null) d.setDataDevolucao(dd.toLocalDate());
        d.setValorMulta(rs.getBigDecimal("valor_multa"));
        d.setObservacao(rs.getString("observacao"));
        d.setStatus(rs.getString("status"));
        return d;
    }
}
