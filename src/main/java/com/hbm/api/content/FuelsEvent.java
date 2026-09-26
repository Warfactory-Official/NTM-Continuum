package com.hbm.api.content;

import com.hbm.scripting.NTMEvents;
import com.hbm.scripting.fuels.BreedingFuels;
import com.hbm.scripting.fuels.PileFuels;
import com.hbm.scripting.fuels.PlateFuels;
import com.hbm.scripting.fuels.PwrFuels;
import com.hbm.scripting.fuels.PwrPartBlocks;
import com.hbm.scripting.fuels.RbmkColumns;
import com.hbm.scripting.fuels.RbmkFuels;
import com.hbm.scripting.fuels.RtgFuels;
import com.hbm.scripting.fuels.WatzFuels;
import com.hbm.scripting.fuels.ZirnoxFuels;

/** {@code NTMEvents.fuels}: reactor fuels and parts. Minted ids stay fixed only while the script keeps declaring them. */
public final class FuelsEvent {

    FuelsEvent() {}

    public final RbmkFuels rbmk = new RbmkFuels();
    public final WatzFuels watz = new WatzFuels();
    public final PwrFuels pwr = new PwrFuels();
    public final ZirnoxFuels zirnox = new ZirnoxFuels();
    public final PlateFuels researchReactor = new PlateFuels();
    public final RtgFuels rtg = new RtgFuels();
    public final PileFuels pile = new PileFuels();
    public final BreedingFuels breeding = new BreedingFuels();
    public final RbmkColumns rbmkColumns = new RbmkColumns();
    public final PwrPartBlocks pwrParts = new PwrPartBlocks();
}
