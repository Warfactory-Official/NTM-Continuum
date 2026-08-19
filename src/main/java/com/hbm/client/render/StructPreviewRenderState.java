package com.hbm.client.render;

import dev.golem.core.render.CompatRenderState;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

/** Render state for {@link StructPreviewRenderer}: the cells of the pattern still missing. */
public class StructPreviewRenderState extends CompatRenderState {

    /** One ghost cube: offset from the struct core plus the textures it draws with. */
    public record Ghost(BlockPos offset, net.minecraft.resources.Identifier top,
                        net.minecraft.resources.Identifier side) {}

    public final List<Ghost> missing = new ArrayList<>();
}
