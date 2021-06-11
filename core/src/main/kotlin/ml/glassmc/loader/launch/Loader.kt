package ml.glassmc.loader.launch

class Loader(private val transformers: List<ITransformer>): ClassLoader() {

    private val parentLoader: ClassLoader = getSystemClassLoader()

    private val loadedClasses: MutableList<String> = ArrayList()

    override fun loadClass(name: String): Class<*> {
        loadedClasses.add(name)

        if(name.startsWith("java.") || name.startsWith("jdk.") || name.startsWith("sun.")) {
            return this.parentLoader.loadClass(name)
        }
        var data = loadClassData(name) ?: throw ClassNotFoundException(name)

        for(transformer in this.transformers) {
            data = transformer.transform(name, data)
        }

        return defineClass(name, data, 0, data.size)
    }

    private fun loadClassData(className: String): ByteArray? {
        val inputStream = parentLoader.getResourceAsStream(className.replace(".", "/") + ".class")
        return inputStream?.readBytes()
    }

}