fun Stmt.setTypes () {
    fun fe (e: Expr) {
        e.wtype = when (e) {
            is Expr.Unit, is Expr.Nat, is Expr.UCons, is Expr.UNull, is Expr.Func -> e.wtype!!
            is Expr.As -> {
                when (e.tk_.sym) {
                    ":+" -> e.xtype ?: e.e.wtype!!
                    ":-" -> e.xtype?.noalias() ?: e.e.wtype!!
                    else -> error("bug found")
                }.let { ret ->
                    e.e.wtype.let {
                        when (it) {
                            is Type.Active  -> Type.Active (it.tk_,ret).clone(e,e.tk.lin,e.tk.col)
                            is Type.Actives -> Type.Actives(it.tk_,it.len,it).clone(e,e.tk.lin,e.tk.col)
                            else -> ret
                        }
                    }
                }
            }
            is Expr.Upref -> {
                val id = e.toBaseVar()?.tk_?.id ?: "GLOBAL"   // uppercase /x -> /X
                val scp1 = Tk.Id(TK.XID,e.tk.lin,e.tk.col, id.toUpperCase())
                Type.Pointer(e.tk_, Scope(scp1,null), e.pln.wtype!!)
            }
            is Expr.Dnref -> e.ptr.wtype.let {
                if (it is Type.Nat) it else {
                    All_assert_tk(e.tk, it is Type.Pointer) {
                        "invalid operand to `\\´ : not a pointer"
                    }
                    (it!! as Type.Pointer).pln
                }
            }
            is Expr.TCons -> Type.Tuple(e.tk_, e.arg.map { it.wtype!! })
            is Expr.New   -> Type.Pointer(Tk.Chr(TK.CHAR,e.tk.lin,e.tk.col,'/'), e.xscp!!, e.arg.wtype!!)
            is Expr.Call -> e.f.wtype.let { tpd ->
                when (tpd) {
                    is Type.Nat -> tpd
                    is Type.Func -> {
                        if (tpd.xscps.second!!.size != e.xscps.first!!.size) {
                            // TODO: may fail before check2, return anything
                            Type.Nat(Tk.Nat(TK.NATIVE, e.tk.lin, e.tk.col, null, "ERR"))
                        } else {
                            tpd.out.mapScps(true,
                                tpd.xscps.second!!.map { it.scp1.id }.zip(e.xscps.first!!).toMap()
                            )
                        }
                    }
                    is Type.Active, is Type.Actives -> {
                        All_assert_tk(e.f.tk, e.wup is Stmt.SSpawn) {
                            "invalid call : not a function"
                        }
                        tpd
                    }
                    else -> {
                        All_assert_tk(e.f.tk, false) {
                            "invalid call : not a function"
                        }
                        error("impossible case")
                    }
                }
            }
            is Expr.TDisc -> e.tup.wtype.let {
                All_assert_tk(e.tk, it is Type.Tuple) {
                    "invalid discriminator : type mismatch : expected tuple : have ${it!!.tostr()}"
                }
                val (MIN, MAX) = Pair(1, (it as Type.Tuple).vec.size)
                All_assert_tk(e.tk, MIN <= e.tk_.num && e.tk_.num <= MAX) {
                    "invalid discriminator : out of bounds"
                }
                it.vec[e.tk_.num - 1]
            }
            is Expr.Field -> e.tsk.wtype.let {
                All_assert_tk(e.tk, it is Type.Active) {
                    "invalid \"pub\" : type mismatch : expected active task : have ${e.tsk.wtype!!.tostr()}"
                }
                when (e.tk_.id) {
                    "state" -> Type.Nat(Tk.Nat(TK.XNAT, e.tk.lin, e.tk.col, null, "int"))
                    "pub"   -> ((it as Type.Active).tsk as Type.Func).pub!!
                    "ret"   -> ((it as Type.Active).tsk as Type.Func).out
                    else -> error("bug found")
                }

            }
            is Expr.UDisc, is Expr.UPred -> {
                val (tk_,uni) = when (e) {
                    is Expr.UPred -> Pair(e.tk_,e.uni)
                    is Expr.UDisc -> Pair(e.tk_,e.uni)
                    else -> error("impossible case")
                }
                val str = if (e is Expr.UDisc) "discriminator" else "predicate"

                val isrec = uni.wtype!!.isrec()
                val tp = uni.wtype!!.noalias()

                All_assert_tk(e.tk, tp is Type.Union) {
                    "invalid $str : not an union"
                }
                All_assert_tk(e.tk, tk_.num!=0 || isrec) {
                    "invalid $str : expected recursive alias"
                }

                val (MIN, MAX) = Pair(if (isrec) 0 else 1, (tp as Type.Union).vec.size)
                All_assert_tk(e.tk, MIN <= tk_.num && tk_.num <= MAX) {
                    "invalid $str : out of bounds"
                }

                when (e) {
                    is Expr.UDisc -> if (e.tk_.num == 0) {
                        Type.Unit(Tk.Sym(TK.UNIT, e.tk.lin, e.tk.col, "()"))
                    } else {
                        tp.vec[e.tk_.num - 1]
                    }
                    is Expr.UPred -> Type.Nat(Tk.Nat(TK.XNAT, e.tk.lin, e.tk.col, null, "int"))
                    else -> error("bug found")
                }
            }
            is Expr.Var -> e.env(e.tk_.id)!!.toType()
            else -> TODO()
        }.clone(e,e.tk.lin,e.tk.col)
    }
    this.visit(null, ::fe, null, null)
}
