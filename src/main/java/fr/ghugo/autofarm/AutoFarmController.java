package fr.ghugo.autofarm;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
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
		// Un menu est ouvert (inventaire, chat, pause...) : on met en pause sans avancer le chrono.
		if (mc.screen != null) {
			releaseKeys(mc);
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

	/** Maintient le clic gauche uniquement quand le bloc visé doit être cassé. */
	private static void breakTarget(Minecraft mc) {
		boolean attack = false;
		if (mc.hitResult instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
			attack = shouldBreak(mc.level.getBlockState(blockHit.getBlockPos()));
		}
		mc.options.keyAttack.setDown(attack);
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
