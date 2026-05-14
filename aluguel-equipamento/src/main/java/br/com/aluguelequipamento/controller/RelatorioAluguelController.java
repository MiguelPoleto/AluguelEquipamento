package br.com.aluguelequipamento.controller;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.com.App;
import br.com.aluguelequipamento.model.dao.RelatorioAluguelDAO;
import br.com.aluguelequipamento.model.domain.RelatorioAluguel;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.view.JasperViewer;

public class RelatorioAluguelController {

    @FXML private DatePicker dpDataInicio;
    @FXML private DatePicker dpDataFim;
    @FXML private Label      lblStatus;
    @FXML private Button     btnGerar;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {
        // Padrão: mês atual
        dpDataInicio.setValue(LocalDate.now().withDayOfMonth(1));
        dpDataFim.setValue(LocalDate.now());
        lblStatus.setText("");
    }

    // ── Utilitário de alerta ────────────────────────────────────
    private void alerta(Alert.AlertType tipo, String titulo, String msg) {
        Alert a = new Alert(tipo);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    // ── Gerar relatório ────────────────────────────────────────
    @FXML
    private void handleGerarRelatorio() {
        // 1. Validar datas
        LocalDate inicio = dpDataInicio.getValue();
        LocalDate fim    = dpDataFim.getValue();

        if (inicio == null || fim == null) {
            alerta(Alert.AlertType.WARNING, "Período inválido",
                    "Informe as datas de início e fim do período.");
            return;
        }
        if (fim.isBefore(inicio)) {
            alerta(Alert.AlertType.WARNING, "Período inválido",
                    "A data de fim não pode ser anterior à data de início.");
            return;
        }

        lblStatus.setText("Gerando relatório...");
        btnGerar.setDisable(true);

        try {
            // 2. Buscar dados filtrados no banco
            RelatorioAluguelDAO dao = new RelatorioAluguelDAO();
            List<RelatorioAluguel> dados = dao.listarPorPeriodo(inicio, fim);

            if (dados.isEmpty()) {
                alerta(Alert.AlertType.INFORMATION, "Sem dados",
                        "Nenhum aluguel encontrado para o período informado.");
                lblStatus.setText("Nenhum registro encontrado.");
                return;
            }

            // 3. Compilar o JRXML (feito em tempo de execução)
            InputStream jrxmlStream = getClass().getResourceAsStream(
                    "/br/com/aluguelequipamento/relatorios/relatorio_aluguel.jrxml");

            if (jrxmlStream == null) {
                alerta(Alert.AlertType.ERROR, "Erro",
                        "Arquivo relatorio_aluguel.jrxml não encontrado no classpath.\n" +
                        "Verifique se o arquivo está em src/main/resources/br/com/aluguelequipamento/relatorios/");
                return;
            }

            JasperReport jasperReport = JasperCompileManager.compileReport(jrxmlStream);

            // 4. Montar parâmetros
            Map<String, Object> parametros = new HashMap<>();
            parametros.put("PERIODO",
                    inicio.format(FMT) + " a " + fim.format(FMT));

            // 5. Criar DataSource com os dados do banco
            JRBeanCollectionDataSource dataSource =
                    new JRBeanCollectionDataSource(dados);

            // 6. Preencher o relatório
            JasperPrint jasperPrint = JasperFillManager.fillReport(
                    jasperReport, parametros, dataSource);

            // 7. Exibir no JasperViewer
            //    false = não fecha a aplicação ao fechar o viewer
            JasperViewer viewer = new JasperViewer(jasperPrint, false);
            viewer.setTitle("Relatório de Aluguel — " + inicio.format(FMT) + " a " + fim.format(FMT));
            viewer.setVisible(true);

            lblStatus.setText(dados.size() + " registro(s) encontrado(s) para o período.");

        } catch (JRException e) {
            alerta(Alert.AlertType.ERROR, "Erro no JasperReports", e.getMessage());
            lblStatus.setText("Erro ao gerar relatório.");
            e.printStackTrace();
        } catch (Exception e) {
            alerta(Alert.AlertType.ERROR, "Erro", e.getMessage());
            lblStatus.setText("Erro inesperado.");
            e.printStackTrace();
        } finally {
            btnGerar.setDisable(false);
        }
    }

    @FXML
    private void handleLimpar() {
        dpDataInicio.setValue(LocalDate.now().withDayOfMonth(1));
        dpDataFim.setValue(LocalDate.now());
        lblStatus.setText("");
    }

    @FXML
    private void switchToPrimary() throws IOException {
        App.setRoot("primary");
    }
}
