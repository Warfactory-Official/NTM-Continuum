package com.hbm.tileentity.network;

import com.hbm.tileentity.IControlReceiverFilter;
import com.hbm.api.conveyor.IConveyorBelt;
import com.hbm.api.conveyor.IEnterableBlock;
import com.hbm.entity.item.EntityMovingItem;
import com.hbm.module.ModulePatternMatcher;
import com.hbm.util.InventoryUtil;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import com.hbm.inventory.container.ContainerCraneExtractor;
import com.hbm.items.ModConveyorItems;
import com.hbm.tileentity.ModConveyorBlockEntities;
import com.hbm.util.ConveyorInventories;

/**
 * 1.7 {@code tileentity/network/TileEntityCraneExtractor} ("Conveyor Ejector"): pulls from the inventory on its
 * output side (note the switcheroo) and drops onto the belt at its input side, through nine ghost filters.
 */
public class TileEntityCraneExtractor extends TileEntityCraneBase implements IControlReceiverFilter {

    private static final int[] ACCESS = {9, 10, 11, 12, 13, 14, 15, 16, 17};

    public boolean isWhitelist = false;
    public boolean maxEject = false;
    public final ModulePatternMatcher matcher = new ModulePatternMatcher(9);

    public TileEntityCraneExtractor(BlockPos pos, BlockState state) {
        super(ModConveyorBlockEntities.CRANE_EXTRACTOR.get(), pos, state, 20);
    }

    @Override
    public String getDefaultName() {
        return "container.craneExtractor";
    }

    /** Slots 18 (stack) and 19 (ejector) plug with the 1.7 upgrade sound. */
    @Override
    protected int[] getUpgradeSlotRange() {
        return new int[]{18, 19};
    }

    /** 1.7: the buffer (9..17) is what breaking drops; the filters are ghosts and the upgrades ride along. */
    @Override
    protected int[] droppedBand() {
        return new int[]{9, 20};
    }

    public void serverTick() {
        if (level == null) return;

        int delay = ModConveyorItems.ejectorDelay(getItem(19));

        if (level.getGameTime() % delay == 0 && !level.hasNeighborSignal(worldPosition)) {
            int amount = ModConveyorItems.stackAmount(getItem(18));

            Direction inputSide = getOutputSide(); // note the switcheroo!
            Direction outputSide = getInputSide();
            Container te = ConveyorInventories.containerAt(level, worldPosition.relative(inputSide));
            Block b = level.getBlockState(worldPosition.relative(outputSide)).getBlock();

            int[] access = null;
            WorldlyContainer sided = null;

            if (te instanceof WorldlyContainer w) {
                sided = w;
                access = ConveyorInventories.extractMasquerade(te, inputSide.getOpposite());
            }

            boolean hasSent = false;
            IConveyorBelt belt = b instanceof IConveyorBelt c ? c : null;

            /* try to send items from a connected inv, if present */
            if (te != null) {
                int size = access == null ? te.getContainerSize() : access.length;

                for (int i = 0; i < size; i++) {
                    int index = access == null ? i : access[i];
                    ItemStack stack = te.getItem(index);

                    if (!stack.isEmpty() && (sided == null || sided.canTakeItemThroughFace(index, stack, inputSide.getOpposite()))) {

                        int maxTarget = Math.min(amount, stack.getMaxStackSize());
                        if (this.maxEject && stack.getCount() < maxTarget) continue;
                        boolean match = this.matchesFilter(stack);

                        if ((isWhitelist && match) || (!isWhitelist && !match)) {
                            stack = stack.copy();
                            int toSend = Math.min(amount, stack.getCount());

                            if (belt != null) {
                                te.removeItem(index, toSend);
                                te.setChanged();
                                stack.setCount(toSend);
                                sendItem(stack, belt, outputSide);
                            } else {
                                stack.setCount(toSend);
                                ItemStack remaining = InventoryUtil.tryAddItemToInventory(this.inventory, 9, 17, stack);
                                te.removeItem(index, toSend - remaining.getCount());
                                te.setChanged();
                            }
                            hasSent = true;
                            break;
                        }
                    }
                }
            }

            /* if no item has been sent, send buffered items while ignoring the filter */
            if (!hasSent && belt != null) {

                for (int i = 9; i < 18; i++) {
                    ItemStack stack = getItem(i);

                    if (!stack.isEmpty()) {
                        stack = stack.copy();
                        int toSend = Math.min(amount, stack.getCount());

                        int maxTarget = Math.min(amount, stack.getMaxStackSize());
                        if (this.maxEject && stack.getCount() < maxTarget) continue;

                        removeItem(i, toSend);
                        stack.setCount(toSend);
                        sendItem(stack, belt, outputSide);

                        break;
                    }
                }
            }
        }

        networkPackNT();
    }

    private void sendItem(ItemStack stack, IConveyorBelt belt, Direction outputSide) {
        BlockPos target = worldPosition.relative(outputSide);
        EntityMovingItem moving = new EntityMovingItem(level);
        Vec3 pos = new Vec3(worldPosition.getX() + 0.5 + outputSide.getStepX() * 0.55,
                worldPosition.getY() + 0.5 + outputSide.getStepY() * 0.55,
                worldPosition.getZ() + 0.5 + outputSide.getStepZ() * 0.55);
        Vec3 snap = belt.getClosestSnappingPosition(level, target, pos);
        moving.snapTo(snap.x, snap.y, snap.z, 0F, 0F);
        moving.setItemStack(stack);
        level.addFreshEntity(moving);

        if (belt instanceof IEnterableBlock enterable) {
            if (enterable.canItemEnter(level, target, outputSide.getOpposite(), moving)) {
                enterable.onItemEnter(level, target, outputSide.getOpposite(), moving);
                moving.discard();
            }
        }
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(isWhitelist);
        buf.writeBoolean(maxEject);
        this.matcher.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        isWhitelist = buf.readBoolean();
        maxEject = buf.readBoolean();
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

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return ACCESS;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot > 8 && slot < 18;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int side) {
        return slot > 8 && slot < 18;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ContainerCraneExtractor(containerId, inventory, this);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        this.isWhitelist = input.getBooleanOr("isWhitelist", false);
        this.maxEject = input.getBooleanOr("maxEject", false);
        this.matcher.load(input);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putBoolean("isWhitelist", this.isWhitelist);
        output.putBoolean("maxEject", this.maxEject);
        this.matcher.save(output);
    }

    @Override
    public boolean hasPermission(Player player) {
        return super.hasPermission(player);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("whitelist")) this.isWhitelist = !this.isWhitelist;
        if (data.contains("maxEject")) this.maxEject = !this.maxEject;
        if (data.contains("slot")) setFilterContents(data);
        setChanged();
    }

    @Override
    public int[] getFilterSlots() {
        return new int[]{0, 9};
    }

    @Override
    public CompoundTag getSettings(Level level, BlockPos pos) {
        return super.getSettings(level, pos);
    }

    @Override
    public void pasteSettings(CompoundTag nbt, int index, Level level, Player player, BlockPos pos) {
        super.pasteSettings(nbt, index, level, player, pos);
    }

    @Override
    public String[] infoForDisplay(Level level, BlockPos pos) {
        return super.infoForDisplay(level, pos);
    }
}
