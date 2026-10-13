package com.hbm.blocks.machine;

import com.hbm.api.block.IToolable;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityMachineAutosaw;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import com.hbm.client.render.RenderThresher;
import com.hbm.tileentity.machine.TileEntityMachineThresher;

/**
 * The automatic thresher: the autosaw's sibling. One horizontal-facing INVISIBLE block drawn by
 * {@code RenderThresher}; a fluid identifier retypes its fuel tank, a screwdriver suspends it.
 *
 * <p>1.7 stores the facing as the metadata {@code 3/4/2/5} by player yaw quadrant, which is the player's
 * own horizontal look direction; the tile then works with its opposite.
 */
public class MachineThresher extends CompatBlock
        implements TickingBlock, IFluidConnectorBlockMK2, IToolable, ILookOverlay {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public MachineThresher(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineThresher(pos, state);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Meta 3/4/2/5 by yaw quadrant is the direction the player looks, unlike BlockMachineBase's opposite.
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.SUCCESS;
        if (!(stack.getItem() instanceof IItemFluidId identifier)) {
            return super.onUseItem(state, level, pos, player, hand, stack, hit);
        }

        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (level.getBlockEntity(pos) instanceof TileEntityMachineThresher thresher) {
            FluidType type = identifier.getType(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            if (TileEntityMachineAutosaw.acceptedFuels.contains(type)) {
                thresher.tank.setTankType(type);
                thresher.setChanged();
                player.sendSystemMessage(Component.literal("Changed type to ")
                        .append(type.getDisplayName()).append("!").withStyle(ChatFormatting.YELLOW));
                return InteractionResult.SUCCESS;
            }
        }

        return super.onUseItem(state, level, pos, player, hand, stack, hit);
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                 BlockHitResult hit) {
        return level.isClientSide() || player.isShiftKeyDown()
                ? InteractionResult.SUCCESS : InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    public boolean onScrew(Level level, Player player, BlockPos pos, Direction side,
                           Vec3 hit, InteractionHand hand, ToolType tool) {
        if (tool != ToolType.SCREWDRIVER
                || !(level.getBlockEntity(pos) instanceof TileEntityMachineThresher thresher)) return false;

        if (!level.isClientSide()) {
            thresher.isSuspended = !thresher.isSuspended;
            thresher.setChanged();
        }
        return true;
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        return dir != ForgeDirection.UNKNOWN
                && TileEntityMachineAutosaw.acceptedFuels.contains(type)
                && world.getBlockEntity(new BlockPos(x, y, z)) instanceof TileEntityMachineThresher;
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityMachineThresher thresher)) return null;

        List<String> text = new ArrayList<>();
        text.add(thresher.tank.getTankType().getDisplayNameString() + ": "
                + thresher.tank.getFill() + "/" + thresher.tank.getMaxFill() + "mB");
        if (thresher.isSuspended) {
            text.add("§c! " + Component.translatable("tile.machine_thresher.suspended").getString() + " !");
        }
        return new ILookOverlay.Data(Component.translatable("block.hbm.machine_thresher").getString(),
                0xffff00, 0x404000, text);
    }
}
