package com.urijip.server.family.service;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

@Component
public class InviteCodeGenerator {

	// 서로 헷갈리는 0·O, 1·I는 뺀다.
	private static final String CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
	private static final int LENGTH = 6;

	private final SecureRandom random = new SecureRandom();

	public String generate() {
		StringBuilder code = new StringBuilder(LENGTH);
		for (int i = 0; i < LENGTH; i++) {
			code.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
		}
		return code.toString();
	}

}
