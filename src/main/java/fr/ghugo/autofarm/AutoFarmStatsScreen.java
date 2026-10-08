package fr.ghugo.autofarm;

import fr.ghugo.autofarm.compat.BaseScreen;
import fr.ghugo.autofarm.compat.Compat;
import fr.ghugo.autofarm.compat.Draw;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Écran des statistiques (touche L par défaut) : session en cours, total de toutes les sessions et réglages. */
public class AutoFarmStatsScreen extends BaseScreen {
	private static final int WHITE = 0xFFFFFFFF;
	private static final int GRAY = 0xFFA0A0A0;
	private static final int GOLD = 0xFFFFAA00;
	private static final int ROW_HEIGHT = 14;
	private static final int TOP = 36;

	private final Screen parent;
	private int scroll;
	private boolean confirmResetTotal;

	public AutoFarmStatsScreen(Screen parent) {
		super(Component.literal("Auto Farm — Statistiques"));
		this.parent = parent;
	}

	private int visibleRows() {
		return Math.max(1, (this.height - 84 - TOP) / ROW_HEIGHT);
	}

	@Override
	protected void init() {
		int left = this.width / 2 - 205;
		List<AutoFarmStats.Row> rows = AutoFarmStats.rows();
		int visible = visibleRows();
		scroll = Math.clamp(scroll, 0, Math.max(0, rows.size() - visible));

		// Case « visible / masqué » devant chaque ligne.
		for (int i = scroll; i < Math.min(rows.size(), scroll + visible); i++) {
			String name = rows.get(i).name();
			boolean hidden = AutoFarmStats.isHidden(name);
			addRenderableWidget(Button.builder(Component.literal(hidden ? "§c✖" : "§a✔"), b -> {
				AutoFarmStats.toggleHidden(name);
				rebuildWidgets();
			}).tooltip(tip(hidden
					? "Masqué dans le panneau à l'écran et les résumés. Cliquez pour l'afficher."
					: "Affiché dans le panneau à l'écran et les résumés. Cliquez pour le masquer."))
					.bounds(left, TOP + (i - scroll) * ROW_HEIGHT - 2, 14, 12).build());
		}

		// Réglages.
		int y = this.height - 76;
		EditBox alerts = new EditBox(this.font, left + 90, y, 320, 20, Component.literal("Alertes chat"));
		alerts.setMaxLength(500);
		alerts.setValue(AutoFarmConfig.chatAlerts);
		alerts.setResponder(s -> AutoFarmConfig.chatAlerts = s);
		alerts.setTooltip(tip("Textes du chat qui déclenchent une alerte (titre à l'écran, son de cloche différent du captcha, notification téléphone), séparés par ; . Ex. booster de moisson vient d'expirer ; Moisson Dorée. Le farm continue. Vide = aucune alerte."));
		addRenderableWidget(alerts);
		y = this.height - 52;
		addRenderableWidget(Button.builder(toggleLabel("Panneau à l'écran", AutoFarmConfig.statsHud), b -> {
			AutoFarmConfig.statsHud = !AutoFarmConfig.statsHud;
			b.setMessage(toggleLabel("Panneau à l'écran", AutoFarmConfig.statsHud));
			AutoFarmConfig.save();
		}).tooltip(tip("Affiche en haut à gauche de l'écran les statistiques de la session pendant le farm (lignes cochées ci-dessus)."))
				.bounds(left, y, 150, 20).build());
		addRenderableWidget(Button.builder(phoneLabel(), b -> {
			AutoFarmConfig.statsPhoneMode = (AutoFarmConfig.statsPhoneMode + 1) % 3;
			b.setMessage(phoneLabel());
			AutoFarmConfig.save();
		}).tooltip(tip("Envoi du bilan de la session sur le téléphone (ntfy) : jamais, à l'arrêt du farm, ou à l'arrêt et toutes les X minutes. Nécessite le topic ntfy du menu principal."))
				.bounds(left + 154, y, 180, 20).build());
		EditBox minutes = new EditBox(this.font, left + 338, y, 40, 20, Component.literal("Minutes"));
		minutes.setMaxLength(4);
		minutes.setValue(String.valueOf(AutoFarmConfig.statsPhoneMinutes));
		Compat.restrictInput(minutes, "\\d{0,4}", s -> {
			if (!s.isEmpty()) {
				AutoFarmConfig.statsPhoneMinutes = Math.max(1, Integer.parseInt(s));
			}
		});
		minutes.setTooltip(tip("Intervalle en minutes du bilan périodique sur le téléphone."));
		addRenderableWidget(minutes);

		y = this.height - 28;
		addRenderableWidget(Button.builder(Component.literal("Remettre la session à 0"), b -> {
			AutoFarmStats.resetSession();
			rebuildWidgets();
		}).bounds(left, y, 136, 20).build());
		addRenderableWidget(Button.builder(Component.literal(confirmResetTotal ? "§cConfirmer ?" : "Remettre le total à 0"), b -> {
			if (confirmResetTotal) {
				AutoFarmStats.resetTotal();
				confirmResetTotal = false;
			} else {
				confirmResetTotal = true;
			}
			rebuildWidgets();
		}).tooltip(tip("Efface le total de toutes les sessions (cliquez deux fois pour confirmer).")).bounds(left + 140, y, 136, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Retour"), b -> onClose()).bounds(left + 280, y, 130, 20).build());
	}

	@Override
	protected void drawContent(Draw draw, int mouseX, int mouseY) {
		int cx = this.width / 2;
		int left = cx - 205;
		draw.centered(this.title.getString(), cx, 6, WHITE);
		draw.text("Session", cx + 20, 22, GOLD);
		draw.text("Total", cx + 120, 22, GOLD);

		List<AutoFarmStats.Row> rows = AutoFarmStats.rows();
		int visible = visibleRows();
		for (int i = scroll; i < Math.min(rows.size(), scroll + visible); i++) {
			AutoFarmStats.Row row = rows.get(i);
			int y = TOP + (i - scroll) * ROW_HEIGHT;
			int color = AutoFarmStats.isHidden(row.name()) ? GRAY : WHITE;
			draw.text(row.name(), left + 18, y, color);
			draw.text(row.session(), cx + 20, y, color);
			draw.text(row.total(), cx + 120, y, color);
		}
		if (rows.size() > visible) {
			draw.centered("molette : défiler (" + (scroll + 1) + "-" + Math.min(rows.size(), scroll + visible) + " / " + rows.size() + ")",
					cx, this.height - 88, GRAY);
		}
		draw.text("min", left + 382, this.height - 46, WHITE);
		draw.text("Alertes chat :", left, this.height - 70, WHITE);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		int before = scroll;
		scroll = Math.clamp(scroll - (int) Math.signum(scrollY), 0, Math.max(0, AutoFarmStats.rows().size() - visibleRows()));
		if (scroll != before) {
			rebuildWidgets();
		}
		return true;
	}

	private static Component phoneLabel() {
		return Component.literal("Bilan tél : " + switch (AutoFarmConfig.statsPhoneMode) {
			case 0 -> "§cjamais";
			case 1 -> "§aà l'arrêt";
			default -> "§aarrêt + toutes les";
		});
	}

	private static Component toggleLabel(String name, boolean on) {
		return Component.literal(name + " : " + (on ? "§aOUI" : "§cNON"));
	}

	private static Tooltip tip(String text) {
		return Tooltip.create(Component.literal(text));
	}

	@Override
	public void onClose() {
		AutoFarmConfig.save();
		Compat.setScreen(this.minecraft, parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
