package ml.glassmc.loader

interface Reference {
    val listeners: Map<Class<*>, Class<out Listener>>
}