package com.urijip.server.family.dto.request;

import jakarta.validation.constraints.NotBlank;

public record FamilyJoinRequest(@NotBlank String inviteCode) {
}
