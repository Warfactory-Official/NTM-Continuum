package com.hbm.blocks.network;

import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.api.fluidmk2.IFluidConnectorMK2;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.hbm.uninos.graph.LevelNodeGraph;
import dev.golem.core.gui.Menus;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CarriedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import com.hbm.api.block.IPneumaticConnector;

/**
 * Shared trunk of the pneumatic machine/storage blocks: a right-click opens the menu (1.7 {@code onBlockActivated}
 * with {@code FMLNetworkHandler.openGui}, refused while sneaking) and the pneumatic connection rule. The graph node
 * lifecycle belongs to the block entity, which is also what re-declares it on chunk load.
 */
public abstract class PneumaticStorageBlockBase extends CarriedBlock
        implements TickingBlock, IPneumaticConnector, IFluidConnectorBlockMK2 {

    protected PneumaticStorageBlockBase(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                 BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof MenuProvider provider) {
            Menus.openWithPos(serverPlayer, provider, pos);
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * A foreign block appearing beside a conductor is the one endpoint transition nothing else announces; re-probe
     * this node's own faces.
     */
    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighbourBlock,
                                     boolean movedByPiston) {
        if (level instanceof ServerLevel sl) LevelNodeGraph.invalidateEndpointsAt(sl, pos);
    }

    /** The storage banks take compressed air on any face; the machines take no fluid at all. */
    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        return world.getBlockEntity(new BlockPos(x, y, z)) instanceof IFluidConnectorMK2 connector
                && connector.canConnect(type, dir);
    }
}
