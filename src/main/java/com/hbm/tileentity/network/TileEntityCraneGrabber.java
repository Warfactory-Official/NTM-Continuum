package com.hbm.tileentity.network;

import com.hbm.tileentity.IControlReceiverFilter;
import com.hbm.api.conveyor.IConveyorBelt;
import com.hbm.entity.item.EntityMovingItem;
import com.hbm.modules.ModulePatternMatcher;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import com.hbm.blocks.ModConveyorBlocks;
import com.hbm.inventory.container.ContainerCraneGrabber;
import com.hbm.items.ModConveyorItems;
import com.hbm.tileentity.ModConveyorBlockEntities;
import com.hbm.util.ConveyorInventories;

/**
 * 1.7 {@code tileentity/network/TileEntityCraneGrabber}: lifts items off the belt in front of its input side and
 * puts them on the belt, or into the inventory, at its output side.
 */
public class TileEntityCraneGrabber extends TileEntityCraneBase implements IControlReceiverFilter {

    public boolean isWhitelist = false;
    public final ModulePatternMatcher matcher = new ModulePatternMatcher(9);
    public long lastGrabbedTick = 0;

    public TileEntityCraneGrabber(BlockPos pos, BlockState state) {
        super(ModConveyorBlockEntities.CRANE_GRABBER.get(), pos, state, 11);
    }

    @Override
    public String getDefaultName() {
        return "container.craneGrabber";
    }

    @Override
    protected int[] getUpgradeSlotRange() {
        return new int[]{9, 10};
    }

    /** 1.7: only the two upgrades drop; the nine filters are ghosts. */
    @Override
    protected int[] droppedBand() {
        return new int[]{9, 11};
    }

    public void serverTick() {
        if (level == null) return;

        int delay = ModConveyorItems.ejectorDelay(getItem(10));

        if (level.getGameTime() >= lastGrabbedTick + delay && !level.hasNeighborSignal(worldPosition)) {
            int amount = ModConveyorItems.stackAmount(getItem(9));

            Direction inputSide = getInputSide();
            Direction outputSide = getOutputSide();
            BlockPos outputPos = worldPosition.relative(outputSide);
            Block beltBlock = level.getBlockState(outputPos).getBlock();

            // unholy copy paste bullshit because i can't be assed to rework the entire thing
            if (beltBlock instanceof IConveyorBelt belt) {

                for (EntityMovingItem item : grabbable(inputSide)) {
                    if (item.isRemoved()) continue;
                    ItemStack stack = item.getItemStack();
                    boolean match = this.matchesFilter(stack);
                    if (this.isWhitelist && !match || !this.isWhitelist && match) continue;

                    lastGrabbedTick = level.getGameTime();

                    Vec3 pos = new Vec3(worldPosition.getX() + 0.5 + outputSide.getStepX() * 0.55,
                            worldPosition.getY() + 0.5 + outputSide.getStepY() * 0.55,
                            worldPosition.getZ() + 0.5 + outputSide.getStepZ() * 0.55);
                    Vec3 snap = belt.getClosestSnappingPosition(level, outputPos, pos);
                    EntityMovingItem newItem = new EntityMovingItem(level);
                    newItem.setItemStack(item.getItemStack().copy());
                    newItem.snapTo(snap.x, snap.y, snap.z, 0F, 0F);
                    item.discard();
                    level.addFreshEntity(newItem);
                    break;
                }

            } else {

                Container te = ConveyorInventories.containerAt(level, outputPos);

                if (te != null) {
                    int[] access = ConveyorInventories.masquerade(te, outputSide.getOpposite());

                    /*
                     * due to this really primitive way of just offsetting the AABB instead of contracting it, there's a wacky
                     * edge-case where it's possible to feed the grabber by inserting items from the side if there's a triple
                     * lane conveyor in front of the grabbing end. this is such a non-issue that i'm not going to bother trying
                     * to fuck with the AABB further, since that's just a major headache for no practical benefit
                     */
                    for (EntityMovingItem item : grabbable(inputSide)) {
                        ItemStack stack = item.getItemStack();
                        boolean match = this.matchesFilter(stack);
                        if (this.isWhitelist && !match || !this.isWhitelist && match) continue;

                        lastGrabbedTick = level.getGameTime();

                        ItemStack copy = stack.copy();
                        int toAdd = Math.min(stack.getCount(), amount);
                        copy.setCount(toAdd);
                        ItemStack ret = ConveyorInventories.addToInventory(te, access, copy, outputSide.getOpposite());
                        int didAdd = toAdd - ret.getCount();
                        stack = stack.copy();
                        stack.shrink(didAdd);

                        if (stack.isEmpty()) {
                            item.discard();
                        } else {
                            item.setItemStack(stack);
                        }

                        amount -= didAdd;
                        if (amount <= 0) break;
                    }
                }
            }
        }

        networkPackNT();
    }

    /** 1.7: the reach shrinks in front of a double or triple belt so only the nearest lane is taken from. */
    private List<EntityMovingItem> grabbable(Direction inputSide) {
        double reach = 1D;
        if (inputSide.getAxis().isHorizontal()) { // ignore if pointing up or down
            Block b = level.getBlockState(worldPosition.relative(inputSide)).getBlock();
            if (b == ModConveyorBlocks.CONVEYOR_DOUBLE.get()) reach = 0.5D;
            if (b == ModConveyorBlocks.CONVEYOR_TRIPLE.get()) reach = 0.33D;
        }

        double x = worldPosition.getX() + inputSide.getStepX() * reach;
        double y = worldPosition.getY() + inputSide.getStepY() * reach;
        double z = worldPosition.getZ() + inputSide.getStepZ() * reach;

        return level.getEntitiesOfClass(EntityMovingItem.class,
                new AABB(x + 0.1875D, y + 0.1875D, z + 0.1875D, x + 0.8125D, y + 0.8125D, z + 0.8125D));
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(this.isWhitelist);
        this.matcher.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.isWhitelist = buf.readBoolean();
        this.matcher.deserialize(buf);
    }

    public boolean matchesFilter(ItemStack stack) {
        for (int i = 0; i < 9; i++) {
            ItemStack filter = getItem(i);
            if (!filter.isEmpty() && this.matcher.isValidForFilter(filter, i, stack)) return true;
        }
        return false;
    }

    @Override
    public void nextMode(int i) {
        if (level != null) this.matcher.nextMode(level, getItem(i), i);
        setChanged();
    }

    /** 1.7 default: nothing accessible to automation. */
    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return new int[]{};
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ContainerCraneGrabber(containerId, inventory, this);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        this.isWhitelist = input.getBooleanOr("isWhitelist", false);
        this.matcher.load(input);
        this.lastGrabbedTick = input.getLongOr("lastGrabbedTick", 0L);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putBoolean("isWhitelist", this.isWhitelist);
        this.matcher.save(output);
        output.putLong("lastGrabbedTick", lastGrabbedTick);
    }

    @Override
    public boolean hasPermission(Player player) {
        return super.hasPermission(player);
    }

    @Override
    public int[] getFilterSlots() {
        return new int[]{0, 9};
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("whitelist")) this.isWhitelist = !this.isWhitelist;
        if (data.contains("slot")) setFilterContents(data);
        setChanged();
    }

    @Override
    public CompoundTag getSettings(net.minecraft.world.level.Level level, BlockPos pos) {
        return super.getSettings(level, pos);
    }

    @Override
    public void pasteSettings(CompoundTag nbt, int index, net.minecraft.world.level.Level level, Player player, BlockPos pos) {
        super.pasteSettings(nbt, index, level, player, pos);
    }

    @Override
    public String[] infoForDisplay(net.minecraft.world.level.Level level, BlockPos pos) {
        return super.infoForDisplay(level, pos);
    }
}
