package br.com.aluguelequipamento.controller;

import java.io.IOException;
import java.net.URL;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

import br.com.App;
import br.com.aluguelequipamento.model.dao.ClienteDAO;
import br.com.aluguelequipamento.model.domain.Cliente;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

public class ClienteController implements Initializable {

    @FXML private TextField txtNome;
    @FXML private TextField txtEmail;
    @FXML private TextField txtTelefone;
    @FXML private TextField txtEndereco;
    @FXML private TextField txtCpf;
    @FXML private DatePicker dpDataCadastro;
    @FXML private TableView<Cliente> tableView;
    @FXML private TableColumn<Cliente, Integer> colId;
    @FXML private TableColumn<Cliente, String> colNome;
    @FXML private TableColumn<Cliente, String> colEmail;
    @FXML private TableColumn<Cliente, String> colTelefone;
    @FXML private TableColumn<Cliente, String> colCpf;
    @FXML private TableColumn<Cliente, Date> colDataCadastro;

    private List<Cliente> listClientes;
    private ObservableList<Cliente> observableListClientes;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        dpDataCadastro.setValue(LocalDate.now());
        carregarTableView();
        tableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> selecionarItemTableViewClientes(newValue));
    }
    private boolean validarCampos() {
        StringBuilder erros = new StringBuilder();

        if (txtNome.getText().isBlank())
            erros.append("• Nome é obrigatório.\n");
        else if (txtNome.getText().length() > 100)
            erros.append("• Nome deve ter no máximo 100 caracteres.\n");

        if (txtCpf.getText().isBlank())
            erros.append("• CPF é obrigatório.\n");
        else if (txtCpf.getText().length() > 14)
            erros.append("• CPF deve ter no máximo 14 caracteres.\n");

        if (txtTelefone.getText().isBlank())
            erros.append("• Telefone é obrigatório.\n");
        else if (txtTelefone.getText().length() > 15)
            erros.append("• Telefone deve ter no máximo 15 caracteres.\n");

        if (dpDataCadastro.getValue() == null)
            erros.append("• Data de cadastro é obrigatória.\n");

        if (!erros.isEmpty()) {
            alerta(Alert.AlertType.WARNING, "Campos obrigatórios", erros.toString());
            return false;
        }
        return true;
    }

    private void alerta(Alert.AlertType tipo, String titulo, String mensagem) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }

    private Cliente construirCliente() {
        Cliente c = new Cliente();
        c.setNome(txtNome.getText().trim());
        c.setEmail(txtEmail.getText().trim());
        c.setTelefone(txtTelefone.getText().trim());
        c.setCpf(txtCpf.getText().trim());
        c.setEndereco(txtEndereco.getText().trim());
        c.setDataCadastro(dpDataCadastro.getValue());
        return c;
    }

    private void limparCampos() {
        txtNome.clear();
        txtEmail.clear();
        txtTelefone.clear();
        txtCpf.clear();
        txtEndereco.clear();
        dpDataCadastro.setValue(LocalDate.now());
        tableView.getSelectionModel().clearSelection();
    }

    public void carregarTableView() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colTelefone.setCellValueFactory(new PropertyValueFactory<>("telefone"));
        colCpf.setCellValueFactory(new PropertyValueFactory<>("cpf"));
        colDataCadastro.setCellValueFactory(new PropertyValueFactory<>("dataCadastro"));

        ClienteDAO clienteDAO = new ClienteDAO();
        try {
            listClientes = clienteDAO.listar();
            observableListClientes = FXCollections.observableArrayList(listClientes);
            tableView.setItems(observableListClientes);
        } catch (SQLException e) {
            alerta(Alert.AlertType.ERROR, "Erro", "Erro ao carregar clientes:\n" + e.getMessage());
        }
    }

    public void selecionarItemTableViewClientes(Cliente cliente) {
        if (cliente != null) {
            txtNome.setText(cliente.getNome());
            txtEmail.setText(cliente.getEmail());
            txtTelefone.setText(cliente.getTelefone());
            txtCpf.setText(cliente.getCpf());
            txtEndereco.setText(cliente.getEndereco());
            dpDataCadastro.setValue(cliente.getDataCadastro());
        } else {
            limparCampos();
        }
    }

    @FXML
    private void handleNovo(ActionEvent event) {
        if (!validarCampos()) return;

        ClienteDAO clienteDAO = new ClienteDAO();
        try {
            clienteDAO.inserir(construirCliente());
            alerta(Alert.AlertType.INFORMATION, "Sucesso", "Cliente cadastrado com sucesso!");
            limparCampos();         // ← limpa após inserir
            carregarTableView();
        } catch (SQLException e) {
            alerta(Alert.AlertType.ERROR, "Erro ao inserir", e.getMessage());
        }
    }

    @FXML
    private void handleSalvar(ActionEvent event) {
        Cliente selecionado = tableView.getSelectionModel().getSelectedItem();
        if (selecionado == null) {
            alerta(Alert.AlertType.WARNING, "Nenhum registro selecionado",
                    "Selecione um cliente na tabela para alterar.");
            return;
        }
        if (!validarCampos()) return;

        selecionado.setNome(txtNome.getText().trim());
        selecionado.setEmail(txtEmail.getText().trim());
        selecionado.setTelefone(txtTelefone.getText().trim());
        selecionado.setCpf(txtCpf.getText().trim());
        selecionado.setEndereco(txtEndereco.getText().trim());
        selecionado.setDataCadastro(dpDataCadastro.getValue());

        ClienteDAO clienteDAO = new ClienteDAO();
        try {
            clienteDAO.alterar(selecionado);
            alerta(Alert.AlertType.INFORMATION, "Sucesso", "Cliente alterado com sucesso!");
            limparCampos();
            carregarTableView();
        } catch (SQLException e) {
            alerta(Alert.AlertType.ERROR, "Erro ao alterar", e.getMessage());
        }
    }

    @FXML
    private void handleExcluir(ActionEvent event) {
        Cliente selecionado = tableView.getSelectionModel().getSelectedItem();
        if (selecionado == null) {
            alerta(Alert.AlertType.WARNING, "Nenhum registro selecionado", "Selecione um cliente na tabela para excluir.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar exclusão");
        confirm.setHeaderText(null);
        confirm.setContentText("Deseja excluir o cliente \"" + selecionado.getNome() + "\"?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        ClienteDAO clienteDAO = new ClienteDAO();
        try {
            clienteDAO.excluir(selecionado.getId());
            alerta(Alert.AlertType.INFORMATION, "Sucesso", "Cliente excluído com sucesso!");
            limparCampos();
            carregarTableView();
        } catch (SQLException e) {
            alerta(Alert.AlertType.ERROR, "Erro ao excluir", e.getMessage());
        }
    }

    @FXML
    private void handleLimpar(ActionEvent event) {
        limparCampos();
    }

    @FXML
    private void switchToPrimary() throws IOException {
        App.setRoot("primary");
    }
}
