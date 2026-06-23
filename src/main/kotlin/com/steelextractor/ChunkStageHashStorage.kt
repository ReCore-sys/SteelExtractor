package com.steelextractor

import net.minecraft.core.SectionPos
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.LightLayer
import net.minecraft.world.level.chunk.status.ChunkStatus
import net.minecraft.world.level.lighting.LevelLightEngine
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

data class DimChunkPos(val pos: ChunkPos, val dimension: String)

data class BlockHashResult(
    val hash: String,
    val sectionData: List<IntArray?>
)

data class LightSectionData(
    val state: Int,
    val bytes: ByteArray?
)

data class LightChunkData(
    val minSection: Int,
    val sectionCount: Int,
    val skySources: IntArray,
    val sky: List<LightSectionData>,
    val block: List<LightSectionData>
)

data class LightHashResult(
    val hash: String,
    val data: LightChunkData
)

object ChunkStageHashStorage {
    private val hashes = ConcurrentHashMap<Pair<DimChunkPos, String>, String>()
    private val blockData = ConcurrentHashMap<Pair<DimChunkPos, String>, List<IntArray?>>()
    private val lightData = ConcurrentHashMap<DimChunkPos, LightChunkData>()
    private val trackedChunks = ConcurrentHashMap.newKeySet<DimChunkPos>()
    private val readyChunks = ConcurrentHashMap.newKeySet<DimChunkPos>()

    @Volatile
    private var readyLatch: CountDownLatch? = null

    /** Set by SteelExtractor before generating each dimension's chunks. Read by the mixin. */
    @Volatile
    var currentDimension: String = ""

    /** When false, block data is not stored in memory and binary dump is skipped. */
    @Volatile
    var enableBinaryDump: Boolean = false

    fun startTracking(chunks: Set<DimChunkPos>) {
        trackedChunks.addAll(chunks)
        readyLatch = CountDownLatch(chunks.size)
    }

    fun isTracking(pos: ChunkPos, dimension: String): Boolean {
        return trackedChunks.contains(DimChunkPos(pos, dimension))
    }

    fun markReady(pos: ChunkPos, dimension: String): Boolean {
        val key = DimChunkPos(pos, dimension)
        if (trackedChunks.contains(key) && readyChunks.add(key)) {
            readyLatch?.countDown()
            return true
        }
        return false
    }

    fun waitForAllReady(timeoutSeconds: Long): Boolean {
        return readyLatch?.await(timeoutSeconds, TimeUnit.SECONDS) ?: true
    }

    fun storeHash(pos: ChunkPos, dimension: String, stageName: String, hash: String) {
        hashes[Pair(DimChunkPos(pos, dimension), stageName)] = hash
    }

    fun storeBlockData(pos: ChunkPos, dimension: String, stageName: String, data: List<IntArray?>) {
        blockData[Pair(DimChunkPos(pos, dimension), stageName)] = data
    }

    fun storeLightData(pos: ChunkPos, dimension: String, data: LightChunkData) {
        lightData[DimChunkPos(pos, dimension)] = data
    }

    fun getAllHashes(): Map<Pair<DimChunkPos, String>, String> {
        return hashes.toMap()
    }

    fun getAllLightData(): Map<DimChunkPos, LightChunkData> {
        return lightData.toMap()
    }

    fun getAllBlockData(): Map<Pair<DimChunkPos, String>, List<IntArray?>> {
        return blockData.toMap()
    }

    fun getTrackedChunks(): Set<DimChunkPos> {
        return trackedChunks.toSet()
    }

    fun getReadyCount(): Int = readyChunks.size
    fun getTrackedCount(): Int = trackedChunks.size

    fun clear() {
        hashes.clear()
        blockData.clear()
        lightData.clear()
        trackedChunks.clear()
        readyChunks.clear()
        readyLatch = null
    }

    private fun MessageDigest.updateInt(value: Int) {
        update((value shr 24).toByte())
        update((value shr 16).toByte())
        update((value shr 8).toByte())
        update(value.toByte())
    }

    fun computeLightHash(lightEngine: LevelLightEngine, chunk: net.minecraft.world.level.chunk.ChunkAccess): String {
        return computeLightHashWithData(lightEngine, chunk).hash
    }

    fun computeLightHashWithData(
        lightEngine: LevelLightEngine,
        chunk: net.minecraft.world.level.chunk.ChunkAccess
    ): LightHashResult {
        val pos = chunk.pos
        val md = MessageDigest.getInstance("MD5")
        val minSection = lightEngine.minLightSection
        val sectionCount = lightEngine.lightSectionCount
        val skySources = skySources(chunk)

        md.updateInt(minSection)
        md.updateInt(sectionCount)
        val skyData = mutableListOf<LightSectionData>()
        val blockData = mutableListOf<LightSectionData>()
        for (layer in listOf(LightLayer.SKY, LightLayer.BLOCK)) {
            md.update(if (layer == LightLayer.SKY) 0.toByte() else 1.toByte())
            val listener = lightEngine.getLayerListener(layer)
            val rawData = if (layer == LightLayer.SKY) skyData else blockData
            for (sectionIndex in 0 until sectionCount) {
                val sectionPos = SectionPos.of(pos, minSection + sectionIndex)
                val data = listener.getDataLayerData(sectionPos)
                if (data == null) {
                    md.update(0.toByte())
                    rawData.add(LightSectionData(0, null))
                } else if (data.isEmpty) {
                    md.update(1.toByte())
                    rawData.add(LightSectionData(1, null))
                } else {
                    val bytes = data.copy().data
                    md.update(2.toByte())
                    md.update(bytes)
                    rawData.add(LightSectionData(2, bytes))
                }
            }
        }

        return LightHashResult(
            md.digest().joinToString("") { "%02x".format(it) },
            LightChunkData(
                minSection,
                sectionCount,
                skySources,
                skyData,
                blockData
            )
        )
    }

    private fun skySources(chunk: net.minecraft.world.level.chunk.ChunkAccess): IntArray {
        val sources = chunk.skyLightSources
        val values = IntArray(16 * 16)
        for (z in 0 until 16) {
            for (x in 0 until 16) {
                values[x + z * 16] = sources.getLowestSourceY(x, z)
            }
        }
        return values
    }

    fun computeBlockHash(sections: Iterable<net.minecraft.world.level.chunk.LevelChunkSection>): String {
        val md = MessageDigest.getInstance("MD5")
        for (section in sections) {
            if (section.hasOnlyAir()) {
                md.update(0.toByte())
            } else {
                val states = section.states
                for (y in 0 until 16) {
                    for (z in 0 until 16) {
                        for (x in 0 until 16) {
                            val stateId = net.minecraft.world.level.block.Block.getId(states.get(x, y, z))
                            md.update((stateId shr 24).toByte())
                            md.update((stateId shr 16).toByte())
                            md.update((stateId shr 8).toByte())
                            md.update(stateId.toByte())
                        }
                    }
                }
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    fun computeBlockHashWithData(sections: Iterable<net.minecraft.world.level.chunk.LevelChunkSection>): BlockHashResult {
        val md = MessageDigest.getInstance("MD5")
        val sectionDataList = mutableListOf<IntArray?>()

        for (section in sections) {
            if (section.hasOnlyAir()) {
                md.update(0.toByte())
                sectionDataList.add(null)
            } else {
                val stateIds = IntArray(4096)
                var idx = 0
                val states = section.states
                for (y in 0 until 16) {
                    for (z in 0 until 16) {
                        for (x in 0 until 16) {
                            val state = states.get(x, y, z)
                            val stateId = net.minecraft.world.level.block.Block.getId(state)
                            stateIds[idx] = stateId
                            idx++
                            md.update((stateId shr 24).toByte())
                            md.update((stateId shr 16).toByte())
                            md.update((stateId shr 8).toByte())
                            md.update(stateId.toByte())
                        }
                    }
                }
                sectionDataList.add(stateIds)
            }
        }

        val hash = md.digest().joinToString("") { "%02x".format(it) }
        return BlockHashResult(hash, sectionDataList)
    }
}
