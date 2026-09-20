package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.Ids;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Geometry;
import dev.golem.core.render.gem.Quads;
import dev.golem.core.render.gem.Surface;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import org.joml.Vector3fc;

/** The four-section, non-piped fuel-channel inventory model from {@code RBMKColumnModel.Kind.FUEL}. */
public final class RBMKColumnItemModel {

    private static final WavefrontObj ELEMENT =
            WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/rbmk/rbmk_element.obj"));
    private static final WavefrontObj RODS =
            WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/rbmk/rbmk_element_rods.obj"));
    private static final int SECTIONS = 4;

    private final float scale, yaw, lift;
    private final ItemGem body;

    public RBMKColumnItemModel(Identifier side, Identifier top, Identifier inner, Identifier fuel, float scale, float yaw, float lift) {
        this.scale = scale;
        this.yaw = yaw;
        this.lift = lift;
        this.body = declare(skin(side), skin(top), skin(inner), skin(fuel));
    }

    /**
     * The item definitions name atlas sprites ({@code hbm:blocks/rbmk/rbmk_element_side}) because that is
     * how every other model JSON spells textures, but these ids are bound directly against the resource
     * pack, which needs the full {@code textures/….png} path. Accept both spellings.
     */
    public static Identifier normalize(Identifier id) {
        if (id == null) return null;
        String path = id.getPath();
        return path.startsWith("textures/") && path.endsWith(".png") ? id
                : Ids.of(id.getNamespace(), "textures/" + path + ".png");
    }

    static Surface skin(Identifier texture) {
        return Surface.cutout(normalize(texture).toString());
    }

    /**
     * Four casing sections, each with the element's inner tube and cap inside it.
     *
     * <p>Two framing details survive from the baked model this replaces. The casing is one extent, not
     * four — the sections tile, so the column's box is recorded against the first of them. And the OBJ
     * parts are authored centred on the origin (x/z in [-0.5, 0.5]) while the casing boxes span [0, 1],
     * so they are shifted into block space or they ride half a block off the shell.
     */
    private static ItemGem declare(Surface side, Surface top, Surface inner, Surface fuel) {
        ItemGem.Builder builder = ItemGem.builder();
        for (int section = 0; section < SECTIONS; section++) {
            builder.part(shell(section), side,
                    section == 0 ? RBMKColumnGeometry.bounds(0F, 0F, 0F, 1F, SECTIONS, 1F) : null);
            builder.part(ELEMENT.part("Inner"), inner, ELEMENT.boundsIncluding(Set.of("Inner")))
                    .translate(.5F, section, .5F);
            builder.part(ELEMENT.part("Cap"), top, ELEMENT.boundsIncluding(Set.of("Cap")))
                    .translate(.5F, section, .5F);
            builder.part(RODS.all(), fuel, null).translate(.5F, section, .5F);
        }
        return builder.build();
    }

    private static Geometry shell(int section) {
        return RBMKColumnGeometry.sides(Quads.named(Constants.MOD_ID + ":rbmk_fuel_shell_" + section),
                0F, section, 0F, 1F, section + 1F, 1F).build();
    }

    /** {@code RenderRBMKRod:36-37}: fuel columns are non-piped and sit 0.675 below the item origin. */
    public void submit(PoseStack pose, SubmitSink sink, int light, int overlay) {
        pose.pushPose();
        RBMKItemModelBounds.applyColumnTransform(pose, scale, yaw, lift);
        body.submit(pose, sink, light, overlay);
        pose.popPose();
    }

    public void extents(Consumer<Vector3fc> output) {
        PoseStack pose = new PoseStack();
        RBMKItemModelBounds.applyColumnTransform(pose, scale, yaw, lift);
        body.extents(pose, 0F, output);
    }
}
