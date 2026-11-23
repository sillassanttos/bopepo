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
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Cobrança devolvida pela API: dados gerais, boleto e Pix.
 * <p>
 * Serve tanto para o detalhe ({@code GET /cobrancas/{codigoSolicitacao}})
 * quanto para cada item da lista ({@code GET /cobrancas}). Enquanto a cobrança
 * está {@link SituacaoCobrancaInter#EM_PROCESSAMENTO}, boleto e Pix podem vir
 * vazios.
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CobrancaInter {

    private Dados cobranca;
    private Boleto boleto;
    private Pix pix;

    public Dados getCobranca() {
        return cobranca;
    }

    /** Dados do boleto, ou {@code null} se ainda não foram gerados. */
    public Boleto getBoleto() {
        return boleto;
    }

    /** Dados do Pix, ou {@code null} se não houver Pix (por exemplo, sem chave cadastrada). */
    public Pix getPix() {
        return pix;
    }

    @JsonIgnore
    public String getCodigoSolicitacao() {
        return cobranca == null ? null : cobranca.getCodigoSolicitacao();
    }

    @JsonIgnore
    public SituacaoCobrancaInter getSituacao() {
        return cobranca == null ? null : cobranca.getSituacao();
    }

    /** Indica se o boleto já tem linha digitável e código de barras. */
    @JsonIgnore
    public boolean isBoletoDisponivel() {
        return boleto != null && boleto.getCodigoBarras() != null && boleto.getLinhaDigitavel() != null;
    }

    void setCobranca(Dados cobranca) {
        this.cobranca = cobranca;
    }

    void setBoleto(Boleto boleto) {
        this.boleto = boleto;
    }

    void setPix(Pix pix) {
        this.pix = pix;
    }

    /** Dados gerais da cobrança. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Dados {

        private String codigoSolicitacao;
        private String seuNumero;
        private SituacaoCobrancaInter situacao;
        private String dataSituacao;
        private String dataEmissao;
        private String dataVencimento;
        private BigDecimal valorNominal;
        private BigDecimal valorTotalRecebido;
        private String origemRecebimento;
        private String tipoCobranca;
        private String motivoCancelamento;
        private Boolean arquivada;
        private List<EncargoInter> descontos;
        private EncargoInter multa;
        private EncargoInter mora;
        private PessoaInter pagador;

        public String getCodigoSolicitacao() {
            return codigoSolicitacao;
        }

        public String getSeuNumero() {
            return seuNumero;
        }

        public SituacaoCobrancaInter getSituacao() {
            return situacao;
        }

        public LocalDate getDataSituacao() {
            return data(dataSituacao);
        }

        public LocalDate getDataEmissao() {
            return data(dataEmissao);
        }

        public LocalDate getDataVencimento() {
            return data(dataVencimento);
        }

        public BigDecimal getValorNominal() {
            return valorNominal;
        }

        public BigDecimal getValorTotalRecebido() {
            return valorTotalRecebido;
        }

        /** {@code BOLETO} ou {@code PIX}, quando paga. */
        public String getOrigemRecebimento() {
            return origemRecebimento;
        }

        /** {@code SIMPLES}, {@code PARCELADO} ou {@code RECORRENTE}. */
        public String getTipoCobranca() {
            return tipoCobranca;
        }

        public String getMotivoCancelamento() {
            return motivoCancelamento;
        }

        public Boolean getArquivada() {
            return arquivada;
        }

        public List<EncargoInter> getDescontos() {
            return descontos == null ? Collections.<EncargoInter>emptyList() : descontos;
        }

        public EncargoInter getMulta() {
            return multa;
        }

        public EncargoInter getMora() {
            return mora;
        }

        public PessoaInter getPagador() {
            return pagador;
        }

        void setCodigoSolicitacao(String codigoSolicitacao) {
            this.codigoSolicitacao = codigoSolicitacao;
        }

        void setSeuNumero(String seuNumero) {
            this.seuNumero = seuNumero;
        }

        void setSituacao(SituacaoCobrancaInter situacao) {
            this.situacao = situacao;
        }

        void setDataSituacao(String dataSituacao) {
            this.dataSituacao = dataSituacao;
        }

        void setDataEmissao(String dataEmissao) {
            this.dataEmissao = dataEmissao;
        }

        void setDataVencimento(String dataVencimento) {
            this.dataVencimento = dataVencimento;
        }

        void setValorNominal(BigDecimal valorNominal) {
            this.valorNominal = valorNominal;
        }

        void setValorTotalRecebido(BigDecimal valorTotalRecebido) {
            this.valorTotalRecebido = valorTotalRecebido;
        }

        void setOrigemRecebimento(String origemRecebimento) {
            this.origemRecebimento = origemRecebimento;
        }

        void setTipoCobranca(String tipoCobranca) {
            this.tipoCobranca = tipoCobranca;
        }

        void setMotivoCancelamento(String motivoCancelamento) {
            this.motivoCancelamento = motivoCancelamento;
        }

        void setArquivada(Boolean arquivada) {
            this.arquivada = arquivada;
        }

        void setDescontos(List<EncargoInter> descontos) {
            this.descontos = descontos;
        }

        void setMulta(EncargoInter multa) {
            this.multa = multa;
        }

        void setMora(EncargoInter mora) {
            this.mora = mora;
        }

        void setPagador(PessoaInter pagador) {
            this.pagador = pagador;
        }
    }

    /** Dados do boleto (código de barras e linha digitável sem formatação). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Boleto {

        private String nossoNumero;
        private String codigoBarras;
        private String linhaDigitavel;

        /** Nosso número com o dígito verificador (11 posições). */
        public String getNossoNumero() {
            return nossoNumero;
        }

        /** Código de barras com 44 dígitos. */
        public String getCodigoBarras() {
            return codigoBarras;
        }

        /** Linha digitável com 47 dígitos, sem pontos nem espaços. */
        public String getLinhaDigitavel() {
            return linhaDigitavel;
        }

        void setNossoNumero(String nossoNumero) {
            this.nossoNumero = nossoNumero;
        }

        void setCodigoBarras(String codigoBarras) {
            this.codigoBarras = codigoBarras;
        }

        void setLinhaDigitavel(String linhaDigitavel) {
            this.linhaDigitavel = linhaDigitavel;
        }
    }

    /** Dados do Pix da cobrança. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Pix {

        private String txid;
        private String pixCopiaECola;

        public String getTxid() {
            return txid;
        }

        /** Texto do QR Code (BR Code), o "Pix copia e cola". */
        public String getPixCopiaECola() {
            return pixCopiaECola;
        }

        void setTxid(String txid) {
            this.txid = txid;
        }

        void setPixCopiaECola(String pixCopiaECola) {
            this.pixCopiaECola = pixCopiaECola;
        }
    }

    static LocalDate data(String valor) {
        if (valor == null || valor.length() < 10) {
            return null;
        }
        return LocalDate.parse(valor.substring(0, 10));
    }
}
