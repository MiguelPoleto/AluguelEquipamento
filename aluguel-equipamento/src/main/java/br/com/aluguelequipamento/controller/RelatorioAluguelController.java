package br.com.aluguelequipamento.controller;

import br.com.App;
import br.com.aluguelequipamento.model.dao.RelatorioAluguelDAO;
import br.com.aluguelequipamento.model.domain.RelatorioAluguel;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.view.JasperViewer;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RelatorioAluguelController {

    @FXML private DatePicker dpDataInicio;
    @FXML private DatePicker dpDataFim;
    @FXML private Label lblStatus;
    @FXML private Button btnGerar;
    @FXML private TableView<RelatorioAluguel> tableAlugueis;
    @FXML private TableColumn<RelatorioAluguel, Integer> colId;
    @FXML private TableColumn<RelatorioAluguel, String> colCliente;
    @FXML private TableColumn<RelatorioAluguel, String> colEquipamento;
    @FXML private TableColumn<RelatorioAluguel, String> colDataRetirada;
    @FXML private TableColumn<RelatorioAluguel, String> colDataPrevDevolucao;
    @FXML private TableColumn<RelatorioAluguel, String> colDataDevolucao;
    @FXML private TableColumn<RelatorioAluguel, BigDecimal> colValorTotal;
    @FXML private TableColumn<RelatorioAluguel, String> colStatus;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final RelatorioAluguelDAO dao = new RelatorioAluguelDAO();

    @FXML
    public void initialize() {
        dpDataInicio.setValue(LocalDate.now().withDayOfMonth(1));
        dpDataFim.setValue(LocalDate.now());
        configurarTabela();
        carregarTabela();
    }

    private void configurarTabela() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCliente.setCellValueFactory(new PropertyValueFactory<>("nomeCliente"));
        colEquipamento.setCellValueFactory(new PropertyValueFactory<>("nomeEquipamento"));
        colDataRetirada.setCellValueFactory(new PropertyValueFactory<>("dataRetirada"));
        colDataPrevDevolucao.setCellValueFactory(new PropertyValueFactory<>("dataPrevDevolucao"));
        colDataDevolucao.setCellValueFactory(new PropertyValueFactory<>("dataDevolucao"));
        colValorTotal.setCellValueFactory(new PropertyValueFactory<>("valorTotal"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
    }

    private void alerta(Alert.AlertType tipo, String titulo, String msg) {
        Alert a = new Alert(tipo);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    @FXML
    private void handleGerarRelatorio() {
        LocalDate inicio = dpDataInicio.getValue();
        LocalDate fim = dpDataFim.getValue();

        if (!periodoValido(inicio, fim)) {
            return;
        }

        lblStatus.setText("Gerando relatorio...");
        btnGerar.setDisable(true);

        try {
            List<RelatorioAluguel> dados = dao.listarPorPeriodo(inicio, fim);
            tableAlugueis.setItems(FXCollections.observableArrayList(dados));

            if (dados.isEmpty()) {
                alerta(Alert.AlertType.INFORMATION, "Sem dados",
                        "Nenhum aluguel encontrado para o periodo informado.");
                lblStatus.setText("Nenhum registro encontrado.");
                return;
            }

            InputStream jrxmlStream = getClass().getResourceAsStream(
                    "/br/com/aluguelequipamento/relatorios/relatorio_aluguel.jrxml");

            if (jrxmlStream == null) {
                alerta(Alert.AlertType.ERROR, "Erro",
                        "Arquivo relatorio_aluguel.jrxml nao encontrado no classpath.\n" +
                        "Verifique se o arquivo esta em src/main/resources/br/com/aluguelequipamento/relatorios/");
                return;
            }

            JasperReport jasperReport = JasperCompileManager.compileReport(jrxmlStream);

            Map<String, Object> parametros = new HashMap<>();
            parametros.put("PERIODO", inicio.format(FMT) + " a " + fim.format(FMT));

            JRMapCollectionDataSource dataSource = new JRMapCollectionDataSource(montarDataSource(dados));
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parametros, dataSource);

            JasperViewer viewer = new JasperViewer(jasperPrint, false);
            viewer.setTitle("Relatorio de Aluguel - " + inicio.format(FMT) + " a " + fim.format(FMT));
            viewer.setVisible(true);

            lblStatus.setText(dados.size() + " registro(s) encontrado(s) para o periodo.");
        } catch (JRException e) {
            alerta(Alert.AlertType.ERROR, "Erro no JasperReports", e.getMessage());
            lblStatus.setText("Erro ao gerar relatorio.");
        } catch (Exception e) {
            alerta(Alert.AlertType.ERROR, "Erro", e.getMessage());
            lblStatus.setText("Erro inesperado.");
        } finally {
            btnGerar.setDisable(false);
        }
    }

    @FXML
    private void handleLimpar() {
        dpDataInicio.setValue(LocalDate.now().withDayOfMonth(1));
        dpDataFim.setValue(LocalDate.now());
        carregarTabela();
    }

    @FXML
    private void switchToPrimary() throws IOException {
        App.setRoot("primary");
    }

    private void carregarTabela() {
        LocalDate inicio = dpDataInicio.getValue();
        LocalDate fim = dpDataFim.getValue();

        if (!periodoValidoSemAlerta(inicio, fim)) {
            tableAlugueis.setItems(FXCollections.observableArrayList());
            lblStatus.setText("Informe um periodo valido.");
            return;
        }

        try {
            List<RelatorioAluguel> dados = dao.listarPorPeriodo(inicio, fim);
            tableAlugueis.setItems(FXCollections.observableArrayList(dados));
            lblStatus.setText(dados.size() + " registro(s) encontrado(s) para o periodo.");
        } catch (Exception e) {
            tableAlugueis.setItems(FXCollections.observableArrayList());
            lblStatus.setText("Erro ao carregar tabela: " + e.getMessage());
        }
    }

    private boolean periodoValido(LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null) {
            alerta(Alert.AlertType.WARNING, "Periodo invalido",
                    "Informe as datas de inicio e fim do periodo.");
            return false;
        }
        if (fim.isBefore(inicio)) {
            alerta(Alert.AlertType.WARNING, "Periodo invalido",
                    "A data de fim nao pode ser anterior a data de inicio.");
            return false;
        }
        return true;
    }

    private boolean periodoValidoSemAlerta(LocalDate inicio, LocalDate fim) {
        return inicio != null && fim != null && !fim.isBefore(inicio);
    }

    private Collection<Map<String, ?>> montarDataSource(List<RelatorioAluguel> dados) {
        Collection<Map<String, ?>> linhas = new ArrayList<>();
        for (RelatorioAluguel aluguel : dados) {
            Map<String, Object> linha = new HashMap<>();
            linha.put("id", aluguel.getId());
            linha.put("nomeCliente", aluguel.getNomeCliente());
            linha.put("nomeEquipamento", aluguel.getNomeEquipamento());
            linha.put("dataRetirada", aluguel.getDataRetirada());
            linha.put("dataPrevDevolucao", aluguel.getDataPrevDevolucao());
            linha.put("dataDevolucao", aluguel.getDataDevolucao());
            linha.put("valorTotal", aluguel.getValorTotal());
            linha.put("status", aluguel.getStatus());
            linhas.add(linha);
        }
        return linhas;
    }
}
