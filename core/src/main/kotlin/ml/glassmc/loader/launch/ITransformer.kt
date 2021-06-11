package ml.glassmc.loader.launch

interface ITransformer {
    fun transform(className: String, data: ByteArray): ByteArray
}