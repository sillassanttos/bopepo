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
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Uma página da pesquisa de cobranças ({@code GET /cobrancas}).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaginaCobrancasInter {

    private int totalPaginas;
    private int totalElementos;
    private int tamanhoPagina;
    private boolean primeiraPagina;
    private boolean ultimaPagina;
    private int numeroDeElementos;
    private List<CobrancaInter> cobrancas;

    public int getTotalPaginas() {
        return totalPaginas;
    }

    public int getTotalElementos() {
        return totalElementos;
    }

    public int getTamanhoPagina() {
        return tamanhoPagina;
    }

    public boolean isPrimeiraPagina() {
        return primeiraPagina;
    }

    public boolean isUltimaPagina() {
        return ultimaPagina;
    }

    public int getNumeroDeElementos() {
        return numeroDeElementos;
    }

    public List<CobrancaInter> getCobrancas() {
        return cobrancas == null ? Collections.<CobrancaInter>emptyList() : cobrancas;
    }

    void setTotalPaginas(int totalPaginas) {
        this.totalPaginas = totalPaginas;
    }

    void setTotalElementos(int totalElementos) {
        this.totalElementos = totalElementos;
    }

    void setTamanhoPagina(int tamanhoPagina) {
        this.tamanhoPagina = tamanhoPagina;
    }

    void setPrimeiraPagina(boolean primeiraPagina) {
        this.primeiraPagina = primeiraPagina;
    }

    void setUltimaPagina(boolean ultimaPagina) {
        this.ultimaPagina = ultimaPagina;
    }

    void setNumeroDeElementos(int numeroDeElementos) {
        this.numeroDeElementos = numeroDeElementos;
    }

    void setCobrancas(List<CobrancaInter> cobrancas) {
        this.cobrancas = cobrancas;
    }
}
