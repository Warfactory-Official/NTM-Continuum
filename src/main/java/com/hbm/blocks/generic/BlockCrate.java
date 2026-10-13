package com.hbm.blocks.generic;

import com.hbm.blocks.machine.storage.BlockCrateSupply;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.special.ItemCell;
import com.hbm.items.weapon.grenade.ItemGrenadeExtra;
import com.hbm.items.weapon.grenade.ItemGrenadeFilling;
import com.hbm.items.weapon.grenade.ItemGrenadeFuze;
import com.hbm.items.weapon.grenade.ItemGrenadeShell;
import com.hbm.items.weapon.grenade.ItemGrenadeUniversal;
import com.hbm.sound.ModSounds;
import dev.golem.core.Ids;
import dev.golem.core.world.CompatBlock;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

/**
 * 1.7 {@code BlockCrate}: the supply, weapon, hazmat (lead), machine (metal) and red loot crates. A gravity
 * block (the old class extended {@code BlockFalling}); a crowbar spills its loot and breaks it with the
 * crate-break sound; mining it just drops the crate.
 *
 * <p>The old drop tables were hardcoded lists where an entry appeared {@code weight} times and one
 * was picked uniformly. They are kept as lists of item ids so that an id this port does not (yet) have
 * keeps its weight as an empty draw rather than shifting the odds of everything else ("this is
 * implemented in the most braindead way imaginable", the original's own words).
 */
public class BlockCrate extends CompatBlock {

    public enum Kind { SUPPLY, WEAPON, LEAD, METAL, RED }

    private final Kind kind;

    public BlockCrate(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    // BlockFalling, spelt out: vanilla's FallingBlock has no interaction hook this port can reach

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        level.scheduleTick(pos, this, 2);
    }

    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighbourBlock, boolean movedByPiston) {
        level.scheduleTick(pos, this, 2);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (FallingBlock.isFree(level.getBlockState(pos.below())) && pos.getY() > level.getMinY()) {
            FallingBlockEntity.fall(level, pos, state);
        }
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, ItemStack held, BlockHitResult hit) {
        if (!held.is(BlockCrateSupply.CROWBAR)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide()) {
            dropItems(level, pos);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            level.playSound(null, pos, ModSounds.CRATE_BREAK.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    private static final List<Supplier<ItemStack>> SUPPLY = new ArrayList<>();
    private static final List<Supplier<ItemStack>> WEAPON = new ArrayList<>();
    private static final List<Supplier<ItemStack>> LEAD = new ArrayList<>();
    private static final List<Supplier<ItemStack>> METAL = new ArrayList<>();
    private static final List<Supplier<ItemStack>> RED = new ArrayList<>();
    private static boolean dropsSet;

    private static void setDrops() {
        if (dropsSet) return;
        dropsSet = true;
        // Supply Crate
        add(SUPPLY, "syringe_metal_stimpak", 10);
        add(SUPPLY, "syringe_antidote", 5);
        add(SUPPLY, () -> ItemGrenadeUniversal.make(ItemGrenadeShell.EnumGrenadeShell.FRAG,
                ItemGrenadeFilling.EnumGrenadeFilling.HE, ItemGrenadeFuze.EnumGrenadeFuze.S3,
                ItemGrenadeExtra.EnumGrenadeExtra.FRAG_SLEEVE), 8);
        add(SUPPLY, () -> ItemGrenadeUniversal.make(ItemGrenadeShell.EnumGrenadeShell.STICK,
                ItemGrenadeFilling.EnumGrenadeFilling.HE, ItemGrenadeFuze.EnumGrenadeFuze.IMPACT), 6);
        add(SUPPLY, () -> ItemGrenadeUniversal.make(ItemGrenadeShell.EnumGrenadeShell.FRAG,
                ItemGrenadeFilling.EnumGrenadeFilling.INC, ItemGrenadeFuze.EnumGrenadeFuze.S7), 4);
        add(SUPPLY, "ammo_container", 2);
        // Weapon Crate
        add(WEAPON, "gun_light_revolver", 10);
        add(WEAPON, "gun_maresleg", 7);
        add(WEAPON, "gun_heavy_revolver", 5);
        add(WEAPON, "gun_greasegun", 5);
        add(WEAPON, "gun_liberator", 2);
        add(WEAPON, "gun_flaregun", 8);
        add(WEAPON, "gun_panzerschreck", 1);
        // Lead Crate
        add(LEAD, "ingot_uranium", 10);
        add(LEAD, "ingot_u238", 8);
        add(LEAD, "ingot_plutonium", 7);
        add(LEAD, "ingot_pu240", 6);
        add(LEAD, "ingot_neptunium", 7);
        add(LEAD, "ingot_uranium_fuel", 8);
        add(LEAD, "ingot_plutonium_fuel", 7);
        add(LEAD, "ingot_mox_fuel", 6);
        add(LEAD, "nugget_uranium", 10);
        add(LEAD, "nugget_u238", 8);
        add(LEAD, "nugget_plutonium", 7);
        add(LEAD, "nugget_pu240", 6);
        add(LEAD, "nugget_neptunium", 7);
        add(LEAD, "nugget_uranium_fuel", 8);
        add(LEAD, "nugget_plutonium_fuel", 7);
        add(LEAD, "nugget_mox_fuel", 6);
        add(LEAD, () -> ItemCell.getFullCell(Fluids.DEUTERIUM), 8);
        add(LEAD, () -> ItemCell.getFullCell(Fluids.TRITIUM), 8);
        add(LEAD, () -> ItemCell.getFullCell(Fluids.UF6), 8);
        add(LEAD, () -> ItemCell.getFullCell(Fluids.PUF6), 8);
        add(LEAD, "pellet_rtg", 6);
        add(LEAD, "pellet_rtg_weak", 7);
        add(LEAD, "powder_yellowcake", 10);
        // Metal Crate
        add(METAL, "machine_press", 10);
        add(METAL, "machine_reactor", 6); // 1.7 field machine_reactor_breeding, registry id machine_reactor
        add(METAL, "machine_wood_burner", 10);
        add(METAL, "machine_diesel", 8);
        add(METAL, "machine_rtg_grey", 4); // 1.7 field machine_rtg, registry id machine_rtg_grey
        add(METAL, "red_pylon", 9);
        add(METAL, "battery_lead", 10); // 1.7: battery_pack sub-item BATTERY_LEAD (the port registers it as battery_lead)
        add(METAL, "machine_electric_furnace_off", 8);
        add(METAL, "machine_assembly_machine", 10);
        add(METAL, "machine_fluidtank", 7);
        add(METAL, "centrifuge_element", 6);
        add(METAL, "motor", 8);
        add(METAL, "coil_tungsten", 7);
        add(METAL, "photo_panel", 3);
        add(METAL, "coil_copper", 10);
        add(METAL, "blade_titanium", 3);
        add(METAL, "piston_selenium", 6);
        // Red Crate
        add(RED, "mysteryshovel", 1);
        add(RED, "gun_heavy_revolver_lilmac", 1);
        add(RED, "gun_autoshotgun_sexy", 1);
        add(RED, "gun_maresleg_broken", 1);
        add(RED, "ammo_secret_m44_equestrian", 1);
        add(RED, "ammo_secret_g12_equestrian", 1);
        add(RED, "ammo_secret_bmg50_equestrian", 1);
        add(RED, "battery_spark", 1);
        add(RED, "bottle_sparkle", 1);
        add(RED, "bottle_rad", 1);
        add(RED, "ring_starmetal", 1);
        add(RED, "flame_pony", 1);
        add(RED, "ntm_dirt", 1);
        add(RED, "broadcaster_pc", 1);
    }

    private static void add(List<Supplier<ItemStack>> list, String id, int weight) {
        add(list, () -> stackOf(id), weight);
    }

    private static void add(List<Supplier<ItemStack>> list, Supplier<ItemStack> stack, int weight) {
        for (int i = 0; i < weight; i++) list.add(stack);
    }

    /** The item with this hbm id, or an empty stack (an unported entry keeps its weight). */
    private static ItemStack stackOf(String id) {
        Item item = BuiltInRegistries.ITEM.getValue(Ids.of("hbm", id));
        return item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    private List<Supplier<ItemStack>> table() {
        return switch (kind) {
            case SUPPLY -> SUPPLY;
            case WEAPON -> WEAPON;
            case LEAD -> LEAD;
            case METAL -> METAL;
            case RED -> RED;
        };
    }

    public void dropItems(Level level, BlockPos pos) {
        Random rand = new Random();
        setDrops();
        List<Supplier<ItemStack>> table = table();
        List<ItemStack> list = new ArrayList<>();
        int i = rand.nextInt(3) + 3;
        if (kind == Kind.WEAPON) {
            i = 1 + rand.nextInt(2);
            if (rand.nextInt(100) == 34) i = 25;
        }
        for (int j = 0; j < i; j++) {
            list.add(table.get(rand.nextInt(table.size())).get());
        }
        if (kind == Kind.RED) {
            list.clear();
            for (Supplier<ItemStack> entry : table) list.add(entry.get());
        }
        for (ItemStack stack : list) {
            if (stack.isEmpty()) continue;
            float f = rand.nextFloat() * 0.8F + 0.1F;
            float f1 = rand.nextFloat() * 0.8F + 0.1F;
            float f2 = rand.nextFloat() * 0.8F + 0.1F;
            ItemEntity entityitem = new ItemEntity(level, pos.getX() + f, pos.getY() + f1, pos.getZ() + f2, stack.copy());
            float f3 = 0.05F;
            entityitem.setDeltaMovement((float) rand.nextGaussian() * f3, (float) rand.nextGaussian() * f3 + 0.2F,
                    (float) rand.nextGaussian() * f3);
            level.addFreshEntity(entityitem);
        }
    }
}
