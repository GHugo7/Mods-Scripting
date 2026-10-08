package fr.ghugo.autofarm;

import fr.ghugo.autofarm.compat.BaseScreen;
import fr.ghugo.autofarm.compat.Compat;
import fr.ghugo.autofarm.compat.Draw;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Interface de configuration (touche du menu, K par défaut). */
public class AutoFarmScreen extends BaseScreen {
	private static final int WHITE = 0xFFFFFFFF;
	private static final int GRAY = 0xFFA0A0A0;
	private static final int RED = 0xFFFF5555;

	private final Screen parent;
	private EditBox leftBox;
	private EditBox rightBox;
	private EditBox tripsBox;
	private EditBox loopsBox;
	private EditBox commandBox;
	private EditBox beforeBox;
	private EditBox waitBox;
	private EditBox captchaBox;
	private EditBox randomBox;
	private EditBox phoneBox;
	private String error = "";

	public AutoFarmScreen(Screen parent) {
		super(Component.literal("Auto Farm Sweep"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int cx = this.width / 2;
		int left = cx - 205;
		int right = cx + 5;

		// Colonne de gauche : déplacement et boucles.
		int y = 36;
		leftBox = numberBox(cx - 100, y, format(AutoFarmConfig.leftSeconds), "Secondes à gauche");
		leftBox.setTooltip(tip("Durée (en secondes) pendant laquelle le joueur va à gauche. Décimales acceptées (ex. 7.5)."));
		y += 22;
		rightBox = numberBox(cx - 100, y, format(AutoFarmConfig.rightSeconds), "Secondes à droite");
		rightBox.setTooltip(tip("Durée (en secondes) pendant laquelle le joueur va à droite. Décimales acceptées (ex. 7.5)."));
		y += 22;
		tripsBox = numberBox(cx - 100, y, String.valueOf(AutoFarmConfig.trips), "Allers-retours");
		Compat.restrictInput(tripsBox, "\\d{0,4}", null);
		tripsBox.setTooltip(tip("Nombre d'allers-retours (gauche puis droite) dans une boucle. Minimum 1."));
		y += 22;
		loopsBox = numberBox(cx - 100, y, String.valueOf(AutoFarmConfig.loops), "Boucles");
		Compat.restrictInput(loopsBox, "\\d{0,5}", null);
		loopsBox.setHint(Component.literal("0 = infini"));
		loopsBox.setTooltip(tip("Nombre de fois que toute la boucle (les allers-retours + la commande de fin) est répétée. 0 ou vide = sans fin."));
		y += 22;
		commandBox = new EditBox(this.font, cx - 100, y, 95, 20, Component.literal("Commande de fin"));
		commandBox.setMaxLength(256);
		commandBox.setValue(AutoFarmConfig.endCommand);
		commandBox.setHint(Component.literal("ex. /home farm"));
		commandBox.setTooltip(tip("Commande envoyée à la fin de chaque boucle (ex. /home farm). Plusieurs commandes : séparez-les par ; . Vide = aucune."));
		addRenderableWidget(commandBox);
		y += 22;
		beforeBox = numberBox(cx - 100, y, format(AutoFarmConfig.beforeCommandSeconds), "Délai avant");
		beforeBox.setTooltip(tip("Temps (en secondes) où le joueur reste immobile à la fin des allers-retours, avant d'envoyer la commande de fin. Plus naturel qu'une téléportation instantanée."));
		y += 22;
		waitBox = numberBox(cx - 100, y, format(AutoFarmConfig.endWaitSeconds), "Attente");
		addRenderableWidget(Button.builder(Component.literal("§6Statistiques"), b ->
				Compat.setScreen(this.minecraft, new AutoFarmStatsScreen(this))
		).tooltip(tip("Ouvre l'écran des statistiques : argent, cultures, récompenses et événements de la session et de toutes les sessions. Touche " + AutoFarmClient.statsKey() + ".")).bounds(left, 190, 200, 20).build());
		waitBox.setTooltip(tip("Temps d'attente (en secondes) après la commande de fin, avant de repartir (ex. le temps de la téléportation)."));

		// Colonne de droite : captcha, casse et alertes.
		y = 36;
		captchaBox = new EditBox(this.font, cx + 110, y, 95, 20, Component.literal("Texte du captcha"));
		captchaBox.setMaxLength(100);
		captchaBox.setValue(AutoFarmConfig.captchaText);
		captchaBox.setTooltip(tip("Si un message du chat contient ce texte, le mod se met en pause (en gardant sa progression) et vous prévient. Vide = désactivé."));
		addRenderableWidget(captchaBox);
		y += 22;
		addRenderableWidget(Button.builder(toggleLabel("Casse auto", AutoFarmConfig.breakBlocks), b -> {
			AutoFarmConfig.breakBlocks = !AutoFarmConfig.breakBlocks;
			b.setMessage(toggleLabel("Casse auto", AutoFarmConfig.breakBlocks));
		}).tooltip(tip("Maintient le clic gauche pour casser le bloc que vous visez pendant les déplacements. NON = le mod se déplace seulement.")).bounds(right, y, 98, 20).build());
		addRenderableWidget(Button.builder(toggleLabel("Notif PC", AutoFarmConfig.desktopNotification), b -> {
			AutoFarmConfig.desktopNotification = !AutoFarmConfig.desktopNotification;
			b.setMessage(toggleLabel("Notif PC", AutoFarmConfig.desktopNotification));
		}).tooltip(tip("Au captcha, affiche une notification Windows / Mac / Linux, même si Minecraft est en arrière-plan.")).bounds(right + 102, y, 98, 20).build());
		y += 22;
		addRenderableWidget(Button.builder(toggleLabel("Seulement cultures", AutoFarmConfig.cropsOnly), b -> {
			AutoFarmConfig.cropsOnly = !AutoFarmConfig.cropsOnly;
			b.setMessage(toggleLabel("Seulement cultures", AutoFarmConfig.cropsOnly));
		}).tooltip(tip("OUI = ne casse que les cultures (blé, carottes, patates, betteraves, verrues, cacao, canne, cactus, bambou, melon, citrouille) : la terre labourée et les autres blocs ne sont jamais cassés. NON = casse n'importe quel bloc visé.")).bounds(right, y, 200, 20).build());
		y += 22;
		addRenderableWidget(Button.builder(toggleLabel("Seulement mûres", AutoFarmConfig.matureOnly), b -> {
			AutoFarmConfig.matureOnly = !AutoFarmConfig.matureOnly;
			b.setMessage(toggleLabel("Seulement mûres", AutoFarmConfig.matureOnly));
		}).tooltip(tip("OUI = ignore les cultures qui n'ont pas fini de pousser (blé, carottes, patates, betteraves, verrues, cacao). NON = casse aussi les jeunes pousses.")).bounds(right, y, 200, 20).build());
		y += 22;
		addRenderableWidget(Button.builder(toggleLabel("Alarme", AutoFarmConfig.loudAlarm), b -> {
			AutoFarmConfig.loudAlarm = !AutoFarmConfig.loudAlarm;
			b.setMessage(toggleLabel("Alarme", AutoFarmConfig.loudAlarm));
		}).tooltip(tip("Au captcha, joue une série de bips pendant environ 5 secondes, au volume réglé à droite.")).bounds(right, y, 98, 20).build());
		AbstractSliderButton volumeSlider = new AbstractSliderButton(right + 102, y, 98, 20, volumeLabel(), AutoFarmConfig.alarmVolume) {
			@Override
			protected void updateMessage() {
				setMessage(volumeLabel());
			}

			@Override
			protected void applyValue() {
				AutoFarmConfig.alarmVolume = this.value;
			}
		};
		randomBox = numberBox(cx + 110, y + 22, String.valueOf(AutoFarmConfig.delayRandomPercent), "Hasard délais");
		Compat.restrictInput(randomBox, "\\d{0,3}", null);
		randomBox.setTooltip(tip("Variation aléatoire du délai avant commande et de l'attente après, pour ne jamais avoir exactement le même temps. Ex. 20 % sur 2 s = entre 1,6 et 2,4 s. 0 = délais fixes."));
		phoneBox = new EditBox(this.font, cx + 110, y + 44, 95, 20, Component.literal("Topic ntfy"));
		phoneBox.setMaxLength(200);
		phoneBox.setValue(AutoFarmConfig.phoneTopic);
		phoneBox.setHint(Component.literal("vide = désactivé"));
		phoneBox.setTooltip(tip("Nom de votre topic dans l'appli ntfy (Android/iPhone) pour recevoir les alertes sur le téléphone : captcha et mentions. Choisissez un nom long et difficile à deviner (ex. farm-ghugo-8k2q). Vide = désactivé."));
		addRenderableWidget(phoneBox);
		addRenderableWidget(Button.builder(toggleLabel("Mention → tél", AutoFarmConfig.phoneOnMention), b -> {
			AutoFarmConfig.phoneOnMention = !AutoFarmConfig.phoneOnMention;
			b.setMessage(toggleLabel("Mention → tél", AutoFarmConfig.phoneOnMention));
		}).tooltip(tip("Envoie une notification sur le téléphone quand un autre joueur écrit votre pseudo dans le chat ou vous envoie un message privé. Vos propres messages sont ignorés. Nécessite le topic ntfy.")).bounds(right, y + 66, 200, 20).build());
		volumeSlider.setTooltip(tip("Volume des bips de l'alarme (indépendant du volume de Minecraft). Utilisez « Tester » pour l'essayer."));
		addRenderableWidget(volumeSlider);

		// Boutons du bas.
		y = 216;
		boolean running = AutoFarmController.isRunning();
		boolean paused = AutoFarmController.isPaused();
		addRenderableWidget(Button.builder(Component.literal(running ? "§cArrêter" : "§aDémarrer"), b -> {
			if (AutoFarmController.isRunning()) {
				AutoFarmController.stop(this.minecraft, "Auto Farm arrêté.");
				onClose();
			} else if (applyValues()) {
				AutoFarmConfig.save();
				Compat.setScreen(this.minecraft, null);
				AutoFarmController.start(this.minecraft);
			}
		}).tooltip(tip("Démarre depuis le début, ou arrête complètement (la progression est perdue). Touche " + AutoFarmClient.toggleKey() + ".")).bounds(left, y, 100, 20).build());
		Button pauseButton = addRenderableWidget(Button.builder(Component.literal(paused ? "§aReprendre" : "§ePause"), b -> {
			if (AutoFarmController.isPaused()) {
				if (!applyValues()) {
					return;
				}
				AutoFarmConfig.save();
				Compat.setScreen(this.minecraft, null);
				AutoFarmController.resume(this.minecraft);
			} else {
				AutoFarmController.pause(this.minecraft, "En pause (" + AutoFarmClient.pauseKey() + " pour reprendre).");
				Compat.setScreen(this.minecraft, null);
			}
		}).tooltip(tip("Met en pause sans perdre la progression, puis reprend exactement au même endroit. Touche " + AutoFarmClient.pauseKey() + ".")).bounds(left + 103, y, 100, 20).build());
		pauseButton.active = running;
		addRenderableWidget(Button.builder(Component.literal("Enregistrer"), b -> {
			if (applyValues()) {
				AutoFarmConfig.save();
				onClose();
			}
		}).bounds(left + 206, y, 100, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Tester l'alerte"), b -> {
			if (applyValues()) {
				AutoFarmAlert.trigger(this.minecraft, "Ceci est un test de l'alerte captcha.");
			}
		}).tooltip(tip("Déclenche l'alerte captcha maintenant (son, notification PC et téléphone, titre) pour vérifier que tout marche.")).bounds(left + 309, y, 100, 20).build());
	}

	private EditBox numberBox(int x, int y, String value, String label) {
		EditBox box = new EditBox(this.font, x, y, 95, 20, Component.literal(label));
		box.setMaxLength(8);
		Compat.restrictInput(box, "\\d{0,5}([.,]\\d{0,2})?", null);
		box.setValue(value);
		addRenderableWidget(box);
		return box;
	}

	private boolean applyValues() {
		try {
			double left = parse(leftBox.getValue());
			double right = parse(rightBox.getValue());
			int trips = tripsBox.getValue().isEmpty() ? 1 : Integer.parseInt(tripsBox.getValue());
			int loops = loopsBox.getValue().isEmpty() ? 0 : Integer.parseInt(loopsBox.getValue());
			double wait = waitBox.getValue().isEmpty() ? 0 : parse(waitBox.getValue());
			double before = beforeBox.getValue().isEmpty() ? 0 : parse(beforeBox.getValue());
			if (left <= 0 || right <= 0) {
				error = "Les durées doivent être supérieures à 0.";
				return false;
			}
			AutoFarmConfig.leftSeconds = left;
			AutoFarmConfig.rightSeconds = right;
			AutoFarmConfig.trips = Math.max(1, trips);
			AutoFarmConfig.loops = loops;
			AutoFarmConfig.endCommand = commandBox.getValue().trim();
			AutoFarmConfig.beforeCommandSeconds = before;
			AutoFarmConfig.delayRandomPercent = randomBox.getValue().isEmpty() ? 0 : Math.min(100, Integer.parseInt(randomBox.getValue()));
			AutoFarmConfig.endWaitSeconds = wait;
			AutoFarmConfig.captchaText = captchaBox.getValue().trim();
			AutoFarmConfig.phoneTopic = phoneBox.getValue().trim();
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
	protected void drawContent(Draw draw, int mouseX, int mouseY) {
		int cx = this.width / 2;
		draw.centered(this.title.getString(), cx, 6, WHITE);
		draw.centered("État : " + AutoFarmController.status(), cx, 20, WHITE);
		int left = cx - 205;
		draw.text("Secondes à gauche :", left, 42, WHITE);
		draw.text("Secondes à droite :", left, 64, WHITE);
		draw.text("Allers-retours :", left, 86, WHITE);
		draw.text("Boucles :", left, 108, WHITE);
		draw.text("Commande de fin :", left, 130, WHITE);
		draw.text("Délai avant (s) :", left, 152, WHITE);
		draw.text("Attente après (s) :", left, 174, WHITE);
		draw.text("Texte captcha :", cx + 5, 42, WHITE);
		draw.text("Hasard délais (%) :", cx + 5, 152, WHITE);
		draw.text("Téléphone (ntfy) :", cx + 5, 174, WHITE);
		if (error.isEmpty()) {
			draw.centered(AutoFarmClient.menuKey() + " : menu  |  " + AutoFarmClient.toggleKey() + " : démarrer/arrêter  |  " + AutoFarmClient.pauseKey() + " : pause  |  survolez pour l'aide", cx, 242, GRAY);
		} else {
			draw.centered(error, cx, 242, RED);
		}
	}

	@Override
	public void onClose() {
		Compat.setScreen(this.minecraft, parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
