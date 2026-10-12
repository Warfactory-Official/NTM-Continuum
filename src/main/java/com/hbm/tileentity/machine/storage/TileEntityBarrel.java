package com.hbm.tileentity.machine.storage;

import com.hbm.api.energymk2.IEnergyHandlerMK2.ConnectionPriority;
import com.hbm.api.fluidmk2.FluidGraph;
import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.inventory.container.ContainerBarrel;
import com.hbm.tileentity.ModFluidNetBlockEntities;
import com.hbm.blocks.ModFluidNetBlocks;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Corrosive;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.items.machine.ItemFluidBucket;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.saveddata.TomSaveData;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.tileentity.machine.MachineFluidContainerIO;
import com.hbm.uninos.graph.EndpointRegistry;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;
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
import net.minecraft.world.level.block.state.BlockState;

/**
 * The 1.7 {@code TileEntityBarrel}: a one-block fluid store with the fluid tank's four modes
 * (0 receive / 1 buffer / 2 send / 3 off), six canister slots, a comparator output, and the material
 * rules that eat a barrel filled with the wrong thing ({@link #checkFluidInteraction}). Its item form
 * keeps the tank ({@link #collectCarriedData}), the 1.7 {@code IPersistentNBT}.
 *
 * <p>Networking follows the machine fluid tank: buffer mode holds the barrel's own cell so runs on two
 * sides are one net; the other modes let the ducts find it by probing.
 */
public class TileEntityBarrel extends TileEntityMachineBase implements MenuProvider, IFluidStandardTransceiverMK2, IRORValueProvider, IRORInteractive {

    public final FluidTankNTM tank;
    public short mode = 0;
    public static final short MODES = 4;
    public byte lastRedstone = 0;
    private long[] cells = FluidGraph.NONE;

    public TileEntityBarrel(BlockPos pos, BlockState state) {
        super(ModFluidNetBlockEntities.BARREL.get(), pos, state, 6);
        this.tank = new FluidTankNTM(Fluids.NONE, capacityOf(state.getBlock()));
    }

    /** For 1.7's subclasses that pass their own capacity to {@code TileEntityBarrel(int)} (the orbus). */
    protected TileEntityBarrel(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos,
                               BlockState state, int capacity) {
        super(type, pos, state, 6);
        this.tank = new FluidTankNTM(Fluids.NONE, capacity);
    }

    /** The barrel's capacity is a property of its block (12/6/16/24/16 thousand mB). */
    public static int capacityOf(Block block) {
        return block instanceof com.hbm.blocks.machine.BlockFluidBarrel barrel ? barrel.capacity : 0;
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

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityBarrel barrel) {
        byte comp = barrel.getComparatorPower(); // do comparator shenanigans
        if (comp != barrel.lastRedstone) {
            barrel.setChanged();
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
        }
        barrel.lastRedstone = comp;

        barrel.updateTypeFromFluidId();
        MachineFluidContainerIO.load(barrel.tank, barrel.inventory, 2, 3);
        MachineFluidContainerIO.unload(barrel.tank, barrel.inventory, 4, 5);

        // In buffer mode, acts like a pipe block, providing fluid to its own node
        // otherwise, it is a regular providing/receiving machine, blocking further propagation
        if (level instanceof ServerLevel server) barrel.updateCells(server);
        if (barrel.mode == 2) {
            for (DirPos dp : barrel.getConPos()) barrel.tryProvide(barrel.tank, level, dp.getPos(), dp.getDir());
        }

        if (barrel.tank.getFill() > 0) barrel.checkFluidInteraction();
        barrel.networkPackNT(); // 1.7 packed every 50 ticks; unchanged payloads are skipped here
    }

    /** 1.7 {@code tank.setType(0, 1, slots)}: an identifier in slot 0 sets the type and passes to slot 1. */
    private void updateTypeFromFluidId() {
        ItemStack id = inventory.getStackInSlot(0);
        if (id.isEmpty() || !(id.getItem() instanceof IItemFluidId ident)) return;
        if (!inventory.getStackInSlot(1).isEmpty()) return;
        FluidType t = ident.getType(level, getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), id);
        if (t == tank.getTankType()) return;
        tank.setTankType(t);
        // 1.7 moves the whole identifier stack: slots[out] = slots[in].copy(); slots[in] = null.
        inventory.setStackInSlot(1, id.copy());
        inventory.setStackInSlot(0, ItemStack.EMPTY);
        setChanged();
    }

    public void cycleMode() {
        mode = (short) ((mode + 1) % MODES);
        setChanged();
    }

    public DirPos[] getConPos() {
        int x = getBlockPos().getX(), y = getBlockPos().getY(), z = getBlockPos().getZ();
        return new DirPos[]{
                new DirPos(x + 1, y, z, Library.POS_X),
                new DirPos(x - 1, y, z, Library.NEG_X),
                new DirPos(x, y + 1, z, Library.POS_Y),
                new DirPos(x, y - 1, z, Library.NEG_Y),
                new DirPos(x, y, z + 1, Library.POS_Z),
                new DirPos(x, y, z - 1, Library.NEG_Z)
        };
    }

    private void updateCells(ServerLevel level) {
        cells = mode == 1 && tank.getTankType() != Fluids.NONE
                ? FluidGraph.hold(level, this, tank.getTankType(), getConPos())
                : FluidGraph.release(level, cells);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (cells.length > 0 && FluidGraph.dropCells(this, cells) && level instanceof ServerLevel server) {
            EndpointRegistry.of(server).withdraw(getBlockPos());
        }
    }

    private static FluidType bucketFluid(ItemStack stack) {
        if (stack.getItem() instanceof ItemFluidBucket) return ItemFluidBucket.getFluid(stack);
        if (stack.is(Items.WATER_BUCKET)) return Fluids.WATER;
        if (stack.is(Items.LAVA_BUCKET)) return Fluids.LAVA;
        return Fluids.NONE;
    }

    /**
     * 1.7 {@code isItemValidForSlot}: slot 2 takes a full container whose content fits the barrel, slot 4
     * an empty container the barrel's fluid can fill. Slots 0/1 take identifiers (the port's spelling of
     * the 1.7 {@code tank.setType(0, 1, slots)} slot pair).
     */
    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (slot == 0 || slot == 1) return stack.getItem() instanceof IItemFluidId;
        if (slot == 2) {
            FluidType type;
            int amount;
            if (stack.getItem() instanceof IFillableItem fillable) {
                type = fillable.getFirstFluidType(stack);
                amount = fillable.getFill(stack);
            } else {
                type = bucketFluid(stack);
                amount = ItemFluidBucket.VOLUME;
            }
            return type != null && type != Fluids.NONE && amount > 0 && amount <= tank.getMaxFill()
                    && (tank.getTankType() == Fluids.NONE || tank.getTankType() == type);
        }
        if (slot == 4) {
            if (tank.getTankType() == Fluids.NONE) return false;
            if (stack.is(Items.BUCKET)) return ItemFluidBucket.VOLUME <= tank.getMaxFill();
            return stack.getItem() instanceof IFillableItem fillable && fillable.getFill(stack) == 0
                    && fillable.acceptsFluid(tank.getTankType(), stack);
        }
        return false; // 1.7: the output slots 3 and 5 accept nothing (they are filled internally)
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
    public static final dev.golem.core.hook.Hook<AntimatterBlast> ANTIMATTER_BLAST = dev.golem.core.hook.Hook.notifying();

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
        // 1.7 also had a barrel_corroded branch here (leaks, random collapse); that barrel never gets a
        // tile in 1.7 (createNewTileEntity returns null for it), so the branch is dead and is not ported.
        // For when Tom's firestorm hits a barrel full of water
        if (type == Fluids.WATER && level instanceof ServerLevel server) {
            TomSaveData tom = TomSaveData.getExisting(server);
            if (tom != null && tom.fire > 1e-5 && level.getBrightness(LightLayer.SKY, pos) > 7) {
                level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 5, true, Level.ExplosionInteraction.TNT);
            }
        }
    }

    @Override
    public boolean canConnect(FluidType fluid, ForgeDirection dir) {
        return fluid == tank.getTankType();
    }

    @Override public FluidTankNTM[] getSendingTanks() { return (mode == 1 || mode == 2) ? new FluidTankNTM[]{tank} : new FluidTankNTM[0]; }
    @Override public FluidTankNTM[] getReceivingTanks() { return (mode == 0 || mode == 1) ? new FluidTankNTM[]{tank} : new FluidTankNTM[0]; }
    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[]{tank}; }

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
