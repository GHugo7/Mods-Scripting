package fr.ghugo.autofarm;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Notifications sur le téléphone via ntfy (https://ntfy.sh) : captcha et mentions du pseudo dans le chat.
 */
public final class AutoFarmPhone {
	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
	/** Séparateur entre l'auteur et le message : « <Pseudo> msg », « [Rang] Pseudo: msg », « Pseudo » msg »... */
	private static final Pattern SENDER_SEPARATOR = Pattern.compile("(?<![-=])[:>»]\\s");
	/** Messages privés courants (« Bob -> moi », « Bob whispers to you »...). */
	private static final Pattern PRIVATE_MESSAGE = Pattern.compile(
			"(->|→|➡)\\s*(moi|me|you|vous)\\b|whispers to you|te chuchote|vous chuchote", Pattern.CASE_INSENSITIVE);
	private static final long MENTION_COOLDOWN_MS = 5_000;
	private static long lastMentionAt;

	private AutoFarmPhone() {
	}

	public static boolean isEnabled() {
		return !AutoFarmConfig.phoneTopic.isBlank();
	}

	/** Envoie une notification (en arrière-plan) si un topic ntfy est configuré. */
	public static void send(String title, String message, boolean urgent) {
		if (!isEnabled()) {
			return;
		}
		String topic = AutoFarmConfig.phoneTopic.trim();
		String url = topic.startsWith("http://") || topic.startsWith("https://") ? topic : "https://ntfy.sh/" + topic;
		HttpRequest request;
		try {
			request = HttpRequest.newBuilder(URI.create(url))
					.timeout(Duration.ofSeconds(15))
					// Les en-têtes HTTP doivent rester en ASCII : accents retirés du titre.
					.header("Title", ascii(title))
					.header("Priority", urgent ? "urgent" : "high")
					.header("Tags", urgent ? "rotating_light" : "speech_balloon")
					.POST(HttpRequest.BodyPublishers.ofString(message, StandardCharsets.UTF_8))
					.build();
		} catch (IllegalArgumentException e) {
			AutoFarmClient.LOGGER.warn("Adresse ntfy invalide : {}", url, e);
			return;
		}
		HTTP.sendAsync(request, HttpResponse.BodyHandlers.discarding()).whenComplete((response, error) -> {
			if (error != null) {
				AutoFarmClient.LOGGER.warn("Échec de l'envoi de la notification téléphone", error);
			} else if (response.statusCode() >= 300) {
				AutoFarmClient.LOGGER.warn("ntfy a répondu {}", response.statusCode());
			}
		});
	}

	/** Prévient sur le téléphone si quelqu'un d'autre mentionne notre pseudo ou nous écrit en privé. */
	public static void onChatMessage(Minecraft mc, Component message) {
		if (!isEnabled() || !AutoFarmConfig.phoneOnMention || mc.getUser() == null) {
			return;
		}
		String text = ChatFormatting.stripFormatting(message.getString());
		if (text == null || !isMentionOf(text, mc.getUser().getName())) {
			return;
		}
		long now = System.currentTimeMillis();
		if (now - lastMentionAt < MENTION_COOLDOWN_MS) {
			return;
		}
		lastMentionAt = now;
		send("Auto Farm - Mention dans le chat", text, false);
	}

	/**
	 * Vrai si le message vient d'un autre joueur et contient notre pseudo (ou est un message privé).
	 * L'auteur est la partie avant le premier séparateur (« : », « > », « » ») : si notre pseudo y figure,
	 * c'est notre propre message et on l'ignore.
	 */
	static boolean isMentionOf(String text, String me) {
		if (me == null || me.isBlank()) {
			return false;
		}
		if (PRIVATE_MESSAGE.matcher(text).find()) {
			// Message privé reçu (« [Bob -> moi] ... ») : nos messages envoyés sont « [moi -> Bob] », non concernés.
			return true;
		}
		Matcher sep = SENDER_SEPARATOR.matcher(text);
		if (!sep.find()) {
			// Pas de format « auteur : message » : message du serveur, pas d'un joueur.
			return false;
		}
		String sender = text.substring(0, sep.start());
		String content = text.substring(sep.end());
		Pattern name = Pattern.compile("(?<![A-Za-z0-9_])" + Pattern.quote(me) + "(?![A-Za-z0-9_])", Pattern.CASE_INSENSITIVE);
		if (name.matcher(sender).find()) {
			return false;
		}
		return name.matcher(content).find();
	}

	private static String ascii(String s) {
		String normalized = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD);
		return normalized.replaceAll("[^\\x20-\\x7E]", "");
	}
}
