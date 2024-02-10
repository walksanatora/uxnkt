package net.walksanator.uxnkt.vm.varvara

import Device
import net.walksanator.uxnkt.vm.msbToShort
import net.walksanator.uxnkt.vm.toBytes
import kotlin.experimental.and
import kotlin.experimental.or

class ConsoleDevice : Device() {
    var callbackVector: Short = 0x0000
    var read: Byte = 0x00
    var type: Byte = 0x00
    var write: Byte  = 0x00
    var error: Byte = 0x00
    override fun readByte(address: Byte): Byte {
        return when (address.toInt()) {
            0x00 -> readShort(0x00).toBytes().first  //CONSOLE callback vector upper half
            0x01 -> readShort(0x00).toBytes().second //CONSOLE callback vector lower half
            0x02 -> read
            0x07 -> type
            0x08 -> write
            0x09 -> error
            0x03, 0x04, 0x05, 0x06, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F -> backingBuffer[address.toInt()]
            else -> throw IllegalStateException("Unreachable Branch in net.walksanator.uxnkt.vm.varvara.ConsoleDevice#readByte")
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun writeByte(address: Byte, byte: Byte) {
        //println("writted to console: %s addr: %s".format(byte.toHexString(),address.toHexString()))
        when(address.toInt()) {
            0x00 -> callbackVector = callbackVector.and(0x00FF).or(byte.toShort()) //CONSOLE
            0x01 -> callbackVector = callbackVector.and(0xFF0).or(byte.toShort().rotateLeft(8)) //CONSOLE
            0x02 -> read = byte
            0x07 -> type = byte
            0x08 -> {
                write = byte
                print(Char(byte.toInt()))
            }
            0x09 -> error = byte
            0x03, 0x04, 0x05, 0x06, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F -> backingBuffer[address.toInt()] = byte
        }
    }

    override fun readShort(address: Byte): Short {
       return when (address.toInt()) {
           0x00 -> callbackVector
           0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F ->
               readByte(address).msbToShort(readByte((address+1).toByte()))
           else -> throw IllegalStateException("Unreachable Branch in net.walksanator.uxnkt.vm.varvara.ConsoleDevice#readShort")
       }
    }

    override fun writeShort(address: Byte, short: Short) {
        val pair = short.toBytes()
        when(address.toInt()) {
            0x00 -> callbackVector = short
            0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F -> {
                writeByte(address,pair.first)
                writeByte((address+1).toByte(), pair.second)
            }
        }
    }
}