package br.com.aluguelequipamento.controller;

import java.io.IOException;
import java.sql.Date;
import java.util.List;

import br.com.App;
import br.com.aluguelequipamento.model.dao.ClienteDAO;
import br.com.aluguelequipamento.model.dao.DevolucaoDAO;
import br.com.aluguelequipamento.model.dao.EquipamentoDAO;
import br.com.aluguelequipamento.model.dao.RetiradaDAO;
import br.com.aluguelequipamento.model.domain.Cliente;
import br.com.aluguelequipamento.model.domain.Devolucao;
import br.com.aluguelequipamento.model.domain.Equipamento;
import br.com.aluguelequipamento.model.domain.Retirada;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

public class DevolucaoController {

    @FXML private TableView<Devolucao> tableView;
    @FXML private TableColumn<Devolucao, Integer> colId;
    @FXML private TableColumn<Devolucao, String> colRetirada;
    @FXML private TableColumn<Devolucao, Date> colDataDevolucao;
    @FXML private TableColumn<Devolucao, String> colObservacao;
    @FXML private TableColumn<Devolucao, String> colStatus;

    @FXML private ComboBox<Retirada> cmbRetirada;
    @FXML private DatePicker dpDataDevolucao;
    @FXML private TextField txtObservacao;

    @FXML private Label lblInfoCliente;
    @FXML private Label lblInfoEquipamento;
    @FXML private Label lblInfoDataRetirada;
    @FXML private Label lblInfoPrevDev;
    @FXML private Label lblInfoValorAluguel;
    @FXML private Label lblMensagem;
    @FXML private Label lblStatus;

    private List<Devolucao> listDevolucoes;
    private ObservableList<Devolucao> observableListDevolucoes = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colRetirada.setCellValueFactory(new PropertyValueFactory<>("descricaoRetirada"));
        colDataDevolucao.setCellValueFactory(new PropertyValueFactory<>("dataDevolucao"));
        colObservacao.setCellValueFactory(new PropertyValueFactory<>("observacao"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        carregarTableView();
        carregarComboBoxRetiradas();

        cmbRetirada.getSelectionModel().selectedItemProperty().addListener((observer, oldValue, newValue) -> selecionarItemComboBoxRetirada(newValue));
        tableView.getSelectionModel().selectedItemProperty().addListener((observer, oldValue, newValue) -> selecionarItemTableView(newValue));
    }

    private void alerta(Alert.AlertType tipo, String titulo, String mensagem) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }

    private boolean validarCampos() {
        StringBuilder erros = new StringBuilder();

        if (cmbRetirada.getValue() == null)
            erros.append("• Selecione uma retirada.\n");

        if (dpDataDevolucao.getValue() == null)
            erros.append("• Data de devolução é obrigatória.\n");

        if (!erros.isEmpty()) {
            alerta(Alert.AlertType.WARNING, "Campos inválidos", erros.toString());
            return false;
        }
        return true;
    }

    private void limparCampos() {
        cmbRetirada.getSelectionModel().clearSelection();
        dpDataDevolucao.setValue(null);
        txtObservacao.clear();
        lblInfoCliente.setText("—");
        lblInfoEquipamento.setText("—");
        lblInfoDataRetirada.setText("—");
        lblInfoPrevDev.setText("—");
        lblInfoValorAluguel.setText("—");
        lblMensagem.setText("");
        lblStatus.setText("");
        tableView.getSelectionModel().clearSelection();
    }

    private void carregarTableView() {
        try {
            listDevolucoes = new DevolucaoDAO().listar();
            observableListDevolucoes = FXCollections.observableArrayList(listDevolucoes);
            tableView.setItems(observableListDevolucoes);
        } catch (Exception e) {
            alerta(Alert.AlertType.ERROR, "Erro", "Erro ao carregar devoluções:\n" + e.getMessage());
        }
    }

    private void carregarComboBoxRetiradas() {
        try {
            List<Retirada> lista = new RetiradaDAO().listar();
            cmbRetirada.setItems(FXCollections.observableArrayList(lista));
        } catch (Exception e) {
            alerta(Alert.AlertType.ERROR, "Erro", "Erro ao carregar retiradas:\n" + e.getMessage());
        }
    }

    private void selecionarItemComboBoxRetirada(Retirada retirada) {
        if (retirada == null) {
            lblInfoCliente.setText("—");
            lblInfoEquipamento.setText("—");
            lblInfoDataRetirada.setText("—");
            lblInfoPrevDev.setText("—");
            lblInfoValorAluguel.setText("—");
            return;
        }
        try {
            Cliente cliente = new ClienteDAO().buscarPorId(retirada.getClienteId());
            Equipamento equip = new EquipamentoDAO().buscarPorId(retirada.getEquipamentoId());

            lblInfoCliente.setText(cliente != null ? cliente.getNome() : "—");
            lblInfoEquipamento.setText(equip != null ? equip.getNome() : "—");
            lblInfoDataRetirada.setText(retirada.getDataRetirada() != null? retirada.getDataRetirada().toString() : "—");
            lblInfoPrevDev.setText(retirada.getDataPrevDevolucao() != null? retirada.getDataPrevDevolucao().toString() : "—");
            lblInfoValorAluguel.setText(retirada.getValorTotal() != null? "R$ " + retirada.getValorTotal() : "—");
        } catch (Exception e) {
            alerta(Alert.AlertType.ERROR, "Erro", "Erro ao carregar dados da retirada:\n" + e.getMessage());
        }
    }

    public void selecionarItemTableView(Devolucao devolucao) {
        if (devolucao != null) {
            cmbRetirada.getItems().stream().filter(r -> r.getId() == devolucao.getRetiradaId()).findFirst().ifPresent(cmbRetirada::setValue);
            dpDataDevolucao.setValue(devolucao.getDataDevolucao());
            txtObservacao.setText(devolucao.getObservacao());
            lblStatus.setText(devolucao.getStatus());
        } else {
            limparCampos();
        }
    }

    @FXML
    private void handleNovo() {
        if (!validarCampos()) return;

        Devolucao devolucao = new Devolucao();
        devolucao.setRetiradaId(cmbRetirada.getValue().getId());
        devolucao.setDataDevolucao(dpDataDevolucao.getValue());
        devolucao.setObservacao(txtObservacao.getText().trim());
        devolucao.setStatus("concluida");

        try {
            new DevolucaoDAO().inserir(devolucao);
            alerta(Alert.AlertType.INFORMATION, "Sucesso", "Devolução registrada com sucesso!");
            limparCampos();
            carregarTableView();
            carregarComboBoxRetiradas();
        } catch (Exception e) {
            alerta(Alert.AlertType.ERROR, "Erro ao inserir", e.getMessage());
        }
    }

    @FXML
    private void handleSalvar() {
        Devolucao selecionada = tableView.getSelectionModel().getSelectedItem();
        if (selecionada == null) {
            alerta(Alert.AlertType.WARNING, "Nenhum registro selecionado",
                    "Selecione uma devolução na tabela para alterar.");
            return;
        }
        if (!validarCampos()) return;

        selecionada.setRetiradaId(cmbRetirada.getValue().getId());
        selecionada.setDataDevolucao(dpDataDevolucao.getValue());
        selecionada.setObservacao(txtObservacao.getText().trim());
        selecionada.setStatus("concluida");

        try {
            new DevolucaoDAO().alterar(selecionada);
            alerta(Alert.AlertType.INFORMATION, "Sucesso", "Devolução alterada com sucesso!");
            limparCampos();
            carregarTableView();
        } catch (Exception e) {
            alerta(Alert.AlertType.ERROR, "Erro ao alterar", e.getMessage());
        }
    }

    @FXML
    private void handleExcluir() {
        Devolucao selecionada = tableView.getSelectionModel().getSelectedItem();
        if (selecionada == null) {
            alerta(Alert.AlertType.WARNING, "Nenhum registro selecionado",
                    "Selecione uma devolução na tabela para excluir.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar exclusão");
        confirm.setHeaderText(null);
        confirm.setContentText("Deseja excluir esta devolução?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            new DevolucaoDAO().excluir(selecionada.getId());
            alerta(Alert.AlertType.INFORMATION, "Sucesso", "Devolução excluída com sucesso!");
            limparCampos();
            carregarTableView();
            carregarComboBoxRetiradas();
        } catch (Exception e) {
            alerta(Alert.AlertType.ERROR, "Erro ao excluir", e.getMessage());
        }
    }

    @FXML
    private void handleLimpar() {
        limparCampos();
    }

    @FXML
    private void switchToPrimary() throws IOException {
        App.setRoot("primary");
    }
}
