package com.hbm.blocks.machine;

import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.lib.DirPos;
import dev.golem.core.registry.TickingBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityRefueler;
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
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/** 1.7 refueler: a thin wall leaves standing room inside the fuel-transfer area. */
public class BlockRefueler extends CompatBlock implements TickingBlock, IFluidConnectorBlockMK2, ICapabilityBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape NORTH = box(0, 0, 12, 16, 16, 16);
    private static final VoxelShape SOUTH = box(0, 0, 0, 16, 16, 4);
    private static final VoxelShape WEST = box(12, 0, 0, 16, 16, 16);
    private static final VoxelShape EAST = box(0, 0, 0, 4, 16, 16);

    public BlockRefueler(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityRefueler(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE; // RefuelerRenderer draws the OBJ
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
            default -> NORTH;
        };
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        if (!stack.isEmpty() && stack.getItem() instanceof IItemFluidId identifier) {
            if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TileEntityRefueler te) {
                FluidType type = identifier.getType(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                te.setTankType(type);
                player.sendSystemMessage(Component.translatable("chat.fractioning.changedto",
                        type.getDisplayName()).withStyle(ChatFormatting.YELLOW));
            }
            return InteractionResult.SUCCESS;
        }
        return super.onUseItem(state, level, pos, player, hand, stack, hit);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        return world.getBlockEntity(new BlockPos(x, y, z)) instanceof TileEntityRefueler te
                && IEnergyConnectorBlock.isPort(portOf(te), new BlockPos(x, y, z), dir);
    }

    private static DirPos[] portOf(TileEntityRefueler te) {
        // pipe connects on the side opposite the facing (matches the TE's subscribe port)
        Direction facing = te.getBlockState().hasProperty(FACING) ? te.getBlockState().getValue(FACING) : Direction.NORTH;
        ForgeDirection dir = ForgeDirection.getOrientation(facing.get3DDataValue()).getOpposite();
        BlockPos p = te.getBlockPos();
        return new DirPos[]{ new DirPos(p.getX() + dir.offsetX, p.getY(), p.getZ() + dir.offsetZ, dir) };
    }

    /** Live connection rules of the corresponding 1.7 tile, shared by native and foreign contacts. */
    private static final MachineCaps CAPS = MachineCaps.blockKeyed().fluidIn()
            .fluidFaces(BlockEntity.class, (be, side) -> side == be.getBlockState().getValue(FACING).getOpposite());

    @Override
    public MachineCaps caps() { return CAPS; }
}
