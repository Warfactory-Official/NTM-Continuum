package com.hbm.tileentity.network;

import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.entity.item.EntityDeliveryDrone;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.lib.ForgeDirection;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;
import com.hbm.inventory.container.ContainerDroneCrate;
import com.hbm.packet.DroneLinePayload;
import com.hbm.tileentity.ModDroneBlockEntities;

/**
 * 1.7 {@code TileEntityDroneCrate}: the transport drones' loading bay. Sending, it fills the drone parked above it
 * with its 18 slots or its tank; receiving, it takes the drone's cargo. Slot 18 holds the fluid identifier that
 * types the tank.
 */
public class TileEntityDroneCrate extends TileEntityMachineBase
        implements MenuProvider, IControlReceiver, IDroneLinkable, IFluidStandardTransceiverMK2, WorldlyContainer {

    public static final int CARGO_SLOTS = 18;
    public static final int SLOT_FLUID_ID = 18;
    public static final int SLOT_COUNT = 19;

    private static final int[] SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17};
    private static final FluidTankNTM[] NONE = new FluidTankNTM[0];

    public final FluidTankNTM tank = new FluidTankNTM(Fluids.NONE, 64_000);

    public @Nullable BlockPos next;
    /** Sending: the crate fills a drone. Receiving: a drone fills the crate. */
    public boolean sendingMode = false;
    public boolean itemType = true;

    public TileEntityDroneCrate(BlockPos pos, BlockState state) {
        super(ModDroneBlockEntities.DRONE_CRATE.get(), pos, state, SLOT_COUNT);
    }

    @Override
    public String getDefaultName() {
        return "container.droneCrate";
    }

    @Override
    public Component getDisplayName() {
        return hasCustomName() ? Component.literal(getName()) : Component.translatable(getDefaultName());
    }

    public BlockPos getCoord() {
        return worldPosition.above();
    }

    public void serverTick() {
        if (!(level instanceof ServerLevel server)) return;

        // 1.7 tank.setType(18, slots): the identifier in the last slot names the tank's fluid
        ItemStack id = inventory.getStackInSlot(SLOT_FLUID_ID);
        if (id.getItem() instanceof IItemFluidId ident) {
            FluidType type = ident.getType(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), id);
            if (type != tank.getTankType()) {
                tank.setTankType(type);
                setChanged();
            }
        }

        if (next != null) {
            BlockPos pos = getCoord();
            for (EntityDeliveryDrone drone : server.getEntitiesOfClass(EntityDeliveryDrone.class, new AABB(pos))) {
                if (drone.getDeltaMovement().length() >= 0.05) continue;

                drone.setTarget(next.getX() + 0.5, next.getY(), next.getZ() + 0.5);

                if (sendingMode && itemType) loadItems(server, drone);
                if (!sendingMode && itemType) unloadItems(server, drone);
                if (sendingMode && !itemType) loadFluid(server, drone);
                if (!sendingMode && !itemType) unloadFluid(server, drone);
            }

            DroneLinePayload.send(server, pos, next, 1D, 0x00ffff);
        }

        networkPackNT();
    }

    protected void loadItems(ServerLevel server, EntityDeliveryDrone drone) {
        if (drone.getAppearance() != 0) return;

        boolean loaded = false;

        for (int i = 0; i < CARGO_SLOTS; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            loaded = true;
            drone.setItem(i, stack.copy());
            inventory.setStackInSlot(i, ItemStack.EMPTY);
        }

        if (loaded) {
            setChanged();
            drone.setAppearance(1);
            playUnpack(server);
        }
    }

    /** 1.7 pairs the slot indices: a drone slot lands in the crate slot of the same index or waits. */
    protected void unloadItems(ServerLevel server, EntityDeliveryDrone drone) {
        if (drone.getAppearance() != 1) return;

        boolean emptied = true;

        for (int i = 0; i < CARGO_SLOTS; i++) {
            ItemStack droneSlot = drone.getItem(i);
            ItemStack own = inventory.getStackInSlot(i);

            if (own.isEmpty() && !droneSlot.isEmpty()) {
                inventory.setStackInSlot(i, droneSlot.copy());
                drone.setItem(i, ItemStack.EMPTY);
            } else if (!own.isEmpty() && !droneSlot.isEmpty()) {
                emptied = false;
            }
        }

        setChanged();

        if (emptied) {
            drone.setAppearance(0);
            playUnpack(server);
        }
    }

    protected void loadFluid(ServerLevel server, EntityDeliveryDrone drone) {
        if (drone.getAppearance() != 0) return;

        if (tank.getFill() > 0) {
            drone.fluid = new FluidStack(tank.getTankType(), tank.getFill());
            tank.setFill(0);
            drone.setAppearance(2);
            playUnpack(server);
            setChanged();
        }
    }

    protected void unloadFluid(ServerLevel server, EntityDeliveryDrone drone) {
        if (drone.getAppearance() != 2) return;

        if (drone.fluid != null && drone.fluid.type == tank.getTankType()) {

            if (drone.fluid.fill + tank.getFill() <= tank.getMaxFill()) {
                tank.setFill(tank.getFill() + drone.fluid.fill);
                drone.fluid = null;
                drone.setAppearance(0);
            } else {
                int overshoot = drone.fluid.fill + tank.getFill() - tank.getMaxFill();
                tank.setFill(tank.getMaxFill());
                drone.fluid = new FluidStack(drone.fluid.type, overshoot, drone.fluid.pressure);
            }
            playUnpack(server);
            setChanged();
        }
    }

    private void playUnpack(ServerLevel server) {
        server.playSound(null, worldPosition, ModSounds.ITEM_UNPACK.get(), SoundSource.BLOCKS, 0.5F, 0.75F);
    }

    @Override
    public BlockPos getPoint() {
        return worldPosition.above();
    }

    @Override
    public void setNextTarget(BlockPos target) {
        this.next = target.immutable();
        setChanged();
    }

    @Override
    public boolean hasPermission(Player player) {
        return stillValid(player);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (Tags.contains(data, "mode")) {
            sendingMode = !sendingMode;
            markChanged();
        }
        if (Tags.contains(data, "type")) {
            itemType = !itemType;
            markChanged();
        }
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot < CARGO_SLOTS;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot < CARGO_SLOTS;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public boolean canConnect(FluidType type, ForgeDirection dir) {
        // 1.7 never overrode this: the IFluidConnectorMK2 default, every side, fluid or item mode.
        return dir != ForgeDirection.UNKNOWN;
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return new FluidTankNTM[]{tank};
    }

    /** 1.7: a receiving fluid crate pushes to the pipes, a sending one draws from them. */
    @Override
    public FluidTankNTM[] getSendingTanks() {
        return !sendingMode && !itemType ? new FluidTankNTM[]{tank} : NONE;
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return sendingMode && !itemType ? new FluidTankNTM[]{tank} : NONE;
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(next != null);
        if (next != null) buf.writeBlockPos(next);
        buf.writeBoolean(sendingMode);
        buf.writeBoolean(itemType);
        tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        next = buf.readBoolean() ? buf.readBlockPos() : null;
        sendingMode = buf.readBoolean();
        itemType = buf.readBoolean();
        tank.deserialize(buf);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        next = input.read("next", BlockPos.CODEC).orElse(null);
        sendingMode = input.getBooleanOr("mode", false);
        itemType = input.getBooleanOr("type", true);
        tank.readFromNBT(input, "t");
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        if (next != null) output.store("next", BlockPos.CODEC, next);
        output.putBoolean("mode", sendingMode);
        output.putBoolean("type", itemType);
        tank.writeToNBT(output, "t");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerDroneCrate(containerId, playerInventory, this);
    }
}
