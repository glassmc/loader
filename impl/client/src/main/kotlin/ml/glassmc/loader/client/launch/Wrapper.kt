package ml.glassmc.loader.client.launch

import ml.glassmc.loader.GlassLoader
import ml.glassmc.loader.client.GlassClient
import ml.glassmc.loader.client.hook.ClientInitializeHook

class Wrapper {

    fun launch(args: Array<String>) {
        GlassClient.setup(args[args.binarySearch("--version") + 1])
        GlassLoader.runHooks(ClientInitializeHook::class.java)

        val mainClass = Class.forName("net.minecraft.client.main.Main")
        val mainMethod = mainClass.getMethod("main", Array<String>::class.java)
        mainMethod.invoke(null, args)
    }

}