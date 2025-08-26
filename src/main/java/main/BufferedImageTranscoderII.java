package main;



import java.awt.image.BufferedImage;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;

public class BufferedImageTranscoderII extends ImageTranscoder {

    private BufferedImage image;

    @Override
    public BufferedImage createImage(int w, int h) {
        return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    }

    @Override
    public void writeImage(BufferedImage img, TranscoderOutput output) {
        this.image = img;
    }

    public BufferedImage getImage() {
        return image;
    }

    public static BufferedImage transcode(String uri, float width, float height) throws TranscoderException {
    	BufferedImageTranscoderII transcoder = new BufferedImageTranscoderII();
        transcoder.addTranscodingHint(KEY_WIDTH, width);
        transcoder.addTranscodingHint(KEY_HEIGHT, height);
        transcoder.transcode(new TranscoderInput(uri), null);
        return transcoder.getImage();
    }
}
