package com.hbm.tileentity.deco;

import com.hbm.blocks.generic.BlockGlyphid;
import com.hbm.config.MobConfig;
import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.mob.glyphid.EntityGlyphid;
import com.hbm.entity.mob.glyphid.EntityGlyphidBlaster;
import com.hbm.entity.mob.glyphid.EntityGlyphidBombardier;
import com.hbm.entity.mob.glyphid.EntityGlyphidBrawler;
import com.hbm.entity.mob.glyphid.EntityGlyphidDigger;
import com.hbm.entity.mob.glyphid.EntityGlyphidScout;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.tileentity.ModDecoCrateBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.registry.Entities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

/**
 * 1.7 {@code BlockGlyphidSpawner.TileEntityGlpyhidSpawner}: off peaceful, on its first tick and then every
 * {@link MobConfig#swarmCooldown} ticks, while the world holds fewer than {@link MobConfig#spawnMax} glyphids and
 * at most three are near (or always, for a radioactive hive), it spawns a swarm sized and composed by the local
 * soot pollution, each member tried at seven heights around the block. A non-radioactive hive in sooty enough
 * air may add a scout that goes off to found another.
 */
public class TileEntityGlyphidSpawner extends TileEntityLoadedBase {

    /** 1.7's {@code spawnMap}: each type with its {@code {base, soot scaling, soot threshold}} chances. */
    private static final List<Entry> SPAWN_MAP = new java.util.concurrent.CopyOnWriteArrayList<>();

    private record Entry(Supplier<? extends EntityType<? extends EntityGlyphid>> type, int[] chance) {}

    /**
     * Adds a glyphid kind to every hive's swarm roll, after 1.7's eight: {@code chance} is {@code {base, soot scaling,
     * soot threshold}} as in {@code MobConfig} (an array the addon may keep and change from its config). Call at mod
     * construction.
     */
    public static void addSwarmMember(Supplier<? extends EntityType<? extends EntityGlyphid>> type, int[] chance) {
        SPAWN_MAP.add(new Entry(type, chance));
    }

    static {
        // big thanks to martin for the suggestion of using functions
        SPAWN_MAP.add(new Entry(ModNtmEntities.GLYPHID, MobConfig.glyphidChance));
        SPAWN_MAP.add(new Entry(EntityGlyphidBombardier.TYPE, MobConfig.bombardierChance));
        SPAWN_MAP.add(new Entry(EntityGlyphidBrawler.TYPE, MobConfig.brawlerChance));
        SPAWN_MAP.add(new Entry(EntityGlyphidDigger.TYPE, MobConfig.diggerChance));
        SPAWN_MAP.add(new Entry(EntityGlyphidBlaster.TYPE, MobConfig.blasterChance));
        SPAWN_MAP.add(new Entry(ModNtmEntities.GLYPHID_BEHEMOTH, MobConfig.behemothChance));
        SPAWN_MAP.add(new Entry(ModNtmEntities.GLYPHID_BRENDA, MobConfig.brendaChance));
        SPAWN_MAP.add(new Entry(ModNtmEntities.GLYPHID_NUCLEAR, MobConfig.johnsonChance));
    }

    boolean initialSpawn = true;

    public TileEntityGlyphidSpawner(BlockPos pos, BlockState state) {
        super(ModDecoCrateBlockEntities.GLYPHID_SPAWNER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityGlyphidSpawner spawner) {
        if (level instanceof ServerLevel server && level.getDifficulty() != Difficulty.PEACEFUL) spawner.update(server, state);
    }

    private void update(ServerLevel level, BlockState state) {
        if (!(initialSpawn || level.getGameTime() % MobConfig.swarmCooldown == 0)) return;

        initialSpawn = false;
        setChanged();
        int count = 0;

        for (Entity e : level.getAllEntities()) {
            if (e instanceof EntityGlyphid) {
                count++;
                if (count >= MobConfig.spawnMax) return;
            }
        }

        BlockPos p = worldPosition;
        List<EntityGlyphid> list = level.getEntitiesOfClass(EntityGlyphid.class,
                new AABB(p.getX() - 5, p.getY() + 1, p.getZ() - 5, p.getX() + 6, p.getY() + 7, p.getZ() + 6));
        float soot = PollutionHandler.getPollution(level, p, com.hbm.handler.pollution.PollutionType.SOOT);

        int subtype = state.getValue(BlockGlyphid.TYPE).ordinal();
        if (list.size() <= 3 || subtype == EntityGlyphid.TYPE_RADIOACTIVE) {

            for (EntityGlyphid glyphid : createSwarm(level, soot, subtype)) {
                trySpawnEntity(level, glyphid);
            }

            // 1.7 tested !initialSpawn after clearing it, so a first-tick swarm may bring a scout as well
            if (!initialSpawn && level.getRandom().nextInt(MobConfig.scoutSwarmSpawnChance + 1) == 0
                    && soot >= MobConfig.scoutThreshold && subtype != EntityGlyphid.TYPE_RADIOACTIVE) {
                EntityGlyphidScout scout = Entities.createTriggered(ModNtmEntities.GLYPHID_SCOUT.get(), level);
                if (scout != null) {
                    if (subtype == 1) scout.setSubtype(EntityGlyphid.TYPE_INFECTED);
                    trySpawnEntity(level, scout);
                }
            }
        }
    }

    public void trySpawnEntity(ServerLevel level, EntityGlyphid glyphid) {
        double offsetX = glyphid.getRandom().nextGaussian() * 3;
        double offsetZ = glyphid.getRandom().nextGaussian() * 3;

        for (int i = 0; i < 7; i++) {
            glyphid.snapTo(worldPosition.getX() + 0.5 + offsetX, worldPosition.getY() - 2 + i,
                    worldPosition.getZ() + 0.5 + offsetZ, level.getRandom().nextFloat() * 360.0F, 0.0F);
            if (canSpawnHere(level, glyphid)) {
                level.addFreshEntity(glyphid);
                return;
            }
        }
    }

    /** 1.7 {@code EntityGlyphid.getCanSpawnHere}: not peaceful, no entity or block in the way, no liquid. */
    private static boolean canSpawnHere(ServerLevel level, EntityGlyphid glyphid) {
        return level.getDifficulty() != Difficulty.PEACEFUL && level.isUnobstructed(glyphid)
                && level.noCollision(glyphid) && !level.containsAnyLiquid(glyphid.getBoundingBox());
    }

    public List<EntityGlyphid> createSwarm(ServerLevel level, float soot, int meta) {
        Random rand = new Random();
        List<EntityGlyphid> currentSpawns = new ArrayList<>();
        int swarmAmount = (int) Math.min(MobConfig.baseSwarmSize * Math.max(MobConfig.swarmScalingMult * (soot / MobConfig.sootStep), 1), 10);
        int cap = 100;

        while (currentSpawns.size() <= swarmAmount && cap >= 0) {
            // (dys)functional programing
            for (Entry glyphid : SPAWN_MAP) {
                int[] chance = glyphid.chance();
                int adjustedChance = (int) (chance[0] + (chance[1] - chance[1] / Math.max(((soot + 1) / 3), 1)));
                if (soot >= chance[2] && rand.nextInt(100) <= adjustedChance) {
                    EntityGlyphid entity = Entities.createTriggered(glyphid.type().get(), level);
                    if (entity == null) continue;
                    if (meta == 1) entity.setSubtype(EntityGlyphid.TYPE_INFECTED);
                    if (meta == 2) entity.setSubtype(EntityGlyphid.TYPE_RADIOACTIVE);
                    currentSpawns.add(entity);
                }
            }
            cap--;
        }
        return currentSpawns;
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putBoolean("initialSpawn", initialSpawn);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        this.initialSpawn = input.getBooleanOr("initialSpawn", true);
    }
}
