package com.hbm.blocks.bomb;

public class BlockSemtex extends BlockTNTBase {
    public BlockSemtex(Properties properties) { super(properties); }
    @Override public int kindId() { return 2; }
    @Override public String textureName() { return "semtex"; }
    @Override public float blastStrength() { return 12F; }
}
