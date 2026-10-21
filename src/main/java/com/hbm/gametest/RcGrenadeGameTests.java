package com.hbm.gametest;

import com.hbm.entity.effect.EntityFireLingering;
import com.hbm.entity.grenade.EntityGrenadeUniversal;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.items.weapon.grenade.ItemGrenadeFilling.EnumGrenadeFilling;
import com.hbm.items.weapon.grenade.ItemGrenadeFuze.EnumGrenadeFuze;
import com.hbm.items.weapon.grenade.ItemGrenadeShell.EnumGrenadeShell;
import com.hbm.items.weapon.grenade.ItemGrenadeUniversal;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

/** Release sweep: every 1.7 universal grenade filling is thrown into a stone cavity and its fuse and effect measured. */
public final class RcGrenadeGameTests {

    private static final Logger LOG = LogUtils.getLogger();
    private static final int R = 14;

    private RcGrenadeGameTests() {}

    private static int solid(ServerLevel level, BlockPos c) {
        int n = 0;
        for (int x = -R; x <= R; x++) for (int y = -R; y <= R; y++) for (int z = -R; z <= R; z++) {
            if (!level.getBlockState(c.offset(x, y, z)).isAir()) n++;
        }
        return n;
    }

    private static void box(ServerLevel level, BlockPos c, boolean stone) {
        for (int x = -R; x <= R; x++) for (int y = -R; y <= R; y++) for (int z = -R; z <= R; z++) {
            boolean cavity = Math.abs(x) <= 2 && Math.abs(z) <= 2 && y >= 0 && y <= 4;
            level.setBlock(c.offset(x, y, z), stone && !cavity ? Blocks.STONE.defaultBlockState()
                    : Blocks.AIR.defaultBlockState(), 2);
        }
    }

    private static int count(ServerLevel level, BlockPos c, Class<? extends Entity> type) {
        return level.getEntitiesOfClass(type, new AABB(c).inflate(R + 10)).size();
    }

    /** Measures one grenade; returns "fuse=N destroyed=N bullets=N fires=N". */
    private static String run(GameTestHelper helper, EnumGrenadeShell shell, EnumGrenadeFilling filling,
            EnumGrenadeFuze fuze, int[] fuseOut, int[] destroyedOut, int[] bulletsOut, int[] firesOut) {
        ServerLevel level = helper.getLevel();
        BlockPos c = helper.absolutePos(new BlockPos(2, 2, 2)).offset(0, 30, 0);
        box(level, c, true);
        int before = solid(level, c);
        EntityGrenadeUniversal g = new EntityGrenadeUniversal(level, ItemGrenadeUniversal.make(shell, filling, fuze));
        g.setPos(c.getX() + 0.5D, c.getY() + 3D, c.getZ() + 0.5D);
        level.addFreshEntity(g);
        int fuse = -1;
        for (int t = 1; t <= 400; t++) {
            g.tick();
            if (g.isRemoved()) { fuse = t; break; }
        }
        int bullets = count(level, c, EntityBulletBaseMK4.class);
        int fires = count(level, c, EntityFireLingering.class);
        int destroyed = before - solid(level, c);
        fuseOut[0] = fuse;
        destroyedOut[0] = destroyed;
        bulletsOut[0] = bullets;
        firesOut[0] = fires;
        for (Entity e : level.getEntities((Entity) null, new AABB(c).inflate(R + 10))) e.discard();
        box(level, c, false);
        return "fuse=" + fuse + " destroyed=" + destroyed + " bullets=" + bullets + " fires=" + fires;
    }

    public static void grenadeFillings(GameTestHelper helper) {
        List<String> bad = new ArrayList<>();
        int[] fuse = new int[1], destroyed = new int[1], bullets = new int[1], fires = new int[1];
        for (EnumGrenadeFilling filling : EnumGrenadeFilling.values()) {
            for (EnumGrenadeShell shell : filling.compatibleShells) {
                try {
                    String r = run(helper, shell, filling, EnumGrenadeFuze.S3, fuse, destroyed, bullets, fires);
                    LOG.info("RCGREN {} {} {}", filling, shell, r);
                    if (fuse[0] < 59 || fuse[0] > 62) bad.add(filling + "/" + shell + " 3s fuse at tick " + fuse[0]);
                    if (filling == EnumGrenadeFilling.CLUSTER && bullets[0] != (shell == EnumGrenadeShell.FRAG ? 37 : 30)) {
                        bad.add("cluster " + shell + " pellets " + bullets[0]);
                    }
                    if (filling == EnumGrenadeFilling.CLUSTER_HEAVY && bullets[0] != 15) {
                        bad.add("heavy cluster pellets " + bullets[0]);
                    }
                    // only the demolition fillings carry a block processor
                    boolean demo = filling == EnumGrenadeFilling.DEMO || filling == EnumGrenadeFilling.NUCLEAR_DEMO;
                    if (demo != (destroyed[0] > 0)) bad.add(filling + " destroyed " + destroyed[0] + " blocks");
                    boolean fire = filling == EnumGrenadeFilling.INC || filling == EnumGrenadeFilling.WP;
                    if (fire && fires[0] < 1) bad.add(filling + " left no fire");
                } catch (RuntimeException e) {
                    bad.add(filling + "/" + shell + ": " + e);
                }
            }
        }
        for (EnumGrenadeFuze fz : new EnumGrenadeFuze[] {EnumGrenadeFuze.S7, EnumGrenadeFuze.S15}) {
            run(helper, EnumGrenadeShell.FRAG, EnumGrenadeFilling.POWDER, fz, fuse, destroyed, bullets, fires);
            int want = fz == EnumGrenadeFuze.S7 ? 140 : 300;
            LOG.info("RCGREN fuze {} exploded at tick {}", fz, fuse[0]);
            if (Math.abs(fuse[0] - want) > 2) bad.add(fz + " fuse tick " + fuse[0] + ", 1.7 " + want);
        }
        helper.assertTrue(bad.isEmpty(), "grenade sweep: " + bad);
        helper.succeed();
    }
}
