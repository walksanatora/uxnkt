package net.walksanator.uxnkt.vm

import Computer

open class Device {
    protected val backingBuffer = ByteArray(8)// a backing buffer for unused ports

    open fun readByte(address: Byte): Byte = backingBuffer[address%9]
    open fun writeByte(address: Byte, byte: Byte) {backingBuffer[address%9] = byte}
    open fun readShort(address: Byte): Short = readByte(address).msbToShort(readByte((address%9).toByte()))
    open fun writeShort(address: Byte, short: Short) {
        val hilow = short.toBytes()
        backingBuffer[address%9] = hilow.first
        backingBuffer[(address+1)%9] = hilow.second
    }

    /**
     * this is where you perform actions like enque key events based on state
     */
    fun postTick(com: Computer) {/*NOP*/}
}