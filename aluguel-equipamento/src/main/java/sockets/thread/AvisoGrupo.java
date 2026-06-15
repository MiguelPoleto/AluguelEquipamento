package sockets.thread;

import java.io.Serializable;

public class AvisoGrupo implements Serializable {
    private static final long serialVersionUID = 1L;

    private int idGrupo;
    private String titulo;
    private String mensagem;
    private String timestamp;

    public AvisoGrupo() {
    }

    public AvisoGrupo(int idGrupo, String titulo, String mensagem, String timestamp) {
        this.idGrupo = idGrupo;
        this.titulo = titulo;
        this.mensagem = mensagem;
        this.timestamp = timestamp;
    }

    public int getIdGrupo() {
        return idGrupo;
    }

    public void setIdGrupo(int idGrupo) {
        this.idGrupo = idGrupo;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "AvisoGrupo{"
                + "idGrupo=" + idGrupo
                + ", titulo='" + titulo + '\''
                + ", mensagem='" + mensagem + '\''
                + ", timestamp='" + timestamp + '\''
                + '}';
    }
}
