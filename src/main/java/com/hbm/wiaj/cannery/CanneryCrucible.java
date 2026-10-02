package com.hbm.wiaj.cannery;

import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.machine.ItemScraps;
import com.hbm.tileentity.machine.TileEntityCrucible;
import com.hbm.tileentity.machine.TileEntityFoundryBasin;
import com.hbm.tileentity.machine.TileEntityFoundryCastingBase;
import com.hbm.wiaj.JarScene;
import com.hbm.wiaj.JarScript;
import com.hbm.wiaj.WorldInAJar;
import com.hbm.wiaj.actions.ActionCreateActor;
import com.hbm.wiaj.actions.ActionRemoveActor;
import com.hbm.wiaj.actions.ActionRotateBy;
import com.hbm.wiaj.actions.ActionSetBlock;
import com.hbm.wiaj.actions.ActionSetTile;
import com.hbm.wiaj.actions.ActionSetZoom;
import com.hbm.wiaj.actions.ActionUpdateActor;
import com.hbm.wiaj.actions.ActionWait;
import com.hbm.wiaj.actors.ActorFancyPanel;
import com.hbm.wiaj.actors.ActorFancyPanel.Orientation;
import com.hbm.wiaj.actors.ActorTileEntity;
import com.hbm.wiaj.actors.JarRenderers;
import com.hbm.wiaj.cannery.CanneryFirebox.ActorFirebox;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jspecify.annotations.Nullable;

public class CanneryCrucible extends CanneryBase {

    @Override
    public ItemStack getIcon() {
        return stack("hbm:machine_crucible");
    }

    @Override
    public String getName() {
        return "cannery.crucible";
    }

    public CanneryBase[] seeAlso() {
        return new CanneryBase[] {
                new CanneryFoundryChannel()
        };
    }

    @Override
    public JarScript createScript() {
        WorldInAJar world = new WorldInAJar(5, 4, 5);
        JarScript script = new JarScript(world);

        JarScene scene0 = new JarScene(script);
        scene0.add(new ActionSetZoom(3, 0));

        for(int x = 0; x < 5 ; x++) {
            for(int z = 0; z < 5; z++) {
                scene0.add(new ActionSetBlock(x, 0, z, Blocks.BRICKS));
            }
        }
        scene0.add(new ActionWait(5));
        for(int x = 1; x < 4 ; x++) {
            for(int z = 1; z < 4; z++) {
                scene0.add(new ActionSetBlock(x, 1, z, Blocks.BRICKS));
            }
        }
        scene0.add(new ActionWait(5));

        scene0.add(new ActionSetTile(2, 2, 2, crucible(null)));
        scene0.add(new ActionCreateActor(1, new ActorTileEntity(new JarRenderers.Crucible(), at(2, 2, 2))));

        scene0.add(new ActionWait(10));
        scene0.add(new ActionCreateActor(0, new ActorFancyPanel(font(), 0, -30, new Object[][] {{resolveKey("cannery.crucible.0")}}, 200)
                .setColors(colorCopper).setOrientation(Orientation.BOTTOM)));

        scene0.add(new ActionWait(60));
        scene0.add(new ActionRemoveActor(0));
        scene0.add(new ActionCreateActor(0, new ActorFancyPanel(font(), 0, -30, new Object[][] {{resolveKey("cannery.crucible.1")}}, 200)
                .setColors(colorCopper).setOrientation(Orientation.BOTTOM)));

        scene0.add(new ActionWait(60));
        scene0.add(new ActionRemoveActor(0));
        scene0.add(new ActionWait(10));

        for(int x = 1; x < 4 ; x++) for(int z = 1; z < 4; z++) scene0.add(new ActionSetBlock(x, 1, z, Blocks.AIR));

        CompoundTag firebox = new CompoundTag(); firebox.putDouble("x", 2); firebox.putDouble("y", 1); firebox.putDouble("z", 2); firebox.putInt("rotation", 5);
        scene0.add(new ActionCreateActor(2, new ActorTileEntity(new ActorFirebox(), firebox)));

        scene0.add(new ActionUpdateActor(2, "open", true));
        scene0.add(new ActionWait(30));
        scene0.add(new ActionUpdateActor(2, "isOn", true));

        scene0.add(new ActionCreateActor(0, new ActorFancyPanel(font(), -50, 25, new Object[][] {{new ItemStack(Items.COAL)}}, 0)
                .setColors(colorCopper).setOrientation(Orientation.RIGHT)));

        scene0.add(new ActionWait(20));
        scene0.add(new ActionRemoveActor(0));
        scene0.add(new ActionWait(10));
        scene0.add(new ActionUpdateActor(2, "open", false));
        scene0.add(new ActionWait(30));

        JarScene scene1 = new JarScene(script);

        scene1.add(new ActionCreateActor(0, new ActorFancyPanel(font(), 0, -30, new Object[][] {{resolveKey("cannery.crucible.2")}}, 200)
                .setColors(colorCopper).setOrientation(Orientation.BOTTOM)));
        scene1.add(new ActionWait(60));

        scene1.add(new ActionRemoveActor(0));
        scene1.add(new ActionRotateBy(45, -60, 20));
        scene1.add(new ActionWait(10));

        scene1.add(new ActionCreateActor(0, new ActorFancyPanel(font(), 0, 0, new Object[][] {{resolveKey("cannery.crucible.3")}}, 200)
                .setColors(colorCopper).setOrientation(Orientation.CENTER)));
        scene1.add(new ActionWait(40));

        scene1.add(new ActionRemoveActor(0));
        scene1.add(new ActionCreateActor(0, new ActorFancyPanel(font(), -30, 0, new Object[][] {{resolveKey("cannery.crucible.4")}}, 200)
                .setColors(colorCopper).setOrientation(Orientation.LEFT)));
        scene1.add(new ActionWait(60));

        scene1.add(new ActionRemoveActor(0));
        scene1.add(new ActionCreateActor(0, new ActorFancyPanel(font(), -30, 0, new Object[][] {{resolveKey("cannery.crucible.5")}}, 200)
                .setColors(colorCopper).setOrientation(Orientation.LEFT)));
        scene1.add(new ActionWait(60));

        scene1.add(new ActionRemoveActor(0));
        scene1.add(new ActionCreateActor(0, new ActorFancyPanel(font(), -30, 0, new Object[][] {{resolveKey("cannery.crucible.6")}}, 200)
                .setColors(colorCopper).setOrientation(Orientation.LEFT)));
        scene1.add(new ActionWait(60));

        scene1.add(new ActionRemoveActor(0));
        scene1.add(new ActionWait(20));
        scene1.add(new ActionCreateActor(0, new ActorFancyPanel(font(), 30, 0, new Object[][] {{resolveKey("cannery.crucible.7")}}, 200)
                .setColors(colorCopper).setOrientation(Orientation.RIGHT)));
        scene1.add(new ActionWait(60));

        scene1.add(new ActionRemoveActor(0));
        scene1.add(new ActionCreateActor(0, new ActorFancyPanel(font(), 30, 0, new Object[][] {{resolveKey("cannery.crucible.8")}}, 200)
                .setColors(colorCopper).setOrientation(Orientation.RIGHT)));
        scene1.add(new ActionWait(60));

        scene1.add(new ActionRemoveActor(0));
        scene1.add(new ActionCreateActor(0, new ActorFancyPanel(font(), 30, 0, new Object[][] {{resolveKey("cannery.crucible.9")}}, 200)
                .setColors(colorCopper).setOrientation(Orientation.RIGHT)));
        scene1.add(new ActionWait(60));

        scene1.add(new ActionRemoveActor(0));
        scene1.add(new ActionRotateBy(-45, 60, 20));
        scene1.add(new ActionWait(10));

        JarScene scene2 = new JarScene(script);
        scene2.add(new ActionCreateActor(0, new ActorFancyPanel(font(), 30, 0, new Object[][] {{resolveKey("cannery.crucible.10")}}, 200)
                .setColors(colorCopper).setOrientation(Orientation.RIGHT)));
        scene2.add(new ActionWait(60));

        scene2.add(new ActionRemoveActor(0));
        scene2.add(new ActionWait(10));
        scene2.add(new ActionCreateActor(0, new ActorFancyPanel(font(), 0, -40, new Object[][] {{new ItemStack(Items.GOLD_INGOT, 11)}}, 0)
                .setColors(colorCopper).setOrientation(Orientation.BOTTOM)));
        scene2.add(new ActionWait(20));

        scene2.add(new ActionRemoveActor(0));
        scene2.add(new ActionSetTile(2, 2, 2, crucible(new MaterialStack(Mats.MAT_GOLD, q(MaterialShapes.BLOCK, 16)))));
        scene2.add(new ActionWait(10));

        scene2.add(new ActionSetTile(0, 1, 2, basin(ItemStack.EMPTY, ItemStack.EMPTY, null, 0)));
        scene2.add(new ActionCreateActor(3, new ActorTileEntity(new JarRenderers.Foundry(), at(0, 1, 2))));
        scene2.add(new ActionSetBlock(0, 1, 2, block("hbm:foundry_basin")));

        scene2.add(new ActionCreateActor(0, new ActorFancyPanel(font(), 42, 25, new Object[][] {{stack("hbm:mold_block")}}, 0)
                .setColors(colorCopper).setOrientation(Orientation.BOTTOM)));
        scene2.add(new ActionWait(20));
        scene2.add(new ActionRemoveActor(0));
        scene2.add(new ActionSetTile(0, 1, 2, basin(stack("hbm:mold_block"), ItemStack.EMPTY, null, 0)));

        scene2.add(new ActionWait(20));

        for(int i = 0; i < 60; i++) {
            final int j = i;
            scene2.add(new ActionSetTile(2, 2, 2, crucible(new MaterialStack(Mats.MAT_GOLD, q(MaterialShapes.BLOCK, 60 - j, 60) * 16))));
            scene2.add(new ActionSetTile(0, 1, 2, basin(stack("hbm:mold_block"), ItemStack.EMPTY, Mats.MAT_GOLD, q(MaterialShapes.BLOCK, j + 1, 60))));
            scene2.add(new ActionWait(1));
        }

        scene2.add(new ActionWait(40));
        scene2.add(new ActionSetTile(0, 1, 2, basin(stack("hbm:mold_block"), new ItemStack(Blocks.GOLD_BLOCK), null, 0)));
        scene2.add(new ActionWait(20));
        scene2.add(new ActionSetTile(0, 1, 2, basin(stack("hbm:mold_block"), ItemStack.EMPTY, null, 0)));
        scene2.add(new ActionCreateActor(0, new ActorFancyPanel(font(), 42, 25, new Object[][] {{new ItemStack(Blocks.GOLD_BLOCK)}}, 0)
                .setColors(colorCopper).setOrientation(Orientation.BOTTOM)));
        scene2.add(new ActionWait(20));
        scene2.add(new ActionRemoveActor(0));

        scene2.add(new ActionCreateActor(0, new ActorFancyPanel(font(), 0, -30, new Object[][] {{resolveKey("cannery.crucible.11")}}, 200)
                .setColors(colorCopper).setOrientation(Orientation.BOTTOM)));
        scene2.add(new ActionWait(60));
        scene2.add(new ActionRemoveActor(0));
        scene2.add(new ActionCreateActor(0, new ActorFancyPanel(font(), 0, -30, new Object[][] {{new ItemStack(Items.IRON_SHOVEL), " -> ", ItemScraps.create(new MaterialStack(Mats.MAT_GOLD, 1))}}, 200)
                .setColors(colorCopper).setOrientation(Orientation.BOTTOM)));
        scene2.add(new ActionSetTile(2, 2, 2, crucible(null)));
        scene2.add(new ActionWait(20));
        scene2.add(new ActionRemoveActor(0));

        script.addScene(scene0).addScene(scene1).addScene(scene2);

        return script;
    }

    /** Actor data pointing at a jar tile, 1.7's {@code setInteger("x"/"y"/"z")}. */
    static CompoundTag at(int x, int y, int z) {
        CompoundTag data = new CompoundTag();
        data.putInt("x", x);
        data.putInt("y", y);
        data.putInt("z", z);
        return data;
    }

    /** 1.7 {@code MaterialShapes.q(unit)}. */
    static int q(MaterialShapes shape, int unit) {
        return shape.quantity * unit;
    }

    /** 1.7 {@code MaterialShapes.q(unit, parts)}. */
    static int q(MaterialShapes shape, int unit, int parts) {
        return shape.quantity * unit / parts;
    }

    /**
     * Upstream's {@code TileEntityFauxCrucible { meta = 14; }}: a crucible that reports metadata 14, the core
     * of a crucible facing west, and optionally holds one recipe stack.
     */
    static TileEntityCrucible crucible(@Nullable MaterialStack recipe) {
        BlockState state = block("hbm:machine_crucible");
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST);
        }
        TileEntityCrucible crucible = new TileEntityCrucible(BlockPos.ZERO, state);
        if (recipe != null) crucible.recipeStack.add(recipe);
        return crucible;
    }

    /**
     * A basin holding {@code mold} and {@code output} and {@code amount} of {@code type}. The mold goes
     * where the client-side renderer reads it, the synced copy. 1.7 wrote the mold as a stack of size 0,
     * which is simply the empty stack now, so the stacks here are of size 1.
     */
    static TileEntityFoundryBasin basin(ItemStack mold, ItemStack output, @Nullable NTMMaterial type, int amount) {
        return casting(new TileEntityFoundryBasin(BlockPos.ZERO, block("hbm:foundry_basin")), mold, output, type, amount);
    }

    static <T extends TileEntityFoundryCastingBase> T casting(T tile, ItemStack mold, ItemStack output,
                                                              @Nullable NTMMaterial type, int amount) {
        tile.syncMold = mold;
        tile.syncOutput = output;
        tile.type = type;
        tile.amount = amount;
        return tile;
    }
}
