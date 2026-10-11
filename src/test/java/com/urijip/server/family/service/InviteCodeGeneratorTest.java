package com.urijip.server.family.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class InviteCodeGeneratorTest {

	private final InviteCodeGenerator generator = new InviteCodeGenerator();

	@RepeatedTest(20)
	void codeHasSixCharacters() {
		assertThat(generator.generate()).hasSize(6);
	}

	@RepeatedTest(20)
	void codeUsesOnlyUppercaseLettersAndDigitsThatAreEasyToRead() {
		assertThat(generator.generate()).matches("[A-HJ-NP-Z2-9]{6}");
	}

	@Test
	void codesAreDifferentEachTime() {
		Set<String> codes = Stream.generate(generator::generate)
				.limit(100)
				.collect(Collectors.toSet());

		assertThat(codes).hasSizeGreaterThan(95);
	}

}
