package net.walksanator.uxnkt.vm
import Device
import net.walksanator.uxnkt.vm.util.Either
import java.util.*
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.experimental.xor

enum class UxnError {
    Underflow, Overflow, ZeroDiv
}

class Stack {
    val s = ByteArray(0x100) //stack
    var sp: Byte = 0 //stack pointer

    fun updateStackPointer(operandBytes: Byte, resultBytes: Byte, keepMode: Boolean): Optional<UxnError> {
        if (operandBytes > sp) {
            return Optional.of(UxnError.Underflow)
        }

        val newSp: Int = (if (keepMode) {
            sp
        } else {
            // The subtraction of operandBytes does not need to be checked, as we have already
            // checked that operandBytes <= sp.
            (sp - operandBytes).toByte()
        } + resultBytes)
        if (newSp > Byte.MAX_VALUE) return Optional.of(UxnError.Overflow)
        sp = newSp.toByte()

        return Optional.empty()
    }

    fun getByte(offset: Byte): Byte = s[offset.toInt()]
    fun getShort(offset: Byte): Short {
        val msb = getByte((offset + 1).toByte())
        val lsb = getByte(offset)
        return (msb.toShort().rotateRight(8).or(lsb.toShort()))
    }

    fun setByte(offset: Byte, byte: Byte) {
        s[(sp - offset)] = byte
    }

    fun setShort(offset: Byte, short: Short) {
        val msb: Byte = short.rotateLeft(8).toByte()
        val lsb: Byte = short.and(0xff).toByte()
        setByte((offset + 1).toByte(), msb)
        setByte(offset, lsb)
    }
}

@OptIn(ExperimentalStdlibApi::class)
class Uxn(val ram: ByteArray) {
    var pc: Short = 0x100
    val ws = Stack() // Working net.walksanator.uxnkt.vm.Stack
    val rs = Stack() // Run net.walksanator.uxnkt.vm.Stack
    var devices = Array<Optional<Device>>(16) { Optional.empty() }

    init {
        if (ram.size < 0xFFFF) {
            throw IllegalArgumentException("ram must be atleast 0xFFFF bytes. it is currently %s".format(ram.size.toHexString()))
        }
    }

    /**
     * steps VM execution by one instruction
     *
     * @Return left means whether execution is still needed. right means the code crashed
     */
    fun step(): Either<Boolean, UxnError> {
        val instruction = ram[pc.toInt()]
        pc = (pc + 1).toShort()

        val keepMode: Boolean = instruction.and(128.toByte()).toInt() != 0
        val returnMode: Boolean = instruction.and(64).toInt() != 0
        val immediate: Boolean = instruction.and(31).toInt() == 0

        val stack = if (returnMode) {
            rs
        } else {
            ws
        }

        val maskedInstruction = if (immediate) {
            instruction
        } else {
            instruction.and(0b00111111)
        }

        val t = stack.getByte(1)
        val n = stack.getByte(2)
        val l = stack.getByte(3)
        val h2 = stack.getShort(2)
        val t2 = stack.getShort(1)
        val n2 = stack.getShort(3)
        val l2 = stack.getShort(5)
        print("%s ".format(instruction.toHexString()))
        when (maskedInstruction) {
            // BRK
            0x00.b -> {
                return Either.left(true)
            }
            // JCI
            0x20.b -> {
                val msb = ram[pc.toInt()]
                val lsb = ram[pc + 1]
                pc = (pc + 2).toShort()
                val stackret = stack.updateStackPointer(1, 0, false)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                if (t.toInt() != 0) {
                    pc = (pc + msb.msbToShort(lsb)).toShort()
                }
            }
            // JMI
            0x40.b -> {
                val msb = ram[pc.toInt()]
                val lsb = ram[pc + 1]
                pc = (pc + 2).toShort()
                pc = (pc + msb.msbToShort(lsb)).toShort()
            }
            // JSI
            0x60.b -> {
                val msb = ram[pc.toInt()]
                val lsb = ram[pc + 1]
                pc = (pc + 2).toShort()
                val stackret = rs.updateStackPointer(0, 2, false)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                rs.setShort(1, pc)
                pc = (pc + msb.msbToShort(lsb)).toShort()
            }
            // LIT
            0x80.b -> {
                val stackret = stack.updateStackPointer(0, 1, true)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(1, ram[pc.toInt()])
                pc = (pc + 1).toShort()
            }
            // LIT2
            0xa0.b -> {
                val stackret = stack.updateStackPointer(0, 2, true)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                val msb = ram[pc.toInt()]
                val lsb = ram[pc + 1]
                stack.setShort(1, msb.msbToShort(lsb))
                pc = (pc + 2).toShort()
            }
            // LITr
            0xc0.b -> {
                val stackret = stack.updateStackPointer(0, 1, true)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(1, ram[pc.toInt()])
                pc = (pc + 1).toShort()
            }
            // LIT2r
            0xe0.b -> {
                val stackret = stack.updateStackPointer(0, 2, true)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                val msb = ram[pc.toInt()]
                val lsb = ram[pc + 1]
                stack.setShort(1, msb.msbToShort(lsb))
                pc = (pc + 2).toShort()
            }
            // END of immediate instrs
            // INC(2)
            0x01.b -> {
                val stackret = stack.updateStackPointer(1, 1, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(1, (t + 1).toByte())
            }

            0x21.b -> {
                val stackret = stack.updateStackPointer(2, 2, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setShort(1, (t2 + 1).toShort() )
            }
            // POP(2)
            0x02.b -> {
                val stackret = stack.updateStackPointer(1, 0, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
            }
            
            0x22.b -> {
                val stackret = stack.updateStackPointer(2, 0, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
            }
            // NIP(2)
            0x03.b -> {
                val stackret = stack.updateStackPointer(2, 1, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(1, t)
            }

            0x23.b -> {
                val stackret = stack.updateStackPointer(4, 2, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setShort(1, t2)
            }
            // SWP(2)
            0x04.b -> {
                val stackret = stack.updateStackPointer(2, 2, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(1, n)
                stack.setByte(2, t)
            }

            0x24.b -> {
                val stackret = stack.updateStackPointer(4, 4, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setShort(1, n2)
                stack.setShort(3, t2)
            }
            // ROT(2)
            0x05.b -> {
                val stackret = stack.updateStackPointer(3, 3, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(1, l)
                stack.setByte(2, t)
                stack.setByte(3, n)
            }

            0x25.b -> {
                val stackret = stack.updateStackPointer(6, 6, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setShort(1, l2)
                stack.setShort(3, t2)
                stack.setShort(5, n2)
            }

            // DUP(2)
            0x06.b -> {
                val stackret = stack.updateStackPointer(1, 2, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(1, t)
                stack.setByte(2, t)
            }

            0x26.b -> {
                val stackret = stack.updateStackPointer(2, 4, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setShort(1, t2)
                stack.setShort(3, t2)
            }

            // OVR(2)
            0x07.b -> {
                val stackret = stack.updateStackPointer(2, 3, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(1, n)
                stack.setByte(2, t)
                stack.setByte(3, n)
            }

            0x27.b -> {
                val stackret = stack.updateStackPointer(4, 6, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setShort(1, n2)
                stack.setShort(3, t2)
                stack.setShort(5, n2)
            }
            // EQU(2)
            0x08.b -> {
                val stackret = stack.updateStackPointer(2, 1, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(1, (n == t).into())
            }

            0x28.b -> {
                val stackret = stack.updateStackPointer(4, 1, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(1, (n2 == t2).into())
            }
            // NEQ(2)
            0x09.b -> {
                val stackret = stack.updateStackPointer(2, 1, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(1, (n != t).into())
            }

            0x29.b -> {
                val stackret = stack.updateStackPointer(4, 1, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(1, (n2 != t2).into())
            }
            // GTH(2)
            0x0a.b -> {
                val stackret = stack.updateStackPointer(2, 1, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(1, (n > t).into())
            }

            0x2a.b -> {
                val stackret = stack.updateStackPointer(4, 1, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(1, (n2 > t2).into())
            }

            // LTH(2)
            0x0b.b -> {
                val stackret = stack.updateStackPointer(2, 1, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(1, (n < t).into())
            }

            0x2b.b -> {
                val stackret = stack.updateStackPointer(4, 1, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(1, (n2 < t2).into())
            }
            // JMP(2)
            0x0c.b -> {
                val stackret = stack.updateStackPointer(1, 0, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                pc = (pc.toInt() + t).toShort() //TODO: WATCH THIS idk if I did it right
            }

            0x2c.b -> {
                val stackret = stack.updateStackPointer(2, 0, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                pc = t2
            }
            // JCN(2)
            0x0d.b -> {
                val stackret = stack.updateStackPointer(2, 0, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                if (n.toInt() != 0) {
                    pc = (pc.toInt() + t).toShort()
                }
            }
            0x2d.b -> {
                val stackret = stack.updateStackPointer(3, 0, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                if (l.toInt() != 0) {
                    pc = t2
                }
            }
            // JSR(2)
            0x0e.b -> {
                val stackret = stack.updateStackPointer(1, 0, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                val stackret2 = rs.updateStackPointer(0, 2, false)
                if (stackret2.isPresent) {
                    return Either.right(stackret2.get())
                }

                rs.setShort(1, pc)
                pc = (pc.toInt() + t).toShort() //TODO: WATCH I checked on kotlin playground and this looks *okay* but i am still unsure
            }

            0x2e.b -> {
                val stackret = stack.updateStackPointer(2, 0, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                val stackret2 = rs.updateStackPointer(0, 2, false)
                if (stackret2.isPresent) {
                    return Either.right(stackret2.get())
                }
                rs.setShort(1, pc)
                pc = t2
            }
            // STH(2)
            0x0f.b -> {
                val stackret = stack.updateStackPointer(1, 0, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                val otherStack = if (returnMode) { ws } else { rs }
                val stackret2 = otherStack.updateStackPointer(0, 1, false)
                if (stackret2.isPresent) {
                    return Either.right(stackret2.get())
                }
                otherStack.setByte(1, t)
            }

            0x2f.b -> {
                val stackret = stack.updateStackPointer(2, 0, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                val otherStack = if (returnMode) { ws } else { rs }
                val stackret2 = otherStack.updateStackPointer(0, 2, false)
                if (stackret2.isPresent) {
                    return Either.right(stackret2.get())
                }
                otherStack.setShort(1, t2)
            }
            // LDZ(2)
            0x10.b -> {
                val stackret = stack.updateStackPointer(1, 1, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(1, ram[t.toInt()])
            }

            0x30.b -> {
                val stackret = stack.updateStackPointer(1, 2, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(1, ram[(t+1)])
                stack.setByte(2, ram[t.toInt()])
            }
            // STZ(2)
            0x11.b -> {
                val stackret = stack.updateStackPointer(2, 0, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                ram[t.toInt()] = n
            }

            0x31.b -> {
                val stackret = stack.updateStackPointer(3, 0, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                ram[(t+1)] = n
                ram[t.toInt()] = l
            }

            // LDR(2)
            0x12.b -> {
                val stackret = stack.updateStackPointer(1, 1, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(
                    1,
                    ram[pc.toInt() + t],
                )
            }

            0x32.b -> {
                val stackret = stack.updateStackPointer(1, 2, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(
                    1,
                    ram[pc.toInt() + t + 1]
                )
                stack.setByte(
                    2,
                    ram[pc.toInt() + t],
                )
            }

            // STR(2)
            0x13.b -> {
                val stackret = stack.updateStackPointer(2, 0, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                ram[pc.toInt() + t] = n
            }

            0x33.b -> {
                val stackret = stack.updateStackPointer(3, 0, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                ram[pc.toInt() + t] = l
                ram[pc.toInt() + t +1] = n
            }

            // LDA(2)
            0x14.b -> {
                val stackret = stack.updateStackPointer(2, 1, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(1, ram[t2.toInt()])
            }

            0x34.b -> {
                val stackret = stack.updateStackPointer(2, 2, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(1, ram[t2.toInt() +1 ])
                stack.setByte(2, ram[t2.toInt()])
            }

            // STA(2)
            0x15.b -> {
                val stackret = stack.updateStackPointer(3, 0, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                ram[t2.toInt()] = l
            }

            0x35.b -> {
                val stackret = stack.updateStackPointer(4, 0, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }

                val value = n2.toBytes()
                ram[t2.toInt()] = value.first
                ram[t2.toInt() + 1] = value.second
            }

            // DEI(2)
            0x16.b -> {
                val stackret = stack.updateStackPointer(1, 1, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                val device = devices[t.and(0xf0.toByte()).rotateRight(4).toInt()]
                if (device.isPresent) {
                    stack.setByte(
                        1,
                        device.get().readByte(t.and(0x0f))
                    )
                } else {
                    stack.setByte(1,0x00)
                }
            }

            0x36.b -> {
                val stackret = stack.updateStackPointer(1, 2, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                val device = devices[t.and(0xf0.toByte()).rotateRight(4).toInt()]
                if (device.isPresent) {
                    stack.setShort(1, device.get().readShort(t.and(0x0f)))
                } else {
                    stack.setShort(1, 0x0000)
                }
            }
            // DEO(2)
            0x17.b -> {
                val stackret = stack.updateStackPointer(2, 0, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                val stackret2 = stack.updateStackPointer(1, 1, keepMode)
                if (stackret2.isPresent) {
                    return Either.right(stackret2.get())
                }
                val device = devices[t.and(0xf0.toByte()).rotateRight(4).toInt()]
                if (device.isPresent) {
                    device.get().writeByte(t.and(0x0f), n)
                }
            }

            0x37.b -> {
                val stackret = stack.updateStackPointer(3, 0, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                val device = devices[t.and(0xf0.toByte()).rotateRight(4).toInt()]
                if (device.isPresent) {
                    device.get().writeShort(t.and(0x0f), h2)
                }
            }
            // ADD(2)
            0x18.b -> {
                val stackret = stack.updateStackPointer(2, 1, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(1, (n+t).toByte())
            }

            0x38.b -> {
                val stackret = stack.updateStackPointer(4, 2, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setShort(1, (n2+t2).toShort())
            }
            // SUB(2)
            0x19.b -> {
                val stackret = stack.updateStackPointer(2, 1, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(1, (n +t).toByte())
            }

            0x39.b -> {
                val stackret = stack.updateStackPointer(4, 2, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setShort(1, (n2-t2).toShort())
            }
            // MUL(2)
            0x1a.b -> {
                val stackret = stack.updateStackPointer(2, 1, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(1, (n*t).toByte())
            }

            0x3a.b -> {
                val stackret = stack.updateStackPointer(4, 2, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setShort(1, (n2*t2).toShort())
            }
            // DIV(2)
            0x1b.b -> {
                if (t.toInt() == 0) {return Either.right(UxnError.ZeroDiv)}
                val quotient = n / t
                val stackret = stack.updateStackPointer(2, 1, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(1, quotient.toByte())
            }

            0x3b.b -> {
                if (t2.toInt() == 0) {return Either.right(UxnError.ZeroDiv)}
                val quotient = n2 / t2
                val stackret = stack.updateStackPointer(4, 2, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setShort(1, quotient.toShort())
            }
            // AND(2)
            0x1c.b -> {
                val stackret = stack.updateStackPointer(2, 1, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(1, n.and(t))
            }

            0x3c.b -> {
                val stackret = stack.updateStackPointer(4, 2, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setShort(1, n2.and(t2))
            }

            // ORA(2)
            0x1d.b -> {
                val stackret = stack.updateStackPointer(2, 1, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(1, n.or(t))
            }

            0x3d.b -> {
                val stackret = stack.updateStackPointer(4, 2, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setShort(1, n2.or(t2))
            }

            // EOR(2)
            0x1e.b -> {
                val stackret = stack.updateStackPointer(2, 1, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(1, n.xor(t))
            }

            0x3e.b -> {
                val stackret = stack.updateStackPointer(4, 2, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setShort(1, n2.xor(t2))
            }

            // SFT(2)
            0x1f.b -> {
                val stackret = stack.updateStackPointer(2, 1, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setByte(1, 
                    n.rotateRight(t.and(0x0f).toInt())
                        .rotateLeft(t.and(0xf0.toByte()).rotateRight(4).toInt())
                )
            }

            0x3f.b -> {
                val stackret = stack.updateStackPointer(3, 2, keepMode)
                if (stackret.isPresent) {
                    return Either.right(stackret.get())
                }
                stack.setShort(1, //TODO: CHECK THIS it may be implemented wrong
                        h2.rotateRight(t.and(0x0f).toInt())
                            .rotateLeft(t.and(0xf0.toByte()).rotateRight(4).toInt())
                )
            }

            // Impossible.
            else -> {
                throw IllegalStateException(
                    "Reached the unreachable! instruction %s masked %s".format(
                        instruction.toHexString(),
                        maskedInstruction.toHexString()
                    )
                )
            }
        }

        return Either.left(false)
    }
}

fun Short.toBytes(): Pair<Byte,Byte> {
    return Pair(
        this.rotateRight(8).toByte(),
        this.and(0xff).toByte()
    )
}
fun Byte.msbToShort(lsb: Byte): Short = (this.toShort().rotateRight(8).or(lsb.toShort()))
fun Boolean.into(): Byte {
    return (if (this) {
        1
    } else {
        0
    }).toByte()
}
inline val Int.b get() = this.toByte()