fun Tk.lincol (src: String): String {
    if (true) {
        //return src
    }
    return """
        
        ^[${this.lin},${this.col}]
        $src
        ^[]
        
    """.trimIndent()
}

fun Type.tostr (lc: Boolean = false): String {
    fun List<Tk.Id>?.idx (i: Int, c: Char): String {
        return if (this == null) "" else this[i].id+c
    }
    return when (this) {
        is Type.Unit    -> "()"
        is Type.Nat     -> this.tk_.toce()
        is Type.Pointer -> this.xscp!!.let { "/" + this.pln.tostr(lc) + " @" + it.scp1.anon2local() }
        is Type.Alias   -> this.tk_.id + this.xscps.let { if (it==null) "" else it.let {
            if (it.size == 0) "" else " @[" + it.map { it.scp1.anon2local() }.joinToString(",") + "]"
        }}
        is Type.Tuple   -> "[" + this.vec.mapIndexed { i,v -> this.yids.idx(i,':') + v.tostr(lc) }.joinToString(",") + "]"
        is Type.Union   -> "<" + this.vec.mapIndexed { i,v -> this.yids.idx(i,'=') + v.tostr(lc) }.joinToString(",") + ">"
        is Type.Active  -> "active " + this.tsk.tostr(lc)
        is Type.Actives -> "active {${this.len?.num ?: ""}} " + this.tsk.tostr(lc)
        is Type.Alias   -> this.tk_.id + this.xscps!!.let {
            if (it.size == 0) "" else " @[" + it.map { it.scp1.anon2local() }.joinToString(",") + "]"
        }
        is Type.Func    -> {
            val ctrs = this.xscps.third.let {
                if (it == null || it.isEmpty()) "" else ": " + it.map { it.first + ">" + it.second }
                    .joinToString(",")
            }
            val scps = this.xscps.second.let { if (it==null) "" else " @[" + it.map { it.scp1.anon2local() }.joinToString(",") + ctrs + "] -> " }
            this.tk_.key + scps + this.inp.tostr(lc) + " -> " + this.pub.let { if (it == null) "" else it.tostr(lc) + " -> " } + this.out.tostr(lc)
        }
    }.let {
        if (!lc) it else {
            this.tk.lincol(it)
        }
    }
}

fun Expr.tostr (lc: Boolean = false): String {
    return when (this) {
        is Expr.Unit  -> "()"
        is Expr.Var   -> this.tk_.id
        is Expr.Nat   -> if (this.xtype==null) this.tk_.toce() else "(" + this.tk_.toce() + ": " + this.xtype!!.tostr(lc) + ")"
        is Expr.Pak   -> if (this.xtype==null) this.e.tostr(lc) else ("(" + this.e.tostr(lc) + " " + this.tk_.sym + " " + this.xtype!!.tostr(lc) + ")")
        is Expr.Unpak -> this.e.wtype.let { if (it==null || it.noact() !is Type.Alias) this.e.tostr(lc) else ("(" + this.e.tostr(lc) + " " + this.tk_.sym + ")") }
        is Expr.Upref -> "(/" + this.pln.tostr(lc) + ")"
        is Expr.Dnref -> "(" + this.ptr.tostr(lc) + "\\)"
        is Expr.TCons -> "[" + this.arg.map { it.tostr(lc) }.joinToString(",") + "]"
        is Expr.UCons -> "<." + this.tk.tostr() + " " + this.arg.tostr(lc) + ">" + this.wtype.let { if (it==null) "" else ": "+it.tostr(lc) }
        is Expr.UNull -> "<.0>" + this.wtype.let { if (it==null) "" else ": "+it.tostr(lc) }
        is Expr.TDisc -> "(" + this.tup.tostr(lc) + "." + this.tk.tostr() + ")"
        is Expr.Field -> "(" + this.tsk.tostr(lc) + ".${this.tk_.id})"
        is Expr.UDisc -> "(" + this.uni.tostr(lc) + "!" + this.tk.tostr() + ")"
        is Expr.UPred -> "(" + this.uni.tostr(lc) + "?" + this.tk.tostr() + ")"
        is Expr.New -> "(new " + this.arg.tostr(lc) + this.xscp.let { if (it==null) "" else ": @" + this.xscp!!.scp1.anon2local() } + ")"
        is Expr.Call -> {
            val inps = this.xscps.first.let { if (it==null) "" else " @[" + it.map { it.scp1.anon2local() }.joinToString(",") + "]" }
            val out = this.xscps.second.let { if (it == null) "" else ": @" + it.scp1.anon2local() }
            "(" + this.f.tostr(lc) + inps + " " + this.arg.tostr(lc) + out + ")"
        }
        is Expr.Func -> this.ftp()!!.tostr(lc) + " " + this.block.tostr(lc)
    }.let {
        if (!lc) it else {
            this.tk.lincol(it)
        }
    }
}

fun Stmt.tostr (lc: Boolean = false): String {
    return when (this) {
        is Stmt.Nop -> "\n"
        is Stmt.Native -> "native " + (if (this.istype) "type " else "") + this.tk_.toce() + "\n"
        is Stmt.Var -> "var " + this.tk_.id + this.xtype.let { if (it==null) " = var" else (": "+it.tostr()) } + "\n"
        is Stmt.Set -> "set " + this.dst.tostr(lc) + " = " + this.src.tostr(lc) + "\n"
        is Stmt.Break -> "break\n"
        is Stmt.Return -> "return\n"
        is Stmt.Seq -> this.s1.tostr(lc) + this.s2.tostr(lc)
        is Stmt.SCall -> "call " + this.e.tostr(lc) + "\n"
        is Stmt.Input -> (if (this.dst == null) "" else "set " + this.dst.tostr(lc) + " = ") +
            "input " + this.lib.id + " " + this.arg.tostr(lc) + this.xtype.let { if (it==null) "" else ": " + it.tostr(lc) } +
            "\n"
        is Stmt.Output -> "output " + this.lib.id + " " + this.arg.tostr(lc) + "\n"
        is Stmt.If -> "if " + this.tst.tostr(lc) + "\n" + this.true_.tostr(lc) + "else\n" + this.false_.tostr(lc)
        is Stmt.Loop -> "loop " + this.block.tostr(lc)
        is Stmt.Block -> (if (this.iscatch) "catch " else "") + "{" + (if (this.scp1.isanon()) "" else " @" + this.scp1!!.id) + "\n" + this.body.tostr(lc) + "}\n"
        is Stmt.SSpawn -> (if (this.dst == null) "" else "set " + this.dst.tostr(lc) + " = ") + "spawn " + this.call.tostr(lc) + "\n"
        is Stmt.DSpawn -> "spawn " + this.call.tostr(lc) + " in " + this.dst.tostr(lc) + "\n"
        is Stmt.Await -> "await " + this.e.tostr(lc) + "\n"
        is Stmt.Pause -> (if (this.pause) "pause " else "resume ") + this.tsk.tostr(lc) + "\n"
        is Stmt.Emit -> when (this.tgt) {
            is Scope -> "emit @" + this.tgt.scp1.anon2local() + " " + this.e.tostr(lc) + "\n"
            is Expr  -> "emit " + this.tgt.tostr(lc) + " " + this.e.tostr(lc) + "\n"
            else -> error("bug found")
        }
        is Stmt.Throw -> "throw\n"
        is Stmt.DLoop -> "loop " + this.i.tostr(lc) + " in " + this.tsks.tostr(lc) + " " + this.block.tostr(lc)
        is Stmt.Typedef -> {
            val scps = this.xscp1s.first.let { if (it == null) "" else " @[" + this.xscp1s.first!!.map { it.id }.joinToString(",") + "]" }
            "type " + this.tk_.id + scps + " = " + this.type.tostr(lc) + "\n"
        }
        else -> error("bug found")
    }.let {
        if (!lc) it else {
            this.tk.lincol(it)
        }
    }
}
