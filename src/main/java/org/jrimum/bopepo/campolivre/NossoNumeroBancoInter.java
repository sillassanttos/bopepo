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
package org.jrimum.bopepo.campolivre;

import org.jrimum.vallia.Modulo;

/**
 * Regras do nosso número do Banco Inter (077).
 * <p>
 * O nosso número tem 10 dígitos e um dígito verificador calculado em
 * <strong>módulo 10</strong> (pesos 2 e 1, da direita para a esquerda) sobre
 * agência (4) + carteira (3) + nosso número (10). Na carteira 112 o Inter
 * devolve o nosso número já com o dígito (11 posições, por exemplo
 * {@code 90021942001}); {@link #isValido(int, int, String)} confere esse valor.
 * </p>
 *
 * @since 2.0.2
 */
public final class NossoNumeroBancoInter {

    /** Tamanho do nosso número sem o dígito verificador. */
    public static final int TAMANHO = 10;

    private NossoNumeroBancoInter() {
    }

    /**
     * Calcula o dígito verificador do nosso número.
     *
     * @param agencia código da agência, sem dígito (no Inter, 1)
     * @param carteira código da carteira (no Inter, normalmente 112)
     * @param nossoNumero nosso número com até 10 dígitos, sem o dígito
     * @return dígito verificador, de 0 a 9
     * @throws IllegalArgumentException se algum valor estiver fora do formato
     */
    public static int calcularDigito(int agencia, int carteira, String nossoNumero) {

        if (agencia < 0 || agencia > 9999) {
            throw new IllegalArgumentException("Agência [" + agencia + "] deve ter até 4 dígitos.");
        }
        if (carteira < 0 || carteira > 999) {
            throw new IllegalArgumentException("Carteira [" + carteira + "] deve ter até 3 dígitos.");
        }
        if (nossoNumero == null || nossoNumero.isEmpty() || nossoNumero.length() > TAMANHO
                || !soDigitos(nossoNumero)) {
            throw new IllegalArgumentException("Nosso número [" + nossoNumero
                    + "] deve ter até " + TAMANHO + " dígitos numéricos.");
        }

        String base = String.format("%04d%03d%0" + TAMANHO + "d", agencia, carteira, Long.parseLong(nossoNumero));
        int resto = Modulo.calculeMod10(base, 1, 2);

        return resto == 0 ? 0 : 10 - resto;
    }

    /**
     * Confere um nosso número de 11 posições (10 dígitos + dígito verificador),
     * como o Inter devolve na API e imprime no boleto.
     *
     * @param agencia código da agência, sem dígito
     * @param carteira código da carteira
     * @param nossoNumeroComDigito 11 dígitos numéricos
     * @return {@code true} se o dígito confere
     */
    public static boolean isValido(int agencia, int carteira, String nossoNumeroComDigito) {

        if (nossoNumeroComDigito == null || nossoNumeroComDigito.length() != TAMANHO + 1
                || !soDigitos(nossoNumeroComDigito)) {
            return false;
        }

        int digito = nossoNumeroComDigito.charAt(TAMANHO) - '0';

        return digito == calcularDigito(agencia, carteira, nossoNumeroComDigito.substring(0, TAMANHO));
    }

    private static boolean soDigitos(String valor) {
        return valor.chars().allMatch(c -> c >= '0' && c <= '9');
    }
}
