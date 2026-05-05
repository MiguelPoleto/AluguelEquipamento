package br.com.aluguelequipamento.model.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import br.com.aluguelequipamento.model.domain.Manutencao;

/*
    Miguel
*/
public class ManutencaoDAO {
    private static final String SQL_BASE = "SELECT m.*, e.nome AS nome_equipamento " +
            "FROM manutencao m " +
            "JOIN equipamento e ON e.id = m.equipamento_id ";

    public List<Manutencao> listar() throws SQLException {
        List<Manutencao> lista = new ArrayList<>();
        try (Statement st = ConexaoDAO.getConexao().createStatement();
                ResultSet rs = st.executeQuery(SQL_BASE + "ORDER BY m.data_inicio DESC")) {
            while (rs.next())
                lista.add(mapear(rs));
        }
        return lista;
    }

    public List<Manutencao> listarEmAndamento() throws SQLException {
        List<Manutencao> lista = new ArrayList<>();
        try (Statement st = ConexaoDAO.getConexao().createStatement();
                ResultSet rs = st.executeQuery(SQL_BASE + "WHERE m.status = \'em_andamento\' ORDER BY m.data_inicio")) {
            while (rs.next())
                lista.add(mapear(rs));
        }
        return lista;
    }

    public Manutencao buscarPorId(int id) throws SQLException {
        String sql = SQL_BASE + "WHERE m.id = ?";
        try (PreparedStatement ps = ConexaoDAO.getConexao().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return mapear(rs);
            }
        }
        return null;
    }

    /** RN: Maximo de 10 manutencoes em andamento simultaneamente. */
    public int contarEmAndamento() throws SQLException {
        String sql = "SELECT COUNT(*) FROM manutencao WHERE status = \'em_andamento\'";
        try (Statement st = ConexaoDAO.getConexao().createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            if (rs.next())
                return rs.getInt(1);
        }
        return 0;
    }

    public void inserir(Manutencao m) throws SQLException {
        Connection conn = ConexaoDAO.getConexao();
        boolean autoCommitOriginal = conn.getAutoCommit();
        try {
            conn.setAutoCommit(false);

            // 1) Seleciona o equipamento informado.
            validarEquipamento(conn, m.getEquipamentoId());

            // 2) Conta manutencoes em andamento para aplicar a RN do limite 10.
            int emAndamento = contarEmAndamento(conn);
            if ("em_andamento".equals(m.getStatus()) && emAndamento >= 10) {
                throw new SQLException("Limite de 10 manutencoes em andamento atingido.");
            }

            // 3) Verifica se o equipamento ja possui manutencao em andamento.
            if ("em_andamento".equals(m.getStatus())
                    && existeManutencaoEmAndamentoEquipamento(conn, m.getEquipamentoId(), 0)) {
                throw new SQLException("Equipamento ja possui manutencao em andamento.");
            }

            // 4) Insere a manutencao.
            inserir(conn, m);

            // 5) Altera o status do equipamento se a manutencao estiver em andamento.
            atualizarStatusEquipamento(conn, m.getEquipamentoId(), m.getStatus());

            conn.commit();
        } catch (SQLException ex) {
            conn.rollback();
            throw ex;
        } finally {
            conn.setAutoCommit(autoCommitOriginal);
        }
    }

    private void inserir(Connection conn, Manutencao m) throws SQLException {
        String sql = "INSERT INTO manutencao (equipamento_id, descricao, data_inicio, data_previsao, data_fim, status) "
                +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, m.getEquipamentoId());
            ps.setString(2, m.getDescricao());
            ps.setDate(3, Date.valueOf(m.getDataInicio()));
            if (m.getDataPrevisao() != null)
                ps.setDate(4, Date.valueOf(m.getDataPrevisao()));
            else
                ps.setNull(4, Types.DATE);
            if (m.getDataFim() != null)
                ps.setDate(5, Date.valueOf(m.getDataFim()));
            else
                ps.setNull(5, Types.DATE);
            ps.setString(6, m.getStatus());
            ps.executeUpdate();
        }
    }

    public void alterar(Manutencao m) throws SQLException {
        String sql = "UPDATE manutencao SET equipamento_id=?, descricao=?, data_inicio=?, " +
                "data_previsao=?, data_fim=?, status=? WHERE id=?";

        try (PreparedStatement ps = ConexaoDAO.getConexao().prepareStatement(sql)) {
            ps.setInt(1, m.getEquipamentoId());
            ps.setString(2, m.getDescricao());
            ps.setDate(3, Date.valueOf(m.getDataInicio()));

            if (m.getDataPrevisao() != null)
                ps.setDate(4, Date.valueOf(m.getDataPrevisao()));
            else
                ps.setNull(4, Types.DATE);

            if (m.getDataFim() != null)
                ps.setDate(5, Date.valueOf(m.getDataFim()));
            else
                ps.setNull(5, Types.DATE);

            ps.setString(6, m.getStatus());
            ps.setInt(7, m.getId());

            ps.executeUpdate();
        }
    }

    public void excluir(int id) throws SQLException {
        String sql = "DELETE FROM manutencao WHERE id = ?";
        try (PreparedStatement ps = ConexaoDAO.getConexao().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
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

    private int contarEmAndamento(Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM manutencao WHERE status = \'em_andamento\'";
        try (Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            if (rs.next())
                return rs.getInt(1);
        }
        return 0;
    }

    private boolean existeManutencaoEmAndamentoEquipamento(Connection conn, int equipamentoId, int idIgnorar)
            throws SQLException {
        String sql = "SELECT id FROM manutencao WHERE equipamento_id = ? AND id <> ? AND status = \'em_andamento\'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, equipamentoId);
            ps.setInt(2, idIgnorar);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void atualizarStatusEquipamento(Connection conn, int equipamentoId, String statusManutencao) throws SQLException {
        String sql = "UPDATE equipamento SET status = " +
                "CASE WHEN ? = \'em_andamento\' THEN \'em_manutencao\' ELSE status END " +
                "WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, statusManutencao);
            ps.setInt(2, equipamentoId);
            ps.executeUpdate();
        }
    }

    private Manutencao mapear(ResultSet rs) throws SQLException {
        Manutencao m = new Manutencao();
        m.setId(rs.getInt("id"));
        m.setEquipamentoId(rs.getInt("equipamento_id"));
        m.setNomeEquipamento(rs.getString("nome_equipamento"));
        m.setDescricao(rs.getString("descricao"));
        Date di = rs.getDate("data_inicio");
        if (di != null)
            m.setDataInicio(di.toLocalDate());
        Date dp = rs.getDate("data_previsao");
        if (dp != null)
            m.setDataPrevisao(dp.toLocalDate());
        Date df = rs.getDate("data_fim");
        if (df != null)
            m.setDataFim(df.toLocalDate());
        m.setStatus(rs.getString("status"));
        return m;
    }
}
