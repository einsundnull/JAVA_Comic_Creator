package main;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import dev.brachtendorf.jimagehash.hash.Hash;
import dev.brachtendorf.jimagehash.hashAlgorithms.PerceptiveHash;

public class VisualDuplicateFinder {
	static final PerceptiveHash phash = new PerceptiveHash(64);
//	static final PerceptiveHash phash = new PerceptiveHash(64);

	public static void processImagesHashOnly(Path dir, Path targetDir) throws Exception {
		List<Path> files = Files.list(dir).filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".jpg"))
				.collect(Collectors.toList());

		Set<String> seen = new HashSet<>();
		for (Path file1 : files) {
			String hash1 = getImageHash(file1);
			if (seen.contains(hash1))
				continue;
			for (Path file2 : files) {
				if (file1.equals(file2))
					continue;
				String hash2 = getImageHash(file2);
				if (hash1.equals(hash2)) {
					Path longer = file1.getFileName().toString().length() > file2.getFileName().toString().length()
							? file1
							: file2;
					Files.copy(longer, targetDir.resolve(longer.getFileName()), StandardCopyOption.REPLACE_EXISTING);
					break;
				}
			}
			seen.add(hash1);
		}
	}

	public static void processImagesPixelCompare(Path dir, Path targetDir) throws Exception {
		List<Path> files = Files.list(dir).filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".jpg"))
				.collect(Collectors.toList());

		for (int i = 0; i < files.size(); i++) {
			for (int j = i + 1; j < files.size(); j++) {
				if (isVisuallyIdentical(files.get(i), files.get(j))) {
					Path longer = files.get(i).getFileName().toString().length() > files.get(j).getFileName().toString()
							.length() ? files.get(i) : files.get(j);
					Files.copy(longer, targetDir.resolve(longer.getFileName()), StandardCopyOption.REPLACE_EXISTING);
				}
			}
		}
	}

	public static String getImageHash(Path path) throws IOException {
		BufferedImage img = ImageIO.read(path.toFile());
		Hash hash = phash.hash(img);
		return hash.getHashValue().toString(16);
	}

	public static boolean isVisuallyIdentical(Path file1, Path file2) throws IOException {
		BufferedImage img1 = ImageIO.read(file1.toFile());
		BufferedImage img2 = ImageIO.read(file2.toFile());
		if (img1 == null || img2 == null)
			return false;
		if (img1.getWidth() != img2.getWidth() || img1.getHeight() != img2.getHeight())
			return false;
		for (int y = 0; y < img1.getHeight(); y++)
			for (int x = 0; x < img1.getWidth(); x++)
				if (img1.getRGB(x, y) != img2.getRGB(x, y))
					return false;
		return true;
	}
}
