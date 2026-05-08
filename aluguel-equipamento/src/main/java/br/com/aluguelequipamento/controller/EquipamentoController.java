package br.com.aluguelequipamento.controller;

import br.com.App;
import br.com.aluguelequipamento.model.dao.EquipamentoDAO;
import br.com.aluguelequipamento.model.domain.Equipamento;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/*
    Miguel
*/
public class EquipamentoController {

    // ── Tabela ──────────────────────────────────────────────
    @FXML
    private TextField txtBusca;
    @FXML
    private TableView<Equipamento> tableEquipamentos;
    @FXML
    private TableColumn<Equipamento, Integer> colId;
    @FXML
    private TableColumn<Equipamento, String> colNome;
    @FXML
    private TableColumn<Equipamento, String> colCategoria;
    @FXML
    private TableColumn<Equipamento, BigDecimal> colValor;
    @FXML
    private TableColumn<Equipamento, String> colStatus;
    @FXML
    private Label lblContador;

    // ── Formulário ──────────────────────────────────────────
    @FXML
    private Label lblTituloForm;
    @FXML
    private TextField txtNome;
    @FXML
    private TextArea txtDescricao;
    @FXML
    private ComboBox<String> cbCategoria;
    @FXML
    private TextField txtValorDiaria;
    @FXML
    private ComboBox<String> cbStatus;
    @FXML
    private DatePicker dpDataCadastro;
    @FXML
    private Label lblErro;
    @FXML
    private Button btnExcluir;

    private final EquipamentoDAO dao = new EquipamentoDAO();
    private Equipamento equipamentoSelecionado = null;

    // ── Inicialização ────────────────────────────────────────
    @FXML
    public void initialize() {

        // ── Configurar colunas ─────────────────────────────
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        colValor.setCellValueFactory(new PropertyValueFactory<>("valorDiaria"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // ── Formatar coluna valor ──────────────────────────
        colValor.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal val, boolean empty) {
                super.updateItem(val, empty);
                setText(empty || val == null ? null
                        : "R$ " + String.format("%.2f", val));
            }
        });

        // ── Coluna STATUS (formatada + colorida) ───────────
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
                    case "disponivel" -> setStyle("-fx-text-fill: #1e6b3a; -fx-font-weight: bold;");
                    case "reservado" -> setStyle("-fx-text-fill: #7a4200; -fx-font-weight: bold;");
                    case "alugado" -> setStyle("-fx-text-fill: #1a4f7a; -fx-font-weight: bold;");
                    case "em_manutencao" -> setStyle("-fx-text-fill: #8b1a1a; -fx-font-weight: bold;");
                    default -> setStyle("");
                }
            }
        });

        // ── ComboBox Categoria ─────────────────────────────
        cbCategoria.setItems(FXCollections.observableArrayList(
                "Construção", "Jardinagem", "Limpeza Industrial",
                "Energia", "Ferramentas", "Eventos e Festas", "Outro"));

        // ── ComboBox STATUS ───────────
        cbStatus.setItems(FXCollections.observableArrayList(
                "disponivel", "reservado", "alugado", "em_manutencao"));

        cbStatus.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }

                setText(formatarStatus(item));

                switch (item) {
                    case "disponivel" -> setStyle("-fx-text-fill: #1e6b3a;");
                    case "reservado" -> setStyle("-fx-text-fill: #7a4200;");
                    case "alugado" -> setStyle("-fx-text-fill: #1a4f7a;");
                    case "em_manutencao" -> setStyle("-fx-text-fill: #8b1a1a;");
                    default -> setStyle("");
                }
            }
        });
        
        cbStatus.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }

                setText(formatarStatus(item));

                switch (item) {
                    case "disponivel" -> setStyle("-fx-text-fill: #1e6b3a;");
                    case "reservado" -> setStyle("-fx-text-fill: #7a4200;");
                    case "alugado" -> setStyle("-fx-text-fill: #1a4f7a;");
                    case "em_manutencao" -> setStyle("-fx-text-fill: #8b1a1a;");
                    default -> setStyle("");
                }
            }
        });

        cbStatus.setValue("disponivel");

        // ── Data ───────────────────────────────────────────
        dpDataCadastro.setValue(LocalDate.now());

        // ── Carregar tabela ────────────────────────────────
        carregarTabela(dao::listar);
    }

    private String formatarStatus(String status) {
        if (status == null)
            return "";

        String formatado = status.replace("_", " ");
        return formatado.substring(0, 1).toUpperCase() + formatado.substring(1);
    }

    // ── Carregar tabela ──────────────────────────────────────
    private void carregarTabela(DAOSupplier<List<Equipamento>> supplier) {
        try {
            tableEquipamentos.setItems(
                    FXCollections.observableArrayList(supplier.get()));
            atualizarContador();
        } catch (SQLException e) {
            mostrarErro("Erro ao carregar dados: " + e.getMessage());
        }
    }

    private void atualizarContador() {
        try {
            int total = dao.listar().size();
            int disponiveis = dao.listarDisponiveis().size();
            lblContador.setText(total + " cadastrados · " + disponiveis + " disponíveis");
        } catch (SQLException e) {
            lblContador.setText("Erro ao carregar");
        }
    }

    @FXML
    private void listarTodos() {
        txtBusca.clear();
        carregarTabela(dao::listar);
    }

    @FXML
    private void buscar() {
        String termo = txtBusca.getText().trim().toLowerCase();
        if (termo.isEmpty()) {
            carregarTabela(dao::listar);
            return;
        }
        try {
            List<Equipamento> todos = dao.listar();
            List<Equipamento> filtrados = todos.stream()
                    .filter(e -> e.getNome().toLowerCase().contains(termo)
                            || e.getCategoria().toLowerCase().contains(termo))
                    .toList();
            tableEquipamentos.setItems(FXCollections.observableArrayList(filtrados));
        } catch (SQLException e) {
            mostrarErro("Erro na busca: " + e.getMessage());
        }
    }

    // ── Selecionar linha da tabela → preencher form ──────────
    @FXML
    private void selecionarEquipamento(MouseEvent event) {
        Equipamento sel = tableEquipamentos.getSelectionModel().getSelectedItem();
        if (sel == null)
            return;
        equipamentoSelecionado = sel;

        txtNome.setText(sel.getNome());
        txtDescricao.setText(sel.getDescricao());
        cbCategoria.setValue(sel.getCategoria());
        txtValorDiaria.setText(sel.getValorDiaria().toPlainString());
        cbStatus.setValue(sel.getStatus());
        dpDataCadastro.setValue(sel.getDataCadastro());

        lblTituloForm.setText("Editar Equipamento #" + sel.getId());
        btnExcluir.setDisable(false);
        lblErro.setText("");
    }

    // ── Salvar (inserir ou alterar) ──────────────────────────
    @FXML
    private void salvar() {
        lblErro.setText("");

        // Validações
        if (txtNome.getText().isBlank()) {
            mostrarErro("O campo Nome é obrigatório.");
            return;
        }
        if (txtNome.getText().length() > 100) {
            mostrarErro("Nome deve ter no máximo 100 caracteres.");
            return;
        }
        if (cbCategoria.getValue() == null) {
            mostrarErro("Selecione uma categoria.");
            return;
        }
        if (txtValorDiaria.getText().isBlank()) {
            mostrarErro("O campo Valor da Diária é obrigatório.");
            return;
        }
        if (cbStatus.getValue() == null) {
            mostrarErro("Selecione um status.");
            return;
        }
        if (dpDataCadastro.getValue() == null) {
            mostrarErro("Informe a data de cadastro.");
            return;
        }

        BigDecimal valor;
        try {
            valor = new BigDecimal(txtValorDiaria.getText().replace(",", "."));
            if (valor.compareTo(BigDecimal.ZERO) <= 0)
                throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            mostrarErro("Valor da Diária inválido. Use formato: 150.00");
            return;
        }

        Equipamento e = new Equipamento();
        e.setNome(txtNome.getText().trim());
        e.setDescricao(txtDescricao.getText().trim());
        e.setCategoria(cbCategoria.getValue());
        e.setValorDiaria(valor);
        e.setStatus(cbStatus.getValue());
        e.setDataCadastro(dpDataCadastro.getValue());

        try {
            if (equipamentoSelecionado == null) {
                dao.inserir(e);
                mostrarSucesso("Equipamento inserido com sucesso!");
            } else {
                e.setId(equipamentoSelecionado.getId());
                dao.alterar(e);
                mostrarSucesso("Equipamento alterado com sucesso!");
            }
            limpar();
            carregarTabela(dao::listar);
        } catch (SQLException ex) {
            mostrarErro("Erro ao salvar: " + ex.getMessage());
        }
    }

    // ── Excluir ──────────────────────────────────────────────
    @FXML
    private void excluir() {
        if (equipamentoSelecionado == null)
            return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar exclusão");
        confirm.setHeaderText("Excluir equipamento?");
        confirm.setContentText("Deseja excluir \"" + equipamentoSelecionado.getNome() + "\"?\n"
                + "Esta ação não pode ser desfeita.");

        confirm.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.OK) {
                try {
                    dao.excluir(equipamentoSelecionado.getId());
                    mostrarSucesso("Equipamento excluído com sucesso!");
                    limpar();
                    carregarTabela(dao::listar);
                } catch (SQLException ex) {
                    mostrarErro("Erro ao excluir: " + ex.getMessage());
                }
            }
        });
    }

    // ── Limpar formulário ────────────────────────────────────
    @FXML
    private void limpar() {
        equipamentoSelecionado = null;
        txtNome.clear();
        txtDescricao.clear();
        cbCategoria.setValue(null);
        txtValorDiaria.clear();
        cbStatus.setValue("disponivel");
        dpDataCadastro.setValue(LocalDate.now());
        lblTituloForm.setText("Novo Equipamento");
        btnExcluir.setDisable(true);
        lblErro.setText("");
        tableEquipamentos.getSelectionModel().clearSelection();
    }

    // ── Voltar ao menu ───────────────────────────────────────
    @FXML
    private void voltarMenu() throws IOException {
        App.setRoot("primary");
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

    // Interface funcional para o lambda do carregarTabela
    @FunctionalInterface
    interface DAOSupplier<T> {
        T get() throws SQLException;
    }
}
