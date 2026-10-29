package com.hbm.blocks.network;

import com.hbm.blocks.generic.ItemStateBlock;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.api.block.IToolable;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.hbm.uninos.graph.LevelNodeGraph;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.Compat;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import com.hbm.api.block.IPneumaticConnector;
import com.hbm.tileentity.network.TileEntityPneumoTubePaintable;
import com.hbm.tileentity.network.pneumatic.TileEntityPneumoTube;

/**
 * 1.7 {@code PneumoTubePaintableBlock}: a tube inside a full cube that camouflages as any right-clicked full
 * block. It draws no arms; the tubes beside it reach it as an {@link IPneumaticConnector}. 1.7 drew it as a
 * two-pass full cube (the camo or plain face, then the in/out overlay unless meta 1 disabled it); both passes are
 * its block model here, the camo and overlays added where it stands.
 */
public class PneumoTubePaintableBlock extends CompatBlock
        implements TickingBlock, IToolable, IFluidConnectorBlockMK2, IPneumaticConnector, ICapabilityBlock {

    /** 1.7 meta 1: the DEFUSER hides the overlay pass. */
    public static final BooleanProperty OVERLAY = BooleanProperty.create("overlay");

    public PneumoTubePaintableBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(OVERLAY, true));
    }

    /** 1.7 {@code BlockCablePaintable.allowedPaint}: any full-cube block but grass and itself. */
    public static boolean allowedPaint(Block paint, Block that) {
        if (paint == Blocks.GRASS_BLOCK) return false;
        return paint != that && Compat.isSolidRender(paint.defaultBlockState());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OVERLAY);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL; // camo and port overlays are added where it stands (Disguises)
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        if (stack.getItem() instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            if (allowedPaint(block, this) && level.getBlockEntity(pos) instanceof TileEntityPneumoTubePaintable tube
                    && tube.getCamo() == null) {
                if (!level.isClientSide()) tube.setCamo(ItemStateBlock.of(blockItem, stack));
                return InteractionResult.SUCCESS;
            }
        } else {
            ToolType tool = ToolType.getType(stack);
            if (tool == ToolType.SCREWDRIVER || tool == ToolType.HAND_DRILL || tool == ToolType.DEFUSER) {
                return InteractionResult.TRY_WITH_EMPTY_HAND;
            }
        }
        if (PneumoTube.useGasIdentifier(level, pos, player, stack)) return InteractionResult.SUCCESS;
        return PneumoTube.openTubeMenu(level, pos, player);
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                 BlockHitResult hit) {
        return PneumoTube.openTubeMenu(level, pos, player);
    }

    @Override
    public boolean onScrew(Level level, Player player, BlockPos pos, Direction side, Vec3 hit,
                           InteractionHand hand, ToolType tool) {
        BlockState state = level.getBlockState(pos);

        if (tool == ToolType.HAND_DRILL) {
            // 1.7: the drill strips the paint, because the screwdriver already walks the tube's ports
            if (level.getBlockEntity(pos) instanceof TileEntityPneumoTubePaintable tube && tube.getCamo() != null) {
                if (!level.isClientSide()) tube.setCamo(null);
            }
            return true;
        }

        if (tool == ToolType.SCREWDRIVER) return PneumoTube.screwDirections(level, player, pos);

        if (tool == ToolType.DEFUSER) {
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(OVERLAY, !state.getValue(OVERLAY)), Block.UPDATE_ALL);
            }
            return true;
        }

        return false;
    }

    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighbourBlock,
                                     boolean movedByPiston) {
        if (level instanceof ServerLevel sl) LevelNodeGraph.invalidateEndpointsAt(sl, pos);
        if (level.getBlockEntity(pos) instanceof TileEntityPneumoTube tube) tube.refreshAirMask();
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityPneumoTubePaintable(pos, state);
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        return world.getBlockEntity(new BlockPos(x, y, z)) instanceof TileEntityPneumoTube tube
                && tube.canConnect(type, dir);
    }

    @Override
    public MachineCaps caps() { return PneumoTube.CAPS; }
}
