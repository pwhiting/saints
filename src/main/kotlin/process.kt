import com.opencsv.CSVReaderHeaderAware
import com.rometools.rome.feed.synd.SyndEnclosure
import com.rometools.rome.feed.synd.SyndEnclosureImpl
import com.rometools.rome.feed.synd.SyndEntryImpl
import com.rometools.rome.feed.synd.SyndFeed
import com.rometools.rome.feed.synd.SyndFeedImpl
import com.rometools.rome.io.SyndFeedOutput
import java.io.BufferedReader
import java.io.FileReader
import java.io.FileWriter
import java.time.LocalDateTime.now
import java.time.ZoneOffset
import java.util.Date

fun main(args: Array<String>) {
    if (args.size != 2) {
        println("include csv with audio files as first argument and output file as second")
        return
    }

    val csvFilename = args[0]
    val outputFilename = args[1]

    writeRSS(buildRSS(readCsvFile(csvFilename)), outputFilename)
}

enum class CSVHeaders(val value: String) {
    ID("id"),
    TITLE("title"),
    MEDIA("media_url"),
    SIZE("file_size"),
    DURATION("duration"),
    VOICE("voice")
}

data class RssAudioItem(val title: String, val size: Long, val url: String, val type: String = "audio/mpeg")

/**
 * Read the CSV file
 * @param csvFilename CSV Filename with RSS audio data, first row must contain column names
 * @return List of RssAudioItems
 */
fun readCsvFile(csvFilename: String): List<RssAudioItem> {
    val rssAudioItemList = mutableListOf<RssAudioItem>()

    // Read csv file
    CSVReaderHeaderAware(BufferedReader(FileReader(csvFilename))).use { csvReader ->
        // Read all the CSV columns names and validate
        val csvColumnNameMap = csvReader.readMap()
        CSVHeaders.values().forEach { key ->
            if (!csvColumnNameMap.containsKey(key.value)) {
                throw IllegalArgumentException("unexpected column ${key.value} in csv file")
            }
        }

        // Read all the row data an place into rssAudioItemList
        var csvRowDataMap: Map<String, String>? = csvReader.readMap()
        while (csvRowDataMap != null) {
            // skip any audio with a voice type of female
            val voiceType = csvRowDataMap[CSVHeaders.VOICE.value] ?: ""
            if (voiceType != "female") {
                val rssAudioItem = RssAudioItem(
                        title = csvRowDataMap[CSVHeaders.TITLE.value] ?: "",
                        size = csvRowDataMap[CSVHeaders.SIZE.value]?.toLongOrNull() ?: 0,
                        url = csvRowDataMap[CSVHeaders.MEDIA.value] ?: ""
                )

                rssAudioItemList.add(rssAudioItem)
            }

            // read next row
            csvRowDataMap = csvReader.readMap()
        }
    }

    return rssAudioItemList
}

/**
 * Build RSS Feed
 * @param rssAudioItemList List of RSS audio items
 * @return RSS feed
 */
fun buildRSS(rssAudioItemList: List<RssAudioItem>): SyndFeedImpl {
    val feed = SyndFeedImpl().apply {
        feedType = "rss_2.0"
        title = "Saints"
        link = "http://pete.dugg.in/s"
        description = "Saints podcast"
    }

    var dateCounter = now().toEpochSecond(ZoneOffset.UTC)
    rssAudioItemList.forEach { rssAudioItem ->
        val enc = SyndEnclosureImpl().apply {
            length = rssAudioItem.size
            type = rssAudioItem.type
            url = rssAudioItem.url
        }

        val entry = SyndEntryImpl().apply {
            title = rssAudioItem.title
            link = rssAudioItem.url
            publishedDate = Date(1000L * dateCounter++)
            enclosures = listOf<SyndEnclosure>(enc)
        }

        feed.entries.add(entry)
    }

    return feed
}

/**
 * Write SyncFeed to output file
 * @param feed SyndFeed
 * @param outputFilename name of file to be written
 */
fun writeRSS(feed: SyndFeed, outputFilename: String) {
    FileWriter(outputFilename).use { writer ->
        SyndFeedOutput().output(feed, writer)
    }
}
