package fissionworks;

import fissionworks.sim.Reactor;
import fissionworks.sim.Zirnox;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

@Mod(FissionWorks.ID)
public class FissionWorks {
    public static final String ID = "fissionworks";

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ID);
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ID);

    private static DeferredItem<Item> rod(String name, Zirnox.Fuel fuel) {
        return ITEMS.registerItem(name, p -> new FuelRodItem(p, fuel), new Item.Properties().durability(FuelRodItem.LIFE));
    }

    private static Properties metal() {
        return Properties.of().strength(3.5f, 8f).sound(SoundType.METAL);
    }

    public static final DeferredBlock<PileBlock> CASING = BLOCKS.registerBlock("pile_casing", PileBlock::new, metal());
    public static final DeferredBlock<PanelBlock> PANEL = BLOCKS.registerBlock("pile_panel", PanelBlock::new, metal());
    public static final DeferredBlock<RodBlock> ROD = BLOCKS.registerBlock("pile_rod", RodBlock::new, metal());
    public static final DeferredBlock<FuelBlock> FUEL = BLOCKS.registerBlock("pile_fuel", FuelBlock::new,
            metal().lightLevel(s -> s.getValue(PileBlock.FORMED) ? s.getValue(FuelBlock.GLOW) * 3 : 0));
    public static final DeferredBlock<PileBlock> MODERATOR =
            BLOCKS.registerBlock("pile_moderator", PileBlock::new, metal());
    public static final DeferredBlock<PileBlock> REFLECTOR =
            BLOCKS.registerBlock("pile_reflector", PileBlock::new, metal());
    public static final DeferredBlock<PileBlock> ABSORBER =
            BLOCKS.registerBlock("pile_absorber", PileBlock::new, metal());

    public static final DeferredBlock<ZirnoxBlock> ZIRNOX = BLOCKS.registerBlock("zirnox", ZirnoxBlock::new, metal());

    public static final DeferredItem<BlockItem> FUEL_ITEM = ITEMS.registerSimpleBlockItem(FUEL);
    public static final DeferredItem<Item> ROD_NATURAL = rod("rod_natural_uranium", Zirnox.Fuel.NATURAL_URANIUM);
    public static final DeferredItem<Item> ROD_URANIUM = rod("rod_uranium", Zirnox.Fuel.URANIUM);
    public static final DeferredItem<Item> ROD_THORIUM = rod("rod_thorium", Zirnox.Fuel.THORIUM);
    public static final DeferredItem<Item> ROD_SPENT = ITEMS.registerSimpleItem("rod_spent", new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> CANISTER = ITEMS.registerSimpleItem("canister_co2", new Item.Properties().stacksTo(16));

    static {
        ITEMS.registerSimpleBlockItem(CASING);
        ITEMS.registerSimpleBlockItem(PANEL);
        ITEMS.registerSimpleBlockItem(ROD);
        ITEMS.registerSimpleBlockItem(MODERATOR);
        ITEMS.registerSimpleBlockItem(REFLECTOR);
        ITEMS.registerSimpleBlockItem(ABSORBER);
        ITEMS.registerSimpleBlockItem(ZIRNOX);
        TABS.register("main", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup." + ID))
                .icon(() -> FUEL_ITEM.get().getDefaultInstance())
                .displayItems((params, out) -> ITEMS.getEntries().forEach(e -> out.accept(e.get())))
                .build());
    }

    public static final Supplier<BlockEntityType<ReactorBE>> REACTOR_BE = BLOCK_ENTITIES.register("reactor",
            () -> BlockEntityType.Builder.of(ReactorBE::new, PANEL.get()).build(null));

    public static final Supplier<BlockEntityType<ZirnoxBE>> ZIRNOX_BE = BLOCK_ENTITIES.register("zirnox",
            () -> BlockEntityType.Builder.of(ZirnoxBE::new, ZIRNOX.get()).build(null));

    public FissionWorks(IEventBus bus) {
        ModFluids.TYPES.register(bus);
        ModFluids.FLUIDS.register(bus);
        BLOCKS.register(bus);
        ITEMS.register(bus);
        TABS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }

    /** What a block is inside the hall, or null if it may not go there. Casing is an empty column. */
    public static Reactor.Kind kindOf(Block block) {
        if (block == CASING.get()) return Reactor.Kind.EMPTY;
        if (block == FUEL.get()) return Reactor.Kind.FUEL;
        if (block == ROD.get()) return Reactor.Kind.ROD;
        if (block == MODERATOR.get()) return Reactor.Kind.MODERATOR;
        if (block == REFLECTOR.get()) return Reactor.Kind.REFLECTOR;
        return block == ABSORBER.get() ? Reactor.Kind.ABSORBER : null;
    }
}
