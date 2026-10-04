package com.hbm.tileentity.machine;

import com.hbm.inventory.fluid.trait.FT_Polluting;
import com.hbm.inventory.fluid.trait.FluidReleaseType;
import com.hbm.api.fluidmk2.FluidGraph;
import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
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
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.uninos.graph.EndpointRegistry;
import net.minecraft.server.level.ServerLevel;
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


/**
 * The fluid tank multiblock's core BlockEntity — a tank-backed fluid transceiver
 * ({@link IFluidStandardTransceiverMK2}) on the {@link TileEntityMachineBase} tower. Holds a 256,000 mB
 * tank and a {@code mode} flag (receive / both / send / off); the {@link #serverTick} wires it into the
 * fluid network according to that mode, exactly like the 1.12.2 tank.
 *
 * <p>1.7's hazards are ported: an explosion (first hit), antimatter or a highly corrosive fluid tears it open
 * ({@link #hasExploded}); it then leaves the network, leaks, burns if flammable, and is repaired with a blowtorch and
 * six steel plates. OpenComputers/control panel hooks are dropped. The side ladder ({@link IClimbable}) is kept. The tank defaults to {@code NONE}; drop a
 * {@code fluid_identifier} in slot 0 to set its type (it then passes to slot 1), like the 1.12.2 tank.
 */
public class TileEntityMachineFluidTank extends TileEntityMachineBase implements MenuProvider, IFluidStandardTransceiverMK2, IClimbable, IRORValueProvider, IRORInteractive, com.hbm.interfaces.IRepairable {

    /** 1.7 {@code hasExploded}: torn open by an explosion, antimatter or a highly corrosive fluid; leaks until repaired. */
    public boolean hasExploded = false;
    /** 1.7 {@code onFire}: a flammable tank that exploded burns while it leaks, until extinguished. */
    public boolean onFire = false;
    /** 1.7 {@code lastExplosion}: one explosion damages the tank once however many of its cells it reaches. */
    public @org.jspecify.annotations.Nullable Object lastExplosion;

    public static final int CAPACITY = 256_000;
    /** 0 = receive-only, 1 = both (acts like a pipe), 2 = send-only, 3 = disabled. */
    public short mode = 0;
    public static final short MODES = 4;

    public final FluidTankNTM tank = new FluidTankNTM(Fluids.NONE, CAPACITY);

    private long[] cells = FluidGraph.NONE;

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
        FluidType type = tank.tank.getTankType();
        if (!tank.hasExploded) {
            tank.updateTypeFromFluidId();
            if (tank.tank.getPressure() == 0) {
                MachineFluidContainerIO.load(tank.tank, tank.inventory, 2, 3);
            }
            type = tank.tank.getTankType();
            if (level instanceof ServerLevel server) tank.updateCells(server, type);
            if (tank.mode == 2) {
                for (DirPos dp : tank.getConPos()) tank.tryProvide(tank.tank, level, dp.getPos(), dp.getDir());
            }
        } else if (level instanceof ServerLevel server) {
            tank.cells = FluidGraph.release(server, tank.cells); // 1.7: a torn tank leaves the network
        }

        // 1.7: antimatter does not stay in a tank, a highly corrosive fluid eats through it
        if (tank.tank.getFill() > 0) {
            if (isAntimatter(type)) {
                amatBlast(level, pos);
                tank.explode();
                tank.tank.setFill(0);
            }
            com.hbm.inventory.fluid.trait.FT_Corrosive corrosive = type.getTrait(com.hbm.inventory.fluid.trait.FT_Corrosive.class);
            if (corrosive != null && corrosive.isHighlyCorrosive()) tank.explode();

            if (tank.hasExploded) {
                int leaking;
                if (isAntimatter(type)) leaking = tank.tank.getFill();
                else if (com.hbm.inventory.fluid.trait.FluidTraitSimple.isGaseousAny(type)) leaking = Math.min(tank.tank.getFill(), tank.tank.getMaxFill() / 100);
                else leaking = Math.min(tank.tank.getFill(), tank.tank.getMaxFill() / 10000);
                tank.updateLeak(leaking);
            }
        }

        MachineFluidContainerIO.unload(tank.tank, tank.inventory, 4, 5);

        tank.setChanged();
        tank.networkPackNT();
    }

    /** 1.7 {@code isAntimatter}: the {@code FT_Amat} carriers (AMAT, ASCHRAB, STELLAR_FLUX). */
    private static boolean isAntimatter(FluidType type) {
        return type.isAntimatter();
    }

    /** 1.7 {@code new ExplosionVNT(world, x + 0.5, y + 1.5, z + 0.5, 5F).makeAmat().setBlockAllocator(null).setBlockProcessor(null).explode()}. */
    private static void amatBlast(Level level, BlockPos pos) {
        new com.hbm.explosion.vanillant.ExplosionVNT(level, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 5F)
                .makeAmat().setBlockAllocator(null).setBlockProcessor(null).explode();
    }

    /** 1.7 {@code explode()}: called when the tank breaks from hazardous contents or external force. */
    public void explode() {
        this.hasExploded = true;
        this.onFire = com.hbm.inventory.fluid.FluidCombustion.isFlammable(tank.getTankType());
        this.setChanged();
    }

    /**
     * 1.7 {@code updateLeak}: every tick after the explosion {@code amount} mB escape - antimatter all at once (each
     * tick another antimatter blast), a burning flammable tank sets everything in the 4x5x4 box around it alight
     * and throws gas flames, a gas rises as a coloured plume every 5 ticks. Every 5 ticks the burning fuel's
     * {@code FT_Polluting} burn table, or the escaping gas's spill table, is applied to {@code amount * 5} mB.
     * (1.7 also calls {@code FluidType.onFluidRelease}, an empty method.)
     */
    public void updateLeak(int amount) {
        if (!hasExploded || amount <= 0 || !(level instanceof ServerLevel server)) return;
        tank.setFill(Math.max(0, tank.getFill() - amount));
        FluidType type = tank.getTankType();
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        net.minecraft.util.RandomSource rand = server.getRandom();

        if (isAntimatter(type)) {
            amatBlast(server, worldPosition);
        } else if (com.hbm.inventory.fluid.FluidCombustion.isFlammable(type) && onFire) {
            for (net.minecraft.world.entity.Entity e : server.getEntitiesOfClass(net.minecraft.world.entity.Entity.class,
                    new AABB(x - 1.5, y, z - 1.5, x + 2.5, y + 5, z + 2.5))) {
                dev.golem.core.world.Effects.ignite(e, 5);
            }
            // 1.7 ParticleUtil.spawnGasFlame: no scale (the gas fire's default)
            net.minecraft.nbt.CompoundTag flame = new net.minecraft.nbt.CompoundTag();
            flame.putDouble("moX", rand.nextGaussian() * 0.2);
            flame.putDouble("moY", 0.1);
            flame.putDouble("moZ", rand.nextGaussian() * 0.2);
            com.hbm.particle.helper.HbmEffectNT.GasFlame.send(server, x + rand.nextDouble(), y + 0.5 + rand.nextDouble(),
                    z + rand.nextDouble(), flame, 150);

            if (server.getGameTime() % 5 == 0) {
                FT_Polluting.pollute(server, worldPosition, type, FluidReleaseType.BURN, amount * 5);
            }
        } else if (com.hbm.inventory.fluid.trait.FluidTraitSimple.isGaseousAny(type)) {
            if (server.getGameTime() % 5 == 0) {
                net.minecraft.nbt.CompoundTag data = new net.minecraft.nbt.CompoundTag();
                data.putString("type", "tower");
                data.putFloat("lift", 1F);
                data.putFloat("base", 1F);
                data.putFloat("max", 5F);
                data.putInt("life", 100 + rand.nextInt(20));
                data.putInt("color", type.getColor());
                com.hbm.particle.helper.HbmEffectNT.Tower.send(server, x + 0.5, y + 1, z + 0.5, data, 150);
            }

            if (server.getGameTime() % 5 == 0) {
                FT_Polluting.pollute(server, worldPosition, type, FluidReleaseType.SPILL, amount * 5);
            }
        }
    }

    /** 1.7 {@code tryExtinguish}: foam or CO2 put a burning tank out; water does too, unless it is a liquid fuel, which it blows up. */
    @Override
    public void tryExtinguish(Level world, BlockPos pos, EnumExtinguishType type) {
        if (!this.hasExploded || !this.onFire) return;
        if (type == EnumExtinguishType.WATER) {
            // extinguishing oil with water is a terrible idea!
            if (tank.getTankType().hasTrait(com.hbm.inventory.fluid.trait.FluidTraitSimple.FT_Liquid.class)) {
                world.explode(null, worldPosition.getX() + 0.5, worldPosition.getY() + 1.5, worldPosition.getZ() + 0.5,
                        5F, true, Level.ExplosionInteraction.BLOCK);
            } else {
                this.onFire = false;
                this.setChanged();
                return;
            }
        }
        if (type == EnumExtinguishType.FOAM || type == EnumExtinguishType.CO2) {
            this.onFire = false;
            this.setChanged();
        }
    }

    public boolean isDamaged() {
        return this.hasExploded;
    }

    private final java.util.List<com.hbm.inventory.RecipesCommon.AStack> repair = new java.util.ArrayList<>();

    /** 1.7 {@code getRepairMaterials}: six steel plates. */
    public java.util.List<com.hbm.inventory.RecipesCommon.AStack> getRepairMaterials() {
        if (!repair.isEmpty()) return repair;
        repair.add(new com.hbm.inventory.RecipesCommon.TagStack(net.minecraft.tags.TagKey.create(
                net.minecraft.core.registries.Registries.ITEM, dev.golem.core.Ids.of("c", "plates/steel")), 6));
        return repair;
    }

    public void repair() {
        this.hasExploded = false;
        this.setChanged();
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
        // pass the identifier from the in-slot (0) to the out-slot (1)
        inventory.insertItem(1, id.copy(), false);
        inventory.extractItem(0, 1, false);
        setChanged();
    }

    /** The full fluid carried by a bucket, or NONE when it is not a fluid source. */
    private static FluidType bucketFluid(ItemStack stack) {
        if (stack.getItem() instanceof ItemFluidBucket) return ItemFluidBucket.getFluid(stack);
        if (stack.is(Items.WATER_BUCKET)) return Fluids.WATER;
        if (stack.is(Items.LAVA_BUCKET)) return Fluids.LAVA;
        return Fluids.NONE;
    }

    /** Slot 2 mirrors the source tank's loadTank path: it accepts only a full, matching fluid source. */
    private boolean isDrainable(ItemStack stack) {
        if (MachineFluidContainerIO.acceptsInfiniteSource(tank, stack)) return true;
        if (stack.isEmpty() || tank.getPressure() != 0 || tank.getFill() >= tank.getMaxFill()) return false;
        FluidType type;
        int amount;
        if (stack.getItem() instanceof IFillableItem fillable) {
            type = fillable.getFirstFluidType(stack);
            amount = fillable.getFill(stack);
        } else {
            type = bucketFluid(stack);
            amount = ItemFluidBucket.VOLUME;
        }
        return type != null && type != Fluids.NONE && amount > 0
                && (tank.getTankType() == Fluids.NONE || tank.getTankType() == type)
                && tank.fill(type, amount, false) > 0;
    }

    /** Slot 4 mirrors the source tank's unloadTank path: it accepts only an empty container to fill. */
    private boolean isFillable(ItemStack stack) {
        if (MachineFluidContainerIO.acceptsInfiniteSink(tank, stack)) return true;
        if (stack.isEmpty() || tank.getPressure() != 0 || tank.getTankType() == Fluids.NONE
                || tank.getFill() <= 0) return false;
        if (stack.is(Items.BUCKET)) return true;
        if (!(stack.getItem() instanceof IFillableItem fillable)) return false;

        // Slot 4 is the source's empty-canister input; filled canisters belong in slot 2.
        return fillable.getFill(stack) == 0 && fillable.getFirstFluidType(stack) == Fluids.NONE
                && fillable.acceptsFluid(tank.getTankType(), stack);
    }

    /**
     * Slot roles: 0/1 = identifier in/out (identifiers <b>only</b>); 2/3 = full source into tank and
     * emptied result; 4/5 = empty container from which the tank is extracted and filled result. Slots 3/5
     * remain handler-valid so the machine can insert results; player insertion is blocked by the menu's
     * take-only slots.
     */
    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case 0, 1 -> stack.getItem() instanceof IItemFluidId;
            case 2 -> isDrainable(stack);
            case 4 -> isFillable(stack);
            default -> true; // 3 + 5: outputs — the machine inserts results here; player UI is take-only
        };
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
            ForgeDirection dir = MachineCore.facing(getBlockState());
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

    /**
     * 1.7.10's eight fixed (unrotated) positions two blocks out, all facing the four inner-corner proxy
     * cells (core ± 1, ± 1). Whatever the facing, the four along the long axis fall inside the 5×3
     * footprint and are dead; the other four sit against the long sides, which is where 1.7 connects.
     */
    public DirPos[] getConPos() {
        int x = getBlockPos().getX(), y = getBlockPos().getY(), z = getBlockPos().getZ();
        return new DirPos[]{
                new DirPos(x + 2, y, z - 1, Library.POS_X), new DirPos(x + 2, y, z + 1, Library.POS_X),
                new DirPos(x - 2, y, z - 1, Library.NEG_X), new DirPos(x - 2, y, z + 1, Library.NEG_X),
                new DirPos(x - 1, y, z + 2, Library.POS_Z), new DirPos(x + 1, y, z + 2, Library.POS_Z),
                new DirPos(x - 1, y, z - 2, Library.NEG_Z), new DirPos(x + 1, y, z - 2, Library.NEG_Z)};
    }

    /**
     * Buffer mode makes the tank a length of duct: it holds its own footprint as cells, so runs touching
     * two different ports are one network. Every other mode gives them back and lets the ducts find it by
     * probing, which is all a plain provider or receiver ever needed.
     */
    private void updateCells(ServerLevel level, FluidType type) {
        cells = mode == 1 && type != Fluids.NONE
                ? FluidGraph.hold(level, this, type, getConPos())
                : FluidGraph.release(level, cells);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        unregisterClimbable();
        if (cells.length > 0 && FluidGraph.dropCells(this, cells) && level instanceof ServerLevel server) {
            EndpointRegistry.of(server).withdraw(getBlockPos());
        }
    }

    // --- IFluidStandardTransceiverMK2 (tank-backed, gated by mode) ---

    @Override
    public FluidTankNTM[] getAllTanks() {
        return new FluidTankNTM[]{tank};
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        if (hasExploded) return new FluidTankNTM[0];
        return (mode == 1 || mode == 2) ? new FluidTankNTM[]{tank} : new FluidTankNTM[0];
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        if (hasExploded) return new FluidTankNTM[0];
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
        buf.writeBoolean(hasExploded);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        mode = buf.readShort();
        tank.deserialize(buf);
        hasExploded = buf.readBoolean();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putShort("mode", mode);
        tank.writeToNBT(output, "tank");
        output.putBoolean("exploded", hasExploded);
        output.putBoolean("onFire", onFire);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        mode = (short) input.getShortOr("mode", (short) 0);
        tank.readFromNBT(input, "tank");
        hasExploded = input.getBooleanOr("exploded", false);
        onFire = input.getBooleanOr("onFire", false);
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
