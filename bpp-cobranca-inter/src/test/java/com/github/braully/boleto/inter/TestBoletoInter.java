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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.jrimum.bopepo.Boleto;
import org.jrimum.bopepo.view.BoletoViewer;
import org.jrimum.domkee.banco.Cedente;
import org.jrimum.domkee.banco.NumeroDaConta;
import org.junit.Test;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Conversão da cobrança do Inter para o boleto do Bopepo.
 * <p>
 * A resposta gravada usa o código de barras de um boleto real do Inter
 * (fórum do Projeto ACBr, tópico 79070); pagador e beneficiário são de
 * demonstração.
 * </p>
 */
public class TestBoletoInter {

    private static final ObjectMapper JSON = new ObjectMapper()
            .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE, true);

    private static final Cedente BENEFICIARIO = new Cedente("Beneficiário de Demonstração", "11.222.333/0001-81");

    private static CobrancaInter cobranca(String arquivo) throws Exception {
        return JSON.readValue(TransporteFalso.recurso(arquivo), CobrancaInter.class);
    }

    @Test
    public void seMontaOBoletoComOMesmoCodigoDeBarrasELinhaDoBanco() throws Exception {
        CobrancaInter c = cobranca("cobranca-detalhe.json");

        Boleto boleto = BoletoInter.paraBoleto(c, BENEFICIARIO, new NumeroDaConta(123456789, "0"));

        assertEquals(c.getBoleto().getCodigoBarras(), boleto.getCodigoDeBarras().write());
        assertEquals(c.getBoleto().getLinhaDigitavel(), boleto.getLinhaDigitavel().write().replaceAll("[ .]", ""));
        assertEquals("9002194200", boleto.getTitulo().getNossoNumero());
        assertEquals("1", boleto.getTitulo().getDigitoDoNossoNumero());
        assertEquals("PED-1001", boleto.getTitulo().getNumeroDoDocumento());
        assertEquals(c.getPix().getPixCopiaECola(), boleto.getPixCopiaECola());
    }

    @Test
    public void seGeraOPdfComQrCode() throws Exception {
        Boleto boleto = BoletoInter.paraBoleto(cobranca("cobranca-detalhe.json"), BENEFICIARIO,
                new NumeroDaConta(123456789, "0"));

        byte[] pdf = new BoletoViewer(boleto).getPdfAsByteArray();

        assertTrue(pdf.length > 1000);
        assertEquals("%PDF", new String(pdf, 0, 4, "US-ASCII"));
    }

    @Test
    public void seSemPixOBoletoSaiSemQrCode() throws Exception {
        String semPix = TransporteFalso.recurso("cobranca-detalhe.json").replaceAll("(?s),\\s*\"pix\"\\s*:\\s*\\{[^}]*\\}", "");
        CobrancaInter c = JSON.readValue(semPix, CobrancaInter.class);

        assertNull(c.getPix());
        assertNull(BoletoInter.paraBoleto(c, BENEFICIARIO, new NumeroDaConta(123456789, "0")).getPixCopiaECola());
    }

    @Test(expected = IllegalStateException.class)
    public void seNaoMontaBoletoEmProcessamento() throws Exception {
        BoletoInter.paraBoleto(cobranca("cobranca-em-processamento.json"), BENEFICIARIO, new NumeroDaConta(1, "0"));
    }

    @Test(expected = IllegalStateException.class)
    public void seNaoMontaBoletoQuandoOValorNaoConfereComOCodigoDeBarras() throws Exception {
        String valorErrado = TransporteFalso.recurso("cobranca-detalhe.json").replace("\"valorNominal\": 270.00", "\"valorNominal\": 271.00");

        BoletoInter.paraBoleto(JSON.readValue(valorErrado, CobrancaInter.class), BENEFICIARIO, new NumeroDaConta(1, "0"));
    }

    @Test(expected = IllegalStateException.class)
    public void seNaoMontaBoletoComDigitoDoNossoNumeroErrado() throws Exception {
        // troca o DV (última posição do código de barras) de 1 para 7
        String dvErrado = TransporteFalso.recurso("cobranca-detalhe.json")
                .replace("07791980100000270000001112092125590021942001", "07791980100000270000001112092125590021942007");

        BoletoInter.paraBoleto(JSON.readValue(dvErrado, CobrancaInter.class), BENEFICIARIO, new NumeroDaConta(1, "0"));
    }
}
