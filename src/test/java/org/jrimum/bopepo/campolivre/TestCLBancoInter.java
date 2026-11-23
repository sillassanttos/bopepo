package org.jrimum.bopepo.campolivre;

import static org.junit.Assert.assertEquals;

import org.jrimum.bopepo.BancosSuportados;
import org.jrimum.bopepo.parametro.ParametroBancoInter;
import org.jrimum.domkee.banco.Agencia;
import org.jrimum.domkee.banco.Carteira;
import org.jrimum.domkee.banco.NumeroDaConta;
import org.jrimum.domkee.banco.ParametrosBancariosMap;
import org.junit.Before;
import org.junit.Test;

/**
 * <p>
 * Teste unitário do campo livre do Banco Inter (077).
 * </p>
 * <p>
 * Os dados vêm de um boleto emitido pelo próprio Inter (agência 0001,
 * carteira 112, operação 0921255, nosso número 9002194200-1), publicado no
 * fórum do Projeto ACBr (tópico 79070). A linha digitável completa é conferida
 * em {@code TestBoletoBancoInter}.
 * </p>
 */
public class TestCLBancoInter extends AbstractCampoLivreBaseTest<CLBancoInter> {

	@Before
	public void setUp() {

		titulo.getContaBancaria().setBanco(BancosSuportados.BANCO_INTER.create());
		titulo.getContaBancaria().setAgencia(new Agencia(1, "9"));
		titulo.getContaBancaria().setNumeroDaConta(new NumeroDaConta(123456789, "0"));
		titulo.getContaBancaria().setCarteira(new Carteira(112));
		titulo.setParametrosBancarios(new ParametrosBancariosMap(ParametroBancoInter.NUMERO_DA_OPERACAO, 921255));
		titulo.setNossoNumero("9002194200");
		titulo.setDigitoDoNossoNumero("1");

		createCampoLivreToTest();

		// agência(4) + carteira(3) + operação(7) + nosso número(10) + DV(1)
		setCampoLivreEsperadoComoString("0001112092125590021942001");
	}

	@Test
	public void seCalculaEGravaODigitoQuandoOTituloVemSemEle() {

		titulo.setDigitoDoNossoNumero(null);

		createCampoLivreToTest();

		assertEquals("0001112092125590021942001", writeCampoLivre());
		assertEquals("1", titulo.getDigitoDoNossoNumero());
	}

	@Test(expected = CampoLivreException.class)
	public void seNaoPermiteDigitoDoNossoNumeroErrado() {

		titulo.setDigitoDoNossoNumero("7");

		createCampoLivreToTest();
	}

	@Test(expected = CampoLivreException.class)
	public void seNaoPermiteTituloSemNumeroDaOperacao() {

		titulo.setParametrosBancarios(null);

		createCampoLivreToTest();
	}

	@Test(expected = CampoLivreException.class)
	public void seNaoPermiteNumeroDaOperacaoComMaisDe7Digitos() {

		titulo.setParametrosBancarios(new ParametrosBancariosMap(ParametroBancoInter.NUMERO_DA_OPERACAO, 10000000));

		createCampoLivreToTest();
	}

	@Test(expected = CampoLivreException.class)
	public void seNaoPermiteAgenciaNula() {

		testeSeNaoPermiteAgenciaNula();
	}

	@Test(expected = CampoLivreException.class)
	public void seNaoPermiteCarteiraNula() {

		testeSeNaoPermiteCarteiraNula();
	}

	@Test(expected = CampoLivreException.class)
	public void seNaoPermiteCarteiraComCodigoAcimaDe3Digitos() {

		testeSeNaoPermiteCarteiraComCodigoAcimaDoLimite(1000);
	}

	@Test(expected = CampoLivreException.class)
	public void seNaoPermiteNossoNumeroNulo() {

		testeSeNaoPermiteNossoNumeroNulo();
	}

	@Test(expected = CampoLivreException.class)
	public void seNaoPermiteNossoNumeroComTamanhoDiferenteDe10() {

		testeSeNaoPermiteNossoNumeroComTamanhoDiferenteDoEspecificado(11);
	}

	@Test(expected = CampoLivreException.class)
	public void seNaoPermiteNossoNumeroComBrancos() {

		testeSeNaoPermiteNossoNumeroComBrancos(NossoNumeroBancoInter.TAMANHO);
	}
}
