package com.hbm.items;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.items.machine.ItemCassette.TrackType;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import dev.golem.core.dejson.Tints;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;

import java.util.EnumMap;
import java.util.Map;
import com.hbm.items.machine.ItemCassette;

/**
 * The flattened {@code siren_track} family. 1.7 registers one item, stack size 1, on the template tab and
 * lists damage values 1.. in creative; {@code NULL} (meta 0) is a blank the machine reads as "no track" and
 * is not an item here.
 */
public final class ModPowerGenItems {
    private ModPowerGenItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(ITEMS);
    private static final Map<TrackType, RegistryObject<ItemCassette>> TRACKS = new java.util.LinkedHashMap<>();

    static {
        for (TrackType track : TrackType.VALUES) {
            if (track == TrackType.NULL) continue;
            // An addon's track ("<namespace>:<name>") is the addon's to give a cassette, in its own namespace. On
            // loaders that construct the addon first (1.20.1 Forge) its tracks are already in VALUES here.
            if (!track.isBuiltIn()) continue;
            // 1.7 ItemCassette: pass 0 is the plain cassette, pass 1 the overlay tinted with the track colour.
            RegistryObject<ItemCassette> item = REG.item(track.registryName(),
                            p -> new ItemCassette(p, track), p -> p.stacksTo(1))
                    .transform(Presets.sprite("hbm:items/cassette", "hbm:items/cassette_overlay")
                            .tints(Tints.constant(-1), Tints.customModelData(0, track.getColor())))
                    .tab(HbmTab.TEMPLATES);
            TRACKS.put(track, item);
        }
    }

    public static ItemCassette track(TrackType track) {
        return TRACKS.get(track).get();
    }

    public static Map<TrackType, RegistryObject<ItemCassette>> tracks() {
        return TRACKS;
    }

    public static void init() {}
}
