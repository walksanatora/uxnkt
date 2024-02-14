package net.walksanator.uxnkt.vm.varvara

import net.walksanator.uxnkt.vm.Device
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
            0x00, 0x01, 0x0E -> super.readByte(address)
            0x02 -> lastExpansion.toBytes().first                 //EXPANSION upper
            0x03 -> lastExpansion.toBytes().second //EXPANSION lower
            0x04 -> uxn.ws.sp.toByte() // working stack stack pointer
            0x05 -> uxn.rs.sp.toByte() // return stack stack pointer
            0x06 -> metadataLocation.toBytes().first                  //METADATA upper
            0x07 -> metadataLocation.toBytes().second    //METADATA lower
            0x08 -> sysRed.toBytes().first                  //Red upper
            0x09 -> sysRed.toBytes().second    //Red lower
            0x0A -> sysGreen.toBytes().first                  //Green upper
            0x0B -> sysGreen.toBytes().second    //Green lower
            0x0C -> sysBlue.toBytes().first                  //Blue upper
            0x0D -> sysBlue.toBytes().second    //Blue lower
            0x0F -> state
            else -> {throw IllegalStateException("Unreachable Branch in net.walksanator.uxnkt.vm.varvara.SystemDevice#readByte")}
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun writeByte(address: Byte, byte: Byte) {
        //println("writing %s to %s system device".format(byte.toHexString(),address.toHexString()))
        when (address.toInt()) {
            0x00, 0x01 -> super.writeByte(address, byte)
            0x02 -> lastExpansion = lastExpansion.and(0x00FF).or(byte.toShort())
            0x03 -> lastExpansion = lastExpansion.and(0xFF0).or(byte.toShort().rotateLeft(8))
            0x04 -> uxn.ws.sp = byte.toShort()
            0x05 -> uxn.rs.sp = byte.toShort()
            0x06 -> metadataLocation = metadataLocation.and(0x00FF).or(byte.toShort())
            0x07 -> metadataLocation = metadataLocation.and(0xFF0).or(byte.toShort().rotateLeft(8))
            0x08 -> sysRed = sysRed.and(0x00FF).or(byte.toShort())
            0x09 -> sysRed = sysRed.and(0xFF0).or(byte.toShort().rotateLeft(8))
            0x0A -> sysGreen = sysGreen.and(0x00FF).or(byte.toShort())
            0x0B -> sysGreen = sysGreen.and(0xFF0).or(byte.toShort().rotateLeft(8))
            0x0C -> sysBlue = sysBlue.and(0x00FF).or(byte.toShort())
            0x0D -> sysBlue = sysBlue.and(0xFF0).or(byte.toShort().rotateLeft(8))
            0x0E -> super.writeByte(address, byte)/*TODO: print debugging info*/
            0x0F -> state = byte
        }
    }

}