package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.handler.ability.IToolAreaAbility;
import com.hbm.handler.ability.IToolHarvestAbility;
import com.hbm.handler.ability.IWeaponAbility;
import com.hbm.handler.ability.ToolPreset;
import com.hbm.items.ModAbilityToolItems;
import com.hbm.items.tool.ItemChainsaw;
import com.hbm.items.tool.ItemToolAbility;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.CentrifugeRecipes;
import com.hbm.inventory.recipes.CrystallizerRecipes;
import com.hbm.inventory.recipes.ShredderRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.ModToolItems;
import com.hbm.items.armor.ItemModDefuser;
import net.minecraft.world.entity.monster.Creeper;
import com.hbm.items.tool.ItemToolAbilityPower;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
    public static void presets(GameTestHelper helper) {
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
    public static void hammer(GameTestHelper helper) {
        ServerPlayer player = GameTestHelpers.mockServerPlayerInLevel(helper);
        ItemStack stack = new ItemStack(ModAbilityToolItems.starmetal_pickaxe.get());
        ItemToolAbility tool = (ItemToolAbility) stack.getItem();
        select(tool, stack, new ToolPreset(IToolAreaAbility.HAMMER, 0, IToolHarvestAbility.NONE, 0));
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);

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
    public static void veinSmelter(GameTestHelper helper) {
        ServerPlayer player = survivor(helper);
        ItemStack stack = new ItemStack(ModAbilityToolItems.schrabidium_pickaxe.get());
        ItemToolAbility tool = (ItemToolAbility) stack.getItem();
        select(tool, stack, new ToolPreset(IToolAreaAbility.RECURSION, 0, IToolHarvestAbility.SMELTER, 0));
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);

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
     * "the same held item" afterwards (no re-equip per hit). The chainsaw asks for 1.7's undipped raise.
     */
    public static void chainsawWear(GameTestHelper helper) {
        ServerPlayer player = survivor(helper);
        ItemStack stack = new ItemStack(ModAbilityToolItems.chainsaw.get());
        ItemChainsaw saw = ModAbilityToolItems.chainsaw.get();
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
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
    public static void weapon(GameTestHelper helper) {
        ServerPlayer player = GameTestHelpers.mockServerPlayerInLevel(helper);
        LivingEntity pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(1, 2, 1));
        IWeaponAbility.STUN.onHit(0, helper.getLevel(), player, pig, ModAbilityToolItems.steel_sword.get());
        MobEffectInstance slow = pig.getEffect(MobEffects.SLOWNESS);
        helper.assertTrue(slow != null && slow.getAmplifier() == 4 && slow.getDuration() == 40,
                "stun must apply slowness V for 40 ticks");
        helper.assertTrue(pig.getEffect(MobEffects.WEAKNESS) != null, "stun must apply weakness");
        helper.succeed();
    }

    // Release audit: the ability figures of 1.7 through the real destroy path.

    public static final GameTests.Function RC_HAMMER =
            TESTS.function("rc_tool_hammer_wear", 60, 14, () -> AbilityToolGameTests::rcHammerWear);
    public static final GameTests.Function RC_FLAT =
            TESTS.function("rc_tool_hammer_flat", 120, 14, () -> AbilityToolGameTests::rcHammerFlat);
    public static final GameTests.Function RC_VEIN =
            TESTS.function("rc_tool_vein_radius", 60, 14, () -> AbilityToolGameTests::rcVeinRadius);
    public static final GameTests.Function RC_HARVEST =
            TESTS.function("rc_tool_harvest_abilities", 120, 14, () -> AbilityToolGameTests::rcHarvest);
    public static final GameTests.Function RC_EXPLOSION =
            TESTS.function("rc_tool_explosion", 80, 24, () -> AbilityToolGameTests::rcExplosion);
    public static final GameTests.Function RC_POWER =
            TESTS.function("rc_tool_power_cost", 60, 14, () -> AbilityToolGameTests::rcPower);
    public static final GameTests.Function RC_DIG =
            TESTS.function("rc_tool_dig_rules", 40, () -> AbilityToolGameTests::rcDigRules);

    public static final GameTests.Function RC_DEFUSER =
            TESTS.function("rc_tool_defuser_creeper", 60, () -> AbilityToolGameTests::rcDefuser);

    /** Right-clicking a creeper with the defuser removes its fuse: a safety fuse drops, the creeper stops swelling. */
    public static void rcDefuser(GameTestHelper helper) {
        ServerPlayer player = survivor(helper);
        ItemStack stack = new ItemStack(ModToolItems.defuser.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        Creeper creeper = helper.spawnWithNoFreeWill(EntityType.CREEPER, new BlockPos(1, 2, 1));
        stack.interactLivingEntity(player, creeper, InteractionHand.MAIN_HAND);
        BlockPos at = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.assertTrue(dropped(helper, at, ModItems.safety_fuse.get()) == 1, "the defuser must make the creeper drop a safety fuse");
        helper.assertTrue(ItemModDefuser.isDefused(creeper), "the creeper must be marked defused");
        helper.succeed();
    }

    private static ServerPlayer survivor(GameTestHelper helper) {
        return GameTestHelpers.survivalServerPlayerInLevel(helper);
    }

    private static void fillCube(GameTestHelper helper, BlockPos centre, int r, Block block) {
        for (BlockPos p : BlockPos.betweenClosed(centre.offset(-r, -r, -r), centre.offset(r, r, r))) {
            helper.getLevel().setBlock(p, block.defaultBlockState(), 3);
        }
    }

    private static int solid(GameTestHelper helper, BlockPos centre, int r) {
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(centre.offset(-r, -r, -r), centre.offset(r, r, r))) {
            if (!helper.getLevel().getBlockState(p).isAir()) n++;
        }
        return n;
    }

    private static void clearDrops(GameTestHelper helper, BlockPos near) {
        for (ItemEntity e : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(near).inflate(12))) {
            e.discard();
        }
    }

    private static int dropped(GameTestHelper helper, BlockPos near, Item item) {
        int n = 0;
        for (ItemEntity e : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(near).inflate(12))) {
            if (e.getItem().is(item)) n += e.getItem().getCount();
        }
        return n;
    }

    private static ItemStack holding(ServerPlayer player, Item item, ToolPreset preset) {
        ItemStack stack = new ItemStack(item);
        select((ItemToolAbility) item, stack, preset);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return stack;
    }

    /** HAMMER level 1 is range 2 (1.7 {1, 2, 3, 4}): the 5x5x5 cube goes and each block costs one use. */
    public static void rcHammerWear(GameTestHelper helper) {
        ServerPlayer player = survivor(helper);
        ItemStack stack = holding(player, ModAbilityToolItems.starmetal_pickaxe.get(),
                new ToolPreset(IToolAreaAbility.HAMMER, 1, IToolHarvestAbility.NONE, 0));
        BlockPos c = helper.absolutePos(new BlockPos(6, 7, 6));
        fillCube(helper, c, 4, Blocks.STONE);
        player.gameMode.destroyBlock(c);
        int left = solid(helper, c, 4);
        helper.assertTrue(left == 729 - 125, "hammer range 2 must clear 125 of 729 blocks, left " + left);
        helper.assertTrue(helper.getLevel().getBlockState(c.offset(3, 0, 0)).is(Blocks.STONE), "range is 2, not 3");
        helper.assertTrue(stack.getDamageValue() == 125, "125 blocks cost 125 uses, wear " + stack.getDamageValue());
        helper.succeed();
    }

    /** HAMMER_FLAT clears the plane facing the player: 25 blocks for range 2, whatever the face. */
    public static void rcHammerFlat(GameTestHelper helper) {
        ServerPlayer player = survivor(helper);
        holding(player, ModAbilityToolItems.starmetal_pickaxe.get(),
                new ToolPreset(IToolAreaAbility.HAMMER_FLAT, 1, IToolHarvestAbility.NONE, 0));
        BlockPos c = helper.absolutePos(new BlockPos(6, 7, 6));
        for (Direction face : Direction.values()) {
            fillCube(helper, c, 4, Blocks.STONE);
            for (int i = 1; i <= 4; i++) helper.getLevel().setBlock(c.relative(face, i), Blocks.AIR.defaultBlockState(), 3);
            Vec3 eye = Vec3.atCenterOf(c).add(face.getStepX() * 3.0, face.getStepY() * 3.0, face.getStepZ() * 3.0);
            player.setPos(eye.x, eye.y - player.getEyeHeight(), eye.z);
            player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(c));
            player.gameMode.destroyBlock(c);
            int removed = 0;
            for (BlockPos p : BlockPos.betweenClosed(c.offset(-4, -4, -4), c.offset(4, 4, 4))) {
                BlockPos d = p.subtract(c);
                int along = d.getX() * face.getStepX() + d.getY() * face.getStepY() + d.getZ() * face.getStepZ();
                boolean shaft = along > 0 && d.distManhattan(BlockPos.ZERO) == along;
                boolean inPlane = along == 0 && Math.abs(d.getX()) <= 2 && Math.abs(d.getY()) <= 2 && Math.abs(d.getZ()) <= 2;
                boolean air = helper.getLevel().getBlockState(p).isAir();
                if (inPlane) {
                    helper.assertTrue(air, face + ": the plane block " + d + " must be gone");
                    removed++;
                } else if (!shaft) {
                    helper.assertTrue(!air, face + ": the block " + d + " outside the plane must stay");
                }
            }
            helper.assertTrue(removed == 25, face + ": the plane is 25 blocks, was " + removed);
        }
        helper.succeed();
    }

    /** RECURSION level 1 is radius 4: five ore blocks of a long line go, and stone never chains. */
    public static void rcVeinRadius(GameTestHelper helper) {
        ServerPlayer player = survivor(helper);
        holding(player, ModAbilityToolItems.cobalt_pickaxe.get(),
                new ToolPreset(IToolAreaAbility.RECURSION, 1, IToolHarvestAbility.NONE, 0));
        BlockPos start = helper.absolutePos(new BlockPos(2, 3, 2));
        for (int i = 0; i < 12; i++) helper.getLevel().setBlock(start.east(i), Blocks.IRON_ORE.defaultBlockState(), 3);
        helper.getLevel().setBlock(start.below(), Blocks.COAL_ORE.defaultBlockState(), 3);
        player.gameMode.destroyBlock(start);
        for (int i = 0; i < 12; i++) {
            boolean air = helper.getLevel().getBlockState(start.east(i)).isAir();
            helper.assertTrue(air == (i <= 4), "ore " + i + (air ? " was mined" : " survived") + ", radius 4 takes 0..4");
        }
        helper.assertTrue(helper.getLevel().getBlockState(start.below()).is(Blocks.COAL_ORE), "another ore must not follow");

        BlockPos row = helper.absolutePos(new BlockPos(2, 3, 5));
        for (int i = 0; i < 5; i++) helper.getLevel().setBlock(row.east(i), Blocks.STONE.defaultBlockState(), 3);
        player.gameMode.destroyBlock(row);
        int stone = 0;
        for (int i = 0; i < 5; i++) if (helper.getLevel().getBlockState(row.east(i)).is(Blocks.STONE)) stone++;
        helper.assertTrue(stone == 4, "the vein miner must not chain through stone (recursionStone false), left " + stone);
        helper.succeed();
    }

    /** What each harvest ability turns a mined block into, against the recipe tables 1.7 consulted. */
    public static void rcHarvest(GameTestHelper helper) {
        ServerPlayer player = survivor(helper);
        BlockPos at = helper.absolutePos(new BlockPos(6, 4, 6));

        // silk touch: the block itself
        holding(player, ModAbilityToolItems.schrabidium_pickaxe.get(),
                new ToolPreset(IToolAreaAbility.NONE, 0, IToolHarvestAbility.SILK, 0));
        helper.getLevel().setBlock(at, Blocks.DIAMOND_ORE.defaultBlockState(), 3);
        clearDrops(helper, at);
        player.gameMode.destroyBlock(at);
        helper.assertTrue(dropped(helper, at, Items.DIAMOND_ORE) == 1 && dropped(helper, at, Items.DIAMOND) == 0,
                "silk touch must drop the ore block itself");

        // luck: level index 5 is fortune 9, so ten ores give far more than ten gems
        holding(player, ModAbilityToolItems.schrabidium_pickaxe.get(),
                new ToolPreset(IToolAreaAbility.NONE, 0, IToolHarvestAbility.LUCK, 5));
        clearDrops(helper, at);
        for (int i = 0; i < 10; i++) {
            helper.getLevel().setBlock(at, Blocks.DIAMOND_ORE.defaultBlockState(), 3);
            player.gameMode.destroyBlock(at);
        }
        helper.assertTrue(dropped(helper, at, Items.DIAMOND) >= 25,
                "fortune 9 on ten diamond ores must give well over ten diamonds, got " + dropped(helper, at, Items.DIAMOND));

        // smelter: the drops of the block, smelted
        holding(player, ModAbilityToolItems.schrabidium_pickaxe.get(),
                new ToolPreset(IToolAreaAbility.NONE, 0, IToolHarvestAbility.SMELTER, 0));
        clearDrops(helper, at);
        helper.getLevel().setBlock(at, Blocks.IRON_ORE.defaultBlockState(), 3);
        player.gameMode.destroyBlock(at);
        holding(player, ModAbilityToolItems.schrabidium_axe.get(),
                new ToolPreset(IToolAreaAbility.NONE, 0, IToolHarvestAbility.SMELTER, 0));
        helper.getLevel().setBlock(at, Blocks.OAK_LOG.defaultBlockState(), 3);
        player.gameMode.destroyBlock(at);
        holding(player, ModAbilityToolItems.schrabidium_shovel.get(),
                new ToolPreset(IToolAreaAbility.NONE, 0, IToolHarvestAbility.SMELTER, 0));
        helper.getLevel().setBlock(at, Blocks.SAND.defaultBlockState(), 3);
        player.gameMode.destroyBlock(at);
        helper.assertTrue(dropped(helper, at, Items.IRON_INGOT) == 1, "smelter: iron ore must give an iron ingot");
        helper.assertTrue(dropped(helper, at, Items.CHARCOAL) == 1, "smelter: a log must give charcoal");
        helper.assertTrue(dropped(helper, at, Items.GLASS) == 1, "smelter: sand must give glass");

        // shredder: the shredder table's result, and a plain drop where it only has scrap
        holding(player, ModAbilityToolItems.schrabidium_pickaxe.get(),
                new ToolPreset(IToolAreaAbility.NONE, 0, IToolHarvestAbility.SHREDDER, 0));
        clearDrops(helper, at);
        ItemStack shredded = ShredderRecipes.getShredderResult(new ItemStack(Items.IRON_ORE));
        helper.assertTrue(!shredded.isEmpty() && !shredded.is(ModItems.scrap.get()), "iron ore must have a shredder recipe");
        helper.getLevel().setBlock(at, Blocks.IRON_ORE.defaultBlockState(), 3);
        player.gameMode.destroyBlock(at);
        helper.assertTrue(dropped(helper, at, shredded.getItem()) == shredded.getCount(),
                "shredder: iron ore must give " + shredded + ", got " + dropped(helper, at, shredded.getItem()));
        helper.assertTrue(dropped(helper, at, Items.RAW_IRON) == 0, "shredder: no raw iron beside the shredded result");
        clearDrops(helper, at);
        holding(player, ModAbilityToolItems.schrabidium_shovel.get(),
                new ToolPreset(IToolAreaAbility.NONE, 0, IToolHarvestAbility.SHREDDER, 0));
        Block plain = null;
        for (Block candidate : new Block[]{Blocks.CLAY, Blocks.SOUL_SAND, Blocks.MYCELIUM, Blocks.SNOW_BLOCK, Blocks.GRAVEL}) {
            ItemStack recipe = ShredderRecipes.getShredderResult(new ItemStack(candidate));
            if (recipe.isEmpty() || recipe.is(ModItems.scrap.get())) {
                plain = candidate;
                break;
            }
        }
        helper.assertTrue(plain != null, "some shovel block must lack a shredder recipe");
        helper.getLevel().setBlock(at, plain.defaultBlockState(), 3);
        player.gameMode.destroyBlock(at);
        helper.assertTrue(dropped(helper, at, ModItems.scrap.get()) == 0 && !helper.getLevel().getEntitiesOfClass(
                ItemEntity.class, new AABB(at).inflate(2)).isEmpty(),
                "shredder: " + plain + " only has scrap, so it drops its normal loot");

        // centrifuge
        holding(player, ModAbilityToolItems.chlorophyte_pickaxe.get(),
                new ToolPreset(IToolAreaAbility.NONE, 0, IToolHarvestAbility.CENTRIFUGE, 0));
        clearDrops(helper, at);
        ItemStack[] spun = CentrifugeRecipes.getOutput(new ItemStack(Items.IRON_ORE));
        helper.assertTrue(spun != null, "iron ore must have a centrifuge recipe");
        helper.getLevel().setBlock(at, Blocks.IRON_ORE.defaultBlockState(), 3);
        player.gameMode.destroyBlock(at);
        for (ItemStack out : spun) {
            if (out == null || out.isEmpty()) continue;
            int expected = 0;
            for (ItemStack other : spun) if (other != null && other.is(out.getItem())) expected += other.getCount();
            helper.assertTrue(dropped(helper, at, out.getItem()) == expected,
                    "centrifuge: " + out.getItem() + " x" + expected + ", got " + dropped(helper, at, out.getItem()));
        }
        helper.assertTrue(dropped(helper, at, Items.RAW_IRON) == 0, "centrifuge: no raw iron beside the products");

        // mercury touch: 4 to 8 for the ore, 8 to 14 for the block
        holding(player, ModAbilityToolItems.chlorophyte_pickaxe.get(),
                new ToolPreset(IToolAreaAbility.NONE, 0, IToolHarvestAbility.MERCURY, 0));
        for (int pass = 0; pass < 2; pass++) {
            Block block = pass == 0 ? Blocks.REDSTONE_ORE : Blocks.REDSTONE_BLOCK;
            int low = pass == 0 ? 4 : 8;
            int high = pass == 0 ? 8 : 14;
            for (int i = 0; i < 6; i++) {
                clearDrops(helper, at);
                helper.getLevel().setBlock(at, block.defaultBlockState(), 3);
                player.gameMode.destroyBlock(at);
                int got = dropped(helper, at, ModItems.nugget_mercury.get());
                helper.assertTrue(got >= low && got <= high, block + ": mercury " + low + ".." + high + ", got " + got);
            }
        }
        clearDrops(helper, at);
        helper.getLevel().setBlock(at, Blocks.DEEPSLATE_REDSTONE_ORE.defaultBlockState(), 3);
        player.gameMode.destroyBlock(at);
        helper.assertTrue(dropped(helper, at, ModItems.nugget_mercury.get()) >= 4,
                "mercury touch must also work on deepslate redstone ore");

        // crystallizer
        holding(player, ModAbilityToolItems.mese_pickaxe.get(),
                new ToolPreset(IToolAreaAbility.NONE, 0, IToolHarvestAbility.CRYSTALLIZER, 0));
        clearDrops(helper, at);
        CrystallizerRecipes.CrystallizerRecipe grown =
                CrystallizerRecipes.getOutput(new ItemStack(Items.IRON_ORE), Fluids.PEROXIDE);
        helper.assertTrue(grown != null, "iron ore must have a peroxide crystallizer recipe");
        helper.getLevel().setBlock(at, Blocks.IRON_ORE.defaultBlockState(), 3);
        player.gameMode.destroyBlock(at);
        helper.assertTrue(dropped(helper, at, grown.output.getItem()) == grown.output.getCount(),
                "crystallizer: iron ore must give " + grown.output + ", got " + dropped(helper, at, grown.output.getItem()));
        helper.succeed();
    }

    /** EXPLOSION level 3 is strength 15: a crater, every block dropped, nobody hurt. */
    public static void rcExplosion(GameTestHelper helper) {
        ServerPlayer player = survivor(helper);
        holding(player, ModAbilityToolItems.mese_pickaxe.get(),
                new ToolPreset(IToolAreaAbility.EXPLOSION, 3, IToolHarvestAbility.NONE, 0));
        BlockPos c = helper.absolutePos(new BlockPos(12, 12, 12));
        fillCube(helper, c, 10, Blocks.DIRT);
        player.setPos(c.getX() + 0.5, c.getY() + 12, c.getZ() + 0.5);
        float health = player.getHealth();
        player.gameMode.destroyBlock(c);
        int left = solid(helper, c, 10);
        int gone = 21 * 21 * 21 - left;
        helper.assertTrue(helper.getLevel().getBlockState(c).isAir(), "the mined block must be gone");
        helper.assertTrue(gone > 3000, "strength 15 must cut a crater past radius 10, removed " + gone);
        helper.assertTrue(player.getHealth() == health, "NOHURT: the miner takes no damage");
        helper.assertTrue(dropped(helper, c, Items.DIRT) > 100, "ALLDROP: the dirt must drop, got " + dropped(helper, c, Items.DIRT));
        helper.succeed();
    }

    /** 1.7 ItemToolAbilityPower: 100 HE per block, nothing happens below that, a hit costs one use too. */
    public static void rcPower(GameTestHelper helper) {
        ServerPlayer player = survivor(helper);
        ItemStack stack = holding(player, ModAbilityToolItems.elec_pickaxe.get(),
                new ToolPreset(IToolAreaAbility.HAMMER, 0, IToolHarvestAbility.NONE, 0));
        ItemToolAbilityPower tool = (ItemToolAbilityPower) stack.getItem();
        helper.assertTrue(tool.getCharge(stack) == 500_000L, "a fresh powered tool is full: " + tool.getCharge(stack));
        BlockPos c = helper.absolutePos(new BlockPos(5, 6, 5));
        fillCube(helper, c, 3, Blocks.STONE);
        player.gameMode.destroyBlock(c);
        helper.assertTrue(solid(helper, c, 3) == 343 - 27, "hammer range 1 clears 27 blocks");
        helper.assertTrue(tool.getCharge(stack) == 500_000L - 27 * 100, "27 blocks cost 2700 HE, charge " + tool.getCharge(stack));
        tool.setCharge(stack, 99);
        helper.assertTrue(!tool.canOperate(stack), "below the consumption the tool is dead");
        helper.assertTrue(stack.getDestroySpeed(Blocks.STONE.defaultBlockState()) <= 1.0F, "a dead powered tool digs at base speed");
        long before = solid(helper, c, 3);
        BlockPos next = c.offset(2, 0, 0);
        player.gameMode.destroyBlock(next);
        helper.assertTrue(before - solid(helper, c, 3) <= 1, "a dead tool must not run its hammer");
        helper.succeed();
    }

    /** Mining speed and tier gates: the figures of the 1.7 tool materials. */
    public static void rcDigRules(GameTestHelper helper) {
        float steel = new ItemStack(ModAbilityToolItems.steel_pickaxe.get()).getDestroySpeed(Blocks.STONE.defaultBlockState());
        helper.assertTrue(steel == 8.0F, "steel pickaxe digs stone at its material efficiency 8, got " + steel);
        float cmb = new ItemStack(ModAbilityToolItems.cmb_pickaxe.get()).getDestroySpeed(Blocks.STONE.defaultBlockState());
        helper.assertTrue(cmb == 40.0F, "CMB pickaxe digs stone at 40, got " + cmb);
        float dirtWithPick = new ItemStack(ModAbilityToolItems.steel_pickaxe.get()).getDestroySpeed(Blocks.DIRT.defaultBlockState());
        helper.assertTrue(dirtWithPick == 1.0F, "a pickaxe digs dirt at base speed, got " + dirtWithPick);
        float shovelStone = new ItemStack(ModAbilityToolItems.steel_shovel.get()).getDestroySpeed(Blocks.STONE.defaultBlockState());
        helper.assertTrue(shovelStone == 1.0F, "a shovel digs stone at base speed, got " + shovelStone);
        float minerBoth = new ItemStack(ModAbilityToolItems.bismuth_pickaxe.get()).getDestroySpeed(Blocks.DIRT.defaultBlockState());
        helper.assertTrue(minerBoth == 50.0F, "the miner type digs dirt too, at 50, got " + minerBoth);
        helper.assertTrue(new ItemStack(ModAbilityToolItems.titanium_pickaxe.get())
                .isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()), "harvest level 3 breaks obsidian");
        helper.assertTrue(!new ItemStack(ModAbilityToolItems.desh_pickaxe.get())
                .isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()), "harvest level 2 does not break obsidian");
        helper.assertTrue(new ItemStack(ModAbilityToolItems.desh_pickaxe.get())
                .isCorrectToolForDrops(Blocks.DIAMOND_ORE.defaultBlockState()), "harvest level 2 breaks diamond ore");
        helper.assertTrue(((ItemToolAbility) ModAbilityToolItems.bismuth_pickaxe.get()).canBreakRock(null, null,
                new ItemStack(ModAbilityToolItems.bismuth_pickaxe.get()), Blocks.STONE.defaultBlockState(), BlockPos.ZERO),
                "the bismuth pickaxe breaks depth rock");
        helper.assertTrue(!((ItemToolAbility) ModAbilityToolItems.starmetal_pickaxe.get()).canBreakRock(null, null,
                new ItemStack(ModAbilityToolItems.starmetal_pickaxe.get()), Blocks.STONE.defaultBlockState(), BlockPos.ZERO),
                "the starmetal pickaxe does not");
        helper.succeed();
    }

    private static void select(ItemToolAbility tool, ItemStack stack, ToolPreset preset) {
        ItemToolAbility.Configuration config = tool.getConfiguration(stack);
        config.presets.add(preset);
        config.currentPreset = config.presets.size() - 1;
        tool.setConfiguration(stack, config);
    }
}
