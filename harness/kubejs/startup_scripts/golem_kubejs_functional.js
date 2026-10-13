// KubeJS functional check for the harness scenario KubeJsSupport: a material registered by a startup script.
// The scenario checks it through the HBM binding ("kjs material" in chat).
HBMEvents.registry(event => {
  event.material('kubejs_testium').displayName('Testium').colors(0xA0E0FF, 0x406080, 0xFF8040)
})
