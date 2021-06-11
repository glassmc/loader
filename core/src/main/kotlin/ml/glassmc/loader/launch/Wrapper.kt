package ml.glassmc.loader.launch

class Wrapper {

    fun launch(target: String, args: Array<String>) {
        val targetClass = Class.forName(target)
        val targetMethod = targetClass.getMethod("main", Array<String>::class.java)
        targetMethod.invoke(null, args)
    }

}