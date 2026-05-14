package br.com.aluguelequipamento.model.dao;

import br.com.aluguelequipamento.model.domain.RelatorioAluguel;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class RelatorioAluguelDAO {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Retorna todos os aluguéis (retiradas + devoluções) dentro do período informado,
     * ordenados por data de retirada.
     *
     * Transações realizadas:
     * (1) SELECT retirada JOIN cliente JOIN equipamento LEFT JOIN devolucao
     *     filtrado por data_retirada BETWEEN dataInicio AND dataFim
     */
    public List<RelatorioAluguel> listarPorPeriodo(LocalDate dataInicio,
                                                        LocalDate dataFim) throws SQLException {
        List<RelatorioAluguel> lista = new ArrayList<>();

        String sql = """
                SELECT
                    rt.id,
                    c.nome                          AS nome_cliente,
                    e.nome                          AS nome_equipamento,
                    rt.data_retirada,
                    rt.data_prev_devolucao,
                    d.data_devolucao,
                    rt.valor_total,
                    rt.status
                FROM retirada rt
                JOIN  cliente     c  ON c.id = rt.cliente_id
                JOIN  equipamento e  ON e.id = rt.equipamento_id
                LEFT  JOIN devolucao d ON d.retirada_id = rt.id
                WHERE rt.data_retirada BETWEEN ? AND ?
                ORDER BY rt.data_retirada
                """;

        try (PreparedStatement ps = ConexaoDAO.getConexao().prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(dataInicio));
            ps.setDate(2, Date.valueOf(dataFim));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date devDate = rs.getDate("data_devolucao");
                    String dataDev = devDate != null
                            ? devDate.toLocalDate().format(FMT)
                            : "—";

                    lista.add(new RelatorioAluguel(
                            rs.getInt("id"),
                            rs.getString("nome_cliente"),
                            rs.getString("nome_equipamento"),
                            rs.getDate("data_retirada").toLocalDate().format(FMT),
                            rs.getDate("data_prev_devolucao").toLocalDate().format(FMT),
                            dataDev,
                            rs.getBigDecimal("valor_total"),
                            rs.getString("status")
                    ));
                }
            }
        }
        return lista;
    }
}
