package br.com.aluguelequipamento.model.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/*
    Miguel
*/
public class Equipamento {
    private int id;
    private String nome;
    private String descricao;
    private String categoria;
    private BigDecimal valorDiaria;
    private String status;
    private LocalDate dataCadastro;

    public Equipamento() {}

    public Equipamento(int id, String nome, String descricao, String categoria,
                       BigDecimal valorDiaria, String status, LocalDate dataCadastro) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.categoria = categoria;
        this.valorDiaria = valorDiaria;
        this.status = status;
        this.dataCadastro = dataCadastro;
    }

    @Override
    public String toString() {
        return nome + " [" + status + "]";
    }

    public int getId() { 
        return id; 
    }
    public void setId(int id) { 
        this.id = id; 
    }
    public String getNome() {
         return nome; 
    }
    public void setNome(String nome) { 
        this.nome = nome; 
    }
    public String getDescricao() { 
        return descricao; 
    }
    public void setDescricao(String descricao) {
        this.descricao = descricao; 
    }
    public String getCategoria() {
        return categoria; 
    }
    public void setCategoria(String categoria) { 
        this.categoria = categoria; 
    }
    public BigDecimal getValorDiaria() { 
        return valorDiaria; 
    }
    public void setValorDiaria(BigDecimal valorDiaria) { 
        this.valorDiaria = valorDiaria; 
    }
    public String getStatus() { 
        return status; 
    }
    public void setStatus(String status) { 
        this.status = status; 
    }
    public LocalDate getDataCadastro() { 
        return dataCadastro; 
    }
    public void setDataCadastro(LocalDate dataCadastro) { 
        this.dataCadastro = dataCadastro; 
    }
}

