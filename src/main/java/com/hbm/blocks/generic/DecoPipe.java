package com.hbm.blocks.generic;

import net.minecraft.world.level.block.RotatedPillarBlock;

/**
 * The decorative steel pipe (1.7 {@code BlockPipe}): an axis pillar with an octagonal OBJ body drawn by
 * {@code RenderPipe}. The old block kept full-cube bounds and only changed its render type, so the port
 * is a plain {@link RotatedPillarBlock} whose geometry lives in the authored {@code template_pipe*}
 * element models; {@link Style} picks which of the four old {@code rType} bodies the blockstate uses.
 * Placement on a face's axis is vanilla pillar behaviour, which is exactly what the old
 * {@code onBlockPlaced} computed (0/4/8 by clicked side).
 */
public class DecoPipe extends RotatedPillarBlock {

    /** The old {@code rType}: 0 plain, 1 rimmed, 2 quad, 3 rimmed inside a mesh frame. */
    public enum Style {
        PLAIN("template_pipe"), RIM("template_pipe_rim"), QUAD("template_pipe_quad"), FRAMED("template_pipe_framed");

        public final String template;

        Style(String template) {
            this.template = template;
        }
    }

    public final Style style;

    public DecoPipe(Properties properties, Style style) {
        super(properties);
        this.style = style;
    }
}
