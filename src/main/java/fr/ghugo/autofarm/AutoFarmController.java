package fr.ghugo.autofarm;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Locale;

/**
 * Machine à états : dans une boucle, N allers-retours (gauche pendant X s, droite pendant Y s) en cassant les
 * cultures visées, puis commande de fin de boucle et attente, et on recommence selon le nombre de boucles.
 */
public final class AutoFarmController {
	private enum Phase { LEFT, RIGHT, WAIT }

	private static boolean running;
	private static boolean paused;
	private static Phase phase = Phase.LEFT;
	private static int ticksLeftInPhase;
	/** Allers-retours terminés dans la boucle en cours. */
	private static int tripsDone;
	/** Boucles terminées. */
	private static int loopsDone;
	private static int pausedTicks;

	private AutoFarmController() {
	}

	public static boolean isRunning() {
		return running;
	}

	public static boolean isPaused() {
		return running && paused;
	}

	public static void toggle(Minecraft mc) {
		if (running) {
			stop(mc, "Auto Farm arrêté.");
		} else {
			start(mc);
		}
	}

	public static void start(Minecraft mc) {
		if (mc.player == null) {
			return;
		}
		running = true;
		paused = false;
		phase = Phase.LEFT;
		ticksLeftInPhase = toTicks(AutoFarmConfig.leftSeconds);
		tripsDone = 0;
		loopsDone = 0;
		chat(mc, "§a[Auto Farm] Démarré (" + AutoFarmClient.pauseKey() + " : pause, " + AutoFarmClient.toggleKey() + " : arrêter).");
	}

	public static void stop(Minecraft mc, String message) {
		if (!running) {
			return;
		}
		running = false;
		paused = false;
		releaseKeys(mc);
		if (message != null) {
			chat(mc, "§e[Auto Farm] " + message);
		}
	}

	/** Met en pause en gardant la progression (phase, temps restant, allers-retours, boucles). */
	public static void pause(Minecraft mc, String message) {
		if (!running || paused) {
			return;
		}
		paused = true;
		pausedTicks = 0;
		releaseKeys(mc);
		chat(mc, "§e[Auto Farm] " + message);
	}

	/** Reprend exactement là où la pause a eu lieu. */
	public static void resume(Minecraft mc) {
		if (!running || !paused) {
			return;
		}
		paused = false;
		chat(mc, "§a[Auto Farm] Reprise (" + progress() + ").");
	}

	public static void togglePause(Minecraft mc) {
		if (!running) {
			chat(mc, "§7[Auto Farm] Rien à mettre en pause : appuyez sur " + AutoFarmClient.toggleKey() + " pour démarrer.");
		} else if (paused) {
			resume(mc);
		} else {
			pause(mc, "En pause (" + AutoFarmClient.pauseKey() + " pour reprendre).");
		}
	}

	/** Met en pause si le message reçu contient le texte du captcha. */
	public static void onChatMessage(Minecraft mc, Component message) {
		String trigger = AutoFarmConfig.captchaText;
		if (!running || paused || trigger.isEmpty()) {
			return;
		}
		String text = ChatFormatting.stripFormatting(message.getString());
		if (text != null && text.toLowerCase(Locale.ROOT).contains(trigger.toLowerCase(Locale.ROOT))) {
			pause(mc, "En pause : captcha détecté. Faites le captcha puis appuyez sur " + AutoFarmClient.pauseKey() + " pour reprendre.");
			AutoFarmAlert.trigger(mc, "Captcha détecté, le farm est en pause.");
		}
	}

	/** Appelé au début de chaque tick client (20 fois par seconde). */
	public static void tick(Minecraft mc) {
		if (!running) {
			return;
		}
		if (mc.player == null || mc.level == null || mc.gameMode == null) {
			running = false;
			paused = false;
			return;
		}
		if (!mc.player.isAlive()) {
			stop(mc, "Arrêté (joueur mort).");
			return;
		}
		if (paused) {
			releaseKeys(mc);
			if (pausedTicks++ % 40 == 0) {
				mc.gui.setOverlayMessage(Component.literal("§eAuto Farm en pause §7(" + progress() + ") §f— " + AutoFarmClient.pauseKey() + " pour reprendre"), false);
			}
			return;
		}

		if (ticksLeftInPhase <= 0 && !nextPhase(mc)) {
			return;
		}

		boolean moving = phase != Phase.WAIT;
		mc.options.keyLeft.setDown(phase == Phase.LEFT);
		mc.options.keyRight.setDown(phase == Phase.RIGHT);

		if (moving && AutoFarmConfig.breakBlocks) {
			breakTarget(mc);
		} else {
			mc.options.keyAttack.setDown(false);
		}

		if (ticksLeftInPhase % 10 == 0) {
			String what = switch (phase) {
				case LEFT -> "← Gauche";
				case RIGHT -> "Droite →";
				case WAIT -> "Attente";
			};
			mc.gui.setOverlayMessage(Component.literal("§6Auto Farm §f" + what + " §7"
					+ String.format("%.1f", ticksLeftInPhase / 20.0) + "s | " + progress()), false);
		}

		ticksLeftInPhase--;
	}

	/** Passe à la phase suivante. Retourne false si le farm vient de se terminer. */
	private static boolean nextPhase(Minecraft mc) {
		switch (phase) {
			case LEFT -> setPhase(Phase.RIGHT, AutoFarmConfig.rightSeconds);
			case RIGHT -> {
				tripsDone++;
				if (tripsDone < AutoFarmConfig.trips) {
					setPhase(Phase.LEFT, AutoFarmConfig.leftSeconds);
					break;
				}
				// Fin de la boucle.
				loopsDone++;
				tripsDone = 0;
				boolean sent = sendEndCommand(mc);
				if (AutoFarmConfig.loops > 0 && loopsDone >= AutoFarmConfig.loops) {
					stop(mc, "Terminé (" + loopsDone + " boucle(s) de " + AutoFarmConfig.trips + " aller(s)-retour(s)).");
					return false;
				}
				if (sent && AutoFarmConfig.endWaitSeconds > 0) {
					setPhase(Phase.WAIT, AutoFarmConfig.endWaitSeconds);
				} else {
					setPhase(Phase.LEFT, AutoFarmConfig.leftSeconds);
				}
			}
			case WAIT -> setPhase(Phase.LEFT, AutoFarmConfig.leftSeconds);
		}
		return true;
	}

	private static void setPhase(Phase next, double seconds) {
		phase = next;
		ticksLeftInPhase = toTicks(seconds);
	}

	/** Envoie la ou les commandes de fin de boucle (séparées par « ; »). */
	private static boolean sendEndCommand(Minecraft mc) {
		boolean sent = false;
		for (String part : AutoFarmConfig.endCommand.split(";")) {
			String cmd = part.trim();
			if (cmd.isEmpty() || mc.player == null) {
				continue;
			}
			if (cmd.startsWith("/")) {
				mc.player.connection.sendCommand(cmd.substring(1));
			} else {
				mc.player.connection.sendChat(cmd);
			}
			sent = true;
		}
		return sent;
	}

	private static String progress() {
		String loopInfo = AutoFarmConfig.loops > 0
				? (loopsDone + 1) + "/" + AutoFarmConfig.loops
				: (loopsDone + 1) + "/∞";
		return "aller-retour " + (tripsDone + 1) + "/" + AutoFarmConfig.trips + " | boucle " + loopInfo;
	}

	/** Texte d'état pour le menu. */
	public static String status() {
		if (!running) {
			return "§7Arrêté";
		}
		return (paused ? "§eEn pause" : "§aEn cours") + " §7(" + progress() + ")";
	}

	private static void chat(Minecraft mc, String message) {
		mc.gui.getChat().addMessage(Component.literal(message));
	}

	/**
	 * Casse le bloc visé s'il doit l'être. Sans menu ouvert, on maintient le clic gauche (comportement vanilla).
	 * Avec un menu ouvert (Échap, inventaire...), Minecraft ignore le clic gauche : on casse alors directement
	 * les blocs qui se cassent en un coup (cultures).
	 */
	private static void breakTarget(Minecraft mc) {
		boolean attack = false;
		if (mc.hitResult instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
			BlockPos pos = blockHit.getBlockPos();
			BlockState state = mc.level.getBlockState(pos);
			attack = shouldBreak(state);
			if (attack && mc.screen != null && state.getDestroyProgress(mc.player, mc.level, pos) >= 1.0F) {
				if (mc.gameMode.startDestroyBlock(pos, blockHit.getDirection())) {
					mc.player.swing(InteractionHand.MAIN_HAND);
				}
			}
		}
		mc.options.keyAttack.setDown(attack && mc.screen == null);
	}

	private static boolean shouldBreak(BlockState state) {
		if (state.isAir()) {
			return false;
		}
		Block block = state.getBlock();
		boolean crop = block instanceof CropBlock
				|| block instanceof NetherWartBlock
				|| block instanceof CocoaBlock
				|| block instanceof SugarCaneBlock
				|| block instanceof CactusBlock
				|| block instanceof BambooStalkBlock
				|| block == Blocks.MELON
				|| block == Blocks.PUMPKIN;
		if (AutoFarmConfig.cropsOnly && !crop) {
			return false;
		}
		if (AutoFarmConfig.matureOnly) {
			if (block instanceof CropBlock cropBlock) {
				return cropBlock.isMaxAge(state);
			}
			if (block instanceof NetherWartBlock) {
				return state.getValue(NetherWartBlock.AGE) >= 3;
			}
			if (block instanceof CocoaBlock) {
				return state.getValue(CocoaBlock.AGE) >= 2;
			}
		}
		return true;
	}

	private static void releaseKeys(Minecraft mc) {
		mc.options.keyLeft.setDown(false);
		mc.options.keyRight.setDown(false);
		mc.options.keyAttack.setDown(false);
	}

	private static int toTicks(double seconds) {
		return Math.max(1, (int) Math.round(seconds * 20.0));
	}
}
