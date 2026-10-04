package fr.ghugo.autofarm;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Statistiques de farm, lues dans le chat du serveur :
 * <ul>
 *     <li>argent et cultures : uniquement le « Bilan de moisson » (les gains des événements y sont déjà inclus) ;</li>
 *     <li>récompenses : « Tu as reçu / obtenu / gagné … » (Crystaux, LuckyBlock, Token, fragments…) ;</li>
 *     <li>événements : « La Moisson Dorée explose… », « L'Aura Solaire Suprême s'embrase… » (nombre d'apparitions).</li>
 * </ul>
 * Deux compteurs : la session en cours et le total de toutes les sessions (sauvegardé dans config/autofarm-stats.properties).
 */
public final class AutoFarmStats {
	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("autofarm-stats.properties");
	}

	public static final String DURATION = "Durée";
	public static final String MONEY = "Argent";
	public static final String MONEY_RATE = "Argent / heure";
	public static final String CROPS = "Cultures";
	public static final String CROPS_RATE = "Cultures / heure";

	/** « Bénéfices : 72 457 731.61$ ». */
	private static final Pattern PROFIT = Pattern.compile(
			"b[ée]n[ée]fices?\\s*:\\s*([0-9][0-9 \\u00a0\\u202f.,]*)\\s*\\$", Pattern.CASE_INSENSITIVE);
	/** « Cultures récoltées : 605 ». */
	private static final Pattern CROPS_HARVESTED = Pattern.compile(
			"cultures? r[ée]colt[ée]es?\\s*:\\s*([0-9][0-9 \\u00a0\\u202f.,]*)", Pattern.CASE_INSENSITIVE);
	/** « Tu as reçu +5 Crystaux ! », « Tu as reçu un LuckyBlock ! », « Tu as gagné 1 Token ! »… */
	private static final Pattern REWARD = Pattern.compile(
			"tu as (?:re[çc]u|obtenu|gagn[ée])\\s+(\\+?[0-9][0-9 \\u00a0\\u202f.,]*|une?\\s+|des\\s+)?(.+?)\\s*(?:[!.]|$)",
			Pattern.CASE_INSENSITIVE);
	/** « La Moisson Dorée explose… », « L'Aura Solaire Suprême s'embrase… » (au moins deux mots en majuscule). */
	private static final Pattern EVENT = Pattern.compile(
			"^\\s*(?:\\[[^\\]]*\\]\\s*)?(?:La|Le|Les|L['’])\\s*(\\p{Lu}[\\p{L}-]*(?:\\s+\\p{Lu}[\\p{L}-]*)+)\\s");

	/** Compteurs d'une période (session ou total). */
	public static final class Counters {
		double money;
		long crops;
		long ticks;
		final Map<String, Double> rewards = new LinkedHashMap<>();
		final Map<String, Long> events = new LinkedHashMap<>();

		void clear() {
			money = 0;
			crops = 0;
			ticks = 0;
			rewards.clear();
			events.clear();
		}

		boolean hasData() {
			return money > 0 || crops > 0 || !rewards.isEmpty() || !events.isEmpty();
		}

		double hours() {
			return ticks / 20.0 / 3600.0;
		}
	}

	public static final Counters SESSION = new Counters();
	public static final Counters TOTAL = new Counters();

	private AutoFarmStats() {
	}

	// ---- Collecte ----

	public static void resetSession() {
		SESSION.clear();
	}

	public static void resetTotal() {
		TOTAL.clear();
		save();
	}

	/** Appelé à chaque tick où le farm tourne (hors pause). */
	public static void tick() {
		SESSION.ticks++;
		TOTAL.ticks++;
		if (TOTAL.ticks % 1200 == 0) {
			save();
		}
	}

	/** Lit un message du chat (une ou plusieurs lignes). */
	public static void onChat(String text) {
		for (String line : text.split("\\R")) {
			parseLine(line);
		}
	}

	static void parseLine(String line) {
		Matcher m = PROFIT.matcher(line);
		if (m.find()) {
			double v = parseAmount(m.group(1));
			SESSION.money += v;
			TOTAL.money += v;
			return;
		}
		m = CROPS_HARVESTED.matcher(line);
		if (m.find()) {
			long v = (long) parseAmount(m.group(1));
			SESSION.crops += v;
			TOTAL.crops += v;
			return;
		}
		m = REWARD.matcher(line);
		if (m.find()) {
			String name = m.group(2).trim();
			// « Tu as reçu 625,000,000 $ » : argent d'un événement, déjà compté dans le bilan de moisson.
			if (name.isEmpty() || name.startsWith("$")) {
				return;
			}
			String amount = m.group(1);
			double v = amount == null || !Character.isDigit(amount.replace("+", "").charAt(0)) ? 1 : parseAmount(amount);
			SESSION.rewards.merge(name, v, Double::sum);
			TOTAL.rewards.merge(name, v, Double::sum);
			return;
		}
		m = EVENT.matcher(line);
		if (m.find()) {
			String name = m.group(1).trim();
			SESSION.events.merge(name, 1L, Long::sum);
			TOTAL.events.merge(name, 1L, Long::sum);
		}
	}

	/**
	 * « 72 457 731.61 » → 72457731.61, « 625,000,000 » → 625000000. Espaces = séparateurs de milliers ; un « , » ou
	 * « . » suivi de 1 ou 2 chiffres à la fin est la partie décimale, sinon c'est aussi un séparateur de milliers.
	 */
	static double parseAmount(String raw) {
		String s = raw.replaceAll("[+ \\u00a0\\u202f]", "").replaceAll("[.,]+$", "");
		Matcher decimal = Pattern.compile("[.,](\\d{1,2})$").matcher(s);
		String fraction = "";
		if (decimal.find()) {
			fraction = decimal.group(1);
			s = s.substring(0, decimal.start());
		}
		s = s.replaceAll("[.,]", "");
		if (s.isEmpty()) {
			return 0;
		}
		try {
			return Double.parseDouble(fraction.isEmpty() ? s : s + "." + fraction);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	// ---- Affichage ----

	/** Une ligne de statistiques : nom, valeur de la session, valeur totale. */
	public record Row(String name, String session, String total) {
	}

	/** Toutes les lignes (fixes puis récompenses puis événements), y compris celles masquées. */
	public static List<Row> rows() {
		List<Row> rows = new ArrayList<>();
		rows.add(new Row(DURATION, duration(SESSION), duration(TOTAL)));
		rows.add(new Row(MONEY, money(SESSION.money) + "$", money(TOTAL.money) + "$"));
		rows.add(new Row(MONEY_RATE, compact(perHour(SESSION, SESSION.money)) + "$", compact(perHour(TOTAL, TOTAL.money)) + "$"));
		rows.add(new Row(CROPS, group((long) SESSION.crops), group((long) TOTAL.crops)));
		rows.add(new Row(CROPS_RATE, compact(perHour(SESSION, SESSION.crops)), compact(perHour(TOTAL, TOTAL.crops))));
		Set<String> rewards = new LinkedHashSet<>(TOTAL.rewards.keySet());
		rewards.addAll(SESSION.rewards.keySet());
		for (String name : rewards) {
			rows.add(new Row(name, number(SESSION.rewards.getOrDefault(name, 0.0)), number(TOTAL.rewards.getOrDefault(name, 0.0))));
		}
		Set<String> events = new LinkedHashSet<>(TOTAL.events.keySet());
		events.addAll(SESSION.events.keySet());
		for (String name : events) {
			rows.add(new Row(eventLabel(name), group(SESSION.events.getOrDefault(name, 0L)), group(TOTAL.events.getOrDefault(name, 0L))));
		}
		return rows;
	}

	private static String eventLabel(String name) {
		return "Événement : " + name;
	}

	public static boolean isHidden(String rowName) {
		return AutoFarmConfig.isStatHidden(rowName);
	}

	public static void toggleHidden(String rowName) {
		AutoFarmConfig.toggleStatHidden(rowName);
		AutoFarmConfig.save();
	}

	/** Lignes visibles de la session pour le panneau à l'écran (valeurs nulles omises, sauf durée et argent). */
	public static List<String> hudLines() {
		List<String> lines = new ArrayList<>();
		for (Row row : rows()) {
			if (isHidden(row.name())) {
				continue;
			}
			boolean always = row.name().equals(DURATION) || row.name().equals(MONEY) || row.name().equals(MONEY_RATE);
			if (always || !isZero(row.session())) {
				lines.add("§7" + row.name() + " : §f" + row.session());
			}
		}
		return lines;
	}

	/** Résumé de la session (lignes visibles, valeurs non nulles) pour le chat et le téléphone. */
	public static String summary() {
		StringBuilder sb = new StringBuilder();
		for (Row row : rows()) {
			if (isHidden(row.name()) || (isZero(row.session()) && !row.name().equals(DURATION))) {
				continue;
			}
			if (!sb.isEmpty()) {
				sb.append('\n');
			}
			sb.append(row.name()).append(" : ").append(row.session());
		}
		return sb.toString();
	}

	private static boolean isZero(String value) {
		return value.replaceAll("[^0-9]", "").replace("0", "").isEmpty();
	}

	private static double perHour(Counters c, double value) {
		double h = c.hours();
		return h > 0 ? value / h : 0;
	}

	private static String duration(Counters c) {
		long seconds = c.ticks / 20;
		long h = seconds / 3600;
		long m = (seconds % 3600) / 60;
		return h > 0 ? h + "h" + String.format("%02d", m) : m + "min";
	}

	private static String money(double value) {
		return group((long) value);
	}

	private static String number(double value) {
		return value == Math.rint(value) ? group((long) value) : String.format(Locale.FRANCE, "%.2f", value);
	}

	private static String group(long value) {
		return String.format(Locale.FRANCE, "%,d", value).replace(' ', ' ').replace(' ', ' ');
	}

	/** 145000000 → « 145 M », 12500 → « 12,5 k ». */
	static String compact(double value) {
		String[] units = {"", " k", " M", " Md", " T"};
		int i = 0;
		while (Math.abs(value) >= 1000 && i < units.length - 1) {
			value /= 1000;
			i++;
		}
		return (value >= 100 || i == 0 ? String.format(Locale.FRANCE, "%.0f", value) : String.format(Locale.FRANCE, "%.1f", value)) + units[i];
	}

	// ---- Sauvegarde du total ----

	public static void load() {
		Path file = file();
		if (!Files.exists(file)) {
			return;
		}
		Properties p = new Properties();
		try (Reader reader = Files.newBufferedReader(file)) {
			p.load(reader);
			TOTAL.clear();
			TOTAL.money = Double.parseDouble(p.getProperty("money", "0"));
			TOTAL.crops = Long.parseLong(p.getProperty("crops", "0"));
			TOTAL.ticks = Long.parseLong(p.getProperty("ticks", "0"));
			for (String key : p.stringPropertyNames()) {
				if (key.startsWith("reward.")) {
					TOTAL.rewards.put(key.substring(7), Double.parseDouble(p.getProperty(key)));
				} else if (key.startsWith("event.")) {
					TOTAL.events.put(key.substring(6), Long.parseLong(p.getProperty(key)));
				}
			}
		} catch (IOException | NumberFormatException e) {
			AutoFarmClient.LOGGER.warn("Impossible de lire {}", file, e);
		}
	}

	public static void save() {
		Properties p = new Properties();
		p.setProperty("money", String.valueOf(TOTAL.money));
		p.setProperty("crops", String.valueOf(TOTAL.crops));
		p.setProperty("ticks", String.valueOf(TOTAL.ticks));
		TOTAL.rewards.forEach((k, v) -> p.setProperty("reward." + k, String.valueOf(v)));
		TOTAL.events.forEach((k, v) -> p.setProperty("event." + k, String.valueOf(v)));
		Path file = file();
		try {
			Files.createDirectories(file.getParent());
			try (Writer writer = Files.newBufferedWriter(file)) {
				p.store(writer, "Auto Farm - statistiques totales");
			}
		} catch (IOException e) {
			AutoFarmClient.LOGGER.warn("Impossible d'écrire {}", file, e);
		}
	}
}
