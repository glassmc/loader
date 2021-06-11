package ml.glassmc.loader.launch

class Launcher {

    lateinit var target: String
    val arguments: MutableList<String> = ArrayList()
    val transformers: MutableList<ITransformer> = ArrayList()

    fun launch() {
        val classLoader = Loader(transformers)

        val `class` = classLoader.loadClass("ml.glassmc.loader.launch.Wrapper")
        val method = `class`.getMethod("launch", String::class.java, Array<String>::class.java)
        method.invoke(`class`.newInstance(), target, arguments)
    }

}