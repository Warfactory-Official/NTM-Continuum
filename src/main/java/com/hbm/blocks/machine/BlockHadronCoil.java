package com.hbm.blocks.machine;

import dev.golem.core.world.CompatBlock;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

/**
 * 1.7 {@code BlockHadronCoil}: a dense coil block whose {@link #factor} is its coil strength, shown as
 * {@code info.coil: <factor>} in the tooltip. The particle accelerator that read it is gone from 1.7 too; the
 * blocks survive as storage for their wire, which a shapeless recipe gives back.
 *
 * <p>1.7 drew them with connected textures ({@code IBlockCT}, the {@code _ct} sheets); the port has no
 * connected-texture renderer, so each face shows the plain texture (as the PWR casing does).
 */
public class BlockHadronCoil extends CompatBlock {

    public final int factor;

    public BlockHadronCoil(Properties properties, int factor) {
        super(properties);
        this.factor = factor;
    }

    public static void addInformation(int factor, List<Component> list) {
        list.add(Component.translatable("info.coil").append(": " + String.format(Locale.US, "%,d", factor)));
    }
}
