package net.walksanator.uxnkt.vm
import com.google.gson.GsonBuilder
import com.google.gson.annotations.Expose
import java.nio.file.Path
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.experimental.xor
import kotlin.math.absoluteValue

class WrappingByteArray(private val wrapped: ByteArray) {
     val size: Int
        get() = wrapped.size
    constructor(size: Int) : this(ByteArray(size))
    operator fun get(idx: Int) = wrapped[idx.toUShort().toInt() % wrapped.size]
    operator fun set(idx: Int, v: Byte) {wrapped[idx.toUShort().toInt() % wrapped.size] = v}
    operator fun get(idx: Short) = wrapped[idx.toUShort().toInt() % wrapped.size]
    operator fun set(idx: Short, v: Byte) {wrapped[idx.toUShort().toInt() % wrapped.size] = v}
    fun inner(): ByteArray = wrapped
}
class Stack : Cloneable {
    @Expose var s = ByteArray(0x100) //stack
    @Expose var sp: Short = 0 //stack pointer
        set(v) {
            val mod = v % 256
            val real = if (mod < 0) {
                256+mod
            } else {
                mod
            }
            field = real.toShort()
        }
    var spr: Short = 0

     public override fun clone(): Stack {
         val new = Stack()
         new.s = s.clone()
         new.sp = sp
         new.spr = spr
         new.warp()
         return new
     }

    fun warp() {
         sp = (sp+spr).toShort()
         spr = 0
    }

    fun popByte(sim: Boolean): Byte {
        if(sim){spr = (spr + 1).toShort()}
        sp = (sp - 1).toShort()
        return s[sp%256]
    }
    fun popShort(sim: Boolean): Short {
        val lsb = popByte(sim)
        val msb = popByte(sim)
        return msb.msbToShort(lsb)
    }

    fun pushByte(byte: Byte) {
        warp()
        s[(sp % 256).absoluteValue] = byte
        sp = (sp + 1).toShort()
    }
    fun pushShort(short: Short) {
        val bytes = short.toBytes()
        pushByte(bytes.first)
        pushByte(bytes.second)
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString(): String {
        val builder = StringBuilder()
        builder.append("[")
        for (i in 0..<sp) {
            builder.append(s[i].toHexString())
            if (i != ( sp - 1)) {
                builder.append(',')
            }
        }
        builder.append("]")
        return builder.toString()
    }
}

data class ExecutionState(
    @Expose val pc: Short,
    @Expose val ws: Stack,
    @Expose val rs: Stack,
    @Expose val ram: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ExecutionState

        if (pc != other.pc) return false
        if (ws != other.ws) return false
        if (rs != other.rs) return false
        if (!ram.contentEquals(other.ram)) return false

        return true
    }
    override fun hashCode(): Int {
        var result = pc.toInt()
        result = 31 * result + ws.hashCode()
        result = 31 * result + rs.hashCode()
        result = 31 * result + ram.contentHashCode()
        return result
    }

}

@OptIn(ExperimentalStdlibApi::class)
class Uxn(val ram: WrappingByteArray) {
    constructor(ba: ByteArray): this(WrappingByteArray(ba))
    var pc: Short = 0x100
    val ws = Stack() // Working net.walksanator.uxnkt.vm.Stack
    val rs = Stack() // Run net.walksanator.uxnkt.vm.Stack
    var devices = Array<Device>(16) { Device() }

    val executionLog: MutableList<ExecutionState> = mutableListOf()

    init {
        if (ram.size < 0x10000) {
            throw IllegalArgumentException("ram must be atleast 0x10000 bytes. it is currently %s".format(ram.size.toHexString()))
        }
    }

    fun dumpFrames(file: Path) {
        val gson = GsonBuilder().excludeFieldsWithoutExposeAnnotation().create()
        for ((i,frame) in executionLog.withIndex()) {
            val dump = file.resolve("frame_%s.json".format(i)).toFile()
            dump.delete()
            dump.writeText(
                gson.toJson(frame
                )
            )
        }
    }

    fun captureFrame() {
        executionLog.add(
            ExecutionState(
                pc,
                ws.clone(),rs.clone(),ram.inner().clone()
            )
        )
    }

    fun getDevice(addr: Byte): Device = devices[addr.and(0xF0.toByte()).rotateRight(4).toInt()]

    /**
     * steps VM execution by one instruction
     *
     * @Return if the VM reached a BRK instruction
     */
    fun step(): Boolean {
        if (System.getProperty("debug") != null) {
            captureFrame()
        }

        val instruction = ram[pc]
        pc = (pc + 1).toShort()

        //println("instr: %s".format(instruction.toUxnInstrStr()))

        val keepMode: Boolean = instruction.and(0b10000000.toByte()).toInt() != 0
        val returnMode: Boolean = instruction.and(0b01000000).toInt() != 0
        val immediate: Boolean = instruction.and(0b00011111).toInt() == 0
        val shortMode: Boolean = instruction.and(0b00100000).toInt() != 0

        val stack = if (returnMode) { rs } else { ws }
        stack.warp()

        val maskedInstruction = if (immediate) {
            instruction //immediate instr
        } else {
            instruction.and(0x1f) // as-opcode
        }.toInt().and(0xFF)
        when (maskedInstruction) {
            //#region immediate
            // NOP
            0x00 -> return true
            // JCI
            0x20 -> {
                val msb = ram[pc.toInt()]
                val addr = msb.msbToShort(ram[pc+1])
                pc = (pc + 2).toShort()
                if (stack.popByte(false).toInt() != 0) {
                    pc = (pc + addr).toShort()
                }
            }
            // JMI
            0x40 -> {
                val msb = ram[pc.toInt()]
                val addr = msb.msbToShort(ram[pc+1])
                pc = (pc + 2 + addr).toShort()
            }
            // JSI
            0x60 -> {
                val msb = ram[pc.toInt()]
                val addr = msb.msbToShort(ram[pc+1])
                pc = (pc + 2).toShort()
                rs.pushShort(pc)
                pc = (pc + addr).toShort()
            }
            // LIT
            0x80 -> {
                stack.pushByte(ram[pc.toInt()])
                pc = (pc + 1).toShort()
            }
            // LIT2
            0xA0 -> {
                val msb = ram[pc.toInt()]
                val res = msb.msbToShort(ram[pc+1])
                pc = (pc + 2).toShort()
                stack.pushShort(res)
            }
            // LITr
            0xC0 -> {
                stack.pushByte(ram[pc.toInt()])
                pc = (pc + 1).toShort()
            }
            // LIT2r
            0xE0 -> {
                val msb = ram[pc.toInt()]
                val res = msb.msbToShort(ram[pc+1])
                pc = (pc + 2).toShort()
                stack.pushShort(res)
            }
            //#endregion
            //#region basic stack
            // INC a -- a+1
            0x01 -> {
                if (shortMode) {
                    val a = stack.popShort(keepMode)
                    stack.pushShort((a+1).toShort())
                } else {
                    val a = stack.popByte(keepMode)
                    stack.pushByte((a+1).toByte())
                }
            }
            // POP a --
            0x02 -> {
                if (shortMode) {
                    stack.popShort(keepMode)
                } else {
                    stack.popByte(keepMode)
                }
            }
            // NIP a b -- a
            0x03 -> {
                if (shortMode) {
                    val b = stack.popShort(keepMode)
                    stack.popShort(keepMode) // a
                    stack.pushShort(b)
                } else {
                    val b = stack.popByte(keepMode)
                    stack.popByte(keepMode) // a
                    stack.pushByte(b)
                }
            }
            // SWP a b -- b a
            0x04 -> {
                if (shortMode) {
                    val b = stack.popShort(keepMode)
                    val a = stack.popShort(keepMode)
                    stack.pushShort(b)
                    stack.pushShort(a)
                } else {
                    val b = stack.popByte(keepMode)
                    val a = stack.popByte(keepMode)
                    stack.pushByte(b)
                    stack.pushByte(a)
                }
            }
            // ROT a b c -- b c a
            0x05 -> {
                if (shortMode) {
                    val c = stack.popShort(keepMode)
                    val b = stack.popShort(keepMode)
                    val a = stack.popShort(keepMode)
                    stack.pushShort(b)
                    stack.pushShort(c)
                    stack.pushShort(a)
                } else {
                    val c = stack.popByte(keepMode)
                    val b = stack.popByte(keepMode)
                    val a = stack.popByte(keepMode)
                    stack.pushByte(b)
                    stack.pushByte(c)
                    stack.pushByte(a)
                }
            }
            // DUP a -- a a
            0x06 -> {
                if (shortMode) {
                    val a =  stack.popShort(keepMode)
                    stack.pushShort(a)
                    stack.pushShort(a)
                } else {
                    val a = stack.popByte(keepMode)
                    stack.pushByte(a)
                    stack.pushByte(a)
                }
            }
            // OVR a b -- a b a
            0x07 -> {
                if (shortMode) {
                    val b = stack.popShort(keepMode)
                    val a = stack.popShort(keepMode)
                    stack.pushShort(a)
                    stack.pushShort(b)
                    stack.pushShort(a)
                } else {
                    val b = stack.popByte(keepMode)
                    val a = stack.popByte(keepMode)
                    stack.pushByte(a)
                    stack.pushByte(b)
                    stack.pushByte(a)
                }
            }
            //#endregion
            //#region comparisons
            // EQU a b -- bool8
            0x08 -> {
                if (shortMode) {
                    val b = stack.popShort(keepMode)
                    val a = stack.popShort(keepMode)
                    if (a==b) {
                        stack.pushByte(1)
                    } else {
                        stack.pushByte(0)
                    }
                } else {
                    val b = stack.popByte(keepMode)
                    val a = stack.popByte(keepMode)
                    if (a==b) {
                        stack.pushByte(1)
                    } else {
                        stack.pushByte(0)
                    }
                }
            }
            // NEQ a b -- bool8
            0x09 -> {
                if (shortMode) {
                    val b = stack.popShort(keepMode)
                    val a = stack.popShort(keepMode)
                    if (a!=b) {
                        stack.pushByte(1)
                    } else {
                        stack.pushByte(0)
                    }
                } else {
                    val b = stack.popByte(keepMode)
                    val a = stack.popByte(keepMode)
                    if (a!=b) {
                        stack.pushByte(1)
                    } else {
                        stack.pushByte(0)
                    }
                }
            }
            // GTH a b -- bool8
            0x0A -> {
                if (shortMode) {
                    val b = stack.popShort(keepMode).toUShort()
                    val a = stack.popShort(keepMode).toUShort()
                    if (a>b) {
                        stack.pushByte(1)
                    } else {
                        stack.pushByte(0)
                    }
                } else {
                    val b = stack.popByte(keepMode).toUByte()
                    val a = stack.popByte(keepMode).toUByte()
                    if (a>b) {
                        stack.pushByte(1)
                    } else {
                        stack.pushByte(0)
                    }
                }
            }
            // LTH a b -- bool8
            0x0B -> {
                if (shortMode) {
                    val b = stack.popShort(keepMode).toUShort()
                    val a = stack.popShort(keepMode).toUShort()
                    if (a<b) {
                        stack.pushByte(1)
                    } else {
                        stack.pushByte(0)
                    }
                } else {
                    val b = stack.popByte(keepMode).toUByte()
                    val a = stack.popByte(keepMode).toUByte()
                    if (a<b) {
                        stack.pushByte(1)
                    } else {
                        stack.pushByte(0)
                    }
                }
            }
            //#endregion
            //#region JMPs
            // JMP addr --
            0x0C -> {
                pc = if (shortMode) {
                    stack.popShort(keepMode)
                } else {
                    (pc + stack.popByte(keepMode)).toShort()
                }
            }
            // JCN cond8 addr --
            0x0D -> {
                val tgt = if (shortMode) {
                    stack.popShort(keepMode)
                } else {
                    (pc + stack.popByte(keepMode)).toShort()
                }
                val cond = stack.popByte(keepMode)
                if (cond.toInt() != 0) {
                    pc = tgt
                }
            }
            // JSR addr --
            0x0E -> {
                val other = if (returnMode) {ws} else {rs}
                other.pushShort(pc)
                pc = if (shortMode) {
                    stack.popShort(keepMode)
                } else {
                    (pc + stack.popByte(keepMode)).toShort()
                }
            }
            // STH a --
            0x0F -> {
                val other = if (returnMode) {ws} else {rs}
                if (shortMode) {
                    val a = stack.popShort(keepMode)
                    other.pushShort(a)
                } else {
                    val a = stack.popByte(keepMode)
                    other.pushByte(a)
                }
            }
            //#endregion
            //#region memory
            // LDZ addr8 -- value
            0x10 -> {
                val addr = stack.popByte(keepMode)
                if (shortMode) {
                    stack.pushShort(
                        ram[addr.toInt()].msbToShort(ram[(addr + 1)%256])
                    )
                } else {
                    stack.pushByte(ram[addr.toInt()])
                }
            }
            // STZ value addr8 --
            0x11 -> {
                val addr = stack.popByte(keepMode).toInt()
                if (shortMode) {
                    val value = stack.popShort(keepMode)
                    val lr = value.toBytes()
                    ram[addr] = lr.first
                    ram[(addr + 1)%256] = lr.second
                } else {
                    val value = stack.popByte(keepMode)
                    ram[addr] = value
                }
            }
            // LDR addr8 -- value
            0x12 -> {
                val addr = stack.popByte(keepMode).toInt()
                if (shortMode) {
                    stack.pushShort(
                        ram[pc + addr].msbToShort(
                            ram[(pc + addr + 1)]
                        )
                    )
                } else {
                    stack.pushByte(ram[pc + addr])
                }
            }
            // STR value addr8 --
            0x13 -> {
                val addr = stack.popByte(keepMode)
                if (shortMode) {
                    val value = stack.popShort(keepMode)
                    val lr = value.toBytes()
                    ram[pc + addr] = lr.first
                    ram[(pc + addr + 1)%0x10000] = lr.second
                } else {
                    val value = stack.popByte(keepMode)
                    ram[pc + addr] = value
                }
            }
            // LDA addr16 -- value
            0x14 -> {
                val addr = stack.popShort(keepMode).toInt()
                if (shortMode) {
                    stack.pushShort(
                        ram[addr].msbToShort(
                            ram[addr + 1]
                        )
                    )
                } else {
                    stack.pushByte(ram[addr])
                }
            }
            // STA value addr16 --
            0x15 -> {
                val addr = stack.popShort(keepMode)
                if (shortMode) {
                    val value = stack.popShort(keepMode)
                    val lr = value.toBytes()
                    ram[addr] = lr.first
                    ram[addr + 1] = lr.second
                } else {
                    val value = stack.popByte(keepMode)
                    ram[addr] = value
                }
            }
            // DEI device8 -- value
            0x16 -> {
                val dev = stack.popByte(keepMode)
                val port = dev.and(0x0F)
                val device = getDevice(dev)
                if (shortMode) {
                    val ldevid = (dev+1).toByte()
                    val lport = ldevid.and(0x0F)
                    val ldev = getDevice(ldevid)
                    val value = device.readByte(port).msbToShort(ldev.readByte(lport))
                    stack.pushShort(value)
                } else {
                    val value = device.readByte(port)
                    stack.pushByte(value)
                }
            }
            // DEO value device8 --
            0x17 -> {
                val dev = stack.popByte(keepMode)
                val port = dev.and(0x0F)
                val device = getDevice(dev)
                if (shortMode) {
                    val value = stack.popShort(keepMode).toBytes()
                    val ldevid = (dev+1).toByte()
                    val lport = ldevid.and(0x0F)
                    val ldev = getDevice(ldevid)
                    device.writeByte(port,value.first)
                    ldev.writeByte(lport,value.second)
                } else {
                    val value = stack.popByte(keepMode)
                    device.writeByte(port,value)
                }

            }
            //#endregion
            //#region math
            // ADD a b -- a+b
            0x18 -> {
                if (shortMode) {
                    val b = stack.popShort(keepMode)
                    val a = stack.popShort(keepMode)
//                    println("ADD2 %s + %s = %s".format(
//                        a.toHexString(),
//                        b.toHexString(),
//                        (a+b).toShort().toHexString())
//                    )
                    stack.pushShort((a+b).toShort())
                } else {
                    val b = stack.popByte(keepMode)
                    val a = stack.popByte(keepMode)
                    stack.pushByte((a+b).toByte())
                }
            }
            // SUB a b  -- a-b
            0x19 -> {
                if (shortMode) {
                    val b = stack.popShort(keepMode)
                    val a = stack.popShort(keepMode)
                    stack.pushShort((a-b).toShort())
                } else {
                    val b = stack.popByte(keepMode)
                    val a = stack.popByte(keepMode)
                    stack.pushByte((a-b).toByte())
                }
            }
            // MUL a b -- a*b
            0x1A -> {
                if (shortMode) {
                    val b = stack.popShort(keepMode)
                    val a = stack.popShort(keepMode)
                    stack.pushShort((a*b).toShort())
                } else {
                    val b = stack.popByte(keepMode)
                    val a = stack.popByte(keepMode)
                    stack.pushByte((a*b).toByte())
                }
            }
            // DIV a b -- a/b
            0x1B -> {
                if (shortMode) {
                    val b = stack.popShort(keepMode).toUShort()
                    val a = stack.popShort(keepMode).toUShort()
                    if (b.toInt() == 0) {
                        stack.pushShort(0)
                    } else {
                        stack.pushShort((a/b).toShort())
                    }
                } else {
                    val b = stack.popByte(keepMode).toUByte()
                    val a = stack.popByte(keepMode).toUByte()
                    if (b.toInt() == 0) {
                        stack.pushByte(0)
                    } else {
                        stack.pushByte((a/b).toByte())
                    }
                }
            }
            // AND a b -- a&b
            0x1C -> {
                if (shortMode) {
                    val b = stack.popShort(keepMode)
                    val a = stack.popShort(keepMode)
                    stack.pushShort(a.and(b))
                } else {
                    val b = stack.popByte(keepMode)
                    val a = stack.popByte(keepMode)
                    stack.pushByte(a.and(b))
                }
            }
            // OR a b -- a||b
            0x1D -> {
                if (shortMode) {
                    val b = stack.popShort(keepMode)
                    val a = stack.popShort(keepMode)
                    stack.pushShort(a.or(b))
                } else {
                    val b = stack.popByte(keepMode)
                    val a = stack.popByte(keepMode)
                    stack.pushByte(a.or(b))
                }
            }
            // XOR a b -- a xor b
            0x1E -> {
                if (shortMode) {
                    val b = stack.popShort(keepMode)
                    val a = stack.popShort(keepMode)
                    stack.pushShort(a.xor(b))
                } else {
                    val b = stack.popByte(keepMode)
                    val a = stack.popByte(keepMode)
                    stack.pushByte(a.xor(b))
                }
            }
            // SFT a shift8 -- c
            0x1F -> {
                val shift = stack.popByte(keepMode)
                val left = shift.and(0xF0.toByte()).rotateRight(4).toInt()
                val right = shift.and(0x0F).toInt()
                //println("SFT shift8: %s %s/%s".format(shift.toHexString(),left.toHexString(),right.toHexString()))
                if (shortMode) {
                    val a = stack.popShort(keepMode).toUShort().toInt()
                    val c = (a shr right).toShort().toInt() shl left
                    //println("SFT2 a: %s c: %s".format(a.toShort().toHexString(),c.toShort().toHexString()))
                    stack.pushShort(c.toShort())
                } else {
                    val a = stack.popByte(keepMode).toUByte().toInt()
                    val c = (a shr right).toByte().toInt() shl left
                    //println("SFT a: %s c: %s".format(a.toByte().toHexString(),c.toByte().toHexString()))
                    stack.pushByte(c.toByte())
                }
            }
            else -> {throw IllegalStateException("Unreachable branch hit on instr %s masked: %s".format(instruction.toHexString(),maskedInstruction.toHexString()))}
            //#endregion
        }

        devices.onEach { it.postTick(this) }

        return false
    }
}

fun Short.toBytes(): Pair<Byte,Byte> {
    return Pair(
        this.rotateRight(8).toByte(),
        this.and(0xff).toByte()
    )
}
fun Byte.msbToShort(lowerByte: Byte): Short {
    // Convert the bytes to integers and combine them using bitwise operations
    return ((this.toInt() and 0xFF shl 8) or (lowerByte.toInt() and 0xFF)).toShort()
}

fun Short.replaceLowerByte(newLowerByte: Byte): Short {
    return (this.toInt() and 0xFF00 or (newLowerByte.toInt() and 0xFF)).toShort()
}
fun Short.replaceUpperByte(newUpperByte: Byte): Short {
    return (this.toInt() and 0xFF or (newUpperByte.toInt() shl 8)).toShort()
}

fun Short.unsign(): Int = this.toUShort().toInt()

val InstrNameMap: List<String> = listOf("BRK","INC", "POP","NIP", "SWP","ROT", "DUP","OVR", "EQU","NEQ", "GTH","LTH","JMP","JCN","JSR","STH", "LDZ","STZ", "LDR","STR", "LDA","STA", "DEI","DEO", "ADD","SUB", "MUL","DIV", "AND","ORA", "EOR","SFT", "JCI","INC2", "POP2","NIP2", "SWP2","ROT2", "DUP2","OVR2", "EQU2","NEQ2", "GTH2","LTH2", "JMP2","JCN2", "JSR2","STH2", "LDZ2","STZ2", "LDR2","STR2", "LDA2","STA2", "DEI2","DEO2", "ADD2","SUB2", "MUL2","DIV2", "AND2","ORA2", "EOR2","SFT2", "JMI","INCr", "POPr","NIPr", "SWPr","ROTr", "DUPr","OVRr", "EQUr","NEQr", "GTHr","LTHr", "JMPr","JCNr", "JSRr","STHr", "LDZr","STZr", "LDRr","STRr", "LDAr","STAr", "DEIr","DEOr", "ADDr","SUBr", "MULr","DIVr", "ANDr","ORAr", "EORr","SFTr", "JSI","INC2r", "POP2r","NIP2r", "SWP2r","ROT2r", "DUP2r","OVR2r", "EQU2r","NEQ2r", "GTH2r","LTH2r", "JMP2r","JCN2r", "JSR2r","STH2r", "LDZ2r","STZ2r", "LDR2r","STR2r", "LDA2r","STA2r", "DEI2r","DEO2r", "ADD2r","SUB2r", "MUL2r","DIV2r", "AND2r","ORA2r", "EOR2r","SFT2r", "LIT","INCk", "POPk","NIPk", "SWPk","ROTk", "DUPk","OVRk", "EQUk","NEQk", "GTHk","LTHk", "JMPk","JCNk", "JSRk","STHk", "LDZk","STZk", "LDRk","STRk", "LDAk","STAk", "DEIk","DEOk", "ADDk","SUBk", "MULk","DIVk", "ANDk","ORAk", "EORk","SFTk","LIT2", "INC2k","POP2k", "NIP2k","SWP2k", "ROT2k","DUP2k", "OVR2k","EQU2k", "NEQ2k","GTH2k", "LTH2k","JMP2k", "JCN2k","JSR2k", "STH2k","LDZ2k", "STZ2k","LDR2k", "STR2k","LDA2k", "STA2k","DEI2k", "DEO2k","ADD2k", "SUB2k","MUL2k", "DIV2k","AND2k", "ORA2k","EOR2k", "SFT2k","LITr", "INCkr","POPkr", "NIPkr","SWPkr", "ROTkr","DUPkr", "OVRkr","EQUkr", "NEQkr","GTHkr", "LTHkr","JMPkr", "JCNkr","JSRkr", "STHkr","LDZkr", "STZkr","LDRkr", "STRkr","LDAkr", "STAkr","DEIkr", "DEOkr","ADDkr", "SUBkr","MULkr", "DIVkr","ANDkr", "ORAkr","EORkr", "SFTkr","LIT2r", "INC2kr","POP2kr", "NIP2kr","SWP2kr", "ROT2kr","DUP2kr", "OVR2kr","EQU2kr", "NEQ2kr","GTH2kr", "LTH2kr","JMP2kr", "JCN2kr","JSR2kr", "STH2kr","LDZ2kr", "STZ2kr","LDR2kr", "STR2kr","LDA2kr", "STA2kr","DEI2kr", "DEO2kr","ADD2kr", "SUB2kr","MUL2kr", "DIV2kr","AND2kr", "ORA2kr","EOR2kr", "SFT2kr")
fun Byte.toUxnInstrStr(): String = InstrNameMap[this.toUByte().toInt()]
