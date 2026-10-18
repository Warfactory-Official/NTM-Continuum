package com.hbm.tileentity.machine.storage;

import com.hbm.api.energymk2.IEnergyHandlerMK2.ConnectionPriority;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.machine.BlockFluidBarrel;
import com.hbm.inventory.FluidContainerRegistry;
import com.hbm.inventory.container.ContainerBarrel;
import com.hbm.tileentity.ModFluidNetBlockEntities;
import com.hbm.blocks.ModFluidNetBlocks;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Corrosive;
import com.hbm.inventory.fluid.trait.FluidReleaseType;
import com.hbm.inventory.fluid.trait.FluidTrait;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.items.machine.ItemFluidBucket;
import com.hbm.lib.ForgeDirection;
import com.hbm.saveddata.TomSaveData;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.tileentity.machine.MachineFluidContainerIO;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;
import dev.golem.core.hook.Hook;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.tileentity.TilePortShapes;

/**
 * The 1.7 {@code TileEntityBarrel}: a one-block fluid store with the fluid tank's four modes
 * (0 receive / 1 buffer / 2 send / 3 off), six canister slots, a comparator output, and the material
 * rules that eat a barrel filled with the wrong thing ({@link #checkFluidInteraction}). Its item form
 * keeps the tank ({@link #collectCarriedData}), the 1.7 {@code IPersistentNBT}.
 *
 * <p>Its six faces are one passthrough port, so ducts on two sides are one net in every mode; the mode only sets
 * the speeds and buffer mode's LOW priority.
 */
public class TileEntityBarrel extends TileEntityMachineBase implements MenuProvider, IFluidStandardTransceiverMK2, IRORValueProvider, IRORInteractive {

    public final FluidTankNTM tank;
    public short mode = 0;
    public static final short MODES = 4;
    public byte lastRedstone = 0;

    public TileEntityBarrel(BlockPos pos, BlockState state) {
        super(ModFluidNetBlockEntities.BARREL.get(), pos, state, 6);
        this.tank = new FluidTankNTM(Fluids.NONE, capacityOf(state.getBlock()));
    }

    /** For 1.7's subclasses that pass their own capacity to {@code TileEntityBarrel(int)} (the orbus). */
    protected TileEntityBarrel(BlockEntityType<?> type, BlockPos pos,
                               BlockState state, int capacity) {
        super(type, pos, state, 6);
        this.tank = new FluidTankNTM(Fluids.NONE, capacity);
    }

    /** The barrel's capacity is a property of its block (12/6/16/24/16 thousand mB). */
    public static int capacityOf(Block block) {
        return block instanceof BlockFluidBarrel barrel ? barrel.capacity : 0;
    }

    @Override
    public String getDefaultName() {
        return "container.barrel";
    }

    public byte getComparatorPower() {
        if (tank.getFill() == 0) return 0;
        double frac = (double) tank.getFill() / (double) tank.getMaxFill() * 15D;
        return (byte) (Mth.clamp((int) frac + 1, 0, 15));
    }

    @Override
    public long getDemand(FluidType type, int pressure) {
        if (this.mode == 2 || this.mode == 3) return 0;
        if (tank.getPressure() != pressure) return 0;
        return type == tank.getTankType() ? tank.getMaxFill() - tank.getFill() : 0;
    }

    @Override
    public long getFluidAvailable(FluidType type, int pressure) {
        if (this.mode == 0 || this.mode == 3) return 0;
        if (tank.getPressure() != pressure) return 0;
        return type == tank.getTankType() ? tank.getFill() : 0;
    }

    protected PortDef[] cachedPorts;
    public PortDef[] getPorts() { if (cachedPorts == null) cachedPorts = TilePortShapes.around(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()); return cachedPorts; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityBarrel barrel) {
        barrel.setupFluidPorts(barrel.getPorts());
        barrel.updatePortFIFO();

        byte comp = barrel.getComparatorPower(); // do comparator shenanigans
        if (comp != barrel.lastRedstone) {
            barrel.setChanged();
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
        }
        barrel.lastRedstone = comp;

        barrel.tank.setType(barrel.inventory, 0, 1);
        MachineFluidContainerIO.load(barrel.tank, barrel.inventory, 2, 3);
        MachineFluidContainerIO.unload(barrel.tank, barrel.inventory, 4, 5);

        if (barrel.tank.getFill() > 0) barrel.checkFluidInteraction();
        barrel.networkPackNT(); // 1.7 packed every 50 ticks; unchanged payloads are skipped here
    }

    public void cycleMode() {
        mode = (short) ((mode + 1) % MODES);
        setChanged();
    }

    /**
     * 1.7 {@code isItemValidForSlot}: slot 2 takes a full registry container whose content fits the barrel, slot 4 an
     * empty one the barrel's fluid fills within its capacity. Slots 0/1 take identifiers (the
     * {@code tank.setType(0, 1, slots)} pair).
     */
    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (slot == 0 || slot == 1) return stack.getItem() instanceof IItemFluidId;
        FluidType type = tank.getTankType();
        if (slot == 2) {
            int content = FluidContainerRegistry.INSTANCE.contentOf(stack, type);
            // port: the universal bucket, which 1.7's registry never had
            if (content <= 0 && stack.getItem() instanceof ItemFluidBucket && ItemFluidBucket.getFluid(stack) == type) content = ItemFluidBucket.VOLUME;
            return content > 0 && content <= tank.getMaxFill();
        }
        if (slot == 4) {
            ItemStack full = FluidContainerRegistry.INSTANCE.fullOf(stack, type);
            if (!full.isEmpty()) return FluidContainerRegistry.INSTANCE.contentOf(full, type) <= tank.getMaxFill();
            // port: an empty bucket takes any fluid as the universal bucket
            return stack.is(Items.BUCKET) && type != Fluids.NONE && ItemFluidBucket.VOLUME <= tank.getMaxFill();
        }
        return false;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return new int[]{2, 3, 4, 5};
    }

    /** 1.7 {@code isAntimatter}: the {@code FT_Amat} carriers (AMAT, ASCHRAB, STELLAR_FLUX). */
    public static boolean isAntimatter(FluidType type) {
        return type.isAntimatter();
    }

    /** Replaces the blast of antimatter filled into a matter barrel (ADDON-API-INVENTORY H-68). */
    @FunctionalInterface
    public interface AntimatterBlast {
        /** Called after the barrel was removed; return true when the listener made the blast. */
        boolean explode(Level level, BlockPos pos, FluidType type);
    }

    /** The first listener answering true replaces 1.7's vanilla strength-5 explosion; HBM registers none. */
    public static final Hook<AntimatterBlast> ANTIMATTER_BLAST = Hook.notifying();

    public void checkFluidInteraction() {
        if (level == null || level.isClientSide()) return;
        Block b = getBlockState().getBlock();
        FluidType type = tank.getTankType();
        BlockPos pos = getBlockPos();
        // for when you fill antimatter into a matter tank
        if (b != ModFluidNetBlocks.barrel_antimatter.get() && isAntimatter(type)) {
            level.destroyBlock(pos, false);
            for (AntimatterBlast listener : ANTIMATTER_BLAST) {
                if (listener.explode(level, pos, type)) return;
            }
            level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 5, true, Level.ExplosionInteraction.TNT);
            return;
        }
        // for when you fill hot or corrosive liquids into a plastic tank
        if (b == ModFluidNetBlocks.barrel_plastic.get() && (type.hasTrait(FT_Corrosive.class) || type.isHot())) {
            level.destroyBlock(pos, false);
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
            return;
        }
        if (b == ModFluidNetBlocks.barrel_corroded.get()) {
            if (level.getRandom().nextInt(3) == 0) {
                tank.setFill(tank.getFill() - 1);
                FluidTrait.onRelease(level, pos, type, FluidReleaseType.SPILL, 1);
            }
            if (level.getRandom().nextInt(3 * 60 * 20) == 0) level.destroyBlock(pos, false);
        }
        if (b == ModFluidNetBlocks.barrel_steel.get() && isHighlyCorrosive(type)) {
            int fill = tank.getFill();
            level.setBlock(pos, ModFluidNetBlocks.barrel_corroded.get().defaultBlockState(), 3);
            if (level.getBlockEntity(pos) instanceof TileEntityBarrel barrel) {
                barrel.tank.setTankType(type);
                barrel.tank.setFill(fill);
            }
        }
        // For when Tom's firestorm hits a barrel full of water
        if (type == Fluids.WATER && level instanceof ServerLevel server) {
            TomSaveData tom = TomSaveData.getExisting(server);
            if (tom != null && tom.fire > 1e-5 && level.getBrightness(LightLayer.SKY, pos) > 7) {
                level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 5, true, Level.ExplosionInteraction.TNT);
            }
        }
    }

    private static boolean isHighlyCorrosive(FluidType type) {
        FT_Corrosive corrosive = type.getTrait(FT_Corrosive.class);
        return corrosive != null && corrosive.isHighlyCorrosive();
    }

    @Override
    public boolean canConnect(FluidType fluid, ForgeDirection dir) {
        return fluid == tank.getTankType();
    }

    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{tank}; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{tank}; }
    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[]{tank}; }

    @Override
    public long getProviderSpeed(FluidType type, int pressure) {
        return (mode == 1 || mode == 2) ? 1_000_000_000 : 0;
    }

    @Override
    public long getReceiverSpeed(FluidType type, int pressure) {
        return (mode == 0 || mode == 1) ? 1_000_000_000 : 0;
    }

    @Override
    public ConnectionPriority getFluidPriority() {
        return mode == 1 ? ConnectionPriority.LOW : ConnectionPriority.NORMAL;
    }

    // --- item form: the 1.7 IPersistentNBT (only a non-empty barrel carries its tank) ---

    @Override
    protected CompoundTag collectCarriedData() {
        if (tank.getFill() == 0) return null;
        CompoundTag data = new CompoundTag();
        data.putInt("fill", tank.getFill());
        Fluids.write(data, "type", tank.getTankType());
        data.putInt("max", tank.getMaxFill());
        data.putShort("mode", mode);
        return data;
    }

    @Override
    protected void applyCarriedData(CompoundTag data) {
        tank.setTankType(Fluids.read(data, "type"));
        tank.setFill(Math.min(Tags.getIntOr(data, "fill", 0), tank.getMaxFill()));
        mode = (short) Tags.getIntOr(data, "mode", 0);
    }

    @Override
    protected void removeSavedComponents(DataOut output) {
        super.removeSavedComponents(output);
        output.discard("inventory");
        output.discard("tank");
        output.discard("mode");
    }

    // --- MenuProvider ---

    @Override
    public Component getDisplayName() {
        return Component.translatable(getName());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerBarrel(containerId, playerInventory, this);
    }

    // --- sync + persistence (1.7 keys) ---

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeShort(mode);
        tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        mode = buf.readShort();
        tank.deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putShort("mode", mode);
        tank.writeToNBT(output, "tank");
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        mode = input.getShortOr("mode", (short) 0);
        tank.readFromNBT(input, "tank");
    }

    // --- Redstone over Radio ---

    @Override
    public String[] getFunctionInfo() {
        return new String[]{
                PREFIX_VALUE + "type",
                PREFIX_VALUE + "fill",
                PREFIX_VALUE + "fillpercent",
                PREFIX_FUNCTION + "setmode" + NAME_SEPARATOR + "mode (0-3)",
                PREFIX_FUNCTION + "setmode" + NAME_SEPARATOR + "mode" + PARAM_SEPARATOR + "fallback (0-3)",
        };
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "type").equals(name)) return tank.getTankType().getName();
        if ((PREFIX_VALUE + "fill").equals(name)) return "" + tank.getFill();
        if ((PREFIX_VALUE + "fillpercent").equals(name)) return "" + (tank.getFill() * 100 / Math.max(1, tank.getMaxFill()));
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setmode").equals(name) && params.length > 0) {
            int m = IRORInteractive.parseInt(params[0], 0, 3);
            if (m != this.mode) {
                this.mode = (short) m;
                this.markChanged();
            } else if (params.length > 1) {
                this.mode = (short) IRORInteractive.parseInt(params[1], 0, 3);
                this.markChanged();
            }
        }
        return null;
    }
}
