/* 
 * Copyright 2014 JRimum Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 * Created at: 12/01/2014 - 17:09:13
 *
 * ================================================================================
 *
 * Direitos autorais 2014 JRimum Project
 *
 * Licenciado sob a Licença Apache, Versão 2.0 ("LICENÇA"); você não pode 
 * usar esse arquivo exceto em conformidade com a esta LICENÇA. Você pode obter uma 
 * cópia desta LICENÇA em http://www.apache.org/licenses/LICENSE-2.0 A menos que 
 * haja exigência legal ou acordo por escrito, a distribuição de software sob esta 
 * LICENÇA se dará “COMO ESTÁ”, SEM GARANTIAS OU CONDIÇÕES DE QUALQUER TIPO, sejam 
 * expressas ou tácitas. Veja a LICENÇA para a redação específica a reger permissões 
 * e limitações sob esta LICENÇA.
 * 
 * Criado em: 12/01/2014 - 17:09:13 
 * 
 */
package org.jrimum.bopepo.excludes;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.lowagie.text.pdf.PRIndirectReference;
import com.lowagie.text.pdf.PRStream;
import com.lowagie.text.pdf.PRTokeniser;
import com.lowagie.text.pdf.PdfContentParser;
import com.lowagie.text.pdf.PdfDictionary;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfNumber;
import com.lowagie.text.pdf.PdfObject;
import com.lowagie.text.pdf.PdfReader;

/**
 * Utilitário de testes para extrair as imagens de um PDF.
 * <p>
 * Reescrito para o OpenPDF, que não tem o parser de conteúdo do iText 5
 * ({@code PdfReaderContentParser}): interpreta o conteúdo de cada página com o
 * {@link PdfContentParser} e registra cada imagem desenhada (operador
 * {@code Do}), inclusive dentro de XObjects de formulário. Cada imagem é
 * identificada pela referência do objeto no PDF, de modo que a mesma imagem
 * desenhada duas vezes fica numa só chave com dois elementos.
 * </p>
 *
 * @author <a href="http://gilmatryx.googlepages.com/">Gilmar P.S.L.</a>
 */
public class PDFs {

    public static Map<String, Collection<BufferedImage>> getImages(byte[] pdf) throws IOException {

        Multimap<String, BufferedImage> imagens = ArrayListMultimap.create();
        PdfReader reader = new PdfReader(pdf);

        try {
            for (int pagina = 1; pagina <= reader.getNumberOfPages(); pagina++) {
                PdfDictionary recursos = reader.getPageN(pagina).getAsDict(PdfName.RESOURCES);
                coletarImagens(reader.getPageContent(pagina), recursos, imagens, new HashSet<String>());
            }
        } finally {
            reader.close();
        }

        return imagens.asMap();
    }

    /**
     * Interpreta o conteúdo e, a cada operador {@code Do}, registra a imagem
     * desenhada; em XObjects de formulário, desce no conteúdo deles.
     */
    private static void coletarImagens(byte[] conteudo, PdfDictionary recursos,
            Multimap<String, BufferedImage> imagens, Set<String> formulariosEmAndamento) throws IOException {

        if (conteudo == null || recursos == null) {
            return;
        }

        PdfDictionary xObjects = recursos.getAsDict(PdfName.XOBJECT);

        if (xObjects == null) {
            return;
        }

        PdfContentParser parser = new PdfContentParser(new PRTokeniser(conteudo));
        List<PdfObject> operandos = new ArrayList<>();

        while (!parser.parse(operandos).isEmpty()) {

            PdfObject operador = operandos.get(operandos.size() - 1);

            if (!"Do".equals(operador.toString()) || operandos.size() < 2
                    || !(operandos.get(0) instanceof PdfName)) {
                continue;
            }

            PdfName nome = (PdfName) operandos.get(0);
            PdfObject referencia = xObjects.get(nome);
            PdfObject objeto = PdfReader.getPdfObject(referencia);

            if (!(objeto instanceof PRStream)) {
                continue;
            }

            PRStream stream = (PRStream) objeto;
            PdfName subtipo = stream.getAsName(PdfName.SUBTYPE);
            String chave = chaveDoObjeto(referencia, nome);

            if (PdfName.IMAGE.equals(subtipo)) {
                imagens.put(chave, decodificar(stream));
            } else if (PdfName.FORM.equals(subtipo) && formulariosEmAndamento.add(chave)) {
                PdfDictionary recursosDoForm = stream.getAsDict(PdfName.RESOURCES);
                coletarImagens(PdfReader.getStreamBytes(stream),
                        recursosDoForm != null ? recursosDoForm : recursos, imagens, formulariosEmAndamento);
                formulariosEmAndamento.remove(chave);
            }
        }
    }

    private static String chaveDoObjeto(PdfObject referencia, PdfName nome) {
        if (referencia instanceof PRIndirectReference) {
            PRIndirectReference ref = (PRIndirectReference) referencia;
            return String.format("Ref-%s-%s", ref.getNumber(), ref.getGeneration());
        }
        return "Direto-" + nome;
    }

    /**
     * Decodifica as imagens que o Bopepo gera: JPEG (DCTDecode) e mapas de
     * pixels de 8 bits em RGB ou tons de cinza (FlateDecode). Para outros
     * formatos devolve {@code null}: a imagem continua sendo contada.
     */
    private static BufferedImage decodificar(PRStream stream) throws IOException {

        PdfObject filtro = PdfReader.getPdfObject(stream.get(PdfName.FILTER));

        if (PdfName.DCTDECODE.equals(filtro)) {
            return ImageIO.read(new ByteArrayInputStream(PdfReader.getStreamBytesRaw(stream)));
        }

        PdfNumber largura = stream.getAsNumber(PdfName.WIDTH);
        PdfNumber altura = stream.getAsNumber(PdfName.HEIGHT);
        PdfNumber bits = stream.getAsNumber(PdfName.BITSPERCOMPONENT);
        PdfObject espacoDeCor = PdfReader.getPdfObject(stream.get(PdfName.COLORSPACE));

        if (largura == null || altura == null || bits == null || bits.intValue() != 8) {
            return null;
        }

        int componentes;
        if (PdfName.DEVICERGB.equals(espacoDeCor)) {
            componentes = 3;
        } else if (PdfName.DEVICEGRAY.equals(espacoDeCor)) {
            componentes = 1;
        } else {
            return null;
        }

        byte[] pixels = PdfReader.getStreamBytes(stream);
        int w = largura.intValue();
        int h = altura.intValue();

        if (pixels.length < w * h * componentes) {
            return null;
        }

        BufferedImage imagem = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int i = (y * w + x) * componentes;
                int r = pixels[i] & 0xFF;
                int g = componentes == 3 ? pixels[i + 1] & 0xFF : r;
                int b = componentes == 3 ? pixels[i + 2] & 0xFF : r;
                imagem.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }

        return imagem;
    }
}
