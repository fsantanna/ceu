fun check_00_after_envs (s: Stmt) {
    fun ft (tp: Type) {
        when (tp) {
            is Type.Named -> {
                val def = tp.env(tp.tk_.id)
                All_assert_tk(tp.tk, def is Stmt.Typedef) {
                    "undeclared type \"${tp.tk_.id}\""
                }
            }
        }
    }
    fun fe (e: Expr) {
        when (e) {
            is Expr.Var -> {
                if (e.tk_.id == "evt") {
                    All_assert_tk(e.tk, e.env("Event") != null) {
                        "undeclared type \"Event\""
                    }
                }
                All_assert_tk(e.tk, e.env(e.tk_.id) != null) {
                    "undeclared variable \"${e.tk_.id}\""
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
        fun Any.toTk (): Tk {
            return when (this) {
                is Type         -> this.tk
                is Stmt.Var     -> this.tk
                is Stmt.Block   -> this.tk
                is Stmt.Typedef -> this.tk
                else -> error("bug found")
            }
        }

        when (s) {
            is Stmt.Var -> {
                val dcl = s.env(s.tk_.id)
                All_assert_tk(s.tk, dcl == null) {
                    "invalid declaration : \"${s.tk_.id}\" is already declared (ln ${dcl!!.toTk().lin})"
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
                    val dcl = s.env(it.id)
                    All_assert_tk(it, dcl == null) {
                        "invalid scope : \"@${it.id}\" is already declared (ln ${dcl!!.toTk().lin})"
                    }
                }
                if (s.iscatch) {
                    All_assert_tk(s.tk, s.ups_first { it is Expr.Func } != null) {
                        "invalid `catch` : requires enclosing task"
                    }
                }
            }
            is Stmt.Typedef -> {
                val isrec = s.type.flattenLeft().any { it is Type.Named && it.tk_.id==s.tk_.id }
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
