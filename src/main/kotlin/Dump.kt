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
    return "[${this.tk.lin}] " + " ".repeat(spc) + "Type." + when (this) {
        is Type.Unit -> "Unit\n"
        is Type.Nat  -> "Nat '" + this.tk_.src + "'\n"
        is Type.Pointer -> "Pointer " + (this.xscp?.scp1?.id ?: "none") + "\n" + this.pln.dump(spc+4)
        is Type.Tuple -> "Tuple\n" + this.vec.map { it.dump(spc+4) }.joinToString("")
        is Type.Union -> "Union\n" + (if (this.common==null) "" else this.common.dump(spc+4)) + this.vec.map { it.dump(spc+4) }.joinToString("")
        is Type.Active -> "Active\n" + this.tsk.dump(spc+4)
        is Type.Actives -> "Actives\n" + this.tsk.dump(spc+4)
        is Type.Alias -> "Alias '" + this.tk_.id + "'\n"
        is Type.Func -> "Func\n" + this.inp.dump(spc+4) + (if (this.pub==null) "" else this.pub?.dump(spc+4)) + this.out.dump(spc+4)
    }
}

fun Expr.dump (spc: Int = 0): String {
    return "[${this.tk.lin}] " + " ".repeat(spc) + "Expr." + when (this) {
        is Expr.Unit  -> "Unit\n"
        is Expr.Var   -> "Var '" + this.tk_.id + "'\n"
        is Expr.Nat   -> "Nat '" + this.tk_.src + "'\n"
        is Expr.Cast  -> "Cast\n" + this.e.dump(spc+4) + this.type.dump(spc+4)
        is Expr.Pak   -> "Pak\n" + (this.xtype?.dump(spc+4)?:none(spc+4)) + this.e.dump(spc+4)
        is Expr.Unpak -> "Unpak\n" + this.e.dump(spc+4)
        is Expr.Upref -> "Upref\n" + this.pln.dump(spc+4)
        is Expr.Dnref -> "Dnref\n" + this.ptr.dump(spc+4)
        is Expr.TCons -> "TCons\n" + this.arg.map { it.dump(spc+4) }.joinToString("")
        is Expr.UCons -> "UCons ." + this.tk.tostr() + "\n" + this.arg.dump(spc+4)
        is Expr.UNull -> "UNull\n"
        is Expr.TDisc -> "TDisc ." + this.tk.tostr() + "\n" + this.tup.dump(spc+4)
        is Expr.Field -> "Field ." + this.tk_.id + "\n" + this.tsk.dump(spc+4)
        is Expr.UDisc -> "UDisc !" + this.tk.tostr() + "\n" + this.uni.dump(spc+4)
        is Expr.UPred -> "UPred ?" + this.tk.tostr() + "\n" + this.uni.dump(spc+4)
        is Expr.New   -> "New\n" + this.arg.dump(spc+4)
        is Expr.Call  -> "Call\n" + this.f.dump(spc+4) + this.arg.dump(spc+4)
        is Expr.Func  -> "Func\n" + this.xtype?.dump(spc+4) + this.block.dump(spc+4)
    }
}

fun Stmt.dump (spc: Int = 0): String {
    return "[${this.tk.lin}] " + " ".repeat(spc) + "Stmt." + when (this) {
        is Stmt.Nop -> "Nop\n"
        is Stmt.Native -> "Native " + this.tk_.toce() + "\n"
        is Stmt.Var -> "Var " + this.tk_.id + "\n" + (this.xtype?.dump(spc+4) ?: "")
        is Stmt.Set -> "Set\n" + this.dst.dump(spc+4) + this.src.dump(spc+4)
        is Stmt.Break -> "Break\n"
        is Stmt.Return -> "Return\n"
        is Stmt.Seq -> "Seq\n" + this.s1.dump(spc+4) + this.s2.dump(spc+4)
        is Stmt.SCall -> "SCall\n" + this.e.dump(spc+4)
        is Stmt.Input -> "Input " + this.lib.id + "\n" +
                (this.xtype?.dump(spc+4) ?: "") +
                (if (this.dst == null) none(spc+4) else this.dst.dump(spc+4)) +
                this.arg.dump(spc+4)
        is Stmt.Output -> "Output " + this.lib.id + "\n" + this.arg.dump(spc+4)
        is Stmt.If -> "If\n" + this.tst.dump(spc+4) + this.true_.dump(spc+4) + this.false_.dump(spc+4)
        is Stmt.Loop -> "Loop\n" + this.block.dump(spc+4)
        is Stmt.Block -> "Block" +
                (if (this.iscatch) " (catch)" else "") +
                (if (this.scp1?.id.isanon()) "" else " @" + (this.scp1?.id ?: "anon")) +
                "\n" +
                this.body.dump(spc+4)
        is Stmt.SSpawn -> "SSpawn\n" +
                (if (this.dst == null) none(spc+4) else this.dst.dump(spc+4)) +
                this.call.dump(spc+4)
        is Stmt.DSpawn -> "DSpawn\n" + this.call.dump(spc+4) + this.dst.dump(spc+4)
        is Stmt.Await -> "Await\n" + this.e.dump(spc+4)
        is Stmt.Pause -> "Pause\n" + this.tsk.dump(spc+4)
        is Stmt.Emit -> "Emit" + when (this.tgt) {
            is Scope -> " @" + this.tgt.scp1.id.anon2local() + "\n"
            is Expr -> this.tgt.dump(spc+4)
            else -> error("bug found")
        } + this.e.dump(spc+4)
        is Stmt.Throw -> "Throw\n"
        is Stmt.DLoop -> "DLoop\n" + this.i.dump(spc+4) + this.tsks.dump(spc+4) + this.block.dump(spc+4)
        is Stmt.Typedef -> "Typedef " + this.tk_.id + "\n" + this.type.dump(spc+4)
        else -> error("bug found")
    }
}
