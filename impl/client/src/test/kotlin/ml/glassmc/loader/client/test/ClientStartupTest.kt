package ml.glassmc.loader.client.test

import ml.glassmc.loader.client.launch.main
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.reflect.InvocationTargetException

class ClientStartupTest {

    @Test
    fun test() {
        assertThrows<InvocationTargetException> {
            main(arrayOf("--version", "1.8.9"))
        }
    }

}