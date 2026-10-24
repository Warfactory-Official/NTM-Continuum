package com.hbm.client;

/** Registers the native material selector and molten-colour tint before models are decoded. */
public final class ScrapsRendering {
    private ScrapsRendering() {}

    public static void install() {
        //? if >=26.1 {
        //? if fabric {
        /*net.minecraft.client.renderer.item.properties.select.SelectItemModelProperties.ID_MAPPER.put(
                dev.golem.core.Ids.of("hbm", "scrap_material"), ScrapMaterialProperty.TYPE);
        net.minecraft.client.color.item.ItemTintSources.ID_MAPPER.put(
                dev.golem.core.Ids.of("hbm", "scraps_color"), ScrapsColorTintSource.MAP_CODEC);
        *///?} elif neoforge {
        var bus = dev.golem.core.platform.NeoForgeEntrypoint.modBus();
        bus.addListener((net.neoforged.neoforge.client.event.RegisterSelectItemModelPropertyEvent event) ->
                event.register(dev.golem.core.Ids.of("hbm", "scrap_material"), ScrapMaterialProperty.TYPE));
        bus.addListener((net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.ItemTintSources event) ->
                event.register(dev.golem.core.Ids.of("hbm", "scraps_color"), ScrapsColorTintSource.MAP_CODEC));
        //?}
        //?}
    }
}
