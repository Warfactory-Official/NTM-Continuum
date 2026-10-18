package com.hbm.tileentity.machine;

import com.hbm.blocks.machine.MachineTapeDrive;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.inventory.NTMItemHandler;
import com.hbm.inventory.container.ContainerTapeDrive;
import com.hbm.items.machine.ItemDrive;
import com.hbm.items.machine.ItemDrive.EnumDriveType;
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.7 {@code TileEntityMachineTapeDrive}, the drive enclosure. Placed against the bottom of a ground station, it
 * swaps one drive a satellite has data for every 10 ticks. {@link #tapes} is the client's light per slot.
 */
public class TileEntityMachineTapeDrive extends TileEntityMachineBase implements MenuProvider, WorldlyContainer {

    public static final byte SLOT_EMPTY = 0;
    public static final byte SLOT_ANY = 1;
    public static final byte SLOT_EMPTY_TAPE = 2;
    public static final byte SLOT_FILLED_TAPE = 3;
    private static final int[] SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};

    public final byte[] tapes = new byte[12];

    public TileEntityMachineTapeDrive(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_TAPE_DRIVE.get(), pos, state, 12);
    }

    @Override
    protected NTMItemHandler createInventory(int slotCount) {
        return new NTMItemHandler(slotCount, 1) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return isItemValidForSlot(slot, stack);
            }
        };
    }

    @Override public String getDefaultName() { return "container.machineTapeDrive"; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineTapeDrive te) {
        if (level.getGameTime() % 10 == 0) te.readSatellite((ServerLevel) level, pos, state);
        te.networkPackNT();
    }

    private void readSatellite(ServerLevel level, BlockPos pos, BlockState state) {
        BlockPos behind = pos.relative(state.getValue(MachineTapeDrive.FACING).getOpposite());
        BlockEntity connected = MultiblockSurface.coreEntity(level, behind);
        // 1.7 reaches the ground station through its proxies, which are only its bottom layer
        if (!(connected instanceof TileEntityMachineSatLink link) || link.getBlockPos().getY() != behind.getY() || !link.connected) return;

        SatelliteSavedData data = SatelliteSavedData.get(level, pos);
        Satellite satellite = data.getSatFromFreq(link.freq);
        if (satellite == null || !satellite.hasData(level)) return;

        for (int i = 0; i < 12; i++) {
            EnumDriveType type = ItemDrive.typeOf(inventory.getStackInSlot(i));
            if (type == null) continue;
            EnumDriveType ret = satellite.getOutputData(type);

            if (ret != null) {
                satellite.consumeData();
                inventory.setStackInSlot(i, ItemDrive.stack(ret, 1));
                data.setDirty();
                break;
            }
        }
    }

    @Override public boolean isItemValidForSlot(int slot, ItemStack stack) { return stack.getItem() instanceof ItemDrive; }
    @Override public int getMaxStackSize() { return 1; }

    @Override public int[] getSlotsForFace(Direction side) { return SLOTS; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return isItemValidForSlot(slot, stack); }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        EnumDriveType type = ItemDrive.typeOf(stack);
        return type != EnumDriveType.DISK_EMPTY && type != EnumDriveType.FLASH_EMPTY;
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        for (int i = 0; i < 12; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            byte type = SLOT_EMPTY;
            if (!stack.isEmpty()) {
                type = SLOT_ANY;
                EnumDriveType drive = ItemDrive.typeOf(stack);
                if (drive == EnumDriveType.DISK_EMPTY || drive == EnumDriveType.FLASH_EMPTY) {
                    type = SLOT_EMPTY_TAPE;
                } else if (drive != null && drive != EnumDriveType.DISK_BROKEN && drive != EnumDriveType.FLASH_BROKEN) {
                    type = SLOT_FILLED_TAPE;
                }
            }
            buf.writeByte(type);
        }
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        for (int i = 0; i < 12; i++) tapes[i] = buf.readByte();
    }

    @Override public Component getDisplayName() { return Component.translatable(getName()); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) { return new ContainerTapeDrive(id, inv, this); }
}
