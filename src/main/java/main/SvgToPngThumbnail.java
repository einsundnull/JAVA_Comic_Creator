package main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;

public class SvgToPngThumbnail {

	public static void createThumbnail(File svgFile, File pngFile, float width, float height)
			throws TranscoderException, IOException {
		try (InputStream in = new FileInputStream(svgFile); OutputStream out = new FileOutputStream(pngFile)) {

			TranscoderInput input = new TranscoderInput(in);
			TranscoderOutput output = new TranscoderOutput(out);

			PNGTranscoder transcoder = new PNGTranscoder();
			transcoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH, width);
			transcoder.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, height);

			transcoder.transcode(input, output);
		}
	}
}
