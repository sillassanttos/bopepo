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

import java.net.URI;

/**
 * Ambientes da API do Banco Inter.
 * <p>
 * Endereços conforme a especificação OpenAPI oficial do portal
 * developers.inter.co (Autenticação OAuth e API Cobrança v3).
 * </p>
 */
public enum AmbienteInter {

    /** Produção: emite boletos reais. */
    PRODUCAO("https://cdpj.partners.bancointer.com.br"),

    /** Sandbox: ambiente de testes do Inter, com credenciais próprias. */
    SANDBOX("https://cdpj-sandbox.partners.uatinter.co");

    private final String base;

    AmbienteInter(String base) {
        this.base = base;
    }

    /** Endereço do token OAuth ({@code POST}). */
    public URI token() {
        return URI.create(base + "/oauth/v2/token");
    }

    /** Endereço base da API Cobrança v3, sem barra no final. */
    public String cobranca() {
        return base + "/cobranca/v3";
    }
}
