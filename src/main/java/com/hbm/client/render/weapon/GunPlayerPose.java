package com.hbm.client.render.weapon;

import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import dev.golem.core.client.FrameTime;
import dev.golem.core.render.HumanoidPose;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Applies 1.7's held Sedna gun pose to the already prepared humanoid model. */
public final class GunPlayerPose {

    private GunPlayerPose() {}

    /**
     * Equivalent of 1.7's {@code ModEventHandlerRenderer.onRenderHeldGun}.
     *
     * <p>The source sets {@code aimedBow} on the whole model, not only on the firing arm. The current
     * renderer can also submit a gun from the offhand, so the same source pose is selected from either
     * hand; mainhand wins when both slots contain guns, matching the source's {@code getHeldItem()}.
     */
    public static void apply(HumanoidPose parts) {
        Player player = parts.player();
        if (player == null) return;

        ItemStack held = heldGun(player);
        if (held.isEmpty()) return;

        ModelPart head = parts.head();
        ModelPart rightArm = parts.rightArm();
        ModelPart leftArm = parts.leftArm();
        float age = player.tickCount + FrameTime.partialTick();
        float bobZ = (float) Math.cos(age * 0.09F) * 0.05F + 0.05F;
        float bobX = (float) Math.sin(age * 0.067F) * 0.05F;

        // These are ModelArmorBase's aimedBow values, including its post-pose arm bob.
        rightArm.xRot = -(float) Math.PI / 2F + head.xRot + bobX;
        leftArm.xRot = -(float) Math.PI / 2F + head.xRot - bobX;
        rightArm.yRot = -0.1F + head.yRot;
        leftArm.yRot = 0.1F + head.yRot + 0.4F;
        rightArm.zRot = bobZ;
        leftArm.zRot = -bobZ;

        // The source renderer uses the left arm's ordinary bow position for its two-gun layouts.
        if (isAkimbo(held)) leftArm.yRot = 0.1F + head.yRot;
    }

    private static ItemStack heldGun(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof ItemGunBaseNT) return main;
        ItemStack off = player.getOffhandItem();
        return off.getItem() instanceof ItemGunBaseNT ? off : ItemStack.EMPTY;
    }

    /** The six source ItemRenderWeaponBase registrations whose isAkimbo() returned true. */
    private static boolean isAkimbo(ItemStack stack) {
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return switch (path) {
            case "gun_light_revolver_dani", "gun_maresleg_akimbo", "gun_uzi_akimbo",
                    "gun_star_f_akimbo", "gun_minigun_dual", "gun_aberrator_eott" -> true;
            default -> false;
        };
    }
}
