fun Type.tostr(): String {
    return Tostr().tostr(this)
}

fun Expr.tostr(): String {
    return Tostr().tostr(this)
}

fun Stmt.tostr(): String {
    return Tostr().tostr(this)
}

open class Tostr
{
    open fun tostr (tp: Type): String {
        return when (tp) {
            is Type.Unit -> "()"
            is Type.Nat -> tp.tk_.toce()
            is Type.Pointer -> tp.xscp!!.let { "/" + this.tostr(tp.pln) + " @" + it.scp1.anon2local() }
            is Type.Tuple -> "[" + tp.vec.map { this.tostr(it) }.joinToString(",") + "]"
            is Type.Union -> "<" + tp.vec.map { this.tostr(it) }.joinToString(",") + ">"
            is Type.Active -> "active " + this.tostr(tp.tsk)
            is Type.Actives -> "active {${tp.len?.num ?: ""}} " + this.tostr(tp.tsk)
            is Type.Alias -> tp.tk_.id + tp.xscps!!.let {
                if (it.size == 0) "" else " @[" + it.map { it.scp1.anon2local() }.joinToString(",") + "]"
            }
            is Type.Func -> {
                val ctrs = tp.xscps.third.let {
                    if (it == null || it.isEmpty()) "" else ": " + it.map { it.first + ">" + it.second }
                        .joinToString(",")
                }
                val scps = " @[" + tp.xscps.second!!.map { it.scp1.anon2local() }.joinToString(",") + ctrs + "]"
                tp.tk_.key + scps + " -> " + this.tostr(tp.inp) + " -> " + tp.pub.let { if (it == null) "" else this.tostr(it) + " -> " } + this.tostr(tp.out)
            }
        }
    }

    fun upcast (e: Expr, v: String): String {
        return if (e.wtype !is Type.Alias) v else {
            "(" + v + ":+ " + this.tostr(e.wtype!!) + ")"
        }
    }
    fun dncast (dn: Type?, v: String): String {
        return if (dn !is Type.Alias) v else {
            "(" + v + ":- " + this.tostr(dn) + ")"
        }
    }

    open fun tostr (e: Expr): String {
        return when (e) {
            is Expr.Unit -> this.upcast(e, "()")
            is Expr.Var -> e.tk_.id
            is Expr.Nat -> "(" + e.tk_.toce() + ": " + this.tostr(e.xtype!!) + ")"
            is Expr.As  -> if (e.xtype==null) this.tostr(e.e) else ("(" + this.tostr(e.e) + " " + e.tk_.sym + " " + this.tostr(e.xtype!!) + ")")
            is Expr.Upref -> "(/" + this.tostr(e.pln) + ")"
            is Expr.Dnref -> "(" + this.tostr(e.ptr) + "\\)"
            is Expr.TCons -> "[" + e.arg.map { this.tostr(it) }.joinToString(",") + "]"
            is Expr.UCons -> "<." + e.tk_.num + " " + this.tostr(e.arg) + ">: " + this.tostr(e.wtype!!)
            is Expr.UNull -> "<.0>: " + this.tostr(e.xtype!!)
            is Expr.TDisc -> "(" + this.tostr(e.tup) + "." + e.tk_.num + ")"
            is Expr.Field -> {
                val tsk = this.dncast(e.tsk.wtype!!.noact(), this.tostr(e.tsk))
                "(" + tsk + ".${e.tk_.id})"
            }
            is Expr.UDisc -> {
                val uni = this.tostr(e.uni).let {
                    if (e.tk_.num == 0) it else this.dncast(e.uni.wtype, it)
                }
                "(" + uni + "!" + e.tk_.num + ")"
            }
            is Expr.UPred -> {
                val uni = this.tostr(e.uni).let {
                    if (e.tk_.num == 0) it else this.dncast(e.uni.wtype, it)
                }
                "(" + uni + "?" + e.tk_.num + ")"
            }
            is Expr.New -> "(new " + this.tostr(e.arg) + ": @" + e.xscp!!.scp1.anon2local() + ")"
            is Expr.Call -> {
                val inps = " @[" + e.xscps.first!!.map { it.scp1.anon2local() }.joinToString(",") + "]"
                val out = e.xscps.second.let { if (it == null) "" else ": @" + it.scp1.anon2local() }
                "(" + this.tostr(e.f) + inps + " " + this.tostr(e.arg) + out + ")"
            }
            is Expr.Func -> this.tostr(e.ftp()!!) + " " + this.tostr(e.block)
        }
    }

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
