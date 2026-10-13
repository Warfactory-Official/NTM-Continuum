// KubeJS functional check for the harness scenario KubeJsSupport (dev runs with -Pkubejs.run=true only). The same file
// runs on every KubeJS node (8.x, 7.x, 6.x): it uses only syntax the Golem Core adapter promises on all three.
// Positive signals: recipe ids the client receives in Golem Core's recipe-table sync, and "[kjs] ..." chat lines.

ServerEvents.recipes(event => {
  // typed positional arguments: output item, input ingredient, stamp
  event.recipes.hbm.press('minecraft:diamond', 'minecraft:coal_block', 'flat').id('kubejs:press_typed')
  // a counted output and a tag input
  event.recipes.hbm.shredder('2x minecraft:gravel', '#minecraft:logs').id('kubejs:shredder_tag')
  // a plain object passes through unchanged (HBM's own ingredient form)
  event.recipes.hbm.press('minecraft:emerald', {ingredient: 'minecraft:gold_block', count: 2}, 'flat').id('kubejs:press_raw')
  // a generic table: required name first, the rest by builder methods (camelCase aliases), a fluid output
  event.recipes.hbm.chemical_plant('kjs.test')
    .inputItems(['2x minecraft:iron_ingot'])
    .outputFluids([Fluid.of('hbm:oxygen', 500)])
    .duration(40)
    .power(100)
    .id('kubejs:chem_typed')
  // removal by id, and by type + output (Shape.read answers the plain output form)
  event.remove({id: 'hbm:press/ball_resin'})
  event.remove({type: 'hbm:press', output: 'hbm:biomass_compressed'})
})

// observe-only events: report to chat so the client log shows them
HBMEvents.explosion(event => {
  event.level.server.runCommandSilent(`say [kjs] explosion ${event.kind} size ${event.size}`)
})
HBMEvents.entityIrradiated(event => {
  event.entity.level.server.runCommandSilent(`say [kjs] irradiated ${event.hazard} dose>0 ${event.dose > 0}`)
})

// the HBM binding, driven from chat
const ExplosionVNT = Java.loadClass('com.hbm.explosion.vanillant.ExplosionVNT')
PlayerEvents.chat(event => {
  const msg = event.message
  const player = event.player
  const server = player.level.server
  if (msg == 'kjs binding') {
    // On the server thread: KubeJS 6 posts chat events off it (Architectury's chat hook), and chunk radiation reads
    // only loaded chunks, which vanilla answers on the server thread alone.
    server.execute(() => {
      HBM.setRadiation(player, 123)
      const fluid = HBM.fluid('hbm:oxygen')
      const iron = HBM.material('iron')
      HBM.setChunkRadiation(player.level, player.blockPosition(), 77)
      server.runCommandSilent(`say [kjs] rad ${Math.round(HBM.getRadiation(player))} fluid ${fluid ? fluid.id : 'none'} iron ${iron ? 'yes' : 'no'} chunk ${Math.round(HBM.getChunkRadiation(player.level, player.blockPosition()))}`)
    })
  }
  if (msg == 'kjs material') {
    // No block-level declarations here: Rhino scopes them to the whole handler function.
    server.runCommandSilent(`say [kjs] material ${HBM.material('kubejs_testium') ? HBM.material('kubejs_testium').name : 'none'}`)
  }
  if (msg == 'kjs boom') {
    server.execute(() => new ExplosionVNT(player.level, player.x + 8, player.y, player.z, 2.0).makeStandard().explode())
  }
  if (msg == 'kjs irradiate') {
    server.execute(() => {
      const pig = player.level.createEntity('minecraft:pig')
      pig.setPosition(player.x + 3, player.y, player.z)
      pig.spawn()
      HBM.contaminate(pig, 'RADIATION', 'NONE', 10)
    })
  }
})
