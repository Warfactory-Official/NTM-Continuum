package com.hbm.tileentity.machine.pile;

import dev.golem.core.data.Tags;

import com.hbm.api.block.IPileNeutronReceiver;
import com.hbm.tileentity.ModNuclearMiscBlockEntities;
import dev.golem.core.data.CompatBlockEntity;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.blocks.machine.pile.BlockGraphiteDrilledBase;
import com.hbm.blocks.machine.pile.BlockGraphiteNeutronDetector;

/**
 * The legacy pile detector rod: not a {@link TileEntityPileBase} (it casts nothing), it toggles the
 * neighbouring control rods when its flux crosses the configured limit.
 */
@Deprecated
public class TileEntityPileNeutronDetector extends CompatBlockEntity implements IPileNeutronReceiver {

    public int lastNeutrons;
    public int neutrons;
    public int maxNeutrons = 10;

    public TileEntityPileNeutronDetector(BlockPos pos, BlockState state) {
        super(ModNuclearMiscBlockEntities.PILE_NEUTRON_DETECTOR.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityPileNeutronDetector pile) {
        // lastNeutrons is used to reduce the responsiveness of control rods; should cut down on sound/updates
        // whilst keeping them still useful for automatic control. Even with it, the auto rods are *very*
        // subject to triggering on and off rapidly - this is necessary, as rays in smaller piles aren't
        // guaranteed to consistently flood all surrounding areas
        if (!(state.getBlock() instanceof BlockGraphiteNeutronDetector detector)) return;
        boolean out = state.getValue(BlockGraphiteDrilledBase.OUT);
        if (pile.neutrons >= pile.maxNeutrons && out) detector.triggerRods(level, pos);
        if (pile.neutrons < pile.maxNeutrons && pile.lastNeutrons < pile.maxNeutrons && !out) detector.triggerRods(level, pos);

        pile.lastNeutrons = pile.neutrons;
        pile.neutrons = 0;
    }

    @Override
    public void receiveNeutrons(int n) {
        this.neutrons += n;
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        maxNeutrons = input.getIntOr("maxNeutrons", 0);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putInt("maxNeutrons", maxNeutrons);
    }

    public void writeSnapshot(CompoundTag tag) {
        tag.putInt("maxNeutrons", maxNeutrons);
    }

    public void readSnapshot(CompoundTag tag) {
        maxNeutrons = Tags.getIntOr(tag, "maxNeutrons", 0);
    }
}
