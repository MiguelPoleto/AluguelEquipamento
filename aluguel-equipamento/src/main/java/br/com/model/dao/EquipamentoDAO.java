package br.com.model.dao;

import java.sql.Statement;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import br.com.model.domain.Equipamento;

public class EquipamentoDAO {
     public List<Equipamento> listar() throws SQLException {
        List<Equipamento> lista = new ArrayList<>();
        String sql = "SELECT * FROM equipamento ORDER BY nome";
        try (Statement st = ConexaoDAO.getConexao().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public List<Equipamento> listarDisponiveis() throws SQLException {
        List<Equipamento> lista = new ArrayList<>();
        String sql = "SELECT * FROM equipamento WHERE status = \'disponivel\' ORDER BY nome";
        try (Statement st = ConexaoDAO.getConexao().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public Equipamento buscarPorId(int id) throws SQLException {
        String sql = "SELECT * FROM equipamento WHERE id = ?";
        try (PreparedStatement ps = ConexaoDAO.getConexao().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        }
        return null;
    }

    public void inserir(Equipamento e) throws SQLException {
        String sql = "INSERT INTO equipamento (nome, descricao, categoria, valor_diaria, status, data_cadastro) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = ConexaoDAO.getConexao().prepareStatement(sql)) {
            ps.setString(1, e.getNome());
            ps.setString(2, e.getDescricao());
            ps.setString(3, e.getCategoria());
            ps.setBigDecimal(4, e.getValorDiaria());
            ps.setString(5, e.getStatus());
            ps.setDate(6, Date.valueOf(e.getDataCadastro()));
            ps.executeUpdate();
        }
    }

    public void alterar(Equipamento e) throws SQLException {
        String sql = "UPDATE equipamento SET nome=?, descricao=?, categoria=?, " +
                     "valor_diaria=?, status=?, data_cadastro=? WHERE id=?";
        try (PreparedStatement ps = ConexaoDAO.getConexao().prepareStatement(sql)) {
            ps.setString(1, e.getNome());
            ps.setString(2, e.getDescricao());
            ps.setString(3, e.getCategoria());
            ps.setBigDecimal(4, e.getValorDiaria());
            ps.setString(5, e.getStatus());
            ps.setDate(6, Date.valueOf(e.getDataCadastro()));
            ps.setInt(7, e.getId());
            ps.executeUpdate();
        }
    }

    public void atualizarStatus(int id, String status) throws SQLException {
        String sql = "UPDATE equipamento SET status = ? WHERE id = ?";
        try (PreparedStatement ps = ConexaoDAO.getConexao().prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void excluir(int id) throws SQLException {
        String sql = "DELETE FROM equipamento WHERE id = ?";
        try (PreparedStatement ps = ConexaoDAO.getConexao().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private Equipamento mapear(ResultSet rs) throws SQLException {
        Equipamento e = new Equipamento();
        e.setId(rs.getInt("id"));
        e.setNome(rs.getString("nome"));
        e.setDescricao(rs.getString("descricao"));
        e.setCategoria(rs.getString("categoria"));
        e.setValorDiaria(rs.getBigDecimal("valor_diaria"));
        e.setStatus(rs.getString("status"));
        Date d = rs.getDate("data_cadastro");
        if (d != null) e.setDataCadastro(d.toLocalDate());
        return e;
    }

}
