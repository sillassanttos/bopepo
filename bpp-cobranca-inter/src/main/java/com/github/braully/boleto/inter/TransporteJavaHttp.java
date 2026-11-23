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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

import javax.net.ssl.SSLContext;

/**
 * {@link TransporteHttp} com {@code java.net.http.HttpClient} e certificado do
 * cliente (mTLS), como o Inter exige em todas as chamadas.
 */
public final class TransporteJavaHttp implements TransporteHttp {

    private final HttpClient http;
    private final Duration tempoLimite;

    /**
     * @param sslContext contexto com o certificado do cliente (veja {@link CertificadoInter})
     * @param tempoLimite tempo máximo de cada requisição
     */
    public TransporteJavaHttp(SSLContext sslContext, Duration tempoLimite) {
        this.tempoLimite = Objects.requireNonNull(tempoLimite, "tempoLimite");
        this.http = HttpClient.newBuilder()
                .sslContext(Objects.requireNonNull(sslContext, "sslContext"))
                .connectTimeout(tempoLimite)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Override
    public RespostaHttp enviar(RequisicaoHttp requisicao) throws IOException, InterruptedException {

        HttpRequest.BodyPublisher corpo = requisicao.getCorpo() == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(requisicao.getCorpo(), StandardCharsets.UTF_8);

        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(requisicao.getUrl()))
                .timeout(tempoLimite)
                .method(requisicao.getMetodo(), corpo);

        for (Map.Entry<String, String> cabecalho : requisicao.getCabecalhos().entrySet()) {
            builder.header(cabecalho.getKey(), cabecalho.getValue());
        }

        HttpResponse<String> resposta = http.send(builder.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        return new RespostaHttp(resposta.statusCode(), resposta.body());
    }
}
