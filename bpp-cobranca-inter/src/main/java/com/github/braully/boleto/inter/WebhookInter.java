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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Webhook cadastrado para receber as notificações de cobrança.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebhookInter {

    private String webhookUrl;
    private String criacao;
    private String atualizacao;

    public String getWebhookUrl() {
        return webhookUrl;
    }

    /** Data e hora do cadastro (RFC 3339). */
    public String getCriacao() {
        return criacao;
    }

    /** Data e hora da última alteração (RFC 3339), se houver. */
    public String getAtualizacao() {
        return atualizacao;
    }

    void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    void setCriacao(String criacao) {
        this.criacao = criacao;
    }

    void setAtualizacao(String atualizacao) {
        this.atualizacao = atualizacao;
    }
}
