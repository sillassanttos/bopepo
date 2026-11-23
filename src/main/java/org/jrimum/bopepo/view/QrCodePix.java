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
package org.jrimum.bopepo.view;

import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

/**
 * Gera a imagem do QR Code Pix (BR Code) para o boleto híbrido.
 * <p>
 * Usa correção de erro nível M, que é a recomendada pelo Manual de Padrões
 * para Iniciação do Pix, e margem de 1 módulo: o campo do template já fica
 * numa área em branco do boleto.
 * </p>
 *
 * @since 2.0.2
 */
public final class QrCodePix {

    /** Lado da imagem em pixels; o PDF redimensiona para o tamanho do campo. */
    private static final int LADO_PIXELS = 400;

    private static final int PRETO = 0xFF000000;
    private static final int BRANCO = 0xFFFFFFFF;

    private QrCodePix() {
    }

    /**
     * @param brCode texto do Pix copia e cola
     * @return imagem quadrada, em preto e branco
     * @throws IllegalArgumentException se o texto for vazio ou não couber num QR Code
     */
    public static BufferedImage gerarImagem(String brCode) {

        if (brCode == null || brCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Texto do Pix (BR Code) vazio.");
        }

        Map<EncodeHintType, Object> dicas = new EnumMap<>(EncodeHintType.class);
        dicas.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        dicas.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        dicas.put(EncodeHintType.MARGIN, 1);

        BitMatrix matriz;
        try {
            matriz = new QRCodeWriter().encode(brCode.trim(), BarcodeFormat.QR_CODE, LADO_PIXELS, LADO_PIXELS, dicas);
        } catch (WriterException e) {
            throw new IllegalArgumentException("Não foi possível gerar o QR Code do Pix: " + e.getMessage(), e);
        }

        int largura = matriz.getWidth();
        int altura = matriz.getHeight();
        BufferedImage imagem = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < altura; y++) {
            for (int x = 0; x < largura; x++) {
                imagem.setRGB(x, y, matriz.get(x, y) ? PRETO : BRANCO);
            }
        }

        return imagem;
    }
}
