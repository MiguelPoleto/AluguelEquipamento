package br.com.aluguelequipamento.controller;

import br.com.App;
import br.com.aluguelequipamento.model.dao.ClienteDAO;
import br.com.aluguelequipamento.model.dao.ConexaoDAO;
import br.com.aluguelequipamento.model.dao.EquipamentoDAO;
import br.com.aluguelequipamento.model.dao.ReservaDAO;
import br.com.aluguelequipamento.model.domain.Cliente;
import br.com.aluguelequipamento.model.domain.Equipamento;
import br.com.aluguelequipamento.model.domain.Reserva;

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
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
    Miguel
*/
public class RelatorioReservasController {

    // ── Filtros ──────────────────────────────────────────────
    @FXML
    private ComboBox<String> cbFiltroStatus;
    @FXML
    private ComboBox<Cliente> cbFiltroCliente;
    @FXML
    private ComboBox<Equipamento> cbFiltroEquipamento;

    // ── Tabela ───────────────────────────────────────────────
    @FXML
    private TableView<Reserva> tableReservas;
    @FXML
    private TableColumn<Reserva, Integer> colId;
    @FXML
    private TableColumn<Reserva, String> colCliente;
    @FXML
    private TableColumn<Reserva, String> colEquipamento;
    @FXML
    private TableColumn<Reserva, LocalDate> colDataInicio;
    @FXML
    private TableColumn<Reserva, LocalDate> colDataFim;
    @FXML
    private TableColumn<Reserva, String> colStatus;
    @FXML
    private TableColumn<Reserva, String> colObservacao;

    // ── Rodapé de resumo ─────────────────────────────────────
    @FXML
    private Label lblContador;
    @FXML
    private Label lblErro;

    private final ReservaDAO reservaDAO = new ReservaDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final EquipamentoDAO equipamentoDAO = new EquipamentoDAO();

    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ── Inicialização ────────────────────────────────────────
    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCliente.setCellValueFactory(new PropertyValueFactory<>("nomeCliente"));
        colEquipamento.setCellValueFactory(new PropertyValueFactory<>("nomeEquipamento"));
        colDataInicio.setCellValueFactory(new PropertyValueFactory<>("dataInicio"));
        colDataFim.setCellValueFactory(new PropertyValueFactory<>("dataFim"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colObservacao.setCellValueFactory(new PropertyValueFactory<>("observacao"));

        configurarColunaData(colDataInicio);
        configurarColunaData(colDataFim);
        configurarColunaStatus();
        configurarColunaObservacao();

        cbFiltroStatus.setItems(FXCollections.observableArrayList(
                "Todos", "ativa", "cancelada", "concluida"));
        cbFiltroStatus.setValue("Todos");
        configurarComboStatus(cbFiltroStatus);
        configurarComboCliente(cbFiltroCliente);
        configurarComboEquipamento(cbFiltroEquipamento);

        carregarCombos();
        carregarTabela();
    }

    // ── Carregar combos ──────────────────────────────────────
    private void carregarCombos() {
        try {
            cbFiltroCliente.setItems(FXCollections.observableArrayList(clienteDAO.listar()));
        } catch (SQLException e) {
            mostrarErro("Erro ao carregar clientes: " + e.getMessage());
        }
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
        try {
            List<Reserva> filtradas = reservaDAO.listar().stream()
                    .filter(r -> "Todos".equals(cbFiltroStatus.getValue())
                            || cbFiltroStatus.getValue() == null
                            || r.getStatus().equals(cbFiltroStatus.getValue()))
                    .filter(r -> cbFiltroCliente.getValue() == null
                            || r.getClienteId() == cbFiltroCliente.getValue().getId())
                    .filter(r -> cbFiltroEquipamento.getValue() == null
                            || r.getEquipamentoId() == cbFiltroEquipamento.getValue().getId())
                    .toList();

            tableReservas.setItems(FXCollections.observableArrayList(filtradas));
            atualizarContador(filtradas.size());
        } catch (SQLException e) {
            mostrarErro("Erro ao filtrar reservas: " + e.getMessage());
        }
    }

    // ── Listar todos ─────────────────────────────────────────
    @FXML
    private void listarTodos() {
        cbFiltroStatus.setValue("Todos");
        cbFiltroCliente.setValue(null);
        cbFiltroEquipamento.setValue(null);
        lblErro.setText("");
        carregarTabela();
    }

    // ── Imprimir (JasperReports) ─────────────────────────────
    /**
     * Gera o relatório em uma nova janela usando JasperViewer.
     *
     * Parâmetros passados ao JRXML:
     * P_STATUS (String) — "%" para todos, ou o valor exato do status
     * P_CLIENTE_ID (Integer) — 0 para todos os clientes
     * P_EQUIPAMENTO_ID (Integer) — 0 para todos os equipamentos
     *
     * O JRXML usa essas variáveis com LIKE / OR para filtrar no SQL.
     * Compile o arquivo relatorio-reservas.jrxml no Jaspersoft Studio e coloque
     * o .jasper gerado em:
     * src/main/resources/br/com/aluguelequipamento/relatorios/relatorio-reservas.jasper
     */
    @FXML
    private void imprimir() {
        lblErro.setText("");
        try {
            // 1) Montar parâmetros com os filtros ativos da tela
            Map<String, Object> params = new HashMap<>();

            String statusFiltro = cbFiltroStatus.getValue();
            params.put("P_STATUS",
                    (statusFiltro == null || "Todos".equals(statusFiltro)) ? "%" : statusFiltro);

            params.put("P_CLIENTE_ID",
                    cbFiltroCliente.getValue() != null
                            ? cbFiltroCliente.getValue().getId()
                            : 0);

            params.put("P_EQUIPAMENTO_ID",
                    cbFiltroEquipamento.getValue() != null
                            ? cbFiltroEquipamento.getValue().getId()
                            : 0);

            // 2) Carregar o arquivo .jasper compilado do classpath
            InputStream jasperStream = getClass().getResourceAsStream(
                    "/br/com/aluguelequipamento/view/relatorios/relatorio-manutencao.jrxml");

            if (jasperStream == null) {
                mostrarErro("Arquivo .jasper não encontrado.");
                return;
            }

            JasperReport jasperReport = JasperCompileManager.compileReport(jasperStream);

            // 3) Preencher o relatório com a conexão ao banco
            Connection conn = ConexaoDAO.getConexao();
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, conn);

            // 4) Exibir em nova janela
            JasperViewer viewer = new JasperViewer(jasperPrint, false);
            viewer.setTitle("Relatório — Listagem de Reservas");
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
            List<Reserva> lista = reservaDAO.listar();
            tableReservas.setItems(FXCollections.observableArrayList(lista));
            atualizarContador(lista.size());
        } catch (SQLException e) {
            mostrarErro("Erro ao carregar reservas: " + e.getMessage());
        }
    }

    private void atualizarContador(int total) {
        try {
            int ativas = reservaDAO.listarAtivas().size();
            lblContador.setText(total + " reserva(s) exibida(s) · " + ativas + " ativa(s) no total");
        } catch (SQLException e) {
            lblContador.setText(total + " reserva(s) exibida(s)");
        }
    }

    // ── Voltar ao menu ───────────────────────────────────────
    @FXML
    private void voltarMenu() throws IOException {
        App.setRoot("primary");
    }

    // ── Formatação de colunas ────────────────────────────────
    private void configurarColunaData(TableColumn<Reserva, LocalDate> col) {
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
                    case "ativa" -> setStyle("-fx-text-fill: #7a4200; -fx-font-weight: bold;");
                    case "cancelada" -> setStyle("-fx-text-fill: #8b1a1a; -fx-font-weight: bold;");
                    case "concluida" -> setStyle("-fx-text-fill: #1e6b3a; -fx-font-weight: bold;");
                    default -> setStyle("");
                }
            }
        });
    }

    private void configurarColunaObservacao() {
        colObservacao.setCellFactory(c -> new TableCell<>() {
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

    private void configurarComboCliente(ComboBox<Cliente> combo) {
        combo.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(Cliente c, boolean empty) {
                super.updateItem(c, empty);
                setText(empty || c == null ? null : c.getNome());
            }
        });
        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Cliente c, boolean empty) {
                super.updateItem(c, empty);
                setText(empty || c == null ? null : c.getNome());
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
