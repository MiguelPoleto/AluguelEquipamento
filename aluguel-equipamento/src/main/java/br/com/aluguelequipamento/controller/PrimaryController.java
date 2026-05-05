package br.com.aluguelequipamento.controller;

import br.com.App;
import br.com.aluguelequipamento.model.dao.*;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.io.IOException;
import java.sql.SQLException;

public class PrimaryController {

    @FXML private Label lblEquipamentos;
    @FXML private Label lblClientes;
    @FXML private Label lblReservas;
    @FXML private Label lblRetiradas;
    @FXML private Label lblManutencoes;

    @FXML
    public void initialize() {
        carregarResumos();
    }

    private void carregarResumos() {
        try {
            int totalEquip = new EquipamentoDAO().listar().size();
            int disponiveis = new EquipamentoDAO().listarDisponiveis().size();
            lblEquipamentos.setText(totalEquip + " cadastrados · " + disponiveis + " disponíveis");
        } catch (SQLException e) {
            lblEquipamentos.setText("Erro ao carregar");
        }

        try {
            int totalClientes = new ClienteDAO().listar().size();
            lblClientes.setText(totalClientes + " cadastrados");
        } catch (SQLException e) {
            lblClientes.setText("Erro ao carregar");
        }

        try {
            int ativas = new ReservaDAO().listarAtivas().size();
            lblReservas.setText(ativas + " reservas ativas");
        } catch (SQLException e) {
            lblReservas.setText("Erro ao carregar");
        }

        try {
            int ativas = new RetiradaDAO().listarAtivas().size();
            lblRetiradas.setText(ativas + " retiradas ativas");
        } catch (SQLException e) {
            lblRetiradas.setText("Erro ao carregar");
        }

        try {
            int em = new ManutencaoDAO().contarEmAndamento();
            lblManutencoes.setText(em + " em andamento · limite 10");
        } catch (SQLException e) {
            lblManutencoes.setText("Erro ao carregar");
        }
    }

    // Navegação — cadastros
    @FXML private void abrirEquipamentos() throws IOException {
        App.setRoot("equipamento");
    }

    @FXML private void abrirClientes() throws IOException {
        App.setRoot("cliente");
    }

    // Navegação — processos
    @FXML private void abrirReservas() throws IOException {
        App.setRoot("reserva");
    }

    @FXML private void abrirRetiradas() throws IOException {
        App.setRoot("retirada");
    }

    @FXML private void abrirDevolucoes() throws IOException {
        App.setRoot("devolucao");
    }

    @FXML private void abrirManutencoes() throws IOException {
        App.setRoot("manutencao");
    }

    // Relatórios
    @FXML private void abrirRelatorioAluguel() throws IOException {
        App.setRoot("relatorio-aluguel");
    }
    @FXML private void abrirRelatorioReservas() throws IOException {
        App.setRoot("relatorio-reservas");
    }
    @FXML private void abrirRelatorioManutencao() throws IOException {
        App.setRoot("relatorio-manutencao");
    }

    // Banco de dados
    @FXML private void abrirBancoDados() throws IOException {
        App.setRoot("banco-dados");
    }

    @FXML private void sair() {
        System.exit(0);
    }
}
