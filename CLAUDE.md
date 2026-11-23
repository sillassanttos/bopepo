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
- [ ] iText 5 (AGPL) → OpenPDF.
- [ ] Versões fixas (log4j com faixa aberta), JAXB → Jakarta.
- [ ] Banco Inter: campo livre, CNAB 400 e cliente da API Cobrança v3.
- [ ] QR Code Pix (Bolepix) no PDF.
- [ ] CI no GitHub Actions.
