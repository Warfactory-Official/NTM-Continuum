package com.hbm.recipes.data;

import static com.hbm.hazard.HazardRegistry.*;

import com.hbm.api.data.HazardDataMaps;
import com.hbm.api.data.HazardDataMaps.Emission;
import dev.golem.core.data.DataMapSource;

/**
 * 1.7 {@code BlockHazard}: a hazard block's chunk radiation is the RADIATION level of its block item (taken from
 * {@code HazardSystem} once at start-up, {@code BlockHazard.onBlockAdded}), so each value is the block item's 1.7
 * {@code HazardRegistry} / {@code OreDictManager} frame level. The file lists the blocks by id.
 */
public final class RadiationEmissionSource extends DataMapSource<Emission> {

    public RadiationEmissionSource() {
        super(HazardDataMaps.RADIATION_EMISSION, false);
    }

    private void block(String id, float radiation) {
        put("hbm:" + id, new Emission(radiation, 0F));
    }

    @Override
    protected void values() {
        block("block_actinium", ac227 * block);
        // port: block_au198 is not a 1.7 block (AU198's frame has no block shape); port-only, at the frame's block strength
        block("block_au198", au198 * block);
        // 1.7 block_corium = new BlockHazard(Material.iron): its item's RADIATION 150 only, no area pulse
        block("block_corium", 150F);
        block("block_mox_fuel", mox * block);
        block("block_neptunium", np237 * block);
        block("block_plutonium", pu * block);
        block("block_plutonium_fuel", puf * block);
        block("block_polonium", po210 * block);
        block("block_pu238", pu238 * block);
        block("block_pu239", pu239 * block);
        block("block_pu240", pu240 * block);
        block("block_pu_mix", purg * block);
        block("block_ra226", ra226 * block);
        block("block_schrabidate", sb * block);
        block("block_schrabidium", sa326 * block);
        block("block_schrabidium_fuel", saf * block);
        block("block_schraranium", sr * block);
        block("block_solinium", sa327 * block);
        block("block_thorium", th232 * block);
        block("block_thorium_fuel", thf * block);
        block("block_trinitite", trn * block);
        block("block_u233", u233 * block);
        block("block_u235", u235 * block);
        block("block_u238", u238 * block);
        block("block_uranium", u * block);
        block("block_uranium_fuel", uf * block);
        block("block_waste", wst * block);
        block("block_waste_painted", wst * block);
        block("block_waste_vitrified", wstv * block);
    }
}
