package net.walksanator.uxnkt.vm

import java.util.function.Consumer

abstract class Computer(val cpu: Uxn) {

    /**
     * enqueues a event
     *
     * @param prerun a consumer to run before starting the UXN cpu (eg: setting device values or dumping some values into memory)<br> should also set PC to the desired vector
     */
    abstract fun queue(prerun: Consumer<Computer>)

    /**
     * runs the computer until it halts (BRK instr) or defers (eg: runs out of fuel)
     */
    abstract fun run()
}