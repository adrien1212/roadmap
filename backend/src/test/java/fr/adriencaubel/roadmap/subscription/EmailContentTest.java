package fr.adriencaubel.roadmap.subscription;

import fr.adriencaubel.roadmap.smtp.OutgoingEmail;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailContentTest {

	private static final String CONFIRM = "https://example.com/r/abc/confirm?token=t0k3n";

	@Test
	void asksTheReaderToConfirm() {
		OutgoingEmail email = EmailContent.confirmation("jane@example.com", "Product roadmap 2026", "Acme", CONFIRM);

		assertThat(email.to()).isEqualTo("jane@example.com");
		assertThat(email.subject()).isEqualTo("Confirm your email for Product roadmap 2026");
		assertThat(email.html()).contains("href=\"" + CONFIRM + "\"").contains("Acme");
		assertThat(email.text()).contains(CONFIRM).contains("Acme");
	}

	@Test
	void namesTheRoadmapWhenThereIsNoBrand() {
		OutgoingEmail email = EmailContent.confirmation("jane@example.com", "Product roadmap 2026", null, CONFIRM);

		assertThat(email.text()).doesNotContain("null");
		assertThat(email.html()).doesNotContain("null");
	}

	@Test
	void escapesOwnerTextInHtml() {
		OutgoingEmail email = EmailContent.confirmation("jane@example.com", "<script>alert(1)</script>", "A & B", CONFIRM);

		assertThat(email.html()).doesNotContain("<script>").contains("&lt;script&gt;").contains("A &amp; B");
	}

	@Test
	void keepsSubjectsOnOneLine() {
		OutgoingEmail email = EmailContent.confirmation("jane@example.com", "Q3\r\nBcc: x@y.com", null, CONFIRM);

		assertThat(email.subject()).doesNotContain("\r").doesNotContain("\n");
	}

	private static final String PAGE = "https://example.com/r/abc";
	private static final String UNSUBSCRIBE = "https://example.com/r/abc/unsubscribe?token=t0k3n";
	private static final String ONE_CLICK = "https://example.com/api/public/subscriptions/unsubscribe?token=t0k3n";

	private OutgoingEmail shipped(String title, String description, String category) {
		return EmailContent.shipped("jane@example.com", new EmailContent.ShippedItem(title, description, category),
				"Product roadmap 2026", "Acme", PAGE, UNSUBSCRIBE, ONE_CLICK);
	}

	@Test
	void announcesTheShippedItem() {
		OutgoingEmail email = shipped("SSO & SCIM", "Sign in with your IdP.\nProvision people with SCIM.", "Security");

		assertThat(email.to()).isEqualTo("jane@example.com");
		assertThat(email.subject()).isEqualTo("Shipped: SSO & SCIM");
		assertThat(email.text()).contains("SSO & SCIM is shipped").contains("Provision people with SCIM.").contains(PAGE);
		assertThat(email.html()).contains("SSO &amp; SCIM is shipped").contains("Security").contains("href=\"" + PAGE + "\"");
	}

	@Test
	void keepsLineBreaksOfTheDescriptionInHtml() {
		OutgoingEmail email = shipped("SSO", "Line 1\nLine 2", null);

		assertThat(email.html()).contains("Line 1<br>Line 2");
	}

	@Test
	void escapesOwnerTextInTheShippedEmail() {
		OutgoingEmail email = shipped("<img src=x onerror=alert(1)>", "<b>bold</b>", "<i>cat</i>");

		assertThat(email.html()).doesNotContain("<img").doesNotContain("<b>bold").doesNotContain("<i>cat");
	}

	@Test
	void letsTheReaderUnsubscribeInOneClick() {
		OutgoingEmail email = shipped("SSO", null, null);

		assertThat(email.text()).contains(UNSUBSCRIBE).contains("Made with Product name");
		assertThat(email.html()).contains("href=\"" + UNSUBSCRIBE + "\"").contains("subscribed to Product roadmap 2026 by Acme");
		assertThat(email.headers())
				.containsEntry("List-Unsubscribe", "<" + ONE_CLICK + ">")
				.containsEntry("List-Unsubscribe-Post", "List-Unsubscribe=One-Click");
	}

	@Test
	void namesNobodyElse() {
		OutgoingEmail email = shipped("SSO", null, null);

		assertThat(email.text() + email.html()).doesNotContain("@example.com\n").doesNotContain("joe@");
	}

	@Test
	void asksTheReaderToConfirmForOneItem() {
		OutgoingEmail email = EmailContent.itemConfirmation("jane@example.com", "SSO & SCIM", "Product roadmap 2026", "Acme",
				CONFIRM, UNSUBSCRIBE);

		assertThat(email.subject()).isEqualTo("Confirm your email to hear when SSO & SCIM ships");
		assertThat(email.text()).contains(CONFIRM).contains(UNSUBSCRIBE).contains("Acme");
		assertThat(email.html()).contains("href=\"" + CONFIRM + "\"").contains("SSO &amp; SCIM").contains("href=\"" + UNSUBSCRIBE + "\"");
	}

	@Test
	void tellsAReaderWaitingForTheItemWhyTheyGetIt() {
		OutgoingEmail email = EmailContent.shippedForItem("jane@example.com", new EmailContent.ShippedItem("SSO", null, null),
				"Product roadmap 2026", "Acme", PAGE, UNSUBSCRIBE, ONE_CLICK);

		assertThat(email.subject()).isEqualTo("Shipped: SSO");
		assertThat(email.text()).contains("you asked to hear when SSO ships").contains("Get every update from Product roadmap 2026")
				.contains(UNSUBSCRIBE).doesNotContain("you subscribed to");
		assertThat(email.html()).contains("you asked to hear when SSO ships").contains("href=\"" + UNSUBSCRIBE + "\"");
		assertThat(email.headers()).containsEntry("List-Unsubscribe", "<" + ONE_CLICK + ">")
				.containsEntry("List-Unsubscribe-Post", "List-Unsubscribe=One-Click");
	}

	@Test
	void escapesTheItemTitleInTheItemEmails() {
		OutgoingEmail confirmation = EmailContent.itemConfirmation("jane@example.com", "<b>x</b>", "Q3", null, CONFIRM, UNSUBSCRIBE);
		OutgoingEmail shipped = EmailContent.shippedForItem("jane@example.com", new EmailContent.ShippedItem("<b>x</b>", null, null),
				"Q3", null, PAGE, UNSUBSCRIBE, ONE_CLICK);

		assertThat(confirmation.html()).doesNotContain("<b>x");
		assertThat(shipped.html()).doesNotContain("<b>x");
	}

	@Test
	void tellsOnlyTheInboxThatTheAddressIsAlreadySubscribed() {
		OutgoingEmail email = EmailContent.alreadySubscribed("jane@example.com", null, "Product roadmap 2026", "Acme", PAGE,
				UNSUBSCRIBE, ONE_CLICK);

		assertThat(email.to()).isEqualTo("jane@example.com");
		assertThat(email.subject()).isEqualTo("You're already subscribed to Product roadmap 2026");
		assertThat(email.text()).contains(PAGE).contains(UNSUBSCRIBE);
		assertThat(email.html()).contains("href=\"" + UNSUBSCRIBE + "\"");
		assertThat(email.headers()).containsEntry("List-Unsubscribe", "<" + ONE_CLICK + ">");
	}

	@Test
	void namesTheItemWhenTheAddressAlreadyWaitsForIt() {
		OutgoingEmail email = EmailContent.alreadySubscribed("jane@example.com", "SSO & SCIM", "Product roadmap 2026", "Acme",
				PAGE, UNSUBSCRIBE, ONE_CLICK);

		assertThat(email.subject()).isEqualTo("You're already getting an email when SSO & SCIM ships");
		assertThat(email.html()).contains("SSO &amp; SCIM").doesNotContain("SSO & SCIM ships</");
	}
}
