package net.walksanator.uxnkt.vm.varvara

import net.walksanator.uxnkt.vm.Computer
import net.walksanator.uxnkt.vm.Uxn
import java.util.function.Consumer
import kotlin.collections.ArrayDeque

open class VarvaraComputer(ram: ByteArray, var fuel: Int) : Computer(Uxn(ram)) {

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
                if (eventQueue.size > 0){
                    val event = eventQueue.removeLast()
                    cpu.pc = event.first
                    event.second.accept(this)
                } else {
                    break
                }
            }
            if (consumeFuel) {fuel -= 1}
        }
    }

    override fun queue(startpos: Short, prerun: Consumer<Computer>) {
        eventQueue.addFirst(Pair(startpos,prerun))
    }
}