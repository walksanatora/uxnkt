package net.walksanator.uxnkt

import net.walksanator.uxnkt.vm.varvara.VarvaraComputer
import java.io.File
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.exists

fun main(args: Array<String>) {
    val ram = ByteArray(0x10000)
    val file = File(args.firstOrNull()?: "output.rom")
    file.readBytes().onEachIndexed { idx, byte ->
        ram[0x100+idx] = byte
    }
    val varvara = VarvaraComputer(ram,1000)
    varvara.consumeFuel = false
    varvara.run()
    val orom = Path("statdump")
    if (!orom.exists()) {
        Files.createDirectory(orom)
    }
    varvara.cpu.dumpExecTo(orom)
}