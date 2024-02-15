package net.walksanator.uxnkt.vm.varvara

import Computer
import net.walksanator.uxnkt.vm.Uxn
import java.util.*
import java.util.function.Consumer
import kotlin.collections.ArrayDeque

class VarvaraComputer(ram: ByteArray, var fuel: Int) : Computer(Uxn(ram)) {

    val system = SystemDevice(cpu)
    val console = ConsoleDevice()

    val file1 = FileDevice()
    val file2 = FileDevice()

    private val eventQueue: ArrayDeque<Pair<Short, Consumer<Computer>>> = ArrayDeque()
    var consumeFuel = true

    init {
        cpu.devices[0] = system
        cpu.devices[1] = console

        cpu.devices[10] = file1
        cpu.devices[11] = file2
    }

    override fun run() {
        while (fuel > 0) {
            val res = cpu.step()
            if (res) {
                break //CPU signaled no more instructions (BRK) so we exit
            }
            if (consumeFuel) {fuel -= 1}
        }
    }

    override fun queue(startpos: Short, prerun: Consumer<Computer>) {
        eventQueue.addFirst(Pair(startpos,prerun))
    }
}