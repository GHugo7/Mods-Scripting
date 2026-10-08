package fr.ghugo.autofarm;

import fr.ghugo.autofarm.compat.Compat;
import fr.ghugo.autofarm.compat.Draw;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

/** Petit panneau de statistiques en haut à gauche de l'écran pendant le farm. */
public final class AutoFarmHud {
	private AutoFarmHud() {
	}

	static void register() {
		Compat.registerHud("stats", AutoFarmHud::render);
	}

	private static void render(Draw draw) {
		Minecraft mc = Minecraft.getInstance();
		if (!AutoFarmConfig.statsHud || !AutoFarmController.isRunning() || Compat.hudHidden(mc)) {
			return;
		}
		List<String> lines = new ArrayList<>();
		lines.add("§6Auto Farm " + (AutoFarmController.isPaused() ? "§e(en pause)" : "§a(en cours)"));
		lines.addAll(AutoFarmStats.hudLines());
		int width = 0;
		for (String line : lines) {
			width = Math.max(width, draw.width(line));
		}
		draw.fill(2, 2, 2 + width + 6, 2 + lines.size() * 10 + 4, 0x90000000);
		for (int i = 0; i < lines.size(); i++) {
			draw.text(lines.get(i), 5, 5 + i * 10, 0xFFFFFFFF);
		}
	}
}
