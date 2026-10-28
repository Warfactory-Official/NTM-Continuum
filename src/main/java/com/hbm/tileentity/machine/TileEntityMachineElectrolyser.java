package com.hbm.tileentity.machine;

import com.hbm.blocks.machine.MachineElectrolyser;
import com.hbm.inventory.material.NTMMaterial;
import dev.golem.core.data.Tags;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.inventory.container.ContainerMachineElectrolyser;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.recipes.ElectrolyserFluidRecipes;
import com.hbm.inventory.recipes.ElectrolyserMetalRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.util.CrucibleUtil;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.ItemStacks;
import java.util.ArrayList;
import java.util.Arrays;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import com.hbm.module.portmanager.ModulePortManFluidAdaptive;
import com.hbm.module.portmanager.ModulePortManPower;
import com.hbm.tileentity.TilePort.PortDef;

/** The dual fluid/metal electrolyser, ported from the 1.12.2 machine. */
public class TileEntityMachineElectrolyser extends TileEntityMachineBase
        implements MenuProvider, IEnergyReceiverMK2, IFluidStandardTransceiverMK2, IControlReceiver,
        WorldlyContainer, IUpgradeInfoProvider {

    public static final long MAX_POWER = 20_000_000L;
    public static final int BASE_USAGE_FLUID = 10_000;
    public static final int BASE_USAGE_METAL = 10_000;
    public static final int BASE_FLUID_DURATION = 100;
    public static final int BASE_METAL_DURATION = 600;
    public static final int MAX_MATERIAL = 16 * 72;
    private static final int[] IO_SLOTS = {11, 12, 13, 14, 15, 16, 17, 18, 19, 20};

    public long power;
    public int usageFluid = BASE_USAGE_FLUID;
    public int usageMetal = BASE_USAGE_METAL;
    public int progressFluid;
    public int progressMetal;
    public int fluidDuration = BASE_FLUID_DURATION;
    public int metalDuration = BASE_METAL_DURATION;
    public Mats.@Nullable MaterialStack leftStack;
    public Mats.@Nullable MaterialStack rightStack;
    public int lastSelectedGUI;

    public final FluidTankNTM[] tanks = {
            new FluidTankNTM(Fluids.WATER, 16_000),
            new FluidTankNTM(Fluids.HYDROGEN, 16_000),
            new FluidTankNTM(Fluids.OXYGEN, 16_000),
            new FluidTankNTM(Fluids.NITRIC_ACID, 16_000)
    };
    public final UpgradeManagerNT upgradeManager = new UpgradeManagerNT(this);

    protected final ModulePortManFluidAdaptive moduleFluidPorts = new ModulePortManFluidAdaptive(this).setInputTanks(getReceivingTanks()).setOutputTanks(getSendingTanks());
    protected final ModulePortManPower modulePowerPorts = new ModulePortManPower(this).onlyInput();

    public TileEntityMachineElectrolyser(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_ELECTROLYSER.get(), pos, state, 21);
        ElectrolyserFluidRecipes.register();
        ElectrolyserMetalRecipes.register();
    }

    @Override
    public String getDefaultName() {
        return "container.machineElectrolyser";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineElectrolyser te) {
        te.moduleFluidPorts.update(te.getPorts());
        te.modulePowerPorts.update(te.getPorts());

        te.power = Library.chargeTEFromItems(te.inventory, 0, te.power, MAX_POWER);
        te.tanks[0].setType(te.inventory, 3, 4);
        MachineFluidContainerIO.load(te.tanks[0], te.inventory, 5, 6);
        MachineFluidContainerIO.unload(te.tanks[1], te.inventory, 7, 8);
        MachineFluidContainerIO.unload(te.tanks[2], te.inventory, 9, 10);

        te.upgradeManager.checkSlots(te.inventory, 1, 2);
        int speedLevel = te.upgradeManager.getLevel(UpgradeType.SPEED);
        int powerLevel = te.upgradeManager.getLevel(UpgradeType.POWER);
        te.usageFluid = BASE_USAGE_FLUID - BASE_USAGE_FLUID * powerLevel / 4 + BASE_USAGE_FLUID * speedLevel;
        te.usageMetal = BASE_USAGE_METAL - BASE_USAGE_METAL * powerLevel / 4 + BASE_USAGE_METAL * speedLevel;
        te.fluidDuration = te.getDurationFluid();
        te.metalDuration = te.getDurationMetal();

        for (int cycle = 0; cycle < te.getCycleCount(); cycle++) {
            if (te.canProcessFluid()) {
                te.progressFluid++;
                te.power -= te.usageFluid;
                if (te.progressFluid >= te.fluidDuration) {
                    te.processFluid();
                    te.progressFluid = 0;
                    te.setChanged();
                }
            }
            if (te.canProcessMetal()) {
                te.progressMetal++;
                te.power -= te.usageMetal;
                if (te.progressMetal >= te.metalDuration) {
                    te.processMetal();
                    te.progressMetal = 0;
                    te.setChanged();
                }
            }
        }

        te.pourMaterial(level, true, true);
        te.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachineElectrolyser te) {
        // The legacy machine's visible animation is entirely represented by its progress bars.
    }

    private boolean canProcessFluid() {
        if (power < usageFluid) return false;
        ElectrolyserFluidRecipes.ElectrolysisRecipe recipe = ElectrolyserFluidRecipes.getRecipe(tanks[0].getTankType());
        if (recipe == null || tanks[0].getFill() < recipe.amount) return false;
        if (recipe.output1.type == tanks[1].getTankType()
                && recipe.output1.fill + tanks[1].getFill() > tanks[1].getMaxFill()) return false;
        if (recipe.output2.type == tanks[2].getTankType()
                && recipe.output2.fill + tanks[2].getFill() > tanks[2].getMaxFill()) return false;
        return canFitByproducts(recipe.byproduct, 11);
    }

    private void processFluid() {
        ElectrolyserFluidRecipes.ElectrolysisRecipe recipe = ElectrolyserFluidRecipes.getRecipe(tanks[0].getTankType());
        if (recipe == null) return;
        tanks[0].setFill(tanks[0].getFill() - recipe.amount);
        tanks[1].setTankType(recipe.output1.type);
        tanks[2].setTankType(recipe.output2.type);
        tanks[1].setFill(tanks[1].getFill() + recipe.output1.fill);
        tanks[2].setFill(tanks[2].getFill() + recipe.output2.fill);
        addByproducts(recipe.byproduct, 11);
    }

    private boolean canProcessMetal() {
        ItemStack input = inventory.getStackInSlot(14);
        if (input.isEmpty() || power < usageMetal || tanks[3].getFill() < 100) return false;
        ElectrolyserMetalRecipes.ElectrolysisMetalRecipe recipe = ElectrolyserMetalRecipes.getRecipe(input);
        if (recipe == null) return false;
        if (!canFitMaterial(leftStack, recipe.output1) || !canFitMaterial(rightStack, recipe.output2)) return false;
        return canFitByproducts(recipe.byproduct, 15);
    }

    private void processMetal() {
        ElectrolyserMetalRecipes.ElectrolysisMetalRecipe recipe = ElectrolyserMetalRecipes.getRecipe(inventory.getStackInSlot(14));
        if (recipe == null) return;
        leftStack = addMaterial(leftStack, recipe.output1);
        rightStack = addMaterial(rightStack, recipe.output2);
        addByproducts(recipe.byproduct, 15);
        tanks[3].setFill(tanks[3].getFill() - 100);
        inventory.extractItem(14, 1, false);
    }

    private void pourMaterial(Level level, boolean left, boolean right) {
        ForgeDirection facing = MachineCore.facing(getBlockState());
        ForgeDirection leftDirection = facing.getOpposite();
        if (left && leftStack != null) leftStack = pour(level, leftStack, leftDirection);
        if (right && rightStack != null) rightStack = pour(level, rightStack, facing);
    }

    private Mats.@Nullable MaterialStack pour(Level level, Mats.MaterialStack stack, ForgeDirection direction) {
        List<Mats.MaterialStack> stacks = new ArrayList<>(List.of(stack));
        CrucibleUtil.pourFullStack(level, worldPosition.getX() + 0.5D + direction.offsetX * 5.875D,
                worldPosition.getY() + 2D, worldPosition.getZ() + 0.5D + direction.offsetZ * 5.875D,
                6D, true, stacks, getPourQuanta(), new Vec3[1]);
        return stacks.isEmpty() ? null : stacks.get(0);
    }

    public int getPourQuanta() {
        int speedLevel = upgradeManager.getLevel(UpgradeType.SPEED);
        return MaterialShapes.NUGGET.quantity * 3 * Math.max(getCycleCount() * speedLevel, 1);
    }

    public int getDurationFluid() {
        ElectrolyserFluidRecipes.ElectrolysisRecipe recipe = ElectrolyserFluidRecipes.getRecipe(tanks[0].getTankType());
        int base = recipe == null ? BASE_FLUID_DURATION : recipe.duration;
        int speed = upgradeManager.getLevel(UpgradeType.SPEED) - Math.min(upgradeManager.getLevel(UpgradeType.POWER), 1);
        return (int) Math.ceil(base * Math.max(1F - 0.25F * speed, 0.2F));
    }

    public int getDurationMetal() {
        ElectrolyserMetalRecipes.ElectrolysisMetalRecipe recipe = ElectrolyserMetalRecipes.getRecipe(inventory.getStackInSlot(14));
        int base = recipe == null ? BASE_METAL_DURATION : recipe.duration;
        int speed = upgradeManager.getLevel(UpgradeType.SPEED) - Math.min(upgradeManager.getLevel(UpgradeType.POWER), 1);
        return (int) Math.ceil(base * Math.max(1F - 0.25F * speed, 0.2F));
    }

    public int getCycleCount() {
        return Math.min(1 + upgradeManager.getLevel(UpgradeType.OVERDRIVE) * 2, 7);
    }

    private boolean canFitMaterial(Mats.@Nullable MaterialStack current, Mats.@Nullable MaterialStack output) {
        if (output == null) return true;
        return current == null || (current.material == output.material && current.amount + output.amount <= MAX_MATERIAL);
    }

    private static Mats.@Nullable MaterialStack addMaterial(Mats.@Nullable MaterialStack current, Mats.@Nullable MaterialStack output) {
        if (output == null) return current;
        if (current == null) return output.copy();
        current.amount += output.amount;
        return current;
    }

    private boolean canFitByproducts(ItemStack[] outputs, int firstSlot) {
        for (int i = 0; i < outputs.length; i++) {
            ItemStack existing = inventory.getStackInSlot(firstSlot + i);
            ItemStack output = outputs[i];
            if (!existing.isEmpty() && (!ItemStacks.sameItemAndData(existing, output)
                    || existing.getCount() + output.getCount() > existing.getMaxStackSize())) return false;
        }
        return true;
    }

    private void addByproducts(ItemStack[] outputs, int firstSlot) {
        for (int i = 0; i < outputs.length; i++) {
            ItemStack existing = inventory.getStackInSlot(firstSlot + i);
            if (existing.isEmpty()) inventory.setStackInSlot(firstSlot + i, outputs[i].copy());
            else existing.grow(outputs[i].getCount());
        }
    }

    protected PortDef[] cachedPorts;

    public PortDef[] getPorts() {
        if (cachedPorts == null) {
            ForgeDirection dir = MachineCore.facing(getBlockState());
            ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
            int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
            List<PortDef> ports = new ArrayList<>(List.of(
                    PortDef.make(x - dir.offsetX * 5 - rot.offsetX * 1, y, z - dir.offsetZ * 5 - rot.offsetZ * 1, dir.getOpposite()),
                    PortDef.make(x - dir.offsetX * 5 + rot.offsetX * 0, y, z - dir.offsetZ * 5 + rot.offsetZ * 0, dir.getOpposite()),
                    PortDef.make(x - dir.offsetX * 5 + rot.offsetX * 1, y, z - dir.offsetZ * 5 + rot.offsetZ * 1, dir.getOpposite()),

                    PortDef.make(x - dir.offsetX * 4 + rot.offsetX * 1, y, z - dir.offsetZ * 4 + rot.offsetZ * 1, rot),
                    PortDef.make(x - dir.offsetX * 2 + rot.offsetX * 1, y, z - dir.offsetZ * 2 + rot.offsetZ * 1, rot),
                    PortDef.make(x + dir.offsetX * 0 + rot.offsetX * 1, y, z + dir.offsetZ * 0 + rot.offsetZ * 1, rot),
                    PortDef.make(x + dir.offsetX * 2 + rot.offsetX * 1, y, z + dir.offsetZ * 2 + rot.offsetZ * 1, rot),
                    PortDef.make(x + dir.offsetX * 4 + rot.offsetX * 1, y, z + dir.offsetZ * 4 + rot.offsetZ * 1, rot),

                    PortDef.make(x - dir.offsetX * 4 - rot.offsetX * 3, y, z - dir.offsetZ * 4 - rot.offsetZ * 3, rot.getOpposite()),
                    PortDef.make(x - dir.offsetX * 2 - rot.offsetX * 3, y, z - dir.offsetZ * 2 - rot.offsetZ * 3, rot.getOpposite()),
                    PortDef.make(x + dir.offsetX * 0 - rot.offsetX * 3, y, z + dir.offsetZ * 0 - rot.offsetZ * 3, rot.getOpposite()),
                    PortDef.make(x + dir.offsetX * 2 - rot.offsetX * 3, y, z + dir.offsetZ * 2 - rot.offsetZ * 3, rot.getOpposite()),
                    PortDef.make(x + dir.offsetX * 4 - rot.offsetX * 3, y, z + dir.offsetZ * 4 - rot.offsetZ * 3, rot.getOpposite()),

                    PortDef.make(x + dir.offsetX * 5 - rot.offsetX * 1, y, z + dir.offsetZ * 5 - rot.offsetZ * 1, dir),
                    PortDef.make(x + dir.offsetX * 5 + rot.offsetX * 0, y, z + dir.offsetZ * 5 + rot.offsetZ * 0, dir),
                    PortDef.make(x + dir.offsetX * 5 + rot.offsetX * 1, y, z + dir.offsetZ * 5 + rot.offsetZ * 1, dir)));
            if (MachineElectrolyser.sidePorts) {
                // hbm-space's end cells beside the hall, which 1.7 does not have
                ports.add(PortDef.make(x - dir.offsetX * 5 + rot.offsetX, y, z - dir.offsetZ * 5 + rot.offsetZ, rot));
                ports.add(PortDef.make(x + dir.offsetX * 5 + rot.offsetX, y, z + dir.offsetZ * 5 + rot.offsetZ, rot));
            }
            cachedPorts = ports.toArray(new PortDef[0]);
        }
        return cachedPorts;
    }

    public DirPos[] getConPos() { return PortDef.connections(getPorts()); }

    @Override
    public void setRemoved() {
        if (level != null) {
            moduleFluidPorts.destroy();
            modulePowerPorts.destroy();
        }
        super.setRemoved();
    }

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return MAX_POWER; }
    @Override public long getReceiverSpeed() { return MAX_POWER; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{tanks[0], tanks[3]}; }
    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{tanks[1], tanks[2]}; }
    @Override public FluidTankNTM[] getAllTanks() { return tanks; }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return Library.isBattery(stack);
        if (slot == 1 || slot == 2) return Library.isMachineUpgrade(stack);
        if (slot == 3 || slot == 4) return stack.getItem() instanceof IItemFluidId;
        // 1.7 ContainerElectrolyser: plain slots, anything a loader might take
        if (slot >= 5 && slot <= 10) return true;
        if (slot == 14) return ElectrolyserMetalRecipes.getRecipe(stack) != null;
        return slot == 11 || slot == 12 || slot == 13 || slot >= 15;
    }

    @Override public int[] getSlotsForFace(Direction side) { return IO_SLOTS; }
    @Override public int[] getAccessibleSlotsFromSide(int side) { return IO_SLOTS; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == 14 && isItemValidForSlot(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot >= 11 && slot != 14; }

    @Override protected int[] getUpgradeSlotRange() { return new int[]{1, 2}; }
    @Override public boolean canProvideInfo(UpgradeType type, int level, boolean extendedInfo) { return type == UpgradeType.SPEED || type == UpgradeType.POWER || type == UpgradeType.OVERDRIVE; }
    @Override public void provideInfo(UpgradeType type, int level, List<Component> info, boolean extendedInfo) {
        info.add(IUpgradeInfoProvider.getStandardLabel(ModMachineBlocks.machine_electrolyser.get()));
        if (type == UpgradeType.SPEED) {
            info.add(Component.translatable(KEY_SPEED, "-" + level * 25 + "%").withStyle(ChatFormatting.GREEN));
            info.add(Component.translatable(KEY_CONSUMPTION, "+" + level * 100 + "%").withStyle(ChatFormatting.RED));
        } else if (type == UpgradeType.POWER) {
            info.add(Component.translatable(KEY_CONSUMPTION, "-" + level * 25 + "%").withStyle(ChatFormatting.GREEN));
        } else if (type == UpgradeType.OVERDRIVE) {
            info.add(Component.literal("YES").withStyle(IUpgradeInfoProvider.getBlink() ? ChatFormatting.RED : ChatFormatting.DARK_GRAY));
        }
    }
    @Override public HashMap<UpgradeType, Integer> getValidUpgrades() {
        HashMap<UpgradeType, Integer> upgrades = new HashMap<>();
        upgrades.put(UpgradeType.SPEED, 3); upgrades.put(UpgradeType.POWER, 3); upgrades.put(UpgradeType.OVERDRIVE, 3);
        return upgrades;
    }

    @Override public boolean hasPermission(Player player) { return stillValid(player); }
    @Override public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMachineElectrolyser(containerId, playerInventory, this, lastSelectedGUI != 0);
    }
    @Override public void receiveControl(CompoundTag data) {
        if (data.contains("sgm")) lastSelectedGUI = 1;
        if (data.contains("sgf")) lastSelectedGUI = 0;
    }

    @Override public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        for (FluidTankNTM tank : tanks) tank.serialize(buf);
        buf.writeLong(power); buf.writeInt(progressFluid); buf.writeInt(progressMetal);
        buf.writeInt(fluidDuration); buf.writeInt(metalDuration);
        buf.writeInt(usageFluid); buf.writeInt(usageMetal); buf.writeInt(lastSelectedGUI);
        writeMaterial(buf, leftStack); writeMaterial(buf, rightStack);
    }
    @Override public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        for (FluidTankNTM tank : tanks) tank.deserialize(buf);
        power = buf.readLong(); progressFluid = buf.readInt(); progressMetal = buf.readInt();
        fluidDuration = buf.readInt(); metalDuration = buf.readInt();
        usageFluid = buf.readInt(); usageMetal = buf.readInt(); lastSelectedGUI = buf.readInt();
        leftStack = readMaterial(buf); rightStack = readMaterial(buf);
    }
    private static void writeMaterial(RegistryFriendlyByteBuf buf, Mats.@Nullable MaterialStack stack) {
        buf.writeBoolean(stack != null); if (stack != null) { buf.writeInt(stack.material.id); buf.writeInt(stack.amount); }
    }
    private static Mats.@Nullable MaterialStack readMaterial(RegistryFriendlyByteBuf buf) {
        return buf.readBoolean() ? new Mats.MaterialStack(Mats.matById.get(buf.readInt()), buf.readInt()) : null;
    }
    @Override protected void saveData(DataOut output) {
        super.saveData(output);
        for (int i = 0; i < tanks.length; i++) tanks[i].writeToNBT(output, "tank" + i);
        output.putLong("power", power); output.putInt("progressFluid", progressFluid); output.putInt("progressMetal", progressMetal);
        output.putInt("lastSelectedGUI", lastSelectedGUI); saveMaterial(output, "left", leftStack); saveMaterial(output, "right", rightStack);
    }
    @Override protected void loadData(DataIn input) {
        super.loadData(input);
        for (int i = 0; i < tanks.length; i++) tanks[i].readFromNBT(input, "tank" + i);
        power = input.getLongOr("power", 0L); progressFluid = input.getIntOr("progressFluid", 0); progressMetal = input.getIntOr("progressMetal", 0);
        lastSelectedGUI = input.getIntOr("lastSelectedGUI", 0); leftStack = loadMaterial(input, "left"); rightStack = loadMaterial(input, "right");
    }
    private static void saveMaterial(DataOut out, String key, Mats.@Nullable MaterialStack stack) {
        if (stack != null) { Mats.write(out, key + "Type", stack.material); out.putInt(key + "Amount", stack.amount); } // by name
    }
    private static Mats.@Nullable MaterialStack loadMaterial(DataIn in, String key) {
        NTMMaterial mat = Mats.read(in, key + "Type");
        return mat == null ? null : new Mats.MaterialStack(mat, in.getIntOr(key + "Amount", 0));
    }
}
