// set increasing @i to each pointer in type
// - increment only for function types
// - do not increment for alias types
fun List<Type>.increasing (toinc: Boolean): List<Scope> {
    var c = if (toinc) 'h' else 'i'     // first is 'i'
    return this
        .filter { it is Type.Pointer || it is Type.Named }
        //.let { it as List<Type.Pointer> }
        .map { tp ->
            when (tp) {
                is Type.Pointer -> {
                    tp.xscp = tp.xscp ?: let {
                        if (toinc) {
                            c += 1  // infer implicit scope incrementally
                        }
                        Scope(Tk.Scp(TK.XSCP, tp.tk.lin, tp.tk.col, c + ""), null)
                    }
                    listOf(tp.xscp!!)
                }
                is Type.Named -> {
                    //assert(tp.xscp1s == null)
                    val isself = tp.ups_first { it is Stmt.Typedef }.let {
                        (it != null) && ((it as Stmt.Typedef).tk_.id == tp.tk_.id)
                    }
                    when {
                        isself -> emptyList()              // do not infer inside self declaration (it is inferred there)
                        (tp.xscps != null) -> tp.xscps!!  // just forward existing? (TODO: assert above failed)
                        else -> {
                            val def = tp.env(tp.tk_.id)!! as Stmt.Typedef
                            tp.xscps = tp.xscps ?: def.xscp1s.first!!.map {
                                c += 1  // infer implicit scope incrementally
                                Scope(Tk.Scp(TK.Xide, tp.tk.lin, tp.tk.col, c + ""), null)
                            }
                            tp.xscps!!
                        }
                    }
                }
                else -> error("bug found")
            }
            .filter { it.scp1.isscopepar() }
            //.let { print("111: "); println(it) ; it}
            .filter { scp ->
                // do not return constant scopes and local outer/lexical scopes
                // do not add if already in outer Func
                //      var outer = func {@a1}->... {          // receives env
                //          return func @a1->()->/_int@a1 {   // holds/uses outer env
                tp.ups_tolist()
                    .filter { it is Type.Func || it is Stmt.Typedef }
                    //.let { println(it) ; it }
                    .any {
                        //println(it.toType().tostr())
                        (it !is Type.Func) || (it.xscps.second?.none { it.scp1.id==scp.scp1.id } ?: true)
                    }
            }
            //.let {print("222: "); println(it) ; it}
        }
        .flatten()
}

fun Stmt.xinfScp1s () {
    fun ft (tp: Type) {
        when (tp) {
            is Type.Named -> {
                val def = tp.env(tp.tk_.id)
                when {
                    (tp.xscps != null) -> {}
                    (def !is Stmt.Typedef) -> {} // will be an error in Check_01
                    (tp.wup.let { it is Type.Pointer && it.xscp!=null } && def.xscp1s.first.let { it!=null && it.size>0 }) -> {
                        // copy alias scope from enclosing pointer scope
                        // var x: /List @A --> /List @[A] @A
                        assert(def.xscp1s.first!!.size == 1) { "can't choose from multiple scopes" }
                        tp.xscps = tp.xscps ?: listOf((tp.wup as Type.Pointer).xscp!!)
                    }
                    // do not infer inside func/typedef declaration (it is inferred there)
                    (tp.ups_first { it is Stmt.Typedef && it==def || it is Type.Func } != null) -> {}
                    else -> {
                        val size = def.xscp1s.first.let { if (it == null) 0 else it.size }
                        tp.xscps = tp.xscps ?: List(size) { Scope(Tk.Scp(TK.XSCP, tp.tk.lin, tp.tk.col, tp.localBlockScp1Id()), null) }
                    }
                }
            }
            is Type.Pointer -> {
                when {
                    (tp.xscp != null) -> {}
                    (tp.pln is Type.Named && tp.pln.xscps!=null && tp.pln.xscps!!.size>0) -> {
                        // copy alias scope to enclosing pointer scope
                        // var x: /List @[A] --> /List @[A] @A
                        assert(tp.pln.xscps!!.size == 1) { "can't choose from multiple scopes" }
                        tp.xscp = tp.pln.xscps!![0]
                    }
                    // do not infer to LOCAL if inside function/typedef declaration
                    (tp.ups_first { it is Type.Func || it is Stmt.Typedef } != null) -> {}
                    else -> {
                        tp.xscp = Scope(Tk.Scp(TK.XSCP, tp.tk.lin, tp.tk.col, tp.localBlockScp1Id()), null)
                    }
                }
            }
            is Type.Func -> {
                val inp_pub_out = (tp.inp.flattenLeft() + (tp.pub?.flattenLeft() ?: emptyList()) + tp.out.flattenLeft()).increasing(true)

                // {closure} + {explicit scopes} + implicit inp_out
                val second = let {
                    // remove scopes that are declared in outer Funcs
                    val outers: List<Scope> = tp.ups_tolist().let {
                        val es = it.filter { it is Expr.Func }.let { it as List<Expr.Func> }.map { it.xtype }
                        val ts = it.filter { it is Type.Func }.let { it as List<Type.Func> }
                        (es + ts).map { it?.xscps?.second ?: emptyList() }.flatten()
                    }
                    fun noneInUps (x: Scope): Boolean {
                        return outers.none { it.scp1.id==x.scp1.id }
                    }

                    ((tp.xscps.second ?: emptyList()) + inp_pub_out.filter(::noneInUps))
                        .distinctBy { it.scp1.id }
                        .filter { it.scp1.isscopepar() }
                        
                }
                tp.xscps = Triple(tp.xscps.first, tp.xscps.second ?: second, tp.xscps.third ?: emptyList())
            }
        }
    }
    fun fe (e: Expr) {
        when (e) {
            is Expr.Func -> {
                if (e.xtype?.xscps?.first != null) {
                    return
                }
                val lvlF = 1 + e.ups_tolist().count { it is Stmt.Block }
                var lvlM  = Int.MAX_VALUE
                var scp: Tk.Scp? = null
                fun fx (x: Expr) {
                    when (x) {
                        is Expr.Var -> {
                            if (x.tk_.id in arrayOf("arg","pub","ret","evt")) {
                                return
                            }
                            val env = x.env(x.tk_.id)!!
                            val lvlV = env.ups_tolist().count { it is Stmt.Block }
                            //println(x.tk_.id + ": $lvlF > $lvlV")
                            if (lvlV>0 && lvlF>lvlV && lvlV<lvlM) {
                                lvlM = lvlV
                                scp = env.ups_first_block()!!.let {
                                    if (it.scp1?.id.isanon()) {
                                        it.scp1 = Tk.Scp(TK.XSCP, it.tk.lin, it.tk.col, "X${it.n}")
                                    }
                                    it.scp1!!
                                }
                            }
                        }
                    }
                }
                e.visit(null, ::fx, null, null)
                if (scp != null) {
                    e.xtype?.xscps = Triple(Scope(scp!!,null), e.xtype?.xscps?.second, e.xtype?.xscps?.third)
                }
            }
        }
    }
    fun fs (s: Stmt) {
        when (s) {
            is Stmt.Typedef -> {
                val tps  = s.type.flattenLeft()
                val scps = tps.increasing(false)
                val fst  = ((s.xscp1s.first?.map { Scope(it,null) } ?: emptyList()) + scps)
                    .distinctBy { it.scp1.id }
                    
                //println(tps)
                tps.filter { it is Type.Named && it.xisrec }.let { it as List<Type.Named> }.forEach {
                    it.xscps = it.xscps ?: fst
                }
                s.xscp1s = Pair (
                    s.xscp1s.first  ?: fst.map { it.scp1 },
                    s.xscp1s.second ?: emptyList()
                )
            }
        }
    }
    this.visit(::fs, ::fe, ::ft, null)
}
