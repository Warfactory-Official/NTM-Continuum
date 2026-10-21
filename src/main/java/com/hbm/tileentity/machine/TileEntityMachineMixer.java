package com.hbm.tileentity.machine;

import com.hbm.util.SavedIds;
import dev.golem.core.data.Tags;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.inventory.container.ContainerMixer;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.MixerRecipes;
import com.hbm.inventory.recipes.MixerRecipes.MixerRecipe;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.lib.Library;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import java.util.ArrayList;
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

import java.util.HashMap;
import java.util.List;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.tileentity.TilePortShapes;

/** Mixer machine, ported from the 1.12.2 output-fluid-selected implementation. */
public class TileEntityMachineMixer extends TileEntityMachineBase implements MenuProvider,
        IEnergyReceiverMK2, IFluidStandardTransceiverMK2, IControlReceiver, WorldlyContainer, IUpgradeInfoProvider {

    public static final long MAX_POWER = 10_000;
    private static final int[] IO_SLOTS = {1};

    public long power;
    public int progress;
    public int processTime;
    /**
     * The selected row of the output fluid by its name ({@link MixerRecipe#name}), empty for that fluid's first row.
     * 1.7 kept an index ({@code recipeIndex}); a name survives a datapack reload that changes the table, and is what is
     * synced and saved.
     */
    public String recipeName = "";
    public float rotation;
    public float prevRotation;
    public boolean wasOn;
    public int consumption = 50;

    /** Slots 0/1/2 are battery, solid ingredient, and output-fluid identifier. */
    public final FluidTankNTM[] tanks = {
            new FluidTankNTM(Fluids.NONE, 16_000),
            new FluidTankNTM(Fluids.NONE, 16_000),
            new FluidTankNTM(Fluids.NONE, 24_000)
    };
    public final UpgradeManagerNT upgradeManager = new UpgradeManagerNT(this);

    public TileEntityMachineMixer(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_MIXER.get(), pos, state, 5);
        MixerRecipes.INSTANCE.registerRecipes();
    }

    @Override
    public String getDefaultName() {
        return "container.machineMixer";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineMixer te) {
        te.setupAllPorts(te.getPorts());
        te.updatePortPIFIFO();

        te.power = Library.chargeTEFromItems(te.inventory, 0, te.power, MAX_POWER);
        te.updateOutputType();

        te.upgradeManager.checkSlots(te.inventory, 3, 4);
        int speedLevel = te.upgradeManager.getLevel(UpgradeType.SPEED);
        int powerLevel = te.upgradeManager.getLevel(UpgradeType.POWER);
        int overdriveLevel = te.upgradeManager.getLevel(UpgradeType.OVERDRIVE);

        te.consumption = 50;
        te.consumption += speedLevel * 150;
        te.consumption -= (int) (te.consumption * powerLevel * 0.25D);
        te.consumption *= overdriveLevel * 3 + 1;

        // The legacy machine only exposed five connection anchors around the core.

        te.wasOn = te.canProcess();
        if (te.wasOn) {
            te.progress++;
            te.power -= te.consumption;

            te.processTime -= te.processTime * speedLevel / 4;
            te.processTime /= overdriveLevel + 1;
            if (te.processTime <= 0) te.processTime = 1;

            if (te.progress >= te.processTime) {
                te.process();
                te.progress = 0;
            }
        } else {
            te.progress = 0;
        }

        te.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachineMixer te) {
        te.prevRotation = te.rotation;
        if (te.wasOn) te.rotation += 20F;
        if (te.rotation >= 360F) {
            te.rotation -= 360F;
            te.prevRotation -= 360F;
        }
    }

    /** 1.7 keeps {@code recipeIndex} across a new output fluid and wraps it onto that fluid's rows. */
    private void updateOutputType() {
        int index = recipeIndex();
        if (!tanks[2].setType(inventory, 2, 2)) return;
        MixerRecipe[] recipes = MixerRecipes.getOutput(tanks[2].getTankType());
        recipeName = recipes == null || recipes.length == 0 ? "" : recipes[index % recipes.length].name;
        setChanged();
    }

    public boolean canProcess() {
        MixerRecipe recipe = selectedRecipe();
        if (recipe == null) {
            return false;
        }

        tanks[0].setTankType(recipe.input1 != null ? recipe.input1.type : Fluids.NONE);
        tanks[1].setTankType(recipe.input2 != null ? recipe.input2.type : Fluids.NONE);

        if (recipe.input1 != null && tanks[0].getFill() < recipe.input1.fill) return false;
        if (recipe.input2 != null && tanks[1].getFill() < recipe.input2.fill) return false;
        if (power < consumption) return false;
        if (recipe.output + tanks[2].getFill() > tanks[2].getMaxFill()) return false;
        if (recipe.solidInput != null && !recipe.solidInput.matchesRecipe(inventory.getStackInSlot(1), false)) return false;

        processTime = recipe.processTime;
        return true;
    }

    private void process() {
        MixerRecipe recipe = selectedRecipe();
        if (recipe == null) return;
        if (recipe.input1 != null) tanks[0].setFill(tanks[0].getFill() - recipe.input1.fill);
        if (recipe.input2 != null) tanks[1].setFill(tanks[1].getFill() - recipe.input2.fill);
        if (recipe.solidInput != null) inventory.extractItem(1, recipe.solidInput.stacksize, false);
        tanks[2].setFill(tanks[2].getFill() + recipe.output);
        setChanged();
    }

    public int getConsumption() { return consumption; }

    public int getProgressScaled(int scale) {
        return processTime <= 0 ? 0 : progress * scale / processTime;
    }

    public long getPowerScaled(long scale) {
        return power * scale / MAX_POWER;
    }

    protected PortDef[] cachedPorts;
    public PortDef[] getPorts() { if (cachedPorts == null) cachedPorts = TilePortShapes.mixer(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()); return cachedPorts; }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case 0 -> Library.isBattery(stack);
            case 1 -> {
                MixerRecipe recipe = selectedRecipe();
                yield recipe != null && recipe.solidInput != null && recipe.solidInput.matchesRecipe(stack, true);
            }
            case 2 -> stack.getItem() instanceof IItemFluidId;
            case 3, 4 -> Library.isMachineUpgrade(stack);
            default -> false;
        };
    }

    @Override
    protected int[] getUpgradeSlotRange() {
        return new int[]{3, 4};
    }

    @Override public int[] getSlotsForFace(Direction side) { return IO_SLOTS; }
    @Override public int[] getAccessibleSlotsFromSide(int side) { return IO_SLOTS; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == 1 && isItemValidForSlot(slot, stack);
    }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == 1; }

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return MAX_POWER; }
    @Override public long getReceiverSpeed() { return MAX_POWER; }

    @Override public FluidTankNTM[] getAllTanks() { return tanks; }
    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{tanks[2]}; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{tanks[0], tanks[1]}; }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("toggle")) {
            // 1.7: the next row of the output fluid, wrapping
            MixerRecipe[] recipes = MixerRecipes.getOutput(tanks[2].getTankType());
            recipeName = recipes == null || recipes.length == 0
                    ? "" : recipes[(recipeIndex() + 1) % recipes.length].name;
            progress = 0;
            setChanged();
        }
    }

    private static List<String> signatures(FluidType output) {
        List<String> keys = new ArrayList<>();
        MixerRecipe[] recipes = MixerRecipes.getOutput(output);
        if (recipes == null) return keys;
        for (MixerRecipe recipe : recipes) {
            keys.add((recipe.input1 == null ? "-" : recipe.input1.type.getRegistryName()) + "+"
                    + (recipe.input2 == null ? "-" : recipe.input2.type.getRegistryName()) + "+" + (recipe.solidInput != null));
        }
        return keys;
    }

    /** The selected row's position among the output fluid's rows (the GUI's "n / m"); 0 when the name is unknown. */
    public int recipeIndex() {
        return Math.max(0, MixerRecipes.indexOf(tanks[2].getTankType(), recipeName));
    }

    /**
     * The selected row, normalising the selection: a name the output fluid has no row for (none chosen yet, or a reload
     * removed it) falls back to the first row, as 1.7's index did when it was out of range.
     */
    public MixerRecipe selectedRecipe() {
        MixerRecipe[] recipes = MixerRecipes.getOutput(tanks[2].getTankType());
        if (recipes == null || recipes.length == 0) {
            if (!recipeName.isEmpty()) {
                recipeName = "";
                setChanged();
            }
            return null;
        }
        MixerRecipe recipe = recipes[recipeIndex()];
        if (!recipe.name.equals(recipeName)) {
            recipeName = recipe.name;
            setChanged();
        }
        return recipe;
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.containerMenu instanceof ContainerMixer menu
                && menu.getMixer() == this && menu.stillValid(player);
    }

    @Override public Component getDisplayName() { return Component.translatable("block.hbm.machine_mixer"); }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMixer(containerId, playerInventory, this);
    }

    @Override
    public boolean canProvideInfo(UpgradeType type, int level, boolean extendedInfo) {
        return type == UpgradeType.SPEED || type == UpgradeType.POWER || type == UpgradeType.OVERDRIVE;
    }

    @Override
    public void provideInfo(UpgradeType type, int level, List<Component> info, boolean extendedInfo) {
        info.add(IUpgradeInfoProvider.getStandardLabel(ModMachineBlocks.machine_mixer.get()));
        if (type == UpgradeType.SPEED) {
            info.add(Component.translatable(KEY_DELAY, "-" + level * 25 + "%").withStyle(ChatFormatting.GREEN));
            info.add(Component.translatable(KEY_CONSUMPTION, "+" + level * 300 + "%").withStyle(ChatFormatting.RED));
        } else if (type == UpgradeType.POWER) {
            info.add(Component.translatable(KEY_CONSUMPTION, "-" + level * 25 + "%").withStyle(ChatFormatting.GREEN));
        } else if (type == UpgradeType.OVERDRIVE) {
            info.add(Component.literal("YES").withStyle(IUpgradeInfoProvider.getBlink()
                    ? ChatFormatting.RED : ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    public HashMap<UpgradeType, Integer> getValidUpgrades() {
        HashMap<UpgradeType, Integer> upgrades = new HashMap<>();
        upgrades.put(UpgradeType.SPEED, 3);
        upgrades.put(UpgradeType.POWER, 3);
        upgrades.put(UpgradeType.OVERDRIVE, 6);
        return upgrades;
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeInt(processTime);
        buf.writeInt(progress);
        buf.writeUtf(recipeName);
        buf.writeBoolean(wasOn);
        for (FluidTankNTM tank : tanks) tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        processTime = buf.readInt();
        progress = buf.readInt();
        recipeName = buf.readUtf();
        wasOn = buf.readBoolean();
        for (FluidTankNTM tank : tanks) tank.deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("power", power);
        output.putInt("progress", progress);
        output.putInt("processTime", processTime);
        // the chosen row by its name, never its index
        output.putString("recipe", recipeName);
        for (int i = 0; i < tanks.length; i++) tanks[i].writeToNBT(output, "tank" + i);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        power = input.getLongOr("power", 0L);
        progress = input.getIntOr("progress", 0);
        processTime = input.getIntOr("processTime", 0);
        for (int i = 0; i < tanks.length; i++) tanks[i].readFromNBT(input, "tank" + i);
        String saved = input.getStringOr("recipe", null);
        if (saved == null) {
            if (input.getIntOr("recipe", Integer.MIN_VALUE) != Integer.MIN_VALUE) SavedIds.warnNumeric("recipe index", "recipe");
            saved = "";
        }
        if (saved.contains("#") && MixerRecipes.indexOf(tanks[2].getTankType(), saved) < 0) {
            // a save from before the recipes were data names the row by its inputs and occurrence ("hbm:water+-+true#0")
            int index = SavedIds.fromListKey(signatures(tanks[2].getTankType()), saved);
            MixerRecipe[] recipes = MixerRecipes.getOutput(tanks[2].getTankType());
            saved = index < 0 || recipes == null ? "" : recipes[index].name;
        }
        recipeName = saved;
    }
}
