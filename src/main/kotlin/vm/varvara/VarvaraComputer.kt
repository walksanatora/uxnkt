package net.walksanator.uxnkt.vm.varvara

import Computer
import net.walksanator.uxnkt.vm.Uxn
import java.util.*
import java.util.function.Consumer
import kotlin.collections.ArrayDeque

class VarvaraComputer(ram: ByteArray, var fuel: Int) : Computer() {
    val cpu = Uxn(ram)

    val system = SystemDevice(cpu)
    val console = ConsoleDevice()

    private val eventQueue: ArrayDeque<Pair<Short, Consumer<Computer>>> = ArrayDeque()
    var consumeFuel = true

    init {
        cpu.devices[0] = system
        cpu.devices[1] = console
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