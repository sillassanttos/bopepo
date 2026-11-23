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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Leitura do certificado mTLS em PKCS#12 e em PEM (PKCS#8 e PKCS#1).
 * <p>
 * O certificado é gerado na hora com o {@code keytool} do próprio JDK, numa
 * pasta temporária. Nenhuma chave fica no repositório.
 * </p>
 */
public class TestCertificadoInter {

    private static final char[] SENHA = "senha-de-teste".toCharArray();

    private static Path pfx;
    private static X509Certificate certificadoGerado;
    private static PrivateKey chaveGerada;

    @BeforeClass
    public static void gerarCertificado() throws Exception {

        Path pasta = Files.createTempDirectory("cert-inter-teste");
        pasta.toFile().deleteOnExit();
        pfx = pasta.resolve("teste.p12");

        String keytool = System.getProperty("java.home") + File.separator + "bin" + File.separator + "keytool";
        Process p = new ProcessBuilder(keytool, "-genkeypair", "-alias", "teste", "-keyalg", "RSA", "-keysize", "2048",
                "-validity", "30", "-dname", "CN=Teste de Demonstracao", "-storetype", "PKCS12",
                "-keystore", pfx.toString(), "-storepass", new String(SENHA), "-keypass", new String(SENHA))
                .redirectErrorStream(true).start();
        p.getInputStream().readAllBytes();
        assertTrue("keytool demorou demais", p.waitFor(60, TimeUnit.SECONDS));
        assertEquals("keytool falhou", 0, p.exitValue());
        pfx.toFile().deleteOnExit();

        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream in = Files.newInputStream(pfx)) {
            ks.load(in, SENHA);
        }
        certificadoGerado = (X509Certificate) ks.getCertificate("teste");
        chaveGerada = (PrivateKey) ks.getKey("teste", SENHA);
    }

    @Test
    public void seLePkcs12ECriaOContextoTls() throws Exception {
        CertificadoInter cert = CertificadoInter.dePkcs12(pfx, SENHA);

        assertEquals(certificadoGerado, cert.getCertificado());
        assertNotNull(cert.criarSslContext());
        assertTrue(cert.venceEm(Duration.ofDays(31)));
        assertFalse(cert.venceEm(Duration.ofDays(1)));
    }

    @Test
    public void seLePemComChavePkcs8() throws Exception {
        CertificadoInter cert = CertificadoInter.dePem(pemCertificado(), pem("PRIVATE KEY", chaveGerada.getEncoded()));

        assertEquals(certificadoGerado, cert.getCertificado());
        assertNotNull(cert.criarSslContext());
    }

    @Test
    public void seLePemComChaveRsaPkcs1() throws Exception {
        byte[] pkcs1 = extrairPkcs1(chaveGerada.getEncoded());
        CertificadoInter cert = CertificadoInter.dePem(pemCertificado(), pem("RSA PRIVATE KEY", pkcs1));

        assertEquals(certificadoGerado.getNotAfter().toInstant(), cert.getValidoAte());
        assertNotNull(cert.criarSslContext());
        assertTrue(Arrays.equals(chaveGerada.getEncoded(), CertificadoInter.pkcs1ParaPkcs8(pkcs1)));
    }

    @Test(expected = GeneralSecurityException.class)
    public void seRecusaChaveComSenha() throws Exception {
        CertificadoInter.dePem(pemCertificado(), pem("ENCRYPTED PRIVATE KEY", new byte[] { 1, 2, 3 }));
    }

    @Test(expected = GeneralSecurityException.class)
    public void seRecusaPemSemChave() throws Exception {
        CertificadoInter.dePem(pemCertificado(), pemCertificado());
    }

    private static String pemCertificado() throws Exception {
        return pem("CERTIFICATE", certificadoGerado.getEncoded());
    }

    private static String pem(String tipo, byte[] der) {
        return "-----BEGIN " + tipo + "-----\n" + Base64.getMimeEncoder(64, new byte[] { '\n' }).encodeToString(der)
                + "\n-----END " + tipo + "-----\n";
    }

    /**
     * Extrai a chave PKCS#1 de dentro da estrutura PKCS#8:
     * SEQUENCE { INTEGER, SEQUENCE { OID, NULL }, OCTET STRING { pkcs1 } }.
     */
    private static byte[] extrairPkcs1(byte[] pkcs8) {
        int[] pos = { 0 };
        lerCabecalho(pkcs8, pos, 0x30);                 // SEQUENCE externa: entra no conteúdo
        int versao = lerCabecalho(pkcs8, pos, 0x02);    // versão: pula o conteúdo
        pos[0] += versao;
        int algoritmo = lerCabecalho(pkcs8, pos, 0x30); // AlgorithmIdentifier: pula o conteúdo
        pos[0] += algoritmo;
        int tamanho = lerCabecalho(pkcs8, pos, 0x04);   // OCTET STRING com a chave PKCS#1
        return Arrays.copyOfRange(pkcs8, pos[0], pos[0] + tamanho);
    }

    /** Lê tag e tamanho DER; deixa {@code pos} no início do conteúdo e devolve o tamanho. */
    private static int lerCabecalho(byte[] der, int[] pos, int tagEsperada) {
        assertEquals(tagEsperada, der[pos[0]++] & 0xFF);
        int tamanho = der[pos[0]++] & 0xFF;
        if (tamanho >= 0x80) {
            int bytes = tamanho & 0x7F;
            tamanho = 0;
            for (int i = 0; i < bytes; i++) {
                tamanho = (tamanho << 8) | (der[pos[0]++] & 0xFF);
            }
        }
        return tamanho;
    }
}
