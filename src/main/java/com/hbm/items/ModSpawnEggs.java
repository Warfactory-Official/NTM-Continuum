package com.hbm.items;

import com.hbm.Constants;
import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.mob.EntityParasiteMaggot;
import com.hbm.entity.mob.glyphid.EntityGlyphidBlaster;
import com.hbm.entity.mob.glyphid.EntityGlyphidBombardier;
import com.hbm.entity.mob.glyphid.EntityGlyphidBrawler;
import com.hbm.entity.mob.glyphid.EntityGlyphidDigger;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import dev.golem.core.dejson.Tints;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.registry.Tabs;
import dev.golem.core.world.SpawnEggs;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Spawn eggs for the 29 mobs 1.7 {@code EntityMappings.addMob} registered, with 1.7's egg colours (History §19ej-7).
 *
 * <p>1.7 gave each mob a vanilla {@code spawn_egg} damage value ({@code ModEntityList.registerEntityEgg}), shown in
 * the vanilla tab beside the vanilla eggs and named "Spawn " + the mob's name. Modern eggs are one item per mob:
 * {@code hbm:<entity id>_spawn_egg}, in the vanilla spawn-egg tab in 1.7's {@code addMob} order, drawn as the
 * pre-1.21.5 two-layer egg tinted with the two 1.7 colours.
 */
public final class ModSpawnEggs {

    private ModSpawnEggs() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(ITEMS);

    /** Every egg, in 1.7 {@code addMob} order. */
    public static final List<RegistryObject<Item>> EGGS = new ArrayList<>();
    /** The entity id path each egg spawns, parallel to {@link #EGGS}. */
    public static final List<String> ENTITIES = new ArrayList<>();
    /** 1.7 primary / secondary colours, parallel to {@link #EGGS}. */
    public static final List<int[]> COLORS = new ArrayList<>();

    static {
        egg("entity_mob_nuclear_creeper", () -> ModNtmEntities.CREEPER_NUCLEAR.get(), 0x204131, 0x75CE00);
        egg("entity_mob_tainted_creeper", () -> ModNtmEntities.CREEPER_TAINTED.get(), 0x813b9b, 0xd71fdd);
        egg("entity_mob_phosgene_creeper", () -> ModNtmEntities.CREEPER_PHOSGENE.get(), 0xE3D398, 0xB8A06B);
        egg("entity_mob_volatile_creeper", () -> ModNtmEntities.CREEPER_VOLATILE.get(), 0xC28153, 0x4D382C);
        egg("entity_mob_gold_creeper", () -> ModNtmEntities.CREEPER_GOLD.get(), 0xECC136, 0x9E8B3E);
        egg("entity_mob_hunter_chopper", () -> ModNtmEntities.HUNTER_CHOPPER.get(), 0x000020, 0x2D2D72);
        egg("entity_cyber_crab", () -> ModNtmEntities.CYBER_CRAB.get(), 0xAAAAAA, 0x444444);
        egg("entity_tesla_crab", () -> ModNtmEntities.TESLA_CRAB.get(), 0xAAAAAA, 0x440000);
        egg("entity_taint_crab", () -> ModNtmEntities.TAINT_CRAB.get(), 0xAAAAAA, 0xFF00FF);
        egg("entity_mob_mask_man", () -> ModNtmEntities.MASK_MAN.get(), 0x818572, 0xC7C1B7);
        egg("entity_fucc_a_ducc", () -> ModNtmEntities.DUCK.get(), 0xd0d0d0, 0xFFBF00);
        egg("entity_elder_one", () -> ModNtmEntities.QUACKOS.get(), 0xd0d0d0, 0xFFBF00);
        egg("entity_pigeon", () -> ModNtmEntities.PIGEON.get(), 0xC8C9CD, 0x858894);
        egg("entity_fbi", () -> ModNtmEntities.FBI.get(), 0x008000, 0x404040);
        egg("entity_fbi_drone", () -> ModNtmEntities.FBI_DRONE.get(), 0x008000, 0x404040);
        egg("entity_radiation_blaze", () -> ModNtmEntities.RAD_BEAST.get(), 0x303030, 0x008000);
        egg("entity_glyphid", () -> ModNtmEntities.GLYPHID.get(), 0x724A21, 0xD2BB72);
        egg("entity_glyphid_brawler", () -> EntityGlyphidBrawler.TYPE.get(), 0x273038, 0xD2BB72);
        egg("entity_glyphid_behemoth", () -> ModNtmEntities.GLYPHID_BEHEMOTH.get(), 0x267F00, 0xD2BB72);
        egg("entity_glyphid_brenda", () -> ModNtmEntities.GLYPHID_BRENDA.get(), 0x4FC0C0, 0xA0A0A0);
        egg("entity_glyphid_bombardier", () -> EntityGlyphidBombardier.TYPE.get(), 0xDDD919, 0xDBB79D);
        egg("entity_glyphid_blaster", () -> EntityGlyphidBlaster.TYPE.get(), 0xD83737, 0xDBB79D);
        egg("entity_glyphid_scout", () -> ModNtmEntities.GLYPHID_SCOUT.get(), 0x273038, 0xB9E36B);
        egg("entity_glyphid_nuclear", () -> ModNtmEntities.GLYPHID_NUCLEAR.get(), 0x267F00, 0xA0A0A0);
        egg("entity_glyphid_digger", () -> EntityGlyphidDigger.TYPE.get(), 0x273038, 0x724A21);
        egg("entity_plastic_bag", () -> ModNtmEntities.PLASTIC_BAG.get(), 0xd0d0d0, 0x808080);
        egg("entity_parasite_maggot", () -> EntityParasiteMaggot.TYPE.get(), 0xd0d0d0, 0x808080);
        egg("entity_test_dummy", () -> ModNtmEntities.TEST_DUMMY.get(), 0xffffff, 0x000000);
        egg("entity_undead_soldier", () -> ModNtmEntities.UNDEAD_SOLDIER.get(), 0x749F30, 0x6C5B44);
    }

    private static void egg(String entity, Supplier<EntityType<? extends Mob>> type, int primary, int secondary) {
        RegistryObject<Item> egg = REG.<Item>item(entity + "_spawn_egg", p -> SpawnEggs.create(type, primary, secondary, p))
                .transform(Presets.sprite("hbm:items/spawn_egg", "hbm:items/spawn_egg_overlay")
                        .tints(Tints.constant(0xFF000000 | primary), Tints.constant(0xFF000000 | secondary)));
        EGGS.add(egg);
        ENTITIES.add(entity);
        COLORS.add(new int[] {primary, secondary});
    }

    /** The eggs, read-only, in 1.7 order. */
    public static List<RegistryObject<Item>> eggs() {
        return Collections.unmodifiableList(EGGS);
    }

    public static void init() {
        // The vanilla spawn-egg tab by id: its CreativeModeTabs constant is private on Fabric 26.1, and a Stonecutter
        // substring rule rewrites the constant's name.
        Tabs.modifyEntries(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB,
                dev.golem.core.Ids.of("minecraft", "spawn_eggs")), entries -> {
            for (RegistryObject<Item> egg : EGGS) entries.accept(new ItemStack(egg.get()));
        });
    }
}
