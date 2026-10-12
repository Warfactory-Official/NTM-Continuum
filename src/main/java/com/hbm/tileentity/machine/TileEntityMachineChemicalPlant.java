package com.hbm.tileentity.machine;

import dev.golem.core.data.Tags;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.client.sound.ChemPlantSoundHandler;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.inventory.container.ContainerMachineChemicalPlant;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.tileentity.IUpgradeInfoProvider;
import net.minecraft.ChatFormatting;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.items.machine.ItemFluidBucket;
import com.hbm.lib.DirPos;
import com.hbm.lib.Library;
import com.hbm.modules.machine.ModuleMachineChemplant;
import com.hbm.inventory.recipes.ChemicalPlantRecipes;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;

/**
 * Chemical Plant — the first functional <b>fluid</b> machine, built on the assembly machine's recipe
 * framework (selector + control packet) plus the fluid I/O added to {@link ModuleMachineChemplant}/
 * {@link com.hbm.modules.machine.ModuleMachineBase}. 22 slots: battery, blueprint, 2 upgrades, 3 item-in,
 * 3 item-out, 3 fluid-in (bucket fill → empty out) and 3 fluid-out (empty bucket in → full out). It has 3
 * input + 3 output tanks; the selected recipe {@link com.hbm.modules.machine.ModuleMachineBase#setupTanks
 * conforms} them to its fluids, then the {@link #serverTick} feeds the input tanks from the fluid network
 * (and from buckets) and pushes the output tanks back out.
 *
 * <p>As 1.7.10: a battery in slot 0 charges the machine and the buffer is {@code max(power, recipe.power ×
 * 100, 100,000)}. Container I/O covers HBM/vanilla buckets and {@code IFillableItem} tanks. Upgrades
 * (slots 2–3: SPEED/POWER/OVERDRIVE) use the 1.7 formulas.
 */
public class TileEntityMachineChemicalPlant extends TileEntityMachineBase
        implements MenuProvider, IEnergyReceiverMK2, IFluidStandardTransceiverMK2, IControlReceiver, WorldlyContainer, IUpgradeInfoProvider, IRORValueProvider, IRORInteractive {

    /** Slots a hopper/pipe may touch: the 3 item inputs + the 3 item outputs. */
    private static final int[] IO_SLOTS = {4, 5, 6, 7, 8, 9};

    public long power;
    public long maxPower = 100_000;
    public boolean didProcess = false;

    public final FluidTankNTM[] inputTanks = new FluidTankNTM[3];
    public final FluidTankNTM[] outputTanks = new FluidTankNTM[3];

    // --- client-only animation state (not saved; advanced while didProcess, like the 1.12.2 anim) ---
    public int anim;
    public int prevAnim;

    public final ModuleMachineChemplant chemplantModule;
    public final UpgradeManagerNT upgradeManager = new UpgradeManagerNT(this);

    public TileEntityMachineChemicalPlant(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_CHEMICAL_PLANT.get(), pos, state, 22);
        ChemicalPlantRecipes.INSTANCE.registerRecipes(); // idempotent; registries are flushed by now
        for (int i = 0; i < 3; i++) {
            inputTanks[i] = new FluidTankNTM(Fluids.NONE, 24_000);
            outputTanks[i] = new FluidTankNTM(Fluids.NONE, 24_000);
        }
        this.chemplantModule = new ModuleMachineChemplant(0, this, this.inventory)
                .itemInput(4, 5, 6).itemOutput(7, 8, 9)
                .fluidInput(inputTanks[0], inputTanks[1], inputTanks[2])
                .fluidOutput(outputTanks[0], outputTanks[1], outputTanks[2]);
    }

    @Override
    public String getDefaultName() {
        return "container.machine_chemical_plant";
    }

    // --- server: power + fluids + processing ---

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineChemicalPlant te) {
        if (te.maxPower <= 0) te.maxPower = 1_000_000;
        // 1.7: the buffer floats to the selected recipe's power x100, never below the stored power or 100k,
        // and a battery in slot 0 charges the machine.
        com.hbm.inventory.recipes.loader.GenericRecipe recipe = te.chemplantModule.getRecipe();
        if (recipe != null) te.maxPower = recipe.power * 100;
        te.maxPower = com.hbm.util.BobMathUtil.max(te.power, te.maxPower, 100_000);
        te.power = Library.chargeTEFromItems(te.inventory, 0, te.power, te.maxPower);

        // Fluid network: subscribe input tanks to matching nets, push output tanks out, plus bucket I/O.
        for (DirPos dp : te.getConPos()) {
            te.trySubscribe(level, dp);
            for (FluidTankNTM tank : te.inputTanks) if (tank.getTankType() != com.hbm.inventory.fluid.Fluids.NONE) te.trySubscribe(tank.getTankType(), level, dp);
            for (FluidTankNTM tank : te.outputTanks) if (tank.getFill() > 0) te.tryProvide(tank, level, dp);
        }
        te.fillInputTanks();
        te.unloadOutputTanks();

        // Upgrades (1.12.2 formulas, identical to the assembly machine).
        te.upgradeManager.checkSlots(te.inventory, 2, 3);
        double speed = 1D;
        double pow = 1D;
        speed += Math.min(te.upgradeManager.getLevel(UpgradeType.SPEED), 3) / 3D;
        speed += Math.min(te.upgradeManager.getLevel(UpgradeType.OVERDRIVE), 3);
        pow -= Math.min(te.upgradeManager.getLevel(UpgradeType.POWER), 3) * 0.25D;
        pow += Math.min(te.upgradeManager.getLevel(UpgradeType.SPEED), 3) * 1D;
        pow += Math.min(te.upgradeManager.getLevel(UpgradeType.OVERDRIVE), 3) * 10D / 3D;

        ItemStack blueprint = te.inventory.getStackInSlot(1);
        te.chemplantModule.update(speed, pow, true, blueprint);
        te.didProcess = te.chemplantModule.didProcess;
        // 1.7: every working tick turns a machined meteorite sword in the battery slot into the treated one
        if (te.didProcess && te.inventory.getStackInSlot(0).is(com.hbm.items.ModAbilityToolItems.meteorite_sword_machined.get())) {
            te.inventory.setStackInSlot(0, new ItemStack(com.hbm.items.ModAbilityToolItems.meteorite_sword_treated.get()));
        }

        if (te.chemplantModule.markDirty) te.setChanged();
        te.networkPackNT();
    }

    /** Client ticker: advance the animation counter (gated by the synced {@code didProcess}) + the sound loop. */
    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachineChemicalPlant te) {
        ChemPlantSoundHandler.tickLoop(te);
        te.prevAnim = te.anim;
        if (te.didProcess) te.anim++;
    }

    /** Drains a full container in each fluid-in load slot (10..12) INTO that input tank, emptied container
     * → out slot (13..15). Handles HBM/vanilla buckets and the universal tank/barrel ({@link IFillableItem}). */
    private void fillInputTanks() {
        for (int i = 0; i < 3; i++) {
            ItemStack in = inventory.getStackInSlot(10 + i);
            if (in.isEmpty()) continue;
            FluidTankNTM tank = inputTanks[i];

            // 1.7 FluidLoaderInfinite.emptyItem: an infinite item feeds the tank from its slot and is never moved or consumed
            if (com.hbm.tileentity.machine.MachineFluidContainerIO.loadInfinite(tank, in)) {
                setChanged();
                continue;
            }
            if (in.getItem() instanceof IFillableItem fillable) {
                int held = fillable.getFill(in);
                FluidType heldType = fillable.getFirstFluidType(in);
                if (held <= 0 || heldType == Fluids.NONE) continue;
                int addable = tank.fill(heldType, held, false);
                if (addable <= 0) continue; // tank full or wrong type
                ItemStack work = single(in);
                int taken = fillable.tryEmpty(heldType, addable, work);
                if (taken <= 0 || !fits(13 + i, work)) continue;
                tank.fill(heldType, taken, true);
                moveResult(10 + i, 13 + i, work);
            } else {
                FluidType fluid = bucketFluid(in);
                if (fluid == Fluids.NONE) continue;
                if (tank.fill(fluid, ItemFluidBucket.VOLUME, false) < ItemFluidBucket.VOLUME) continue;
                ItemStack empty = new ItemStack(Items.BUCKET);
                if (!fits(13 + i, empty)) continue;
                tank.fill(fluid, ItemFluidBucket.VOLUME, true);
                moveResult(10 + i, 13 + i, empty);
            }
        }
    }

    /** Fills an empty container in each fluid-out load slot (16..18) FROM that output tank, filled container
     * → out slot (19..21). Handles the universal tank/barrel ({@link IFillableItem}) and empty buckets. */
    private void unloadOutputTanks() {
        for (int i = 0; i < 3; i++) {
            FluidTankNTM tank = outputTanks[i];
            FluidType fluid = tank.getTankType();
            if (fluid == Fluids.NONE || tank.getFill() <= 0) continue;
            ItemStack in = inventory.getStackInSlot(16 + i);
            if (in.isEmpty()) continue;

            if (in.getItem() instanceof IFillableItem fillable) {
                if (!fillable.acceptsFluid(fluid, in)) continue;
                ItemStack work = single(in);
                int remainder = fillable.tryFill(fluid, tank.getFill(), work);
                int moved = tank.getFill() - remainder;
                if (moved <= 0 || !fits(19 + i, work)) continue;
                tank.setFill(tank.getFill() - moved);
                moveResult(16 + i, 19 + i, work);
            } else if (in.is(Items.BUCKET) && tank.getFill() >= ItemFluidBucket.VOLUME) {
                ItemStack full = fluid == Fluids.WATER ? new ItemStack(Items.WATER_BUCKET)
                        : fluid == Fluids.LAVA ? new ItemStack(Items.LAVA_BUCKET)
                        : ItemFluidBucket.make(fluid);
                if (!fits(19 + i, full)) continue;
                tank.setFill(tank.getFill() - ItemFluidBucket.VOLUME);
                moveResult(16 + i, 19 + i, full);
            }
        }
    }

    private static FluidType bucketFluid(ItemStack stack) {
        if (stack.getItem() instanceof ItemFluidBucket) return ItemFluidBucket.getFluid(stack);
        if (stack.is(Items.WATER_BUCKET)) return Fluids.WATER;
        if (stack.is(Items.LAVA_BUCKET)) return Fluids.LAVA;
        return Fluids.NONE;
    }

    /** Any fluid container the load slots accept: a tank/barrel, an HBM fluid bucket, or a vanilla bucket. */
    private static boolean isFluidContainer(ItemStack stack) {
        return stack.getItem() instanceof IFillableItem
                || stack.getItem() instanceof ItemFluidBucket
                || stack.is(Items.BUCKET) || stack.is(Items.WATER_BUCKET) || stack.is(Items.LAVA_BUCKET);
    }

    private static ItemStack single(ItemStack stack) {
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }

    private boolean fits(int outSlot, ItemStack stack) {
        return inventory.insertItem(outSlot, stack.copy(), true).isEmpty();
    }

    private void moveResult(int inSlot, int outSlot, ItemStack result) {
        inventory.extractItem(inSlot, 1, false);
        inventory.insertItem(outSlot, result, false);
        setChanged();
    }

    /** Power connection points around the 3×3 footprint (one block out from each edge cell). Same as assembly. */
    public DirPos[] getConPos() {
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        return new DirPos[]{
                new DirPos(x + 2, y, z - 1, Library.POS_X), new DirPos(x + 2, y, z, Library.POS_X), new DirPos(x + 2, y, z + 1, Library.POS_X),
                new DirPos(x - 2, y, z - 1, Library.NEG_X), new DirPos(x - 2, y, z, Library.NEG_X), new DirPos(x - 2, y, z + 1, Library.NEG_X),
                new DirPos(x - 1, y, z + 2, Library.POS_Z), new DirPos(x, y, z + 2, Library.POS_Z), new DirPos(x + 1, y, z + 2, Library.POS_Z),
                new DirPos(x - 1, y, z - 2, Library.NEG_Z), new DirPos(x, y, z - 2, Library.NEG_Z), new DirPos(x + 1, y, z - 2, Library.NEG_Z),
        };
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return true;                                       // battery slot (1.7 accepts anything)
        if (slot == 1) return stack.getItem() instanceof ItemBlueprints;  // template
        if (slot >= 2 && slot <= 3) return Library.isMachineUpgrade(stack); // upgrades (any type, like SlotUpgrade)
        if (slot >= 7 && slot <= 9) return false;                         // item outputs (module writes via setStackInSlot)
        if (slot >= 10 && slot <= 12) return isFluidContainer(stack);     // fluid-in load (bucket / tank / barrel)
        if (slot >= 13 && slot <= 15) return true;                        // emptied-container out (machine-insert)
        if (slot >= 16 && slot <= 18) return isFluidContainer(stack);     // fluid-out load (empty bucket / tank / barrel)
        if (slot >= 19 && slot <= 21) return true;                        // filled-container out (machine-insert)
        return chemplantModule.isItemValidForPool(slot, stack, inventory.getStackInSlot(1)); // item inputs 4..6
    }

    // --- WorldlyContainer: hoppers/pipes feed item inputs, pull item outputs ---

    @Override public int[] getSlotsForFace(Direction side) { return IO_SLOTS; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return slot >= 4 && slot <= 6 && isItemValidForSlot(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot >= 7 && slot <= 9; }

    // --- IUpgradeInfoProvider (identical to the assembly machine's 1.12.2 provideInfo) ---

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
        info.add(IUpgradeInfoProvider.getStandardLabel(ModMachineBlocks.machine_chemical_plant.get()));
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

    // --- IFluidStandardTransceiverMK2 ---

    @Override public FluidTankNTM[] getReceivingTanks() { return inputTanks; }
    @Override public FluidTankNTM[] getSendingTanks() { return outputTanks; }
    @Override public FluidTankNTM[] getAllTanks() {
        return new FluidTankNTM[]{inputTanks[0], inputTanks[1], inputTanks[2], outputTanks[0], outputTanks[1], outputTanks[2]};
    }

    // --- IControlReceiver: the in-GUI recipe selector sets the active recipe (index 0) ---

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("index") && data.contains("selection")) {
            if (Tags.getIntOr(data, "index", 0) == 0) {
                this.chemplantModule.setRecipe(Tags.getStringOr(data, "selection", "null"), false);
                this.chemplantModule.progress = 0D;
                setChanged();
            }
        }
    }

    // --- MenuProvider ---

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.hbm.machine_chemical_plant");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMachineChemicalPlant(containerId, playerInventory, this);
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[]{PREFIX_VALUE + "progress", PREFIX_VALUE + "recipe", PREFIX_VALUE + "active",
                PREFIX_FUNCTION + "setrecipe" + NAME_SEPARATOR + "name"};
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.containerMenu instanceof com.hbm.inventory.container.ContainerMachineChemicalPlant menu
                && menu.getChemplant() == this && menu.stillValid(player);
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "progress").equals(name)) return "" + (int) Math.round(chemplantModule.progress * 100);
        if ((PREFIX_VALUE + "recipe").equals(name)) return chemplantModule.recipe;
        if ((PREFIX_VALUE + "active").equals(name)) return "" + (didProcess ? 1 : 0);
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setrecipe").equals(name) && params.length == 1) {
            chemplantModule.setRecipe(params[0], true);
            setChanged();
        }
        return null;
    }

    // --- sync (BufPacket) + persistence ---

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        for (FluidTankNTM tank : inputTanks) tank.serialize(buf);
        for (FluidTankNTM tank : outputTanks) tank.serialize(buf);
        buf.writeLong(power);
        buf.writeLong(maxPower);
        buf.writeBoolean(didProcess);
        chemplantModule.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        for (FluidTankNTM tank : inputTanks) tank.deserialize(buf);
        for (FluidTankNTM tank : outputTanks) tank.deserialize(buf);
        power = buf.readLong();
        maxPower = buf.readLong();
        didProcess = buf.readBoolean();
        chemplantModule.deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        for (int i = 0; i < 3; i++) {
            inputTanks[i].writeToNBT(output, "i" + i);
            outputTanks[i].writeToNBT(output, "o" + i);
        }
        output.putLong("power", power);
        output.putLong("maxPower", maxPower);
        chemplantModule.save(output);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        for (int i = 0; i < 3; i++) {
            inputTanks[i].readFromNBT(input, "i" + i);
            outputTanks[i].readFromNBT(input, "o" + i);
        }
        power = input.getLongOr("power", 0L);
        maxPower = input.getLongOr("maxPower", 100_000L);
        chemplantModule.load(input);
    }
}
