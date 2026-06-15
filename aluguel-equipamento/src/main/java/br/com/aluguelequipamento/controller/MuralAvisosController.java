package br.com.aluguelequipamento.controller;

import br.com.App;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import sockets.thread.AvisoGrupo;
import sockets.thread.ContadorGrupo;
import sockets.thread.RespostaMural;

public class MuralAvisosController {

    private static final String HOST = "127.0.0.1";
    private static final int PORTA = 12345;

    @FXML private TextField txtGrupo;
    @FXML private Button btnConectar;
    @FXML private Label lblGrupoAtual;
    @FXML private Label lblTemaGrupo;
    @FXML private Label lblStatus;
    @FXML private Label lblUltimaAtualizacao;
    @FXML private Label lblTituloDestaque;
    @FXML private Label lblMensagemDestaque;
    @FXML private Label lblTimestampDestaque;
    @FXML private Label lblTotalAvisos;
    @FXML private TableView<ContadorGrupo> tabelaRanking;
    @FXML private TableColumn<ContadorGrupo, String> colPosicao;
    @FXML private TableColumn<ContadorGrupo, String> colGrupo;
    @FXML private TableColumn<ContadorGrupo, String> colAcessos;
    @FXML private ListView<String> listaAvisos;

    private Thread threadAvisos;

    @FXML
    public void initialize() {
        configurarTabela();
        txtGrupo.setText("1");
        lblStatus.setText("Informe um grupo de 1 a 10 e conecte ao servidor.");
        lblUltimaAtualizacao.setText("-");
        lblGrupoAtual.setText("Nenhum grupo carregado");
        lblTemaGrupo.setText("Aguardando conexão");
        lblTituloDestaque.setText("Sem aviso selecionado");
        lblMensagemDestaque.setText("Os avisos recebidos do servidor aparecerão aqui.");
        lblTimestampDestaque.setText("-");
        lblTotalAvisos.setText("0 avisos recebidos");
    }

    private void configurarTabela() {
        colPosicao.setCellValueFactory(cell -> {
            int posicao = tabelaRanking.getItems().indexOf(cell.getValue()) + 1;
            return new SimpleStringProperty(String.valueOf(posicao));
        });
        colGrupo.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getIdGrupo() + " - " + cell.getValue().getNomeGrupo()));
        colAcessos.setCellValueFactory(cell -> new SimpleStringProperty(
                String.valueOf(cell.getValue().getQuantidadeUtilizacoes())));
    }

    @FXML
    private void conectarServidor() {
        Integer idGrupo = lerGrupoInformado();
        if (idGrupo == null) {
            return;
        }

        btnConectar.setDisable(true);
        lblStatus.setText("Conectando ao servidor " + HOST + ":" + PORTA + "...");

        Thread threadConexao = new Thread(() -> buscarRespostaServidor(idGrupo), "socket-mural-avisos");
        threadConexao.setDaemon(true);
        threadConexao.start();
    }

    private Integer lerGrupoInformado() {
        try {
            int idGrupo = Integer.parseInt(txtGrupo.getText().trim());
            if (idGrupo < 1 || idGrupo > 10) {
                lblStatus.setText("O número do grupo deve estar entre 1 e 10.");
                return null;
            }
            return idGrupo;
        } catch (NumberFormatException e) {
            lblStatus.setText("Informe um número inteiro para o grupo.");
            return null;
        }
    }

    private void buscarRespostaServidor(int idGrupo) {
        try (Socket socket = new Socket(HOST, PORTA);
                ObjectOutputStream saida = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream entrada = new ObjectInputStream(socket.getInputStream())) {

            saida.writeObject(idGrupo);
            saida.flush();

            Object resposta = entrada.readObject();
            if (!(resposta instanceof RespostaMural mural)) {
                throw new IOException("Resposta inesperada do servidor.");
            }

            Platform.runLater(() -> carregarResposta(mural));
        } catch (IOException | ClassNotFoundException e) {
            Platform.runLater(() -> {
                pararLoopAvisos();
                lblStatus.setText("Não foi possível conectar ao servidor: " + e.getMessage());
                btnConectar.setDisable(false);
            });
        }
    }

    private void carregarResposta(RespostaMural mural) {
        pararLoopAvisos();

        lblGrupoAtual.setText("Grupo atual: " + mural.getIdGrupo() + " - " + mural.getNomeGrupo());
        lblTemaGrupo.setText("Tema carregado: " + mural.getNomeGrupo());
        lblUltimaAtualizacao.setText(mural.getUltimaAtualizacao());
        lblStatus.setText("Dados recebidos com sucesso.");
        btnConectar.setDisable(false);

        List<ContadorGrupo> ranking = new ArrayList<>(mural.getRanking() == null ? List.of() : mural.getRanking());
        ranking.sort(Comparator.comparingInt(ContadorGrupo::getQuantidadeUtilizacoes).reversed()
                .thenComparingInt(ContadorGrupo::getIdGrupo));
        tabelaRanking.setItems(FXCollections.observableArrayList(ranking));

        List<AvisoGrupo> avisos = new ArrayList<>(mural.getAvisosGrupo() == null ? List.of() : mural.getAvisosGrupo());
        listaAvisos.setItems(FXCollections.observableArrayList(
                avisos.stream()
                        .map(aviso -> aviso.getTimestamp() + " - " + aviso.getTitulo() + ": " + aviso.getMensagem())
                        .toList()));
        lblTotalAvisos.setText(avisos.size() + (avisos.size() == 1 ? " aviso recebido" : " avisos recebidos"));

        if (avisos.isEmpty()) {
            lblTituloDestaque.setText("Nenhum aviso recebido");
            lblMensagemDestaque.setText("O servidor não retornou mensagens para este grupo.");
            lblTimestampDestaque.setText("-");
            return;
        }

        exibirAviso(avisos.get(0));
        iniciarLoopAvisos(avisos);
    }

    private void iniciarLoopAvisos(List<AvisoGrupo> avisos) {
        threadAvisos = new Thread(() -> {
            int indice = 0;
            while (!Thread.currentThread().isInterrupted()) {
                AvisoGrupo aviso = avisos.get(indice);
                Platform.runLater(() -> exibirAviso(aviso));
                indice = (indice + 1) % avisos.size();

                try {
                    Thread.sleep(4000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "loop-avisos-grupo");
        threadAvisos.setDaemon(true);
        threadAvisos.start();
    }

    private void exibirAviso(AvisoGrupo aviso) {
        lblTituloDestaque.setText(aviso.getTitulo());
        lblMensagemDestaque.setText(aviso.getMensagem());
        lblTimestampDestaque.setText("Recebido em " + aviso.getTimestamp());
    }

    private void pararLoopAvisos() {
        if (threadAvisos != null && threadAvisos.isAlive()) {
            threadAvisos.interrupt();
        }
    }

    @FXML
    private void voltar() throws IOException {
        pararLoopAvisos();
        App.setRoot("primary");
    }
}
