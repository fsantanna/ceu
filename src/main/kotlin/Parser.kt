object Parser
{
    fun type (preid: Tk.Id?=null, prefunc: Tk.Fix?=null): Type {
        return when {
            (preid!=null || alls.acceptVar("Id")) -> {
                // preid!=null: <List, ...>, needed b/c of <Cons=List, ...>
                val tk0 = preid ?: alls.tk0 as Tk.Id
                val subs = mutableListOf<Tk>()
                while (alls.acceptFix(".")) {
                    alls.acceptVar("Num") || (CE1 && alls.acceptVar("Id")) || alls.err_expected("field")
                    subs.add(alls.tk0)
                }

                // TODO: args=null -> args=emptyList (should be null to be inferrable)
                val hasargs = if (CE1) alls.acceptFix("\${") else alls.acceptFix_err("\${")
                val args = if (!hasargs) emptyList() else {
                    val args = mutableListOf<Type>()
                    if (!alls.checkFix("}")) {
                        while (true) {
                            val tp = Parser.type()
                            args.add(tp)
                            if (!alls.acceptFix(",")) {
                                break
                            }
                        }
                    }
                    alls.acceptFix_err("}")
                    args
                }

                val hasats = if (CE1) alls.acceptFix("@{") else alls.acceptFix_err("@{")
                val scps = if (!hasats) null else {
                    val ret = this.scp1s { }
                    alls.acceptFix_err("}")
                    ret
                }

                Type.Named(tk0, subs, false, args, scps?.map { Scope(it,null) }, null)
            }
            (prefunc!=null || alls.acceptFix("func") || alls.acceptFix("task")) -> {
                val tk0 = prefunc ?: alls.tk0 as Tk.Fix

                val hasats = if (CE1) alls.checkFix("@{") else alls.checkFix_err("@{")
                val (scps, ctrs) = if (hasats) {
                    val (x, y) = this.scopepars()
                    alls.acceptFix_err("->")
                    Pair(x, y)
                } else {
                    Pair(null, null)
                }

                // input & pub & output
                val inp = this.type()
                val pub = if (tk0.str != "func") {
                    alls.acceptFix_err("->")
                    this.type()
                } else null
                alls.acceptFix_err("->")
                val out = this.type() // right associative

                Type.Func(tk0,
                    Triple(
                        Scope(Tk.Scp("LOCAL", tk0.lin, tk0.col),null),
                        if (scps==null) null else scps.map { Scope(it,null) },
                        ctrs
                    ),
                    inp, pub, out)
            }
            (alls.acceptVar("Par")) -> {
                val tk0 = alls.tk0 as Tk.Par
                Type.Par(tk0)
            }
            alls.acceptFix("/") -> {
                val tk0 = alls.tk0 as Tk.Fix
                val pln = this.type()
                val scp = if (CE1) alls.acceptVar("Scp") else alls.acceptVar_err("Scp")
                Type.Pointer(tk0, if (!scp) null else Scope(alls.tk0 as Tk.Scp,null), pln)
            }
            alls.acceptFix("()") -> Type.Unit(alls.tk0 as Tk.Fix)
            alls.acceptVar("Nat") -> Type.Nat(alls.tk0 as Tk.Nat)
            alls.acceptFix("(") -> {
                val tp = this.type()
                alls.acceptFix_err(")")
                tp
            }
            alls.acceptFix("[") -> {
                val tk0 = alls.tk0 as Tk.Fix

                val hasid = alls.acceptVar("id")
                val id = alls.tk0
                val haseq = hasid && alls.acceptFix(":")
                if (!(CE1 || !haseq)) alls.err_tk_unexpected(alls.tk0)

                val tp = when {
                    !hasid -> this.type(null)
                    haseq  -> this.type(null)
                    (id is Tk.id) -> { All_err_tk(id, "unexpected variable identifier"); error("") }
                    else -> TODO() //this.type(id as Tk.Id)
                }
                val tps = mutableListOf(tp)
                val ids = if (haseq) mutableListOf(id) else null

                while (true) {
                    if (!(alls.acceptFix(",") || (CE1 && alls.hasln)) || alls.checkFix("]")) {
                        break
                    }
                    if (haseq) {
                        alls.acceptVar_err("id")
                        ids!!.add(alls.tk0 as Tk.id)
                        alls.acceptFix_err(":")
                    }
                    val tp2 = this.type()
                    tps.add(tp2)
                }
                alls.acceptFix_err("]")
                Type.Tuple(tk0, tps, ids as List<Tk.id>?).let {
                    if (!alls.acceptFix("+")) it else {
                        //if (!CE1) alls.err_tk0_unexpected()
                        alls.checkFix_err("<")
                        val uni = this.type() as Type.Union

                        fun Type?.add (inc: Type.Tuple): Type.Tuple {
                            val inc_ = inc.clone(inc.tk,uni) as Type.Tuple
                            return when (this) {
                                null, is Type.Unit -> inc_
                                is Type.Tuple -> {
                                    val ok = (this.yids==null && inc_.yids==null || this.yids!=null && inc_.yids!=null)
                                    All_assert_tk(this.tk, ok) {
                                        "incompatible field names"
                                    }
                                    Type.Tuple (
                                        this.tk_,
                                        inc_.vec + this.vec,
                                        if (this.yids == null) null else inc_.yids!!+this.yids!!
                                    )
                                }
                                else -> {
                                    All_err_tk(this.tk, "expected tuple type")
                                    error("unreachable code")
                                }
                            }
                        }
                        fun Type.Union.insert (inc: Type.Tuple): Type.Union {
                            val ok = (this.yids==null && inc.yids==null || this.yids!=null && inc.yids!=null)
                            All_assert_tk(this.tk, ok) {
                                "missing subtype or field identifiers"
                            }
                            val vec = this.vec.map {
                                if (it is Type.Union) {
                                    it.insert(inc)
                                } else {
                                    it.add(inc)
                                }
                            }
                            return Type.Union (
                                this.tk_,
                                this.common.add(inc),
                                vec,
                                yids
                            )
                        }

                        uni.insert(it)
                    }
                }
            }
            alls.acceptFix("<") -> {
                val tk0 = alls.tk0 as Tk.Fix

                val hasid = alls.acceptVar("Id")
                val id = alls.tk0
                val haseq = hasid && alls.acceptFix("=")
                if (!(CE1 || !haseq)) alls.err_tk_unexpected(alls.tk0)

                val tp = when {
                    !hasid -> this.type(null)
                    haseq  -> this.type(null)
                    (id is Tk.id) -> { All_err_tk(id, "unexpected variable identifier"); error("") }
                    else -> this.type(id as Tk.Id)
                }
                val tps = mutableListOf(tp)
                val ids = if (haseq) mutableListOf(id) else null

                while (true) {
                    if (!(alls.acceptFix(",") || (CE1 && alls.hasln)) || alls.checkFix(">")) {
                        break
                    }
                    if (haseq) {
                        alls.acceptVar_err("Id")
                        ids!!.add(alls.tk0 as Tk.Id)
                        alls.acceptFix_err("=")
                    }
                    val tp2 = this.type()
                    tps.add(tp2)
                }
                alls.acceptFix_err(">")
                Type.Union(tk0, null, tps, ids as List<Tk.Id>?)
            }
            alls.acceptFix("active") -> {
                val tk0 = alls.tk0 as Tk.Fix
                val (isdyn,len) = if (!alls.acceptFix("{")) Pair(false,null) else {
                    val len = if (alls.acceptVar("Num")) alls.tk0 as Tk.Num else null
                    alls.acceptFix_err("}")
                    Pair(true, len)
                }
                //alls.checkX_err("task)
                val task = this.type()
                //assert(task is Type.Func && task.tk.enu == TK.TASK)
                All_assert_tk(tk0, task is Type.Named || task is Type.Func && task.tk.str=="task") {
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

    fun scp1s (f: (Tk.Scp) -> Unit): List<Tk.Scp> {
        val scps = mutableListOf<Tk.Scp>()
        while (alls.acceptVar("id") || alls.acceptVar("ID")) {
            val tk = Tk.Scp(alls.tk0.str, alls.tk0.lin, alls.tk0.col)
            f(tk)
            scps.add(tk)
            if (!alls.acceptFix(",")) {
                break
            }
        }
        return scps
    }
    fun scopepars (): Pair<List<Tk.Scp>, List<Pair<String, String>>> {
        alls.acceptFix_err("@{")
        val scps = this.scp1s {
            All_assert_tk(it, it.str.none { it.isUpperCase() }) { "invalid scope parameter identifier" }
        }
        val ctrs = mutableListOf<Pair<String, String>>()
        if (alls.acceptFix(":")) {
            while (alls.acceptVar("id")) {
                val id1 = (alls.tk0 as Tk.id).str
                alls.acceptFix_err(">")
                alls.acceptVar_err("id")
                val id2 = (alls.tk0 as Tk.id).str
                ctrs.add(Pair(id1, id2))
                if (!alls.acceptFix(",")) {
                    break
                }
            }
        }
        alls.acceptFix_err("}")
        return Pair(scps, ctrs)
    }

    fun expr_one (preid: Tk.id?): Expr {
        return when {
            alls.acceptFix("()") -> Expr.Unit(alls.tk0 as Tk.Fix)
            (preid!=null || alls.acceptVar("id")) -> Expr.Var(alls.tk0 as Tk.id)
            alls.acceptVar("Nat") -> {
                val tk0 = alls.tk0 as Tk.Nat
                val tp = if (!alls.acceptFix(":")) null else {
                    this.type()
                }
                Expr.Nat(tk0, tp)
            }
            alls.acceptFix("Null") -> {
                val tk0 = alls.tk0 as Tk.Fix
                val tp = if (!alls.acceptFix(":")) null else {
                    val tp = this.type()
                    All_assert_tk(tp.tk,tp is Type.Pointer && tp.pln is Type.Named) {
                        "invalid type : expected pointer to alias type"
                    }
                    tp
                }
                Expr.UNull(tk0, tp as Type.Pointer?)
            }
            alls.acceptFix("active") || alls.checkVar("Id") -> {
                // Bool.False, Pair [x,y], active Task {}
                val isact = (alls.tk0.str == "active")
                alls.checkVar_err("Id")
                val id = alls.tk1
                //var tp = this.type() as Type.Named
                val tp = this.type() as Type.Named
                val e = when {
                    // Bool.False
                    (tp.subs.size > 0) -> {
                        var ret = if (alls.checkExpr()) this.expr() else {
                            if (!CE1) alls.err_tk_unexpected(alls.tk1)
                            Expr.Unit(Tk.Fix("()", alls.tk1.lin, alls.tk1.col))
                        }
                        for (tk in tp.subs.reversed()) {
                            ret = Expr.UCons(tk, null, ret)
                        }
                        // remove subs from UCons
                        //tp = Type.Named(tp.tk_, emptyList(), tp.xisrec, tp.xscps)
                        ret
                    }
                    // Pair [x,y]
                    alls.checkExpr() -> {
                        this.expr()
                    }
                    // active Task {}
                    alls.checkFix("{") -> {
                        val block = this.block(null)
                        Expr.Func(id, null, block)
                    }
                    // Unit
                    else -> {
                        if (!CE1) alls.err_tk_unexpected(alls.tk1)
                        Expr.Unit(Tk.Fix("()", alls.tk1.lin, alls.tk1.col))
                    }
                }
                Expr.Pak(id, e, isact, tp)
            }

            alls.acceptFix("<") -> {
                alls.acceptFix_err(".")

                alls.acceptVar("Id") || alls.acceptVar_err("Num")
                val dsc = alls.tk0
                All_assert_tk(alls.tk0, alls.tk0 is Tk.Num || alls.tk0 is Tk.Id) {
                    "invalid discriminator : expected index or type identifier"
                }
                if (!(CE1 || dsc is Tk.Num)) alls.err_tk_unexpected(alls.tk0)

                val cons = if (alls.checkExpr()) {
                    this.expr()
                } else {
                    Expr.Unit(Tk.Fix("()", alls.tk1.lin, alls.tk1.col))
                }
                alls.acceptFix_err(">")
                val ok = if (CE1) alls.acceptFix(":") else alls.acceptFix_err(":")
                val tp = if (!ok) null else {
                    val tp = this.type()
                    All_assert_tk(tp.tk,tp is Type.Union) {
                        "invalid type : expected union type"
                    }
                    tp
                }
                if (tp != null) {
                    Expr.UCons(dsc, tp as Type.Union?, cons)
                } else {
                    Expr.Pak(dsc, Expr.UCons(dsc, tp as Type.Union?, cons), null, null)
                }
            }
            alls.acceptFix("new") -> {
                val tk0 = alls.tk0
                val e = this.expr()
                All_assert_tk(tk0, e is Expr.Pak || (e is Expr.UCons)) {
                    "invalid `new` : expected constructor"
                }

                val scp = if (!alls.acceptFix(":")) null else {
                    alls.acceptVar_err("Scp")
                    alls.tk0 as Tk.Scp
                }
                Expr.New(tk0 as Tk.Fix, if (scp==null) null else Scope(scp,null), e)
            }
            alls.acceptFix("if") -> {
                val tk0 = alls.tk0
                val e = this.expr()
                alls.acceptFix_err("{")
                val t = this.expr()
                alls.acceptFix_err("}")
                alls.acceptFix_err("else")
                alls.acceptFix_err("{")
                val f = this.expr()
                alls.acceptFix_err("}")
                Expr.If(tk0 as Tk.Fix, e, t, f)
            }
            alls.acceptFix("/") -> {
                val tk0 = alls.tk0 as Tk.Fix
                val e = this.expr()
                All_assert_tk(
                    alls.tk0,
                    e is Expr.Nat || e is Expr.Var || e is Expr.TDisc || e is Expr.Dnref || e is Expr.Upref
                ) {
                    "unexpected operand to `/´"
                }
                Expr.Upref(tk0, e)
            }
            alls.acceptFix("(") -> {
                val e = this.expr()
                alls.acceptFix_err(")")
                e
            }
            alls.acceptFix("[") -> {
                val tk0 = alls.tk0 as Tk.Fix

                val hasid = alls.acceptVar("id")
                val id = alls.tk0
                val haseq = alls.acceptFix("=")
                if (!(CE1 || !haseq)) alls.err_tk_unexpected(alls.tk0)

                val e = this.expr(if (hasid && !haseq) (id as Tk.id) else null)
                val es = mutableListOf(e)
                val ids = if (haseq) mutableListOf(id as Tk.id) else null

                while (true) {
                    if (!(alls.acceptFix(",") || (CE1 && alls.hasln)) || alls.checkFix("]")) {
                        break
                    }
                    if (haseq) {
                        alls.acceptVar_err("id")
                        ids!!.add(alls.tk0 as Tk.id)
                        alls.acceptFix("=")
                    }
                    val e2 = this.expr()
                    es.add(e2)
                }
                alls.acceptFix_err("]")
                val ret = Expr.TCons(tk0, es, ids)
                if (!CE1) ret else {
                    Expr.Pak(ret.tk, ret, null, null)
                }
            }
            alls.checkFix("task") || alls.checkFix("func") -> {
                val tk = alls.tk1 as Tk.Fix
                val tp = this.type() as Type.Func
                val catch = if (!CE1) null else {
                    //All_nest("if err?Escape {if eq [err!Escape,_10] {_1} else {_0}} else {_0}") {
                    All_nest("_(task1->err.tag==CEU_ERROR_ESCAPE && task1->err.Escape==$N)") {
                        this.expr()
                    } as Expr
                }
                val block = this.block(if (alls.checkFix("catch")) null else catch)
                Expr.Func(tk, tp, block)
            }

            // CE1

            alls.acceptFix("ifs") -> {
                if (!CE1) alls.err_tk_unexpected(alls.tk0)
                val tk0 = alls.tk0 as Tk.Fix
                alls.acceptFix_err("{")
                val tst1 = this.expr()
                alls.acceptFix_err("{")
                val blk1 = this.expr()
                alls.acceptFix_err("}")
                val tsts: MutableList<Pair<Expr?,Expr>> = mutableListOf(Pair(tst1,blk1))
                while (! (alls.checkFix("}") || alls.acceptFix("else")) ) {
                    val tsti = this.expr()
                    alls.acceptFix_err("{")
                    val blki = this.expr()
                    alls.acceptFix_err("}")
                    tsts.add(Pair(tsti,blki))
                }
                if (alls.tk0.str == "else") {
                    alls.acceptFix_err("{")
                    val blk = this.expr()
                    alls.acceptFix_err("}")
                    tsts.add(Pair(null,blk))
                } else {
                    val blk = All_nest("{ _((assert(0 && \"runtime error : missing \\\"ifs\\\" case\"),0);):_int }") {
                        alls.acceptFix_err("{")
                        val blk = this.expr()
                        alls.acceptFix_err("}")
                        blk
                    } as Expr
                    tsts.add(Pair(null,blk))
                }
                alls.acceptFix_err("}")

                fun f (tsts: List<Pair<Expr?,Expr>>): Expr {
                    val (tst,blk) = tsts.first()
                    return if (tst == null) {
                        blk
                    } else {
                        Expr.If(tk0, tst, blk, f(tsts.drop(1)))
                    }
                }
                f(tsts) as Expr.If
            }

            else -> {
                alls.err_expected("expression")
                error("unreachable")
            }
        }
    }
    fun expr (preid: Tk.id? = null): Expr {
        var e = this.expr_dots(preid)
        if (alls.hasln) {
            return e
        }

        // call
        if (alls.checkExpr() || alls.checkFix("@{")) {
            val hasats = if (CE1) alls.acceptFix("@{") else alls.acceptFix_err("@{")
            val iscps = if (!hasats) null else {
                val ret = this.scp1s { }
                alls.acceptFix_err("}")
                ret
            }
            val arg = this.expr()
            val oscp = if (!alls.acceptFix(":")) null else {
                alls.acceptVar_err("Scp")
                alls.tk0 as Tk.Scp
            }
            e = Expr.Call(e.tk,
                if (e is Expr.Unpak || !CE1) e else {
                    Expr.Unpak(Tk.Fix("~",e.tk.lin,e.tk.col), true, e)
                },
                arg,
                Pair(
                    if (iscps==null) null else iscps.map { Scope(it,null) },
                    if (oscp==null) null else Scope(oscp,null)
                )
            )
            if (CE1) {
                e = Expr.Pak(e.tk, e, null, null)
            }
        }
        return e
    }
    fun expr_dots (preid: Tk.id?): Expr {
        var e = this.expr_one(preid)

        // one!1~\.2?1
        while (alls.acceptFix("::") ||
               alls.acceptFix("\\") ||
               alls.acceptFix("~")  ||
               alls.acceptFix(".")  ||
               alls.acceptFix("!")  ||
               alls.acceptFix("?")
        ) {
            val tk0 = alls.tk0 as Tk.Fix

            if (tk0.str in arrayOf(".","!","?")) {
                alls.acceptVar("id") || alls.acceptVar("Id") || alls.acceptVar("Num") || alls.acceptFix("Null") || alls.err_tk_unexpected(alls.tk1)

                if (tk0.str == ".") {
                    All_assert_tk(alls.tk0, alls.tk0 !is Tk.Id) {
                        "invalid field : unexpected type identifier"
                    }
                } else {
                    val str = if (tk0.str == "!") "discriminator" else "predicate"
                    if (alls.tk0.str == "Null") {
                        All_assert_tk(alls.tk0, tk0.str=="?" && (e is Expr.Dnref || (e is Expr.Unpak && e.e is Expr.Dnref))) {
                            "invalid $str : union cannot be null"
                        }
                    }
                    All_assert_tk(alls.tk0, alls.tk0 !is Tk.id) {
                        "invalid $str : unexpected variable identifier"
                    }
                }
                // automatic unpack only for [.,!,?]
                //  pt.x, list!1, list?0
                e = when {
                    !CE1 -> e
                    (e is Expr.Unpak) -> e
                    (e is Expr.UPred) -> e
                    else -> Expr.Unpak(tk0,true,e)
                }
            }

            e = when (tk0.str) {
                "::" -> {
                    val tp = this.type()
                    Expr.Cast(tk0, e, tp)
                }
                "\\" -> {
                    All_assert_tk(
                        alls.tk0,
                        e is Expr.Nat || e is Expr.Var || e is Expr.TDisc || e is Expr.UDisc || e is Expr.Dnref || e is Expr.Upref || e is Expr.Call
                    ) {
                        "unexpected operand to `\\´"
                    }
                    Expr.Dnref(tk0, e)
                }
                "~" -> Expr.Unpak(tk0, false, e)
                "?" -> Expr.UPred(alls.tk0, e)
                "!" -> Expr.UDisc(alls.tk0, e)
                "." -> {
                    if (alls.tk0 is Tk.id && alls.tk0.istask()) {
                        Expr.Field(alls.tk0 as Tk.id, e)
                    } else {
                        Expr.TDisc(alls.tk0, e)
                    }
                }
                else -> error("bug found")
            }
        }
        return e
    }
    fun where (s: Stmt): Stmt {
        alls.acceptFix_err("where")
        if (!CE1) alls.err_tk_unexpected(alls.tk0)
        val tk0 = alls.tk0
        val blk = this.block(null)
        assert(blk.catch==null && blk.scp1?.str.isanon()) { "TODO" }
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
                        blk.tk_, blk.catch, blk.scp1,
                        Stmt.Seq(tk0, blk.body, s.s2)
                    )
                )
            }
            (s.s2 is Stmt.XReturn) -> {
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

    fun attr (): Attr {
        var e = when {
            alls.acceptVar("id") -> Attr.Var(alls.tk0 as Tk.id)
            alls.acceptVar("Nat") -> {
                val tk0 = alls.tk0
                alls.acceptFix_err(":")
                val tp = this.type()
                Attr.Nat(tk0 as Tk.Nat, tp)
            }
            alls.acceptFix("\\") -> {
                val tk0 = alls.tk0 as Tk.Fix
                val e = this.attr()
                All_assert_tk(
                    alls.tk0,
                    e is Attr.Nat || e is Attr.Var || e is Attr.TDisc || e is Attr.UDisc || e is Attr.Dnref
                ) {
                    "unexpected operand to `\\´"
                }
                Attr.Dnref(tk0, e)
            }
            alls.acceptFix("(") -> {
                val e = this.attr()
                alls.acceptFix_err(")")
                e
            }
            else -> {
                alls.err_expected("expression")
                error("unreachable")
            }
        }

        // one.1!\~.2.1?
        while (alls.acceptFix("\\") ||
               alls.acceptFix("~") ||
               alls.acceptFix(".") ||
               alls.acceptFix("!")
        ) {
            val chr = alls.tk0 as Tk.Fix

            if (chr.str in arrayOf(".","!")) {
                alls.acceptVar("id") || alls.acceptVar_err("Num")
                if (chr.str == ".") {
                    All_assert_tk(alls.tk0, alls.tk0 is Tk.Num || alls.tk0 is Tk.id) {
                        "invalid field : expected index or variable identifier"
                    }
                } else {
                    All_assert_tk(alls.tk0, alls.tk0 is Tk.Num || alls.tk0 is Tk.id) {
                        "invalid discriminator : expected index or type identifier"
                    }
                }
                // automatic unpack only for [.,!]
                //  pt.x, list!1, list?0
                e = if (CE1 && e !is Attr.Unpak) Attr.Unpak(chr,true,e) else e
            }

            e = when (chr.str) {
                "\\" -> {
                    All_assert_tk(
                        alls.tk0,
                        e is Attr.Nat || e is Attr.Var || e is Attr.TDisc || e is Attr.UDisc || e is Attr.Dnref
                    ) {
                        "unexpected operand to `\\´"
                    }
                    Attr.Dnref(chr, e)
                }
                "~" -> Attr.Unpak(chr, true, e)
                "!" -> Attr.UDisc(alls.tk0, e)
                "." -> {
                    if (alls.tk0 is Tk.id && alls.tk0.istask()) {
                        Attr.Field(alls.tk0 as Tk.id, e)
                    } else {
                        Attr.TDisc(alls.tk0, e)
                    }
                }
                else -> error("bug found")
            }
        }
        return e
    }

    fun block (catch: Expr?): Stmt.Block {
        val c = catch ?: if (alls.acceptFix("catch")) this.expr() else null
        alls.acceptFix_err("{")
        val tk0 = alls.tk0 as Tk.Fix
        val scp1 = if (!alls.acceptVar("Scp")) null else {
            val tk = alls.tk0 as Tk.Scp
            All_assert_tk(tk, tk.str.none { it.isLowerCase() }) {
                "invalid scope constant identifier"
            }
            tk
        }
        val ss = this.stmts()
        alls.acceptFix_err("}")
        return Stmt.Block(tk0, c, scp1, ss)
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
        var first = true
        while (true) {
            val ok = alls.acceptFix(";") || alls.hasln || first //|| (alls.tk1.lin==1 && alls.tk1.col==1)
            val err = alls.tk1
            first = false

            val isend = alls.checkFix("}") || alls.checkVar("Eof")
            if (!isend) {
                val s = this.stmt()
                All_assert_tk(err, ok) { "expected \";\"" }
                ret = enseq(ret, s)
            } else {
                break
            }
        }
        return ret
    }
    fun await_spawn (s: Stmt, dst: Expr?): Stmt {
        return All_nest("""
        {
            var tsk_$N = ${s.tostr(true)}
            var st_$N = tsk_$N.status
            if _(${D}st_$N == TASK_AWAITING) {
                await tsk_$N
            }
            ${if (dst == null) "" else "set ${dst.tostr(true)} = tsk_$N.ret"}
        }
        
        """.trimIndent()) {
            this.stmts()
        } as Stmt
    }
    fun await_event (): String {
        return if (alls.acceptVar("Clk")) {
            "" + alls.tk0.str + "ms"
        } else {
            this.expr().tostr(true)
        }
    }
    fun stmt_set (dst: Expr): Stmt {
        alls.acceptFix_err("=")
        val tk0 = alls.tk0 as Tk.Fix
        return when {
            alls.checkFix("input") -> {
                val s = this.stmt() as Stmt.Input
                Stmt.Input(s.tk_, s.xtype, dst, s.arg)
            }
            alls.checkFix("spawn") -> {
                val s = this.stmt()
                All_assert_tk(s.tk, s is Stmt.SSpawn) { "unexpected dynamic `spawn`" }
                s as Stmt.SSpawn
                Stmt.SSpawn(s.tk_, dst, s.call)
            }
            alls.acceptFix("await") -> {
                if (!CE1) alls.err_tk_unexpected(alls.tk0)
                alls.checkFix_err("spawn")
                val s = this.stmt()
                All_assert_tk(s.tk, s is Stmt.SSpawn) { "unexpected dynamic `spawn`" }
                s as Stmt.SSpawn
                await_spawn(s, dst)
            }
            else -> {
                val src = this.expr()
                Stmt.Set(tk0, dst, src)
            }
        }
    }
    fun stmt (): Stmt {
        return when {
            // variables
            alls.acceptFix("var") -> {
                alls.acceptVar_err("id")
                val tk_id = alls.tk0 as Tk.id
                val tp = if (!alls.acceptFix(":")) null else {
                    this.type()
                }
                when {
                    (tp==null && alls.acceptFix("var")) -> {
                        if (!CE1) alls.err_tk_unexpected(alls.tk0)
                        // var x var   <-- no type, no assign (used by intermediate compilation)
                        Stmt.Var(tk_id, null)
                    }
                    alls.checkFix("=") -> {
                        if (!CE1) alls.err_tk_unexpected(alls.tk0)
                        val dst = Expr.Var(tk_id)
                        val set = stmt_set(dst)
                        Stmt.Seq(tk_id, Stmt.Var(tk_id, tp), set)
                    }
                    else -> {
                        if (tp == null) {
                            alls.err_expected("type declaration")
                        }
                        Stmt.Var(tk_id, tp)
                    }
                }
            }
            alls.acceptFix("set") -> {
                val dst = this.attr().toExpr()
                stmt_set(dst)
            }

            // invocations
            alls.acceptFix("output") -> {
                val tk = alls.tk0 as Tk.Fix
                val arg1 = this.expr()
                All_assert_tk(arg1.tk, arg1 is Expr.Pak) { "expected constructor" }
                arg1 as Expr.Pak
                val arg2 = if (arg1.tk.str == "Output") arg1 else {
                    All_assert_tk(arg1.tk, CE1) {
                        "expected \"Output\" constructor : have \"${arg1.tk.str}\""
                    }
                    val nopar = arg1.tostr().removeSurrounding("(",")")
                    All_nest("Output.$nopar\n") {
                        this.expr()
                    } as Expr.Pak
                }
                All_assert_tk(arg2.e.tk, arg2.e is Expr.UCons) { "expected union constructor" }
                Stmt.Output(tk, arg2)
            }
            alls.checkFix("input")   -> {
                alls.acceptFix("input")
                val tk = alls.tk0 as Tk.Fix

                val arg1 = this.expr()
                All_assert_tk(arg1.tk, arg1 is Expr.Pak) { "expected constructor" }
                arg1 as Expr.Pak
                val arg2 = if (arg1.tk.str == "Input") arg1 else {
                    All_assert_tk(arg1.tk, CE1) {
                        "expected \"Input\" constructor : have \"${arg1.tk.str}\""
                    }
                    val nopar = arg1.tostr().removeSurrounding("(",")")
                    All_nest("Input.$nopar\n") {
                        this.expr()
                    } as Expr.Pak
                }
                All_assert_tk(arg2.e.tk, arg2.e is Expr.UCons) { "expected union constructor" }

                val tp = if (CE1) {
                    if (!alls.acceptFix(":")) null else this.type()
                } else {
                    alls.acceptFix_err(":")
                    this.type()
                }
                Stmt.Input(tk, tp, null, arg2)
            }
            alls.acceptFix("native") -> {
                val istype = alls.acceptFix("type")
                alls.acceptVar_err("Nat")
                Stmt.Native(alls.tk0 as Tk.Nat, istype)
            }
            alls.acceptFix("call")   -> {
                val tk0 = alls.tk0 as Tk.Fix
                val e = this.expr()
                All_assert_tk(tk0, e.unpak() is Expr.Call) { "expected call expression" }
                Stmt.SCall(tk0, e.unpak() as Expr.Call)
            }

            // control flow
            alls.acceptFix("if")     -> {
                val tk0 = alls.tk0 as Tk.Fix
                val tst = this.expr()
                val true_ = this.block(null)
                val false_ = if (alls.acceptFix("else")) {
                    this.block(null)
                } else {
                    Stmt.Block(Tk.Fix("{", alls.tk1.lin, alls.tk1.col), null, null, Stmt.Nop(alls.tk0))
                }
                Stmt.If(tk0, tst, true_, false_)
            }
            alls.acceptFix("loop")   -> {
                val tk0 = alls.tk0 as Tk.Fix
                val catch = if (!CE1) null else {
                    //All_nest("if err?Escape {if eq [err!Escape,_10] {_1} else {_0}} else {_0}") {
                    All_nest("_(task1->err.tag==CEU_ERROR_ESCAPE && task1->err.Escape==$N)") {
                        this.expr()
                    } as Expr
                }
                if (alls.checkFix("catch") || alls.checkFix("{")) {
                    val block = this.block(if (alls.checkFix("catch")) null else catch)
                    // add additional block to break out w/ goto and cleanup
                    Stmt.Block(
                        block.tk_, null, null,
                        Stmt.Loop(tk0, block)
                    )
                } else {
                    val i = this.expr()
                    All_assert_tk(alls.tk0, i is Expr.Var) {
                        "expected variable expression"
                    }
                    alls.acceptFix_err("in")
                    val tsks = this.expr()
                    val block = this.block(if (alls.checkFix("catch")) null else catch)
                    // add additional block to break out w/ goto and cleanup
                    Stmt.Block(
                        block.tk_, null, null,
                        Stmt.DLoop(tk0, i as Expr.Var, tsks, block)
                    )
                }
            }
            alls.acceptFix("throw")  -> {
                val tk0 = alls.tk0 as Tk.Fix
                val e = this.expr()
                Stmt.Throw(tk0, e)
            }
            alls.checkFix("catch") || alls.checkFix("{") -> this.block(null)
            alls.acceptFix("return") -> {
                if (!CE1) alls.err_tk_unexpected(alls.tk0)
                if (!alls.checkExpr()) {
                    Stmt.XReturn(alls.tk0 as Tk.Fix)
                } else {
                    if (!CE1) alls.err_tk_unexpected(alls.tk0)
                    val tk0 = alls.tk0
                    val e = this.expr()
                    All_nest(tk0.lincol("""
                        set ret = ${e.tostr(true)}
                        return
                        
                    """.trimIndent())) {
                        this.stmts()
                    } as Stmt
                }
            }
            alls.acceptFix("break") -> {
                if (!CE1) alls.err_tk_unexpected(alls.tk0)
                Stmt.XBreak(alls.tk0 as Tk.Fix)
            }

            // tasks
            alls.acceptFix("spawn") -> {
                val tk0 = alls.tk0 as Tk.Fix
                if (alls.checkFix("{")) {
                    if (!CE1) alls.err_tk_unexpected(alls.tk0)
                    val block = this.block(null)
                    All_nest("spawn (task _ -> _ -> _ ${block.tostr(true)}) ()\n") {
                        this.stmt()
                    } as Stmt
                } else {
                    val e = this.expr()
                    All_assert_tk(tk0, e.unpak() is Expr.Call) { "expected call expression" }
                    if (alls.acceptFix("in")) {
                        val tsks = this.expr()
                        Stmt.DSpawn(tk0, tsks, e)
                    } else {
                        Stmt.SSpawn(tk0, null, e)
                    }
                }
            }
            alls.acceptFix("pause") -> {
                val tk0 = alls.tk0 as Tk.Fix
                val e = this.expr()
                Stmt.Pause(tk0, e, true)
            }
            alls.acceptFix("resume") -> {
                val tk0 = alls.tk0 as Tk.Fix
                val e = this.expr()
                Stmt.Pause(tk0, e, false)
            }

            // events
            alls.acceptFix("emit") -> {
                val tk0 = alls.tk0 as Tk.Fix
                val tgt = if (alls.acceptVar("Scp")) {
                    Scope(alls.tk0 as Tk.Scp, null)
                } else {
                    this.expr_dots(null)
                }
                val e = this.expr()
                Stmt.Emit(tk0, tgt, e)
            }
            alls.acceptFix("await") -> {
                val tk0 = alls.tk0 as Tk.Fix
                when {
                    alls.acceptVar("Clk") -> {
                        if (!CE1) alls.err_tk_unexpected(alls.tk0)
                        val clk = alls.tk0 as Tk.Clk
                        All_nest(
                            """
                            {
                                var ms_$N: _int = _${clk.str}
                                loop {
                                    await evt?Timer
                                    set ms_$N = sub [ms_$N, evt!Timer]
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
                    alls.checkFix("spawn") -> {
                        if (!CE1) alls.err_tk_unexpected(alls.tk0)
                        alls.checkFix_err("spawn")
                        val s = this.stmt()
                        All_assert_tk(s.tk, s is Stmt.SSpawn) { "unexpected dynamic `spawn`" }
                        s as Stmt.SSpawn
                        await_spawn(s, null)
                    }
                    else -> {
                        val e = this.expr()
                        Stmt.Await(tk0, e)
                    }
                }
            }

            // types
            alls.acceptFix("type") -> {
                alls.acceptVar_err("Id")
                val id = alls.tk0 as Tk.Id

                val haspars = if (CE1) alls.acceptFix("\${") else alls.acceptFix_err("\${")
                val pars = if (!haspars) emptyList() else {
                    val pars = mutableListOf<Tk.id>()
                    if (!alls.checkFix("}")) {
                        while (alls.acceptVar("id")) {
                            pars.add(alls.tk0 as Tk.id)
                            if (!alls.acceptFix(",")) {
                                break
                            }
                        }
                    }
                    alls.acceptFix_err("}")
                    pars
                }

                val hasats = if (CE1) alls.checkFix("@{") else alls.checkFix_err("@{")
                val scps = if (hasats) this.scopepars() else Pair(null, null)
                (CE1 && alls.acceptFix("+=")) || alls.acceptFix_err("=")
                val isinc = (alls.tk0.str == "+=")
                val tp = this.type()
                if (isinc) {
                    All_assert_tk(tp.tk, tp is Type.Union) { "expected union type" }
                }
                Stmt.Typedef(id, isinc, pars, scps, tp, null, true)
            }

            // CE1

            alls.acceptFix("func") || alls.acceptFix("task") -> {
                if (!CE1) alls.err_tk_unexpected(alls.tk0)
                val tk = alls.tk0 as Tk.Fix
                alls.acceptVar_err("id")
                val id = alls.tk0 as Tk.id
                alls.acceptFix_err(":")
                val tp = this.type(prefunc=tk) //as Type.Func
                alls.checkFix_err("{")  // no catch
                val catch = All_nest("_(task1->err.tag==CEU_ERROR_ESCAPE && task1->err.Escape==$N)") {
                    this.expr()
                } as Expr
                val blk = this.block(catch)
                All_nest("var ${id.str} = ${tp.tostr(true)} ${blk.tostr(true)}\n") {
                    this.stmt()
                } as Stmt
            }
            alls.acceptFix("ifs") -> {
                if (!CE1) alls.err_tk_unexpected(alls.tk0)
                val tk0 = alls.tk0 as Tk.Fix
                alls.acceptFix_err("{")
                val tst1 = this.expr()
                val blk1 = this.block(null)
                val tsts: MutableList<Pair<Expr?,Stmt.Block>> = mutableListOf(Pair(tst1,blk1))
                while (! (alls.checkFix("}") || alls.acceptFix("else")) ) {
                    val tsti = this.expr()
                    val blki = this.block(null)
                    tsts.add(Pair(tsti,blki))
                }
                if (alls.tk0.str == "else") {
                    val blk = this.block(null)
                    tsts.add(Pair(null,blk))
                } else {
                    val blk = All_nest("{ native _(assert(0 && \"runtime error : missing \\\"ifs\\\" case\");) }") {
                        this.block(null)
                    } as Stmt.Block
                    tsts.add(Pair(null,blk))
                }
                alls.acceptFix_err("}")

                fun f (tsts: List<Pair<Expr?,Stmt.Block>>): Stmt {
                    val (tst,blk) = tsts.first()
                    return if (tst == null) {
                        blk
                    } else {
                        Stmt.If(tk0, tst, blk, Stmt.Block(tk0, null, null, f(tsts.drop(1))))
                    }
                }
                f(tsts) as Stmt.If
            }
            alls.acceptFix("defer") -> {
                if (!CE1) alls.err_tk_unexpected(alls.tk0)
                val blk = this.block(null)
                All_nest(
                    """
                    spawn {
                        await evt?Kill
                        ${blk.tostr(true)}
                    }
                    
                """.trimIndent()
                ) {
                    this.stmt()
                } as Stmt
            }
            alls.acceptFix("every") -> {
                if (!CE1) alls.err_tk_unexpected(alls.tk0)
                val evt = this.await_event()
                val blk = this.block(null)
                All_nest("""
                    loop {
                        await $evt
                        ${blk.tostr(true)}
                    }
                    
                """.trimIndent()) {
                    this.stmt()
                } as Stmt
            }
            alls.acceptFix("pauseon") -> {
                if (!CE1) alls.err_tk_unexpected(alls.tk0)
                val pred = this.expr() as Expr.UPred
                val blk = this.block(null)
                All_nest(
                    """
                    {
                        var tsk_$N = spawn ${blk.tostr(true)}
                        watching tsk_$N {
                            loop {
                                await ${pred.tostr(true)}
                                var x_$N = ${pred.uni.tostr(true)}!${pred.tk.str}
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
            alls.acceptFix("par") -> {
                if (!CE1) alls.err_tk_unexpected(alls.tk0)
                val pars = mutableListOf<Stmt.Block>()
                pars.add(this.block(null))
                while (alls.acceptFix("with")) {
                    pars.add(this.block(null))
                }
                val srcs = pars.map { "spawn ${it.tostr(true)}" }.joinToString("\n")
                All_nest(srcs + "await _0\n") {
                    this.stmts()
                } as Stmt
            }
            alls.acceptFix("parand") || alls.acceptFix("paror") -> {
                if (!CE1) alls.err_tk_unexpected(alls.tk0)
                val op = if (alls.tk0.str == "parand") "&&" else "||"
                val pars = mutableListOf<Stmt.Block>()
                pars.add(this.block(null))
                while (alls.acceptFix("with")) {
                    pars.add(this.block(null))
                }
                val spws =
                    pars.mapIndexed { i, x -> "var tk_${N}_$i = spawn { ${x.body.tostr(true)} }" }.joinToString("\n")
                val oks =
                    pars.mapIndexed { i, _ -> "var ok_${N}_$i: _int = _((${D}tk_${N}_$i->task0.status == TASK_DEAD))" }
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
                            await evt?Task
                            var tk_$N = evt!Task
                            $sets
                        }
                    }

                """.trimIndent()
                ) {
                    this.stmt()
                } as Stmt
            }
            alls.acceptFix("watching") -> {
                if (!CE1) alls.err_tk_unexpected(alls.tk0)
                val evt = this.await_event()
                val blk = this.block(null)
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
            else -> {
                alls.err_expected("statement")
                error("unreachable")
            }
        }.let { it1 ->
            val it2 = if (!alls.checkFix("where")) it1 else {
                if (!CE1) alls.err_tk_unexpected(alls.tk0)
                this.where(it1)
            }
            val it3 = if (!alls.acceptFix("until")) it2 else {
                if (!CE1) alls.err_tk_unexpected(alls.tk0)
                //val tk0 = alls.tk0
                //All_assert_tk(tk0, stmt !is Stmt.Var) { "unexpected `until`" }

                val cnd = this.expr()
                val if1 = All_nest("""
                    if ${cnd.tostr(true)} {
                        break
                    }
                    
                    """.trimIndent()
                ) {
                    this.stmt()
                } as Stmt
                val if2 = if (!alls.checkFix("where")) if1 else this.where(if1)

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
}
