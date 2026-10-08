package fr.ghugo.autofarm;

import fr.ghugo.autofarm.compat.Compat;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

public class AutoFarmClient implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("autofarm");

	private static KeyMapping openMenuKey;
	private static KeyMapping toggleKey;
	private static KeyMapping pauseKey;
	private static KeyMapping statsKey;

	/** Nom de la touche actuellement configurée (tient compte des changements dans Options > Contrôles). */
	public static String menuKey() {
		return keyName(openMenuKey);
	}

	public static String toggleKey() {
		return keyName(toggleKey);
	}

	public static String pauseKey() {
		return keyName(pauseKey);
	}

	public static String statsKey() {
		return keyName(statsKey);
	}

	private static String keyName(KeyMapping key) {
		if (key == null || key.isUnbound()) {
			return "(aucune touche)";
		}
		return key.getTranslatedKeyMessage().getString().toUpperCase(Locale.ROOT);
	}

	@Override
	public void onInitializeClient() {
		AutoFarmConfig.load();

		openMenuKey = Compat.registerKey(new KeyMapping(
				"key.autofarm.open_menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, KeyMapping.Category.MISC));
		toggleKey = Compat.registerKey(new KeyMapping(
				"key.autofarm.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, KeyMapping.Category.MISC));
		pauseKey = Compat.registerKey(new KeyMapping(
				"key.autofarm.pause", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, KeyMapping.Category.MISC));
		statsKey = Compat.registerKey(new KeyMapping(
				"key.autofarm.stats", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_L, KeyMapping.Category.MISC));
		AutoFarmStats.load();
		AutoFarmHud.register();

		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			AutoFarmController.onChatMessage(Minecraft.getInstance(), message);
			if (!overlay) {
				AutoFarmPhone.onChatMessage(Minecraft.getInstance(), message);
				AutoFarmChatAlerts.onChatMessage(Minecraft.getInstance(), message);
			}
		});
		ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
			AutoFarmController.onChatMessage(Minecraft.getInstance(), message);
			AutoFarmPhone.onChatMessage(Minecraft.getInstance(), message);
			AutoFarmChatAlerts.onChatMessage(Minecraft.getInstance(), message);
		});

		ClientTickEvents.START_CLIENT_TICK.register(AutoFarmController::tick);
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			while (openMenuKey.consumeClick()) {
				Compat.setScreen(mc, new AutoFarmScreen(Compat.screen(mc)));
			}
			while (toggleKey.consumeClick()) {
				AutoFarmController.toggle(mc);
			}
			while (pauseKey.consumeClick()) {
				AutoFarmController.togglePause(mc);
			}
			while (statsKey.consumeClick()) {
				Compat.setScreen(mc, new AutoFarmStatsScreen(Compat.screen(mc)));
			}
		});
	}
}
