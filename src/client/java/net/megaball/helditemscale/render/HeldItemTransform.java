package net.megaball.helditemscale.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.megaball.helditemscale.config.ModConfig;
import net.minecraft.world.InteractionHand;

/**
 * The only place that applies the position/scale settings. Called from the first-person item mixin.
 * Allocation-free and does nothing when the hand is at its default settings.
 */
public final class HeldItemTransform {
	private HeldItemTransform() {
	}

	/**
	 * Pushes a new matrix and applies translation then scale for the given hand.
	 *
	 * @return true if a matrix was pushed; the caller MUST then call {@link PoseStack#popPose()}.
	 */
	public static boolean push(PoseStack poseStack, InteractionHand hand) {
		ModConfig.HandSettings settings = hand == InteractionHand.MAIN_HAND
			? ModConfig.get().mainHand
			: ModConfig.get().offHand;
		if (settings.isDefault()) {
			return false;
		}
		poseStack.pushPose();
		// Translate first, then scale: the offset is not itself multiplied by the scale.
		poseStack.translate(settings.getPosition(0), settings.getPosition(1), settings.getPosition(2));
		poseStack.scale(settings.getScale(0), settings.getScale(1), settings.getScale(2));
		return true;
	}
}
