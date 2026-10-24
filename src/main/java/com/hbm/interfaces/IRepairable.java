package com.hbm.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.util.InventoryUtil;
import java.util.List;

/** Damaged machine state, repair materials, and the fire-extinguisher callback. */
public interface IRepairable {
    boolean isDamaged();

    List<AStack> getRepairMaterials();

    void repair(Player player);

    /** Checks the whole bill before consuming anything; client prediction never mutates the inventory. */
    static boolean tryRepairMultiblock(Level level, BlockPos core, Player player) {
        if (!(level.getBlockEntity(core) instanceof IRepairable repairable) || !repairable.isDamaged()) return false;
        var materials = repairable.getRepairMaterials();
        if (!materials.isEmpty() && !InventoryUtil.doesPlayerHaveAStacks(player, materials, !level.isClientSide())) return false;
        if (!level.isClientSide()) repairable.repair(player);
        return true;
    }

    void tryExtinguish(Level level, BlockPos pos, EnumExtinguishType type);

    enum EnumExtinguishType {
        WATER,
        FOAM,
        SAND,
        CO2
    }
}
