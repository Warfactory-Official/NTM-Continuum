package com.hbm.tileentity.network.pneumatic;

import com.hbm.tileentity.IControlReceiverFilter;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.module.ModulePatternMatcher;
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
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import com.hbm.blocks.ModPneumaticContent;
import com.hbm.blocks.network.PneumoTube;
import com.hbm.inventory.container.ContainerPneumoTube;
import com.hbm.tileentity.ModPneumaticBlockEntities;
import com.hbm.uninos.PneumaticGraph;
import com.hbm.uninos.networkproviders.PneumaticNetwork;

/**
 * 1.7 {@code TileEntityPneumoTube}. {@code ForgeDirection.UNKNOWN} is spelled as a null direction here. A tube with
 * an insertion face is a compressor (it pulls from the inventory there and pushes into the network); a tube with an
 * ejection face is an endpoint (the inventory there receives).
 */
public class TileEntityPneumoTube extends TileEntityMachineBase
        implements MenuProvider, IFluidStandardReceiverMK2, IControlReceiverFilter {

    public static final int SLOT_COUNT = 15;

    public final ModulePatternMatcher pattern = new ModulePatternMatcher(SLOT_COUNT);
    public final FluidTankNTM compair;
    public @Nullable Direction insertionDir;
    public @Nullable Direction ejectionDir;
    /** Faces drawing a compressed-air nozzle (1.7 {@code canConnectToAir}); server-derived, synced for the renderer. */
    public byte airMask;
    public boolean whitelist = false;
    public boolean redstone = false;
    public byte sendOrder = 0;
    public byte receiveOrder = 0;
    public int soundDelay = 0;
    public int sendCounter = 0;

    /** The endpoint flag last pushed to the graph, so the self-endpoint bit is only touched on a change. */
    private boolean declaredEndpoint;

    public TileEntityPneumoTube(BlockPos pos, BlockState state) {
        this(ModPneumaticBlockEntities.PNEUMATIC_TUBE.get(), pos, state);
    }

    protected TileEntityPneumoTube(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, SLOT_COUNT);
        this.compair = new FluidTankNTM(Fluids.AIR, 4_000).withPressure(1);
    }

    public static int getRangeFromPressure(int pressure) {
        return switch (pressure) {
            case 1 -> 10;
            case 2 -> 25;
            case 3 -> 100;
            case 4 -> 250;
            case 5 -> 1_000;
            default -> 0;
        };
    }

    // tactfully copy pasted from BlockPos
    public static int getIdentifier(BlockPos pos) {
        return (pos.getY() + pos.getZ() * 27644437) * 27644437 + pos.getX();
    }

    public static byte dirIndex(@Nullable Direction dir) {
        return (byte) (dir == null ? 6 : dir.ordinal());
    }

    public static @Nullable Direction dirFromIndex(int index) {
        return index < 0 || index > 5 ? null : Direction.from3DDataValue(index);
    }

    @Override
    public String getDefaultName() {
        return "container.pneumoTube";
    }

    @Override
    public Component getDisplayName() {
        return hasCustomName() ? Component.literal(getName()) : Component.translatable(getDefaultName());
    }

    public boolean matchesFilter(ItemStack stack) {
        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack filter = inventory.getStackInSlot(i);
            if (!filter.isEmpty() && this.pattern.isValidForFilter(filter, i, stack)) {
                return true;
            }
        }
        return false;
    }

    public void serverTick() {
        if (!(level instanceof ServerLevel sl)) return;

        if (this.soundDelay > 0) this.soundDelay--;

        boolean endpoint = this.isEndpoint();
        if (endpoint != declaredEndpoint) {
            PneumaticGraph.setEndpoint(sl, worldPosition, endpoint);
            declaredEndpoint = endpoint;
        }

        long time = sl.getGameTime();
        // 1.7 re-subscribed its air intake every ten ticks; the nozzle faces follow the same clock
        if (time % 10 == 0) refreshAirMask();

        if (this.isCompressor() && (!sl.hasNeighborSignal(worldPosition) ^ this.redstone)) {

            int randTime = Math.abs((int) (time + getIdentifier(worldPosition)));

            if (randTime % 5 == 0 && this.compair.getFill() >= 50) {
                PneumaticNetwork net = PneumaticNetwork.at(sl, worldPosition);
                Container sendFrom = PneumaticNetwork.containerAt(sl, worldPosition.relative(insertionDir));

                if (net != null && sendFrom != null) {

                    if (net.send(sl, sendFrom, this, this.insertionDir.getOpposite(), sendOrder, receiveOrder,
                            getRangeFromPressure(compair.getPressure()), sendCounter)) {
                        this.compair.setFill(this.compair.getFill() - 50);

                        if (this.soundDelay <= 0 && !this.muffled) {
                            sl.playSound(null, worldPosition, ModPneumaticContent.TUBE_FWOOMP.get(), SoundSource.BLOCKS,
                                    0.25F, 0.9F + sl.getRandom().nextFloat() * 0.2F);
                            this.soundDelay = 20;
                        }
                    }

                    this.sendCounter++;
                }
            }
        }

        this.networkPackNT();
    }

    /** 1.7 {@code PneumoTube.canConnectToAir}, recomputed against the world into the synced mask. */
    public void refreshAirMask() {
        if (level == null) return;
        int air = 0;
        if (isCompressor()) {
            for (Direction dir : Direction.values()) {
                if (dir == ejectionDir || dir == insertionDir) continue;
                BlockPos neighbour = worldPosition.relative(dir);
                if (level.getBlockEntity(neighbour) instanceof TileEntityPneumoTube) continue;
                if (Library.canConnectFluid(level, neighbour, ForgeDirection.getOrientation(dir), gas())) {
                    air |= 1 << dir.ordinal();
                }
            }
        }
        if (this.airMask != (byte) air) {
            this.airMask = (byte) air;
            setChanged();
        }
    }

    @Override
    public long getReceiverSpeed(FluidType type, int pressure) {
        return Mth.clamp((this.compair.getMaxFill() - this.compair.getFill()) / 25, 1, 100);
    }

    @Override
    public boolean canConnect(FluidType type, ForgeDirection dir) {
        Direction side = dir.toDirection();
        return side != null && side != this.insertionDir && side != this.ejectionDir && type == gas()
                && this.isCompressor();
    }

    /**
     * hbm-space: a compressor runs on the gas a fluid identifier picked (ADDON-API-INVENTORY V-177), not only on
     * compressed air. Off in plain HBM (1.7: air only); NTM: Space turns it on, since off Earth there may be no air.
     */
    public static boolean anyGas = false;

    /** The gas this compressor takes: its tank's type with {@link #anyGas}, else 1.7's air. */
    public FluidType gas() {
        return anyGas ? this.compair.getTankType() : Fluids.AIR;
    }

    public boolean isCompressor() {
        return this.insertionDir != null;
    }

    public boolean isEndpoint() {
        return this.ejectionDir != null;
    }

    public void nextMode(int index) {
        this.pattern.nextMode(level, inventory.getStackInSlot(index), index);
    }

    public void initPattern(ItemStack stack, int index) {
        this.pattern.initPatternSmart(level, stack, index);
    }

    /** 1.7 {@code PneumoTube} was a plain {@code BlockContainer} with no {@code breakBlock}: the filter slots are not dropped. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        PneumaticGraph.dropCell(this, worldPosition);
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (level instanceof ServerLevel server) {
            PneumaticGraph.addNode(server, worldPosition);
            this.declaredEndpoint = false;
        }
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(redstone);
        buf.writeBoolean(whitelist);
        buf.writeByte(sendOrder);
        buf.writeByte(receiveOrder);
        pattern.serialize(buf);
        compair.serialize(buf);
        buf.writeByte(dirIndex(insertionDir));
        buf.writeByte(dirIndex(ejectionDir));
        buf.writeByte(airMask);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.redstone = buf.readBoolean();
        this.whitelist = buf.readBoolean();
        this.sendOrder = buf.readByte();
        this.receiveOrder = buf.readByte();
        pattern.deserialize(buf);
        compair.deserialize(buf);
        Direction insertion = dirFromIndex(buf.readByte());
        Direction ejection = dirFromIndex(buf.readByte());
        byte air = buf.readByte();
        boolean changed = insertion != insertionDir || ejection != ejectionDir || air != airMask;
        this.insertionDir = insertion;
        this.ejectionDir = ejection;
        this.airMask = air;
        if (changed && level != null && level.isClientSide()) {
            // 1.7 onDataPacket: "that's right, we're gonna cheat" - the shape depends on tile state
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 0);
        }
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        this.insertionDir = dirFromIndex(input.getByteOr("insertionDir", (byte) 6));
        this.ejectionDir = dirFromIndex(input.getByteOr("ejectionDir", (byte) 6));
        this.airMask = input.getByteOr("airMask", (byte) 0);
        this.compair.readFromNBT(input, "tank");
        this.pattern.load(input);

        this.sendOrder = input.getByteOr("sendOrder", (byte) 0);
        this.receiveOrder = input.getByteOr("receiveOrder", (byte) 0);
        this.sendCounter = input.getIntOr("sendCounter", 0);

        this.whitelist = input.getBooleanOr("whitelist", false);
        this.redstone = input.getBooleanOr("redstone", false);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putByte("insertionDir", dirIndex(insertionDir));
        output.putByte("ejectionDir", dirIndex(ejectionDir));
        output.putByte("airMask", airMask);
        this.compair.writeToNBT(output, "tank");
        this.pattern.save(output);

        output.putByte("sendOrder", sendOrder);
        output.putByte("receiveOrder", receiveOrder);
        output.putInt("sendCounter", sendCounter);

        output.putBoolean("whitelist", whitelist);
        output.putBoolean("redstone", redstone);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerPneumoTube(containerId, playerInventory, this);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (Tags.contains(data, "whitelist")) {
            this.whitelist = !this.whitelist;
        }
        if (Tags.contains(data, "redstone")) {
            this.redstone = !this.redstone;
        }
        if (Tags.contains(data, "pressure")) {
            int pressure = this.compair.getPressure() + 1;
            if (pressure > 5) pressure = 1;
            this.compair.withPressure(pressure);
        }
        if (Tags.contains(data, "send")) {
            this.sendOrder++;
            if (this.sendOrder > 2) this.sendOrder = 0;
        }
        if (Tags.contains(data, "receive")) {
            this.receiveOrder++;
            if (this.receiveOrder > 1) this.receiveOrder = 0;
        }
        if (Tags.contains(data, "slot")) {
            setFilterContents(data);
        }

        this.setChanged();
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.containerMenu instanceof ContainerPneumoTube menu && menu.tube() == this && stillValid(player);
    }

    @Override
    public int[] getFilterSlots() {
        return new int[]{0, SLOT_COUNT};
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return new FluidTankNTM[]{compair};
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return new FluidTankNTM[]{compair};
    }

    /** 1.7 places the tube's own filter bank in ghost slots; nothing may be inserted by automation. */
    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }
}
