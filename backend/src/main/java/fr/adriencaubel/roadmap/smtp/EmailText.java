package fr.adriencaubel.roadmap.smtp;

/** Text that goes into an email: escaped for its HTML part, kept on one line for its headers. */
public final class EmailText {

	private EmailText() {
	}

	public static String escape(String text) {
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
				.replace("\"", "&quot;").replace("'", "&#39;");
	}

	/** No line breaks in headers: a title can't add headers (e.g. Bcc) to an email. */
	public static String oneLine(String text) {
		return text.replaceAll("[\\r\\n]+", " ").strip();
	}
}
