package com.hbm.tileentity.machine;

import com.hbm.api.fluidmk2.FluidNode;
import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.BlockDummyable;
import com.hbm.interfaces.IClimbable;
import com.hbm.inventory.container.ContainerMachineFluidTank;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.items.machine.ItemFluidBucket;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.uninos.UniNodespace;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;

import java.util.HashSet;

/**
 * The fluid tank multiblock's core BlockEntity — a tank-backed fluid transceiver
 * ({@link IFluidStandardTransceiverMK2}) on the {@link TileEntityMachineBase} tower. Holds a 256,000 mB
 * tank and a {@code mode} flag (receive / both / send / off); the {@link #serverTick} wires it into the
 * fluid network according to that mode, exactly like the 1.12.2 tank.
 *
 * <p>Slim port: the 1.12.2 original's explosion/fire/corrosive/antimatter hazards, OpenComputers/control
 * panel hooks, repair, and canister item I/O are dropped — the demo needs the network behaviour, the GUI
 * fill readout, and (kept) the side ladder ({@link IClimbable}). The tank defaults to {@code NONE}; drop a
 * {@code fluid_identifier} in slot 0 to set its type (it then passes to slot 1), like the 1.12.2 tank.
 */
public class TileEntityMachineFluidTank extends TileEntityMachineBase implements MenuProvider, IFluidStandardTransceiverMK2, IClimbable, IRORValueProvider, IRORInteractive {

    public static final int CAPACITY = 256_000;
    /** 0 = receive-only, 1 = both (acts like a pipe), 2 = send-only, 3 = disabled. */
    public short mode = 0;
    public static final short MODES = 4;

    public final FluidTankNTM tank = new FluidTankNTM(Fluids.NONE, CAPACITY);

    protected FluidNode node;
    protected FluidType lastType;

    private boolean climbRegistered = false;
    private AABB ladderAABB;

    public TileEntityMachineFluidTank(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_TANK.get(), pos, state, 6);
    }

    @Override
    public String getDefaultName() {
        return "container.fluidtank";
    }

    /** Client ticker: just keep the side-ladder registered (climbing is checked on both sides). */
    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachineFluidTank tank) {
        tank.ensureClimbRegistered();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineFluidTank tank) {
        tank.ensureClimbRegistered();
        tank.updateTypeFromFluidId();
        tank.processCanister(2, 3); // canister bay A: fill the tank from / drain into a container
        tank.processCanister(4, 5); // canister bay B: same (both bays are interchangeable)
        FluidType type = tank.tank.getTankType();

        if (tank.mode == 1) {
            // Buffer mode: behave like a pipe — own a node and act as both provider and receiver.
            if (tank.node == null || tank.node.expired || type != tank.lastType) {
                tank.node = (FluidNode) UniNodespace.getNode(level, pos, type.getNetworkProvider());
                if (tank.node == null || tank.node.expired || type != tank.lastType) {
                    tank.node = tank.createNode(type);
                    UniNodespace.createNode(level, tank.node);
                    tank.lastType = type;
                }
            }
            if (tank.node != null && tank.node.hasValidNet()) {
                tank.node.net.addProvider(tank);
                tank.node.net.addReceiver(tank);
            }
        } else {
            if (tank.node != null) {
                UniNodespace.destroyNode(level, pos, type.getNetworkProvider());
                tank.node = null;
            }
            for (DirPos dp : tank.getConPos()) {
                FluidNode dirNode = (FluidNode) UniNodespace.getNode(level, dp.getPos(), type.getNetworkProvider());
                if (tank.mode == 2) {
                    tank.tryProvide(tank.tank, level, dp.getPos(), dp.getDir());
                } else if (dirNode != null && dirNode.hasValidNet()) {
                    dirNode.net.removeProvider(tank);
                }
                if (tank.mode == 0) {
                    if (dirNode != null && dirNode.hasValidNet()) dirNode.net.addReceiver(tank);
                } else if (dirNode != null && dirNode.hasValidNet()) {
                    dirNode.net.removeReceiver(tank);
                }
            }
        }

        tank.setChanged();
        tank.networkPackNT();
    }

    public void cycleMode() {
        mode = (short) ((mode + 1) % MODES);
        setChanged();
    }

    /**
     * Slot 0 (in) holds a fluid identifier; adopt its type and move the identifier to slot 1 (out),
     * mirroring the 1.12.2 {@code tank.setType(0, 1, inventory)} — the identifier passes from the top
     * slot to the bottom slot once consumed. Only fires when the type actually changes and slot 1 is free.
     */
    private void updateTypeFromFluidId() {
        ItemStack id = inventory.getStackInSlot(0);
        if (id.isEmpty() || !(id.getItem() instanceof IItemFluidId ident)) return;
        if (!inventory.getStackInSlot(1).isEmpty()) return; // out slot must be empty to receive it

        FluidType t = ident.getType(level, getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), id);
        if (t == tank.getTankType()) return;

        tank.setTankType(t);
        if (node != null) {
            UniNodespace.destroyNode(level, getBlockPos(), tank.getTankType().getNetworkProvider());
            node = null;
        }
        // pass the identifier from the in-slot (0) to the out-slot (1)
        inventory.insertItem(1, id.copy(), false);
        inventory.extractItem(0, 1, false);
        setChanged();
    }

    /**
     * Processes a canister bay {@code (in → out)}: a <b>full</b> container drains INTO the machine tank, an
     * <b>empty</b> one is filled FROM it, and the modified single container moves to {@code out}. Handles
     * both the universal tank/barrel ({@link IFillableItem}) and buckets (HBM {@link ItemFluidBucket} +
     * vanilla water/lava/empty). <b>Both</b> bays (2→3 and 4→5) run this, so either can fill <i>or</i>
     * drain — the port's two canister bays are interchangeable (more forgiving than the 1.12.2 directional
     * load/unload). Only fires when the out slot can hold the result (no loss); a NONE tank adopts the
     * incoming fluid, and the network node is reset on a type change.
     */
    private void processCanister(int inSlot, int outSlot) {
        ItemStack in = inventory.getStackInSlot(inSlot);
        if (in.isEmpty()) return;
        FluidType tankType = tank.getTankType();

        // --- universal tank / barrel ---
        if (in.getItem() instanceof IFillableItem fillable) {
            int held = fillable.getFill(in);
            FluidType heldType = fillable.getFirstFluidType(in);

            // full -> tank (drain)
            if (held > 0 && heldType != null && heldType != Fluids.NONE
                    && (tankType == Fluids.NONE || tankType == heldType)) {
                int addable = tank.fill(heldType, held, false);
                if (addable > 0) {
                    ItemStack work = single(in);
                    int taken = fillable.tryEmpty(heldType, addable, work);
                    if (taken > 0 && fits(outSlot, work)) {
                        FluidType before = tank.getTankType();
                        tank.fill(heldType, taken, true);
                        moveToOut(inSlot, outSlot, work);
                        resetNodeOnTypeChange(before);
                        return;
                    }
                }
            }
            // empty/same -> filled from tank
            if (tankType != Fluids.NONE && tank.getFill() > 0 && fillable.acceptsFluid(tankType, in)) {
                ItemStack work = single(in);
                int remainder = fillable.tryFill(tankType, tank.getFill(), work);
                int moved = tank.getFill() - remainder;
                if (moved > 0 && fits(outSlot, work)) {
                    tank.setFill(tank.getFill() - moved);
                    moveToOut(inSlot, outSlot, work);
                }
            }
            return;
        }

        // --- buckets ---
        FluidType bucketFluid = bucketFluid(in);
        if (bucketFluid != Fluids.NONE && (tankType == Fluids.NONE || tankType == bucketFluid)
                && tank.fill(bucketFluid, ItemFluidBucket.VOLUME, false) >= ItemFluidBucket.VOLUME) {
            // full bucket -> tank, empty bucket out
            ItemStack empty = new ItemStack(Items.BUCKET);
            if (fits(outSlot, empty)) {
                FluidType before = tank.getTankType();
                tank.fill(bucketFluid, ItemFluidBucket.VOLUME, true);
                moveToOut(inSlot, outSlot, empty);
                resetNodeOnTypeChange(before);
            }
        } else if (in.is(Items.BUCKET) && tankType != Fluids.NONE && tank.getFill() >= ItemFluidBucket.VOLUME) {
            // empty bucket <- tank: water/lava -> vanilla bucket, else an HBM fluid bucket
            ItemStack filled = tankType == Fluids.WATER ? new ItemStack(Items.WATER_BUCKET)
                    : tankType == Fluids.LAVA ? new ItemStack(Items.LAVA_BUCKET)
                    : ItemFluidBucket.make(tankType);
            if (fits(outSlot, filled)) {
                tank.setFill(tank.getFill() - ItemFluidBucket.VOLUME);
                moveToOut(inSlot, outSlot, filled);
            }
        }
    }

    /** The fluid a full bucket in {@code stack} would give the tank (NONE if it's not a full bucket). */
    private static FluidType bucketFluid(ItemStack stack) {
        if (stack.getItem() instanceof ItemFluidBucket) return ItemFluidBucket.getFluid(stack);
        if (stack.is(Items.WATER_BUCKET)) return Fluids.WATER;
        if (stack.is(Items.LAVA_BUCKET)) return Fluids.LAVA;
        return Fluids.NONE;
    }

    private static ItemStack single(ItemStack stack) {
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }

    private boolean fits(int outSlot, ItemStack stack) {
        return inventory.insertItem(outSlot, stack.copy(), true).isEmpty();
    }

    private void moveToOut(int inSlot, int outSlot, ItemStack result) {
        inventory.extractItem(inSlot, 1, false);
        inventory.insertItem(outSlot, result, false);
        setChanged();
    }

    /** Resets the network node when the tank's fluid type changed, so it re-forms on the right nodespace. */
    private void resetNodeOnTypeChange(FluidType before) {
        if (before != tank.getTankType() && node != null) {
            UniNodespace.destroyNode(level, getBlockPos(), before.getNetworkProvider());
            node = null;
        }
    }

    /**
     * Slot roles: 0/1 = identifier in/out (identifiers <b>only</b>); 2 and 4 = canister input bays (any
     * fluid container — bucket or universal tank/barrel, full <i>or</i> empty). Slots 3/5 are the result
     * outputs: they must return {@code true} so the machine's own {@link com.hbm.inventory.NTMItemHandler#insertItem} can
     * place the emptied/filled container there (the handler rejects an insert whose slot is invalid) —
     * player insertion into them is blocked instead by the take-only slots in
     * {@link com.hbm.inventory.container.ContainerMachineFluidTank}.
     */
    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case 0, 1 -> stack.getItem() instanceof IItemFluidId;
            case 2, 4 -> isFluidContainer(stack);
            default -> true; // 3 + 5: outputs — the machine inserts results here; player UI is take-only
        };
    }

    /** Any fluid container the canister bays accept: a tank/barrel, an HBM fluid bucket, or a vanilla bucket. */
    private static boolean isFluidContainer(ItemStack stack) {
        return stack.getItem() instanceof IFillableItem
                || stack.getItem() instanceof ItemFluidBucket
                || stack.is(Items.BUCKET) || stack.is(Items.WATER_BUCKET) || stack.is(Items.LAVA_BUCKET);
    }

    // --- IClimbable: a functional ladder up one side (ported from the 1.12.2 tank) ---

    private void ensureClimbRegistered() {
        if (!climbRegistered) {
            registerClimbable();
            climbRegistered = true;
        }
    }

    /** The ladder box: a 1×3×1 column hugging one side of the tank, on the back-left relative to facing. */
    private AABB getLadderAABB() {
        if (ladderAABB == null) {
            ForgeDirection dir = ForgeDirection.getOrientation(getBlockState().getValue(BlockDummyable.META) - BlockDummyable.offset);
            ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
            BlockPos p = getBlockPos();
            ladderAABB = new AABB(p.getX(), p.getY(), p.getZ(), p.getX() + 1, p.getY() + 3, p.getZ() + 1)
                    .move(dir.offsetX * 0.5 - rot.offsetX * 2.25, 0, dir.offsetZ * 0.5 - rot.offsetZ * 2.25);
        }
        return ladderAABB;
    }

    @Override
    public boolean isEntityInClimbAABB(LivingEntity entity) {
        return entity.getBoundingBox().intersects(getLadderAABB());
    }

    /** The 8 network-connection positions around the 3×3 footprint edges (ported verbatim). */
    public DirPos[] getConPos() {
        int x = getBlockPos().getX(), y = getBlockPos().getY(), z = getBlockPos().getZ();
        return new DirPos[]{
                new DirPos(x + 2, y, z - 1, Library.POS_X), new DirPos(x + 2, y, z + 1, Library.POS_X),
                new DirPos(x - 2, y, z - 1, Library.NEG_X), new DirPos(x - 2, y, z + 1, Library.NEG_X),
                new DirPos(x - 1, y, z + 2, Library.POS_Z), new DirPos(x + 1, y, z + 2, Library.POS_Z),
                new DirPos(x - 1, y, z - 2, Library.NEG_Z), new DirPos(x + 1, y, z - 2, Library.NEG_Z)};
    }

    protected FluidNode createNode(FluidType type) {
        DirPos[] conPos = getConPos();
        HashSet<BlockPos> posSet = new HashSet<>();
        posSet.add(getBlockPos());
        for (DirPos dp : conPos) {
            posSet.add(new BlockPos(dp.getPos().getX() - dp.getDir().offsetX,
                    dp.getPos().getY() - dp.getDir().offsetY, dp.getPos().getZ() - dp.getDir().offsetZ));
        }
        return new FluidNode(type.getNetworkProvider(), posSet.toArray(new BlockPos[0])).setConnections(conPos);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        unregisterClimbable();
        if (level != null && !level.isClientSide() && node != null) {
            UniNodespace.destroyNode(level, getBlockPos(), tank.getTankType().getNetworkProvider());
        }
    }

    // --- IFluidStandardTransceiverMK2 (tank-backed, gated by mode) ---

    @Override
    public FluidTankNTM[] getAllTanks() {
        return new FluidTankNTM[]{tank};
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return (mode == 1 || mode == 2) ? new FluidTankNTM[]{tank} : new FluidTankNTM[0];
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return (mode == 0 || mode == 1) ? new FluidTankNTM[]{tank} : new FluidTankNTM[0];
    }

    // --- MenuProvider ---

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.hbm.machine_fluidtank");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMachineFluidTank(containerId, playerInventory, this);
    }

    // --- sync (BufPacket) + persistence ---

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
        mode = (short) input.getShortOr("mode", (short) 0);
        tank.readFromNBT(input, "tank");
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[]{PREFIX_VALUE + "type", PREFIX_VALUE + "fill", PREFIX_VALUE + "fillpercent",
                PREFIX_FUNCTION + "setmode" + NAME_SEPARATOR + "mode (0-3)",
                PREFIX_FUNCTION + "setmode" + NAME_SEPARATOR + "mode" + PARAM_SEPARATOR + "fallback (0-3)"};
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "type").equals(name)) return tank.getTankType().getName();
        if ((PREFIX_VALUE + "fill").equals(name)) return "" + tank.getFill();
        if ((PREFIX_VALUE + "fillpercent").equals(name)) return "" + (tank.getFill() * 100 / tank.getMaxFill());
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setmode").equals(name) && params.length > 0) {
            int value = IRORInteractive.parseInt(params[0], 0, 3);
            if (value != mode) mode = (short) value;
            else if (params.length > 1) mode = (short) IRORInteractive.parseInt(params[1], 0, 3);
            setChanged();
        }
        return null;
    }
}
