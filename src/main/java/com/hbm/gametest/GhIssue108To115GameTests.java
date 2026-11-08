package com.hbm.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hbm.Constants;
import com.hbm.blocks.ModFluidNetBlocks;
import com.hbm.blocks.ModPwrBlocks;
import com.hbm.blocks.machine.BlockPWR;
import com.hbm.blocks.turret.ModTurretBlocks;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.entity.ModEntities;
import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.missile.EntityMissileAntiBallistic;
import com.hbm.entity.missile.EntityMissileTier1.EntityMissileGeneric;
import com.hbm.entity.mob.EntityDuck;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.entity.projectile.EntitySawblade;
import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.inventory.recipes.CentrifugeRecipes;
import com.hbm.inventory.recipes.CrystallizerRecipes;
import com.hbm.items.ModArmorItems;
import com.hbm.items.ModArmorSetBItems;
import com.hbm.items.ModEntityItems;
import com.hbm.items.ModFoodItems;
import com.hbm.items.ModMachineItems;
import com.hbm.items.ModNuclearMiscItems;
import com.hbm.items.armor.ArmorFSBFueled;
import com.hbm.items.armor.ArmorFSBPowered;
import com.hbm.items.tool.ItemDrone;
import com.hbm.items.tool.ItemModMinecart;
import com.hbm.items.weapon.grenade.ItemGrenadeExtra;
import com.hbm.items.weapon.grenade.ItemGrenadeFilling;
import com.hbm.items.weapon.grenade.ItemGrenadeFuze;
import com.hbm.items.weapon.grenade.ItemGrenadeShell;
import com.hbm.items.weapon.sedna.BulletConfigs;
import com.hbm.items.weapon.sedna.factory.XFactoryCatapult;
import com.hbm.tileentity.machine.TileEntityBlockPWR;
import com.hbm.tileentity.machine.TileEntityMachineRadarNT;
import com.hbm.tileentity.machine.TileEntityPWRController;
import com.hbm.tileentity.turret.TileEntityTurretHoward;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.registry.Entities;
import dev.golem.core.world.Compat;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;

/** GitHub issues 108 to 119: each test states the 1.7 value or the crash it guards against. */
public final class GhIssue108To115GameTests {

    private GhIssue108To115GameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    public static final GameTests.Function CRATER_NAMES =
            TESTS.function("gh108_crater_biome_names", 20, () -> GhIssue108To115GameTests::craterBiomeNames);
    public static final GameTests.Function DUCK_FALL =
            TESTS.function("gh109_duck_no_fall_damage", 20, () -> GhIssue108To115GameTests::duckNoFallDamage);
    public static final GameTests.Function BARREL_TICK =
            TESTS.function("gh110_barrel_without_tick_starts_emitting", 60, () -> GhIssue108To115GameTests::barrelStartsEmitting);
    public static final GameTests.Function PWR_LOAD =
            TESTS.function("gh111_pwr_port_cell_loads_beside_controller", 20, () -> GhIssue108To115GameTests::pwrPortCellLoads);
    public static final GameTests.Function CERTUS_ROWS =
            TESTS.function("gh114_no_rows_with_an_empty_input", 20, () -> GhIssue108To115GameTests::noEmptyInputRows);
    public static final GameTests.Function BULLET_SPREAD =
            TESTS.function("gh115_bullet_scatter_is_the_config_spread", 20, () -> GhIssue108To115GameTests::bulletSpread);
    public static final GameTests.Function ARMOR_DRAIN =
            TESTS.function("gh140_suits_drain_through_1_7_armor_wear", 20, () -> GhIssue108To115GameTests::suitDrain);
    public static final GameTests.Function GRENADE_ROUNDS =
            TESTS.function("gh137_grenade_rounds_reach_clients", 20, () -> GhIssue108To115GameTests::grenadeRounds);
    public static final GameTests.Function CHIP_TAB =
            TESTS.function("gh117_turret_chip_is_in_the_weapons_tab", 20, () -> GhIssue108To115GameTests::chipInTab);
    public static final GameTests.Function TAB_AUDIT =
            TESTS.function("gh117_items_1_7_lists_are_in_a_tab", 20, () -> GhIssue108To115GameTests::tabAudit);
    public static final GameTests.Function CIWS_MISSILE =
            TESTS.function("gh117_ciws_tracks_a_descending_missile", 20, () -> GhIssue108To115GameTests::ciwsTracksMissile);
    public static final GameTests.Function ABM_ACTIVATION =
            TESTS.function("gh119_abm_climbs_40_ticks_then_locks_from_the_radar_list", 20, () -> GhIssue108To115GameTests::abmActivation);
    public static final GameTests.Function SAWBLADE_BOUNCE =
            TESTS.function("gh150_sawblade_bounces_and_breaks_soft_blocks", 40, 8,
                    () -> GhIssue108To115GameTests::sawbladeBounce);
    public static final GameTests.Function SAWBLADE_LANDS =
            TESTS.function("gh150_slow_sawblade_lands", 60, 8, () -> GhIssue108To115GameTests::sawbladeLands);

    public static void init() {}

    private static EntitySawblade sawblade(GameTestHelper helper, Vec3 at, Vec3 motion) {
        EntitySawblade blade = new EntitySawblade(ModEntities.SAWBLADE.get(), helper.getLevel());
        blade.setPos(helper.absoluteVec(at));
        blade.setDeltaMovement(motion);
        helper.getLevel().addFreshEntity(blade);
        return blade;
    }

    /** 1.7 {@code EntitySawblade.onImpact}: at 0.75 or faster it reverses off the face and breaks anything under 50. */
    public static void sawbladeBounce(GameTestHelper helper) {
        BlockPos wall = new BlockPos(6, 2, 2);
        helper.setBlock(wall, Blocks.OAK_PLANKS);
        helper.setBlock(wall.above(), Blocks.OAK_PLANKS);
        EntitySawblade blade = sawblade(helper, new Vec3(2.5D, 2.5D, 2.5D), new Vec3(1D, 0.05D, 0D));
        helper.succeedWhen(() -> {
            helper.assertTrue(blade.getDeltaMovement().x < 0D, "blade did not bounce: " + blade.getDeltaMovement());
            helper.assertTrue(helper.getBlockState(wall).isAir() || helper.getBlockState(wall.above()).isAir(),
                    "the planks it hit are still there");
        });
    }

    /** 1.7: slower than 0.75 it settles, adding 6 to its orientation, which stops its spin. */
    public static void sawbladeLands(GameTestHelper helper) {
        BlockPos floor = new BlockPos(2, 1, 2);
        helper.setBlock(floor, Blocks.STONE);
        EntitySawblade blade = sawblade(helper, new Vec3(2.5D, 3.5D, 2.5D), new Vec3(0D, -0.1D, 0D));
        helper.succeedWhen(() -> helper.assertTrue(blade.getOrientation() >= 6,
                "orientation " + blade.getOrientation() + " never gained the landed 6"));
    }

    /** 1.7 {@code setBiomeName}: Crater, Inner Crater, Outer Crater. */
    public static void craterBiomeNames(GameTestHelper helper) {
        try (var in = GhIssue108To115GameTests.class.getResourceAsStream("/assets/hbm/lang/en_us.json")) {
            helper.assertTrue(in != null, "en_us.json is in the jar");
            JsonObject lang = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            helper.assertTrue("Crater".equals(lang.get("biome.hbm.crater").getAsString()), "crater");
            helper.assertTrue("Inner Crater".equals(lang.get("biome.hbm.crater_inner").getAsString()), "inner crater");
            helper.assertTrue("Outer Crater".equals(lang.get("biome.hbm.crater_outer").getAsString()), "outer crater");
        } catch (IOException e) {
            helper.fail("en_us.json: " + e);
        }
        helper.succeed();
    }

    /** 1.7 {@code EntityChicken.fall} does nothing, for the duck and for Quackos. */
    public static void duckNoFallDamage(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (EntityType<? extends EntityDuck> type : List.<EntityType<? extends EntityDuck>>of(
                ModNtmEntities.DUCK.get(), ModNtmEntities.QUACKOS.get())) {
            EntityDuck duck = Entities.create(type, level, EntitySpawnReason.MOB_SUMMONED);
            float health = duck.getHealth();
            duck.causeFallDamage(30.0D, 1.0F, level.damageSources().fall());
            helper.assertTrue(duck.getHealth() == health, type + " lost health to a fall");
        }
        helper.succeed();
    }

    /** A barrel generated with no scheduled tick: a random tick starts its 5 RAD per second, as onBlockAdded did in 1.7. */
    public static void barrelStartsEmitting(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos rel = new BlockPos(1, 2, 1);
        BlockPos pos = helper.absolutePos(rel);
        var block = ModFluidNetBlocks.yellow_barrel.get();
        helper.setBlock(rel, block.defaultBlockState());
        level.getBlockTicks().clearArea(new BoundingBox(pos));
        helper.assertFalse(level.getBlockTicks().hasScheduledTick(pos, block), "the tick was cleared");
        BlockState state = level.getBlockState(pos);
        helper.assertTrue(state.isRandomlyTicking(), "the barrel ticks randomly");
        double before = ChunkRadiationManager.proxy.getRadiation(level, pos);
        state.randomTick(level, pos, level.getRandom());
        helper.assertTrue(level.getBlockTicks().hasScheduledTick(pos, block), "the random tick starts the cycle");
        helper.runAfterDelay(45, () -> {
            helper.assertTrue(ChunkRadiationManager.proxy.getRadiation(level, pos) > before, "the barrel added radiation");
            helper.succeed();
        });
    }

    /** Assembled port cells load with their core in reach: binding its tanks to the cell crashed the world. */
    public static void pwrPortCellLoads(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos coreRel = new BlockPos(1, 2, 1), cellRel = new BlockPos(2, 2, 1);
        helper.setBlock(coreRel, ModPwrBlocks.PWR_CONTROLLER.get().defaultBlockState());
        helper.setBlock(cellRel, ModPwrBlocks.PWR_BLOCK.get().defaultBlockState().setValue(BlockPWR.PORT, true));
        BlockPos core = helper.absolutePos(coreRel);
        TileEntityPWRController controller = (TileEntityPWRController) level.getBlockEntity(core);
        TileEntityBlockPWR cell = (TileEntityBlockPWR) level.getBlockEntity(helper.absolutePos(cellRel));
        controller.assembled = true;
        cell.block = ModPwrBlocks.PWR_PORT.get();
        cell.coreX = core.getX();
        cell.coreY = core.getY();
        cell.coreZ = core.getZ();
        helper.assertTrue(cell.getAllTanks().length > 0, "the port cell answers with the controller's tanks");
        cell.clearRemoved();
        controller.clearRemoved();
        helper.succeed();
    }

    /** A row whose ore tag no mod fills (AE2 has no certus ore) has nothing to show and can never run. */
    public static void noEmptyInputRows(GameTestHelper helper) {
        for (var recipe : CentrifugeRecipes.recipes) {
            helper.assertFalse(recipe.input.getDisplayStacks().isEmpty(), "centrifuge " + recipe.name + " has no input");
        }
        CrystallizerRecipes.register();
        for (var recipe : CrystallizerRecipes.recipes) {
            helper.assertFalse(recipe.input.getDisplayStacks().isEmpty(), "crystallizer " + recipe.name + " has no input");
        }
        helper.succeed();
    }

    /** 1.7 {@code headingForceMult} 1: looking along z, sideways scatter has the standard deviation of the spread. */
    public static void bulletSpread(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ArmorStand shooter = Entities.create(EntityType.ARMOR_STAND, level, EntitySpawnReason.MOB_SUMMONED);
        shooter.setPos(helper.absolutePos(new BlockPos(1, 2, 1)).getCenter());
        shooter.setYRot(0F);
        shooter.setXRot(0F);
        float spread = XFactoryCatapult.nuke_tots.spread;
        helper.assertTrue(spread == 0.1F, "1.7 tiny tots spread is 0.1");
        int shots = 2000;
        double sum = 0D;
        for (int i = 0; i < shots; i++) {
            var bullet = new EntityBulletBaseMK4(shooter, XFactoryCatapult.nuke_tots, 1F, spread, 0D, 0D, 0D);
            double x = bullet.getDeltaMovement().x;
            sum += x * x;
        }
        double deviation = Math.sqrt(sum / shots);
        helper.assertTrue(deviation > 0.09D && deviation < 0.11D, "sideways scatter " + deviation + ", expected 0.1");
        helper.succeed();
    }

    /** A round whose config has no index reaches the client with none, and the client discards it. */
    private static void grenadeRounds(GameTestHelper helper) {
        helper.assertTrue(BulletConfigs.indexOf(ItemGrenadeFilling.fragmentation) >= 0, "fragmentation is not registered");
        helper.assertTrue(BulletConfigs.indexOf(ItemGrenadeFilling.pellets) >= 0, "pellets are not registered");
        helper.assertTrue(BulletConfigs.indexOf(ItemGrenadeFilling.pellets_heavy) >= 0, "heavy pellets are not registered");
        helper.assertTrue(BulletConfigs.indexOf(ItemGrenadeFilling.laser) >= 0, "the grenade's laser is not registered");
        helper.succeed();
    }

    /** 1.7 spends suits through vanilla armor wear: a player's damage / 4 (at least 1) times consumption, per piece. */
    private static void suitDrain(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        ItemStack suit = new ItemStack(ModArmorItems.t51_plate.get());
        ArmorFSBPowered powered = (ArmorFSBPowered) suit.getItem();
        player.setItemSlot(EquipmentSlot.CHEST, suit);
        helper.assertTrue(chargeLost(player, level, player.damageSources().mobAttack(zombie), 8F, powered, suit) == 2_000L, "an 8 damage hit costs 2 x consumption");
        helper.assertTrue(chargeLost(player, level, player.damageSources().mobAttack(zombie), 2F, powered, suit) == 1_000L, "a small hit costs at least 1 x consumption");
        helper.assertTrue(chargeLost(player, level, player.damageSources().generic(), 8F, powered, suit) == 0L, "damage that bypasses armor costs nothing");

        ItemStack steam = new ItemStack(ModArmorSetBItems.steamsuit_plate.get());
        ArmorFSBFueled fueled = (ArmorFSBFueled) steam.getItem();
        player.setItemSlot(EquipmentSlot.CHEST, steam);
        int fill = fueled.getFill(steam);
        player.setHealth(player.getMaxHealth());
        player.invulnerableTime = 0;
        Compat.hurt(player, level, player.damageSources().mobAttack(zombie), 8F);
        helper.assertTrue(fill - fueled.getFill(steam) == 2 * fueled.consumption, "fuel suits burn the same way, lost " + (fill - fueled.getFill(steam)));

        LivingEntity wearer = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 1));
        ItemStack worn = new ItemStack(ModArmorItems.t51_plate.get());
        wearer.setItemSlot(EquipmentSlot.CHEST, worn);
        long full = powered.getCharge(worn);
        Compat.hurt(wearer, level, wearer.damageSources().generic(), 1F);
        Compat.hurt(wearer, level, wearer.damageSources().mobAttack(zombie), 8F);
        helper.assertTrue(powered.getCharge(worn) == full, "only a player's armor wears in 1.7");
        helper.assertTrue(!new ItemStack(ModArmorItems.taurun_plate.get()).isDamageableItem(), "1.7 ArmorTaurun calls setMaxDamage(0)");
        helper.succeed();
    }

    private static long chargeLost(Player player, ServerLevel level, DamageSource source, float amount, ArmorFSBPowered suit, ItemStack stack) {
        long before = suit.getCharge(stack);
        player.setHealth(player.getMaxHealth());
        player.invulnerableTime = 0;
        Compat.hurt(player, level, source, amount);
        return before - suit.getCharge(stack);
    }

    /** JEI lists what the creative tabs show, so an item in no tab cannot be found in either. */
    private static void chipInTab(GameTestHelper helper) {
        boolean weapons = false;
        for (ItemStack stack : ModTabs.displayStacks(ModTabs.get(HbmTab.WEAPONS).getKey())) {
            weapons |= stack.is(ModMachineItems.turret_chip.get());
        }
        helper.assertTrue(weapons, "the turret chip is in 1.7's weapon tab");
        helper.succeed();
    }

    /** Items 1.7's creative tabs show, each family through its {@code getSubItems}. */
    private static void tabAudit(GameTestHelper helper) {
        List<ItemStack> weapons = ModTabs.displayStacks(ModTabs.get(HbmTab.WEAPONS).getKey());
        List<ItemStack> machines = ModTabs.displayStacks(ModTabs.get(HbmTab.MACHINES).getKey());
        List<ItemStack> consumables = ModTabs.displayStacks(ModTabs.get(HbmTab.CONSUMABLES).getKey());
        for (var shell : ItemGrenadeShell.EnumGrenadeShell.values()) assertListed(helper, weapons, ModEntityItems.grenade_shell.get(shell));
        for (var filling : ItemGrenadeFilling.EnumGrenadeFilling.values()) assertListed(helper, weapons, ModEntityItems.grenade_filling.get(filling));
        for (var fuze : ItemGrenadeFuze.EnumGrenadeFuze.values()) assertListed(helper, weapons, ModEntityItems.grenade_fuze.get(fuze));
        for (var extra : ItemGrenadeExtra.EnumGrenadeExtra.values()) assertListed(helper, weapons, ModEntityItems.grenade_extra.get(extra));
        for (var drone : ItemDrone.EnumDroneType.values()) assertListed(helper, machines, ModEntityItems.drone.get(drone));
        assertListed(helper, consumables, ModFoodItems.fooditem.get());
        assertListed(helper, consumables, ModFoodItems.med_schizophrenia.get());
        assertListed(helper, consumables, ModNuclearMiscItems.QUARTZ_PLUTONIUM.get());
        int carts = ItemModMinecart.subItems().size();
        helper.assertTrue(carts == 12, "1.7 lists 12 cart and chassis pairs, here " + carts);
        helper.succeed();
    }

    private static void assertListed(GameTestHelper helper, List<ItemStack> tab, Item item) {
        helper.assertTrue(tab.stream().anyMatch(stack -> stack.is(item)), item + " is in no tab 1.7 lists it in");
    }

    /** 1.7's flat 250 blocks with players on or off; a missile is a target only on its way down. */
    private static void ciwsTracksMissile(GameTestHelper helper) {
        TileEntityTurretHoward howard = new TileEntityTurretHoward(helper.absolutePos(new BlockPos(1, 1, 1)),
                ModTurretBlocks.turret_howard.get().defaultBlockState());
        howard.targetPlayers = true;
        helper.assertTrue(howard.getDetectorRange() == 250D, "range with players on " + howard.getDetectorRange());
        EntityMissileGeneric missile = new EntityMissileGeneric(helper.getLevel(), 0F, 100F, 0F, 300, 0);
        helper.assertTrue(!howard.entityAcceptableTarget(missile), "a climbing missile is not a target");
        missile.setDeltaMovement(0, -1, 0);
        helper.assertTrue(howard.entityAcceptableTarget(missile), "a descending missile is a target");
        helper.succeed();
    }

    /** The missile sits below the ABM, which a box from the ABM's height upward cannot see. */
    private static void abmActivation(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos at = helper.absolutePos(new BlockPos(1, 2, 1));
        EntityMissileGeneric missile = new EntityMissileGeneric(level, at.getX(), at.getY(), at.getZ(), at.getX() + 300, at.getZ());
        level.addFreshEntity(missile);
        TileEntityMachineRadarNT.updateSystem(level.getServer());
        EntityMissileAntiBallistic abm = new EntityMissileAntiBallistic(level);
        abm.setPos(at.getX(), at.getY() + 3, at.getZ());
        for (int i = 0; i < 40; i++) abm.tick();
        helper.assertTrue(abm.tracking == null && abm.getDeltaMovement().x == 0D, "steered inside the 40-tick climb");
        helper.assertTrue(abm.getY() > at.getY() + 100, "climbed to " + (abm.getY() - at.getY()));
        abm.tick();
        helper.assertTrue(abm.tracking == missile, "must lock onto the radar list's missile, has " + abm.tracking);
        abm.discard();
        missile.discard();
        helper.succeed();
    }
}
