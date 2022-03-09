fun Scope.check (up: Any) {
    val id = this.scp1.id
    val ok = when {
        (id == "GLOBAL") -> true
        (id == "LOCAL") -> true
        (up.ups_first { it is Type.Func || it is Stmt.Typedef } != null) -> true  // (@i1 -> ...)
        up.env(id).let {                              // { @aaa ... @aaa }
            it is Stmt.Block && (id==it.scp1?.id || id=="B"+it.n) ||
            it is Stmt.Var   && id==it.tk_.id.toUpperCase()
        } -> true
        (up.ups_first {                                     // [@i1, ...] { @i1 }
            it is Stmt.Typedef && (it.xscp1s.first!!.any { it.id==id })
         || it is Expr.Func    && (it.ftp()?.xscps?.second?.any { it.scp1.id==id } ?: false)
        } != null) -> true
        else -> false
    }
    All_assert_tk(this.scp1, ok) {
        "undeclared scope \"@$id\""
    }
}

// need to check UNull/UCons on check_01 (Ce0) and check_02 (Ce1, b/c no type at check_01)

fun Expr.UCons.check (tp: Type) {
    All_assert_tk(tp.tk, tp is Type.Union) { "invalid type : expected union" }
    val uni = tp as Type.Union
    val ok = (uni.vec.size >= this.tk.field2num(uni.yids))
    All_assert_tk(this.tk, ok) {
        "invalid union constructor : out of bounds"
    }
}

fun check_01_before_tps (s: Stmt) {
    fun ft (tp: Type) {
        when (tp) {
            is Type.Pointer -> tp.xscp?.check(tp)
            is Type.Func -> {
                val ptrs = (tp.inp.flattenLeft() + tp.out.flattenLeft()).filter { it is Type.Pointer } as List<Type.Pointer>
                val ok = ptrs.all {
                    val ptr = it.xscp!!.scp1.id
                    when {
                        (ptr == "GLOBAL") -> true
                        (
                            tp.xscps.second?.any { ptr==it.scp1.id } ?: false      // (@i1 -> ...@i1...)
                        ) -> true
                        (tp.ups_first {                     // { @aaa \n ...@aaa... }
                            it is Stmt.Block && it.scp1.let { it!=null && it.id==ptr }
                        } != null) -> true
                        else -> false
                    }
                }
                // all pointers must be listed either in "func.clo" or "func.scps"
                All_assert_tk(tp.tk, ok) {
                    "invalid function type : missing scope argument"
                }
            }
        }
    }
    fun fe (e: Expr) {
        when (e) {
            is Expr.UCons -> {
                if (e.xtype != null) e.check(e.xtype!!)
            }

            is Expr.Func -> {
                val outers: List<Scope> = e.ups_tolist().let {
                    val es = it.filter { it is Expr.Func }.let { it as List<Expr.Func> }.map { it.ftp() }
                    val ts = it.filter { it is Type.Func }.let { it as List<Type.Func> }
                    (es + ts).map { it?.xscps?.second ?: emptyList() }.flatten()
                }
                val err = outers.find { out -> e?.ftp()?.xscps?.second!!.any { it.scp1.id==out.scp1.id } }
                All_assert_tk(e.tk, err==null) {
                    "invalid scope : \"@${err!!.scp1.id}\" is already declared (ln ${err!!.scp1.lin})"
                }
            }

            is Expr.New  -> e.xscp?.check(e)
            is Expr.Call -> {
                e.xscps.second.let { it?.check(e) }
                e.xscps.first?.forEach { it.check(e) }
            }
        }
    }
    s.visit(null, ::fe, ::ft, null)
}
