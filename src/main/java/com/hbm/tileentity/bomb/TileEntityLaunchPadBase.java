package com.hbm.tileentity.bomb;

import dev.golem.core.data.Tags;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.api.item.IDesignatorItem;
import com.hbm.entity.missile.EntityMissileAntiBallistic;
import com.hbm.entity.missile.EntityMissileBaseNT;
import com.hbm.entity.missile.EntityMissileStealth;
import com.hbm.entity.missile.EntityMissileTier0;
import com.hbm.entity.missile.EntityMissileTier1;
import com.hbm.entity.missile.EntityMissileTier2;
import com.hbm.entity.missile.EntityMissileTier3;
import com.hbm.entity.missile.EntityMissileTier4;
import com.hbm.interfaces.IBomb.BombReturnCode;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.ModMissileItems;
import com.hbm.items.machine.ItemFluidBucket;
import com.hbm.items.weapon.ItemMissileStandard;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The shared half of every launch pad — 1.12.2's {@code TileEntityLaunchPadBase}.
 *
 * <p>A pad is a small machine with one job: hold a missile, two fuels and 75 kHE, and turn a target
 * into a flying entity. Its seven slots are missile (0), designator (1), battery (2), fuel in/out
 * (3/4) and oxidiser in/out (5/6).
 *
 * <p><b>The tanks are not configured by the player.</b> They take their fluid types from whatever
 * missile is in slot 0 — {@link ItemMissileStandard.MissileFuel} names the pair — so swapping a V2 for
 * an Atlas silently re-labels both tanks. That is upstream's design and the reason the GUI shows
 * fuel/oxidiser rather than named fluids.
 *
 * <p><b>Launching</b> costs 75 kHE and one full {@code fuelCap} of each fluid, and consumes the missile
 * item. A redstone rising edge on any block of the multiblock launches at the designator's target
 * ({@link #launchFromDesignator}); a radar or a detonator can launch at a coordinate or an entity.
 *
 * <p>Deferred: the OpenComputers component (no OC on any target), and the {@code IFluidCopiable} /
 * connection-anchor conveniences.
 */
public abstract class TileEntityLaunchPadBase extends TileEntityMachineBase
        implements IEnergyReceiverMK2, IFluidStandardReceiverMK2, com.hbm.api.block.IRadarCommandReceiver {

    /**
     * Which entity each missile item becomes. Upstream keys this by {@code ComparableStack}; a plain
     * {@link Item} key is the modern equivalent now that damage values are gone.
     *
     * <p>Only the missiles whose warheads are ported appear here. An item that is missing is not
     * launchable, and {@link #instantiateMissile} returns null for it — which the pad reports as
     * {@code ERROR_MISSING_COMPONENT}, the same answer it gives for an empty slot.
     */
    public static final Map<Item, MissileFactory> missiles = new HashMap<>();

    @FunctionalInterface
    public interface MissileFactory {
        EntityMissileBaseNT create(Level world, float x, float y, float z, int targetX, int targetZ);
    }

    public static void registerLaunchables() {
        if (!missiles.isEmpty()) return;

        // Tier 0
        missiles.put(ModMissileItems.missile_micro.get(), EntityMissileTier0.EntityMissileMicro::new);
        missiles.put(ModMissileItems.missile_taint.get(), EntityMissileTier0.EntityMissileTaint::new);
        missiles.put(ModMissileItems.missile_test.get(), EntityMissileTier0.EntityMissileTest::new);
        // Tier 1
        missiles.put(ModMissileItems.missile_generic.get(), EntityMissileTier1.EntityMissileGeneric::new);
        missiles.put(ModMissileItems.missile_decoy.get(), EntityMissileTier1.EntityMissileDecoy::new);
        missiles.put(ModMissileItems.missile_incendiary.get(), EntityMissileTier1.EntityMissileIncendiary::new);
        missiles.put(ModMissileItems.missile_buster.get(), EntityMissileTier1.EntityMissileBunkerBuster::new);
        // Tier 2
        missiles.put(ModMissileItems.missile_strong.get(), EntityMissileTier2.EntityMissileStrong::new);
        missiles.put(ModMissileItems.missile_incendiary_strong.get(), EntityMissileTier2.EntityMissileIncendiaryStrong::new);
        missiles.put(ModMissileItems.missile_buster_strong.get(), EntityMissileTier2.EntityMissileBusterStrong::new);
        missiles.put(ModMissileItems.missile_emp_strong.get(), EntityMissileTier2.EntityMissileEMPStrong::new);
        // Tier 3
        missiles.put(ModMissileItems.missile_burst.get(), EntityMissileTier3.EntityMissileBurst::new);
        missiles.put(ModMissileItems.missile_inferno.get(), EntityMissileTier3.EntityMissileInferno::new);
        // Tier 4
        missiles.put(ModMissileItems.missile_nuclear.get(), EntityMissileTier4.EntityMissileNuclear::new);
        missiles.put(ModMissileItems.missile_nuclear_cluster.get(), EntityMissileTier4.EntityMissileMirv::new);
        missiles.put(ModMissileItems.missile_doomsday.get(), EntityMissileTier4.EntityMissileDoomsday::new);
        missiles.put(ModMissileItems.missile_n2.get(), EntityMissileTier4.EntityMissileN2::new);
        // Specials
        missiles.put(ModMissileItems.missile_stealth.get(), EntityMissileStealth::new);

        // Logged because the count is the signal: this table decides which missiles a pad will even
        // accept, and it has to be built on BOTH sides — the client predicts slot placement, so an empty
        // table there would refuse every missile in the GUI while the server was perfectly happy.
        com.hbm.Constants.LOG.info("[HBM] {} launchable missiles registered", missiles.size() + 1);
    }

    public long power;
    public static final long maxPower = 100_000;
    /** What a launch costs. A pad below this is "not ready" no matter how full its tanks are. */
    public static final long launchCost = 75_000;

    public int prevRedstonePower;
    public int redstonePower;

    public int state = STATE_MISSING;
    public static final int STATE_MISSING = 0;
    public static final int STATE_LOADING = 1;
    public static final int STATE_READY = 2;

    public final FluidTankNTM[] tanks;

    /**
     * Client-side: the missile the pad is holding, for the pad's renderer.
     *
     * <p>It has two sources, and it needs both. The periodic network pack carries it while the pad is
     * being used, but {@code networkPackNT} only sends when the payload <em>changes</em> — so a player
     * who logs in next to an idle, fully loaded pad would never receive one, and the pad would render
     * empty forever. {@link #syncFullTagOnChunkLoad()} closes that: the whole block entity (inventory
     * included) travels with the chunk, and {@link #refreshRenderMissile()} derives this from slot 0.
     */
    public @Nullable ItemStack toRender;

    public TileEntityLaunchPadBase(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos,
                                   BlockState state, int slots) {
        super(type, pos, state, slots);
        this.tanks = new FluidTankNTM[]{
                new FluidTankNTM(Fluids.NONE, 24_000),
                new FluidTankNTM(Fluids.NONE, 24_000)};
    }

    @Override
    public String getDefaultName() {
        return "container.launchPad";
    }

    /**
     * The pad's renderer draws the loaded missile, which the client cannot otherwise know about — a
     * block entity's inventory is server-side. Worth the tag: a launch pad is a landmark, and one that
     * renders empty until something happens to it is a bug report.
     */
    @Override
    protected boolean syncFullTagOnChunkLoad() {
        return true;
    }

    /** Re-derives {@link #toRender} from slot 0. Run after a load, on both sides. */
    public void refreshRenderMissile() {
        ItemStack loaded = inventory.getStackInSlot(0);
        this.toRender = loaded.isEmpty() ? null : loaded.copy();
    }

    /** Where this pad reaches out to for power and fluid. */
    public abstract DirPos[] getConPos();

    /** Any extra condition beyond a valid, fuelled missile — the small pad's reload delay. */
    public abstract boolean isReadyForLaunch();

    /** How far above the core the missile spawns. */
    public abstract double getLaunchOffset();

    protected void serverTickBase(Level level) {

        // Rising edge only: a pad wired to a lever fires once when it is thrown, not every tick it is on.
        if (this.redstonePower > 0 && this.prevRedstonePower <= 0) {
            this.launchFromDesignator();
        }
        this.prevRedstonePower = this.redstonePower;

        this.power = Library.chargeTEFromItems(inventory, 2, power, maxPower);
        loadTank(tanks[0], 3, 4);
        loadTank(tanks[1], 5, 6);

        if (this.isMissileValid() && inventory.getStackInSlot(0).getItem() instanceof ItemMissileStandard missile) {
            setFuel(missile);
        }

        this.networkPackNT();
    }

    /** Drains a fluid container in {@code inSlot} into {@code tank}, emptied container → {@code outSlot}. */
    private void loadTank(FluidTankNTM tank, int inSlot, int outSlot) {
        ItemStack in = inventory.getStackInSlot(inSlot);
        if (in.isEmpty() || tank.getTankType() == Fluids.NONE) return;

        if (in.getItem() instanceof IFillableItem fillable) {
            int held = fillable.getFill(in);
            FluidType heldType = fillable.getFirstFluidType(in);
            if (held <= 0 || heldType != tank.getTankType()) return;
            int addable = tank.fill(heldType, held, false);
            if (addable <= 0) return;
            ItemStack work = in.copy();
            work.setCount(1);
            int taken = fillable.tryEmpty(heldType, addable, work);
            if (taken <= 0 || !inventory.insertItem(outSlot, work.copy(), true).isEmpty()) return;
            tank.fill(heldType, taken, true);
            inventory.extractItem(inSlot, 1, false);
            inventory.insertItem(outSlot, work, false);
            setChanged();
        } else if (in.getItem() instanceof ItemFluidBucket) {
            FluidType fluid = ItemFluidBucket.getFluid(in);
            if (fluid != tank.getTankType()) return;
            if (tank.fill(fluid, ItemFluidBucket.VOLUME, false) < ItemFluidBucket.VOLUME) return;
            ItemStack empty = new ItemStack(Items.BUCKET);
            if (!inventory.insertItem(outSlot, empty.copy(), true).isEmpty()) return;
            tank.fill(fluid, ItemFluidBucket.VOLUME, true);
            inventory.extractItem(inSlot, 1, false);
            inventory.insertItem(outSlot, empty, false);
            setChanged();
        }
    }

    /** Conforms both tanks to whichever fuel pair the loaded missile burns. */
    public void setFuel(ItemMissileStandard missile) {
        switch (missile.fuel) {
            case ETHANOL_PEROXIDE -> {
                tanks[0].setTankType(Fluids.ETHANOL);
                tanks[1].setTankType(Fluids.PEROXIDE);
            }
            case KEROSENE_PEROXIDE -> {
                tanks[0].setTankType(Fluids.KEROSENE);
                tanks[1].setTankType(Fluids.PEROXIDE);
            }
            case KEROSENE_LOXY -> {
                tanks[0].setTankType(Fluids.KEROSENE);
                tanks[1].setTankType(Fluids.OXYGEN);
            }
            case JETFUEL_LOXY -> {
                tanks[0].setTankType(Fluids.KEROSENE_REFORM);
                tanks[1].setTankType(Fluids.OXYGEN);
            }
            case SOLID -> { /* pre-fuelled: the tanks are left alone */ }
        }
    }

    // --- launch conditions ---

    public boolean isMissileValid() {
        return !inventory.getStackInSlot(0).isEmpty() && isMissileValid(inventory.getStackInSlot(0));
    }

    /**
     * Whether this pad will accept and fire {@code stack}.
     *
     * <p>Upstream asks only for {@link ItemMissileStandard#launchable}. The port adds a second half:
     * the missile must also have an <em>entity</em> in {@link #missiles}. Roughly half the missile items
     * exist without one (their warheads need subsystems that are not ported — see IMPLEMENTATION.md
     * §20), and without this check such a missile loads happily, reports {@code STATE_READY}, and then
     * does nothing at all when fired, with no message anywhere. Refusing it at the slot is the same
     * mechanism upstream uses for the rusted doomsday missile, and it is the difference between "this
     * build cannot fly that yet" and "the launch pad is broken".
     */
    public boolean isMissileValid(ItemStack stack) {
        return stack.getItem() instanceof ItemMissileStandard missile && missile.launchable
                && canBeInstantiated(stack.getItem());
    }

    /** Whether a missile item has a flying entity in this build. The ABM is built directly, not via the table. */
    public static boolean canBeInstantiated(Item item) {
        return missiles.containsKey(item) || item == ModMissileItems.missile_anti_ballistic.get();
    }

    public boolean hasFuel() {
        if (this.power < launchCost) return false;

        if (!inventory.getStackInSlot(0).isEmpty()
                && inventory.getStackInSlot(0).getItem() instanceof ItemMissileStandard missile) {
            return tanks[0].getFill() >= missile.fuelCap && tanks[1].getFill() >= missile.fuelCap;
        }
        return false;
    }

    /** Missile present and launchable, fuelled, and whatever else this pad type demands. */
    public boolean canLaunch() {
        return this.isMissileValid() && this.hasFuel() && this.isReadyForLaunch();
    }

    // --- launching ---

    /** Builds the missile entity for the loaded item, or null if that item cannot fly. */
    public @Nullable Entity instantiateMissile(int targetX, int targetZ) {
        ItemStack stack = inventory.getStackInSlot(0);
        if (stack.isEmpty()) return null;

        Entity missile = createMissileEntity(level, stack.getItem(),
                worldPosition.getX() + 0.5F, (float) (worldPosition.getY() + getLaunchOffset()),
                worldPosition.getZ() + 0.5F, targetX, targetZ);
        if (missile == null) return null;

        // The pad's facing, which the tier-4 contrail reads. Upstream derives it from the block
        // metadata; here the dummyable block's rotation is the same value in the 2..5 range.
        if (missile instanceof EntityMissileTier4) {
            missile.getEntityData().set(EntityMissileTier4.ROT_IDX, getPadRotation());
        }
        return missile;
    }

    /**
     * Builds the flying entity for a missile item, or null if this build has none for it.
     *
     * <p>Static and item-keyed rather than a method on the pad, because the launch pad's screen needs
     * the same answer to draw its preview and must not grow a second copy of the table — the ABM's
     * special case in particular, which is built directly rather than through {@link #missiles}.
     */
    public static @Nullable Entity createMissileEntity(Level level, net.minecraft.world.item.Item item,
            float x, float y, float z, int targetX, int targetZ) {
        if (item == ModMissileItems.missile_anti_ballistic.get()) {
            EntityMissileAntiBallistic missile = new EntityMissileAntiBallistic(level);
            missile.setPos(x, y, z);
            return missile;
        }

        MissileFactory factory = missiles.get(item);
        return factory == null ? null : factory.create(level, x, y, z, targetX, targetZ);
    }

    /** The facing byte handed to a tier-4 missile, in vanilla's 2..5 horizontal range. */
    protected byte getPadRotation() {
        return 5;
    }

    /** Spawns the missile, charges the launch, and consumes the item. */
    public void finalizeLaunch(Entity missile) {
        if (missile instanceof EntityMissileBaseNT ballistic) ballistic.setThrower(detonator);
        if (missile instanceof EntityMissileAntiBallistic abm) abm.setThrower(detonator);

        level.addFreshEntity(missile);
        level.playSound(null, worldPosition.getX() + 0.5, worldPosition.getY(), worldPosition.getZ() + 0.5,
                ModSounds.MISSILE_TAKEOFF.get(), SoundSource.BLOCKS, 2.0F, 1.0F);

        this.power -= launchCost;

        if (!inventory.getStackInSlot(0).isEmpty()
                && inventory.getStackInSlot(0).getItem() instanceof ItemMissileStandard item) {
            tanks[0].setFill(tanks[0].getFill() - item.fuelCap);
            tanks[1].setFill(tanks[1].getFill() - item.fuelCap);
        }

        inventory.extractItem(0, 1, false);
        setChanged();
    }

    /**
     * Who gets credit for the blast. Upstream reads a thread-local detonator context set by the
     * detonator item; the port sets it directly on the pad before firing, which covers the same two
     * callers (the redstone edge, which has none, and the detonator, which does).
     */
    public @Nullable LivingEntity detonator;

    public BombReturnCode launchFromDesignator() {
        if (!canLaunch()) return BombReturnCode.ERROR_MISSING_COMPONENT;

        boolean needsDesignator = needsDesignator(inventory.getStackInSlot(0).getItem());

        int targetX = 0;
        int targetZ = 0;

        ItemStack designatorStack = inventory.getStackInSlot(1);
        if (!designatorStack.isEmpty() && designatorStack.getItem() instanceof IDesignatorItem designator) {
            if (!designator.isReady(level, designatorStack, worldPosition) && needsDesignator) {
                return BombReturnCode.ERROR_MISSING_COMPONENT;
            }
            Vec3 coords = designator.getCoords(level, designatorStack, worldPosition);
            targetX = (int) Math.floor(coords.x);
            targetZ = (int) Math.floor(coords.z);
        } else if (needsDesignator) {
            return BombReturnCode.ERROR_MISSING_COMPONENT;
        }

        return this.launchToCoordinate(targetX, targetZ);
    }

    public BombReturnCode launchToCoordinate(int targetX, int targetZ) {
        if (!canLaunch()) return BombReturnCode.ERROR_MISSING_COMPONENT;

        Entity e = instantiateMissile(targetX, targetZ);
        if (e == null) return BombReturnCode.ERROR_MISSING_COMPONENT;

        finalizeLaunch(e);
        return BombReturnCode.LAUNCHED;
    }

    /** Fired at a moving target: an ABM is handed the entity to chase, everything else its position. */
    public BombReturnCode launchToEntity(Entity entity) {
        if (!canLaunch()) return BombReturnCode.ERROR_MISSING_COMPONENT;

        Entity e = instantiateMissile((int) Math.floor(entity.getX()), (int) Math.floor(entity.getZ()));
        if (e == null) return BombReturnCode.ERROR_MISSING_COMPONENT;

        if (e instanceof EntityMissileAntiBallistic abm) abm.tracking = entity;

        finalizeLaunch(e);
        return BombReturnCode.LAUNCHED;
    }

    /** 1.7 {@code IRadarCommandReceiver}: a radar's linked launch command fires at the map position. */
    @Override
    public boolean sendCommandPosition(int x, int y, int z) {
        return this.launchToCoordinate(x, z) == BombReturnCode.LAUNCHED;
    }

    /** 1.7 {@code IRadarCommandReceiver}: a radar's linked launch command fires at the blip's entity. */
    @Override
    public boolean sendCommandEntity(Entity target) {
        return this.launchToEntity(target) == BombReturnCode.LAUNCHED;
    }

    /** The ABM aims itself, so it is the one missile that launches with an empty designator slot. */
    public boolean needsDesignator(Item item) {
        return item != ModMissileItems.missile_anti_ballistic.get();
    }

    /**
     * Tracks how many blocks of the multiblock are currently powered, so the pad sees one rising edge
     * per redstone change rather than one per block.
     */
    public void updateRedstonePower(Level level, BlockPos pos) {
        boolean powered = level.hasNeighborSignal(pos);
        boolean contained = activatedBlocks.contains(pos.asLong());

        if (!contained && powered) {
            activatedBlocks.add(pos.asLong());
            if (redstonePower == -1) redstonePower = 0;
            redstonePower++;
        } else if (contained && !powered) {
            activatedBlocks.remove(pos.asLong());
            redstonePower--;
            if (redstonePower == 0) redstonePower = -1;
        }
        setChanged();
    }

    private final it.unimi.dsi.fastutil.longs.LongOpenHashSet activatedBlocks =
            new it.unimi.dsi.fastutil.longs.LongOpenHashSet(4);

    // --- GUI gauges ---

    public int getFuelState() {
        return getGaugeState(0);
    }

    public int getOxidizerState() {
        return getGaugeState(1);
    }

    /** 1 = enough, -1 = not enough, 0 = the missile does not use this tank (or there is none). */
    public int getGaugeState(int tank) {
        if (inventory.getStackInSlot(0).isEmpty()) return 0;

        if (inventory.getStackInSlot(0).getItem() instanceof ItemMissileStandard missile) {
            if (missile.fuel == ItemMissileStandard.MissileFuel.SOLID) return 0;
            return tanks[tank].getFill() >= missile.fuelCap ? 1 : -1;
        }
        return 0;
    }

    // --- inventory rules ---

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        // Slot 0 only, as upstream: a pad accepts a missile from a pipe and nothing else. Slots 4/6
        // must stay true so loadTank's own inserts are not rejected (the chemplant convention).
        if (slot == 0) return this.isMissileValid(stack);
        return slot == 4 || slot == 6;
    }

    // --- power + fluid ---

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return maxPower; }
    @Override public FluidTankNTM[] getAllTanks() { return this.tanks; }
    @Override public FluidTankNTM[] getReceivingTanks() { return this.tanks; }

    @Override
    public boolean canConnect(ForgeDirection dir) {
        return dir != ForgeDirection.UP && dir != ForgeDirection.DOWN;
    }

    // --- sync + persistence ---

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(this.power);
        buf.writeInt(this.state);
        tanks[0].serialize(buf);
        tanks[1].serialize(buf);

        ItemStack missile = inventory.getStackInSlot(0);
        buf.writeBoolean(!missile.isEmpty());
        if (!missile.isEmpty()) {
            buf.writeVarInt(net.minecraft.core.registries.BuiltInRegistries.ITEM.getId(missile.getItem()));
        }
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.power = buf.readLong();
        this.state = buf.readInt();
        tanks[0].deserialize(buf);
        tanks[1].deserialize(buf);

        this.toRender = buf.readBoolean()
                ? new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.byId(buf.readVarInt()))
                : null;
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("power", power);
        tanks[0].writeToNBT(output, "t0");
        tanks[1].writeToNBT(output, "t1");
        output.putInt("redstonePower", redstonePower);
        output.putInt("prevRedstonePower", prevRedstonePower);
        int[] blocks = new int[activatedBlocks.size() * 3];
        int i = 0;
        for (long packed : activatedBlocks) {
            BlockPos p = BlockPos.of(packed);
            blocks[i++] = p.getX();
            blocks[i++] = p.getY();
            blocks[i++] = p.getZ();
        }
        output.putIntArray("activatedBlocks", blocks);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        power = input.getLongOr("power", 0L);
        tanks[0].readFromNBT(input, "t0");
        tanks[1].readFromNBT(input, "t1");
        redstonePower = input.getIntOr("redstonePower", 0);
        prevRedstonePower = input.getIntOr("prevRedstonePower", 0);
        activatedBlocks.clear();
        int[] blocks = input.getIntArrayOr("activatedBlocks", new int[0]);
        for (int i = 0; i + 2 < blocks.length; i += 3) {
            activatedBlocks.add(new BlockPos(blocks[i], blocks[i + 1], blocks[i + 2]).asLong());
        }
        // Runs on the client too, for the chunk-load tag: this is what makes an untouched pad render its
        // missile the moment it comes into view.
        refreshRenderMissile();
    }
}
