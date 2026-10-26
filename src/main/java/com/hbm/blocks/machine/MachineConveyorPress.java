package com.hbm.blocks.machine;

import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.lib.ForgeDirection;
import com.hbm.blocks.ITooltipProvider;
import com.hbm.api.block.IToolable;
import com.hbm.api.conveyor.IConveyorBelt;
import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.items.machine.ItemStamp;
import com.hbm.sound.ModSounds;
import com.hbm.util.BobMathUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import com.hbm.tileentity.machine.TileEntityConveyorPress;

/**
 * The conveyor press: a one-wide, three-high folded column ({@code {2,0,0,0,0,0}}, offset 0) whose bottom
 * cell is the core and whose middle cell carries a belt. Items ride the belt across the block, at
 * right angles to the facing ({@code getRotation(UP)}, clockwise), and get pressed under the piston.
 * A stamp is installed by right-clicking with it and removed with a screwdriver.
 */
public final class MachineConveyorPress extends BlockMultiblockMachine
        implements IConveyorBelt, IEnergyConnectorBlock, ILookOverlay, IToolable, ITooltipProvider, ICapabilityBlock {

    private static final int[] DIMENSIONS = {2, 0, 0, 0, 0, 0};

    public MachineConveyorPress(Properties properties) { super(properties); }

    @Override protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityConveyorPress(pos, state);
    }

    @Override public int[] getDimensions() { return DIMENSIONS; }
    @Override public int getOffset() { return 0; }

    /** 1.7 {@code canConnect(dir) != DOWN} on the core; the cable ports are the core's four flanks. */
    @Override public int coreMask() { return MASK_HORIZONTAL | MASK_DOWN; }

    private static @Nullable TileEntityConveyorPress pressAt(Level level, BlockPos pos) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        return core != null && level.getBlockEntity(core) instanceof TileEntityConveyorPress press ? press : null;
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        if (!(stack.getItem() instanceof ItemStamp)) return super.onUseItem(state, level, pos, player, hand, stack, hit);
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        TileEntityConveyorPress press = pressAt(level, pos);
        if (press == null || !press.inventory.getStackInSlot(0).isEmpty()) return InteractionResult.TRY_WITH_EMPTY_HAND;

        press.inventory.setStackInSlot(0, stack.copyWithCount(1));
        stack.shrink(1);
        level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, ModSounds.UPGRADE_PLUG.get(),
                SoundSource.BLOCKS, 1.0F, 1.0F);
        press.markChanged();
        return InteractionResult.SUCCESS;
    }

    /** No menu: a bare right-click on the press does nothing, as in 1.7. */
    @Override
    protected InteractionResult useAtCore(BlockState state, Level level, BlockPos core, Player player, BlockHitResult hit) {
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    public boolean onScrew(Level level, Player player, BlockPos pos, Direction side, Vec3 hit, InteractionHand hand, ToolType tool) {
        if (tool != ToolType.SCREWDRIVER) return false;
        TileEntityConveyorPress press = pressAt(level, pos);
        if (press == null || press.inventory.getStackInSlot(0).isEmpty()) return false;
        if (level.isClientSide()) return true;

        ItemStack stamp = press.inventory.getStackInSlot(0).copy();
        if (!player.getInventory().add(stamp)) {
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1D, pos.getZ() + 0.5, stamp));
        } else {
            player.inventoryMenu.broadcastChanges();
        }

        press.inventory.setStackInSlot(0, ItemStack.EMPTY);
        press.markChanged();
        return true;
    }

    /** 1.7: the belt cell is the one directly above the core. */
    @Override
    public boolean canItemStay(Level level, BlockPos pos, Vec3 itemPos) {
        BlockState below = level.getBlockState(pos.below());
        return below.getBlock() == this && !below.getValue(CELL);
    }

    /** 1.7 {@code getTravelDirection}: the core's facing turned by {@code getRotation(UP)}, i.e. clockwise. */
    public Direction getTravelDirection(Level level, BlockPos pos, @Nullable Vec3 itemPos) {
        BlockState below = level.getBlockState(pos.below());
        Direction facing = below.hasProperty(FACING) ? below.getValue(FACING) : Direction.NORTH;
        return facing.getClockWise();
    }

    @Override
    public Vec3 getTravelLocation(Level level, BlockPos pos, Vec3 itemPos, double speed) {
        Direction dir = getTravelDirection(level, pos, itemPos);
        Vec3 snap = getClosestSnappingPosition(level, pos, itemPos);
        Vec3 clamped = clamp(pos, itemPos);
        Vec3 dest = new Vec3(snap.x - dir.getStepX() * speed, snap.y - dir.getStepY() * speed, snap.z - dir.getStepZ() * speed);
        Vec3 motion = dest.subtract(clamped);
        double len = motion.length();
        return new Vec3(clamped.x + motion.x / len * speed, clamped.y + motion.y / len * speed, clamped.z + motion.z / len * speed);
    }

    @Override
    public Vec3 getClosestSnappingPosition(Level level, BlockPos pos, Vec3 itemPos) {
        Direction dir = getTravelDirection(level, pos, itemPos);
        Vec3 clamped = clamp(pos, itemPos);
        double posX = pos.getX() + 0.5;
        double posZ = pos.getZ() + 0.5;
        if (dir.getStepX() != 0) posX = clamped.x;
        if (dir.getStepZ() != 0) posZ = clamped.z;
        return new Vec3(posX, pos.getY() + 0.25, posZ);
    }

    /** 1.7 mutated the item position in place; the clamp is the same, applied to a copy. */
    private static Vec3 clamp(BlockPos pos, Vec3 itemPos) {
        return new Vec3(Mth.clamp(itemPos.x, pos.getX(), pos.getX() + 1D), itemPos.y,
                Mth.clamp(itemPos.z, pos.getZ(), pos.getZ() + 1D));
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        TileEntityConveyorPress press = pressAt(level, pos);
        if (press == null) return null;

        List<String> text = new ArrayList<>();
        text.add(BobMathUtil.getShortNumber(press.power) + "HE / " + BobMathUtil.getShortNumber(TileEntityConveyorPress.maxPower) + "HE");
        text.add("Installed stamp: " + (press.syncStack.isEmpty() ? "§cNONE" : press.syncStack.getHoverName().getString()));

        return new ILookOverlay.Data(Component.translatable("block.hbm.machine_conveyor_press").getString(),
                0xffff00, 0x404000, text);
    }

    @Override
    public int[][] getAllPorts(ForgeDirection dir) {
        return rotatePorts(new int[][] {
                {0, 0, 0, ForgeDirection.EAST.ordinal()},
                {0, 0, 0, ForgeDirection.WEST.ordinal()},
                {0, 0, 0, ForgeDirection.SOUTH.ordinal()},
                {0, 0, 0, ForgeDirection.NORTH.ordinal()},
                {0, 0, 0, ForgeDirection.DOWN.ordinal()},
        }, dir);
    }

    /** Live connection rules of the corresponding 1.7 tile, shared by native and foreign contacts. */
    private static final MachineCaps CAPS = MachineCaps.blockKeyed().powerIn().fe()
            .faces(BlockEntity.class, (be, side) -> side != Direction.DOWN);

    @Override
    public MachineCaps caps() { return CAPS; }
}
