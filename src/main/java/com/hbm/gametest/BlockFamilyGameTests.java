package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.blocks.ModBeaconBlocks;
import com.hbm.blocks.ModFallingBlocks;
import com.hbm.blocks.ModGlassBlocks;
import com.hbm.blocks.ModLadderBlocks;
import com.hbm.blocks.generic.BlockNTMGlass;
import com.hbm.blocks.generic.BlockNTMLadder;
import dev.golem.core.Ids;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.List;

/** Registry, tag, and vanilla-behaviour contracts for the beacon/falling/glass/ladder block wave. */
public final class BlockFamilyGameTests {

    private static final TagKey<Block> BEACON_BASE_BLOCKS =
            TagKey.create(Registries.BLOCK, Ids.of("minecraft", "beacon_base_blocks"));
    private static final TagKey<Block> CLIMBABLE =
            TagKey.create(Registries.BLOCK, Ids.of("minecraft", "climbable"));

    private BlockFamilyGameTests() {}

    private static final List<RegistryObject<? extends Block>> BEACON = List.of(
            ModBeaconBlocks.block_cadmium, ModBeaconBlocks.block_bismuth, ModBeaconBlocks.block_coltan,
            ModBeaconBlocks.block_tantalium, ModBeaconBlocks.block_niobium, ModBeaconBlocks.block_lanthanium,
            ModBeaconBlocks.block_actinium, ModBeaconBlocks.block_polymer, ModBeaconBlocks.block_bakelite,
            ModBeaconBlocks.block_rubber, ModBeaconBlocks.block_zirconium);
    private static final List<String> BEACON_IDS = List.of(
            "block_cadmium", "block_bismuth", "block_coltan", "block_tantalium", "block_niobium",
            "block_lanthanium", "block_actinium", "block_polymer", "block_bakelite", "block_rubber",
            "block_zirconium");
    private static final List<RegistryObject<? extends Block>> FALLING = List.of(
            ModFallingBlocks.block_scrap, ModFallingBlocks.block_electrical_scrap, ModFallingBlocks.moon_turf,
            ModFallingBlocks.block_red_phosphorus, ModFallingBlocks.sand_boron, ModFallingBlocks.sand_gold,
            ModFallingBlocks.sand_quartz);
    private static final List<String> FALLING_IDS = List.of(
            "block_scrap", "block_electrical_scrap", "moon_turf", "block_red_phosphorus", "sand_boron",
            "sand_gold", "sand_quartz");
    private static final List<RegistryObject<? extends Block>> GLASS = List.of(
            ModGlassBlocks.reinforced_glass, ModGlassBlocks.reinforced_laminate, ModGlassBlocks.glass_uranium,
            ModGlassBlocks.glass_trinitite, ModGlassBlocks.glass_polonium, ModGlassBlocks.glass_ash,
            ModGlassBlocks.glass_polarized, ModGlassBlocks.hadron_plating_glass,
            ModGlassBlocks.hadron_analysis_glass);
    private static final List<String> GLASS_IDS = List.of(
            "reinforced_glass", "reinforced_laminate", "glass_uranium", "glass_trinitite", "glass_polonium",
            "glass_ash", "glass_polarized", "hadron_plating_glass", "hadron_analysis_glass");
    private static final List<RegistryObject<? extends Block>> LADDERS = List.of(
            ModLadderBlocks.ladder_sturdy, ModLadderBlocks.ladder_iron, ModLadderBlocks.ladder_gold,
            ModLadderBlocks.ladder_aluminium, ModLadderBlocks.ladder_copper, ModLadderBlocks.ladder_titanium,
            ModLadderBlocks.ladder_lead, ModLadderBlocks.ladder_cobalt, ModLadderBlocks.ladder_steel,
            ModLadderBlocks.ladder_tungsten, ModLadderBlocks.ladder_red, ModLadderBlocks.ladder_red_top);
    private static final List<String> LADDER_IDS = List.of(
            "ladder_sturdy", "ladder_iron", "ladder_gold", "ladder_aluminium", "ladder_copper",
            "ladder_titanium", "ladder_lead", "ladder_cobalt", "ladder_steel", "ladder_tungsten",
            "ladder_red", "ladder_red_top");

    public static void registryAndTags(GameTestHelper helper) {
        assertFamily(helper, BEACON, BEACON_IDS, true, false);
        assertFamily(helper, FALLING, FALLING_IDS, false, false);
        assertFamily(helper, GLASS, GLASS_IDS, false, false);
        assertFamily(helper, LADDERS, LADDER_IDS, false, true);

        for (RegistryObject<? extends Block> entry : BEACON) {
            helper.assertTrue(entry.get().defaultBlockState().is(BEACON_BASE_BLOCKS),
                    "Beacon block missing beacon_base_blocks tag: " + BuiltInRegistries.BLOCK.getKey(entry.get()));
        }
        for (RegistryObject<? extends Block> entry : LADDERS) {
            helper.assertTrue(entry.get().defaultBlockState().is(CLIMBABLE),
                    "Ladder missing climbable tag: " + BuiltInRegistries.BLOCK.getKey(entry.get()));
        }
        helper.succeed();
    }

    public static void familyBehaviour(GameTestHelper helper) {
        for (RegistryObject<? extends Block> entry : FALLING) {
            helper.assertTrue(entry.get() instanceof FallingBlock,
                    "Falling family lost FallingBlock behavior: " + BuiltInRegistries.BLOCK.getKey(entry.get()));
        }

        BlockNTMGlass reinforced = ModGlassBlocks.reinforced_glass.get();
        BlockNTMGlass laminate = ModGlassBlocks.reinforced_laminate.get();
        helper.assertTrue(reinforced.isRadResistant(helper.getLevel(), BlockPos.ZERO),
                "Reinforced glass must remain radiation-resistant");
        helper.assertTrue(laminate.isRadResistant(helper.getLevel(), BlockPos.ZERO),
                "Reinforced laminate must remain radiation-resistant");
        helper.assertFalse(ModGlassBlocks.glass_uranium.get().isRadResistant(helper.getLevel(), BlockPos.ZERO),
                "Uranium glass must not be marked as a radiation seal");

        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockNTMLadder regular = ModLadderBlocks.ladder_red.get();
        BlockNTMLadder top = ModLadderBlocks.ladder_red_top.get();
        helper.assertFalse(regular.canSurvive(regular.defaultBlockState(), helper.getLevel(), pos),
                "Normal ladder must require a supporting block");
        helper.assertTrue(top.canSurvive(top.defaultBlockState(), helper.getLevel(), pos),
                "Red ladder top must survive without a supporting block");
        double height = top.getShape(top.defaultBlockState(), helper.getLevel(), pos, CollisionContext.empty())
                .max(Direction.Axis.Y);
        helper.assertTrue(height == 0.25D, "Red ladder top height is " + height + ", expected 0.25");
        helper.succeed();
    }

    private static void assertFamily(GameTestHelper helper, List<RegistryObject<? extends Block>> entries,
                                     List<String> ids, boolean beacon, boolean ladder) {
        for (int i = 0; i < entries.size(); i++) {
            Block block = entries.get(i).get();
            Identifier expected = Ids.of(Constants.MOD_ID, ids.get(i));
            helper.assertTrue(expected.equals(BuiltInRegistries.BLOCK.getKey(block)),
                    "Wrong block id: expected " + expected + ", got " + BuiltInRegistries.BLOCK.getKey(block));
            Item item = BuiltInRegistries.ITEM.getValue(expected);
            helper.assertTrue(expected.equals(BuiltInRegistries.ITEM.getKey(item)),
                    "Missing block item for " + expected);
            if (beacon) helper.assertTrue(block.defaultBlockState().is(BEACON_BASE_BLOCKS), expected + " is not beaconable");
            if (ladder) helper.assertTrue(block.defaultBlockState().is(CLIMBABLE), expected + " is not climbable");
        }
    }
}
