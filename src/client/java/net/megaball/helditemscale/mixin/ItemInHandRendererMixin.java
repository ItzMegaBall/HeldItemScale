package net.megaball.helditemscale.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.megaball.helditemscale.render.HeldItemTransform;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Wraps the single call that draws the held item model inside the first-person
 * {@code ItemInHandRenderer.renderArmWithItem(...)}.
 *
 * <ul>
 *   <li>The arm is drawn by other calls in that method, so it is never scaled.</li>
 *   <li>{@code renderArmWithItem} only runs for the local player's first-person hands, so third-person,
 *       inventory, GUI, dropped items, item frames and other players are never touched.</li>
 *   <li>The hand comes from the method's own {@link InteractionHand} argument, not the hotbar slot.</li>
 *   <li>The matrix pushed here is popped in a finally block, right after the original call returns.</li>
 *   <li>It wraps (not replaces) the call, so other mods' injections around it still run.</li>
 * </ul>
 */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
	@WrapOperation(
		method = "renderArmWithItem",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"
		)
	)
	private void helditemscale$transformHeldItem(
		ItemInHandRenderer instance,
		LivingEntity entity,
		ItemStack stack,
		ItemDisplayContext displayContext,
		PoseStack poseStack,
		SubmitNodeCollector collector,
		int light,
		Operation<Void> original,
		@Local(argsOnly = true) InteractionHand hand
	) {
		boolean pushed = HeldItemTransform.push(poseStack, hand);
		try {
			original.call(instance, entity, stack, displayContext, poseStack, collector, light);
		} finally {
			if (pushed) {
				poseStack.popPose();
			}
		}
	}
}
