fun Type.dump (spc: Int = 0): String {
    return " ".repeat(spc) + "Type." + when (this) {
        is Type.Unit -> "Unit\n"
        is Type.Nat  -> "Nat '" + this.tk_.src + "'\n"
        is Type.Pointer -> "Pointer\n" + this.pln.dump(spc+4)
        is Type.Tuple -> "Tuple\n" + this.vec.forEach { it.dump(spc+4) }
        is Type.Union -> "Union\n" + this.vec.forEach { it.dump(spc+4) }
        is Type.Active -> "Active\n" + this.tsk.dump(spc+4)
        is Type.Actives -> "Actives\n" + this.tsk.dump(spc+4)
        is Type.Alias -> "Alias '" + this.tk_.id + "'\n"
        is Type.Func -> "Func\n" + this.inp.dump(spc+4) + this.pub?.dump(spc+4) + this.out.dump(spc+4)
    }
}

fun Expr.dump (spc: Int = 0): String {
    return " ".repeat(spc) + "Expr." + when (this) {
        is Expr.Unit  -> "Unit\n"
        is Expr.Var   -> "Var '" + this.tk_.id + "'\n"
        is Expr.Nat   -> "Nat '" + this.tk_.src + "'\n"
        is Expr.As    -> "As " + this.tk_.sym + " " + this.xtype?.dump() + this.e.dump(spc+4)
        is Expr.Upref -> "Upref\n" + this.pln.dump(spc+4)
        is Expr.Dnref -> "Dnref\n" + this.ptr.dump(spc+4)
        is Expr.TCons -> "TCons\n" + this.arg.forEach { it.dump(spc+4) }
        is Expr.UCons -> "UCons ." + this.tk_.num + "\n" + this.arg.dump(spc+4)
        is Expr.UNull -> "UNull\n"
        is Expr.TDisc -> "TDisc ." + this.tk_.num + "\n" + this.tup.dump(spc+4)
        is Expr.Field -> "Field ." + this.tk_.id + "\n" + this.tsk.dump(spc+4)
        is Expr.UDisc -> "UDisc !" + this.tk_.num + "\n" + this.uni.dump(spc+4)
        is Expr.UPred -> "UPred ?" + this.tk_.num + "\n" + this.uni.dump(spc+4)
        is Expr.New   -> "New\n" + this.arg.dump(spc+4)
        is Expr.Call  -> "Call\n" + this.f.dump(spc+4) + this.arg.dump(spc+4)
        is Expr.Func  -> "Func\n" + this.ftp()!!.dump(spc+4) //+ this.tostr(e.block)
    }
}

/*
    open fun tostr (s: Stmt): String {
        return when (s) {
            is Stmt.Nop -> "\n"
            is Stmt.Native -> "native " + (if (s.istype) "type " else "") + s.tk_.toce() + "\n"
            is Stmt.Var -> "var " + s.tk_.id + ": " + this.tostr(s.xtype!!) + "\n"
            is Stmt.Set -> "set " + this.tostr(s.dst) + " = " + this.tostr(s.src) + "\n"
            is Stmt.Break -> "break\n"
            is Stmt.Return -> "return\n"
            is Stmt.Seq -> this.tostr(s.s1) + this.tostr(s.s2)
            is Stmt.SCall -> "call " + this.tostr(s.e) + "\n"
            is Stmt.Input -> (if (s.dst == null) "" else "set " + this.tostr(s.dst) + " = ") + "input " + s.lib.id + " " + this.tostr(s.arg) + ": " + this.tostr(s.xtype!!) + "\n"
            is Stmt.Output -> "output " + s.lib.id + " " + this.tostr(s.arg) + "\n"
            is Stmt.If -> "if " + this.tostr(s.tst) + "\n" + this.tostr(s.true_) + "else\n" + this.tostr(s.false_)
            is Stmt.Loop -> "loop " + this.tostr(s.block)
            is Stmt.Block -> (if (s.iscatch) "catch " else "") + "{" + (if (s.scp1.isanon()) "" else " @" + s.scp1!!.id) + "\n" + this.tostr(s.body) + "}\n"
            is Stmt.SSpawn -> (if (s.dst == null) "" else "set " + this.tostr(s.dst) + " = ") + "spawn " + this.tostr(s.call) + "\n"
            is Stmt.DSpawn -> "spawn " + this.tostr(s.call) + " in " + this.tostr(s.dst) + "\n"
            is Stmt.Await -> "await " + this.tostr(s.e) + "\n"
            is Stmt.Pause -> (if (s.pause) "pause " else "resume ") + this.tostr(s.tsk) + "\n"
            is Stmt.Emit -> when (s.tgt) {
                is Scope -> "emit @" + s.tgt.scp1.anon2local() + " " + this.tostr(s.e) + "\n"
                is Expr  -> "emit " + s.tgt.tostr() + " " + this.tostr(s.e) + "\n"
                else -> error("bug found")
            }
            is Stmt.Throw -> "throw\n"
            is Stmt.DLoop -> "loop " + this.tostr(s.i) + " in " + this.tostr(s.tsks) + " " + this.tostr(s.block)
            is Stmt.Typedef -> {
                val scps = " @[" + s.xscp1s.first!!.map { it.id }.joinToString(",") + "]"
                "type " + s.tk_.id + scps + " = " + this.tostr(s.type) + "\n"
            }
            else -> error("bug found")
        }
    }
}
*/