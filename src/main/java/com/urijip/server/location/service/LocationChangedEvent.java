package com.urijip.server.location.service;

import com.urijip.server.location.dto.response.LocationChangedMessage;

public record LocationChangedEvent(Long familyId, LocationChangedMessage message) {
}
