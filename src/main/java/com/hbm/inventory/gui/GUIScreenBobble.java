package com.hbm.inventory.gui;

import com.hbm.blocks.generic.BlockBobble.BobbleType;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.deco.TileEntityBobble;
import dev.golem.core.gui.CompatScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.KeyInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.7 {@code GUIScreenBobble}: the Fallout 3 popup sound and a 300x150 translucent dark-green panel naming the
 * figure, what its person contributed and the inscription on the bottom, each '$' a line break. Mellow's name
 * shuffles back and forth into GEORGEWILLIAMPATON over 1.5 s. No container; Escape or the inventory key closes it.
 */
public class GUIScreenBobble extends CompatScreen {

    private final String name;
    private final @org.jspecify.annotations.Nullable String contribution;
    private final @org.jspecify.annotations.Nullable String inscription;
    private final boolean mellow;

    public GUIScreenBobble(TileEntityBobble bobble) {
        this(bobble.type().name, bobble.type().contribution, bobble.type().inscription, bobble.type() == BobbleType.MELLOW);
    }

    /**
     * The same panel for an addon's bobblehead (ADDON-API-INVENTORY C-24): its name, contribution and inscription ('$' a
     * line break, null for none).
     */
    public GUIScreenBobble(String name, @org.jspecify.annotations.Nullable String contribution,
                           @org.jspecify.annotations.Nullable String inscription) {
        this(name, contribution, inscription, false);
    }

    private GUIScreenBobble(String name, @org.jspecify.annotations.Nullable String contribution,
                            @org.jspecify.annotations.Nullable String inscription, boolean mellow) {
        super(Component.literal("Bobblehead"));
        this.name = name;
        this.contribution = contribution;
        this.inscription = inscription;
        this.mellow = mellow;
    }

    @Override
    protected void init() {
        super.init();
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.BOBBLE.get(), 1.0F));
    }

    @Override
    protected void extractForegroundCompat(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int sizeX = 300;
        int sizeY = 150;
        int left = (this.width - sizeX) / 2;
        int top = (this.height - sizeY) / 2;
        // 1.7 tessellator colour (0, 0.2, 0, 0.8)
        GuiDraw.fill(graphics, left, top, left + sizeX, top + sizeY, 0xCC003300);

        int nextLevel = top + 10;
        centered(graphics, "Nuclear Tech Commemorative Bobblehead", left, sizeX, nextLevel, 0x00ff00);
        nextLevel += 10;

        String bobbleName = name;
        if (mellow) bobbleName = anagramIt(bobbleName, "GEORGEWILLIAMPATON");
        centered(graphics, bobbleName, left, sizeX, nextLevel, 0x009900);
        nextLevel += 20;

        if (contribution != null) {
            centered(graphics, "Has contributed", left, sizeX, nextLevel, 0x00ff00);
            nextLevel += 10;
            for (String text : contribution.split("\\$")) {
                centered(graphics, text, left, sizeX, nextLevel, 0x009900);
                nextLevel += 10;
            }
            nextLevel += 10;
        }

        if (inscription != null) {
            centered(graphics, "On the bottom is the following inscription:", left, sizeX, nextLevel, 0x00ff00);
            nextLevel += 10;
            for (String text : inscription.split("\\$")) {
                centered(graphics, text, left, sizeX, nextLevel, 0x009900);
                nextLevel += 10;
            }
        }
    }

    private void centered(GuiGraphicsExtractor graphics, String text, int left, int sizeX, int y, int color) {
        GuiDraw.text(graphics, font, text, left + sizeX / 2 - font.width(text) / 2, y, 0xFF000000 | color, true);
    }

    @Override
    protected boolean keyPressedCompat(KeyInput event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.key() == 256 || event.matches(mc.options.keyInventory)) {
            mc.setScreen(null);
            return true;
        }
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Animates the letters (from -> to) back and forth over 1.5 seconds. */
    private static String anagramIt(String from, String to) {
        double t = Math.sin((double) System.currentTimeMillis() / 1500.0) * 0.75 + 0.5;

        char[] lettersFrom = from.toCharArray();
        char[] lettersTo = to.toCharArray();
        boolean[] hasPairedLetter = new boolean[lettersFrom.length];
        List<double[]> letterTargets = new ArrayList<>(); // {position, character}

        for (int i = 0; i < lettersFrom.length; i++) {
            char letterFrom = lettersFrom[i];
            for (int o = 0; o < lettersTo.length; o++) {
                if (letterFrom == lettersTo[o] && !hasPairedLetter[o]) {
                    letterTargets.add(new double[]{lerp(i, o, t), lettersFrom[i]});
                    hasPairedLetter[o] = true;
                    break;
                }
            }
        }

        for (int i = 0; i < letterTargets.size(); i++) {
            for (int j = i + 1; j < letterTargets.size(); j++) {
                if (letterTargets.get(i)[0] > letterTargets.get(j)[0]) {
                    double[] temp = letterTargets.get(i);
                    letterTargets.set(i, letterTargets.get(j));
                    letterTargets.set(j, temp);
                }
            }
        }

        StringBuilder anagrammedText = new StringBuilder();
        for (double[] in : letterTargets) anagrammedText.append((char) in[1]);
        return anagrammedText.toString();
    }

    private static double lerp(double a, double b, double t) {
        t = Math.max(Math.min(t, 1), 0);
        return a * (1 - t) + b * t;
    }
}
