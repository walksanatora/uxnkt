package net.walksanator.uxnkt.vm

interface IDevice {
    /**
     * takes a byte (but only reads the lower 4 bits)
     * extenders may ignore this implementation in the case of multi-device accesses
     * @param address the address. default impl only cares about lowest 4 bits
     * @return the byte read
     */
    fun readByte(address: Byte): Byte

    /**
     * writes a single byte into devices memory
     * @param address the address to read. default impl only cares about lowest 4 bits
     * @param byte the value to write into that address
     */
    fun writeByte(address: Byte, byte: Byte)

    /**
     * this is where you perform actions like enque key events based on state
     */
    fun postTick(uxn: Uxn)
}