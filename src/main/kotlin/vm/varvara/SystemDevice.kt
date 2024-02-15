package net.walksanator.uxnkt.vm.varvara

import net.walksanator.uxnkt.vm.*
import kotlin.experimental.and
import kotlin.experimental.or

class SystemDevice(val uxn: Uxn) : Device() {
    var lastExpansion: Short = 0x000
    var metadataLocation: Short = 0x000// probally later could parse this location for enabling bonus VM features perhaps

    var sysRed: Short = 0x000F
    var sysBlue: Short = 0xFF77.toShort()
    var sysGreen: Short = 0x7777

    var state: Byte = 0x0 //literally exitcode

    var runExpansionFunction = false

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
            0x02 -> lastExpansion = lastExpansion.replaceUpperByte(byte)
            0x03 -> {
                lastExpansion = lastExpansion.replaceLowerByte(byte)
                runExpansionFunction = true
            }
            0x04 -> uxn.ws.sp = byte.toShort()
            0x05 -> uxn.rs.sp = byte.toShort()
            0x06 -> metadataLocation = metadataLocation.replaceUpperByte(byte)
            0x07 -> metadataLocation = metadataLocation.replaceLowerByte(byte)
            0x08 -> sysRed = sysRed.replaceUpperByte(byte)
            0x09 -> sysRed = sysRed.replaceLowerByte(byte)
            0x0A -> sysGreen = sysGreen.replaceUpperByte(byte)
            0x0B -> sysGreen = sysGreen.replaceLowerByte(byte)
            0x0C -> sysBlue = sysBlue.replaceUpperByte(byte)
            0x0D -> sysBlue = sysBlue.replaceLowerByte(byte)
            0x0E -> super.writeByte(address, byte)/*TODO: print debugging info*/
            0x0F -> state = byte
        }
    }

    override fun postTick(uxn: Uxn) {
        if (runExpansionFunction) {
            val instr = uxn.ram[lastExpansion]
            when (instr.toUByte().toInt()) {
                0x01 -> /*copy*/ {
                    val length = uxn.ram[lastExpansion + 1].msbToShort(uxn.ram[lastExpansion + 2]).unsign()
                    val src_bank = uxn.ram[lastExpansion + 3].msbToShort(uxn.ram[lastExpansion + 4]).unsign()
                    val src_addr = uxn.ram[lastExpansion + 5].msbToShort(uxn.ram[lastExpansion + 6]).unsign()
                    val dst_bank = uxn.ram[lastExpansion + 7].msbToShort(uxn.ram[lastExpansion + 8]).unsign()
                    val dst_addr = uxn.ram[lastExpansion + 9].msbToShort(uxn.ram[lastExpansion + 10]).unsign()
                    if (src_bank != 0 || dst_bank != 0) {
                        return //the default impl does not have any other banks
                    }
                    for (i in 0..< length) {
                        uxn.ram[dst_addr + i] = uxn.ram[src_addr + i]
                    }
                }
                else -> {}
            }

            runExpansionFunction = false
        }
    }

}