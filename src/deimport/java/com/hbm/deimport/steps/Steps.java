package com.hbm.deimport.steps;

import com.hbm.deimport.engine.Step;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Every step in run order. A done step never runs again; a step added here later also runs in worlds already imported,
 * where it finds the leftovers and the backup. A DE addon's converter adds its own steps with {@link #add}.
 */
public final class Steps {
    /** The live phase, which the importer also drives tick by tick. */
    public static final LiveStep LIVE = new LiveStep();
    public static final List<Step> ALL = List.of(
            new BackupStep(),
            new SubLevelsStep(),
            new AirshipPlotsStep(),
            new RegionsStep(),
            new PlayersStep(),
            new PlayerFilesStep(),
            new SavedDataStep(),
            new RadiationStep(),
            new RbmkDialsStep(),
            LIVE);
    private static final Map<String, List<Step>> ADDED = new ConcurrentHashMap<>();

    private Steps() {}

    /** Runs {@code step} right before the step named {@code before}, such as {@code regions}. */
    public static void add(String before, Step step) {
        if (ALL.stream().noneMatch(s -> name(s).equals(before))) throw new IllegalArgumentException("no step " + before);
        ADDED.computeIfAbsent(before, b -> new CopyOnWriteArrayList<>()).add(step);
    }

    public static List<Step> all() {
        List<Step> out = new ArrayList<>();
        for (Step step : ALL) {
            out.addAll(ADDED.getOrDefault(name(step), List.of()));
            out.add(step);
        }
        return out;
    }

    private static String name(Step step) {
        return step.id().substring(0, step.id().indexOf('@'));
    }
}
