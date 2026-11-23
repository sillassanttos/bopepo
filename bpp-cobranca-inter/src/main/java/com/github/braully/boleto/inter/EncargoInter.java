/*
 * Copyright 2026 JRimum Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package com.github.braully.boleto.inter;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Multa, mora ou desconto de uma cobrança do Inter.
 * <p>
 * Use as fábricas, que já trazem os códigos da API: {@link #multaPercentual},
 * {@link #multaValorFixo}, {@link #moraTaxaMensal}, {@link #moraValorDia},
 * {@link #descontoPercentual} e {@link #descontoValorFixo}. Valores e taxas são
 * arredondados para 2 casas, como o banco faz.
 * </p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EncargoInter {

    private String codigo;
    private BigDecimal taxa;
    private BigDecimal valor;
    private Integer quantidadeDias;

    public EncargoInter() {
    }

    private EncargoInter(String codigo, BigDecimal taxa, BigDecimal valor, Integer quantidadeDias) {
        this.codigo = codigo;
        this.taxa = taxa;
        this.valor = valor;
        this.quantidadeDias = quantidadeDias;
    }

    /** Multa percentual sobre o valor do título (ex.: 2 = 2%). */
    public static EncargoInter multaPercentual(BigDecimal taxa) {
        return new EncargoInter("PERCENTUAL", positivo("multa.taxa", taxa), null, null);
    }

    /** Multa de valor fixo, em reais. */
    public static EncargoInter multaValorFixo(BigDecimal valor) {
        return new EncargoInter("VALORFIXO", null, positivo("multa.valor", valor), null);
    }

    /** Mora (juros) percentual ao mês (ex.: 1 = 1% a.m.). */
    public static EncargoInter moraTaxaMensal(BigDecimal taxa) {
        return new EncargoInter("TAXAMENSAL", positivo("mora.taxa", taxa), null, null);
    }

    /** Mora (juros) em reais por dia de atraso. */
    public static EncargoInter moraValorDia(BigDecimal valor) {
        return new EncargoInter("VALORDIA", null, positivo("mora.valor", valor), null);
    }

    /**
     * Desconto percentual para pagamento até {@code diasAntesDoVencimento}
     * dias antes do vencimento.
     */
    public static EncargoInter descontoPercentual(BigDecimal taxa, int diasAntesDoVencimento) {
        return new EncargoInter("PERCENTUALDATAINFORMADA", positivo("desconto.taxa", taxa), null,
                Validacoes.faixa("desconto.quantidadeDias", diasAntesDoVencimento, 0, 9999));
    }

    /**
     * Desconto em reais para pagamento até {@code diasAntesDoVencimento} dias
     * antes do vencimento.
     */
    public static EncargoInter descontoValorFixo(BigDecimal valor, int diasAntesDoVencimento) {
        return new EncargoInter("VALORFIXODATAINFORMADA", null, positivo("desconto.valor", valor),
                Validacoes.faixa("desconto.quantidadeDias", diasAntesDoVencimento, 0, 9999));
    }

    private static BigDecimal positivo(String campo, BigDecimal valor) {
        BigDecimal v = Validacoes.valor(campo, valor, null, null);
        if (v.signum() <= 0) {
            throw new IllegalArgumentException("Campo [" + campo + "] deve ser maior que zero.");
        }
        return v;
    }

    public String getCodigo() {
        return codigo;
    }

    public BigDecimal getTaxa() {
        return taxa;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public Integer getQuantidadeDias() {
        return quantidadeDias;
    }

    void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    void setTaxa(BigDecimal taxa) {
        this.taxa = taxa;
    }

    void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    void setQuantidadeDias(Integer quantidadeDias) {
        this.quantidadeDias = quantidadeDias;
    }
}
