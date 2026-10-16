package com.urijip.server.photo.service;

import java.util.Arrays;
import java.util.Optional;

// 올릴 수 있는 이미지 형식. 썸네일을 만들려면 서버가 디코딩할 수 있어야 해서 JPEG, PNG만 받는다.
public enum ImageFormat {

	JPEG("jpeg", "jpg", "image/jpeg"),
	PNG("png", "png", "image/png");

	private final String imageIoName;
	private final String extension;
	private final String contentType;

	ImageFormat(String imageIoName, String extension, String contentType) {
		this.imageIoName = imageIoName;
		this.extension = extension;
		this.contentType = contentType;
	}

	public static Optional<ImageFormat> fromImageIoName(String name) {
		return Arrays.stream(values())
				.filter(format -> format.imageIoName.equalsIgnoreCase(name))
				.findFirst();
	}

	public String extension() {
		return extension;
	}

	public String contentType() {
		return contentType;
	}

}
