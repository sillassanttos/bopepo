Bopepo Changelog
====================================================

2.0.2-SNAPSHOT (fork sillassanttos/bopepo)
----------------------------------------------------------------------

**Improvement**

 * Geração de PDF com o OpenPDF 1.4.2 (LGPL/MPL) no lugar do iText 5.5 (AGPL),
   permitindo o uso da biblioteca em sistemas de código fechado.
 * Versões fixas de dependências: log4j 2.26.1 (antes, faixa [2.0.0,2.100)),
   guava 33.8.0, commons-io 2.22.0, junit 4.13.2 (testes).
 * Compilação com `release 11`, para garantir só a API do Java 11 mesmo em JDK mais novo.

**Fix**

 * Testes de imagens do PDF (logos e código de barras) reescritos para o OpenPDF
   e reativados; estavam desligados desde a troca para o iText 5.
 * Gerador de nosso número aleatório dos testes de campo livre completava sem zeros
   à esquerda e deixava um teste do Bancoob instável.
 * Exemplos do README que não rodavam (CPF/CNPJ inválidos, nosso número do BB).


0.2.3 (2014/02/01) main release GitHub
----------------------------------------------------------------------

**Improvement**

 * #1: Mudar passagem de parâmetros bancários de string para enum
 * #2: Colocar complemento do endereço do sacado como campo padrão no boleto
 * #4: Novo Banco: BRB
 * #5: Novo enum com todos os campos padrões do boleto
 * #6: Atualizar templates dos boletos em função da nova nomenclatura BACEN
 * #7: Substituir funções de texto do utilix por similar em texgit
 * #8: Inserir meta-dados realcionados ao PDF
 * #9: Campos formatados em função do padrão de cada Banco
 * #11: Novo Banco: CECRED


0.2.3-SNAPSHOT (2011-02-09) TracSVN
----------------------------------------------------------------------

**Fix**

 * #25: Implementação BANCO SANTANDER S.A. - Correção do uso de dígitos verificadores.
 * #13: Banco Real - Mudança do número do documento pelo nosso número

**Improvement**

 * #54: Implementação Campo Livre SICOB - CEF
 * #58: Bancoob
 * Novas formas de processamento em lote
 * Novas formas para definição de template
 * Opção de definição de compressão do PDF
 * Classe para geração isolada de código de barras
 * Classe para geração de pdfs estilo relatório
 

0.3-Litio-GeracaoGuia-VersaoAvaliacao-2011-04-14 (2011/04/14) TracSVN
----------------------------------------------------------------------

 * Ajustes construtores de OrgaoRecebedor e Arrecadacao, que sofreram ajustes básicos nos parâmetros.   
 * Novo exemplo MinhaPrimeiraGuiaASerPagaEmVariosBancos.


0.2.3-SNAPSHOT (2011-02-09) TracSVN
----------------------------------------------------------------------

**Improvement**

 * Novo Banco: Banco do Nordeste
 * Novo Boleto: Banco do Brasil convênio 7 dígitos nosso número 17 dígitos.


0.3-Litio-GeracaoGuia-VersaoAvaliacao-2010-09-17 (2010/09/17) TracSVN
----------------------------------------------------------------------

0.3-Litio-GeracaoGuia-VersaoAvaliacao-2010-09-09 (2010/09/09) TracSVN
----------------------------------------------------------------------

0.3-Litio-GeracaoGuia-VersaoAvaliacao-2010-06-10 (2010/06/10) TracSVN
----------------------------------------------------------------------

0.2.2 (2009/10/18) main release TracSVN
----------------------------------------------------------------------

0.2.1-INC2X (2008/11/13) incubation TracSVN
----------------------------------------------------------------------

0.2.1-INC2 (2008/07/15) incubation TracSVN
----------------------------------------------------------------------
   
0.2.1-INC (2008/06/15) incubation TracSVN
----------------------------------------------------------------------
  
Domkee Changelog
=============================

0.2.3 (2014/02/01) suport bopepo-0.2.3
---------------------------------------
Utilix Changelog
Texgit Changelog
=============================

0.2.3 (2014/02/01) suport bopepo-0.2.3
---------------------------------------

**Improvement**

 * #2: Remover classes relacionadas a LineOfFields
 * #1: Implementação de testes para funcionalidade LineOfFields
 
 
0.2.0-SNAPSHOT (2010/12/07) incubation 
