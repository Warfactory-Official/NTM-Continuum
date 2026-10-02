package com.hbm.gametest;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModFallingBlocks;
import com.hbm.blocks.ModFalloutBlocks;
import com.hbm.blocks.ModHazardBlocks;
import com.hbm.blocks.bomb.ModBombBlocks;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.entity.ModEntities;
import com.hbm.entity.effect.EntityMist;
import com.hbm.entity.projectile.EntityChemical;
import com.hbm.interfaces.IRepairable;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.tileentity.machine.oil.TileEntityMachineRefinery;
import com.hbm.tileentity.turret.TileEntityTurretFritz;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.world.Effects;
import dev.golem.core.world.LightLevels;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 {@code EntityChemical} drag and block impact, the creative-player rule shared with {@code EntityMist}, the
 * mist's final-tick hit and its armour wear on every mob, and the Fritz turret's 1.7 fuel tests. Every expected
 * number is worked out from the 1.7 code in the javadoc. Registered from {@link HbmGameTestFunctions}.
 */
public final class ChemicalImpactGameTests {

    private ChemicalImpactGameTests() {}

    private static EntityChemical spray(GameTestHelper helper, FluidType type) {
        EntityChemical chemical = new EntityChemical(ModEntities.CHEMICAL.get(), helper.getLevel());
        chemical.setFluid(type);
        return chemical;
    }

    private static void impact(GameTestHelper helper, FluidType type, BlockPos rel) {
        BlockPos abs = helper.absolutePos(rel);
        spray(helper, type).blockImpact(new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false));
    }

    private static LivingEntity pig(GameTestHelper helper, BlockPos pos) {
        LivingEntity pig = helper.spawnWithNoFreeWill(EntityType.PIG, pos);
        pig.setNoGravity(true);
        pig.setDeltaMovement(Vec3.ZERO);
        return pig;
    }

    private static boolean is(GameTestHelper helper, BlockPos rel, Block block) {
        return helper.getLevel().getBlockState(helper.absolutePos(rel)).is(block);
    }

    /**
     * 1.7 {@code getAirDrag}/{@code getWaterDrag}: only GAS gets 0.95 air and 1.0 water drag; GASFLAME falls to the
     * 0.99 / 0.8 defaults. One tick of natural gas (GASFLAME, gravity -0.01) launched at (0.5, 0, 0) in open air
     * leaves {@code x = 0.5 * 0.99 = 0.495} and {@code y = 0 * 0.99 + 0.01 = 0.01}; over its 20-tick life it keeps
     * {@code 0.99^20 = 0.818} of its speed rather than {@code 0.95^20 = 0.358}.
     */
    public static void drag(GameTestHelper helper) {
        EntityChemical flame = spray(helper, Fluids.GAS);
        EntityChemical gas = spray(helper, Fluids.CARBONDIOXIDE);
        helper.assertTrue(flame.style() == EntityChemical.ChemicalStyle.GASFLAME, "natural gas sprays as GASFLAME");
        helper.assertTrue(gas.style() == EntityChemical.ChemicalStyle.GAS, "CO2 sprays as GAS");
        helper.assertTrue(flame.getAirDrag() == 0.99F && flame.getWaterDrag() == 0.8F,
                "GASFLAME drag must be 1.7's 0.99 air / 0.8 water, is " + flame.getAirDrag() + " / " + flame.getWaterDrag());
        helper.assertTrue(gas.getAirDrag() == 0.95F && gas.getWaterDrag() == 1F,
                "GAS drag must be 0.95 air / 1.0 water, is " + gas.getAirDrag() + " / " + gas.getWaterDrag());

        Vec3 start = helper.absoluteVec(new Vec3(2.5D, 6.5D, 2.5D));
        flame.setPos(start.x, start.y, start.z);
        flame.setDeltaMovement(0.5D, 0D, 0D);
        flame.tick();
        Vec3 v = flame.getDeltaMovement();
        helper.assertTrue(Math.abs(v.x - 0.495D) < 1.0E-6D && Math.abs(v.y - 0.01D) < 1.0E-6D,
                "one air tick of a gas flame must leave (0.495, 0.01), has " + v);
        flame.discard();
        gas.discard();
        helper.succeed();
    }

    /**
     * 1.7's spray and mist scans have no creative filter. A creative player sprayed with modified pheromone gets
     * the spray table (resistance III 2,400 t, speed II 6,000 t, haste V 2,400 t, and strength III 2,400 t for a
     * player and pheromone type 2), and experience juice gives them one point (total 1). Only spectators, which 1.7
     * lacks, are skipped. Sulfuric acid (rating 50) wears {@code 50 / 40 = 1} durability off a pig's helmet, but
     * none off a creative player's: 1.7's {@code damageItem} never wears a creative player's items.
     */
    public static void creative(GameTestHelper helper) {
        LivingEntity pig = pig(helper, new BlockPos(1, 3, 1));
        helper.runAfterDelay(1, () -> {
            Player creative = GameTestHelpers.mockPlayer(helper, GameType.CREATIVE);
            // A real creative player's abilities (26.1's mock only reports the game mode): without invulnerability
            // vanilla's own hurtArmor would wear the helmet on the acid hit, which is not what this test is about.
            creative.getAbilities().invulnerable = true;
            creative.getAbilities().instabuild = true;
            Player spectator = GameTestHelpers.mockPlayer(helper, GameType.SPECTATOR);
            helper.assertTrue(EntityChemical.isAffected(creative) && EntityMist.isAffected(creative),
                    "sprays and clouds must act on a creative player, as 1.7");
            helper.assertTrue(!EntityChemical.isAffected(spectator) && !EntityMist.isAffected(spectator),
                    "spectators are skipped");

            spray(helper, Fluids.PHEROMONE_M).affect(creative, 1D);
            expect(helper, creative, Effects.resistance(), 2400, 2, "creative spray resistance");
            expect(helper, creative, Effects.speed(), 6000, 1, "creative spray speed");
            expect(helper, creative, Effects.haste(), 2400, 4, "creative spray haste");
            expect(helper, creative, Effects.strength(), 2400, 2, "creative spray strength");

            creative.experienceLevel = 0;
            creative.experienceProgress = 0F;
            creative.totalExperience = 0;
            EntityChemical xp = spray(helper, Fluids.XPJUICE);
            xp.affect(creative, 1D);
            helper.assertTrue(creative.totalExperience == 1 && xp.isRemoved(),
                    "experience juice gives a creative player 1 point and is used up, total " + creative.totalExperience);

            creative.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            spray(helper, Fluids.SULFURIC_ACID).affect(creative, 1D);
            helper.assertTrue(creative.getItemBySlot(EquipmentSlot.HEAD).getDamageValue() == 0,
                    "acid must not wear a creative player's helmet, damage "
                            + creative.getItemBySlot(EquipmentSlot.HEAD).getDamageValue());
            pig.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            spray(helper, Fluids.SULFURIC_ACID).affect(pig, 1D);
            helper.assertTrue(pig.getItemBySlot(EquipmentSlot.HEAD).getDamageValue() == 1,
                    "acid rating 50 / 40 = 1 durability off a pig's helmet, has "
                            + pig.getItemBySlot(EquipmentSlot.HEAD).getDamageValue());
            pig.discard();
            helper.succeed();
        });
    }

    /**
     * 1.7 {@code EntityMist}: at {@code ticksExisted >= maxAge} it calls {@code setDead()} but still runs that tick, so
     * a 300 °C hot-steam cloud deals its unscaled boil hit {@code 0.2 + (300 - 100) * 0.02 = 4.2} on its last tick.
     * Corrosive clouds wear armour on every living thing ({@code ArmorUtil.damageSuit} for i 0..3): sulfuric acid,
     * rating 50, takes {@code 50 / 50 = 1} durability off each piece a pig wears.
     */
    public static void mist(GameTestHelper helper) {
        LivingEntity pig = pig(helper, new BlockPos(2, 2, 2));
        helper.runAfterDelay(1, () -> {
            ServerLevel level = helper.getLevel();
            pig.setHealth(pig.getMaxHealth());
            float before = pig.getHealth();
            EntityMist steam = new EntityMist(level);
            steam.setType(Fluids.HOTSTEAM).setArea(4F, 3F).setDuration(150);
            steam.setPos(pig.getX(), pig.getY(), pig.getZ());
            steam.tickCount = 149;
            steam.tick();
            float took = before - pig.getHealth();
            helper.assertTrue(Math.abs(took - 4.2F) < 1.0E-4F, "the final tick must still boil for 4.2, took " + took);
            helper.assertTrue(steam.isRemoved(), "the cloud is gone after its final tick");

            pig.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            pig.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
            EntityMist acid = new EntityMist(level);
            acid.setType(Fluids.SULFURIC_ACID).setArea(4F, 3F).setDuration(150);
            acid.setPos(pig.getX(), pig.getY(), pig.getZ());
            acid.tickCount = 29;
            acid.tick();
            int helmet = pig.getItemBySlot(EquipmentSlot.HEAD).getDamageValue();
            int boots = pig.getItemBySlot(EquipmentSlot.FEET).getDamageValue();
            helper.assertTrue(helmet == 1 && boots == 1,
                    "an acid cloud must wear 1 durability off a pig's helmet and boots, has " + helmet + " / " + boots);
            acid.discard();
            pig.discard();
            helper.succeed();
        });
    }

    /**
     * 1.7 {@code onImpact}: a burning spray sets fire on the hit block's air neighbours; balefire fluid sets
     * {@code ModBlocks.balefire}, anything else vanilla fire. Fire placed where it cannot stand removes itself on
     * placement in both versions, so the top face (over the stone) is the one that stays.
     */
    public static void fire(GameTestHelper helper) {
        BlockPos stone = new BlockPos(2, 2, 2);
        BlockPos top = stone.above();
        BlockPos stone2 = new BlockPos(6, 2, 6);
        BlockPos top2 = stone2.above();
        helper.setBlock(stone, Blocks.STONE);
        helper.setBlock(stone2, Blocks.STONE);
        helper.assertTrue(EntityChemical.getStyleFromType(Fluids.BALEFIRE) == EntityChemical.ChemicalStyle.BURNING,
                "balefire fluid sprays as BURNING");
        impact(helper, Fluids.BALEFIRE, stone);
        boolean balefire = is(helper, top, ModBombBlocks.balefire.get());
        impact(helper, Fluids.GAS, stone2);
        boolean fire = is(helper, top2, Blocks.FIRE);
        // Put both out in the same tick: left alone they would spread into neighbouring plots.
        for (BlockPos p : BlockPos.betweenClosed(stone.offset(-1, -1, -1), stone.offset(1, 1, 1))) helper.setBlock(p, Blocks.AIR);
        for (BlockPos p : BlockPos.betweenClosed(stone2.offset(-1, -1, -1), stone2.offset(1, 1, 1))) helper.setBlock(p, Blocks.AIR);
        helper.assertTrue(balefire, "a balefire spray must light balefire, not vanilla fire");
        helper.assertTrue(fire, "a natural gas spray must light vanilla fire");
        helper.succeed();
    }

    /**
     * 1.7 {@code getExtinguishingType} and its impact branch: CO2 maps to CO2; water, heavy water and coolant to
     * WATER. A CO2 spray hitting any block of a burning exploded refinery puts it out through {@code tryExtinguish}
     * (resolved to the core, as {@code CompatExternal.getCoreFromPos}). A water spray (a LIQUID) also washes fallout
     * off x/z -2..2 and y 0..1 around the hit block: (6,2,6) and (4,3,4) go, (7,2,4) and (4,4,2) stay.
     */
    public static void extinguish(GameTestHelper helper) {
        helper.assertTrue(EntityChemical.getExtinguishingType(Fluids.CARBONDIOXIDE) == IRepairable.EnumExtinguishType.CO2
                        && EntityChemical.getExtinguishingType(Fluids.WATER) == IRepairable.EnumExtinguishType.WATER
                        && EntityChemical.getExtinguishingType(Fluids.HEAVYWATER) == IRepairable.EnumExtinguishType.WATER
                        && EntityChemical.getExtinguishingType(Fluids.COOLANT) == IRepairable.EnumExtinguishType.WATER
                        && EntityChemical.getExtinguishingType(Fluids.OIL) == null,
                "1.7 extinguishing types: CO2, and WATER for water / heavy water / coolant only");

        ServerLevel level = helper.getLevel();
        BlockPos rel = new BlockPos(13, 1, 13);
        var block = ModMachineBlocks.machine_refinery.get();
        helper.setBlock(rel, block.defaultBlockState());
        block.fillSpace(level, helper.absolutePos(rel), Direction.NORTH);
        TileEntityMachineRefinery refinery = GameTestHelpers.blockEntity(helper, rel, TileEntityMachineRefinery.class);
        refinery.hasExploded = true;
        refinery.onFire = true;
        impact(helper, Fluids.CARBONDIOXIDE, rel.above(4));
        helper.assertTrue(!refinery.onFire, "a CO2 spray on the refinery's tower must put the fire out");

        for (BlockPos p : BlockPos.betweenClosed(new BlockPos(1, 1, 1), new BlockPos(7, 1, 7))) helper.setBlock(p, Blocks.STONE);
        BlockPos hit = new BlockPos(4, 2, 4);
        helper.setBlock(hit, Blocks.STONE);
        helper.setBlock(new BlockPos(4, 3, 2), Blocks.STONE);
        BlockState fallout = ModFalloutBlocks.fallout.get().defaultBlockState();
        BlockPos[] cleared = {new BlockPos(6, 2, 6), new BlockPos(4, 3, 4)};
        BlockPos[] kept = {new BlockPos(7, 2, 4), new BlockPos(4, 4, 2)};
        for (BlockPos p : cleared) helper.setBlock(p, fallout);
        for (BlockPos p : kept) helper.setBlock(p, fallout);
        for (BlockPos p : cleared) helper.assertTrue(is(helper, p, ModFalloutBlocks.fallout.get()), "fallout placed at " + p);
        for (BlockPos p : kept) helper.assertTrue(is(helper, p, ModFalloutBlocks.fallout.get()), "fallout placed at " + p);
        impact(helper, Fluids.WATER, hit);
        for (BlockPos p : cleared) helper.assertTrue(!is(helper, p, ModFalloutBlocks.fallout.get()), "water must wash fallout off " + p);
        for (BlockPos p : kept) helper.assertTrue(is(helper, p, ModFalloutBlocks.fallout.get()), "fallout at " + p + " is outside the area");
        helper.succeed();
    }

    /**
     * 1.7 seed slurry over the 3x3x3 cube around the hit (4,3,4): dirt, podzol and dead dirt under a light block
     * (combined light 15, opacity 0) turn to grass; dirt under stone (opacity 15) stays; waste earth turns to grass
     * even under stone; cobblestone, stone bricks, concrete bricks, a top concrete-brick slab and east-facing upside
     * concrete-brick stairs become their mossy variants with the same shape; a double slab and cobblestone outside
     * the cube (6,3,4) are left alone.
     */
    public static void seedSlurry(GameTestHelper helper) {
        for (BlockPos p : BlockPos.betweenClosed(new BlockPos(2, 1, 2), new BlockPos(6, 1, 6))) helper.setBlock(p, Blocks.STONE);
        BlockState light = Blocks.LIGHT.defaultBlockState();
        helper.setBlock(new BlockPos(3, 2, 3), Blocks.DIRT);
        helper.setBlock(new BlockPos(3, 3, 3), light);
        helper.setBlock(new BlockPos(4, 2, 3), Blocks.PODZOL);
        helper.setBlock(new BlockPos(4, 3, 3), light);
        helper.setBlock(new BlockPos(3, 2, 5), ModFallingBlocks.dirt_dead.get());
        helper.setBlock(new BlockPos(3, 3, 5), light);
        helper.setBlock(new BlockPos(5, 2, 5), Blocks.DIRT);
        helper.setBlock(new BlockPos(5, 3, 5), Blocks.STONE);
        helper.setBlock(new BlockPos(5, 2, 3), ModFalloutBlocks.waste_earth.get());
        helper.setBlock(new BlockPos(5, 3, 3), Blocks.STONE);
        helper.setBlock(new BlockPos(4, 3, 4), Blocks.STONE_BRICKS);
        helper.setBlock(new BlockPos(4, 2, 4), Blocks.COBBLESTONE);
        helper.setBlock(new BlockPos(6, 3, 4), Blocks.COBBLESTONE);
        helper.setBlock(new BlockPos(3, 4, 3), ModHazardBlocks.brick_concrete.get());
        helper.setBlock(new BlockPos(5, 4, 5), ModBlocks.brick_concrete_slab.get().defaultBlockState()
                .setValue(SlabBlock.TYPE, SlabType.TOP));
        helper.setBlock(new BlockPos(5, 4, 3), ModBlocks.brick_concrete_slab.get().defaultBlockState()
                .setValue(SlabBlock.TYPE, SlabType.DOUBLE));
        helper.setBlock(new BlockPos(3, 4, 5), ModBlocks.brick_concrete_stairs.get().defaultBlockState()
                .setValue(StairBlock.FACING, Direction.EAST).setValue(StairBlock.HALF, Half.TOP));

        // Light propagates on the light engine's schedule, so give it a few ticks before reading it.
        helper.runAfterDelay(5, () -> {
            ServerLevel level = helper.getLevel();
            BlockPos lit = helper.absolutePos(new BlockPos(3, 3, 3));
            BlockPos dark = helper.absolutePos(new BlockPos(5, 3, 5));
            helper.assertTrue(LightLevels.combined(level, lit) >= 9 && LightLevels.opacity(level, lit) <= 2,
                    "the light block must read light >= 9 and opacity <= 2, has " + LightLevels.combined(level, lit)
                            + " / " + LightLevels.opacity(level, lit));
            helper.assertTrue(LightLevels.opacity(level, dark) > 2, "stone must read opacity > 2, has "
                    + LightLevels.opacity(level, dark));

            impact(helper, Fluids.SEEDSLURRY, new BlockPos(4, 3, 4));

            helper.assertTrue(is(helper, new BlockPos(3, 2, 3), Blocks.GRASS_BLOCK), "lit dirt becomes grass");
            helper.assertTrue(is(helper, new BlockPos(4, 2, 3), Blocks.GRASS_BLOCK), "lit podzol (1.7 dirt) becomes grass");
            helper.assertTrue(is(helper, new BlockPos(3, 2, 5), Blocks.GRASS_BLOCK), "lit dead dirt becomes grass");
            helper.assertTrue(is(helper, new BlockPos(5, 2, 5), Blocks.DIRT), "dirt under stone stays dirt");
            helper.assertTrue(is(helper, new BlockPos(5, 2, 3), Blocks.GRASS_BLOCK), "waste earth becomes grass unconditionally");
            helper.assertTrue(is(helper, new BlockPos(4, 3, 4), Blocks.MOSSY_STONE_BRICKS), "stone bricks become mossy");
            helper.assertTrue(is(helper, new BlockPos(4, 2, 4), Blocks.MOSSY_COBBLESTONE), "cobblestone becomes mossy");
            helper.assertTrue(is(helper, new BlockPos(6, 3, 4), Blocks.COBBLESTONE), "cobblestone outside the cube stays");
            helper.assertTrue(is(helper, new BlockPos(3, 4, 3), ModHazardBlocks.brick_concrete_mossy.get()),
                    "concrete bricks become mossy");
            BlockState slab = level.getBlockState(helper.absolutePos(new BlockPos(5, 4, 5)));
            helper.assertTrue(slab.is(ModBlocks.brick_concrete_mossy_slab.get()) && slab.getValue(SlabBlock.TYPE) == SlabType.TOP,
                    "a top concrete-brick slab becomes a top mossy slab, is " + slab);
            helper.assertTrue(is(helper, new BlockPos(5, 4, 3), ModBlocks.brick_concrete_slab.get()),
                    "a double slab (1.7's separate double-slab block) is left alone");
            BlockState stairs = level.getBlockState(helper.absolutePos(new BlockPos(3, 4, 5)));
            helper.assertTrue(stairs.is(ModBlocks.brick_concrete_mossy_stairs.get())
                            && stairs.getValue(StairBlock.FACING) == Direction.EAST && stairs.getValue(StairBlock.HALF) == Half.TOP,
                    "concrete-brick stairs become mossy stairs with the same facing and half, is " + stairs);
            helper.succeed();
        });
    }

    /**
     * 1.7 {@code TileEntityTurretFritz}: the ammo list shows {@code FT_Combustible && FT_Liquid} fluids, firing takes
     * {@code FT_Flammable && FT_Liquid}. Diesel (flammable, combustible, liquid) is both; oil (flammable,
     * calculated fuel with combustion multiplier 0, so not combustible) fires but is not listed; smear gains
     * {@code FT_Combustible} from {@code registerCalculatedFuel(..., 1.25, LOW)} and is both, as is hydrogen
     * (liquid + evaporating); natural gas (gaseous)
     * and hot oil (not flammable) are neither.
     */
    public static void fritzFuel(GameTestHelper helper) {
        Fluids.init();
        check(helper, Fluids.DIESEL, true, true);
        check(helper, Fluids.OIL, false, true);
        check(helper, Fluids.SMEAR, true, true);
        check(helper, Fluids.HYDROGEN, true, true);
        check(helper, Fluids.GAS, false, false);
        check(helper, Fluids.HOTOIL, false, false);
        double range = new TileEntityTurretFritz(BlockPos.ZERO,
                com.hbm.blocks.turret.ModTurretBlocks.turret_fritz.get().defaultBlockState()).getDetectorRange();
        helper.assertTrue(range == 48D, "the Fritz detects at 1.7's 48 blocks, has " + range);
        helper.succeed();
    }

    /**
     * 1.7 {@code EntityMist}: its scan excludes only itself, so clouds act on clouds. A 600 °C ultra-hot steam cloud
     * sets an overlapping natural gas cloud on fire ({@code setFire(10)} = 200 ticks); on its next tick the burning
     * flammable cloud explodes ({@code createExplosion(intensity * 15)}) and is removed. A cool non-flammable water
     * cloud ({@code temperature < 50}) puts a burning cloud out instead ({@code extinguish()}), so it does not explode.
     * The gas clouds are on tick 148 of 150, so the blast is {@code (1 - 149 / 150) * 15 = 0.1}: harmless.
     */
    public static void mistOnMist(GameTestHelper helper) {
        helper.runAfterDelay(1, () -> {
            ServerLevel level = helper.getLevel();
            Vec3 at = helper.absoluteVec(new Vec3(3.5D, 2D, 3.5D));
            EntityMist gas = new EntityMist(level);
            gas.setType(Fluids.GAS).setArea(4F, 3F).setDuration(150);
            gas.setPos(at.x, at.y, at.z);
            gas.tickCount = 148;
            level.addFreshEntity(gas);
            EntityMist hot = new EntityMist(level);
            hot.setType(Fluids.ULTRAHOTSTEAM).setArea(4F, 3F).setDuration(150);
            hot.setPos(at.x, at.y, at.z);
            hot.tickCount = 29;
            hot.tick();
            int fire = gas.getRemainingFireTicks();
            boolean burning = gas.isOnFire();
            gas.tick();
            boolean exploded = gas.isRemoved();
            gas.discard();
            helper.assertTrue(burning && fire == 200, "a 600 °C cloud sets the gas cloud on fire for 200 ticks, has "
                    + fire + " (burning " + burning + ")");
            helper.assertTrue(exploded, "a burning flammable cloud explodes and is removed on its next tick");

            Vec3 at2 = helper.absoluteVec(new Vec3(3.5D, 6D, 3.5D));
            EntityMist gas2 = new EntityMist(level);
            gas2.setType(Fluids.GAS).setArea(4F, 3F).setDuration(150);
            gas2.setPos(at2.x, at2.y, at2.z);
            gas2.tickCount = 148;
            level.addFreshEntity(gas2);
            dev.golem.core.world.Effects.ignite(gas2, 10);
            EntityMist water = new EntityMist(level);
            water.setType(Fluids.WATER).setArea(4F, 3F).setDuration(150);
            water.setPos(at2.x, at2.y, at2.z);
            water.tickCount = 29;
            water.tick();
            boolean out = !gas2.isOnFire();
            gas2.tick();
            boolean survived = !gas2.isRemoved();
            gas2.discard();
            helper.assertTrue(out && survived, "a water cloud puts a burning gas cloud out (out " + out
                    + ", not exploded " + survived + ")");
            helper.succeed();
        });
    }

    /**
     * 1.7 {@code damageSuit} goes through {@code ItemStack.damageItem}, so Unbreaking applies per point: armour takes
     * the point when {@code nextFloat() < 0.6}, otherwise only when {@code nextInt(level + 1) == 0}. For Unbreaking
     * III that is {@code 0.6 + 0.4 / 4 = 0.7} of the points (σ over 4,000 trials = 0.0072; accepted 0.67..0.73,
     * over 4σ either way). Unenchanted boots take every point, 4,000 of 4,000.
     */
    public static void unbreaking(GameTestHelper helper) {
        LivingEntity pig = pig(helper, new BlockPos(1, 3, 1));
        helper.runAfterDelay(1, () -> {
            ItemStack helmet = new ItemStack(Items.DIAMOND_HELMET);
            dev.golem.core.world.Durability.enchantUnbreaking(helper.getLevel(), helmet, 3);
            ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
            pig.setItemSlot(EquipmentSlot.HEAD, helmet);
            pig.setItemSlot(EquipmentSlot.FEET, boots);
            int trials = 4000;
            int helmetPoints = 0;
            int bootPoints = 0;
            for (int i = 0; i < trials; i++) {
                pig.getItemBySlot(EquipmentSlot.HEAD).setDamageValue(0);
                pig.getItemBySlot(EquipmentSlot.FEET).setDamageValue(0);
                com.hbm.handler.ArmorUtil.damageWholeSuit(pig, 1);
                helmetPoints += pig.getItemBySlot(EquipmentSlot.HEAD).getDamageValue();
                bootPoints += pig.getItemBySlot(EquipmentSlot.FEET).getDamageValue();
            }
            double fraction = helmetPoints / (double) trials;
            helper.assertTrue(bootPoints == trials, "unenchanted boots take every point, " + bootPoints + " / " + trials);
            helper.assertTrue(fraction > 0.67D && fraction < 0.73D,
                    "Unbreaking III armour takes 0.7 of the points (1.7), took " + fraction);

            ItemStack worn = new ItemStack(Items.IRON_BOOTS);
            worn.setDamageValue(worn.getMaxDamage() - 1);
            pig.setItemSlot(EquipmentSlot.FEET, worn);
            pig.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            com.hbm.handler.ArmorUtil.damageWholeSuit(pig, 1);
            helper.assertTrue(pig.getItemBySlot(EquipmentSlot.FEET).isEmpty(), "a worn-through piece breaks");
            pig.discard();
            helper.succeed();
        });
    }

    private static void check(GameTestHelper helper, FluidType type, boolean listed, boolean fires) {
        helper.assertTrue(TileEntityTurretFritz.isListedAmmo(type) == listed,
                type.getName() + " listed must be " + listed);
        helper.assertTrue(TileEntityTurretFritz.canFire(type) == fires, type.getName() + " fires must be " + fires);
    }

    private static void expect(GameTestHelper helper, LivingEntity entity, Object effect, int duration, int amplifier,
            String what) {
        MobEffectInstance instance = Effects.get(entity, effect);
        helper.assertTrue(instance != null && instance.getDuration() == duration && instance.getAmplifier() == amplifier,
                what + " must be " + duration + " ticks at amplifier " + amplifier + ", has " + instance);
    }
}
