package net.walksanator.uxnkt.vm.varvara

import net.walksanator.uxnkt.vm.Computer
import net.walksanator.uxnkt.vm.Uxn
import net.walksanator.uxnkt.vm.WrappingByteArray
import java.util.function.Consumer
import kotlin.collections.ArrayDeque

open class VarvaraComputer(ram: ByteArray, var fuel: Int, val bonusPages: List<WrappingByteArray>) : Computer(Uxn(ram)) {

    constructor(ram: ByteArray, fuel: Int) : this(ram,fuel, listOf())
    constructor(ram: ByteArray, fuel: Int, bonusPages: Int) : this(ram,fuel, List(bonusPages) {
        WrappingByteArray(
            0x10000
        )
    })

    val system = SystemDevice(cpu,bonusPages)
    val console = ConsoleDevice()

    val file1 = FileDevice()
    val file2 = FileDevice()

    var spin = false //are we waiting on events
    var halt = false //have we fully stopped execution

    private val eventQueue: ArrayDeque<Consumer<Computer>> = ArrayDeque()
    var consumeFuel = true

    init {
        cpu.devices[0] = system
        cpu.devices[1] = console

        cpu.devices[10] = file1
        cpu.devices[11] = file2
    }

    override fun run() {
        if (halt) {return} //we literally need a reset
        if (spin) {
            if (eventQueue.size > 0) {
                eventQueue.removeLast().accept(this)
                spin = false
            } else {
                return //we have no events so we have nothing to do
            }
        }

        while (fuel > 0) {
            val res = cpu.step()
            if (res) {
                if (eventQueue.size > 0 && system.state.toInt() == 0){
                    eventQueue.removeLast().accept(this)
                } else {
                    if (system.state.toInt() != 0) {
                        eventQueue.clear() //clear queue as we have halted
                        halt = true //set halt flag
                    } else {
                        spin = true // we just have no events left so we SPIN
                    }
                    break //event queue is empty so we defer
                }
            }
            if (consumeFuel) {fuel -= 1}
        }
    }

    override fun queue(prerun: Consumer<Computer>) {
        eventQueue.addFirst(prerun)
    }
}