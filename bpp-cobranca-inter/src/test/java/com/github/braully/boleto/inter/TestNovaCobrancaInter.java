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

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.Test;

/**
 * Validações locais do pedido de emissão, com os limites da especificação.
 */
public class TestNovaCobrancaInter {

    private static final LocalDate VENCIMENTO = LocalDate.of(2026, 12, 15);

    @Test
    public void seArredondaOValorParaDuasCasas() {
        assertEquals(new BigDecimal("10.13"),
                new NovaCobrancaInter("A", new BigDecimal("10.125"), VENCIMENTO).getValorNominal());
    }

    @Test(expected = IllegalArgumentException.class)
    public void seNaoPermiteValorAbaixoDe2e50() {
        new NovaCobrancaInter("A", new BigDecimal("2.49"), VENCIMENTO);
    }

    @Test(expected = IllegalArgumentException.class)
    public void seNaoPermiteSeuNumeroComMaisDe15Caracteres() {
        new NovaCobrancaInter("1234567890123456", BigDecimal.TEN, VENCIMENTO);
    }

    @Test(expected = IllegalArgumentException.class)
    public void seNaoPermiteMaisDe60DiasDeAgenda() {
        new NovaCobrancaInter("A", BigDecimal.TEN, VENCIMENTO).numDiasAgenda(61);
    }

    @Test(expected = IllegalArgumentException.class)
    public void seNaoPermiteSeisLinhasDeMensagem() {
        new NovaCobrancaInter("A", BigDecimal.TEN, VENCIMENTO).mensagem("1", "2", "3", "4", "5", "6");
    }

    @Test(expected = IllegalArgumentException.class)
    public void seNaoPermiteLinhaDeMensagemComMaisDe78Caracteres() {
        new NovaCobrancaInter("A", BigDecimal.TEN, VENCIMENTO).mensagem("x".repeat(79));
    }

    @Test(expected = IllegalArgumentException.class)
    public void seNaoPermiteSoPix() {
        new NovaCobrancaInter("A", BigDecimal.TEN, VENCIMENTO).formasRecebimento(FormaRecebimentoInter.PIX);
    }

    @Test(expected = IllegalArgumentException.class)
    public void seNaoPermiteMoraNoLugarDaMulta() {
        new NovaCobrancaInter("A", BigDecimal.TEN, VENCIMENTO).multa(EncargoInter.moraTaxaMensal(BigDecimal.ONE));
    }

    @Test(expected = IllegalArgumentException.class)
    public void seNaoPermiteEncargoZerado() {
        EncargoInter.multaPercentual(BigDecimal.ZERO);
    }

    @Test
    public void seDeduzOTipoDePessoaPeloDocumento() {
        assertEquals("FISICA", PessoaInter.de("111.444.777-35", "A").getTipoPessoa());
        assertEquals("JURIDICA", PessoaInter.de("11.222.333/0001-81", "A").getTipoPessoa());
        assertEquals("11222333000181", PessoaInter.de("11.222.333/0001-81", "A").getCpfCnpj());
    }

    @Test(expected = IllegalArgumentException.class)
    public void seNaoPermiteDocumentoComTamanhoErrado() {
        PessoaInter.de("123", "A");
    }

    @Test(expected = IllegalArgumentException.class)
    public void seNaoPermiteUfInexistente() {
        PessoaInter.de("111.444.777-35", "A").endereco("Rua", "1", null, "Cidade", "XX", "30110000");
    }

    @Test(expected = IllegalArgumentException.class)
    public void seNaoPermiteCepIncompleto() {
        PessoaInter.de("111.444.777-35", "A").endereco("Rua", "1", null, "Cidade", "MG", "3011");
    }

    @Test
    public void seSeparaDddETelefone() {
        PessoaInter p = PessoaInter.de("111.444.777-35", "A").telefone("(31) 3333-4444");
        assertEquals("31", p.getDdd());
        assertEquals("33334444", p.getTelefone());
    }

    @Test(expected = IllegalArgumentException.class)
    public void seNaoPermitePagadorSemEndereco() {
        new NovaCobrancaInter("A", BigDecimal.TEN, VENCIMENTO).pagador(PessoaInter.de("111.444.777-35", "A")).validar();
    }

    @Test(expected = IllegalArgumentException.class)
    public void seNaoPermitePeriodoInvertidoNoFiltro() {
        new FiltroCobrancasInter(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 11, 1));
    }
}
