package net.walksanator.uxnkt

import net.walksanator.uxnkt.vm.varvara.VarvaraComputer
import java.io.File

fun main() {
    val ram = ByteArray(0xFFFF)
    val file = File("console.write.tal.rom")
    file.readBytes().onEachIndexed { idx, byte ->
        ram[0x100+idx] = byte
    }
    val varvara = VarvaraComputer(ram,1000)
    varvara.consumeFuel = false
    varvara.run()
    println()
}