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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Filtros da pesquisa de cobranças ({@code GET /cobrancas}).
 * <p>
 * O período ({@code dataInicial} e {@code dataFinal}) é obrigatório. Por
 * padrão, o banco filtra pela data de vencimento.
 * </p>
 */
public class FiltroCobrancasInter {

    /** Campo de data usado no período. */
    public enum FiltrarDataPor {
        VENCIMENTO, EMISSAO, PAGAMENTO
    }

    /** Ordenação da lista. */
    public enum OrdenarPor {
        PESSOA_PAGADORA, TIPO_COBRANCA, CODIGO_COBRANCA, IDENTIFICADOR, DATA_EMISSAO, DATA_VENCIMENTO, VALOR, STATUS
    }

    private final LocalDate dataInicial;
    private final LocalDate dataFinal;
    private FiltrarDataPor filtrarDataPor;
    private SituacaoCobrancaInter situacao;
    private String pessoaPagadora;
    private String cpfCnpjPessoaPagadora;
    private String seuNumero;
    private Integer itensPorPagina;
    private int paginaAtual;
    private OrdenarPor ordenarPor;
    private Boolean decrescente;

    public FiltroCobrancasInter(LocalDate dataInicial, LocalDate dataFinal) {
        this.dataInicial = Objects.requireNonNull(dataInicial, "dataInicial");
        this.dataFinal = Objects.requireNonNull(dataFinal, "dataFinal");
        if (dataFinal.isBefore(dataInicial)) {
            throw new IllegalArgumentException("dataFinal [" + dataFinal + "] antes de dataInicial [" + dataInicial + "].");
        }
    }

    public FiltroCobrancasInter filtrarDataPor(FiltrarDataPor campo) {
        this.filtrarDataPor = campo;
        return this;
    }

    public FiltroCobrancasInter situacao(SituacaoCobrancaInter situacao) {
        if (situacao == SituacaoCobrancaInter.DESCONHECIDA) {
            throw new IllegalArgumentException("Situação DESCONHECIDA não serve como filtro.");
        }
        this.situacao = situacao;
        return this;
    }

    public FiltroCobrancasInter pessoaPagadora(String nome) {
        this.pessoaPagadora = Validacoes.textoOpcional("pessoaPagadora", nome, 100);
        return this;
    }

    /** CPF ou CNPJ do pagador; a máscara é removida. */
    public FiltroCobrancasInter cpfCnpjPessoaPagadora(String cpfCnpj) {
        String digitos = Validacoes.soDigitos(cpfCnpj);
        this.cpfCnpjPessoaPagadora = digitos.isEmpty() ? null : digitos;
        return this;
    }

    public FiltroCobrancasInter seuNumero(String seuNumero) {
        this.seuNumero = Validacoes.textoOpcional("seuNumero", seuNumero, 15);
        return this;
    }

    /** Itens por página (1 a 1000; o padrão do banco é 100). */
    public FiltroCobrancasInter itensPorPagina(int itens) {
        this.itensPorPagina = Validacoes.faixa("itensPorPagina", itens, 1, 1000);
        return this;
    }

    /** Página a buscar, começando em 0. */
    public FiltroCobrancasInter paginaAtual(int pagina) {
        this.paginaAtual = Validacoes.faixa("paginaAtual", pagina, 0, Integer.MAX_VALUE);
        return this;
    }

    public FiltroCobrancasInter ordenarPor(OrdenarPor campo, boolean decrescente) {
        this.ordenarPor = campo;
        this.decrescente = decrescente;
        return this;
    }

    int getPaginaAtual() {
        return paginaAtual;
    }

    /** Monta a query string, já codificada, sem o "?". */
    String paraQueryString() {

        Map<String, String> p = new LinkedHashMap<>();
        p.put("dataInicial", dataInicial.toString());
        p.put("dataFinal", dataFinal.toString());
        if (filtrarDataPor != null) {
            p.put("filtrarDataPor", filtrarDataPor.name());
        }
        if (situacao != null) {
            p.put("situacao", situacao.name());
        }
        if (pessoaPagadora != null) {
            p.put("pessoaPagadora", pessoaPagadora);
        }
        if (cpfCnpjPessoaPagadora != null) {
            p.put("cpfCnpjPessoaPagadora", cpfCnpjPessoaPagadora);
        }
        if (seuNumero != null) {
            p.put("seuNumero", seuNumero);
        }
        if (itensPorPagina != null) {
            p.put("paginacao.itensPorPagina", itensPorPagina.toString());
        }
        p.put("paginacao.paginaAtual", Integer.toString(paginaAtual));
        if (ordenarPor != null) {
            p.put("ordenarPor", ordenarPor.name());
            p.put("tipoOrdenacao", Boolean.TRUE.equals(decrescente) ? "DESC" : "ASC");
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : p.entrySet()) {
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)).append('=')
                    .append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }
}
