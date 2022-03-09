// Triple<lvl,par,depth>
data class Scope (var scp1: Tk.Scp, var scp2: Triple<Int,String?,Int?>?)

fun String?.isanon (): Boolean {
    return (this==null || (this.length>=2 && this[0]=='B' && this[1].isDigit()))
}

fun String.anon2local (): String {
    return if (this.isanon()) "LOCAL" else this
}

fun Any.localBlockScp1Id (): String {
    return this.ups_first_block().let { if (it == null) "GLOBAL" else ("B"+it.n) }
}

fun Stmt.setScp1s () {
    fun fx (up: Any, scp: Scope) {
        scp.scp1 = if (scp.scp1.id != "LOCAL") scp.scp1 else {
            Tk.Scp(TK.XSCP, scp.scp1.lin, scp.scp1.col, up.localBlockScp1Id())
        }
    }
    this.visit(null, null, null, ::fx)
}

fun Scope.toScp2 (up: Any): Triple<Int,String?,Int?> {
    val lvl = up.ups_tolist().filter { it is Expr.Func }.count() // level of function nesting
    return when (this.scp1.id) { // 2xExpr.Func, otherwise no level between outer/arg/body
        "GLOBAL" -> Triple(0, null, 0)
        else -> {
            val blk = up.env(this.scp1.id)
            if (blk != null) {
                // @A, @x, ...
                val one = if (blk is Stmt.Block) 1 else 0
                Triple(lvl, null, one + blk.ups_tolist().let { it.count{it is Stmt.Block} + 2*it.count{it is Expr.Func} })
            } else {
                Triple(lvl, this.scp1.id, null)
            }
        }
    }
}

fun Stmt.setScp2s () {
    fun fx (up: Any, scp: Scope) {
        scp.scp2 = scp.toScp2(up)
    }
    this.visit(null, null, null, ::fx)
}
