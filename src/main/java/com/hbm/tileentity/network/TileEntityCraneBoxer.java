package com.hbm.tileentity.network;

import com.hbm.api.conveyor.IConveyorBelt;
import com.hbm.entity.item.EntityMovingPackage;
import com.hbm.interfaces.IControlReceiver;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import com.hbm.inventory.container.ContainerCraneBoxer;
import com.hbm.tileentity.ModConveyorBlockEntities;
import com.hbm.util.ConveyorInventories;

/**
 * 1.7 {@code tileentity/network/TileEntityCraneBoxer}: packs full stacks (4, 8 or 16 of them) or, on a redstone
 * edge, everything it holds, into one package that rides the belt at its output side.
 */
public class TileEntityCraneBoxer extends TileEntityCraneBase implements IControlReceiver {

    public static final int SLOT_COUNT = 7 * 3;
    private static final int[] ACCESS = ConveyorInventories.range(SLOT_COUNT);

    public byte mode = 0;
    public static final byte MODE_4 = 0;
    public static final byte MODE_8 = 1;
    public static final byte MODE_16 = 2;
    public static final byte MODE_REDSTONE = 3;

    private boolean lastRedstone = false;

    public TileEntityCraneBoxer(BlockPos pos, BlockState state) {
        super(ModConveyorBlockEntities.CRANE_BOXER.get(), pos, state, SLOT_COUNT);
    }

    @Override
    public String getDefaultName() {
        return "container.craneBoxer";
    }

    public void serverTick() {
        if (level == null) return;

        boolean redstone = level.hasNeighborSignal(worldPosition);

        if (mode == MODE_REDSTONE && redstone && !lastRedstone) {
            IConveyorBelt belt = beltAtOutput();

            int pack = 0;
            for (int i = 0; i < SLOT_COUNT; i++) if (!getItem(i).isEmpty()) pack++;

            if (belt != null && pack > 0) {
                List<ItemStack> box = new ArrayList<>();
                for (int i = 0; i < SLOT_COUNT && pack > 0; i++) {
                    if (!getItem(i).isEmpty()) {
                        pack--;
                        box.add(getItem(i).copy());
                        setItem(i, ItemStack.EMPTY);
                    }
                }
                ship(belt, box);
            }
        }

        this.lastRedstone = redstone;

        if (mode != MODE_REDSTONE && level.getGameTime() % 2 == 0) {
            int pack = switch (mode) {
                case MODE_4 -> 4;
                case MODE_8 -> 8;
                case MODE_16 -> 16;
                default -> 1;
            };

            int fullStacks = 0;
            for (int i = 0; i < SLOT_COUNT; i++) {
                ItemStack stack = getItem(i);
                if (!stack.isEmpty() && stack.getCount() == stack.getMaxStackSize()) fullStacks++;
            }

            IConveyorBelt belt = beltAtOutput();

            if (belt != null && fullStacks >= pack) {
                List<ItemStack> box = new ArrayList<>();
                for (int i = 0; i < SLOT_COUNT && pack > 0; i++) {
                    ItemStack stack = getItem(i);
                    if (!stack.isEmpty() && stack.getCount() == stack.getMaxStackSize()) {
                        pack--;
                        box.add(stack.copy());
                        setItem(i, ItemStack.EMPTY);
                    }
                }
                ship(belt, box);
            }
        }

        networkPackNT();
    }

    private IConveyorBelt beltAtOutput() {
        Block b = level.getBlockState(worldPosition.relative(getOutputSide())).getBlock();
        return b instanceof IConveyorBelt belt ? belt : null;
    }

    /** 1.7 filled {@code box[pack--]} from the end, so the package carries the slots in reverse order. */
    private void ship(IConveyorBelt belt, List<ItemStack> box) {
        java.util.Collections.reverse(box);
        Direction outputSide = getOutputSide();
        BlockPos target = worldPosition.relative(outputSide);
        EntityMovingPackage moving = new EntityMovingPackage(level);
        Vec3 pos = new Vec3(worldPosition.getX() + 0.5 + outputSide.getStepX() * 0.55,
                worldPosition.getY() + 0.5 + outputSide.getStepY() * 0.55,
                worldPosition.getZ() + 0.5 + outputSide.getStepZ() * 0.55);
        Vec3 snap = belt.getClosestSnappingPosition(level, target, pos);
        moving.snapTo(snap.x, snap.y, snap.z, 0F, 0F);
        moving.setItemStacks(box);
        level.addFreshEntity(moving);
        setChanged();
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeByte(this.mode);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.mode = buf.readByte();
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return ACCESS;
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        this.mode = input.getByteOr("mode", (byte) 0);
        this.lastRedstone = input.getBooleanOr("lastRedstone", false);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putByte("mode", mode);
        output.putBoolean("lastRedstone", lastRedstone);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ContainerCraneBoxer(containerId, inventory, this);
    }

    @Override
    public boolean hasPermission(Player player) {
        return super.hasPermission(player);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("toggle")) this.mode = (byte) ((this.mode + 1) % 4);
        setChanged();
    }
}
