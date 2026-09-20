package com.hbm.blocks.bomb;

import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.effect.EntityEMPBlast;
import com.hbm.explosion.ExplosionChaos;
import com.hbm.explosion.ExplosionNukeGeneric;
import com.hbm.interfaces.IBomb;
import com.hbm.sound.ModGunSounds;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code BombFloat}, which served two blocks: the Levitation Bomb ({@code float_bomb}) lifts every block
 * in a 15-sphere fifty blocks up and shoves entities with it, while the EMP Device ({@code emp_bomb}) drains
 * machines in a 50-block radius and spawns the expanding EMP wave. Both play the spark-shot sound first.
 */
public class BombFloat extends CompatBlock implements IBomb {

    private final boolean emp;

    public BombFloat(Properties properties, boolean emp) {
        super(properties);
        this.emp = emp;
    }

    public boolean isEmp() {
        return emp;
    }

    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block block, boolean movedByPiston) {
        if (level.hasNeighborSignal(pos)) explode(level, pos, null);
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, @Nullable Entity detonator) {
        // 1.7: "hbm:weapon.sparkShoot" at volume 5 - the sounds.json entry picked one of three files.
        level.playSound(null, pos, ModGunSounds.SPARK_SHOOT[level.getRandom().nextInt(ModGunSounds.SPARK_SHOOT.length)].get(),
                SoundSource.BLOCKS, 5.0F, level.getRandom().nextFloat() * 0.2F + 0.9F);

        if (!level.isClientSide()) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            if (!emp) {
                ExplosionChaos.floater(level, pos, 15, 50);
                ExplosionChaos.move(level, pos, 15, 0, 50, 0);
            } else {
                ExplosionNukeGeneric.empBlast(level, pos, 50);
                EntityEMPBlast wave = new EntityEMPBlast(ModNtmEntities.EMP_BLAST.get(), level);
                wave.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                wave.setMaxAge(50);
                level.addFreshEntity(wave);
            }
        }
        return BombReturnCode.DETONATED;
    }
}
