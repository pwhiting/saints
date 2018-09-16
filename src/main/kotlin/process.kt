import com.rometools.rome.feed.synd.*
import com.rometools.rome.io.SyndFeedOutput
import com.opencsv.CSVReaderHeaderAware
import java.io.BufferedReader
import java.io.FileReader
import java.io.FileWriter
import java.time.LocalDateTime.*
import java.time.ZoneOffset
import java.util.*
import com.opencsv.bean.CsvBindByName



/**
 * Build minimal RSS file
 * @param audioFiles list of maps containing audio file info
 * @return SyndFeed containing the RSS file
 */
fun buildRSS(audioFiles: List<MutableMap<String, String>>): SyndFeedImpl {
    val feed = SyndFeedImpl().apply {
        feedType = "rss_2.0"
        title = "Saints"
        link = "http://pete.dugg.in/s"
        description = "Saints podcast"
    }
    // create a date variable to be used for publishedDate - set it to current time
    var dateCounter = now().toEpochSecond(ZoneOffset.UTC)

    audioFiles.forEach { audioFile ->
        val enc = SyndEnclosureImpl().apply {
            length = audioFile[CSVHeaders.SIZE.value]?.toLongOrNull() ?: 0
            type = "audio/mpeg"
            url = audioFile[CSVHeaders.MEDIA.value]
        }
        val entry = SyndEntryImpl().apply {
            title = audioFile[CSVHeaders.TITLE.value]
            link = audioFile[CSVHeaders.MEDIA.value]
            publishedDate = Date(1000L * dateCounter--)
            enclosures = listOf<SyndEnclosure>(enc)
        }
        feed.entries.add(entry)
    }
    return feed
}

/**
 * @param feed RSS feed to be written to outputFileName
 * @param outputFileName filename to write
 */
fun writeRSS(feed: SyndFeed, outputFileName: String) {
    FileWriter(outputFileName).use { writer ->
        SyndFeedOutput().output(feed, writer)
    }
}

fun main(args: Array<String>) {
    try {
        if (args.size != 2) throw Exception("include csv with audio files as first argument and output file as second")
        val input = args[0]
        val output = args[1]
        writeRSS(buildRSS(readFile(input)), output)
    } catch (e: Exception) {
        println("Terminating with errors\n $e")
    }
}

enum class CSVHeaders(val value: String) {
    ID("id"),
    TITLE("title"),
    MEDIA("media_url"),
    SIZE("file_size"),
    DURATION("duration"),
    VOICE("voice")
}

/**
 * Read the CSV file
 * @param csvFilename CSV Filename with RSS audio data, first row must contain column names
 * @return List of RssAudioItems
 */
fun readFile(csvFilename: String): List<MutableMap<String, String>> {

    val audioFiles = mutableListOf<MutableMap<String, String>>()
    CSVReaderHeaderAware(BufferedReader(FileReader(csvFilename))).use { csvReader ->
        var audioFile = csvReader.readMap()
        var missing = ""
        CSVHeaders.values().forEach { key ->
            if (!audioFile.containsKey(key.value)) missing += "\n\tmissing column ${key.value} in csv file"
        }
        if (missing.length > 0) throw Exception("malformed CSV file: $csvFilename. $missing")
        while (audioFile != null) {
            if (audioFile[CSVHeaders.VOICE.value] == "male") audioFiles.add(audioFile)
            audioFile = csvReader.readMap()
        }
    }
    return audioFiles
}


