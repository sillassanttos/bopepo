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
package org.jrimum.bopepo;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;

import com.github.braully.boleto.BoletoCobranca;

import org.jrimum.bopepo.parametro.ParametroBancoInter;
import org.jrimum.domkee.banco.Agencia;
import org.jrimum.domkee.banco.Carteira;
import org.jrimum.domkee.banco.Cedente;
import org.jrimum.domkee.banco.ContaBancaria;
import org.jrimum.domkee.banco.NumeroDaConta;
import org.jrimum.domkee.banco.ParametrosBancariosMap;
import org.jrimum.domkee.banco.Sacado;
import org.jrimum.domkee.banco.TipoDeMoeda;
import org.jrimum.domkee.banco.Titulo;
import org.jrimum.utilix.DateFormat;
import org.junit.Before;
import org.junit.Test;

/**
 * Confere o boleto do Banco Inter (077) de ponta a ponta contra um boleto
 * emitido pelo próprio banco.
 * <p>
 * Referência: linha digitável publicada no fórum do Projeto ACBr (tópico
 * 79070, agosto de 2024), de um boleto de R$ 270,00 com vencimento em
 * 07/08/2024 (fator 9801), agência 0001, carteira 112, operação 0921255 e
 * nosso número 9002194200-1. Os nomes abaixo são de demonstração.
 * </p>
 */
public class TestBoletoBancoInter {

	private static final String LINHA_DO_BANCO = "07790001161209212559500219420015198010000027000";

	private Titulo titulo;

	@Before
	public void setUp() {

		ContaBancaria conta = new ContaBancaria(BancosSuportados.BANCO_INTER.create());
		conta.setAgencia(new Agencia(1, "9"));
		conta.setCarteira(new Carteira(112));
		conta.setNumeroDaConta(new NumeroDaConta(123456789, "0"));

		titulo = new Titulo(conta, new Sacado("Pagador de Demonstração"), new Cedente("Beneficiário de Demonstração"));
		titulo.setParametrosBancarios(new ParametrosBancariosMap(ParametroBancoInter.NUMERO_DA_OPERACAO, 921255));
		titulo.setNossoNumero("9002194200");
		titulo.setTipoDeMoeda(TipoDeMoeda.REAL);
		titulo.setValor(new BigDecimal("270.00"));
		titulo.setDataDoVencimento(DateFormat.DDMMYYYY_B.parse("07/08/2024"));
	}

	@Test
	public void seLinhaDigitavelIgualADoBanco() {

		Boleto boleto = new Boleto(titulo);

		assertEquals("07790.00116 12092.125595 00219.420015 1 98010000027000", boleto.getLinhaDigitavel().write());
		assertEquals(LINHA_DO_BANCO, boleto.getLinhaDigitavel().write().replaceAll("[ .]", ""));
	}

	@Test
	public void seCodigoDeBarrasIgualAoDoBanco() {

		Boleto boleto = new Boleto(titulo);

		// Código de barras: banco/moeda (4) + DV geral (1) + fator (4) + valor (10) + campo livre (25)
		assertEquals("07791980100000270000001112092125590021942001", boleto.getCodigoDeBarras().write());
	}

	@Test
	public void seNossoNumeroSaiComODigitoCalculado() {

		new Boleto(titulo);

		assertEquals("1", titulo.getDigitoDoNossoNumero());
	}

	@Test
	public void seFachadaGeraALinhaDoBancoComOperacaoEmTexto() {

		BoletoCobranca boleto = new BoletoCobranca();
		boleto.banco("077").agencia("1-9").conta("123456789-0").carteira("112")
				.parametroBancario(ParametroBancoInter.NUMERO_DA_OPERACAO, "921255")
				.nossoNumero("9002194200-1").valor(270.00).dataVencimento("07/08/2024");
		boleto.sacado("Pagador de Demonstração").cedente("Beneficiário de Demonstração");

		assertEquals(LINHA_DO_BANCO, boleto.getLinhaDigitavel().write().replaceAll("[ .]", ""));
	}
}
