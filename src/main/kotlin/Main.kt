package net.walksanator.uxnkt

import com.google.gson.GsonBuilder
import net.walksanator.uxnkt.vm.varvara.VarvaraComputer
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*

@OptIn(ExperimentalPathApi::class)
fun main(args: Array<String>) {

    val ram = ByteArray(0x10000)
    val rom = args.firstOrNull()?: run {
        System.setProperty("debug","")
        "cpy.rom"
    }
    val orom = Path("statdump")
    if (System.getProperty("debug")!=null) {
        orom.deleteRecursively()
        orom.deleteIfExists()
    }
    if (!orom.exists()) {
        Files.createDirectory(orom)
    }
    println("loading rom: %s".format(rom))
    println("root is at %s".format(File(".").canonicalPath))
    val file = File(rom)
    file.readBytes().onEachIndexed { idx, byte ->
        ram[0x100+idx] = byte
    }
    val varvara = VarvaraComputer(ram,1000)
    varvara.consumeFuel = false
    varvara.run()

    if (System.getProperty("debug") == null) {
        varvara.cpu.captureFrame()
        val gson = GsonBuilder().excludeFieldsWithoutExposeAnnotation().create()
        val dump = File("final_frame.json")
        dump.delete()
        dump.writeText(
            gson.toJson(varvara.cpu.executionLog.last())
        )
    } else {
        varvara.cpu.dumpFrames(orom)
    }

}