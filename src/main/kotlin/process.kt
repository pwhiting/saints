import com.rometools.rome.feed.synd.*
import com.rometools.rome.io.SyndFeedOutput
import java.io.FileReader
import java.io.FileWriter
import java.time.LocalDateTime.*
import java.time.ZoneOffset
import java.util.*
import com.opencsv.bean.CsvBindByName
import com.opencsv.bean.CsvToBeanBuilder

class AudioInfo {
    @CsvBindByName
    val title: String? = null
    @CsvBindByName
    val media_url: String? = null
    @CsvBindByName
    val file_size: Long = 0L
    @CsvBindByName
    val voice: String? = null
}

fun main(args: Array<String>) {
    try {
        if (args.size != 2) throw Exception("include csv with audio files as first argument and output file as second")
        val inputFilename = args[0]
        val outputFilename = args[1]
        val artifacts = CsvToBeanBuilder<AudioInfo>(FileReader(inputFilename)).
                withType(AudioInfo::class.java).build().parse()
        val rss = buildRSS(artifacts)
        FileWriter(outputFilename).use { writer -> SyndFeedOutput().output(rss, writer) }
    } catch (e: Exception) {
        println("Terminating with errors\n $e")
    }
}

/**
 * Build minimal RSS file
 * @param artifacts list of AudioInfo class instances containing audio file info
 * @return SyndFeed containing the RSS file
 */
fun buildRSS(artifacts: List<AudioInfo>): SyndFeedImpl {
    var dateCounter = now().toEpochSecond(ZoneOffset.UTC)
    val feed = SyndFeedImpl().apply {
        feedType = "rss_2.0"
        title = "Saints"
        link = "http://pete.dugg.in/s"
        description = "Saints podcast"
        publishedDate = Date(1000L * dateCounter)
    }
    artifacts.filter {it.voice=="male"}.forEach {artifact ->
        val enc = SyndEnclosureImpl().apply {
            length = artifact.file_size
            type = "audio/mpeg"
            url = artifact.media_url
        }
        val entry = SyndEntryImpl().apply {
            title = artifact.title
            link = artifact.media_url
            publishedDate = Date(1000L * dateCounter--)
            enclosures = listOf<SyndEnclosure>(enc)
        }
        feed.entries.add(entry)
    }
    return feed
}
