object TestTweak : SyntaxTweak, TweakParser<TestTweak> {
    const val ID = "script-test"

    override val id get() = ID

    override val supportedReferenceTypes = emptyEnumSet<SyntaxTweak.ReferenceType>()

    override fun TweakParser.ParseContext.parse() = this@TestTweak
}

register(TestTweak.ID, TestTweak)
