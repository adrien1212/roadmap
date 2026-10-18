package fr.adriencaubel.roadmap.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SignInReturnTest {

	@Test
	void returnsOnlyToAJoinPage() {
		assertThat(SignInReturn.safe("/join/Xk3_-abc")).isEqualTo("/join/Xk3_-abc");
		assertThat(SignInReturn.safe("/join/Xk3_-abc?accept=1")).isEqualTo("/join/Xk3_-abc?accept=1");
	}

	@Test
	void ignoresAnythingElse() {
		assertThat(SignInReturn.safe(null)).isNull();
		assertThat(SignInReturn.safe("https://evil.example/join/abc")).isNull();
		assertThat(SignInReturn.safe("//evil.example/join/abc")).isNull();
		assertThat(SignInReturn.safe("/join/abc/../../roadmaps")).isNull();
		assertThat(SignInReturn.safe("/join/abc?next=https://evil.example")).isNull();
		assertThat(SignInReturn.safe("/roadmaps")).isNull();
	}
}
