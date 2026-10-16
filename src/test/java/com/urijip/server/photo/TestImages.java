package com.urijip.server.photo;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import javax.imageio.ImageIO;

import org.springframework.mock.web.MockMultipartFile;

public final class TestImages {

	private TestImages() {
	}

	public static byte[] image(String format, int width, int height) {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		try {
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			ImageIO.write(image, format, output);
			return output.toByteArray();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static MockMultipartFile jpeg(int width, int height) {
		return new MockMultipartFile("files", "photo.jpg", "image/jpeg", image("jpg", width, height));
	}

	public static MockMultipartFile file(byte[] content) {
		return new MockMultipartFile("files", "photo.jpg", "image/jpeg", content);
	}

}
