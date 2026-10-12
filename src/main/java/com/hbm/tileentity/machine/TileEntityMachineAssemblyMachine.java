package com.hbm.tileentity.machine;

import dev.golem.core.data.Tags;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.client.sound.AssemblerSoundHandler;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.inventory.container.ContainerMachineAssemblyMachine;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.lib.DirPos;
import com.hbm.lib.Library;
import com.hbm.modules.machine.ModuleMachineAssembler;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import net.minecraft.ChatFormatting;
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
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;

import java.util.Random;

/**
 * Assembly Machine — a functional powered machine. The server tick draws power from adjacent networks,
 * auto-selects a recipe from the inserted template (blueprint) + ingredient, and runs it via
 * {@link ModuleMachineAssembler}. Its {@code didProcess} flag is synced to the client where it gates
 * the arm/ring {@link #clientTick animation} (so the machine only moves while actually assembling).
 *
 * <p>As 1.7.10: item and fluid recipes (one 4,000 mB input and one output tank, resized per recipe by the
 * module), power from the network and from a battery in slot 0, a buffer of {@code max(power,
 * recipe.power × 100, 100,000)}, the blueprint templates, machine upgrades (slots 2–3:
 * SPEED/POWER/OVERDRIVE) and hopper/pipe access to the 12 inputs and the output.
 */
public class TileEntityMachineAssemblyMachine extends TileEntityMachineBase implements MenuProvider, IEnergyReceiverMK2,
        com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2, WorldlyContainer, IControlReceiver, IUpgradeInfoProvider, IRORValueProvider, IRORInteractive {

    /** Slots a hopper/pipe may touch: the 12 inputs (insert) + the output (extract). */
    private static final int[] IO_SLOTS = {4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};

    public long power;
    public long maxPower = 100_000;
    public boolean didProcess = false;
    public final com.hbm.inventory.fluid.tank.FluidTankNTM inputTank =
            new com.hbm.inventory.fluid.tank.FluidTankNTM(com.hbm.inventory.fluid.Fluids.NONE, 4_000);
    public final com.hbm.inventory.fluid.tank.FluidTankNTM outputTank =
            new com.hbm.inventory.fluid.tank.FluidTankNTM(com.hbm.inventory.fluid.Fluids.NONE, 4_000);

    public final ModuleMachineAssembler assemblerModule;
    public final UpgradeManagerNT upgradeManager = new UpgradeManagerNT(this);

    // --- client-only animation state (not saved; recomputed each tick, gated by didProcess) ---
    public final AssemblerArm[] arms = {new AssemblerArm(), new AssemblerArm()};
    public double ring;
    public double prevRing;
    public double ringTarget;
    public double ringSpeed;
    public int ringDelay;
    /** Client-only: whether the machine was processing last client tick (drives the "stop" sound). */
    private boolean clientWasProcessing = false;
    private final Random rand = new Random();

    public TileEntityMachineAssemblyMachine(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_ASSEMBLY_MACHINE.get(), pos, state, 17);
        AssemblyMachineRecipes.INSTANCE.registerRecipes(); // idempotent; registries are flushed by now
        this.assemblerModule = new ModuleMachineAssembler(0, this, this.inventory).itemInput(4).itemOutput(16)
                .fluidInput(inputTank).fluidOutput(outputTank);
    }

    @Override
    public String getDefaultName() {
        return "container.machine_assembly_machine";
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return true;                                       // battery slot (1.7 accepts anything)
        if (slot == 1) return stack.getItem() instanceof ItemBlueprints;  // template
        if (slot >= 2 && slot <= 3) return Library.isMachineUpgrade(stack); // upgrades (any type, like SlotUpgrade)
        if (slot == 16) return false;                                     // output
        // recipe inputs 4..15: accept the current recipe's inputs OR any recipe in the inserted
        // template's pool (so an ingredient can go in before the recipe auto-selects)
        return assemblerModule.isItemValidForPool(slot, stack, inventory.getStackInSlot(1));
    }

    // --- WorldlyContainer: lets hoppers/pipes feed the input buffer and pull from the output ---

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
        return slot == 16 || assemblerModule.isSlotClogged(slot); // 1.7: the output, or a clogged input
    }

    // --- server: power + processing ---

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineAssemblyMachine te) {
        if (te.maxPower <= 0) te.maxPower = 1_000_000;
        // 1.7: the buffer floats to the selected recipe's power x100, never below the stored power or 100k.
        com.hbm.inventory.recipes.loader.GenericRecipe recipe = te.assemblerModule.getRecipe();
        if (recipe != null) te.maxPower = recipe.power * 100;
        te.maxPower = com.hbm.util.BobMathUtil.max(te.power, te.maxPower, 100_000);
        te.power = Library.chargeTEFromItems(te.inventory, 0, te.power, te.maxPower);

        for (DirPos dp : te.getConPos()) {
            te.trySubscribe(level, dp);
            if (te.inputTank.getTankType() != com.hbm.inventory.fluid.Fluids.NONE) te.trySubscribe(te.inputTank.getTankType(), level, dp);
            if (te.outputTank.getFill() > 0) te.tryProvide(te.outputTank, level, dp);
        }

        // Upgrades (1.12.2 formulas): SPEED +1/3 progress & +100% power each, OVERDRIVE +1 progress &
        // +333% power each, POWER -25% power each; levels clamped by getValidUpgrades via the manager.
        te.upgradeManager.checkSlots(te.inventory, 2, 3);
        double speed = 1D;
        double pow = 1D;
        speed += Math.min(te.upgradeManager.getLevel(UpgradeType.SPEED), 3) / 3D;
        speed += Math.min(te.upgradeManager.getLevel(UpgradeType.OVERDRIVE), 3);
        pow -= Math.min(te.upgradeManager.getLevel(UpgradeType.POWER), 3) * 0.25D;
        pow += Math.min(te.upgradeManager.getLevel(UpgradeType.SPEED), 3) * 1D;
        pow += Math.min(te.upgradeManager.getLevel(UpgradeType.OVERDRIVE), 3) * 10D / 3D;

        // Recipe is chosen by the player in the in-GUI selector (receiveControl), not auto-selected.
        ItemStack blueprint = te.inventory.getStackInSlot(1);
        te.assemblerModule.update(speed, pow, true, blueprint);
        te.didProcess = te.assemblerModule.didProcess;
        // 1.7: every working tick turns an alloyed meteorite sword in the battery slot into the machined one
        if (te.didProcess && te.inventory.getStackInSlot(0).is(com.hbm.items.ModAbilityToolItems.meteorite_sword_alloyed.get())) {
            te.inventory.setStackInSlot(0, new ItemStack(com.hbm.items.ModAbilityToolItems.meteorite_sword_machined.get()));
        }

        if (te.assemblerModule.markDirty) te.setChanged();
        te.networkPackNT();
    }

    /** Power connection points around the 3×3 footprint (one block out from each edge cell). */
    public DirPos[] getConPos() {
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        return new DirPos[]{
                new DirPos(x + 2, y, z - 1, Library.POS_X), new DirPos(x + 2, y, z, Library.POS_X), new DirPos(x + 2, y, z + 1, Library.POS_X),
                new DirPos(x - 2, y, z - 1, Library.NEG_X), new DirPos(x - 2, y, z, Library.NEG_X), new DirPos(x - 2, y, z + 1, Library.NEG_X),
                new DirPos(x - 1, y, z + 2, Library.POS_Z), new DirPos(x, y, z + 2, Library.POS_Z), new DirPos(x + 1, y, z + 2, Library.POS_Z),
                new DirPos(x - 1, y, z - 2, Library.NEG_Z), new DirPos(x, y, z - 2, Library.NEG_Z), new DirPos(x + 1, y, z - 2, Library.NEG_Z),
        };
    }

    // --- IUpgradeInfoProvider (formulas above; text ported from the 1.12.2 provideInfo) ---

    @Override
    protected int[] getUpgradeSlotRange() {
        return new int[]{2, 3};
    }

    @Override
    public boolean canProvideInfo(UpgradeType type, int level, boolean extendedInfo) {
        return type == UpgradeType.SPEED || type == UpgradeType.POWER || type == UpgradeType.OVERDRIVE;
    }

    @Override
    public void provideInfo(UpgradeType type, int level, java.util.List<Component> info, boolean extendedInfo) {
        info.add(IUpgradeInfoProvider.getStandardLabel(ModMachineBlocks.machine_assembly_machine.get()));
        if (type == UpgradeType.SPEED) {
            info.add(Component.translatable(KEY_SPEED, "+" + (level * 100 / 3) + "%").withStyle(ChatFormatting.GREEN));
            info.add(Component.translatable(KEY_CONSUMPTION, "+" + (level * 50) + "%").withStyle(ChatFormatting.RED));
        }
        if (type == UpgradeType.POWER) {
            info.add(Component.translatable(KEY_CONSUMPTION, "-" + (level * 25) + "%").withStyle(ChatFormatting.GREEN));
        }
        if (type == UpgradeType.OVERDRIVE) {
            info.add(Component.literal("YES").withStyle(IUpgradeInfoProvider.getBlink() ? ChatFormatting.RED : ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    public java.util.HashMap<UpgradeType, Integer> getValidUpgrades() {
        java.util.HashMap<UpgradeType, Integer> upgrades = new java.util.HashMap<>();
        upgrades.put(UpgradeType.SPEED, 3);
        upgrades.put(UpgradeType.POWER, 3);
        upgrades.put(UpgradeType.OVERDRIVE, 3);
        return upgrades;
    }

    // --- IEnergyReceiverMK2 ---

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return maxPower; }
    @Override public long getReceiverSpeed() { return maxPower; }

    @Override public com.hbm.inventory.fluid.tank.FluidTankNTM[] getReceivingTanks() { return new com.hbm.inventory.fluid.tank.FluidTankNTM[]{inputTank}; }
    @Override public com.hbm.inventory.fluid.tank.FluidTankNTM[] getSendingTanks() { return new com.hbm.inventory.fluid.tank.FluidTankNTM[]{outputTank}; }
    @Override public com.hbm.inventory.fluid.tank.FluidTankNTM[] getAllTanks() { return new com.hbm.inventory.fluid.tank.FluidTankNTM[]{inputTank, outputTank}; }

    // --- MenuProvider ---

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.hbm.machine_assembly_machine");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMachineAssemblyMachine(containerId, playerInventory, this);
    }

    // --- IControlReceiver: the in-GUI recipe selector sets the active recipe (index 0) ---

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("index") && data.contains("selection")) {
            int index = Tags.getIntOr(data, "index", 0);
            String selection = Tags.getStringOr(data, "selection", "null");
            if (index == 0) {
                this.assemblerModule.setRecipe(selection, false);
                this.assemblerModule.progress = 0D;
                setChanged();
            }
        }
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[]{PREFIX_VALUE + "progress", PREFIX_VALUE + "recipe", PREFIX_VALUE + "active",
                PREFIX_FUNCTION + "setrecipe" + NAME_SEPARATOR + "name"};
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.containerMenu instanceof ContainerMachineAssemblyMachine menu
                && menu.getAssembler() == this && menu.stillValid(player);
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "progress").equals(name)) return "" + (int) Math.round(assemblerModule.progress * 100);
        if ((PREFIX_VALUE + "recipe").equals(name)) return assemblerModule.recipe;
        if ((PREFIX_VALUE + "active").equals(name)) return "" + (didProcess ? 1 : 0);
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setrecipe").equals(name) && params.length == 1) {
            assemblerModule.setRecipe(params[0], true);
            setChanged();
        }
        return null;
    }

    // --- sync (BufPacket) + persistence ---

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        inputTank.serialize(buf);
        outputTank.serialize(buf);
        buf.writeLong(power);
        buf.writeLong(maxPower);
        buf.writeBoolean(didProcess);
        assemblerModule.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        inputTank.deserialize(buf);
        outputTank.deserialize(buf);
        power = buf.readLong();
        maxPower = buf.readLong();
        didProcess = buf.readBoolean();
        assemblerModule.deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        inputTank.writeToNBT(output, "i");
        outputTank.writeToNBT(output, "o");
        output.putLong("power", power);
        output.putLong("maxPower", maxPower);
        assemblerModule.save(output);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        inputTank.readFromNBT(input, "i");
        outputTank.readFromNBT(input, "o");
        power = input.getLongOr("power", 0L);
        maxPower = input.getLongOr("maxPower", 100_000L);
        assemblerModule.load(input);
    }

    // --- client: animation (driven by the synced didProcess) ---

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachineAssemblyMachine te) {
        boolean running = te.didProcess;

        AssemblerSoundHandler.tickLoop(te); // looping motor hum while processing
        if (te.clientWasProcessing && !running) {
            AssemblerSoundHandler.playStop(level, pos, te.getVolume(0.25F)); // processing just ended
        }
        te.clientWasProcessing = running;

        for (AssemblerArm arm : te.arms) {
            arm.updateInterp();
            if (running) arm.updateArm();
            else arm.returnToNullPos();
            // The striker snaps to -0.75 at full extension — the moment a part is struck.
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
                        1.25F + te.rand.nextFloat() * 0.25F); // ring kicks to a new target
            }
        }
    }

    /**
     * One robotic arm of the assembler — a small state machine moving four interpolated angles (pivot,
     * arm, piston, striker offset). Ported verbatim from the 1.12.2 inner class.
     */
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
