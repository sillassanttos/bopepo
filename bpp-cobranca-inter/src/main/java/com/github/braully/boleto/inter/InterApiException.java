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
 * Erro devolvido pela API do Inter (ou falha de comunicação com ela).
 * <p>
 * Quando o banco responde no formato {@code Problema} da especificação, o
 * título, o detalhe e as violações ({@code violacoes}) ficam disponíveis e
 * entram na mensagem.
 * </p>
 */
public class InterApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int status;
    private final String titulo;
    private final String detalhe;
    private final List<Violacao> violacoes;
    private final String corpo;

    InterApiException(String operacao, int status, String titulo, String detalhe, List<Violacao> violacoes,
            String corpo) {
        super(montarMensagem(operacao, status, titulo, detalhe, violacoes));
        this.status = status;
        this.titulo = titulo;
        this.detalhe = detalhe;
        this.violacoes = violacoes == null ? Collections.<Violacao>emptyList() : violacoes;
        this.corpo = corpo;
    }

    InterApiException(String operacao, Throwable causa) {
        super("Falha de comunicação com o Inter em [" + operacao + "]: " + causa.getMessage(), causa);
        this.status = 0;
        this.titulo = null;
        this.detalhe = null;
        this.violacoes = Collections.emptyList();
        this.corpo = null;
    }

    /** Código HTTP, ou 0 se a falha foi de comunicação. */
    public int getStatus() {
        return status;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDetalhe() {
        return detalhe;
    }

    public List<Violacao> getViolacoes() {
        return violacoes;
    }

    /** Corpo bruto da resposta, para diagnóstico. */
    public String getCorpo() {
        return corpo;
    }

    private static String montarMensagem(String operacao, int status, String titulo, String detalhe,
            List<Violacao> violacoes) {

        StringBuilder sb = new StringBuilder("Inter recusou [").append(operacao).append("] com HTTP ").append(status);
        if (titulo != null) {
            sb.append(": ").append(titulo);
        }
        if (detalhe != null) {
            sb.append(" - ").append(detalhe);
        }
        if (violacoes != null) {
            for (Violacao v : violacoes) {
                sb.append("\n  * ");
                if (v.getPropriedade() != null) {
                    sb.append(v.getPropriedade()).append(": ");
                }
                sb.append(v.getRazao());
                if (v.getValor() != null) {
                    sb.append(" (valor: ").append(v.getValor()).append(')');
                }
            }
        }
        return sb.toString();
    }

    /** Uma violação de regra apontada pelo banco. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Violacao {

        private String razao;
        private String propriedade;
        private String valor;

        public String getRazao() {
            return razao;
        }

        public String getPropriedade() {
            return propriedade;
        }

        public String getValor() {
            return valor;
        }

        void setRazao(String razao) {
            this.razao = razao;
        }

        void setPropriedade(String propriedade) {
            this.propriedade = propriedade;
        }

        void setValor(String valor) {
            this.valor = valor;
        }
    }

    /** Corpo de erro no formato {@code Problema} da especificação. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Problema {

        private Integer status;
        private String title;
        private String detail;
        private List<Violacao> violacoes;

        Integer getStatus() {
            return status;
        }

        String getTitle() {
            return title;
        }

        String getDetail() {
            return detail;
        }

        List<Violacao> getViolacoes() {
            return violacoes;
        }

        void setStatus(Integer status) {
            this.status = status;
        }

        void setTitle(String title) {
            this.title = title;
        }

        void setDetail(String detail) {
            this.detail = detail;
        }

        void setViolacoes(List<Violacao> violacoes) {
            this.violacoes = violacoes;
        }
    }
}
