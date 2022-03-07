open class Parser
{
    fun event (): String {
        return if (alls.accept(TK.XCLK)) {
            "" + (alls.tk0 as Tk.Clk).ms + "ms"
        } else {
            this.expr().tostr(true)
        }
    }

    open fun type (): Type {
        return when {
            alls.accept(TK.XID) -> {
                val tk0 = alls.tk0.astype()
                val scps = if (alls.accept(TK.ATBRACK)) {
                    val ret = this.scp1s { it.asscope() }
                    alls.accept_err(TK.CHAR, ']')
                    ret
                } else {
                    null
                }
                Type.Alias(tk0, false, scps?.map { Scope(it,null) })
            }
            alls.accept(TK.CHAR, '/') -> {
                val tk0 = alls.tk0 as Tk.Chr
                val pln = this.type()
                val scp = if (alls.accept(TK.CHAR, '@')) {
                    alls.accept_err(TK.XID)
                    alls.tk0.asscope()
                } else {
                    null
                }
                Type.Pointer(tk0, if (scp==null) null else Scope(scp,null), pln)
            }
            alls.accept(TK.FUNC) || alls.accept(TK.TASK) -> {
                val tk0 = alls.tk0 as Tk.Key

                val (scps, ctrs) = if (alls.check(TK.ATBRACK)) {
                    val (x, y) = this.scopepars()
                    alls.accept_err(TK.ARROW)
                    Pair(x, y)
                } else {
                    Pair(null, null)
                }

                // input & pub & output
                val inp = this.type()
                val pub = if (tk0.enu != TK.FUNC) {
                    alls.accept_err(TK.ARROW)
                    this.type()
                } else null
                alls.accept_err(TK.ARROW)
                val out = this.type() // right associative

                Type.Func(tk0,
                    Triple(
                        Scope(Tk.Id(TK.XID, tk0.lin, tk0.col, "LOCAL"),null),
                        if (scps==null) null else scps.map { Scope(it,null) },
                        ctrs
                    ),
                    inp, pub, out)
            }
            alls.accept(TK.UNIT) -> Type.Unit(alls.tk0 as Tk.Sym)
            alls.accept(TK.XNAT) -> Type.Nat(alls.tk0 as Tk.Nat)
            alls.accept(TK.CHAR, '(') -> {
                val tp = this.type()
                alls.accept_err(TK.CHAR, ')')
                tp
            }
            alls.accept(TK.CHAR, '[') || alls.accept(TK.CHAR, '<') -> {
                val tk0 = alls.tk0 as Tk.Chr
                val tp = this.type()
                val tps = arrayListOf(tp)
                while (true) {
                    if (!alls.accept(TK.CHAR, ',')) {
                        break
                    }
                    val tp2 = this.type()
                    tps.add(tp2)
                }
                if (tk0.chr == '[') {
                    alls.accept_err(TK.CHAR, ']')
                    Type.Tuple(tk0, tps)
                } else {
                    alls.accept_err(TK.CHAR, '>')
                    Type.Union(tk0, tps)
                }
            }
            alls.accept(TK.ACTIVE) -> {
                val tk0 = alls.tk0 as Tk.Key
                val (isdyn,len) = if (!alls.accept(TK.CHAR,'{')) Pair(false,null) else {
                    val len = if (alls.accept(TK.XNUM)) alls.tk0 as Tk.Num else null
                    alls.accept_err(TK.CHAR, '}')
                    Pair(true, len)
                }
                //alls.check_err(TK.TASK)
                val task = this.type()
                //assert(task is Type.Func && task.tk.enu == TK.TASK)
                All_assert_tk(tk0, task is Type.Alias || task is Type.Func && task.tk.enu==TK.TASK) {
                    "invalid type : expected task type"
                }
                if (isdyn) {
                    Type.Actives(tk0, len, task)
                } else {
                    Type.Active(tk0, task)
                }
            }
            else -> {
                alls.err_expected("type")
                error("unreachable")
            }
        }
    }

    open fun expr_one (): Expr {
        return when {
            alls.tk1.istype() -> {
                val id = alls.tk1 as Tk.Id
                val tp = this.type() as Type.Alias
                val e = if (alls.accept(TK.CHAR, '.')) {
                    alls.accept_err(TK.XNUM)
                    val num = alls.tk0 as Tk.Num
                    all().assert_tk(num, num.num>0) {
                        "invalid union constructor : expected positive index"
                    }
                    val cons = if (alls.checkExpr()) this.expr() else {
                        Expr.Unit(Tk.Sym(TK.UNIT, alls.tk1.lin, alls.tk1.col, "()"))
                    }
                    Expr.UCons(num, null, cons)
                } else {
                    val block = this.block()
                    Expr.Func(id, null, block)
                }
                Expr.Pak(Tk.Sym(TK.XAS,id.lin,id.col,":+"), e, tp)
            }
            alls.accept(TK.XNAT) -> {
                val tk0 = alls.tk0 as Tk.Nat
                val tp = if (!alls.accept(TK.CHAR, ':')) null else {
                    this.type()
                }
                Expr.Nat(tk0, tp)
            }
            alls.accept(TK.CHAR, '<') -> {
                val tkx = alls.tk0
                alls.accept_err(TK.CHAR, '.')
                alls.accept_err(TK.XNUM)
                val tk0 = alls.tk0 as Tk.Num
                val cons = when {
                    (tk0.num == 0) -> null
                    alls.checkExpr() -> this.expr()
                    else -> Expr.Unit(Tk.Sym(TK.UNIT, alls.tk1.lin, alls.tk1.col, "()"))
                }
                alls.accept_err(TK.CHAR, '>')
                val tp = if (!alls.accept(TK.CHAR, ':')) null else {
                    val tp = this.type()
                    when (tk0.num) {
                        0 -> all().assert_tk(tp.tk,tp is Type.Pointer && tp.pln is Type.Alias) {
                            "invalid type : expected pointer to alias type"
                        }
                        else -> all().assert_tk(tp.tk,tp is Type.Union) {
                            "invalid type : expected union type"
                        }
                    }
                    tp
                }
                when {
                    (tk0.num == 0) -> Expr.UNull(tk0, tp as Type.Pointer?)
                    (tp != null)   -> Expr.UCons(tk0, tp as Type.Union?, cons!!)
                    else -> {
                        assert(INFER)
                        Expr.Pak (
                            Tk.Sym(TK.XAS,tk0.lin,tk0.col,":+"),
                            Expr.UCons(tk0, tp as Type.Union?, cons!!),
                            null
                        )
                    }
                }
            }
            alls.accept(TK.NEW) -> {
                val tk0 = alls.tk0
                val e = this.expr()
                all().assert_tk(tk0, e is Expr.Pak || (e is Expr.UCons && e.tk_.num!=0)) {
                    "invalid `new` : expected constructor"
                }

                val scp = if (alls.accept(TK.CHAR, ':')) {
                    alls.accept_err(TK.CHAR, '@')
                    alls.accept_err(TK.XID)
                    alls.tk0.asscope()
                } else {
                    null
                }
                Expr.New(tk0 as Tk.Key, if (scp==null) null else Scope(scp,null), e)
            }
            alls.accept(TK.UNIT) -> Expr.Unit(alls.tk0 as Tk.Sym)
            alls.tk1.isvar() && alls.accept(TK.XID) -> Expr.Var(alls.tk0 as Tk.Id)
            alls.accept(TK.CHAR, '/') -> {
                val tk0 = alls.tk0 as Tk.Chr
                val e = this.expr()
                all().assert_tk(
                    alls.tk0,
                    e is Expr.Nat || e is Expr.Var || e is Expr.TDisc || e is Expr.Dnref || e is Expr.Upref
                ) {
                    "unexpected operand to `/´"
                }
                Expr.Upref(tk0, e)
            }
            alls.accept(TK.CHAR, '(') -> {
                val e = this.expr()
                alls.accept_err(TK.CHAR, ')')
                e
            }
            alls.accept(TK.CHAR, '[') -> {
                val tk0 = alls.tk0 as Tk.Chr
                val e = this.expr()
                val es = arrayListOf(e)
                while (true) {
                    if (!alls.accept(TK.CHAR, ',')) {
                        break
                    }
                    val e2 = this.expr()
                    es.add(e2)
                }
                alls.accept_err(TK.CHAR, ']')
                val ret = Expr.TCons(tk0, es)
                if (!INFER) ret else {
                    Expr.Pak(
                        Tk.Sym(TK.XAS, tk0.lin, tk0.col, ":+"),
                        ret,
                null
                    )
                }
            }
            alls.check(TK.TASK) || alls.check(TK.FUNC) -> {
                val tk = alls.tk1 as Tk.Key
                val tp = this.type() as Type.Func
                val block = this.block()
                Expr.Func(tk, tp, block)
            }
            else -> {
                alls.err_expected("expression")
                error("unreachable")
            }
        }
    }

    open fun expr (): Expr {
        var e = this.expr_dots()
        // call
        if (alls.checkExpr() || alls.check(TK.ATBRACK)) {
            val iscps = if (!alls.accept(TK.ATBRACK)) null else {
                val ret = this.scp1s { it.asscope() }
                alls.accept_err(TK.CHAR, ']')
                ret
            }
            val arg = this.expr()
            val oscp = if (!alls.accept(TK.CHAR, ':')) null else {
                alls.accept_err(TK.CHAR, '@')
                alls.accept_err(TK.XID)
                alls.tk0.asscope()
            }
            e = Expr.Call(e.tk,
                if (e is Expr.Unpak || !INFER) e else {
                    Expr.Unpak(Tk.Sym(TK.XAS,e.tk.lin,e.tk.col,":-"), e)
                },
                arg,
                Pair(
                    if (iscps==null) null else iscps.map { Scope(it,null) },
                    if (oscp==null) null else Scope(oscp,null)
                ))
        }
        return e
    }

    open fun stmt (): Stmt {
        return when {
            alls.accept(TK.SET) -> {
                val dst = this.attr().toExpr()
                alls.accept_err(TK.CHAR, '=')
                val tk0 = alls.tk0 as Tk.Chr
                when {
                    alls.accept(TK.AWAIT) -> {
                        val e = this.expr()
                        All_assert_tk(e.tk, e is Expr.Call) { "expected task call" }
                        All_nest(
                            """
                        {
                            var tsk_$N = spawn ${e.tostr(true)}
                            var st_$N = tsk_$N.state
                            if _(${D}st_$N == TASK_AWAITING) {
                                await tsk_$N
                            }
                            set ${dst.tostr(true)} = tsk_$N.ret
                        }
                    """.trimIndent()
                        ) {
                            this.stmts()
                        } as Stmt
                    }
                    alls.accept(TK.INPUT) -> {
                        val tk = alls.tk0 as Tk.Key
                        alls.accept_err(TK.XID)
                        val lib = (alls.tk0 as Tk.Id)
                        val arg = this.expr()
                        alls.accept_err(TK.CHAR, ':')
                        val tp = this.type()
                        Stmt.Input(tk, tp, dst, lib, arg)
                    }
                    alls.check(TK.SPAWN) -> {
                        val s = this.stmt()
                        All_assert_tk(s.tk, s is Stmt.SSpawn) { "unexpected dynamic `spawn`" }
                        val ss = s as Stmt.SSpawn
                        Stmt.SSpawn(ss.tk_, dst, ss.call)
                    }
                    else -> {
                        val src = this.expr()
                        Stmt.Set(tk0, dst, src)
                    }
                }
            }
            alls.accept(TK.VAR) -> {
                alls.accept_err(TK.XID)
                val tk_id = alls.tk0 as Tk.Id
                val tp = if (!alls.accept(TK.CHAR, ':')) null else {
                    this.type()
                }
                if (!alls.accept(TK.CHAR, '=')) {
                    if (tp == null) {
                        alls.err_expected("type declaration")
                    }
                    Stmt.Var(tk_id, tp)
                } else if (alls.accept(TK.VAR)) {
                    Stmt.Var(tk_id, null)
                } else {
                    fun tpor(inf: String): String? {
                        return if (tp == null) inf else null
                    }
                    val tk0 = alls.tk0 as Tk.Chr
                    val dst = Expr.Var(tk_id)
                    val src = when {
                        alls.accept(TK.INPUT) -> {
                            val tk = alls.tk0 as Tk.Key
                            alls.accept_err(TK.XID)
                            val lib = (alls.tk0 as Tk.Id)
                            val arg = this.expr()
                            val inp = if (!alls.accept(TK.CHAR, ':')) null else this.type()
                            Stmt.Input(tk, inp, dst, lib, arg)
                        }
                        alls.check(TK.SPAWN) -> {
                            val s = this.stmt()
                            All_assert_tk(s.tk, s is Stmt.SSpawn) { "unexpected dynamic `spawn`" }
                            val ss = s as Stmt.SSpawn
                            Stmt.SSpawn(ss.tk_, dst, ss.call)
                        }
                        alls.accept(TK.AWAIT) -> {
                            val e = this.expr()
                            All_assert_tk(e.tk, e is Expr.Call) { "expected task call" }
                            val ret = All_nest(
                                """
                                {
                                    var tsk_$N = spawn ${e.tostr(true)}
                                    var st_$N = tsk_$N.state
                                    if _(${D}st_$N == TASK_AWAITING) {
                                        await tsk_$N
                                    }
                                    set ${tk_id.id} = tsk_$N.ret
                                }
                            """.trimIndent()
                            ) {
                                this.stmts()
                            }
                            ret as Stmt
                        }
                        else -> {
                            val src = this.expr()
                            Stmt.Set(tk0, dst, src)
                        }
                    }
                    Stmt.Seq(tk_id, Stmt.Var(tk_id, tp), src)
                }
            }
            alls.accept(TK.INPUT) -> {
                val tk = alls.tk0 as Tk.Key
                alls.accept_err(TK.XID)
                val lib = (alls.tk0 as Tk.Id)
                val arg = this.expr()
                val tp = if (!alls.accept(TK.CHAR, ':')) null else this.type()
                Stmt.Input(tk, tp, null, lib, arg)
            }
            alls.accept(TK.IF) -> {
                val tk0 = alls.tk0 as Tk.Key
                val tst = this.expr()
                val true_ = this.block()
                val false_ = if (alls.accept(TK.ELSE)) {
                    this.block()
                } else {
                    Stmt.Block(Tk.Chr(TK.CHAR, alls.tk1.lin, alls.tk1.col, '{'), false, null, Stmt.Nop(alls.tk0))
                }
                Stmt.If(tk0, tst, true_, false_)
            }
            alls.accept(TK.RETURN) -> {
                if (!alls.checkExpr()) {
                    Stmt.Return(alls.tk0 as Tk.Key)
                } else {
                    val tk0 = alls.tk0
                    val e = this.expr()
                    //println(e.tostr(true))
                    All_nest(
                        tk0.lincol(
                            """
                        set ret = ${e.tostr(true)}
                        return
                        
                    """.trimIndent()
                        )
                    ) {
                        this.stmts()
                    } as Stmt
                }
            }
            alls.accept(TK.TYPE) -> {
                alls.accept_err(TK.XID)
                val id = alls.tk0.astype()
                val scps = if (alls.check(TK.ATBRACK)) this.scopepars() else Pair(null, null)
                alls.accept_err(TK.CHAR, '=')
                val tp = this.type()
                Stmt.Typedef(id, scps, tp)
            }
            alls.accept(TK.NATIVE) -> {
                val istype = alls.accept(TK.TYPE)
                alls.accept_err(TK.XNAT)
                Stmt.Native(alls.tk0 as Tk.Nat, istype)
            }
            alls.accept(TK.CALL) -> {
                val tk0 = alls.tk0 as Tk.Key
                val e = this.expr()
                All_assert_tk(tk0, e is Expr.Call) { "expected call expression" }
                Stmt.SCall(tk0, e as Expr.Call)
            }
            alls.accept(TK.SPAWN) -> {
                val tk0 = alls.tk0 as Tk.Key
                if (alls.check(TK.CHAR, '{')) {
                    val block = this.block()
                    All_nest("spawn (task _ -> _ -> _ ${block.tostr(true)}) ()") {
                        this.stmt()
                    } as Stmt
                } else {
                    val e = this.expr()
                    All_assert_tk(tk0, e.unpak() is Expr.Call) { "expected call expression" }
                    if (alls.accept(TK.IN)) {
                        val tsks = this.expr()
                        Stmt.DSpawn(tk0, tsks, e)
                    } else {
                        Stmt.SSpawn(tk0, null, e)
                    }
                }
            }
            alls.accept(TK.PAUSEIF) -> {
                val pred = this.expr() as Expr.UPred
                val blk = this.block()
                All_nest(
                    """
                    {
                        var tsk_$N = spawn ${blk.tostr(true)}
                        watching tsk_$N {
                            loop {
                                await ${pred.tostr(true)}
                                var x_$N = ${pred.uni.tostr(true)}!${pred.tk_.num}
                                if x_$N {
                                    pause tsk_$N
                                } else {
                                    resume tsk_$N
                                }
                            }
                        }
                    }
                    
                """.trimIndent()
                ) {
                    this.stmt()
                } as Stmt
            }
            alls.accept(TK.PAR) -> {
                val pars = mutableListOf<Stmt.Block>()
                pars.add(this.block())
                while (alls.accept(TK.WITH)) {
                    pars.add(this.block())
                }
                val srcs = pars.map { "spawn ${it.tostr(true)}" }.joinToString("\n")
                All_nest(srcs + "await _0\n") {
                    this.stmts()
                } as Stmt
            }
            alls.accept(TK.PARAND) || alls.accept(TK.PAROR) -> {
                val op = if (alls.tk0.enu == TK.PARAND) "&&" else "||"
                val pars = mutableListOf<Stmt.Block>()
                pars.add(this.block())
                while (alls.accept(TK.WITH)) {
                    pars.add(this.block())
                }
                val spws =
                    pars.mapIndexed { i, x -> "var tk_${N}_$i = spawn { ${x.body.tostr(true)} }" }.joinToString("\n")
                val oks =
                    pars.mapIndexed { i, _ -> "var ok_${N}_$i: _int = _((${D}tk_${N}_$i->task0.state == TASK_DEAD))" }
                        .joinToString("\n")
                val sets =
                    pars.mapIndexed { i, _ -> "set ok_${N}_$i = _(${D}ok_${N}_$i || (((uint64_t)${D}tk_${N}_$i)==${D}tk_$N))" }
                        .joinToString("\n")
                val chks = pars.mapIndexed { i, _ -> "${D}ok_${N}_$i" }.joinToString(" $op ")

                All_nest(
                    """
                    {
                        $spws
                        $oks
                        loop {
                            if _($chks) {
                                break
                            }
                            await evt?2
                            var tk_$N = evt!2
                            $sets
                        }
                    }

                """.trimIndent()
                ) {
                    this.stmt()
                } as Stmt
            }
            alls.accept(TK.EVERY) -> {
                val evt = this.event()
                val blk = this.block()
                All_nest(
                    """
                    loop {
                        await $evt
                        ${blk.tostr(true)}
                    }
                    
                """.trimIndent()
                ) {
                    this.stmt()
                } as Stmt
            }
            alls.accept(TK.WATCHING) -> {
                val evt = this.event()
                val blk = this.block()
                All_nest(
                    """
                    paror {
                        await $evt
                    } with
                        ${blk.tostr(true)}
                    
                """.trimIndent()
                ) {
                    this.stmt()
                } as Stmt
            }
            alls.accept(TK.PAUSE) -> {
                val tk0 = alls.tk0 as Tk.Key
                val e = this.expr()
                Stmt.Pause(tk0, e, true)
            }
            alls.accept(TK.RESUME) -> {
                val tk0 = alls.tk0 as Tk.Key
                val e = this.expr()
                Stmt.Pause(tk0, e, false)
            }
            alls.accept(TK.AWAIT) -> {
                val tk0 = alls.tk0 as Tk.Key
                when {
                    alls.accept(TK.XCLK) -> {
                        val clk = alls.tk0 as Tk.Clk
                        All_nest(
                            """
                            {
                                var ms_$N: _int = _${clk.ms}
                                loop {
                                    await evt?5
                                    set ms_$N = sub [ms_$N, evt!5]
                                    if lte [ms_$N,_0] {
                                        break
                                    }
                                }
                            }
                        """.trimIndent()
                        ) {
                            this.stmt()
                        } as Stmt
                    }
                    else -> {
                        val e = this.expr()
                        Stmt.Await(tk0, e)
                    }
                }
            }
            alls.accept(TK.EMIT) -> {
                val tk0 = alls.tk0 as Tk.Key
                val tgt = if (alls.accept(TK.CHAR, '@')) {
                    alls.accept_err(TK.XID)
                    Scope(alls.tk0.asscope(), null)
                } else {
                    this.expr_dots()
                }
                val e = this.expr()
                Stmt.Emit(tk0, tgt, e)
            }
            alls.accept(TK.THROW) -> {
                Stmt.Throw(alls.tk0 as Tk.Key)
            }
            alls.accept(TK.LOOP) -> {
                val tk0 = alls.tk0 as Tk.Key
                if (alls.check(TK.CHAR, '{')) {
                    val block = this.block()
                    // add additional block to break out w/ goto and cleanup
                    Stmt.Block(
                        block.tk_, false, null,
                        Stmt.Loop(tk0, block)
                    )
                } else {
                    val i = this.expr()
                    All_assert_tk(alls.tk0, i is Expr.Var) {
                        "expected variable expression"
                    }
                    alls.accept_err(TK.IN)
                    val tsks = this.expr()
                    val block = this.block()
                    // add additional block to break out w/ goto and cleanup
                    Stmt.Block(
                        block.tk_, false, null,
                        Stmt.DLoop(tk0, i as Expr.Var, tsks, block)
                    )
                }
            }
            alls.accept(TK.BREAK) -> Stmt.Break(alls.tk0 as Tk.Key)
            alls.accept(TK.CATCH) || alls.check(TK.CHAR, '{') -> this.block()
            alls.accept(TK.OUTPUT) -> {
                val tk = alls.tk0 as Tk.Key
                alls.accept_err(TK.XID)
                val lib = (alls.tk0 as Tk.Id)
                val arg = this.expr()
                Stmt.Output(tk, lib, arg)
            }
            else -> {
                alls.err_expected("statement")
                error("unreachable")
            }
        }.let { it1 ->
            val it2 = if (!alls.check(TK.WHERE)) it1 else this.where(it1)
            val it3 = if (!alls.accept(TK.UNTIL)) it2 else {
                //val tk0 = alls.tk0
                //All_assert_tk(tk0, stmt !is Stmt.Var) { "unexpected `until`" }

                val cnd = this.expr()
                val if1 = All_nest(
                    """
                    if ${cnd.tostr(true)} {
                        break
                    }
                    
                """.trimIndent()
                ) {
                    this.stmt()
                } as Stmt
                val if2 = if (!alls.check(TK.WHERE)) if1 else this.where(if1)

                All_nest(
                    """
                    loop {
                        ${it2.tostr(true)}
                        ${if2.tostr(true)}
                    }
                    
                """.trimIndent()
                ) {
                    this.stmt()
                } as Stmt
            }
            //println(it3.tostr(true))
            it3
        }
    }

    fun scp1s (f: (Tk) -> Tk.Id): List<Tk.Id> {
        val scps = mutableListOf<Tk.Id>()
        while (alls.accept(TK.XID)) {
            scps.add(f(alls.tk0))
            if (!alls.accept(TK.CHAR, ',')) {
                break
            }
        }
        return scps
    }

    fun scopepars (): Pair<List<Tk.Id>, List<Pair<String, String>>> {
        alls.accept_err(TK.ATBRACK)
        val scps = this.scp1s { it.asscopepar() }
        val ctrs = mutableListOf<Pair<String, String>>()
        if (alls.accept(TK.CHAR, ':')) {
            while (alls.accept(TK.XID)) {
                val id1 = alls.tk0.asscopepar().id
                alls.accept_err(TK.CHAR, '>')
                alls.accept_err(TK.XID)
                val id2 = alls.tk0.asscopepar().id
                ctrs.add(Pair(id1, id2))
                if (!alls.accept(TK.CHAR, ',')) {
                    break
                }
            }
        }
        alls.accept_err(TK.CHAR, ']')
        return Pair(scps, ctrs)
    }

    fun expr_as (e: Expr): Expr {
        return if (!alls.accept(TK.XAS)) e else {
            val tk0 = alls.tk0 as Tk.Sym
            if (tk0.sym == ":+") {
                val type = this.type()
                All_assert_tk(alls.tk0, type is Type.Alias) {
                    "expected alias type"
                }
                Expr.Pak(tk0, e, type as Type.Alias)
            } else {
                Expr.Unpak(tk0, e)
            }
        }
    }

    fun expr_dots (): Expr {
        var e = this.expr_as(this.expr_one())

        // one!1\.2?1
        while (alls.accept(TK.CHAR, '\\') || alls.accept(TK.CHAR, '.') || alls.accept(TK.CHAR, '!') || alls.accept(
                TK.CHAR,
                '?'
            )
        ) {
            val chr = alls.tk0 as Tk.Chr
            e = if (chr.chr == '\\') {
                all().assert_tk(
                    alls.tk0,
                    e is Expr.Nat || e is Expr.Var || e is Expr.TDisc || e is Expr.UDisc || e is Expr.Dnref || e is Expr.Upref || e is Expr.Call
                ) {
                    "unexpected operand to `\\´"
                }
                Expr.Dnref(chr, e)
            } else {
                val ok = when {
                    (chr.chr != '.') -> false
                    alls.accept(TK.XID) -> {
                        val tk = alls.tk0 as Tk.Id
                        all().assert_tk(tk, tk.id=="pub" || tk.id=="ret" || tk.id=="state") {
                            "unexpected \"${tk.id}\""
                        }
                        true
                    }
                    else -> false
                }
                if (!ok) {
                    alls.accept_err(TK.XNUM)
                }
                val num = if (ok) null else (alls.tk0 as Tk.Num)
                /*
                all().assert_tk(alls.tk0, e !is Expr.TCons && e !is Expr.UCons && e !is Expr.UNull) {
                    "invalid discriminator : unexpected constructor"
                }
                */
                if (chr.chr == '?' || chr.chr == '!') {
                    All_assert_tk(alls.tk0, num!!.num != 0 || e is Expr.Dnref) {
                        "invalid discriminator : union cannot be <.0>"
                    }
                }
                when {
                    (chr.chr == '?') -> Expr.UPred(num!!, e)
                    (chr.chr == '!') -> Expr.UDisc(num!!, e)
                    (chr.chr == '.') -> {
                        val xas = if (!INFER) e else {
                            Expr.Unpak(Tk.Sym(TK.XAS,alls.tk0.lin,alls.tk0.col,":-"), e)
                        }
                        if (alls.tk0.enu == TK.XID) {
                            Expr.Field(alls.tk0 as Tk.Id, xas)
                        } else {
                            Expr.TDisc(num!!, xas)
                        }
                    }
                    else -> error("impossible case")
                }
            }
            e = this.expr_as(e)
        }
        return e
    }

    fun where (s: Stmt): Stmt {
        alls.accept_err(TK.WHERE)
        val tk0 = alls.tk0
        val blk = this.block()
        assert(!blk.iscatch && blk.scp1.isanon()) { "TODO" }
        return when {
            (s !is Stmt.Seq) -> {
                All_nest("""
                    {
                        ${blk.body.tostr(true)}
                        ${s.tostr(true)}
                    }
                    
                """.trimIndent()) {
                    this.stmt()
                } as Stmt
            }
            (s.s1 is Stmt.Var) -> {
                /*
                    val old = All_nest("""
                        ${until.s1.tostr(true)}        // this wouldn't work b/c var has no type yet
                        {
                            ${blk.body.tostr(true)}
                            ${until.s2.tostr(true)}
                        }
                    """.trimIndent())
                 */
                Stmt.Seq(
                    tk0, s.s1,
                    Stmt.Block(
                        blk.tk_, blk.iscatch, blk.scp1,
                        Stmt.Seq(tk0, blk.body, s.s2)
                    )
                )
            }
            (s.s2 is Stmt.Return) -> {
                All_nest("""
                    {
                        ${blk.body.tostr(true)}
                        ${s.s1.tostr(true)}
                    }
                    ${s.s2.tostr(true)}
                    
                """.trimIndent()) {
                    this.stmt()
                } as Stmt
            }
            else -> error("bug found")
        }
    }
    fun attr_unpak (e: Attr): Attr {
        return if (!alls.accept(TK.XAS)) e else {
            val tk0 = alls.tk0 as Tk.Sym
            Attr.Unpak(tk0, e)
        }
    }

    fun attr (): Attr {
        var e = when {
            alls.accept(TK.XID) -> Attr.Var(alls.tk0 as Tk.Id)
            alls.accept(TK.XNAT) -> {
                alls.accept_err(TK.CHAR, ':')
                val tp = this.type()
                Attr.Nat(alls.tk0 as Tk.Nat, tp)
            }
            alls.accept(TK.CHAR, '\\') -> {
                val tk0 = alls.tk0 as Tk.Chr
                val e = this.attr()
                all().assert_tk(
                    alls.tk0,
                    e is Attr.Nat || e is Attr.Var || e is Attr.TDisc || e is Attr.UDisc || e is Attr.Dnref
                ) {
                    "unexpected operand to `\\´"
                }
                Attr.Dnref(tk0, e)
            }
            alls.accept(TK.CHAR, '(') -> {
                val e = this.attr()
                alls.accept_err(TK.CHAR, ')')
                e
            }
            else -> {
                alls.err_expected("expression")
                error("unreachable")
            }
        }

        e = this.attr_unpak(e)

        // one.1!\.2.1?
        while (alls.accept(TK.CHAR, '\\') || alls.accept(TK.CHAR, '.') || alls.accept(TK.CHAR, '!')) {
            val chr = alls.tk0 as Tk.Chr
            e = if (chr.chr == '\\') {
                all().assert_tk(
                    alls.tk0,
                    e is Attr.Nat || e is Attr.Var || e is Attr.TDisc || e is Attr.UDisc || e is Attr.Dnref
                ) {
                    "unexpected operand to `\\´"
                }
                Attr.Dnref(chr, e)
            } else {
                val ok = when {
                    (chr.chr != '.') -> false
                    alls.accept(TK.XID) -> {
                        val tk = alls.tk0 as Tk.Id
                        all().assert_tk(tk, tk.id == "pub") {
                            "unexpected \"${tk.id}\""
                        }
                        true
                    }
                    else -> false
                }
                if (!ok) {
                    alls.accept_err(TK.XNUM)
                }
                val num = if (ok) null else (alls.tk0 as Tk.Num)
                when {
                    (chr.chr == '!') -> Attr.UDisc(num!!, e)
                    (chr.chr == '.') -> {
                        if (alls.tk0.enu == TK.XID) {
                            Attr.Field(alls.tk0 as Tk.Id, e)
                        } else {
                            Attr.TDisc(num!!, e)
                        }
                    }
                    else -> error("impossible case")
                }
            }
            e = this.attr_unpak(e)
        }
        return e
    }

    fun block (): Stmt.Block {
        val iscatch = (alls.tk0.enu == TK.CATCH)
        alls.accept_err(TK.CHAR, '{')
        val tk0 = alls.tk0 as Tk.Chr
        val scp1 = if (!alls.accept(TK.CHAR, '@')) null else {
            alls.accept_err(TK.XID)
            alls.tk0.asscopecst()
        }
        val ss = this.stmts()
        alls.accept_err(TK.CHAR, '}')
        return Stmt.Block(tk0, iscatch, scp1, ss)
    }

    fun stmts (): Stmt {
        fun enseq(s1: Stmt, s2: Stmt): Stmt {
            return when {
                (s1 is Stmt.Nop) -> s2
                (s2 is Stmt.Nop) -> s1
                else -> Stmt.Seq(s1.tk, s1, s2)
            }
        }

        var ret: Stmt = Stmt.Nop(alls.tk0)
        while (true) {
            alls.accept(TK.CHAR, ';')
            val isend = alls.check(TK.CHAR, '}') || alls.check(TK.EOF)
            if (!isend) {
                val s = this.stmt()
                ret = enseq(ret, s)
            } else {
                break
            }
        }
        return ret
    }
}
