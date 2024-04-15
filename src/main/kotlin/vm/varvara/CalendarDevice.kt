package net.walksanator.uxnkt.vm.varvara

import net.walksanator.uxnkt.vm.Device
import net.walksanator.uxnkt.vm.toBytes
import java.util.*

class CalendarDevice : Device() {
    val cal = Calendar.getInstance()

    override fun readByte(address: Byte): Byte {
         return when (address.toInt()) {
                0x00 -> cal.get(Calendar.YEAR).toShort().toBytes().first
                0x01 -> cal.get(Calendar.YEAR).toShort().toBytes().second
                0x02 -> cal.get(Calendar.MONTH).toShort().toByte()
                0x03 -> cal.get(Calendar.DAY_OF_MONTH).toByte()
                0x04 -> cal.get(Calendar.HOUR).toByte()
                0x05 -> cal.get(Calendar.MINUTE).toByte()
                0x06 -> cal.get(Calendar.SECOND).toByte()
                0x07 -> cal.get(Calendar.DAY_OF_WEEK).toByte()
                0x08 -> cal.get(Calendar.DAY_OF_YEAR).shr(8).toByte()
                0x09 -> cal.get(Calendar.DAY_OF_YEAR).toByte()
                0x0A -> if (cal.timeZone.inDaylightTime(Date())) {1} else {0}
                else -> super.readByte(address)
         }
    }
}