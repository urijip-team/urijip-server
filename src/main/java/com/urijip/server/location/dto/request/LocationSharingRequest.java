package com.urijip.server.location.dto.request;

import jakarta.validation.constraints.NotNull;

public record LocationSharingRequest(@NotNull Boolean sharingEnabled) {
}
