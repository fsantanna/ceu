// Need to infer:
//  var x: ? = ...
//  var x: _int = input std: ?
//  var x: <(),()> = <.1>: ?
//  var x: _int = _v: ?

fun Type.mapScp1 (up: Any, to: Tk.Scp): Type {
    fun Type.aux (): Type {
        return when (this) {
            is Type.Unit, is Type.Nat, is Type.Active, is Type.Actives -> this
            is Type.Tuple   -> Type.Tuple(this.tk_, this.vec.map { it.aux() }, this.yids)
            is Type.Union   -> Type.Union(this.tk_, this.common?.aux() as Type.Tuple?, this.vec.map { it.aux() }, this.yids)
            is Type.Func    -> this
            is Type.Pointer -> Type.Pointer(this.tk_, Scope(to,null), this.pln.aux())
            is Type.Named   -> Type.Named(this.tk_, this.subs, this.xisrec,
                /*listOf(to),*/ this.xscps!!.map{Scope(to,null)})   // TODO: wrong
        }
    }
    return this.aux().clone(up, this.tk.lin, this.tk.col)
}

fun Expr.xinfTypes (inf: Type?) {
    /*
    val inf = if (inf_ !is Type.Named) inf_ else {
        Type.Named(inf_.tk_, emptyList(), inf_.xisrec, inf_.xscps).setUpEnv(inf_.getUp()!!)
    }
     */
    this.wtype = when (this) {
        is Expr.Unit  -> this.wtype!!   //inf ?: this.wtype!!
        is Expr.Nat   -> {
            All_assert_tk(this.tk, this.xtype!=null || inf!=null) {
                "invalid inference : undetermined type"
            }
            this.xtype = this.xtype ?: inf!!.clone(this,this.tk.lin,this.tk.col)
            this.xtype!!
        }
        is Expr.Cast -> {
            this.e.xinfTypes(inf)
            this.type
        }
        is Expr.Pak -> {
            when {
                (this.xtype != null) -> {
                    val tp = this.xtype!!
                    val unpak = tp.react_noalias(this)
                    this.e.xinfTypes(unpak)
                    if (!this.isact!!) tp else {
                        Type.Active(
                            Tk.Fix("active", this.tk.lin, this.tk.col),
                            tp
                        )
                    }
                }
                (inf != null) -> {
                    this.e.xinfTypes(inf.react_noalias(this))
                    if (inf.noact() is Type.Named) {
                        //this.isact = tp is Type.Active
                        this.xtype = inf
                    }
                    inf
                }
                else -> {
                    this.e.xinfTypes(null)
                    this.e.wtype!!.react_noalias(this)
                }
            }
        }
        is Expr.Unpak -> {
            this.e.xinfTypes(inf)
            this.e.wtype!!.react_noalias(this)
        }

        is Expr.Upref -> {
            All_assert_tk(this.tk, inf==null || inf is Type.Pointer) { "invalid inference : type mismatch"}
            this.pln.xinfTypes((inf as Type.Pointer?)?.pln)
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
                "invalid inference : undetermined type"
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
                val x = (inf as Type.Union).vec[num-1]
                this.arg.xinfTypes(x)
                this.xtype = inf.clone(this,this.tk.lin,this.tk.col) as Type.Union
                this.xtype!!
            }
        }
        is Expr.UNull -> {
            All_assert_tk(this.tk, this.xtype!=null || inf!=null) {
                "invalid inference : undetermined type"
            }
            this.xtype = this.xtype ?: (inf?.clone(this,this.tk.lin,this.tk.col) as Type.Pointer)
            this.xtype!!
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
                this.xtype = inf as Type.Func
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
                val ftp = it.noactnoalias() as Type.Func
                when (this.tk.str) {
                    "status" -> Type.Nat(Tk.Nat("_int", this.tk.lin, this.tk.col))
                    "pub"   -> ftp.pub!!
                    "ret"   -> ftp.out
                    else    -> error("bug found")

                }
            }
        }
        is Expr.UDisc, is Expr.UPred -> {
            // not possible to infer big (union) from small (disc/pred)
            val (tk_,uni) = when (this) {
                is Expr.UPred -> { this.uni.xinfTypes(null) ; Pair(this.tk_,this.uni) }
                is Expr.UDisc -> { this.uni.xinfTypes(null) ; Pair(this.tk_,this.uni) }
                else -> error("impossible case")
            }
            val tp = uni.wtype!!
            val xtp = tp.noalias()

            val str = if (this is Expr.UDisc) "discriminator" else "predicate"

            All_assert_tk(this.tk, xtp is Type.Union) {
                "invalid $str : not an union"
            }
            xtp as Type.Union
            assert(tk.str!="Null" || tp.isrec()) { "bug found" }

            val num = if (this.tk.str == "Null") null else {
                val num = this.tk.field2num(xtp.yids)
                All_assert_tk(this.tk, num != null) {
                    "invalid discriminator : unknown discriminator \"${this.tk.str}\""
                }
                val MIN = if (xtp.common == null) 1 else 0
                All_assert_tk(this.tk, MIN <= num!! && num!! <= xtp.vec.size) {
                    "invalid $str : out of bounds"
                }
                num
            }

            when (this) {
                is Expr.UDisc -> if (num == 0) xtp.common!! else xtp.vec[num!! - 1]
                is Expr.UPred -> Type.Nat(Tk.Nat("_int", this.tk.lin, this.tk.col))
                else -> error("bug found")
            }
        }
        is Expr.Var -> {
            val s = this.env(this.tk.str)!!
            val ret = when {
                (s !is Stmt.Var)  -> s.toType()
                (s.xtype == null) -> {
                    s.xtype = inf
                    inf
                }
                else              -> s.xtype!!
            }
            All_assert_tk(this.tk, ret != null) {
                "invalid inference : undetermined type"
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

                            val clo: List<Pair<Tk.Scp,Tk.Scp>> = if (this.upspawn()==null && inf is Type.Func) {
                                listOf(Pair((ftp.out as Type.Func).xscps.first.scp1,inf.xscps.first.scp1))
                            } else {
                                emptyList()
                            }

                            val ret1s: List<Tk.Scp> = if (inf == null) {
                                // no attribution expected, save to @LOCAL as shortest scope possible
                                ftp.out.flattenLeft()
                                    .map { it.toScp1s() }
                                    .flatten()
                                    .map { Tk.Scp(ftp.localBlockScp1Id(), ftp.tk.lin, ftp.tk.col) }
                            } else {
                                inf.flattenLeft()
                                   .map { it.toScp1s() }
                                   .flatten()
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
                            val scp1s: List<Tk.Scp> = (clo + inp_out)
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
    }.clone(this, this.tk.lin, this.tk.col)
}

fun Stmt.xinfTypes (inf: Type? = null) {
    fun unit (): Type {
        return Type.Unit(Tk.Fix("()", this.tk.lin, this.tk.col)).setUpEnv(this)
    }
    when (this) {
        is Stmt.Nop, is Stmt.Break, is Stmt.Return, is Stmt.Native, is Stmt.Throw, is Stmt.Typedef -> {}
        is Stmt.Var -> { this.xtype = this.xtype ?: inf?.clone(this,this.tk.lin,this.tk.col) }
        is Stmt.Set -> {
            try {
                this.dst.xinfTypes(null)
                this.src.xinfTypes(this.dst.wtype!!)
            } catch (e: Throwable){
                if (e.message.let { it!=null && !it.contains("invalid inference") }) {
                    throw e
                }
                this.src.xinfTypes(null)
                this.dst.xinfTypes(this.src.wtype!!)
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
        is Stmt.Emit  -> {
            if (this.tgt is Expr) {
                this.tgt.xinfTypes(null)
            }
            this.e.xinfTypes(Type.Named(Tk.Id("Event", this.tk.lin, this.tk.col), emptyList(), false, emptyList() /*null*/).setUpEnv(this))
        }
        is Stmt.Pause -> this.tsk.xinfTypes(null)
        is Stmt.Input -> {
            //All_assert_tk(this.tk, this.xtype!=null || inf!=null) {
            //    "invalid inference : undetermined type"
            //}
            // inf is at least Unit
            this.arg.xinfTypes(null)
            this.dst?.xinfTypes(null)
            this.xtype = this.xtype ?: (this.dst?.wtype ?: inf)?.clone(this,this.tk.lin,this.tk.col) ?: unit()
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
        is Stmt.Block -> this.body.xinfTypes(null)
        is Stmt.Seq -> {
            this.s1.xinfTypes(null)
            this.s2.xinfTypes(null)
        }
        else -> TODO(this.toString())
    }
}
