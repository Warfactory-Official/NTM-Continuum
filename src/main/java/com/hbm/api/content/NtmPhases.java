package com.hbm.api.content;

import com.hbm.scripting.ScriptedContent;
import com.hbm.api.content.recipes.TableHandler;
import dev.golem.core.content.Phase;
import dev.golem.core.content.Pipeline;
import dev.golem.core.content.Replay;

/**
 * NTM's content phases on Golem Core's {@link Pipeline}; subscribe with {@code Pipeline.on} from a Golem mod's
 * {@code init}. Registry content (shapes → materials → fluids → fuels) before the flush; recipes (replayable: edits
 * replay on every table rebuild and script reload) and hazards (after setup, registries live) after.
 */
public final class NtmPhases {

    private NtmPhases() {}

    public static final Phase<ShapesEvent> SHAPES = Phase.of("hbm:shapes", Phase.Stage.BEFORE_FLUSH, ShapesEvent::new);
    public static final Phase<MaterialsEvent> MATERIALS = Phase.<MaterialsEvent>of("hbm:materials", Phase.Stage.BEFORE_FLUSH, MaterialsEvent::new)
            .onPosted(event -> ScriptedContent.mintMaterials());
    public static final Phase<FluidsEvent> FLUIDS = Phase.<FluidsEvent>of("hbm:fluids", Phase.Stage.BEFORE_FLUSH, FluidsEvent::new)
            .onPosted(event -> ScriptedContent.mintAutogenItems());
    public static final Phase<FuelsEvent> FUELS = Phase.of("hbm:fuels", Phase.Stage.BEFORE_FLUSH, FuelsEvent::new);
    public static final Phase<RecipesEvent> RECIPES = Phase.<RecipesEvent>replayable("hbm:recipes", Phase.Stage.BEFORE_FLUSH, RecipesEvent::new)
            .onReset(() -> {
                Replay.clearEdits();
                new RecipesEvent().tables().values().forEach(TableHandler::reset);
            });
    public static final Phase<HazardsEvent> HAZARDS = Phase.<HazardsEvent>replayable("hbm:hazards", Phase.Stage.AFTER_SETUP, HazardsEvent::new)
            .onReset(HazardsEvent::undoAll)
            .onPosted(event -> event.edits().forEach(ScriptedContent::runSafely));

    static {
        Pipeline.declare(SHAPES, MATERIALS, FLUIDS, FUELS, RECIPES, HAZARDS);
    }

    /** Declares the phases (class init). */
    public static void init() {}
}
