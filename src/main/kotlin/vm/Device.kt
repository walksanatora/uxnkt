abstract class Device {
    val backingBuffer = ByteArray(8)// a backing buffer for unused ports

    abstract fun readByte(address: Byte): Byte
    abstract fun writeByte(address: Byte, byte: Byte)
    abstract fun readShort(address: Byte): Short
    abstract fun writeShort(address: Byte, short: Short)

    /**
     * this is where you perform actions like enque key events based on state
     */
    fun postTick(com: Computer) {/*NOP*/}
}