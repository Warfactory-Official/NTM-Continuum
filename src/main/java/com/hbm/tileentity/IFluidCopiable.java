package com.hbm.tileentity;

import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.api.fluidmk2.IFluidUserMK2;
import com.hbm.interfaces.ICopiable;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code IFluidCopiable}: the settings tool copies every typed tank's fluid (1.7 {@code fluidID}, one int per
 * tank; here {@code fluids}, one registry name per tank - a copied setting lives on in the tool item, so it is never
 * a numeric id, History section 19ej-ids) and pastes the selected one into the first receiving tank. The display list is each copied fluid's
 * translation key, which the tool shows in the info HUD with the selected entry in aqua.
 */
public interface IFluidCopiable extends ICopiable {

    /** First type for the normal paste, the following ones for the alt paste. */
    default FluidType[] getFluidsToCopy() {
        IFluidUserMK2 tile = (IFluidUserMK2) this;
        List<FluidType> types = new ArrayList<>();
        for (FluidTankNTM tank : tile.getAllTanks()) {
            if (!ItemFluidIds.hasNoID(tank.getTankType())) types.add(tank.getTankType());
        }
        return types.toArray(new FluidType[0]);
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
        FluidType[] fluids = getFluidsToCopy();
        if (fluids.length > 0) Fluids.writeList(tag, "fluids", fluids);
        return tag;
    }

    @Override
    default void pasteSettings(CompoundTag nbt, int index, Level level, Player player, BlockPos pos) {
        FluidTankNTM tank = getTankToPaste();
        if (tank == null) return;
        FluidType[] fluids = Fluids.readList(nbt, "fluids");
        if (fluids.length > 0 && index < fluids.length) tank.setTankType(fluids[index]);
    }

    @Override
    default String[] infoForDisplay(Level level, BlockPos pos) {
        FluidType[] fluids = getFluidsToCopy();
        String[] names = new String[fluids.length];
        for (int i = 0; i < fluids.length; i++) names[i] = fluids[i].getTranslationKey();
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
