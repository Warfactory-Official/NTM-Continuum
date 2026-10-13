package com.hbm.gametest;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModDecoCrateBlocks;
import com.hbm.blocks.ModDungeonBlocks;
import com.hbm.entity.projectile.EntityMeteor;
import com.hbm.handler.ArmorModHandler;
import com.hbm.handler.BossSpawnHandler;
import com.hbm.items.ModArmorModItems;
import com.hbm.world.feature.GlyphidHive;
import com.hbm.world.gen.ConfigPlacement;
import com.hbm.world.gen.WorldgenKeys;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTestHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;

/**
 * GameTests for the 1.7 world features: what the overworld biome actually carries after the biome
 * modifiers ran, the hive schematic, and the meteor charms.
 */
public final class WorldFeatureGameTests {

    private WorldFeatureGameTests() {}

    /** Every hbm placed feature in the biome at the test position, per generation step, in order. */
    private static List<List<String>> features(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<List<String>> steps = new ArrayList<>();
        for (HolderSet<PlacedFeature> step : level.getBiome(helper.absolutePos(BlockPos.ZERO)).value().getGenerationSettings().features()) {
            List<String> ids = new ArrayList<>();
            for (Holder<PlacedFeature> holder : step) {
                holder.unwrapKey().ifPresent(key -> ids.add(Ids.keyId(key).toString()));
            }
            steps.add(ids);
        }
        return steps;
    }

    /**
     * The overworld biome carries 1.7's features at 1.7's steps: rare earths (not a second asbestos) in the schist, the
     * limestone vein, the MapGen terrain at raw generation, the strata before the veins, the surface features after
     * vanilla's vegetation, and the pink tree last.
     */
    public static void overworldFeatureLists(GameTestHelper helper) {
        List<List<String>> steps = features(helper);
        // GenerationStep.Decoration ordinals: 0 raw, 2 local_modifications, 6 underground_ores, 9 vegetal, 10 top layer
        helper.assertTrue(steps.size() >= 11, "the biome lists " + steps.size() + " steps");
        helper.assertTrue(steps.get(0).containsAll(List.of("hbm:oil_bubble", "hbm:bedrock_oil", "hbm:oil_sand_bubble")),
                "raw_generation: " + steps.get(0));
        helper.assertTrue(steps.get(2).containsAll(List.of("hbm:schist_stratum", "hbm:ore_caves", "hbm:ore_layers")),
                "local_modifications: " + steps.get(2));
        List<String> ores = steps.get(6);
        for (String id : List.of("hbm:ore_gneiss_rare", "hbm:ore_gneiss_asbestos", "hbm:limestone", "hbm:gas_bubble_flammable",
                "hbm:cluster_copper", "hbm:bedrock_ore", "hbm:coltan_deposit", "hbm:stone_keyhole", "hbm:cluster_depth_iron")) {
            helper.assertTrue(ores.contains(id), "underground_ores lacks " + id + ": " + ores);
        }
        // listed (hbm:config reads hbm.cfg at generation); 1.7's explosive bubble rate 0 places nothing
        helper.assertTrue(ores.contains("hbm:gas_bubble_explosive")
                && ConfigPlacement.value(WorldgenKeys.EXPLOSIVE_BUBBLE) == 0,
                "1.7's explosive bubble rate is 0");
        helper.assertTrue(ores.indexOf("hbm:ore_gneiss_iron") < ores.indexOf("hbm:ore_uranium")
                && ores.indexOf("hbm:ore_uranium") < ores.indexOf("hbm:limestone"), "1.7 vein order: " + ores);
        helper.assertTrue(steps.get(9).contains("hbm:weed") && steps.get(9).contains("hbm:fallen_meteorite")
                && steps.get(9).contains("hbm:glyphid_hive"), "vegetal_decoration: " + steps.get(9));
        helper.assertTrue(steps.get(10).contains("hbm:pink_tree"), "top_layer_modification: " + steps.get(10));

        // rare earths are the gneiss slot's block
        for (Holder<PlacedFeature> holder : helper.getLevel().getBiome(helper.absolutePos(BlockPos.ZERO)).value()
                .getGenerationSettings().features().get(6)) {
            String id = holder.unwrapKey().map(k -> Ids.keyId(k).toString()).orElse("");
            if (!id.equals("hbm:ore_gneiss_rare")) continue;
            var config = holder.value().feature().value().config();
            helper.assertTrue(config instanceof OreConfiguration ore
                            && ore.targetStates.get(0).state.getBlock() == ModBlocks.ore_gneiss_rare.get(),
                    "hbm:ore_gneiss_rare places " + config);
        }
        helper.succeed();
    }

    /** 1.7 {@code GlyphidHive.generateSmall}: the dome's block counts, spawners only in the core, a 3 x 3 floor. */
    public static void glyphidHive(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos c = helper.absolutePos(new BlockPos(0, 30, 0));
        GlyphidHive.generateSmall(level, c.getX(), c.getY(), c.getZ(), RandomSource.create(7L), false, true);
        int base = 0, spawner = 0, floor = 0, cells = 0;
        for (BlockPos pos : BlockPos.betweenClosed(c.offset(-5, -2, -5), c.offset(5, 2, 5))) {
            var state = level.getBlockState(pos);
            if (state.is(ModDecoCrateBlocks.glyphid_base.get())) base++;
            else if (state.is(ModDecoCrateBlocks.glyphid_spawner.get())) spawner++;
            else if (state.is(ModDungeonBlocks.deco_loot.get()) || state.is(Blocks.SKELETON_SKULL)) floor++;
        }
        for (int[][] layer : GlyphidHive.SCHEMATIC_SMALL) for (int[] row : layer) for (int v : row) if (v != 0) cells++;
        helper.assertTrue(floor == 9, "the hive floor holds " + floor + " skulls/piles, 1.7 has 9");
        helper.assertTrue(base + spawner + floor == cells, "hive cells " + (base + spawner + floor) + " of " + cells);
        helper.assertTrue(spawner > 0 && spawner <= 21, spawner + " spawners (only the 21 core cells may be one)");
        for (BlockPos pos : BlockPos.betweenClosed(c.offset(-5, -2, -5), c.offset(5, 2, 5))) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        helper.succeed();
    }

    /** 1.7 {@code BossSpawnHandler}: a Meteor Charm helmet mod stops the strike, a Charm of Protection makes it safe. */
    public static void meteorCharms(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        BlockPos at = helper.absolutePos(new BlockPos(1, 2, 1));
        player.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);

        ItemStack helmet = new ItemStack(Items.IRON_HELMET);
        ArmorModHandler.applyMod(helmet, new ItemStack(ModArmorModItems.meteor_charm.get()));
        player.setItemSlot(EquipmentSlot.HEAD, helmet);
        helper.assertTrue(BossSpawnHandler.strike(level, player) == null, "a Meteor Charm lets the strike through");

        helmet = new ItemStack(Items.IRON_HELMET);
        ArmorModHandler.applyMod(helmet, new ItemStack(ModArmorModItems.protection_charm.get()));
        player.setItemSlot(EquipmentSlot.HEAD, helmet);
        EntityMeteor safe = BossSpawnHandler.strike(level, player);
        helper.assertTrue(safe != null && safe.safe, "a Charm of Protection diverts the meteor (safe) instead of stopping it");
        helper.assertTrue(safe.getY() == 384 && safe.getDeltaMovement().y == -2.5, "meteor spawn " + safe.position() + " " + safe.getDeltaMovement());
        // diverted: it flies away from the player, never towards
        Vec3 away = new Vec3(safe.getX() - player.getX(), 0, safe.getZ() - player.getZ());
        helper.assertTrue(away.dot(safe.getDeltaMovement().multiply(1, 0, 1)) >= 0, "a diverted meteor heads away from the player");
        safe.discard();

        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        EntityMeteor plain = BossSpawnHandler.strike(level, player);
        helper.assertTrue(plain != null && !plain.safe, "an unprotected strike spawns an unsafe meteor");
        plain.discard();
        helper.succeed();
    }
}
