package com.urijip.server.member.dto.request;

import com.urijip.server.member.entity.UserStatus;
import jakarta.validation.constraints.NotNull;

public record AdminUserStatusRequest(@NotNull UserStatus status) {
}
