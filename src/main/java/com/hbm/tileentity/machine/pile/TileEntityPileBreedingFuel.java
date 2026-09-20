package com.hbm.tileentity.machine.pile;

import dev.golem.core.data.Tags;

import com.hbm.api.block.IPileNeutronReceiver;
import com.hbm.tileentity.ModNuclearMiscBlockEntities;
import com.hbm.blocks.ModNuclearMiscBlocks;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.blocks.machine.pile.BlockGraphiteDrilledBase;

/** The legacy pile lithium cell: absorbs flux until it becomes a tritium cell. */
@Deprecated
public class TileEntityPileBreedingFuel extends TileEntityPileBase implements IPileNeutronReceiver {

    public int neutrons;
    public int lastNeutrons;
    public int progress;
    /** 1.7 {@code GeneralConfig.enable528 ? 50000 : 30000}; the port has no 528 mode. */
    public static final int maxProgress = 30000;

    public TileEntityPileBreedingFuel(BlockPos pos, BlockState state) {
        super(ModNuclearMiscBlockEntities.PILE_BREEDING_FUEL.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityPileBreedingFuel pile) {
        pile.react();

        if (pile.progress >= maxProgress) {
            BlockState current = level.getBlockState(pos);
            level.setBlock(pos, ModNuclearMiscBlocks.BLOCK_GRAPHITE_TRITIUM.get().defaultBlockState()
                    .setValue(BlockGraphiteDrilledBase.AXIS, current.getValue(BlockGraphiteDrilledBase.AXIS))
                    .setValue(BlockGraphiteDrilledBase.ALUMINUM, current.getValue(BlockGraphiteDrilledBase.ALUMINUM))
                    .setValue(BlockGraphiteDrilledBase.OUT, current.getValue(BlockGraphiteDrilledBase.OUT)), 3);
        }
    }

    private void react() {
        this.lastNeutrons = this.neutrons;
        this.progress += this.neutrons;
        this.neutrons = 0;

        if (lastNeutrons <= 0) return;

        for (int i = 0; i < 2; i++) this.castRay(1);
    }

    @Override
    public void receiveNeutrons(int n) {
        this.neutrons += n;
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        progress = input.getIntOr("progress", 0);
        neutrons = input.getIntOr("neutrons", 0);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putInt("progress", progress);
        output.putInt("neutrons", neutrons);
    }

    @Override
    public void writeSnapshot(CompoundTag tag) {
        tag.putInt("progress", progress);
        tag.putInt("neutrons", neutrons);
    }

    @Override
    public void readSnapshot(CompoundTag tag) {
        progress = Tags.getIntOr(tag, "progress", 0);
        neutrons = Tags.getIntOr(tag, "neutrons", 0);
    }
}
