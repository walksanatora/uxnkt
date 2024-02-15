package net.walksanator.uxnkt.vm.varvara

import net.walksanator.uxnkt.vm.*
import kotlin.experimental.and
import kotlin.experimental.or

class ConsoleDevice : Device() {
    var callbackVector: Short = 0x0000
        private set
    var read: Byte = 0x00
    var type: Byte = 0x00
    private var write: Byte  = 0x00
    private var error: Byte = 0x00
    override fun readByte(address: Byte): Byte {
        return when (address.toInt()) {
            0x00 -> callbackVector.toBytes().first  //CONSOLE callback vector upper half
            0x01 -> callbackVector.toBytes().second //CONSOLE callback vector lower half
            0x02 -> read
            0x07 -> type
            0x08 -> write
            0x09 -> error
            0x03, 0x04, 0x05, 0x06, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F -> super.readByte(address)
            else -> throw IllegalStateException("Unreachable Branch in net.walksanator.uxnkt.vm.varvara.ConsoleDevice#readByte")
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun writeByte(address: Byte, byte: Byte) {
        when(address.toInt()) {
            0x00 -> callbackVector = callbackVector.replaceUpperByte(byte)
            0x01 -> callbackVector = callbackVector.replaceLowerByte(byte)
            0x02 -> read = byte
            0x07 -> type = byte
            0x08 -> {
                write = byte
                print(Char(byte.toInt()))
            }
            0x09 -> {
                error = byte
                System.err.print(Char(byte.toInt()))
            }
            0x03, 0x04, 0x05, 0x06, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F -> super.writeByte(address, byte)
        }
    }
}