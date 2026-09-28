package com.hbm.client;

import com.hbm.blocks.generic.BlockWandJigsaw;
import com.hbm.blocks.generic.BlockWandStructure;
import com.hbm.blocks.generic.BlockWandTandem;
import com.hbm.blocks.generic.ModStructureWandBlocks;
import com.hbm.client.render.HbmBlockEntityRenderers;
import com.hbm.client.render.RenderWandStructure;
import com.hbm.inventory.gui.GUIStructureLoad;
import com.hbm.inventory.gui.GUIStructureSave;
import com.hbm.inventory.gui.GUIWandJigsaw;
import com.hbm.inventory.gui.GUIWandTandem;
import net.minecraft.client.Minecraft;

/**
 * Client side of the structure wand blocks: the screens their right-click opens (1.7 opened them client-side with
 * no container) and the saving block's box renderer.
 */
public final class StructureWandClientRegistrations {

    private StructureWandClientRegistrations() {}

    public static void registerRenderers(HbmBlockEntityRenderers.Sink sink) {
        BlockWandJigsaw.OPEN_SCREEN = tile -> Minecraft.getInstance().setScreen(new GUIWandJigsaw(tile));
        BlockWandTandem.OPEN_SCREEN = tile -> Minecraft.getInstance().setScreen(new GUIWandTandem(tile));
        BlockWandStructure.OPEN_SAVE = tile -> Minecraft.getInstance().setScreen(new GUIStructureSave(tile));
        BlockWandStructure.OPEN_LOAD = tile -> Minecraft.getInstance().setScreen(new GUIStructureLoad(tile));

        sink.register(ModStructureWandBlocks.WAND_STRUCTURE_ENTITY.get(), ctx -> new RenderWandStructure());
    }
}
