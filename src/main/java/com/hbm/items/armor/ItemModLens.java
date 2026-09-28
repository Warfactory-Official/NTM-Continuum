package com.hbm.items.armor;

import com.hbm.handler.ArmorModHandler;
import com.hbm.items.ISatChip;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * 1.7 {@code ItemModLens} (Neutrino Lens): a satellite chip for a helmet's special slot. While a scanner satellite
 * answers on its frequency, every tick it scans one Y layer of the 7x7 chunks around the wearer (the layer cycles with
 * world time over {@code max(min(y + 10, 255), 64)} layers, from 0 as in 1.7) and marks ores and treasure for the
 * wearer's client ({@code RenderOverhead} markers, 15 s, 300 blocks), each kind with its 1-in-N chance and colour,
 * stopping after 100 hits. Chunks that are not loaded are skipped rather than loaded.
 */
public class ItemModLens extends ItemArmorMod implements ISatChip {

    public ItemModLens(Item.Properties properties) {
        super(ArmorModHandler.extra, true, false, false, false, properties);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(coloured(ChatFormatting.AQUA, "Satellite Frequency: " + getFreq(stack)));
        tooltip.add(Component.empty());
        super.appendCompatHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void addArmorModDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(modLine(ChatFormatting.AQUA, stack, " (Freq: " + getFreq(stack) + ")"));
    }

    /** 1.7 {@code modUpdate}'s table: block id, 1-in-N chance, label (null: unlabelled), colour. */
    private static final Object[][] TARGETS = {
            {"hbm:ore_alexandrite", 1, "Alexandrite", 0x00ffff},
            {"hbm:ore_oil", 300, "Oil", 0xa0a0a0},
            {"hbm:ore_bedrock_oil", 300, "Bedrock Oil", 0xa0a0a0},
            {"hbm:ore_coltan", 5, "Coltan", 0xa0a000},
            {"hbm:stone_gneiss", 5000, "Schist", 0x8080ff},
            {"hbm:ore_australium", 1000, "Australium", 0xffff00},
            {"minecraft:end_portal_frame", 1, "End Portal", 0x40b080},
            {"hbm:volcano_core", 1, "Volcano Core", 0xff4000},
            {"hbm:pink_log", 1, "Pink Log", 0xff00ff},
            {"hbm:bobblehead", 1, "A Treasure!", 0xff0000},
            {"hbm:deco_loot", 1, null, 0x800000},
            {"hbm:crate_ammo", 1, null, 0x800000},
            {"hbm:crate_can", 1, null, 0x800000},
            {"hbm:ore_bedrock", 1, "Bedrock Ore", 0xff0000},
    };

    @Override
    public void modUpdate(net.minecraft.world.entity.LivingEntity entity, ItemStack armor) {
        if (!(entity instanceof net.minecraft.server.level.ServerPlayer player)) return;
        if (!(player.level() instanceof net.minecraft.server.level.ServerLevel level)) return;
        ItemStack lens = com.hbm.handler.ArmorModHandler.pryMods(armor)[com.hbm.handler.ArmorModHandler.extra];
        if (lens == null || lens.isEmpty()) return;

        int freq = getFreq(lens);
        if (!(com.hbm.saveddata.SatelliteSavedData.get(level).getSatFromFreq(freq)
                instanceof com.hbm.saveddata.satellites.SatelliteScanner)) return;

        int x = net.minecraft.util.Mth.floor(player.getX());
        int y = net.minecraft.util.Mth.floor(player.getY());
        int z = net.minecraft.util.Mth.floor(player.getZ());
        int range = 3;
        int cX = x >> 4;
        int cZ = z >> 4;
        int height = Math.max(Math.min(y + 10, 255), 64);
        int seg = (int) (level.getGameTime() % height);

        net.minecraft.world.level.block.Block[] blocks = new net.minecraft.world.level.block.Block[TARGETS.length];
        for (int i = 0; i < TARGETS.length; i++) {
            blocks[i] = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(dev.golem.core.Ids.parse((String) TARGETS[i][0]))
                    .orElse(net.minecraft.world.level.block.Blocks.AIR);
        }

        int hits = 0;
        net.minecraft.core.BlockPos.MutableBlockPos pos = new net.minecraft.core.BlockPos.MutableBlockPos();
        for (int chunkX = cX - range; chunkX <= cX + range; chunkX++) {
            for (int chunkZ = cZ - range; chunkZ <= cZ + range; chunkZ++) {
                net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) continue;
                for (int ix = 0; ix < 16; ix++) {
                    for (int iz = 0; iz < 16; iz++) {
                        int aX = (chunkX << 4) + ix;
                        int aZ = (chunkZ << 4) + iz;
                        net.minecraft.world.level.block.Block b = chunk.getBlockState(pos.set(aX, seg, aZ)).getBlock();
                        for (int i = 0; i < TARGETS.length; i++) {
                            if (blocks[i] == net.minecraft.world.level.block.Blocks.AIR || blocks[i] != b) continue;
                            if (player.getRandom().nextInt((Integer) TARGETS[i][1]) == 0) {
                                com.hbm.util.OverheadMarkers.send(player, new net.minecraft.core.BlockPos(aX, seg, aZ),
                                        (Integer) TARGETS[i][3], (String) TARGETS[i][2], 15_000, 300D);
                                hits++;
                            }
                        }
                        if (hits > 100) return;
                    }
                }
            }
        }
    }
}
