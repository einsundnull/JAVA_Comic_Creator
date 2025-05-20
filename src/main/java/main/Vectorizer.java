package main;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.StringWriter;

import de.quasimondo.potrace.*;
import de.quasimondo.potrace.trace.*;

public class Vectorizer {

	public static String rasterToSVG(BufferedImage inputImage) throws IOException {
		BufferedImage binary = binarize(inputImage, 0.45);

		PotraceBitmap pb = new PotraceBitmap(binary);

		Potrace potrace = new Potrace();
		potrace.setTurnPolicy(TurnPolicy.MINORITY);
		potrace.setTurdSize(2);
		potrace.setAlphaMax(1.0 - 0.2);
		potrace.setOptiCurve(true);
		potrace.setBlackLevel(0.5);

		PotracePath[] paths = potrace.trace(pb);

		StringWriter writer = new StringWriter();
		potrace.toSVG(paths, binary.getWidth(), binary.getHeight(), writer);
		return writer.toString();
	}

	private static BufferedImage binarize(BufferedImage src, double threshold) {
		int width = src.getWidth(), height = src.getHeight();
		BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
		WritableRaster raster = out.getRaster();

		for (int y = 0; y < height; y++)
			for (int x = 0; x < width; x++) {
				int rgb = src.getRGB(x, y);
				int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
				double luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b;
				int value = (luminance / 255.0 < threshold) ? 0 : 1;
				raster.setSample(x, y, 0, value);
			}

		return out;
	}
}
