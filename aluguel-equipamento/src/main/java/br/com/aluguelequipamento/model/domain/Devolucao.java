package br.com.aluguelequipamento.model.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Devolucao {
    private int id;
    private int retiradaId;
    private String descricaoRetirada;
    private LocalDate dataDevolucao;
    private BigDecimal valorMulta;
    private String observacao;
    private String status;

    public Devolucao() {}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getRetiradaId() {
        return retiradaId;
    }

    public void setRetiradaId(int retiradaId) {
        this.retiradaId = retiradaId;
    }

    public String getDescricaoRetirada() {
        return descricaoRetirada;
    }

    public void setDescricaoRetirada(String descricaoRetirada) {
        this.descricaoRetirada = descricaoRetirada;
    }

    public LocalDate getDataDevolucao() {
        return dataDevolucao;
    }

    public void setDataDevolucao(LocalDate dataDevolucao) {
        this.dataDevolucao = dataDevolucao;
    }

    public BigDecimal getValorMulta() {
        return valorMulta;
    }

    public void setValorMulta(BigDecimal valorMulta) {
        this.valorMulta = valorMulta;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    
}
