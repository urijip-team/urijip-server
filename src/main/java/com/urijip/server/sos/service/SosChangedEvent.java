package com.urijip.server.sos.service;

import com.urijip.server.sos.dto.response.SosResponse;

public record SosChangedEvent(Long familyId, SosResponse message) {
}
