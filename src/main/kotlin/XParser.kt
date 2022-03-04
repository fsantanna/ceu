class XParser: Parser()
{
    fun event (): String {
        return if (alls.accept(TK.XCLK)) {
            "" + (alls.tk0 as Tk.Clk).ms + "ms"
        } else {
            this.expr().xtostr()
        }
    }

    override fun type (): Type {
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
                        ctrs),
                    inp, pub, out)
            }
            else -> super.type()
        }
    }

    override fun expr_one (): Expr {
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
                Expr.As(Tk.Sym(TK.XAS,id.lin,id.col,":+"), e, tp)
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
                    (tp is Type.Alias) -> Expr.As (  // TODO: upcast
                        Tk.Sym(TK.XAS,tp.tk.lin,tp.tk.col,":+"),
                        Expr.UCons(tk0, null, cons!!),
                        tp
                    )
                    else -> Expr.As (
                        Tk.Sym(TK.XAS,cons!!.tk.lin,cons!!.tk.col,":+"),
                        Expr.UCons(tk0, tp as Type.Union?, cons!!),
                        null
                    )
                }
            }
            alls.accept(TK.NEW) -> {
                val tk0 = alls.tk0
                val e = this.expr()
                all().assert_tk(tk0, e is Expr.As || (e is Expr.UCons && e.tk_.num!=0)) {
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
            else -> super.expr_one()
        }
    }

    override fun expr (): Expr {
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
            e = Expr.Call(e.tk, e, arg,
                Pair(
                    if (iscps==null) null else iscps.map { Scope(it,null) },
                    if (oscp==null) null else Scope(oscp,null)
                ))
        }
        return e
    }

    override fun stmt(): Stmt {
        return when {
            alls.accept(TK.SET) -> {
                val dst = this.attr().toExpr()
                alls.accept_err(TK.CHAR, '=')
                val tk0 = alls.tk0 as Tk.Chr
                if (alls.accept(TK.AWAIT)) {
                    val e = this.expr()
                    All_assert_tk(e.tk, e is Expr.Call) { "expected task call" }
                    All_nest("""
                        {
                            var tsk_$N = spawn ${e.xtostr()}
                            var st_$N = tsk_$N.state
                            if _(${D}st_$N == TASK_AWAITING) {
                                await tsk_$N
                            }
                            set ${dst.xtostr()} = tsk_$N.ret
                        }
                    """.trimIndent()) {
                        this.stmts()
                    } as Stmt
                } else {
                    this.set_tail(tk0, dst)
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
                    Stmt.Var(tk_id, tp, null)
                } else if (alls.accept(TK.VAR)) {
                    Lexer.lex() // accept any KEY
                    Stmt.Var(tk_id, null, (alls.tk0 as Tk.Key).key)
                } else {
                    fun tpor (inf: String): String? {
                        return if (tp == null) inf else null
                    }
                    val tk0 = alls.tk0 as Tk.Chr
                    val dst = Expr.Var(tk_id)
                    val (inf,src) = when {
                        alls.accept(TK.INPUT) -> {
                            val tk = alls.tk0 as Tk.Key
                            alls.accept_err(TK.XID)
                            val lib = (alls.tk0 as Tk.Id)
                            val arg = this.expr()
                            val inp = if (!alls.accept(TK.CHAR, ':')) null else this.type()
                            Pair(tpor("input"), Stmt.Input(tk, inp, dst, lib, arg))
                        }
                        alls.check(TK.SPAWN) -> {
                            val s = this.stmt()
                            All_assert_tk(s.tk, s is Stmt.SSpawn) { "unexpected dynamic `spawn`" }
                            val ss = s as Stmt.SSpawn
                            Pair(tpor("spawn"), Stmt.SSpawn(ss.tk_, dst, ss.call))
                        }
                        alls.accept(TK.AWAIT) -> {
                            val e = this.expr()
                            All_assert_tk(e.tk, e is Expr.Call) { "expected task call" }
                            val ret = All_nest("""
                                {
                                    var tsk_$N = spawn ${e.xtostr()}
                                    var st_$N = tsk_$N.state
                                    if _(${D}st_$N == TASK_AWAITING) {
                                        await tsk_$N
                                    }
                                    set ${tk_id.id} = tsk_$N.ret
                                }
                            """.trimIndent()) {
                                this.stmts()
                            }
                            Pair(tpor("await"), ret as Stmt)
                        }
                        else -> {
                            val src = this.expr()
                            Pair(tpor("set"), Stmt.Set(tk0, dst, src))
                        }
                    }
                    Stmt.Seq(tk_id, Stmt.Var(tk_id,tp,inf), src)
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
                    All_nest(tk0.lincol("""
                        set ret = ${e.xtostr()}
                        return
                        
                    """.trimIndent())) {
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
            alls.accept(TK.SPAWN) -> {
                val tk0 = alls.tk0 as Tk.Key
                if (alls.check(TK.CHAR,'{')) {
                    val block = this.block()
                    All_nest("spawn (task _ -> _ -> _ ${block.xtostr()}) ()") {
                        this.stmt()
                    } as Stmt
                } else {
                    val e = this.expr()
                    All_assert_tk(tk0, e is Expr.Call) { "expected call expression" }
                    if (alls.accept(TK.IN)) {
                        val tsks = this.expr()
                        Stmt.DSpawn(tk0, tsks, e as Expr.Call)
                    } else {
                        Stmt.SSpawn(tk0, null, e as Expr.Call)
                    }
                }
            }
            alls.accept(TK.PAUSEIF) -> {
                val pred = this.expr() as Expr.UPred
                val blk = this.block()
                All_nest("""
                    {
                        var tsk_$N = spawn ${blk.xtostr()}
                        watching tsk_$N {
                            loop {
                                await ${pred.xtostr()}
                                var x_$N = ${pred.uni.xtostr()}!${pred.tk_.num}
                                if x_$N {
                                    pause tsk_$N
                                } else {
                                    resume tsk_$N
                                }
                            }
                        }
                    }
                    
                """.trimIndent()) {
                    this.stmt()
                } as Stmt
            }
            alls.accept(TK.PAR) -> {
                var pars = mutableListOf<Stmt.Block>()
                pars.add(this.block())
                while (alls.accept(TK.WITH)) {
                    pars.add(this.block())
                }
                val srcs = pars.map { "spawn ${it.xtostr()}" }.joinToString("\n")
                All_nest(srcs + "await _0\n") {
                    this.stmts()
                } as Stmt
            }
            alls.accept(TK.EVERY) -> {
                val evt = this.event()
                val blk = this.block()
                All_nest("""
                    loop {
                        await $evt
                        ${blk.xtostr()}
                    }
                    
                """.trimIndent()) {
                    this.stmt()
                } as Stmt
            }
            alls.accept(TK.WATCHING) -> {
                val evt = this.event()
                val blk = this.block()
                All_nest("""
                    paror {
                        await $evt
                    } with
                        ${blk.xtostr()}
                    
                """.trimIndent()) {
                    this.stmt()
                } as Stmt
            }
            alls.accept(TK.AWAIT) -> {
                val tk0 = alls.tk0 as Tk.Key
                when {
                    alls.accept(TK.XCLK) -> {
                        val clk = alls.tk0 as Tk.Clk
                        All_nest("""
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
                        """.trimIndent()) {
                            this.stmt()
                        } as Stmt
                    }
                    else -> {
                        val e = this.expr()
                        Stmt.Await(tk0, e)
                    }
                }
            }

            alls.accept(TK.PARAND) || alls.accept(TK.PAROR) -> {
                val op = if (alls.tk0.enu==TK.PARAND) "&&" else "||"
                var pars = mutableListOf<Stmt.Block>()
                pars.add(this.block())
                while (alls.accept(TK.WITH)) {
                    pars.add(this.block())
                }
                val spws = pars.mapIndexed { i,x -> "var tk_${N}_$i = spawn { ${x.body.xtostr()} }" }.joinToString("\n")
                val oks  = pars.mapIndexed { i,_ -> "var ok_${N}_$i: _int = _((${D}tk_${N}_$i->task0.state == TASK_DEAD))" }.joinToString("\n")
                val sets = pars.mapIndexed { i,_ -> "set ok_${N}_$i = _(${D}ok_${N}_$i || (((uint64_t)${D}tk_${N}_$i)==${D}tk_$N))" }.joinToString("\n")
                val chks = pars.mapIndexed { i,_ -> "${D}ok_${N}_$i" }.joinToString(" $op ")

                All_nest("""
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

                """.trimIndent()) {
                    this.stmt()
                } as Stmt
            }
            else -> super.stmt()
        }.let { it1 ->
            val it2 = if (!alls.check(TK.WHERE)) it1 else this.where(it1)
            val it3 = if (!alls.accept(TK.UNTIL)) it2 else {
                //val tk0 = alls.tk0
                //All_assert_tk(tk0, stmt !is Stmt.Var) { "unexpected `until`" }

                val cnd = this.expr()
                val if1 = All_nest("""
                    if ${cnd.xtostr()} {
                        break
                    }
                    
                """.trimIndent()) {
                    this.stmt()
                } as Stmt
                val if2 = if (!alls.check(TK.WHERE)) if1 else this.where(if1)

                All_nest("""
                    loop {
                        ${it2.xtostr()}
                        ${if2.xtostr()}
                    }
                    
                """.trimIndent()) {
                    this.stmt()
                } as Stmt
            }
            //println(it3.xtostr())
            it3
        }
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
                        ${blk.body.xtostr()}
                        ${s.xtostr()}
                    }
                    
                """.trimIndent()) {
                    this.stmt()
                } as Stmt
            }
            (s.s1 is Stmt.Var) -> {
                /*
                    val old = All_nest("""
                        ${until.s1.xtostr()}        // this wouldn't work b/c var has no type yet
                        {
                            ${blk.body.xtostr()}
                            ${until.s2.xtostr()}
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
                        ${blk.body.xtostr()}
                        ${s.s1.xtostr()}
                    }
                    ${s.s2.xtostr()}
                    
                """.trimIndent()) {
                    this.stmt()
                } as Stmt
            }
            else -> error("bug found")
        }
    }
}
