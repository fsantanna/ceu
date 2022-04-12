internal fun name (dmp: String): String {
    return dmp.takeWhile { it!=' ' && it!='\n' }
}
fun Type.name (): String {
    return name(this.dump())
}
fun Expr.name (): String {
    return name(this.dump())
}
fun Stmt.name (): String {
    return name(this.dump())
}

internal fun none (spc: Int): String {
    return " ".repeat(spc) + "none\n"
}

fun Type.dump (spc: Int = 0): String {
    fun pre (n:Int): String {
        return "[${this.tk.lin}] " + " ".repeat(spc+n)
    }
    return pre(0) + "Type." + when (this) {
        is Type.Unit -> "Unit\n"
        is Type.Nat  -> "Nat '" + this.tk.str + "'\n"
        is Type.Pointer -> "Pointer " + (this.xscp?.scp1?.str ?: "none") + "\n" + this.pln.dump(spc+4)
        is Type.Tuple -> "Tuple\n" + this.vec.map { it.dump(spc+4) }.joinToString("")
        is Type.Union -> "Union\n" + (if (this.common==null) "" else this.common.dump(spc+4)) + this.vec.map { it.dump(spc+4) }.joinToString("")
        is Type.Active -> "Active\n" + this.tsk.dump(spc+4)
        is Type.Actives -> "Actives\n" + this.tsk.dump(spc+4)
        is Type.Named -> {
            val subs = this.subs.map { '.'+it.str }.joinToString("") + "'\n"
            val args = if (this.xargs == null) {
                pre(4) + "no args\n"
            } else {
                this.xargs!!.map {it.dump(spc+4) }.joinToString("")
            }
            "Named '" + this.tk.str + subs + args
        }
        is Type.Func -> "Func\n" + this.inp.dump(spc+4) + (this.pub?.dump(spc+4)?:pre(4)+"no pub\n") + this.out.dump(spc+4)
        is Type.Par -> "Par " + this.tk.str + "\n" + (this.xtype?.dump(spc+4) ?: pre(4)+"no type\n")
    }
}

fun Expr.dump (spc: Int = 0): String {
    fun pre (n:Int=0): String {
        return "[${this.tk.lin}] " + " ".repeat(spc+n)
    }
    fun Type?.x (): String {
        return (this?.dump(spc+4) ?: pre(4)+"no type\n")
    }
    return (pre(0) + "Expr." + when (this) {
        is Expr.Unit  -> "Unit\n"
        is Expr.Var   -> "Var '" + this.tk.str + "'\n"
        is Expr.Nat   -> "Nat '" + this.tk.str + "'\n" + this.xtype.x()
        is Expr.Cast  -> "Cast\n" + this.e.dump(spc+4) + this.type.dump(spc+4)
        is Expr.Named   -> "Named\n" + this.xtype.x() + this.e.dump(spc+4)
        is Expr.UNamed -> "Unamed\n" + this.e.dump(spc+4)
        is Expr.Upref -> "Upref\n" + this.pln.dump(spc+4)
        is Expr.Dnref -> "Dnref\n" + this.ptr.dump(spc+4)
        is Expr.TCons -> "TCons\n" + this.arg.map { it.dump(spc+4) }.joinToString("")
        is Expr.UCons -> "UCons ." + this.tk.str + "\n" + this.xtype.x() + this.arg.dump(spc+4)
        is Expr.UNull -> "UNull\n" + this.xtype.x()
        is Expr.TDisc -> "TDisc ." + this.tk.str + "\n" + this.tup.dump(spc+4)
        is Expr.Field -> "Field ." + this.tk.str + "\n" + this.tsk.dump(spc+4)
        is Expr.UDisc -> "UDisc !" + this.tk.str + "\n" + this.uni.dump(spc+4)
        is Expr.UPred -> "UPred ?" + this.tk.str + "\n" + this.uni.dump(spc+4)
        is Expr.New   -> "New\n" + this.arg.dump(spc+4)
        is Expr.Call  -> "Call\n" + this.f.dump(spc+4) + this.arg.dump(spc+4)
        is Expr.Func  -> "Func\n" + this.xtype.x() + this.block.dump(spc+4)
        is Expr.If    -> "If\n" + this.tst.dump(spc+4) + this.true_.dump(spc+4) + this.false_.dump(spc+4)
    }).let {
        //"{{"+this.wtype!!.dump(spc)+"}}" + it
        it
    }
}

fun Stmt.dump (spc: Int = 0): String {
    fun pre (n:Int=0): String {
        return "[${this.tk.lin}] " + " ".repeat(spc+n)
    }
    fun Type?.x (): String {
        return (this?.dump(spc+4) ?: pre(4)+"no type\n")
    }
    return pre() + "Stmt." + when (this) {
        is Stmt.Nop -> "Nop\n"
        is Stmt.Native -> "Native " + this.tk.str + "\n"
        is Stmt.Var -> "Var " + this.tk.str + "\n" + this.xtype.x()
        is Stmt.Set -> "Set\n" + this.dst.dump(spc+4) + this.src.dump(spc+4)
        is Stmt.XBreak -> "Break\n"
        is Stmt.XReturn -> "Return\n"
        is Stmt.Seq -> "Seq\n" + this.s1.dump(spc+4) + this.s2.dump(spc+4)
        is Stmt.SCall -> "SCall\n" + this.e.dump(spc+4)
        is Stmt.Input -> "Input " + "\n" +
                this.xtype.x() +
                (if (this.dst == null) none(spc+4) else this.dst.dump(spc+4)) +
                this.arg.dump(spc+4)
        is Stmt.Output -> "Output " + "\n" + this.arg.dump(spc+4)
        is Stmt.If -> "If\n" + this.tst.dump(spc+4) + this.true_.dump(spc+4) + this.false_.dump(spc+4)
        is Stmt.Loop -> "Loop\n" + this.block.dump(spc+4)
        is Stmt.Block -> "Block" +
                (if (this.catch==null) "" else " (catch)") +
                (if (this.scp1?.str.isanon()) "" else " @" + (this.scp1?.str ?: "anon")) +
                "\n" +
                this.body.dump(spc+4)
        is Stmt.SSpawn -> "SSpawn\n" +
                (if (this.dst == null) none(spc+4) else this.dst.dump(spc+4)) +
                this.call.dump(spc+4)
        is Stmt.DSpawn -> "DSpawn\n" + this.call.dump(spc+4) + this.dst.dump(spc+4)
        is Stmt.Await -> "Await\n" + this.e.dump(spc+4)
        is Stmt.Pause -> "Pause\n" + this.tsk.dump(spc+4)
        is Stmt.Emit -> "Emit" + when (this.tgt) {
            is Scope -> " @" + this.tgt.scp1.str.anon2local() + "\n"
            is Expr -> "\n" + this.tgt.dump(spc+4)
            else -> error("bug found")
        } + this.e.dump(spc+4)
        is Stmt.Throw -> "Throw\n" + this.e.dump(spc+4)
        is Stmt.DLoop -> "DLoop\n" + this.i.dump(spc+4) + this.tsks.dump(spc+4) + this.block.dump(spc+4)
        is Stmt.Typedef -> "Typedef " + this.tk.str + "\n" + this.type.dump(spc+4)
        else -> error("bug found")
    }
}
