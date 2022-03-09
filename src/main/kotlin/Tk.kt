fun Tk.astype (): Tk.ide {
    val id = this as Tk.ide
    All_assert_tk(this, this.istype()) { "invalid type identifier" }
    return id
}
fun Tk.istype (): Boolean {
    return (this is Tk.ide) && this.id.istype()
}
fun String.istype (): Boolean {
    return this.length>1 && this[0].isUpperCase() && this.any { it.isLowerCase() }
}

fun Tk.asvar (): Tk.ide {
    val id = this as Tk.ide
    All_assert_tk(this, this.isvar()) { "invalid variable identifier" }
    return id
}
fun Tk.isvar (): Boolean {
    return (this is Tk.ide) && this.id.isvar()
}
fun String.isvar (): Boolean {
    return this.length>0 && this[0].isLowerCase()
}

fun Tk.asscope (): Tk.ide {
    val id = this as Tk.ide
    All_assert_tk(this, id.isscopecst() || id.isscopepar()) { "invalid scope identifier" }
    return id
}
fun Tk.isscopepar (): Boolean {
    val id = this as Tk.ide
    return id.id.none { it.isUpperCase() }
}
fun Tk.asscopepar (): Tk.ide {
    All_assert_tk(this, this.isscopepar()) { "invalid scope parameter identifier" }
    return this as Tk.ide
}
fun Tk.isscopecst (): Boolean {
    val id = this as Tk.ide
    return id.id.none { it.isLowerCase() }
}
fun Tk.asscopecst (): Tk.ide {
    All_assert_tk(this, this.isscopecst()) { "invalid scope constant identifier" }
    return this as Tk.ide
}

fun Tk.Nat.toce (): String {
    val (op, cl) = when (this.chr) {
        '{' -> Pair("{", "}")
        '(' -> Pair("(", ")")
        else -> Pair("", "")
    }
    return "_" + op + this.src + cl
}

fun Tk.field2num (ids: List<Tk.ide>?): Int {
    return when (this) {
        is Tk.Num -> this.num
        is Tk.ide  -> if (this.id == "Null") 0 else ids!!.indexOfFirst{it.id==this.id}+1
        else -> error("bug found")
    }
}

fun Tk.tostr (): String {
    return when (this) {
        is Tk.Num -> this.num.toString()
        is Tk.ide  -> this.id
        else -> error("bug found")
    }
}

fun Tk?.isnull (): Boolean {
    return when (this) {
        null      -> false
        is Tk.Num -> (this.num == 0)
        is Tk.ide  -> (this.id == "Null")
        else      -> error("bug found")
    }
}

fun Tk.istask (): Boolean {
    return this is Tk.ide && this.id in arrayOf("pub","ret","state")
}

fun TK.toErr (chr: Char?): String {
    return when (this) {
        TK.EOF     -> "end of file"
        TK.CHAR    -> "`" + chr!! + "´"
        TK.XNAT    -> "`_´"
        TK.Xide     -> "identifier"
        TK.XNUM    -> "number"
        TK.ARROW   -> "`->´"
        TK.ATBRACK -> "`@[´"
        TK.ELSE    -> "`else`"
        TK.IN      -> "`in`"
        TK.INPUT   -> "`input`"
        TK.TASK    -> "`task`"
        else -> TODO(this.toString())
    }
}
