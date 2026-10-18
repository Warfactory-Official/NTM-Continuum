package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.container.ContainerMachineRockMill;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.RockMillRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.lib.Library;
import com.hbm.module.machine.ModuleMachineRockMill;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.util.BobMathUtil;
import com.hbm.util.TickPhase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** 1.7 {@code TileEntityMachineRockMill}. Slots: 0 battery, 1 blueprint, 2-4 inputs, 5-7 outputs. */
public class TileEntityMachineRockMill extends TileEntityMachineBase
        implements MenuProvider, IEnergyReceiverMK2, IFluidStandardTransceiverMK2, IControlReceiver, WorldlyContainer {

    private static final int[] IO_SLOTS = {2, 3, 4, 5, 6, 7};
    public static final float ACCELERATION = 0.1F;
    public static final float MAX_SPEED = 15F;

    public final FluidTankNTM[] inputTanks = {new FluidTankNTM(Fluids.NONE, 4_000)};
    public final FluidTankNTM[] outputTanks = {new FluidTankNTM(Fluids.NONE, 4_000)};

    public long power;
    public long maxPower = 2_500;
    public boolean didProcess = false;

    public float rotation;
    public float prevRotation;
    public float rotationSpeed = 0F;
    public boolean frame = false;

    public final ModuleMachineRockMill rockMillModule;
    private PortDef[] cachedPorts;

    public TileEntityMachineRockMill(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_ROCKMILL.get(), pos, state, 8);
        RockMillRecipes.INSTANCE.registerRecipes();
        this.rockMillModule = new ModuleMachineRockMill(0, this, inventory)
                .itemInput(2).itemOutput(5)
                .fluidInput(inputTanks[0]).fluidOutput(outputTanks[0]);
        this.rockMillModule.owner(this);
    }

    @Override public String getDefaultName() { return "container.machineRockMill"; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineRockMill te) {
        if (te.maxPower <= 0) te.maxPower = 2_500;

        te.setupAllPorts(te.getPorts());
        te.updatePortPIFIFO();

        GenericRecipe recipe = te.rockMillModule.getRecipe();
        if (recipe != null) te.maxPower = recipe.power * 100;
        te.maxPower = BobMathUtil.max(te.power, te.maxPower, 2_500);
        te.power = Library.chargeTEFromItems(te.inventory, 0, te.power, te.maxPower);

        te.rockMillModule.update(1D, 1D, true, te.inventory.getStackInSlot(1));
        te.didProcess = te.rockMillModule.didProcess;
        if (te.rockMillModule.markDirty) te.setChanged();

        if (te.didProcess && TickPhase.every(level, pos, 3)) {
            SoundEvent sound = Blocks.STONE.defaultBlockState().getSoundType().getStepSound();
            if (recipe != null && recipe.getIcon().getItem() instanceof BlockItem item) {
                sound = item.getBlock().defaultBlockState().getSoundType().getStepSound();
            }
            level.playSound(null, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, sound, SoundSource.BLOCKS,
                    te.getVolume(1.0F), 0.75F);
        }

        te.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachineRockMill te) {
        te.prevRotation = te.rotation;
        te.rotationSpeed += ACCELERATION * (te.didProcess ? 1 : -1);
        te.rotationSpeed = Mth.clamp(te.rotationSpeed, 0F, MAX_SPEED);
        te.rotation += te.rotationSpeed;

        if (te.rotation >= 360F) {
            te.prevRotation -= 360F;
            te.rotation -= 360F;
        }

        if (level.getGameTime() % 20 == 0) te.frame = !level.getBlockState(pos.above(3)).isAir();

        if (te.didProcess && level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 35, false) != null) {
            GenericRecipe recipe = te.rockMillModule.getRecipe();
            Block block = Blocks.GRAVEL;
            if (recipe != null && recipe.getIcon().getItem() instanceof BlockItem item) block = item.getBlock();

            double angle = level.getRandom().nextDouble() * Math.PI * 2D;
            double vx = Math.cos(angle);
            double vz = -Math.sin(angle);
            double speed = 0.125D;
            level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, block.defaultBlockState()),
                    pos.getX() + 0.5 + vx * 2.25, pos.getY() + 1.5, pos.getZ() + 0.5 + vz * 2.25,
                    vx * speed, -0.1D, vz * speed);
        }
    }

    /** 1.7 {@code getPorts}: two ports on each side of the 5×5 footprint. */
    public PortDef[] getPorts() {
        if (cachedPorts == null) {
            int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
            cachedPorts = new PortDef[] {
                    PortDef.make(x + 2, y, z + 1, Library.POS_X),
                    PortDef.make(x + 2, y, z - 1, Library.POS_X),
                    PortDef.make(x - 2, y, z + 1, Library.NEG_X),
                    PortDef.make(x - 2, y, z - 1, Library.NEG_X),
                    PortDef.make(x + 1, y, z + 2, Library.POS_Z),
                    PortDef.make(x - 1, y, z + 2, Library.POS_Z),
                    PortDef.make(x + 1, y, z - 2, Library.NEG_Z),
                    PortDef.make(x - 1, y, z - 2, Library.NEG_Z),
            };
        }
        return cachedPorts;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return Library.isBattery(stack);
        if (slot == 1) return stack.getItem() instanceof ItemBlueprints;
        return rockMillModule.isItemValid(slot, stack);
    }

    @Override public int[] getSlotsForFace(Direction side) { return IO_SLOTS; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return slot >= 2 && slot <= 4 && isItemValidForSlot(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return (slot >= 5 && slot <= 7) || rockMillModule.isSlotClogged(slot); }

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return maxPower; }

    @Override public FluidTankNTM[] getReceivingTanks() { return inputTanks; }
    @Override public FluidTankNTM[] getSendingTanks() { return outputTanks; }
    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[] {inputTanks[0], outputTanks[0]}; }

    @Override public Component getDisplayName() { return Component.translatable(getName()); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) { return new ContainerMachineRockMill(id, inv, this); }

    @Override
    public boolean hasPermission(Player player) {
        return stillValid(player);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("index") && data.contains("selection") && Tags.getIntOr(data, "index", -1) == 0) {
            rockMillModule.setRecipe(Tags.getStringOr(data, "selection", "null"), false);
            setChanged();
        }
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        for (FluidTankNTM tank : inputTanks) tank.serialize(buf);
        for (FluidTankNTM tank : outputTanks) tank.serialize(buf);
        buf.writeLong(power);
        buf.writeLong(maxPower);
        buf.writeBoolean(didProcess);
        rockMillModule.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        for (FluidTankNTM tank : inputTanks) tank.deserialize(buf);
        for (FluidTankNTM tank : outputTanks) tank.deserialize(buf);
        power = buf.readLong();
        maxPower = buf.readLong();
        didProcess = buf.readBoolean();
        rockMillModule.deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        inputTanks[0].writeToNBT(output, "i0");
        outputTanks[0].writeToNBT(output, "o0");
        output.putLong("power", power);
        output.putLong("maxPower", maxPower);
        rockMillModule.save(output);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        inputTanks[0].readFromNBT(input, "i0");
        outputTanks[0].readFromNBT(input, "o0");
        power = input.getLongOr("power", 0L);
        maxPower = input.getLongOr("maxPower", 2_500L);
        rockMillModule.load(input);
    }
}
