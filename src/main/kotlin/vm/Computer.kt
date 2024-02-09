import java.util.function.Consumer

abstract class Computer {
    abstract fun queue(startpos: Short, prerun: Consumer<Computer>)
    abstract fun run()
}