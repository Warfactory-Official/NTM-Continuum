package com.hbm.tileentity.machine;

import dev.golem.core.data.Tags;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm.explosion.vanillant.standard.ExplosionEffectWeapon;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.interfaces.ICopiable;
import com.hbm.lib.Library;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.ItemStacks;
import dev.golem.core.world.Recipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import com.hbm.inventory.container.ContainerMicrowave;
import com.hbm.tileentity.ModProductionBlockEntities;

/**
 * The microwave, ported from the original {@code TileEntityMicrowave}: cooks anything with a smelting
 * recipe as long as the input or the result is food, at a player-chosen speed 0..5; at speed 5 it blows
 * itself up. The OpenComputers callbacks are not carried.
 */
public class TileEntityMicrowave extends TileEntityMachineBase
        implements IEnergyReceiverMK2, MenuProvider, WorldlyContainer, ICopiable {

    public long power;
    public static final long maxPower = 50000;
    public static final int consumption = 50;
    public static final int maxTime = 300;
    public int time;
    public int speed;
    public static final int maxSpeed = 5;

    public TileEntityMicrowave(BlockPos pos, BlockState state) {
        super(ModProductionBlockEntities.MICROWAVE.get(), pos, state, 3);
    }

    @Override
    public String getDefaultName() {
        return "container.microwave";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMicrowave te) {
        for (com.hbm.lib.ForgeDirection dir : com.hbm.lib.ForgeDirection.VALID_DIRECTIONS) {
            te.trySubscribe(level, pos.getX() + dir.offsetX, pos.getY() + dir.offsetY, pos.getZ() + dir.offsetZ, dir);
        }
        te.power = Library.chargeTEFromItems(te.inventory, 2, te.power, maxPower);

        if (te.canProcess(level)) {

            if (te.speed >= maxSpeed) {
                level.destroyBlock(pos, false);
                ExplosionVNT vnt = new ExplosionVNT(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 5);
                vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, 50));
                vnt.setPlayerProcessor(new PlayerProcessorStandard());
                vnt.setSFX(new ExplosionEffectWeapon(10, 2.5F, 1F));
                vnt.explode();
                return;
            }

            if (te.time >= maxTime) {
                te.process(level);
                te.time = 0;
            }

            if (te.canProcess(level)) {
                te.power -= consumption;
                te.time += te.speed * 2;
            }
        }

        te.networkPackNT();
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeInt(time);
        buf.writeInt(speed);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        time = buf.readInt();
        speed = buf.readInt();
    }

    @Override
    public void handleButtonPacket(int value, int meta) {
        if (value == 0) speed++;
        if (value == 1) speed--;

        if (speed < 0) speed = 0;
        if (speed > maxSpeed) speed = maxSpeed;
    }

    private @Nullable ItemStack smeltingResult(Level level, ItemStack input) {
        return Recipes.smeltingResult(level, input).orElse(null);
    }

    private void process(Level level) {
        ItemStack stack = smeltingResult(level, inventory.getStackInSlot(0));
        if (stack == null) return;

        ItemStack out = inventory.getStackInSlot(1);
        if (out.isEmpty()) {
            inventory.setStackInSlot(1, stack.copy());
        } else {
            out.grow(stack.getCount());
        }

        inventory.extractItem(0, 1, false);
        this.setChanged();
    }

    private boolean canProcess(Level level) {
        if (speed == 0) return false;
        if (power < consumption) return false;

        ItemStack input = inventory.getStackInSlot(0);
        if (input.isEmpty()) return false;
        ItemStack stack = smeltingResult(level, input);
        if (stack == null) return false;

        if (ItemStacks.foodProperties(input) == null && ItemStacks.foodProperties(stack) == null) return false;
        ItemStack out = inventory.getStackInSlot(1);
        if (out.isEmpty()) return true;
        if (!ItemStack.isSameItem(stack, out)) return false;

        return stack.getCount() + out.getCount() <= stack.getMaxStackSize();
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        // The old check asked the furnace registry directly; a client level cannot, so the slot validator
        // stays permissive there and the server-side ticker still decides what actually cooks.
        return slot == 0 && (level == null || level.isClientSide() || smeltingResult(level, stack) != null);
    }

    @Override public int[] getSlotsForFace(Direction side) { return side == Direction.DOWN ? new int[]{1} : new int[]{0}; }
    @Override public int[] getAccessibleSlotsFromSide(int side) { return side == 0 ? new int[]{1} : new int[]{0}; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return isItemValidForSlot(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == 1; }

    public long getPowerScaled(int i) { return (power * i) / maxPower; }
    public int getProgressScaled(int i) { return (time * i) / maxTime; }
    public int getSpeedScaled(int i) { return (speed * i) / maxSpeed; }

    @Override public void setPower(long i) { power = i; }
    @Override public long getPower() { return power; }
    @Override public long getMaxPower() { return maxPower; }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        power = input.getLongOr("power", 0L);
        speed = input.getIntOr("speed", 0);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("power", power);
        output.putInt("speed", speed);
    }

    @Override public Component getDisplayName() { return Component.translatable(getName()); }
    @Override public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMicrowave(containerId, playerInventory, this);
    }

    /** 1.7 writes the tag and then returns null — the copy tool never receives the speed. Kept as-is. */
    @Override
    public @Nullable CompoundTag getSettings(Level level, BlockPos pos) {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("microSpeed", speed);
        return null;
    }

    @Override
    public void pasteSettings(CompoundTag nbt, int index, Level level, Player player, BlockPos pos) {
        if (nbt.contains("microSpeed")) speed = Mth.clamp(Tags.getIntOr(nbt, "microSpeed", 0), 0, maxSpeed);
    }

    @Override
    public String[] infoForDisplay(Level level, BlockPos pos) {
        return new String[]{"copyTool.speed"};
    }
}
