package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.world.chunk.ChunkLoadingManager;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.world.Protection;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * End to end with the real WarForge mod in the mods folder: the centre chunk of a crater is made a Safe Zone through
 * {@code /f zone safe}, a blast from nobody is fired over it. Skips when WarForge is absent.
 */
public final class WarforgeProtectionGameTests {

    private WarforgeProtectionGameTests() {}

    private static final int RADIUS = 40;
    private static final int OFFSET_X = -4 * 20_000;

    public static void safeZoneSurvivesBlast(GameTestHelper helper) {
        //? if neoforge && >=1.21 && <26.1 {
        /*if (!net.neoforged.fml.ModList.get().isLoaded("warforge")) {
            Constants.LOG.info("[WF-E2E] warforge absent, skipped");
            helper.succeed();
            return;
        }
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO).offset(OFFSET_X, RADIUS + 2, 0);
        int half = RADIUS;
        BlockState dirt = Blocks.DIRT.defaultBlockState();
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                int x = origin.getX() + dx, z = origin.getZ() + dz;
                LevelChunk chunk = level.getChunk(x >> 4, z >> 4);
                for (int dy = -half; dy <= 0; dy++) {
                    int fy = origin.getY() + dy;
                    chunk.getSection(chunk.getSectionIndex(fy)).setBlockState(x & 15, fy & 15, z & 15, dirt, false);
                }
            }
        }

        ServerPlayer player = new ServerPlayer(level.getServer(), level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "wf-e2e"),
                net.minecraft.server.level.ClientInformation.createDefault());
        player.setPos(origin.getX() + 0.5D, origin.getY() - 5, origin.getZ() + 0.5D);
        level.getServer().getPlayerList().op(player.getGameProfile());
        run(level, player, "f zone safe");
        int safeChunkX = origin.getX() >> 4, safeChunkZ = origin.getZ() >> 4;

        helper.assertTrue(!Protection.mayAlter(level, origin.below(3), Protection.Source.NONE, Protection.Cause.EXPLOSION),
                "WarForge did not close the safe-zone chunk (is /f zone safe applied?)");
        helper.assertTrue(Protection.mayAlter(level, origin.offset(64, 0, 0), Protection.Source.NONE, Protection.Cause.EXPLOSION),
                "unclaimed chunk was closed");
        helper.assertTrue(!Protection.mayHarm(level, player, Protection.Source.NONE, Protection.Hazard.RADIATION),
                "radiation may harm a player standing in the safe zone");

        ChunkLoadingManager.requestTicket(level, "warforge_e2e").force(safeChunkX, safeChunkZ);
        EntityNukeExplosionMK5 blast = EntityNukeExplosionMK5.statFacNoRad(
                level, RADIUS, origin.getX(), origin.getY(), origin.getZ());
        level.addFreshEntity(blast);
        poll(helper, 800, blast, () -> {
            int safeLost = 0, openLost = 0;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int dx = -half; dx <= half; dx++) {
                for (int dz = -half; dz <= half; dz++) {
                    int x = origin.getX() + dx, z = origin.getZ() + dz;
                    for (int dy = -half; dy <= 0; dy++) {
                        pos.set(x, origin.getY() + dy, z);
                        if (!level.getBlockState(pos).isAir()) continue;
                        if ((x >> 4) == safeChunkX && (z >> 4) == safeChunkZ) safeLost++;
                        else openLost++;
                    }
                }
            }
            Constants.LOG.info("[WF-E2E] safe chunk lost {} blocks, surrounding chunks lost {}", safeLost, openLost);
            helper.assertTrue(safeLost == 0, "safe zone chunk lost " + safeLost + " blocks");
            helper.assertTrue(openLost > 0, "the blast destroyed nothing outside the safe zone");
            run(level, player, "f zone remove");
            helper.succeed();
        });
        *///?} else {
        helper.succeed();
        //?}
    }

    /** The player has no connection, so the command's feedback packet throws after the command took effect. */
    private static void run(ServerLevel level, ServerPlayer player, String command) {
        //? if neoforge && >=1.21 && <26.1 {
        /*try {
            level.getServer().getCommands().performPrefixedCommand(
                    player.createCommandSourceStack().withPermission(4), command);
        } catch (NullPointerException expected) {
            Constants.LOG.info("[WF-E2E] /{} ran, feedback not deliverable", command);
        }
        *///?}
    }

    private static void poll(GameTestHelper helper, int tries, EntityNukeExplosionMK5 blast, Runnable then) {
        if (!blast.isAlive()) {
            then.run();
            return;
        }
        if (tries <= 0) {
            helper.fail("the blast did not finish");
            return;
        }
        blast.tick();
        helper.runAfterDelay(2, () -> poll(helper, tries - 1, blast, then));
    }
}
