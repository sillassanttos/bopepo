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

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Cliente da API Cobrança v3 (boleto com Pix) do Banco Inter.
 * <p>
 * Contrato conforme a especificação OpenAPI oficial do portal
 * developers.inter.co. O que este cliente faz além de repassar as chamadas:
 * </p>
 * <ul>
 * <li>reaproveita o token OAuth até perto de expirar (o banco limita a 5
 * pedidos de token por minuto) e renova sozinho se o banco responder 401;</li>
 * <li>confere os limites da especificação antes de chamar o banco;</li>
 * <li>{@link #emitirEAguardar} junta a emissão assíncrona com a consulta, até o
 * boleto ficar pronto;</li>
 * <li>erros do banco viram {@link InterApiException} com as violações na
 * mensagem;</li>
 * <li>a simulação de pagamento ({@link #pagarNoSandbox}) só roda no sandbox.</li>
 * </ul>
 *
 * <pre>
 * CertificadoInter cert = CertificadoInter.dePem(Paths.get("local/inter/cert.crt"), Paths.get("local/inter/cert.key"));
 * ClienteInterCobranca inter = ClienteInterCobranca.builder(AmbienteInter.SANDBOX)
 *         .credenciais(System.getenv("INTER_CLIENT_ID"), System.getenv("INTER_CLIENT_SECRET"))
 *         .certificado(cert)
 *         .build();
 * CobrancaInter cobranca = inter.emitirEAguardar(novaCobranca, Duration.ofSeconds(30));
 * </pre>
 */
public final class ClienteInterCobranca {

    /** Escopos usados pelas operações deste cliente. */
    public static final String ESCOPOS_PADRAO = "boleto-cobranca.read boleto-cobranca.write";

    /** Antecedência para renovar o token antes de ele expirar. */
    private static final Duration MARGEM_TOKEN = Duration.ofSeconds(60);

    private static final ObjectMapper JSON = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE, true);

    private final AmbienteInter ambiente;
    private final String clientId;
    private final String clientSecret;
    private final String escopos;
    private final String contaCorrente;
    private final TransporteHttp transporte;
    private final Clock relogio;
    private final Espera espera;
    private final Duration intervaloConsulta;

    private String token;
    private Instant tokenExpiraEm = Instant.MIN;

    private ClienteInterCobranca(Builder b) {
        this.ambiente = b.ambiente;
        this.clientId = b.clientId;
        this.clientSecret = b.clientSecret;
        this.escopos = b.escopos;
        this.contaCorrente = b.contaCorrente;
        this.transporte = b.transporte;
        this.relogio = b.relogio;
        this.espera = b.espera;
        this.intervaloConsulta = b.intervaloConsulta;
    }

    public static Builder builder(AmbienteInter ambiente) {
        return new Builder(ambiente);
    }

    public AmbienteInter getAmbiente() {
        return ambiente;
    }

    // ------------------------------------------------------------------ cobrança

    /**
     * Emite a cobrança. A emissão é assíncrona: o banco devolve só o
     * {@code codigoSolicitacao}, e o boleto fica pronto em seguida (veja
     * {@link #emitirEAguardar}).
     * <p>
     * O banco recusa, por 30 minutos, uma cobrança com o mesmo seu número,
     * valor, vencimento e CPF/CNPJ do pagador (controle de duplicidade).
     * </p>
     *
     * @return o {@code codigoSolicitacao} da cobrança
     */
    public String emitir(NovaCobrancaInter cobranca) {
        Objects.requireNonNull(cobranca, "cobranca");
        cobranca.validar();
        JsonNode resposta = lerArvore(chamar("emitir cobrança", "POST", "/cobrancas", paraJson(cobranca)));
        JsonNode codigo = resposta.get("codigoSolicitacao");
        if (codigo == null || codigo.asText().isEmpty()) {
            throw new IllegalStateException("O Inter não devolveu o codigoSolicitacao na emissão.");
        }
        return codigo.asText();
    }

    /**
     * Emite a cobrança e consulta até o boleto ficar disponível ou a emissão
     * falhar.
     *
     * @param limite tempo máximo de espera
     * @return a cobrança com boleto (e Pix, se houver)
     * @throws IllegalStateException se a emissão falhar ({@code FALHA_EMISSAO})
     *             ou não terminar dentro do limite; a mensagem traz o
     *             {@code codigoSolicitacao} para consulta posterior
     */
    public CobrancaInter emitirEAguardar(NovaCobrancaInter cobranca, Duration limite) {
        return aguardarBoleto(emitir(cobranca), limite);
    }

    /**
     * Consulta a cobrança até o boleto ficar disponível ou a emissão falhar.
     */
    public CobrancaInter aguardarBoleto(String codigoSolicitacao, Duration limite) {

        Objects.requireNonNull(limite, "limite");
        Instant fim = relogio.instant().plus(limite);

        while (true) {
            CobrancaInter c = consultar(codigoSolicitacao);

            if (c.getSituacao() == SituacaoCobrancaInter.FALHA_EMISSAO) {
                throw new IllegalStateException("O Inter não conseguiu emitir a cobrança " + codigoSolicitacao + ".");
            }
            if (c.isBoletoDisponivel() && c.getSituacao() != SituacaoCobrancaInter.EM_PROCESSAMENTO) {
                return c;
            }
            if (!relogio.instant().plus(intervaloConsulta).isBefore(fim)) {
                throw new IllegalStateException("A cobrança " + codigoSolicitacao + " ainda está em processamento após "
                        + limite.getSeconds() + "s; consulte de novo mais tarde.");
            }
            try {
                espera.esperar(intervaloConsulta);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Espera pela cobrança " + codigoSolicitacao + " interrompida.", e);
            }
        }
    }

    /** Consulta o detalhe da cobrança. */
    public CobrancaInter consultar(String codigoSolicitacao) {
        String codigo = Validacoes.identificador("codigoSolicitacao", codigoSolicitacao);
        return ler(chamar("consultar cobrança", "GET", "/cobrancas/" + codigo, null), CobrancaInter.class);
    }

    /** Pesquisa uma página de cobranças. */
    public PaginaCobrancasInter listar(FiltroCobrancasInter filtro) {
        Objects.requireNonNull(filtro, "filtro");
        return ler(chamar("listar cobranças", "GET", "/cobrancas?" + filtro.paraQueryString(), null),
                PaginaCobrancasInter.class);
    }

    /**
     * Pesquisa todas as páginas, a partir da página do filtro.
     *
     * @param limitePaginas proteção contra laços longos (ex.: 50)
     */
    public List<CobrancaInter> listarTodas(FiltroCobrancasInter filtro, int limitePaginas) {

        Objects.requireNonNull(filtro, "filtro");
        List<CobrancaInter> todas = new ArrayList<>();
        int pagina = filtro.getPaginaAtual();

        for (int lidas = 0; lidas < limitePaginas; lidas++, pagina++) {
            PaginaCobrancasInter p = listar(filtro.paginaAtual(pagina));
            todas.addAll(p.getCobrancas());
            if (p.isUltimaPagina() || p.getCobrancas().isEmpty() || pagina + 1 >= p.getTotalPaginas()) {
                return todas;
            }
        }

        throw new IllegalStateException("A pesquisa passou de " + limitePaginas + " páginas; refine o filtro.");
    }

    /**
     * Cancela a cobrança.
     *
     * @param motivo motivo do cancelamento (até 50 caracteres)
     */
    public void cancelar(String codigoSolicitacao, String motivo) {
        String codigo = Validacoes.identificador("codigoSolicitacao", codigoSolicitacao);
        Map<String, String> corpo = Collections.singletonMap("motivoCancelamento",
                Validacoes.texto("motivoCancelamento", motivo, 1, 50));
        chamar("cancelar cobrança", "POST", "/cobrancas/" + codigo + "/cancelar", paraJson(corpo));
    }

    /** PDF da cobrança gerado pelo próprio Inter. */
    public byte[] pdf(String codigoSolicitacao) {
        String codigo = Validacoes.identificador("codigoSolicitacao", codigoSolicitacao);
        JsonNode resposta = lerArvore(chamar("obter PDF", "GET", "/cobrancas/" + codigo + "/pdf", null));
        JsonNode pdf = resposta.get("pdf");
        if (pdf == null || pdf.asText().isEmpty()) {
            throw new IllegalStateException("O Inter não devolveu o PDF da cobrança " + codigo + ".");
        }
        return Base64.getMimeDecoder().decode(pdf.asText());
    }

    /**
     * Simula o pagamento de uma cobrança. <strong>Só existe no
     * sandbox</strong>; em produção, lança {@link IllegalStateException} sem
     * chamar o banco.
     */
    public void pagarNoSandbox(String codigoSolicitacao, FormaRecebimentoInter pagarCom) {
        if (ambiente != AmbienteInter.SANDBOX) {
            throw new IllegalStateException("A simulação de pagamento só existe no sandbox do Inter.");
        }
        if (pagarCom != FormaRecebimentoInter.BOLETO && pagarCom != FormaRecebimentoInter.PIX) {
            throw new IllegalArgumentException("Pague com BOLETO ou PIX.");
        }
        String codigo = Validacoes.identificador("codigoSolicitacao", codigoSolicitacao);
        chamar("pagar no sandbox", "POST", "/cobrancas/" + codigo + "/pagar",
                paraJson(Collections.singletonMap("pagarCom", pagarCom.name())));
    }

    // ------------------------------------------------------------------ webhook

    /**
     * Cadastra (ou troca) o endereço que recebe as notificações das cobranças.
     *
     * @param url endereço {@code https://} público do seu sistema
     */
    public void cadastrarWebhook(String url) {
        if (url == null || !url.matches("^https://\\S+$")) {
            throw new IllegalArgumentException("O webhook precisa de uma URL https:// sem espaços: [" + url + "].");
        }
        chamar("cadastrar webhook", "PUT", "/cobrancas/webhook",
                paraJson(Collections.singletonMap("webhookUrl", url)));
    }

    /** Webhook cadastrado, ou vazio se não houver. */
    public Optional<WebhookInter> consultarWebhook() {
        try {
            return Optional.of(ler(chamar("consultar webhook", "GET", "/cobrancas/webhook", null), WebhookInter.class));
        } catch (InterApiException e) {
            if (e.getStatus() == 404) {
                return Optional.empty();
            }
            throw e;
        }
    }

    public void excluirWebhook() {
        chamar("excluir webhook", "DELETE", "/cobrancas/webhook", null);
    }

    /**
     * Lê o corpo de uma notificação recebida no webhook (uma lista de
     * cobranças). Não depende de credenciais: use no endpoint que recebe o
     * {@code POST} do banco.
     */
    public static List<CallbackCobrancaInter> lerCallbacks(String corpo) {
        try {
            return JSON.readValue(corpo, new TypeReference<List<CallbackCobrancaInter>>() {
            });
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Notificação do Inter em formato inesperado: " + e.getOriginalMessage(), e);
        }
    }

    // ------------------------------------------------------------------ HTTP

    private RespostaHttp chamar(String operacao, String metodo, String caminho, String corpo) {

        RespostaHttp resposta = enviarComToken(operacao, metodo, caminho, corpo);

        if (resposta.getStatus() == 401) {
            // token revogado ou expirado antes do previsto: renova e tenta uma vez
            invalidarToken();
            resposta = enviarComToken(operacao, metodo, caminho, corpo);
        }

        if (!resposta.isSucesso()) {
            throw erro(operacao, resposta);
        }
        return resposta;
    }

    private RespostaHttp enviarComToken(String operacao, String metodo, String caminho, String corpo) {

        Map<String, String> cabecalhos = new LinkedHashMap<>();
        cabecalhos.put("Authorization", "Bearer " + obterToken());
        cabecalhos.put("Accept", "application/json");
        if (corpo != null) {
            cabecalhos.put("Content-Type", "application/json");
        }
        if (contaCorrente != null) {
            cabecalhos.put("x-conta-corrente", contaCorrente);
        }

        return enviar(operacao, new RequisicaoHttp(metodo, ambiente.cobranca() + caminho, cabecalhos, corpo));
    }

    private synchronized String obterToken() {

        if (token != null && relogio.instant().isBefore(tokenExpiraEm.minus(MARGEM_TOKEN))) {
            return token;
        }

        String corpo = "client_id=" + codificar(clientId) + "&client_secret=" + codificar(clientSecret)
                + "&scope=" + codificar(escopos) + "&grant_type=client_credentials";

        Map<String, String> cabecalhos = new LinkedHashMap<>();
        cabecalhos.put("Content-Type", "application/x-www-form-urlencoded");
        cabecalhos.put("Accept", "application/json");

        RespostaHttp resposta = enviar("obter token",
                new RequisicaoHttp("POST", ambiente.token().toString(), cabecalhos, corpo));

        if (!resposta.isSucesso()) {
            throw erro("obter token", resposta);
        }

        JsonNode json = lerArvore(resposta);
        JsonNode acesso = json.get("access_token");
        if (acesso == null || acesso.asText().isEmpty()) {
            throw new IllegalStateException("O Inter não devolveu o access_token.");
        }

        long segundos = json.has("expires_in") ? json.get("expires_in").asLong(3600) : 3600;
        token = acesso.asText();
        tokenExpiraEm = relogio.instant().plusSeconds(segundos);
        return token;
    }

    private synchronized void invalidarToken() {
        token = null;
        tokenExpiraEm = Instant.MIN;
    }

    private RespostaHttp enviar(String operacao, RequisicaoHttp requisicao) {
        try {
            return transporte.enviar(requisicao);
        } catch (IOException e) {
            throw new InterApiException(operacao, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterApiException(operacao, e);
        }
    }

    private static InterApiException erro(String operacao, RespostaHttp resposta) {

        InterApiException.Problema problema = null;
        String corpo = resposta.getCorpo();

        if (!corpo.trim().isEmpty()) {
            try {
                problema = JSON.readValue(corpo, InterApiException.Problema.class);
            } catch (JsonProcessingException e) {
                // corpo fora do formato Problema: fica só no getCorpo()
            }
        }

        return new InterApiException(operacao, resposta.getStatus(),
                problema == null ? null : problema.getTitle(),
                problema == null ? null : problema.getDetail(),
                problema == null ? null : problema.getViolacoes(), corpo);
    }

    private static String paraJson(Object valor) {
        try {
            return JSON.writeValueAsString(valor);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao montar o JSON para o Inter.", e);
        }
    }

    private static <T> T ler(RespostaHttp resposta, Class<T> tipo) {
        try {
            return JSON.readValue(resposta.getCorpo(), tipo);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Resposta do Inter em formato inesperado: " + e.getOriginalMessage(), e);
        }
    }

    private static JsonNode lerArvore(RespostaHttp resposta) {
        try {
            return JSON.readTree(resposta.getCorpo());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Resposta do Inter em formato inesperado: " + e.getOriginalMessage(), e);
        }
    }

    private static String codificar(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8);
    }

    // ------------------------------------------------------------------ builder

    /** Pausa entre consultas; substituível nos testes. */
    @FunctionalInterface
    public interface Espera {
        void esperar(Duration duracao) throws InterruptedException;
    }

    /** Monta o {@link ClienteInterCobranca}. */
    public static final class Builder {

        private final AmbienteInter ambiente;
        private String clientId;
        private String clientSecret;
        private String escopos = ESCOPOS_PADRAO;
        private String contaCorrente;
        private TransporteHttp transporte;
        private CertificadoInter certificado;
        private Duration tempoLimite = Duration.ofSeconds(30);
        private Clock relogio = Clock.systemUTC();
        private Espera espera = duracao -> Thread.sleep(duracao.toMillis());
        private Duration intervaloConsulta = Duration.ofSeconds(2);

        private Builder(AmbienteInter ambiente) {
            this.ambiente = Objects.requireNonNull(ambiente, "ambiente");
        }

        /** Client ID e client secret da aplicação criada no Internet Banking PJ. */
        public Builder credenciais(String clientId, String clientSecret) {
            this.clientId = Validacoes.texto("clientId", clientId, 1, 200);
            this.clientSecret = Validacoes.texto("clientSecret", clientSecret, 1, 200);
            return this;
        }

        /** Certificado mTLS da aplicação. Cria o transporte HTTP padrão. */
        public Builder certificado(CertificadoInter certificado) {
            this.certificado = Objects.requireNonNull(certificado, "certificado");
            return this;
        }

        /** Escopos do token, separados por espaço. Padrão: {@link #ESCOPOS_PADRAO}. */
        public Builder escopos(String escopos) {
            this.escopos = Validacoes.texto("escopos", escopos, 1, 500);
            return this;
        }

        /**
         * Conta corrente (só números, com dígito, sem zeros à esquerda), para
         * aplicações com mais de uma conta. Vai no cabeçalho
         * {@code x-conta-corrente}.
         */
        public Builder contaCorrente(String conta) {
            if (conta == null || !conta.matches("[1-9][0-9]*")) {
                throw new IllegalArgumentException(
                        "Conta corrente deve ter só números, com o dígito e sem zeros à esquerda: [" + conta + "].");
            }
            this.contaCorrente = conta;
            return this;
        }

        public Builder tempoLimite(Duration tempoLimite) {
            this.tempoLimite = Objects.requireNonNull(tempoLimite, "tempoLimite");
            return this;
        }

        /** Intervalo entre consultas em {@link ClienteInterCobranca#emitirEAguardar}. Padrão: 2 s. */
        public Builder intervaloConsulta(Duration intervalo) {
            this.intervaloConsulta = Objects.requireNonNull(intervalo, "intervalo");
            return this;
        }

        /** Transporte próprio (testes, proxy, outro cliente HTTP). Dispensa o certificado. */
        public Builder transporte(TransporteHttp transporte) {
            this.transporte = Objects.requireNonNull(transporte, "transporte");
            return this;
        }

        Builder relogio(Clock relogio) {
            this.relogio = Objects.requireNonNull(relogio, "relogio");
            return this;
        }

        Builder espera(Espera espera) {
            this.espera = Objects.requireNonNull(espera, "espera");
            return this;
        }

        public ClienteInterCobranca build() {

            if (clientId == null || clientSecret == null) {
                throw new IllegalStateException("Informe as credenciais (client ID e client secret).");
            }
            if (transporte == null) {
                if (certificado == null) {
                    throw new IllegalStateException("Informe o certificado mTLS do Inter (ou um transporte próprio).");
                }
                try {
                    transporte = new TransporteJavaHttp(certificado.criarSslContext(), tempoLimite);
                } catch (GeneralSecurityException e) {
                    throw new IllegalStateException("Não foi possível usar o certificado do Inter: " + e.getMessage(), e);
                }
            }
            return new ClienteInterCobranca(this);
        }
    }
}
