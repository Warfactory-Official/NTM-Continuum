package com.hbm.tileentity.machine;

import dev.golem.core.data.Tags;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.inventory.container.ContainerMachinePUREX;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.PUREXRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.lib.DirPos;
import com.hbm.lib.Library;
import com.hbm.modules.machine.ModuleMachinePUREX;
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

import java.util.HashMap;
import java.util.List;

/**
 * PUREX reprocessing plant — a 1:1 port of the 1.12.2 {@code TileEntityMachinePUREX}. 13 slots (0 battery,
 * 1 blueprint, 2–3 upgrades, 4–6 item inputs, 7–12 item outputs), three 24,000 mB reagent tanks and one
 * 24,000 mB product tank whose types are set by the selected recipe, and the original's dynamic power
 * ceiling: {@code maxPower = recipe.power * 100}, floored at the current buffer and at 1,000,000.
 *
 * <p>Reagents arrive through the fluid network only — the original has no canister bays. Automation reaches
 * slots 4–12 through {@link WorldlyContainer}, and an input holding something the current recipe cannot use
 * may be pulled back out ({@code isSlotClogged}) so a wrong item can never deadlock the machine.
 *
 * <p>Hot ZIRNOX waste is rejected: the recipes match on the absence of the hot-state component, so spent
 * fuel must cool in a waste drum first.
 */
public final class TileEntityMachinePUREX extends TileEntityMachineBase
        implements MenuProvider, IEnergyReceiverMK2, IFluidStandardTransceiverMK2, IControlReceiver,
        IUpgradeInfoProvider, WorldlyContainer, IRORValueProvider {

    /** Slots automation may touch: the three item inputs and the six outputs. */
    private static final int[] IO_SLOTS = {4, 5, 6, 7, 8, 9, 10, 11, 12};
    public static final long BASE_MAX_POWER = 1_000_000L;

    public final FluidTankNTM[] inputTanks = {
            new FluidTankNTM(Fluids.NONE, 24_000), new FluidTankNTM(Fluids.NONE, 24_000), new FluidTankNTM(Fluids.NONE, 24_000)};
    public final FluidTankNTM[] outputTanks = {new FluidTankNTM(Fluids.NONE, 24_000)};
    public long power;
    public long maxPower = BASE_MAX_POWER;
    public boolean didProcess;
    /** Whether a block sits 5 above the core — the renderer's upper frame section (polled once a second). */
    public boolean frame;

    // --- client-only animation state (advanced while didProcess, like the 1.12.2 anim) ---
    public int anim;
    public int prevAnim;

    public final ModuleMachinePUREX purexModule;
    public final UpgradeManagerNT upgradeManager = new UpgradeManagerNT(this);
    /** Cached port ring — queried every tick and again by {@code MachinePUREX.canConnect} during meshing. */
    private DirPos[] conPos;

    public TileEntityMachinePUREX(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_PUREX.get(), pos, state, 13);
        PUREXRecipes.INSTANCE.registerRecipes();
        purexModule = new ModuleMachinePUREX(0, this, inventory).itemInput(4).itemOutput(7)
                .fluidInput(inputTanks[0], inputTanks[1], inputTanks[2])
                .fluidOutput(outputTanks[0]);
    }

    @Override public String getDefaultName() { return "container.machinePUREX"; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachinePUREX te) {
        for (DirPos port : te.getConPos()) {
            te.trySubscribe(level, port);
            for (FluidTankNTM tank : te.inputTanks) if (tank.getTankType() != Fluids.NONE) te.trySubscribe(tank.getTankType(), level, port);
            for (FluidTankNTM tank : te.outputTanks) if (tank.getFill() > 0) te.tryProvide(tank, level, port);
        }

        te.updateMaxPower();
        te.power = Library.chargeTEFromItems(te.inventory, 0, te.power, te.maxPower);

        te.upgradeManager.checkSlots(te.inventory, 2, 3);
        double speed = 1D;
        double pow = 1D;
        speed += Math.min(te.upgradeManager.getLevel(UpgradeType.SPEED), 3) / 3D;
        speed += Math.min(te.upgradeManager.getLevel(UpgradeType.OVERDRIVE), 3);
        pow -= Math.min(te.upgradeManager.getLevel(UpgradeType.POWER), 3) * 0.25D;
        pow += Math.min(te.upgradeManager.getLevel(UpgradeType.SPEED), 3) * 1D;
        pow += Math.min(te.upgradeManager.getLevel(UpgradeType.OVERDRIVE), 3) * 10D / 3D;

        te.purexModule.update(speed, pow, true, te.inventory.getStackInSlot(1));
        te.didProcess = te.purexModule.didProcess;
        if (te.purexModule.markDirty) te.setChanged();

        if (level.getGameTime() % 20 == 0) te.frame = !level.getBlockState(pos.above(5)).isAir();
        te.networkPackNT();
    }

    /** Client ticker: advance the animation counter, gated by the synced {@code didProcess}. */
    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachinePUREX te) {
        te.prevAnim = te.anim;
        if (te.didProcess) te.anim++;
    }

    /**
     * The original's dynamic ceiling: 100 ticks' worth of the selected recipe's draw, never below the
     * current buffer (so downgrading a recipe can't break the power bar) nor below 1,000,000.
     */
    private void updateMaxPower() {
        if (maxPower <= 0) maxPower = BASE_MAX_POWER;
        GenericRecipe recipe = purexModule.getRecipe();
        if (recipe != null) maxPower = recipe.power * 100L;
        maxPower = Math.max(power, Math.max(maxPower, BASE_MAX_POWER));
    }

    /** The 20 network ports: one ring of cells just outside the 5×5 footprint, at core height. */
    public DirPos[] getConPos() {
        if (conPos == null) {
            int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
            DirPos[] ports = new DirPos[20];
            int n = 0;
            for (int i = -2; i <= 2; i++) {
                ports[n++] = new DirPos(x + 3, y, z + i, Library.POS_X);
                ports[n++] = new DirPos(x - 3, y, z + i, Library.NEG_X);
                ports[n++] = new DirPos(x + i, y, z + 3, Library.POS_Z);
                ports[n++] = new DirPos(x + i, y, z - 3, Library.NEG_Z);
            }
            conPos = ports;
        }
        return conPos;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return Library.isBattery(stack);
        if (slot == 1) return stack.getItem() instanceof ItemBlueprints;
        if (slot >= 2 && slot <= 3) return Library.isMachineUpgrade(stack);
        if (slot >= 7 && slot <= 12) return true; // outputs — the module writes them via setStackInSlot
        return purexModule.isItemValidForPool(slot, stack, inventory.getStackInSlot(1)); // inputs 4..6
    }

    // --- WorldlyContainer: pipes/hoppers feed the item inputs and pull the outputs ---

    @Override public int[] getSlotsForFace(Direction side) { return IO_SLOTS; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= 4 && slot <= 6 && isItemValidForSlot(slot, stack);
    }
    /** Outputs always, plus a clogged input so a wrong item can be pulled back out (old {@code canExtractItem}). */
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return (slot >= 7 && slot <= 12) || purexModule.isSlotClogged(slot);
    }

    // --- IUpgradeInfoProvider (the 1.12.2 PUREX panel, identical to the chemical plant's) ---

    @Override protected int[] getUpgradeSlotRange() { return new int[]{2, 3}; }

    @Override
    public boolean canProvideInfo(UpgradeType type, int level, boolean extendedInfo) {
        return type == UpgradeType.SPEED || type == UpgradeType.POWER || type == UpgradeType.OVERDRIVE;
    }

    @Override
    public void provideInfo(UpgradeType type, int level, List<Component> info, boolean extendedInfo) {
        info.add(IUpgradeInfoProvider.getStandardLabel(ModMachineBlocks.machine_purex.get()));
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
    public HashMap<UpgradeType, Integer> getValidUpgrades() {
        HashMap<UpgradeType, Integer> upgrades = new HashMap<>();
        upgrades.put(UpgradeType.SPEED, 3);
        upgrades.put(UpgradeType.POWER, 3);
        upgrades.put(UpgradeType.OVERDRIVE, 3);
        return upgrades;
    }

    // --- energy + fluids ---

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return maxPower; }
    @Override public long getReceiverSpeed() { return maxPower; }
    @Override public FluidTankNTM[] getReceivingTanks() { return inputTanks; }
    @Override public FluidTankNTM[] getSendingTanks() { return outputTanks; }
    @Override public FluidTankNTM[] getAllTanks() {
        return new FluidTankNTM[]{inputTanks[0], inputTanks[1], inputTanks[2], outputTanks[0]};
    }

    // --- menu + recipe selection ---

    @Override public Component getDisplayName() { return Component.translatable("block.hbm.machine_purex"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) { return new ContainerMachinePUREX(id, inv, this); }
    @Override public void receiveControl(CompoundTag data) {
        if (Tags.getIntOr(data, "index", -1) == 0) {
            String selection = Tags.getStringOr(data, "selection", "null");
            purexModule.setRecipe(PUREXRecipes.INSTANCE.getRecipe(selection) != null ? selection : "null", false);
            purexModule.progress = 0D;
            setChanged();
        }
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[]{PREFIX_VALUE + "progress", PREFIX_VALUE + "recipe", PREFIX_VALUE + "active"};
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "progress").equals(name)) return "" + (int) Math.round(purexModule.progress * 100);
        if ((PREFIX_VALUE + "recipe").equals(name)) return purexModule.recipe;
        if ((PREFIX_VALUE + "active").equals(name)) return "" + (didProcess ? 1 : 0);
        return null;
    }

    // --- sync + persistence ---

    @Override public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        for (FluidTankNTM tank : inputTanks) tank.serialize(buf);
        outputTanks[0].serialize(buf);
        buf.writeLong(power); buf.writeLong(maxPower); buf.writeBoolean(didProcess); buf.writeBoolean(frame);
        purexModule.serialize(buf);
    }
    @Override public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        for (FluidTankNTM tank : inputTanks) tank.deserialize(buf);
        outputTanks[0].deserialize(buf);
        power = buf.readLong(); maxPower = buf.readLong(); didProcess = buf.readBoolean(); frame = buf.readBoolean();
        purexModule.deserialize(buf);
    }
    @Override protected void saveData(DataOut output) {
        super.saveData(output);
        for (int i = 0; i < inputTanks.length; i++) inputTanks[i].writeToNBT(output, "i" + i);
        outputTanks[0].writeToNBT(output, "o0");
        output.putLong("power", power); output.putLong("maxPower", maxPower);
        purexModule.save(output);
    }
    @Override protected void loadData(DataIn input) {
        super.loadData(input);
        for (int i = 0; i < inputTanks.length; i++) inputTanks[i].readFromNBT(input, "i" + i);
        outputTanks[0].readFromNBT(input, "o0");
        power = input.getLongOr("power", 0L); maxPower = input.getLongOr("maxPower", BASE_MAX_POWER);
        purexModule.load(input);
    }
}
