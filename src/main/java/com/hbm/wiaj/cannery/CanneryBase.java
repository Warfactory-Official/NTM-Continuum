package com.hbm.wiaj.cannery;

import com.hbm.wiaj.JarScript;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public abstract class CanneryBase {

    public static final int[] colorCopper =	new int[] {0xFFFDCA88, 0xFFD57C4F, 0xFFAB4223, 0xFF1A1F22};
    public static final int[] colorGold =	new int[] {0xFFFFFDE0, 0xFFFAD64A, 0xFFDC9613, 0xFF1A1F22};
    public static final int[] colorBlue =	new int[] {0xFFA5D9FF, 0xFF39ACFF, 0xFF1A6CA7, 0xFF1A1F22};
    public static final int[] colorGrey =	new int[] {0xFFD1D1D1, 0xFF919191, 0xFF5D5D5D, 0xFF302E36};

    public abstract ItemStack getIcon();
    public abstract String getName();
    public abstract JarScript createScript();
    public CanneryBase[] seeAlso() { return new CanneryBase[0]; }

    /// SHORTHANDS FOR THE SCRIPTS ///

    /** {@code Minecraft.getMinecraft().fontRenderer}. */
    protected static Font font() {
        return Minecraft.getInstance().font;
    }

    /** {@code I18nUtil.resolveKey}. */
    protected static String resolveKey(String key) {
        return I18n.get(key);
    }

    /** The registered item {@code id} ({@code namespace:path}); 1.7's meta items are flattened to their own ids. */
    protected static Item item(String id) {
        return BuiltInRegistries.ITEM.getValue(dev.golem.core.Ids.parse(id));
    }

    protected static ItemStack stack(String id) {
        return new ItemStack(item(id));
    }

    protected static ItemStack stack(String id, int count) {
        return new ItemStack(item(id), count);
    }

    /** The default state of block {@code id}, air if nothing is registered under it. */
    protected static BlockState block(String id) {
        net.minecraft.world.level.block.Block block = dev.golem.core.dejson.BlockStates.block(id);
        return block == null ? Blocks.AIR.defaultBlockState() : block.defaultBlockState();
    }
}
