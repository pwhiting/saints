import com.rometools.rome.feed.synd.*
import com.rometools.rome.io.SyndFeedOutput
import com.opencsv.CSVReaderHeaderAware
import java.io.BufferedReader
import java.io.FileReader
import java.io.FileWriter
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.LocalDateTime.*
import java.time.ZoneOffset
import java.util.*
import javax.swing.text.DateFormatter

fun main(args: Array<String>) {
    if (args.size != 2) {
        println("include csv with audio files as first argument and output file as second")
        return
    }
    try {
        writeRSS(buildRSS(readFile(args[0])),args[1])
    } catch (e: Exception) {
        println("Terminating with errors: $e")
    }
}

enum class CSVHeaders(val value:String) {
    ID ("id"),
    TITLE ("title"),
    MEDIA ("media_url"),
    SIZE ( "file_size"),
    DURATION ( "duration"),
    VOICE ("voice")
}

fun readFile(file: String): List<MutableMap<String, String>> {
    val csvReader = CSVReaderHeaderAware(BufferedReader(FileReader(file)))
    var audioFile = csvReader.readMap()
    val audioFiles = mutableListOf<MutableMap<String, String>>()
    var success = true
    for (key in CSVHeaders.values()){
        if(!audioFile.containsKey(key.value)) {
            println("missing column ${key.value} in csv file")
            success = false
        }

    }
    while (audioFile != null) {
        audioFiles.add(audioFile)
        audioFile = csvReader.readMap()
    }
    csvReader.close()
    if(!success) throw IllegalArgumentException("malformed CSV file $file")
    return audioFiles
}

fun buildRSS(audioFiles: List<MutableMap<String, String>>): SyndFeedImpl {
    val feed = SyndFeedImpl()
    feed.feedType = "rss_2.0"
    feed.title = "Saints"
    feed.link = "http://pete.dugg.in/s"
    feed.description = "Saints podcast"
    var dateCounter = now().toEpochSecond(ZoneOffset.UTC)
    for(file in audioFiles) {
        if(file[CSVHeaders.VOICE.value]=="female") continue
        val entry = SyndEntryImpl()
        entry.title = file[CSVHeaders.TITLE.value]
        entry.link = file[CSVHeaders.MEDIA.value]
        entry.publishedDate = Date(1000L*dateCounter++)
        val enc = SyndEnclosureImpl()
        enc.length=file[CSVHeaders.SIZE.value]?.toLongOrNull() ?: 0
        enc.type="audio/mpeg"
        enc.url=file[CSVHeaders.MEDIA.value]
        entry.enclosures= listOf<SyndEnclosure>(enc)
        feed.entries.add(entry)
    }
    return feed
}

fun writeRSS(feed: SyndFeed,file: String) {
    val writer = FileWriter(file)
    SyndFeedOutput().output(feed,writer)
    writer.close()
}
