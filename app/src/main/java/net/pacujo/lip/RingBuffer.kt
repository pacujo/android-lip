package net.pacujo.lip

class RingBuffer<T>(val capacity: Int) : Iterable<T> {
    init {
        require(capacity > 0)
    }

    constructor(capacity: Int, iterable: Iterable<T>) : this(capacity) {
        for (e in iterable)
            add(e)
    }

    private val longCap = capacity.toLong()
    private val store = ArrayList<T>(capacity)
    private var count = 0L
    val size
        get() = if (count > longCap) capacity else count.toInt()

    private fun pos(longIndex: Long) = (longIndex % longCap).toInt()

    private fun full() = count >= longCap

    fun add(e: T) {
        if (full())
            store[pos(count++)] = e
        else {
            store.add(e)
            count++
        }
    }

    operator fun get(index: Int) =
        store[
            if (full())
                pos(longIndex = count - longCap + index.toLong())
            else
                index
        ]

    override fun iterator(): Iterator<T> = RBIterator()

    private inner class RBIterator : Iterator<T> {
        var nextIndex = 0

        override fun hasNext() = nextIndex < size

        override fun next() = get(nextIndex++)
    }
}

fun <T> Iterable<T>.toRingBuffer(capacity: Int) =
    RingBuffer(capacity = capacity, iterable = this)
