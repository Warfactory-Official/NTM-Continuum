package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.client.sound.AssemblerSoundHandler;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.container.ContainerMachineAssemblyMachine;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.lib.DirPos;
import com.hbm.lib.Library;
import com.hbm.modules.machine.ModuleMachineAssembler;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Random;

public class TileEntityMachineAssemblyMachine extends TileEntityMachineBase implements MenuProvider, IEnergyReceiverMK2, WorldlyContainer, IControlReceiver {

    private static final int[] IO_SLOTS = {4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};

    public long power;
    public long maxPower = 100_000;
    public boolean didProcess = false;

    public final ModuleMachineAssembler assemblerModule;

    public final AssemblerArm[] arms = {new AssemblerArm(), new AssemblerArm()};
    public double ring;
    public double prevRing;
    public double ringTarget;
    public double ringSpeed;
    public int ringDelay;

    private boolean clientWasProcessing = false;
    private final Random rand = new Random();

    public TileEntityMachineAssemblyMachine(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_ASSEMBLY_MACHINE.get(), pos, state, 17);
        AssemblyMachineRecipes.INSTANCE.registerRecipes();
        this.assemblerModule = new ModuleMachineAssembler(0, this, this.inventory).itemInput(4).itemOutput(16);
    }

    @Override
    public String getDefaultName() {
        return "container.machine_assembly_machine";
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return true;
        if (slot == 1) return stack.getItem() instanceof ItemBlueprints;
        if (slot >= 2 && slot <= 3) return false;
        if (slot == 16) return false;

        return assemblerModule.isItemValidForPool(slot, stack, inventory.getStackInSlot(1));
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return IO_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= 4 && slot <= 15 && isItemValidForSlot(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == 16;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineAssemblyMachine te) {
        for (DirPos dp : te.getConPos()) te.trySubscribe(level, dp);

        ItemStack blueprint = te.inventory.getStackInSlot(1);
        te.assemblerModule.update(1.0D, 1.0D, true, blueprint);
        te.didProcess = te.assemblerModule.didProcess;

        if (te.assemblerModule.markDirty) te.setChanged();
        te.networkPackNT();
    }

    public DirPos[] getConPos() {
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        return new DirPos[]{
                new DirPos(x + 2, y, z - 1, Library.POS_X), new DirPos(x + 2, y, z, Library.POS_X), new DirPos(x + 2, y, z + 1, Library.POS_X),
                new DirPos(x - 2, y, z - 1, Library.NEG_X), new DirPos(x - 2, y, z, Library.NEG_X), new DirPos(x - 2, y, z + 1, Library.NEG_X),
                new DirPos(x - 1, y, z + 2, Library.POS_Z), new DirPos(x, y, z + 2, Library.POS_Z), new DirPos(x + 1, y, z + 2, Library.POS_Z),
                new DirPos(x - 1, y, z - 2, Library.NEG_Z), new DirPos(x, y, z - 2, Library.NEG_Z), new DirPos(x + 1, y, z - 2, Library.NEG_Z),
        };
    }

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return maxPower; }
    @Override public long getReceiverSpeed() { return maxPower; }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.hbm.machine_assembly_machine");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMachineAssemblyMachine(containerId, playerInventory, this);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("index") && data.contains("selection")) {
            int index = data.getIntOr("index", 0);
            String selection = data.getStringOr("selection", "null");
            if (index == 0) {
                this.assemblerModule.recipe = selection;
                this.assemblerModule.progress = 0D;
                setChanged();
            }
        }
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeLong(maxPower);
        buf.writeBoolean(didProcess);
        assemblerModule.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        maxPower = buf.readLong();
        didProcess = buf.readBoolean();
        assemblerModule.deserialize(buf);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        assemblerModule.save(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        power = input.getLongOr("power", 0L);
        assemblerModule.load(input);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachineAssemblyMachine te) {
        boolean running = te.didProcess;

        AssemblerSoundHandler.tickLoop(te);
        if (te.clientWasProcessing && !running) {
            AssemblerSoundHandler.playStop(level, pos, te.getVolume(0.25F));
        }
        te.clientWasProcessing = running;

        for (AssemblerArm arm : te.arms) {
            arm.updateInterp();
            if (running) arm.updateArm();
            else arm.returnToNullPos();

            if (!te.muffled && arm.prevAngles[3] != arm.angles[3] && arm.angles[3] == -0.75) {
                AssemblerSoundHandler.playStrike(level, pos, te.getVolume(0.5F));
            }
        }

        te.prevRing = te.ring;
        if (!running) return;

        if (te.ring != te.ringTarget) {
            double ringDelta = Math.abs(te.ringTarget - te.ring);
            if (ringDelta <= te.ringSpeed) te.ring = te.ringTarget;
            if (te.ringTarget > te.ring) te.ring += te.ringSpeed;
            if (te.ringTarget < te.ring) te.ring -= te.ringSpeed;
            if (te.ringTarget == te.ring) {
                if (te.ringTarget >= 360) { te.ringTarget -= 360; te.ring -= 360; te.prevRing -= 360; }
                if (te.ringTarget <= -360) { te.ringTarget += 360; te.ring += 360; te.prevRing += 360; }
                te.ringDelay = 20 + te.rand.nextInt(21);
            }
        } else {
            if (te.ringDelay > 0) te.ringDelay--;
            if (te.ringDelay <= 0) {
                te.ringTarget += (te.rand.nextDouble() * 2 - 1) * 135;
                te.ringSpeed = 10D + te.rand.nextDouble() * 5D;
                if (!te.muffled) AssemblerSoundHandler.playStart(level, pos, te.getVolume(0.25F),
                        1.25F + te.rand.nextFloat() * 0.25F);
            }
        }
    }

    public static class AssemblerArm {

        public final double[] angles = new double[4];
        public final double[] prevAngles = new double[4];
        public final double[] targetAngles = new double[4];
        public final double[] speed = new double[4];

        private final Random rand = new Random();
        private ArmActionState state = ArmActionState.ASSUME_POSITION;
        private int actionDelay = 0;

        private static final double[][] POSITIONS = {
                {45, -15, -5}, {15, 15, -15}, {25, 10, -15}, {30, 0, -10}, {70, -10, -25},
        };

        public enum ArmActionState { ASSUME_POSITION, EXTEND_STRIKER, RETRACT_STRIKER }

        public AssemblerArm() {
            resetSpeed();
        }

        public void updateInterp() {
            System.arraycopy(angles, 0, prevAngles, 0, angles.length);
        }

        private void returnToNullPos() {
            for (int i = 0; i < 4; i++) targetAngles[i] = 0;
            for (int i = 0; i < 3; i++) speed[i] = 3;
            speed[3] = 0.25;
            state = ArmActionState.RETRACT_STRIKER;
            move();
        }

        private void resetSpeed() {
            speed[0] = 15; speed[1] = 15; speed[2] = 15; speed[3] = 0.5;
        }

        public void updateArm() {
            resetSpeed();
            if (actionDelay > 0) { actionDelay--; return; }

            switch (state) {
                case ASSUME_POSITION -> {
                    if (move()) { actionDelay = 2; state = ArmActionState.EXTEND_STRIKER; targetAngles[3] = -0.75D; }
                }
                case EXTEND_STRIKER -> {
                    if (move()) { state = ArmActionState.RETRACT_STRIKER; targetAngles[3] = 0D; }
                }
                case RETRACT_STRIKER -> {
                    if (move()) { actionDelay = 2 + rand.nextInt(5); chooseNewArmPosition(); state = ArmActionState.ASSUME_POSITION; }
                }
            }
        }

        private void chooseNewArmPosition() {
            double[] chosen = POSITIONS[rand.nextInt(POSITIONS.length)];
            targetAngles[0] = chosen[0];
            targetAngles[1] = chosen[1];
            targetAngles[2] = chosen[2];
        }

        private boolean move() {
            boolean didMove = false;
            for (int i = 0; i < angles.length; i++) {
                if (angles[i] == targetAngles[i]) continue;
                didMove = true;
                double angle = angles[i], target = targetAngles[i], turn = speed[i];
                if (Math.abs(angle - target) <= turn) { angles[i] = target; continue; }
                angles[i] += angle < target ? turn : -turn;
            }
            return !didMove;
        }

        public double[] getPositions(float interp) {
            return new double[]{
                    lerp(prevAngles[0], angles[0], interp),
                    lerp(prevAngles[1], angles[1], interp),
                    lerp(prevAngles[2], angles[2], interp),
                    lerp(prevAngles[3], angles[3], interp),
            };
        }

        private static double lerp(double prev, double now, float f) {
            return prev + (now - prev) * f;
        }
    }
}
