package com.hbm.tileentity.machine.pile;

import com.hbm.config.GeneralConfig;
import dev.golem.core.data.Tags;

import com.hbm.api.block.IPileNeutronReceiver;
import com.hbm.blocks.ModNuclearBlocks;
import com.hbm.tileentity.ModNuclearMiscBlockEntities;
import com.hbm.blocks.ModNuclearMiscBlocks;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.blocks.machine.pile.BlockGraphiteDrilledBase;

/** The legacy pile uranium fuel cell: reacts to incoming flux, heats, breeds towards plutonium. */
@Deprecated
public class TileEntityPileFuel extends TileEntityPileBase implements IPileNeutronReceiver {

    public int heat;
    public static final int maxHeat = 1000;
    public int neutrons;
    public int lastNeutrons;
    public int progress;
    /** 1.7 {@code GeneralConfig.enable528 ? 75000 : 50000}, fixed when the class loads, as in 1.7. */
    public static final int maxProgress = GeneralConfig.enable528 ? 75000 : 50000;

    public TileEntityPileFuel(BlockPos pos, BlockState state) {
        super(ModNuclearMiscBlockEntities.PILE_FUEL.get(), pos, state);
        // pu239 rods cringe :( (1.7 createNewTileEntity with meta bit 8 set)
        if (state.hasProperty(BlockGraphiteDrilledBase.OUT) && state.getValue(BlockGraphiteDrilledBase.OUT)) {
            progress = maxProgress - 1000;
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityPileFuel pile) {
        pile.dissipateHeat();
        pile.checkRedstone(pile.react());
        pile.transmute();

        if (pile.heat >= maxHeat) {
            level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4, true, Level.ExplosionInteraction.BLOCK);
            level.setBlockAndUpdate(pos, ModNuclearBlocks.gas_radon_dense.get().defaultBlockState());
            return;
        }

        if (level.getRandom().nextFloat() * 2F <= pile.heat / (float) maxHeat && level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.25 + level.getRandom().nextDouble() * 0.5,
                    pos.getY() + 1, pos.getZ() + 0.25 + level.getRandom().nextDouble() * 0.5, 1, 0, 0.05, 0, 0);
        }

        if (pile.progress >= maxProgress) {
            BlockState current = level.getBlockState(pos);
            level.setBlock(pos, ModNuclearMiscBlocks.BLOCK_GRAPHITE_PLUTONIUM.get().defaultBlockState()
                    .setValue(BlockGraphiteDrilledBase.AXIS, current.getValue(BlockGraphiteDrilledBase.AXIS))
                    .setValue(BlockGraphiteDrilledBase.ALUMINUM, current.getValue(BlockGraphiteDrilledBase.ALUMINUM)), 3);
        }
    }

    private void dissipateHeat() {
        // remove 5% of the stored heat per tick; 6.5% for windscale
        this.heat -= getBlockState().getValue(BlockGraphiteDrilledBase.ALUMINUM) ? heat * 0.065 : heat * 0.05;
    }

    private int react() {
        // max heat reduces reaction by 50% due to thermal expansion
        int reaction = (int) (this.neutrons * (1D - ((double) this.heat / (double) maxHeat) * 0.5D));

        this.lastNeutrons = this.neutrons;
        this.neutrons = 0;

        int lastProgress = this.progress;
        this.progress += reaction;

        if (reaction <= 0) return lastProgress;

        this.heat += reaction;

        for (int i = 0; i < 12; i++) this.castRay((int) Math.max(reaction * 0.25, 1));

        return lastProgress;
    }

    private void checkRedstone(int lastProgress) {
        int lastLevel = Mth.clamp((lastProgress * 16) / maxProgress, 0, 15);
        int newLevel = Mth.clamp((progress * 16) / maxProgress, 0, 15);
        if (lastLevel != newLevel && level != null) level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
    }

    private void transmute() {
        BlockState state = getBlockState();
        if (state.getValue(BlockGraphiteDrilledBase.OUT)) {
            // Might be subject to change, but 1000 seems like a good number.
            if (this.progress < maxProgress - 1000) this.progress = maxProgress - 1000;
        } else if (this.progress >= maxProgress - 1000) {
            level.setBlock(worldPosition, state.setValue(BlockGraphiteDrilledBase.OUT, true), 3);
        }
    }

    @Override
    public void receiveNeutrons(int n) {
        this.neutrons += n;
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        heat = input.getIntOr("heat", 0);
        progress = input.getIntOr("progress", 0);
        neutrons = input.getIntOr("neutrons", 0);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putInt("heat", heat);
        output.putInt("progress", progress);
        output.putInt("neutrons", neutrons);
    }

    @Override
    public void writeSnapshot(CompoundTag tag) {
        tag.putInt("heat", heat);
        tag.putInt("progress", progress);
        tag.putInt("neutrons", neutrons);
    }

    @Override
    public void readSnapshot(CompoundTag tag) {
        heat = Tags.getIntOr(tag, "heat", 0);
        progress = Tags.getIntOr(tag, "progress", 0);
        neutrons = Tags.getIntOr(tag, "neutrons", 0);
    }
}
