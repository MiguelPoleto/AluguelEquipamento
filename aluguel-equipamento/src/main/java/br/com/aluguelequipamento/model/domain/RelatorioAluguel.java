package br.com.aluguelequipamento.model.domain;

import java.math.BigDecimal;

/**
 * DTO utilizado como fonte de dados para o Relatório de Aluguel (JasperReports).
 * Os nomes dos getters devem bater exatamente com os <field name="..."> do JRXML.
 */
public class RelatorioAluguel {

    private Integer    id;
    private String     nomeCliente;
    private String     nomeEquipamento;
    private String     dataRetirada;       // já formatado: dd/MM/yyyy
    private String     dataPrevDevolucao;  // já formatado: dd/MM/yyyy
    private String     dataDevolucao;      // "—" quando ainda não devolvido
    private BigDecimal valorTotal;
    private String     status;

    public RelatorioAluguel() {}

    public RelatorioAluguel(Integer id, String nomeCliente, String nomeEquipamento,
                                 String dataRetirada, String dataPrevDevolucao,
                                 String dataDevolucao, BigDecimal valorTotal, String status) {
        this.id               = id;
        this.nomeCliente      = nomeCliente;
        this.nomeEquipamento  = nomeEquipamento;
        this.dataRetirada     = dataRetirada;
        this.dataPrevDevolucao= dataPrevDevolucao;
        this.dataDevolucao    = dataDevolucao;
        this.valorTotal       = valorTotal;
        this.status           = status;
    }

    public Integer    getId()               { return id; }
    public String     getNomeCliente()      { return nomeCliente; }
    public String     getNomeEquipamento()  { return nomeEquipamento; }
    public String     getDataRetirada()     { return dataRetirada; }
    public String     getDataPrevDevolucao(){ return dataPrevDevolucao; }
    public String     getDataDevolucao()    { return dataDevolucao; }
    public BigDecimal getValorTotal()       { return valorTotal; }
    public String     getStatus()           { return status; }
}
