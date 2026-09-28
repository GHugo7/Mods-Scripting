package fr.ghugo.autofarm;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Interface de configuration (touche K). */
public class AutoFarmScreen extends Screen {
	private static final int WHITE = 0xFFFFFFFF;
	private static final int GRAY = 0xFFA0A0A0;
	private static final int RED = 0xFFFF5555;

	private final Screen parent;
	private EditBox leftBox;
	private EditBox rightBox;
	private EditBox cyclesBox;
	private EditBox captchaBox;
	private String error = "";

	public AutoFarmScreen(Screen parent) {
		super(Component.literal("Auto Farm Sweep"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int cx = this.width / 2;
		int y = 32;

		leftBox = numberBox(cx + 5, y, format(AutoFarmConfig.leftSeconds), "Secondes à gauche");
		y += 22;
		rightBox = numberBox(cx + 5, y, format(AutoFarmConfig.rightSeconds), "Secondes à droite");
		y += 22;
		cyclesBox = numberBox(cx + 5, y, String.valueOf(AutoFarmConfig.cycles), "Allers-retours");
		cyclesBox.setFilter(s -> s.matches("\\d{0,5}"));
		cyclesBox.setHint(Component.literal("0 = infini"));
		cyclesBox.setTooltip(tip("Nombre d'allers-retours (gauche puis droite) avant de s'arrêter. 0 ou vide = sans fin."));
		y += 22;
		captchaBox = new EditBox(this.font, cx + 5, y, 95, 20, Component.literal("Texte du captcha"));
		captchaBox.setMaxLength(100);
		captchaBox.setValue(AutoFarmConfig.captchaText);
		captchaBox.setTooltip(tip("Si un message du chat contient ce texte, le mod s'arrête et vous prévient. Vide = désactivé."));
		addRenderableWidget(captchaBox);
		y += 24;

		addRenderableWidget(Button.builder(toggleLabel("Casse auto", AutoFarmConfig.breakBlocks), b -> {
			AutoFarmConfig.breakBlocks = !AutoFarmConfig.breakBlocks;
			b.setMessage(toggleLabel("Casse auto", AutoFarmConfig.breakBlocks));
		}).tooltip(tip("Maintient le clic gauche pour casser le bloc que vous visez pendant les déplacements. NON = le mod se déplace seulement.")).bounds(cx - 100, y, 98, 20).build());
		addRenderableWidget(Button.builder(toggleLabel("Notif PC", AutoFarmConfig.desktopNotification), b -> {
			AutoFarmConfig.desktopNotification = !AutoFarmConfig.desktopNotification;
			b.setMessage(toggleLabel("Notif PC", AutoFarmConfig.desktopNotification));
		}).tooltip(tip("Au captcha, affiche une notification Windows / Mac / Linux, même si Minecraft est en arrière-plan.")).bounds(cx + 2, y, 98, 20).build());
		y += 22;
		addRenderableWidget(Button.builder(toggleLabel("Seulement cultures", AutoFarmConfig.cropsOnly), b -> {
			AutoFarmConfig.cropsOnly = !AutoFarmConfig.cropsOnly;
			b.setMessage(toggleLabel("Seulement cultures", AutoFarmConfig.cropsOnly));
		}).tooltip(tip("OUI = ne casse que les cultures (blé, carottes, patates, betteraves, verrues, cacao, canne, cactus, bambou, melon, citrouille) : la terre labourée et les autres blocs ne sont jamais cassés. NON = casse n'importe quel bloc visé.")).bounds(cx - 100, y, 200, 20).build());
		y += 22;
		addRenderableWidget(Button.builder(toggleLabel("Seulement mûres", AutoFarmConfig.matureOnly), b -> {
			AutoFarmConfig.matureOnly = !AutoFarmConfig.matureOnly;
			b.setMessage(toggleLabel("Seulement mûres", AutoFarmConfig.matureOnly));
		}).tooltip(tip("OUI = ignore les cultures qui n'ont pas fini de pousser (blé, carottes, patates, betteraves, verrues, cacao). NON = casse aussi les jeunes pousses.")).bounds(cx - 100, y, 200, 20).build());
		y += 22;
		addRenderableWidget(Button.builder(toggleLabel("Alarme", AutoFarmConfig.loudAlarm), b -> {
			AutoFarmConfig.loudAlarm = !AutoFarmConfig.loudAlarm;
			b.setMessage(toggleLabel("Alarme", AutoFarmConfig.loudAlarm));
		}).tooltip(tip("Au captcha, joue une série de bips pendant environ 5 secondes, au volume réglé à droite.")).bounds(cx - 100, y, 98, 20).build());
		AbstractSliderButton volumeSlider = new AbstractSliderButton(cx + 2, y, 98, 20, volumeLabel(), AutoFarmConfig.alarmVolume) {
			@Override
			protected void updateMessage() {
				setMessage(volumeLabel());
			}

			@Override
			protected void applyValue() {
				AutoFarmConfig.alarmVolume = this.value;
			}
		};
		volumeSlider.setTooltip(tip("Volume des bips de l'alarme (indépendant du volume de Minecraft). Utilisez « Tester » pour l'essayer."));
		addRenderableWidget(volumeSlider);
		y += 26;
		Component startLabel = Component.literal(AutoFarmController.isRunning() ? "§cArrêter" : "§aDémarrer");
		addRenderableWidget(Button.builder(startLabel, b -> {
			if (AutoFarmController.isRunning()) {
				AutoFarmController.stop(this.minecraft, "Auto Farm arrêté.");
				onClose();
			} else if (applyValues()) {
				AutoFarmConfig.save();
				this.minecraft.setScreen(null);
				AutoFarmController.start(this.minecraft);
			}
		}).bounds(cx - 100, y, 66, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Enregistrer"), b -> {
			if (applyValues()) {
				AutoFarmConfig.save();
				onClose();
			}
		}).bounds(cx - 32, y, 66, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Tester"), b ->
				AutoFarmAlert.trigger(this.minecraft, "Ceci est un test de l'alerte captcha.")
		).tooltip(tip("Déclenche l'alerte captcha maintenant (son, notification, titre) pour vérifier le volume.")).bounds(cx + 36, y, 64, 20).build());
	}

	private EditBox numberBox(int x, int y, String value, String label) {
		EditBox box = new EditBox(this.font, x, y, 95, 20, Component.literal(label));
		box.setMaxLength(8);
		box.setFilter(s -> s.matches("\\d{0,5}([.,]\\d{0,2})?"));
		box.setValue(value);
		addRenderableWidget(box);
		return box;
	}

	private boolean applyValues() {
		try {
			double left = parse(leftBox.getValue());
			double right = parse(rightBox.getValue());
			int cycles = cyclesBox.getValue().isEmpty() ? 0 : Integer.parseInt(cyclesBox.getValue());
			if (left <= 0 || right <= 0) {
				error = "Les durées doivent être supérieures à 0.";
				return false;
			}
			AutoFarmConfig.leftSeconds = left;
			AutoFarmConfig.rightSeconds = right;
			AutoFarmConfig.cycles = cycles;
			AutoFarmConfig.captchaText = captchaBox.getValue().trim();
			error = "";
			return true;
		} catch (NumberFormatException e) {
			error = "Valeur invalide.";
			return false;
		}
	}

	private static double parse(String s) {
		return Double.parseDouble(s.replace(',', '.'));
	}

	private static String format(double d) {
		return d == Math.rint(d) ? String.valueOf((long) d) : String.valueOf(d);
	}

	private static Tooltip tip(String text) {
		return Tooltip.create(Component.literal(text));
	}

	private static Component volumeLabel() {
		return Component.literal("Volume : " + Math.round(AutoFarmConfig.alarmVolume * 100) + "%");
	}

	private static Component toggleLabel(String name, boolean on) {
		return Component.literal(name + " : " + (on ? "§aOUI" : "§cNON"));
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		int cx = this.width / 2;
		graphics.drawCenteredString(this.font, this.title, cx, 6, WHITE);
		if (error.isEmpty()) {
			graphics.drawCenteredString(this.font, "J : démarrer / arrêter  |  survolez pour l'aide", cx, 18, GRAY);
		} else {
			graphics.drawCenteredString(this.font, error, cx, 18, RED);
		}
		graphics.drawString(this.font, "Secondes à gauche :", cx - 100, 38, WHITE);
		graphics.drawString(this.font, "Secondes à droite :", cx - 100, 60, WHITE);
		graphics.drawString(this.font, "Allers-retours :", cx - 100, 82, WHITE);
		graphics.drawString(this.font, "Arrêt si le chat dit :", cx - 100, 104, WHITE);
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
