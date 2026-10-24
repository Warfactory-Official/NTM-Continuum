package com.hbm.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.hbm.Constants;
import com.hbm.api.data.ArmorDataMaps;
import com.hbm.api.data.RadiationDataMaps;
import com.hbm.api.data.RadiationDataMaps.Transformation;
import com.hbm.api.item.IGasMask;
import com.hbm.capability.HbmLivingProps;
import com.hbm.config.RadiationConfig;
import com.hbm.config.WorldConfig;
import com.hbm.entity.ModNtmEntities;
import com.hbm.handler.ArmorModHandler;
import com.hbm.handler.EntityEffectHandler;
import com.hbm.handler.HazmatRegistry;
import com.hbm.items.ModArmorItems;
import com.hbm.items.ModArmorModItems;
import com.hbm.items.ModArmorModules;
import com.hbm.items.ModGearItems;
import com.hbm.lib.HbmBlockTags;
import com.hbm.lib.HbmEntityTags;
import com.hbm.util.ArmorRegistry;
import com.hbm.util.ArmorRegistry.HazardClass;
import com.hbm.util.ContaminationUtil;
import com.hbm.world.biome.ModBiomes;
import dev.golem.core.Ids;
import dev.golem.core.data.DataFiles;
import dev.golem.core.data.DataMaps;
import dev.golem.core.data.DataMaps.DataMap;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.world.BlockChanges;
import dev.golem.core.world.Biomes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Radiation and hazard values as data: the shipped maps and tags give 1.7's values, and a pack changes them. */
public final class RadiationDataGameTests {

    private RadiationDataGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    public static final GameTests.Function DEFAULTS =
            TESTS.function("rad_data_defaults_equal_17", 20, () -> RadiationDataGameTests::defaults);
    public static final GameTests.Function TRANSFORM =
            TESTS.function("rad_data_transformation_override", 40, () -> RadiationDataGameTests::transformation);
    public static final GameTests.Function AMBIENT =
            TESTS.function("rad_data_ambient_override", 40, () -> RadiationDataGameTests::ambient);
    public static final GameTests.Function ARMOR =
            TESTS.function("rad_data_armor_override", 20, () -> RadiationDataGameTests::armor);
    public static final GameTests.Function CHANGES =
            TESTS.function("rad_data_block_changes", 20, () -> RadiationDataGameTests::blockChanges);

    private static final List<Object[]> CHANGED = new ArrayList<>();
    private static volatile BlockPos watched;

    public static void init() {
        BlockChanges.onChange((level, pos, oldState, newState) -> {
            if (pos.equals(watched)) CHANGED.add(new Object[] {oldState, newState});
        });
    }

    public static void defaults(GameTestHelper helper) {
        Map<String, Transformation> transformations = RadiationDataMaps.TRANSFORMATIONS.byId();
        helper.assertTrue(transformations.size() == 4
                        && transformations.get("minecraft:creeper").equals(new Transformation(200D, 3, "hbm:entity_mob_nuclear_creeper", 100F))
                        && transformations.get("minecraft:cow").equals(new Transformation(50D, 1, "minecraft:mooshroom", 0F))
                        && transformations.get("minecraft:villager").equals(new Transformation(500D, 1, "minecraft:zombie", 0F))
                        && transformations.get("hbm:entity_fucc_a_ducc").equals(new Transformation(200D, 1, "hbm:entity_elder_one", 0F)),
                "transformations differ from 1.7: " + transformations);

        double[] cladding = {ArmorDataMaps.cladding(ModArmorModules.cladding_paint.get()), ArmorDataMaps.cladding(ModArmorModules.cladding_rubber.get()),
                ArmorDataMaps.cladding(ModArmorModules.cladding_lead.get()), ArmorDataMaps.cladding(ModArmorModules.cladding_desh.get()),
                ArmorDataMaps.cladding(ModArmorModules.cladding_ghiorsium.get())};
        helper.assertTrue(cladding[0] == 0.025D && cladding[1] == 0.005D && cladding[2] == 0.1D && cladding[3] == 0.2D
                && cladding[4] == 0.5D, "cladding differs from 1.7: " + List.of(cladding[0], cladding[1], cladding[2], cladding[3], cladding[4]));

        List<HazardClass> mono = List.of(HazardClass.GAS_LUNG, HazardClass.GAS_BLISTERING, HazardClass.BACTERIA);
        List<HazardClass> others = List.of(HazardClass.GAS_BLISTERING);
        expectBlacklist(helper, ModGearItems.gas_mask.get(), others);
        expectBlacklist(helper, ModGearItems.gas_mask_m65.get(), others);
        expectBlacklist(helper, ModGearItems.gas_mask_olde.get(), others);
        expectBlacklist(helper, ModGearItems.gas_mask_mono.get(), mono);
        expectBlacklist(helper, ModArmorModItems.attachment_mask.get(), others);
        expectBlacklist(helper, ModArmorModItems.attachment_mask_mono.get(), mono);
        expectBlacklist(helper, ModArmorModItems.hazmat_paa_helmet.get(), List.of());

        for (EntityType<?> type : List.of(ModNtmEntities.CREEPER_NUCLEAR.get(), ModNtmEntities.QUACKOS.get(), EntityType.MOOSHROOM,
                EntityType.ZOMBIE, EntityType.HUSK, EntityType.DROWNED, EntityType.ZOMBIE_VILLAGER, EntityType.ZOMBIFIED_PIGLIN,
                EntityType.SKELETON, EntityType.STRAY, EntityType.WITHER_SKELETON, EntityType.OCELOT, EntityType.CAT,
                EntityType.ZOMBIE_HORSE, EntityType.SKELETON_HORSE, EntityType.ARMOR_STAND)) {
            helper.assertTrue(type.builtInRegistryHolder().is(HbmEntityTags.RADIATION_IMMUNE), EntityType.getKey(type) + " is not radiation immune");
        }
        for (EntityType<?> type : List.of(EntityType.CREEPER, EntityType.COW, EntityType.VILLAGER, EntityType.PIG,
                ModNtmEntities.DUCK.get(), EntityType.PLAYER)) {
            helper.assertTrue(!type.builtInRegistryHolder().is(HbmEntityTags.RADIATION_IMMUNE), EntityType.getKey(type) + " must not be radiation immune");
        }
        LivingEntity zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
        LivingEntity cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(1, 2, 1));
        helper.assertTrue(ContaminationUtil.isRadImmune(zombie) && !ContaminationUtil.isRadImmune(cow), "isRadImmune does not read the tag");

        helper.assertTrue(!BuiltInRegistries.BLOCK.getTagOrEmpty(HbmBlockTags.RADIATION_RESISTANT).iterator().hasNext(),
                "hbm:radiation_resistant must ship empty");

        // no shipped ambient values: the crater biomes and the nether keep their hbm.cfg keys
        helper.assertTrue(RadiationDataMaps.BIOME_RADIATION.byId().isEmpty() && RadiationDataMaps.BIOME_RADIATION.byTag().isEmpty()
                && RadiationDataMaps.DIMENSION_RADIATION.byId().isEmpty(), "ambient radiation maps must ship empty");
        var registries = helper.getLevel().registryAccess();
        helper.assertTrue(EntityEffectHandler.craterBiomeRad(Biomes.holder(registries, ModBiomes.crater_inner), true)
                        == WorldConfig.craterBiomeInnerRad * WorldConfig.craterBiomeWaterMult
                        && EntityEffectHandler.craterBiomeRad(plains(helper), true) == 0F,
                "biome radiation differs from the config keys");
        helper.assertTrue(RadiationConfig.hellRad == 0.1D, "hellRad must be 1.7's 0.1");
        helper.succeed();
    }

    /** A pack's map entry turns pigs into zombified piglins; a failed roll deals the fallback damage instead. */
    public static void transformation(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        AABB box = new AABB(helper.absolutePos(pos)).inflate(8);
        withOverride(helper, RadiationDataMaps.TRANSFORMATIONS, """
                {"values": {"minecraft:pig": {"threshold": 10, "result": "minecraft:zombified_piglin"},
                            "minecraft:sheep": {"threshold": 10, "chance": 2147483647, "result": "minecraft:zombie", "fallback_damage": 3}}}""", () -> {
            LivingEntity pig = helper.spawnWithNoFreeWill(EntityType.PIG, pos);
            HbmLivingProps.setRadiation(pig, 9);
            EntityEffectHandler.onUpdate(pig);
            helper.assertTrue(!pig.isRemoved(), "a pig below the threshold must stay");
            HbmLivingProps.setRadiation(pig, 10);
            EntityEffectHandler.onUpdate(pig);
            helper.assertTrue(pig.isRemoved() && !helper.getLevel().getEntitiesOfClass(LivingEntity.class, box,
                    e -> e.getType() == EntityType.ZOMBIFIED_PIGLIN).isEmpty(), "the pig did not become a zombified piglin");

            LivingEntity sheep = helper.spawnWithNoFreeWill(EntityType.SHEEP, pos);
            float health = sheep.getHealth();
            HbmLivingProps.setRadiation(sheep, 10);
            EntityEffectHandler.onUpdate(sheep);
            helper.assertTrue(!sheep.isRemoved() && sheep.getHealth() == health - 3F,
                    "a failed roll must deal the fallback damage, health " + sheep.getHealth() + " of " + health);
            helper.assertTrue(RadiationDataMaps.TRANSFORMATIONS.get(EntityType.COW) != null, "the override dropped the shipped values");
        });
        helper.assertTrue(RadiationDataMaps.TRANSFORMATIONS.get(EntityType.PIG) == null, "the shipped values were not restored");
        helper.succeed();
    }

    /** A pack gives plains and the test's dimension ambient radiation. */
    public static void ambient(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        String dimension = Ids.keyId(level.dimension()).toString();
        withOverride(helper, RadiationDataMaps.BIOME_RADIATION, """
                {"values": {"minecraft:plains": {"radiation": 3, "wet_multiplier": 2}}}""", () -> {
            helper.assertTrue(EntityEffectHandler.craterBiomeRad(plains(helper), false) == 3F
                    && EntityEffectHandler.craterBiomeRad(plains(helper), true) == 6F, "the biome entry was not read");
        });
        withOverride(helper, RadiationDataMaps.DIMENSION_RADIATION, "{\"values\": {\"" + dimension + "\": 100}}", () -> {
            LivingEntity pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(1, 2, 1));
            HbmLivingProps.setRadiation(pig, 0);
            EntityEffectHandler.onUpdate(pig);
            double rad = HbmLivingProps.getRadiation(pig);
            helper.assertTrue(rad > 0D, "the dimension floor gave no radiation in " + dimension);
            pig.discard();
        });
        helper.assertTrue(RadiationDataMaps.DIMENSION_RADIATION.byId().isEmpty(), "the shipped values were not restored");
        helper.succeed();
    }

    /** A pack changes a cladding value and empties a gas mask's blacklist. */
    public static void armor(GameTestHelper helper) {
        ItemStack chest = new ItemStack(ModArmorItems.steel_plate.get());
        ArmorModHandler.applyMod(chest, new ItemStack(ModArmorModules.cladding_rubber.get()));
        helper.assertTrue(HazmatRegistry.getCladding(chest) == 0.005D, "rubber cladding is +0.005");
        withOverride(helper, ArmorDataMaps.RADIATION_CLADDING, "{\"values\": {\"hbm:cladding_rubber\": 1.5}}",
                () -> helper.assertTrue(HazmatRegistry.getCladding(chest) == 1.5D, "the cladding override was not read"));

        ItemStack mask = new ItemStack(ModGearItems.gas_mask.get());
        ((IGasMask) mask.getItem()).installFilter(mask, new ItemStack(ModGearItems.gas_mask_filter.get()));
        helper.assertTrue(!ArmorRegistry.getProtectionFromItem(mask).contains(HazardClass.GAS_BLISTERING),
                "the gas mask must not protect against blister gas");
        withOverride(helper, ArmorDataMaps.GAS_MASK_BLACKLIST, "{\"values\": {\"hbm:gas_mask\": []}}",
                () -> helper.assertTrue(ArmorRegistry.getProtectionFromItem(mask).contains(HazardClass.GAS_BLISTERING),
                        "the emptied blacklist was not read"));
        helper.succeed();
    }

    /** Core's block change hook reports a set and a removal with both states. */
    public static void blockChanges(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        CHANGED.clear();
        watched = pos;
        try {
            helper.getLevel().setBlockAndUpdate(pos, Blocks.OBSIDIAN.defaultBlockState());
            helper.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 0);
        } finally {
            watched = null;
        }
        helper.assertTrue(CHANGED.size() == 2, "expected two changes, got " + CHANGED.size());
        helper.assertTrue(((BlockState) CHANGED.get(0)[1]).is(Blocks.OBSIDIAN) && ((BlockState) CHANGED.get(1)[0]).is(Blocks.OBSIDIAN)
                && ((BlockState) CHANGED.get(1)[1]).isAir(), "the changes did not carry the old and new states");
        helper.succeed();
    }

    private static void expectBlacklist(GameTestHelper helper, Item item, List<HazardClass> expected) {
        List<HazardClass> actual = ((IGasMask) item).getBlacklist(new ItemStack(item));
        helper.assertTrue(actual.equals(expected), BuiltInRegistries.ITEM.getKey(item) + " blacklist " + actual + ", 1.7 " + expected);
    }

    private static Holder<Biome> plains(GameTestHelper helper) {
        return Biomes.holder(helper.getLevel().registryAccess(), ResourceKey.create(Registries.BIOME, Ids.minecraft("plains")));
    }

    /** Runs {@code body} with {@code json} loaded above the map's datapack files, then reloads only those. */
    private static void withOverride(GameTestHelper helper, DataMap<?, ?> map, String json, Runnable body) {
        List<JsonElement> shipped = DataFiles.readStacks(DataMaps.FOLDER, helper.getLevel().getServer().getResourceManager())
                .getOrDefault(map.fileId(), List.of());
        List<JsonElement> stack = new ArrayList<>(shipped);
        stack.add(JsonParser.parseString(json));
        map.load(stack);
        try {
            body.run();
        } finally {
            map.load(shipped);
        }
    }
}
