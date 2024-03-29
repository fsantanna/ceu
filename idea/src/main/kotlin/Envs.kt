// s = Stmt.Var (var), Type (arg/ret), Block (@xxx)

fun Any.getEnv (): Any? {
    return when (this) {
        is Type -> this.wenv
        is Expr -> this.wenv
        is Stmt -> this.wenv
        else -> error("bug found")
    }
}

fun Any.getTk (): Tk {
    return when (this) {
        is Type -> this.tk
        is Expr -> this.tk
        is Stmt -> this.tk
        else -> error("bug found")
    }
}

fun Any.getType (): Type {
    return when (this) {
        is Type         -> this
        is Stmt.Var     -> this.xtype!!
        is Stmt.Typedef -> this.xtype ?: this.type
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

fun Type.nonat_ (): Type? {
    return if (this is Type.Nat && this.tk.str=="_") null else this
}
fun Any.env (id: String): Any? {
    return this.env_first_map {
        when {
            (it is Stmt.Typedef && it.tk.str==id) -> it
            (it is Stmt.Var     && it.tk.str==id) -> {
                val fst = it.env(id)    // look first (parametric) declaration
                fst ?: it
            }
            (it is Stmt.Var     && it.tk.str.toUpperCase()==id) -> it.ups_first_block()
            (it is Stmt.Block   && (it.scp1?.str==id || "B"+it.n==id)) -> it
            (it is Expr.Func) -> {
                when {
                    (it.ftp() == null) -> if (id in listOf("arg","pub","ret","evt","err")) true else null
                    (id == "arg") -> it.ftp()!!.inp.nonat_()
                    (id == "pub") -> it.ftp()!!.pub!!.nonat_()
                    (id == "ret") -> it.ftp()!!.out.nonat_()
                    (id == "evt") -> Type.Named (
                        Tk.Id("Event", it.tk.lin, it.tk.col),
                        emptyList(),
                        false,
                        emptyList(),
                        emptyList(),
                        null
                    ).clone(it.tk,it).nonat_()
                    (id == "err") -> Type.Named (
                        Tk.Id("Error", it.tk.lin, it.tk.col),
                        emptyList(),
                        false,
                        emptyList(),
                        emptyList(),
                        null
                    ).clone(it.tk,it).nonat_()
                    else  -> null
                }
            }
            else -> null
        }
    }
}
fun Any.envs (id: String): List<Stmt.Var> {
    return this.env_all()
        .filter { it is Stmt.Var && it.tk.str==id }
        .let { it as List<Stmt.Var> }
}

//////////////////////////////////////////////////////////////////////////////

fun Stmt.setEnvs (env: Any?): Any? {
    this.wenv = env
    fun ft (tp: Type) { // recursive typedef
        tp.wenv = if (this is Stmt.Typedef) this else env
        when (tp) {
            is Type.Named -> {
                tp.xisrec = tp.env(tp.tk.str)?.getType()?.let {
                    it.flattenLeft().any { it is Type.Named && it.tk.str==tp.tk.str }
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
        is Stmt.Nop, is Stmt.Native, is Stmt.XReturn, is Stmt.XBreak -> env
        is Stmt.Var    -> { this.xtype?.visit(::ft,null) ; this }
        is Stmt.Set    -> { this.dst.visit(null,::fe,::ft,null) ; this.src.visit(null,::fe,::ft,null) ; env }
        is Stmt.SCall  -> { this.e.visit(null,::fe,::ft,null) ; env }
        is Stmt.SSpawn -> { this.dst?.visit(null,::fe,::ft,null) ; this.call.visit(null,::fe,::ft,null) ; env }
        is Stmt.DSpawn -> { this.dst.visit(null,::fe,::ft,null) ; this.call.visit(null,::fe,::ft,null) ; env }
        is Stmt.Await  -> { this.e.visit(null,::fe,::ft,null) ; env }
        is Stmt.Pause  -> { this.tsk.visit(null,::fe,::ft,null) ; env }
        is Stmt.Throw  -> { this.e.visit(null,::fe,::ft,null) ; env }
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
            this.catch?.visit(null,::fe,::ft,null)
            this.body.setEnvs(this) // also include blocks w/o labels b/c of inference
            env
        }
        is Stmt.Typedef -> { this.type.visit(::ft,null) ; this.xtype?.visit(::ft,null) ; this }
        else -> TODO(this.toString()) // do not remove this line b/c we may add new cases
    }
}
