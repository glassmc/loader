package ml.glassmc.loader.client.launch

import ml.glassmc.loader.GlassLoader
import ml.glassmc.loader.Loader
import ml.glassmc.loader.client.GlassClient
import ml.glassmc.loader.client.hook.ClientPreInitializeHook

fun main(args: Array<String>) {
    GlassClient.setup(args[args.binarySearch("--version") + 1])
    GlassLoader.runHooks(ClientPreInitializeHook::class.java)
    val classLoader = Loader()

    val wrapperClass = classLoader.loadClass("ml.glassmc.loader.client.launch.Wrapper")
    val method = wrapperClass.getMethod("launch", Array<String>::class.java)
    method.invoke(wrapperClass.newInstance(), args)
}