package com.hbm.tileentity;

import com.hbm.entity.projectile.EntityShrapnel;
import com.hbm.blocks.ModFluidNetBlocks;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockMutatorLava;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.particle.helper.HbmEffectNT;
import dev.golem.core.data.CompatBlockEntity;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import com.hbm.blocks.bomb.BlockVolcano;

/**
 * 1.7 {@code BlockVolcano.TileEntityVolcanoCore}, ported with its timings: every 10 ticks the active
 * cores blast their vertical magma channel open and raise the magma level, the smoldering core blasts
 * a 15-block chamber and melts the surface, all but the smoldering one spew lava blobs and smoke, and
 * every core keeps a 3x3x3 lava sphere around itself. On {@code getUpdateRate} (288 ticks for the
 * growing metas, one hour for the static extinguishing one, 10 otherwise) a growing core climbs one
 * block by placing itself above and leaving lava behind (below y 200; the fresh core starts its timer
 * at 0), and an extinguishing one turns to lava instead. A growing <em>active</em> core never
 * extinguishes: past y 200 it erupts in place forever, as in 1.7.
 *
 * <p>The volcano itself is built by what the core throws, not by the core: the {@link EntityShrapnel}
 * blobs pool lava and monoxide where they land (rising ones blast a lava pocket into whatever roof they
 * hit), {@link #raiseMagma} stacks lava on top of pooled lava, and the volcanic lava solidifies into
 * basalt on its random tick. Deviation from 1.7/ntm-next: the port's volcanic lava is a static block
 * (see {@link com.hbm.blocks.fluid.BlockStaticFluid}), so the pools do not flow down the flanks.
 *
 * <p>{@code ExplosionNT} with {@code NODROP | LAVA_V/LAVA_R | NOSOUND | ALLMOD | NOHURT} is not in this
 * tree; {@link #lavaBlast} is the same blast on the {@link ExplosionVNT} pipeline, as ntm-next does it:
 * the standard ray allocator, the no-drop block processor and {@link BlockMutatorLava}, with no entity,
 * player or SFX processors (NOHURT drops the whole entity pass and NOSOUND the SFX; ALLMOD only gates the
 * scenery pass the lava attributes never take). The smoke column uses the ported cooling-tower puff in
 * place of the old scale-100 vanilla smoke, which needed reflection into the particle.
 */
public class TileEntityVolcanoCore extends CompatBlockEntity {

    /** 1.7 updateEntity: every arm but the growth/extinguish one is gated on {@code volcanoTimer % 10}. */
    public static final int CYCLE = 10;
    /** 1.7 getUpdateRate for the growing metas: 250 climbs per hour. */
    public static final int GROWING_UPDATE_RATE = 60 * 60 * 20 / 250;
    /** 1.7 shouldGrow: the core climbs while {@code yCoord < 200}. */
    public static final int MAX_GROW_Y = 200;

    public int volcanoTimer;

    public TileEntityVolcanoCore(BlockPos pos, BlockState state) {
        super(ModTerrainBlockEntities.VOLCANO_CORE.get(), pos, state);
    }

    private BlockVolcano.Mode mode() {
        return getBlockState().getBlock() instanceof BlockVolcano v ? v.mode : BlockVolcano.Mode.STATIC_ACTIVE;
    }

    public boolean isRadioactive() {
        return getBlockState().getBlock() instanceof BlockVolcano v && v.radioactive;
    }

    protected Block getLava() {
        return isRadioactive() ? ModFluidNetBlocks.rad_lava_block.get() : ModFluidNetBlocks.volcanic_lava_block.get();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityVolcanoCore core) {
        if (!(level instanceof ServerLevel world)) return;
        core.volcanoTimer++;

        if (core.volcanoTimer % CYCLE == 0) {
            if (core.hasVerticalChannel()) {
                core.blastMagmaChannel(world);
                core.raiseMagma(world);
            }

            double magmaChamber = core.magmaChamberSize();
            if (magmaChamber > 0) core.blastMagmaChamber(world, magmaChamber);

            if (core.mode() == BlockVolcano.Mode.SMOLDERING) core.meltSurface(world, 50, 50D, 10D);

            if (core.isSpewing()) core.spawnBlobs(world);
            if (core.isSmoking()) core.spawnSmoke(world);

            core.surroundLava(world);
        }

        if (core.volcanoTimer >= core.getUpdateRate()) {
            core.volcanoTimer = 0;

            if (core.shouldGrow()) {
                world.setBlock(pos.above(), state, 3);
                world.setBlockAndUpdate(pos, core.getLava().defaultBlockState());
            } else if (core.mode().extinguishing) {
                world.setBlockAndUpdate(pos, core.getLava().defaultBlockState());
            }
        }
    }

    @Override
    protected void loadData(DataIn in) {
        this.volcanoTimer = in.getIntOr("timer", 0);
    }

    @Override
    protected void saveData(DataOut out) {
        out.putInt("timer", this.volcanoTimer);
    }

    private boolean shouldGrow() {
        return mode().growing && worldPosition.getY() < MAX_GROW_Y;
    }

    private boolean isSmoking() { return mode() != BlockVolcano.Mode.SMOLDERING; }
    private boolean isSpewing() { return mode() != BlockVolcano.Mode.SMOLDERING; }
    private boolean hasVerticalChannel() { return mode() != BlockVolcano.Mode.SMOLDERING; }
    private double magmaChamberSize() { return mode() == BlockVolcano.Mode.SMOLDERING ? 15 : 0; }

    public int getUpdateRate() {
        return switch (mode()) {
            case STATIC_EXTINGUISHING -> 60 * 60 * 20;          // once per hour
            case GROWING_ACTIVE, GROWING_EXTINGUISHING -> GROWING_UPDATE_RATE; // 250x per hour
            default -> 10;
        };
    }

    /** Two magma explosions: one from bedrock to the core, one from the core to 15 blocks above. */
    private void blastMagmaChannel(ServerLevel world) {
        RandomSource rand = world.getRandom();
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        lavaBlast(world, getLava(), x + 0.5, y + rand.nextInt(15) + 1.5, z + 0.5, 7);
        // 1.7: rand.nextInt(y + 1), i.e. anywhere between bedrock (y 0) and the core; the world floor is negative now,
        // and Random.nextInt throws for a bound <= 0 when the core sits at or below y 0.
        int floor = world.getMinY();
        lavaBlast(world, getLava(), x + 0.5 + rand.nextGaussian() * 3, floor + rand.nextInt(Math.max(1, y - floor + 1)), z + 0.5 + rand.nextGaussian() * 3, 10);
    }

    /** Two magma explosions at random positions around the core, one at normal and one at half range. */
    private void blastMagmaChamber(ServerLevel world, double size) {
        RandomSource rand = world.getRandom();
        for (int i = 0; i < 2; i++) {
            double dist = size / (double) (i + 1);
            lavaBlast(world, getLava(), worldPosition.getX() + 0.5 + rand.nextGaussian() * dist,
                    worldPosition.getY() + 0.5 + rand.nextGaussian() * dist,
                    worldPosition.getZ() + 0.5 + rand.nextGaussian() * dist, 7);
        }
    }

    /** Randomly selects surface blocks and converts them into lava if solid or air if not solid. */
    private void meltSurface(ServerLevel world, int count, double radius, double depth) {
        RandomSource rand = world.getRandom();
        float obsidian = Blocks.OBSIDIAN.getExplosionResistance();
        for (int i = 0; i < count; i++) {
            int x = (int) Math.floor(worldPosition.getX() + rand.nextGaussian() * radius);
            int z = (int) Math.floor(worldPosition.getZ() + rand.nextGaussian() * radius);
            // gaussian distribution makes conversions more likely on the surface and rarer at the bottom
            int y = world.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) + 1 - (int) Math.floor(Math.abs(rand.nextGaussian() * depth));
            BlockPos at = new BlockPos(x, y, z);
            BlockState b = world.getBlockState(at);
            if (!b.isAir() && b.getBlock().getExplosionResistance() < obsidian) {
                world.setBlockAndUpdate(at, Compat.isSolidRender(b, world, at) ? getLava().defaultBlockState() : Blocks.AIR.defaultBlockState());
            }
        }
    }

    /** Increases the magma level in a small radius around the core. */
    private void raiseMagma(ServerLevel world) {
        RandomSource rand = world.getRandom();
        BlockPos at = new BlockPos(worldPosition.getX() - 10 + rand.nextInt(21),
                worldPosition.getY() + rand.nextInt(11), worldPosition.getZ() - 10 + rand.nextInt(21));
        if (world.getBlockState(at).isAir() && world.getBlockState(at.below()).is(getLava())) {
            world.setBlockAndUpdate(at, getLava().defaultBlockState());
        }
    }

    /** Creates a 3x3x3 lava sphere around the core. */
    private void surroundLava(ServerLevel world) {
        BlockState lava = getLava().defaultBlockState();
        for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) for (int k = -1; k <= 1; k++) {
            if (i != 0 || j != 0 || k != 0) world.setBlockAndUpdate(worldPosition.offset(i, j, k), lava);
        }
    }

    /** Spews specially tagged shrapnels which create volcanic lava and monoxide clouds. */
    private void spawnBlobs(ServerLevel world) {
        RandomSource rand = world.getRandom();
        for (int i = 0; i < 3; i++) {
            EntityShrapnel frag = new EntityShrapnel(world, worldPosition.getX() + 0.5, worldPosition.getY() + 1.5, worldPosition.getZ() + 0.5);
            frag.setDeltaMovement(rand.nextGaussian() * 0.2D, 1D + rand.nextDouble(), rand.nextGaussian() * 0.2D);
            if (isRadioactive()) frag.setRadVolcano(true); else frag.setVolcano(true);
            world.addFreshEntity(frag);
        }
    }

    /** I SEE SMOKE, AND WHERE THERE'S SMOKE THERE'S FIRE! (1.7: scale-100 vanilla smoke, 200-250 ticks, rising 2.5-3.5/t) */
    private void spawnSmoke(ServerLevel world) {
        CompoundTag data = new CompoundTag();
        data.putFloat("lift", 3F);
        data.putFloat("base", 7.5F);
        data.putFloat("max", 7.5F);
        data.putInt("life", 200 + world.getRandom().nextInt(50));
        data.putInt("color", 0x505050);
        HbmEffectNT.Tower.send(world, worldPosition.getX() + 0.5, worldPosition.getY() + 10, worldPosition.getZ() + 0.5, data, 250);
    }

    /**
     * The 1.7 {@code ExplosionNT} with {@code NODROP | LAVA_V/LAVA_R | NOSOUND | ALLMOD | NOHURT}, shared
     * with the volcano blobs ({@link EntityShrapnel}): vanilla's 16-ray fan (power
     * {@code size * (0.7 + rand * 0.6)}, step 0.3, cost {@code (resistance + 0.3) * step}), every hit
     * block removed without drops and refilled with {@code lava} where it was a full cube. No sound, no
     * entity damage. The core itself (resistance 10,000) never yields to a ray.
     */
    public static void lavaBlast(ServerLevel world, Block lava, double x, double y, double z, float size) {
        new ExplosionVNT(world, x, y, z, size)
                .setBlockAllocator(new BlockAllocatorStandard())
                .setBlockProcessor(new BlockProcessorStandard().setNoDrop().withBlockEffect(new BlockMutatorLava(lava)))
                .explode();
    }
}
