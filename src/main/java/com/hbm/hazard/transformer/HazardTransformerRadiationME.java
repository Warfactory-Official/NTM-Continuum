package com.hbm.hazard.transformer;

import com.hbm.config.GeneralConfig;
import com.hbm.hazard.HazardEntry;
import com.hbm.hazard.HazardRegistry;
import com.hbm.hazard.HazardSystem;
import dev.golem.core.storage.CellContents;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/** 1.7 {@code HazardTransformerRadiationME}: an AE2 item cell or portable cell carries its contents' radiation in full. */
public class HazardTransformerRadiationME implements IHazardTransformer {

    @Override
    public void transformPost(ItemStack stack, List<HazardEntry> entries) {
        // 1.7 registers this transformer only while LBSM's safe ME drives are off
        if (GeneralConfig.enableLBSM && GeneralConfig.enableLBSMSafeMEDrives) return;
        float radiation = 0;
        for (CellContents.Stored held : CellContents.items(stack)) {
            radiation += (float) (HazardSystem.getHazardLevelFromStack(held.type(), HazardRegistry.RADIATION) * held.count());
        }
        if (radiation > 0) entries.add(new HazardEntry(HazardRegistry.RADIATION, radiation));
    }
}
