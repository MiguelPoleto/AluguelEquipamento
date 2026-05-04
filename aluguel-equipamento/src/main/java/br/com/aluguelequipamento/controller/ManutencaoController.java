package br.com.aluguelequipamento.controller;

import br.com.App;
import br.com.aluguelequipamento.model.dao.EquipamentoDAO;
import br.com.aluguelequipamento.model.dao.ManutencaoDAO;
import br.com.aluguelequipamento.model.domain.Equipamento;
import br.com.aluguelequipamento.model.domain.Manutencao;

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
public class ManutencaoController {

    // ── Tabela ───────────────────────────────────────────────
    @FXML
    private ComboBox<String> cbFiltroStatus;
    @FXML
    private ComboBox<Equipamento> cbFiltroEquipamento;
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

    // ── Formulário ───────────────────────────────────────────
    @FXML
    private Label lblTituloForm;
    @FXML
    private Label lblContador;
    @FXML
    private Label lblLimiteAviso;
    @FXML
    private ComboBox<Equipamento> cbEquipamento;
    @FXML
    private TextArea txtDescricao;
    @FXML
    private DatePicker dpDataInicio;
    @FXML
    private DatePicker dpDataPrevisao;
    @FXML
    private DatePicker dpDataFim;
    @FXML
    private ComboBox<String> cbStatus;
    @FXML
    private Label lblErro;
    @FXML
    private Button btnExcluir;

    private final ManutencaoDAO manutencaoDAO = new ManutencaoDAO();
    private final EquipamentoDAO equipamentoDAO = new EquipamentoDAO();
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private Manutencao manutencaoSelecionada = null;

    // ── Inicialização ────────────────────────────────────────
    @FXML
    public void initialize() {
        // Colunas
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

        // ComboBoxes de status
        cbStatus.setItems(FXCollections.observableArrayList("em_andamento", "concluida"));
        cbStatus.setValue("em_andamento");

        cbFiltroStatus.setItems(FXCollections.observableArrayList(
                "Todos", "em_andamento", "concluida"));
        cbFiltroStatus.setValue("Todos");

        configurarComboStatus(cbStatus);
        configurarComboStatus(cbFiltroStatus);
        configurarComboEquipamento(cbEquipamento);
        configurarComboEquipamento(cbFiltroEquipamento);

        // Datas padrão
        dpDataInicio.setValue(LocalDate.now());
        dpDataPrevisao.setValue(LocalDate.now().plusDays(7));

        cbStatus.setOnAction(e -> {
            boolean concluida = "concluida".equals(cbStatus.getValue());
            dpDataFim.setDisable(!concluida);
        });

        dpDataFim.setDisable(!"concluida".equals(cbStatus.getValue()));

        carregarCombosEquipamento();
        carregarTabela();
        atualizarContador();
    }

    // ── Carregar combos de equipamento ───────────────────────
    private void carregarCombosEquipamento() {
        try {
            List<Equipamento> todos = equipamentoDAO.listar();
            cbEquipamento.setItems(FXCollections.observableArrayList(todos));
            cbFiltroEquipamento.setItems(FXCollections.observableArrayList(todos));
        } catch (SQLException e) {
            mostrarErro("Erro ao carregar equipamentos: " + e.getMessage());
        }
    }

    // ── Atualizar contador RN ────────────────────────────────
    private void atualizarContador() {
        try {
            int em = manutencaoDAO.contarEmAndamento();
            lblContador.setText(em + "/10 em andamento");
            if (em >= 10) {
                lblContador.setStyle(
                        "-fx-text-fill: #8b1a1a; -fx-font-weight: bold;" +
                                "-fx-background-color: #fcebeb; -fx-background-radius: 5;" +
                                "-fx-padding: 4 10; -fx-font-size: 12;");
                lblLimiteAviso.setStyle("-fx-text-fill: #8b1a1a; -fx-font-size: 12;");
                lblLimiteAviso.setText(
                        "⚠ Limite atingido! Não é possível abrir novas manutenções.");
            } else {
                lblContador.setStyle(
                        "-fx-text-fill: #1e6b3a; -fx-font-weight: bold;" +
                                "-fx-background-color: #eaf3de; -fx-background-radius: 5;" +
                                "-fx-padding: 4 10; -fx-font-size: 12;");
                lblLimiteAviso.setText("");
            }
        } catch (SQLException e) {
            lblContador.setText("Erro");
        }
    }

    // ── Filtrar ──────────────────────────────────────────────
    @FXML
    private void filtrar() {
        try {
            List<Manutencao> filtradas = manutencaoDAO.listar().stream()
                    .filter(m -> "Todos".equals(cbFiltroStatus.getValue())
                            || cbFiltroStatus.getValue() == null
                            || m.getStatus().equals(cbFiltroStatus.getValue()))
                    .filter(m -> cbFiltroEquipamento.getValue() == null
                            || m.getEquipamentoId() == cbFiltroEquipamento.getValue().getId())
                    .toList();
            tableManutencoes.setItems(FXCollections.observableArrayList(filtradas));
        } catch (SQLException e) {
            mostrarErro("Erro ao filtrar: " + e.getMessage());
        }
    }

    @FXML
    private void listarTodos() {
        cbFiltroStatus.setValue("Todos");
        cbFiltroEquipamento.setValue(null);
        carregarTabela();
    }

    // ── Selecionar linha → preencher form ────────────────────
    @FXML
    private void selecionarManutencao(MouseEvent event) {
        Manutencao sel = tableManutencoes.getSelectionModel().getSelectedItem();
        if (sel == null)
            return;
        manutencaoSelecionada = sel;

        cbEquipamento.setValue(localizarEquipamento(sel.getEquipamentoId()));
        txtDescricao.setText(sel.getDescricao());
        dpDataInicio.setValue(sel.getDataInicio());
        dpDataPrevisao.setValue(sel.getDataPrevisao());
        dpDataFim.setValue(sel.getDataFim());
        cbStatus.setValue(sel.getStatus());

        lblTituloForm.setText("Editar Manutenção #" + sel.getId());
        btnExcluir.setDisable(false);
        lblErro.setText("");
    }

    // ── Salvar ───────────────────────────────────────────────
    @FXML
    private void salvar() {
        lblErro.setText("");

        // Validações de campo
        if (cbEquipamento.getValue() == null) {
            mostrarErro("Selecione um equipamento.");
            return;
        }
        if (txtDescricao.getText().isBlank()) {
            mostrarErro("A descrição do problema é obrigatória.");
            return;
        }
        if (txtDescricao.getText().length() > 500) {
            mostrarErro("Descrição deve ter no máximo 500 caracteres.");
            return;
        }
        if (dpDataInicio.getValue() == null) {
            mostrarErro("Informe a data de entrada.");
            return;
        }
        if (dpDataPrevisao.getValue() == null) {
            mostrarErro("Informe a data de previsão.");
            return;
        }
        if (dpDataPrevisao.getValue().isBefore(dpDataInicio.getValue())) {
            mostrarErro("Data de previsão deve ser maior ou igual à data de entrada.");
            return;
        }
        if (dpDataFim.getValue() != null && dpDataFim.getValue().isBefore(dpDataInicio.getValue())) {
            mostrarErro("Data de conclusão deve ser maior ou igual à data de entrada.");
            return;
        }
        if (cbStatus.getValue() == null) {
            mostrarErro("Selecione um status.");
            return;
        }
        if ("concluida".equals(cbStatus.getValue()) && dpDataFim.getValue() == null) {
            mostrarErro("Informe a data de conclusão.");
            return;
        }

        // RN: máximo 10 em andamento (somente na inserção ou ao mudar para
        // em_andamento)
        try {
            boolean inserindo = manutencaoSelecionada == null;
            boolean mudandoParaAndamento = !inserindo
                    && "em_andamento".equals(cbStatus.getValue())
                    && !"em_andamento".equals(manutencaoSelecionada.getStatus());

            if ((inserindo || mudandoParaAndamento)
                    && "em_andamento".equals(cbStatus.getValue())) {
                int emAndamento = manutencaoDAO.contarEmAndamento();
                if (emAndamento >= 10) {
                    mostrarErro("Limite de 10 manutenções em andamento atingido. "
                            + "Conclua uma manutenção antes de abrir nova.");
                    return;
                }
            }
        } catch (SQLException e) {
            mostrarErro("Erro ao verificar limite: " + e.getMessage());
            return;
        }

        Manutencao m = new Manutencao();
        m.setEquipamentoId(cbEquipamento.getValue().getId());
        m.setDescricao(txtDescricao.getText().trim());
        m.setDataInicio(dpDataInicio.getValue());
        m.setDataPrevisao(dpDataPrevisao.getValue());
        m.setDataFim(dpDataFim.getValue());
        m.setStatus(cbStatus.getValue());

        try {
            if (manutencaoSelecionada == null) {
                // Inserção: atualiza status do equipamento para em_manutencao
                manutencaoDAO.inserir(m);
                equipamentoDAO.atualizarStatus(
                        m.getEquipamentoId(), "em_manutencao");
                mostrarSucesso("Manutenção registrada com sucesso!");
            } else {
                m.setId(manutencaoSelecionada.getId());
                // Se concluída, libera equipamento para disponível
                if ("concluida".equals(m.getStatus())
                        && "em_andamento".equals(manutencaoSelecionada.getStatus())) {
                    equipamentoDAO.atualizarStatus(
                            m.getEquipamentoId(), "disponivel");
                }
                manutencaoDAO.alterar(m);
                mostrarSucesso("Manutenção alterada com sucesso!");
            }
            limpar();
            carregarCombosEquipamento();
            carregarTabela();
            atualizarContador();
        } catch (SQLException ex) {
            mostrarErro("Erro ao salvar: " + ex.getMessage());
        }
    }

    // ── Excluir ──────────────────────────────────────────────
    @FXML
    private void excluir() {
        if (manutencaoSelecionada == null)
            return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar exclusão");
        confirm.setHeaderText("Excluir manutenção?");
        confirm.setContentText("Deseja excluir a manutenção #"
                + manutencaoSelecionada.getId() + "?\n"
                + "Esta ação não pode ser desfeita.");

        confirm.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.OK) {
                try {
                    // Se estava em andamento, libera equipamento
                    if ("em_andamento".equals(manutencaoSelecionada.getStatus())) {
                        equipamentoDAO.atualizarStatus(
                                manutencaoSelecionada.getEquipamentoId(), "disponivel");
                    }
                    manutencaoDAO.excluir(manutencaoSelecionada.getId());
                    mostrarSucesso("Manutenção excluída com sucesso!");
                    limpar();
                    carregarCombosEquipamento();
                    carregarTabela();
                    atualizarContador();
                } catch (SQLException ex) {
                    mostrarErro("Erro ao excluir: " + ex.getMessage());
                }
            }
        });
    }

    // ── Limpar ───────────────────────────────────────────────
    @FXML
    private void limpar() {
        manutencaoSelecionada = null;
        cbEquipamento.setValue(null);
        txtDescricao.clear();
        dpDataInicio.setValue(LocalDate.now());
        dpDataFim.setValue(LocalDate.now().plusDays(7));
        cbStatus.setValue("em_andamento");
        lblTituloForm.setText("Nova Manutenção");
        btnExcluir.setDisable(true);
        lblErro.setText("");
        tableManutencoes.getSelectionModel().clearSelection();
    }

    @FXML
    private void voltarMenu() throws IOException {
        App.setRoot("primary");
    }

    // ── Helpers internos ─────────────────────────────────────
    private void carregarTabela() {
        try {
            tableManutencoes.setItems(
                    FXCollections.observableArrayList(manutencaoDAO.listar()));
        } catch (SQLException e) {
            mostrarErro("Erro ao carregar: " + e.getMessage());
        }
    }

    private Equipamento localizarEquipamento(int id) {
        return cbEquipamento.getItems().stream()
                .filter(e -> e.getId() == id)
                .findFirst().orElse(null);
    }

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
                    case "em_andamento" ->
                        setStyle("-fx-text-fill: #8b1a1a; -fx-font-weight: bold;");
                    case "concluida" ->
                        setStyle("-fx-text-fill: #1e6b3a; -fx-font-weight: bold;");
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
                // Truncar descrição longa na tabela
                setText(s.length() > 30 ? s.substring(0, 27) + "..." : s);
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

    private void mostrarSucesso(String msg) {
        lblErro.setStyle("-fx-text-fill: #1e6b3a; -fx-font-size: 12;");
        lblErro.setText(msg);
    }
}