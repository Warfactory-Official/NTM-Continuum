package com.hbm.client.render;

import java.util.function.Predicate;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * An addon's wrecked look for one of HBM's machine renderers (ADDON-API-INVENTORY C-23): while {@code damaged} answers
 * true for the block entity, the renderer draws {@code model} (every group, with {@code texture}) in place of its own
 * parts, turned by {@code extraYaw} degrees about Y after the machine's own placement. Set it on the renderer's static
 * slot during client start-up, before resources load; HBM sets none (plain HBM has no wrecked machines).
 * NTM: Space's damaged Leviathan turbine and powered condenser use it.
 *
 * @param model   an OBJ, {@code "<ns>:models/....obj"}
 * @param texture {@code "<ns>:textures/....png"}
 * @param cull    back-face culling as the 1.7 renderer drew it
 */
public record DamagedView(Predicate<BlockEntity> damaged, String model, String texture, float extraYaw, boolean cull) {}
