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
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.math.BigDecimal;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;

import org.jrimum.bopepo.Boleto;
import org.jrimum.domkee.banco.Cedente;
import org.jrimum.domkee.banco.NumeroDaConta;
import org.junit.Test;

/**
 * Teste de ponta a ponta no <strong>sandbox</strong> do Inter.
 * <p>
 * Só roda quando estas variáveis de ambiente existem (sem elas, o teste é
 * ignorado):
 * </p>
 * <ul>
 * <li>{@code INTER_SANDBOX_CLIENT_ID} e {@code INTER_SANDBOX_CLIENT_SECRET};</li>
 * <li>{@code INTER_SANDBOX_CRT} e {@code INTER_SANDBOX_KEY}: caminhos do
 * certificado e da chave (guarde em {@code local\inter\}, fora do git).</li>
 * </ul>
 * <p>
 * O ambiente é fixo em {@link AmbienteInter#SANDBOX}: este teste nunca emite em
 * produção. Ele emite uma cobrança de R$ 2,50, confere o boleto montado pelo
 * Bopepo contra o do banco, baixa o PDF do Inter e cancela a cobrança.
 * </p>
 */
public class TestIntegracaoSandboxInter {

    @Test
    public void seEmiteConfereBaixaPdfECancelaNoSandbox() throws Exception {

        String id = System.getenv("INTER_SANDBOX_CLIENT_ID");
        String segredo = System.getenv("INTER_SANDBOX_CLIENT_SECRET");
        String crt = System.getenv("INTER_SANDBOX_CRT");
        String key = System.getenv("INTER_SANDBOX_KEY");
        assumeTrue("credenciais do sandbox do Inter ausentes", id != null && segredo != null && crt != null && key != null);

        ClienteInterCobranca inter = ClienteInterCobranca.builder(AmbienteInter.SANDBOX)
                .credenciais(id, segredo)
                .certificado(CertificadoInter.dePem(Paths.get(crt), Paths.get(key)))
                .build();

        String seuNumero = "T" + (System.currentTimeMillis() % 100_000_000_000_000L);
        NovaCobrancaInter nova = new NovaCobrancaInter(seuNumero, new BigDecimal("2.50"), LocalDate.now().plusDays(10))
                .pagador(PessoaInter.de("111.444.777-35", "Pagador de Demonstração")
                        .endereco("Rua de Demonstração", "100", "Centro", "Belo Horizonte", "MG", "30110000"));

        CobrancaInter cobranca = inter.emitirEAguardar(nova, Duration.ofSeconds(60));
        try {
            assertTrue(cobranca.isBoletoDisponivel());

            Boleto boleto = BoletoInter.paraBoleto(cobranca, new Cedente("Beneficiário de Demonstração"),
                    new NumeroDaConta(1, "0"));
            assertEquals(cobranca.getBoleto().getCodigoBarras(), boleto.getCodigoDeBarras().write());

            byte[] pdf = inter.pdf(cobranca.getCodigoSolicitacao());
            assertEquals("%PDF", new String(pdf, 0, 4, "US-ASCII"));
        } finally {
            inter.cancelar(cobranca.getCodigoSolicitacao(), "Teste automatizado no sandbox");
        }
    }
}
