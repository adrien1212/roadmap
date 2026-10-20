package fr.adriencaubel.roadmap.subscription;

import fr.adriencaubel.roadmap.smtp.EmailText;
import fr.adriencaubel.roadmap.smtp.OutgoingEmail;

import java.util.Map;

/** What Readers receive. Owner text is HTML-escaped; subjects are kept on one line. */
final class EmailContent {

	private EmailContent() {
	}

	static OutgoingEmail confirmation(String to, String roadmapName, String brandName, String confirmUrl) {
		String who = brandName != null ? brandName : roadmapName;
		String subject = oneLine("Confirm your email for " + roadmapName);
		String text = """
				Confirm that you want an email from %s each time an Item of %s ships:

				%s

				If you didn't ask for this, ignore this email: you won't be subscribed.
				""".formatted(who, roadmapName, confirmUrl);
		String html = """
				<p>Confirm that you want an email from %s each time an Item of <strong>%s</strong> ships.</p>
				<p><a href="%s">Confirm my email</a></p>
				<p style="color:#6b7280">If you didn't ask for this, ignore this email: you won't be subscribed.</p>
				""".formatted(escape(who), escape(roadmapName), escape(confirmUrl));
		return new OutgoingEmail(to, subject, text, html);
	}

	/** "Notify me when this ships" for one Item: confirm, or cancel before anything is sent. */
	static OutgoingEmail itemConfirmation(String to, String itemTitle, String roadmapName, String brandName,
			String confirmUrl, String cancelUrl) {
		String who = brandName != null ? brandName : roadmapName;
		String subject = oneLine("Confirm your email to hear when " + itemTitle + " ships");
		String text = """
				Confirm that you want an email from %s when %s ships:

				%s

				If you didn't ask for this, ignore this email: you won't get anything.
				Changed your mind? Cancel: %s
				""".formatted(who, itemTitle, confirmUrl, cancelUrl);
		String html = """
				<p>Confirm that you want an email from %s when <strong>%s</strong> ships.</p>
				<p><a href="%s">Confirm my email</a></p>
				<p style="color:#6b7280">If you didn't ask for this, ignore this email: you won't get anything.
				Changed your mind? <a href="%s" style="color:#6b7280">Cancel</a>.</p>
				""".formatted(escape(who), escape(itemTitle), escape(confirmUrl), escape(cancelUrl));
		return new OutgoingEmail(to, subject, text, html);
	}

	/** The Item fields a Reader sees; plain text from the Owner. */
	record ShippedItem(String title, String description, String categoryName) {
	}

	/** One Reader subscribed to the Roadmap, one Item that became Shipped. */
	static OutgoingEmail shipped(String to, ShippedItem item, String roadmapName, String brandName, String pageUrl,
			String unsubscribeUrl, String oneClickUnsubscribeUrl) {
		String who = brandName != null ? brandName : roadmapName;
		return shipped(to, item, roadmapName, who, pageUrl, unsubscribeUrl, oneClickUnsubscribeUrl,
				"You get this email because you subscribed to " + roadmapName + " by " + who + ".", false);
	}

	/** One Reader waiting for this Item ("Notify me when this ships"), now Shipped. */
	static OutgoingEmail shippedForItem(String to, ShippedItem item, String roadmapName, String brandName, String pageUrl,
			String unsubscribeUrl, String oneClickUnsubscribeUrl) {
		String who = brandName != null ? brandName : roadmapName;
		return shipped(to, item, roadmapName, who, pageUrl, unsubscribeUrl, oneClickUnsubscribeUrl,
				"You get this email because you asked to hear when " + item.title() + " ships.", true);
	}

	private static OutgoingEmail shipped(String to, ShippedItem item, String roadmapName, String who, String pageUrl,
			String unsubscribeUrl, String oneClickUnsubscribeUrl, String reason, boolean forItem) {
		String subject = oneLine("Shipped: " + item.title());
		String description = item.description() == null || item.description().isBlank() ? null : item.description().strip();
		String everyUpdate = "Get every update from " + roadmapName;

		StringBuilder text = new StringBuilder();
		text.append(who).append("\n\n").append(item.title()).append(" is shipped\n");
		if (item.categoryName() != null) {
			text.append(item.categoryName()).append('\n');
		}
		if (description != null) {
			text.append('\n').append(description).append('\n');
		}
		text.append("\nSee the Roadmap: ").append(pageUrl)
				.append("\n\n--\n").append(reason)
				.append("\nUnsubscribe in one click: ").append(unsubscribeUrl);
		if (forItem) {
			text.append("\n").append(everyUpdate).append(": ").append(pageUrl);
		}
		text
				.append("\nMade with Product name\n");

		String category = item.categoryName() == null ? ""
				: " <span style=\"color:#374151;font-size:13px\">" + escape(item.categoryName()) + "</span>";
		String body = description == null ? ""
				: "<p style=\"color:#374151\">" + escape(description).replace("\n", "<br>") + "</p>";
		String html = """
				<div style="font-family:Inter,-apple-system,Segoe UI,Roboto,sans-serif;color:#111111;max-width:560px">
				<p style="font-weight:600">%s</p>
				<h1 style="font-size:22px;margin:16px 0 8px">%s is shipped</h1>
				<p><span style="background:#111111;color:#ffffff;border-radius:9999px;padding:3px 10px;font-size:13px">Shipped</span>%s</p>
				%s
				<p><a href="%s" style="display:inline-block;background:#111111;color:#ffffff;border-radius:8px;padding:10px 18px;text-decoration:none;font-weight:600">See the Roadmap</a></p>
				<hr style="border:0;border-top:1px solid #f3f4f6;margin:24px 0 12px">
				<p style="color:#6b7280;font-size:13px">%s <a href="%s" style="color:#6b7280">Unsubscribe</a> in one click.</p>
				%s<p style="color:#6b7280;font-size:13px">Made with Product name</p>
				</div>
				""".formatted(escape(who), escape(item.title()), category, body, escape(pageUrl), escape(reason),
				escape(unsubscribeUrl), forItem
						? "<p style=\"color:#6b7280;font-size:13px\"><a href=\"" + escape(pageUrl) + "\" style=\"color:#6b7280\">"
								+ escape(everyUpdate) + "</a></p>\n"
						: "");

		// RFC 8058: mail clients show an Unsubscribe button that posts to this URL.
		return new OutgoingEmail(to, subject, text.toString(), html, Map.of(
				"List-Unsubscribe", "<" + oneClickUnsubscribeUrl + ">",
				"List-Unsubscribe-Post", "List-Unsubscribe=One-Click"));
	}

	static String escape(String text) {
		return EmailText.escape(text);
	}

	static String oneLine(String text) {
		return EmailText.oneLine(text);
	}
}
