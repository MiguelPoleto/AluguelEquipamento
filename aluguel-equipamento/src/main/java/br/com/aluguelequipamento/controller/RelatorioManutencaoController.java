package br.com.aluguelequipamento.controller;

import br.com.App;
import br.com.aluguelequipamento.model.dao.ConexaoDAO;
import br.com.aluguelequipamento.model.dao.EquipamentoDAO;
import br.com.aluguelequipamento.model.dao.ManutencaoDAO;
import br.com.aluguelequipamento.model.domain.Equipamento;
import br.com.aluguelequipamento.model.domain.Manutencao;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.view.JasperViewer;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
    Miguel
*/
public class RelatorioManutencaoController {

    // ── Filtros ──────────────────────────────────────────────
    @FXML
    private ComboBox<String> cbFiltroStatus;
    @FXML
    private ComboBox<Equipamento> cbFiltroEquipamento;
    @FXML
    private DatePicker dpFiltroDataDe;
    @FXML
    private DatePicker dpFiltroDataAte;

    // ── Tabela ───────────────────────────────────────────────
    @FXML
    private TableView<Manutencao> tableManutencoes;
    @FXML
    private TableColumn<Manutencao, Integer> colId;
    @FXML
    private TableColumn<Manutencao, String> colEquipamento;
    @FXML
    private TableColumn<Manutencao, LocalDate> colDataInicio;
    @FXML
    private TableColumn<Manutencao, LocalDate> colDataPrevisao;
    @FXML
    private TableColumn<Manutencao, LocalDate> colDataFim;
    @FXML
    private TableColumn<Manutencao, String> colStatus;
    @FXML
    private TableColumn<Manutencao, String> colDescricao;

    // ── Rodapé de resumo ─────────────────────────────────────
    @FXML
    private Label lblContador;
    @FXML
    private Label lblErro;

    private final ManutencaoDAO manutencaoDAO = new ManutencaoDAO();
    private final EquipamentoDAO equipamentoDAO = new EquipamentoDAO();

    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ── Inicialização ────────────────────────────────────────
    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colEquipamento.setCellValueFactory(new PropertyValueFactory<>("nomeEquipamento"));
        colDataInicio.setCellValueFactory(new PropertyValueFactory<>("dataInicio"));
        colDataPrevisao.setCellValueFactory(new PropertyValueFactory<>("dataPrevisao"));
        colDataFim.setCellValueFactory(new PropertyValueFactory<>("dataFim"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colDescricao.setCellValueFactory(new PropertyValueFactory<>("descricao"));

        configurarColunaData(colDataInicio);
        configurarColunaData(colDataPrevisao);
        configurarColunaData(colDataFim);
        configurarColunaStatus();
        configurarColunaDescricao();

        cbFiltroStatus.setItems(FXCollections.observableArrayList(
                "Todos", "em_andamento", "concluida"));
        cbFiltroStatus.setValue("Todos");
        configurarComboStatus(cbFiltroStatus);
        configurarComboEquipamento(cbFiltroEquipamento);

        carregarComboEquipamento();
        carregarTabela();
    }

    // ── Carregar combo equipamento ───────────────────────────
    private void carregarComboEquipamento() {
        try {
            cbFiltroEquipamento.setItems(FXCollections.observableArrayList(equipamentoDAO.listar()));
        } catch (SQLException e) {
            mostrarErro("Erro ao carregar equipamentos: " + e.getMessage());
        }
    }

    // ── Filtrar ──────────────────────────────────────────────
    @FXML
    private void filtrar() {
        lblErro.setText("");

        if (dpFiltroDataDe.getValue() != null && dpFiltroDataAte.getValue() != null
                && dpFiltroDataAte.getValue().isBefore(dpFiltroDataDe.getValue())) {
            mostrarErro("Data final do filtro não pode ser anterior à data inicial.");
            return;
        }

        try {
            List<Manutencao> filtradas = manutencaoDAO.listar().stream()
                    .filter(m -> "Todos".equals(cbFiltroStatus.getValue())
                            || cbFiltroStatus.getValue() == null
                            || m.getStatus().equals(cbFiltroStatus.getValue()))
                    .filter(m -> cbFiltroEquipamento.getValue() == null
                            || m.getEquipamentoId() == cbFiltroEquipamento.getValue().getId())
                    .filter(m -> dpFiltroDataDe.getValue() == null
                            || (m.getDataInicio() != null
                                    && !m.getDataInicio().isBefore(dpFiltroDataDe.getValue())))
                    .filter(m -> dpFiltroDataAte.getValue() == null
                            || (m.getDataInicio() != null
                                    && !m.getDataInicio().isAfter(dpFiltroDataAte.getValue())))
                    .toList();

            tableManutencoes.setItems(FXCollections.observableArrayList(filtradas));
            atualizarContador(filtradas.size());
        } catch (SQLException e) {
            mostrarErro("Erro ao filtrar manutenções: " + e.getMessage());
        }
    }

    // ── Listar todos ─────────────────────────────────────────
    @FXML
    private void listarTodos() {
        cbFiltroStatus.setValue("Todos");
        cbFiltroEquipamento.setValue(null);
        dpFiltroDataDe.setValue(null);
        dpFiltroDataAte.setValue(null);
        lblErro.setText("");
        carregarTabela();
    }

    // ── Imprimir (JasperReports) ─────────────────────────────
    /**
     * Gera o relatório de manutenções em uma nova janela usando JasperViewer.
     *
     * Parâmetros passados ao JRXML:
     * P_STATUS (String) — "%" para todos, ou o status exato
     * P_EQUIPAMENTO_ID (Integer) — 0 para todos os equipamentos
     * P_DATA_DE (Date) — null para sem limite inferior
     * P_DATA_ATE (Date) — null para sem limite superior
     *
     * O JRXML usa essas variáveis para filtrar no SQL (LIKE / IS NULL OR).
     * Compile o arquivo relatorio-manutencao.jrxml no Jaspersoft Studio e coloque
     * o .jasper gerado em:
     * src/main/resources/br/com/aluguelequipamento/relatorios/relatorio-manutencao.jasper
     */
    @FXML
    private void imprimir() {
        lblErro.setText("");

        // Validação de datas antes de imprimir
        if (dpFiltroDataDe.getValue() != null && dpFiltroDataAte.getValue() != null
                && dpFiltroDataAte.getValue().isBefore(dpFiltroDataDe.getValue())) {
            mostrarErro("Data final não pode ser anterior à data inicial.");
            return;
        }

        try {
            // 1) Montar parâmetros com os filtros ativos da tela
            Map<String, Object> params = new HashMap<>();

            String statusFiltro = cbFiltroStatus.getValue();
            params.put("P_STATUS",
                    (statusFiltro == null || "Todos".equals(statusFiltro)) ? "%" : statusFiltro);

            params.put("P_EQUIPAMENTO_ID",
                    cbFiltroEquipamento.getValue() != null
                            ? cbFiltroEquipamento.getValue().getId()
                            : 0);

            // Datas: passa null quando não houver filtro (o JRXML trata com IS NULL OR)
            params.put("P_DATA_DE",
                    dpFiltroDataDe.getValue() != null
                            ? Date.valueOf(dpFiltroDataDe.getValue())
                            : null);

            params.put("P_DATA_ATE",
                    dpFiltroDataAte.getValue() != null
                            ? Date.valueOf(dpFiltroDataAte.getValue())
                            : null);

            // 2) Carregar o arquivo .jasper compilado do classpath
            InputStream jasperStream = getClass().getResourceAsStream(
                    "/br/com/aluguelequipamento/view/relatorios/relatorio-manutencao.jrxml");

            if (jasperStream == null) {
                mostrarErro("Arquivo JRXML do relatório de manutenção não encontrado.");
                return;
            }

            JasperReport jasperReport = JasperCompileManager.compileReport(jasperStream);

            // 3) Preencher o relatório com a conexão ao banco
            Connection conn = ConexaoDAO.getConexao();
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, conn);

            // 4) Exibir em nova janela
            JasperViewer viewer = new JasperViewer(jasperPrint, false);
            viewer.setTitle("Relatório — Manutenção de Equipamentos");
            viewer.setVisible(true);

        } catch (JRException e) {
            mostrarErro("Erro ao gerar relatório: " + e.getMessage());
        } catch (SQLException e) {
            mostrarErro("Erro de banco de dados: " + e.getMessage());
        }
    }

    // ── Carregar tabela ──────────────────────────────────────
    private void carregarTabela() {
        try {
            List<Manutencao> lista = manutencaoDAO.listar();
            tableManutencoes.setItems(FXCollections.observableArrayList(lista));
            atualizarContador(lista.size());
        } catch (SQLException e) {
            mostrarErro("Erro ao carregar manutenções: " + e.getMessage());
        }
    }

    private void atualizarContador(int total) {
        try {
            int emAndamento = manutencaoDAO.contarEmAndamento();
            lblContador.setText(total + " manutenção(ões) exibida(s) · "
                    + emAndamento + "/10 em andamento");
        } catch (SQLException e) {
            lblContador.setText(total + " manutenção(ões) exibida(s)");
        }
    }

    // ── Voltar ao menu ───────────────────────────────────────
    @FXML
    private void voltarMenu() throws IOException {
        App.setRoot("primary");
    }

    // ── Formatação de colunas ────────────────────────────────
    private void configurarColunaData(TableColumn<Manutencao, LocalDate> col) {
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate d, boolean empty) {
                super.updateItem(d, empty);
                setText(empty || d == null ? null : d.format(fmt));
            }
        });
    }

    private void configurarColunaStatus() {
        colStatus.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(formatarStatus(s));
                switch (s) {
                    case "em_andamento" -> setStyle("-fx-text-fill: #8b1a1a; -fx-font-weight: bold;");
                    case "concluida" -> setStyle("-fx-text-fill: #1e6b3a; -fx-font-weight: bold;");
                    default -> setStyle("");
                }
            }
        });
    }

    private void configurarColunaDescricao() {
        colDescricao.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) {
                    setText(null);
                    return;
                }
                setText(s.length() > 35 ? s.substring(0, 32) + "..." : s);
                setTooltip(new Tooltip(s));
            }
        });
    }

    private void configurarComboStatus(ComboBox<String> combo) {
        combo.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                setText(empty || s == null ? null : formatarStatus(s));
            }
        });
        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                setText(empty || s == null ? null : formatarStatus(s));
            }
        });
    }

    private void configurarComboEquipamento(ComboBox<Equipamento> combo) {
        combo.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(Equipamento e, boolean empty) {
                super.updateItem(e, empty);
                setText(empty || e == null ? null
                        : e.getNome() + " [" + formatarStatus(e.getStatus()) + "]");
            }
        });
        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Equipamento e, boolean empty) {
                super.updateItem(e, empty);
                setText(empty || e == null ? null
                        : e.getNome() + " [" + formatarStatus(e.getStatus()) + "]");
            }
        });
    }

    // ── Helpers ──────────────────────────────────────────────
    private String formatarStatus(String s) {
        if (s == null || s.isBlank() || "Todos".equals(s))
            return s;
        String f = s.replace("_", " ");
        return f.substring(0, 1).toUpperCase() + f.substring(1);
    }

    private void mostrarErro(String msg) {
        lblErro.setStyle("-fx-text-fill: #8b1a1a; -fx-font-size: 12;");
        lblErro.setText(msg);
    }
}
