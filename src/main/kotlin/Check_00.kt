fun check_00_after_envs (s: Stmt) {
    fun ft (tp: Type) {
        when (tp) {
            is Type.Named -> {
                val def = tp.env(tp.tk.str)
                All_assert_tk(tp.tk, def is Stmt.Typedef) {
                    "undeclared type \"${tp.tk.str}\""
                }
            }
        }
    }
    fun fe (e: Expr) {
        when (e) {
            is Expr.Var -> {
                if (e.tk.str == "evt") {
                    All_assert_tk(e.tk, e.env("Event") != null) {
                        "undeclared type \"Event\""
                    }
                }
                All_assert_tk(e.tk, e.env(e.tk.str) != null) {
                    "undeclared variable \"${e.tk.str}\""
                }
            }

            is Expr.Upref -> {
                var track = false   // start tracking count if crosses UDisc
                var count = 1       // must remain positive after track (no uprefs)
                for (ee in e.flattenRight()) {
                    count = when (ee) {
                        is Expr.UDisc -> { track=true ; 1 }
                        is Expr.Dnref -> count+1
                        is Expr.Upref -> count-1
                        else -> count
                    }
                }
                All_assert_tk(e.tk, !track || count>0) {
                    "invalid operand to `/´ : union discriminator"
                }
            }
        }
    }
    fun fs (s: Stmt) {
        when (s) {
            is Stmt.Var -> {
                val dcl = s.env(s.tk.str)
                All_assert_tk(s.tk, dcl == null) {
                    "invalid declaration : \"${s.tk.str}\" is already declared (ln ${dcl!!.getTk().lin})"
                }
            }
            is Stmt.Return -> {
                val ok = s.ups_first { it is Expr.Func } != null
                All_assert_tk(s.tk, ok) {
                    "invalid return : no enclosing function"
                }
            }
            is Stmt.Block -> {
                s.scp1?.let {
                    val dcl = s.env(it.str)
                    All_assert_tk(it, dcl == null) {
                        "invalid scope : \"@${it.str}\" is already declared (ln ${dcl!!.getTk().lin})"
                    }
                }
                if (s.iscatch) {
                    All_assert_tk(s.tk, s.ups_first { it is Expr.Func } != null) {
                        "invalid `catch` : requires enclosing task"
                    }
                }
            }
            is Stmt.Typedef -> {
                val dcl = s.env(s.tk.str) as Stmt.Typedef?
                if (dcl == null) {
                    All_assert_tk(s.tk, !s.isinc) {
                        "invalid declaration : \"${s.tk.str}\" is not yet declared"
                    }
                    s.xtype = s.type.clone(s.type.tk, s, s)
                } else {
                    All_assert_tk(s.tk, s.isinc) {
                        "invalid declaration : \"${s.tk.str}\" is already declared (ln ${dcl.tk.lin})"
                    }
                    All_assert_tk(s.tk, dcl.type is Type.Union) {
                        "invalid declaration : \"${s.tk.str}\" must be of union type (ln ${dcl.tk.lin})"
                    }
                    if (s.isinc) {
                        val old = dcl.xtype as Type.Union
                        val inc = s.type.setUpEnv(dcl,dcl) as Type.Union
                        assert(inc.common == null) { "TODO" }

                        val yids = when {
                            (old.yids==null && inc.yids==null) -> null
                            (old.yids!=null && inc.yids!=null) -> old.yids + inc.yids
                            else -> error("TODO")
                        }

                        // TODO: check funcs should not mutate the AST
                        dcl.xisact = false
                        s.xtype = Type.Union(old.tk_, old.common, old.vec+inc.vec, yids).setUpEnv(dcl,dcl)
                    }
                }
                val isrec = s.type.flattenLeft().any { it is Type.Named && it.tk.str==s.tk.str }
                if (isrec) {
                    All_assert_tk(s.tk, s.type !is Type.Pointer) {
                        "invalid recursive type : cannot be a pointer"
                    }
                }
            }
            is Stmt.Emit -> {
                All_assert_tk(s.tk, s.env("Event")!=null) {
                    "invalid `emit` : undeclared type \"Event\""
                }
            }
        }
    }
    s.visit(::fs, ::fe, ::ft, null)
}
