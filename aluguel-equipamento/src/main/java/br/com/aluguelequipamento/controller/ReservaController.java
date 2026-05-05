package br.com.aluguelequipamento.controller;

import br.com.App;
import br.com.aluguelequipamento.model.dao.ClienteDAO;
import br.com.aluguelequipamento.model.dao.EquipamentoDAO;
import br.com.aluguelequipamento.model.dao.ReservaDAO;
import br.com.aluguelequipamento.model.domain.Cliente;
import br.com.aluguelequipamento.model.domain.Equipamento;
import br.com.aluguelequipamento.model.domain.Reserva;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/*
    Miguel
*/
public class ReservaController {

    // ── Tabela e filtros ─────────────────────────────────────
    @FXML private ComboBox<String> cbFiltroStatus;
    @FXML private ComboBox<Cliente> cbFiltroCliente;
    @FXML private ComboBox<Equipamento> cbFiltroEquipamento;
    @FXML private TableView<Reserva> tableReservas;
    @FXML private TableColumn<Reserva, Integer> colId;
    @FXML private TableColumn<Reserva, String> colCliente;
    @FXML private TableColumn<Reserva, String> colEquipamento;
    @FXML private TableColumn<Reserva, LocalDate> colDataInicio;
    @FXML private TableColumn<Reserva, LocalDate> colDataFim;
    @FXML private TableColumn<Reserva, String> colStatus;
    @FXML private Label lblContador;

    // ── Formulário ───────────────────────────────────────────
    @FXML private Label lblTituloForm;
    @FXML private ComboBox<Cliente> cbCliente;
    @FXML private ComboBox<Equipamento> cbEquipamento;
    @FXML private DatePicker dpDataInicio;
    @FXML private DatePicker dpDataFim;
    @FXML private ComboBox<String> cbStatus;
    @FXML private TextArea txtObservacao;
    @FXML private Label lblErro;
    @FXML private Button btnExcluir;

    private final ReservaDAO reservaDAO = new ReservaDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final EquipamentoDAO equipamentoDAO = new EquipamentoDAO();
    private final DateTimeFormatter formatadorData = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private Reserva reservaSelecionada = null;

    // ── Inicialização ────────────────────────────────────────
    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCliente.setCellValueFactory(new PropertyValueFactory<>("nomeCliente"));
        colEquipamento.setCellValueFactory(new PropertyValueFactory<>("nomeEquipamento"));
        colDataInicio.setCellValueFactory(new PropertyValueFactory<>("dataInicio"));
        colDataFim.setCellValueFactory(new PropertyValueFactory<>("dataFim"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        configurarColunaData(colDataInicio);
        configurarColunaData(colDataFim);
        configurarColunaStatus();

        cbStatus.setItems(FXCollections.observableArrayList("ativa", "cancelada", "concluida"));
        cbStatus.setValue("ativa");
        cbFiltroStatus.setItems(FXCollections.observableArrayList("Todos", "ativa", "cancelada", "concluida"));
        cbFiltroStatus.setValue("Todos");

        configurarComboStatus(cbStatus);
        configurarComboStatus(cbFiltroStatus);
        configurarComboEquipamento(cbEquipamento);
        configurarComboEquipamento(cbFiltroEquipamento);
        carregarCombos();
        dpDataInicio.setValue(LocalDate.now());
        dpDataFim.setValue(LocalDate.now().plusDays(1));
        carregarTabela();
    }

    // ── Carregar combos ──────────────────────────────────────
    private void carregarCombos() {
        try {
            List<Cliente> clientes = clienteDAO.listar();
            cbCliente.setItems(FXCollections.observableArrayList(clientes));
            cbFiltroCliente.setItems(FXCollections.observableArrayList(clientes));
        } catch (SQLException e) {
            mostrarErro("Erro ao carregar clientes: " + e.getMessage());
        }

        try {
            List<Equipamento> equipamentos = equipamentoDAO.listar();
            cbEquipamento.setItems(FXCollections.observableArrayList(equipamentos));
            cbFiltroEquipamento.setItems(FXCollections.observableArrayList(equipamentos));
        } catch (SQLException e) {
            mostrarErro("Erro ao carregar equipamentos: " + e.getMessage());
        }
    }

    // ── Filtrar listagem ─────────────────────────────────────
    @FXML
    private void filtrar() {
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
        } catch (SQLException e) {
            mostrarErro("Erro ao filtrar reservas: " + e.getMessage());
        }
    }

    @FXML
    private void listarTodos() {
        cbFiltroStatus.setValue("Todos");
        cbFiltroCliente.setValue(null);
        cbFiltroEquipamento.setValue(null);
        carregarTabela();
    }

    // ── Selecionar linha da tabela → preencher form ──────────
    @FXML
    private void selecionarReserva(MouseEvent event) {
        Reserva sel = tableReservas.getSelectionModel().getSelectedItem();
        if (sel == null) {
            return;
        }
        reservaSelecionada = sel;

        cbCliente.setValue(localizarCliente(sel.getClienteId()));
        cbEquipamento.setValue(localizarEquipamento(sel.getEquipamentoId()));
        dpDataInicio.setValue(sel.getDataInicio());
        dpDataFim.setValue(sel.getDataFim());
        cbStatus.setValue(sel.getStatus());
        txtObservacao.setText(sel.getObservacao());

        lblTituloForm.setText("Editar Reserva #" + sel.getId());
        btnExcluir.setDisable(false);
        lblErro.setText("");
    }

    // ── Salvar (inserir ou alterar) ──────────────────────────
    @FXML
    private void salvar() {
        lblErro.setText("");

        // Validações
        if (cbCliente.getValue() == null) {
            mostrarErro("Selecione um cliente.");
            return;
        }
        if (cbEquipamento.getValue() == null) {
            mostrarErro("Selecione um equipamento.");
            return;
        }
        if (dpDataInicio.getValue() == null) {
            mostrarErro("Informe a data inicial.");
            return;
        }
        if (dpDataFim.getValue() == null) {
            mostrarErro("Informe a data final.");
            return;
        }
        if (dpDataFim.getValue().isBefore(dpDataInicio.getValue())) {
            mostrarErro("A data final deve ser maior ou igual a data inicial.");
            return;
        }
        if (cbStatus.getValue() == null) {
            mostrarErro("Selecione um status.");
            return;
        }
        if (txtObservacao.getText() != null && txtObservacao.getText().length() > 500) {
            mostrarErro("Observação deve ter no máximo 500 caracteres.");
            return;
        }

        Reserva r = new Reserva();
        r.setClienteId(cbCliente.getValue().getId());
        r.setEquipamentoId(cbEquipamento.getValue().getId());
        r.setDataInicio(dpDataInicio.getValue());
        r.setDataFim(dpDataFim.getValue());
        r.setStatus(cbStatus.getValue());
        r.setObservacao(txtObservacao.getText() == null ? "" : txtObservacao.getText().trim());

        try {
            int idIgnorar = reservaSelecionada == null ? 0 : reservaSelecionada.getId();
            if ("ativa".equals(r.getStatus())
                    && reservaDAO.existeConflito(r.getEquipamentoId(), r.getDataInicio(), r.getDataFim(),
                            r.getClienteId(), idIgnorar)) {
                mostrarErro("Equipamento já reservado por outro cliente neste período.");
                return;
            }

            if (reservaSelecionada == null) {
                reservaDAO.inserir(r);
                mostrarSucesso("Reserva inserida com sucesso!");
            } else {
                r.setId(reservaSelecionada.getId());
                reservaDAO.alterar(r);
                mostrarSucesso("Reserva alterada com sucesso!");
            }
            limpar();
            carregarCombos();
            carregarTabela();
        } catch (SQLException ex) {
            mostrarErro("Erro ao salvar: " + ex.getMessage());
        }
    }

    // ── Excluir ──────────────────────────────────────────────
    @FXML
    private void excluir() {
        if (reservaSelecionada == null) {
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar exclusão");
        confirm.setHeaderText("Excluir reserva?");
        confirm.setContentText("Deseja excluir a reserva #" + reservaSelecionada.getId() + "?\n"
                + "Esta ação não pode ser desfeita.");

        confirm.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.OK) {
                try {
                    reservaDAO.excluir(reservaSelecionada.getId());
                    mostrarSucesso("Reserva excluída com sucesso!");
                    limpar();
                    carregarCombos();
                    carregarTabela();
                } catch (SQLException ex) {
                    mostrarErro("Erro ao excluir: " + ex.getMessage());
                }
            }
        });
    }

    // ── Limpar formulário ────────────────────────────────────
    @FXML
    private void limpar() {
        reservaSelecionada = null;
        cbCliente.setValue(null);
        cbEquipamento.setValue(null);
        dpDataInicio.setValue(LocalDate.now());
        dpDataFim.setValue(LocalDate.now().plusDays(1));
        cbStatus.setValue("ativa");
        txtObservacao.clear();
        lblTituloForm.setText("Nova Reserva");
        btnExcluir.setDisable(true);
        lblErro.setText("");
        tableReservas.getSelectionModel().clearSelection();
    }

    // ── Voltar ao menu ───────────────────────────────────────
    @FXML
    private void voltarMenu() throws IOException {
        App.setRoot("primary");
    }

    // ── Carregar tabela ──────────────────────────────────────
    private void carregarTabela() {
        try {
            tableReservas.setItems(FXCollections.observableArrayList(reservaDAO.listar()));
            atualizarContador();
        } catch (SQLException e) {
            mostrarErro("Erro ao carregar reservas: " + e.getMessage());
        }
    }

    private void atualizarContador() {
        try {
            int total = reservaDAO.listar().size();
            int ativas = reservaDAO.listarAtivas().size();
            lblContador.setText(total + " reservas · " + ativas + " ativas");
        } catch (SQLException e) {
            lblContador.setText("Erro ao carregar");
        }
    }

    // ── Localizar objetos selecionados ───────────────────────
    private Cliente localizarCliente(int id) {
        return cbCliente.getItems().stream()
                .filter(c -> c.getId() == id)
                .findFirst()
                .orElse(null);
    }

    private Equipamento localizarEquipamento(int id) {
        return cbEquipamento.getItems().stream()
                .filter(e -> e.getId() == id)
                .findFirst()
                .orElse(null);
    }

    // ── Formatação visual ────────────────────────────────────
    private void configurarColunaData(TableColumn<Reserva, LocalDate> coluna) {
        coluna.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate data, boolean empty) {
                super.updateItem(data, empty);
                setText(empty || data == null ? null : data.format(formatadorData));
            }
        });
    }

    private void configurarColunaStatus() {
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);

                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                    return;
                }

                setText(formatarStatus(status));

                switch (status) {
                    case "ativa" -> setStyle("-fx-text-fill: #7a4200; -fx-font-weight: bold;");
                    case "cancelada" -> setStyle("-fx-text-fill: #8b1a1a; -fx-font-weight: bold;");
                    case "concluida" -> setStyle("-fx-text-fill: #1e6b3a; -fx-font-weight: bold;");
                    default -> setStyle("");
                }
            }
        });
    }

    private void configurarComboEquipamento(ComboBox<Equipamento> combo) {
        combo.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(Equipamento item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatarEquipamento(item));
            }
        });
        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Equipamento item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatarEquipamento(item));
            }
        });
    }

    private void configurarComboStatus(ComboBox<String> combo) {
        combo.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatarStatus(item));
            }
        });
        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatarStatus(item));
            }
        });
    }

    private String formatarEquipamento(Equipamento equipamento) {
        return equipamento.getNome() + " - " + formatarStatus(equipamento.getStatus());
    }

    private String formatarStatus(String status) {
        if (status == null || status.isBlank()) {
            return "";
        }
        if ("Todos".equals(status)) {
            return status;
        }
        String formatado = status.replace("_", " ");
        return formatado.substring(0, 1).toUpperCase() + formatado.substring(1);
    }

    // ── Helpers ──────────────────────────────────────────────
    private void mostrarErro(String msg) {
        lblErro.setStyle("-fx-text-fill: #c0392b; -fx-font-size: 12;");
        lblErro.setText(msg);
    }

    private void mostrarSucesso(String msg) {
        lblErro.setStyle("-fx-text-fill: #1e6b3a; -fx-font-size: 12;");
        lblErro.setText(msg);
    }
}
