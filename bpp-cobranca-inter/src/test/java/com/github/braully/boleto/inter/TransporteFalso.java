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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Transporte em memória: responde o token sozinho e devolve, em ordem, as
 * respostas enfileiradas para as demais chamadas. Guarda todas as requisições
 * para conferência. Nenhuma chamada sai para a rede.
 */
class TransporteFalso implements TransporteHttp {

    final List<RequisicaoHttp> requisicoes = new ArrayList<>();
    private final Deque<RespostaHttp> respostas = new ArrayDeque<>();
    int tokensEmitidos;

    TransporteFalso responder(int status, String corpo) {
        respostas.addLast(new RespostaHttp(status, corpo));
        return this;
    }

    TransporteFalso responderArquivo(int status, String arquivo) {
        return responder(status, recurso(arquivo));
    }

    @Override
    public RespostaHttp enviar(RequisicaoHttp requisicao) {
        requisicoes.add(requisicao);
        if (requisicao.getUrl().endsWith("/oauth/v2/token")) {
            tokensEmitidos++;
            return new RespostaHttp(200, "{\"access_token\":\"token-" + tokensEmitidos
                    + "\",\"token_type\":\"Bearer\",\"expires_in\":3600,\"scope\":\"boleto-cobranca.read boleto-cobranca.write\"}");
        }
        if (respostas.isEmpty()) {
            throw new AssertionError("Requisição inesperada: " + requisicao);
        }
        return respostas.removeFirst();
    }

    /** Requisições à API de cobrança (sem as de token). */
    List<RequisicaoHttp> chamadasDaApi() {
        List<RequisicaoHttp> api = new ArrayList<>();
        for (RequisicaoHttp r : requisicoes) {
            if (!r.getUrl().endsWith("/oauth/v2/token")) {
                api.add(r);
            }
        }
        return api;
    }

    RequisicaoHttp ultimaChamadaDaApi() {
        List<RequisicaoHttp> api = chamadasDaApi();
        return api.get(api.size() - 1);
    }

    static String recurso(String arquivo) {
        try (InputStream in = TransporteFalso.class.getResourceAsStream("/inter/" + arquivo)) {
            if (in == null) {
                throw new IllegalArgumentException("Recurso de teste não encontrado: " + arquivo);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
