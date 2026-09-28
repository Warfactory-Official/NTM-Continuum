package com.hbm.world.gen.nbt;

/**
 * 1.7 {@code INBTBlockTransformable}: the metadata rotations an {@link NBTStructure} applies to a 1.7 block when it
 * builds with a non-zero {@code coordBaseMode} (1 = 90 degrees clockwise, 2 = 180, 3 = 90 counter-clockwise).
 *
 * <p>1.7 structure files store a block as registry name plus metadata, so the rotation runs in metadata space
 * exactly as 1.7 ran it, before {@link LegacyStructurePalette} turns the rotated pair into a flattened state. Which
 * rule a 1.7 block used followed its 1.7 class; {@link LegacyStructurePalette} records that per name. The helpers
 * are copied verbatim, including their odd corners: {@link #transformMetaDeco} has no {@code break} after each
 * inner switch, so an unmatched meta falls through into the next rotation's table (it never matters, because the
 * tables only list 2..5), and standing signs use the wall-sign table although they store 16 rotations.
 */
public interface INBTBlockTransformable {

    static int transformMetaDeco(int meta, int coordBaseMode) {
        if (coordBaseMode == 0) return meta;
        switch (coordBaseMode) {
            case 1: //West
                switch (meta) {
                    case 2: return 5;
                    case 3: return 4;
                    case 4: return 2;
                    case 5: return 3;
                }
            case 2: //North
                switch (meta) {
                    case 2: return 3;
                    case 3: return 2;
                    case 4: return 5;
                    case 5: return 4;
                }
            case 3: //East
                switch (meta) {
                    case 2: return 4;
                    case 3: return 5;
                    case 4: return 3;
                    case 5: return 2;
                }
        }
        return meta;
    }

    static int transformMetaDecoModel(int meta, int coordBaseMode) {
        if (coordBaseMode == 0) return meta;
        int rot = (meta + coordBaseMode) % 4;
        int type = (meta / 4) * 4;

        return rot | type;
    }

    /** 1.7 {@code BlockDecoModel.transformMeta}: rotation in bits 2..3 (N 0, S 1, W 2, E 3), type in bits 0..1. */
    static int transformMetaDecoModelShifted(int meta, int coordBaseMode) {
        if (coordBaseMode == 0) return meta;
        int rot = meta >> 2;
        int type = meta & 3;

        switch (coordBaseMode) {
            default: break;
            case 1:
                if ((rot & 3) < 2) rot = rot ^ 3;
                else rot = rot ^ 2;
                break;
            case 2:
                rot = rot ^ 1;
                break;
            case 3:
                if ((rot & 3) < 2) rot = rot ^ 2;
                else rot = rot ^ 3;
                break;
        }
        return (rot << 2) | type;
    }

    static int transformMetaStairs(int meta, int coordBaseMode) {
        if (coordBaseMode == 0) return meta;
        switch (coordBaseMode) {
            case 1: //West
                if ((meta & 3) < 2) meta = meta ^ 2;
                else meta = meta ^ 3;
                break;
            case 2: //North
                meta = meta ^ 1;
                break;
            case 3: //East
                if ((meta & 3) < 2) meta = meta ^ 3;
                else meta = meta ^ 2;
                break;
        }
        return meta;
    }

    // same as stairs but 1 & 3 flipped
    static int transformMetaTrapdoor(int meta, int coordBaseMode) {
        if (coordBaseMode == 0) return meta;
        switch (coordBaseMode) {
            case 1:
                if ((meta & 3) < 2) meta = meta ^ 3;
                else meta = meta ^ 2;
                break;
            case 2:
                meta = meta ^ 1;
                break;
            case 3:
                if ((meta & 3) < 2) meta = meta ^ 2;
                else meta = meta ^ 3;
                break;
        }
        return meta;
    }

    static int transformMetaPillar(int meta, int coordBaseMode) {
        if (coordBaseMode == 0 || coordBaseMode == 2) return meta;
        int type = meta & 3;
        int rot = meta & 12;

        if (rot == 4) return type | 8;
        if (rot == 8) return type | 4;

        return meta;
    }

    static int transformMetaDirectional(int meta, int coordBaseMode) {
        if (coordBaseMode == 0) return meta;
        int rot = meta & 3;
        int other = meta & 12;

        switch (coordBaseMode) {
            default: break;
            case 1: rot = (rot + 1) % 4; break;
            case 2: rot ^= 2; break;
            case 3: rot = (rot + 3) % 4; break;
        }

        return other | rot;
    }

    static int transformMetaTorch(int meta, int coordBaseMode) {
        if (coordBaseMode == 0) return meta;
        switch (coordBaseMode) {
            case 1: //West
                switch (meta) {
                    case 1: return 3;
                    case 2: return 4;
                    case 3: return 2;
                    case 4: return 1;
                }
            case 2: //North
                switch (meta) {
                    case 1: return 2;
                    case 2: return 1;
                    case 3: return 4;
                    case 4: return 3;
                }
            case 3: //East
                switch (meta) {
                    case 1: return 4;
                    case 2: return 3;
                    case 3: return 1;
                    case 4: return 2;
                }
        }
        return meta;
    }

    static int transformMetaDoor(int meta, int coordBaseMode) {
        if (coordBaseMode == 0) return meta;
        if (meta == 8 || meta == 9) return meta; // ignore top parts

        return transformMetaDirectional(meta, coordBaseMode);
    }

    static int transformMetaLever(int meta, int coordBaseMode) {
        if (coordBaseMode == 0) return meta;
        if (meta <= 0 || meta >= 7) { //levers suck ass
            switch (coordBaseMode) {
                case 1: case 3: meta ^= 0b111;
            }
        } else if (meta >= 5) {
            switch (coordBaseMode) {
                case 1: case 3: meta = (meta + 1) % 2 + 5;
            }
        } else {
            meta = transformMetaTorch(meta, coordBaseMode);
        }

        return meta;
    }

    static int transformMetaVine(int meta, int coordBaseMode) {
        int result = 0;

        for (int i = 0; i < 4; i++) {
            int bit = 1 << i;
            if ((meta & bit) != 0) {
                result |= rotateVineBit(bit, coordBaseMode);
            }
        }

        return result;
    }

    static int rotateVineBit(int bit, int coordBaseMode) {
        int index;

        switch (bit) {
            case 1: index = 0; break; // south
            case 2: index = 1; break; // west
            case 4: index = 2; break; // north
            case 8: index = 3; break; // east
            default: return 0;
        }

        int rotated = index;

        switch (coordBaseMode) {
            case 1: rotated = (index + 1) % 4; break;
            case 2: rotated = (index + 2) % 4; break;
            case 3: rotated = (index + 3) % 4; break;
        }

        switch (rotated) {
            case 0: return 1;
            case 1: return 2;
            case 2: return 4;
            case 3: return 8;
        }

        return 0;
    }

    /** 1.7 {@code BlockDummyable.transformMeta}: the direction survives the core offset (+10) and extra (+6) bands. */
    static int transformMetaDummyable(int meta, int coordBaseMode) {
        final int offset = 10, extra = 6;
        boolean isOffset = meta >= 12; // squishing causes issues
        boolean isExtra = !isOffset && meta >= extra;

        if (isOffset) meta -= offset;
        else if (isExtra) meta -= extra;

        meta = transformMetaDeco(meta, coordBaseMode);

        if (isOffset) meta += offset;
        else if (isExtra) meta += extra;

        return meta;
    }

    /** 1.7 {@code Floodlight.transformMeta}: the deco table on meta % 6, the +6 "flip" band kept. */
    static int transformMetaFloodlight(int meta, int coordBaseMode) {
        if (meta >= 6) return transformMetaFloodlight(meta - 6, coordBaseMode) + 6;
        if (coordBaseMode == 0) return meta;
        switch (coordBaseMode) {
            case 1:
                switch (meta) { case 2: return 5; case 3: return 4; case 4: return 2; case 5: return 3; }
                break;
            case 2:
                switch (meta) { case 2: return 3; case 3: return 2; case 4: return 5; case 5: return 4; }
                break;
            case 3:
                switch (meta) { case 2: return 4; case 3: return 5; case 4: return 3; case 5: return 2; }
                break;
        }
        return meta;
    }
}
