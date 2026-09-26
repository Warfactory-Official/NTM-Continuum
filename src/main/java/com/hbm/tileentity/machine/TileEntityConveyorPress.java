package com.hbm.tileentity.machine;

import net.minecraft.core.Direction;
import dev.golem.core.data.Tags;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.entity.item.EntityMovingItem;
import com.hbm.inventory.recipes.PressRecipes;
import com.hbm.items.machine.ItemStamp;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.ItemStacks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;
import com.hbm.tileentity.ModProductionBlockEntities;

/**
 * The conveyor press core, ported from the original {@code TileEntityConveyorPress}: while a stamp is
 * installed and a single pressable {@link EntityMovingItem} sits on the belt cell above, the piston
 * strokes down at {@code speed} per tick for {@code usage} HE, swaps the item for its press output at the
 * bottom, waits five ticks and retracts.
 */
public class TileEntityConveyorPress extends TileEntityMachineBase implements IEnergyReceiverMK2 {

    public int usage = 100;
    public long power = 0;
    public static final long maxPower = 50000;

    public double speed = 0.125;
    public double press;
    public double renderPress;
    public double lastPress;
    private double syncPress;
    private int turnProgress;
    protected boolean isRetracting = false;
    private int delay;

    public ItemStack syncStack = ItemStack.EMPTY;

    public TileEntityConveyorPress(BlockPos pos, BlockState state) {
        super(ModProductionBlockEntities.CONVEYOR_PRESS.get(), pos, state, 1);
    }

    @Override
    public String getDefaultName() {
        return "";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityConveyorPress te) {
        if (te.delay <= 0) {

            if (te.isRetracting) {

                if (te.canRetract()) {
                    te.press -= te.speed;
                    te.power -= te.usage;

                    if (te.press <= 0) {
                        te.press = 0;
                        te.isRetracting = false;
                        te.delay = 0;
                    }
                }

            } else {

                if (te.canExtend(level)) {
                    te.press += te.speed;
                    te.power -= te.usage;

                    if (te.press >= 1) {
                        te.press = 1;
                        te.isRetracting = true;
                        te.delay = 5;
                        te.process(level);
                    }
                }
            }

        } else {
            te.delay--;
        }

        te.syncState();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityConveyorPress te) {
        // approach-based interpolation, GO!
        te.lastPress = te.renderPress;

        if (te.turnProgress > 0) {
            te.renderPress = te.renderPress + ((te.syncPress - te.renderPress) / (double) te.turnProgress);
            --te.turnProgress;
        } else {
            te.renderPress = te.syncPress;
        }
    }

    public DirPos[] getConPos() {
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        return new DirPos[]{
                new DirPos(x + 1, y, z, ForgeDirection.EAST),
                new DirPos(x - 1, y, z, ForgeDirection.WEST),
                new DirPos(x, y, z + 1, ForgeDirection.SOUTH),
                new DirPos(x, y, z - 1, ForgeDirection.NORTH),
        };
    }

    private AABB beltBox() {
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        return new AABB(x, y + 1, z, x + 1, y + 1.5, z + 1);
    }

    public boolean canExtend(Level level) {
        if (this.power < usage) return false;
        ItemStack stamp = inventory.getStackInSlot(0);
        if (stamp.isEmpty()) return false;

        List<EntityMovingItem> items = level.getEntitiesOfClass(EntityMovingItem.class, beltBox());
        if (items.isEmpty()) return false;

        int x = worldPosition.getX(), z = worldPosition.getZ();
        for (EntityMovingItem item : items) {
            ItemStack stack = item.getItemStack();
            if (!PressRecipes.getOutput(stack, stamp).isEmpty() && stack.getCount() == 1) {

                double d0 = 0.35;
                double d1 = 0.65;
                if (item.getX() > x + d0 && item.getX() < x + d1 && item.getZ() > z + d0 && item.getZ() < z + d1) {
                    item.setPos(x + 0.5, item.getY(), z + 0.5);
                }

                return true;
            }
        }

        return false;
    }

    public void process(Level level) {
        ItemStack stamp = inventory.getStackInSlot(0);
        List<EntityMovingItem> items = level.getEntitiesOfClass(EntityMovingItem.class, beltBox());

        for (EntityMovingItem item : items) {
            ItemStack stack = item.getItemStack();
            ItemStack output = PressRecipes.getOutput(stack, stamp);

            if (!output.isEmpty() && stack.getCount() == 1) {
                item.discard();
                EntityMovingItem out = new EntityMovingItem(level);
                out.setPos(item.getX(), item.getY(), item.getZ());
                out.setItemStack(output.copy());
                level.addFreshEntity(out);
            }
        }

        level.playSound(null, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                ModSounds.PRESS_OPERATE.get(), SoundSource.BLOCKS, getVolume(1.5F), 1.0F);

        if (stamp.isDamageableItem() && stamp.getMaxDamage() != 0) {
            stamp.setDamageValue(stamp.getDamageValue() + 1);
            if (stamp.getDamageValue() >= stamp.getMaxDamage()) {
                inventory.setStackInSlot(0, ItemStack.EMPTY);
            } else {
                inventory.setStackInSlot(0, stamp);
            }
        }
    }

    public boolean canRetract() {
        return this.power >= usage;
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(this.power);
        buf.writeDouble(this.press);
        ItemStacks.writeOptional(buf, inventory.getStackInSlot(0));
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.power = buf.readLong();
        this.syncPress = buf.readDouble();
        this.syncStack = ItemStacks.readOptional(buf);
        this.turnProgress = 2;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return stack.getItem() instanceof ItemStamp;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return new int[]{0};
    }

    @Override public long getPower() { return power; }
    @Override public long getMaxPower() { return maxPower; }
    @Override public void setPower(long power) { this.power = power; }

    @Override
    public boolean canConnect(Direction dir) {
        return dir != Direction.DOWN;
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        this.power = input.getLongOr("power", 0L);
        this.press = input.getDoubleOr("press", 0D);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("power", power);
        output.putDouble("press", press);
    }
}
