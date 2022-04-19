import java.lang.Integer.max

// Need to infer:
//  var x: ? = ...
//  var x: _int = input std: ?
//  var x: <(),()> = <.1>: ?
//  var x: _int = _v: ?

fun Stmt.setXargs () {
    // set Type.Named.xargs=${} when typedef is also empty
    fun ft (tp: Type) {
        when (tp) {
            is Type.Named -> {
                if (tp.def()!!.pars.isEmpty()) {
                    tp.xargs = emptyList()
                }
            }
        }
    }
    this.visit(null, null, ::ft, null)
}

fun Type.mapScp1 (up: Any, to: Tk.Scp): Type {
    fun Type.aux (): Type {
        return when (this) {
            is Type.Unit, is Type.Nat, is Type.Active, is Type.Actives, is Type.Par -> this
            is Type.Tuple   -> Type.Tuple(this.tk_, this.vec.map { it.aux() }, this.yids)
            is Type.Union   -> Type.Union(this.tk_, this.common?.aux() as Type.Tuple?, this.vec.map { it.aux() }, this.yids)
            is Type.Func    -> this
            is Type.Pointer -> Type.Pointer(this.tk_, Scope(to,null), this.pln.aux())
            is Type.Named   -> Type.Named(this.tk_, this.subs, this.xisrec, this.xargs?.map { it.aux() },
                /*listOf(to),*/ this.xscps!!.map{Scope(to,null)}, null)   // TODO: wrong
        }
    }
    return this.aux().clone(this.tk,up)
}

fun Type.isConcrete (): Boolean {
    return !this.flattenLeft().any {
        it is Type.Named && it.xargs==null || it is Type.Par && it.xtype==null
    }
}
fun Type.hasPar (): Boolean {
    return !this.flattenLeft().any {
        it is Type.Par && it.xtype==null
    }
}

fun Expr.xinfTypes (inf_: Type?) {
    val inf = when {
        (inf_ !is Type.Par) -> inf_
        (inf_.xtype != null) -> inf_.xtype!!
        else -> null
    }
    this.wtype = when (this) {
        is Expr.Unit  -> this.wtype!!   //inf ?: this.wtype!!
        is Expr.Nat   -> {
            All_assert_tk(this.tk, this.xtype!=null || inf!=null) {
                "invalid inference : cannot determine type"
            }
            val ret = this.xtype ?: inf!!.clone(this.tk, this)
            if (this.xtype==null && ret.isConcrete()) {
                this.xtype = ret
            }
            ret
        }
        is Expr.Cast -> {
            this.e.xinfTypes(this.type)
            this.type
        }
        is Expr.Named -> {
            val tp = this.xtype?.second as Type.Named?
            val infArgs = (tp?.xargs == null)
            if (infArgs) {
                tp?.xargs = emptyList()
            }
            when {
                // explicit type
                (this.xtype != null) -> {
                    tp as Type.Named
                    this.e.xinfTypes(tp.nm_uact_uname_act(this))
                    if (infArgs) {
                        tp.xargs = tp.def()!!.uninstantiate(this.e.wtype!!)
                        All_assert_tk(this.tk, tp.xargs != null) {
                            "invalid inference : cannot determine type"
                        }
                        if (!this.e.wtype!!.isConcrete()) {
                            // reinfer this.e to populate missing parameters
                            this.e.xinfTypes(tp.nm_uact_uname_act(this))
                        }
                        All_assert_tk(this.tk, this.e.wtype!!.isConcrete()) {
                            "invalid inference : cannot determine type"
                        }
                    }
                    if (!this.xtype!!.first) tp else {
                        Type.Active(
                            Tk.Fix("active", this.tk.lin, this.tk.col),
                            tp
                        )
                    }
                }
                // no explicit, but inf is Named, so set this.xtype
                (inf!=null && inf.nm_isActiveNamed()) -> {
                    val nm_inf  = inf.nm_uact()
                    this.e.xinfTypes(inf.nm_uact_uname_act(this))
                    //println(this.e.wtype?.dump())
                    nm_inf.xargs = nm_inf.def()!!.uninstantiate(this.e.wtype!!)
                    All_assert_tk(this.tk, nm_inf.xargs != null) {
                        "invalid inference : cannot determine type"
                    }
                    //println(nm_inf.xargs)
                    if (!this.e.wtype!!.isConcrete()) {
                        // reinfer this.e to populate missing parameters
                        this.e.xinfTypes(inf.nm_uact_uname_act(this))
                    }
                    assert(this.e.wtype!!.isConcrete())
                    val ret = inf.clone(this.tk, this)
                    assert(ret.isConcrete())
                    this.xtype = if (inf is Type.Active || inf is Type.Actives) {
                        Pair(true, ret.nm_uact())
                    } else {
                        Pair(false, ret)
                    }
                    ret
                }
                // no explicit, not Named, do not set this.xtype
                else -> {
                    this.e.xinfTypes(inf)
                    this.e.wtype!!
                }
            }
       }
        is Expr.UNamed -> {
            this.e.xinfTypes(inf)
            if (this.e.wtype!!.nm_isActiveNamed()) {
                this.e.wtype!!.nm_uact_uname_act(this)
            } else {
                this.e.wtype!!
            }
        }
        is Expr.Upref -> {
            All_assert_tk(this.tk, inf==null || inf is Type.Nat || inf is Type.Pointer) { "invalid inference : type mismatch"}
            val pln = when (inf) {
                is Type.Nat -> inf
                is Type.Pointer -> inf.pln
                else -> null
            }
            this.pln.xinfTypes(pln)
            this.pln.wtype!!.let {
                val base = this.toBaseVar()
                val blk  = base?.env(base?.tk.str)?.ups_first { it is Stmt.Block || it is Expr.Func }
                val id   = when {
                    (base == null) -> "GLOBAL"
                    (blk == null) -> "GLOBAL"
                    else -> base.tk.str
                }
                val scp1 = Tk.Scp(id.toUpperCase(),this.tk.lin,this.tk.col)
                Type.Pointer(this.tk_, Scope(scp1,null), this.pln.wtype!!)
            }
        }
        is Expr.Dnref -> {
            this.ptr.xinfTypes(inf?.let {
                val scp1 = Tk.Scp(this.localBlockScp1Id(),this.tk.lin,this.tk.col)
                Type.Pointer (
                    Tk.Fix("/",this.tk.lin,this.tk.col),
                    Scope(scp1,null),
                    it
                ).setUpEnv(inf.getUp()!!)
            })
            this.ptr.wtype!!.let {
                if (it is Type.Nat) it else {
                    All_assert_tk(this.tk, it is Type.Pointer) {
                        "invalid operand to `\\´ : not a pointer"
                    }
                    (it as Type.Pointer).pln
                }
            }
        }
        is Expr.TCons -> {
            if (inf != null) {
                All_assert_tk(this.tk, inf is Type.Tuple) {
                    "invalid inference : type mismatch"
                }
                inf as Type.Tuple
                All_assert_tk(this.tk, inf.vec.size >= this.arg.size) {
                    "invalid constructor : out of bounds"
                }
            }
            inf as Type.Tuple?
            this.arg.forEachIndexed { i,e ->
                if (inf is Type.Tuple && this.yids!=null) {
                    val id = this.yids[i]
                    val idx = id.field2num(inf.yids)
                    All_assert_tk(id, idx != null) {
                        "invalid constructor : unknown discriminator \"${id.str}\""
                    }
                    All_assert_tk(id, idx == i+1) {
                        "invalid constructor : invalid position for \"${id.str}\""
                    }
                }
                e.xinfTypes(if (inf==null) null else inf.vec[i])
            }
            Type.Tuple(this.tk_, this.arg.map { it.wtype!! }, null)
        }
        is Expr.UCons -> {
            All_assert_tk(this.tk, this.xtype!=null || inf!=null) {
                "invalid inference : cannot determine type"
            }
            this.check(this.xtype ?: inf!!)
            val num = ((this.xtype ?: inf) as Type.Union).yids.let { this.tk.field2num(it) }!!
            if (this.xtype != null) {
                val x = this.xtype!!.vec[num-1]
                this.arg.xinfTypes(x)
                this.xtype!!
            } else {
                assert(inf != null)
                    //.mapScp1(this, Tk.Id(TK.XID, this.tk.lin, this.tk.col,"LOCAL")) // TODO: not always LOCAL
                All_assert_tk(this.tk, inf is Type.Union) { "invalid inference : type mismatch : expected union : have ${inf!!.tostr()}"}
                inf as Type.Union
                val idx = inf.vec[num-1]
                this.arg.xinfTypes(idx)
                val ret = Type.Union (
                    inf.tk_,
                    inf.common,
                    inf.vec.take(num-1) + this.arg.wtype!! + inf.vec.takeLast(inf.vec.size-num),
                    inf.yids
                ).clone(this.tk,this) as Type.Union
                if (ret.isConcrete()) {
                    this.xtype = ret
                } else {
                    // not a concrete type yet
                }
                ret
            }
        }
        is Expr.UNull -> {
            All_assert_tk(this.tk, this.xtype!=null || inf!=null) {
                "invalid inference : cannot determine type"
            }
            val ret = this.xtype ?: (inf!!.clone(this.tk,this) as Type.Pointer)
            if (this.xtype==null && ret.isConcrete()) {
                this.xtype = ret
            }
            ret
                //.mapScp1(this, Tk.Id(TK.XID, this.tk.lin, this.tk.col,"LOCAL")) // TODO: not always LOCAL
        }
        is Expr.New -> {
            All_assert_tk(this.tk, inf==null || inf is Type.Pointer) {
                "invalid inference : type mismatch"
            }
            this.arg.xinfTypes((inf as Type.Pointer?)?.pln)
            if (this.xscp == null) {
                if (inf is Type.Pointer) {
                    this.xscp = inf.xscp
                } else {
                    this.xscp = Scope(Tk.Scp(this.localBlockScp1Id(), this.tk.lin, this.tk.col), null)
                }
            }
            Type.Pointer (
                Tk.Fix("/", this.tk.lin, this.tk.col),
                this.xscp!!,
                this.arg.wtype!!
            )
        }
        is Expr.Func -> {
            if (this.xtype == null) {
                assert(inf != null) { "bug found" }
                All_assert_tk(this.tk, inf is Type.Func) {
                    "invalid type : expected function type"
                }
                inf as Type.Func
                assert(inf.isConcrete())
                this.xtype = inf.clone(this.tk, this) as Type.Func
                this.wtype = this.xtype // must set wtype before b/c block may access arg/ret/etc
            }
            this.block.xinfTypes(null)
            this.xtype ?: inf!!
        }
        is Expr.TDisc -> {
            this.tup.xinfTypes(null)  // not possible to infer big (tuple) from small (disc)
            this.tup.wtype!!.let {
                All_assert_tk(this.tk, it is Type.Tuple) {
                    "invalid discriminator : type mismatch : expected tuple : have ${it.tostr()}"
                }
                it as Type.Tuple
                val num = this.tk.field2num(it.yids)
                All_assert_tk(this.tk, num != null) {
                    "invalid discriminator : unknown \"${this.tk.str}\""
                }
                All_assert_tk(this.tk, 1 <= num!! && num!! <= it.vec.size) {
                    "invalid discriminator : out of bounds"
                }
                it.vec[num - 1]
            }
        }
        is Expr.Field -> {
            this.tsk.xinfTypes(null)  // not possible to infer big (tuple) from small (disc)
            this.tsk.wtype!!.let {
                All_assert_tk(this.tk, it is Type.Active) {
                    "invalid \"pub\" : type mismatch : expected active task"
                }
                val ftp = it.act_uact() as Type.Func
                when (this.tk.str) {
                    "status" -> Type.Nat(Tk.Nat("_int", this.tk.lin, this.tk.col))
                    "pub"   -> ftp.pub!!
                    "ret"   -> ftp.out
                    else    -> error("bug found")

                }
            }
        }
        is Expr.UDisc -> {
            // not possible to infer big (union) from small (disc/pred)
            this.uni.xinfTypes(null)
            val xtp = this.uni.wtype!!
            All_assert_tk(this.tk, xtp is Type.Union) {
                "invalid discriminator : not an union"
            }
            xtp as Type.Union

            val num = this.tk.field2num(xtp.yids)
            All_assert_tk(this.tk, num != null) {
                "invalid discriminator : unknown discriminator \"${this.tk.str}\""
            }
            val MIN = if (xtp.common == null) 1 else 0
            All_assert_tk(this.tk, MIN <= num!! && num!! <= xtp.vec.size) {
                "invalid discriminator : out of bounds"
            }

            if (num == 0) {
                xtp.common!!
            } else {
                xtp.vec[num!! - 1]
            }
        }
        is Expr.UPred -> {
            // not possible to infer big (union) from small (disc/pred)
            this.uni.xinfTypes(null)
            val xtp = this.uni.wtype!!
            All_assert_tk(this.tk, xtp is Type.Union) {
                "invalid predicate : not an union"
            }
            xtp as Type.Union

            if (this.tk.str != "Null") {
                val num = this.tk.field2num(xtp.yids)
                All_assert_tk(this.tk, num != null) {
                    "invalid predicate : unknown discriminator \"${this.tk.str}\""
                }
                val MIN = if (xtp.common == null) 1 else 0
                All_assert_tk(this.tk, MIN <= num!! && num!! <= xtp.vec.size) {
                    "invalid predicate : out of bounds"
                }
            }

            val num = this.tk.field2num(xtp.yids)
            when {
                (this.wup !is Expr.UPred) -> Type.Nat(Tk.Nat("_int", this.tk.lin, this.tk.col))
                (num == 0) -> xtp.common!!
                else -> xtp.vec[num!! - 1]
            }
        }
        is Expr.If -> {
            this.tst.xinfTypes(Type.Nat(Tk.Nat("_int", this.tk.lin, this.tk.col)).setUpEnv(this))
            this.true_.xinfTypes(inf)
            this.false_.xinfTypes(inf)
            this.true_.wtype!!
        }
        is Expr.Var -> {
            val s = this.env(this.tk.str)!!
            //println(this.getUp())
            val ret = when {
                (s !is Stmt.Var) -> s.getType()
                // TODO: hack to substitute s.xtype if currently "_" (see x18_clone_rec)
                //(s.xtype.let { it==null || it is Type.Nat && it.tk.str=="_" }) -> {
                s.xtype.let { it!=null && (it.isConcrete() || inf==null || !inf.isConcrete()) } -> {
                    s.xtype!!
                }
                (inf == null) -> null
                else -> {
                    val ret = inf.clone(this.tk, this)
                    All_assert_tk(ret.tk, ret.isConcrete()) {
                        "invalid inference : cannot determine type"
                    }
                    if (s.xtype.let { it==null || it.hasPar() }) {
                        s.xtype = ret  // set var.xtype=inf if Stmt.Var doesn't know its type yet
                    }
                    ret
                }
            }
            All_assert_tk(this.tk, ret != null) {
                "invalid inference : cannot determine type"
            }
            ret!!
        }
        is Expr.Call -> {
            val nat = Type.Nat(Tk.Nat("_", this.tk.lin, this.tk.col)).setUpEnv(this)
            this.f.xinfTypes(nat)    // no infer for functions, default _ for nat

            this.f.wtype!!.let { ftp ->
                when (ftp) {
                    is Type.Nat -> {
                        this.arg.xinfTypes(nat)
                        this.xscps = Pair(this.xscps.first ?: emptyList(), this.xscps.second)
                        ftp
                    }
                    is Type.Func -> {
                        val e = this

                        // TODO: remove after change increasing?
                        this.arg.xinfTypes(ftp.inp.mapScp1(e, Tk.Scp(this.localBlockScp1Id(), this.tk.lin, this.tk.col)))

                        // Calculates type scopes {...}:
                        //  call f @[...] arg

                        this.xscps = let {
                            // scope of expected closure environment
                            //      var f: func {@LOCAL} -> ...     // f will hold env in @LOCAL
                            //      set f = call g {@LOCAL} ()      // pass it for the builder function

                            fun Type.toScp1s (): List<Tk.Scp> {
                                return when (this) {
                                    is Type.Pointer -> listOf(this.xscp!!.scp1)
                                    is Type.Named   -> this.xscps!!.map { it.scp1 }
                                    is Type.Func    -> listOf(this.xscps.first.scp1)
                                    else -> emptyList()
                                }
                            }

                            /*
                            val clo: List<Pair<Tk.Scp,Tk.Scp>> = if (this.upspawn()==null && inf is Type.Func) {
                                listOf(Pair((ftp.out as Type.Func).xscps.first.scp1,inf.xscps.first.scp1))
                            } else {
                                emptyList()
                            }
                             */

                            val ret1s: List<Tk.Scp> = let {
                                val ftps = ftp.out.flattenLeft()
                                    .map { it.toScp1s() }
                                    .flatten()
                                    // no attribution expected, save to @LOCAL as shortest scope possibl
                                    .map { Tk.Scp(ftp.localBlockScp1Id(), ftp.tk.lin, ftp.tk.col) }
                                val infs = inf?.flattenLeft()?.map { it.toScp1s() }?.flatten() ?: emptyList()
                                infs + ftps.takeLast(max(0,ftps.size-infs.size))
                            }//.filter { it.isscopepar() }  // ignore constant labels (they not args)

                            val inp_out = let {
                                //assert(ret1s.distinctBy { it.str }.size <= 1) { "TODO: multiple pointer returns" }
                                val arg1s: List<Tk.Scp> = this.arg.wtype!!.flattenLeft()
                                    .map { it.toScp1s() }
                                    .flatten()
                                // func inp -> out  ==>  { inp, out }
                                val inp_out: List<Tk.Scp> = (ftp.inp.flattenLeft() + ftp.out.flattenLeft())
                                    .map { it.toScp1s() }
                                    .flatten()
                                inp_out.zip(arg1s+ret1s)
                            }

                            // [ (inp,arg), (out,ret) ] ==> remove all repeated inp/out
                            // TODO: what if out/ret are not the same for the removed reps?
                            val scp1s: List<Tk.Scp> = (/*clo +*/ inp_out)
                                .filter { it.first.isscopepar() }  // ignore constant labels (they not args)
                                .distinctBy { it.first.str }
                                .map { it.second }

                            Pair (
                                this.xscps.first  ?: scp1s.map { Scope(it,null) },
                                this.xscps.second ?: if (ret1s.size==0) null else Scope(ret1s[0],null)
                            )
                        }

                        // calculates return of "e" call based on "e.f" function type
                        // "e" passes "e.arg" with "e.scp1s.first" scopes which may affect "e.f" return scopes
                        // we want to map these input scopes into "e.f" return scopes
                        //  var f: func /@a1 -> /@b_1
                        //              /     /---/
                        //  call f {@scp1,@scp2}  -->  /@scp2
                        //  f passes two scopes, first goes to @a1, second goes to @b_1 which is the return
                        //  so @scp2 maps to @b_1
                        // zip [[{@scp1a,@scp1b},{@scp2a,@scp2b}],{@a1,@b_1}]

                        when {
                            (this.upspawn() != null) -> {
                                Type.Active (
                                    Tk.Fix("active",this.tk.lin,this.tk.col),
                                    this.f.wtype!!
                                )
                            }
                            (ftp.xscps.second!!.size != this.xscps.first!!.size) -> {
                                // TODO: may fail before check2, return anything
                                Type.Nat(Tk.Nat("_ERR", this.tk.lin, this.tk.col))
                            }
                            else -> {
                                ftp.out.mapScps(
                                    true,
                                    ftp.xscps.second!!.map { it.scp1.str }.zip(this.xscps.first!!).toMap()
                                )
                            }
                        }
                    }
                    else -> {
                        All_assert_tk(this.f.tk, false) {
                            "invalid call : not a function"
                        }
                        error("impossible case")
                    }
                }
            }
        }
    }.let {
        if (inf_ is Type.Par /*&& it !is Type.Par*/) {
            assert(it !is Type.Par)
            inf_.xtype = it.clone(this.tk, this)
        }
        it.clone(this.tk, this)
    }
}

fun Stmt.xinfTypes (inf: Type? = null) {
    fun unit (): Type {
        return Type.Unit(Tk.Fix("()", this.tk.lin, this.tk.col)).setUpEnv(this)
    }
    when (this) {
        is Stmt.Nop, is Stmt.Native, is Stmt.XBreak, is Stmt.XReturn, is Stmt.Typedef -> {}
        is Stmt.Var -> { assert(inf == null) }
        is Stmt.Set -> {
            this.wup.let {
                if (it is Stmt.Seq && it.s2==this && it.s1 is Stmt.Var && it.s1.xtype==null) {
                    if (this.src is Expr.Func) {
                        // TODO: remove hack
                        it.s1.xtype = this.src.xtype!!.clone(it.s1.tk, it.s1)
                    }
                }
            }
            try {
                this.dst.xinfTypes(null)
                this.src.xinfTypes(this.dst.wtype!!)
                assert(this.src.wtype!!.isConcrete())
                this.dst.xinfTypes(this.src.wtype!!)    // var x: /List = ... -- (make x concrete)
            } catch (e: Throwable){
                if (e.message.let { it!=null && !it.contains("invalid inference") }) {
                    throw e
                }
                this.src.xinfTypes(null)
                this.dst.xinfTypes(this.src.wtype!!)
                assert(this.dst.wtype!!.isConcrete())
                //this.src.xinfTypes(this.dst.wtype!!)    // var x: /List = ... -- (make x concrete)
                //assert(this.src.wtype!!.isConcrete())
            }
        }
        is Stmt.SCall -> this.e.xinfTypes(unit())
        is Stmt.SSpawn -> {
            try {
                this.dst!!.xinfTypes(null)
                this.call.xinfTypes(this.dst!!.wtype!!)
            } catch (e: Throwable) {
                if (e.message.let { it!=null && !it.contains("invalid inference") }) {
                    throw e
                }
                this.call.xinfTypes(null)
                this.dst?.xinfTypes(this.call.wtype!!)
            }
        }
        is Stmt.DSpawn -> {
            this.dst.xinfTypes(null)
            this.call.xinfTypes(null)
        }
        is Stmt.Await -> this.e.xinfTypes(Type.Nat(Tk.Nat("_int", this.tk.lin, this.tk.col)).setUpEnv(this))
        is Stmt.Throw -> this.e.xinfTypes(Type.Named(Tk.Id("Error", this.tk.lin, this.tk.col), emptyList(), false, emptyList(), emptyList(), null).setUpEnv(this))
        is Stmt.Emit  -> {
            if (this.tgt is Expr) {
                this.tgt.xinfTypes(null)
            }
            this.e.xinfTypes(Type.Named(Tk.Id("Event", this.tk.lin, this.tk.col), emptyList(), false, emptyList(), emptyList(), null).setUpEnv(this))
        }
        is Stmt.Pause -> this.tsk.xinfTypes(null)
        is Stmt.Input -> {
            //All_assert_tk(this.tk, this.xtype!=null || inf!=null) {
            //    "invalid inference : cannot determine type"
            //}
            // inf is at least Unit
            this.arg.xinfTypes(null)
            this.dst?.xinfTypes(null)
            this.xtype = this.xtype ?: (this.dst?.wtype ?: inf)?.clone(this.tk,this) ?: unit()
        }
        is Stmt.Output -> this.arg.xinfTypes(null)  // no inf b/c output always depends on the argument
        is Stmt.If -> {
            this.tst.xinfTypes(Type.Nat(Tk.Nat("_int", this.tk.lin, this.tk.col)).setUpEnv(this))
            this.true_.xinfTypes(null)
            this.false_.xinfTypes(null)
        }
        is Stmt.Loop -> this.block.xinfTypes(null)
        is Stmt.DLoop -> {
            this.tsks.xinfTypes(null)
            this.i.xinfTypes(null)
            this.block.xinfTypes(null)
        }
        is Stmt.Block -> {
            this.catch?.xinfTypes(Type.Nat(Tk.Nat("_int", this.tk.lin, this.tk.col)).setUpEnv(this))
            this.body.xinfTypes(null)
        }
        is Stmt.Seq -> {
            this.s1.xinfTypes(null)
            this.s2.xinfTypes(null)
        }
        else -> TODO(this.toString())
    }
}
