package ml.glassmc.loader

import ml.glassmc.loader.ShardSpecification.Version.ComparisonResult
import java.util.ArrayList
import kotlin.math.max

class ShardSpecification(val id: String, val version: String) {

    fun isSatisfied(other: ShardSpecification): Boolean {
        if (id != other.id) {
            return false
        }
        if (version == "*") {
            return true
        }

        val thisVersion = Version(version.replace("[^0-9.,x]+".toRegex(), ""))
        val otherVersion = Version(other.version)
        if (version.startsWith(">")) {
            if (otherVersion.compare(thisVersion) == ComparisonResult.GREATER) {
                return true
            }
        }
        if (version.startsWith("<")) {
            if (otherVersion.compare(thisVersion) == ComparisonResult.LESS) {
                return true
            }
        }
        return if (!(version.contains("<") || version.contains(">")) || version.contains("=")) {
            otherVersion.compare(thisVersion) == ComparisonResult.EQUAL
        } else false
    }

    private class Version constructor(versionString: String) {

        private val separated: MutableList<String> = ArrayList()

        init {
            separated.addAll(versionString.split("."))
        }

        fun compare(other: Version): ComparisonResult {
            val maxSize = max(this.separated.size, other.separated.size)
            for (i in 0 until maxSize) {
                val thisNumberString = if (this.separated.size > i) this.separated[i] else "0"
                val otherNumberString = if (other.separated.size > i) other.separated[i] else "0"

                if (thisNumberString != "x" && otherNumberString != "x") {
                    val thisNumber = thisNumberString.toInt()
                    val otherNumber = otherNumberString.toInt()

                    if (thisNumber > otherNumber) {
                        return ComparisonResult.GREATER
                    }
                    if (thisNumber < otherNumber) {
                        return ComparisonResult.LESS
                    }
                }
            }
            return ComparisonResult.EQUAL
        }

        enum class ComparisonResult {
            GREATER, LESS, EQUAL
        }
    }
}