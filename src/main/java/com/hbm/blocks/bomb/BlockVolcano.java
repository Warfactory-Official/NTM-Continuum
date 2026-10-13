package com.hbm.blocks.bomb;

import dev.golem.core.world.CompatBlock;
import dev.golem.core.world.CompatBlockItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import dev.golem.core.registry.TickingBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Locale;
import com.hbm.tileentity.TileEntityVolcanoCore;

/**
 * 1.7 {@code BlockVolcano} ({@code volcano_core} / {@code volcano_rad_core}): the unbreakable core that
 * drives a volcano. The old block carried its five behaviours as item metas; they are flattened here to
 * {@code volcano_core[_<mode>]} (meta 0, the static active core, keeps the bare id). The tooltip is the
 * old {@code addInformation}: DOES GROW / DOES EXTINGUISH lines, or SHIELD VOLCANO for the smoldering one.
 */
public class BlockVolcano extends CompatBlock implements TickingBlock {

    /** The old metas 0..4, in order. */
    public enum Mode {
        STATIC_ACTIVE(false, false),
        STATIC_EXTINGUISHING(false, true),
        GROWING_ACTIVE(true, false),
        GROWING_EXTINGUISHING(true, true),
        SMOLDERING(false, false);

        public final boolean growing;
        public final boolean extinguishing;

        Mode(boolean growing, boolean extinguishing) {
            this.growing = growing;
            this.extinguishing = extinguishing;
        }

        public String id(boolean radioactive) {
            String base = radioactive ? "volcano_rad_core" : "volcano_core";
            return this == STATIC_ACTIVE ? base : base + "_" + name().toLowerCase(Locale.ROOT);
        }
    }

    public final Mode mode;
    public final boolean radioactive;

    public BlockVolcano(Properties properties, Mode mode, boolean radioactive) {
        super(properties);
        this.mode = mode;
        this.radioactive = radioactive;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityVolcanoCore(pos, state);
    }

    /** 1.7 {@code ITooltipProvider.addInformation}. */
    public static final class VolcanoItem extends CompatBlockItem {
        private final Mode mode;

        public VolcanoItem(Block block, Item.Properties properties, Mode mode) {
            super(block, properties);
            this.mode = mode;
        }

        @Override
        protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                             List<Component> tooltip, TooltipFlag flag) {
            if (mode == Mode.SMOLDERING) {
                tooltip.add(Component.literal("SHIELD VOLCANO").withStyle(ChatFormatting.GOLD));
                return;
            }
            tooltip.add(mode.growing
                    ? Component.literal("DOES GROW").withStyle(ChatFormatting.RED)
                    : Component.literal("DOES NOT GROW").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(mode.extinguishing
                    ? Component.literal("DOES EXTINGUISH").withStyle(ChatFormatting.RED)
                    : Component.literal("DOES NOT EXTINGUISH").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
