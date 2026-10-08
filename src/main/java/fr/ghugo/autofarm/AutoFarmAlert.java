package fr.ghugo.autofarm;

import fr.ghugo.autofarm.compat.Compat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.util.Locale;

/** Prévient le joueur quand il n'est pas devant l'écran (captcha). */
public final class AutoFarmAlert {
	private static final float SAMPLE_RATE = 44100f;

	private AutoFarmAlert() {
	}

	public static void trigger(Minecraft mc, String message) {
		mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.0F));
		Compat.title(mc, Component.literal("§c§lCAPTCHA !"),
				Component.literal("§fAuto Farm en pause — " + AutoFarmClient.pauseKey() + " pour reprendre"));
		// Fait clignoter l'icône de Minecraft dans la barre des tâches.
		long window = GLFW.glfwGetCurrentContext();
		if (window != 0L) {
			GLFW.glfwRequestWindowAttention(window);
		}

		if (AutoFarmConfig.loudAlarm) {
			Thread thread = new Thread(AutoFarmAlert::playAlarm, "autofarm-alarm");
			thread.setDaemon(true);
			thread.start();
		}
		AutoFarmPhone.send("Auto Farm - CAPTCHA", message, true);
		if (AutoFarmConfig.desktopNotification) {
			Thread thread = new Thread(() -> notifyDesktop(message), "autofarm-notify");
			thread.setDaemon(true);
			thread.start();
		}
	}

	/** Bips à plein volume, joués directement par Java (indépendant du volume de Minecraft). */
	private static void playAlarm() {
		AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
		try (SourceDataLine line = AudioSystem.getSourceDataLine(format)) {
			line.open(format);
			line.start();
			for (int i = 0; i < 12; i++) {
				writeTone(line, i % 2 == 0 ? 880 : 1320, 0.25);
				writeTone(line, 0, 0.12);
			}
			line.drain();
		} catch (Exception e) {
			AutoFarmClient.LOGGER.warn("Impossible de jouer l'alarme", e);
		}
	}

	private static void writeTone(SourceDataLine line, double frequency, double seconds) {
		int samples = (int) (SAMPLE_RATE * seconds);
		// Volume au carré : la sensation de volume suit mieux le curseur.
		double amplitude = AutoFarmConfig.alarmVolume * AutoFarmConfig.alarmVolume * 0.9;
		byte[] buffer = new byte[samples * 2];
		for (int i = 0; i < samples; i++) {
			short value = frequency <= 0 ? 0
					: (short) (Math.sin(2 * Math.PI * frequency * i / SAMPLE_RATE) * amplitude * Short.MAX_VALUE);
			buffer[2 * i] = (byte) value;
			buffer[2 * i + 1] = (byte) (value >> 8);
		}
		line.write(buffer, 0, buffer.length);
	}

	/** Notification du système d'exploitation (Windows, macOS ou Linux). */
	private static void notifyDesktop(String message) {
		String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		String safe = message.replace("'", " ").replace("\"", " ");
		try {
			if (os.contains("win")) {
				String script = "Add-Type -AssemblyName System.Windows.Forms; Add-Type -AssemblyName System.Drawing; "
						+ "$n = New-Object System.Windows.Forms.NotifyIcon; "
						+ "$n.Icon = [System.Drawing.SystemIcons]::Warning; $n.Visible = $true; "
						+ "$n.ShowBalloonTip(15000, 'Auto Farm - CAPTCHA', '" + safe + "', 'Warning'); "
						+ "Start-Sleep -Seconds 15; $n.Dispose()";
				new ProcessBuilder("powershell", "-NoProfile", "-WindowStyle", "Hidden", "-Command", script).start();
			} else if (os.contains("mac")) {
				new ProcessBuilder("osascript", "-e",
						"display notification \"" + safe + "\" with title \"Auto Farm - CAPTCHA\" sound name \"Sosumi\"").start();
			} else {
				new ProcessBuilder("notify-send", "-u", "critical", "Auto Farm - CAPTCHA", safe).start();
			}
		} catch (Exception e) {
			AutoFarmClient.LOGGER.warn("Impossible d'afficher la notification", e);
		}
	}
}
