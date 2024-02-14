package net.walksanator.uxnkt.vm

import Computer

open class Device {
    @Suppress("MemberVisibilityCanBePrivate")
    protected val backingBuffer = ByteArray(8)// a backing buffer for unused ports

    open fun readByte(address: Byte): Byte = backingBuffer[address%9]
    open fun writeByte(address: Byte, byte: Byte) {backingBuffer[address%9] = byte}

    /**
     * this is where you perform actions like enque key events based on state
     */
    fun postTick(com: Computer) {/*NOP*/}
}