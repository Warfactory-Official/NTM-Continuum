package com.hbm.blocks.bomb;

public class BlockTNT extends BlockTNTBase {
    public BlockTNT(Properties properties) { super(properties); }
    @Override public int kindId() { return 0; }
    @Override public String textureName() { return "tnt"; }
    @Override public float blastStrength() { return 10F; }
}
