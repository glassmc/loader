package ml.glassmc.loader

class ShardInfo(val specification: ShardSpecification, val hooks: Map<Class<*>, Class<out Listener>>, val requires: List<ShardSpecification>, val breaks: List<ShardSpecification>)