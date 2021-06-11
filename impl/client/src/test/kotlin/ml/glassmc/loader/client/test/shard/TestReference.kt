package ml.glassmc.loader.client.test.shard

import ml.glassmc.loader.Listener
import ml.glassmc.loader.Reference
import ml.glassmc.loader.client.hook.ClientInitializeHook
import java.util.HashMap

class TestReference : Reference {

    override val listeners: Map<Class<*>, Class<out Listener>>
        get() = object: HashMap<Class<*>, Class<out Listener>>() {
            init {
                put(ClientInitializeHook::class.java, TestClientInitializeListener::class.java)
            }
        }

}