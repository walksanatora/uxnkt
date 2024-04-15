package net.walksanator.uxnkt.vm

/**
 * A device implementation, classes should ideally extend this. but in cases where they cant just extend the interface
 * when the memory value is not used the address should be treated as a normal RAM value (store the values written)
 */
open class Device : IDevice {

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
    override fun readByte(address: Byte): Byte = backingBuffer[address%8]

    /**
     * writes a single byte into devices memory
     * @param address the address to read. default impl only cares about lowest 4 bits
     * @param byte the value to write into that address
     */
    override fun writeByte(address: Byte, byte: Byte) {backingBuffer[address%8] = byte}

    /**
     * this is where you perform actions like enque key events based on state
     */
    override fun postTick(uxn: Uxn) {/*NOP*/}
}