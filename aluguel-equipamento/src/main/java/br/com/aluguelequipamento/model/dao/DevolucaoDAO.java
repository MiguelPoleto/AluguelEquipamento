package br.com.aluguelequipamento.model.dao;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import br.com.aluguelequipamento.model.domain.Devolucao;


/*
    Alessandro
*/
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
    java.sql.Connection conn = ConexaoDAO.getConexao();
    boolean autoCommitOriginal = conn.getAutoCommit();
    try {
        conn.setAutoCommit(false);

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

        conn.commit();
    } catch (SQLException ex) {
        conn.rollback();
        throw ex;
    } finally {
        conn.setAutoCommit(autoCommitOriginal);
    }
}

    public void alterar(Devolucao d) throws SQLException {
        String sql = "UPDATE devolucao SET retirada_id=?, data_devolucao=?, " +
                     "observacao=?, status=? WHERE id=?";
        try (PreparedStatement ps = ConexaoDAO.getConexao().prepareStatement(sql)) {
            ps.setInt(1, d.getRetiradaId());
            ps.setDate(2, Date.valueOf(d.getDataDevolucao()));
            ps.setString(3, d.getObservacao());
            ps.setString(4, d.getStatus());
            ps.setInt(5, d.getId());
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
        d.setObservacao(rs.getString("observacao"));
        d.setStatus(rs.getString("status"));
        return d;
    }
}
