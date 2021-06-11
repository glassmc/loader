package ml.glassmc.loader

class Loader(): ClassLoader() {

    private val parentLoader: ClassLoader = getSystemClassLoader()

    private val loadedClasses: MutableList<String> = ArrayList()

    override fun loadClass(name: String): Class<*> {
        loadedClasses.add(name)

        if(name.startsWith("java.") || name.startsWith("jdk.") || name.startsWith("sun.")) {
            return parentLoader.loadClass(name)
        }
        var data = loadClassData(name)!!

        /*for(transformer in transformers) {
            data = transformer.transform(name, data)
        }*/

        return defineClass(name, data, 0, data.size)
    }

    private fun loadClassData(className: String): ByteArray? {
        val inputStream = parentLoader.getResourceAsStream(className.replace(".", "/") + ".class")
        return inputStream?.readBytes()
    }

}