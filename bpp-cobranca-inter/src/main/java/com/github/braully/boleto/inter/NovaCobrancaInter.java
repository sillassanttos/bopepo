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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Pedido de emissão de cobrança (boleto com Pix) na API Cobrança v3 do Inter.
 * <p>
 * Os limites da especificação são conferidos aqui, antes da chamada ao banco:
 * seu número com até 15 caracteres, valor de 2,50 a 99.999.999,99, até 60 dias
 * de agenda, 5 linhas de mensagem com até 78 caracteres e pagador completo.
 * </p>
 *
 * <pre>
 * NovaCobrancaInter cobranca = new NovaCobrancaInter("PED-1001", new BigDecimal("150.00"), LocalDate.of(2026, 12, 15))
 *         .pagador(PessoaInter.de("111.444.777-35", "Pagador de Demonstração")
 *                 .endereco("Rua Exemplo", "100", "Centro", "Belo Horizonte", "MG", "30110-000"))
 *         .multa(EncargoInter.multaPercentual(new BigDecimal("2")))
 *         .mora(EncargoInter.moraTaxaMensal(new BigDecimal("1")));
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "seuNumero", "valorNominal", "dataVencimento", "numDiasAgenda", "pagador", "desconto", "multa",
        "mora", "mensagem", "beneficiarioFinal", "formasRecebimento" })
public class NovaCobrancaInter {

    static final BigDecimal VALOR_MINIMO = new BigDecimal("2.50");
    static final BigDecimal VALOR_MAXIMO = new BigDecimal("99999999.99");

    private final String seuNumero;
    private final BigDecimal valorNominal;
    private final LocalDate dataVencimento;
    private int numDiasAgenda;
    private PessoaInter pagador;
    private EncargoInter desconto;
    private EncargoInter multa;
    private EncargoInter mora;
    private Map<String, String> mensagem;
    private PessoaInter beneficiarioFinal;
    private List<String> formasRecebimento;

    /**
     * @param seuNumero código da cobrança no seu sistema (até 15 caracteres)
     * @param valorNominal valor do título, de 2,50 a 99.999.999,99
     * @param dataVencimento data de vencimento
     */
    public NovaCobrancaInter(String seuNumero, BigDecimal valorNominal, LocalDate dataVencimento) {
        this.seuNumero = Validacoes.texto("seuNumero", seuNumero, 1, 15);
        this.valorNominal = Validacoes.valor("valorNominal", valorNominal, VALOR_MINIMO, VALOR_MAXIMO);
        Validacoes.exigir(dataVencimento, "dataVencimento");
        this.dataVencimento = dataVencimento;
    }

    /**
     * Dias corridos depois do vencimento até a baixa automática (0 a 60).
     */
    public NovaCobrancaInter numDiasAgenda(int dias) {
        this.numDiasAgenda = Validacoes.faixa("numDiasAgenda", dias, 0, 60);
        return this;
    }

    public NovaCobrancaInter pagador(PessoaInter pagador) {
        this.pagador = pagador;
        return this;
    }

    public NovaCobrancaInter beneficiarioFinal(PessoaInter beneficiarioFinal) {
        this.beneficiarioFinal = beneficiarioFinal;
        return this;
    }

    public NovaCobrancaInter desconto(EncargoInter desconto) {
        this.desconto = exigirCodigo(desconto, "desconto", "PERCENTUALDATAINFORMADA", "VALORFIXODATAINFORMADA");
        return this;
    }

    public NovaCobrancaInter multa(EncargoInter multa) {
        this.multa = exigirCodigo(multa, "multa", "PERCENTUAL", "VALORFIXO");
        return this;
    }

    public NovaCobrancaInter mora(EncargoInter mora) {
        this.mora = exigirCodigo(mora, "mora", "TAXAMENSAL", "VALORDIA");
        return this;
    }

    /**
     * Mensagem impressa no boleto: até 5 linhas de até 78 caracteres.
     */
    public NovaCobrancaInter mensagem(String... linhas) {
        if (linhas == null || linhas.length == 0) {
            this.mensagem = null;
            return this;
        }
        if (linhas.length > 5) {
            throw new IllegalArgumentException("A mensagem aceita no máximo 5 linhas; recebidas " + linhas.length + ".");
        }
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < linhas.length; i++) {
            String linha = Validacoes.textoOpcional("mensagem.linha" + (i + 1), linhas[i], 78);
            if (linha != null) {
                m.put("linha" + (i + 1), linha);
            }
        }
        this.mensagem = m.isEmpty() ? null : m;
        return this;
    }

    /**
     * Formas de recebimento. Sem esta chamada, o Inter emite boleto e, se houver
     * chave Pix cadastrada, também o Pix. Pela especificação, não é possível
     * pedir só Pix nesta API.
     */
    public NovaCobrancaInter formasRecebimento(FormaRecebimentoInter... formas) {
        List<String> lista = new ArrayList<>();
        for (FormaRecebimentoInter f : Arrays.asList(formas)) {
            if (!lista.contains(f.name())) {
                lista.add(f.name());
            }
        }
        if (lista.size() == 1 && lista.contains(FormaRecebimentoInter.PIX.name())) {
            throw new IllegalArgumentException(
                    "A API Cobrança não aceita só PIX como forma de recebimento; use a API Pix (cobv) para isso.");
        }
        this.formasRecebimento = lista.isEmpty() ? null : lista;
        return this;
    }

    /** Confere os obrigatórios que dependem de mais de um campo. */
    void validar() {
        Validacoes.exigir(pagador, "pagador");
        pagador.validarObrigatorios("pagador");
        if (beneficiarioFinal != null) {
            beneficiarioFinal.validarObrigatorios("beneficiarioFinal");
        }
    }

    private static EncargoInter exigirCodigo(EncargoInter encargo, String campo, String... codigos) {
        if (encargo == null) {
            return null;
        }
        if (!Arrays.asList(codigos).contains(encargo.getCodigo())) {
            throw new IllegalArgumentException("Código [" + encargo.getCodigo() + "] não serve para " + campo
                    + "; use um de " + Arrays.toString(codigos) + ".");
        }
        return encargo;
    }

    public String getSeuNumero() {
        return seuNumero;
    }

    public BigDecimal getValorNominal() {
        return valorNominal;
    }

    @JsonIgnore
    public LocalDate getDataVencimento() {
        return dataVencimento;
    }

    @JsonProperty("dataVencimento")
    String getDataVencimentoIso() {
        return dataVencimento.toString();
    }

    public int getNumDiasAgenda() {
        return numDiasAgenda;
    }

    public PessoaInter getPagador() {
        return pagador;
    }

    public EncargoInter getDesconto() {
        return desconto;
    }

    public EncargoInter getMulta() {
        return multa;
    }

    public EncargoInter getMora() {
        return mora;
    }

    public Map<String, String> getMensagem() {
        return mensagem;
    }

    public PessoaInter getBeneficiarioFinal() {
        return beneficiarioFinal;
    }

    public List<String> getFormasRecebimento() {
        return formasRecebimento;
    }
}
