package com.hbm.tileentity.machine;

import dev.golem.core.data.Tags;

import dev.golem.core.multiblock.MultiblockSurface;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.items.machine.ItemReactorSensor;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.blocks.ModNuclearMiscBlocks;
import com.hbm.inventory.container.ContainerReactorControl;
import com.hbm.tileentity.ModNuclearMiscBlockEntities;

/**
 * The reactor remote control block: a sensor in slot 0 links it to a research reactor, and the heat-to-level
 * function (linear / quadratic / logarithmic between the two set points) drives that reactor's target level.
 */
public final class TileEntityReactorControl extends TileEntityMachineBase implements MenuProvider, IControlReceiver {

    public TileEntityReactorResearch reactor;

    public boolean isLinked;

    public int flux;
    public double level;
    public int heat;

    public double levelLower;
    public double levelUpper;
    public double heatLower;
    public double heatUpper;
    public RodFunction function = RodFunction.LINEAR;

    public TileEntityReactorControl(BlockPos pos, BlockState state) {
        super(ModNuclearMiscBlockEntities.REACTOR_CONTROL.get(), pos, state, 1);
    }

    @Override
    public String getDefaultName() {
        return "container.reactorControl";
    }

    public static void serverTick(Level world, BlockPos pos, BlockState state, TileEntityReactorControl te) {
        te.isLinked = te.establishLink(world);

        if (te.isLinked) {
            double fauxLevel;

            double lowerBound = Math.min(te.heatLower, te.heatUpper);
            double upperBound = Math.max(te.heatLower, te.heatUpper);

            if (te.heat < lowerBound) {
                fauxLevel = te.levelLower;
            } else if (te.heat > upperBound) {
                fauxLevel = te.levelUpper;
            } else {
                fauxLevel = te.getTargetLevel(te.function, te.heat);
            }

            double level = Mth.clamp(fauxLevel * 0.01D, 0D, 1D);

            if (level != te.level) te.reactor.setTarget(level);
        }

        te.syncState();
    }

    private boolean establishLink(Level world) {
        ItemStack sensor = inventory.getStackInSlot(0);
        if (!sensor.isEmpty() && sensor.getItem() instanceof ItemReactorSensor) {
            BlockPos target = ItemReactorSensor.getTarget(sensor);
            if (target == null) return false;

            if (world.getBlockState(target).getBlock() == ModNuclearMiscBlocks.REACTOR_RESEARCH.get()) {
                BlockPos core = MultiblockSurface.coreOfAny(world, target);
                if (core != null && world.getBlockEntity(core) instanceof TileEntityReactorResearch research) {
                    reactor = research;
                    this.flux = reactor.totalFlux;
                    this.level = reactor.rodLevel;
                    this.heat = reactor.heat;
                    return true;
                }
            }
        }
        return false;
    }

    public double getTargetLevel(RodFunction function, int heat) {
        return switch (function) {
            case LINEAR -> (heat - this.heatLower) * ((this.levelUpper - this.levelLower) / (this.heatUpper - this.heatLower)) + this.levelLower;
            case LOG -> Math.pow((heat - this.heatUpper) / (this.heatLower - this.heatUpper), 2) * (this.levelLower - this.levelUpper) + this.levelUpper;
            case QUAD -> Math.pow((heat - this.heatLower) / (this.heatUpper - this.heatLower), 2) * (this.levelUpper - this.levelLower) + this.levelLower;
        };
    }

    public int[] getDisplayData() {
        if (this.isLinked) {
            int[] data = new int[3];
            data[0] = (int) (this.level * 100);
            data[1] = this.flux;
            data[2] = (int) Math.round((this.heat) * 0.00002 * 980 + 20);
            return data;
        }
        return new int[]{0, 0, 0};
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("function")) {
            this.function = RodFunction.values()[Mth.clamp(Tags.getIntOr(data, "function", 0), 0, RodFunction.values().length - 1)];
        } else {
            this.levelLower = Tags.getDoubleOr(data, "levelLower", 0D);
            this.levelUpper = Tags.getDoubleOr(data, "levelUpper", 0D);
            this.heatLower = Tags.getDoubleOr(data, "heatLower", 0D);
            this.heatUpper = Tags.getDoubleOr(data, "heatUpper", 0D);
        }
        this.setChanged();
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.distanceToSqr(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()) < 20D * 20D;
    }

    @Override public boolean isItemValidForSlot(int slot, ItemStack stack) { return true; }

    public enum RodFunction {
        LINEAR,
        QUAD,
        LOG
    }

    @Override public Component getDisplayName() { return Component.translatable(getName()); }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerReactorControl(containerId, playerInventory, this);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(heat);
        buf.writeDouble(level);
        buf.writeInt(flux);
        buf.writeBoolean(isLinked);
        buf.writeDouble(levelLower);
        buf.writeDouble(levelUpper);
        buf.writeDouble(heatLower);
        buf.writeDouble(heatUpper);
        buf.writeByte(function.ordinal());
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        heat = buf.readInt();
        level = buf.readDouble();
        flux = buf.readInt();
        isLinked = buf.readBoolean();
        levelLower = buf.readDouble();
        levelUpper = buf.readDouble();
        heatLower = buf.readDouble();
        heatUpper = buf.readDouble();
        function = RodFunction.values()[buf.readByte()];
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putBoolean("isLinked", isLinked);
        output.putDouble("levelLower", levelLower);
        output.putDouble("levelUpper", levelUpper);
        output.putDouble("heatLower", heatLower);
        output.putDouble("heatUpper", heatUpper);
        output.putInt("function", function.ordinal());
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        isLinked = input.getBooleanOr("isLinked", false);
        levelLower = input.getDoubleOr("levelLower", 0D);
        levelUpper = input.getDoubleOr("levelUpper", 0D);
        heatLower = input.getDoubleOr("heatLower", 0D);
        heatUpper = input.getDoubleOr("heatUpper", 0D);
        function = RodFunction.values()[Mth.clamp(input.getIntOr("function", 0), 0, RodFunction.values().length - 1)];
    }
}
