package com.hbm.client.render.item;

import com.hbm.client.model.WavefrontObj;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.SubmitSink;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import static com.hbm.client.render.item.CustomItemRenderers.*;

/**
 * 1.7 {@code ItemRenderGavel}: the four gavels share one OBJ in four skins; the mese gavel is drawn twice the
 * size. A dropped gavel is shifted half a unit and then posed like the held one, hence the fall-through.
 */
final class ItemRenderGavel implements CustomItemRenderer {

    private static final Identifier WOOD = tex("textures/models/weapons/gavel_wood.png");
    private static final Identifier LEAD = tex("textures/models/weapons/gavel_lead.png");
    private static final Identifier DIAMOND = tex("textures/models/weapons/gavel_diamond.png");
    private static final Identifier MESE = tex("textures/models/weapons/gavel_mese.png");

    private final WavefrontObj gavel = obj("models/weapons/gavel.obj");

    @Override
    public boolean handles(ItemStack stack, Type type) {
        return type != Type.INVENTORY;
    }

    @Override
    public void render(ItemStack stack, Type type, PoseStack pose, SubmitSink sink, int light, int overlay,
                       @Nullable LivingEntity owner) {
        String id = id(stack);
        Identifier skin = switch (id) {
            case "lead_gavel" -> LEAD;
            case "diamond_gavel" -> DIAMOND;
            case "mese_gavel" -> MESE;
            default -> WOOD;
        };
        boolean mese = id.equals("mese_gavel");

        pose.pushPose();
        switch (type) {
            case EQUIPPED_FIRST_PERSON -> {
                pose.translate(1D, 0.5D, 0D);
                // 1.7 read Minecraft.thePlayer; in first person the owner is that player
                if (owner != null && owner.isBlocking()) pose.translate(-0.5D, 0D, 0D);
                pose.mulPose(Axis.ZP.rotationDegrees(45F));
                pose.mulPose(Axis.YP.rotationDegrees(90F));
                if (mese) pose.scale(2F, 2F, 2F);
            }
            case ENTITY, EQUIPPED -> {
                if (type == Type.ENTITY) pose.translate(-0.5D, 0D, 0D);
                pose.scale(0.5F, 0.5F, 0.5F);
                pose.mulPose(Axis.ZP.rotationDegrees(45F));
                pose.translate(1.375D, 0D, 0D);
                pose.mulPose(Axis.YP.rotationDegrees(90F));
                if (mese) {
                    pose.scale(2F, 2F, 2F);
                    pose.translate(0D, 0.25D, 0D);
                }
            }
            default -> { }
        }
        all(pose, sink, gavel, culled(skin), light, overlay);
        pose.popPose();
    }
}
