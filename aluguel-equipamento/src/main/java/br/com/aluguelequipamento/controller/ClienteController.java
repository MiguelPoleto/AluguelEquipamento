package br.com.aluguelequipamento.controller;

import java.io.IOException;
import java.net.URL;
import java.sql.Date;
import java.util.List;
import java.util.ResourceBundle;

import br.com.App;
import br.com.aluguelequipamento.model.dao.ClienteDAO;
import br.com.aluguelequipamento.model.domain.Cliente;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

public class ClienteController implements Initializable {
    @FXML
    private TextField txtNome;
    @FXML
    private TextField txtEmail;
    @FXML
    private TextField txtTelefone;
    @FXML
    private TextField txtEndereco;
    @FXML
    private TextField txtCpf;
    @FXML
    private TableView<Cliente> tableView;
    @FXML
    private TableColumn<Cliente, String> colNome;
    @FXML
    private TableColumn<Cliente, String> colEmail;
    @FXML
    private TableColumn<Cliente, String> colTelefone;
    @FXML
    private TableColumn<Cliente, String> colEndereco;
    @FXML
    private TableColumn<Cliente, String> colCpf;
    @FXML
    private TableColumn<Cliente, Integer> colId;
    @FXML
    private TableColumn<Cliente, Date> colDataCadastro;
    private List<Cliente> listClientes;
    private ObservableList<Cliente> observableListClientes;

    public void initialize(URL url, ResourceBundle rb) {
        
    } 

    public void carregarTableView() {
        ClienteDAO clienteDAO = new ClienteDAO();
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));      
        colTelefone.setCellValueFactory(new PropertyValueFactory<>("telefone"));
        colEndereco.setCellValueFactory(new PropertyValueFactory<>("endereco"));
        colCpf.setCellValueFactory(new PropertyValueFactory<>("cpf"));
        colDataCadastro.setCellValueFactory(new PropertyValueFactory<>("dataCadastro"));
        
    }
    @FXML
    private void handleNovo(ActionEvent event) {
        System.out.println("Novo clicado");
    }

    @FXML
    private void handleSalvar(ActionEvent event) {
        System.out.println("Salvar clicado");
    }

    @FXML
    private void handleExcluir(ActionEvent event) {
        System.out.println("Excluir clicado");
    }

    @FXML
    private void handleLimpar(ActionEvent event) {
        System.out.println("Limpar clicado");
    }

    @FXML
    private void switchToPrimary() throws IOException {
        App.setRoot("primary");
    }
}