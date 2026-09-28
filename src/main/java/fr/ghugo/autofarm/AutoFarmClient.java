package fr.ghugo.autofarm;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoFarmClient implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("autofarm");

	private static KeyMapping openMenuKey;
	private static KeyMapping toggleKey;
	private static KeyMapping pauseKey;

	@Override
	public void onInitializeClient() {
		AutoFarmConfig.load();

		openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.autofarm.open_menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, KeyMapping.Category.MISC));
		toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.autofarm.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, KeyMapping.Category.MISC));
		pauseKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.autofarm.pause", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, KeyMapping.Category.MISC));

		ClientReceiveMessageEvents.GAME.register((message, overlay) ->
				AutoFarmController.onChatMessage(Minecraft.getInstance(), message));
		ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) ->
				AutoFarmController.onChatMessage(Minecraft.getInstance(), message));

		ClientTickEvents.START_CLIENT_TICK.register(AutoFarmController::tick);
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			while (openMenuKey.consumeClick()) {
				mc.setScreen(new AutoFarmScreen(mc.screen));
			}
			while (toggleKey.consumeClick()) {
				AutoFarmController.toggle(mc);
			}
			while (pauseKey.consumeClick()) {
				AutoFarmController.togglePause(mc);
			}
		});
	}
}
