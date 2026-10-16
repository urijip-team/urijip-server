package com.urijip.server.family.dto.request;

import com.urijip.server.family.entity.FamilyStatus;
import jakarta.validation.constraints.NotNull;

public record AdminFamilyStatusRequest(@NotNull FamilyStatus status) {
}
