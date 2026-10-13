// Golem Core KubeJS adapter smoke check (dev runs with -Pkubejs.run=true only). Positive signals, in logs/kubejs/server.log:
// whether KubeJS has HBM recipe table schemas, and whether the HBM script binding exists.
ServerEvents.recipes(event => {
  console.info(`[golem-kubejs] event.recipes.hbm.press: ${typeof event.recipes.hbm.press}`)
  console.info(`[golem-kubejs] HBM binding: ${typeof HBM}`)
})
