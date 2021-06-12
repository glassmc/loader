package ml.glassmc.loader.client

import ml.glassmc.loader.GlassLoader
import ml.glassmc.loader.client.hook.ClientInitializeHook

class ClientWrapper {

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            GlassLoader.runHooks(ClientInitializeHook::class.java)

            val mainClass = Class.forName("net.minecraft.client.main.Main")
            val mainMethod = mainClass.getMethod("main", Array<String>::class.java)
            mainMethod.invoke(null, args)
        }

    }

}