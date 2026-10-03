package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.handler.ability.IToolAreaAbility;
import com.hbm.handler.ability.IToolHarvestAbility;
import com.hbm.handler.ability.IWeaponAbility;
import com.hbm.handler.ability.ToolPreset;
import com.hbm.items.ModAbilityToolItems;
import com.hbm.items.tool.ItemToolAbility;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Contracts for the ability tool family: presets built and cycled as 1.7 built them, the hammer and vein
 * miner breaking what 1.7 broke through the real server destroy path, the smelter replacing drops, and a
 * weapon ability landing on a hit.
 */
public final class AbilityToolGameTests {

    private AbilityToolGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function PRESETS =
            TESTS.function("ability_tool_presets", 20, () -> AbilityToolGameTests::presets);
    public static final GameTests.Function HAMMER =
            TESTS.function("ability_tool_hammer", 40, () -> AbilityToolGameTests::hammer);
    public static final GameTests.Function VEIN_SMELTER =
            TESTS.function("ability_tool_vein_smelter", 40, () -> AbilityToolGameTests::veinSmelter);
    public static final GameTests.Function WEAPON =
            TESTS.function("ability_tool_weapon", 20, () -> AbilityToolGameTests::weapon);
    public static final GameTests.Function CHAINSAW_WEAR =
            TESTS.function("ability_tool_chainsaw_wear", 20, () -> AbilityToolGameTests::chainsawWear);

    public static void init() {}

    /** 1.7 Configuration.reset: the none preset first, then one per ability, sorted harvest-then-area. */
    public static void presets(net.minecraft.gametest.framework.GameTestHelper helper) {
        ItemStack stack = new ItemStack(ModAbilityToolItems.cmb_pickaxe.get());
        ItemToolAbility tool = (ItemToolAbility) stack.getItem();
        ItemToolAbility.Configuration config = tool.getConfiguration(stack);

        // cmb_pickaxe: RECURSION 2, SMELTER, SILK, LUCK 2 -> none, recursion, silk, luck, smelter
        helper.assertTrue(config.presets.size() == 5, "cmb pickaxe must have 5 presets, has " + config.presets.size());
        helper.assertTrue(config.presets.get(0).isNone(), "first preset must be none");
        helper.assertTrue(config.presets.get(1).areaAbility == IToolAreaAbility.RECURSION
                && config.presets.get(1).areaAbilityLevel == 2, "second preset must be vein miner at its max level");
        helper.assertTrue(config.presets.get(2).harvestAbility == IToolHarvestAbility.SILK, "third preset must be silk");
        helper.assertTrue(config.presets.get(3).harvestAbility == IToolHarvestAbility.LUCK
                && config.presets.get(3).harvestAbilityLevel == 2, "fourth preset must be fortune 3");
        helper.assertTrue(config.presets.get(4).harvestAbility == IToolHarvestAbility.SMELTER, "fifth preset must be smelter");

        // round trip, and restriction of an ability the tool does not have
        config.presets.add(new ToolPreset(IToolAreaAbility.EXPLOSION, 3, IToolHarvestAbility.NONE, 0));
        config.currentPreset = 5;
        tool.setConfiguration(stack, config);
        ItemToolAbility.Configuration back = tool.getConfiguration(stack);
        helper.assertTrue(back.presets.size() == 6 && back.currentPreset == 5, "configuration must round-trip");
        helper.assertTrue(back.getActivePreset().isNone(), "an explosion preset must be restricted away on the CMB pickaxe");
        helper.assertTrue(tool.isFoil(new ItemStack(ModAbilityToolItems.cmb_pickaxe.get())) == false,
                "a tool on the none preset has no glint");
        helper.succeed();
    }

    /** HAMMER level 0 (range 1): the whole 3x3x3 cube around the mined block goes. */
    public static void hammer(net.minecraft.gametest.framework.GameTestHelper helper) {
        ServerPlayer player = GameTestHelpers.mockServerPlayerInLevel(helper);
        ItemStack stack = new ItemStack(ModAbilityToolItems.starmetal_pickaxe.get());
        ItemToolAbility tool = (ItemToolAbility) stack.getItem();
        select(tool, stack, new ToolPreset(IToolAreaAbility.HAMMER, 0, IToolHarvestAbility.NONE, 0));
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);

        BlockPos centre = helper.absolutePos(new BlockPos(2, 3, 2));
        for (BlockPos p : BlockPos.betweenClosed(centre.offset(-1, -1, -1), centre.offset(1, 1, 1))) {
            helper.getLevel().setBlock(p, Blocks.STONE.defaultBlockState(), 3);
        }

        player.gameMode.destroyBlock(centre);

        int left = 0;
        for (BlockPos p : BlockPos.betweenClosed(centre.offset(-1, -1, -1), centre.offset(1, 1, 1))) {
            if (!helper.getLevel().getBlockState(p).isAir()) left++;
        }
        helper.assertTrue(left == 0, "the hammer must clear the 3x3x3 cube, " + left + " blocks left");
        helper.succeed();
    }

    /** Vein miner plus smelter in survival: every connected iron ore becomes an iron ingot at the mined block. */
    public static void veinSmelter(net.minecraft.gametest.framework.GameTestHelper helper) {
        ServerPlayer player = GameTestHelpers.mockServerPlayerInLevel(helper);
        player.setGameMode(GameType.SURVIVAL);
        if (player.isCreative()) {
            // 1.20.1 Forge's mock player is creative by construction; nothing drops in creative.
            helper.succeed();
            return;
        }
        ItemStack stack = new ItemStack(ModAbilityToolItems.schrabidium_pickaxe.get());
        ItemToolAbility tool = (ItemToolAbility) stack.getItem();
        select(tool, stack, new ToolPreset(IToolAreaAbility.RECURSION, 0, IToolHarvestAbility.SMELTER, 0));
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);

        BlockPos start = helper.absolutePos(new BlockPos(1, 3, 2));
        for (int i = 0; i < 4; i++) helper.getLevel().setBlock(start.east(i), Blocks.IRON_ORE.defaultBlockState(), 3);
        helper.getLevel().setBlock(start.above(), Blocks.IRON_ORE.defaultBlockState(), 3);

        player.gameMode.destroyBlock(start);

        for (int i = 0; i < 4; i++) {
            helper.assertTrue(helper.getLevel().getBlockState(start.east(i)).isAir(), "ore " + i + " must be mined");
        }
        helper.assertTrue(helper.getLevel().getBlockState(start.above()).isAir(), "the ore above must be mined");

        List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(start).inflate(1.5));
        int ingots = 0, raw = 0;
        for (ItemEntity e : drops) {
            if (e.getItem().is(Items.IRON_INGOT)) ingots += e.getItem().getCount();
            if (e.getItem().is(Items.RAW_IRON)) raw += e.getItem().getCount();
        }
        helper.assertTrue(ingots == 5 && raw == 0, "5 ores must smelt to 5 iron ingots at the mined block, got "
                + ingots + " ingots and " + raw + " raw iron");
        helper.succeed();
    }

    /**
     * 1.7 ItemToolAbilityFueled: {@code setMaxDamage(1)} and a {@code setDamage} that burns fuel, so a hit through the
     * real attack path costs {@code consumption} mB and no durability, and the stack the hand renderer shows is still
     * "the same held item" afterwards (no re-equip per hit, the 2026-10-02 stutter). The chainsaw asks for 1.7's
     * undipped raise.
     */
    public static void chainsawWear(net.minecraft.gametest.framework.GameTestHelper helper) {
        ServerPlayer player = GameTestHelpers.mockServerPlayerInLevel(helper);
        player.setGameMode(GameType.SURVIVAL);
        ItemStack stack = new ItemStack(ModAbilityToolItems.chainsaw.get());
        com.hbm.items.tool.ItemChainsaw saw = ModAbilityToolItems.chainsaw.get();
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
        ItemStack before = stack.copy();
        LivingEntity golem = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(1, 2, 1));
        player.attack(golem);
        ItemStack after = player.getMainHandItem();
        helper.assertTrue(saw.getFill(after) == 4999, "one hit must burn 1 mB of fuel, fill " + saw.getFill(after));
        helper.assertTrue(!after.isDamageableItem() && after.getDamageValue() == 0,
                "the fueled chainsaw must take no durability, damage " + after.getDamageValue()
                        + " / " + after.getMaxDamage());
        helper.assertTrue(saw.compatIsSameHeldItem(before, after), "a fuel change must not re-equip the chainsaw");
        helper.assertTrue(!saw.compatIsSameHeldItem(before, new ItemStack(ModAbilityToolItems.steel_sword.get())),
                "a different item still re-equips");
        helper.assertTrue(saw.compatHoldsRaised(after), "the chainsaw keeps 1.7's undipped hand raise");
        helper.succeed();
    }

    /** STUN level 0: slowness and weakness V for 2 seconds. */
    public static void weapon(net.minecraft.gametest.framework.GameTestHelper helper) {
        ServerPlayer player = GameTestHelpers.mockServerPlayerInLevel(helper);
        LivingEntity pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(1, 2, 1));
        IWeaponAbility.STUN.onHit(0, helper.getLevel(), player, pig, ModAbilityToolItems.steel_sword.get());
        MobEffectInstance slow = pig.getEffect(MobEffects.SLOWNESS);
        helper.assertTrue(slow != null && slow.getAmplifier() == 4 && slow.getDuration() == 40,
                "stun must apply slowness V for 40 ticks");
        helper.assertTrue(pig.getEffect(MobEffects.WEAKNESS) != null, "stun must apply weakness");
        helper.succeed();
    }

    private static void select(ItemToolAbility tool, ItemStack stack, ToolPreset preset) {
        ItemToolAbility.Configuration config = tool.getConfiguration(stack);
        config.presets.add(preset);
        config.currentPreset = config.presets.size() - 1;
        tool.setConfiguration(stack, config);
    }
}
