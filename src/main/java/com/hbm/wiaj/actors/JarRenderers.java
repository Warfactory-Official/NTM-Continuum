package com.hbm.wiaj.actors;

import com.hbm.Constants;
import com.hbm.client.render.FoundryRenderHelper;
import com.hbm.client.render.HbmBlockEntityRenderers;
import com.hbm.client.render.StirlingRenderState;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityFoundryChannel;
import com.hbm.tileentity.machine.TileEntityFoundryOutlet;
import com.hbm.tileentity.machine.TileEntityFoundryTank;
import com.hbm.tileentity.network.TileEntityFluidDuctNT;
import com.hbm.wiaj.WorldInAJar;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.data.Tags;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.GemBER;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The bridge from the jar to the port's machine renderers.
 *
 * <p>Upstream the machine TESRs themselves implemented {@link ITileActorRenderer}: {@code RenderStirling},
 * {@code RenderCrucible} and {@code RenderFoundry} each had a {@code renderActor} next
 * to their world path. The port's renderers are declared models ({@link GemBER}) driven by a render state,
 * so an actor here fills that state from its data and replays the very renderer the placed machine uses:
 * same mesh, same parts, same animation channels. Nothing about a machine's look is restated for the jar.
 *
 * <p>Everything is drawn fully lit, as upstream set the lightmap to 240/240 before the actor pass.
 */
public final class JarRenderers {

    private JarRenderers() {}

    /** Light for every jar model: 1.7 set the lightmap to 240/240 before drawing actors. */
    public static final int FULLBRIGHT = FoundryRenderHelper.FULLBRIGHT;

    /**
     * The position render states claim. A state's position only feeds the instanced backend's ownership
     * check, and a jar model must never be mistaken for a machine the world is drawing somewhere.
     */
    private static final BlockPos OFF_WORLD = new BlockPos(0, -30_000_000, 0);

    /** The declared renderer the port registered for {@code type}, or null when it has none. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static @Nullable GemBER<BlockEntity, CompatRenderState> gem(BlockEntityType<?> type) {
        for (HbmBlockEntityRenderers.GemDeclared<?, ?> declared : HbmBlockEntityRenderers.gemDeclared()) {
            if (declared.type() == type) return (GemBER) declared.renderer().get();
        }
        return null;
    }

    /** Replays {@code renderer} for an already-filled {@code state} at the current pose. */
    public static <S extends CompatRenderState> void submit(GemBER<?, S> renderer, S state, PoseStack pose,
                                                           SubmitSink sink, CameraInfo camera) {
        state.lightCoords = FULLBRIGHT;
        state.blockPos = OFF_WORLD;
        renderer.submit(state, pose, sink, camera);
    }

    /**
     * Draws a block entity parked in the jar at {@code (x, y, z)} through its registered renderer, reading
     * it exactly as the renderer reads a placed one. The block entity takes the jar's block state for the
     * cell first, which is where its connections were worked out.
     */
    public static void drawTile(WorldInAJar world, BlockEntity tile, int x, int y, int z, PoseStack pose,
                                SubmitSink sink, CameraInfo camera, float interp) {
        GemBER<BlockEntity, CompatRenderState> renderer = gem(tile.getType());
        if (renderer == null) return;
        try {
            drawTile(renderer, world, tile, x, y, z, pose, sink, camera, interp);
        } catch (RuntimeException ex) {
            // One renderer that cannot read a block entity outside a level must not take the whole
            // presentation down; say so once per type, loudly, and draw the rest.
            if (FAILED.add(tile.getType())) {
                Constants.LOG.error("World in a jar: cannot draw {} outside a level", tile.getType(), ex);
            }
        }
    }

    private static final Set<BlockEntityType<?>> FAILED = new HashSet<>();

    private static void drawTile(GemBER<BlockEntity, CompatRenderState> renderer, WorldInAJar world, BlockEntity tile,
                                 int x, int y, int z, PoseStack pose, SubmitSink sink, CameraInfo camera, float interp) {
        // A tile standing in its own block takes that block's jar state; a tile parked where no block was
        // placed (the crucible's) keeps the state it was made with.
        if (world.getBlock(x, y, z).getBlock() == tile.getBlockState().getBlock()) {
            tile.setBlockState(world.getBlock(x, y, z));
        }
        // Renderers that draw an item (a casting's mold) extract it against the level; the tile is never
        // added to it, this only lends it one to read from.
        if (Minecraft.getInstance().level != null) tile.setLevel(Minecraft.getInstance().level);
        CompatRenderState state = renderer.createRenderState();
        renderer.extract(tile, state, interp, Vec3.ZERO);
        pose.pushPose();
        pose.translate(x, y, z);
        submit(renderer, state, pose, sink, camera);
        pose.popPose();
    }

    /**
     * Whether a tile is drawn with the jar's blocks rather than by an actor. These are the blocks 1.7 drew
     * with an ISBRH in the block pass: ducts, and the foundry's channels, tanks and outlets, whose molten
     * metal the ISBRH read from the tile, and which the port draws with a block-entity renderer instead.
     * Crucibles, basins and molds were TESRs upstream too and only ever appear through an actor.
     */
    public static boolean drawnWithBlocks(Object tile) {
        return tile instanceof TileEntityFluidDuctNT || tile instanceof TileEntityFoundryChannel
                || tile instanceof TileEntityFoundryTank || tile instanceof TileEntityFoundryOutlet;
    }

    /** 1.7 {@code rotation} (a ForgeDirection ordinal) as the yaw every actor applied. */
    public static float rotationYaw(int rotation) {
        return switch (rotation) {
            case 5 -> 90F;
            case 2 -> 180F;
            case 4 -> 270F;
            default -> 0F; // 3
        };
    }

    /// THE FOUR TESRS THAT DOUBLED AS ACTORS UPSTREAM ///

    /** 1.7 {@code RenderStirling} as an actor: the engine spun by the actor's own {@code speed}. */
    public static class Stirling implements ITileActorRenderer {

        @Override
        public void renderActor(WorldInAJar world, PoseStack pose, SubmitSink sink, CameraInfo camera, int ticks, float interp, CompoundTag data) {
            GemBER<BlockEntity, CompatRenderState> renderer = gem(ModBlockEntities.MACHINE_STIRLING.get());
            if (renderer == null) return;
            StirlingRenderState state = (StirlingRenderState) renderer.createRenderState();
            state.yaw = rotationYaw(Tags.getIntOr(data, "rotation", 0));
            float lastSpin = Tags.getFloatOr(data, "lastSpin", 0F);
            float spin = Tags.getFloatOr(data, "spin", 0F);
            state.rotation = lastSpin + (spin - lastSpin) * interp;
            state.type = Tags.getIntOr(data, "type", 0);
            state.hasCog = Tags.getBooleanOr(data, "hasCog", false);
            pose.pushPose();
            pose.translate(Tags.getDoubleOr(data, "x", 0D), Tags.getDoubleOr(data, "y", 0D), Tags.getDoubleOr(data, "z", 0D));
            submit(renderer, state, pose, sink, camera);
            pose.popPose();
        }

        @Override
        public void updateActor(int ticks, CompoundTag data) {
            spin(data);
        }
    }

    /**
     * The spin the Stirling engine used upstream, unchanged, including {@code lastSpin} starting from zero
     * rather than from the stored value, so the first tick after a wrap interpolates from {@code -360}.
     */
    private static void spin(CompoundTag data) {
        float lastSpin;
        float spin = Tags.getFloatOr(data, "spin", 0F);
        float speed = Tags.getFloatOr(data, "speed", 0F);

        lastSpin = spin;
        spin += speed;

        if(spin >= 360) {
            lastSpin -= 360;
            spin -= 360;
        }

        data.putFloat("lastSpin", lastSpin);
        data.putFloat("spin", spin);
    }

    /** 1.7 {@code RenderCrucible} as an actor: draws the crucible tile parked in the jar at x/y/z. */
    public static class Crucible implements ITileActorRenderer {

        @Override
        public void renderActor(WorldInAJar world, PoseStack pose, SubmitSink sink, CameraInfo camera, int ticks, float interp, CompoundTag data) {
            renderJarTile(world, pose, sink, camera, interp, data);
        }

        @Override
        public void updateActor(int ticks, CompoundTag data) { }
    }

    /** 1.7 {@code RenderFoundry} as an actor: draws the basin or mold tile parked in the jar at x/y/z. */
    public static class Foundry implements ITileActorRenderer {

        @Override
        public void renderActor(WorldInAJar world, PoseStack pose, SubmitSink sink, CameraInfo camera, int ticks, float interp, CompoundTag data) {
            renderJarTile(world, pose, sink, camera, interp, data);
        }

        @Override
        public void updateActor(int ticks, CompoundTag data) { }
    }

    /** {@code renderTileEntityAt(world.getTileEntity(x, y, z), x, y, z, interp)}. */
    private static void renderJarTile(WorldInAJar world, PoseStack pose, SubmitSink sink, CameraInfo camera, float interp, CompoundTag data) {
        int x = Tags.getIntOr(data, "x", 0);
        int y = Tags.getIntOr(data, "y", 0);
        int z = Tags.getIntOr(data, "z", 0);
        if (world.getTileEntity(x, y, z) instanceof BlockEntity tile) {
            drawTile(world, tile, x, y, z, pose, sink, camera, interp);
        }
    }
}
