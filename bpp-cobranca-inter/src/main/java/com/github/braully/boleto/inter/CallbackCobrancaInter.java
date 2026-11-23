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

/**
 * Notificação que o Inter envia ao webhook cadastrado quando uma cobrança muda
 * de situação (emitida, paga, cancelada etc.).
 * <p>
 * O corpo da notificação é uma lista; leia com
 * {@link ClienteInterCobranca#lerCallbacks(String)}.
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CallbackCobrancaInter {

    private String codigoSolicitacao;
    private String seuNumero;
    private SituacaoCobrancaInter situacao;
    private String dataHoraSituacao;
    private BigDecimal valorTotalRecebido;
    private String origemRecebimento;
    private String nossoNumero;
    private String codigoBarras;
    private String linhaDigitavel;
    private String txid;
    private String pixCopiaECola;

    public String getCodigoSolicitacao() {
        return codigoSolicitacao;
    }

    public String getSeuNumero() {
        return seuNumero;
    }

    public SituacaoCobrancaInter getSituacao() {
        return situacao;
    }

    /** Data e hora da situação, como o banco envia (RFC 3339). */
    public String getDataHoraSituacao() {
        return dataHoraSituacao;
    }

    public BigDecimal getValorTotalRecebido() {
        return valorTotalRecebido;
    }

    public String getOrigemRecebimento() {
        return origemRecebimento;
    }

    public String getNossoNumero() {
        return nossoNumero;
    }

    public String getCodigoBarras() {
        return codigoBarras;
    }

    public String getLinhaDigitavel() {
        return linhaDigitavel;
    }

    public String getTxid() {
        return txid;
    }

    public String getPixCopiaECola() {
        return pixCopiaECola;
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

    void setDataHoraSituacao(String dataHoraSituacao) {
        this.dataHoraSituacao = dataHoraSituacao;
    }

    void setValorTotalRecebido(BigDecimal valorTotalRecebido) {
        this.valorTotalRecebido = valorTotalRecebido;
    }

    void setOrigemRecebimento(String origemRecebimento) {
        this.origemRecebimento = origemRecebimento;
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

    void setTxid(String txid) {
        this.txid = txid;
    }

    void setPixCopiaECola(String pixCopiaECola) {
        this.pixCopiaECola = pixCopiaECola;
    }
}
