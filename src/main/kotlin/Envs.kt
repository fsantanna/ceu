// s = Stmt.Var (var), Type (arg/ret), Block (@xxx)

fun Any.getEnv (): Any? {
    return when (this) {
        is Type -> this.wenv
        is Expr -> this.wenv
        is Stmt -> this.wenv
        else -> error("bug found")
    }
}

fun Any.toType (): Type {
    return when (this) {
        is Type         -> this
        is Stmt.Var     -> this.xtype!!
        is Stmt.Typedef -> this.type
        else -> error("bug found")
    }
}

fun Any.env_all (): List<Any> {
    return this.getEnv()?.let { listOf(it) + it.env_all() } ?: emptyList()
}

fun Any.env_first (f: (Any)->Boolean): Any? {
    fun aux (env: Any?): Any? {
        return when {
            (env == null) -> null
            f(env) -> env
            else -> aux(env.getEnv())
        }
    }
    return aux (this.getEnv())
}

fun <T> Any.env_first_map (f: (Any)->T): T? {
    fun aux (env: Any?): T? {
        return if (env == null) {
            null
        } else {
            val v = f(env)
            if (v != null) {
                v
            } else {
                aux(env.getEnv())
            }
        }
    }
    return aux (this.getEnv())
}

fun Any.env (id: String): Any? {
    // keep types apart from var/block
    val xid = if (id.istype()) id else id.toUpperCase()
    return this.env_first_map {
        when {
            (it is Stmt.Typedef && it.tk_.id==xid)                  -> it
            (it is Stmt.Var     && it.tk_.id.toUpperCase()==xid)    -> it
            (it is Stmt.Block   && it.scp1?.id?.toUpperCase()==xid) -> it
            (it is Expr.Func) -> {
                fun Type.nonat_ (): Type? {
                    return if (this is Type.Nat && this.tk_.src=="") null else this
                }
                when {
                    (it.ftp() == null) -> if (id in listOf("arg","pub","ret","evt")) true else null
                    (id == "arg") -> it.ftp()!!.inp.nonat_()
                    (id == "pub") -> it.ftp()!!.pub!!.nonat_()
                    (id == "ret") -> it.ftp()!!.out.nonat_()
                    (id == "evt") -> Type.Alias (
                        Tk.Id(TK.XID, it.tk.lin, it.tk.col, "Event"),
                        false,
                        emptyList()
                    ).clone(it, it.tk.lin, it.tk.col).nonat_()
                    else  -> null
                }
            }
            else -> null
        }
    }
}

//////////////////////////////////////////////////////////////////////////////

fun Stmt.setEnvs (env: Any?): Any? {
    this.wenv = env
    fun ft (tp: Type) { // recursive typedef
        tp.wenv = if (this is Stmt.Typedef) this else env
        when (tp) {
            is Type.Alias -> {
                tp.xisrec = tp.env(tp.tk_.id)?.toType()?.let {
                    it.flattenLeft().any { it is Type.Alias && it.tk_.id==tp.tk_.id }
                } ?: false
            }
        }
    }
    fun fe (e: Expr) {
        e.wenv = env
        when (e) {
            is Expr.Func -> e.block.setEnvs(e)
        }
    }
    return when (this) {
        is Stmt.Nop, is Stmt.Native, is Stmt.Return, is Stmt.Break, is Stmt.Throw -> env
        is Stmt.Var    -> { this.xtype?.visit(::ft,null) ; this }
        is Stmt.Set    -> { this.dst.visit(null,::fe,::ft,null) ; this.src.visit(null,::fe,::ft,null) ; env }
        is Stmt.SCall  -> { this.e.visit(null,::fe,::ft,null) ; env }
        is Stmt.SSpawn -> { this.dst?.visit(null,::fe,::ft,null) ; this.call.visit(null,::fe,::ft,null) ; env }
        is Stmt.DSpawn -> { this.dst.visit(null,::fe,::ft,null) ; this.call.visit(null,::fe,::ft,null) ; env }
        is Stmt.Await  -> { this.e.visit(null,::fe,::ft,null) ; env }
        is Stmt.Pause  -> { this.tsk.visit(null,::fe,::ft,null) ; env }
        is Stmt.Emit  -> {
            if (this.tgt is Expr) {
                this.tgt.visit(null,::fe,::ft,null)
            }
            this.e.visit(null,::fe,::ft,null)
            env
        }
        is Stmt.Input  -> { this.dst?.visit(null,::fe,::ft,null) ; this.arg.visit(null,::fe,::ft,null) ; this.xtype?.visit(::ft,null) ; env }
        is Stmt.Output -> { this.arg.visit(null,::fe,::ft,null) ; env }
        is Stmt.Seq -> {
            val e1 = this.s1.setEnvs(env)
            val e2 = this.s2.setEnvs(e1)
            e2
        }
        is Stmt.If -> {
            this.tst.visit(null,::fe,::ft,null)
            this.true_.setEnvs(env)
            this.false_.setEnvs(env)
            env
        }
        is Stmt.Loop  -> { this.block.setEnvs(env) ; env }
        is Stmt.DLoop -> { this.i.visit(null,::fe,::ft,null) ; this.tsks.visit(null,::fe,::ft,null) ; this.block.setEnvs(env) ; env }
        is Stmt.Block -> {
            this.body.setEnvs(this) // also include blocks w/o labels b/c of inference
            env
        }
        is Stmt.Typedef -> { this.type.visit(::ft,null) ; this }
        else -> TODO(this.toString()) // do not remove this line b/c we may add new cases
    }
}
