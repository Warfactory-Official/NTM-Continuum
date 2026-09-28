package com.hbm.client.render;

import com.hbm.client.model.WavefrontObj;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;

import java.util.Map;

/**
 * Inventory icons of the other RBMK console panels, built the way {@link RBMKGaugeIcon} builds the gauge's:
 * the panel slab (the item definition's {@code Panel} surface) plus the parts each 1.7 block's
 * {@code renderInventoryBlock} drew over it, at the placed renderer's positions and the same quarter turn.
 * The blank and display panels are the slab alone, as in 1.7.
 */
final class RBMKPanelIcon {

    private RBMKPanelIcon() {}

    static boolean handles(String animation) {
        return switch (animation) {
            case "rbmk_panel", "rbmk_key_pad", "rbmk_lever", "rbmk_numitron", "rbmk_indicator" -> true;
            default -> false;
        };
    }

    static void declare(ItemGem.Builder builder, String animation, WavefrontObj model, Surface surface,
                        Map<String, Surface> partSurfaces) {
        Surface panel = partSurfaces.get(RBMKGaugeIcon.PANEL);
        Gem.Part<ItemPose> facing = builder.joint()
                .translate(.5F, 0F, .5F)
                .rotate(Gem.Axes.Y, RBMKGaugeIcon.ICON_YAW);
        builder.child(facing, RBMKGaugeIcon.SLAB, panel, new float[][] {{0F, 0F, 0F}, {RBMKGaugeIcon.PANEL_DEPTH, 1F, 1F}})
                .translate(-.5F, 0F, -.5F);

        switch (animation) {
            case "rbmk_key_pad" -> {
                for (int i = 0; i < 4; i++) {
                    Gem.Part<ItemPose> key = facing.joint().translate(.25F, (i / 2) * -.5F + .25F, (i % 2) * -.5F + .25F);
                    builder.child(key, model.part("Socket"), surface, null);
                    // glColor3f(0.65F, 0F, 0F): every inventory button is the same dim red
                    builder.child(key, model.part("Button"), surface, null).tint(state -> 0xFFA60000);
                }
            }
            case "rbmk_lever" -> {
                for (int i = 0; i < 2; i++) {
                    Gem.Part<ItemPose> lever = facing.joint().translate(.25F, 0F, i * -.5F + .25F);
                    builder.child(lever, model.part("Base"), surface, null);
                    builder.child(lever, model.part("Lever"), surface, null);
                }
            }
            case "rbmk_numitron" -> {
                for (int i = 0; i < 2; i++) {
                    builder.child(facing.joint().translate(.25F, i * -.5F + .25F, 0F), model.all(), surface, null);
                }
            }
            case "rbmk_indicator" -> {
                // the inventory pass walks the rows with (i % 2) * 0.5 - 0.25, the mirror of the placed order
                for (int i = 0; i < 6; i++) {
                    builder.child(facing.joint().translate(.25F, (i / 2) * -.3125F + .3125F, (i % 2) * .5F - .25F),
                            model.all(), surface, null);
                }
            }
            default -> { }
        }
    }
}
