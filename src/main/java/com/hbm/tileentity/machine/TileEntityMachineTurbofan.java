package com.hbm.tileentity.machine;

import dev.golem.core.data.Tags;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.inventory.container.ContainerMachineTurbofan;
import com.hbm.inventory.fluid.FluidCombustion;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.ModItems;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.items.machine.ItemMachineUpgrade;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.lib.ModDamageSources;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.inventory.fluid.trait.FluidReleaseType;
import com.hbm.tileentity.TileEntityMachinePolluting;
import com.hbm.util.TickPhase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;

/**
 * The legacy afterburning turbofan, including its thrust and blade damage volumes.
 *
 * <p>Smoke: once a second while burning, the fuel's {@code FT_Polluting} burn table goes into the 150 mB
 * smoke tanks. 1.7 calls {@code sendSmoke} at every port but lists only the blood tank as sending, so no
 * smoke is ever available to a duct; the tanks fill and then vent their overflow into the region. Kept.
 */
public class TileEntityMachineTurbofan extends TileEntityMachinePolluting
        implements IEnergyProviderMK2, IFluidStandardTransceiverMK2, IUpgradeInfoProvider, MenuProvider {

    public static final long MAX_POWER = 1_000_000L;
    public final FluidTankNTM tank = new FluidTankNTM(Fluids.KEROSENE, 24_000);
    public final FluidTankNTM blood = new FluidTankNTM(Fluids.BLOOD, 24_000);
    public long power;
    public int afterburner;
    public boolean wasOn;
    public boolean showBlood;
    public int output;
    public int consumption;
    public float spin;
    public float lastSpin;
    public int momentum;

    private final UpgradeManagerNT upgradeManager = new UpgradeManagerNT(this);

    public TileEntityMachineTurbofan(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_TURBOFAN.get(), pos, state, 5, 150);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineTurbofan fan) {
        fan.output = 0;
        fan.consumption = 0;
        fan.updateFuelType();
        fan.loadTank(level);
        fan.blood.setTankType(Fluids.BLOOD);
        fan.wasOn = false;

        fan.upgradeManager.checkSlots(fan.inventory, 2, 2);
        fan.afterburner = fan.upgradeManager.getLevel(ItemMachineUpgrade.UpgradeType.AFTERBURN);
        if (fan.inventory.getStackInSlot(2).is(ModItems.flame_pony.get())) fan.afterburner = 100;

        long burnValue = 0L;
        int amountToBurn = Math.min(1 + fan.afterburner, fan.tank.getFill());
        boolean redstone = false;
        for (DirPos connection : fan.getConPos()) {
            if (level.hasNeighborSignal(connection.getPos())) {
                redstone = true;
                break;
            }
        }

        if (!redstone) {
            FluidCombustion.Combustible fuel = FluidCombustion.getCombustible(fan.tank.getTankType());
            if (fuel != null && fuel.grade() == FluidCombustion.FuelGrade.AERO) burnValue = fuel.energyPerBucket() / 1_000L;

            // hbm-space: without air nothing burns (com.hbm.api.environment.Environment)
            if (amountToBurn > 0 && !com.hbm.api.environment.Environment.breatheAir(level, pos, amountToBurn)) amountToBurn = 0;

            // Odd but 1.7: any fluid in the tank burns, at zero output unless it is AERO-grade fuel. The fan spins, the
            // tank drains and the burn pollutes all the same; only the exhaust (below) needs a burn value.
            if (amountToBurn > 0) {
                fan.wasOn = true;
                fan.tank.setFill(fan.tank.getFill() - amountToBurn);
                fan.output = (int) (burnValue * amountToBurn * (1D + Math.min(fan.afterburner / 3D, 4D)));
                fan.power += fan.output;
                fan.consumption = amountToBurn;

                if (TickPhase.every(level, pos, 20L)) fan.pollute(fan.tank.getTankType(), FluidReleaseType.BURN, amountToBurn * 5);
            }
        }

        fan.power = Library.chargeItemsFromTE(fan.inventory, 3, fan.power, fan.power);
        for (DirPos connection : fan.getConPos()) {
            fan.tryProvide(level, connection.getPos(), connection.getDir());
            fan.trySubscribe(fan.tank.getTankType(), level, connection);
            if (fan.blood.getFill() > 0) fan.tryProvide(fan.blood, level, connection);
            fan.sendSmoke(connection);
        }

        if (burnValue > 0 && amountToBurn > 0) fan.applyExhaust(level, amountToBurn);
        fan.power = Math.min(fan.power, MAX_POWER);
        fan.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachineTurbofan fan) {
        fan.lastSpin = fan.spin;
        if (fan.wasOn) fan.momentum = Math.min(100, fan.momentum + 1);
        else fan.momentum = Math.max(0, fan.momentum - 1);
        fan.spin += fan.momentum / 2F;
        if (fan.spin >= 360F) {
            fan.spin -= 360F;
            fan.lastSpin -= 360F;
        }
        com.hbm.client.sound.TurbofanSoundHandler.tickLoop(fan);
    }

    private void updateFuelType() {
        ItemStack id = inventory.getStackInSlot(4);
        if (!(id.getItem() instanceof IItemFluidId identifier)) return;
        FluidType type = identifier.getType(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), id);
        if (type != tank.getTankType()) tank.setTankType(type);
    }

    private void loadTank(Level level) {
        ItemStack in = inventory.getStackInSlot(0);
        // 1.7 FluidLoaderInfinite.emptyItem: an infinite item feeds the tank from its slot and is never moved or consumed
        if (!in.isEmpty() && com.hbm.tileentity.machine.MachineFluidContainerIO.loadInfinite(tank, in)) {
            setChanged();
            return;
        }
        if (in.isEmpty() || !(in.getItem() instanceof IFillableItem fillable)) return;
        int held = fillable.getFill(in);
        FluidType heldType = fillable.getFirstFluidType(in);
        if (held <= 0 || heldType == Fluids.NONE) return;
        int addable = tank.fill(heldType, held, false);
        if (addable <= 0) return;
        ItemStack work = in.copy();
        work.setCount(1);
        int taken = fillable.tryEmpty(heldType, addable, work);
        if (taken <= 0 || !inventory.insertItem(1, work.copy(), true).isEmpty()) return;
        tank.fill(heldType, taken, true);
        inventory.extractItem(0, 1, false);
        inventory.insertItem(1, work, false);
        setChanged();
    }

    private void applyExhaust(Level level, int amountToBurn) {
        ForgeDirection dir = getFacing().getRotation(ForgeDirection.UP);
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        if (afterburner > 90 && level.getRandom().nextInt(30) == 0) {
            level.playSound(null, worldPosition.above(), com.hbm.sound.ModSounds.TURBOFAN_DAMAGE.get(),
                    SoundSource.BLOCKS, 3.0F, 0.95F + level.getRandom().nextFloat() * 0.2F);
        }
        affectBox(level, exhaustBox(dir, rot, -3.5D, -19.5D, 1.5D), entity -> {
            if (afterburner > 0) {
                entity.setRemainingFireTicks(100);
                entity.hurt(level.damageSources().inFire(), 5F);
            }
            push(entity, dir);
        });
        affectBox(level, exhaustBox(dir, rot, 3.5D, 8.5D, 1.5D), entity -> push(entity, dir));
        affectBox(level, exhaustBox(dir, rot, 3.5D, 3.75D, 1.5D), entity -> {
            if (Compat.hurt(entity, (net.minecraft.server.level.ServerLevel) level, ModDamageSources.turbofan(level), 1000F)
                    && !entity.isAlive()
                    && entity instanceof LivingEntity) {
                level.playSound(null, entity.blockPosition(), SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.BLOCKS,
                        2F, 0.95F + level.getRandom().nextFloat() * 0.2F);
                blood.setFill(Math.min(blood.getMaxFill(), blood.getFill() + 50));
                showBlood = true;
            }
            entity.makeStuckInBlock(level.getBlockState(worldPosition), new Vec3(0.25D, 0.05D, 0.25D));
        });
    }

    private static void push(Entity entity, ForgeDirection dir) {
        entity.setDeltaMovement(entity.getDeltaMovement().add(-dir.offsetX * 0.2D, 0D, -dir.offsetZ * 0.2D));
    }

    private void affectBox(Level level, AABB box, java.util.function.Consumer<Entity> action) {
        for (Entity entity : level.getEntitiesOfClass(Entity.class, box)) action.accept(entity);
    }

    private AABB exhaustBox(ForgeDirection dir, ForgeDirection rot, double start, double end, double width) {
        double minX = worldPosition.getX() + 0.5D + dir.offsetX * start - rot.offsetX * width;
        double maxX = worldPosition.getX() + 0.5D + dir.offsetX * end + rot.offsetX * width;
        double minZ = worldPosition.getZ() + 0.5D + dir.offsetZ * start - rot.offsetZ * width;
        double maxZ = worldPosition.getZ() + 0.5D + dir.offsetZ * end + rot.offsetZ * width;
        return new AABB(Math.min(minX, maxX), worldPosition.getY(), Math.min(minZ, maxZ),
                Math.max(minX, maxX), worldPosition.getY() + 3D, Math.max(minZ, maxZ));
    }

    public ForgeDirection getFacing() {
        return MachineCore.facing(getBlockState());
    }

    public DirPos[] getConPos() {
        ForgeDirection dir = getFacing().getRotation(ForgeDirection.UP);
        ForgeDirection rot = dir.getRotation(ForgeDirection.DOWN);
        return new DirPos[]{
                new DirPos(worldPosition.getX() + rot.offsetX * 2, worldPosition.getY(), worldPosition.getZ() + rot.offsetZ * 2, rot),
                new DirPos(worldPosition.getX() + rot.offsetX * 2 - dir.offsetX, worldPosition.getY(), worldPosition.getZ() + rot.offsetZ * 2 - dir.offsetZ, rot),
                new DirPos(worldPosition.getX() - rot.offsetX * 2, worldPosition.getY(), worldPosition.getZ() - rot.offsetZ * 2, rot.getOpposite()),
                new DirPos(worldPosition.getX() - rot.offsetX * 2 - dir.offsetX, worldPosition.getY(), worldPosition.getZ() - rot.offsetZ * 2 - dir.offsetZ, rot.getOpposite())
        };
    }

    @Override public boolean canConnect(FluidType type, ForgeDirection dir) { return dir != ForgeDirection.DOWN; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{tank}; }
    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{blood}; }
    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[]{tank, blood, smoke, smoke_leaded, smoke_poison}; }
    @Override public long getPower() { return power; }
    @Override public void setPower(long value) { power = value; }
    @Override public long getMaxPower() { return MAX_POWER; }
    public long getPowerScaled(long size) { return power * size / MAX_POWER; }
    @Override public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 3) return Library.isBattery(stack);
        if (slot == 4) return stack.getItem() instanceof IItemFluidId;
        if (slot == 2) return Library.isMachineUpgrade(stack) || stack.is(ModItems.flame_pony.get());
        return true;
    }
    @Override protected int[] getUpgradeSlotRange() { return new int[]{2, 2}; }
    @Override public String getDefaultName() { return "container.machineTurbofan"; }
    @Override public Component getDisplayName() { return Component.translatable(getDefaultName()); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ContainerMachineTurbofan(id, inventory, this);
    }
    @Override public boolean canProvideInfo(ItemMachineUpgrade.UpgradeType type, int level, boolean extendedInfo) {
        return type == ItemMachineUpgrade.UpgradeType.AFTERBURN;
    }
    @Override public void provideInfo(ItemMachineUpgrade.UpgradeType type, int level, List<Component> info, boolean extendedInfo) {
        info.add(IUpgradeInfoProvider.getStandardLabel(com.hbm.blocks.machine.ModMachineBlocks.machine_turbofan.get()));
        if (type == ItemMachineUpgrade.UpgradeType.AFTERBURN) {
            info.add(Component.translatable(IUpgradeInfoProvider.KEY_EFFICIENCY,
                    "+" + (int) (level * 100 * (1 + Math.min(level / 3D, 4D)))));
            info.add(Component.translatable(IUpgradeInfoProvider.KEY_CONSUMPTION, "+" + level * 100 + "%"));
        }
    }
    @Override public HashMap<ItemMachineUpgrade.UpgradeType, Integer> getValidUpgrades() {
        HashMap<ItemMachineUpgrade.UpgradeType, Integer> result = new HashMap<>();
        result.put(ItemMachineUpgrade.UpgradeType.AFTERBURN, 3);
        return result;
    }

    @Override public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power); buf.writeInt(afterburner); buf.writeBoolean(wasOn); buf.writeBoolean(showBlood);
        tank.serialize(buf); blood.serialize(buf);
    }
    @Override public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong(); afterburner = buf.readInt(); wasOn = buf.readBoolean(); showBlood = buf.readBoolean();
        tank.deserialize(buf); blood.deserialize(buf);
    }
    @Override protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("powerTime", power); output.putInt("afterburner", afterburner); output.putBoolean("showBlood", showBlood);
        tank.writeToNBT(output, "tank"); blood.writeToNBT(output, "blood");
    }
    @Override protected void loadData(DataIn input) {
        super.loadData(input);
        power = input.getLongOr("powerTime", 0L); afterburner = input.getIntOr("afterburner", 0);
        showBlood = input.getBooleanOr("showBlood", false); tank.readFromNBT(input, "tank"); blood.readFromNBT(input, "blood");
    }
}
