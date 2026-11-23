/*
 * Copyright 2010 JRimum Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * Created at: 11/08/2010 - 10:23:00
 * 
 * ================================================================================
 * 
 * Direitos autorais 2008 JRimum Project
 * 
 * Licenciado sob a Licença Apache, Versão 2.0 ("LICENÇA"); você não pode usar
 * esse arquivo exceto em conformidade com a esta LICENÇA. Você pode obter uma
 * cópia desta LICENÇA em http://www.apache.org/licenses/LICENSE-2.0 A menos que
 * haja exigência legal ou acordo por escrito, a distribuição de software sob
 * esta LICENÇA se dará “COMO ESTÁ”, SEM GARANTIAS OU CONDIÇÕES DE QUALQUER
 * TIPO, sejam expressas ou tácitas. Veja a LICENÇA para a redação específica a
 * reger permissões e limitações sob esta LICENÇA.
 * 
 * Criado em: 11/08/2010 - 10:23:00
 */



package org.jrimum.bopepo.campolivre;

import static java.lang.String.format;

import org.apache.commons.lang3.StringUtils;
import org.jrimum.bopepo.parametro.ParametroBancoInter;
import org.jrimum.domkee.banco.Titulo;
import org.jrimum.texgit.Fillers;
import org.jrimum.texgit.FixedField;
import org.jrimum.utilix.Objects;

/**
 * O campo livre do Banco Inter (077) segue esta forma:
 *
 * <table border="1" cellpadding="0" cellspacing="0" style="border-collapse:
 * collapse" bordercolor="#111111" width="60%" id="campolivre">
 * <tr>
 * <thead>
 * <th>Posição</th>
 * <th>Tamanho</th>
 * <th>Conteúdo</th>
 * </thead>
 * </tr>
 * <tr>
 * <td>20-23</td>
 * <td>4</td>
 * <td>Código da agência, sem dígito (no Inter, 0001)</td>
 * </tr>
 * <tr>
 * <td>24-26</td>
 * <td>3</td>
 * <td>Carteira (normalmente 112)</td>
 * </tr>
 * <tr>
 * <td>27-33</td>
 * <td>7</td>
 * <td>Número da operação ({@link ParametroBancoInter#NUMERO_DA_OPERACAO})</td>
 * </tr>
 * <tr>
 * <td>34-43</td>
 * <td>10</td>
 * <td>Nosso número, sem o dígito</td>
 * </tr>
 * <tr>
 * <td>44-44</td>
 * <td>1</td>
 * <td>Dígito do nosso número: módulo 10 sobre agência + carteira + nosso
 * número (veja {@link NossoNumeroBancoInter})</td>
 * </tr>
 * </table>
 *
 * <p>
 * O layout foi conferido com a linha digitável de um boleto emitido pelo
 * próprio Inter. Se o título vier sem o dígito do nosso número, ele é
 * calculado e gravado no título, para sair também no boleto impresso. Se vier
 * com o dígito, ele é conferido: um dígito errado é recusado, em vez de gerar
 * um código de barras que o banco não reconhece.
 * </p>
 *
 * @since 0.2
 */
class CLBancoInter extends AbstractCLBancoInter {

	private static final long serialVersionUID = 858563493013156459L;

	/**
	 * Número de campos = 5.
	 */
	private static final Integer FIELDS_LENGTH = Integer.valueOf(5);

	private static final Integer AGENCIA_LENGTH = Integer.valueOf(4);

	private static final Integer CARTEIRA_LENGTH = Integer.valueOf(3);

	private static final Integer OPERACAO_LENGTH = Integer.valueOf(7);

	private static final Integer NOSSO_NUMERO_LENGTH = Integer.valueOf(NossoNumeroBancoInter.TAMANHO);

	private static final Integer DIGITO_LENGTH = Integer.valueOf(1);

	/**
	 * Cria um campo livre instanciando o número de fields ({@code FIELDS_LENGTH}) deste campo.
	 */
	CLBancoInter() {

		super(FIELDS_LENGTH);
	}

	@Override
	protected void checkValues(Titulo titulo) {

		checkAgenciaNotNull(titulo);
		checkCodigoDaAgencia(titulo);
		checkCodigoDaAgenciaMenorOuIgualQue(titulo, 9999);
		checkCarteiraNotNull(titulo);
		checkCodigoDaCarteira(titulo);
		checkCodigoDaCarteiraMenorOuIgualQue(titulo, 999);
		checkParametroBancario(titulo, ParametroBancoInter.NUMERO_DA_OPERACAO);
		checkParametroBancarioMenorOuIgualQue(titulo, ParametroBancoInter.NUMERO_DA_OPERACAO, 9999999);
		checkNossoNumero(titulo);
		checkTamanhoDoNossoNumero(titulo, NN10);

		if (StringUtils.isNotBlank(titulo.getDigitoDoNossoNumero())) {

			checkDigitoDoNossoNumero(titulo);
			checkTamanhoDigitoDoNossoNumero(titulo, 1);

			String esperado = String.valueOf(calcularDigito(titulo));

			Objects.checkArgument(esperado.equals(titulo.getDigitoDoNossoNumero()),
					format("Dígito do nosso número [%s-%s] não confere: pelas regras do Inter (módulo 10 sobre "
							+ "agência, carteira e nosso número) o dígito é [%s].",
							titulo.getNossoNumero(), titulo.getDigitoDoNossoNumero(), esperado));
		}
	}

	@Override
	protected void addFields(Titulo titulo) {

		String digito = String.valueOf(calcularDigito(titulo));

		if (StringUtils.isBlank(titulo.getDigitoDoNossoNumero())) {
			titulo.setDigitoDoNossoNumero(digito);
		}

		this.add(new FixedField<Integer>(titulo.getContaBancaria().getAgencia().getCodigo(), AGENCIA_LENGTH, Fillers.ZERO_LEFT));
		this.add(new FixedField<Integer>(titulo.getContaBancaria().getCarteira().getCodigo(), CARTEIRA_LENGTH, Fillers.ZERO_LEFT));
		this.add(new FixedField<Integer>(titulo.getParametrosBancarios().<Integer>getValor(ParametroBancoInter.NUMERO_DA_OPERACAO), OPERACAO_LENGTH, Fillers.ZERO_LEFT));
		this.add(new FixedField<String>(titulo.getNossoNumero(), NOSSO_NUMERO_LENGTH, Fillers.ZERO_LEFT));
		this.add(new FixedField<String>(digito, DIGITO_LENGTH));
	}

	private static int calcularDigito(Titulo titulo) {

		return NossoNumeroBancoInter.calcularDigito(
				titulo.getContaBancaria().getAgencia().getCodigo(),
				titulo.getContaBancaria().getCarteira().getCodigo(),
				titulo.getNossoNumero());
	}
}
