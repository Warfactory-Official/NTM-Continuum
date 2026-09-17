package com.hbm.tileentity.machine.dfc;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.machine.dfc.CoreComponent;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.container.ContainerCoreStabilizer;
import com.hbm.items.machine.ItemLens;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.Tags;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * What keeps the core from detonating. Every tick it spends {@code watts^4} HE to raise the core's field
 * to at least {@code watts}, and burns the same number of ticks off its lens doing it — so the field a
 * core can hold is bounded by how much lens the player is willing to feed it.
 *
 * <p>The thirteenth machine with a redstone-over-radio surface.
 */
public class TileEntityCoreStabilizer extends TileEntityMachineBase
        implements MenuProvider, IControlReceiver, IEnergyReceiverMK2, IRORValueProvider, IRORInteractive {

    public static final long maxPower = 2_500_000_000L;
    public static final int range = 15;

    public long power;
    public int watts;
    public int beam;

    public TileEntityCoreStabilizer(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DFC_STABILIZER.get(), pos, state, 1);
    }

    @Override
    public String getDefaultName() {
        return "container.dfcStabilizer";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityCoreStabilizer stabilizer) {
        stabilizer.tickServer(level);
    }

    protected void tickServer(Level level) {
        BlockPos pos = getBlockPos();
        watts = Mth.clamp(watts, 1, 100);
        long demand = (long) Math.pow(watts, 4);

        beam = 0;

        if (power >= demand && hasWorkingLens()) {
            ForgeDirection dir = facing();

            for (int i = 1; i <= range; i++) {
                BlockPos target = pos.offset(dir.offsetX * i, dir.offsetY * i, dir.offsetZ * i);
                BlockEntity be = level.getBlockEntity(target);

                if (be instanceof TileEntityCore core) {
                    core.field = Math.max(core.field, watts);
                    this.power -= demand;
                    beam = i;
                    wearLens();
                    break;
                }

                if (!level.getBlockState(target).isAir()) break;
            }
        }

        networkPackNT();
    }

    private ItemStack lens() {
        return inventory.getStackInSlot(0);
    }

    private boolean hasWorkingLens() {
        ItemStack stack = lens();
        return stack.getItem() instanceof ItemLens lens && ItemLens.getLensDamage(stack) < lens.maxDamage;
    }

    private void wearLens() {
        ItemStack stack = lens();
        if (!(stack.getItem() instanceof ItemLens lens)) return;
        long damage = ItemLens.getLensDamage(stack) + watts;
        if (damage >= lens.maxDamage) inventory.setStackInSlot(0, ItemStack.EMPTY);
        else ItemLens.setLensDamage(stack, damage);
    }

    public ForgeDirection facing() {
        return ForgeDirection.getOrientation(getBlockState().getValue(CoreComponent.FACING).ordinal());
    }

    public long getPowerScaled(long i) {
        return (power * i) / maxPower;
    }

    public int getWattsScaled(int i) {
        return (watts * i) / 100;
    }

    @Override public long getPower() { return power; }
    @Override public void setPower(long i) { this.power = i; }
    @Override public long getMaxPower() { return maxPower; }
    @Override public boolean canConnect(ForgeDirection dir) { return dir != ForgeDirection.UNKNOWN; }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return stack.getItem() instanceof ItemLens;
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeInt(watts);
        buf.writeInt(beam);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.power = buf.readLong();
        this.watts = buf.readInt();
        this.beam = buf.readInt();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("power", power);
        output.putInt("watts", watts);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        power = input.getLongOr("power", 0L);
        watts = input.getIntOr("watts", 0);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getDefaultName());
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new ContainerCoreStabilizer(id, playerInventory, this);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (Tags.hasInt(data, "watts")) {
            this.watts = Mth.clamp(Tags.getIntOr(data, "watts", 1), 1, 100);
            setChanged();
        }
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.distanceToSqr(getBlockPos().getCenter()) <= 64D;
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[]{
                PREFIX_VALUE + "durability", PREFIX_VALUE + "durabilitypercent",
                PREFIX_FUNCTION + "setpower" + NAME_SEPARATOR + "percent"};
    }

    @Override
    public String provideRORValue(String name) {
        ItemStack stack = lens();
        boolean present = stack.getItem() instanceof ItemLens;
        long remaining = present
                ? ((ItemLens) stack.getItem()).maxDamage - ItemLens.getLensDamage(stack) : 0L;

        if ((PREFIX_VALUE + "durability").equals(name)) return "" + remaining;
        if ((PREFIX_VALUE + "durabilitypercent").equals(name)) {
            return "" + (present ? remaining * 100 / ((ItemLens) stack.getItem()).maxDamage : 0L);
        }
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setpower").equals(name) && params.length > 0) {
            this.watts = IRORInteractive.parseInt(params[0], 0, 100);
            setChanged();
            return null;
        }
        return null;
    }
}
