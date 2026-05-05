package br.com.aluguelequipamento.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

import br.com.App;
import br.com.aluguelequipamento.model.dao.ClienteDAO;
import br.com.aluguelequipamento.model.dao.EquipamentoDAO;
import br.com.aluguelequipamento.model.dao.ReservaDAO;
import br.com.aluguelequipamento.model.dao.RetiradaDAO;
import br.com.aluguelequipamento.model.domain.Cliente;
import br.com.aluguelequipamento.model.domain.Equipamento;
import br.com.aluguelequipamento.model.domain.Reserva;
import br.com.aluguelequipamento.model.domain.Retirada;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

public class RetiradaController {

    @FXML private TableView<Retirada> tableView;
    @FXML private TableColumn<Retirada, Integer> colId;
    @FXML private TableColumn<Retirada, String> colCliente;
    @FXML private TableColumn<Retirada, String> colEquipamento;
    @FXML private TableColumn<Retirada, String> colDataRetirada;
    @FXML private TableColumn<Retirada, String> colDataPrevDev;
    @FXML private TableColumn<Retirada, Double> colValorTotal;
    @FXML private TableColumn<Retirada, String> colStatus;

    @FXML private ComboBox<Cliente> cmbCliente;
    @FXML private ComboBox<Equipamento> cmbEquipamento;
    @FXML private ComboBox<Reserva> cmbReserva;
    @FXML private ComboBox<String> cmbStatus;
    @FXML private DatePicker dpDataRetirada;
    @FXML private DatePicker dpDataPrevDev;
    @FXML private TextField txtValorTotal;
    @FXML private TextField txtObservacao;

    private List<Retirada> listRetiradas;
    private ObservableList<Retirada> observableListRetiradas;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCliente.setCellValueFactory(new PropertyValueFactory<>("nomeCliente"));
        colEquipamento.setCellValueFactory(new PropertyValueFactory<>("nomeEquipamento"));
        colDataRetirada.setCellValueFactory(new PropertyValueFactory<>("dataRetirada"));
        colDataPrevDev.setCellValueFactory(new PropertyValueFactory<>("dataPrevDevolucao"));
        colValorTotal.setCellValueFactory(new PropertyValueFactory<>("valorTotal"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        dpDataRetirada.setValue(LocalDate.now());
        cmbStatus.setItems(FXCollections.observableArrayList("ativa", "concluida"));
        cmbStatus.setValue("ativa");

        carregarTableView();
        carregarComboBoxClientes();
        carregarComboBoxEquipamentos();
        carregarComboBoxReservas();

        cmbEquipamento.valueProperty().addListener((obs, a, n) -> calcularValor());
        dpDataRetirada.valueProperty().addListener((obs, a, n) -> calcularValor());
        dpDataPrevDev.valueProperty().addListener((obs, a, n)  -> calcularValor());

        tableView.getSelectionModel().selectedItemProperty().addListener(
                (obs, a, n) -> selecionarItemTableView(n));
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

        if (cmbCliente.getValue() == null)
            erros.append("• Selecione um cliente.\n");

        if (cmbEquipamento.getValue() == null) {
            erros.append("• Selecione um equipamento.\n");
        } else if ("em_manutencao".equals(cmbEquipamento.getValue().getStatus())) {
            // Barreira extra caso o equipamento tenha mudado de status após ser carregado
            erros.append("• O equipamento selecionado está em manutenção e não pode ser retirado.\n");
        }

        if (dpDataRetirada.getValue() == null)
            erros.append("• Data de retirada é obrigatória.\n");

        if (dpDataPrevDev.getValue() == null) {
            erros.append("• Data prevista de devolução é obrigatória.\n");
        } else if (dpDataRetirada.getValue() != null
                && dpDataPrevDev.getValue().isBefore(dpDataRetirada.getValue())) {
            erros.append("• Data de devolução não pode ser anterior à data de retirada.\n");
        }

        if (txtValorTotal.getText().isBlank() || txtValorTotal.getText().equals("Data inválida"))
            erros.append("• Valor total inválido. Verifique as datas.\n");

        if (!erros.isEmpty()) {
            alerta(Alert.AlertType.WARNING, "Campos inválidos", erros.toString());
            return false;
        }
        return true;
    }

    private BigDecimal converterValor() {
        String texto = txtValorTotal.getText()
                .replace("R$", "")
                .replace("\u00a0", "")
                .replace(".", "")
                .replace(",", ".")
                .trim();
        try {
            return new BigDecimal(texto);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private void limparCampos() {
        cmbCliente.setValue(null);
        cmbEquipamento.setValue(null);
        cmbReserva.setValue(null);
        cmbStatus.setValue("ativa");
        dpDataRetirada.setValue(LocalDate.now());
        dpDataPrevDev.setValue(null);
        txtValorTotal.clear();
        txtObservacao.clear();
        tableView.getSelectionModel().clearSelection();
    }

    public void carregarTableView() {
        RetiradaDAO dao = new RetiradaDAO();
        try {
            listRetiradas = dao.listar();
            observableListRetiradas = FXCollections.observableArrayList(listRetiradas);
            tableView.setItems(observableListRetiradas);
        } catch (Exception e) {
            alerta(Alert.AlertType.ERROR, "Erro", "Erro ao carregar retiradas:\n" + e.getMessage());
        }
    }

    public void carregarComboBoxClientes() {
        try {
            cmbCliente.getItems().setAll(new ClienteDAO().listar());
        } catch (Exception e) {
            alerta(Alert.AlertType.ERROR, "Erro", "Erro ao carregar clientes:\n" + e.getMessage());
        }
    }

    /**
     * Carrega apenas equipamentos que NÃO estão em manutenção.
     * Status bloqueado: "em_manutencao"
     */
    public void carregarComboBoxEquipamentos() {
        try {
            List<Equipamento> todos = new EquipamentoDAO().listar();
            List<Equipamento> disponiveis = todos.stream()
                    .filter(e -> !"em_manutencao".equals(e.getStatus()))
                    .toList();
            cmbEquipamento.getItems().setAll(disponiveis);
        } catch (Exception e) {
            alerta(Alert.AlertType.ERROR, "Erro", "Erro ao carregar equipamentos:\n" + e.getMessage());
        }
    }

    public void carregarComboBoxReservas() {
        try {
            cmbReserva.getItems().setAll(new ReservaDAO().listar());
        } catch (Exception e) {
            alerta(Alert.AlertType.ERROR, "Erro", "Erro ao carregar reservas:\n" + e.getMessage());
        }
    }

    public void calcularValor() {
        Equipamento eq = cmbEquipamento.getValue();
        LocalDate retirada  = dpDataRetirada.getValue();
        LocalDate devolucao = dpDataPrevDev.getValue();

        if (eq == null || retirada == null || devolucao == null) {
            txtValorTotal.clear();
            return;
        }
        if (devolucao.isBefore(retirada)) {
            txtValorTotal.setText("Data inválida");
            return;
        }

        long dias = ChronoUnit.DAYS.between(retirada, devolucao);
        if (dias == 0) dias = 1;

        BigDecimal total = eq.getValorDiaria().multiply(BigDecimal.valueOf(dias));
        txtValorTotal.setText(NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(total));
    }

    private void selecionarItemTableView(Retirada retirada) {
        if (retirada != null) {
            cmbCliente.getItems().stream()
                    .filter(c -> c.getId() == retirada.getClienteId())
                    .findFirst().ifPresent(cmbCliente::setValue);

            cmbEquipamento.getItems().stream()
                    .filter(e -> e.getId() == retirada.getEquipamentoId())
                    .findFirst().ifPresent(cmbEquipamento::setValue);

            dpDataRetirada.setValue(retirada.getDataRetirada());
            dpDataPrevDev.setValue(retirada.getDataPrevDevolucao());
            txtValorTotal.setText(NumberFormat.getCurrencyInstance(new Locale("pt", "BR"))
                    .format(retirada.getValorTotal()));
            txtObservacao.setText(retirada.getObservacao());
            cmbStatus.setValue(retirada.getStatus());
        } else {
            limparCampos();
        }
    }

    @FXML
    private void handleNovo() {
        // Não permite inserir se houver linha selecionada na tabela
        if (tableView.getSelectionModel().getSelectedItem() != null) {
            alerta(Alert.AlertType.WARNING, "Registro selecionado",
                    "Há um registro selecionado na tabela.\n" +
                    "Clique em 'Limpar' para deselecionar antes de adicionar um novo.");
            return;
        }

        if (!validarCampos()) return;

        Retirada retirada = new Retirada();
        retirada.setClienteId(cmbCliente.getValue().getId());
        retirada.setEquipamentoId(cmbEquipamento.getValue().getId());
        retirada.setDataRetirada(dpDataRetirada.getValue());
        retirada.setDataPrevDevolucao(dpDataPrevDev.getValue());
        retirada.setStatus(cmbStatus.getValue());
        retirada.setObservacao(txtObservacao.getText().trim());
        retirada.setValorTotal(converterValor());

        try {
            new RetiradaDAO().inserir(retirada);
            alerta(Alert.AlertType.INFORMATION, "Sucesso", "Retirada registrada com sucesso!");
            limparCampos();         // ← limpa após inserir
            carregarTableView();
            carregarComboBoxEquipamentos();
        } catch (Exception e) {
            alerta(Alert.AlertType.ERROR, "Erro ao inserir", e.getMessage());
        }
    }

    @FXML
    private void handleSalvar() {
        Retirada selecionada = tableView.getSelectionModel().getSelectedItem();
        if (selecionada == null) {
            alerta(Alert.AlertType.WARNING, "Nenhum registro selecionado",
                    "Selecione uma retirada na tabela para alterar.");
            return;
        }
        if (!validarCampos()) return;

        selecionada.setClienteId(cmbCliente.getValue().getId());
        selecionada.setEquipamentoId(cmbEquipamento.getValue().getId());
        selecionada.setDataRetirada(dpDataRetirada.getValue());
        selecionada.setDataPrevDevolucao(dpDataPrevDev.getValue());
        selecionada.setStatus(cmbStatus.getValue());
        selecionada.setObservacao(txtObservacao.getText().trim());
        selecionada.setValorTotal(converterValor());

        try {
            new RetiradaDAO().alterar(selecionada);
            alerta(Alert.AlertType.INFORMATION, "Sucesso", "Retirada alterada com sucesso!");
            limparCampos();
            carregarTableView();
        } catch (Exception e) {
            alerta(Alert.AlertType.ERROR, "Erro ao alterar", e.getMessage());
        }
    }

    @FXML
    private void handleExcluir() {
        Retirada selecionada = tableView.getSelectionModel().getSelectedItem();
        if (selecionada == null) {
            alerta(Alert.AlertType.WARNING, "Nenhum registro selecionado",
                    "Selecione uma retirada na tabela para excluir.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar exclusão");
        confirm.setHeaderText(null);
        confirm.setContentText("Deseja excluir esta retirada?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            new RetiradaDAO().excluir(selecionada.getId());
            alerta(Alert.AlertType.INFORMATION, "Sucesso", "Retirada excluída com sucesso!");
            limparCampos();
            carregarTableView();
            carregarComboBoxEquipamentos();
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
