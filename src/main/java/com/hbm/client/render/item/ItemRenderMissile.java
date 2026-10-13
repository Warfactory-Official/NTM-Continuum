package com.hbm.client.render.item;

import com.hbm.client.render.entity.MissilePronter;
import com.hbm.handler.MissileStruct;
import com.hbm.items.weapon.ItemCustomMissile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.SubmitSink;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code ItemRenderMissile}: an assembled custom missile drawn from its own parts. In the inventory it is
 * scaled to twenty pixels whatever its height, laid across the slot and turned about its long axis at one
 * degree every 25 ms; in hand and on the ground it is a fifth of its size.
 *
 * <p>1.7 only declined a stack whose struct was null, and {@code getStruct} returns a struct for every custom
 * missile, one of four empty parts for a stack with no parts recorded. Such a stack therefore drew nothing at
 * all, not its sprite: an empty slot icon. That is odd, but it is what 1.7 did, and the port keeps it.
 */
final class ItemRenderMissile implements CustomItemRenderer {

    private final MissilePronter pronter = new MissilePronter();

    @Override
    public boolean handles(ItemStack stack, Type type) {
        return ItemCustomMissile.getStruct(stack) != null;
    }

    @Override
    public boolean animated(ItemStack stack, Type type) {
        return type == Type.INVENTORY;
    }

    @Override
    public void render(ItemStack stack, Type type, PoseStack pose, SubmitSink sink, int light, int overlay,
                       @Nullable LivingEntity owner) {
        MissileStruct missile = ItemCustomMissile.getStruct(stack);
        if (missile == null) return;

        pose.pushPose();
        if (type == Type.INVENTORY) {
            double height = missile.height();
            if (height == 0D) height = 4D;
            double size = 20;
            double scale = size / height;

            pose.translate(height / 2 * scale, 0D, 0D);
            pose.mulPose(Axis.ZP.rotationDegrees(135F));
            pose.mulPose(Axis.XP.rotationDegrees(215F));
            pose.translate(7D, 14D, 0D);
            pose.scale((float) -scale, (float) -scale, (float) -scale);
            // (int) (millis / 25 % 360) about -Y: whole degrees, as the long arithmetic gave them
            pose.mulPose(Axis.YP.rotationDegrees(-(float) (System.currentTimeMillis() / 25 % 360)));
        } else {
            float s = 0.2F;
            pose.scale(s, s, s);
            pose.translate(2D, 0D, 0D);
        }
        pronter.pront(missile, pose, sink, light);
        pose.popPose();
    }
}
