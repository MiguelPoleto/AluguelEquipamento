package sockets.thread;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class Servidor {

    private static final int PORTA = 12345;
    private static final GrupoInfo[] GRUPOS = {
        new GrupoInfo(1, "Aluguel de Equipamentos", new String[]{"Alessandro", "Miguel"},
                new String[]{"Cliente", "Equipamento"},
                new String[]{"Retirada", "Devolucao", "Reserva", "Manutencao"}),
        new GrupoInfo(2, "Achados e Perdidos", new String[]{"Roni", "Antonio"},
                new String[]{"Objeto", "Pessoa"},
                new String[]{"Objeto encontrado", "Devolucao de objeto"}),
        new GrupoInfo(3, "Laboratorio de Pesquisa", new String[]{"Andre", "Marcos Lopes"},
                new String[]{"Pesquisador", "Laboratorio"},
                new String[]{"Reserva", "Manutencao"}),
        new GrupoInfo(4, "Corpo de Bombeiros", new String[]{"Eduardo", "Rui"},
                new String[]{"Viatura", "Tipo de Ocorrencia"},
                new String[]{"Escala de Servico", "Ocorrencias"}),
        new GrupoInfo(5, "Pousada", new String[]{"Lucas", "Marcos Antonio"},
                new String[]{"Hospede", "Quarto", "Funcionario"},
                new String[]{"Reserva", "Check-out"}),
        new GrupoInfo(6, "Biblioteca", new String[]{"Yasmim", "Maria Eduarda"},
                new String[]{"Livro", "Usuario"},
                new String[]{"Devolucao", "Emprestimo"}),
        new GrupoInfo(7, "Transporte Escolar", new String[]{"Jose", "Savio"},
                new String[]{"Aluno", "Motorista"},
                new String[]{"Registro de Acesso", "Registro de Viagem"}),
        new GrupoInfo(8, "Producao de Cachaca", new String[]{"Renan", "Joao"},
                new String[]{"Gerente", "Funcionario"},
                new String[]{"Producao", "Armazenamento"}),
        new GrupoInfo(9, "MMA", new String[]{"Arthur", "Bernard"},
                new String[]{"Lutador", "Arbitro"},
                new String[]{"Agendamento de Luta", "Resultado de Luta"}),
        new GrupoInfo(10, "Atelie", new String[]{"Nadson", "Eduardo", "Marco Antonio"},
                new String[]{"Cliente", "Fabrica", "Tipo de Servico"},
                new String[]{"Atendimento", "Terceirizacao", "Retorno"})
    };

    private static final List<ContadorGrupo> contadores = new ArrayList<>();
    private static final List<AvisoGrupo> avisos = new ArrayList<>();
    private static final Object LOCK = new Object();
    private static final SimpleDateFormat FORMATADOR = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) throws IOException {
        inicializarDados();

        try (ServerSocket servidor = new ServerSocket(PORTA)) {
            System.out.println("Servidor do mural iniciado na porta " + PORTA);
            System.out.println("Aguardando conexoes...");

            while (true) {
                Socket socket = servidor.accept();
                System.out.println("Cliente conectado: " + socket.getInetAddress().getHostAddress());
                new ThreadSockets(socket).start();
            }
        }
    }

    private static void inicializarDados() {
        if (!contadores.isEmpty()) {
            return;
        }

        for (GrupoInfo grupo : GRUPOS) {
            contadores.add(new ContadorGrupo(grupo.idGrupo, grupo.tema, 0));
            adicionarAvisosIniciais(grupo);
        }

        System.out.println("Grupos inicializados:");
        for (ContadorGrupo contador : contadores) {
            System.out.println(contador);
        }
    }

    private static String agora() {
        return FORMATADOR.format(new Date());
    }

    private static RespostaMural montarResposta(int idGrupo) {
        synchronized (LOCK) {
            ContadorGrupo grupoSolicitante = contadores.get(idGrupo - 1);
            grupoSolicitante.incrementarUtilizacoes();

            List<ContadorGrupo> ranking = contadores.stream()
                    .map(contador -> new ContadorGrupo(
                            contador.getIdGrupo(),
                            contador.getNomeGrupo(),
                            contador.getQuantidadeUtilizacoes()))
                    .sorted(Comparator.comparingInt(ContadorGrupo::getQuantidadeUtilizacoes)
                            .reversed()
                            .thenComparingInt(ContadorGrupo::getIdGrupo))
                    .collect(Collectors.toList());

            List<AvisoGrupo> avisosGrupo = avisos.stream()
                    .filter(aviso -> aviso.getIdGrupo() == idGrupo)
                    .sorted(Comparator.comparing(AvisoGrupo::getTimestamp).reversed())
                    .limit(8)
                    .map(aviso -> new AvisoGrupo(
                            aviso.getIdGrupo(),
                            aviso.getTitulo(),
                            aviso.getMensagem(),
                            aviso.getTimestamp()))
                    .collect(Collectors.toList());

            System.out.println("Grupo " + grupoSolicitante.getNomeGrupo()
                    + " consultou o mural. Total de acessos: "
                    + grupoSolicitante.getQuantidadeUtilizacoes());

            return new RespostaMural(
                    grupoSolicitante.getIdGrupo(),
                    grupoSolicitante.getNomeGrupo(),
                    agora(),
                    ranking,
                    avisosGrupo
            );
        }
    }

    private static void adicionarAvisosIniciais(GrupoInfo grupo) {
        avisos.add(new AvisoGrupo(
                grupo.idGrupo,
                "Atualizacao de versao",
                "Versao 2.1 do modulo " + grupo.tema
                        + " disponivel para instalacao no sistema.",
                agora()
        ));
        avisos.add(new AvisoGrupo(
                grupo.idGrupo,
                "Atualizacao de cadastro",
                "O cadastro de " + grupo.cadastros[0]
                        + " sera atualizado para adicao de um novo campo no formulario.",
                agora()
        ));
        avisos.add(new AvisoGrupo(
                grupo.idGrupo,
                "Processo em destaque",
                "Ultima implementacao do processo " + grupo.processos[0]
                        + " foi realizada por " + nomesAlunos(grupo.alunos) + ".",
                agora()
        ));

        if (grupo.cadastros.length > 1) {
            avisos.add(new AvisoGrupo(
                    grupo.idGrupo,
                    "Cadastro monitorado",
                    "O cadastro de " + grupo.cadastros[1]
                            + " entrou na fila de revisao desta semana.",
                    agora()
            ));
        }

        if (grupo.processos.length > 1) {
            avisos.add(new AvisoGrupo(
                    grupo.idGrupo,
                    "Processo monitorado",
                    "A equipe registrou nova atividade no processo "
                            + grupo.processos[1] + ".",
                    agora()
            ));
        }
    }

    private static String nomesAlunos(String[] alunos) {
        if (alunos.length == 0) {
            return "";
        }
        if (alunos.length == 1) {
            return alunos[0];
        }
        if (alunos.length == 2) {
            return alunos[0] + " e " + alunos[1];
        }

        StringBuilder nomes = new StringBuilder();
        for (int i = 0; i < alunos.length; i++) {
            if (i > 0) {
                nomes.append(i == alunos.length - 1 ? " e " : ", ");
            }
            nomes.append(alunos[i]);
        }
        return nomes.toString();
    }

    private static class ThreadSockets extends Thread {

        private final Socket clienteSocket;

        public ThreadSockets(Socket clienteSocket) {
            this.clienteSocket = clienteSocket;
        }

        @Override
        public void run() {
            System.out.println("Thread iniciada: " + Thread.currentThread().getName());

            try (
                    Socket socket = clienteSocket;
                    ObjectInputStream entrada = new ObjectInputStream(socket.getInputStream());
                    ObjectOutputStream saida = new ObjectOutputStream(socket.getOutputStream())
            ) {
                Integer idGrupo = (Integer) entrada.readObject();
                System.out.println("Grupo solicitado: " + idGrupo);

                if (idGrupo == null || idGrupo < 1 || idGrupo > GRUPOS.length) {
                    System.out.println("ID de grupo invalido: " + idGrupo);
                    return;
                }

                RespostaMural resposta = montarResposta(idGrupo);
                saida.writeObject(resposta);
                saida.flush();

                System.out.println("Resposta enviada para o grupo " + idGrupo);
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Erro ao atender cliente: " + e.getMessage());
            }
        }
    }

    private static class GrupoInfo {
        private final int idGrupo;
        private final String tema;
        private final String[] alunos;
        private final String[] cadastros;
        private final String[] processos;

        private GrupoInfo(int idGrupo, String tema, String[] alunos,
                String[] cadastros, String[] processos) {
            this.idGrupo = idGrupo;
            this.tema = tema;
            this.alunos = alunos;
            this.cadastros = cadastros;
            this.processos = processos;
        }
    }
}
