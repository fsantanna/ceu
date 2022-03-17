fun String.istype (): Boolean {
    return this.length>1 && this[0].isUpperCase() && this.any { it.isLowerCase() }
}

fun Tk.Scp.isscopepar (): Boolean {
    return this.str.none { it.isUpperCase() }
}

fun Tk.Nat.toce (): String {
    val (op, cl) = when (this.chr) {
        '{' -> Pair("{", "}")
        '(' -> Pair("(", ")")
        else -> Pair("", "")
    }
    return "_" + op + this.str + cl
}

fun Tk.field2num (ids: List<Tk>?): Int? {
    return when {
        this is Tk.Num -> this.num
        //this is Tk.Key -> null
        (ids == null)  -> null
        this is Tk.ide -> ids!!.indexOfFirst{it.str==this.str}.let { if (it == -1) null else it+1 }
        this is Tk.Ide -> {
            if (this.str == "Common") 0 else {
                ids!!.indexOfFirst { it.str == this.str }.let { if (it == -1) null else it + 1 }
            }
        }
        else -> error("bug found")
    }
}

fun Tk.istask (): Boolean {
    return this is Tk.ide && this.str in arrayOf("pub","ret","status")
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
