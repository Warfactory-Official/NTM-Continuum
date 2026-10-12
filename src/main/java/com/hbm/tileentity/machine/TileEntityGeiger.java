package com.hbm.tileentity.machine;

import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.7 {@code TileEntityGeiger}: the placed counter samples the chunk radiation at its own block
 * ({@code ChunkRadiationManager.proxy.getRadiation}) every tenth tick of its own counter, then clicks on every fifth
 * tick of that counter ({@link #click}: {@code hbm:item.geiger1..6} from the ladder by the last sample, or geiger1 one
 * click tick in 50 with no radiation), at the block. The sample is what redstone-over-radio's {@code rad} reports
 * (rounded up); the comparator ({@link #comparatorLevel}) and the computer's {@code getRads} read the chunk live, as in
 * 1.7.
 *
 * <p>The counter and the sample are not saved (1.7 had no NBT here), so a reloaded counter is silent-by-dose until its
 * tenth tick. 1.7 kept both as {@code float}, and the ladder's strict comparisons and the comparator's
 * {@code ceil(rad / 5f)} are done in {@code float} here too.
 */
public class TileEntityGeiger extends TileEntityLoadedBase implements IRORValueProvider {

    public int timer;
    public float ticker;

    public TileEntityGeiger(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GEIGER.get(), pos, state);
    }

    /** 1.7 {@code check()}: the chunk radiation at this block, as a float. */
    public float check() {
        return (float) ChunkRadiationManager.proxy.getRadiation(level, worldPosition);
    }

    /**
     * hbm-space {@code TileEntityGeiger}: at a dose below 1 the counter clicks only with that probability
     * ({@code rand.nextFloat() > ticker} skips the click; ADDON-API-INVENTORY V-183). Off in plain HBM (1.7 clicks
     * on every fifth tick); NTM: Space turns it on.
     */
    public static boolean probabilisticClicks = false;

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityGeiger geiger) {
        geiger.timer++;

        if (geiger.timer == 10) {
            geiger.timer = 0;
            geiger.ticker = geiger.check();
            // 1.7: "To update the adjacent comparators"
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
        }

        if (geiger.timer % 5 == 0) {
            int r = click(geiger.ticker, level.getRandom());
            if (r > 0) play(level, pos, ModSounds.GEIGER_SOUNDS[r - 1].get());
        }
    }

    /**
     * 1.7 {@code updateEntity}'s choice on a click tick: the geiger sound number 1..6, or 0 for silence. Every draw is
     * from 1.7's {@code worldObj.rand}, in 1.7's order: hbm-space's gate, then the ladder pick; with no radiation the
     * 1-in-50 roll and then {@code 1 + rand.nextInt(1)} (always geiger1, but still a draw).
     */
    public static int click(float ticker, RandomSource rand) {
        if (ticker > 0) {
            if (probabilisticClicks && rand.nextFloat() > ticker) return 0;
            List<Integer> list = new ArrayList<>();
            if (ticker < 1) list.add(0);
            if (ticker < 5) list.add(0);
            if (ticker < 10) list.add(1);
            if (ticker > 5 && ticker < 15) list.add(2);
            if (ticker > 10 && ticker < 20) list.add(3);
            if (ticker > 15 && ticker < 25) list.add(4);
            if (ticker > 20 && ticker < 30) list.add(5);
            if (ticker > 25) list.add(6);
            return list.get(rand.nextInt(list.size()));
        } else if (rand.nextInt(50) == 0) {
            return 1 + rand.nextInt(1);
        }
        return 0;
    }

    /** 1.7 {@code GeigerCounter.getComparatorInputOverride}: +1 per 5 RAD/s, rounded up, 15 at 75 and above. */
    public static int comparatorLevel(float rad) {
        return Math.min((int) Math.ceil(rad / 5f), 15);
    }

    /** 1.7 {@code worldObj.playSoundEffect(xCoord, yCoord, zCoord, ...)}: at the block's corner, not its centre. */
    private static void play(Level level, BlockPos pos, SoundEvent sound) {
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
