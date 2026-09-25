package com.hbm.scripting;

import com.hbm.Constants;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * The {@code NTMEvents} startup-script global. Listeners run once, in this order, when both the
 * scripts and this mod's registries are ready:
 *
 * <pre>
 * NTMEvents.shapes(e => ...)      new material shapes
 * NTMEvents.materials(e => ...)   new materials; autogen shapes on any material
 * NTMEvents.fluids(e => ...)      new fluids; traits on any fluid
 * NTMEvents.fuels(e => ...)       new reactor fuels: RBMK, Watz, PWR, ZIRNOX, research plates, RTG
 * NTMEvents.recipes(e => ...)     machine recipes: e.assembly.add(...), e.shredder.remove(...)
 * NTMEvents.hazards(e => ...)     hazards on any item or tag; new hazard types
 * </pre>
 */
public final class NTMEvents {

    private static volatile NTMEvents current = new NTMEvents();

    final List<Consumer<ShapesEvent>> shapes = new CopyOnWriteArrayList<>();
    final List<Consumer<MaterialsEvent>> materials = new CopyOnWriteArrayList<>();
    final List<Consumer<FluidsEvent>> fluids = new CopyOnWriteArrayList<>();
    final List<Consumer<FuelsEvent>> fuels = new CopyOnWriteArrayList<>();
    final List<Consumer<RecipesEvent>> recipes = new CopyOnWriteArrayList<>();
    final List<Consumer<HazardsEvent>> hazards = new CopyOnWriteArrayList<>();

    private NTMEvents() {}

    static NTMEvents fresh() {
        current = new NTMEvents();
        return current;
    }

    static NTMEvents current() {
        return current;
    }

    public void shapes(Consumer<ShapesEvent> listener) {
        shapes.add(listener);
    }

    public void materials(Consumer<MaterialsEvent> listener) {
        materials.add(listener);
    }

    public void fluids(Consumer<FluidsEvent> listener) {
        fluids.add(listener);
    }

    public void fuels(Consumer<FuelsEvent> listener) {
        fuels.add(listener);
    }

    public void recipes(Consumer<RecipesEvent> listener) {
        recipes.add(listener);
    }

    public void hazards(Consumer<HazardsEvent> listener) {
        hazards.add(listener);
    }

    static <E> void post(String name, List<Consumer<E>> listeners, E event) {
        for (Consumer<E> listener : listeners) {
            try {
                listener.accept(event);
            } catch (RuntimeException e) {
                Constants.LOG.error("[scripts] NTMEvents.{} listener failed: {}", name, e.getMessage(), e);
            }
        }
    }
}
