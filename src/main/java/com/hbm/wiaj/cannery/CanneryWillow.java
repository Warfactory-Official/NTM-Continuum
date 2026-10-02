package com.hbm.wiaj.cannery;

import com.hbm.blocks.ModTerrainBlocks;
import com.hbm.blocks.generic.BlockNTMFlower;
import com.hbm.blocks.generic.BlockTallPlant;
import com.hbm.wiaj.JarScene;
import com.hbm.wiaj.JarScript;
import com.hbm.wiaj.WorldInAJar;
import com.hbm.wiaj.actions.ActionCreateActor;
import com.hbm.wiaj.actions.ActionRemoveActor;
import com.hbm.wiaj.actions.ActionSetBlock;
import com.hbm.wiaj.actions.ActionSetZoom;
import com.hbm.wiaj.actions.ActionWait;
import com.hbm.wiaj.actors.ActorFancyPanel;
import com.hbm.wiaj.actors.ActorFancyPanel.Orientation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

public class CanneryWillow extends CanneryBase {

    @Override
    public ItemStack getIcon() {
        return stack("hbm:plant_item_mustardwillow");
    }

    @Override
    public String getName() {
        return "cannery.willow";
    }

    @Override
    public JarScript createScript() {

        WorldInAJar world = new WorldInAJar(5, 5, 5);
        JarScript script = new JarScript(world);

        JarScene scene0 = new JarScene(script);
        scene0.add(new ActionSetZoom(3, 0));

        for(int x = world.sizeX - 1; x >= 0 ; x--) {
            for(int z = 0; z < world.sizeZ; z++) {
                scene0.add(new ActionSetBlock(x, 1, z, Blocks.GRASS_BLOCK));
                scene0.add(new ActionSetBlock(x, 0, z, Blocks.DIRT));
            }
            scene0.add(new ActionWait(2));
        }

        scene0.add(new ActionWait(8));
        scene0.add(new ActionSetBlock(2, 2, 2, flower(BlockNTMFlower.Type.CD0)));

        scene0.add(new ActionWait(10));

        scene0.add(new ActionCreateActor(0, new ActorFancyPanel(font(), 0, -15, new Object[][] {{resolveKey("cannery.willow.0")}}, 150)
                .setColors(colorCopper).setOrientation(Orientation.BOTTOM)));

        scene0.add(new ActionWait(80));

        scene0.add(new ActionCreateActor(0, new ActorFancyPanel(font(), 0, -15, new Object[][] {{resolveKey("cannery.willow.1")}}, 150)
                .setColors(colorCopper).setOrientation(Orientation.BOTTOM)));

        scene0.add(new ActionWait(80));
        scene0.add(new ActionRemoveActor(0));

        scene0.add(new ActionWait(10));
        scene0.add(new ActionSetBlock(2, 1, 1, Blocks.AIR));
        scene0.add(new ActionWait(10));
        scene0.add(new ActionSetBlock(2, 1, 1, Blocks.WATER));

        scene0.add(new ActionWait(20));

        scene0.add(new ActionCreateActor(0, new ActorFancyPanel(font(), 0, -15, new Object[][] {{resolveKey("cannery.willow.2")}}, 150)
                .setColors(colorCopper).setOrientation(Orientation.BOTTOM)));

        scene0.add(new ActionWait(80));
        scene0.add(new ActionRemoveActor(0));
        scene0.add(new ActionWait(20));
        scene0.add(new ActionSetBlock(2, 2, 2, flower(BlockNTMFlower.Type.CD1)));

        scene0.add(new ActionWait(20));

        scene0.add(new ActionCreateActor(0, new ActorFancyPanel(font(), 0, -15, new Object[][] {{resolveKey("cannery.willow.3")}}, 150)
                .setColors(colorCopper).setOrientation(Orientation.BOTTOM)));

        scene0.add(new ActionWait(80));
        scene0.add(new ActionRemoveActor(0));
        scene0.add(new ActionWait(20));
        scene0.add(new ActionSetBlock(2, 2, 2, tall(BlockTallPlant.Type.CD2, DoubleBlockHalf.LOWER)));
        scene0.add(new ActionSetBlock(2, 3, 2, tall(BlockTallPlant.Type.CD2, DoubleBlockHalf.UPPER)));

        scene0.add(new ActionWait(20));
        scene0.add(new ActionSetBlock(2, 2, 2, tall(BlockTallPlant.Type.CD3, DoubleBlockHalf.LOWER)));
        scene0.add(new ActionSetBlock(2, 3, 2, tall(BlockTallPlant.Type.CD3, DoubleBlockHalf.UPPER)));
        scene0.add(new ActionWait(20));

        scene0.add(new ActionCreateActor(0, new ActorFancyPanel(font(), 0, -35, new Object[][] {{resolveKey("cannery.willow.4")}}, 150)
                .setColors(colorCopper).setOrientation(Orientation.BOTTOM)));
        scene0.add(new ActionWait(80));
        scene0.add(new ActionCreateActor(0, new ActorFancyPanel(font(), 0, -35, new Object[][] {{resolveKey("cannery.willow.5")}}, 200)
                .setColors(colorCopper).setOrientation(Orientation.BOTTOM)));

        scene0.add(new ActionWait(100));
        scene0.add(new ActionRemoveActor(0));
        scene0.add(new ActionWait(20));
        scene0.add(new ActionSetBlock(2, 1, 2, block("hbm:dirt_oily")));

        scene0.add(new ActionWait(20));
        scene0.add(new ActionSetBlock(2, 2, 2, tall(BlockTallPlant.Type.CD4, DoubleBlockHalf.LOWER)));
        scene0.add(new ActionSetBlock(2, 3, 2, tall(BlockTallPlant.Type.CD4, DoubleBlockHalf.UPPER)));
        scene0.add(new ActionSetBlock(2, 1, 2, Blocks.DIRT));
        scene0.add(new ActionWait(20));

        scene0.add(new ActionCreateActor(0, new ActorFancyPanel(font(), 0, -35, new Object[][] {{resolveKey("cannery.willow.6")}}, 150)
                .setColors(colorCopper).setOrientation(Orientation.BOTTOM)));
        scene0.add(new ActionWait(100));
        scene0.add(new ActionRemoveActor(0));

        JarScene scene1 = new JarScene(script);
        scene1.add(new ActionCreateActor(0, new ActorFancyPanel(font(), 0, -35, new Object[][] {{resolveKey("cannery.willow.7")}}, 200)
                .setColors(colorCopper).setOrientation(Orientation.BOTTOM)));
        scene1.add(new ActionWait(100));
        scene1.add(new ActionRemoveActor(0));

        scene1.add(new ActionWait(20));
        scene1.add(new ActionCreateActor(0, new ActorFancyPanel(font(), 10, -25, new Object[][] {{"=", stack("hbm:plant_flower_cd0"), "x1 + ", stack("hbm:plant_item_mustardwillow"), "x3-6"}}, 0)
                .setColors(colorCopper).setOrientation(Orientation.LEFT)));
        scene1.add(new ActionSetBlock(2, 3, 2, Blocks.AIR));
        scene1.add(new ActionSetBlock(2, 2, 2, flower(BlockNTMFlower.Type.CD0)));
        scene1.add(new ActionWait(60));

        scene1.add(new ActionRemoveActor(0));
        scene1.add(new ActionWait(20));
        scene1.add(new ActionCreateActor(0, new ActorFancyPanel(font(), 0, -15, new Object[][] {{resolveKey("cannery.willow.8")}}, 200)
                .setColors(colorCopper).setOrientation(Orientation.BOTTOM)));
        scene1.add(new ActionWait(100));

        scene1.add(new ActionCreateActor(0, new ActorFancyPanel(font(), 0, -15, new Object[][] {{resolveKey("cannery.willow.9")}}, 150)
                .setColors(colorCopper).setOrientation(Orientation.BOTTOM)));
        scene1.add(new ActionWait(100));
        scene1.add(new ActionRemoveActor(0));

        script.addScene(scene0).addScene(scene1);
        return script;
    }

    /** {@code plant_flower} with the metadata of {@code type}; the port has one block per type. */
    private static BlockState flower(BlockNTMFlower.Type type) {
        return ModTerrainBlocks.plant_flower(type).defaultBlockState();
    }

    /** {@code plant_tall} with the metadata of {@code type}, plus 8 for the top half. */
    private static BlockState tall(BlockTallPlant.Type type, DoubleBlockHalf half) {
        return ModTerrainBlocks.plant_tall(type).defaultBlockState().setValue(BlockTallPlant.HALF, half);
    }
}
