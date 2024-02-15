package net.walksanator.uxnkt.vm.varvara

import net.walksanator.uxnkt.vm.*
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolute

class FileDevice(var root: Path) : Device() {
    var holder: File? = null

    constructor() : this(Path(".").absolute())

    var success: Short = 0x0000
    var stat: Short = 0x0000
    var delete: Byte = 0x00
    var append: Byte = 0x00
    var nameptr: Short = 0x0000 // pointer to a null-terminated string for path
    var length: Short = 0x0000
    var read: Short = 0x0000
    var write: Short = 0x0000

    var open_file = false
    var delete_file = false
    var read_file = false
    var write_file = false

    var file_bytes: MutableList<Byte>? = null
    var first_write = true

    override fun writeByte(address: Byte, byte: Byte) {
        when(address.toInt()) {
            0x00, 0x01 -> super.writeByte(address, byte)
            0x02 -> success = success.replaceUpperByte(byte)
            0x03 -> success = success.replaceLowerByte(byte)
            0x04 -> stat = stat.replaceUpperByte(byte)
            0x05 -> stat = stat.replaceLowerByte(byte)
            0x06 -> {
                delete = byte
                delete_file = true
            }
            0x07 -> append = byte
            0x08 -> nameptr = nameptr.replaceUpperByte(byte)
            0x09 -> {
                nameptr = nameptr.replaceLowerByte(byte)
                open_file = true
            }
            0x0A -> length = length.replaceUpperByte(byte)
            0x0B -> length = length.replaceLowerByte(byte)
            0x0C -> read = read.replaceUpperByte(byte)
            0x0D -> {
                read = read.replaceLowerByte(byte)
                read_file = true
            }
            0x0E -> write = write.replaceUpperByte(byte)
            0x0F -> {
                write = write.replaceLowerByte(byte)
                write_file = true
            }
        }
    }

    override fun readByte(address: Byte): Byte {
        return when (address.toInt()) {
            0x00, 0x01 -> super.readByte(address)
            0x02 -> success.toBytes().first
            0x03 -> success.toBytes().second
            0x04 -> stat.toBytes().first
            0x05 -> stat.toBytes().second
            0x06 -> delete
            0x07 -> append
            0x08 -> nameptr.toBytes().first
            0x09 -> nameptr.toBytes().second
            0x0A -> length.toBytes().first
            0x0B -> length.toBytes().second
            0x0C -> read.toBytes().first
            0x0D -> read.toBytes().second
            0x0E -> write.toBytes().first
            0x0F -> write.toBytes().second
            else -> throw IllegalStateException("Unreachable Branch in net.walksanator.uxnkt.vm.varvara.FileDevice#readByte")
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun postTick(uxn: Uxn) {
        if (open_file) {
            val sbuf = StringBuilder()
            var offset = 0
            while (true) {
                val byte = uxn.ram[nameptr + offset]
                if (byte.toInt() == 0) {break}//we found the null terminator
                sbuf.append(
                    byte.toInt().toChar()
                )
                offset += 1
            }
            val file = File(sbuf.toString())
            file_bytes = null
            if (!file.toPath().absolute().startsWith(root.parent)) {// file is not within our root so it is illegal
                success = 0x0000
                holder = null
                length = 0x0000
            } else {
                success = 0x0001
                holder = file
                length = if (holder!!.length() > UShort.MAX_VALUE.toLong()) {
                    0xFFFF.toShort()
                } else { holder!!.length().toShort() }
                if (holder!!.isFile) {
                    file_bytes = holder!!.readBytes().toMutableList()
                }
                first_write = true
            }
            open_file = false
        }
        if (delete_file) {
            holder?.delete()
            delete_file = false
        }
        if (read_file) {
            var remainingBytesInBuf = length.unsign()
            //println("reading file: %s bufsize".format(remainingBytesInBuf))
            if (holder != null) {
                if (holder!!.isDirectory) {
                    success = 0x0000
                    for (file in holder!!.listFiles()!!) {
                        if (remainingBytesInBuf <= 0) {break}
                        val fmt = "%s %s\n".format(
                            if (file.isDirectory) {"----"} else { if(file.length() > 0xFFFF) {"????"} else {file.length().toHexString()}},
                            file.name
                        ).toByteArray(Charset.forName("IBM437"))
                        if (fmt.size > remainingBytesInBuf) {
                            break
                        }
                        remainingBytesInBuf -= fmt.size
                        for ((idx, char) in fmt.withIndex()) {
                            uxn.ram[read + idx] = char
                            success = (success.unsign() + 1).toShort()
                        }
                    }
                } else {
                    if (file_bytes != null) {
                        var idx = 0
                        val bytes = file_bytes!!
                        while (remainingBytesInBuf > 0) {
                            if (bytes.size == 0) { break }
                            uxn.ram[read + idx] = bytes.removeFirst()
                            idx += 1
                            remainingBytesInBuf -= 1
                        }
                        success = idx.toShort()
                    }
                }
            } else {
                success = 0x0000
            }
            //println("read %s bytes".format(success))
            read_file = false
        }
        if (write_file) {
            if (holder?.isFile == true) {
                if (append.toInt() != 0) {
                    val ba = mutableListOf<Byte>()
                    for (i in 0..<length.unsign()) {
                        ba.add(
                            uxn.ram[write + i]
                        )
                    }
                    holder!!.appendBytes(ba.toByteArray())
                } else {
                    val ba = mutableListOf<Byte>()
                    for (i in 0..<length.unsign()) {
                        ba.add(
                            uxn.ram[write + i]
                        )
                    }
                    if (first_write) {
                        holder!!.writeBytes(ba.toByteArray())
                        first_write = false
                    } else {
                        holder!!.appendBytes(ba.toByteArray())
                    }
                }
                success = length
            }
            write_file = false
        }
    }
}