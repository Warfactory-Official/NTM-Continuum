package com.hbm.client.render;

import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.MachineCore;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Generic renderer for a HBM machine that is one or more OBJ bodies standing still, replacing the old
 * per-machine {@code TileEntitySpecialRenderer}s. Animated arms/heads belong to their own renderers.
 *
 * <p>It is attached only to the multiblock <b>core</b> BlockEntity, so it declares once per machine and
 * the model spans the dummy footprint. Facing comes from the core's own state, through {@link com.hbm.tileentity.MachineCore#facingOrdinal}.
 *
 * <p>A machine that {@linkplain #ignoreFacing does not turn with its facing} states its base yaw as a
 * fixed rotation rather than reading a render state that is always zero, which keeps its model rigid.
 */
public class MachineObjRenderer<T extends BlockEntity> extends GemBER<T, MachineRenderState> {

    /**
     * A model file + the standalone texture it samples + its own vertical offset (block units) + the
     * set of OBJ parts to skip (e.g. dynamic fluid overlays, or a frame that only shows conditionally),
     * and whether the old renderer drew the model with face culling disabled.
     */
    public record ModelDef(Identifier obj, Identifier texture, float yOffset, Set<String> exclude, boolean noCull) {
        public ModelDef(Identifier obj, Identifier texture) { this(obj, texture, 0F, Set.of(), false); }
        public ModelDef(Identifier obj, Identifier texture, float yOffset) { this(obj, texture, yOffset, Set.of(), false); }
        public ModelDef(Identifier obj, Identifier texture, Set<String> exclude) { this(obj, texture, 0F, exclude, false); }
        public ModelDef(Identifier obj, Identifier texture, boolean noCull) { this(obj, texture, 0F, Set.of(), noCull); }
        public ModelDef(Identifier obj, Identifier texture, float yOffset, Set<String> exclude) {
            this(obj, texture, yOffset, exclude, false);
        }
    }

    private record Body(WavefrontObj model, Identifier texture, float yOffset, Set<String> exclude, boolean noCull) {}

    private final List<Body> bodies;
    private final float baseYaw;
    private final float scale;
    private final float offsetX;
    private final float offsetZ;
    private final Set<String> frameParts;
    private final int frameHeight;
    /** When true the facing rotation is skipped (old all-zero yaw maps, e.g. the oilburner). */
    private boolean ignoreFacing = false;
    private boolean doubleSided = false;

    public MachineObjRenderer(float baseYaw, float scale, ModelDef... defs) {
        this(baseYaw, scale, 0F, 0F, Set.of(), 0, defs);
    }

    public MachineObjRenderer(float baseYaw, float scale, Set<String> frameParts, int frameHeight, ModelDef... defs) {
        this(baseYaw, scale, 0F, 0F, frameParts, frameHeight, defs);
    }

    /**
     * @param offsetX,offsetZ a translation applied <b>after</b> the facing rotation (block units, in the
     *                        rotated model frame) — the 1.12.2 footprint offset for asymmetric machines
     *                        (whose model isn't centred on the core cell). {@code 0,0} for centred machines.
     * @param frameParts OBJ parts drawn only when a block sits {@code frameHeight} above the core (the
     *                   1.12.2 "frame" condition); skipped otherwise. Empty = no conditional parts.
     */
    public MachineObjRenderer(float baseYaw, float scale, float offsetX, float offsetZ, Set<String> frameParts, int frameHeight, ModelDef... defs) {
        this.baseYaw = baseYaw;
        this.scale = scale;
        this.offsetX = offsetX;
        this.offsetZ = offsetZ;
        this.frameParts = frameParts;
        this.frameHeight = frameHeight;
        List<Body> built = new ArrayList<>();
        for (ModelDef d : defs) {
            built.add(new Body(WavefrontObj.load(d.obj()), d.texture(), d.yOffset(), d.exclude(), d.noCull()));
        }
        this.bodies = built;
    }

    /** Old machines whose StaticTesrBakedModels yaw map is all zeros render facing-independent. */
    public MachineObjRenderer<T> ignoreFacing() {
        this.ignoreFacing = true;
        return this;
    }

    /** Emits reversed faces for thin legacy OBJ geometry that was registered with doubleSided(). */
    public MachineObjRenderer<T> doubleSided() {
        this.doubleSided = true;
        return this;
    }

    /**
     * Each body twice where a machine has a conditional frame: once without those parts, once with only
     * them behind the condition. A bucket's contents have to be settled before the first frame, so the
     * choice is a visibility flag on a second bucket rather than a per-frame group subtraction.
     */
    @Override
    protected void setup(Gem.Model<MachineRenderState> declaration) {
        declaration.offScreen().viewDistance(256);
        for (Body body : bodies) {
            Surface surface = surfaceOf(body);
            place(declaration.part(body.model().allExcept(hiddenIn(body), doubleSided), surface), body);
            Set<String> frame = frameIn(body);
            if (!frame.isEmpty()) {
                place(declaration.part(body.model().parts(frame, doubleSided), surface), body)
                        .visibleIf(state -> state.showFrame);
            }
        }
    }

    /** Centre on the cell, turn to face, step off the centre, then scale. */
    private Gem.Part<MachineRenderState> place(Gem.Part<MachineRenderState> part, Body body) {
        part.translate(0.5F, body.yOffset(), 0.5F);
        if (ignoreFacing) {
            part.rotate(Gem.Axes.Y, baseYaw);
        } else {
            part.angle(Gem.Axes.Y, state -> baseYaw + state.yaw);
        }
        if (offsetX != 0F || offsetZ != 0F) part.translate(offsetX, 0F, offsetZ);
        if (scale != 1F) part.scale(scale);
        return part;
    }

    /**
     * entityCutout (alpha test) not entitySolid: the old TESRs ran with GL alpha-test on, so
     * fully-transparent texels (e.g. the chemical plant's glass) are discarded instead of being drawn
     * as opaque black. A double-sided body carries its reversed faces in the mesh, so it wants the
     * culling type rather than the no-cull one.
     */
    private Surface surfaceOf(Body body) {
        if (doubleSided) return Surface.cutoutCull(body.texture().toString());
        return body.noCull() ? Surface.cutoutNoCull(body.texture().toString())
                : Surface.cutout(body.texture().toString());
    }

    private Set<String> hiddenIn(Body body) {
        if (frameParts.isEmpty()) return body.exclude();
        Set<String> hidden = new LinkedHashSet<>(body.exclude());
        hidden.addAll(frameParts);
        return hidden;
    }

    private Set<String> frameIn(Body body) {
        Set<String> frame = new LinkedHashSet<>(frameParts);
        frame.retainAll(body.model().partNames());
        frame.removeAll(body.exclude());
        return frame;
    }

    @Override
    public MachineRenderState createRenderState() {
        return new MachineRenderState();
    }

    /**
     * {@code meta - offset} is the ForgeDirection ordinal of the facing: N=2, S=3, W=4, E=5. The frame,
     * where a machine has one, shows only when a block sits {@code frameHeight} above the core, which is
     * the 1.12.2 {@code frame = blockState(pos.up(3)) != AIR} check.
     */
    @Override
    public void extract(T blockEntity, MachineRenderState state, float partialTicks, Vec3 cameraPosition) {
        BlockState bs = blockEntity.getBlockState();
        int meta = MachineCore.facingOrdinal(bs, 2);
        state.yaw = switch (meta) {
            case 4 -> 90F;
            case 3 -> 180F;
            case 5 -> 270F;
            default -> 0F;
        };
        state.showFrame = frameParts.isEmpty() || (blockEntity.getLevel() != null
                && !blockEntity.getLevel().getBlockState(blockEntity.getBlockPos().above(frameHeight)).isAir());
    }
}
