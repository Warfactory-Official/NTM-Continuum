package com.hbm.client.render.armor;

import com.hbm.client.jetpack.JetpackClientVisuals;
import com.hbm.handler.ArmorModHandler;
import com.hbm.handler.ArmorUtil;
import com.hbm.items.armor.ArmorLiquidatorMask;
import com.hbm.items.armor.ItemModGasmask;
import com.hbm.items.armor.ItemModTesla;
import com.hbm.items.ModGearItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatHumanoidLayer;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.HumanoidPose;
import dev.golem.core.render.ModelParts;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Worn;
import dev.golem.core.world.ArmorSlot;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * The worn half of the armor modules and the Liquidator hood - 1.7's {@code ItemArmorMod.modRender}
 * calls from {@code RenderPlayerEvent.SetArmorModel} plus {@code ArmorLiquidator.getArmorModel}:
 *
 * <ul>
 * <li>an M65 over the head for the Liquidator helmet ({@code liquidator_helmet.png}) and for a helmet
 * carrying a gas-mask attachment ({@code ModelM65.png} / {@code ModelM65Mono.png}); the canister is drawn
 * only while a filter is installed, as {@code ModelM65.render} checked;</li>
 * <li>the back-mounted tesla coil on a chestplate carrying it.</li>
 * </ul>
 *
 * <p>The M65 is a {@code ModelBiped}, not a {@code ModelArmorBase}: it follows the live head and takes
 * no crouch offset. The tesla is a {@code ModelArmorBase} and takes its +0.2Y crouch offset.
 */
public class ArmorModRenderLayer extends CompatHumanoidLayer {

    private static final Identifier LIQUIDATOR = Ids.of("hbm", "textures/models/armor/liquidator_helmet.png");
    private static final Identifier M65 = Ids.of("hbm", "textures/models/armor/m65.png");
    private static final Identifier M65_MONO = Ids.of("hbm", "textures/models/armor/m65_mono.png");
    /** 1.7 {@code ArmorGasMask.getArmorTexture}: {@code models/GasMask.png} and {@code armor/mask_olde.png}. */
    private static final Identifier GAS_MASK = Ids.of("hbm", "textures/models/armor/gas_mask.png");
    private static final Identifier MASK_OLDE = Ids.of("hbm", "textures/models/armor/mask_olde.png");
    /** 1.7 {@code ArmorHazmatMask} hoods: {@code models/ModelHazRed.png} and {@code models/ModelHazGrey.png}. */
    private static final Identifier HAZ_RED = Ids.of("hbm", "textures/models/armor/haz_red.png");
    private static final Identifier HAZ_GREY = Ids.of("hbm", "textures/models/armor/haz_grey.png");
    /** {@code ModelGasMask.render}: an adult mask is drawn at 1.15. */
    private static final float GAS_MASK_SCALE = 1.15F;

    /** {@code ModelM65.render}: {@code 1/16 * 18}, then {@code 1.01} against z-fighting with the head. */
    private static final float M65_SCALE = 18F / 16F * 1.01F;

    @SuppressWarnings("rawtypes")
    public ArmorModRenderLayer(RenderLayerParent parent) {
        super(parent);
    }

    @Override
    public void submit(Worn worn, PoseStack pose, SubmitSink sink, int light, HumanoidPose parts,
            CameraInfo camera) {
        if (parts.invisible) return;

        ItemStack helmet = worn.get(ArmorSlot.HELMET);
        if (!helmet.isEmpty()) {
            boolean filter = !ArmorUtil.getGasMaskFilterRecursively(helmet).isEmpty();
            if (helmet.getItem() instanceof ArmorLiquidatorMask) m65(pose, sink, light, parts, LIQUIDATOR, filter);
            // 1.7 ArmorGasMask.getArmorModel: ModelGasMask for the gas mask, ModelM65 for the M65, olde and mono masks.
            if (helmet.getItem() == ModGearItems.gas_mask.get()) gasMask(pose, sink, light, parts);
            if (helmet.getItem() == ModGearItems.gas_mask_m65.get()) m65(pose, sink, light, parts, M65, filter);
            if (helmet.getItem() == ModGearItems.gas_mask_olde.get()) m65(pose, sink, light, parts, MASK_OLDE, filter);
            if (helmet.getItem() == ModGearItems.gas_mask_mono.get()) m65(pose, sink, light, parts, M65_MONO, filter);
            // 1.7 ArmorHazmatMask.getArmorModel: the red and grey hoods are an M65 in their own skin.
            if (helmet.getItem() == ModGearItems.hazmat_helmet_red.get()) m65(pose, sink, light, parts, HAZ_RED, filter);
            if (helmet.getItem() == ModGearItems.hazmat_helmet_grey.get()) m65(pose, sink, light, parts, HAZ_GREY, filter);
            if (ArmorModHandler.hasMods(helmet)
                    && ArmorModHandler.pryMod(helmet, ArmorModHandler.helmet_only).getItem() instanceof ItemModGasmask mask) {
                m65(pose, sink, light, parts, mask.isMono() ? M65_MONO : M65, filter);
            }
        }

        ItemStack chest = worn.chest();
        if (!chest.isEmpty() && ArmorModHandler.hasMods(chest)
                && ArmorModHandler.pryMod(chest, ArmorModHandler.plate_only).getItem() instanceof ItemModTesla) {
            ArmorRenderer tesla = ArmorRenderers.get("back_tesla");
            if (tesla == null) return;
            boolean crouching = parts.sneaking
                    && !JetpackClientVisuals.freezesPlayerPose(parts.player());
            HumanoidPose armorPose = crouching == parts.sneaking ? parts
                    : new HumanoidPose(parts.model(), false, parts.baby, parts.invisible, parts.player());
            pose.pushPose();
            if (crouching && !parts.baby) pose.translate(0F, 0.2F, 0F);
            tesla.submit(ArmorSlot.CHESTPLATE, pose, sink, light, armorPose);
            pose.popPose();
        }
    }

    /**
     * 1.7 {@code ModelGasMask.render}: a child mask at 0.75 one block down, an adult one at 1.15; the mask copies
     * the head's X/Y point, yaw and pitch like the M65 does.
     */
    private static void gasMask(PoseStack pose, SubmitSink sink, int light, HumanoidPose parts) {
        ModelPart head = parts.head();
        if (!head.visible) return;
        pose.pushPose();
        if (parts.baby) {
            pose.scale(0.75F, 0.75F, 0.75F);
            pose.translate(0F, 1F, 0F);
        } else {
            pose.scale(GAS_MASK_SCALE, GAS_MASK_SCALE, GAS_MASK_SCALE);
        }
        pose.translate(head.x / 16F, head.y / 16F, 0F);
        pose.mulPose(Axis.YP.rotation(head.yRot));
        pose.mulPose(Axis.XP.rotation(head.xRot));
        ModelParts.submit(sink, pose, CompatRenderTypes.entityCutoutNoCull(GAS_MASK), ModelGasMask.MASK, light,
                OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    private static void m65(PoseStack pose, SubmitSink sink, int light, HumanoidPose parts, Identifier texture,
                            boolean filter) {
        ModelPart head = parts.head();
        if (!head.visible) return;
        pose.pushPose();
        if (parts.baby) {
            // ModelM65.render's child branch: 0.75 scale, then one block down in model space.
            pose.scale(0.75F, 0.75F, 0.75F);
            pose.translate(0F, 1F, 0F);
        }
        pose.scale(M65_SCALE, M65_SCALE, M65_SCALE);
        // setRotationAngles copied the head's X/Y point and yaw/pitch onto the mask; Z and roll never were.
        pose.translate(head.x / 16F, head.y / 16F, 0F);
        pose.mulPose(Axis.YP.rotation(head.yRot));
        pose.mulPose(Axis.XP.rotation(head.xRot));
        var type = CompatRenderTypes.entityCutoutNoCull(texture);
        ModelParts.submit(sink, pose, type, ModelM65.MASK, light, OverlayTexture.NO_OVERLAY);
        if (filter) ModelParts.submit(sink, pose, type, ModelM65.FILTER, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
