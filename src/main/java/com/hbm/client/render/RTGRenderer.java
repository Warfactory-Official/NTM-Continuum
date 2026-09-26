package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.energymk2.IEnergyConnectorMK2;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.client.model.WavefrontObj;
import com.hbm.lib.ForgeDirection;
import dev.golem.core.machine.SyncedBlockEntity;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Legacy RTG body plus the directional connector geometry from RenderRTG. */
public final class RTGRenderer<T extends SyncedBlockEntity> extends GemBER<T, RTGRenderState> {

    private final WavefrontObj body = WavefrontObj.load(
            dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/rtg.obj"));
    private final WavefrontObj connector = WavefrontObj.load(
            dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/rtg_connector.obj"));
    private final Surface cell = Surface.cutoutNoCull(
            Constants.MOD_ID + ":textures/models/machines/rtg_cell.png");
    private final Surface polonium = Surface.cutoutNoCull(
            Constants.MOD_ID + ":textures/models/machines/rtg_polonium.png");
    private final Surface grey = Surface.cutoutNoCull(
            Constants.MOD_ID + ":textures/models/machines/rtg.png");

    @Override
    protected void setup(Gem.Model<RTGRenderState> declaration) {
        Gem.Part<RTGRenderState> machine = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F)
                .rotate(Gem.Axes.Y, 180F);

        machine.child(body.part("Gen"), cell).skin(this::skinOf);
        connector(machine, 0F, state -> state.positiveX);
        connector(machine, 180F, state -> state.negativeX);
        connector(machine, 90F, state -> state.negativeZ);
        connector(machine, -90F, state -> state.positiveZ);
    }

    private void connector(Gem.Part<RTGRenderState> machine, float yaw, Gem.Test<RTGRenderState> present) {
        machine.child(connector.all(), cell)
                .skin(this::skinOf)
                .visibleIf(present)
                .rotate(Gem.Axes.Y, yaw);
    }

    /** The three RTG tiers are one mesh in three skins, picked from the block the renderer is on. */
    private Surface skinOf(RTGRenderState state) {
        return state.grey ? grey : state.polonium ? polonium : cell;
    }

    @Override
    public RTGRenderState createRenderState() {
        return new RTGRenderState();
    }

    @Override
    public void extract(T blockEntity, RTGRenderState state, float partialTicks,
                        Vec3 cameraPosition) {
        state.polonium = blockEntity.getBlockState().is(ModMachineBlocks.rtg_polonium.get());
        state.grey = blockEntity.getBlockState().is(ModMachineBlocks.machine_rtg_grey.get());
        Level level = blockEntity.getLevel();
        if (level == null) return;

        BlockPos pos = blockEntity.getBlockPos();
        state.positiveX = canConnect(level, pos, Direction.EAST);
        state.negativeX = canConnect(level, pos, Direction.WEST);
        state.negativeZ = canConnect(level, pos, Direction.NORTH);
        state.positiveZ = canConnect(level, pos, Direction.SOUTH);
    }

    /** Mirrors Library.canConnect: block-level connector first, then tile-level connector. */
    private static boolean canConnect(Level level, BlockPos pos, Direction direction) {
        BlockPos neighbour = pos.relative(direction);
        ForgeDirection neighbourSide = ForgeDirection.getOrientation(direction.getOpposite());
        BlockState neighbourState = level.getBlockState(neighbour);
        if (neighbourState.getBlock() instanceof IEnergyConnectorBlock connectorBlock
                && connectorBlock.canConnect(level, neighbour, neighbourSide.toDirection())) {
            return true;
        }
        return level.getBlockEntity(neighbour) instanceof IEnergyConnectorMK2 connector
                && connector.canConnect(neighbourSide.toDirection());
    }
}
