package com.hbm.wiaj.cannery;

import com.hbm.wiaj.JarScene;
import com.hbm.wiaj.JarScript;
import com.hbm.wiaj.WorldInAJar;
import com.hbm.wiaj.actions.ActionCreateActor;
import com.hbm.wiaj.actions.ActionOffsetBy;
import com.hbm.wiaj.actions.ActionRemoveActor;
import com.hbm.wiaj.actions.ActionRotateBy;
import com.hbm.wiaj.actions.ActionSetBlock;
import com.hbm.wiaj.actions.ActionSetZoom;
import com.hbm.wiaj.actions.ActionWait;
import com.hbm.wiaj.actors.ActorFancyPanel;
import com.hbm.wiaj.actors.ActorFancyPanel.Orientation;
import com.hbm.wiaj.actors.ActorTileEntity;
import com.hbm.wiaj.actors.JarRenderers;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;

public class CanneryFEnSU extends CanneryBase {

    @Override
    public ItemStack getIcon() {
        return stack("hbm:machine_fensu");
    }

    @Override
    public String getName() {
        return "cannery.fensu";
    }

    public JarScript createScript() {
        WorldInAJar world = new WorldInAJar(11, 5, 5);
        JarScript script = new JarScript(world);

        JarScene scene0 = new JarScene(script);
        scene0.add(new ActionSetZoom(1.5D, 0));
        scene0.add(new ActionOffsetBy(-2D, 0D, 0D, 0));
        CompoundTag fensu = new CompoundTag();
        fensu.putDouble("x", 7);
        fensu.putDouble("y", 1);
        fensu.putDouble("z", 2);
        fensu.putInt("rotation", 4);
        fensu.putFloat("speed", 10F);
        scene0.add(new ActionCreateActor(0, new ActorTileEntity(new JarRenderers.Fensu(), fensu)));

        scene0.add(new ActionCreateActor(1, new ActorFancyPanel(font(), 0, -50, new Object[][] {{resolveKey("cannery.fensu.0")}}, 200)
                .setColors(colorCopper).setOrientation(Orientation.BOTTOM)));
        scene0.add(new ActionWait(80));
        scene0.add(new ActionRemoveActor(1));
        scene0.add(new ActionWait(10));
        scene0.add(new ActionRotateBy(45, 90, 20));

        scene0.add(new ActionCreateActor(1, new ActorFancyPanel(font(), 0, 20, new Object[][] {{resolveKey("cannery.fensu.1")}}, 200)
                .setColors(colorCopper).setOrientation(Orientation.TOP)));
        scene0.add(new ActionWait(60));
        scene0.add(new ActionRemoveActor(1));

        scene0.add(new ActionCreateActor(1, new ActorFancyPanel(font(), 0, 20, new Object[][] {{resolveKey("cannery.fensu.2")}}, 200)
                .setColors(colorCopper).setOrientation(Orientation.TOP)));
        scene0.add(new ActionWait(60));
        scene0.add(new ActionRemoveActor(1));

        scene0.add(new ActionSetBlock(7, 0, 2, block("hbm:red_wire_coated")));
        scene0.add(new ActionWait(10));
        scene0.add(new ActionSetBlock(6, 0, 2, lever(false)));
        scene0.add(new ActionWait(10));
        scene0.add(new ActionRotateBy(0, -60, 20));
        scene0.add(new ActionWait(10));
        scene0.add(new ActionSetBlock(6, 0, 2, lever(true)));
        scene0.add(new ActionWait(20));
        scene0.add(new ActionSetBlock(6, 0, 2, lever(false)));
        scene0.add(new ActionWait(20));
        scene0.add(new ActionRotateBy(-45, -30, 20));
        scene0.add(new ActionOffsetBy(2D, 0D, 0D, 10));
        /// END OF SCENE 1 ///

        JarScene scene1 = new JarScene(script);

        for(int x = world.sizeX - 1; x >= 0 ; x--) {
            for(int z = 0; z < world.sizeZ; z++) {
                if(z == 2 && x > 0 && x < 10)
                    scene1.add(new ActionSetBlock(x, 0, z, block("hbm:red_wire_coated")));
                else
                    scene1.add(new ActionSetBlock(x, 0, z, Blocks.BRICKS));
            }
            scene1.add(new ActionWait(2));
        }

        scene1.add(new ActionWait(18));
        scene1.add(new ActionSetBlock(1, 1, 2, CanneryFirebox.detector(false)));
        scene1.add(new ActionWait(10));
        scene1.add(new ActionSetBlock(1, 1, 2, CanneryFirebox.detector(true)));
        scene1.add(new ActionWait(60));

        script.addScene(scene0).addScene(scene1);
        return script;
    }

    /**
     * 1.7 lever metadata 2 (on the side of the block to its east, facing west), plus 8 when thrown.
     */
    static BlockState lever(boolean powered) {
        return Blocks.LEVER.defaultBlockState()
                .setValue(LeverBlock.FACE, AttachFace.WALL)
                .setValue(LeverBlock.FACING, Direction.WEST)
                .setValue(LeverBlock.POWERED, powered);
    }
}
