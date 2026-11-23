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

import static org.junit.Assert.assertEquals;

import java.awt.image.BufferedImage;

import org.junit.Test;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

/**
 * Teste do gerador de QR Code Pix: a imagem gerada é lida de volta e precisa
 * devolver o mesmo texto.
 */
public class TestQrCodePix {

	/** BR Code fictício, no formato do Pix (dados de demonstração). */
	static final String BR_CODE_DEMONSTRACAO = "00020101021226870014br.gov.bcb.pix2565"
			+ "qrcode.exemplo.invalido/pix/v2/cobv/0123456789abcdef0123456789abcdef"
			+ "5204000053039865406100.235802BR5925BENEFICIARIO DE DEMONSTRA6014BELO HORIZONTE"
			+ "62070503***6304ABCD";

	@Test
	public void seQrCodeGeradoLeOMesmoTexto() throws Exception {

		BufferedImage imagem = QrCodePix.gerarImagem(BR_CODE_DEMONSTRACAO);

		assertEquals(imagem.getWidth(), imagem.getHeight());
		assertEquals(BR_CODE_DEMONSTRACAO, ler(imagem));
	}

	@Test(expected = IllegalArgumentException.class)
	public void seNaoPermiteTextoVazio() {
		QrCodePix.gerarImagem("  ");
	}

	static String ler(BufferedImage imagem) throws Exception {
		int w = imagem.getWidth();
		int h = imagem.getHeight();
		int[] pixels = imagem.getRGB(0, 0, w, h, null, 0, w);
		BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new RGBLuminanceSource(w, h, pixels)));
		return new QRCodeReader().decode(bitmap).getText();
	}
}
