package fr.ghugo.autofarm;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Alertes personnalisées : si un message du chat contient l'un des textes configurés (ex. « booster de moisson
 * vient d'expirer »), titre en jeu, son de cloche et notification téléphone. Le farm continue.
 */
public final class AutoFarmChatAlerts {
	private static final long COOLDOWN_MS = 5_000;
	private static final Map<String, Long> LAST_ALERT = new HashMap<>();

	private AutoFarmChatAlerts() {
	}

	public static void onChatMessage(Minecraft mc, Component message) {
		if (AutoFarmConfig.chatAlerts.isBlank()) {
			return;
		}
		String text = ChatFormatting.stripFormatting(message.getString());
		if (text == null) {
			return;
		}
		// Ne pas se déclencher sur nos propres messages (si on écrit le texte dans le chat).
		if (mc.getUser() != null && AutoFarmPhone.isOwnMessage(text, mc.getUser().getName())) {
			return;
		}
		String lower = text.toLowerCase(Locale.ROOT);
		for (String part : AutoFarmConfig.chatAlerts.split(";")) {
			String trigger = part.trim().toLowerCase(Locale.ROOT);
			if (trigger.isEmpty() || !lower.contains(trigger)) {
				continue;
			}
			long now = System.currentTimeMillis();
			if (now - LAST_ALERT.getOrDefault(trigger, 0L) < COOLDOWN_MS) {
				return;
			}
			LAST_ALERT.put(trigger, now);
			alert(mc, text);
			return;
		}
	}

	private static void alert(Minecraft mc, String text) {
		mc.gui.setTitle(Component.literal("§e§lAlerte"));
		mc.gui.setSubtitle(Component.literal("§f" + text));
		// Son de cloche, différent de celui du captcha (montée de niveau + bips).
		mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BELL, 1.0F));
		AutoFarmPhone.send("Auto Farm - Alerte chat", text, false);
	}
}
