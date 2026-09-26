NTMEvents.shapes(e => {
    e.create('GAMETEST_GEAR', 'gt_gear', 'gt_gears', 144, '%s Test Gear').autogen('hbm:items/plate_cast')
})

NTMEvents.materials(e => {
    e.create('gametestium')
        .colors(0xE0E0FF, 0x303060, 0xA0A0FF)
        .displayName('Gametestium')
        .smelting('SMELTABLE')
        .autogen('CASTPLATE', 'GAMETEST_GEAR')
    e.get('iron').autogen('GAMETEST_GEAR')
})

NTMEvents.fluids(e => {
    e.create('gametest_fluid', 0x40A040, 'custom_toxin')
        .displayName('Gametest Fluid')
        .temperature(321)
        .corrosive(33)
        .flammable(12345)
        .combustible('HIGH', 23456)
})

NTMEvents.fuels(e => {
    e.rbmk.add('gametest', 'Gametest RBMK Fuel Rod', r => r
        .fullName('Gametest Fuel').yield(12345678).reactivity(21).heat(0.5).meltingPoint(1999)
        .burn('LINEAR').deplete('STATIC').neutrons('FAST', 'SLOW').tint(0x123456)
        .radiation(3, 30)
        .pellet('Gametest Pellet', 'Gametest Pellet Fuel', p => p.noXenon()))
    e.watz.add('gametest', 'Gametest Watz Pellet', w => w.passive(10).heatEmission(5).burn(f => f.linear(1)).radiation(1, 2))
    e.zirnox.add('gametest', 'Gametest ZIRNOX Rod', z => z.life(1234).heat(5).radiation(1, 10)
        .waste('gametest', 'Gametest Waste'))
    e.pwr.add('gametest', 'Gametest PWR Fuel Rod', f => f.heat(7.5).radiation(2))
    e.researchReactor.add('gametest', 'Gametest Plate', p => p.life(4321).function('LINEAR').reactivity(12)
        .waste('hbm:waste_plate_u235'))
    e.rtg.add('gametest', 'Gametest RTG Pellet', r => r.heat(9).halfLife(1000).halfLives(2).decay('LEAD'))
})

NTMEvents.recipes(e => {
    e.centrifuge.add('minecraft:tuff', 'minecraft:emerald', '2x minecraft:gold_nugget')
    e.cracking.add('gametest_fluid', '50x water', '10x steam')
    e.assembly.add('gametest.assembly', r => r.duration(7).power(11).inputs('2x minecraft:dead_bush').outputs('minecraft:diamond'))
    e.assembly.remove('ass.pilenu')
    e.chemicalPlant.add('gametest.chem', r => r.duration(9).fluidInputs('100x gametest_fluid').fluidOutputs('50x water'))
    e.shredder.add('minecraft:dead_bush', '3x minecraft:diamond')
})

NTMEvents.hazards(e => {
    e.item('minecraft:dead_bush').add('radiation', 7)
    e.type('kubejs:gametest', t => t.name('Gametest').trait('GREEN'))
    e.item('minecraft:sculk_vein').add('kubejs:gametest', 2)
})
