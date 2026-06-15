package sockets.thread;

import java.io.Serializable;
import java.util.List;

public class RespostaMural implements Serializable {
    private static final long serialVersionUID = 1L;

    private int idGrupo;
    private String nomeGrupo;
    private String ultimaAtualizacao;
    private List<ContadorGrupo> ranking;
    private List<AvisoGrupo> avisosGrupo;

    public RespostaMural() {
    }

    public RespostaMural(int idGrupo, String nomeGrupo, String ultimaAtualizacao,
            List<ContadorGrupo> ranking, List<AvisoGrupo> avisosGrupo) {
        this.idGrupo = idGrupo;
        this.nomeGrupo = nomeGrupo;
        this.ultimaAtualizacao = ultimaAtualizacao;
        this.ranking = ranking;
        this.avisosGrupo = avisosGrupo;
    }

    public int getIdGrupo() {
        return idGrupo;
    }

    public void setIdGrupo(int idGrupo) {
        this.idGrupo = idGrupo;
    }

    public String getNomeGrupo() {
        return nomeGrupo;
    }

    public void setNomeGrupo(String nomeGrupo) {
        this.nomeGrupo = nomeGrupo;
    }

    public String getUltimaAtualizacao() {
        return ultimaAtualizacao;
    }

    public void setUltimaAtualizacao(String ultimaAtualizacao) {
        this.ultimaAtualizacao = ultimaAtualizacao;
    }

    public List<ContadorGrupo> getRanking() {
        return ranking;
    }

    public void setRanking(List<ContadorGrupo> ranking) {
        this.ranking = ranking;
    }

    public List<AvisoGrupo> getAvisosGrupo() {
        return avisosGrupo;
    }

    public void setAvisosGrupo(List<AvisoGrupo> avisosGrupo) {
        this.avisosGrupo = avisosGrupo;
    }
}
