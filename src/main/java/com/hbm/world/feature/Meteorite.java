package com.hbm.world.feature;

import com.hbm.items.ModGunItems;
import com.hbm.blocks.bomb.ModBombBlocks;
import dev.golem.core.world.Compat;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.bomb.BlockTaint;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.lib.ModDamageSources;
import com.hbm.tags.HbmBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

// 1.7 world/feature/Meteorite.java; safeMode now instance
// Specials and impact damage need a real ServerLevel.
public class Meteorite {

    private boolean safeMode;

    private static BlockState bs(dev.golem.core.registry.RegistryObject<? extends Block> handle) {
        return handle.get().defaultBlockState();
    }

    private static BlockState pick(List<BlockState> l, RandomSource rand) {
        return l.get(rand.nextInt(l.size()));
    }

    public void generate(WorldGenLevel level, RandomSource rand, BlockPos pos, boolean safe, boolean allowSpecials,
                         boolean damagingImpact) {
        safeMode = safe;

        if (damagingImpact) {
            ServerLevel server = level.getLevel();
            AABB box = new AABB(pos.getX() - 7.5D, pos.getY() - 7.5D, pos.getZ() - 7.5D,
                    pos.getX() + 7.5D, pos.getY() + 7.5D, pos.getZ() + 7.5D);
            for (Entity entity : server.getEntities(null, box)) {
                Compat.hurt(entity, server, ModDamageSources.source(server, ModDamageSources.METEORITE), 1000F);
            }
        }

        if (com.hbm.config.WorldConfig.enableSpecialMeteors && allowSpecials) {
            switch (rand.nextInt(300)) {
                case 0 -> {
                    List<BlockState> list0 = new ArrayList<>();
                    list0.add(bs(ModBlocks.block_meteor));
                    generateBox(level, rand, pos, list0);
                    return;
                }
                case 1 -> {
                    List<BlockState> list1 = new ArrayList<>(getRandomOre());
                    int i = list1.size();
                    for (int j = 0; j < i; j++) list1.add(bs(ModBlocks.block_meteor_broken));
                    generateSphere7x7(level, rand, pos, list1);
                    return;
                }
                case 2 -> {
                    List<BlockState> list2 = new ArrayList<>(getRandomOre());
                    int k = list2.size() / 2;
                    for (int j = 0; j < k; j++) list2.add(bs(ModBlocks.block_meteor_broken));
                    generateSphere5x5(level, rand, pos, list2);
                    return;
                }
                case 3 -> {
                    generateBox(level, rand, pos, new ArrayList<>(getRandomOre()));
                    return;
                }
                case 4 -> {
                    explode(level, pos, 15F);
                    ExplosionLarge.spawnRubble(level.getLevel(), pos.getX(), pos.getY(), pos.getZ(), 25);
                    return;
                }
                case 5 -> {
                    List<BlockState> list4 = new ArrayList<>();
                    list4.add(bs(ModBlocks.block_meteor_treasure));
                    list4.add(bs(ModBlocks.block_meteor_broken));
                    generateSphere7x7(level, rand, pos, list4);
                    return;
                }
                case 6 -> {
                    List<BlockState> list5 = new ArrayList<>();
                    list5.add(bs(ModBlocks.block_meteor_treasure));
                    list5.add(bs(ModBlocks.block_meteor_treasure));
                    list5.add(bs(ModBlocks.block_meteor_broken));
                    generateSphere5x5(level, rand, pos, list5);
                    return;
                }
                case 7 -> {
                    List<BlockState> list6 = new ArrayList<>();
                    list6.add(bs(ModBlocks.block_meteor_treasure));
                    generateBox(level, rand, pos, list6);
                    return;
                }
                case 8 -> {
                    List<BlockState> list7 = new ArrayList<>();
                    list7.add(bs(ModBlocks.block_meteor_treasure));
                    List<BlockState> list8 = new ArrayList<>();
                    list8.add(bs(ModBlocks.block_meteor_broken) /* 1.7 toxic_block: unregistered here */);
                    generateSphere7x7(level, rand, pos, list7);
                    generateSphere5x5(level, rand, pos, list8);
                    return;
                }
                case 9 -> {
                    List<BlockState> list9 = new ArrayList<>();
                    list9.add(bs(ModBlocks.block_meteor_broken));
                    generateSphere9x9(level, rand, pos, list9);
                    generateSphere7x7(level, rand, pos, getRandomOre());
                    return;
                }
                case 10 -> {
                    List<BlockState> list10 = new ArrayList<>();
                    list10.add(bs(ModBlocks.block_meteor_broken));
                    generateSphere5x5(level, rand, pos, list10);
                    setBlock(level, pos, bs(ModBombBlocks.taint).setValue(BlockTaint.TAINTAGE, 9));
                    return;
                }
                // 1.7: no case 11; falls through
                case 12 -> {
                    explode(level, pos, 10F);
                    ItemStack blaster = new ItemStack(ModGunItems.gun_b92.get());
                    dev.golem.core.world.ItemStacks.setCustomName(blaster, Component.translatable("desc.item.gunB92.starBlaster"));
                    level.addFreshEntity(new ItemEntity(level.getLevel(), pos.getX() + 0.5D, pos.getY() + 0.5D,
                            pos.getZ() + 0.5D, blaster));
                    return;
                }
                default -> {
                }
            }
        }

        switch (rand.nextInt(3)) {
            case 0 -> generateLarge(level, rand, pos);
            case 1 -> generateMedium(level, rand, pos);
            default -> generateSmall(level, rand, pos);
        }
    }

    // 1.7 hullL/opL/ipL/coreL

    // 1.7: safeMode spares terrain, not entities
    private void explode(WorldGenLevel level, BlockPos pos, float strength) {
        level.getLevel().explode(null, null, null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                strength, false, safeMode ? Level.ExplosionInteraction.NONE : Level.ExplosionInteraction.TNT);
    }

    private void generateLarge(WorldGenLevel level, RandomSource rand, BlockPos origin) {
        int hull = rand.nextInt(4);
        int outerPadding = 0;
        if (hull == 2) outerPadding = 1 + rand.nextInt(2);
        else if (hull == 3) outerPadding = 2;
        int innerPadding = rand.nextInt(hull == 0 ? 3 : 2);
        int core = rand.nextInt(2);
        if (innerPadding > 0) core = 2;

        List<BlockState> hullL = hullList(hull);
        List<BlockState> opL = outerPaddingList(outerPadding);
        List<BlockState> ipL = innerPaddingList(innerPadding);
        List<BlockState> coreL = coreList(core);

        switch (rand.nextInt(5)) {
            case 0 -> {
                generateSphere7x7(level, rand, origin, hullL);
                generateStar5x5(level, rand, origin, opL);
                generateStar3x3(level, rand, origin, ipL);
                setBlock(level, origin, pick(coreL, rand));
            }
            case 1 -> {
                generateSphere7x7(level, rand, origin, hullL);
                generateSphere5x5(level, rand, origin, opL);
                generateStar3x3(level, rand, origin, ipL);
                setBlock(level, origin, pick(coreL, rand));
            }
            case 2 -> {
                generateSphere7x7(level, rand, origin, hullL);
                generateSphere5x5(level, rand, origin, opL);
                generateBox(level, rand, origin, ipL);
                setBlock(level, origin, pick(coreL, rand));
            }
            case 3 -> {
                generateSphere7x7(level, rand, origin, hullL);
                generateSphere5x5(level, rand, origin, opL);
                generateBox(level, rand, origin, ipL);
                generateStar3x3(level, rand, origin, getRandomOre());
                setBlock(level, origin, pick(coreL, rand));
            }
            default -> {
                generateSphere7x7(level, rand, origin, hullL);
                generateSphere5x5(level, rand, origin, opL);
                generateStar5x5(level, rand, origin, ipL);
                generateStar3x3(level, rand, origin, getRandomOre());
                setBlock(level, origin, pick(coreL, rand));
            }
        }
    }

    private void generateMedium(WorldGenLevel level, RandomSource rand, BlockPos origin) {
        int hull = rand.nextInt(4);
        int outerPadding = 0;
        if (hull == 2) outerPadding = 1 + rand.nextInt(2);
        else if (hull == 3) outerPadding = 2;
        int innerPadding = rand.nextInt(hull == 0 ? 3 : 2);
        int core = rand.nextInt(2);
        if (innerPadding > 0) core = 2;

        List<BlockState> hullL = hullList(hull);
        List<BlockState> opL = outerPaddingList(outerPadding);
        List<BlockState> ipL = innerPaddingList(innerPadding);
        List<BlockState> coreL = coreList(core);

        // 1.7: sCore independent of core roll
        List<BlockState> sCore = smallCoreList(core);

        switch (rand.nextInt(6)) {
            case 0 -> {
                generateSphere5x5(level, rand, origin, hullL);
                setBlock(level, origin, pick(sCore, rand));
            }
            case 1 -> {
                generateSphere5x5(level, rand, origin, hullL);
                generateStar3x3(level, rand, origin, opL);
                setBlock(level, origin, pick(coreL, rand));
            }
            case 2 -> {
                generateSphere5x5(level, rand, origin, hullL);
                generateBox(level, rand, origin, opL);
                setBlock(level, origin, pick(coreL, rand));
            }
            case 3 -> {
                generateSphere5x5(level, rand, origin, hullL);
                generateBox(level, rand, origin, opL);
                generateStar3x3(level, rand, origin, ipL);
                setBlock(level, origin, pick(coreL, rand));
            }
            case 4 -> {
                generateSphere5x5(level, rand, origin, hullL);
                generateBox(level, rand, origin, ipL);
                setBlock(level, origin, pick(coreL, rand));
            }
            default -> {
                generateSphere5x5(level, rand, origin, hullL);
                generateBox(level, rand, origin, ipL);
                generateStar3x3(level, rand, origin, getRandomOre());
                setBlock(level, origin, pick(coreL, rand));
            }
        }
    }

    private void generateSmall(WorldGenLevel level, RandomSource rand, BlockPos origin) {
        int hull = rand.nextInt(4);
        int core = rand.nextInt(3);

        generateBox(level, rand, origin, hullList(hull));
        setBlock(level, origin, pick(smallCoreList(core), rand));
    }

    private List<BlockState> hullList(int hull) {
        List<BlockState> l = new ArrayList<>();
        switch (hull) {
            case 0 -> l.add(bs(ModBlocks.block_meteor) /* 1.7 block_meteor_molten: unregistered here */);
            case 1 -> l.add(bs(ModBlocks.block_meteor_cobble));
            case 2 -> {
                for (int i = 0; i < 99; i++) l.add(bs(ModBlocks.block_meteor_broken));
                l.add(bs(ModBlocks.block_meteor_treasure));
            }
            case 3 -> {
                l.add(bs(ModBlocks.block_meteor) /* 1.7 block_meteor_molten: unregistered here */);
                l.add(bs(ModBlocks.block_meteor_broken));
            }
            default -> throw new IllegalStateException();
        }
        return l;
    }

    private List<BlockState> outerPaddingList(int outerPadding) {
        List<BlockState> l = new ArrayList<>();
        switch (outerPadding) {
            case 0 -> l.add(bs(ModBlocks.block_meteor_cobble));
            case 1 -> {
                for (int i = 0; i < 99; i++) l.add(bs(ModBlocks.block_meteor_broken));
                l.add(bs(ModBlocks.block_meteor_treasure));
            }
            case 2 -> {
                l.add(bs(ModBlocks.block_meteor_cobble));
                l.add(bs(ModBlocks.block_meteor_broken));
            }
            default -> throw new IllegalStateException();
        }
        return l;
    }

    private List<BlockState> innerPaddingList(int innerPadding) {
        List<BlockState> l = new ArrayList<>();
        switch (innerPadding) {
            case 0 -> {
                for (int i = 0; i < 99; i++) l.add(bs(ModBlocks.block_meteor_broken));
                l.add(bs(ModBlocks.block_meteor_treasure));
            }
            case 1 -> l.add(bs(ModBlocks.block_meteor_broken));
            default -> l.add(bs(ModBlocks.block_meteor_cobble));
        }
        return l;
    }

    private List<BlockState> coreList(int core) {
        List<BlockState> l = new ArrayList<>();
        switch (core) {
            case 0 -> l.add(bs(ModBlocks.block_meteor));
            case 1 -> l.add(bs(ModBlocks.block_meteor_treasure));
            default -> l.addAll(getRandomOre());
        }
        return l;
    }

    // shapes (1.7 generateSphereNxN/StarNxN/Box)

    private List<BlockState> smallCoreList(int core) {
        List<BlockState> l = new ArrayList<>();
        switch (core) {
            case 0 -> l.add(bs(ModBlocks.block_meteor));
            case 1 -> l.add(bs(ModBlocks.block_meteor_treasure));
            default -> {
                l.add(bs(ModBlocks.block_meteor_treasure));
                l.add(bs(ModBlocks.block_meteor));
            }
        }
        return l;
    }

    private List<BlockState> getRandomOre() {
        return List.of(bs(ModBlocks.block_meteor) /* 1.7 ore_meteor_iron: unregistered here */, bs(ModBlocks.block_meteor) /* 1.7 ore_meteor_copper: unregistered here */,
                bs(ModBlocks.block_meteor) /* 1.7 ore_meteor_aluminium: unregistered here */, bs(ModBlocks.block_meteor) /* 1.7 ore_meteor_rareearth: unregistered here */,
                bs(ModBlocks.block_meteor) /* 1.7 ore_meteor_cobalt: unregistered here */);
    }

    private void generateSphere9x9(WorldGenLevel level, RandomSource rand, BlockPos o, List<BlockState> set) {
        for (int a = -4; a < 5; a++)
            for (int b = -1; b < 2; b++)
                for (int c = -1; c < 2; c++) setBlock(level, o.offset(a, b, c), pick(set, rand));
        for (int a = -1; a < 2; a++)
            for (int b = -4; b < 5; b++)
                for (int c = -1; c < 2; c++) setBlock(level, o.offset(a, b, c), pick(set, rand));
        for (int a = -1; a < 2; a++)
            for (int b = -1; b < 2; b++)
                for (int c = -4; c < 5; c++) setBlock(level, o.offset(a, b, c), pick(set, rand));
        for (int a = -1; a < 2; a++)
            for (int b = -3; b < 4; b++)
                for (int c = -3; c < 4; c++) setBlock(level, o.offset(a, b, c), pick(set, rand));
        for (int a = -3; a < 4; a++)
            for (int b = -1; b < 2; b++)
                for (int c = -3; c < 4; c++) setBlock(level, o.offset(a, b, c), pick(set, rand));
        for (int a = -3; a < 4; a++)
            for (int b = -3; b < 4; b++)
                for (int c = -1; c < 2; c++) setBlock(level, o.offset(a, b, c), pick(set, rand));
        for (int a = -3; a < 4; a++)
            for (int b = -2; b < 3; b++)
                for (int c = -2; c < 3; c++) setBlock(level, o.offset(a, b, c), pick(set, rand));
        for (int a = -2; a < 3; a++)
            for (int b = -3; b < 4; b++)
                for (int c = -2; c < 3; c++) setBlock(level, o.offset(a, b, c), pick(set, rand));
        for (int a = -2; a < 3; a++)
            for (int b = -2; b < 3; b++)
                for (int c = -3; c < 4; c++) setBlock(level, o.offset(a, b, c), pick(set, rand));
    }

    private void generateSphere7x7(WorldGenLevel level, RandomSource rand, BlockPos o, List<BlockState> set) {
        for (int a = -3; a < 4; a++)
            for (int b = -1; b < 2; b++)
                for (int c = -1; c < 2; c++) setBlock(level, o.offset(a, b, c), pick(set, rand));
        for (int a = -1; a < 2; a++)
            for (int b = -3; b < 4; b++)
                for (int c = -1; c < 2; c++) setBlock(level, o.offset(a, b, c), pick(set, rand));
        for (int a = -1; a < 2; a++)
            for (int b = -1; b < 2; b++)
                for (int c = -3; c < 4; c++) setBlock(level, o.offset(a, b, c), pick(set, rand));
        for (int a = -2; a < 3; a++)
            for (int b = -2; b < 3; b++)
                for (int c = -1; c < 2; c++) setBlock(level, o.offset(a, b, c), pick(set, rand));
        for (int a = -1; a < 2; a++)
            for (int b = -2; b < 3; b++)
                for (int c = -2; c < 3; c++) setBlock(level, o.offset(a, b, c), pick(set, rand));
        for (int a = -2; a < 3; a++)
            for (int b = -1; b < 2; b++)
                for (int c = -2; c < 3; c++) setBlock(level, o.offset(a, b, c), pick(set, rand));
    }

    private void generateSphere5x5(WorldGenLevel level, RandomSource rand, BlockPos o, List<BlockState> set) {
        for (int a = -2; a < 3; a++)
            for (int b = -1; b < 2; b++)
                for (int c = -1; c < 2; c++) setBlock(level, o.offset(a, b, c), pick(set, rand));
        for (int a = -1; a < 2; a++)
            for (int b = -2; b < 3; b++)
                for (int c = -1; c < 2; c++) setBlock(level, o.offset(a, b, c), pick(set, rand));
        for (int a = -1; a < 2; a++)
            for (int b = -1; b < 2; b++)
                for (int c = -2; c < 3; c++) setBlock(level, o.offset(a, b, c), pick(set, rand));
    }

    private void generateBox(WorldGenLevel level, RandomSource rand, BlockPos o, List<BlockState> set) {
        for (int a = -1; a < 2; a++)
            for (int b = -1; b < 2; b++)
                for (int c = -1; c < 2; c++) setBlock(level, o.offset(a, b, c), pick(set, rand));
    }

    private void generateStar5x5(WorldGenLevel level, RandomSource rand, BlockPos o, List<BlockState> set) {
        generateBox(level, rand, o, set);
        setBlock(level, o.offset(2, 0, 0), pick(set, rand));
        setBlock(level, o.offset(-2, 0, 0), pick(set, rand));
        setBlock(level, o.offset(0, 2, 0), pick(set, rand));
        setBlock(level, o.offset(0, -2, 0), pick(set, rand));
        setBlock(level, o.offset(0, 0, 2), pick(set, rand));
        setBlock(level, o.offset(0, 0, -2), pick(set, rand));
    }

    private void generateStar3x3(WorldGenLevel level, RandomSource rand, BlockPos o, List<BlockState> set) {
        setBlock(level, o, pick(set, rand));
        setBlock(level, o.offset(1, 0, 0), pick(set, rand));
        setBlock(level, o.offset(-1, 0, 0), pick(set, rand));
        setBlock(level, o.offset(0, 1, 0), pick(set, rand));
        setBlock(level, o.offset(0, -1, 0), pick(set, rand));
        setBlock(level, o.offset(0, 0, 1), pick(set, rand));
        setBlock(level, o.offset(0, 0, -1), pick(set, rand));
    }

    // 1.7: overwrites only replaceable terrain + meteorite blocks
    private void setBlock(WorldGenLevel level, BlockPos pos, BlockState state) {
        BlockState target = level.getBlockState(pos);

        if (safeMode && !target.canBeReplaced() && !target.is(HbmBlockTags.METEORITE_REPLACEABLE)) return;

        float hardness = target.getDestroySpeed(level, pos);
        if (hardness != -1F && hardness < 10_000F) level.setBlock(pos, state, 2);
    }
}
