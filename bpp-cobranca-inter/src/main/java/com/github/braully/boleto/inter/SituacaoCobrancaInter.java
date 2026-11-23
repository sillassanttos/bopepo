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

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

/**
 * Situações de uma cobrança ({@code SituacaoCobrancaEnum}).
 */
public enum SituacaoCobrancaInter {

    RECEBIDO,
    A_RECEBER,
    MARCADO_RECEBIDO,
    ATRASADO,
    CANCELADO,
    EXPIRADO,
    FALHA_EMISSAO,
    /** Emissão ainda em andamento: boleto e Pix podem não ter sido gerados. */
    EM_PROCESSAMENTO,
    PROTESTO,

    /**
     * Situação que esta versão da biblioteca ainda não conhece. Evita quebrar
     * a leitura se o banco acrescentar uma situação nova.
     */
    @JsonEnumDefaultValue
    DESCONHECIDA;

    /** Indica se a cobrança já foi paga (pelo banco ou marcada manualmente). */
    public boolean isPaga() {
        return this == RECEBIDO || this == MARCADO_RECEBIDO;
    }

    /** Indica se a cobrança não pode mais ser paga. */
    public boolean isEncerrada() {
        return isPaga() || this == CANCELADO || this == EXPIRADO || this == FALHA_EMISSAO;
    }
}
