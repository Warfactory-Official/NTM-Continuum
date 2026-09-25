NTMEvents.hazards(e => {
    e.radResistant('minecraft:tinted_glass')
    e.radResistant('#minecraft:wool')
    e.radResistant('hbm:ducrete_smooth', false)
    e.block('minecraft:amethyst_block').radiation(4)
    e.armor('minecraft:leather_helmet', a => a.radResistance(0.5).protects('PARTICLE_COARSE', 'GAS_LUNG').damage('PHYS', 2, 0.1))
})
