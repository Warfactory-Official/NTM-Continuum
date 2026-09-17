package com.hbm.gametest;

import com.hbm.entity.ModNtmEntities;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.registry.Entities;
import dev.golem.core.registry.RegistryObject;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;

/**
 * Positive registration checks for the ntm-next entity port: every {@link ModNtmEntities} type resolves,
 * constructs on a server level, and - for living entities - carries an attribute supplier. A missing
 * supplier is the failure that only shows at first spawn (a crash), so it is asserted here instead.
 */
public final class NtmEntityPortGameTests {

    private NtmEntityPortGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite("hbm");
    public static final GameTests.Function REGISTRATION =
            TESTS.function("ntm_entities_registered", 20, () -> NtmEntityPortGameTests::allTypesRegistered);
    public static final GameTests.Function CONSTRUCTION =
            TESTS.function("ntm_entities_construct", 40, () -> NtmEntityPortGameTests::allTypesConstruct);

    public static final GameTests.Function BOSS_DEATH_LOOT =
            TESTS.function("ntm_boss_death_loot", 40, () -> NtmEntityPortGameTests::bossDeathLoot);
    public static final GameTests.Function NPC_BULLETS =
            TESTS.function("ntm_npc_bullet_configs", 20, () -> NtmEntityPortGameTests::npcBulletConfigs);

    public static void init() {}

    /**
     * The mask man's death loot writes a gas-mask filter component; NeoForge rejects a component whose value
     * class has no value equality the moment it is set, which is only reached by killing him.
     */
    public static void bossDeathLoot(GameTestHelper helper) {
        com.hbm.entity.mob.EntityMaskMan maskMan = helper.spawn(ModNtmEntities.MASK_MAN.get(),
                new net.minecraft.core.BlockPos(1, 2, 1));
        dev.golem.core.world.Compat.kill(maskMan, helper.getLevel());
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(!maskMan.isAlive(), "mask man should be dead after kill()");
            helper.succeed();
        });
    }

    /** Every bullet config an NPC fires must be initialised, or the first shot constructs a bullet on null. */
    public static void npcBulletConfigs(GameTestHelper helper) {
        com.hbm.items.weapon.sedna.factory.XFactoryNPC.class.getName();
        helper.assertTrue(com.hbm.items.weapon.sedna.factory.XFactoryNPC.worm_laser != null, "XFactoryNPC.worm_laser is null");
        helper.assertTrue(com.hbm.items.weapon.sedna.factory.XFactoryNPC.maskman_orb != null, "XFactoryNPC.maskman_orb is null");
        helper.assertTrue(com.hbm.items.weapon.sedna.factory.XFactoryNPC.ufo_rocket != null, "XFactoryNPC.ufo_rocket is null");
        helper.assertTrue(com.hbm.items.weapon.sedna.factory.XFactoryWarhead.warhead_turbine != null,
                "XFactoryWarhead.warhead_turbine is null");
        helper.succeed();
    }

    @SuppressWarnings("unchecked")
    private static List<EntityType<?>> portedTypes() {
        List<EntityType<?>> types = new ArrayList<>();
        for (Field field : ModNtmEntities.class.getFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || field.getType() != RegistryObject.class) continue;
            try {
                Object value = ((RegistryObject<?>) field.get(null)).get();
                if (value instanceof EntityType<?> type) types.add(type);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(field.getName(), e);
            }
        }
        return types;
    }

    public static void allTypesRegistered(GameTestHelper helper) {
        List<EntityType<?>> types = portedTypes();
        helper.assertTrue(types.size() >= 100, "expected the 100 ntm-next port types, found " + types.size());
        for (EntityType<?> type : types) {
            String id = EntityType.getKey(type).toString();
            helper.assertTrue(id.startsWith("hbm:"), "ported type registered outside hbm: " + id);
            helper.assertTrue(Entities.byId(id) == type, "registry lookup does not return the port type for " + id);
        }
        helper.succeed();
    }

    public static void allTypesConstruct(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<String> failures = new ArrayList<>();
        for (EntityType<?> type : portedTypes()) {
            String id = EntityType.getKey(type).toString();
            Entity entity;
            try {
                entity = Entities.createDetached(type, level);
            } catch (RuntimeException e) {
                failures.add(id + ": " + e);
                continue;
            }
            if (entity == null) {
                failures.add(id + ": factory returned null");
                continue;
            }
            if (entity instanceof LivingEntity && !DefaultAttributes.hasSupplier(type)) {
                failures.add(id + ": living entity without an attribute supplier");
            }
            entity.discard();
        }
        helper.assertTrue(failures.isEmpty(), String.join("; ", failures));
        helper.succeed();
    }
}
