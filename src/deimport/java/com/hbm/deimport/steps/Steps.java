package com.hbm.deimport.steps;

import com.hbm.deimport.engine.Step;
import java.util.List;

/**
 * Every step in run order. A done step never runs again; a step added here later also runs in worlds already imported,
 * where it finds the leftovers and the backup.
 */
public final class Steps {
    /** The live phase, which the importer also drives tick by tick. */
    public static final LiveStep LIVE = new LiveStep();
    public static final List<Step> ALL = List.of(
            new BackupStep(),
            new SubLevelsStep(),
            new RegionsStep(),
            new PlayersStep(),
            new PlayerFilesStep(),
            new SavedDataStep(),
            new RadiationStep(),
            new RbmkDialsStep(),
            LIVE);

    private Steps() {}
}
