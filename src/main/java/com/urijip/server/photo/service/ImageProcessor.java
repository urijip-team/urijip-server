package com.urijip.server.photo.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

// 올린 파일이 실제 이미지인지 내용으로 확인하고 썸네일을 만든다. 파일 이름과 Content-Type은 믿지 않는다.
@Component
public class ImageProcessor {

	static final int THUMBNAIL_SIZE = 400;

	// 작은 파일이 거대한 이미지로 풀려 메모리를 다 쓰는 것을 막는다. 휴대폰 사진(최대 약 5천만 화소)은 통과한다.
	static final long MAX_PIXELS = 60_000_000L;

	public ProcessedImage process(MultipartFile file) {
		byte[] original = readBytes(file);
		ImageInfo info = readInfo(original);
		return new ProcessedImage(original, createThumbnail(original, info), info.format());
	}

	private byte[] readBytes(MultipartFile file) {
		try {
			return file.getBytes();
		} catch (IOException e) {
			throw new BusinessException(ErrorCode.INVALID_IMAGE);
		}
	}

	private ImageInfo readInfo(byte[] content) {
		try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
			Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
			if (!readers.hasNext()) {
				throw new BusinessException(ErrorCode.INVALID_IMAGE);
			}
			ImageReader reader = readers.next();
			try {
				ImageFormat format = ImageFormat.fromImageIoName(reader.getFormatName())
						.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_IMAGE));
				reader.setInput(input);
				int width = reader.getWidth(0);
				int height = reader.getHeight(0);
				if ((long) width * height > MAX_PIXELS) {
					throw new BusinessException(ErrorCode.INVALID_IMAGE);
				}
				return new ImageInfo(format, Math.max(width, height));
			} finally {
				reader.dispose();
			}
		} catch (IOException e) {
			throw new BusinessException(ErrorCode.INVALID_IMAGE);
		}
	}

	// 긴 변을 THUMBNAIL_SIZE로 줄인다. 이미 작으면 키우지 않는다. 휴대폰 사진의 EXIF 회전 정보도 반영된다.
	private byte[] createThumbnail(byte[] original, ImageInfo info) {
		try {
			var builder = Thumbnails.of(new ByteArrayInputStream(original));
			if (info.longSide() > THUMBNAIL_SIZE) {
				builder.size(THUMBNAIL_SIZE, THUMBNAIL_SIZE);
			} else {
				builder.scale(1.0);
			}
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			builder.outputFormat(info.format().extension()).toOutputStream(output);
			return output.toByteArray();
		} catch (IOException | RuntimeException e) {
			throw new BusinessException(ErrorCode.INVALID_IMAGE);
		}
	}

	private record ImageInfo(ImageFormat format, int longSide) {
	}

	public record ProcessedImage(byte[] original, byte[] thumbnail, ImageFormat format) {
	}

}
