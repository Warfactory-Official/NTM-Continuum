package com.hbm.tileentity.machine;

import dev.golem.core.world.Explosions;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.interfaces.IRepairable;
import com.hbm.inventory.FluidContainerRegistry;
import com.hbm.inventory.RecipesCommon;
import com.hbm.inventory.fluid.FluidCombustion;
import com.hbm.inventory.fluid.trait.FT_Corrosive;
import com.hbm.inventory.fluid.trait.FT_Polluting;
import com.hbm.inventory.fluid.trait.FluidTrait;
import com.hbm.inventory.fluid.trait.FluidReleaseType;
import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.api.energymk2.IEnergyHandlerMK2.ConnectionPriority;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.interfaces.IClimbable;
import com.hbm.inventory.container.ContainerMachineFluidTank;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FluidTraitSimple;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.items.machine.ItemFluidBucket;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.particle.helper.HbmEffectNT;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.Ids;
import dev.golem.core.world.Effects;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
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
import dev.golem.core.data.Tags;
import com.hbm.tileentity.TilePort.PortDef;

/**
 * The fluid tank multiblock's core BlockEntity: a tank-backed fluid transceiver
 * ({@link IFluidStandardTransceiverMK2}) on the {@link TileEntityMachineBase} tower. Holds a 256,000 mB
 * tank and a {@code mode} flag (receive / both / send / off); the {@link #serverTick} wires it into the
 * fluid network according to that mode, exactly like the 1.12.2 tank.
 *
 * <p>1.7's hazards are ported: an explosion (first hit), antimatter or a highly corrosive fluid tears it open
 * ({@link #hasExploded}); it then leaves the network, leaks, burns if flammable, and is repaired with a blowtorch and
 * six steel plates. The 1.7 OpenComputers component is served to CC: Tweaked by
 * {@code com.hbm.compat.computercraft.storage.MachineFluidTankPeripheral}; the control panel hooks are not
 * ported. The side ladder ({@link IClimbable}) is kept. The tank defaults to {@code NONE}; drop a
 * {@code fluid_identifier} in slot 0 to set its type (it then passes to slot 1), like the 1.12.2 tank.
 */
public class TileEntityMachineFluidTank extends TileEntityMachineBase implements MenuProvider, IFluidStandardTransceiverMK2, IClimbable, IRORValueProvider, IRORInteractive, IRepairable,
        WorldlyContainer {

    public static final int SLOT_COUNT = 6;

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

    private boolean climbRegistered = false;
    private AABB ladderAABB;

    public TileEntityMachineFluidTank(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_TANK.get(), pos, state, SLOT_COUNT);
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
        tank.setupFluidPorts(tank.getPorts());
        tank.updatePortFIFO();
        tank.ensureClimbRegistered();
        FluidType type = tank.tank.getTankType();
        if (!tank.hasExploded) {
            tank.tank.setType(tank.inventory, 0, 1);
            MachineFluidContainerIO.identify(tank.tank, tank.inventory, 2);
            MachineFluidContainerIO.load(tank.tank, tank.inventory, 2, 3);
            type = tank.tank.getTankType();
        }

        // 1.7: antimatter does not stay in a tank, a highly corrosive fluid eats through it
        if (tank.tank.getFill() > 0) {
            if (isAntimatter(type)) {
                amatBlast(level, pos);
                tank.explode();
                tank.tank.setFill(0);
            }
            FT_Corrosive corrosive = type.getTrait(FT_Corrosive.class);
            if (corrosive != null && corrosive.isHighlyCorrosive()) tank.explode();

            if (tank.hasExploded) {
                int leaking;
                if (isAntimatter(type)) leaking = tank.tank.getFill();
                else if (FluidTraitSimple.isGaseousAny(type)) leaking = Math.min(tank.tank.getFill(), tank.tank.getMaxFill() / 100);
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
        new ExplosionVNT(level, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 5F)
                .makeAmat().setBlockAllocator(null).setBlockProcessor(null).explode();
    }

    /** 1.7 {@code explode()}: called when the tank breaks from hazardous contents or external force. */
    public void explode() {
        this.hasExploded = true;
        this.onFire = FluidCombustion.isFlammable(tank.getTankType());
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
        RandomSource rand = server.getRandom();

        if (isAntimatter(type)) {
            amatBlast(server, worldPosition);
        } else if (FluidCombustion.isFlammable(type) && onFire) {
            for (Entity e : server.getEntitiesOfClass(Entity.class,
                    new AABB(x - 1.5, y, z - 1.5, x + 2.5, y + 5, z + 2.5))) {
                Effects.ignite(e, 5);
            }
            // 1.7 ParticleUtil.spawnGasFlame: no scale (the gas fire's default)
            CompoundTag flame = new CompoundTag();
            flame.putDouble("moX", rand.nextGaussian() * 0.2);
            flame.putDouble("moY", 0.1);
            flame.putDouble("moZ", rand.nextGaussian() * 0.2);
            HbmEffectNT.GasFlame.send(server, x + rand.nextDouble(), y + 0.5 + rand.nextDouble(),
                    z + rand.nextDouble(), flame, 150);

            if (server.getGameTime() % 5 == 0) {
                FluidTrait.onRelease(server, worldPosition, type, FluidReleaseType.BURN, amount * 5);
            }
        } else if (FluidTraitSimple.isGaseousAny(type)) {
            if (server.getGameTime() % 5 == 0) {
                CompoundTag data = new CompoundTag();
                data.putString("type", "tower");
                data.putFloat("lift", 1F);
                data.putFloat("base", 1F);
                data.putFloat("max", 5F);
                data.putInt("life", 100 + rand.nextInt(20));
                data.putInt("color", type.getColor());
                HbmEffectNT.Tower.send(server, x + 0.5, y + 1, z + 0.5, data, 150);
            }

            if (server.getGameTime() % 5 == 0) {
                FluidTrait.onRelease(server, worldPosition, type, FluidReleaseType.SPILL, amount * 5);
            }
        }
    }

    /** 1.7 {@code tryExtinguish}: foam or CO2 put a burning tank out; water does too, unless it is a liquid fuel, which it blows up. */
    @Override
    public void tryExtinguish(Level world, BlockPos pos, EnumExtinguishType type) {
        if (!this.hasExploded || !this.onFire) return;
        if (type == EnumExtinguishType.WATER) {
            // extinguishing oil with water is a terrible idea!
            if (tank.getTankType().hasTrait(FluidTraitSimple.FT_Liquid.class)) {
                Explosions.explode(world, null, worldPosition.getX() + 0.5, worldPosition.getY() + 1.5, worldPosition.getZ() + 0.5,
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

    private final List<RecipesCommon.AStack> repair = new ArrayList<>();

    /** 1.7 {@code getRepairMaterials}: six steel plates. */
    public List<RecipesCommon.AStack> getRepairMaterials() {
        if (!repair.isEmpty()) return repair;
        repair.add(new RecipesCommon.TagStack(TagKey.create(
                Registries.ITEM, Ids.of("c", "plates/steel")), 6));
        return repair;
    }

    public void repair() {
        this.hasExploded = false;
        this.setChanged();
    }

    @Override
    public void repair(Player player) {
        repair();
    }

    public void cycleMode() {
        mode = (short) ((mode + 1) % MODES);
        setChanged();
    }

    /**
     * Slot 2 takes what 1.7's {@code loadTank} drains: a full registry container of the tank's fluid, the port's
     * universal bucket, a fillable item holding fluid, or an infinite source. 1.7's plain slots took any item by hand;
     * the menus here still refuse items no loader handles. The giant and F6 tanks share it.
     */
    public static boolean isDrainable(FluidTankNTM tank, ItemStack stack) {
        if (MachineFluidContainerIO.acceptsInfiniteSource(tank, stack)) return true;
        FluidType type = tank.getTankType();
        if (type == Fluids.NONE && tank.getFill() == 0) return MachineFluidContainerIO.heldFluid(stack) != Fluids.NONE;
        if (FluidContainerRegistry.INSTANCE.contentOf(stack, type) > 0) return true;
        if (stack.getItem() instanceof ItemFluidBucket) return type == Fluids.NONE || ItemFluidBucket.getFluid(stack) == type;
        return stack.getItem() instanceof IFillableItem fillable && fillable.getFill(stack) > 0
                && (type == Fluids.NONE || fillable.providesFluid(type, stack));
    }

    /**
     * Slot 4 takes what 1.7's {@code unloadTank} fills: an empty registry container (bucket, glass bottle, canister,
     * steel tank, cell), a fillable item that accepts the tank's fluid, or an infinite drain. port: a stackable
     * fillable must come empty (partly filled canisters are port-only and belong in slot 2); an unstackable one
     * (jetpack, tool, gun) is filled in place whatever its fill.
     */
    public static boolean isFillable(FluidTankNTM tank, ItemStack stack) {
        if (MachineFluidContainerIO.acceptsInfiniteSink(tank, stack)) return true;
        FluidType type = tank.getTankType();
        if (stack.isEmpty() || type == Fluids.NONE) return false;
        if (!FluidContainerRegistry.INSTANCE.fullOf(stack, type).isEmpty() || stack.is(Items.BUCKET)) return true;
        return stack.getItem() instanceof IFillableItem fillable && fillable.acceptsFluid(type, stack)
                && (stack.getMaxStackSize() == 1 || fillable.getFill(stack) == 0);
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
            case 2 -> isDrainable(tank, stack);
            case 4 -> isFillable(tank, stack);
            default -> true; // 3 + 5: outputs. The machine inserts results here; player UI is take-only
        };
    }

    // IClimbable: a functional ladder up one side (ported from the 1.12.2 tank)

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

    protected PortDef[] cachedPorts;
    public PortDef[] getPorts() {
        if (cachedPorts == null) {
            ForgeDirection dir = MachineCore.facing(getBlockState());
            ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
            int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
            cachedPorts = new PortDef[] {
                    PortDef.make(x + dir.offsetX + rot.offsetX, y, z + dir.offsetZ + rot.offsetZ, dir),
                    PortDef.make(x + dir.offsetX - rot.offsetX, y, z + dir.offsetZ - rot.offsetZ, dir),
                    PortDef.make(x - dir.offsetX - rot.offsetX, y, z - dir.offsetZ - rot.offsetZ, dir.getOpposite()),
                    PortDef.make(x - dir.offsetX + rot.offsetX, y, z - dir.offsetZ + rot.offsetZ, dir.getOpposite()),
            };
        }
        return cachedPorts;
    }

    public DirPos[] getConPos() { return PortDef.connections(getPorts()); }

    @Override
    public void setRemoved() {
        super.setRemoved();
        unregisterClimbable();
    }

    // IFluidStandardTransceiverMK2 (tank-backed, gated by mode)

    @Override
    public FluidTankNTM[] getAllTanks() {
        return new FluidTankNTM[]{tank};
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return new FluidTankNTM[]{tank};
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return new FluidTankNTM[]{tank};
    }

    // 1.7 runs the tank like a barrel: the mode gates what it holds and wants, the speeds scale with the fill
    @Override
    public long getDemand(FluidType type, int pressure) {
        if (this.mode == 2 || this.mode == 3) return 0;
        if (tank.getPressure() != pressure) return 0;
        return tank.accepts(type) ? tank.getMaxFill() - tank.getFill() : 0;
    }

    @Override
    public long getFluidAvailable(FluidType type, int pressure) {
        if (this.mode == 0 || this.mode == 3) return 0;
        if (tank.getPressure() != pressure) return 0;
        return tank.provides(type) ? tank.getFill() : 0;
    }

    @Override public long getReceiverSpeed(FluidType type, int pressure) { return this.hasExploded ? 0 : (mode == 0 || mode == 1) ? Math.max(500, (tank.getMaxFill() - tank.getFill()) / 100) : 0; }
    @Override public long getProviderSpeed(FluidType type, int pressure) { return this.hasExploded ? 0 : (mode == 1 || mode == 2) ? Math.max(500, tank.getFill() / 100) : 0; }

    @Override
    public ConnectionPriority getFluidPriority() {
        return mode == 1 ? ConnectionPriority.LOW : ConnectionPriority.NORMAL;
    }

    // MenuProvider

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMachineFluidTank(containerId, playerInventory, this);
    }

    /** 1.7 {@code writeNBT}: a tank with fluid in it, or a torn one, travels with its item. */
    @Override
    protected CompoundTag collectCarriedData() {
        if (tank.getFill() == 0 && !hasExploded && !tank.isStrict()) return null;
        CompoundTag data = new CompoundTag();
        tank.writeCarried(data, "tank");
        data.putShort("mode", mode);
        data.putBoolean("exploded", hasExploded);
        data.putBoolean("onFire", onFire);
        return data;
    }

    @Override
    protected void applyCarriedData(CompoundTag data) {
        tank.readCarried(data, "tank");
        mode = (short) Tags.getIntOr(data, "mode", 0);
        hasExploded = Tags.getBooleanOr(data, "exploded", false);
        onFire = Tags.getBooleanOr(data, "onFire", false);
    }

    @Override
    protected void removeSavedComponents(DataOut output) {
        super.removeSavedComponents(output);
        for (String key : new String[] {"inventory", "tank", "tank_max", "tank_type", "tank_p", "mode", "exploded", "onFire"}) {
            output.discard(key);
        }
    }

    // Sync (BufPacket) + persistence

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
    @Override public int[] getSlotsForFace(Direction side) { return new int[0]; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return false; }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return false; }
}
