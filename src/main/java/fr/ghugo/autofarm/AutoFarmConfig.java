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
	/** Nombre d'allers-retours (0 = infini). */
	public static int cycles = 0;
	/** Casser le bloc visé pendant le déplacement. */
	public static boolean breakBlocks = true;
	/** Ne casser que les cultures (pas la terre, etc.). */
	public static boolean cropsOnly = true;
	/** Ne casser que les cultures arrivées à maturité. */
	public static boolean matureOnly = true;
	/** Le mod s'arrête si un message du chat contient ce texte (vide = désactivé). */
	public static String captchaText = "/captcha start";

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
			cycles = Integer.parseInt(p.getProperty("cycles", String.valueOf(cycles)));
			breakBlocks = Boolean.parseBoolean(p.getProperty("breakBlocks", String.valueOf(breakBlocks)));
			cropsOnly = Boolean.parseBoolean(p.getProperty("cropsOnly", String.valueOf(cropsOnly)));
			matureOnly = Boolean.parseBoolean(p.getProperty("matureOnly", String.valueOf(matureOnly)));
			captchaText = p.getProperty("captchaText", captchaText);
		} catch (IOException | NumberFormatException e) {
			AutoFarmClient.LOGGER.warn("Impossible de lire {}", FILE, e);
		}
	}

	public static void save() {
		Properties p = new Properties();
		p.setProperty("leftSeconds", String.valueOf(leftSeconds));
		p.setProperty("rightSeconds", String.valueOf(rightSeconds));
		p.setProperty("cycles", String.valueOf(cycles));
		p.setProperty("breakBlocks", String.valueOf(breakBlocks));
		p.setProperty("cropsOnly", String.valueOf(cropsOnly));
		p.setProperty("matureOnly", String.valueOf(matureOnly));
		p.setProperty("captchaText", captchaText);
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
