package com.hbm.tileentity.machine;

import com.hbm.items.ModMissileItems;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.missile.EntityRocketLambda;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.container.ContainerLaunchpadLambda;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.ISatChip;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
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
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code TileEntityLaunchpadLambda}: a silo whose doors open, an erector lifts the Lambda-98 out and stands it on
 * the closed doors, then lowers again ten seconds before launch. Takes 10 kHE/t while working and 24,000 mB of leaded
 * gasoline and of hydrogen peroxide per flight. In auto mode it starts the countdown as soon as it can launch.
 */
public class TileEntityLaunchpadLambda extends TileEntityMachineBase
        implements MenuProvider, WorldlyContainer, IEnergyReceiverMK2, IFluidStandardReceiverMK2, IControlReceiver,
        IRORValueProvider, IRORInteractive {

    public long power;
    public static final long maxPower = 1_000_000;
    public static final long CONSUMPTION = 10_000;
    public FluidTankNTM[] tanks;

    public static final int INDEX_DOORS = 0;
    public static final int INDEX_ERECTOR = 1;
    public static final int INDEX_ROTOR = 2;
    public static final int INDEX_CLAMPS = 3;
    public static final int INDEX_PISTONS = 4;

    public float[] positions = new float[5];
    public float[] prevPositions = new float[5];
    public float[] speed = new float[5];
    public float[] target = new float[5];
    public float[] syncPositions = new float[5];

    protected int turnProgress;

    /** The rocket stands on the silo doors, detached from the erector. */
    public boolean erected = false;
    /** The erector is on its way up with the rocket. */
    public boolean erecting = false;

    // 1.7 saves these but never reads them
    public int animationProgress = 0;
    /** Ticks every part waits before its next move. */
    public int animationDelay = 0;

    public boolean autolaunch = false;

    private final @Nullable AudioWrapper[] audios = new AudioWrapper[4];

    public static final int COUNTDOWN_DURATION = 200;
    public int countdown;

    protected PortDef @Nullable [] cachedPorts;

    public TileEntityLaunchpadLambda(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LAUNCHPAD_LAMBDA.get(), pos, state, 7);
        tanks = new FluidTankNTM[2];
        tanks[0] = new FluidTankNTM(Fluids.GASOLINE_LEADED, 64_000);
        tanks[1] = new FluidTankNTM(Fluids.PEROXIDE, 64_000);
    }

    @Override
    public String getDefaultName() {
        return "container.launchpadLambda";
    }

    /** The five ports on the front edge of the silo, one block up. */
    public PortDef[] getPorts() {
        if (cachedPorts == null) {
            ForgeDirection dir = MachineCore.facing(getBlockState());
            ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
            int x = worldPosition.getX(), y = worldPosition.getY() + 1, z = worldPosition.getZ();
            cachedPorts = new PortDef[5];
            for (int i = 0; i < 5; i++) {
                int side = 2 + i;
                cachedPorts[i] = PortDef.make(x + dir.offsetX * 7 - rot.offsetX * side, y,
                        z + dir.offsetZ * 7 - rot.offsetZ * side, dir);
            }
        }
        return cachedPorts;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return stack.is(ModMissileItems.missile_lambda.get());
        if (slot == 1) return stack.getItem() instanceof ISatChip && !TileEntityLaunchpadSoyuz.needsOrbiter(stack);
        if (slot == 2) return TileEntityLaunchpadSoyuz.isFluidContainer(stack, tanks[0]);
        if (slot == 3) return false;
        if (slot == 4) return TileEntityLaunchpadSoyuz.isFluidContainer(stack, tanks[1]);
        if (slot == 5) return false;
        if (slot == 6) return Library.isBattery(stack);
        return true;
    }

    private static final int[] ACCESS = {0, 1};

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

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityLaunchpadLambda pad) {
        if (level instanceof ServerLevel server) pad.updateServer(server);
    }

    private void updateServer(ServerLevel level) {
        this.setupAllPorts(getPorts());
        this.updatePortPIFIFO();

        this.power = Library.chargeTEFromItems(inventory, 6, power, maxPower);

        MachineFluidContainerIO.load(tanks[0], inventory, 2, 3);
        MachineFluidContainerIO.load(tanks[1], inventory, 4, 5);

        if (!this.hasRocketLoaded()) {
            this.erected = false;
            this.erecting = false;
            this.countdown = 0;
        }

        if (this.power >= CONSUMPTION) {
            this.updateStates(level);
            this.move();
            this.power -= CONSUMPTION;

            if (this.autolaunch && this.erected && this.canLaunch() && this.countdown <= 0) {
                this.countdown = COUNTDOWN_DURATION;
            }
        }

        this.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityLaunchpadLambda pad) {
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

        double x = worldPosition.getX() + 0.5;
        double z = worldPosition.getZ() + 0.5;
        int y = worldPosition.getY();

        // 1.7 tests the local player's distance; the nearest player stands in for it on a client
        if (this.erected && this.hasOxidizer() && level.getNearestPlayer(x, y + 2, z, 100, false) != null) {
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
                HbmEffectNT.Tower.summonParticle(level, x + level.getRandom().nextGaussian() * 0.25, y + 2,
                        z + level.getRandom().nextGaussian() * 0.25, data);
            }
        }

        handleSound(0, this.syncPositions[INDEX_DOORS] > 0F && this.syncPositions[INDEX_DOORS] < 3F);
        handleSound(1, !this.finishedMoving(INDEX_ERECTOR) && this.wasMoving(INDEX_ERECTOR));
        handleSound(2, this.syncPositions[INDEX_ROTOR] > 0F && this.syncPositions[INDEX_ROTOR] < 180F);
        handleSound(3, this.countdown > 0);

        if (this.wasMoving(INDEX_CLAMPS) && this.syncPositions[INDEX_CLAMPS] == 90F) {
            level.playLocalSound(x, y + 12, z, ModDoorSounds.SLIDING_SEAL_STOP.get(), SoundSource.BLOCKS, 25F, 0.75F, false);
        }
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
                audio.keepAlive();
                audio.updatePosition(worldPosition.getX() + 0.5F, soundY(index), worldPosition.getZ() + 0.5F);
            }
        } else if (this.audios[index] != null) {
            this.audios[index].stopSound();
            this.audios[index] = null;

            if (Math.abs(this.positions[INDEX_ERECTOR] - this.prevPositions[INDEX_ERECTOR]) > 5) return;

            double x = worldPosition.getX() + 0.5, y = soundY(index), z = worldPosition.getZ() + 0.5;
            if (index == 0) level.playLocalSound(x, y, z, ModDoorSounds.GARAGE_STOP.get(), SoundSource.BLOCKS, 35F, 1F, false);
            if (index == 1 && this.target[INDEX_ERECTOR] == 25F) level.playLocalSound(x, y, z, ModDoorSounds.WGH_BIG_STOP.get(), SoundSource.BLOCKS, 25F, 0.5F, false);
            if (index == 1 && this.target[INDEX_ERECTOR] != 25F) level.playLocalSound(x, y, z, ModDoorSounds.WGH_STOP.get(), SoundSource.BLOCKS, 25F, 1F, false);
            if (index == 2) level.playLocalSound(x, y, z, ModDoorSounds.WGH_BIG_STOP.get(), SoundSource.BLOCKS, 25F, 0.75F, false);
        }
    }

    protected @Nullable AudioWrapper createSound(int index) {
        SoundEvent sound;
        float volume = 2F;
        float range;
        float pitch = 1F;
        int keepAlive = 10;
        switch (index) {
            case 0 -> { sound = ModDoorSounds.GARAGE_MOVE.get(); range = 35F; }
            case 1 -> { sound = ModDoorSounds.WGH_START.get(); range = 35F; }
            case 2 -> { sound = ModDoorSounds.WGH_BIG_START.get(); range = 35F; pitch = 0.75F; }
            case 3 -> { sound = ModPowerGenSounds.ALARM_REGULAR_SIREN.get(); volume = 10F; range = 50F; keepAlive = 20; }
            default -> { return null; }
        }
        return AudioSystem.getLoopedSound(sound, SoundSource.BLOCKS, worldPosition.getX() + 0.5F, soundY(index),
                worldPosition.getZ() + 0.5F, volume, range, pitch, keepAlive);
    }

    private float soundY(int index) {
        return worldPosition.getY() + 3 + (index == 2 ? 12 : 0);
    }

    public void updateStates(ServerLevel level) {

        // erecting: open the doors, lift the erector, turn the rocket upright, close the doors, set it down
        if (finishedAllMoving() && this.erecting && !this.erected) {

            if (this.erectorDown() && this.doorsClosed()) {
                this.setTarget(INDEX_DOORS, 3F, 3F, 60);
                this.setTarget(INDEX_PISTONS, 0F, 1F, 1);
                this.setTarget(INDEX_CLAMPS, 0F, 1F, 1);
                this.setTarget(INDEX_ROTOR, 180F, 180F, 1);
            } else if (this.erectorDown() && this.doorsOpen()) {
                this.animationDelay = 20;
                this.setTarget(INDEX_ERECTOR, 27F, 27F, 100);
            } else if (this.erectorUp() && this.rotorDown()) {
                this.animationDelay = 20;
                this.setTarget(INDEX_ROTOR, 0F, 180F, 60);
                this.setTarget(INDEX_DOORS, 0F, 3F, 60);
            } else if (this.erectorUp() && this.rotorUp() && this.doorsClosed()) {
                this.animationDelay = 20;
                this.setTarget(INDEX_ERECTOR, 25F, 2F, 60);
            } else if (this.erectorCenter() && this.rotorUp() && this.doorsClosed()) {
                this.erected = true;
                setChanged();
            } else {
                // off script: run everything back
                this.erecting = false;
                setChanged();
            }
        }

        // lower the erector at T-1
        if (this.erected && this.countdown <= 20 && this.countdown > 0) {
            this.erecting = false;
            setChanged();
        }

        // after a launch, or with nothing to erect: unclamp, swing the clamps up, lower the erector
        if (finishedAllMoving() && !this.erecting) {
            this.setTarget(INDEX_DOORS, 0F, 3F, 60);

            if (this.clampsOn()) {
                this.setTarget(INDEX_PISTONS, 0.75F, 0.75F, 20);
            } else if (this.clampsOff() && this.clampsDown()) {
                this.animationDelay = 10;
                this.setTarget(INDEX_CLAMPS, 90F, 90F, 40);
            } else if (this.clampsUp() && !this.erectorDown()) {
                this.animationDelay = 20;
                this.setTarget(INDEX_ERECTOR, 0F, 25F, 100);
            } else if (this.erectorDown() && this.doorsClosed()) {
                if (this.hasRocketLoaded()) {
                    this.erecting = true;
                    setChanged();
                }
            }
        }

        if (this.erected && this.countdown > 0) {
            this.countdown--;

            if (this.countdown <= 0) {
                setChanged();
                level.playSound(null, worldPosition, ModSounds.SOYUZ_TAKEOFF.get(), SoundSource.BLOCKS, 100F, 1.1F);
                this.liftOff(level);
            }
        }
    }

    public void liftOff(ServerLevel level) {
        EntityRocketLambda rocket = new EntityRocketLambda(ModNtmEntities.LAMBDA_ROCKET.get(), level);
        rocket.snapTo(worldPosition.getX() + 0.5, worldPosition.getY() + 2, worldPosition.getZ() + 0.5, 0F, 0F);
        level.addFreshEntity(rocket);

        tanks[0].setFill(tanks[0].getFill() - 24_000);
        tanks[1].setFill(tanks[1].getFill() - 24_000);

        rocket.setSat(inventory.getStackInSlot(1));

        inventory.setStackInSlot(0, ItemStack.EMPTY);
        inventory.setStackInSlot(1, ItemStack.EMPTY);

        this.erected = false;
        setChanged();
    }

    public boolean doorsClosed() { return this.positions[INDEX_DOORS] == 0F; }
    public boolean doorsOpen() { return this.positions[INDEX_DOORS] == 3F; }
    public boolean erectorDown() { return this.positions[INDEX_ERECTOR] == 0F; }
    public boolean erectorCenter() { return this.positions[INDEX_ERECTOR] == 25F; }
    public boolean erectorUp() { return this.positions[INDEX_ERECTOR] == 27F; }
    public boolean rotorUp() { return this.positions[INDEX_ROTOR] == 0F; }
    public boolean rotorDown() { return this.positions[INDEX_ROTOR] == 180F; }
    public boolean clampsOn() { return this.positions[INDEX_PISTONS] == 0F; }
    public boolean clampsOff() { return this.positions[INDEX_PISTONS] == 0.75F; }
    public boolean clampsUp() { return this.positions[INDEX_CLAMPS] == 90F; }
    public boolean clampsDown() { return this.positions[INDEX_CLAMPS] == 0F; }

    @Override
    public String[] getFunctionInfo() {
        return new String[]{
                PREFIX_VALUE + "countdown",
                PREFIX_VALUE + "power",
                PREFIX_VALUE + "fuel",
                PREFIX_VALUE + "oxidizer",
                PREFIX_VALUE + "isauto",
                PREFIX_VALUE + "canlaunch",
                PREFIX_FUNCTION + "launch",
                PREFIX_FUNCTION + "setauto" + NAME_SEPARATOR + "on/off"};
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "countdown").equals(name)) return "" + countdown;
        if ((PREFIX_VALUE + "power").equals(name)) return "" + power;
        if ((PREFIX_VALUE + "fuel").equals(name)) return "" + tanks[0].getFill();
        if ((PREFIX_VALUE + "oxidizer").equals(name)) return "" + tanks[1].getFill();
        if ((PREFIX_VALUE + "isauto").equals(name)) return autolaunch ? "1" : "0";
        if ((PREFIX_VALUE + "canlaunch").equals(name)) return canLaunch() ? "1" : "0";
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setauto").equals(name) && params.length == 1) {
            this.autolaunch = IRORInteractive.parseBool(params[0]);
            setChanged();
        } else if ((PREFIX_FUNCTION + "launch").equals(name)) {
            if (canLaunch()) this.countdown = COUNTDOWN_DURATION;
        }
        return null;
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        tanks[0].serialize(buf);
        tanks[1].serialize(buf);
        buf.writeLong(power);
        buf.writeBoolean(erecting);
        buf.writeBoolean(erected);
        buf.writeBoolean(autolaunch);
        buf.writeInt(countdown);
        for (float value : this.target) buf.writeFloat(value);
        for (float value : this.positions) buf.writeFloat(value);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        tanks[0].deserialize(buf);
        tanks[1].deserialize(buf);
        this.power = buf.readLong();
        this.erecting = buf.readBoolean();
        this.erected = buf.readBoolean();
        this.autolaunch = buf.readBoolean();
        this.countdown = buf.readInt();
        for (int i = 0; i < this.target.length; i++) this.target[i] = buf.readFloat();
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
        for (int i = 0; i < 5; i++) {
            positions[i] = input.getFloatOr("p" + i, 0F);
            speed[i] = input.getFloatOr("s" + i, 0F);
            target[i] = input.getFloatOr("t" + i, 0F);
        }
        erected = input.getBooleanOr("erected", false);
        erecting = input.getBooleanOr("erecting", false);
        animationProgress = input.getIntOr("animationProgress", 0);
        animationDelay = input.getIntOr("animationDelay", 0);
        autolaunch = input.getBooleanOr("autolaunch", false);
        countdown = input.getIntOr("countdown", 0);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("power", power);
        tanks[0].writeToNBT(output, "tank0");
        tanks[1].writeToNBT(output, "tank1");
        for (int i = 0; i < 5; i++) {
            output.putFloat("p" + i, positions[i]);
            output.putFloat("s" + i, speed[i]);
            output.putFloat("t" + i, target[i]);
        }
        output.putBoolean("erected", erected);
        output.putBoolean("erecting", erecting);
        output.putInt("animationProgress", animationProgress);
        output.putInt("animationDelay", animationDelay);
        output.putBoolean("autolaunch", autolaunch);
        output.putInt("countdown", countdown);
    }

    public void setTarget(int index, float target, float span, int duration) {
        if (span <= 0) span = 1F;
        this.target[index] = target;
        this.speed[index] = span / duration;
    }

    public void move() {
        if (this.animationDelay > 0) this.animationDelay--;

        for (int i = 0; i < this.positions.length; i++) {
            this.prevPositions[i] = this.positions[i];
            if (this.animationDelay > 0) continue;

            if (Math.abs(this.positions[i] - this.target[i]) <= this.speed[i]) {
                this.positions[i] = this.target[i];
            } else if (this.positions[i] < this.target[i]) {
                this.positions[i] += this.speed[i];
            } else {
                this.positions[i] -= this.speed[i];
            }
        }
    }

    public boolean canLaunch() {
        if (!this.hasRocketLoaded()) return false;
        if (!this.hasAllFuel()) return false;
        if (this.power < CONSUMPTION) return false;
        ItemStack sat = inventory.getStackInSlot(1);
        if (sat.isEmpty()) return false;
        if (!this.isItemValidForSlot(1, sat)) return false;
        return !TileEntityLaunchpadSoyuz.needsOrbiter(sat);
    }

    public boolean hasRocketLoaded() { return inventory.getStackInSlot(0).is(ModMissileItems.missile_lambda.get()); }
    public boolean finishedMoving(int index) { return this.positions[index] == this.target[index]; }
    public boolean wasMoving(int index) { return this.positions[index] != this.prevPositions[index]; }

    public boolean finishedAllMoving() {
        for (int i = 0; i < this.positions.length; i++) if (!finishedMoving(i)) return false;
        return true;
    }

    public float getInterpPos(int index, float interp) {
        return prevPositions[index] + (positions[index] - prevPositions[index]) * interp;
    }

    public boolean hasAllFuel() { return hasJetFuel() && hasOxidizer(); }
    public boolean hasJetFuel() { return this.tanks[0].getFill() >= 24_000; }
    public boolean hasOxidizer() { return this.tanks[1].getFill() >= 24_000; }

    @Override public long getPower() { return this.power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return maxPower; }

    @Override public FluidTankNTM[] getReceivingTanks() { return tanks; }
    @Override public FluidTankNTM[] getAllTanks() { return tanks; }

    @Override
    public void setRemoved() {
        super.setRemoved();
        for (int i = 0; i < audios.length; i++) {
            if (audios[i] != null) {
                audios[i].stopSound();
                audios[i] = null;
            }
        }
    }

    @Override
    public boolean hasPermission(Player player) {
        return stillValid(player);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (Tags.contains(data, "auto")) {
            this.autolaunch = Tags.getBooleanOr(data, "auto", false);
            setChanged();
        }
        if (Tags.contains(data, "launch")) {
            if (canLaunch()) this.countdown = COUNTDOWN_DURATION;
        }
    }

    @Override
    public Component getDisplayName() {
        return hasCustomName() ? Component.literal(getName()) : Component.translatable(getName());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerLaunchpadLambda(containerId, playerInventory, this);
    }
}
