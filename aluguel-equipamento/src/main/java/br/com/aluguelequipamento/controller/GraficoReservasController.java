package br.com.aluguelequipamento.controller;

import br.com.App;
import br.com.aluguelequipamento.model.dao.ReservaDAO;

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
public class GraficoReservasController {

    @FXML private BarChart<String, Number> chartReservasEquipamento;
    @FXML private PieChart chartReservasStatus;
    @FXML private Label lblReservasTotal;
    @FXML private Label lblErro;

    private final ReservaDAO reservaDAO = new ReservaDAO();

    @FXML
    public void initialize() {
        carregarGraficos();
    }

    @FXML
    private void carregarGraficos() {
        lblErro.setText("");
        chartReservasEquipamento.getData().clear();
        chartReservasStatus.getData().clear();

        try {
            Map<String, Integer> reservasPorStatus = reservaDAO.contarPorStatus();
            Map<String, Integer> reservasPorEquipamento = reservaDAO.contarPorEquipamento();

            preencherPizza(chartReservasStatus, reservasPorStatus);
            preencherBarra("Reservas", reservasPorEquipamento);
            lblReservasTotal.setText(totalizar(reservasPorStatus) + " reserva(s)");
        } catch (SQLException e) {
            lblErro.setText("Erro ao carregar gráfico de reservas: " + e.getMessage());
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
        chartReservasEquipamento.getData().add(serie);
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
