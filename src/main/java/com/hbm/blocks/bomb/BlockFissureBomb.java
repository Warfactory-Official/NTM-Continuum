package com.hbm.blocks.bomb;

import com.hbm.entity.explosive.EntityPrimedExplosive;
import com.hbm.explosion.ExplosionNukeSmall;
import net.minecraft.world.level.Level;

/**
 * 1.7 {@code BlockFissureBomb}: a TNT-family block whose primed entity goes off as a medium mini-nuke
 * ({@link ExplosionNukeSmall#PARAMS_MEDIUM}). Upstream then sweeps an 11-cube around the blast turning
 * {@code ore_bedrock} into {@code ore_volcano} (crater-biome variant inside a crater) and
 * {@code ore_bedrock_oil} into bedrock; neither bedrock ore nor the volcano ore is ported, so that
 * sweep is deferred with them.
 */
public class BlockFissureBomb extends BlockTNTBase {

    public static final int KIND = 4;

    public BlockFissureBomb(Properties properties) {
        super(properties);
    }

    @Override public int kindId() { return KIND; }
    @Override public String textureName() { return "fissure_bomb"; }
    @Override public float blastStrength() { return 0F; }

    @Override
    public void detonate(Level level, double x, double y, double z, EntityPrimedExplosive primed) {
        ExplosionNukeSmall.explode(level, x, y, z, ExplosionNukeSmall.PARAMS_MEDIUM);
        // Deferred: ore_bedrock -> ore_volcano / ore_bedrock_oil -> bedrock within range 5 (see class comment).
    }
}
