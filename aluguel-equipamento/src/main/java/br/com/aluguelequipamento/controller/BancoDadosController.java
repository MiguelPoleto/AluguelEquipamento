package br.com.aluguelequipamento.controller;

import br.com.App;
import br.com.aluguelequipamento.model.dao.BancoDadosDAO;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public class BancoDadosController {

    @FXML private ComboBox<String> cbTabela;
    @FXML private ComboBox<Integer> cbQuantidade;
    @FXML private Label lblMensagem;
    @FXML private TextArea txtResumo;

    private final BancoDadosDAO bancoDadosDAO = new BancoDadosDAO();
    private final Map<String, String> tabelas = new LinkedHashMap<>();

    @FXML
    public void initialize() {
        tabelas.put("Todas as tabelas", "todas");
        tabelas.put("Clientes", "cliente");
        tabelas.put("Equipamentos", "equipamento");
        tabelas.put("Reservas", "reserva");
        tabelas.put("Retiradas", "retirada");
        tabelas.put("Devoluções", "devolucao");
        tabelas.put("Manutenções", "manutencao");

        cbTabela.setItems(FXCollections.observableArrayList(tabelas.keySet()));
        cbTabela.setValue("Todas as tabelas");

        cbQuantidade.setItems(FXCollections.observableArrayList(5, 10, 15));
        cbQuantidade.setValue(5);

        txtResumo.setText("Escolha a tabela e a quantidade para popular dados fictícios.\n"
                + "Ao apagar uma tabela com vínculos, os dados dependentes também podem ser removidos pelo CASCADE.");
    }

    @FXML
    private void apagarDados() {
        String tabelaLabel = cbTabela.getValue();
        if (tabelaLabel == null) {
            mostrarErro("Selecione uma tabela.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar limpeza");
        confirm.setHeaderText("Apagar dados?");
        confirm.setContentText("Deseja apagar os dados de: " + tabelaLabel + "?\n"
                + "Esta ação não pode ser desfeita.");

        confirm.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.OK) {
                try {
                    bancoDadosDAO.apagarDados(tabelas.get(tabelaLabel));
                    mostrarSucesso("Dados apagados com sucesso.");
                    txtResumo.setText("Limpeza concluída para: " + tabelaLabel);
                } catch (SQLException e) {
                    mostrarErro("Erro ao apagar dados: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void popularDados() {
        String tabelaLabel = cbTabela.getValue();
        Integer quantidade = cbQuantidade.getValue();
        if (tabelaLabel == null) {
            mostrarErro("Selecione uma tabela.");
            return;
        }
        if (quantidade == null) {
            mostrarErro("Selecione a quantidade.");
            return;
        }

        try {
            bancoDadosDAO.popularDados(tabelas.get(tabelaLabel), quantidade);
            mostrarSucesso("Dados fictícios inseridos com sucesso.");
            txtResumo.setText("População concluída.\nTabela: " + tabelaLabel
                    + "\nQuantidade escolhida: " + quantidade
                    + "\nOs registros foram gerados com nomes, datas e valores variados.");
        } catch (SQLException e) {
            mostrarErro("Erro ao popular dados: " + e.getMessage());
        }
    }

    @FXML
    private void voltarMenu() throws IOException {
        App.setRoot("primary");
    }

    private void mostrarErro(String msg) {
        lblMensagem.setStyle("-fx-text-fill: #c0392b; -fx-font-size: 12;");
        lblMensagem.setText(msg);
    }

    private void mostrarSucesso(String msg) {
        lblMensagem.setStyle("-fx-text-fill: #1e6b3a; -fx-font-size: 12;");
        lblMensagem.setText(msg);
    }
}
