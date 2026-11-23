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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

/**
 * Certificado do cliente (mTLS) exigido pela API do Inter.
 * <p>
 * O Inter entrega o certificado em dois arquivos PEM ({@code .crt} e
 * {@code .key}). Eles podem ser lidos direto com {@link #dePem(Path, Path)},
 * sem conversão para {@code .pfx}. O formato PKCS#12 ({@code .pfx}/{@code .p12})
 * também é aceito, com {@link #dePkcs12(Path, char[])}.
 * </p>
 * <p>
 * Os certificados do Inter valem um ano. Com {@link #getValidoAte()} e
 * {@link #venceEm(Duration)} dá para avisar antes que a integração pare.
 * </p>
 * <p>
 * <strong>Segurança:</strong> mantenha o certificado e a chave privada fora do
 * repositório do projeto. Guarde-os fora do git ou num cofre.
 * </p>
 */
public final class CertificadoInter {

    private static final Pattern BLOCO_PEM = Pattern.compile(
            "-----BEGIN ([A-Z0-9 ]+)-----([A-Za-z0-9+/=\\s]+)-----END \\1-----");

    private final KeyStore keyStore;
    private final char[] senhaInterna;
    private final X509Certificate certificado;

    private CertificadoInter(KeyStore keyStore, char[] senhaInterna, X509Certificate certificado) {
        this.keyStore = keyStore;
        this.senhaInterna = senhaInterna;
        this.certificado = certificado;
    }

    /**
     * Lê um certificado PKCS#12 ({@code .pfx}/{@code .p12}).
     */
    public static CertificadoInter dePkcs12(Path arquivo, char[] senha) throws IOException, GeneralSecurityException {
        try (InputStream in = Files.newInputStream(arquivo)) {
            return dePkcs12(in, senha);
        }
    }

    /**
     * Lê um certificado PKCS#12 de um fluxo. O fluxo não é fechado.
     */
    public static CertificadoInter dePkcs12(InputStream in, char[] senha) throws IOException, GeneralSecurityException {

        Objects.requireNonNull(senha, "senha");

        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(in, senha);

        Enumeration<String> aliases = ks.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (ks.isKeyEntry(alias)) {
                Certificate cert = ks.getCertificate(alias);
                if (cert instanceof X509Certificate) {
                    return new CertificadoInter(ks, senha.clone(), (X509Certificate) cert);
                }
            }
        }

        throw new GeneralSecurityException("O arquivo PKCS#12 não tem chave privada com certificado X.509.");
    }

    /**
     * Lê o certificado e a chave privada em PEM, como o Inter entrega.
     * <p>
     * A chave pode estar em PKCS#8 ({@code BEGIN PRIVATE KEY}) ou em PKCS#1 RSA
     * ({@code BEGIN RSA PRIVATE KEY}), sempre sem senha.
     * </p>
     */
    public static CertificadoInter dePem(Path certificadoCrt, Path chaveKey) throws IOException, GeneralSecurityException {
        return dePem(Files.readString(certificadoCrt, StandardCharsets.US_ASCII),
                Files.readString(chaveKey, StandardCharsets.US_ASCII));
    }

    /**
     * Lê o certificado e a chave privada a partir do texto PEM.
     */
    public static CertificadoInter dePem(String certificadoPem, String chavePem) throws GeneralSecurityException {

        Objects.requireNonNull(certificadoPem, "certificadoPem");
        Objects.requireNonNull(chavePem, "chavePem");

        List<X509Certificate> cadeia = lerCertificados(certificadoPem);
        if (cadeia.isEmpty()) {
            throw new GeneralSecurityException("Nenhum certificado encontrado no PEM informado.");
        }

        PrivateKey chave = lerChave(chavePem);

        char[] senha = gerarSenhaInterna();
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try {
            ks.load(null, null);
        } catch (IOException e) {
            throw new GeneralSecurityException(e);
        }
        ks.setKeyEntry("inter", chave, senha, cadeia.toArray(new Certificate[0]));

        return new CertificadoInter(ks, senha, cadeia.get(0));
    }

    /**
     * Cria o {@link SSLContext} com este certificado, para o
     * {@link TransporteJavaHttp}. O certificado do servidor é conferido contra
     * as autoridades confiáveis padrão do Java.
     */
    public SSLContext criarSslContext() throws GeneralSecurityException {

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, senhaInterna);

        SSLContext ssl = SSLContext.getInstance("TLS");
        ssl.init(kmf.getKeyManagers(), null, new SecureRandom());
        return ssl;
    }

    public X509Certificate getCertificado() {
        return certificado;
    }

    /** Fim da validade do certificado. */
    public Instant getValidoAte() {
        return certificado.getNotAfter().toInstant();
    }

    /** Indica se o certificado vence dentro do prazo informado ou se já venceu. */
    public boolean venceEm(Duration prazo) {
        return !Instant.now().plus(prazo).isBefore(getValidoAte());
    }

    private static List<X509Certificate> lerCertificados(String pem) throws GeneralSecurityException {

        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Collection<? extends Certificate> lidos = cf.generateCertificates(
                new ByteArrayInputStream(pem.getBytes(StandardCharsets.US_ASCII)));

        List<X509Certificate> cadeia = new ArrayList<>();
        for (Certificate c : lidos) {
            cadeia.add((X509Certificate) c);
        }
        return cadeia;
    }

    private static PrivateKey lerChave(String pem) throws GeneralSecurityException {

        Matcher m = BLOCO_PEM.matcher(pem);

        while (m.find()) {
            String tipo = m.group(1);
            byte[] der = Base64.getMimeDecoder().decode(m.group(2));

            switch (tipo) {
                case "PRIVATE KEY":
                    return lerPkcs8(der);
                case "RSA PRIVATE KEY":
                    return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(pkcs1ParaPkcs8(der)));
                case "ENCRYPTED PRIVATE KEY":
                    throw new GeneralSecurityException(
                            "Chave privada com senha não é suportada: use o .pfx com dePkcs12 ou tire a senha da chave.");
                default:
                    // outros blocos, como parâmetros EC, são ignorados
            }
        }

        throw new GeneralSecurityException("Nenhuma chave privada encontrada no PEM informado.");
    }

    private static PrivateKey lerPkcs8(byte[] der) throws GeneralSecurityException {

        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);

        for (String algoritmo : new String[] { "RSA", "EC" }) {
            try {
                return KeyFactory.getInstance(algoritmo).generatePrivate(spec);
            } catch (InvalidKeySpecException e) {
                // tenta o próximo algoritmo
            }
        }

        throw new GeneralSecurityException("Chave privada PKCS#8 com algoritmo não suportado (esperado RSA ou EC).");
    }

    /**
     * Embrulha uma chave RSA PKCS#1 na estrutura PKCS#8 (RFC 5208), que é a
     * que o {@link KeyFactory} do Java lê.
     */
    static byte[] pkcs1ParaPkcs8(byte[] pkcs1) {

        byte[] versao = { 0x02, 0x01, 0x00 };
        // AlgorithmIdentifier: SEQUENCE { OID 1.2.840.113549.1.1.1 (rsaEncryption), NULL }
        byte[] algoritmo = { 0x30, 0x0D, 0x06, 0x09, 0x2A, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xF7, 0x0D,
                0x01, 0x01, 0x01, 0x05, 0x00 };
        byte[] chave = der(0x04, pkcs1);

        ByteArrayOutputStream conteudo = new ByteArrayOutputStream();
        conteudo.writeBytes(versao);
        conteudo.writeBytes(algoritmo);
        conteudo.writeBytes(chave);

        return der(0x30, conteudo.toByteArray());
    }

    private static byte[] der(int tag, byte[] valor) {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(tag);

        int tamanho = valor.length;
        if (tamanho < 0x80) {
            out.write(tamanho);
        } else {
            int bytes = (Integer.SIZE - Integer.numberOfLeadingZeros(tamanho) + 7) / 8;
            out.write(0x80 | bytes);
            for (int i = bytes - 1; i >= 0; i--) {
                out.write((tamanho >>> (8 * i)) & 0xFF);
            }
        }

        out.writeBytes(valor);
        return out.toByteArray();
    }

    private static char[] gerarSenhaInterna() {
        byte[] aleatorio = new byte[24];
        new SecureRandom().nextBytes(aleatorio);
        return Base64.getEncoder().encodeToString(aleatorio).toCharArray();
    }
}
