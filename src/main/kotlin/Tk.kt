fun String.istype (): Boolean {
    return this.length>1 && this[0].isUpperCase() && this.any { it.isLowerCase() }
}

fun Tk.Scp.isscopepar (): Boolean {
    return this.str.none { it.isUpperCase() }
}

fun Tk.Nat.payload (): String {
    return when {
        (this.str.length == 1) -> ""
        this.str[1].let { it == '{' || it == '(' } -> this.str.drop(2).take(this.str.length - 3)
        else -> this.str.drop(1)
    }
}

fun Tk.field2num (ids: List<Tk>?): Int? {
    return when {
        this is Tk.Num -> this.str.toInt()
        //this is Tk.Fix -> null
        (ids == null)  -> null
        this is Tk.id -> ids!!.indexOfFirst{it.str==this.str}.let { if (it == -1) null else it+1 }
        this is Tk.Id -> {
            if (this.str == "Common") 0 else {
                ids!!.indexOfFirst { it.str == this.str }.let { if (it == -1) null else it + 1 }
            }
        }
        else -> error("bug found")
    }
}

fun Tk.istask (): Boolean {
    return this is Tk.id && this.str in arrayOf("pub","ret","status")
}

fun Tk.toPay (): String {
    return when {
        (this is Tk.Eof) -> "end of file"
        (this is Tk.Scp) -> "\"@" + this.str + '"'
        (this is Tk.Clk) -> "time constant"
        else             -> '"'+this.str+'"'
    }
}

fun TK.toErr (): String {
    return when (this) {
        TK.EOF -> "end of file"
        TK.NAT -> "\"_\""
        TK.id  -> "variable identifier"
        TK.Id  -> "type identifier"
        TK.ID  -> "uppercase identifier"
        TK.NUM -> "number"
        else   -> TODO(this.toString())
    }
}
