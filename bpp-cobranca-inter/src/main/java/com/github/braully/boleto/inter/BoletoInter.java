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

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Objects;

import org.jrimum.bopepo.BancosSuportados;
import org.jrimum.bopepo.Boleto;
import org.jrimum.bopepo.campolivre.NossoNumeroBancoInter;
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
import org.jrimum.domkee.pessoa.Endereco;
import org.jrimum.domkee.pessoa.UnidadeFederativa;

/**
 * Monta o {@link Boleto} do Bopepo a partir de uma cobrança emitida pelo Inter,
 * para gerar o PDF com o layout do Bopepo em vez do PDF do banco.
 * <p>
 * Agência, carteira, número da operação e nosso número saem do próprio código
 * de barras devolvido pelo banco. Antes de devolver o boleto, o código de
 * barras gerado pelo Bopepo é comparado com o do banco: se forem diferentes, o
 * boleto não é montado, porque o PDF sairia com um código que o banco não
 * reconhece. O Pix copia e cola, quando houver, vira o QR Code do boleto.
 * </p>
 */
public final class BoletoInter {

    private static final String CODIGO_INTER = "077";
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private BoletoInter() {
    }

    /**
     * @param cobranca cobrança com boleto disponível (veja
     *            {@link ClienteInterCobranca#emitirEAguardar})
     * @param beneficiario nome e CPF/CNPJ de quem recebe
     * @param contaCorrente conta corrente do beneficiário, impressa no campo
     *            "Agência/Código do Beneficiário"
     * @return o boleto pronto para o {@code BoletoViewer}
     * @throws IllegalStateException se a cobrança ainda não tem boleto ou se o
     *             código de barras não confere
     */
    public static Boleto paraBoleto(CobrancaInter cobranca, Cedente beneficiario, NumeroDaConta contaCorrente) {

        Objects.requireNonNull(cobranca, "cobranca");
        Objects.requireNonNull(beneficiario, "beneficiario");
        Objects.requireNonNull(contaCorrente, "contaCorrente");

        if (!cobranca.isBoletoDisponivel()) {
            throw new IllegalStateException("A cobrança " + cobranca.getCodigoSolicitacao()
                    + " ainda não tem boleto (situação " + cobranca.getSituacao() + ").");
        }

        CobrancaInter.Dados dados = cobranca.getCobranca();
        String codigoBarras = cobranca.getBoleto().getCodigoBarras();

        if (codigoBarras.length() != 44 || !codigoBarras.startsWith(CODIGO_INTER)
                || !codigoBarras.chars().allMatch(c -> c >= '0' && c <= '9')) {
            throw new IllegalStateException("Código de barras do Inter em formato inesperado: [" + codigoBarras + "].");
        }

        // Campo livre (posições 20 a 44): agência(4) carteira(3) operação(7) nosso número(10) DV(1)
        String campoLivre = codigoBarras.substring(19);
        int agencia = Integer.parseInt(campoLivre.substring(0, 4));
        int carteira = Integer.parseInt(campoLivre.substring(4, 7));
        int operacao = Integer.parseInt(campoLivre.substring(7, 14));
        String nossoNumero = campoLivre.substring(14, 24);
        String digito = campoLivre.substring(24, 25);

        if (!NossoNumeroBancoInter.isValido(agencia, carteira, nossoNumero + digito)) {
            throw new IllegalStateException("Dígito do nosso número no código de barras do Inter não confere: ["
                    + nossoNumero + "-" + digito + "].");
        }

        ContaBancaria conta = new ContaBancaria(BancosSuportados.BANCO_INTER.create());
        conta.setAgencia(new Agencia(agencia, "9"));
        conta.setCarteira(new Carteira(carteira));
        conta.setNumeroDaConta(contaCorrente);

        Titulo titulo = new Titulo(conta, sacado(dados.getPagador()), beneficiario);
        titulo.setParametrosBancarios(new ParametrosBancariosMap(ParametroBancoInter.NUMERO_DA_OPERACAO, operacao));
        titulo.setNossoNumero(nossoNumero);
        titulo.setDigitoDoNossoNumero(digito);
        titulo.setNumeroDoDocumento(dados.getSeuNumero());
        titulo.setTipoDeMoeda(TipoDeMoeda.REAL);
        titulo.setValor(dados.getValorNominal());
        titulo.setDataDoVencimento(data(dados.getDataVencimento()));
        if (dados.getDataEmissao() != null) {
            titulo.setDataDoDocumento(data(dados.getDataEmissao()));
        }

        Boleto boleto = new Boleto(titulo);
        boleto.setLocalPagamento("Pagável em qualquer banco ou pelo Pix");

        String gerado = boleto.getCodigoDeBarras().write();
        if (!gerado.equals(codigoBarras)) {
            throw new IllegalStateException("O código de barras montado pelo Bopepo [" + gerado
                    + "] difere do devolvido pelo Inter [" + codigoBarras + "]: confira valor e vencimento.");
        }

        if (cobranca.getPix() != null) {
            boleto.setPixCopiaECola(cobranca.getPix().getPixCopiaECola());
        }

        return boleto;
    }

    private static Sacado sacado(PessoaInter pagador) {

        if (pagador == null) {
            throw new IllegalStateException("A cobrança veio sem o pagador.");
        }

        Sacado sacado = pagador.getCpfCnpj() == null
                ? new Sacado(pagador.getNome())
                : new Sacado(pagador.getNome(), formatarCpfCnpj(pagador.getCpfCnpj()));

        if (pagador.getEndereco() != null) {
            Endereco endereco = new Endereco();
            endereco.setLogradouro(pagador.getEndereco());
            endereco.setNumero(pagador.getNumero());
            endereco.setComplemento(pagador.getComplemento());
            endereco.setBairro(pagador.getBairro());
            endereco.setLocalidade(pagador.getCidade());
            if (pagador.getUf() != null) {
                endereco.setUF(UnidadeFederativa.valueOfSigla(pagador.getUf()));
            }
            if (pagador.getCep() != null && pagador.getCep().length() == 8) {
                endereco.setCep(pagador.getCep().substring(0, 5) + "-" + pagador.getCep().substring(5));
            }
            sacado.addEndereco(endereco);
        }

        return sacado;
    }

    private static String formatarCpfCnpj(String digitos) {
        if (digitos.length() == 11) {
            return digitos.replaceFirst("(\\d{3})(\\d{3})(\\d{3})(\\d{2})", "$1.$2.$3-$4");
        }
        if (digitos.length() == 14) {
            return digitos.replaceFirst("(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})", "$1.$2.$3/$4-$5");
        }
        return digitos;
    }

    private static Date data(LocalDate data) {
        if (data == null) {
            throw new IllegalStateException("A cobrança veio sem data de vencimento.");
        }
        return Date.from(data.atStartOfDay(FUSO).toInstant());
    }
}
