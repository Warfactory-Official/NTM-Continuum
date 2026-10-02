package com.hbm.wiaj.cannery;

import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.machine.ItemScraps;
import com.hbm.tileentity.machine.TileEntityFoundryChannel;
import com.hbm.tileentity.machine.TileEntityFoundryMold;
import com.hbm.tileentity.machine.TileEntityFoundryOutlet;
import com.hbm.tileentity.machine.TileEntityFoundryTank;
import com.hbm.wiaj.JarScene;
import com.hbm.wiaj.JarScript;
import com.hbm.wiaj.WorldInAJar;
import com.hbm.wiaj.actions.ActionCreateActor;
import com.hbm.wiaj.actions.ActionRemoveActor;
import com.hbm.wiaj.actions.ActionSetBlock;
import com.hbm.wiaj.actions.ActionSetTile;
import com.hbm.wiaj.actions.ActionSetZoom;
import com.hbm.wiaj.actions.ActionWait;
import com.hbm.wiaj.actors.ActorFancyPanel;
import com.hbm.wiaj.actors.ActorFancyPanel.Orientation;
import com.hbm.wiaj.actors.ActorTileEntity;
import com.hbm.wiaj.actors.JarRenderers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jspecify.annotations.Nullable;

import static com.hbm.wiaj.cannery.CanneryCrucible.at;
import static com.hbm.wiaj.cannery.CanneryCrucible.basin;
import static com.hbm.wiaj.cannery.CanneryCrucible.casting;
import static com.hbm.wiaj.cannery.CanneryCrucible.q;

public class CanneryFoundryChannel extends CanneryBase {

    @Override
    public ItemStack getIcon() {
        return stack("hbm:foundry_channel");
    }

    @Override
    public String getName() {
        return "cannery.foundryChannel";
    }

    public CanneryBase[] seeAlso() {
        return new CanneryBase[] {
                new CanneryCrucible()
        };
    }

    @Override
    public JarScript createScript() {
        WorldInAJar world = new WorldInAJar(5, 4, 4);
        JarScript script = new JarScript(world);

        JarScene scene0 = new JarScene(script);

        /// SETUP ///
        scene0.add(new ActionSetZoom(3, 0));

        for(int x = world.sizeX - 1; x >= 0 ; x--) {
            for(int z = 0; z < world.sizeZ; z++) {
                scene0.add(new ActionSetBlock(x, 0, z, Blocks.BRICKS));
            }
        }

        scene0.add(new ActionWait(5));
        scene0.add(new ActionSetBlock(1, 1, 2, Blocks.BRICKS));
        scene0.add(new ActionSetBlock(2, 1, 2, Blocks.BRICKS));
        scene0.add(new ActionSetBlock(3, 1, 2, Blocks.BRICKS));
        scene0.add(new ActionSetBlock(3, 1, 3, Blocks.BRICKS));

        scene0.add(new ActionSetTile(3, 1, 1, basin(stack("hbm:mold_block"), ItemStack.EMPTY, null, 0)));
        scene0.add(new ActionCreateActor(1, new ActorTileEntity(new JarRenderers.Foundry(), at(3, 1, 1))));
        scene0.add(new ActionSetBlock(3, 1, 1, block("hbm:foundry_basin")));
        scene0.add(new ActionSetTile(2, 1, 1, mold(stack("hbm:mold_ingot"), ItemStack.EMPTY, null, 0)));
        scene0.add(new ActionCreateActor(2, new ActorTileEntity(new JarRenderers.Foundry(), at(2, 1, 1))));
        scene0.add(new ActionSetBlock(2, 1, 1, block("hbm:foundry_mold")));

        scene0.add(new ActionWait(5));
        scene0.add(new ActionSetBlock(3, 2, 3, Blocks.BRICKS));
        scene0.add(new ActionSetTile(1, 2, 2, mold(stack("hbm:mold_ingot"), ItemStack.EMPTY, null, 0)));
        scene0.add(new ActionCreateActor(3, new ActorTileEntity(new JarRenderers.Foundry(), at(1, 2, 2))));
        scene0.add(new ActionSetBlock(1, 2, 2, block("hbm:foundry_mold")));
        scene0.add(new ActionSetTile(2, 2, 2, channel(null, 0)));
        scene0.add(new ActionSetBlock(2, 2, 2, block("hbm:foundry_channel")));
        scene0.add(new ActionSetTile(3, 2, 2, channel(null, 0)));
        scene0.add(new ActionSetBlock(3, 2, 2, block("hbm:foundry_channel")));

        scene0.add(new ActionSetTile(2, 2, 1, outlet()));
        scene0.add(new ActionSetBlock(2, 2, 1, outletState()));
        scene0.add(new ActionSetTile(3, 2, 1, outlet()));
        scene0.add(new ActionSetBlock(3, 2, 1, outletState()));
        scene0.add(new ActionWait(5));
        scene0.add(new ActionSetTile(3, 3, 3, tank(Mats.MAT_GOLD, q(MaterialShapes.BLOCK, 1))));
        scene0.add(new ActionSetBlock(3, 3, 3, block("hbm:foundry_tank")));
        scene0.add(new ActionSetTile(3, 3, 2, outlet()));
        scene0.add(new ActionSetBlock(3, 3, 2, outletState()));

        scene0.add(new ActionWait(10));

        scene0.add(new ActionCreateActor(0, new ActorFancyPanel(font(), 0, -25, new Object[][] {{resolveKey("cannery.foundryChannel.0")}}, 200)
                .setColors(colorCopper).setOrientation(Orientation.LEFT)));

        scene0.add(new ActionWait(60));
        scene0.add(new ActionRemoveActor(0));
        scene0.add(new ActionCreateActor(0, new ActorFancyPanel(font(), -5, -40, new Object[][] {{resolveKey("cannery.foundryChannel.1")}}, 200)
                .setColors(colorCopper).setOrientation(Orientation.TOP)));

        scene0.add(new ActionWait(60));
        scene0.add(new ActionRemoveActor(0));
        scene0.add(new ActionWait(10));
        scene0.add(new ActionSetTile(3, 2, 2, channel(Mats.MAT_GOLD, q(MaterialShapes.INGOT, 1))));

        scene0.add(new ActionWait(10));
        scene0.add(new ActionCreateActor(0, new ActorFancyPanel(font(), 0, -25, new Object[][] {{resolveKey("cannery.foundryChannel.2")}}, 200)
                .setColors(colorCopper).setOrientation(Orientation.LEFT)));

        scene0.add(new ActionWait(60));
        scene0.add(new ActionRemoveActor(0));

        for(int i = 0; i < 60; i++) {
            final int j = i;
            scene0.add(new ActionSetTile(3, 1, 1, basin(stack("hbm:mold_block"), ItemStack.EMPTY, Mats.MAT_GOLD, q(MaterialShapes.BLOCK, j, 60))));
            scene0.add(new ActionWait(1));
        }

        JarScene scene1 = new JarScene(script);

        scene1.add(new ActionWait(10));
        scene1.add(new ActionCreateActor(0, new ActorFancyPanel(font(), 0, -25, new Object[][] {{resolveKey("cannery.foundryChannel.3")}}, 200)
                .setColors(colorCopper).setOrientation(Orientation.LEFT)));

        scene1.add(new ActionWait(60));
        scene1.add(new ActionRemoveActor(0));

        scene1.add(new ActionWait(10));
        scene1.add(new ActionSetTile(2, 2, 2, channel(Mats.MAT_GOLD, q(MaterialShapes.INGOT, 1))));
        scene1.add(new ActionWait(10));

        for(int i = 0; i < 60; i++) {
            final int j = i;
            scene1.add(new ActionSetTile(2, 1, 1, mold(stack("hbm:mold_ingot"), ItemStack.EMPTY, Mats.MAT_GOLD, q(MaterialShapes.INGOT, j, 60))));
            scene1.add(new ActionSetTile(1, 2, 2, mold(stack("hbm:mold_ingot"), ItemStack.EMPTY, Mats.MAT_GOLD, q(MaterialShapes.INGOT, j, 60))));
            scene1.add(new ActionWait(1));
        }

        scene1.add(new ActionWait(2));
        scene1.add(new ActionSetTile(3, 3, 3, tank(null, 0)));
        scene1.add(new ActionWait(20));

        scene1.add(new ActionCreateActor(0, new ActorFancyPanel(font(), 0, -25, new Object[][] {{resolveKey("cannery.foundryChannel.4")}}, 200)
                .setColors(colorCopper).setOrientation(Orientation.LEFT)));
        scene1.add(new ActionWait(40));
        scene1.add(new ActionRemoveActor(0));
        scene1.add(new ActionWait(10));

        scene1.add(new ActionSetTile(2, 2, 2, channel(null, 0)));
        scene1.add(new ActionSetTile(3, 2, 2, channel(null, 0)));
        scene1.add(new ActionCreateActor(0, new ActorFancyPanel(font(), 0, -25, new Object[][] {{new ItemStack(Items.IRON_SHOVEL), " -> ", ItemScraps.create(new MaterialStack(Mats.MAT_GOLD, 1))}}, 200)
                .setColors(colorCopper).setOrientation(Orientation.LEFT)));
        scene1.add(new ActionWait(40));
        scene1.add(new ActionRemoveActor(0));

        scene1.add(new ActionWait(60));
        // Upstream's last three tiles hold only the output: the mold item itself is gone from them.
        scene1.add(new ActionSetTile(2, 1, 1, mold(ItemStack.EMPTY, new ItemStack(Items.GOLD_INGOT), null, 0)));
        scene1.add(new ActionSetTile(1, 2, 2, mold(ItemStack.EMPTY, new ItemStack(Items.GOLD_INGOT), null, 0)));
        scene1.add(new ActionSetTile(3, 1, 1, basin(ItemStack.EMPTY, new ItemStack(Blocks.GOLD_BLOCK), null, 0)));

        script.addScene(scene0);
        script.addScene(scene1);

        return script;
    }

    static TileEntityFoundryMold mold(ItemStack mold, ItemStack output, @Nullable NTMMaterial type, int amount) {
        return casting(new TileEntityFoundryMold(BlockPos.ZERO, block("hbm:foundry_mold")), mold, output, type, amount);
    }

    static TileEntityFoundryChannel channel(@Nullable NTMMaterial type, int amount) {
        TileEntityFoundryChannel channel = new TileEntityFoundryChannel(BlockPos.ZERO, block("hbm:foundry_channel"));
        channel.type = type;
        channel.amount = amount;
        return channel;
    }

    static TileEntityFoundryTank tank(@Nullable NTMMaterial type, int amount) {
        TileEntityFoundryTank tank = new TileEntityFoundryTank(BlockPos.ZERO, block("hbm:foundry_tank"));
        tank.type = type;
        tank.amount = amount;
        return tank;
    }

    /**
     * Upstream's {@code TileEntityFauxOutlet}, whose {@code isClosed} is false. The port's outlet reads its
     * redstone from the level, and a tile outside one has none, so a plain outlet already answers the same.
     */
    static TileEntityFoundryOutlet outlet() {
        return new TileEntityFoundryOutlet(BlockPos.ZERO, outletState());
    }

    /** 1.7 {@code foundry_outlet} metadata 2: facing north. */
    static BlockState outletState() {
        BlockState state = block("hbm:foundry_outlet");
        return state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? state.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH) : state;
    }
}
