package ml.glassmc.loader

import org.apache.commons.io.IOUtils
import org.json.JSONObject
import java.util.HashMap
import java.nio.charset.StandardCharsets
import java.util.ArrayList
import java.util.stream.Collectors

object GlassLoader {

    private val virtualShards: MutableList<ShardSpecification> = ArrayList()
    val apis: MutableList<Any> = ArrayList()

    init {
        registerVirtualShard(ShardSpecification("loader", "0.0.1"))
    }

    private fun collectShardInfo(): List<ShardInfo> {
        val classLoader = GlassLoader::class.java.classLoader

        val shardInfo: MutableList<ShardInfo> = ArrayList()

        val shardInfoLocations = classLoader.getResources("shard.json")
        while (shardInfoLocations.hasMoreElements()) {
            val shardInfoLocation = shardInfoLocations.nextElement()
            val shardInfoStream = shardInfoLocation.openStream()
            val shardInfoText = IOUtils.toString(shardInfoStream, StandardCharsets.UTF_8)
            shardInfo.add(parseShardInfo(shardInfoText))
        }
        return shardInfo
    }

    private fun parseShardInfo(shardInfo: String): ShardInfo {
        val shardInfoJSON = JSONObject(shardInfo)

        val id = shardInfoJSON.getString("id")
        val version = shardInfoJSON.getString("version")
        val specification = ShardSpecification(id, version)

        val referenceClass = if (shardInfoJSON.has("reference")) shardInfoJSON.getString("reference") else null
        val hooks: MutableMap<Class<*>, Class<out Listener>> = HashMap()
        if (referenceClass != null) {
            val reference = Class.forName(referenceClass).newInstance() as Reference
            hooks.putAll(reference.listeners)
        }

        val dependenciesJSON = if (shardInfoJSON.has("dependencies")) shardInfoJSON.getJSONObject("dependencies") else JSONObject()
        val dependencies: MutableList<ShardSpecification> = ArrayList()
        for (dependencyID in dependenciesJSON.keySet()) {
            val dependencyVersion = dependenciesJSON.getString(id)
            dependencies.add(ShardSpecification(dependencyID, dependencyVersion))
        }

        return ShardInfo(specification, hooks, dependencies)
    }

    fun runHooks(hookType: Class<*>) {
        val shardInfoComplete = collectShardInfo()
        val shardInfoFiltered = shardInfoComplete.stream()
                .filter { shardInfo: ShardInfo -> shardInfo.hooks.containsKey(hookType) }.collect(Collectors.toList())
        var index = 0
        var failedCounter = 0

        val availableShards = getAvailableShards(shardInfoFiltered)
        while (index < shardInfoFiltered.size) {
            val shardInfo = shardInfoFiltered[index]
            var dependenciesSatisfied = true
            for (dependency in shardInfo.dependencies) {
                var satisfied = false
                for (specification in availableShards) {
                    if (dependency.isSatisfied(specification)) {
                        satisfied = true
                    }
                }
                if (!satisfied) {
                    dependenciesSatisfied = false
                }
            }
            if (dependenciesSatisfied) {
                val hookClass = shardInfo.hooks[hookType]!!
                val hook = hookClass.newInstance() as Listener
                hook.run()

                index++
                failedCounter = 0
            } else {
                shardInfoFiltered.remove(shardInfo)
                shardInfoFiltered.add(shardInfo)
                failedCounter++
            }
            if (failedCounter > shardInfoFiltered.size) {
                println("Failed to load shard(s):")
                for (i in index until shardInfoFiltered.size) {
                    println(" - " + shardInfoFiltered[i].specification.id)
                }
                index = shardInfoFiltered.size
            }
        }
    }

    private fun getAvailableShards(shardInfoList: List<ShardInfo>): List<ShardSpecification> {
        val availableShards: MutableList<ShardSpecification> = ArrayList()
        for (shardInfo in shardInfoList) {
            availableShards.add(shardInfo.specification)
        }
        availableShards.addAll(virtualShards)
        return availableShards
    }

    fun registerVirtualShard(specification: ShardSpecification) {
        virtualShards.add(specification)
    }

    fun registerAPI(api: Any) {
        apis.add(api)
    }

    inline fun <reified T> getAPI(): T {
        for (api in apis) {
            if (api is T) {
                return api
            }
        }
        return null!!
    }
}