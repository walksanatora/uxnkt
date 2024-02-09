package net.walksanator.uxnkt.vm.varvara

import Device
import net.walksanator.uxnkt.vm.Uxn
import net.walksanator.uxnkt.vm.msbToShort
import net.walksanator.uxnkt.vm.toBytes
import kotlin.experimental.and
import kotlin.experimental.or

class SystemDevice(val uxn: Uxn) : Device() {
    var lastExpansion: Short = 0x000
    var metadataLocation: Short = 0x000// probally later could parse this location for enabling bonus VM features perhaps

    var sysRed: Short = 0x000F
    var sysBlue: Short = 0xFF77.toShort()
    var sysGreen: Short = 0x7777

    var state: Byte = 0x0 //literally exitcode

    override fun readByte(address: Byte): Byte {
        return when (address.toInt()) {
            0x00, 0x01, 0x0E -> backingBuffer[address.toInt()]
            0x02 -> readShort(address).toBytes().first                 //EXPANSION upper
            0x03 -> readShort((address-1).toByte()).toBytes().second //EXPANSION lower
            0x04 -> uxn.ws.sp // working stack stack pointer
            0x05 -> uxn.rs.sp // return stack stack pointer
            0x06 -> readShort(address).toBytes().first                  //METADATA upper
            0x07 -> readShort((address-1).toByte()).toBytes().second    //METADATA lower
            0x08 -> readShort(address).toBytes().first                  //Red upper
            0x09 -> readShort((address-1).toByte()).toBytes().second    //Red lower
            0x0A -> readShort(address).toBytes().first                  //Green upper
            0x0B -> readShort((address-1).toByte()).toBytes().second    //Green lower
            0x0C -> readShort(address).toBytes().first                  //Blue upper
            0x0D -> readShort((address-1).toByte()).toBytes().second    //Blue lower
            0x0F -> state
            else -> {throw IllegalStateException("Unreachable Branch in net.walksanator.uxnkt.vm.varvara.SystemDevice#readByte")}
        }
    }

    override fun writeByte(address: Byte, byte: Byte) {
        when (address.toInt()) {
            0x00, 0x01 -> backingBuffer[address.toInt()] = byte
            0x02 -> lastExpansion = lastExpansion.and(0x00FF).or(byte.toShort())
            0x03 -> lastExpansion = lastExpansion.and(0xFF0).or(byte.toShort().rotateLeft(8))
            0x04 -> uxn.ws.sp = byte
            0x05 -> uxn.rs.sp = byte
            0x06 -> metadataLocation = metadataLocation.and(0x00FF).or(byte.toShort())
            0x07 -> metadataLocation = metadataLocation.and(0xFF0).or(byte.toShort().rotateLeft(8))
            0x08 -> sysRed = sysRed.and(0x00FF).or(byte.toShort())
            0x09 -> sysRed = sysRed.and(0xFF0).or(byte.toShort().rotateLeft(8))
            0x0A -> sysGreen = sysGreen.and(0x00FF).or(byte.toShort())
            0x0B -> sysGreen = sysGreen.and(0xFF0).or(byte.toShort().rotateLeft(8))
            0x0C -> sysBlue = sysBlue.and(0x00FF).or(byte.toShort())
            0x0D -> sysBlue = sysBlue.and(0xFF0).or(byte.toShort().rotateLeft(8))
            0x0E -> {/*TODO: print debugging info*/}
            0x0F -> state = byte
        }
    }

    override fun readShort(address: Byte): Short {
        return when(address.toInt()) {
            0x00 -> readByte(address).msbToShort(readByte(0x01))
            0x01 -> readByte(address).msbToShort(readByte(0x02))
            0x02 -> lastExpansion
            0x03 -> readByte(address).msbToShort(readByte(0x04))
            0x04 -> readByte(address).msbToShort(readByte(0x05))
            0x05 -> readByte(address).msbToShort(readByte(0x06))
            0x06 -> metadataLocation
            0x07 -> readByte(address).msbToShort(readByte(0x04))
            0x08 -> sysRed
            0x09 -> readByte(address).msbToShort(readByte(0x0A))
            0x0A -> sysGreen
            0x0B -> readByte(address).msbToShort(readByte(0x0C))
            0x0C -> sysBlue
            0x0D -> readByte(address).msbToShort(readByte(0x0E))
            0x0E -> readByte(address).msbToShort(readByte(0x0F))
            0x0F -> readByte(address).msbToShort(0x0000)
            else -> {throw IllegalStateException("Unreachable arm in net.walksanator.uxnkt.vm.varvara.SystemDevice#readShort")}
        }
    }

    override fun writeShort(address: Byte, short: Short) {
        when(address.toInt()) {
            0x00 -> {/*nop*/}
            0x01 -> writeByte(0x02, short.toBytes().second)
            0x02 -> lastExpansion = short
            0x03 -> { val part = short.toBytes(); writeByte(address,part.first);writeByte(0x04,part.second) }
            0x04 -> { val part = short.toBytes(); writeByte(address,part.first);writeByte(0x05,part.second) }
            0x05 -> { val part = short.toBytes(); writeByte(address,part.first);writeByte(0x06,part.second) }
            0x06 -> metadataLocation = short
            0x07 -> { val part = short.toBytes(); writeByte(address,part.first);writeByte(0x08,part.second) }
            0x08 -> sysRed = short
            0x09 -> { val part = short.toBytes(); writeByte(address,part.first);writeByte(0x0A,part.second) }
            0x0A -> sysGreen = short
            0x0B -> { val part = short.toBytes(); writeByte(address,part.first);writeByte(0x0C,part.second) }
            0x0C -> sysBlue = short
            0x0D -> { val part = short.toBytes(); writeByte(address,part.first);writeByte(0x0E,part.second) }
            0x0E -> { val part = short.toBytes(); writeByte(address,part.first);writeByte(0x0F,part.second) }
            0x0F -> writeByte(address,short.toBytes().first)
            else -> {throw IllegalStateException("Unreachable arm in net.walksanator.uxnkt.vm.varvara.SystemDevice#readShort")}
        }
    }
}