package com.hbm.tileentity.network;

import com.hbm.tileentity.IControlReceiverFilter;
import com.hbm.modules.ModulePatternMatcher;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import com.hbm.inventory.container.ContainerCraneRouter;
import com.hbm.tileentity.ModConveyorBlockEntities;

/** 1.7 {@code tileentity/network/TileEntityCraneRouter} ("Conveyor Sorter"): six five-slot filters, one per side. */
public class TileEntityCraneRouter extends TileEntityMachineBase implements IControlReceiverFilter, net.minecraft.world.MenuProvider {

    public static final int SLOT_COUNT = 5 * 6;

    public final ModulePatternMatcher[] patterns = new ModulePatternMatcher[6]; // why did i make six matchers???
    public int[] modes = new int[6];
    public static final int MODE_NONE = 0;
    public static final int MODE_WHITELIST = 1;
    public static final int MODE_BLACKLIST = 2;
    public static final int MODE_WILDCARD = 3;

    public TileEntityCraneRouter(BlockPos pos, BlockState state) {
        super(ModConveyorBlockEntities.CRANE_ROUTER.get(), pos, state, SLOT_COUNT);
        for (int i = 0; i < patterns.length; i++) patterns[i] = new ModulePatternMatcher(5);
    }

    @Override
    public String getDefaultName() {
        return "container.craneRouter";
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getName());
    }

    public void serverTick() {
        syncState();
    }

    /** 1.7: every slot is a ghost filter; breaking the block drops nothing. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        clearContent();
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        for (ModulePatternMatcher pattern : patterns) pattern.serialize(buf);
        buf.writeVarIntArray(this.modes);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        for (ModulePatternMatcher pattern : patterns) pattern.deserialize(buf);
        int[] read = buf.readVarIntArray();
        if (read.length == 6) this.modes = read;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ContainerCraneRouter(containerId, inventory, this);
    }

    @Override
    public void nextMode(int index) {
        int matcher = index / 5;
        int mIndex = index % 5;
        if (level != null) this.patterns[matcher].nextMode(level, getItem(index), mIndex);
        setChanged();
    }

    public void initPattern(ItemStack stack, int index) {
        int matcher = index / 5;
        int mIndex = index % 5;
        if (level != null) this.patterns[matcher].initPatternSmart(level, stack, mIndex);
        setChanged();
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        for (int i = 0; i < patterns.length; i++) {
            int index = i;
            input.child("pattern" + i).ifPresent(child -> patterns[index].load(child));
        }
        int[] read = input.getIntArrayOr("modes", new int[6]);
        this.modes = read.length == 6 ? read : new int[6];
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        for (int i = 0; i < patterns.length; i++) patterns[i].save(output.child("pattern" + i));
        output.putIntArray("modes", this.modes);
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.distanceToSqr(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()) < 400D;
    }

    @Override
    public int[] getFilterSlots() {
        return new int[]{0, SLOT_COUNT};
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("toggle")) {
            int i = Tags.getIntOr(data, "toggle", 0);
            if (i >= 0 && i < 6) {
                modes[i]++;
                if (modes[i] > 3) modes[i] = 0;
            }
        }
        if (data.contains("slot")) setFilterContents(data);
        setChanged();
    }

    @Override
    public @Nullable CompoundTag getSettings(Level level, BlockPos pos) {
        CompoundTag nbt = new CompoundTag();
        ListTag tags = new ListTag();

        int count = 0;
        for (int i = getFilterSlots()[0]; i < getFilterSlots()[1]; i++) {
            if (!getItem(i).isEmpty()) {
                CompoundTag slotNbt = new CompoundTag();
                slotNbt.putByte("slot", (byte) count);
                IControlReceiverFilter.writeStack(slotNbt, getItem(i), level);
                tags.add(slotNbt);
            }
            count++;
        }

        nbt.put("items", tags);
        nbt.putIntArray("modes", modes);
        return nbt;
    }

    @Override
    public void pasteSettings(CompoundTag nbt, int index, Level level, Player player, BlockPos pos) {
        ListTag items = Tags.getListOrEmpty(nbt, "items");
        int listSize = items.size();

        if (listSize > 0 && nbt.contains("modes")) {
            for (int i = 0; i < listSize; i++) {
                CompoundTag slotNbt = Tags.getCompoundOrEmpty(items, i);
                byte slot = Tags.getByteOr(slotNbt, "slot", (byte) 0);
                ItemStack loaded = IControlReceiverFilter.readFilterStack(slotNbt, level);

                if (!loaded.isEmpty() && slot > index * 5 && slot < Math.min(index * 5 + 5, 30)) {
                    setItem(slot, loaded);
                    nextMode(slot);
                }
            }
            int[] read = Tags.getIntArrayOr(nbt, "modes", modes);
            if (read.length == 6) modes = read;
            setChanged();
        } else {
            IControlReceiverFilter.pasteFilter(this, this, nbt, index, level);
        }
    }

    @Override
    public String[] infoForDisplay(Level level, BlockPos pos) {
        String[] options = new String[patterns.length];
        for (int i = 0; i < options.length; i++) options[i] = "copytool.pattern" + i;
        return options;
    }
}
