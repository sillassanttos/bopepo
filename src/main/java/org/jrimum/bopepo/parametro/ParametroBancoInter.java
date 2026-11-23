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
package org.jrimum.bopepo.parametro;

import org.jrimum.bopepo.BancosSuportados;
import org.jrimum.domkee.banco.ParametroBancario;

/**
 * Parâmetros bancários específicos do {@link BancosSuportados#BANCO_INTER}.
 *
 * @since 2.0.2
 */
public enum ParametroBancoInter implements ParametroBancario<ParametroBancoInter> {

    /**
     * Número da operação de cobrança (até 7 dígitos), informado pelo Inter na
     * contratação da cobrança. Não é o número da conta corrente: é o código
     * que o banco usa para identificar o beneficiário no código de barras.
     */
    NUMERO_DA_OPERACAO;
}
