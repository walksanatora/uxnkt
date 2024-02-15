import net.walksanator.uxnkt.vm.Uxn
import java.util.function.Consumer

abstract class Computer(val cpu: Uxn) {

    /**
     * enqueues a event
     *
     * @param startpos the instruction to start execution at
     * @param prerun a consumer to run before starting the UXN cpu (eg: setting device values or dumping some values into memory)
     */
    abstract fun queue(startpos: Short, prerun: Consumer<Computer>)

    /**
     * runs the computer until it halts (BRK instr) or defers (eg: runs out of fuel)
     */
    abstract fun run()
}