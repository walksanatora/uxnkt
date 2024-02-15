package net.walksanator.uxnkt

import com.google.gson.GsonBuilder
import net.walksanator.uxnkt.vm.varvara.ConsoleDevice
import net.walksanator.uxnkt.vm.varvara.VarvaraComputer
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*

@OptIn(ExperimentalPathApi::class)
fun main(args: Array<String>) {
    val margs = args.toMutableList()
    val ram = ByteArray(0x10000)
    val rom = margs.removeFirstOrNull()?: run {
        System.setProperty("debug","")
        "output.rom"
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

    for (arg in margs) {
        for (char in arg) {
            varvara.queue {
                val console = (it.cpu.devices[1] as ConsoleDevice)
                it.cpu.pc = console.callbackVector
                console.read = char.code.toByte()
                console.type = 2 //argument
            }
        }
        varvara.queue {
            val console = (it.cpu.devices[1] as ConsoleDevice)
            it.cpu.pc = console.callbackVector
            console.read = ' '.code.toByte()
            console.type = 3 //seperator
        }
    }
    if (margs.size > 0) {
        (varvara.cpu.devices[1] as ConsoleDevice).type = margs.size.toByte()
        varvara.queue {
            val console = (it.cpu.devices[1] as ConsoleDevice)
            it.cpu.pc = console.callbackVector
            console.read = ' '.code.toByte()
            console.type = 4 //end of args
        }
    }

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