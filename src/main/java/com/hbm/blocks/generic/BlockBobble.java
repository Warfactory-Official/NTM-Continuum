package com.hbm.blocks.generic;

import com.hbm.tileentity.deco.TileEntityBobble;
import com.hbm.util.ShadyUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * 1.7 {@code BlockBobble}: the commemorative bobbleheads. Hardness 0, iron material, a 5x10x5-pixel box; a
 * right-click (with anything in hand) opens {@code GUIScreenBobble} on the client, which has no container.
 * The creative list skips {@link BobbleType#NONE}.
 */
public class BlockBobble extends BlockTrinket {

    /** Client hook: opens {@code GUIScreenBobble}. Installed by the client registrations. */
    public static Consumer<TileEntityBobble> OPEN_SCREEN = bobble -> {};

    private static final VoxelShape SHAPE = Block.box(5.5D, 0.0D, 5.5D, 10.5D, 10.0D, 10.5D);

    public BlockBobble(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityBobble(pos, state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                 BlockHitResult hit) {
        if (level.isClientSide() && level.getBlockEntity(pos) instanceof TileEntityBobble bobble) {
            OPEN_SCREEN.accept(bobble);
        }
        return InteractionResult.SUCCESS;
    }

    public enum BobbleType {

        NONE("null", "null", null, null, false, "BOARD_BLANK"),
        STRENGTH("Strength", "Strength", null, "It's essential to give your arguments impact.", false, "BRIDGE_BIOS"),
        PERCEPTION("Perception", "Perception", null, "Only through observation will you perceive weakness.", false, "BRIDGE_NORTH"),
        ENDURANCE("Endurance", "Endurance", null, "Always be ready to take one for the team.", false, "BRIDGE_SOUTH"),
        CHARISMA("Charisma", "Charisma", null, "Nothing says pizzaz like a winning smile.", false, "BRIDGE_IO"),
        INTELLIGENCE("Intelligence", "Intelligence", null, "It takes the smartest individuals to realize$there's always more to learn.", false, "BRIDGE_BUS"),
        AGILITY("Agility", "Agility", null, "Never be afraid to dodge the sensitive issues.", false, "BRIDGE_CHIPSET"),
        LUCK("Luck", "Luck", null, "There's only one way to give 110%.", false, "BRIDGE_CMOS"),
        // 1.7 appended System.getProperty("user.name") to the inscription at class-load time
        BOB("Robert \"The Bobcat\" Katzinsky", "HbMinecraft", "Hbm's Nuclear Tech Mod", "I know where you live, " + System.getProperty("user.name"), false, "CPU_SOCKET"),
        FRIZZLE("Frooz", "Frooz", "Weapon models", "BLOOD IS FUEL", true, "CPU_CLOCK"),
        PU238("Pu-238", "Pu-238", "Improved Tom impact mechanics", null, false, "CPU_REGISTER"),
        VT("VT-6/24", "VT-6/24", "Balefire warhead model and general texturework", "You cannot unfuck a horse.", true, "CPU_EXT"),
        DOC("The Doctor", "Doctor17PH", "Russian localization, lunar miner", "Perhaps the moon rocks were too expensive", true, "CPU_CACHE"),
        BLUEHAT("The Blue Hat", "The Blue Hat", "Textures", "payday 2's deagle freeaim champ of the year 2022", true, "MEM_16K_A"),
        PHEO("Pheo", "Pheonix", "Deuterium machines, tantalium textures, Reliant Rocket", "RUN TO THE BEDROOM, ON THE SUITCASE ON THE LEFT,$YOU'LL FIND MY FAVORITE AXE", true, "MEM_16K_B"),
        ADAM29("Adam29", "Adam29", "Ethanol, liquid petroleum gas", "You know, nukes are really quite beatiful.$It's like watching a star be born for a split second.", true, "MEM_16K_C"),
        UFFR("UFFR", "UFFR", "All sorts of things from his PR", "fried shrimp", false, "MEM_SOCKET"),
        VAER("vaer", "vaer", "ZIRNOX", "taken de family out to the weekend cigarette festival", true, "MEM_16K_D"),
        NOS("Dr Nostalgia", "Dr Nostalgia", "SSG and Vortex models", "Take a picture, I'ma pose, paparazzi$I've been drinking, moving like a zombie", true, "BOARD_TRANSISTOR"),
        DRILLGON("Drillgon200", "Drillgon200", "1.12 Port", null, false, "CPU_LOGIC"),
        CIRNO("Cirno", "Cirno", "the only multi layered skin i had", "No brain. Head empty.", true, "BOARD_BLANK"),
        MICROWAVE("Microwave", "Microwave", "OC Compatibility and massive RBMK/packet optimizations", "they call me the food heater$john optimization", true, "BOARD_CONVERTER"),
        PEEP("Peep", "LePeeperSauvage", "Coilgun, Leadburster and Congo Lake models, BDCL QC", "Fluffy ears can't hide in ash, nor snow.", true, "CARD_BOARD"),
        MELLOW("MELLOWARPEGGIATION", "Mellow", "NBT Structures, industrial lighting, animation tools", "Make something cool now, ask for permission later.", true, "CARD_PROCESSOR"),
        ABEL("Abel1502", "Abel1502", "Abilities GUI, optimizations and many QoL improvements", "NANTO SUBARASHII", true, "CPU_REGISTER"),
        // 1.12.2 additions, in its order; three wear their person's real skin
        MOVBLOCK("movblock", "movblock", "Threaded MK5 and some part of 1.12.2 Community Edition", "In three dimensions!", true, "CPU_LOGIC",
                ShadyUtil.movblock),
        LEAFIA("りー (Ri/Leafia)", "Leafia", "Custom Control Panel shenanigans$and porting (such as RoR panels) since 2026", "Control panel #1 sniffer$Also 1.12.2 sniffer$$This was the best I could model", false, "BOARD_BLANK"),
        HACKER6329("Hacker6329", "Hacker6329", "Partially fillable fluid barrels and some fixes.", "\"movblock i told you my v2 system wasn't the issue.\"", true, "CARD_BOARD",
                ShadyUtil.Hacker6329),
        EOS("EOS", "EOS", "Ported some stuff. Refactor code", "string[0] = EOS;", true, "MEM_16K_A", ShadyUtil.EOS),
        GOLEM("TheOriginalGolem", "TheOriginalGolem", "NTM: Continuum, Boxy,$helped with multiple Warfactory projects, And Previously Maintained The 1.12 Reloaded Port.", "mixin hell", true, "CPU_CACHE",
                ShadyUtil.Golem),
        NORWOOD("MrNorwood", "MrNorwood", "NTM:CE, involved in both NTM: NEXT and NTM: Continuum,$and the Warfactory project as a whole.", "\"NTM sucks, Notch is fat, and Warfactory won.\"", true, "CPU_CLOCK",
                ShadyUtil.MrNorwood),
        SILENTYETI("SilentYeti", "SilentYeti", "Warfactory Media Manager, did some porting on NTM:CE,$rebalanced Glider Jetpack, #1 playtester", "\"My Shrine will stand in Memory of my Glory\"", true, "CPU_REGISTER",
                ShadyUtil.SilentYeti);

        public static final BobbleType[] VALUES = values();

        public final String name;         // the title of the tooltip
        public final String label;        // the name engraved in the socket
        /** Not final: an addon may restate it (ADDON-API-INVENTORY E-096; NTM: Space's MELLOW/PEEP credits). */
        public String contribution; // what contributions this person has made, if applicable
        public final String inscription;  // the flavor text
        public final boolean skinLayers;
        /** 1.7 {@code ItemPlasticScrap.ScrapType} the shredder turns this figure into (the port has no scrap subtypes). */
        public String scrap; // not final, see contribution (E-096)
        /** 1.12.2 {@code skinUuid}: the person whose Mojang skin the figure wears, or null for a modelled figure. */
        public final String skinUuid;

        BobbleType(String name, String label, String contribution, String inscription, boolean layers, String scrap) {
            this(name, label, contribution, inscription, layers, scrap, null);
        }

        BobbleType(String name, String label, String contribution, String inscription, boolean layers, String scrap,
                String skinUuid) {
            this.name = name;
            this.label = label;
            this.contribution = contribution;
            this.inscription = inscription;
            this.skinLayers = layers;
            this.scrap = scrap;
            this.skinUuid = skinUuid;
        }

        public static BobbleType of(int ordinal) {
            return VALUES[Math.floorMod(ordinal, VALUES.length)];
        }
    }
}
