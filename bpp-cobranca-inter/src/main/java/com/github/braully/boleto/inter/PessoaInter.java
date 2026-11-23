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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Pagador ou beneficiário final de uma cobrança do Inter.
 * <p>
 * O beneficiário final só usa os campos de identificação e endereço. E-mail,
 * telefone, número e complemento são só do pagador.
 * </p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PessoaInter {

    private String cpfCnpj;
    private String tipoPessoa;
    private String nome;
    private String endereco;
    private String numero;
    private String complemento;
    private String bairro;
    private String cidade;
    private String uf;
    private String cep;
    private String email;
    private String ddd;
    private String telefone;

    public PessoaInter() {
    }

    /**
     * Cria a pessoa com CPF ou CNPJ, com ou sem máscara. O tipo (física ou
     * jurídica) é deduzido pela quantidade de dígitos.
     */
    public static PessoaInter de(String cpfCnpj, String nome) {
        return new PessoaInter().cpfCnpj(cpfCnpj).nome(nome);
    }

    /** CPF (11 dígitos) ou CNPJ (14 dígitos); a máscara é removida. */
    public PessoaInter cpfCnpj(String valor) {
        String digitos = Validacoes.soDigitos(valor);
        if (digitos.length() == 11) {
            this.tipoPessoa = "FISICA";
        } else if (digitos.length() == 14) {
            this.tipoPessoa = "JURIDICA";
        } else {
            throw new IllegalArgumentException("CPF/CNPJ [" + valor + "] deve ter 11 ou 14 dígitos.");
        }
        this.cpfCnpj = digitos;
        return this;
    }

    public PessoaInter nome(String valor) {
        this.nome = Validacoes.texto("nome", valor, 1, 100);
        return this;
    }

    /**
     * Endereço completo.
     *
     * @param logradouro rua, avenida etc. (até 100 caracteres)
     * @param numero número (até 10 caracteres; só vai para o pagador)
     * @param bairro bairro (até 60 caracteres, opcional)
     * @param cidade cidade (até 60 caracteres)
     * @param uf sigla da UF
     * @param cep CEP com ou sem máscara (8 dígitos)
     */
    public PessoaInter endereco(String logradouro, String numero, String bairro, String cidade, String uf, String cep) {
        this.endereco = Validacoes.texto("endereco", logradouro, 1, 100);
        this.numero = Validacoes.textoOpcional("numero", numero, 10);
        this.bairro = Validacoes.textoOpcional("bairro", bairro, 60);
        this.cidade = Validacoes.texto("cidade", cidade, 1, 60);
        this.uf = Validacoes.uf(uf);
        this.cep = Validacoes.cep(cep);
        return this;
    }

    public PessoaInter complemento(String valor) {
        this.complemento = Validacoes.textoOpcional("complemento", valor, 30);
        return this;
    }

    public PessoaInter email(String valor) {
        this.email = Validacoes.textoOpcional("email", valor, 50);
        return this;
    }

    /**
     * Telefone com DDD, com ou sem máscara: {@code "(31) 99999-9999"} vira DDD
     * {@code 31} e telefone {@code 999999999}.
     */
    public PessoaInter telefone(String dddETelefone) {
        String digitos = Validacoes.soDigitos(dddETelefone);
        if (digitos.length() < 10 || digitos.length() > 11) {
            throw new IllegalArgumentException("Telefone [" + dddETelefone + "] deve ter DDD + 8 ou 9 dígitos.");
        }
        this.ddd = digitos.substring(0, 2);
        this.telefone = digitos.substring(2);
        return this;
    }

    /** Confere os campos que a API exige para pagador e beneficiário final. */
    void validarObrigatorios(String papel) {
        Validacoes.exigir(cpfCnpj, papel + ".cpfCnpj");
        Validacoes.exigir(nome, papel + ".nome");
        Validacoes.exigir(endereco, papel + ".endereco");
        Validacoes.exigir(cidade, papel + ".cidade");
        Validacoes.exigir(uf, papel + ".uf");
        Validacoes.exigir(cep, papel + ".cep");
    }

    public String getCpfCnpj() {
        return cpfCnpj;
    }

    public String getTipoPessoa() {
        return tipoPessoa;
    }

    public String getNome() {
        return nome;
    }

    public String getEndereco() {
        return endereco;
    }

    public String getNumero() {
        return numero;
    }

    public String getComplemento() {
        return complemento;
    }

    public String getBairro() {
        return bairro;
    }

    public String getCidade() {
        return cidade;
    }

    public String getUf() {
        return uf;
    }

    public String getCep() {
        return cep;
    }

    public String getEmail() {
        return email;
    }

    public String getDdd() {
        return ddd;
    }

    public String getTelefone() {
        return telefone;
    }

    // Setters usados pelo Jackson ao ler respostas, sem as validações dos métodos acima.

    void setCpfCnpj(String cpfCnpj) {
        this.cpfCnpj = cpfCnpj;
    }

    void setTipoPessoa(String tipoPessoa) {
        this.tipoPessoa = tipoPessoa;
    }

    void setNome(String nome) {
        this.nome = nome;
    }

    void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    void setNumero(String numero) {
        this.numero = numero;
    }

    void setComplemento(String complemento) {
        this.complemento = complemento;
    }

    void setBairro(String bairro) {
        this.bairro = bairro;
    }

    void setCidade(String cidade) {
        this.cidade = cidade;
    }

    void setUf(String uf) {
        this.uf = uf;
    }

    void setCep(String cep) {
        this.cep = cep;
    }

    void setEmail(String email) {
        this.email = email;
    }

    void setDdd(String ddd) {
        this.ddd = ddd;
    }

    void setTelefone(String telefone) {
        this.telefone = telefone;
    }
}
