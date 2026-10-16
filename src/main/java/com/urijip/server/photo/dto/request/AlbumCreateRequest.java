package com.urijip.server.photo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AlbumCreateRequest(@NotBlank @Size(max = 50, message = "50자 이하여야 합니다") String title) {
}
