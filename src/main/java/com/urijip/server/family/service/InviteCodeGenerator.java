package com.urijip.server.family.service;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

@Component
public class InviteCodeGenerator {

	private static final int LENGTH = 6;

	private final SecureRandom random = new SecureRandom();

	public String generate() {
		StringBuilder code = new StringBuilder(LENGTH);
		for (int i = 0; i < LENGTH; i++) {
			code.append((char) ('A' + random.nextInt(26)));
		}
		return code.toString();
	}

}
