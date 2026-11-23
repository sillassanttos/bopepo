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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Teste das regras do nosso número do Banco Inter.
 */
public class TestNossoNumeroBancoInter {

	@Test
	public void seCalculaODigitoDoBoletoReal() {
		// Boleto emitido pelo Inter: 0001/112/9002194200-1
		assertEquals(1, NossoNumeroBancoInter.calcularDigito(1, 112, "9002194200"));
	}

	@Test
	public void seCompletaComZerosAEsquerda() {
		assertEquals(NossoNumeroBancoInter.calcularDigito(1, 112, "0000000123"),
				NossoNumeroBancoInter.calcularDigito(1, 112, "123"));
	}

	@Test
	public void seDigitoZeroQuandoASomaEMultiploDe10() {
		// Agência 0001 e carteira 112 contribuem com 8 na soma (pesos 2 e 1 da direita para a esquerda).
		// Nosso número 0: soma 8, resto 8, dígito 10 - 8 = 2
		assertEquals(2, NossoNumeroBancoInter.calcularDigito(1, 112, "0"));
		// Nosso número 2: soma 8 + 4 = 12, resto 2, dígito 8
		assertEquals(8, NossoNumeroBancoInter.calcularDigito(1, 112, "2"));
		// Nosso número 1: soma 8 + 2 = 10, resto 0, dígito 0
		assertEquals(0, NossoNumeroBancoInter.calcularDigito(1, 112, "1"));
	}

	@Test
	public void seConfereNossoNumeroCom11Posicoes() {
		assertTrue(NossoNumeroBancoInter.isValido(1, 112, "90021942001"));
		assertFalse(NossoNumeroBancoInter.isValido(1, 112, "90021942009"));
		assertFalse(NossoNumeroBancoInter.isValido(1, 112, "9002194200"));
		assertFalse(NossoNumeroBancoInter.isValido(1, 112, "9002194200A"));
		assertFalse(NossoNumeroBancoInter.isValido(1, 112, null));
	}

	@Test(expected = IllegalArgumentException.class)
	public void seNaoPermiteNossoNumeroComMaisDe10Digitos() {
		NossoNumeroBancoInter.calcularDigito(1, 112, "12345678901");
	}

	@Test(expected = IllegalArgumentException.class)
	public void seNaoPermiteNossoNumeroNaoNumerico() {
		NossoNumeroBancoInter.calcularDigito(1, 112, "12345A");
	}

	@Test(expected = IllegalArgumentException.class)
	public void seNaoPermiteCarteiraComMaisDe3Digitos() {
		NossoNumeroBancoInter.calcularDigito(1, 1120, "1");
	}
}
