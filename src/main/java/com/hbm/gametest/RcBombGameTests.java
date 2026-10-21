package com.hbm.gametest;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModExplosivesBlocks;
import com.hbm.blocks.ModFluidNetBlocks;
import com.hbm.blocks.ModCustomMissileBlocks;
import com.hbm.blocks.ModOreBlocks;
import com.hbm.blocks.ModTerrainBlocks;
import com.hbm.blocks.bomb.BlockFissureBomb;
import com.hbm.blocks.bomb.ModBombBlocks;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.entity.explosive.EntityPrimedExplosive;
import com.hbm.entity.item.EntityTntNtm;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.items.ModMissileItems;
import com.hbm.tileentity.bomb.TileEntityLaunchPadBase;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.world.Explosions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Explosives audit: the figures and rules of 1.7 that no other test pins. */
public final class RcBombGameTests {

    private RcBombGameTests() {}

    /** 1.7: launch pad proxies forward the inventory; only slot 0 (the missile) is reachable and nothing comes out. */
    public static void padAutomation(GameTestHelper helper) {
        TileEntityLaunchPadBase.registerLaunchables();
        placePadAndCheck(helper, ModMachineBlocks.launch_pad.get(), new BlockPos(3, 1, 2), true);
        placePadAndCheck(helper, ModCustomMissileBlocks.launch_pad_large.get(), new BlockPos(14, 1, 10), false);
        helper.succeed();
    }

    private static void placePadAndCheck(GameTestHelper helper, Block block, BlockPos clicked, boolean small) {
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        player.setYRot(0F);
        helper.setBlock(clicked, block.defaultBlockState());
        block.setPlacedBy(helper.getLevel(), helper.absolutePos(clicked), block.defaultBlockState(), player,
                new ItemStack(block));
        int offset = small ? 1 : 4;
        BlockPos core = helper.absolutePos(clicked.offset(0, 0, offset));
        BlockPos cell = small ? core.offset(1, 0, 1) : core.offset(2, 0, 3);
        BlockState cellState = helper.getLevel().getBlockState(cell);
        WorldlyContainer container = ((WorldlyContainerHolder) block)
                .getContainer(cellState, helper.getLevel(), cell);
        helper.assertTrue(container != null, block + ": the item-carrying cell " + cell + " has no container");
        helper.assertTrue(container.getSlotsForFace(Direction.NORTH).length == 1
                        && container.getSlotsForFace(Direction.NORTH)[0] == 0,
                block + ": automation must reach exactly the missile slot");
        ItemStack missile = new ItemStack(ModMissileItems.missile_generic.get());
        helper.assertTrue(container.canPlaceItemThroughFace(0, missile, Direction.NORTH),
                block + ": a launchable missile is refused");
        helper.assertFalse(container.canPlaceItemThroughFace(0, new ItemStack(Items.STONE), Direction.NORTH),
                block + ": stone accepted as a missile");
        helper.assertFalse(container.canPlaceItemThroughFace(1, missile, Direction.NORTH),
                block + ": the designator slot took a missile");
        helper.assertFalse(container.canTakeItemThroughFace(0, missile, Direction.DOWN),
                block + ": automation can pull the missile out");
        if (small) {
            BlockPos edge = core.offset(1, 0, 0);
            helper.assertTrue(((WorldlyContainerHolder) block)
                            .getContainer(helper.getLevel().getBlockState(edge), helper.getLevel(), edge) == null,
                    "the middle of the small pad's edge is no proxy and must not expose the inventory");
        }
    }

    /** 1.7 BlockFissureBomb: bedrock ore within 5 blocks becomes a volcano vent, bedrock oil becomes bedrock. */
    public static void fissureBombSweep(GameTestHelper helper) {
        BlockPos centre = new BlockPos(5, 5, 5);
        helper.setBlock(centre.offset(2, 0, 0), ModOreBlocks.ore_bedrock_block.get().defaultBlockState());
        helper.setBlock(centre.offset(-5, -5, 5), ModOreBlocks.ore_bedrock_block.get().defaultBlockState());
        helper.setBlock(centre.offset(0, 3, 0), ModBlocks.ore_bedrock_oil.get().defaultBlockState());
        helper.setBlock(centre.offset(6, 0, 0), ModOreBlocks.ore_bedrock_block.get().defaultBlockState());
        BlockFissureBomb.convertBedrockOres(helper.getLevel(), helper.absolutePos(centre).getX() + 0.5,
                helper.absolutePos(centre).getY() + 0.5, helper.absolutePos(centre).getZ() + 0.5);
        helper.assertTrue(helper.getBlockState(centre.offset(2, 0, 0)).is(ModTerrainBlocks.ore_volcano.get()),
                "bedrock ore in range did not become ore_volcano");
        helper.assertTrue(helper.getBlockState(centre.offset(-5, -5, 5)).is(ModTerrainBlocks.ore_volcano.get()),
                "bedrock ore on the edge of the 11-cube did not become ore_volcano");
        helper.assertTrue(helper.getBlockState(centre.offset(0, 3, 0)).is(Blocks.BEDROCK),
                "bedrock oil did not become bedrock");
        helper.assertTrue(helper.getBlockState(centre.offset(6, 0, 0)).is(ModOreBlocks.ore_bedrock_block.get()),
                "bedrock ore six blocks away is outside the sweep");
        helper.succeed();
    }

    /** The large launcher core is a plain block until its 9x9 ring and scaffold exist (1.7 TileEntityMultiblock). */
    public static void largeLauncherCoreStaysBare(GameTestHelper helper) {
        BlockPos relative = new BlockPos(5, 1, 5);
        helper.setBlock(relative, ModCustomMissileBlocks.struct_launcher_core_large.get().defaultBlockState());
        helper.runAfterDelay(110, () -> {
            helper.assertTrue(helper.getBlockState(relative).is(ModCustomMissileBlocks.struct_launcher_core_large.get()),
                    "a bare large launcher core vanished");
            helper.succeed();
        });
    }

    /** 1.7 createExplosion(entity, x, y, z, strength, true): the true is "smoking", the blast sets no fires. */
    public static void tntLeavesNoFire(GameTestHelper helper) {
        BlockPos floor = new BlockPos(2, 1, 2);
        for (int x = 0; x < 28; x++) {
            for (int z = 0; z < 28; z++) {
                helper.getLevel().setBlock(helper.absolutePos(floor.offset(x, -1, z)), Blocks.STONE.defaultBlockState(), 3);
            }
        }
        BlockPos centre = helper.absolutePos(new BlockPos(14, 2, 14));
        ModBombBlocks.c4.get().detonate(helper.getLevel(), centre.getX() + 0.5, centre.getY() + 0.5,
                centre.getZ() + 0.5, null);
        int fires = 0;
        for (int x = -20; x <= 20; x++) {
            for (int y = -4; y <= 6; y++) {
                for (int z = -20; z <= 20; z++) {
                    if (helper.getLevel().getBlockState(centre.offset(x, y, z)).is(Blocks.FIRE)) fires++;
                }
            }
        }
        helper.assertTrue(fires == 0, "a C-4 blast set " + fires + " fires");
        helper.succeed();
    }

    /** Three cells along each of +x, -x and +z, close enough that the 16-ray lattice always passes through them. */
    private static BlockPos spot(BlockPos centre, int i) {
        int step = 2 + i % 3;
        return switch (i / 3) {
            case 0 -> centre.offset(step, 0, 0);
            case 1 -> centre.offset(-step, 0, 0);
            default -> centre.offset(0, 0, step);
        };
    }

    /** 1.7 onBlockExploded: every detonatable block hit by a VNT blast becomes a primed explosive instead of vanishing. */
    public static void vntBlastChainsDetonatables(GameTestHelper helper) {
        Block[] blocks = {ModExplosivesBlocks.det_miner.get(), ModExplosivesBlocks.block_c4.get(), ModFluidNetBlocks.red_barrel.get(),
                ModBombBlocks.tnt.get(), ModBombBlocks.dynamite.get(), ModBombBlocks.semtex.get(), ModBombBlocks.c4.get(),
                ModExplosivesBlocks.det_charge.get(), ModExplosivesBlocks.det_cord.get()};
        BlockPos centre = helper.absolutePos(new BlockPos(10, 2, 10));
        for (int i = 0; i < blocks.length; i++) {
            helper.getLevel().setBlock(spot(centre, i), blocks[i].defaultBlockState(), 3);
        }
        ExplosionVNT blast = new ExplosionVNT(helper.getLevel(), centre.getX() + 0.5, centre.getY() + 0.5,
                centre.getZ() + 0.5, 12F);
        blast.setBlockAllocator(new BlockAllocatorStandard());
        blast.setBlockProcessor(new BlockProcessorStandard());
        blast.explode();
        for (int i = 0; i < blocks.length; i++) {
            BlockPos at = spot(centre, i);
            var primed = helper.getLevel().getEntitiesOfClass(Entity.class, new AABB(at),
                    e -> e instanceof EntityPrimedExplosive || e instanceof EntityTntNtm);
            helper.assertTrue(helper.getLevel().getBlockState(at).isAir() && primed.size() == 1,
                    blocks[i] + " was not turned into one primed explosive by a VNT blast (" + primed.size() + " entities, block now "
                            + helper.getLevel().getBlockState(at) + ")");
            primed.forEach(Entity::discard);
        }
        helper.succeed();
    }

    /** The same blast on the same spot: vanilla (factor 7) against Explosions (1.7: factor 8, whole points). */
    public static void legacyExplosionDamage(GameTestHelper helper) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 6; z++) {
                helper.getLevel().setBlock(helper.absolutePos(new BlockPos(1 + x, 1, 1 + z)), Blocks.STONE.defaultBlockState(), 3);
            }
        }
        BlockPos at = helper.absolutePos(new BlockPos(2, 2, 3));
        double cx = at.getX() + 0.5;
        double cy = at.getY() + 0.45;
        double cz = at.getZ() + 0.5;
        float vanilla = lostToBlast(helper, false, cx, cy, cz);
        float legacy = lostToBlast(helper, true, cx, cy, cz);
        double dist = Math.sqrt(81.0D + 0.45D * 0.45D);
        double v = 1.0D - dist / 10.0D;
        float expected = (int) ((v * v + v) / 2.0D * 8.0D * 10.0D + 1.0D);
        helper.assertTrue(Math.abs(legacy - expected) < 0.01F && legacy > vanilla,
                "vanilla blast cost " + vanilla + ", the 1.7 blast " + legacy + ", 1.7 deals " + expected);
        helper.succeed();
    }

    /** A blast sourced by a player's arrow kills with the 1.7 amount and the kill is credited to the shooter. */
    public static void projectileExplosionCredit(GameTestHelper helper) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 6; z++) {
                helper.getLevel().setBlock(helper.absolutePos(new BlockPos(1 + x, 1, 1 + z)), Blocks.STONE.defaultBlockState(), 3);
            }
        }
        BlockPos at = helper.absolutePos(new BlockPos(2, 2, 3));
        double cx = at.getX() + 0.5;
        double cy = at.getY() + 0.45;
        double cz = at.getZ() + 0.5;
        float vanilla = lostToBlast(helper, false, cx, cy, cz);
        Player shooter = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        Arrow arrow = new Arrow(EntityType.ARROW, helper.getLevel());
        arrow.setOwner(shooter);
        arrow.setPos(cx, cy, cz);
        LivingEntity pig = helper.spawn(EntityType.PIG, new BlockPos(11, 2, 3));
        if (pig instanceof Mob mob) mob.setNoAi(true);
        // The vanilla factor leaves the pig a sliver of health; only the 1.7 amount kills it.
        pig.setHealth(vanilla + 0.05F);
        Explosions.explode(helper.getLevel(), arrow, cx, cy, cz, 5F, Level.ExplosionInteraction.NONE);
        helper.assertTrue(pig.isDeadOrDying(), "the 1.7 blast did not kill a pig left with " + (vanilla + 0.05F) + " health");
        helper.assertTrue(pig.getKillCredit() == shooter, "the kill was credited to " + pig.getKillCredit() + ", not the shooter");
        pig.discard();
        helper.succeed();
    }

    private static float lostToBlast(GameTestHelper helper, boolean legacy, double cx, double cy, double cz) {
        LivingEntity pig = helper.spawn(EntityType.PIG, new BlockPos(11, 2, 3));
        if (pig instanceof Mob mob) mob.setNoAi(true);
        float before = pig.getHealth();
        if (legacy) {
            Explosions.explode(helper.getLevel(), null, cx, cy, cz, 5F, Level.ExplosionInteraction.NONE);
        } else {
            helper.getLevel().explode(null, cx, cy, cz, 5F, Level.ExplosionInteraction.NONE);
        }
        float lost = before - pig.getHealth();
        pig.discard();
        return lost;
    }
}
