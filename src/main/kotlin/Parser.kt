import java.math.BigDecimal
import utils.fp.Result
import java.math.BigInteger

// 借鉴了 kotlin-bencode （https://github.com/ciferkey/kotlin-bencode）
sealed class Bencode

data class IntLiteral(val value: BigInteger): Bencode()
data class ByteStringLiteral(val content: String): Bencode()
data class ListLiteral(val of: MutableList<Bencode>): Bencode()
data class DictionaryLiteral(val of: MutableMap<Bencode, Bencode>): Bencode()

private const val INTEGER_MARKER = 'i'
private const val LIST_MARKER = 'l'
private const val DICT_MARKER = 'd'
private const val ENDING_MARKER = 'e'
private const val SEPARATOR = ':'

class Decoder(val input: String) {

    val iter = input.iterator()

    fun decode(): Result<Bencode> {
        if (iter.hasNext()) {
            val marker = iter.peek()
            return when (marker) {
                in '0'..'9' -> decodeString()
                INTEGER_MARKER -> decodeInteger()
                LIST_MARKER -> decodeList()
                DICT_MARKER -> decodeDict()
                else -> Result.failure("Unknown identifier $marker")
            }
        }
        return Result.failure("Nothing to decode")
    }

    private fun decodeString(): Result<Bencode> {
        return iter.readWhile { it.isDigit() }
            .flatMap { length ->
                iter.consume(SEPARATOR).flatMap {
                    iter.readFor(length.toInt()).map {
                        ByteStringLiteral(it)
                    }
                }
            }
    }

    private fun decodeInteger(): Result<Bencode> {
        return iter.consume(INTEGER_MARKER).flatMap {
            iter.readUntil(ENDING_MARKER).flatMap {
                Result.of {
                    IntLiteral(it.toBigInteger())
                }
            }
        }
    }

    private fun decodeList(): Result<Bencode> {
        return iter.consume(LIST_MARKER).flatMap {
            Result.of {
                val items = mutableListOf<Bencode>()
                while (!iter.consume(ENDING_MARKER).getOrElse(false)) {
                    decode().map {
                        items.add(it)
                    }
                }
                ListLiteral(items)
            }.mapFailure("Failed decoding list. No TERMINATOR $ENDING_MARKER")
        }
    }

    private fun decodeDict(): Result<Bencode> {
        return iter.consume(DICT_MARKER).flatMap {
            Result.of {
                val items = mutableMapOf<Bencode, Bencode>()
                while (!iter.consume(ENDING_MARKER).getOrElse(false)) {
                    decode().fanout {
                        decode()
                    }.forEach({
                        items[it.first] = it.second
                    }, {}, {})
                }
                DictionaryLiteral(items)
            }.mapFailure("Failed decoding list. No TERMINATOR $ENDING_MARKER")
        }
    }
}

interface CharIterator {
    fun hasNext(): Boolean
    fun nextChar(): Char
    fun peek(): Char
}

fun String.iterator(): CharIterator = object: CharIterator {
    private var index = 0

    override fun hasNext(): Boolean = index < length

    override fun nextChar(): Char {
        return get(index++)
    }

    override fun peek(): Char {
        return get(index)
    }
}

fun CharIterator.readWhile(pred: (Char) -> Boolean): Result<String> {
    return Result.of {
        buildString {
            while (pred(this@readWhile.peek())) {
                append(this@readWhile.nextChar())
            }
        }
    }.mapFailure("Failed reading based on given predicate")
}
fun CharIterator.readUntil(terminator: Char): Result<String> {
    return this.readWhile { it != terminator }.flatMap { string ->
        this.consume(terminator).map { _ ->
            string
        }
    }.mapFailure("Failed reading until '$terminator'")
}
fun CharIterator.consume(char: Char): Result<Boolean> {
    return if (this.peek() == char) {
        this.nextChar()
        Result(true)
    } else {
        Result.failure("Could not consume '$char'")
    }
}
fun CharIterator.readFor(size: Int): Result<String> {
    return Result.of {
        val sub = mutableListOf<Char>()
        for (i in 0 until size) {
            sub.add(this.nextChar())
        }
        sub.joinToString("")
    }.mapFailure("Was not able to read $size characters")
}

inline fun <V, U> Result<V>.fanout(crossinline other: () -> Result<U>): Result<Pair<V, U>> {
    return flatMap { outer ->
        other().map { outer to it }
    }
}

fun main() {
    val message = "d8:announce41:http://bttracker.debian.org:6969/announce7:comment35:" +
            "\"Debian CD from cdimage.debian.org\"13:creation datei1573903810e9:httpse" +
            "edsl145:https://cdimage.debian.org/cdimage/release/10.2.0//srv/cdbuilder." +
            "debian.org/dst/deb-cd/weekly-builds/amd64/iso-cd/debian-10.2.0-amd64-neti" +
            "nst.iso145:https://cdimage.debian.org/cdimage/archive/10.2.0//srv/cdbuild" +
            "er.debian.org/dst/deb-cd/weekly-builds/amd64/iso-cd/debian-10.2.0-amd64-n" +
            "etinst.isoe4:infod6:lengthi351272960e4:name31:debian-10.2.0-amd64-netinst" +
            ".iso12:piece lengthi262144e6:pieces8:xxxxxxxxee"

    val result = Decoder(message).decode()

    println("Simulation completed")
}