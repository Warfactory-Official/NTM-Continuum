package com.hbm.client;

import com.hbm.Constants;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.render.Sprites;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The fluid droplet sprites, computed while the atlas is stitched instead of shipped as files.
 *
 * <p>One greyscale droplet — {@code hbm:items/fluid_icon} — is recoloured onto every registered
 * fluid's own {@link FluidType#getColor()}, giving {@code hbm:items/fluid_icon_<fluid>} for each of
 * them. The band declared here is the art's full range, so the remap is exactly the multiply that
 * {@link com.hbm.items.machine.ItemFluidIcon} gets from its item tint: the droplet drawn from a sprite
 * and the droplet drawn as a stack are the same picture, which they have to be — a recipe screen shows
 * both at once.
 *
 * <p>That item stays the droplet of choice wherever there is an item slot to put it in, JEI's grids
 * above all. These sprites are for everywhere there is not: a fixed corner of a machine screen, an
 * overlay, a category icon — somewhere a droplet has to be drawn at an x and a y rather than handed to
 * a slot. {@link #draw} is that call.
 *
 * <p>The sprite ids come from {@link FluidType#getIconSprite()}, which is built from the same lowercase
 * key this declares each permutation under, so a fluid and its droplet can only ever be named the same
 * way: adding a fluid to {@link Fluids} mints its droplet with it, with no file to draw.
 */
public final class FluidSprites {

    private FluidSprites() {}

    /** The greyscale droplet every fluid's icon is recoloured from. */
    public static final String ART = Constants.MOD_ID + ":" + FluidType.ICON_ART;

    /** The band the droplet art is drawn in: its full range, which makes the remap a plain multiply. */
    private static final int ART_LIGHT = 0xFFFFFF;
    private static final int ART_DARK = 0x000000;

    /** The size an item slot draws an icon at, and so the size a droplet is expected at. */
    private static final int ICON_SIZE = 16;

    /** Declares every fluid's droplet. Client only, before the first resource load. */
    public static void install() {
        Fluids.init();
        FluidType[] fluids = Fluids.getAll();
        Sprites.Remap remap = Sprites.remap(Sprites.Atlas.ITEMS).texture(ART).band(ART_LIGHT, ART_DARK);
        for (FluidType fluid : fluids) {
            remap.tint(fluid.getKey(), fluid.getColor());
        }
        remap.declare();
        Constants.LOG.info("Declared {} fluid droplet sprite(s) for load-time generation", fluids.length);
    }

    /** Draws {@code fluid}'s droplet at the size an item slot would have drawn it. */
    public static void draw(GuiGraphicsExtractor graphics, FluidType fluid, int x, int y) {
        draw(graphics, fluid, x, y, ICON_SIZE, ICON_SIZE);
    }

    /** Draws {@code fluid}'s droplet stretched into a rectangle of its own. */
    public static void draw(GuiGraphicsExtractor graphics, FluidType fluid, int x, int y, int width, int height) {
        GuiDraw.blitSprite(graphics, Sprites.Atlas.ITEMS, fluid.getIconSprite(), x, y, width, height);
    }
}
