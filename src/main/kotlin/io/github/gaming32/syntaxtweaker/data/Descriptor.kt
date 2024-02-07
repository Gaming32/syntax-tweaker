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
                '[' -> {
                    var dims = 1
                    while (index + dims < descriptor.length && descriptor[index + dims] == '[') {
                        dims++
                    }
                    if (index + dims >= descriptor.length) {
                        throw IllegalArgumentException(
                            "Array descriptor's dimensions reach end of descriptor: ${descriptor.substring(index)}"
                        )
                    }
                    if (dims > 255) {
                        throw IllegalArgumentException(
                            "Array descriptor has more than 255 dimensions ($dims): ${descriptor.substring(index)}"
                        )
                    }
                    if (descriptor[index + dims] == 'V') {
                        throw IllegalArgumentException(
                            "Array may not be void at $dims: ${descriptor.substring(index)}"
                        )
                    }
                    verifyDescriptor(descriptor, index + dims)
                }
                '(' -> {
                    if (index > 0) {
                        throw IllegalArgumentException(
                            "Descriptor cannot have inline method descriptor at $index: $descriptor"
                        )
                    }
                    var checkIndex = 1
                    while (checkIndex < descriptor.length && descriptor[checkIndex] != ')') {
                        if (descriptor[checkIndex] == 'V') {
                            throw IllegalArgumentException(
                                "Method descriptor may not have void argument at $checkIndex: $descriptor"
                            )
                        }
                        val newIndex = verifyDescriptor(descriptor, checkIndex)
                        checkIndex = newIndex
                    }
                    if (checkIndex >= descriptor.length) {
                        throw IllegalArgumentException(
                            "Method descriptor doesn't have right paren: $descriptor"
                        )
                    }
                    if (checkIndex + 1 >= descriptor.length) {
                        throw IllegalArgumentException(
                            "Method descriptor doesn't have return: $descriptor"
                        )
                    }
                    verifyDescriptor(descriptor, checkIndex + 1)
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
