fun Expr.flattenRight (): List<Expr> {
    return when (this) {
        is Expr.Unit, is Expr.Var, is Expr.Nat, is Expr.Func -> listOf(this)
        is Expr.TCons -> this.arg.map { it.flattenRight() }.flatten() + this
        is Expr.Call  -> this.f.flattenRight() + this.arg.flattenRight() + this
        is Expr.Cast  -> this.e.flattenRight() + this
        is Expr.Named   -> this.e.flattenRight() + this
        is Expr.UNamed -> this.e.flattenRight() + this
        is Expr.TDisc -> this.tup.flattenRight() + this
        is Expr.Field -> this.tsk.flattenRight() + this
        is Expr.UDisc -> this.uni.flattenRight() + this
        is Expr.UPred -> this.uni.flattenRight() + this
        is Expr.New   -> this.arg.flattenRight() + this
        is Expr.Dnref -> this.ptr.flattenRight() + this
        is Expr.Upref -> this.pln.flattenRight() + this
        is Expr.If    -> this.tst.flattenRight() + this.true_.flattenRight() + this.false_.flattenRight() + this
        is Expr.UCons, is Expr.UNull -> TODO(this.toString())
    }
}

fun Attr.toExpr (): Expr {
    return when (this) {
        is Attr.Var   -> Expr.Var(this.tk_)
        is Attr.Nat   -> Expr.Nat(this.tk_, this.type)
        is Attr.Unpak -> Expr.UNamed(this.tk_, this.isinf, this.e.toExpr())
        is Attr.Dnref -> Expr.Dnref(this.tk_,this.ptr.toExpr())
        is Attr.TDisc -> Expr.TDisc(this.tk_,this.tup.toExpr())
        is Attr.Field   -> Expr.Field(this.tk_,this.tsk.toExpr())
        is Attr.UDisc -> Expr.UDisc(this.tk_,this.uni.toExpr())
    }
}

// Expr.Var or Expr.Nat->null
fun Expr.toBaseVar (): Expr.Var? {
    return when (this) {
        is Expr.Nat -> null
        is Expr.Var -> this
        is Expr.TDisc -> this.tup.toBaseVar()
        is Expr.Dnref -> this.ptr.toBaseVar()
        is Expr.Upref -> this.pln.toBaseVar()
        is Expr.UDisc -> this.uni.toBaseVar()
        is Expr.Named   -> this.e.toBaseVar()
        is Expr.UNamed -> this.e.toBaseVar()
        else -> error("bug found")
    }
}

fun Expr.uname (): Expr {
    return if (this !is Expr.Named) this else this.e.uname()
}

fun Expr.upspawn (): Stmt? {
    val wup = this.wup
    return when (wup) {
        is Stmt.SSpawn, is Stmt.DSpawn -> wup as Stmt
        is Expr.Named -> wup.upspawn()
        else -> null
    }
}
