package fr.ghugo.autofarm;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/** Petit panneau de statistiques en haut à gauche de l'écran pendant le farm. */
public final class AutoFarmHud {
	private AutoFarmHud() {
	}

	static void register() {
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("autofarm", "stats"), AutoFarmHud::render);
	}

	private static void render(GuiGraphics graphics, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		if (!AutoFarmConfig.statsHud || !AutoFarmController.isRunning() || mc.options.hideGui) {
			return;
		}
		List<String> lines = new ArrayList<>();
		lines.add("§6Auto Farm " + (AutoFarmController.isPaused() ? "§e(en pause)" : "§a(en cours)"));
		lines.addAll(AutoFarmStats.hudLines());
		int width = 0;
		for (String line : lines) {
			width = Math.max(width, mc.font.width(line));
		}
		graphics.fill(2, 2, 2 + width + 6, 2 + lines.size() * 10 + 4, 0x90000000);
		for (int i = 0; i < lines.size(); i++) {
			graphics.drawString(mc.font, lines.get(i), 5, 5 + i * 10, 0xFFFFFFFF);
		}
	}
}
