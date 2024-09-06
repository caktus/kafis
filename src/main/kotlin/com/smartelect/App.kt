package com.smartelect

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import com.machinezoo.fingerprintio.TemplateFormatException
import com.machinezoo.sourceafis.FingerprintCompatibility.importTemplate
import com.machinezoo.sourceafis.FingerprintMatcher
import com.machinezoo.sourceafis.FingerprintTemplate
import org.apache.commons.codec.binary.Hex
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

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
    val right_thumbprint_scan: FingerprintTemplate,
    val left_thumbprint_scan: FingerprintTemplate
)

// Custom ThreadFactory to set UncaughtExceptionHandler
class CustomThreadFactory : ThreadFactory {
    override fun newThread(r: Runnable): Thread {
        val thread = Thread(r)
        thread.uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { t, e ->
            System.err.println("Exception in thread ${t.name}: ${e.message}")
            e.printStackTrace(System.err)
        }
        return thread
    }
}

class Hello : CliktCommand() {
    val fileName: String by option(help = "Path to CSV file with fingerprints").required()
    val threadCount: Int by option(help = "Number of threads to start").int().default(1)
    val subjectLimit: Int by option(help = "Compare only this number of subjects to each other").int().default(0)
    val outputScoreLimit: Int by option(help = "Only display scores larger than this value").int().default(-1)

    private fun <A> uniqueCombinations(
        lst: List<A>
    ): Sequence<Pair<A, List<A>>> {
        // Adapted from: https://stackoverflow.com/a/59144418/166053
        return lst.asSequence().mapIndexed { i, v ->
            Pair(v, lst.subList(i + 1, lst.size))
        }.filter { (_, subLst) ->
            subLst.isNotEmpty()
        }
    }

    private fun readCsv(): Sequence<Map<String, String>> {
        val file = File(fileName)
        val reader = Files.newBufferedReader(Paths.get(file.toURI()))
        val csvParser = CSVParser(reader, CSVFormat.DEFAULT.withHeader())
        return csvParser.asSequence().map { it.toMap() }
    }

    private fun performMatch(matcher: FingerprintMatcher, candidate: FingerprintTemplate): Double {
        try {
            return matcher.match(candidate)
        } catch (e: TemplateFormatException) {
            System.err.println("match failure: $e (candidate: $candidate)")
            return -1.0
        }
    }

    private fun getThumbprints(): Sequence<ThumbprintPair> {
        return readCsv().filter {
            // TODO: Allow one or the other to be empty and handle appropriately in matchSingleSubject
            it["left_thumbprint_scan"]?.isNotEmpty() == true && it["right_thumbprint_scan"]?.isNotEmpty() == true
        }.map {
            ThumbprintPair(
                UUID.fromString(it["entity_uuid"]),
                it["center_id"]!!.toInt(),
                importTemplate(Hex.decodeHex(it["right_thumbprint_scan"]!!)),
                importTemplate(Hex.decodeHex(it["left_thumbprint_scan"]!!))
            )
        }
    }

    private fun matchSingleSubject(
        subject: ThumbprintPair,
        candidates: List<ThumbprintPair>
    ): Long {
        val leftMatcher = FingerprintMatcher(subject.left_thumbprint_scan)
        val rightMatcher = FingerprintMatcher(subject.right_thumbprint_scan)
        for (candidate in candidates) {
            val leftScore = performMatch(leftMatcher, candidate.left_thumbprint_scan)
            val rightScore = performMatch(rightMatcher, candidate.right_thumbprint_scan)
            if (leftScore > outputScoreLimit || rightScore > outputScoreLimit)
                println("${subject.entity_uuid},${candidate.entity_uuid},$leftScore,$rightScore")
        }
        return candidates.size.toLong()
    }

    @ExperimentalTime
    override fun run() {
        System.err.println("fileName: $fileName")
        System.err.println("threadCount: $threadCount")
        System.err.println("subjectLimit: $subjectLimit")
        System.err.println("outputScoreLimit: $outputScoreLimit")
        val (combinations, loadTimeTaken) = measureTimedValue {
            getThumbprints().let {
                if (subjectLimit > 0) it.take(subjectLimit) else it
            }.toList().let {
                uniqueCombinations(it).toList()
            }
        }
        val size = combinations.map { (_, candidates) -> candidates.size.toLong() }.sum()
        System.err.println("Loaded $size subjects in $loadTimeTaken")

        val workerPool: ExecutorService = Executors.newFixedThreadPool(threadCount, CustomThreadFactory())
        println("subject_entity_uuid,candidate_entity_uuid,left_score,right_score")
        val (totalMatches, matchTimeTaken) = measureTimedValue {
            combinations.map { (subject, candidates) ->
                workerPool.submit<Long> {
                    matchSingleSubject(subject, candidates)
                }
            }.map { future ->
                future.get() ?: 0L
            }.sum() // sum of candidate counts for all subjects
        }
        val matchRate = ((size.toDouble() / matchTimeTaken.inWholeMilliseconds.toDouble()) * 1000.0).toInt()
        System.err.println("Matched $totalMatches subjects in $matchTimeTaken ($matchRate / sec)")
        if (totalMatches != size)
            System.err.println("WARNING: Matched $totalMatches but expected $size")

        workerPool.shutdown()
        workerPool.awaitTermination(30L, java.util.concurrent.TimeUnit.SECONDS)
    }
}

fun main(args: Array<String>) = Hello().main(args)
