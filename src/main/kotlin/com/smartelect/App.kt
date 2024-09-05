package com.smartelect

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.int
import com.machinezoo.sourceafis.FingerprintCompatibility.convert
import com.machinezoo.sourceafis.FingerprintMatcher
import org.apache.commons.codec.binary.Hex
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

class App {
    // TODO: What does this do?
    val greeting: String
        get() {
            return "Hello World!"
        }
}

data class ThumbprintPair(
    val entity_uuid: UUID,
    val center_id: Int,
    val right_thumbprint_scan: ByteArray,
    val left_thumbprint_scan: ByteArray
)

class Hello : CliktCommand() {
    val fileName: String by option(help = "Path to CSV file with fingerprints").required()
    val threadCount: Int by option(help = "Number of threads to start").int().default(1)
    val limit: Int by option(help="Compare only this number of subjects to each other").int().default(0)

    private fun <A> uniqueCombinations(
        lst: List<A>
    ): Sequence<Pair<A, List<A>>> =
        // Adapted from: https://stackoverflow.com/a/59144418/166053
        sequence {
            lst.forEachIndexed { i, v ->
                val subLst = lst.subList(i + 1, lst.size)
                if (subLst.isNotEmpty())
                    yield(Pair(v, subLst))
            }
        }

    private fun readCsv(): Sequence<Map<String, String>> {
        val file = File(fileName)
        val reader = Files.newBufferedReader(Paths.get(file.toURI()))
        val csvParser = CSVParser(reader, CSVFormat.DEFAULT.withHeader())
        return csvParser.asSequence().map { it.toMap() }
    }

    private fun performMatch(matcher: FingerprintMatcher, candidate: ByteArray): Double {
        return matcher.match(convert(candidate))
    }

    private fun getThumbprints(): Sequence<ThumbprintPair> {
        return readCsv().map {
            ThumbprintPair(
                UUID.fromString(it["entity_uuid"]),
                it["center_id"]!!.toInt(),
                Hex.decodeHex(it["right_thumbprint_scan"]!!.toString()),
                Hex.decodeHex(it["left_thumbprint_scan"]!!.toString())
            )
        }
    }

    private fun matchSingleSubject(
        subject: ThumbprintPair,
        candidates: List<ThumbprintPair>
    ) {
        val leftMatcher = FingerprintMatcher().index(convert(subject.left_thumbprint_scan))
        val rightMatcher = FingerprintMatcher().index(convert(subject.right_thumbprint_scan))

        for (candidate in candidates) {
            val leftScore = performMatch(leftMatcher, candidate.left_thumbprint_scan)
            val rightScore = performMatch(rightMatcher, candidate.right_thumbprint_scan)
            if (leftScore > 40 || rightScore > 40)
                println("$leftScore,$rightScore")
        }
    }

    @ExperimentalTime
    override fun run() {
        System.err.println("fileName: $fileName")
        System.err.println("threadCount: $threadCount")
        System.err.println("limit: $limit")
        val thumbprints = if (limit > 0) {
            getThumbprints().take(limit).toList()
        } else {
            getThumbprints().toList()
        }
        val workerPool: ExecutorService = Executors.newFixedThreadPool(threadCount)
        val combinations = uniqueCombinations(thumbprints).toList()
        val size = combinations.map({ it.second.size.toLong() }).sum()

        val timeTaken = measureTime {
            for ((subject, candidates) in combinations) {
                workerPool.submit {
                    matchSingleSubject(subject, candidates)
                }
            }
            workerPool.shutdown()
            workerPool.awaitTermination(60L, java.util.concurrent.TimeUnit.MINUTES)
        }
        val rate = (size / timeTaken.inWholeSeconds).toInt()
        System.err.println("Matched $size pairs in $timeTaken ($rate / sec)")
    }
}

fun main(args: Array<String>) = Hello().main(args)

