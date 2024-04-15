package net.walksanator.uxnkt.vm.varvara

import net.walksanator.uxnkt.vm.Device
import net.walksanator.uxnkt.vm.toBytes
import java.util.Calendar

class CalendarDevice : Device() {
    val cal = Calendar.getInstance()

    override fun readByte(address: Byte): Byte {
         return when (address.toInt()) {
                0x00 -> cal.get(Calendar.YEAR).toShort().toBytes().first
                0x01 -> cal.get(Calendar.YEAR).toShort().toBytes().second
                0x02 -> cal.get(Calendar.MONTH).toShort().toByte()
                0x03 -> cal.get(Calendar.DAY_OF_MONTH).toByte()
                0x04 -> cal.get(Calendar.HOUR).toByte()
                else -> super.readByte(address)
         }
    }
}