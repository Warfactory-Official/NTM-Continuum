package com.hbm.compat.happyghast;

import com.hbm.Constants;
import com.hbm.blocks.bomb.ModBombBlocks;
import com.hbm.blocks.bomb.NukeMan;
import com.hbm.items.ModBombItems;
import com.hbm.tileentity.bomb.TileEntityNukeMan;
import dev.golem.core.gametest.GameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import com.hbm.config.BombConfig;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The happy-ghast Fat Man. Every check reads a value: the entity's parts and heading, the block's facing and readiness,
 * the armed flag and who is blamed, the height the ghast lifted the bomb to, the distance a dropped bomb fell. No test
 * lets a bomb land: an armed bomb is discarded in the tick it is checked.
 */
final class HappyGhastGameTests {

    private HappyGhastGameTests() {}

    static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    /** Forces the suite's declarations; called while mods construct, before the registries fill. */
    static void init() {}

    private static final int PADDING = 8;

    static {
        TESTS.function("happy_ghast_rig_ready_fat_man", 20, PADDING, () -> HappyGhastGameTests::rigReadyFatMan);
        TESTS.function("happy_ghast_rig_needs_ready_fat_man", 20, PADDING, () -> HappyGhastGameTests::rigNeedsReady);
        TESTS.function("happy_ghast_rigged_fat_man_sets_back_down", 20, PADDING, () -> HappyGhastGameTests::setsBackDown);
        TESTS.function("happy_ghast_carries_fat_man", 160, PADDING, () -> HappyGhastGameTests::ghastCarries);
        TESTS.function("happy_ghast_shears_drop_fat_man", 40, PADDING, () -> HappyGhastGameTests::shearsDrop);
        TESTS.function("happy_ghast_player_lead_does_not_arm", 20, PADDING, () -> HappyGhastGameTests::playerLeadDoesNotArm);
        TESTS.function("happy_ghast_dropped_fat_man_detonates", 60, PADDING, () -> HappyGhastGameTests::droppedDetonates);
    }

    /** A Fat Man with every part in, at {@code rel}, facing legacy 3 (model yaw 90). */
    private static TileEntityNukeMan readyBomb(GameTestHelper helper, BlockPos rel, Player placer) {
        helper.setBlock(rel, ModBombBlocks.nuke_man.get().defaultBlockState().setValue(NukeMan.FACING, 3));
        TileEntityNukeMan nuke = (TileEntityNukeMan) helper.getBlockEntity(rel, TileEntityNukeMan.class);
        nuke.setItem(0, new ItemStack(ModBombItems.man_igniter.get()));
        for (int i = 1; i <= 4; i++) nuke.setItem(i, new ItemStack(ModBombItems.early_explosive_lenses.get()));
        nuke.setItem(5, new ItemStack(ModBombItems.man_core.get()));
        nuke.placerID = placer.getUUID();
        return nuke;
    }

    private static Player sneakingPlayer(GameTestHelper helper, ItemStack held) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        return player;
    }

    private static List<EntityRiggedFatMan> bombsAround(GameTestHelper helper, BlockPos abs) {
        return helper.getLevel().getEntitiesOfClass(EntityRiggedFatMan.class, new AABB(abs).inflate(4));
    }

    private static EntityRiggedFatMan looseBomb(GameTestHelper helper, Vec3 rel) {
        EntityRiggedFatMan bomb = new EntityRiggedFatMan(helper.getLevel(), List.of(), null, 0F);
        Vec3 abs = helper.absoluteVec(rel);
        bomb.setPos(abs.x, abs.y, abs.z);
        bomb.setNoGravity(true);
        helper.getLevel().addFreshEntity(bomb);
        return bomb;
    }

    private static HappyGhast ghast(GameTestHelper helper, Vec3 rel) {
        HappyGhast ghast = EntityType.HAPPY_GHAST.create(helper.getLevel(), EntitySpawnReason.MOB_SUMMONED);
        Vec3 abs = helper.absoluteVec(rel);
        ghast.snapTo(abs.x, abs.y, abs.z, 0F, 0F);
        ghast.setNoAi(true);
        ghast.setNoGravity(true);
        helper.getLevel().addFreshEntity(ghast);
        return ghast;
    }

    /**
     * Sneak + empty hand does nothing; sneak + a lead lifts a ready bomb: the block is gone, one entity holds all six
     * parts and the block's yaw, it is on the player's lead, and the lead was used up.
     */
    private static void rigReadyFatMan(GameTestHelper helper) {
        BlockPos rel = GameTests.paddedCentre(PADDING, 2);
        Player player = sneakingPlayer(helper, ItemStack.EMPTY);
        readyBomb(helper, rel, player);
        BlockPos abs = helper.absolutePos(rel);

        InteractionResult bare = HbmHappyGhast.tryRig(player, helper.getLevel(), InteractionHand.MAIN_HAND, abs);
        helper.assertTrue(bare == null, "sneak + empty hand was taken as a rig: " + bare);
        helper.assertTrue(helper.getLevel().getBlockState(abs).getBlock() instanceof NukeMan, "an empty hand lifted the bomb");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.LEAD, 2));
        InteractionResult result = HbmHappyGhast.tryRig(player, helper.getLevel(), InteractionHand.MAIN_HAND, abs);
        helper.assertTrue(result == InteractionResult.SUCCESS_SERVER, "the ready Fat Man was not rigged: " + result);
        helper.assertTrue(helper.getLevel().getBlockState(abs).isAir(), "the block is still there");
        List<EntityRiggedFatMan> bombs = bombsAround(helper, abs);
        helper.assertTrue(bombs.size() == 1, "expected one rigged Fat Man, found " + bombs.size());
        EntityRiggedFatMan bomb = bombs.getFirst();
        long filled = bomb.parts().stream().filter(s -> !s.isEmpty()).count();
        helper.assertTrue(filled == 6, "the rigged bomb carries " + filled + " of 6 parts");
        helper.assertTrue(bomb.parts().get(5).is(ModBombItems.man_core.get()), "slot 5 is not the core: " + bomb.parts().get(5));
        float expected = EntityRiggedFatMan.yRotForModelYaw(90F); // facing 3 is model yaw 90
        helper.assertTrue(Math.abs(Mth.wrapDegrees(bomb.getYRot() - expected)) < 0.01F,
                "facing 3 should give yRot " + expected + ", got " + bomb.getYRot());
        helper.assertTrue(!bomb.isFalling(), "a freshly rigged bomb is armed");
        helper.assertTrue(bomb.getLeashHolder() == player, "the rigged bomb is not on the player's lead");
        helper.assertTrue(player.getMainHandItem().getCount() == 1, "the lead was not used: " + player.getMainHandItem());
        bomb.discard();
        helper.succeed();
    }

    /** An incomplete bomb stays a block. */
    private static void rigNeedsReady(GameTestHelper helper) {
        BlockPos rel = GameTests.paddedCentre(PADDING, 2);
        Player player = sneakingPlayer(helper, new ItemStack(Items.LEAD));
        TileEntityNukeMan nuke = readyBomb(helper, rel, player);
        nuke.setItem(5, ItemStack.EMPTY);
        BlockPos abs = helper.absolutePos(rel);

        InteractionResult result = HbmHappyGhast.tryRig(player, helper.getLevel(), InteractionHand.MAIN_HAND, abs);
        helper.assertTrue(result == InteractionResult.FAIL, "an incomplete Fat Man answered " + result);
        helper.assertTrue(helper.getLevel().getBlockState(abs).getBlock() instanceof NukeMan, "the incomplete bomb left its block");
        helper.assertTrue(bombsAround(helper, abs).isEmpty(), "an incomplete bomb became an entity");
        helper.succeed();
    }

    /** Rigged and set back down: the same block, facing and parts, still ready. */
    private static void setsBackDown(GameTestHelper helper) {
        BlockPos rel = GameTests.paddedCentre(PADDING, 2);
        Player player = sneakingPlayer(helper, ItemStack.EMPTY);
        BlockPos abs = helper.absolutePos(rel);
        TileEntityNukeMan nuke = readyBomb(helper, rel, player);
        EntityRiggedFatMan bomb = HbmHappyGhast.rig(helper.getLevel(), abs, helper.getLevel().getBlockState(abs), nuke);

        InteractionResult result = bomb.interact(player, InteractionHand.MAIN_HAND, bomb.position());
        helper.assertTrue(result == InteractionResult.SUCCESS_SERVER, "sneak-use on the loose bomb answered " + result);
        helper.assertTrue(bomb.isRemoved(), "the entity outlived its block");
        BlockState state = helper.getLevel().getBlockState(abs);
        helper.assertTrue(state.getBlock() instanceof NukeMan, "no Fat Man block came back: " + state);
        helper.assertTrue(state.getValue(NukeMan.FACING) == 3, "facing 3 came back as " + state.getValue(NukeMan.FACING));
        TileEntityNukeMan back = (TileEntityNukeMan) helper.getBlockEntity(rel, TileEntityNukeMan.class);
        helper.assertTrue(back.isReady(), "the parts did not come back");
        helper.assertTrue(player.getUUID().equals(back.placerID), "the placer was lost");
        helper.succeed();
    }

    /**
     * A still ghast nine blocks up pulls the bomb off the ground on its four ropes, as it lifts a boat: the ropes'
     * ends start about 7.9 apart against vanilla's 6-block slack, so the bomb must rise until they are taut. It starts
     * a quarter turn off the ghast; the ropes, laid out like a boat's, must turn it to the ghast's heading, which puts
     * its nose (the entity's +Z) forward.
     */
    private static void ghastCarries(GameTestHelper helper) {
        HappyGhast ghast = ghast(helper, new Vec3(0.5, 11, 0.5));
        EntityRiggedFatMan bomb = looseBomb(helper, new Vec3(0.5, 2, 0.5));
        bomb.setNoGravity(false);
        bomb.setYRot(90F);
        bomb.setLeashedTo(ghast, true);
        double start = bomb.getY();
        helper.runAfterDelay(100, () -> {
            float off = Mth.wrapDegrees(bomb.getYRot() - ghast.getYRot());
            helper.assertTrue(Math.abs(off) < 15F, "the bomb hangs " + off + " degrees off the ghast's heading");
            double lifted = bomb.getY() - start;
            helper.assertTrue(bomb.isLeashed() && bomb.getLeashHolder() == ghast, "the leash came off");
            helper.assertTrue(lifted > 0.75, "the ghast lifted the bomb only " + lifted + " blocks");
            helper.assertTrue(!bomb.isFalling(), "a carried bomb armed itself");
            bomb.discard();
            ghast.discard();
            helper.succeed();
        });
    }

    /** Shears on a carried bomb: it arms, the shearer is blamed, and it falls by the custom nuke's drop rule. */
    private static void shearsDrop(GameTestHelper helper) {
        HappyGhast ghast = ghast(helper, new Vec3(0.5, 14, 0.5));
        EntityRiggedFatMan bomb = looseBomb(helper, new Vec3(0.5, 10, 0.5));
        bomb.setLeashedTo(ghast, true);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));

        InteractionResult result = bomb.interact(player, InteractionHand.MAIN_HAND, bomb.position());
        helper.assertTrue(result.consumesAction(), "shears on the carried bomb answered " + result);
        helper.assertTrue(!bomb.isLeashed(), "the shears did not cut the leash");
        helper.assertTrue(bomb.isFalling(), "a bomb cut from a happy ghast did not arm");
        double start = bomb.getY();
        helper.runAfterDelay(6, () -> {
            // EntityFallingNuke: v -= 0.05 a tick from rest, so six ticks fall 0.05 * (0+1+...+5) = 0.75 blocks
            double fell = start - bomb.getY();
            helper.assertTrue(fell > 0.5 && fell < 1.1, "the dropped bomb fell " + fell + " blocks in six ticks");
            helper.assertTrue(bomb.getXRot() > 0F, "the dropped bomb did not tip over");
            bomb.discard(); // never let it land
            ghast.discard();
            helper.succeed();
        });
    }

    /**
     * A dropped bomb lands and goes off as the Fat Man: at its landing point it leaves exactly the block's blast (one
     * MK5) and its cloud (one Torex), and is gone itself. Both are discarded the tick they are seen, before any crater.
     */
    private static void droppedDetonates(GameTestHelper helper) {
        BlockPos floor = GameTests.paddedCentre(PADDING, 1);
        helper.setBlock(floor, Blocks.STONE);
        EntityRiggedFatMan bomb = looseBomb(helper, Vec3.atBottomCenterOf(floor).add(0, 2.5, 0));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        bomb.setLeashedTo(ghast(helper, Vec3.atBottomCenterOf(floor).add(0, 9, 0)), true);
        bomb.shearOffAllLeashConnections(player);
        helper.assertTrue(bomb.isFalling(), "the sheared bomb did not arm");
        AABB around = new AABB(helper.absolutePos(floor)).inflate(3);
        helper.succeedWhen(() -> {
            helper.assertTrue(bomb.isRemoved(), "the bomb has not landed yet");
            List<EntityNukeExplosionMK5> blasts = helper.getLevel().getEntitiesOfClass(EntityNukeExplosionMK5.class, around);
            List<EntityNukeTorex> clouds = helper.getLevel().getEntitiesOfClass(EntityNukeTorex.class, around);
            blasts.forEach(Entity::discard);
            clouds.forEach(Entity::discard);
            helper.getLevel().getEntitiesOfClass(HappyGhast.class, around.inflate(8)).forEach(Entity::discard);
            helper.assertTrue(blasts.size() == 1, "the landed bomb left " + blasts.size() + " MK5 blasts");
            helper.assertTrue(clouds.size() == 1 || !BombConfig.enableNukeClouds, "the landed bomb left " + clouds.size() + " clouds");
            helper.assertTrue(blasts.getFirst().detonator != null && blasts.getFirst().detonator.equals(player.getUUID()),
                    "the blast does not blame the player who cut the bomb loose");
        });
    }

    /** Untying a bomb from a player (or a fence) never arms it. */
    private static void playerLeadDoesNotArm(GameTestHelper helper) {
        EntityRiggedFatMan bomb = looseBomb(helper, new Vec3(0.5, 2, 0.5));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        bomb.setLeashedTo(player, true);
        bomb.removeLeash();
        helper.assertTrue(!bomb.isFalling(), "a bomb untied from a player armed itself");
        bomb.discard();
        helper.succeed();
    }
}
