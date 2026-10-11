package com.urijip.server.family.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.RepeatedTest;

class InviteCodeGeneratorTest {

	private final InviteCodeGenerator generator = new InviteCodeGenerator();

	@RepeatedTest(20)
	void codeHasSixCharacters() {
		assertThat(generator.generate()).hasSize(6);
	}

}
