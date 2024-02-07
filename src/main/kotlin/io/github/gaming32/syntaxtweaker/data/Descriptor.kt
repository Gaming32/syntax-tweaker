package io.github.gaming32.syntaxtweaker.data

@JvmInline
value class Descriptor(val raw: String) {
    companion object {
        fun String.toDescriptor() = Descriptor(this)

        fun verifyDescriptor(descriptor: String) {
            if (verifyDescriptor(descriptor, 0) != descriptor.length) {
                throw IllegalArgumentException("Underflow descriptor: $descriptor")
            }
        }

        private fun verifyDescriptor(descriptor: String, index: Int): Int {
            if (index >= descriptor.length) {
                throw IllegalArgumentException("Descriptor is empty")
            }
            return when (descriptor[index]) {
                'V', 'B', 'Z', 'S', 'C', 'I', 'F', 'J', 'D' -> index + 1
                'L' -> {
                    val end = descriptor.indexOf(';', index)
                    if (end == -1) {
                        throw IllegalArgumentException(
                            "Reference descriptor is unterminated: ${descriptor.substring(index)}"
                        )
                    }
                    end + 1
                }
                '(' -> {
                    if (index > 0) {
                        throw IllegalArgumentException(
                            "Descriptor cannot have inline method descriptor at $index: $descriptor"
                        )
                    }
                    val end = descriptor.indexOf(')')
                    if (end == -1) {
                        throw IllegalArgumentException(
                            "Method descriptor is unterminated: ${descriptor.substring(index)}"
                        )
                    }
                    var checkIndex = index + 1
                    while (checkIndex < end) {
                        val newIndex = verifyDescriptor(descriptor, checkIndex)
                        if (descriptor[checkIndex] == 'V') {
                            throw IllegalArgumentException(
                                "Method descriptor may not have void argument at $index: $descriptor"
                            )
                        }
                        checkIndex = newIndex
                    }
                    if (end == descriptor.length - 1) {
                        throw IllegalArgumentException(
                            "Method descriptor has no return: ${descriptor.substring(index)}"
                        )
                    }
                    verifyDescriptor(descriptor, end + 1)
                }
                else -> throw IllegalArgumentException("Unknown descriptor: ${descriptor.substring(index)}")
            }
        }
    }

    init {
        verifyDescriptor(raw)
    }

    val isMethod get() = raw[0] == '('

    override fun toString() = raw

    fun getArgumentCount(): Int {
        if (!isMethod) {
            throw IllegalArgumentException("$this is not a method")
        }
        var count = 0
        var index = 1
        while (raw[index] != ')') {
            count++
            if (raw[index] == 'L') {
                index = raw.indexOf(';', index)
            }
            index++
        }
        return count
    }
}
