// Need to infer:
//  var x: ? = ...
//  var x: _int = input std: ?
//  var x: <(),()> = <.1>: ?
//  var x: _int = _v: ?

fun Type.mapScp1 (up: Any, to: Tk.Id): Type {
    fun Type.aux (): Type {
        return when (this) {
            is Type.Unit, is Type.Nat, is Type.Active, is Type.Actives -> this
            is Type.Tuple   -> Type.Tuple(this.tk_, this.vec.map { it.aux() })
            is Type.Union   -> Type.Union(this.tk_, this.vec.map { it.aux() })
            is Type.Func    -> this
            is Type.Pointer -> Type.Pointer(this.tk_, Scope(to,null), this.pln.aux())
            is Type.Alias   -> Type.Alias(this.tk_, this.xisrec,
                /*listOf(to),*/ this.xscps!!.map{Scope(to,null)})   // TODO: wrong
        }
    }
    return this.aux().clone(up, this.tk.lin, this.tk.col)
}

fun Expr.xinfTypes (inf: Type?) {
    val xinf = inf?.noalias()
    this.wtype = when (this) {
        is Expr.Unit  -> this.wtype!!   //inf ?: this.wtype!!
        is Expr.Nat   -> {
            All_assert_tk(this.tk, this.xtype!=null || inf!=null) {
                "invalid inference : undetermined type"
            }
            this.xtype = this.xtype ?: inf!!.clone(this,this.tk.lin,this.tk.col)
            this.xtype!!
        }
        is Expr.As -> {
            val tp = this.xtype ?: inf
            when (this.tk_.sym) {
                ":+" -> {
                    this.e.xinfTypes(tp?.noalias())
                    if (tp is Type.Alias) {
                        this.xtype = this.xtype ?: (tp.clone(this,this.tk.lin,this.tk.col) as Type.Alias)
                    }
                    this.xtype ?: this.e.wtype!!
                }
                ":-" -> {
                    this.e.xinfTypes(tp)
                    val alias = this.e.wtype.let { if (it is Type.Active) it.tsk else it }
                    if (alias is Type.Alias) {
                        this.xtype = this.xtype ?: (alias.clone(this,this.tk.lin,this.tk.col) as Type.Alias)
                    }
                    (this.xtype ?: this.e.wtype!!).noalias()
                }
                else -> error("bug found")
            }.let { ret ->
                this.e.wtype.let {
                    when (it) {
                        is Type.Active  -> Type.Active (it.tk_,ret).clone(this,this.tk.lin,this.tk.col)
                        is Type.Actives -> Type.Actives(it.tk_,it.len,it).clone(this,this.tk.lin,this.tk.col)
                        else -> ret
                    }
                }
            }
        }

        is Expr.Upref -> {
            All_assert_tk(this.tk, inf==null || xinf is Type.Pointer) { "invalid inference : type mismatch"}
            this.pln.xinfTypes((xinf as Type.Pointer?)?.pln)
            this.pln.wtype!!.let {
                val base = this.toBaseVar()
                val blk  = base?.env(base?.tk_.id)?.ups_first { it is Stmt.Block || it is Expr.Func }
                val id   = when {
                    (base == null) -> "GLOBAL"
                    (blk == null) -> "GLOBAL"
                    else -> base.tk_.id
                }
                val scp1 = Tk.Id(TK.XID,this.tk.lin,this.tk.col, id.toUpperCase())
                Type.Pointer(this.tk_, Scope(scp1,null), this.pln.wtype!!)
            }
        }
        is Expr.Dnref -> {
            this.ptr.xinfTypes(xinf?.let {
                val scp1 = Tk.Id(TK.XID,this.tk.lin,this.tk.col,this.localBlockScp1Id())
                Type.Pointer (
                    Tk.Chr(TK.CHAR,this.tk.lin,this.tk.col,'/'),
                    Scope(scp1,null),
                    it
                )
            })
            this.ptr.wtype!!.let {
                if (it is Type.Nat) it else {
                    All_assert_tk(this.tk, it.noalias() is Type.Pointer) {
                        "invalid operand to `\\´ : not a pointer"
                    }
                    (it.noalias() as Type.Pointer).pln
                }
            }
        }
        is Expr.TCons -> {
            All_assert_tk(this.tk, inf==null || xinf is Type.Tuple) {
                "invalid inference : type mismatch"
            }
            this.arg.forEachIndexed { i,e -> e.xinfTypes(xinf?.let { (it as Type.Tuple).vec[i] }) }
            Type.Tuple(this.tk_, this.arg.map { it.wtype!! })
        }
        is Expr.UCons -> {
            All_assert_tk(this.tk, this.xtype!=null || inf!=null) {
                "invalid inference : undetermined type"
            }
            if (this.xtype != null) {
                val x = (this.xtype!!.noalias() as Type.Union).vec[this.tk_.num - 1]
                this.arg.xinfTypes(x)
                this.xtype!!
            } else {
                assert(inf != null)
                    //.mapScp1(this, Tk.Id(TK.XID, this.tk.lin, this.tk.col,"LOCAL")) // TODO: not always LOCAL
                All_assert_tk(this.tk, xinf is Type.Union) { "invalid inference : type mismatch : expected union : have ${inf!!.tostr()}"}
                this.check(xinf!!)
                val x = (xinf as Type.Union).vec[this.tk_.num-1]
                this.arg.xinfTypes(x)
                this.xtype = xinf.clone(this,this.tk.lin,this.tk.col) as Type.Union
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
            All_assert_tk(this.tk, inf==null || xinf is Type.Pointer) {
                "invalid inference : type mismatch"
            }
            this.arg.xinfTypes((xinf as Type.Pointer?)?.pln)
            if (this.xscp == null) {
                if (xinf is Type.Pointer) {
                    this.xscp = xinf.xscp
                } else {
                    this.xscp = Scope(Tk.Id(TK.XID, this.tk.lin, this.tk.col, this.localBlockScp1Id()), null)
                }
            }
            Type.Pointer (
                Tk.Chr(TK.CHAR, this.tk.lin, this.tk.col, '/'),
                this.xscp!!,
                this.arg.wtype!!
            )
        }
        is Expr.Func -> {
            if (this.xtype == null) {
                assert(xinf != null) { "bug found" }
                All_assert_tk(this.tk, xinf is Type.Func) {
                    "invalid type : expected function type"
                }
                this.xtype = xinf as Type.Func
                this.wtype = this.xtype // must set wtype before b/c block may access arg/ret/etc
            }
            this.block.xinfTypes(null)
            this.xtype ?: xinf!!
        }
        is Expr.TDisc -> {
            this.tup.xinfTypes(null)  // not possible to infer big (tuple) from small (disc)
            this.tup.wtype!!.let {
                All_assert_tk(this.tk, it is Type.Tuple) {
                    "invalid discriminator : type mismatch : expected tuple : have ${it.tostr()}"
                }
                val (MIN, MAX) = Pair(1, (it as Type.Tuple).vec.size)
                All_assert_tk(this.tk, MIN <= this.tk_.num && this.tk_.num <= MAX) {
                    "invalid discriminator : out of bounds"
                }
                it.vec[this.tk_.num - 1]
            }
        }
        is Expr.Field -> {
            this.tsk.xinfTypes(null)  // not possible to infer big (tuple) from small (disc)
            this.tsk.wtype.let {
                All_assert_tk(this.tk, it is Type.Active) {
                    "invalid \"pub\" : type mismatch : expected active task"
                }
                val tsk = (it as Type.Active).tsk
                val ftp = if (tsk is Type.Func) tsk else ((tsk as Type.Active).tsk.noalias() as Type.Func)
                when (this.tk_.id) {
                    "state" -> Type.Nat(Tk.Nat(TK.XNAT, this.tk.lin, this.tk.col, null,"int"))
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
            assert(tk_.num!=0 || tp.isrec()) { "bug found" }

            val (MIN, MAX) = Pair(if (tp.isrec()) 0 else 1, (xtp as Type.Union).vec.size)
            All_assert_tk(this.tk, MIN <= tk_.num && tk_.num <= MAX) {
                "invalid $str : out of bounds"
            }

            when (this) {
                is Expr.UDisc -> if (this.tk_.num == 0) {
                    Type.Unit(Tk.Sym(TK.UNIT, this.tk.lin, this.tk.col, "()"))
                } else {
                    xtp.vec[this.tk_.num - 1]
                }
                is Expr.UPred -> Type.Nat(Tk.Nat(TK.XNAT, this.tk.lin, this.tk.col, null,"int"))
                else -> error("bug found")
            }
        }
        is Expr.Var -> {
            val s = this.env(this.tk_.id)!!
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
            val nat = Type.Nat(Tk.Nat(TK.XNAT, this.tk.lin, this.tk.col, null,""))
            this.f.xinfTypes(nat)    // no infer for functions, default _ for nat

            this.f.wtype!!.noalias().let { ft ->
                when (ft) {
                    is Type.Nat -> {
                        this.arg.xinfTypes(nat)
                        this.xscps = Pair(this.xscps.first ?: emptyList(), this.xscps.second)
                        ft
                    }
                    is Type.Func -> {
                        val e = this

                        // TODO: remove after change increasing?
                        this.arg.xinfTypes(ft.inp.mapScp1(e, Tk.Id(TK.XID, this.tk.lin, this.tk.col,this.localBlockScp1Id())))

                        // Calculates type scopes {...}:
                        //  call f @[...] arg

                        this.xscps = let {
                            // scope of expected closure environment
                            //      var f: func {@LOCAL} -> ...     // f will hold env in @LOCAL
                            //      set f = call g {@LOCAL} ()      // pass it for the builder function

                            fun Type.toScp1s (): List<Tk.Id> {
                                return when (this) {
                                    is Type.Pointer -> listOf(this.xscp!!.scp1)
                                    is Type.Alias   -> this.xscps!!.map { it.scp1 }
                                    is Type.Func    -> this.xscps.first.let { if (it==null) emptyList() else listOf(it.scp1) }
                                    else -> emptyList()
                                }
                            }

                            val clo: List<Pair<Tk.Id,Tk.Id>> = if (xinf is Type.Func && xinf.xscps.first!=null) {
                                listOf(Pair((ft.out as Type.Func).xscps.first!!.scp1,xinf.xscps.first!!.scp1))
                            } else {
                                emptyList()
                            }

                            val ret1s: List<Tk.Id> = if (inf == null) {
                                // no attribution expected, save to @LOCAL as shortest scope possible
                                ft.out.flattenLeft()
                                    .map { it.toScp1s() }
                                    .flatten()
                                    .map { Tk.Id(TK.XID, ft.tk.lin, ft.tk.col, ft.localBlockScp1Id()) }
                            } else {
                                inf.flattenLeft()
                                   .map { it.toScp1s() }
                                   .flatten()
                            }//.filter { it.isscopepar() }  // ignore constant labels (they not args)

                            val inp_out = let {
                                //assert(ret1s.distinctBy { it.id }.size <= 1) { "TODO: multiple pointer returns" }
                                val arg1s: List<Tk.Id> = this.arg.wtype!!.flattenLeft()
                                    .map { it.toScp1s() }
                                    .flatten()
                                // func inp -> out  ==>  { inp, out }
                                val inp_out: List<Tk.Id> = (ft.inp.flattenLeft() + ft.out.flattenLeft())
                                    .map { it.toScp1s() }
                                    .flatten()
                                inp_out.zip(arg1s+ret1s)
                            }

                            // [ (inp,arg), (out,ret) ] ==> remove all repeated inp/out
                            // TODO: what if out/ret are not the same for the removed reps?
                            val scp1s: List<Tk.Id> = (clo + inp_out)
                                .filter { it.first.isscopepar() }  // ignore constant labels (they not args)
                                .distinctBy { it.first.id }
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

                        if (ft.xscps.second!!.size != this.xscps.first!!.size) {
                            // TODO: may fail before check2, return anything
                            Type.Nat(Tk.Nat(TK.NATIVE, this.tk.lin, this.tk.col, null,"ERR"))
                        } else {
                            ft.out.mapScps(true,
                                ft.xscps.second!!.map { it.scp1.id }.zip(this.xscps.first!!).toMap()
                            )
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
        return Type.Unit(Tk.Sym(TK.UNIT, this.tk.lin, this.tk.col, "()")).clone(this, this.tk.lin, this.tk.col)
    }
    when (this) {
        is Stmt.Nop, is Stmt.Break, is Stmt.Return, is Stmt.Native, is Stmt.Throw, is Stmt.Typedef -> {}
        is Stmt.Var -> { this.xtype = this.xtype ?: inf?.clone(this,this.tk.lin,this.tk.col) }
        is Stmt.Set -> {
            try {
                this.dst.xinfTypes(null)
                this.src.xinfTypes(this.dst.wtype!!)
            } catch (e: Throwable){
                this.src.xinfTypes(null)
                this.dst.xinfTypes(this.src.wtype!!)
            }
        }
        is Stmt.SCall -> this.e.xinfTypes(unit())
        is Stmt.SSpawn -> {
            this.call.xinfTypes(null)
            this.dst?.xinfTypes (
                Type.Active (
                    Tk.Key(TK.ACTIVE,this.tk.lin,this.tk.col,"active"),
                    (this.call.noas() as Expr.Call).f.wtype!!.clone(this,this.tk.lin,this.tk.col)
                )
            )
        }
        is Stmt.DSpawn -> {
            this.dst.xinfTypes(null)
            this.call.xinfTypes(null)
        }
        is Stmt.Await -> this.e.xinfTypes(Type.Nat(Tk.Nat(TK.XNAT, this.tk.lin, this.tk.col, null,"int")).clone(this, this.tk.lin, this.tk.col))
        is Stmt.Emit  -> {
            if (this.tgt is Expr) {
                this.tgt.xinfTypes(null)
            }
            this.e.xinfTypes(Type.Alias(Tk.Id(TK.XID, this.tk.lin, this.tk.col,"Event"), false, emptyList() /*null*/).clone(this,this.tk.lin,this.tk.col))
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
            this.tst.xinfTypes(Type.Nat(Tk.Nat(TK.XNAT, this.tk.lin, this.tk.col, null,"int")).clone(this, this.tk.lin, this.tk.col))
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
