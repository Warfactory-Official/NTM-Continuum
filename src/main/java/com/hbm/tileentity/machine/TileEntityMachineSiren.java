package com.hbm.tileentity.machine;

import com.hbm.items.machine.ItemCassette.SoundType;
import com.hbm.items.machine.ItemCassette.TrackType;
import com.hbm.tileentity.TileEntityMachineBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.inventory.container.ContainerMachineSiren;
import com.hbm.items.machine.ItemCassette;
import com.hbm.packet.SirenPayload;
import com.hbm.tileentity.ModPowerGenBlockEntities;

/**
 * 1.7 {@code TileEntityMachineSiren}: one cassette slot. Every server tick it tells the clients around it what
 * to do: a looping track follows the redstone signal; a one-shot ({@code PASS}/{@code SOUND}) track is stopped
 * and restarted once on each rising edge, latched by {@link #lock}.
 */
public class TileEntityMachineSiren extends TileEntityMachineBase implements MenuProvider, WorldlyContainer {

    private static final int[] SLOT = {0};

    public boolean lock = false;
    public boolean active;

    public TileEntityMachineSiren(BlockPos pos, BlockState state) {
        super(ModPowerGenBlockEntities.SIREN.get(), pos, state, 1);
    }

    @Override
    public String getDefaultName() {
        return "container.siren";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineSiren siren) {
        if (!(level instanceof ServerLevel server)) return;

        TrackType type = siren.getCurrentType();
        int id = type.ordinal();
        siren.active = type != TrackType.NULL && level.hasNeighborSignal(pos);

        if (type == TrackType.NULL) {
            SirenPayload.send(server, pos, id, false);
            return;
        }

        if (type.getType() == SoundType.LOOP) {
            SirenPayload.send(server, pos, id, siren.active);
        } else {
            if (!siren.lock && siren.active) {
                siren.lock = true;
                SirenPayload.send(server, pos, id, false);
                SirenPayload.send(server, pos, id, true);
            }
            if (siren.lock && !siren.active) {
                siren.lock = false;
            }
        }
    }

    public TrackType getCurrentType() {
        return ItemCassette.getType(inventory.getStackInSlot(0));
    }

    /**
     * 1.7 {@code isItemValidForSlot} returns false: nothing is inserted from the outside.
     */
    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOT;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getName());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMachineSiren(containerId, playerInventory, this);
    }
}
