package com.hbm.gametest;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModFallingBlocks;
import com.hbm.blocks.machine.rbmk.ModRBMKBlocks;
import com.hbm.entity.ModEntities;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.items.ItemAmmoEnums;
import com.hbm.items.ModAmmoItems;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.XFactoryTool;
import com.hbm.items.weapon.sedna.factory.XFactoryTurret;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Focused contracts for the formerly metadata-backed extinguisher and 240 mm ammo families. */
public final class AmmoParityGameTests {

    private AmmoParityGameTests() {}

    public static void enumCatalog(GameTestHelper helper) {
        String[] fireNames = {"ammo_fireext", "ammo_fireext_foam", "ammo_fireext_sand"};
        for (int i = 0; i < fireNames.length; i++) {
            helper.assertTrue(ItemAmmoEnums.AmmoFireExt.VALUES[i].getInternalName().equals(fireNames[i]),
                    "fire extinguisher enum order/name changed at " + i);
        }
        String[] shellNames = {"ammo_shell", "ammo_shell_explosive", "ammo_shell_apfsds_t",
                "ammo_shell_apfsds_du", "ammo_shell_w9"};
        for (int i = 0; i < shellNames.length; i++) {
            helper.assertTrue(ItemAmmoEnums.Ammo240Shell.VALUES[i].getInternalName().equals(shellNames[i]),
                    "240 mm enum order/name changed at " + i);
        }
        assertItem(helper, ModAmmoItems.fext_water.get(), fireNames[0]);
        assertItem(helper, ModAmmoItems.fext_foam.get(), fireNames[1]);
        assertItem(helper, ModAmmoItems.fext_sand.get(), fireNames[2]);
        assertItem(helper, ModAmmoItems.shell_normal.get(), shellNames[0]);
        assertItem(helper, ModAmmoItems.shell_explosive.get(), shellNames[1]);
        assertItem(helper, ModAmmoItems.shell_ap.get(), shellNames[2]);
        assertItem(helper, ModAmmoItems.shell_du.get(), shellNames[3]);
        assertItem(helper, ModAmmoItems.shell_w9.get(), shellNames[4]);
        helper.succeed();
    }

    public static void configuration(GameTestHelper helper) {
        assertExtinguisher(helper, XFactoryTool.fext_water, ModAmmoItems.fext_water.get(), 0.025F);
        assertExtinguisher(helper, XFactoryTool.fext_foam, ModAmmoItems.fext_foam.get(), 0.05F);
        assertExtinguisher(helper, XFactoryTool.fext_sand, ModAmmoItems.fext_sand.get(), 0.05F);
        assertShell(helper, XFactoryTurret.shell_normal, ModAmmoItems.shell_normal.get(), 1F, false, true,
                "240standard");
        assertShell(helper, XFactoryTurret.shell_explosive, ModAmmoItems.shell_explosive.get(), 1.5F, false, true,
                "240ext");
        assertShell(helper, XFactoryTurret.shell_ap, ModAmmoItems.shell_ap.get(), 2F, true, true, "240w");
        assertShell(helper, XFactoryTurret.shell_du, ModAmmoItems.shell_du.get(), 2.5F, true, false, "240u");
        assertShell(helper, XFactoryTurret.shell_w9, ModAmmoItems.shell_w9.get(), 2.5F, false, true, "240n");
        helper.succeed();
    }

    public static void extinguisherImpacts(GameTestHelper helper) {
        BlockPos base = helper.absolutePos(new BlockPos(5, 2, 2));
        BlockPos foam = base.east();
        helper.getLevel().setBlock(base.below(), Blocks.STONE.defaultBlockState(), 3);
        helper.getLevel().setBlock(foam.below(), Blocks.STONE.defaultBlockState(), 3);
        helper.getLevel().setBlock(base, Blocks.FIRE.defaultBlockState(), 3);
        helper.getLevel().setBlock(foam, ModRBMKBlocks.foam_layer.get().defaultBlockState(), 3);

        EntityBulletBaseMK4 water = bullet(helper, XFactoryTool.fext_water);
        XFactoryTool.LAMBDA_WATER_HIT.accept(water, hit(base, Direction.UP));
        helper.assertTrue(helper.getLevel().getBlockState(base).isAir()
                        && helper.getLevel().getBlockState(foam).isAir() && water.isRemoved(),
                "water must remove fire, foam and itself in the legacy 3x3x3 impact volume");

        BlockPos target = helper.absolutePos(new BlockPos(8, 2, 2));
        BlockPos offsetTarget = target.north();
        helper.getLevel().setBlock(target.below(), Blocks.STONE.defaultBlockState(), 3);
        helper.getLevel().setBlock(offsetTarget.below(), Blocks.STONE.defaultBlockState(), 3);
        helper.getLevel().setBlock(target, Blocks.FIRE.defaultBlockState(), 3);
        XFactoryTool.LAMBDA_FOAM_HIT.accept(bullet(helper, XFactoryTool.fext_foam), hit(target, Direction.NORTH));
        helper.assertTrue(helper.getLevel().getBlockState(target).is(ModRBMKBlocks.foam_layer.get())
                        || helper.getLevel().getBlockState(offsetTarget).is(ModRBMKBlocks.foam_layer.get()),
                "foam must use the legacy 50 percent face offset and layer block");

        BlockPos sand = helper.absolutePos(new BlockPos(11, 2, 2));
        BlockPos offsetSand = sand.north();
        helper.getLevel().setBlock(sand.below(), Blocks.STONE.defaultBlockState(), 3);
        helper.getLevel().setBlock(offsetSand.below(), Blocks.STONE.defaultBlockState(), 3);
        XFactoryTool.LAMBDA_SAND_HIT.accept(bullet(helper, XFactoryTool.fext_sand), hit(sand, Direction.NORTH));
        helper.assertTrue(helper.getLevel().getBlockState(sand).is(ModRBMKBlocks.sand_boron_layer.get())
                        || helper.getLevel().getBlockState(sand).is(ModFallingBlocks.sand_boron.get())
                        || helper.getLevel().getBlockState(offsetSand).is(ModRBMKBlocks.sand_boron_layer.get())
                        || helper.getLevel().getBlockState(offsetSand).is(ModFallingBlocks.sand_boron.get()),
                "sand must place the boron layer/full block family");
        helper.assertTrue(ModRBMKBlocks.sand_boron_layer.get().getDrops(
                        ModRBMKBlocks.sand_boron_layer.get().defaultBlockState()
                                .setValue(SnowLayerBlock.LAYERS, 7), null).isEmpty(),
                "a boron-sand layer drops nothing (1.7 BlockLayering.getItemDropped is null; fix wave 4)");
        helper.succeed();
    }

    private static EntityBulletBaseMK4 bullet(GameTestHelper helper, BulletConfig config) {
        EntityBulletBaseMK4 bullet = new EntityBulletBaseMK4(ModEntities.BULLET_MK4.get(), helper.getLevel());
        bullet.setBulletConfig(config);
        bullet.setPos(5, 3, 2);
        return bullet;
    }

    private static BlockHitResult hit(BlockPos pos, Direction direction) {
        return new BlockHitResult(Vec3.atCenterOf(pos), direction, pos, false);
    }

    private static void assertExtinguisher(GameTestHelper helper, BulletConfig config, Item item, float spread) {
        helper.assertTrue(config.ammoItem() == item && config.ammoReloadCount == 300 && config.expires == 100
                        && config.velocity == 0.75F && config.gravity == 0.04F && config.spread == spread
                        && config.onUpdate != null && config.onRicochet != null && config.onEntityHit != null,
                "extinguisher projectile configuration drifted for " + itemId(item));
    }

    private static void assertShell(GameTestHelper helper, BulletConfig config, Item item, float damage,
            boolean penetrates, boolean falloff, String casingId) {
        BulletConfig.CasingProfile casing = config.casingProfile;
        helper.assertTrue(config.ammoItem() == item && config.damageMult == damage
                        && config.doesPenetrate == penetrates && config.damageFalloffByPen == falloff
                        && casing != null && casing.id().equals(casingId) && casing.scale() == 7.5F
                        && casing.bounceYaw() == 0.02F && casing.bouncePitch() == 0.05F
                        && casing.color() == 0xEBC35E && casing.smokeChance() == 1F
                        && casing.smokeLift() == 0.5D && casing.smokeDuration() == 60
                        && casing.smokeNodeLife() == 20,
                "240 mm shell configuration drifted for " + itemId(item));
    }

    private static void assertItem(GameTestHelper helper, Item item, String id) {
        helper.assertTrue(BuiltInRegistries.ITEM.getKey(item).getPath().equals(id),
                "expected flattened ammo id " + id + ", got " + BuiltInRegistries.ITEM.getKey(item));
    }

    private static String itemId(Item item) {
        return String.valueOf(BuiltInRegistries.ITEM.getKey(item));
    }
}
