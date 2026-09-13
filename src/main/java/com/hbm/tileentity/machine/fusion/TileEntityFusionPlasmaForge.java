package com.hbm.tileentity.machine.fusion;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.container.ContainerMachinePlasmaForge;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.PlasmaForgeRecipe;
import com.hbm.inventory.recipes.PlasmaForgeRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.modules.machine.ModuleMachinePlasma;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.uninos.networkproviders.PlasmaNetwork;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;
import net.minecraft.core.BlockPos;
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
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Random;

/** Functional plasma-forge controller: plasma receiver/provider, item processing and one fluid inlet. */
public class TileEntityFusionPlasmaForge extends TileEntityMachineBase
        implements MenuProvider, IEnergyReceiverMK2, IFluidStandardReceiverMK2, IFusionPowerReceiver,
        IControlReceiver, WorldlyContainer, IRORValueProvider {

    private static final int[] IO_SLOTS = {2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};

    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_BLUEPRINT = 1;
    public static final int SLOT_BOOSTER = 2;
    public static final int FIRST_INPUT = 3;
    public static final int SLOT_OUTPUT = 15;
    public static final long INITIAL_MAX_POWER = 10_000_000L;

    public final FluidTankNTM inputTank = new FluidTankNTM(Fluids.NONE, 16_000);
    public long power;
    public long maxPower = INITIAL_MAX_POWER;
    public boolean didProcess;

    public float plasmaRed;
    public float plasmaGreen;
    public float plasmaBlue;
    public long plasmaEnergy;
    public long plasmaEnergySync;
    public double neutronEnergy;
    public boolean connected;

    private PlasmaNetwork.PlasmaNode receiverNode;
    private PlasmaNetwork.PlasmaNode providerNode;
    public int booster;
    public int maxBooster;
    /** Client-only random phase used by the legacy renderer to keep neighbouring machines out of sync. */
    public int timeOffset = -1;

    /** The booster ingredients shown by the legacy empty-slot tooltip. */
    public static List<ItemStack> getBoosterDisplayStacks() {
        List<ItemStack> stacks = new java.util.ArrayList<>();
        for (Booster booster : BOOSTERS) stacks.addAll(booster.ingredient.getDisplayStacks());
        return stacks;
    }

    public double prevRing;
    public double ring;
    public double ringSpeed;
    public double ringTarget;
    public int ringDelay;
    public final ForgeArm armStriker = new ForgeArm(true);
    public final ForgeArm armJet = new ForgeArm(false);

    public final ModuleMachinePlasma plasmaModule;

    private static final List<Booster> BOOSTERS = List.of(
            new Booster(new com.hbm.inventory.RecipesCommon.ComparableStack(com.hbm.items.ModItems.nugget_co60.get()), 20),
            new Booster(new com.hbm.inventory.RecipesCommon.ComparableStack(com.hbm.items.ModItems.billet_co60.get()), 120),
            new Booster(new com.hbm.inventory.RecipesCommon.ComparableStack(com.hbm.items.ModItems.ingot_co60.get()), 200),
            new Booster(new com.hbm.inventory.RecipesCommon.ComparableStack(com.hbm.items.ModItems.powder_co60.get()), 200),
            new Booster(new com.hbm.inventory.RecipesCommon.ComparableStack(com.hbm.items.ModItems.nugget_sr90.get()), 40),
            new Booster(new com.hbm.inventory.RecipesCommon.ComparableStack(com.hbm.items.ModItems.powder_sr90_tiny.get()), 40),
            new Booster(new com.hbm.inventory.RecipesCommon.ComparableStack(com.hbm.items.ModItems.billet_sr90.get()), 240),
            new Booster(new com.hbm.inventory.RecipesCommon.ComparableStack(com.hbm.items.ModItems.ingot_sr90.get()), 400),
            new Booster(new com.hbm.inventory.RecipesCommon.ComparableStack(com.hbm.items.ModItems.powder_sr90.get()), 400),
            new Booster(new com.hbm.inventory.RecipesCommon.ComparableStack(com.hbm.items.ModItems.nugget_au198.get()), 60),
            new Booster(new com.hbm.inventory.RecipesCommon.ComparableStack(com.hbm.items.ModItems.billet_au198.get()), 360),
            new Booster(new com.hbm.inventory.RecipesCommon.ComparableStack(com.hbm.items.ModItems.ingot_au198.get()), 600),
            new Booster(new com.hbm.inventory.RecipesCommon.ComparableStack(com.hbm.items.ModItems.powder_au198.get()), 600),
            new Booster(new com.hbm.inventory.RecipesCommon.ComparableStack(com.hbm.items.ModItems.powder_i131_tiny.get()), 60),
            new Booster(new com.hbm.inventory.RecipesCommon.ComparableStack(com.hbm.items.ModItems.powder_i131.get()), 600),
            new Booster(new com.hbm.inventory.RecipesCommon.ComparableStack(com.hbm.items.ModItems.powder_xe135_tiny.get()), 60),
            new Booster(new com.hbm.inventory.RecipesCommon.ComparableStack(com.hbm.items.ModItems.powder_xe135.get()), 600),
            new Booster(new com.hbm.inventory.RecipesCommon.ComparableStack(com.hbm.items.ModItems.powder_cs137_tiny.get()), 50),
            new Booster(new com.hbm.inventory.RecipesCommon.ComparableStack(com.hbm.items.ModItems.powder_cs137.get()), 500),
            new Booster(new com.hbm.inventory.RecipesCommon.ComparableStack(com.hbm.items.ModItems.powder_at209.get()), 1_200));

    public TileEntityFusionPlasmaForge(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUSION_PLASMA_FORGE.get(), pos, state, 16);
        PlasmaForgeRecipes.INSTANCE.registerRecipes();
        this.plasmaModule = new ModuleMachinePlasma(0, this, inventory)
                .itemInput(FIRST_INPUT).itemOutput(SLOT_OUTPUT).fluidInput(inputTank);
    }

    @Override public String getDefaultName() { return "container.machinePlasmaForge"; }
    @Override public Component getDisplayName() { return Component.translatable("block.hbm.fusion_plasma_forge"); }
    @Override public boolean receivesFusionPower() { return true; }

    @Override
    public void receiveFusionPower(long fusionPower, double neutronPower, float r, float g, float b) {
        plasmaEnergy = fusionPower;
        neutronEnergy = neutronPower;
        plasmaRed = r;
        plasmaGreen = g;
        plasmaBlue = b;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityFusionPlasmaForge te) {
        te.plasmaEnergySync = te.plasmaEnergy;
        te.plasmaEnergy = 0;

        ForgeDirection facing = FusionNodes.facing(state);
        te.receiverNode = FusionNodes.plasma(level, te.receiverNode,
                pos.offset(facing.offsetX * 5, 2, facing.offsetZ * 5),
                new DirPos(pos.getX() + facing.offsetX * 6, pos.getY() + 2, pos.getZ() + facing.offsetZ * 6, facing));
        te.providerNode = FusionNodes.plasma(level, te.providerNode,
                pos.offset(-facing.offsetX * 5, 2, -facing.offsetZ * 5),
                new DirPos(pos.getX() - facing.offsetX * 6, pos.getY() + 2, pos.getZ() - facing.offsetZ * 6, facing.getOpposite()));
        if (te.receiverNode.hasValidNet()) te.receiverNode.net.addReceiver(te);
        if (te.providerNode.hasValidNet()) te.providerNode.net.addProvider(te);

        PlasmaForgeRecipe recipe = te.plasmaModule.getRecipe() instanceof PlasmaForgeRecipe plasma ? plasma : null;
        if (recipe != null) te.maxPower = recipe.power * 100L;
        te.maxPower = Math.max(Math.max(te.power, te.maxPower), 100_000L);
        te.power = Library.chargeTEFromItems(te.inventory, SLOT_BATTERY, te.power, te.maxPower);


        if (te.booster <= 0) {
            for (Booster candidate : BOOSTERS) {
                if (!candidate.ingredient.matchesRecipe(te.getItem(SLOT_BOOSTER), true)) continue;
                te.booster = te.maxBooster = candidate.ticks;
                ItemStack stack = te.getItem(SLOT_BOOSTER).copy();
                stack.shrink(1);
                te.setItem(SLOT_BOOSTER, stack.getCount() <= 0 ? ItemStack.EMPTY : stack);
                break;
            }
        }

        double speed = te.booster > 0 ? 4D : 1D;
        boolean ignition = recipe == null || recipe.ignitionTemp <= te.plasmaEnergySync;
        te.plasmaModule.update(speed, 1D, ignition, te.getItem(SLOT_BLUEPRINT));
        te.didProcess = te.plasmaModule.didProcess;
        if (te.plasmaModule.markDirty) te.setChanged();

        long powerReceived = (long) Math.ceil(te.plasmaEnergySync * 0.75D);
        te.connected = te.providerNode.hasValidNet() && !te.providerNode.net.receiverEntries.isEmpty();
        if (powerReceived > 0 && te.providerNode.hasValidNet()) {
            for (net.minecraft.world.level.block.entity.BlockEntity thing : te.providerNode.net.receiverEntries.keySet()) {
                if (thing instanceof IFusionPowerReceiver receiver) {
                    receiver.receiveFusionPower(powerReceived, te.neutronEnergy, te.plasmaRed, te.plasmaGreen, te.plasmaBlue);
                }
            }
        }
        if (te.didProcess && te.booster > 0) te.booster--;
        te.neutronEnergy = 0D;
        te.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityFusionPlasmaForge te) {
        if (te.timeOffset == -1) te.timeOffset = level.getRandom().nextInt(30_000);
        te.armStriker.updateArm(te.didProcess);
        te.armJet.updateArm(te.didProcess);
        te.prevRing = te.ring;
        if (!te.didProcess) return;
        if (te.ring != te.ringTarget) {
            double delta = Math.abs(te.ringTarget - te.ring);
            if (delta <= te.ringSpeed) te.ring = te.ringTarget;
            else te.ring += Math.copySign(te.ringSpeed, te.ringTarget - te.ring);
            if (te.ring == te.ringTarget) {
                double sub = te.ringTarget >= 360D ? -360D : 360D;
                te.ringTarget += sub;
                te.ring += sub;
                te.prevRing += sub;
                te.ringDelay = 100 + level.getRandom().nextInt(41);
            }
        } else if (--te.ringDelay <= 0) {
            te.ringTarget += (level.getRandom().nextDouble() + 1D) * 60D * (level.getRandom().nextBoolean() ? -1D : 1D);
            te.ringSpeed = 2.5D;
        }
    }

    public List<DirPos> getConPos() {
        ForgeDirection dir = FusionNodes.facing(getBlockState());
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        return List.of(
                new DirPos(x + dir.offsetX * 6 - rot.offsetX * 2, y, z + dir.offsetZ * 6 - rot.offsetZ * 2, dir),
                new DirPos(x + dir.offsetX * 6 - rot.offsetX, y, z + dir.offsetZ * 6 - rot.offsetZ, dir),
                new DirPos(x + dir.offsetX * 6, y, z + dir.offsetZ * 6, dir),
                new DirPos(x + dir.offsetX * 6 + rot.offsetX, y, z + dir.offsetZ * 6 + rot.offsetZ, dir),
                new DirPos(x + dir.offsetX * 6 + rot.offsetX * 2, y, z + dir.offsetZ * 6 + rot.offsetZ * 2, dir),
                new DirPos(x - dir.offsetX * 6 - rot.offsetX * 2, y, z - dir.offsetZ * 6 - rot.offsetZ * 2, dir.getOpposite()),
                new DirPos(x - dir.offsetX * 6 - rot.offsetX, y, z - dir.offsetZ * 6 - rot.offsetZ, dir.getOpposite()),
                new DirPos(x - dir.offsetX * 6, y, z - dir.offsetZ * 6, dir.getOpposite()),
                new DirPos(x - dir.offsetX * 6 + rot.offsetX, y, z - dir.offsetZ * 6 + rot.offsetZ, dir.getOpposite()),
                new DirPos(x - dir.offsetX * 6 + rot.offsetX * 2, y, z - dir.offsetZ * 6 + rot.offsetZ * 2, dir.getOpposite()));
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[]{PREFIX_VALUE + "progress", PREFIX_VALUE + "recipe", PREFIX_VALUE + "active",
                PREFIX_VALUE + "booster", PREFIX_VALUE + "plasma"};
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "progress").equals(name)) return "" + (int) Math.round(plasmaModule.progress * 100);
        if ((PREFIX_VALUE + "recipe").equals(name)) return plasmaModule.recipe;
        if ((PREFIX_VALUE + "active").equals(name)) return "" + (didProcess ? 1 : 0);
        if ((PREFIX_VALUE + "booster").equals(name)) return "" + booster;
        if ((PREFIX_VALUE + "plasma").equals(name)) return "" + plasmaEnergy;
        return null;
    }

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return maxPower; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{inputTank}; }
    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[]{inputTank}; }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) return true;
        if (slot == SLOT_BLUEPRINT) return stack.getItem() instanceof ItemBlueprints;
        if (plasmaModule.isItemValid(slot, stack)) return true;
        if (slot == SLOT_BOOSTER) for (Booster booster : BOOSTERS) if (booster.ingredient.matchesRecipe(stack, true)) return true;
        return false;
    }

    @Override public int[] getSlotsForFace(net.minecraft.core.Direction side) { return IO_SLOTS; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, net.minecraft.core.Direction side) {
        return slot >= SLOT_BOOSTER && slot < SLOT_OUTPUT && isItemValidForSlot(slot, stack);
    }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, net.minecraft.core.Direction side) {
        return slot == SLOT_OUTPUT || plasmaModule.isSlotClogged(slot);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("index") && data.contains("selection") && Tags.getIntOr(data, "index", 0) == 0) {
            plasmaModule.setRecipe(Tags.getStringOr(data, "selection", "null"), false);
            plasmaModule.progress = 0D;
            setChanged();
        }
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.containerMenu instanceof com.hbm.inventory.container.ContainerMachinePlasmaForge menu
                && menu.getForge() == this && menu.stillValid(player);
    }

    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ContainerMachinePlasmaForge(id, inventory, this);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        inputTank.serialize(buf);
        buf.writeFloat(plasmaRed); buf.writeFloat(plasmaGreen); buf.writeFloat(plasmaBlue);
        buf.writeLong(plasmaEnergySync); buf.writeLong(power); buf.writeLong(maxPower);
        buf.writeBoolean(didProcess); buf.writeBoolean(connected); buf.writeInt(booster); buf.writeInt(maxBooster);
        plasmaModule.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        inputTank.deserialize(buf);
        plasmaRed = buf.readFloat(); plasmaGreen = buf.readFloat(); plasmaBlue = buf.readFloat();
        plasmaEnergySync = buf.readLong(); power = buf.readLong(); maxPower = buf.readLong();
        didProcess = buf.readBoolean(); connected = buf.readBoolean(); booster = buf.readInt(); maxBooster = buf.readInt();
        plasmaModule.deserialize(buf);
    }

    @Override
    protected void saveData(DataOut out) {
        super.saveData(out);
        inputTank.writeToNBT(out, "input");
        out.putLong("power", power); out.putLong("maxPower", maxPower);
        out.putInt("booster", booster); out.putInt("maxBooster", maxBooster);
        plasmaModule.save(out);
    }

    @Override
    protected void loadData(DataIn in) {
        super.loadData(in);
        inputTank.readFromNBT(in, "input");
        power = in.getLongOr("power", 0L); maxPower = in.getLongOr("maxPower", INITIAL_MAX_POWER);
        booster = in.getIntOr("booster", 0); maxBooster = in.getIntOr("maxBooster", 0);
        plasmaModule.load(in);
    }

    @Override
    public void setRemoved() {
        FusionNodes.destroy(level, null, receiverNode);
        FusionNodes.destroy(level, null, providerNode);
        super.setRemoved();
    }

    private record Booster(com.hbm.inventory.RecipesCommon.AStack ingredient, int ticks) {}

    public static class ForgeArm {
        private final boolean striker;
        public final double[] angles;
        public final double[] prevAngles;
        private final double[] targetAngles;
        private final double[] speed;
        private int delay;
        private ArmState state = ArmState.RETIRE;
        private static final Random rand = new Random();

        private ForgeArm(boolean striker) {
            this.striker = striker;
            int count = striker ? 6 : 4;
            angles = new double[count]; prevAngles = new double[count]; targetAngles = new double[count]; speed = new double[count];
            for (int i = 0; i < speed.length; i++) {
                if (i < 3 || i == 4) speed[i] = 15D;
                if (i > 4) speed[i] = 0.5D;
            }
        }

        private void updateArm(boolean active) {
            System.arraycopy(angles, 0, prevAngles, 0, angles.length);
            if (!active) state = ArmState.RETIRE;
            if (state == ArmState.RETIRE) delay = 0;
            if (delay-- > 0) return;
            switch (state) {
                case RETIRE -> { java.util.Arrays.fill(targetAngles, 0D); if (move()) { delay = 10; state = ArmState.REPOSITION; choose(); } }
                case REPOSITION -> { if (move()) { delay = striker ? 5 : 20; if (striker) { state = ArmState.EXTEND1; targetAngles[4] = 0.5D; } else choose(); } }
                case EXTEND1 -> { if (move()) { state = ArmState.RETRACT1; targetAngles[4] = 0D; } }
                case RETRACT1 -> { if (move()) { state = ArmState.EXTEND2; targetAngles[5] = 0.5D; } }
                case EXTEND2 -> { if (move()) { state = ArmState.RETRACT2; targetAngles[5] = 0D; } }
                case RETRACT2 -> {
                    if (move()) {
                        if (rand.nextInt(3) == 0) {
                            delay = 10;
                            state = ArmState.REPOSITION;
                            choose();
                        } else {
                            delay = 5;
                            state = ArmState.EXTEND1;
                            targetAngles[4] = 0.5D;
                        }
                    }
                }
            }
        }

        private boolean move() {
            boolean moving = false;
            for (int i = 0; i < angles.length; i++) {
                double delta = targetAngles[i] - angles[i];
                if (delta == 0D) continue;
                moving = true;
                angles[i] = Math.abs(delta) <= speed[i] ? targetAngles[i] : angles[i] + Math.copySign(speed[i], delta);
            }
            return !moving;
        }

        private void choose() {
            double[][] positions = striker
                    ? new double[][]{{20, -30, -20, 30, 0, 0}, {45, -80, 15, 30, 0, 0}, {30, -45, -10, 30, 0, 0}, {15, -20, -30, 30, 0, 0}, {0, 10, -55, 30, 0, 0}}
                    : new double[][]{{10, 45, -120, 0}, {20, 45, -140, 0}, {0, 30, -80, 0}, {0, 40, -100, 0}, {30, 50, -160, 0}};
            double[] position = positions[rand.nextInt(positions.length)];
            System.arraycopy(position, 0, targetAngles, 0, targetAngles.length);
        }

        public double[] getPositions(float partialTicks) {
            double[] result = new double[angles.length];
            for (int i = 0; i < result.length; i++) result[i] = prevAngles[i] + (angles[i] - prevAngles[i]) * partialTicks;
            return result;
        }
    }

    private enum ArmState { REPOSITION, EXTEND1, RETRACT1, EXTEND2, RETRACT2, RETIRE }
}
