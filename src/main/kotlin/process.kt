import com.rometools.rome.feed.synd.*
import com.rometools.rome.io.SyndFeedOutput
import com.opencsv.CSVReaderHeaderAware
import java.io.BufferedReader
import java.io.FileReader
import java.io.FileWriter
import java.time.LocalDateTime.*
import java.time.ZoneOffset
import java.util.*

fun main(args: Array<String>) {
    try{
        if (args.size != 2) throw Exception("include csv with audio files as first argument and output file as second")
        writeRSS(buildRSS(readFile(args[0])),args[1]) // bad form?
    } catch (e: Exception) {
        println("Terminating with errors\n $e")
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

// read csv file containing rows of audio files with at least the columns in CSVHeaders above.
fun readFile(file: String): List<MutableMap<String, String>> {
    val audioFiles = mutableListOf<MutableMap<String, String>>()
    try {
        val csvReader = CSVReaderHeaderAware(BufferedReader(FileReader(file)))
        var audioFile = csvReader.readMap()
        var missing = ""
        for (key in CSVHeaders.values()) {
            if (!audioFile.containsKey(key.value)) missing+="\n\tmissing column ${key.value} in csv file"
        }
        if(missing.length==0) {
            while (audioFile != null) {
                audioFiles.add(audioFile)
                audioFile = csvReader.readMap()
            }
        }
        csvReader.close()
        if (missing.length>0) throw Exception("malformed CSV file: $file. $missing")
    } catch (e: Exception) {
        throw Exception("Error reading CSV file\n $e")
    }
    return audioFiles
}


// Build a minimal RSS feed using a list of audio files
fun buildRSS(audioFiles: List<MutableMap<String, String>>): SyndFeedImpl {
    val feed = SyndFeedImpl()
    try {
        feed.feedType = "rss_2.0"
        feed.title = "Saints"
        feed.link = "http://pete.dugg.in/s"
        feed.description = "Saints podcast"
        // create a date variable to be used for publishedDate - set it to current time
        var dateCounter = now().toEpochSecond(ZoneOffset.UTC)
        for (file in audioFiles) {
            if (file[CSVHeaders.VOICE.value] == "female") continue
            val entry = SyndEntryImpl()
            entry.title = file[CSVHeaders.TITLE.value]
            entry.link = file[CSVHeaders.MEDIA.value]
            entry.publishedDate = Date(1000L * dateCounter--)
            val enc = SyndEnclosureImpl()
            enc.length = file[CSVHeaders.SIZE.value]?.toLongOrNull() ?: 0
            enc.type = "audio/mpeg"
            enc.url = file[CSVHeaders.MEDIA.value]
            entry.enclosures = listOf<SyndEnclosure>(enc)
            feed.entries.add(entry)
        }
    } catch (e: Exception){
        throw Exception("Error building RSS\n $e")
    }
    return feed
}

fun writeRSS(feed: SyndFeed,file: String) {
    try {
        val writer = FileWriter(file)
        SyndFeedOutput().output(feed, writer)
        writer.close()
    } catch (e: Exception) {
        throw Exception("Error while writing file $file\n $e")
    }
}
