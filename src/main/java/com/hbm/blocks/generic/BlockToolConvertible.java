package com.hbm.blocks.generic;

import com.hbm.api.block.IToolable;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.handler.NTMToolHandler;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.util.InventoryUtil;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * A plain building block that a construction tool can upgrade in place, per the
 * {@link NTMToolHandler} table. Ports the 1.12.2 {@code BlockToolConversion} and the {@code onScrew} half of
 * {@code BlockFusionComponent}. Hitting it with the right tool while carrying the required materials
 * consumes them and swaps the block for its upgraded form.
 *
 * <p>This is how the Watz and fusion torus structures gate their components: the plain casing and the
 * raw fusion component are craftable, but the reactor only accepts the bolted / welded versions, which
 * exist solely as the output of a conversion.
 */
public class BlockToolConvertible extends CompatBlock implements IToolable, ILookOverlay {

    public BlockToolConvertible(Properties properties) {
        super(properties);
    }

    @Override
    public boolean onScrew(Level level, Player player, BlockPos pos, Direction side, Vec3 hit,
                           InteractionHand hand, ToolType tool) {
        if (level.isClientSide()) return false;

        NTMToolHandler.Conversion conversion = NTMToolHandler.get(tool, this);
        if (conversion == null) return false;

        List<AStack> materials = new ArrayList<>(conversion.materials());
        if (!materials.isEmpty() && !InventoryUtil.doesPlayerHaveAStacks(player, materials, true)) return false;

        level.setBlock(pos, conversion.result().get().defaultBlockState(), 3);
        return true;
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        ItemStack held = player.getMainHandItem();
        ToolType tool = ToolType.getType(held);
        if (tool == null) return null;

        NTMToolHandler.Conversion conversion = NTMToolHandler.get(tool, this);
        if (conversion == null) return null;

        List<String> text = new ArrayList<>();
        for (AStack material : conversion.materials()) {
            ItemStack display = material.getCyclingDisplayStack();
            if (display.isEmpty()) continue;
            text.add(display.getCount() + "x " + display.getHoverName().getString());
        }
        return new ILookOverlay.Data(this.getName().getString(), 0xffff00, 0x404000, text);
    }
}
