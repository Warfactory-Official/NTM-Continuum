package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.blocks.machine.Radiobox;
import com.hbm.entity.mob.EntityFBI;
import com.hbm.entity.mob.EntityFBIDrone;
import com.hbm.items.ModItems;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.ModDamageSources;
import com.hbm.tileentity.ModDecoCrateBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1.7 {@code TileEntityRadiobox}: a 500,000 HE receiver. While switched on and holding 25,000 HE (or fitted with a
 * spark battery, which makes it free), every tick it spends that much and deals 20 enervation damage - absolute
 * and armour-piercing - to every hostile mob in a 15-block box, sparing the FBI and their drones.
 */
public class TileEntityRadiobox extends TileEntityLoadedBase implements IEnergyReceiverMK2 {

    long power;
    public static long maxPower = 500000;
    public boolean infinite = false;

    public TileEntityRadiobox(BlockPos pos, BlockState state) {
        super(ModDecoCrateBlockEntities.RADIOBOX.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityRadiobox box) {
        for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            box.trySubscribe(level, pos.getX() + dir.offsetX, pos.getY() + dir.offsetY, pos.getZ() + dir.offsetZ, dir);
        }
        if (state.getValue(Radiobox.ON) && (box.power >= 25000 || box.infinite)) {

            if (!box.infinite) {
                box.power -= 25000;
                box.setChanged();
            }

            int range = 15;
            for (Entity entity : level.getEntitiesOfClass(Entity.class, new AABB(pos.getX() - range, pos.getY() - range,
                    pos.getZ() - range, pos.getX() + range, pos.getY() + range, pos.getZ() + range),
                    e -> e instanceof Enemy)) {
                if (entity instanceof EntityFBI || entity instanceof EntityFBIDrone) continue;
                entity.hurt(ModDamageSources.source(level, ModDamageSources.ENERVATION), 20.0F);
            }
        }
    }

    /** 1.7 {@code Radiobox.breakBlock}: a fitted spark battery comes back out. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null && infinite) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    new ItemStack(ModItems.battery_spark.get()));
        }
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        power = input.getLongOr("power", 0L);
        infinite = input.getBooleanOr("infinite", false);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("power", power);
        output.putBoolean("infinite", infinite);
    }

    @Override public void setPower(long i) { power = i; }
    @Override public long getPower() { return power; }
    @Override public long getMaxPower() { return maxPower; }
}
