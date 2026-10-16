package com.urijip.server.location.controller;

import java.security.Principal;

import com.urijip.server.location.dto.request.LocationSendRequest;
import com.urijip.server.location.service.LocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class LocationSocketController {

	private final LocationService locationService;

	// principal 이름은 CONNECT에서 정한 회원 id다(StompAuthChannelInterceptor).
	@MessageMapping("/location")
	public void sendLocation(Principal principal, @Valid @Payload LocationSendRequest request) {
		locationService.sendLocation(Long.valueOf(principal.getName()), request);
	}

}
