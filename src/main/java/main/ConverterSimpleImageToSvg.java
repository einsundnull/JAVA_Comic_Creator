package main;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class ConverterSimpleImageToSvg {

	public static void main(String[] args) throws Exception {
		initiateProcess(new File(""));
	}

	public static void initiateProcess(File imageFile) {
		BufferedImage image;
		try {
			image = ImageIO.read(imageFile);
			List<List<int[]>> contours = extractContours(image, 30); // Threshold = 30
			writeSvg(contours, image.getWidth(), image.getHeight(), imageFile.getParent()+"/"+"output.svg");
			System.out.println("SVG erzeugt.");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} // PNG oder JPG

	}

	static List<List<int[]>> extractContours(BufferedImage image, int threshold) {
		int w = image.getWidth(), h = image.getHeight();
		boolean[][] edge = new boolean[w][h];
		boolean[][] visited = new boolean[w][h];

		// Kanten markieren
		for (int y = 0; y < h - 1; y++) {
			for (int x = 0; x < w - 1; x++) {
				int c = image.getRGB(x, y);
				int r = image.getRGB(x + 1, y);
				int b = image.getRGB(x, y + 1);
				if (colorDiff(c, r) > threshold || colorDiff(c, b) > threshold)
					edge[x][y] = true;
			}
		}

		// Flood-Fill für zusammenhängende Punkte
		List<List<int[]>> contours = new ArrayList<>();
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				if (edge[x][y] && !visited[x][y]) {
					List<int[]> contour = new ArrayList<>();
					Queue<int[]> queue = new LinkedList<>();
					queue.add(new int[] { x, y });
					visited[x][y] = true;

					while (!queue.isEmpty()) {
						int[] p = queue.poll();
						contour.add(p);
						for (int dx = -1; dx <= 1; dx++) {
							for (int dy = -1; dy <= 1; dy++) {
								int nx = p[0] + dx, ny = p[1] + dy;
								if (nx >= 0 && ny >= 0 && nx < w && ny < h && edge[nx][ny] && !visited[nx][ny]) {
									visited[nx][ny] = true;
									queue.add(new int[] { nx, ny });
								}
							}
						}
					}
					contours.add(contour);
				}
			}
		}
		return contours;
	}

	static int colorDiff(int rgb1, int rgb2) {
		int r1 = (rgb1 >> 16) & 0xFF, g1 = (rgb1 >> 8) & 0xFF, b1 = rgb1 & 0xFF;
		int r2 = (rgb2 >> 16) & 0xFF, g2 = (rgb2 >> 8) & 0xFF, b2 = rgb2 & 0xFF;
		return Math.abs(r1 - r2) + Math.abs(g1 - g2) + Math.abs(b1 - b2);
	}

	static void writeSvg(List<List<int[]>> contours, int width, int height, String filename) throws Exception {
		PrintWriter out = new PrintWriter(filename);
		out.println("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
		out.printf("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\" viewBox=\"0 0 %d %d\">\n",
				width, height, width, height);
		out.println("<g fill=\"none\" stroke=\"black\" stroke-width=\"1\">");

		for (List<int[]> contour : contours) {
			if (contour.size() < 5)
				continue; // kleine ignorieren
			StringBuilder path = new StringBuilder("M ");
			for (int[] p : contour) {
				path.append(p[0]).append(" ").append(p[1]).append(" ");
			}
			out.printf("<path d=\"%s\" />\n", path);
		}

		out.println("</g></svg>");
		out.close();
	}
}
