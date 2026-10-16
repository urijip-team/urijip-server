package com.urijip.server.photo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.CRC32;

import javax.imageio.ImageIO;

import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.photo.TestImages;
import com.urijip.server.photo.service.ImageProcessor.ProcessedImage;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class ImageProcessorTest {

	private final ImageProcessor imageProcessor = new ImageProcessor();

	@Test
	void jpegGetsThumbnailWithLongSide400() throws IOException {
		ProcessedImage processed = imageProcessor.process(TestImages.jpeg(1200, 800));

		assertThat(processed.format()).isEqualTo(ImageFormat.JPEG);
		BufferedImage thumbnail = read(processed.thumbnail());
		assertThat(thumbnail.getWidth()).isEqualTo(400);
		assertThat(thumbnail.getHeight()).isEqualTo(267);
		assertThat(ImageIO.getImageReaders(ImageIO.createImageInputStream(
				new ByteArrayInputStream(processed.thumbnail()))).next().getFormatName()).isEqualToIgnoringCase("jpeg");
	}

	@Test
	void originalIsKeptAsUploaded() throws IOException {
		MockMultipartFile file = TestImages.jpeg(1200, 800);

		assertThat(imageProcessor.process(file).original()).isEqualTo(TestImages.jpeg(1200, 800).getBytes());
	}

	@Test
	void smallPngIsNotEnlarged() throws IOException {
		ProcessedImage processed = imageProcessor.process(new MockMultipartFile("files", "a.png", "image/png",
				TestImages.image("png", 100, 300)));

		assertThat(processed.format()).isEqualTo(ImageFormat.PNG);
		BufferedImage thumbnail = read(processed.thumbnail());
		assertThat(thumbnail.getWidth()).isEqualTo(100);
		assertThat(thumbnail.getHeight()).isEqualTo(300);
	}

	@Test
	void formatIsDetectedFromContentNotFileName() {
		MockMultipartFile pngNamedJpg = new MockMultipartFile("files", "photo.jpg", "image/jpeg",
				TestImages.image("png", 10, 10));

		assertThat(imageProcessor.process(pngNamedJpg).format()).isEqualTo(ImageFormat.PNG);
	}

	@Test
	void rejectsNonImage() {
		assertInvalid("hello".getBytes(StandardCharsets.UTF_8));
	}

	@Test
	void rejectsUnsupportedFormat() {
		assertInvalid(TestImages.image("gif", 10, 10));
	}

	@Test
	void rejectsBrokenImage() {
		byte[] jpeg = TestImages.image("jpg", 500, 500);
		byte[] truncated = Arrays.copyOf(jpeg, 200);

		assertInvalid(truncated);
	}

	@Test
	void rejectsTooManyPixelsBeforeDecoding() throws IOException {
		assertInvalid(pngHeaderOnly(10_000, 10_000));
	}

	private void assertInvalid(byte[] content) {
		assertThatThrownBy(() -> imageProcessor.process(TestImages.file(content)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.INVALID_IMAGE);
	}

	private BufferedImage read(byte[] content) throws IOException {
		return ImageIO.read(new ByteArrayInputStream(content));
	}

	// 크기 정보(IHDR)만 있는 PNG. 디코딩 전에 화소 수로 거절하는지 확인한다.
	private byte[] pngHeaderOnly(int width, int height) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		DataOutputStream data = new DataOutputStream(output);
		data.write(new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n'});

		ByteArrayOutputStream chunk = new ByteArrayOutputStream();
		DataOutputStream chunkData = new DataOutputStream(chunk);
		chunkData.writeBytes("IHDR");
		chunkData.writeInt(width);
		chunkData.writeInt(height);
		chunkData.write(new byte[] {8, 2, 0, 0, 0});
		byte[] chunkBytes = chunk.toByteArray();

		CRC32 crc = new CRC32();
		crc.update(chunkBytes);
		data.writeInt(13);
		data.write(chunkBytes);
		data.writeInt((int) crc.getValue());
		return output.toByteArray();
	}

}
