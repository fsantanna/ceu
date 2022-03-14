fun String.istype (): Boolean {
    return this.length>1 && this[0].isUpperCase() && this.any { it.isLowerCase() }
}

fun Tk.Scp.isscopepar (): Boolean {
    return this.id.none { it.isUpperCase() }
}

fun Tk.id (): String {
    return when (this) {
        is Tk.ide -> this.id
        is Tk.Ide -> this.id
        is Tk.IDE -> this.id
        else -> error("bug found")
    }
}

fun Tk.Nat.toce (): String {
    val (op, cl) = when (this.chr) {
        '{' -> Pair("{", "}")
        '(' -> Pair("(", ")")
        else -> Pair("", "")
    }
    return "_" + op + this.src + cl
}

fun Tk.field2num (ids: List<Tk>?): Int? {
    return when {
        this is Tk.Num -> this.num
        //this is Tk.Key -> null
        (ids == null)  -> null
        this is Tk.ide -> ids!!.indexOfFirst{it.id()==this.id}.let { if (it == -1) null else it+1 }
        this is Tk.Ide -> ids!!.indexOfFirst{it.id()==this.id}.let { if (it == -1) null else it+1 }
        else -> error("bug found")
    }
}

fun Tk.tostr (): String {
    return when (this) {
        is Tk.Num -> this.num.toString()
        is Tk.Ide -> this.id
        is Tk.ide -> this.id
        is Tk.Key -> this.key
        else -> error("bug found")
    }
}

fun Tk.istask (): Boolean {
    return this is Tk.ide && this.id in arrayOf("pub","ret","status")
}

fun TK.toErr (chr: Char?): String {
    return when (this) {
        TK.EOF     -> "end of file"
        TK.CHAR    -> "`" + chr!! + "´"
        TK.XNAT    -> "`_´"
        TK.Xide    -> "variable identifier"
        TK.XIde    -> "type identifier"
        TK.XIDE    -> "uppercase identifier"
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
