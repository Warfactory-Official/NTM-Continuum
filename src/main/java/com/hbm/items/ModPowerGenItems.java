package com.hbm.items;

import com.hbm.Constants;
import com.hbm.api.tab.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.machine.ItemCassette.TrackType;
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
    private static final Map<TrackType, RegistryObject<ItemCassette>> TRACKS = new EnumMap<>(TrackType.class);

    static {
        for (TrackType track : TrackType.VALUES) {
            if (track == TrackType.NULL) continue;
            RegistryObject<ItemCassette> item = ITEMS.registerItem(track.registryName(),
                    p -> new ItemCassette(p, track), p -> p.stacksTo(1));
            TRACKS.put(track, item);
            ModTabs.addTo(HbmTab.TEMPLATES, item);
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
