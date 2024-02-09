package net.walksanator.uxnkt.vm.util

import java.util.*

abstract class Either<L,R> {
    abstract fun isLeft(): Boolean
    abstract fun isRight(): Boolean
    abstract fun left(): Optional<L>
    abstract fun right(): Optional<R>
    companion object {
        fun <L: Any,R: Any> left(left: L) = Left<L,R>(left)
        fun <L: Any,R: Any> right(right: R) = Right<L,R>(right)
    }
}

class Left<L: Any, R: Any>(val left: L) : Either<L,R>() {
    override fun isLeft(): Boolean = true
    override fun isRight(): Boolean = false
    override fun left(): Optional<L> = Optional.of(left)
    override fun right(): Optional<R> = Optional.empty<R>()
}

class Right<L: Any,R: Any>(val right: R) : Either<L,R>() {
    override fun isLeft(): Boolean = false
    override fun isRight(): Boolean = true
    override fun left(): Optional<L> = Optional.empty<L>()
    override fun right(): Optional<R> = Optional.of(right)
}