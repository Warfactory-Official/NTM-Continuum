package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.api.item.IDesignatorItem;
import com.hbm.client.ClientEffects;
import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.missile.EntityRocketSoyuz;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.FluidContainerRegistry;
import com.hbm.inventory.container.ContainerLaunchpadSoyuz;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.ISatChip;
import com.hbm.items.ModFluidNetItems;
import com.hbm.items.ModItems;
import com.hbm.items.ModSatelliteItems;
import com.hbm.items.machine.ItemSatellite.EnumSatType;
import com.hbm.items.special.ItemSoyuz;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.module.portmanager.ModulePortManFluidAdaptive;
import com.hbm.module.portmanager.ModulePortManPower;
import com.hbm.particle.helper.HbmEffectNT;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.sound.ModDoorSounds;
import com.hbm.sound.ModPowerGenSounds;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.util.SavedIds;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.7 {@code TileEntityLaunchpadSoyuz}: a carriage brings the Soyuz from its shed, a rotor stands it up, five struts
 * close on it and it is fuelled for 15 s; a launch then counts down 30 s. 10 kHE/t keeps the machinery moving, and
 * each flight burns 100,000 mB of reformed kerosene and of oxygen. Slots: 0 rocket, 1 designator, 2 satellite,
 * 3 orbital module, 4..7 canisters, 8 battery, 9..26 cargo.
 */
public class TileEntityLaunchpadSoyuz extends TileEntityMachineBase
        implements MenuProvider, WorldlyContainer, IEnergyReceiverMK2, IFluidStandardReceiverMK2, IControlReceiver {

    public long power;
    public static final long maxPower = 1_000_000;
    public static final long CONSUMPTION = 10_000;
    public FluidTankNTM[] tanks;

    public static final int INDEX_STRUT1 = 0;
    public static final int INDEX_STRUT2 = 1;
    public static final int INDEX_STRUT3 = 2;
    public static final int INDEX_STRUT4 = 3;
    public static final int INDEX_STRUT5 = 4;
    /** The delivery carriage. */
    public static final int INDEX_CARRIAGE = 5;
    /** How far the carriage has stood the rocket up. */
    public static final int INDEX_ROTOR = 6;
    /** The carriage tipping after it rams the buffer stops. */
    public static final int INDEX_TILT = 7;

    public float[] positions = new float[8];
    public float[] prevPositions = new float[8];
    public float[] speed = new float[8];
    public float[] target = new float[8];
    public float[] syncPositions = new float[8];

    protected int turnProgress;

    public SoyuzStatus soyuzStatus = SoyuzStatus.ABSENT;
    public ComponentStatus strutStatus = ComponentStatus.RETRACT;
    public ComponentStatus carriageStatus = ComponentStatus.RETRACT;
    public ComponentStatus rotorStatus = ComponentStatus.RETRACT;

    public boolean cargoMode = false;
    public int loadedType = -1;
    public int fuelCountdown = 0;
    public static final int FUEL_DURATION = 15 * 20;

    public static final int COUNTDOWN_DURATION = 600;
    public int countdown;

    private final @Nullable AudioWrapper[] audios = new AudioWrapper[3];

    protected final ModulePortManFluidAdaptive moduleFluidPorts;
    protected final ModulePortManPower modulePowerPorts;
    protected PortDef @Nullable [] cachedPorts;

    public TileEntityLaunchpadSoyuz(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LAUNCHPAD_SOYUZ.get(), pos, state, 27);
        tanks = new FluidTankNTM[2];
        tanks[0] = new FluidTankNTM(Fluids.KEROSENE_REFORM, 128_000);
        tanks[1] = new FluidTankNTM(Fluids.OXYGEN, 128_000);

        moduleFluidPorts = new ModulePortManFluidAdaptive(this).setInputTanks(getReceivingTanks());
        modulePowerPorts = new ModulePortManPower(this);
    }

    @Override
    public String getDefaultName() {
        return "container.launchpadSoyuz";
    }

    private ForgeDirection dir() {
        return MachineCore.facing(getBlockState());
    }

    /** The twelve service ports on the rails' ends, one block up. */
    public PortDef[] getPorts() {
        if (cachedPorts == null) {
            ForgeDirection dir = dir();
            ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
            int x = worldPosition.getX(), y = worldPosition.getY() + 1, z = worldPosition.getZ();

            cachedPorts = new PortDef[] {
                    PortDef.make(x + dir.offsetX * 2 + rot.offsetX, y, z + dir.offsetZ * 2 + rot.offsetZ, dir),
                    PortDef.make(x + dir.offsetX * 2 - rot.offsetX, y, z + dir.offsetZ * 2 - rot.offsetZ, dir),
                    PortDef.make(x + dir.offsetX * 2 - rot.offsetX * 7, y, z + dir.offsetZ * 2 - rot.offsetZ * 7, dir),
                    PortDef.make(x + dir.offsetX * 2 - rot.offsetX * 9, y, z + dir.offsetZ * 2 - rot.offsetZ * 9, dir),
                    PortDef.make(x - dir.offsetX * 10 + rot.offsetX, y, z - dir.offsetZ * 10 + rot.offsetZ, dir.getOpposite()),
                    PortDef.make(x - dir.offsetX * 10 - rot.offsetX, y, z - dir.offsetZ * 10 - rot.offsetZ, dir.getOpposite()),
                    PortDef.make(x - dir.offsetX * 10 - rot.offsetX * 7, y, z - dir.offsetZ * 10 - rot.offsetZ * 7, dir.getOpposite()),
                    PortDef.make(x - dir.offsetX * 10 - rot.offsetX * 9, y, z - dir.offsetZ * 10 - rot.offsetZ * 9, dir.getOpposite()),

                    PortDef.make(x + dir.offsetX - rot.offsetX * 10, y, z + dir.offsetZ - rot.offsetZ * 10, rot.getOpposite()),
                    PortDef.make(x - dir.offsetX - rot.offsetX * 10, y, z - dir.offsetZ - rot.offsetZ * 10, rot.getOpposite()),
                    PortDef.make(x - dir.offsetX * 7 - rot.offsetX * 10, y, z - dir.offsetZ * 7 - rot.offsetZ * 10, rot.getOpposite()),
                    PortDef.make(x - dir.offsetX * 9 - rot.offsetX * 10, y, z - dir.offsetZ * 9 - rot.offsetZ * 10, rot.getOpposite()),
            };
        }
        return cachedPorts;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityLaunchpadSoyuz pad) {
        if (level instanceof ServerLevel server) pad.updateServer(server);
    }

    private void updateServer(ServerLevel level) {
        moduleFluidPorts.update(getPorts());
        modulePowerPorts.update(getPorts());

        this.power = Library.chargeTEFromItems(inventory, 8, power, maxPower);

        MachineFluidContainerIO.load(tanks[0], inventory, 4, 5);
        MachineFluidContainerIO.load(tanks[1], inventory, 6, 7);

        if (!hasRocketLoaded()) {
            boolean isMoving = false;
            for (int i = 0; i < this.positions.length; i++) {
                if (!this.finishedMoving(i)) {
                    isMoving = true;
                    break;
                }
            }
            // once every part is idle, the pad is ready for the next loading run
            if (!isMoving) this.soyuzStatus = SoyuzStatus.ABSENT;
            this.loadedType = -1;
        } else {
            ItemStack rocket = inventory.getStackInSlot(0);
            this.loadedType = ((ItemSoyuz) rocket.getItem()).typeOf(rocket).ordinal();
        }

        if (this.power >= CONSUMPTION) {
            this.updateStates(level);
            this.move();
            this.power -= CONSUMPTION;
        }

        for (int i = 0; i <= INDEX_STRUT5; i++) {
            if (this.finishedMoving(i) && this.wasMoving(i)) {
                ForgeDirection dir = dir();
                ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
                double x = worldPosition.getX() + 0.5 - dir.offsetX * 4 + rot.offsetX * 2;
                double z = worldPosition.getZ() + 0.5 - dir.offsetZ * 4 + rot.offsetZ * 2;
                level.playSound(null, x, worldPosition.getY() + 25, z, ModDoorSounds.SLIDING_SEAL_STOP.get(),
                        SoundSource.BLOCKS, 35F, 0.75F);
            }
        }

        this.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityLaunchpadSoyuz pad) {
        pad.updateClient(level);
    }

    private void updateClient(Level level) {
        for (int i = 0; i < this.positions.length; i++) {
            this.prevPositions[i] = this.positions[i];
            if (this.turnProgress > 0) {
                this.positions[i] = this.positions[i] + ((this.syncPositions[i] - this.positions[i]) / (float) this.turnProgress);
                --this.turnProgress;
            } else {
                this.positions[i] = this.syncPositions[i];
            }
        }

        Vec3 rocket = rocketPos();
        double x = rocket.x;
        double z = rocket.z;
        int y = worldPosition.getY();

        // 1.7 tests the local player's distance; the nearest player stands in for it on a client
        if ((this.soyuzStatus == SoyuzStatus.FUELING || this.soyuzStatus == SoyuzStatus.READY || this.soyuzStatus == SoyuzStatus.LAUNCHING)
                && this.hasOxidizer() && level.getNearestPlayer(worldPosition.getX() + 0.5, y + 2, worldPosition.getZ() + 0.5, 100, false) != null) {
            for (int i = 0; i < 3; i++) {
                CompoundTag data = new CompoundTag();
                data.putString("type", "tower");
                data.putFloat("lift", 0F);
                data.putFloat("base", 0.5F);
                data.putFloat("max", 2F);
                data.putInt("life", 70 + level.getRandom().nextInt(30));
                data.putBoolean("noWind", true);
                data.putFloat("alphaMod", 2F);
                data.putFloat("strafe", 0.075F);
                HbmEffectNT.Tower.summonParticle(level, x + level.getRandom().nextGaussian() * 0.75, y + 4,
                        z + level.getRandom().nextGaussian() * 0.75, data);
            }
        }

        List<EntityRocketSoyuz> entities = level.getEntitiesOfClass(EntityRocketSoyuz.class,
                new AABB(x - 1, y + 4, z - 1, x + 1, y + 14, z + 1));

        if (!entities.isEmpty() || (this.soyuzStatus == SoyuzStatus.LAUNCHING && this.countdown <= 20)) {
            ClientEffects.spawnSmokeShockRand(level, x, y + 1, z, 50, level.getRandom().nextGaussian() * 3 + 6);
        }

        handleSound(0, this.soyuzStatus == SoyuzStatus.LAUNCHING);
        handleSound(1, this.power >= CONSUMPTION && this.positions[INDEX_CARRIAGE] > 0 && this.positions[INDEX_CARRIAGE] < 1);
        handleSound(2, this.power >= CONSUMPTION && this.positions[INDEX_ROTOR] > 0 && this.positions[INDEX_ROTOR] < 1);
    }

    /** Where the rocket stands: four blocks back and four to the side of the core. */
    private Vec3 rocketPos() {
        ForgeDirection dir = dir();
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        return new Vec3(worldPosition.getX() + 0.5 - dir.offsetX * 4 - rot.offsetX * 4, worldPosition.getY(),
                worldPosition.getZ() + 0.5 - dir.offsetZ * 4 - rot.offsetZ * 4);
    }

    protected void handleSound(int index, boolean isRunning) {
        if (isRunning) {
            AudioWrapper audio = this.audios[index];
            if (audio == null || !audio.isPlaying()) {
                if (audio != null) audio.stopSound();
                audio = createSound(index);
                this.audios[index] = audio;
                if (audio != null) audio.startSound();
            }
            if (audio != null) {
                Vec3 pos = getSoundPosition(index);
                audio.keepAlive();
                audio.updatePosition((float) pos.x, (float) pos.y, (float) pos.z);
            }
        } else if (this.audios[index] != null) {
            this.audios[index].stopSound();
            this.audios[index] = null;

            Vec3 pos = getSoundPosition(index);
            if (index == 1) level.playLocalSound(pos.x, pos.y, pos.z, ModDoorSounds.GARAGE_STOP.get(), SoundSource.BLOCKS, 35F, 1F, false);
            if (index == 2) level.playLocalSound(pos.x, pos.y, pos.z, ModDoorSounds.WGH_BIG_STOP.get(), SoundSource.BLOCKS, 35F, 0.75F, false);
        }
    }

    protected @Nullable AudioWrapper createSound(int index) {
        Vec3 pos = getSoundPosition(index);
        SoundEvent sound;
        float range;
        float pitch;
        switch (index) {
            case 0 -> { sound = ModSounds.SOYUZ_READY.get(); range = 100F; pitch = 1.0F; }
            case 1 -> { sound = ModDoorSounds.GARAGE_MOVE.get(); range = 35F; pitch = 1.0F; }
            case 2 -> { sound = ModDoorSounds.WGH_BIG_START.get(); range = 50F; pitch = 0.75F; }
            default -> { return null; }
        }
        return AudioSystem.getLoopedSound(sound, SoundSource.BLOCKS, (float) pos.x, (float) pos.y, (float) pos.z,
                2.0F, range, pitch, 10);
    }

    protected Vec3 getSoundPosition(int index) {
        ForgeDirection rot = dir().getRotation(ForgeDirection.UP);
        Vec3 pos = rocketPos();
        double x = pos.x;
        double z = pos.z;

        if (index == 1) {
            double dist = 24 + 20.5 * (1 - this.positions[INDEX_CARRIAGE]);
            x -= rot.offsetX * dist;
            z -= rot.offsetZ * dist;
        }
        if (index == 2) {
            double dist = 18 + 20.5 * (1 - this.positions[INDEX_CARRIAGE]);
            x -= rot.offsetX * dist;
            z -= rot.offsetZ * dist;
        }
        return new Vec3(x, pos.y + 4, z);
    }

    public void updateStates(ServerLevel level) {

        if (this.soyuzStatus == SoyuzStatus.ABSENT) {

            // return: retract the struts, send the carriage away, tilt it at the buffer stops, retract the rotor
            if (this.strutStatus == ComponentStatus.DEPLOY) {
                this.strutStatus = ComponentStatus.RETRACT;
                for (int i = 0; i <= INDEX_STRUT5; i++) setTarget(i, false, 60 + level.getRandom().nextInt(21));
            }

            if (this.carriageStatus == ComponentStatus.DEPLOY) {
                this.carriageStatus = ComponentStatus.RETRACT;
                setTarget(INDEX_CARRIAGE, false, 100);
            }

            if (this.carriageStatus == ComponentStatus.RETRACT && this.finishedMoving(INDEX_CARRIAGE)) {
                if (wasMoving(INDEX_CARRIAGE)) setTarget(INDEX_TILT, true, 3);

                if (this.target[INDEX_TILT] == 0 && this.rotorStatus == ComponentStatus.DEPLOY) {
                    this.rotorStatus = ComponentStatus.RETRACT;
                    setTarget(INDEX_ROTOR, false, 100);
                }
            }

            if (this.target[INDEX_TILT] > 0 && this.finishedMoving(INDEX_TILT)) setTarget(INDEX_TILT, false, 3);

            // deploy once a rocket is loaded and the carriage and rotor are fully retracted
            if (this.hasRocketLoaded()
                    && this.carriageStatus == ComponentStatus.RETRACT && this.finishedMoving(INDEX_CARRIAGE)
                    && this.rotorStatus == ComponentStatus.RETRACT && this.finishedMoving(INDEX_ROTOR)) {
                this.carriageStatus = ComponentStatus.DEPLOY;
                setTarget(INDEX_CARRIAGE, true, 200);
                this.soyuzStatus = SoyuzStatus.LOADING;
                return;
            }
        }

        if (this.soyuzStatus == SoyuzStatus.LOADING) {

            if (this.rotorStatus == ComponentStatus.RETRACT && this.finishedMoving(INDEX_CARRIAGE)) {
                this.rotorStatus = ComponentStatus.DEPLOY;
                setTarget(INDEX_ROTOR, true, 200);
            }

            if (this.carriageStatus == ComponentStatus.DEPLOY && this.finishedMoving(INDEX_CARRIAGE)
                    && this.rotorStatus == ComponentStatus.DEPLOY && this.finishedMoving(INDEX_ROTOR)) {

                if (this.strutStatus == ComponentStatus.RETRACT) {
                    this.strutStatus = ComponentStatus.DEPLOY;
                    for (int i = 0; i <= INDEX_STRUT5; i++) setTarget(i, true, 60 + level.getRandom().nextInt(21));
                } else {
                    boolean strutsDeployed = true;
                    for (int i = 0; i <= INDEX_STRUT5; i++) {
                        if (!this.finishedMoving(i)) strutsDeployed = false;
                    }

                    if (strutsDeployed) {
                        this.fuelCountdown = FUEL_DURATION;
                        this.soyuzStatus = SoyuzStatus.FUELING;
                        return;
                    }
                }
            }
        }

        if (this.soyuzStatus == SoyuzStatus.FUELING) {
            if (this.hasAllFuel()) {
                if (this.fuelCountdown > 0) {
                    this.fuelCountdown--;
                } else {
                    this.soyuzStatus = SoyuzStatus.READY;
                    return;
                }
            }
        }

        if (this.soyuzStatus == SoyuzStatus.READY) {
            if (!this.hasAllFuel()) {
                this.fuelCountdown = FUEL_DURATION;
                this.soyuzStatus = SoyuzStatus.FUELING;
                return;
            }
        }

        if (this.soyuzStatus == SoyuzStatus.LAUNCHING) {

            if (this.carriageStatus == ComponentStatus.DEPLOY) {
                this.carriageStatus = ComponentStatus.RETRACT;
                this.setTarget(INDEX_CARRIAGE, false, 100);
            }

            if (this.carriageStatus == ComponentStatus.RETRACT && this.finishedMoving(INDEX_CARRIAGE)) {
                if (wasMoving(INDEX_CARRIAGE)) setTarget(INDEX_TILT, true, 3);

                if (this.target[INDEX_TILT] == 0 && this.rotorStatus == ComponentStatus.DEPLOY) {
                    this.rotorStatus = ComponentStatus.RETRACT;
                    setTarget(INDEX_ROTOR, false, 100);
                }
            }

            if (this.target[INDEX_TILT] > 0 && this.finishedMoving(INDEX_TILT)) setTarget(INDEX_TILT, false, 3);

            if (this.countdown > 0) {
                this.countdown--;

                if (countdown % 100 == 0 && countdown > 0) {
                    level.playSound(null, worldPosition, ModPowerGenSounds.ALARM_HATCH.get(), SoundSource.BLOCKS, 100F, 1.1F);
                }
                if (countdown == 20) {
                    level.playSound(null, worldPosition, ModSounds.SOYUZ_TAKEOFF.get(), SoundSource.BLOCKS, 100F, 1.1F);
                }
            } else {

                if (canLaunch()) {
                    this.soyuzStatus = SoyuzStatus.ABSENT;
                    this.liftOff(level);
                } else {
                    this.soyuzStatus = SoyuzStatus.READY;
                }

                for (int i = 0; i <= INDEX_STRUT5; i++) setTarget(i, false, 18 + level.getRandom().nextInt(8));
            }
        }
    }

    public boolean hasRocketLoaded() { return inventory.getStackInSlot(0).getItem() instanceof ItemSoyuz; }
    public boolean finishedMoving(int index) { return this.positions[index] == this.target[index]; }
    public boolean wasMoving(int index) { return this.positions[index] != this.prevPositions[index]; }

    public boolean hasAllFuel() { return hasJetFuel() && hasOxidizer(); }
    public boolean hasJetFuel() { return this.tanks[0].getFill() >= 100_000; }
    public boolean hasOxidizer() { return this.tanks[1].getFill() >= 100_000; }

    public void setTarget(int index, boolean deploy, int duration) {
        this.target[index] = deploy ? 1F : 0F;
        this.speed[index] = 1F / duration;
    }

    public void move() {
        for (int i = 0; i < this.positions.length; i++) {
            this.prevPositions[i] = this.positions[i];

            if (Math.abs(this.positions[i] - this.target[i]) <= this.speed[i]) {
                this.positions[i] = this.target[i];
            } else if (this.positions[i] < this.target[i]) {
                this.positions[i] += this.speed[i];
            } else {
                this.positions[i] -= this.speed[i];
            }
        }
    }

    public float getInterpPos(int index, float interp) {
        return prevPositions[index] + (positions[index] - prevPositions[index]) * interp;
    }

    public boolean canLaunch() {
        if (this.loadedType < 0) return false;
        if (!this.hasAllFuel()) return false;
        if (this.power < CONSUMPTION) return false;

        if (this.cargoMode) {
            if (inventory.getStackInSlot(1).isEmpty()) return false;
            for (int i = 9; i < 27; i++) {
                if (!inventory.getStackInSlot(i).isEmpty()) return true;
            }
            return false;
        }

        if (this.orbital() == 1) return false;
        return !inventory.getStackInSlot(2).isEmpty();
    }

    public void liftOff(ServerLevel level) {
        Vec3 pos = rocketPos();

        EntityRocketSoyuz soyuz = new EntityRocketSoyuz(ModNtmEntities.SOYUZ.get(), level);
        soyuz.setSkin(this.loadedType);
        soyuz.mode = this.cargoMode ? 1 : 0;
        soyuz.snapTo(pos.x, pos.y + 4, pos.z, 0F, 0F);
        level.addFreshEntity(soyuz);

        tanks[0].setFill(tanks[0].getFill() - 100_000);
        tanks[1].setFill(tanks[1].getFill() - 100_000);

        if (!this.cargoMode) {
            soyuz.setSat(inventory.getStackInSlot(2));
            if (this.orbital() == 2) inventory.setStackInSlot(3, ItemStack.EMPTY);
            inventory.setStackInSlot(2, ItemStack.EMPTY);
        } else {
            List<ItemStack> payload = new ArrayList<>();
            for (int i = 9; i < 27; i++) {
                payload.add(inventory.getStackInSlot(i));
                inventory.setStackInSlot(i, ItemStack.EMPTY);
            }

            ItemStack designator = inventory.getStackInSlot(1);
            if (designator.getItem() instanceof IDesignatorItem item) {
                Vec3 target = item.getCoords(level, designator, worldPosition);
                if (target != null) {
                    soyuz.targetX = (int) target.x;
                    soyuz.targetZ = (int) target.z;
                }
            }
            soyuz.setPayload(payload);
        }

        inventory.setStackInSlot(0, ItemStack.EMPTY);
        setChanged();
    }

    /** 0: no orbital module needed, 1: needed and missing, 2: needed and loaded. */
    public int orbital() {
        if (this.cargoMode) return 0;

        if (needsOrbiter(inventory.getStackInSlot(2))) {
            if (inventory.getStackInSlot(3).is(ModItems.missile_soyuz_lander.get())) return 2;
            return 1;
        }
        return 0;
    }

    // port: 1.7 compares the bare metadata, so any item at damage 4 needed an orbiter; here only the lunar ship does
    public static boolean needsOrbiter(ItemStack stack) {
        return stack.is(ModSatelliteItems.sat_gerald.get()) || ModSatelliteItems.satellite.is(stack, EnumSatType.MINER_LUNAR);
    }

    static boolean isFluidContainer(ItemStack stack, FluidTankNTM tank) {
        if (stack.isEmpty()) return false;
        if (stack.is(ModFluidNetItems.fluid_barrel_infinite.get())) return true;
        if (FluidContainerRegistry.INSTANCE.contentOf(stack, tank.getTankType()) > 0) return true;
        return stack.getItem() instanceof IFillableItem fillable && fillable.providesFluid(tank.getTankType(), stack);
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return stack.getItem() instanceof ItemSoyuz;
        if (slot == 1) return stack.getItem() instanceof IDesignatorItem;
        if (slot == 2) return stack.getItem() instanceof ISatChip && !cargoMode;
        if (slot == 3) return stack.is(ModItems.missile_soyuz_lander.get()) && !cargoMode;
        if (slot == 4) return isFluidContainer(stack, tanks[0]);
        if (slot == 5) return false;
        if (slot == 6) return isFluidContainer(stack, tanks[1]);
        if (slot == 7) return false;
        if (slot == 8) return Library.isBattery(stack);
        if (!cargoMode) return false;
        // cargo takes anything the first four slots would not
        for (int i = 0; i <= 3; i++) if (isItemValidForSlot(i, stack)) return false;
        return true;
    }

    private static final int[] ACCESS = {0, 2, 3, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26};

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return ACCESS;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return isItemValidForSlot(slot, stack);
    }

    // 1.7 TileEntityMachineBase.canExtractItem: false
    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        tanks[0].serialize(buf);
        tanks[1].serialize(buf);
        buf.writeLong(power);
        buf.writeInt(loadedType);
        buf.writeBoolean(cargoMode);
        buf.writeInt(countdown);
        buf.writeByte((byte) this.soyuzStatus.ordinal());
        for (float position : this.positions) buf.writeFloat(position);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        tanks[0].deserialize(buf);
        tanks[1].deserialize(buf);
        this.power = buf.readLong();
        this.loadedType = buf.readInt();
        this.cargoMode = buf.readBoolean();
        this.countdown = buf.readInt();
        this.soyuzStatus = SoyuzStatus.values()[Math.floorMod(buf.readByte(), SoyuzStatus.values().length)];

        for (int i = 0; i < this.positions.length; i++) {
            float newSync = buf.readFloat();
            if (this.syncPositions[i] != newSync) {
                this.syncPositions[i] = newSync;
                this.turnProgress = 3;
            }
        }
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        long savedPower = input.getLongOr("power", -1L);
        if (savedPower < 0) return;
        this.power = savedPower;
        tanks[0].readFromNBT(input, "tank0");
        tanks[1].readFromNBT(input, "tank1");
        for (int i = 0; i < 8; i++) {
            positions[i] = input.getFloatOr("p" + i, 0F);
            speed[i] = input.getFloatOr("s" + i, 0F);
            target[i] = input.getFloatOr("t" + i, 0F);
        }
        soyuzStatus = SavedIds.getEnum(input, "soyuzStatus", SoyuzStatus.ABSENT);
        strutStatus = SavedIds.getEnum(input, "strutStatus", ComponentStatus.RETRACT);
        carriageStatus = SavedIds.getEnum(input, "carriageStatus", ComponentStatus.RETRACT);
        rotorStatus = SavedIds.getEnum(input, "rotorStatus", ComponentStatus.RETRACT);
        cargoMode = input.getBooleanOr("cargoMode", false);
        fuelCountdown = input.getIntOr("fuelCountdown", 0);
        countdown = input.getIntOr("countdown", 0);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("power", power);
        tanks[0].writeToNBT(output, "tank0");
        tanks[1].writeToNBT(output, "tank1");
        for (int i = 0; i < 8; i++) {
            output.putFloat("p" + i, positions[i]);
            output.putFloat("s" + i, speed[i]);
            output.putFloat("t" + i, target[i]);
        }
        SavedIds.putEnum(output, "soyuzStatus", soyuzStatus);
        SavedIds.putEnum(output, "strutStatus", strutStatus);
        SavedIds.putEnum(output, "carriageStatus", carriageStatus);
        SavedIds.putEnum(output, "rotorStatus", rotorStatus);
        output.putBoolean("cargoMode", cargoMode);
        output.putInt("fuelCountdown", fuelCountdown);
        output.putInt("countdown", countdown);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        modulePowerPorts.destroy();
        moduleFluidPorts.destroy();
        for (int i = 0; i < audios.length; i++) {
            if (audios[i] != null) {
                audios[i].stopSound();
                audios[i] = null;
            }
        }
    }

    @Override public long getPower() { return this.power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return maxPower; }

    @Override public FluidTankNTM[] getReceivingTanks() { return tanks; }
    @Override public FluidTankNTM[] getAllTanks() { return tanks; }

    public enum SoyuzStatus {
        /** No rocket: every component returns to its rest position. */
        ABSENT,
        /** The rocket is brought to the pad. */
        LOADING,
        /** On the pad, fuelling. */
        FUELING,
        READY,
        /** Counting down. */
        LAUNCHING
    }

    public enum ComponentStatus {
        DEPLOY, RETRACT
    }

    @Override
    public boolean hasPermission(Player player) {
        return stillValid(player);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (Tags.contains(data, "cargo")) {
            this.cargoMode = Tags.getBooleanOr(data, "cargo", false);
            setChanged();
        }

        if (Tags.contains(data, "launch")) {
            if (this.soyuzStatus == SoyuzStatus.READY && canLaunch()) {
                this.soyuzStatus = SoyuzStatus.LAUNCHING;
                this.countdown = COUNTDOWN_DURATION;
                setChanged();
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return hasCustomName() ? Component.literal(getName()) : Component.translatable(getName());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerLaunchpadSoyuz(containerId, playerInventory, this);
    }
}
