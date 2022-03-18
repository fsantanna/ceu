object Parser
{
    fun type (preid: Tk.Id? = null): Type {
        return when {
            (preid!=null || alls.acceptVar("Id")) -> {
                val tk0 = preid ?: alls.tk0 as Tk.Id
                val subs = mutableListOf<Tk>()
                while (alls.acceptFix(".")) {
                    alls.acceptVar("Num") || (CE1 && alls.acceptVar("Id")) || alls.err_expected("field")
                    subs.add(alls.tk0)
                }
                val scps = if (!alls.acceptFix("@[")) null else {
                    val ret = this.scp1s { }
                    alls.acceptFix_err("]")
                    ret
                }
                Type.Named(tk0, subs, false, scps?.map { Scope(it,null) })
            }
            alls.acceptFix("/") -> {
                val tk0 = alls.tk0 as Tk.Fix
                val pln = this.type()
                val scp = alls.acceptVar("Scp")
                Type.Pointer(tk0, if (!scp) null else Scope(alls.tk0 as Tk.Scp,null), pln)
            }
            alls.acceptFix("func") || alls.acceptFix("task") -> {
                val tk0 = alls.tk0 as Tk.Fix

                val (scps, ctrs) = if (alls.checkFix("@[")) {
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
                if (!(CE1 || !haseq)) alls.err_tk0_unexpected()

                val tp = when {
                    !hasid -> this.type(null)
                    haseq  -> this.type(null)
                    (id is Tk.id) -> { All_err_tk(id, "unexpected variable identifier"); error("") }
                    else -> this.type(id as Tk.Id)
                }
                val tps = arrayListOf(tp)
                val ids = if (haseq) arrayListOf(id) else null

                while (true) {
                    if (!alls.acceptFix(",")) {
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
                        alls.checkFix_err("<")
                        val uni = this.type() as Type.Union

                        fun Type?.add (inc: Type.Tuple): Type.Tuple {
                            val inc_ = inc.clone(uni, inc.tk.lin, inc.tk.col) as Type.Tuple
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
                if (!(CE1 || !haseq)) alls.err_tk0_unexpected()

                val tp = when {
                    !hasid -> this.type(null)
                    haseq  -> this.type(null)
                    (id is Tk.id) -> { All_err_tk(id, "unexpected variable identifier"); error("") }
                    else -> this.type(id as Tk.Id)
                }
                val tps = arrayListOf(tp)
                val ids = if (haseq) arrayListOf(id) else null

                while (true) {
                    if (!alls.acceptFix(",")) {
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
        alls.acceptFix_err("@[")
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
        alls.acceptFix_err("]")
        return Pair(scps, ctrs)
    }

    fun expr_one (preid: Tk.id?): Expr {
        return when {
            // Bool.False, Pair [x,y], active Task {}
            alls.acceptFix("active") || alls.checkVar("Id") -> {
                val isact = (alls.tk0.str == "active")
                alls.checkVar_err("Id")
                val id = alls.tk1
                //var tp = this.type() as Type.Named
                val tp = this.type() as Type.Named
                val e = when {
                    // Bool.False
                    (tp.subs.size > 0) -> {
                        var ret = if (alls.checkExpr()) this.expr() else {
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
                    // Func {}
                   else -> {
                        val block = this.block()
                        Expr.Func(id, null, block)
                    }
                }
                Expr.Pak(id, e, isact, tp)
            }
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
            alls.acceptFix("<") -> {
                alls.acceptFix_err(".")

                alls.acceptVar("Id") || alls.acceptVar_err("Num")
                val dsc = alls.tk0
                All_assert_tk(alls.tk0, alls.tk0 is Tk.Num || alls.tk0 is Tk.Id) {
                    "invalid discriminator : expected index or type identifier"
                }
                if (!(CE1 || dsc is Tk.Num)) alls.err_tk0_unexpected()

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
            alls.acceptFix("()") -> Expr.Unit(alls.tk0 as Tk.Fix)
            (preid!=null || alls.acceptVar("id")) -> Expr.Var(alls.tk0 as Tk.id)
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
                if (!(CE1 || !haseq)) alls.err_tk0_unexpected()

                val e = this.expr(if (hasid && !haseq) (id as Tk.id) else null)
                val es = arrayListOf(e)
                val ids = if (haseq) arrayListOf(id as Tk.id) else null

                while (true) {
                    if (!alls.acceptFix(",")) {
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
                val block = this.block()
                Expr.Func(tk, tp, block)
            }
            else -> {
                alls.err_expected("expression")
                error("unreachable")
            }
        }
    }
    fun expr (preid: Tk.id? = null): Expr {
        var e = this.expr_dots(preid)
        // call
        if (alls.checkExpr() || alls.checkFix("@[")) {
            val iscps = if (!alls.acceptFix("@[")) null else {
                val ret = this.scp1s { }
                alls.acceptFix_err("]")
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
                alls.acceptVar("id") || alls.acceptVar("Id") || alls.acceptVar("Num") || alls.acceptFix("Null") || alls.err_tk1_unexpected()

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
        val tk0 = alls.tk0
        val blk = this.block()
        assert(!blk.iscatch && blk.scp1?.str.isanon()) { "TODO" }
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

    fun attr (): Attr {
        var e = when {
            alls.acceptVar("id") -> Attr.Var(alls.tk0 as Tk.id)
            alls.acceptVar("Nat") -> {
                alls.acceptFix_err(":")
                val tp = this.type()
                Attr.Nat(alls.tk0 as Tk.Nat, tp)
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

    fun block (): Stmt.Block {
        val iscatch = (alls.tk0.str == "catch")
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
            alls.acceptFix(";")
            val isend = alls.checkFix("}") || alls.checkVar("Eof")
            if (!isend) {
                val s = this.stmt()
                ret = enseq(ret, s)
            } else {
                break
            }
        }
        return ret
    }
    fun event (): String {
        return if (alls.acceptVar("Clk")) {
            "" + alls.tk0.str + "ms"
        } else {
            this.expr().tostr(true)
        }
    }
    fun stmt (): Stmt {
        return when {
            alls.acceptFix("set") -> {
                val dst = this.attr().toExpr()
                alls.acceptFix_err("=")
                val tk0 = alls.tk0 as Tk.Fix
                when {
                    alls.acceptFix("await") -> {
                        val e = this.expr()
                        All_assert_tk(e.tk, e.unpak() is Expr.Call) { "expected task call" }
                        All_nest(
                            """
                        {
                            var tsk_$N = spawn ${e.tostr(true)}
                            var st_$N = tsk_$N.status
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
                    alls.acceptFix("input") -> {
                        val tk = alls.tk0 as Tk.Fix
                        alls.acceptVar_err("id")
                        val lib = (alls.tk0 as Tk.id)
                        val arg = this.expr()
                        alls.acceptFix_err(":")
                        val tp = this.type()
                        Stmt.Input(tk, tp, dst, lib, arg)
                    }
                    alls.checkFix("spawn") -> {
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
            alls.acceptFix("var") -> {
                alls.acceptVar_err("id")
                val tk_id = alls.tk0 as Tk.id
                val tp = if (!alls.acceptFix(":")) null else {
                    this.type()
                }
                if (!alls.acceptFix("=")) {
                    if (tp == null) {
                        alls.err_expected("type declaration")
                    }
                    Stmt.Var(tk_id, tp)
                } else if (alls.acceptFix("var")) {
                    Stmt.Var(tk_id, null)
                } else {
                    fun tpor(inf: String): String? {
                        return if (tp == null) inf else null
                    }
                    val tk0 = alls.tk0 as Tk.Fix
                    val dst = Expr.Var(tk_id)
                    val src = when {
                        alls.acceptFix("input") -> {
                            val tk = alls.tk0 as Tk.Fix
                            alls.acceptVar_err("id")
                            val lib = (alls.tk0 as Tk.id)
                            val arg = this.expr()
                            val inp = if (!alls.acceptFix(":")) null else this.type()
                            Stmt.Input(tk, inp, dst, lib, arg)
                        }
                        alls.checkFix("spawn") -> {
                            val s = this.stmt()
                            All_assert_tk(s.tk, s is Stmt.SSpawn) { "unexpected dynamic `spawn`" }
                            val ss = s as Stmt.SSpawn
                            Stmt.SSpawn(ss.tk_, dst, ss.call)
                        }
                        alls.acceptFix("await") -> {
                            val e = this.expr()
                            All_assert_tk(e.tk, e.unpak() is Expr.Call) { "expected task call" }
                            val ret = All_nest(
                                """
                                {
                                    var tsk_$N = spawn ${e.tostr(true)}
                                    var st_$N = tsk_$N.status
                                    if _(${D}st_$N == TASK_AWAITING) {
                                        await tsk_$N
                                    }
                                    set ${tk_id.str} = tsk_$N.ret
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
            alls.acceptFix("input") -> {
                val tk = alls.tk0 as Tk.Fix
                alls.acceptVar_err("id")
                val lib = (alls.tk0 as Tk.id)
                val arg = this.expr()
                val tp = if (!alls.acceptFix(":")) null else this.type()
                Stmt.Input(tk, tp, null, lib, arg)
            }
            alls.acceptFix("if") -> {
                val tk0 = alls.tk0 as Tk.Fix
                val tst = this.expr()
                val true_ = this.block()
                val false_ = if (alls.acceptFix("else")) {
                    this.block()
                } else {
                    Stmt.Block(Tk.Fix("{", alls.tk1.lin, alls.tk1.col), false, null, Stmt.Nop(alls.tk0))
                }
                Stmt.If(tk0, tst, true_, false_)
            }
            alls.acceptFix("return") -> {
                if (!alls.checkExpr()) {
                    Stmt.Return(alls.tk0 as Tk.Fix)
                } else {
                    val tk0 = alls.tk0
                    val e = this.expr()
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
            alls.acceptFix("type") -> {
                alls.acceptVar_err("Id")
                val id = alls.tk0 as Tk.Id
                val scps = if (alls.checkFix("@[")) this.scopepars() else Pair(null, null)
                alls.acceptFix_err("=")
                val tp = this.type()
                Stmt.Typedef(id, scps, tp)
            }
            alls.acceptFix("native") -> {
                val istype = alls.acceptFix("type")
                alls.acceptVar_err("Nat")
                Stmt.Native(alls.tk0 as Tk.Nat, istype)
            }
            alls.acceptFix("call") -> {
                val tk0 = alls.tk0 as Tk.Fix
                val e = this.expr()
                All_assert_tk(tk0, e.unpak() is Expr.Call) { "expected call expression" }
                Stmt.SCall(tk0, e.unpak() as Expr.Call)
            }
            alls.acceptFix("spawn") -> {
                val tk0 = alls.tk0 as Tk.Fix
                if (alls.checkFix("{")) {
                    val block = this.block()
                    All_nest("spawn (task _ -> _ -> _ ${block.tostr(true)}) ()") {
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
            alls.acceptFix("await") -> {
                val tk0 = alls.tk0 as Tk.Fix
                when {
                    alls.acceptVar("Clk") -> {
                        val clk = alls.tk0 as Tk.Clk
                        All_nest(
                            """
                            {
                                var ms_$N: _int = _${clk.str}
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
            alls.acceptFix("throw") -> {
                Stmt.Throw(alls.tk0 as Tk.Fix)
            }
            alls.acceptFix("loop") -> {
                val tk0 = alls.tk0 as Tk.Fix
                if (alls.checkFix("{")) {
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
                    alls.acceptFix_err("in")
                    val tsks = this.expr()
                    val block = this.block()
                    // add additional block to break out w/ goto and cleanup
                    Stmt.Block(
                        block.tk_, false, null,
                        Stmt.DLoop(tk0, i as Expr.Var, tsks, block)
                    )
                }
            }
            alls.acceptFix("break") -> Stmt.Break(alls.tk0 as Tk.Fix)
            alls.acceptFix("catch") || alls.checkFix("{") -> this.block()
            alls.acceptFix("output") -> {
                val tk = alls.tk0 as Tk.Fix
                alls.acceptVar_err("id")
                val lib = (alls.tk0 as Tk.id)
                val arg = this.expr()
                Stmt.Output(tk, lib, arg)
            }
            alls.acceptFix("defer") -> {
                if (!CE1) alls.err_tk0_unexpected()
                val blk = this.block()
                All_nest(
                    """
                    spawn {
                        await evt?1
                        ${blk.tostr(true)}
                    }
                    
                """.trimIndent()
                ) {
                    this.stmt()
                } as Stmt
            }
            alls.acceptFix("every") -> {
                if (!CE1) alls.err_tk0_unexpected()
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
            alls.acceptFix("par") -> {
                if (!CE1) alls.err_tk0_unexpected()
                val pars = mutableListOf<Stmt.Block>()
                pars.add(this.block())
                while (alls.acceptFix("with")) {
                    pars.add(this.block())
                }
                val srcs = pars.map { "spawn ${it.tostr(true)}" }.joinToString("\n")
                All_nest(srcs + "await _0\n") {
                    this.stmts()
                } as Stmt
            }
            alls.acceptFix("parand") || alls.acceptFix("paror") -> {
                if (!CE1) alls.err_tk0_unexpected()
                val op = if (alls.tk0.str == "parand") "&&" else "||"
                val pars = mutableListOf<Stmt.Block>()
                pars.add(this.block())
                while (alls.acceptFix("with")) {
                    pars.add(this.block())
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
            alls.acceptFix("pauseif") -> {
                if (!CE1) alls.err_tk0_unexpected()
                val pred = this.expr() as Expr.UPred
                val blk = this.block()
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
            alls.acceptFix("watching") -> {
                if (!CE1) alls.err_tk0_unexpected()
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
            else -> {
                alls.err_expected("statement")
                error("unreachable")
            }
        }.let { it1 ->
            val it2 = if (!alls.checkFix("where")) it1 else {
                if (!CE1) alls.err_tk0_unexpected()
                this.where(it1)
            }
            val it3 = if (!alls.acceptFix("until")) it2 else {
                if (!CE1) alls.err_tk0_unexpected()
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
