package com.hbm.entity.projectile;

import com.hbm.entity.ModNtmEntities;
import com.hbm.blocks.ModFalloutBlocks;
import com.hbm.blocks.ModFallingBlocks;
import com.hbm.config.WorldConfig;
import dev.golem.core.data.SynchedData;
import dev.golem.core.data.CompatEntity;
import com.hbm.client.ClientEffects;
import com.hbm.client.ClientPlayerAccess;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.sound.ModSounds;
import com.hbm.util.DropHeight;
import com.hbm.world.feature.Meteorite;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.world.phys.Vec3;


/**
 * 1.7 entity/projectile/EntityMeteor - the falling meteor of the meteor-strike/shower event: carves a 5-radius channel below y=260, then explodes and buries a Meteorite crater. safe = the diverted meteor, no terrain damage.
 */
public class EntityMeteor extends CompatEntity {

    public boolean safe = false;
    private AudioWrapper audioFly;

    // 1.7: ignoreFrustumCheck is renderer-side in 26.2
    public EntityMeteor(EntityType<? extends EntityMeteor> type, Level level) {
        super(type, level);
    }

    public EntityMeteor(Level level) {
        this(ModNtmEntities.METEOR.get(), level);
    }

    @Override
    protected void defineData(SynchedData builder) {
        // 1.7: entityInit empty; safe is server-side only
    }

    @Override
    public void tick() {
        if (!level().isClientSide() && !WorldConfig.enableMeteorStrikes) {
            discard();
            return;
        }

        this.xo = getX();
        this.yo = getY();
        this.zo = getZ();

        Vec3 motion = getDeltaMovement();
        setDeltaMovement(motion.x, Math.max(motion.y - 0.03D, -2.5D), motion.z);
        move(MoverType.SELF, getDeltaMovement());

        // 1.7: spawn at y=384, carve on the way down
        if (level() instanceof ServerLevel server
                && getY() < DropHeight.clearOfTerrain(server, 260, getX(), getZ())) {
            clearMeteorPath(server, (int) getX(), (int) getY(), (int) getZ());

            if (onGround()) {
                server.explode(this, null, null, getX(), getY(), getZ(), 5F + random.nextFloat(), false,
                        safe ? Level.ExplosionInteraction.NONE : Level.ExplosionInteraction.TNT);

                if (WorldConfig.enableMeteorTails) {
                    ExplosionLarge.spawnRubble(server, getX(), getY(), getZ(), 15);

                    ExplosionLarge.spawnParticles(server, getX(), getY() + 5, getZ(), 75);
                    ExplosionLarge.spawnParticles(server, getX() + 5, getY(), getZ(), 75);
                    ExplosionLarge.spawnParticles(server, getX() - 5, getY(), getZ(), 75);
                    ExplosionLarge.spawnParticles(server, getX(), getY(), getZ() + 5, 75);
                    ExplosionLarge.spawnParticles(server, getX(), getY(), getZ() - 5, 75);
                }

                // 1.7: both horizontal axes offset by motionZ - kept slip
                double motionZ = getDeltaMovement().z;
                int spawnPosX = (int) (Math.round(getX() - 0.5D) + (safe ? 0 : (motionZ * 4)));
                int spawnPosY = (int) Math.round(getY() - (safe ? 0 : 4));
                int spawnPosZ = (int) (Math.round(getZ() - 0.5D) + (safe ? 0 : (motionZ * 4)));

                new Meteorite().generate(server, random, new BlockPos(spawnPosX, spawnPosY, spawnPosZ), safe, true,
                        true);
                clearMeteorPath(server, spawnPosX, spawnPosY, spawnPosZ);

                // 1.12.2: category is CE's; volume is 1.7's attenuation range
                level().playSound(null, getX(), getY(), getZ(), ModSounds.OLD_EXPLOSION.get(), SoundSource.HOSTILE,
                        10000.0F, 0.5F + random.nextFloat() * 0.1F);

                discard();
            }
        }

        if (level().isClientSide()) {
            tickClientAudio();

            if (WorldConfig.enableMeteorTails) {
                // 1.7: ten motionless flame puffs, one gaussian scatter
                Vec3 step = getDeltaMovement();
                for (int i = 0; i < 10; i++) {
                    ClientEffects.spawnRocketFlame(level(),
                            getX() - step.x + random.nextGaussian(),
                            getY() - step.y + random.nextGaussian(),
                            getZ() - step.z + random.nextGaussian(),
                            1F, 0D, 0D, 0D, 300 + random.nextInt(50));
                }
            }
        }
    }

    private void tickClientAudio() {
        if (isRemoved()) {
            if (audioFly != null) audioFly.stopSound();
            return;
        }

        if (audioFly == null) {
            // 1.12.2: category again; 200 range, 10-tick keepalive
            audioFly = AudioSystem.getLoopedSound(ModSounds.METEORITE_FALLING_LOOP.get(), SoundSource.BLOCKS,
                    0F, 0F, 0F, 1F, 200F, 0.9F + random.nextFloat() * 0.2F, 10);
        }
        if (audioFly == null) return;

        if (audioFly.isPlaying()) {
            audioFly.keepAlive();
            audioFly.updateVolume(1F);
            audioFly.updatePosition((float) getX(), (float) (getY() + getBbHeight() / 2F), (float) getZ());
        } else {
            // gate on the local player, not the nearest
            Player player = ClientPlayerAccess.player();
            if (player != null && player.distanceToSqr(getX(), getY(), getZ()) < 210D * 210D) audioFly.startSound();
        }
    }

    // 1.7: getBlocksInRadius(5); walk order load-bearing (entity RNG)
    private void clearMeteorPath(ServerLevel level, int x, int y, int z) {
        int radius = 5;
        int rSq = radius * radius;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz <= rSq) {
                        damageOrDestroyBlock(level, pos.set(x + dx, y + dy, z + dz));
                    }
                }
            }
        }
    }

    private void damageOrDestroyBlock(ServerLevel level, BlockPos pos) {
        if (safe) return;

        BlockState state = level.getBlockState(pos);
        // 1.12.2: air short-circuit; 1.7's null test was never true
        if (state.isAir()) return;

        float hardness = state.getDestroySpeed(level, pos);

        // 26.2: vanilla taxonomy replaces 1.7's leaves/log meta spans - leaves2 (acacia, dark oak) and every
        // post-1.7 species now clear too, and #logs adds the stripped variants and the nether stems.
        if (state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS) || (hardness >= 0F && hardness <= 0.3F)) {
            level.removeBlock(pos, false);
        } else {
            if (hardness < 0F || hardness > 5F) return;

            if (random.nextInt(6) == 1) {
                // 26.2: vanilla taxonomy throughout. #dirt omits podzol (vanilla files it under #grass_blocks),
                // so podzol reaches the last arm and weathers to waste earth rather than 1.7's dead dirt.
                if (state.is(BlockTags.DIRT)) {
                    level.setBlockAndUpdate(pos, ModFallingBlocks.dirt_dead.get().defaultBlockState());
                // 26.2: #sand carries suspicious_sand, so a strike can bury an archaeology site.
                } else if (state.is(BlockTags.SAND)) {
                    level.setBlockAndUpdate(pos, random.nextInt(2) == 1
                            ? Blocks.SANDSTONE.defaultBlockState() : Blocks.GLASS.defaultBlockState());
                // 26.2: 1.7 has no stone variants at all; the tag adds granite/diorite/andesite and the
                // deepslate pair, so deepslate weathers to plain cobblestone rather than its own.
                } else if (state.is(BlockTags.BASE_STONE_OVERWORLD)) {
                    level.setBlockAndUpdate(pos, Blocks.COBBLESTONE.defaultBlockState());
                // 26.2: #grass_blocks is grass/podzol/mycelium, so the latter two weather here too.
                } else if (state.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK) /* 1.7: Blocks.grass */) {
                    level.setBlockAndUpdate(pos, ModFalloutBlocks.waste_earth.get().defaultBlockState());
                }
            }
        }
    }

    @Override
    protected boolean onHurt(DamageSource source, float amount) {
        return false;
    }

    // 1.7: isInRangeToRenderDist always true - 4x4 box would cull
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    protected void loadData(DataIn input) {
        this.safe = input.getBooleanOr("safe", false);
    }

    @Override
    protected void saveData(DataOut output) {
        output.putBoolean("safe", safe);
    }
}
