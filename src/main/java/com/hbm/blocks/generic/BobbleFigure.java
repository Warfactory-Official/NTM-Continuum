package com.hbm.blocks.generic;

/**
 * The figure data of 1.7 {@code RenderBobble}, kept free of Minecraft types so that the block entity renderer
 * and the item-model declarations (which run in a plain JVM) read one table: which skin each bobblehead wears,
 * which body it has, and the limb poses of {@code setupFigurineRotation}. Indexed by the {@code BobbleType}
 * ordinal; the names below follow that enum.
 */
public final class BobbleFigure {

    private BobbleFigure() {}

    public enum Body { GUY, PELLET, FUMO, DRILLGON, LEAFIA, SKIN }

    // NONE STRENGTH PERCEPTION ENDURANCE CHARISMA INTELLIGENCE AGILITY LUCK BOB FRIZZLE PU238 VT DOC BLUEHAT PHEO
    // ADAM29 UFFR VAER NOS DRILLGON CIRNO MICROWAVE PEEP MELLOW ABEL
    public static final String[] NAMES = {"NONE", "STRENGTH", "PERCEPTION", "ENDURANCE", "CHARISMA", "INTELLIGENCE",
            "AGILITY", "LUCK", "BOB", "FRIZZLE", "PU238", "VT", "DOC", "BLUEHAT", "PHEO", "ADAM29", "UFFR", "VAER", "NOS",
            "DRILLGON", "CIRNO", "MICROWAVE", "PEEP", "MELLOW", "ABEL", "MOVBLOCK", "LEAFIA", "HACKER6329", "EOS"};

    /** {@code BobbleType.skinLayers}: false draws the 1.7 "17" single-layer parts. */
    private static final boolean[] LAYERS = {false, false, false, false, false, false, false, false, false, true, false,
            true, true, true, true, true, false, true, true, false, true, true, true, true, true, true, false, true, true};

    public static int index(String name) {
        for (int i = 0; i < NAMES.length; i++) if (NAMES[i].equals(name)) return i;
        return 0;
    }

    public static String suffix(int type) {
        return LAYERS[type] ? "" : "17";
    }

    /** The skin bound before the figure; 1.7's default ({@code NONE}) was {@code ResourceManager.universal}. */
    public static String texture(int type) {
        return switch (NAMES[type]) {
            case "STRENGTH", "PERCEPTION", "ENDURANCE", "CHARISMA", "INTELLIGENCE", "AGILITY", "LUCK" -> "trinkets/vaultboy";
            case "BOB" -> "trinkets/hbm";
            case "PU238" -> "trinkets/pellet";
            case "FRIZZLE" -> "trinkets/frizzle";
            case "VT" -> "trinkets/vt";
            case "DOC" -> "trinkets/doctor17ph";
            case "BLUEHAT" -> "trinkets/thebluehat";
            case "PHEO" -> "trinkets/pheo";
            case "CIRNO" -> "trinkets/cirno";
            case "ADAM29" -> "trinkets/adam29";
            case "UFFR" -> "trinkets/uffr";
            case "VAER" -> "trinkets/vaer";
            case "NOS" -> "trinkets/nos";
            case "DRILLGON" -> "trinkets/drillgon200";
            case "MICROWAVE" -> "trinkets/microwave";
            case "PEEP" -> "trinkets/peep";
            case "MELLOW" -> "trinkets/mellowrpg8";
            case "ABEL" -> "trinkets/abel";
            case "LEAFIA" -> "trinkets/leafia";
            default -> "thegadget3_";
        };
    }

    public static Body body(int type) {
        return switch (NAMES[type]) {
            case "PU238" -> Body.PELLET;
            case "UFFR" -> Body.FUMO;
            case "DRILLGON" -> Body.DRILLGON;
            case "LEAFIA" -> Body.LEAFIA;
            case "MOVBLOCK", "HACKER6329", "EOS" -> Body.SKIN; // old-hbm skinUuid != null
            default -> Body.GUY;
        };
    }

    /** One pose: left arm, right arm, left leg, right leg (x, y, z degrees each), body yaw, head (x, y, z). */
    public record Pose(double[] leftArm, double[] rightArm, double[] leftLeg, double[] rightLeg, double body, double[] head) {}

    /** 1.7 {@code resetFigurineRotation} + {@code setupFigurineRotation}. */
    public static Pose pose(int type) {
        double[] la = {0, 0, 0}, ra = {0, 0, 0}, ll = {0, 0, 0}, rl = {0, 0, 0}, head = {0, 0, 0};
        double body = 0;
        switch (NAMES[type]) {
            case "STRENGTH" -> { la = new double[]{0, 25, 135}; ra = new double[]{0, -45, 135}; ll = new double[]{0, 0, -5};
                rl = new double[]{0, 0, 5}; head = new double[]{15, 0, 0}; }
            case "PERCEPTION" -> { la = new double[]{0, -15, 135}; ra = new double[]{-5, 0, 0}; }
            case "ENDURANCE" -> { body = 45; la = new double[]{0, -25, 30}; ra = new double[]{0, 45, 30}; head = new double[]{0, -45, 0}; }
            case "CHARISMA" -> { body = 45; ra = new double[]{0, -45, 90}; ll = new double[]{0, 0, -5}; rl = new double[]{0, 0, 5};
                head = new double[]{-5, -45, 0}; }
            case "INTELLIGENCE" -> { head = new double[]{0, 30, 0}; la = new double[]{5, 0, 0}; ra = new double[]{15, 0, 170}; }
            case "AGILITY" -> { la = new double[]{0, 0, 60}; ra = new double[]{0, 0, -45}; ll = new double[]{0, 0, -15}; rl = new double[]{0, 0, 45}; }
            case "LUCK" -> { la = new double[]{135, 45, 0}; ra = new double[]{-135, -45, 0}; rl = new double[]{-5, 0, 0}; }
            case "VT" -> { la = new double[]{0, -45, 60}; ra = new double[]{0, 0, 45}; ll = new double[]{2, 0, 0}; rl = new double[]{-2, 0, 0}; }
            case "BLUEHAT" -> la = new double[]{0, 90, 60};
            case "FRIZZLE" -> { la = new double[]{0, 15, 45}; ra = new double[]{0, 0, 80}; ll = new double[]{0, 0, 2}; rl = new double[]{0, 0, -2}; }
            case "ADAM29" -> ra = new double[]{0, 0, 60};
            case "PHEO" -> { la = new double[]{0, 0, 80}; ra = new double[]{0, 0, 45}; }
            case "VAER" -> { la = new double[]{0, -5, 45}; ra = new double[]{0, 15, 45}; }
            case "PEEP" -> { la = new double[]{0, 0, 1}; ra = new double[]{0, 0, 1}; }
            case "MELLOW" -> { la = new double[]{0, 10, 0}; ra = new double[]{0, -10, 0}; ll = new double[]{3, 5, 2}; rl = new double[]{-3, -5, 0}; }
            case "ABEL" -> { la = new double[]{0, 80, 90}; ra = new double[]{0, -80, 90}; }
            default -> { }
        }
        return new Pose(la, ra, ll, rl, body, head);
    }

    /** Limb pivots of {@code renderGuy}: left leg, right leg, left arm, right arm; the head turns about (0, 1.75, 0). */
    public static final double[][] PIVOTS = {{0, 1, -0.125}, {0, 1, 0.125}, {0, 1.625, -0.25}, {0, 1.625, 0.25}};
    public static final String[] LIMBS = {"LL", "RL", "LA", "RA"};
}
