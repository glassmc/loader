package ml.glassmc.loader.client

import ml.glassmc.loader.GlassLoader
import ml.glassmc.loader.ShardSpecification
import ml.glassmc.loader.client.hook.ClientPreInitializeHook

fun main(args: Array<String>) {
    GlassLoader.registerVirtualShard(ShardSpecification("client-loader", "0.0.1"))
    GlassLoader.registerVirtualShard(ShardSpecification("client", args[args.indexOf("--version") + 1]))

    GlassLoader.runHooks(ClientPreInitializeHook::class.java)

    val launcher = GlassLoader.launcher
    launcher.target = "ml.glassmc.loader.client.ClientWrapper"
    launcher.arguments.addAll(args)
    GlassLoader.launcher.launch()
}