package com.hbm.client.render.armor;

import com.hbm.client.model.WavefrontObj;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import dev.golem.core.world.ArmorSlot;
import net.minecraft.resources.Identifier;

/**
 * 1.7 {@code ModelNo9}, the mining helmet: {@code Helmet} and {@code Insignia} with their own textures, and while the
 * helmet's {@code isOn} flag is set the {@code Flame} group untextured in (1, 1, 0.8), unlit at full brightness.
 * The flag is read off the worn stack by {@link WornModelRenderLayer} just before the submit.
 */
public final class No9ArmorRenderer extends ArmorRenderer {

    private static final Identifier OBJ = Ids.of("hbm", "models/armor/no9.obj");

    /** Set by the layer for the stack being drawn; the replay reads it synchronously inside {@code submit}. */
    static boolean lampOn;

    @Override
    protected void setup(Gem.Model<ArmorPose> declaration) {
        WavefrontObj model = WavefrontObj.load(OBJ);
        piece(declaration, ArmorSlot.HELMET, model, "Helmet", Surface.cutout("hbm:textures/armor/no9.png"), Pivot.HEAD);
        piece(declaration, ArmorSlot.HELMET, model, "Insignia",
                Surface.cutout("hbm:textures/armor/no9_insignia.png"), Pivot.HEAD);
        declaration.part(model.part("Flame"), Surface.emissive("hbm:textures/models/misc/white.png"))
                .visibleIf(state -> lampOn && state.slot == ArmorSlot.HELMET)
                .pose(state -> Pivot.HEAD.pose(state.wearer))
                .scale(1F / 16F)
                .tint(state -> 0xFFFFFFCC)
                .light(state -> 0xF000F0);
    }
}
