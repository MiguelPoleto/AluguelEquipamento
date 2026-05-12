package br.com.aluguelequipamento.controller;

import br.com.App;
import br.com.aluguelequipamento.model.dao.ManutencaoDAO;

import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

/*
    Miguel
*/
public class GraficoManutencoesController {

    @FXML private BarChart<String, Number> chartManutencoesEquipamento;
    @FXML private PieChart chartManutencoesStatus;
    @FXML private Label lblManutencoesTotal;
    @FXML private Label lblErro;

    private final ManutencaoDAO manutencaoDAO = new ManutencaoDAO();

    @FXML
    public void initialize() {
        carregarGraficos();
    }

    @FXML
    private void carregarGraficos() {
        lblErro.setText("");
        chartManutencoesEquipamento.getData().clear();
        chartManutencoesStatus.getData().clear();

        try {
            Map<String, Integer> manutencoesPorStatus = manutencaoDAO.contarPorStatus();
            Map<String, Integer> manutencoesPorEquipamento = manutencaoDAO.contarPorEquipamento();

            preencherPizza(chartManutencoesStatus, manutencoesPorStatus);
            preencherBarra("Manutenções", manutencoesPorEquipamento);
            lblManutencoesTotal.setText(totalizar(manutencoesPorStatus) + " manutenção(ões)");
        } catch (SQLException e) {
            lblErro.setText("Erro ao carregar gráfico de manutenções: " + e.getMessage());
        }
    }

    @FXML
    private void voltarMenu() throws IOException {
        App.setRoot("primary");
    }

    private void preencherBarra(String nomeSerie, Map<String, Integer> dados) {
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName(nomeSerie);
        dados.forEach((rotulo, total) -> serie.getData().add(new XYChart.Data<>(rotulo, total)));
        chartManutencoesEquipamento.getData().add(serie);
    }

    private void preencherPizza(PieChart chart, Map<String, Integer> dados) {
        dados.forEach((status, total) -> chart.getData().add(new PieChart.Data(formatarStatus(status), total)));
    }

    private int totalizar(Map<String, Integer> dados) {
        return dados.values().stream().mapToInt(Integer::intValue).sum();
    }

    private String formatarStatus(String status) {
        if (status == null || status.isBlank()) {
            return "";
        }
        String texto = status.replace("_", " ");
        return texto.substring(0, 1).toUpperCase() + texto.substring(1);
    }
}
