package com.hbm.wiaj.actions;

import com.hbm.wiaj.JarScene;
import com.hbm.wiaj.WorldInAJar;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Simple action that places one block instantly with no delay
 *
 * <p>1.7 took a block and a metadata value; a block state is what those two numbers were.
 * @author hmb
 */
public class ActionSetBlock implements IJarAction {

    int x;
    int y;
    int z;
    BlockState b;

    public ActionSetBlock(int x, int y, int z, Block b) {
        this(x, y, z, b.defaultBlockState());
    }

    public ActionSetBlock(int x, int y, int z, BlockState b) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.b = b;
    }

    @Override
    public void act(WorldInAJar world, JarScene scene) {
        world.setBlock(x, y, z, b);
    }

    @Override
    public int getDuration() {
        return 0;
    }
}
