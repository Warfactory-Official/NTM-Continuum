package com.hbm.tileentity.bomb;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.event.HbmEvents;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.api.item.IDesignatorItem;
import com.hbm.entity.missile.EntityMissileCustom;
import com.hbm.handler.MissileStruct;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.ModFuelItems;
import com.hbm.items.weapon.ItemCustomMissile;
import com.hbm.items.weapon.ItemCustomMissilePart;
import com.hbm.items.weapon.ItemCustomMissilePart.PartSize;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.particle.helper.HbmEffectNT;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.tileentity.machine.MachineFluidContainerIO;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.SubLevels;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import com.hbm.tileentity.TilePort.PortDef;

/**
 * The shared body of 1.7's {@code TileEntityCompactLauncher} and {@code TileEntityLaunchTable}. Upstream wrote the
 * two as near-identical copies; the differences are kept as overrides: tank and solid-fuel capacity, which
 * fuselage top size fits ({@link #acceptedSize}), how far the redstone footprint reaches, and whether a missing
 * designator blocks {@link #canLaunch} (compact: yes, table: no).
 *
 * <p>Slots, as upstream: 0 missile, 1 designator, 2/3 fuel/oxidizer in, 4 solid fuel ({@code rocket_fuel},
 * 250 each), 5 battery, 6/7 fuel/oxidizer out. A launch costs 75% of the 100 kHE buffer plus the fuselage's whole
 * tank of fuel, and the missile lands off target by the chip's inaccuracy times the fins'.
 *
 * <p>While a custom missile stands in the ten blocks above the pad, the client puffs fifteen {@code launchSmoke}
 * plumes a tick along one random horizontal axis ({@link #smokeSpread}: compact 0.5, table 0.65). The table's
 * OpenComputers component {@code ntm_custom_launch_pad} is served to CC: Tweaked by
 * {@code com.hbm.compat.computercraft.bomb.LaunchTablePeripheral}.
 */
public abstract class TileEntityCustomMissileLauncher extends TileEntityMachineBase
        implements IEnergyReceiverMK2, IFluidStandardReceiverMK2 {

    public static final int SLOT_MISSILE = 0;

    public long power;
    public static final long maxPower = 100000;
    public int solid;
    public final FluidTankNTM[] tanks;

    /** Client side: the missile standing on the pad, from the server's pack (1.7 {@code TEMissileMultipartPacket}). */
    public @Nullable MissileStruct load;

    protected TileEntityCustomMissileLauncher(BlockEntityType<?> type, BlockPos pos, BlockState state, int tankSize) {
        super(type, pos, state, 8);
        this.tanks = new FluidTankNTM[]{new FluidTankNTM(Fluids.NONE, tankSize), new FluidTankNTM(Fluids.NONE, tankSize)};
    }

    public abstract int getMaxSolid();

    /** The fuselage top size this launcher takes. */
    public abstract PartSize acceptedSize();

    /** Half the side of the square whose blocks listen for redstone. */
    protected abstract int footprintRadius();

    protected abstract boolean requiresDesignatorToLaunch();

    public long getPowerScaled(long i) {
        return (power * i) / maxPower;
    }

    public int getSolidScaled(int i) {
        return (solid * i) / getMaxSolid();
    }

    /** 1.7's gaussian spread of the launch smoke (compact launcher 0.5, launch table 0.65). */
    protected double smokeSpread() {
        return 0.5D;
    }

    /** 1.7 {@code updateEntity}'s client branch: smoke under a custom missile standing on the pad. */
    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityCustomMissileLauncher te) {
        AABB box = new AABB(pos.getX() - 0.5, pos.getY(), pos.getZ() - 0.5,
                pos.getX() + 1.5, pos.getY() + 10, pos.getZ() + 1.5);
        if (level.getEntitiesOfClass(EntityMissileCustom.class, box).isEmpty()) return;
        for (int i = 0; i < 15; i++) {
            boolean dir = level.getRandom().nextBoolean();
            double moX = dir ? 0 : (float) (level.getRandom().nextGaussian() * te.smokeSpread());
            double moZ = !dir ? 0 : (float) (level.getRandom().nextGaussian() * te.smokeSpread());
            CompoundTag data = new CompoundTag();
            data.putDouble("moX", moX);
            data.putDouble("moY", 0);
            data.putDouble("moZ", moZ);
            HbmEffectNT.LaunchSmoke.summonParticle(level, pos.getX() + 0.5, pos.getY() + 0.25,
                    pos.getZ() + 0.5, data);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityCustomMissileLauncher te) {
        te.updateTypes();

        te.setupAllPorts(te.getPorts());
        te.updatePortPIFIFO();

        MachineFluidContainerIO.load(te.tanks[0], te.inventory, 2, 6);
        MachineFluidContainerIO.load(te.tanks[1], te.inventory, 3, 7);

        te.power = Library.chargeTEFromItems(te.inventory, 5, te.power, maxPower);

        ItemStack solidFuel = te.inventory.getStackInSlot(4);
        if (!solidFuel.isEmpty() && solidFuel.getItem() == ModFuelItems.rocket_fuel.get()
                && te.solid + 250 <= te.getMaxSolid()) {
            te.inventory.extractItem(4, 1, false);
            te.solid += 250;
        }

        te.networkPackNT();

        int r = te.footprintRadius();
        outer:
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                if (level.hasNeighborSignal(pos.offset(x, 0, z)) && te.canLaunch()) {
                    te.launchFromDesignator();
                    break outer;
                }
            }
        }
    }

    protected PortDef[] cachedPorts;
    public abstract PortDef[] getPorts();

    public boolean canLaunch() {
        return power >= maxPower * 0.75 && isMissileValid() && (!requiresDesignatorToLaunch() || hasDesignator())
                && hasFuel();
    }

    /** 1.7 IRadarCommandReceiver. Upstream passed {@code posX} for both axes; kept, it is how the radar aims. */
    public boolean sendCommandEntity(Entity target) {
        return sendCommandPosition((int) Math.floor(target.getX()), worldPosition.getY(), (int) Math.floor(target.getX()));
    }

    public boolean sendCommandPosition(int x, int y, int z) {
        if (!canLaunch()) return false;
        this.launchTo(x, z);
        return true;
    }

    public void launchFromDesignator() {
        ItemStack designatorStack = inventory.getStackInSlot(1);
        if (!designatorStack.isEmpty() && designatorStack.getItem() instanceof IDesignatorItem designator) {
            // The designator measures range from the launcher as the world sees it (it may ride a sub-level).
            BlockPos inWorld = SubLevels.toWorldBlock(level, worldPosition);
            if (designator.isReady(level, designatorStack, inWorld)) {
                Vec3 coords = designator.getCoords(level, designatorStack, inWorld);
                this.launchTo((int) Math.floor(coords.x), (int) Math.floor(coords.z));
            }
        }
    }

    public void launchTo(int tX, int tZ) {
        level.playSound(null, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                ModSounds.MISSILE_TAKEOFF.get(), SoundSource.BLOCKS, 10.0F, 1.0F);
        // The launcher as the world sees it: on a moving sub-level (Create Aeronautics) its block sits far away, and
        // both the missile's arc and its inaccuracy are laid out from here.
        Vec3 spawn = SubLevels.toWorld(level, worldPosition.getX() + 0.5D,
                worldPosition.getY() + 2.5D, worldPosition.getZ() + 0.5D);
        int x = (int) Math.floor(spawn.x), z = (int) Math.floor(spawn.z);

        ItemStack missileStack = inventory.getStackInSlot(SLOT_MISSILE);
        ItemCustomMissilePart chip = ItemCustomMissile.chip(missileStack);
        float c = chip != null ? chip.inaccuracy() : 0F;
        float f = 1.0F;

        MissileStruct struct = getStruct(missileStack);
        if (struct != null && struct.fins() != null) {
            ItemCustomMissilePart fins = ItemCustomMissile.stability(missileStack);
            if (fins != null) f = fins.inaccuracy();
        }

        Vec3 target = new Vec3((x - tX) * c * f, 0, (z - tZ) * c * f);
        // 1.7 Vec3.rotateAroundY takes radians; upstream fed it nextFloat() * 360 anyway.
        target = target.yRot(level.getRandom().nextFloat() * 360);

        ItemCustomMissile.Parts parts = ItemCustomMissile.parts(missileStack);
        EntityMissileCustom missile = new EntityMissileCustom(level, (float) spawn.x, (float) spawn.y, (float) spawn.z,
                tX + (int) target.x, tZ + (int) target.z,
                stack(parts == null ? null : parts.warhead()), stack(parts == null ? null : parts.fuselage()),
                stack(parts == null ? null : parts.stability()), stack(parts == null ? null : parts.thruster()));
        level.addFreshEntity(missile);
        HbmEvents.missileLaunched(level, worldPosition, missile, missile.targetX, missile.targetZ, null);

        subtractFuel();

        inventory.setStackInSlot(SLOT_MISSILE, ItemStack.EMPTY);
        setChanged();
    }

    private static ItemStack stack(@Nullable Item item) {
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    public boolean hasFuel() {
        return solidState() != 0 && liquidState() != 0 && oxidizerState() != 0;
    }

    private void subtractFuel() {
        MissileStruct multipart = getStruct(inventory.getStackInSlot(SLOT_MISSILE));
        if (multipart == null || multipart.fuselage() == null) return;

        ItemCustomMissilePart fuselage = multipart.fuselage();
        int fuel = (int) fuselage.fuelAmount();

        // 1.7: kerosene/hydrogen/balefire drain both tanks, xenon the fuel tank, solid the solid store
        ItemCustomMissilePart.FuelType type = fuselage.fuelType();
        if (type.solid()) {
            this.solid -= fuel;
        } else {
            tanks[0].setFill(tanks[0].getFill() - fuel);
            if (type.oxidizer != null) tanks[1].setFill(tanks[1].getFill() - fuel);
        }

        this.power -= (long) (maxPower * 0.75);
    }

    public static @Nullable MissileStruct getStruct(ItemStack stack) {
        return ItemCustomMissile.getStruct(stack);
    }

    public boolean isMissileValid() {
        MissileStruct multipart = getStruct(inventory.getStackInSlot(SLOT_MISSILE));
        if (multipart == null || multipart.fuselage() == null) return false;
        return multipart.fuselage().top == acceptedSize();
    }

    public boolean hasDesignator() {
        ItemStack stack = inventory.getStackInSlot(1);
        return !stack.isEmpty() && stack.getItem() instanceof IDesignatorItem designator
                && designator.isReady(level, stack, SubLevels.toWorldBlock(level, worldPosition));
    }

    /** 1 = enough, 0 = not enough, -1 = this missile burns no solid fuel (or there is none). */
    public int solidState() { return solidState(inventory.getStackInSlot(SLOT_MISSILE)); }

    public int solidState(ItemStack missile) {
        MissileStruct multipart = getStruct(missile);
        if (multipart == null || multipart.fuselage() == null) return -1;
        ItemCustomMissilePart fuselage = multipart.fuselage();
        if (fuselage.fuelType() == ItemCustomMissilePart.FuelType.SOLID) {
            return solid >= fuselage.fuelAmount() ? 1 : 0;
        }
        return -1;
    }

    public int liquidState() { return liquidState(inventory.getStackInSlot(SLOT_MISSILE)); }

    public int liquidState(ItemStack missile) {
        MissileStruct multipart = getStruct(missile);
        if (multipart == null || multipart.fuselage() == null) return -1;
        ItemCustomMissilePart fuselage = multipart.fuselage();
        return !fuselage.fuelType().solid() ? (tanks[0].getFill() >= fuselage.fuelAmount() ? 1 : 0) : -1;
    }

    public int oxidizerState() { return oxidizerState(inventory.getStackInSlot(SLOT_MISSILE)); }

    public int oxidizerState(ItemStack missile) {
        MissileStruct multipart = getStruct(missile);
        if (multipart == null || multipart.fuselage() == null) return -1;
        ItemCustomMissilePart fuselage = multipart.fuselage();
        return fuselage.fuelType().oxidizer != null ? (tanks[1].getFill() >= fuselage.fuelAmount() ? 1 : 0) : -1;
    }

    public void updateTypes() {
        MissileStruct multipart = getStruct(inventory.getStackInSlot(SLOT_MISSILE));
        if (multipart == null || multipart.fuselage() == null) return;

        // 1.7 updateTypes: each fuel names its tanks' fluids; solid fuel and a missing oxidiser leave a tank alone
        ItemCustomMissilePart.FuelType type = multipart.fuselage().fuelType();
        if (type.fuel != null) tanks[0].setTankType(type.fuel.get());
        if (type.oxidizer != null) tanks[1].setTankType(type.oxidizer.get());
    }

    // --- inventory: 1.7 isItemValidForSlot was false everywhere; slots 6/7 must accept our own inserts ---

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == 6 || slot == 7;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return new int[]{0};
    }

    // --- power + fluid ---

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return maxPower; }
    @Override public FluidTankNTM[] getAllTanks() { return tanks; }
    @Override public FluidTankNTM[] getReceivingTanks() { return tanks; }

    @Override
    public boolean canConnect(ForgeDirection dir) {
        return dir != ForgeDirection.UP && dir != ForgeDirection.UNKNOWN;
    }

    // --- sync + persistence ---

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeInt(solid);
        tanks[0].serialize(buf);
        tanks[1].serialize(buf);
        // 1.7 sent the struct in its own packet every tick, an all-empty one when the slot was empty
        MissileStruct struct = getStruct(inventory.getStackInSlot(SLOT_MISSILE));
        writeStruct(buf, struct == null ? MissileStruct.EMPTY : struct);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        solid = buf.readInt();
        tanks[0].deserialize(buf);
        tanks[1].deserialize(buf);
        load = readStruct(buf);
    }

    /** 1.7 {@code MissileStruct.writeToByteBuffer}: four item ids, 0 for an empty or misplaced slot. */
    public static void writeStruct(RegistryFriendlyByteBuf buf, MissileStruct struct) {
        MissileStruct clean = struct.sanitised();
        for (Item item : new Item[]{clean.warhead(), clean.fuselage(), clean.fins(), clean.thruster()}) {
            buf.writeVarInt(item == null ? -1 : BuiltInRegistries.ITEM.getId(item));
        }
    }

    public static MissileStruct readStruct(RegistryFriendlyByteBuf buf) {
        ItemCustomMissilePart[] parts = new ItemCustomMissilePart[4];
        for (int i = 0; i < 4; i++) {
            int id = buf.readVarInt();
            parts[i] = id < 0 ? null : BuiltInRegistries.ITEM.byId(id) instanceof ItemCustomMissilePart p ? p : null;
        }
        return new MissileStruct(parts[0], parts[1], parts[2], parts[3]);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        tanks[0].writeToNBT(output, "fuel");
        tanks[1].writeToNBT(output, "oxidizer");
        output.putInt("solidfuel", solid);
        output.putLong("power", power);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        tanks[0].readFromNBT(input, "fuel");
        tanks[1].readFromNBT(input, "oxidizer");
        solid = input.getIntOr("solidfuel", 0);
        power = input.getLongOr("power", 0L);
        // chunk-load tag on the client: the pad shows its missile before the first pack arrives
        MissileStruct struct = getStruct(inventory.getStackInSlot(SLOT_MISSILE));
        load = struct;
    }
}
