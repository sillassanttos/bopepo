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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Requisição HTTP enviada por um {@link TransporteHttp}.
 */
public final class RequisicaoHttp {

    private final String metodo;
    private final String url;
    private final Map<String, String> cabecalhos;
    private final String corpo;

    public RequisicaoHttp(String metodo, String url, Map<String, String> cabecalhos, String corpo) {
        this.metodo = Objects.requireNonNull(metodo, "metodo");
        this.url = Objects.requireNonNull(url, "url");
        this.cabecalhos = Collections.unmodifiableMap(new LinkedHashMap<>(cabecalhos));
        this.corpo = corpo;
    }

    public String getMetodo() {
        return metodo;
    }

    public String getUrl() {
        return url;
    }

    public Map<String, String> getCabecalhos() {
        return cabecalhos;
    }

    /** Corpo da requisição, ou {@code null} quando não há. */
    public String getCorpo() {
        return corpo;
    }

    @Override
    public String toString() {
        return metodo + " " + url;
    }
}
