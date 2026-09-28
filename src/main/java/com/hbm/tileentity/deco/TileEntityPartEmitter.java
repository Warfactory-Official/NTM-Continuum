package com.hbm.tileentity.deco;

import com.hbm.particle.helper.HbmEffectNT;
import com.hbm.tileentity.ModDecoCrateBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.7 {@code PartEmitter.TileEntityPartEmitter}: a particle source the hand drill cycles through four effects -
 * nothing, a gas flame four blocks up, a thin grey smoke column, and a wide low cooling-tower plume - sent to
 * everyone within {@value #range} blocks every tick.
 */
public class TileEntityPartEmitter extends TileEntityLoadedBase {

    public static final int range = 150;
    public int effect = 0;
    public static final int effectCount = 4;

    public TileEntityPartEmitter(BlockPos pos, BlockState state) {
        super(ModDecoCrateBlockEntities.PART_EMITTER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityPartEmitter emitter) {
        RandomSource rand = level.getRandom();
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        CompoundTag data = new CompoundTag();

        if (emitter.effect == 1) {
            // 1.7 ParticleUtil.spawnGasFlame: no scale, so the gasfire default; 150-block audience
            CompoundTag flame = new CompoundTag();
            flame.putDouble("moX", rand.nextGaussian() * 0.2);
            flame.putDouble("moY", 0.1);
            flame.putDouble("moZ", rand.nextGaussian() * 0.2);
            HbmEffectNT.GasFlame.send(level, pos.getX() + rand.nextDouble(), pos.getY() + 4.5 + rand.nextDouble(),
                    pos.getZ() + rand.nextDouble(), flame, 150);
        }

        if (emitter.effect == 2) {
            data.putString("type", "tower");
            data.putFloat("lift", 5F);
            data.putFloat("base", 0.25F);
            data.putFloat("max", 5F);
            data.putInt("life", 560 + rand.nextInt(20));
            data.putInt("color", 0x404040);
        }
        if (emitter.effect == 3) {
            data.putString("type", "tower");
            data.putFloat("lift", 0.5F);
            data.putFloat("base", 1F);
            data.putFloat("max", 10F);
            data.putInt("life", 750 + rand.nextInt(250));

            x = pos.getX() + 0.5 + rand.nextDouble() * 3 - 1.5;
            y = pos.getY() + 1;
            z = pos.getZ() + 0.5 + rand.nextDouble() * 3 - 1.5;
        }

        if (data.contains("type")) HbmEffectNT.Tower.send(level, x, y, z, data, range);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        this.effect = input.getIntOr("effect", 0);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putInt("effect", this.effect);
    }
}
