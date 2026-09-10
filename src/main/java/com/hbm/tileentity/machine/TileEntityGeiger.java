package com.hbm.tileentity.machine;

import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The placed Geiger counter: samples the chunk dose every ten ticks and clicks at a rate that rises with
 * it. Ported from 1.12.2's {@code TileEntityGeiger}.
 *
 * <p>The click ladder is the same one {@link com.hbm.items.tool.ItemGeigerCounter} runs, but fed from the
 * <i>environment</i> rather than from a carrier's absorbed dose, so a block in a hot chunk rattles while
 * the player holding a counter may not. The sampled reading is what the comparator and the
 * redstone-over-radio {@code rad} value both report, so all three agree on one number per sample.
 */
public class TileEntityGeiger extends TileEntityLoadedBase implements IRORValueProvider {

    public int timer;
    public double ticker;

    public TileEntityGeiger(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GEIGER.get(), pos, state);
    }

    public double check() {
        return ChunkRadiationManager.proxy.getRadiation(level, worldPosition);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityGeiger geiger) {
        geiger.timer++;

        if (geiger.timer == 10) {
            geiger.timer = 0;
            geiger.ticker = geiger.check();
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
        }

        if (geiger.timer % 5 != 0) return;

        if (geiger.ticker > 0) {
            List<Integer> list = new ArrayList<>();
            if (geiger.ticker < 1) list.add(0);
            if (geiger.ticker < 5) list.add(0);
            if (geiger.ticker < 10) list.add(1);
            if (geiger.ticker > 5 && geiger.ticker < 15) list.add(2);
            if (geiger.ticker > 10 && geiger.ticker < 20) list.add(3);
            if (geiger.ticker > 15 && geiger.ticker < 25) list.add(4);
            if (geiger.ticker > 20 && geiger.ticker < 30) list.add(5);
            if (geiger.ticker > 25) list.add(6);

            int r = list.get(level.getRandom().nextInt(list.size()));
            if (r > 0) play(level, pos, ModSounds.GEIGER_SOUNDS[r - 1].get());
        } else if (level.getRandom().nextInt(50) == 0) {
            play(level, pos, ModSounds.GEIGER_SOUNDS[level.getRandom().nextInt(1)].get());
        }
    }

    private static void play(Level level, BlockPos pos, net.minecraft.sounds.SoundEvent sound) {
        level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), sound, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[]{PREFIX_VALUE + "rad"};
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "rad").equals(name)) return "" + (int) Math.ceil(ticker);
        return null;
    }
}
