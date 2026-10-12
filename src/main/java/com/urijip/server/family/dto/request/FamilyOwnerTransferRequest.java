package com.urijip.server.family.dto.request;

import jakarta.validation.constraints.NotNull;

public record FamilyOwnerTransferRequest(@NotNull Long userId) {
}
