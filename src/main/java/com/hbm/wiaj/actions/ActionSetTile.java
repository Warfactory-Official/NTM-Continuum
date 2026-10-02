package com.hbm.wiaj.actions;

import com.hbm.wiaj.JarScene;
import com.hbm.wiaj.WorldInAJar;

/** Parks a tile in the jar: a dummy connector, or a block entity that only exists to be rendered. */
public class ActionSetTile implements IJarAction {

    int x;
    int y;
    int z;
    Object tile;

    public ActionSetTile(int x, int y, int z, Object tile) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.tile = tile;
    }

    @Override
    public int getDuration() {
        return 0;
    }

    @Override
    public void act(WorldInAJar world, JarScene scene) {
        world.setTileEntity(x, y, z, tile);
    }
}
