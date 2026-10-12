package com.hbm.tileentity.bomb;

import com.hbm.lib.DirPos;
import com.hbm.lib.Library;
import com.hbm.inventory.container.ContainerCompactLauncher;
import com.hbm.items.weapon.ItemCustomMissilePart.PartSize;
import com.hbm.tileentity.ModCustomMissileBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.7 {@code TileEntityCompactLauncher}: the 3x3 pad for size 1.0 custom missiles. 25,000 l tanks, 25,000 solid,
 * and it refuses to fire without a ready designator.
 */
public class TileEntityCompactLauncher extends TileEntityCustomMissileLauncher implements MenuProvider {

    public static final int maxSolid = 25000;

    public TileEntityCompactLauncher(BlockPos pos, BlockState state) {
        super(ModCustomMissileBlockEntities.COMPACT_LAUNCHER.get(), pos, state, 25000);
    }

    @Override public String getDefaultName() { return "container.compactLauncher"; }
    @Override public int getMaxSolid() { return maxSolid; }
    @Override public PartSize acceptedSize() { return PartSize.SIZE_10; }
    @Override protected int footprintRadius() { return 1; }
    @Override protected boolean requiresDesignatorToLaunch() { return true; }

    /** 1.7 {@code TileEntityCompactLauncher.getConPos}. */
    @Override
    public DirPos[] connectionPositions() {
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        return new DirPos[]{
                new DirPos(x + 2, y, z + 1, Library.POS_X),
                new DirPos(x + 2, y, z - 1, Library.POS_X),
                new DirPos(x - 2, y, z + 1, Library.NEG_X),
                new DirPos(x - 2, y, z - 1, Library.NEG_X),
                new DirPos(x + 1, y, z + 2, Library.POS_Z),
                new DirPos(x - 1, y, z + 2, Library.POS_Z),
                new DirPos(x + 1, y, z - 2, Library.NEG_Z),
                new DirPos(x - 1, y, z - 2, Library.NEG_Z),
                new DirPos(x + 1, y - 1, z + 1, Library.NEG_Y),
                new DirPos(x + 1, y - 1, z - 1, Library.NEG_Y),
                new DirPos(x - 1, y - 1, z + 1, Library.NEG_Y),
                new DirPos(x - 1, y - 1, z - 1, Library.NEG_Y)
        };
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getName());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerCompactLauncher(containerId, playerInventory, this);
    }
}
