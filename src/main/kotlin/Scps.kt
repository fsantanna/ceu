// Triple<lvl,par,depth>
data class Scope (var scp1: Tk.Id, var scp2: Triple<Int,String?,Int?>?)

fun Tk.Id?.isanon (): Boolean {
    return this.let { (it==null || (it.id.length>=2 && it.id[0]=='B' && it.id[1].isDigit())) }
}

fun Tk.Id.anon2local (): String {
    return if (this.isanon()) "LOCAL" else this.id
}

fun Any.localBlockScp1Id (forceAnon: Boolean): String {
    return (this.ups_first { it is Stmt.Block } as Stmt.Block?).let {
        when {
            (it == null) -> "GLOBAL"
            //else -> "B" + it.n
            forceAnon -> "B" + it.n
            else -> it.scp1!!.id
        }
    }
}

fun Stmt.setScp1s () {
    fun fx (up: Any, scp: Scope) {
        scp.scp1 = if (scp.scp1.id != "LOCAL") scp.scp1 else {
            Tk.Id(TK.XID, scp.scp1.lin, scp.scp1.col, up.localBlockScp1Id(false))
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
                val umn = if (this.scp1.id=="arg")    1 else 0   // "arg" is in between Func-arg-Block
                Triple(lvl, null, one - umn + blk.ups_tolist().let { it.count{it is Stmt.Block} + 2*it.count{it is Expr.Func} })
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
