package com.hbm.items.weapon;

import com.hbm.entity.item.EntityItemBuoyant;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/**
 * 1.7 {@code ItemGrenadeFishing}, the stick of fishing dynamite: a harmless 3-power blast that throws fifteen
 * random water blocks within 7 of the detonation a buoyant fish each.
 *
 * <p>1.7 rolled {@code FishingHooks.getRandomFishable(rand, chance, luck 0, speed 100)}; speed 100 pushes both
 * the junk and the treasure chance below zero, so every roll lands on Forge's fish table, whose weights
 * (cod 60, salmon 25, clownfish 2, pufferfish 13) are reproduced here. The dispenser behaviour 1.7
 * registered for this item is not ported.
 */
public class ItemGrenadeFishing extends ItemGenericGrenade {

    public ItemGrenadeFishing(Properties properties, int fuse) {
        super(properties, fuse);
    }

    @Override
    public void explode(Entity grenade, LivingEntity thrower, Level world, double x, double y, double z) {
        world.explode(null, x, y + 0.25D, z, 3F, false, Level.ExplosionInteraction.NONE);

        int iX = Mth.floor(x);
        int iY = Mth.floor(y);
        int iZ = Mth.floor(z);
        RandomSource rand = world.getRandom();

        for (int i = 0; i < 15; i++) {
            int rX = iX + rand.nextInt(15) - 7;
            int rY = iY + rand.nextInt(15) - 7;
            int rZ = iZ + rand.nextInt(15) - 7;

            if (world.getBlockState(new BlockPos(rX, rY, rZ)).is(Blocks.WATER)) {
                ItemStack loot = getRandomLoot(rand);
                EntityItemBuoyant item = new EntityItemBuoyant(world, rX + 0.5, rY + 0.5, rZ + 0.5, loot.copy());
                item.setDeltaMovement(item.getDeltaMovement().x, 1, item.getDeltaMovement().z);
                world.addFreshEntity(item);
            }
        }
    }

    /** Forge 1.7 {@code FishingHooks.fish}: fish 60, salmon 25, clownfish 2, pufferfish 13. */
    public static ItemStack getRandomLoot(RandomSource rand) {
        int roll = rand.nextInt(100);
        if (roll < 60) return new ItemStack(Items.COD);
        if (roll < 85) return new ItemStack(Items.SALMON);
        if (roll < 87) return new ItemStack(Items.TROPICAL_FISH);
        return new ItemStack(Items.PUFFERFISH);
    }

    @Override
    public int getMaxTimer() {
        return 60;
    }

    @Override
    public double getBounceMod() {
        return 0.5D;
    }
}
