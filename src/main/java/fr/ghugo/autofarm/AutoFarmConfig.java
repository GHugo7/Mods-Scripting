package fr.ghugo.autofarm;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Réglages du mod, sauvegardés dans config/autofarm.properties. */
public final class AutoFarmConfig {
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("autofarm.properties");

	/** Durée (secondes) passée à aller à gauche. */
	public static double leftSeconds = 10.0;
	/** Durée (secondes) passée à aller à droite. */
	public static double rightSeconds = 10.0;
	/** Nombre d'allers-retours (gauche puis droite) dans une boucle. */
	public static int trips = 1;
	/** Nombre de boucles (0 = infini). */
	public static int loops = 0;
	/** Commande(s) envoyée(s) à la fin de chaque boucle, séparées par « ; » (vide = aucune). */
	public static String endCommand = "";
	/** Délai (secondes) à l'arrêt avant d'envoyer la commande de fin de boucle. */
	public static double beforeCommandSeconds = 2.0;
	/** Variation aléatoire (en %) appliquée au délai avant commande et à l'attente après (0 = fixe). */
	public static int delayRandomPercent = 20;
	/** Attente (secondes) après la commande de fin de boucle, avant de repartir. */
	public static double endWaitSeconds = 3.0;
	/** Casser le bloc visé pendant le déplacement. */
	public static boolean breakBlocks = true;
	/** Ne casser que les cultures (pas la terre, etc.). */
	public static boolean cropsOnly = true;
	/** Ne casser que les cultures arrivées à maturité. */
	public static boolean matureOnly = true;
	/** Le mod s'arrête si un message du chat contient ce texte (vide = désactivé). */
	public static String captchaText = "/captcha start";
	/** Alarme sonore forte au captcha. */
	public static boolean loudAlarm = true;
	/** Volume de l'alarme, de 0 à 1. */
	public static double alarmVolume = 0.3;
	/** Notification Windows/macOS/Linux au captcha. */
	public static boolean desktopNotification = true;

	private AutoFarmConfig() {
	}

	public static void load() {
		if (!Files.exists(FILE)) {
			return;
		}
		Properties p = new Properties();
		try (Reader reader = Files.newBufferedReader(FILE)) {
			p.load(reader);
			leftSeconds = Double.parseDouble(p.getProperty("leftSeconds", String.valueOf(leftSeconds)));
			rightSeconds = Double.parseDouble(p.getProperty("rightSeconds", String.valueOf(rightSeconds)));
			trips = Math.max(1, Integer.parseInt(p.getProperty("trips", String.valueOf(trips))));
			loops = Math.max(0, Integer.parseInt(p.getProperty("loops", String.valueOf(loops))));
			endCommand = p.getProperty("endCommand", endCommand);
			beforeCommandSeconds = Math.max(0.0, Double.parseDouble(p.getProperty("beforeCommandSeconds", String.valueOf(beforeCommandSeconds))));
			delayRandomPercent = Math.clamp(Integer.parseInt(p.getProperty("delayRandomPercent", String.valueOf(delayRandomPercent))), 0, 100);
			endWaitSeconds = Math.max(0.0, Double.parseDouble(p.getProperty("endWaitSeconds", String.valueOf(endWaitSeconds))));
			breakBlocks = Boolean.parseBoolean(p.getProperty("breakBlocks", String.valueOf(breakBlocks)));
			cropsOnly = Boolean.parseBoolean(p.getProperty("cropsOnly", String.valueOf(cropsOnly)));
			matureOnly = Boolean.parseBoolean(p.getProperty("matureOnly", String.valueOf(matureOnly)));
			captchaText = p.getProperty("captchaText", captchaText);
			loudAlarm = Boolean.parseBoolean(p.getProperty("loudAlarm", String.valueOf(loudAlarm)));
			alarmVolume = Math.clamp(Double.parseDouble(p.getProperty("alarmVolume", String.valueOf(alarmVolume))), 0.0, 1.0);
			desktopNotification = Boolean.parseBoolean(p.getProperty("desktopNotification", String.valueOf(desktopNotification)));
		} catch (IOException | NumberFormatException e) {
			AutoFarmClient.LOGGER.warn("Impossible de lire {}", FILE, e);
		}
	}

	public static void save() {
		Properties p = new Properties();
		p.setProperty("leftSeconds", String.valueOf(leftSeconds));
		p.setProperty("rightSeconds", String.valueOf(rightSeconds));
		p.setProperty("trips", String.valueOf(trips));
		p.setProperty("loops", String.valueOf(loops));
		p.setProperty("endCommand", endCommand);
		p.setProperty("beforeCommandSeconds", String.valueOf(beforeCommandSeconds));
		p.setProperty("delayRandomPercent", String.valueOf(delayRandomPercent));
		p.setProperty("endWaitSeconds", String.valueOf(endWaitSeconds));
		p.setProperty("breakBlocks", String.valueOf(breakBlocks));
		p.setProperty("cropsOnly", String.valueOf(cropsOnly));
		p.setProperty("matureOnly", String.valueOf(matureOnly));
		p.setProperty("captchaText", captchaText);
		p.setProperty("loudAlarm", String.valueOf(loudAlarm));
		p.setProperty("alarmVolume", String.valueOf(alarmVolume));
		p.setProperty("desktopNotification", String.valueOf(desktopNotification));
		try {
			Files.createDirectories(FILE.getParent());
			try (Writer writer = Files.newBufferedWriter(FILE)) {
				p.store(writer, "Auto Farm Sweep");
			}
		} catch (IOException e) {
			AutoFarmClient.LOGGER.warn("Impossible d'écrire {}", FILE, e);
		}
	}
}
