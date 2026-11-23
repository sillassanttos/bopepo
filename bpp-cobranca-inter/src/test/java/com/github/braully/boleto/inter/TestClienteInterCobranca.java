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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Testes do cliente da API Cobrança v3 do Inter com respostas gravadas no
 * formato da especificação oficial. Nenhum teste acessa a rede.
 */
public class TestClienteInterCobranca {

    private static final String CODIGO = "183e982a-34e5-4bc0-9643-def5432a1234";
    private static final String BASE = "https://cdpj-sandbox.partners.uatinter.co/cobranca/v3";

    private TransporteFalso transporte;
    private RelogioAjustavel relogio;
    private List<Duration> esperas;
    private ClienteInterCobranca inter;

    @Before
    public void setUp() {
        transporte = new TransporteFalso();
        relogio = new RelogioAjustavel(Instant.parse("2026-11-23T12:00:00Z"));
        esperas = new ArrayList<>();
        inter = cliente(AmbienteInter.SANDBOX).build();
    }

    private ClienteInterCobranca.Builder cliente(AmbienteInter ambiente) {
        return ClienteInterCobranca.builder(ambiente)
                .credenciais("client-id-demo", "client-secret-demo")
                .transporte(transporte)
                .relogio(relogio)
                .espera(d -> {
                    esperas.add(d);
                    relogio.avancar(d);
                })
                .intervaloConsulta(Duration.ofSeconds(2));
    }

    // ------------------------------------------------------------------ token

    @Test
    public void sePedeOTokenComCredenciaisEscoposEGrantType() {
        transporte.responderArquivo(200, "cobranca-detalhe.json");

        inter.consultar(CODIGO);

        RequisicaoHttp token = transporte.requisicoes.get(0);
        assertEquals("POST", token.getMetodo());
        assertEquals("https://cdpj-sandbox.partners.uatinter.co/oauth/v2/token", token.getUrl());
        assertEquals("application/x-www-form-urlencoded", token.getCabecalhos().get("Content-Type"));
        assertEquals("client_id=client-id-demo&client_secret=client-secret-demo"
                + "&scope=boleto-cobranca.read+boleto-cobranca.write&grant_type=client_credentials", token.getCorpo());
        assertEquals("Bearer token-1", transporte.ultimaChamadaDaApi().getCabecalhos().get("Authorization"));
    }

    @Test
    public void seReaproveitaOTokenAtePertoDeExpirar() {
        transporte.responderArquivo(200, "cobranca-detalhe.json")
                .responderArquivo(200, "cobranca-detalhe.json")
                .responderArquivo(200, "cobranca-detalhe.json");

        inter.consultar(CODIGO);
        relogio.avancar(Duration.ofMinutes(58));
        inter.consultar(CODIGO);
        assertEquals(1, transporte.tokensEmitidos);

        relogio.avancar(Duration.ofSeconds(61)); // dentro da margem de 60 s antes de expirar
        inter.consultar(CODIGO);
        assertEquals(2, transporte.tokensEmitidos);
        assertEquals("Bearer token-2", transporte.ultimaChamadaDaApi().getCabecalhos().get("Authorization"));
    }

    @Test
    public void seRenovaOTokenERepeteUmaVezQuandoOBancoResponde401() {
        transporte.responder(401, "").responderArquivo(200, "cobranca-detalhe.json");

        CobrancaInter c = inter.consultar(CODIGO);

        assertEquals(CODIGO, c.getCodigoSolicitacao());
        assertEquals(2, transporte.tokensEmitidos);
        assertEquals(2, transporte.chamadasDaApi().size());
    }

    @Test
    public void seMandaContaCorrenteNoCabecalhoQuandoInformada() {
        inter = cliente(AmbienteInter.SANDBOX).contaCorrente("123456789").build();
        transporte.responderArquivo(200, "cobranca-detalhe.json");

        inter.consultar(CODIGO);

        assertEquals("123456789", transporte.ultimaChamadaDaApi().getCabecalhos().get("x-conta-corrente"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void seNaoPermiteContaCorrenteComZeroAEsquerda() {
        cliente(AmbienteInter.SANDBOX).contaCorrente("0123");
    }

    // ------------------------------------------------------------------ emitir

    @Test
    public void seEmiteComOJsonDaEspecificacao() throws Exception {
        transporte.responder(200, "{\"codigoSolicitacao\":\"" + CODIGO + "\"}");

        String codigo = inter.emitir(novaCobrancaCompleta());

        assertEquals(CODIGO, codigo);
        RequisicaoHttp req = transporte.ultimaChamadaDaApi();
        assertEquals("POST", req.getMetodo());
        assertEquals(BASE + "/cobrancas", req.getUrl());
        assertEquals("application/json", req.getCabecalhos().get("Content-Type"));
        ObjectMapper json = new ObjectMapper();
        assertEquals(json.readTree(TransporteFalso.recurso("emitir-requisicao-esperada.json")),
                json.readTree(req.getCorpo()));
    }

    @Test
    public void seNaoChamaOBancoQuandoFaltaOPagador() {
        try {
            inter.emitir(new NovaCobrancaInter("PED-1", new BigDecimal("10"), LocalDate.of(2026, 12, 15)));
            fail("deveria recusar sem pagador");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("pagador"));
        }
        assertTrue(transporte.requisicoes.isEmpty());
    }

    @Test
    public void seEmitirEAguardarConsultaAteOBoletoFicarPronto() {
        transporte.responder(200, "{\"codigoSolicitacao\":\"" + CODIGO + "\"}")
                .responderArquivo(200, "cobranca-em-processamento.json")
                .responderArquivo(200, "cobranca-em-processamento.json")
                .responderArquivo(200, "cobranca-detalhe.json");

        CobrancaInter c = inter.emitirEAguardar(novaCobrancaCompleta(), Duration.ofSeconds(30));

        assertTrue(c.isBoletoDisponivel());
        assertEquals(SituacaoCobrancaInter.A_RECEBER, c.getSituacao());
        assertEquals(2, esperas.size());
        assertEquals(BASE + "/cobrancas/" + CODIGO, transporte.ultimaChamadaDaApi().getUrl());
    }

    @Test
    public void seEmitirEAguardarDesisteNoLimiteInformandoOCodigo() {
        transporte.responder(200, "{\"codigoSolicitacao\":\"" + CODIGO + "\"}");
        for (int i = 0; i < 10; i++) {
            transporte.responderArquivo(200, "cobranca-em-processamento.json");
        }

        try {
            inter.emitirEAguardar(novaCobrancaCompleta(), Duration.ofSeconds(5));
            fail("deveria desistir");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains(CODIGO));
        }
        assertTrue(esperas.size() <= 2);
    }

    @Test(expected = IllegalStateException.class)
    public void seEmitirEAguardarFalhaQuandoOBancoNaoEmite() {
        transporte.responder(200, "{\"codigoSolicitacao\":\"" + CODIGO + "\"}")
                .responder(200, TransporteFalso.recurso("cobranca-em-processamento.json")
                        .replace("EM_PROCESSAMENTO", "FALHA_EMISSAO"));

        inter.emitirEAguardar(novaCobrancaCompleta(), Duration.ofSeconds(30));
    }

    // ------------------------------------------------------------------ consultar / listar

    @Test
    public void seLeODetalheDaCobranca() {
        transporte.responderArquivo(200, "cobranca-detalhe.json");

        CobrancaInter c = inter.consultar(CODIGO);

        assertEquals("GET", transporte.ultimaChamadaDaApi().getMetodo());
        assertEquals("PED-1001", c.getCobranca().getSeuNumero());
        assertEquals(0, new BigDecimal("270.00").compareTo(c.getCobranca().getValorNominal()));
        assertEquals(LocalDate.of(2024, 8, 7), c.getCobranca().getDataVencimento());
        assertEquals("PERCENTUAL", c.getCobranca().getMulta().getCodigo());
        assertEquals(Integer.valueOf(5), c.getCobranca().getDescontos().get(0).getQuantidadeDias());
        assertEquals("Pagador de Demonstração", c.getCobranca().getPagador().getNome());
        assertEquals("90021942001", c.getBoleto().getNossoNumero());
        assertEquals(44, c.getBoleto().getCodigoBarras().length());
        assertEquals(47, c.getBoleto().getLinhaDigitavel().length());
        assertTrue(c.getPix().getPixCopiaECola().startsWith("000201"));
        assertFalse(c.getSituacao().isEncerrada());
    }

    @Test(expected = IllegalArgumentException.class)
    public void seNaoPermiteCodigoQueAlteraOCaminhoDaUrl() {
        inter.consultar("../webhook");
    }

    @Test
    public void seListaComOsFiltrosNaQueryString() {
        transporte.responderArquivo(200, "lista-pagina-0.json");

        PaginaCobrancasInter pagina = inter.listar(new FiltroCobrancasInter(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30))
                .situacao(SituacaoCobrancaInter.A_RECEBER)
                .cpfCnpjPessoaPagadora("111.444.777-35")
                .pessoaPagadora("Pagador de Demonstração")
                .itensPorPagina(2)
                .ordenarPor(FiltroCobrancasInter.OrdenarPor.DATA_VENCIMENTO, true));

        assertEquals(BASE + "/cobrancas?dataInicial=2026-11-01&dataFinal=2026-11-30&situacao=A_RECEBER"
                + "&pessoaPagadora=Pagador+de+Demonstra%C3%A7%C3%A3o&cpfCnpjPessoaPagadora=11144477735"
                + "&paginacao.itensPorPagina=2&paginacao.paginaAtual=0&ordenarPor=DATA_VENCIMENTO&tipoOrdenacao=DESC",
                transporte.ultimaChamadaDaApi().getUrl());
        assertEquals(2, pagina.getTotalPaginas());
        assertEquals(2, pagina.getCobrancas().size());
        assertEquals(0, new BigDecimal("123.45").compareTo(pagina.getCobrancas().get(0).getCobranca().getValorNominal()));
        assertTrue(pagina.getCobrancas().get(1).getSituacao().isPaga());
        assertNull(pagina.getCobrancas().get(1).getBoleto());
    }

    @Test
    public void seListarTodasPercorreAsPaginasEToleraSituacaoNova() {
        transporte.responderArquivo(200, "lista-pagina-0.json").responderArquivo(200, "lista-pagina-1.json");

        List<CobrancaInter> todas = inter.listarTodas(
                new FiltroCobrancasInter(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 31)), 10);

        assertEquals(3, todas.size());
        assertEquals(SituacaoCobrancaInter.DESCONHECIDA, todas.get(2).getSituacao());
        assertTrue(transporte.ultimaChamadaDaApi().getUrl().contains("paginacao.paginaAtual=1"));
    }

    // ------------------------------------------------------------------ cancelar / pdf / sandbox

    @Test
    public void seCancelaComOMotivo() {
        transporte.responder(202, "");

        inter.cancelar(CODIGO, "Pago por outra forma");

        RequisicaoHttp req = transporte.ultimaChamadaDaApi();
        assertEquals("POST", req.getMetodo());
        assertEquals(BASE + "/cobrancas/" + CODIGO + "/cancelar", req.getUrl());
        assertEquals("{\"motivoCancelamento\":\"Pago por outra forma\"}", req.getCorpo());
    }

    @Test(expected = IllegalArgumentException.class)
    public void seNaoPermiteMotivoDeCancelamentoComMaisDe50Caracteres() {
        inter.cancelar(CODIGO, "x".repeat(51));
    }

    @Test
    public void seDecodificaOPdfDoBanco() {
        byte[] pdf = "%PDF-1.4 demonstracao".getBytes(StandardCharsets.US_ASCII);
        transporte.responder(200, "{\"pdf\":\"" + Base64.getEncoder().encodeToString(pdf) + "\"}");

        assertArrayEquals(pdf, inter.pdf(CODIGO));
        assertEquals(BASE + "/cobrancas/" + CODIGO + "/pdf", transporte.ultimaChamadaDaApi().getUrl());
    }

    @Test
    public void sePagaNoSandbox() {
        transporte.responder(204, "");

        inter.pagarNoSandbox(CODIGO, FormaRecebimentoInter.PIX);

        assertEquals(BASE + "/cobrancas/" + CODIGO + "/pagar", transporte.ultimaChamadaDaApi().getUrl());
        assertEquals("{\"pagarCom\":\"PIX\"}", transporte.ultimaChamadaDaApi().getCorpo());
    }

    @Test
    public void seNaoSimulaPagamentoEmProducao() {
        ClienteInterCobranca producao = cliente(AmbienteInter.PRODUCAO).build();
        try {
            producao.pagarNoSandbox(CODIGO, FormaRecebimentoInter.BOLETO);
            fail("não pode simular pagamento em produção");
        } catch (IllegalStateException e) {
            assertTrue(transporte.requisicoes.isEmpty());
        }
    }

    // ------------------------------------------------------------------ erros

    @Test
    public void seErroDoBancoTrazAsViolacoesNaMensagem() {
        transporte.responderArquivo(400, "erro-400.json");

        try {
            inter.emitir(novaCobrancaCompleta());
            fail("deveria lançar InterApiException");
        } catch (InterApiException e) {
            assertEquals(400, e.getStatus());
            assertEquals("Dados inválidos.", e.getTitulo());
            assertEquals(2, e.getViolacoes().size());
            assertEquals("valorNominal", e.getViolacoes().get(0).getPropriedade());
            assertTrue(e.getMessage().contains("pagador.cep: CEP inválido"));
            assertTrue(e.getMessage().contains("emitir cobrança"));
        }
    }

    @Test
    public void seErroSemCorpoNoFormatoProblemaGuardaOCorpoBruto() {
        transporte.responder(503, "<html>indisponível</html>");

        try {
            inter.consultar(CODIGO);
            fail("deveria lançar InterApiException");
        } catch (InterApiException e) {
            assertEquals(503, e.getStatus());
            assertNull(e.getTitulo());
            assertEquals("<html>indisponível</html>", e.getCorpo());
        }
    }

    @Test
    public void seFalhaAoObterTokenViraInterApiException() {
        TransporteHttp recusaToken = req -> new RespostaHttp(403,
                "{\"title\":\"Acesso Negado\",\"status\":403,\"detail\":\"Escopo não liberado\"}");
        ClienteInterCobranca semAcesso = ClienteInterCobranca.builder(AmbienteInter.SANDBOX)
                .credenciais("id", "segredo").transporte(recusaToken).build();
        try {
            semAcesso.consultar(CODIGO);
            fail("deveria lançar InterApiException");
        } catch (InterApiException e) {
            assertEquals(403, e.getStatus());
            assertTrue(e.getMessage().contains("obter token"));
        }
    }

    // ------------------------------------------------------------------ webhook

    @Test
    public void seCadastraEConsultaOWebhook() {
        transporte.responder(204, "")
                .responder(200, "{\"webhookUrl\":\"https://exemplo.invalido/inter\",\"criacao\":\"2026-11-23T12:00:00Z\"}")
                .responder(404, "{\"title\":\"Não Encontrado\",\"status\":404}");

        inter.cadastrarWebhook("https://exemplo.invalido/inter");
        assertEquals("PUT", transporte.ultimaChamadaDaApi().getMetodo());
        assertEquals("{\"webhookUrl\":\"https://exemplo.invalido/inter\"}", transporte.ultimaChamadaDaApi().getCorpo());

        Optional<WebhookInter> webhook = inter.consultarWebhook();
        assertEquals("https://exemplo.invalido/inter", webhook.get().getWebhookUrl());

        assertFalse(inter.consultarWebhook().isPresent());
    }

    @Test(expected = IllegalArgumentException.class)
    public void seNaoPermiteWebhookSemHttps() {
        inter.cadastrarWebhook("http://exemplo.invalido/inter");
    }

    @Test
    public void seLeANotificacaoDoWebhook() {
        List<CallbackCobrancaInter> callbacks = ClienteInterCobranca.lerCallbacks(TransporteFalso.recurso("callback.json"));

        assertEquals(2, callbacks.size());
        assertEquals(SituacaoCobrancaInter.RECEBIDO, callbacks.get(0).getSituacao());
        assertEquals(0, new BigDecimal("270.00").compareTo(callbacks.get(0).getValorTotalRecebido()));
        assertEquals("BOLETO", callbacks.get(0).getOrigemRecebimento());
        assertEquals(SituacaoCobrancaInter.CANCELADO, callbacks.get(1).getSituacao());
    }

    // ------------------------------------------------------------------ apoio

    static NovaCobrancaInter novaCobrancaCompleta() {
        return new NovaCobrancaInter("PED-1001", new BigDecimal("270"), LocalDate.of(2024, 8, 7))
                .numDiasAgenda(30)
                .pagador(PessoaInter.de("111.444.777-35", "Pagador de Demonstração")
                        .endereco("Rua de Demonstração", "100", "Centro", "Belo Horizonte", "mg", "30110-000")
                        .complemento("Sala 1")
                        .email("pagador@exemplo.invalido")
                        .telefone("(31) 99999-0000"))
                .desconto(EncargoInter.descontoPercentual(new BigDecimal("3"), 5))
                .multa(EncargoInter.multaPercentual(new BigDecimal("2")))
                .mora(EncargoInter.moraTaxaMensal(new BigDecimal("1")))
                .mensagem("Pedido PED-1001", "Não receber após 30 dias do vencimento")
                .beneficiarioFinal(PessoaInter.de("11.222.333/0001-81", "Empresa de Demonstração")
                        .endereco("Avenida de Demonstração", null, null, "Belo Horizonte", "MG", "30110000"))
                .formasRecebimento(FormaRecebimentoInter.BOLETO, FormaRecebimentoInter.PIX);
    }

    /** Relógio que só anda quando o teste manda. */
    static final class RelogioAjustavel extends Clock {

        private Instant agora;

        RelogioAjustavel(Instant inicio) {
            this.agora = inicio;
        }

        void avancar(Duration d) {
            agora = agora.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return agora;
        }
    }
}
