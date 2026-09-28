package fr.ghugo.autofarm;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
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

/** Machine à états : gauche pendant X s, droite pendant Y s, en cassant les cultures visées. */
public final class AutoFarmController {
	private enum Phase { LEFT, RIGHT }

	private static boolean running;
	private static Phase phase = Phase.LEFT;
	private static int ticksLeftInPhase;
	private static int cyclesDone;

	private AutoFarmController() {
	}

	public static boolean isRunning() {
		return running;
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
		phase = Phase.LEFT;
		ticksLeftInPhase = toTicks(AutoFarmConfig.leftSeconds);
		cyclesDone = 0;
		mc.gui.getChat().addMessage(Component.literal("§a[Auto Farm] Démarré (touche J pour arrêter)."));
	}

	public static void stop(Minecraft mc, String message) {
		if (!running) {
			return;
		}
		running = false;
		releaseKeys(mc);
		if (message != null) {
			mc.gui.getChat().addMessage(Component.literal("§e[Auto Farm] " + message));
		}
	}

	/** Arrête le mod si le message reçu contient le texte du captcha. */
	public static void onChatMessage(Minecraft mc, Component message) {
		String trigger = AutoFarmConfig.captchaText;
		if (!running || trigger.isEmpty()) {
			return;
		}
		String text = ChatFormatting.stripFormatting(message.getString());
		if (text != null && text.toLowerCase(Locale.ROOT).contains(trigger.toLowerCase(Locale.ROOT))) {
			stop(mc, "Arrêté : captcha détecté. Faites le captcha puis appuyez sur J pour reprendre.");
			mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.0F));
		}
	}

	/** Appelé au début de chaque tick client (20 fois par seconde). */
	public static void tick(Minecraft mc) {
		if (!running) {
			return;
		}
		if (mc.player == null || mc.level == null || mc.gameMode == null) {
			running = false;
			return;
		}
		if (!mc.player.isAlive()) {
			stop(mc, "Arrêté (joueur mort).");
			return;
		}
		if (ticksLeftInPhase <= 0) {
			if (phase == Phase.LEFT) {
				phase = Phase.RIGHT;
				ticksLeftInPhase = toTicks(AutoFarmConfig.rightSeconds);
			} else {
				cyclesDone++;
				if (AutoFarmConfig.cycles > 0 && cyclesDone >= AutoFarmConfig.cycles) {
					stop(mc, "Terminé (" + cyclesDone + " aller(s)-retour(s)).");
					return;
				}
				phase = Phase.LEFT;
				ticksLeftInPhase = toTicks(AutoFarmConfig.leftSeconds);
			}
		}

		mc.options.keyLeft.setDown(phase == Phase.LEFT);
		mc.options.keyRight.setDown(phase == Phase.RIGHT);

		if (AutoFarmConfig.breakBlocks) {
			breakTarget(mc);
		} else {
			mc.options.keyAttack.setDown(false);
		}

		if (ticksLeftInPhase % 10 == 0) {
			String dir = phase == Phase.LEFT ? "← Gauche" : "Droite →";
			String cycleInfo = AutoFarmConfig.cycles > 0
					? " | cycle " + (cyclesDone + 1) + "/" + AutoFarmConfig.cycles
					: " | cycle " + (cyclesDone + 1);
			mc.gui.setOverlayMessage(Component.literal(
					"§6Auto Farm §f" + dir + " §7" + String.format("%.1f", ticksLeftInPhase / 20.0) + "s" + cycleInfo), false);
		}

		ticksLeftInPhase--;
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
