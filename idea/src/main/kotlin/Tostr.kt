fun Tk.lincol (src: String): String {
    if (true) {
        //return src
    }
    return "^[${this.lin},${this.col}]$src^[]"
    /*
    return """
        
        ^[${this.lin},${this.col}]
        $src
        ^[]
        
    """.trimIndent()
    */
}

fun Type.tostr (lc: Boolean = false, ispak: Boolean = false): String {
    fun List<Tk>?.idx (i: Int, c: Char): String {
        return if (this == null) "" else this[i].str+c
    }
    return when (this) {
        is Type.Unit    -> "()"
        is Type.Nat     -> this.tk.str
        is Type.Par     -> this.tk.str
        is Type.Pointer -> "/" + this.pln.tostr(lc) + this.xscp.let { if (it==null) "" else " @" + it.scp1.str.anon2local() }
        is Type.Named   -> {
            val pak_subs = if (!ispak) "" else {
                this.subs.map { '.' + it.str }.joinToString("")
            }
            val args = if (this.xargs == null) "" else {
                " $D{" + this.xargs!!.map { it.tostr(lc) }.joinToString(",") + "}"
            }
            val scps = this.xscps.let { if (it==null) "" else it.let {
                if (it.size == 0) "" else " @{" + it.map { it.scp1.str.anon2local() }.joinToString(",") + "}"
            }}
            this.tk.str + pak_subs + args + scps
        }
        is Type.Tuple   -> "[" + this.vec.mapIndexed { i,v -> this.yids.idx(i,':') + v.tostr(lc) }.joinToString(",") + "]"
        is Type.Union   -> {
            val common = if (this.common == null) "" else (this.common.tostr(lc) + " ")
            common + "<" + this.vec.mapIndexed { i,v -> this.yids.idx(i,'=') + v.tostr(lc) }.joinToString(",") + ">"
        }
        is Type.Active  -> "active " + this.tsk.tostr(lc)
        is Type.Actives -> "active {${this.len?.str ?: ""}} " + this.tsk.tostr(lc)
        is Type.Func    -> {
            val ctrs = this.xscps.third.let {
                if (it == null || it.isEmpty()) "" else ": " + it.map { it.first + ">" + it.second }
                    .joinToString(",")
            }
            val scps = this.xscps.second.let { if (it==null) "" else "@{" + it.map { it.scp1.str.anon2local() }.joinToString(",") + ctrs + "} -> " }
            this.tk.str + " " + scps + this.inp.tostr(lc) + " -> " + this.pub.let { if (it == null) "" else it.tostr(lc) + " -> " } + this.out.tostr(lc)
        }
    }.let {
        if (!lc) it else {
            this.tk.lincol(it)
        }
    }
}

fun Expr.tostr (lc: Boolean = false, pakhassubs: Boolean = false): String {
    // lc: lin/col
    /*
    fun Type.pak (): String { // remove subs from Type.Named
        return (if (this is Type.Active) "active " else "") + (this.noact() as Type.Named).tk.str
    }
     */
    return when (this) {
        is Expr.Unit  -> "()"
        is Expr.Var   -> this.tk.str
        is Expr.Nat   -> if (this.xtype==null) this.tk.str else "(" + this.tk.str + ": " + this.xtype!!.tostr(lc) + ")"
        is Expr.Cast  -> this.e.tostr(lc) + " :: " + this.type.tostr(lc)
        is Expr.Pak   -> {
            val hassubs = this.xtype?.noact().let { it!=null && ((it as Type.Named).subs.size > 0) }
            if (this.xtype==null) this.e.tostr(lc) else ("(" + this.xtype!!.tostr(lc,true) + " " + this.e.tostr(lc,hassubs) + ")")
        }
        is Expr.Unpak -> this.e.wtype.let {
            if (it==null || it.noact() !is Type.Named) {
                this.e.tostr(lc)
            } else {
                ("(" + this.e.tostr(lc) + "~" + "" + ")")
            }
        }
        is Expr.Upref -> "(/" + this.pln.tostr(lc) + ")"
        is Expr.Dnref -> "(" + this.ptr.tostr(lc) + "\\)"
        is Expr.TCons -> "[" + this.arg.map { it.tostr(lc) }.joinToString(",") + "]"
        is Expr.UCons -> {
            if (pakhassubs) this.arg.tostr(lc,pakhassubs) else {
                "<." + this.tk.str + " " + this.arg.tostr(lc) + ">" + this.wtype.let {
                    if (it == null) "" else ": " + it.tostr(lc)
                }
            }
        }
        is Expr.UNull -> "Null" + this.wtype.let { if (it==null) "" else ": "+it.tostr(lc) }
        is Expr.TDisc -> "(" + this.tup.tostr(lc) + "." + this.tk.str + ")"
        is Expr.Field -> "(" + this.tsk.tostr(lc) + ".${this.tk.str})"
        is Expr.UDisc -> "(" + this.uni.tostr(lc) + "!" + this.tk.str + ")"
        is Expr.UPred -> "(" + this.uni.tostr(lc) + "?" + this.tk.str + ")"
        is Expr.New -> "(new " + this.arg.tostr(lc) + this.xscp.let { if (it==null) "" else ": @" + this.xscp!!.scp1.str.anon2local() } + ")"
        is Expr.Call -> {
            val inps = this.xscps.first.let { if (it==null) "" else " @{" + it.map { it.scp1.str.anon2local() }.joinToString(",") + "}" }
            val out = this.xscps.second.let { if (it == null) "" else ": @" + it.scp1.str.anon2local() }
            "(" + this.f.tostr(lc) + inps + " " + this.arg.tostr(lc) + out + ")"
        }
        is Expr.Func -> {
            "("+ (this.ftp()?.tostr(lc) ?: "null") + " " + this.block.tostr(lc) + ")"
        }
        is Expr.If   -> "(if "+ this.tst.tostr(lc) + " { " + this.true_.tostr(lc) + "}  else { " + this.false_.tostr(lc) + " })"
    }.let {
        if (!lc) it else {
            this.tk.lincol(it)
        }
    }
}

fun Stmt.tostr (lc: Boolean = false): String {
    return when (this) {
        is Stmt.Nop -> "\n"
        is Stmt.Native -> "native " + (if (this.istype) "type " else "") + this.tk.str + "\n"
        is Stmt.Var -> "var " + this.tk.str + this.xtype.let { if (it==null) " var" else (": "+it.tostr()) } + "\n"
        is Stmt.Set -> "set " + this.dst.tostr(lc) + " = " + this.src.tostr(lc) + "\n"
        is Stmt.XBreak -> "break\n"
        is Stmt.XReturn -> "return\n"
        is Stmt.Seq -> this.s1.tostr(lc) + this.s2.tostr(lc)
        is Stmt.SCall -> "call " + this.e.tostr(lc) + "\n"
        is Stmt.Input -> (if (this.dst == null) "" else "set " + this.dst.tostr(lc) + " = ") +
            "input " + this.arg.tostr(lc) + this.xtype.let { if (it==null) "" else ": " + it.tostr(lc) } +
            "\n"
        is Stmt.Output -> "output " + this.arg.tostr(lc) + "\n"
        is Stmt.If -> "if " + this.tst.tostr(lc) + "\n" + this.true_.tostr(lc) + "else\n" + this.false_.tostr(lc)
        is Stmt.Loop -> "loop " + this.block.tostr(lc)
        is Stmt.Block -> {
            val catch = this.catch.let {
                // do not generate explicit catch from auto-generated implicit CEU_ERROR_ESCAPE
                if (it==null || (it is Expr.Nat && it.tk.str.contains("CEU_ERROR_ESCAPE"))) "" else {
                    "catch " + it.tostr(lc) + " "
                }
            }
            catch + "{" + (if (this.scp1?.str.isanon()) "" else " @" + this.scp1!!.str) + "\n" + this.body.tostr(lc) + "}\n"
        }
        is Stmt.SSpawn -> (if (this.dst == null) "" else "set " + this.dst.tostr(lc) + " = ") + "spawn " + this.call.tostr(lc) + "\n"
        is Stmt.DSpawn -> "spawn " + this.call.tostr(lc) + " in " + this.dst.tostr(lc) + "\n"
        is Stmt.Await -> "await " + this.e.tostr(lc) + "\n"
        is Stmt.Pause -> (if (this.pause) "pause " else "resume ") + this.tsk.tostr(lc) + "\n"
        is Stmt.Emit -> when (this.tgt) {
            is Scope -> "emit @" + this.tgt.scp1.str.anon2local() + " " + this.e.tostr(lc) + "\n"
            is Expr  -> "emit " + this.tgt.tostr(lc) + " " + this.e.tostr(lc) + "\n"
            else -> error("bug found")
        }
        is Stmt.Throw -> "throw " + this.e.tostr(lc) + "\n"
        is Stmt.DLoop -> "loop " + this.i.tostr(lc) + " in " + this.tsks.tostr(lc) + " " + this.block.tostr(lc)
        is Stmt.Typedef -> {
            val scps = this.xscp1s.first.let { if (it == null) "" else " @{" + this.xscp1s.first!!.map { it.str }.joinToString(",") + "}" }
            val op = if (this.isinc) " += " else " = "
            "type " + this.tk.str + scps + op + this.type.tostr(lc) + "\n"
        }
        else -> error("bug found")
    }.let {
        if (!lc) it else {
            this.tk.lincol(it)
        }
    }
}
