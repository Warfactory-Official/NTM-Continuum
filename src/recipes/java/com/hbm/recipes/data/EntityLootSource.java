package com.hbm.recipes.data;

import com.google.gson.JsonObject;
import dev.golem.core.world.EntityLootData;

/**
 * The death drops of HBM's mobs that scale with looting, each the mob's 1.7 {@code dropFewItems}.
 * {@code rand(n)} is a uniform count {@code 0..n-1}; the looting bonus adds {@code rand(looting + 1)}.
 */
public final class EntityLootSource extends EntityLootData {

    @Override
    protected void registerDefaults() {
        // 1.7 EntityCreeper: a player kill drops 5 + rand(6 + 2 looting) gold crystals, any other death 3
        creeper("entity_mob_gold_creeper",
                pool(item("hbm:crystal_gold", setCount(uniform(constant(5), looting(2, 10, false)))), killedByPlayer()),
                pool(item("hbm:crystal_gold", setCount(constant(3))), not(killedByPlayer())));
        creeper("entity_mob_nuclear_creeper",
                pool(item("minecraft:tnt", setCount(uniform(constant(0), constant(2))), addLooting(false))),
                pool(item("hbm:coin_creeper"), randomChance(1F / 3F)));
        creeper("entity_mob_phosgene_creeper",
                pool(item("minecraft:gunpowder", setCount(uniform(constant(0), constant(2))), addLooting(false))));
        creeper("entity_mob_tainted_creeper",
                pool(item("minecraft:tnt", setCount(uniform(constant(0), constant(2))), addLooting(false))));
        creeper("entity_mob_volatile_creeper",
                pool(item("hbm:sulfur", setCount(uniform(constant(2), constant(4))))),
                pool(item("hbm:stick_tnt", setCount(uniform(constant(1), constant(2))))));

        register("entity_fucc_a_ducc", table(
                feathers(),
                pool(alternatives(
                        item("minecraft:cooked_chicken"), onFire(),
                        item("minecraft:chicken")))));
        // the chicken stays in EntityPigeon: a fat pigeon drops three, and fatness is not saved where loot can see it
        register("entity_pigeon", table(feathers()));
        // 1.7 EntityRADBeast: a player kill drops one polonium nugget per looting level; the rods stay in the class
        register("entity_radiation_blaze", table(
                pool(item("hbm:nugget_polonium", setCount(looting(1, 0, false))), killedByPlayer())));

        // 1.7 EntityLivingBase.onDeath reads looting off a player killer only; a rare drop is nextInt(200) - looting < 5
        register("entity_taint_crab", table(
                pool(item("hbm:coil_copper", setCount(uniform(constant(0), constant(2))), addLooting(true))),
                pool(item("hbm:coil_magnetized_tungsten"), killedByPlayer(), rareDrop())));
        register("entity_tesla_crab", table(
                pool(item("hbm:coil_copper"), killedByPlayer(), rareDrop())));

        // 1.7 EntityGlyphid: half the time (int) scale * 2 + looting meat, grilled when the glyphid burns
        glyphid("entity_glyphid", 1.0);
        glyphid("entity_glyphid_scout", 0.75);
        glyphid("entity_glyphid_brawler", 1.25);
        glyphid("entity_glyphid_digger", 1.3);
        glyphid("entity_glyphid_bombardier", 1.0);
        glyphid("entity_glyphid_blaster", 1.25);
        glyphid("entity_glyphid_behemoth", 1.5);
        glyphid("entity_glyphid_brenda", 2.0);
        glyphid("entity_glyphid_nuclear", 2.0);
    }

    private void glyphid(String id, double scale) {
        JsonObject count = setCount(looting(1, (int) scale * 2, false));
        register(id, table(pool(alternatives(
                item("hbm:glyphid_meat_grilled", count.deepCopy()), onFire(),
                item("hbm:glyphid_meat", count)), randomChance(0.5F))));
    }

    /** {@code rand(3) + rand(1 + looting)} feathers (EntityDuck, EntityPigeon). */
    private static JsonObject feathers() {
        return pool(item("minecraft:feather", setCount(uniform(constant(0), constant(2))), addLooting(false)));
    }

}
