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
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Validações locais com os limites da especificação da API, para falhar antes
 * de chamar o banco e com mensagem clara.
 */
final class Validacoes {

    private static final Set<String> UFS = new HashSet<>(Arrays.asList("AC", "AL", "AP", "AM", "BA", "CE", "DF",
            "ES", "GO", "MA", "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC",
            "SP", "SE", "TO"));

    private Validacoes() {
    }

    static String soDigitos(String valor) {
        if (valor == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(valor.length());
        for (int i = 0; i < valor.length(); i++) {
            char c = valor.charAt(i);
            if (c >= '0' && c <= '9') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    static String texto(String campo, String valor, int minimo, int maximo) {
        String t = valor == null ? "" : valor.trim();
        if (t.length() < minimo || t.length() > maximo) {
            throw new IllegalArgumentException(
                    "Campo [" + campo + "] deve ter de " + minimo + " a " + maximo + " caracteres: [" + valor + "].");
        }
        return t;
    }

    static String textoOpcional(String campo, String valor, int maximo) {
        if (valor == null || valor.trim().isEmpty()) {
            return null;
        }
        return texto(campo, valor, 1, maximo);
    }

    static String uf(String valor) {
        String uf = valor == null ? "" : valor.trim().toUpperCase(Locale.ROOT);
        if (!UFS.contains(uf)) {
            throw new IllegalArgumentException("UF inválida: [" + valor + "].");
        }
        return uf;
    }

    static String cep(String valor) {
        String cep = soDigitos(valor);
        if (cep.length() != 8) {
            throw new IllegalArgumentException("CEP [" + valor + "] deve ter 8 dígitos.");
        }
        return cep;
    }

    static void exigir(Object valor, String campo) {
        if (valor == null) {
            throw new IllegalArgumentException("Campo obrigatório não informado: [" + campo + "].");
        }
    }

    /** Arredonda para 2 casas, como o banco faz, e confere a faixa. */
    static BigDecimal valor(String campo, BigDecimal valor, BigDecimal minimo, BigDecimal maximo) {
        exigir(valor, campo);
        BigDecimal v = valor.setScale(2, RoundingMode.HALF_UP);
        if ((minimo != null && v.compareTo(minimo) < 0) || (maximo != null && v.compareTo(maximo) > 0)) {
            throw new IllegalArgumentException("Campo [" + campo + "] = " + v.toPlainString() + " fora da faixa "
                    + (minimo == null ? "" : minimo.toPlainString()) + " a "
                    + (maximo == null ? "" : maximo.toPlainString()) + ".");
        }
        return v;
    }

    static int faixa(String campo, int valor, int minimo, int maximo) {
        if (valor < minimo || valor > maximo) {
            throw new IllegalArgumentException(
                    "Campo [" + campo + "] = " + valor + " fora da faixa " + minimo + " a " + maximo + ".");
        }
        return valor;
    }

    /**
     * Confere um identificador que vai no caminho da URL (por exemplo, o
     * {@code codigoSolicitacao}), para não montar uma URL com caracteres
     * inesperados.
     */
    static String identificador(String campo, String valor) {
        if (valor == null || !valor.matches("[0-9A-Za-z-]{1,64}")) {
            throw new IllegalArgumentException("Campo [" + campo + "] inválido: [" + valor + "].");
        }
        return valor;
    }
}
