package ml.glassmc.loader.test

import ml.glassmc.loader.GlassLoader
import ml.glassmc.loader.test.shard.TestHook
import org.junit.jupiter.api.Test

class RunHooksTest {

    @Test
    fun test() {
        GlassLoader.runHooks(TestHook::class.java)
    }

}