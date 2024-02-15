package net.walksanator.uxnkt.vm

import Computer

open class Device {

    /**
     * A internal byte buffer for if the value is not intercepted
     */
    @Suppress("MemberVisibilityCanBePrivate")
    protected val backingBuffer = ByteArray(8)

    /**
     * takes a byte (but only reads the lower 4 bits)
     * extenders may ignore this implementation in the case of multi-device accesses
     * @param address the address. default impl only cares about lowest 4 bits
     * @return the byte read
     */
    open fun readByte(address: Byte): Byte = backingBuffer[address%8]

    /**
     * writes a single byte into devices memory
     * @param address the address to read. default impl only cares about lowest 4 bits
     * @param byte the value to write into that address
     */
    open fun writeByte(address: Byte, byte: Byte) {backingBuffer[address%8] = byte}

    /**
     * this is where you perform actions like enque key events based on state
     */
    open fun postTick(uxn: Uxn) {/*NOP*/}
}