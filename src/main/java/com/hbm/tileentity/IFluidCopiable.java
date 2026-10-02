package com.hbm.tileentity;

import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.api.fluidmk2.IFluidUserMK2;
import com.hbm.interfaces.ICopiable;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import dev.golem.core.data.Tags;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code IFluidCopiable}: the settings tool copies every typed tank's fluid id ({@code fluidID}, one int
 * per tank) and pastes the selected one into the first receiving tank. The display list is each copied fluid's
 * translation key, which the tool shows in the info HUD with the selected entry in aqua.
 */
public interface IFluidCopiable extends ICopiable {

    /** First type for the normal paste, the following ones for the alt paste. */
    default int[] getFluidIDToCopy() {
        IFluidUserMK2 tile = (IFluidUserMK2) this;
        List<Integer> types = new ArrayList<>();
        for (FluidTankNTM tank : tile.getAllTanks()) {
            if (!ItemFluidIds.hasNoID(tank.getTankType())) types.add(tank.getTankType().getID());
        }
        return types.stream().mapToInt(Integer::intValue).toArray();
    }

    default @Nullable FluidTankNTM getTankToPaste() {
        // 1.7: "why are we using the transceiver here?" - kept, it is the whole paste rule.
        if (this instanceof IFluidStandardTransceiverMK2 tile) {
            return tile.getReceivingTanks() != null && tile.getReceivingTanks().length > 0
                    ? tile.getReceivingTanks()[0] : null;
        }
        return null;
    }

    @Override
    default @Nullable CompoundTag getSettings(Level level, BlockPos pos) {
        CompoundTag tag = new CompoundTag();
        if (getFluidIDToCopy().length > 0) tag.putIntArray("fluidID", getFluidIDToCopy());
        return tag;
    }

    @Override
    default void pasteSettings(CompoundTag nbt, int index, Level level, Player player, BlockPos pos) {
        FluidTankNTM tank = getTankToPaste();
        if (tank == null) return;
        int[] ids = Tags.getIntArrayOr(nbt, "fluidID", new int[0]);
        if (ids.length > 0 && index < ids.length) tank.setTankType(Fluids.fromID(ids[index]));
    }

    @Override
    default String[] infoForDisplay(Level level, BlockPos pos) {
        int[] ids = getFluidIDToCopy();
        String[] names = new String[ids.length];
        for (int i = 0; i < ids.length; i++) names[i] = Fluids.fromID(ids[i]).getTranslationKey();
        return names;
    }

    /** 1.7 {@code FT_NoID}: fluids no identifier may name (the plasmas and the three smokes). */
    final class ItemFluidIds {
        private ItemFluidIds() {}

        public static boolean hasNoID(com.hbm.inventory.fluid.FluidType type) {
            return type.hasNoID();
        }
    }
}
