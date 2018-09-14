import com.beust.klaxon.*
import java.io.File
import java.io.BufferedReader

import org.redundent.kotlin.xml.xml


fun main(args: Array<String>) {
 //   val bufferedReader: BufferedReader = File("saints.json").bufferedReader()
 //   val inputString = bufferedReader.use { it.readText() }
    val json:JsonArray<JsonObject> = Parser().parse("saints.json") as JsonArray<JsonObject>

    for (mp3 in json) {
        println("test");
    }
    val people = xml("people") {
        xmlns = "http://example.com/people"
        "person" {
            attribute("id", 1)
            "firstName" {
                -"John"
            }
            "lastName" {
                -"Doe"
            }
            "phone" {
                -"555-555-5555"
            }
        }
    }

    val asString = people.toString()}
