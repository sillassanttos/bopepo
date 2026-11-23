# Bopepo (fork pessoal): regras do projeto

> Este projeto segue as regras de `D:\Desenvolvimento\CLAUDE.md`. Este arquivo só acrescenta regras.

## O que é

- Biblioteca Java para gerar boletos (PDF, linha digitável, código de barras), além de remessa e retorno CNAB.
- Fork em `sillassanttos/bopepo` do `braully/bopepo` (que por sua vez é fork do JRimum Bopepo). Licença Apache 2.0.
- Remotos:
  - `origin`: o fork pessoal;
  - `upstream`: o braully. Para trazer as novidades: `git fetch upstream` + merge na `master`.
- Artefato: `io.github.braully:bpp-cobranca`. A troca do `groupId` ainda não foi decidida.
- Pareceres técnicos em `..\PARECER-JRIMUM-BOPEPO.md` e `..\PARECER-BOPEPO-MELHORIAS.md`.

## Ambiente

- **JDK 25 portátil:** `D:\Desenvolvimento\Ferramentas\jdk-25.0.5`. Use `usar-jdk25.ps1`/`.cmd` só no console. O
  `pom.xml` compila para Java 11.
- **Maven central da máquina:** `D:\Desenvolvimento\apache-maven-3.6.3`. O `mvnw` baixaria o 3.8.8; não é necessário.
- **Compilar e testar:** `mvn -B clean test`.

## Regras do projeto

- **ACBr é referência, não código a portar.** Para bancos e APIs (por exemplo, o Inter), estude como o ACBr
  (`D:\Desenvolvimento\Componentes\ACBr\Fontes\ACBrBoleto`) faz: endpoints, campos, fluxo e cálculos. Confira na
  documentação oficial do banco e escreva o Java do zero, melhorando o que der. Nunca traduza o Pascal linha a linha:
  o ACBr é LGPL e este projeto é Apache 2.0.
- **Credenciais de banco ficam em `local\` ou em variáveis de ambiente, nunca no git.** Isso inclui client_id, client
  secret, certificados `.crt`/`.key`/`.pfx` e senhas.
- **Dados de demonstração.** Em exemplos e testes, use CPF/CNPJ fictícios válidos (ex.: `111.444.777-35`,
  `11.222.333/0001-81`) e nomes "de Demonstração".
- **Boletos de teste na API do banco só no sandbox.** Emitir boleto em produção exige ordem expressa do usuário.
- **Arquivos-fonte:** a maioria é UTF-8 sem BOM, com CRLF, mas há arquivos antigos em Latin-1 (ex.: o comentário de
  `CodigoDeBarras.java`). Mantenha a codificação de cada arquivo.

## Situação (checklist)

- [x] Fork criado e clonado (23/11/2026, base braully 2.0.1, commit `201dfadb`).
- [x] Compila do zero com o JDK 25: **720 testes, 0 falhas, 9 ignorados** (23/11/2026, branch `validacao-base`).
  - O teste `TestCLBancoobCobrancaNaoRegistrada.seNaoPermiteNossoNumeroComTamanhoDiferenteDe7` falhava cerca de 1 vez
    em 10. O gerador de nosso número aleatório não completava com zeros. Corrigido.
- [x] Boleto de exemplo do BB com vencimento em 15/12/2026: fator 1661, correto. Prova em `local\validacao\`.
- [x] iText 5 (AGPL) → **OpenPDF 1.4.2** (23/11/2026, branch `openpdf`).
  - A linha 2.x do OpenPDF exige Java 21; a 1.4.x mantém o Java 11.
  - Diferenças de API isoladas em `PDFUtil.posicoesDoCampo`: `getFieldPositions` devolve `float[]` em grupos de 5.
  - Bookmarks: `getBookmarkList`/`shiftPageNumbersInRange`. Info do documento: `setInfoDictionary`.
  - O auxiliar de testes `excludes/PDFs` foi reescrito com `PdfContentParser`. Os 3 testes de imagem, desligados
    desde o iText 5, voltaram e passam (logo e código de barras idênticos pixel a pixel).
  - Resultado: **720 testes, 0 falhas, 6 ignorados**.
- [x] Versões fixas: log4j 2.26.1 (era a faixa `[2.0.0,2.100)`), guava 33.8.0, commons-io 2.22.0, junit 4.13.2;
  compilador 3.14.0 com `release 11`.
- [ ] JAXB `javax` → `jakarta` (afeta o pacote `texgit`; deixado para uma etapa própria).
- [x] Banco Inter: campo livre (23/11/2026, branch `banco-inter-campo-livre`).
  - Layout: agência (4) + carteira (3) + número da operação (7) + nosso número (10) + DV (módulo 10 sobre agência +
    carteira + nosso número).
  - O número da operação é o informado pelo Inter na contratação: `ParametroBancoInter.NUMERO_DA_OPERACAO`, que não é
    a conta corrente.
  - Referência: linha digitável de um boleto real (fórum do ACBr, tópico 79070), conferida em `TestBoletoBancoInter`.
  - Enum `BANCO_INTER` (o `BANCO_INTEMEDIUM` ficou como sinônimo `@Deprecated`). Logo `077.png` neutro, só com texto.
  - Pela fachada: `parametroBancario(ParametroBancoInter.NUMERO_DA_OPERACAO, "921255")`. O valor em texto era
    descartado (bug antigo, corrigido). A fachada recusa zero à esquerda de propósito (`ConfiguracaoJRimum`).
  - Resultado: **741 testes, 0 falhas, 6 ignorados**. PDF de exemplo em `local\validacao\boleto-inter-2026.pdf`.
- [x] Banco Inter: cliente da API Cobrança v3 (23/11/2026, branch `banco-inter-api`).
  - Fica em `bpp-cobranca-inter\`, projeto Maven próprio que depende do `bpp-cobranca` (decisão do usuário: quem não
    usa o Inter não carrega Jackson). Compile com `mvn -B install` na raiz e depois `mvn -B test` na subpasta.
  - Contrato tirado da especificação OpenAPI oficial do portal developers.inter.co (arquivos
    `redocusaurus/swagger-cobranca-bolepix-yaml.yaml` e `swagger-token-yaml.yaml`, lidos em 23/11/2026).
  - Dependência nova: só `jackson-databind` 2.21.7. HTTP com `java.net.http`, mTLS com o certificado PEM ou PKCS#12.
  - Testes sem rede (respostas gravadas): **53 testes, 0 falhas, 1 ignorado**. O ignorado é o teste no sandbox (`TestIntegracaoSandboxInter`)
    e só roda com as variáveis `INTER_SANDBOX_*`; ainda não rodou, por falta de credenciais.
- [ ] Banco Inter: CNAB 400.
- [x] QR Code Pix (Bolepix) no PDF (23/11/2026, branch `banco-inter-api`).
  - `Boleto.setPixCopiaECola(texto)`. O QR é gerado com o ZXing 3.5.4 (Apache 2.0), porque o OpenPDF 1.4 não gera QR.
  - Campo novo `txtFcQrCodePix` (66 x 66 pt) nos dois templates de boleto, à direita do código de barras: x=345, y=27
    (sem sacador avalista) e y=19 (com sacador avalista). Foi acrescentado no PDF por programa
    (`local\AdicionarCampoQrPix.java`); os `.odt` de origem não têm o campo.
  - Núcleo: **744 testes, 0 falhas, 6 ignorados**. O QR dentro do PDF foi lido de volta com o texto exato.
- [ ] CI no GitHub Actions.
