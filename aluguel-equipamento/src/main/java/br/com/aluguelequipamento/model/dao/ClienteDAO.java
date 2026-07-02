package br.com.aluguelequipamento.model.dao;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import br.com.aluguelequipamento.model.domain.Cliente;


/*
    Alessandro
*/
public class ClienteDAO {
public List<Cliente> listar() throws SQLException {
        List<Cliente> lista = new ArrayList<>();
        String sql = "SELECT * FROM cliente ORDER BY nome";
        try (Statement st = ConexaoDAO.getConexao().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public Cliente buscarPorId(int id) throws SQLException {
        String sql = "SELECT * FROM cliente WHERE id = ?";
        try (PreparedStatement ps = ConexaoDAO.getConexao().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        }
        return null;
    }

    public boolean cpfExiste(String cpf, int idIgnorar) throws SQLException {
        String sql = "SELECT id FROM cliente WHERE cpf = ? AND id <> ?";
        try (PreparedStatement ps = ConexaoDAO.getConexao().prepareStatement(sql)) {
            ps.setString(1, cpf);
            ps.setInt(2, idIgnorar);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void inserir(Cliente c) throws SQLException {
        String sql = "INSERT INTO cliente (nome, cpf, telefone, email, endereco, data_cadastro) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = ConexaoDAO.getConexao().prepareStatement(sql)) {
            ps.setString(1, c.getNome());
            ps.setString(2, c.getCpf());
            ps.setString(3, c.getTelefone());
            ps.setString(4, c.getEmail());
            ps.setString(5, c.getEndereco());
            ps.setDate(6, Date.valueOf(c.getDataCadastro()));
            ps.executeUpdate();
        }
    }

    public void alterar(Cliente c) throws SQLException {
        String sql = "UPDATE cliente SET nome=?, cpf=?, telefone=?, email=?, endereco=?, data_cadastro=? WHERE id=?";
        try (PreparedStatement ps = ConexaoDAO.getConexao().prepareStatement(sql)) {
            ps.setString(1, c.getNome());
            ps.setString(2, c.getCpf());
            ps.setString(3, c.getTelefone());
            ps.setString(4, c.getEmail());
            ps.setString(5, c.getEndereco());
            ps.setDate(6, Date.valueOf(c.getDataCadastro()));
            ps.setInt(7, c.getId());
            ps.executeUpdate();
        }
    }

    public void excluir(int id) throws SQLException {
        java.sql.Connection conn = ConexaoDAO.getConexao();
        boolean autoCommitOriginal = conn.getAutoCommit();
        try {
            conn.setAutoCommit(false);

            if (temVinculos(conn, id)) {
                throw new SQLException("Não é possível excluir um cliente que possui histórico de reservas ou retiradas.");
            }

            String sql = "DELETE FROM cliente WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
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

    private boolean temVinculos(java.sql.Connection conn, int clienteId) throws SQLException {
        String sqlReserva = "SELECT id FROM reserva WHERE cliente_id = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sqlReserva)) {
            ps.setInt(1, clienteId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return true;
            }
        }
        String sqlRetirada = "SELECT id FROM retirada WHERE cliente_id = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sqlRetirada)) {
            ps.setInt(1, clienteId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return true;
            }
        }
        return false;
    }

    private Cliente mapear(ResultSet rs) throws SQLException {
        Cliente c = new Cliente();
        c.setId(rs.getInt("id"));
        c.setNome(rs.getString("nome"));
        c.setCpf(rs.getString("cpf"));
        c.setTelefone(rs.getString("telefone"));
        c.setEmail(rs.getString("email"));
        c.setEndereco(rs.getString("endereco"));
        Date d = rs.getDate("data_cadastro");
        if (d != null) c.setDataCadastro(d.toLocalDate());
        return c;
    }

}
