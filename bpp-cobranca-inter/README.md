# bpp-cobranca-inter

Cliente Java da **API Cobrança v3 (boleto com Pix)** do Banco Inter, para o Bopepo.

Fica num artefato separado do `bpp-cobranca`. Assim, quem não usa o Inter não carrega Jackson nem cliente HTTP.

O contrato segue a especificação OpenAPI oficial do portal developers.inter.co:

- Autenticação OAuth: `/oauth/v2/token`;
- API Cobrança: `/cobranca/v3`.

O ACBr serviu só de referência; o código foi escrito do zero.

## O que tem

| Recurso | Método |
|---|---|
| Emitir (assíncrono) | `emitir(NovaCobrancaInter)` → `codigoSolicitacao` |
| Emitir e esperar o boleto ficar pronto | `emitirEAguardar(nova, Duration)` |
| Consultar | `consultar(codigoSolicitacao)` |
| Pesquisar (uma página ou todas) | `listar(FiltroCobrancasInter)`, `listarTodas(filtro, limitePaginas)` |
| Cancelar | `cancelar(codigoSolicitacao, motivo)` |
| PDF do próprio Inter | `pdf(codigoSolicitacao)` |
| Webhook | `cadastrarWebhook(url)`, `consultarWebhook()`, `excluirWebhook()` |
| Ler a notificação recebida no webhook | `ClienteInterCobranca.lerCallbacks(corpo)` |
| Simular pagamento (só no sandbox) | `pagarNoSandbox(codigoSolicitacao, BOLETO ou PIX)` |
| PDF desenhado pelo Bopepo, com QR Code Pix | `BoletoInter.paraBoleto(cobranca, beneficiario, conta)` |

O que este cliente faz além de repassar as chamadas:

- **Token em cache.** O mesmo token é reaproveitado até 60 s antes de expirar, porque o Inter aceita só 5 pedidos de token por minuto. Se o banco responder 401, o cliente renova o token e repete a chamada uma vez.
- **Limites conferidos antes de chamar o banco.** Seu número com até 15 caracteres, valor de 2,50 a 99.999.999,99, até 60 dias de agenda, 5 linhas de mensagem com até 78 caracteres e pagador completo. A UF e o CEP também são conferidos, e o tipo de pessoa é deduzido do CPF ou do CNPJ.
- **Erros legíveis.** `InterApiException` traz o status, o título, o detalhe e cada violação (`propriedade: razão`).
- **Situações novas não quebram a leitura.** Uma situação que a biblioteca ainda não conhece vira `DESCONHECIDA`.
- **Boleto conferido antes do PDF.** `BoletoInter` tira agência, carteira, operação e nosso número do código de barras do banco. Ele confere o DV e compara o código de barras montado pelo Bopepo com o do Inter; se diferirem, o boleto não é gerado.
- **Certificado PEM lido direto.** O `.crt` e o `.key` que o Inter entrega são lidos sem conversão (chave PKCS#8 ou PKCS#1). O PKCS#12 também é aceito. Use `venceEm(Duration)` para avisar antes do vencimento anual do certificado.

## Uso

```java
CertificadoInter cert = CertificadoInter.dePem(Paths.get("local/inter/app.crt"), Paths.get("local/inter/app.key"));

ClienteInterCobranca inter = ClienteInterCobranca.builder(AmbienteInter.SANDBOX)
        .credenciais(System.getenv("INTER_CLIENT_ID"), System.getenv("INTER_CLIENT_SECRET"))
        .certificado(cert)
        .build();

NovaCobrancaInter nova = new NovaCobrancaInter("PED-1001", new BigDecimal("150.00"), LocalDate.of(2026, 12, 15))
        .pagador(PessoaInter.de("111.444.777-35", "Pagador de Demonstração")
                .endereco("Rua Exemplo", "100", "Centro", "Belo Horizonte", "MG", "30110-000"))
        .multa(EncargoInter.multaPercentual(new BigDecimal("2")))
        .mora(EncargoInter.moraTaxaMensal(new BigDecimal("1")));

CobrancaInter cobranca = inter.emitirEAguardar(nova, Duration.ofSeconds(30));

// PDF do Bopepo, com QR Code Pix ao lado do código de barras
Boleto boleto = BoletoInter.paraBoleto(cobranca,
        new Cedente("Minha Empresa", "11.222.333/0001-81"), new NumeroDaConta(123456789, "0"));
new BoletoViewer(boleto).getPdfAsFile("boleto.pdf");

// ou o PDF do próprio Inter
byte[] pdfDoBanco = inter.pdf(cobranca.getCodigoSolicitacao());
```

## Compilar e testar

O módulo depende do `bpp-cobranca` da pasta de cima. Instale o `bpp-cobranca` no repositório local primeiro:

```powershell
mvn -B install
```

```powershell
cd bpp-cobranca-inter
mvn -B test
```

Os testes não acessam a rede. Eles usam respostas gravadas no formato da especificação (`src/test/resources/inter`). O certificado dos testes é gerado na hora com o `keytool` do JDK.

O teste de ponta a ponta `TestIntegracaoSandboxInter` só roda no **sandbox** e só quando existem estas variáveis de ambiente:

- `INTER_SANDBOX_CLIENT_ID`;
- `INTER_SANDBOX_CLIENT_SECRET`;
- `INTER_SANDBOX_CRT`;
- `INTER_SANDBOX_KEY`.

Ele emite uma cobrança de R$ 2,50, confere o boleto, baixa o PDF e cancela a cobrança.

## Segurança

- Client ID, client secret, certificado e chave **nunca entram no git**. Guarde-os fora do repositório (por exemplo, em `local\inter\`) ou num cofre, e passe por variável de ambiente.
- `pagarNoSandbox` recusa produção sem chamar o banco.
- O `codigoSolicitacao` é conferido antes de ir para a URL.

## Fora do escopo por enquanto

Ainda não implementados:

- edição de cobrança (`PATCH /cobrancas/{codigo}`);
- sumário (`/cobrancas/sumario`);
- consulta e reenvio de callbacks;
- validação da assinatura do webhook (a especificação não descreve uma).
