fun check_ctrs (up: Any, dcl_scps: Pair<List<Tk.Scp>, List<Pair<String, String>>>, use_scps: List<Scope>): Boolean {
    val pairs = dcl_scps.first.map { it.str }.zip(use_scps!!)
    dcl_scps.second.forEach { ctr ->   // for each constraint
        // check if call args (x,y) respect this contraint
        val x = pairs.find { it.first==ctr.first  }!!.second
        val y = pairs.find { it.first==ctr.second }!!.second
        if (!x.isNestIn(y,up)) {
            return false
        }
    }
    return true
}

fun check_02_after_tps (s: Stmt) {
    fun ft (tp: Type) {
        when (tp) {
            is Type.Named -> {
                val def = tp.env(tp.tk.str) as Stmt.Typedef
                val s1 = def.xscp1s.first!!.size
                val s2 = tp.xscps!!.size
                All_assert_tk(tp.tk, s1 == s2) {    // xsc1ps may not be available in Check_01
                    "invalid type : scope mismatch : expecting $s1, have $s2 argument(s)"
                }
                All_assert_tk(tp.tk, check_ctrs(tp,def.xscp1s.let { Pair(it.first!!,it.second!!) },tp.xscps!!)) {
                    "invalid type : scope mismatch : constraint mismatch"
                }
            }
        }
    }

    fun fe (e: Expr) {
        when (e) {
            is Expr.Cast  -> {
                e.type.let {
                    val dst = e.type
                    val src = e.e.wtype!!
                    All_assert_tk(e.tk, dst.isSupOf(src) || src.isSupOf(dst)) {
                        "invalid type cast : ${mismatch(dst,src)}"
                    }
                }
            }
            is Expr.Pak   -> {
                e.xtype?.let {
                    val dst = it.noactnoalias()
                    val src = e.e.wtype!!.noact()
                    All_assert_tk(e.tk, dst.isSupOf(src)) {
                        "invalid type pack : ${mismatch(dst,src)}"
                    }
                }
            }
            is Expr.Unpak -> {
                All_assert_tk(e.tk, e.isinf || e.e.wtype?.noact().let { it is Type.Named || it is Type.Nat }) {
                    "invalid type unpack : expected type alias : found ${e.e.wtype!!.tostr()}"
                }
            }
            is Expr.UCons -> {
                e.check(e.xtype!!)
                val sup = e.xtype!!.vec[e.tk.field2num(e.xtype!!.yids)!! - 1]
                val sub = e.arg.wtype!!
                All_assert_tk(e.tk, sup.isSupOf(sub)) {
                    "invalid constructor : ${mismatch(sup,sub)}"
                }
            }
            is Expr.New   -> {
                All_assert_tk(e.tk, ((e.arg as Expr.Pak).xtype!! as Type.Named).xisrec) {
                    "invalid `new` : expected recursive type : have "
                }
            }
            is Expr.Call  -> {
                val func = e.f.wtype
                val ret1 = e.wtype!!
                val arg1 = e.arg.wtype!!

                val istask = (func is Type.Func && func.tk.str=="task")
                if (e.upspawn() == null) {
                    All_assert_tk(e.tk, !istask) { "invalid call : unexpected task" }
                } else {
                    All_assert_tk(e.tk, istask) { "invalid spawn : expected task" }
                }

                val (scp1s,inp1,out1) = when (func) {
                    is Type.Func -> Triple(Pair(func.xscps.second!!,func.xscps.third!!),func.inp,func.out)
                    is Type.Nat  -> Triple(Pair(emptyList(),emptyList()),func,func)
                    else -> error("impossible case")
                }

                val s1 = scp1s.first!!.size
                val s2 = e.xscps.first!!.size
                All_assert_tk(e.tk, s1 == s2) {
                    "invalid call : scope mismatch : expecting $s1, have $s2 argument(s)"
                }
                All_assert_tk(e.tk, check_ctrs(e,Pair(scp1s.first.map { it.scp1 },scp1s.second),e.xscps.first!!)) {
                    "invalid call : scope mismatch : constraint mismatch"
                }

                val (inp2,out2) = if (func is Type.Func) {
                    val map = scp1s.first!!.map { it.scp1.str }.zip(e.xscps.first!!).toMap()
                    Pair (
                        inp1.mapScps(false, map).clone(e.tk,e),
                        out1.mapScps(true,  map).clone(e.tk,e)
                    )
                } else {
                    Pair(inp1,out1)
                }

                //val (inp2,out2) = Pair(inp1,out1)
                /*
                //print("INP1: ") ; println(inp1.tostr())
                //print("INP2: ") ; println(inp2.tostr())
                //print("ARG1: ") ; println(arg1.tostr())
                //println("OUT, RET1, RET2")
                //print("OUT1: ") ; println(out1.tostr())
                //print("OUT2: ") ; println(out2.tostr())
                //print("RET1: ") ; println(ret1.tostr())
                */

                val run = (ret1 is Type.Active || ret1 is Type.Actives)
                val ok1 = inp2.isSupOf(arg1)
                val ok2 = run || ret1.isSupOf(out2)
                All_assert_tk(e.f.tk, ok1 && ok2) {
                    if (ok1) {
                        "invalid call : ${mismatch(ret1,out2)}"
                    } else {
                        "invalid call : ${mismatch(inp2,arg1)}"
                    }
                }
            }
            is Expr.If    -> {
                All_assert_tk(e.tk, e.tst.wtype is Type.Nat) {
                    "invalid condition : type mismatch : expected _int : have ${e.tst.wtype!!.tostr()}"
                }
                val t = e.true_.wtype!!
                val f = e.false_.wtype!!
                All_assert_tk(s.tk, t.isSupOf(f) && f.isSupOf(t)) {
                    "invalid \"if\" : ${mismatch(t,f)}"
                }
            }

        }
    }
    fun fs (s: Stmt) {
        when (s) {
            is Stmt.Await -> {
                All_assert_tk(s.tk, s.e.wtype is Type.Active || s.e.wtype.let { it is Type.Nat && it.tk.str=="_int" }) {
                    "invalid condition : type mismatch : expected _int : have ${s.e.wtype!!.tostr()}"
                }
            }
            is Stmt.SSpawn -> {
                val call = s.call.wtype!!
                All_assert_tk(s.tk, call is Type.Active) {
                    "invalid `spawn` : type mismatch : expected active task : have ${call.tostr()}"
                }
                if (s.dst != null) {
                    val dst = s.dst.wtype
                    All_assert_tk(s.dst.tk, dst is Type.Active) {
                        "invalid `spawn` : type mismatch : expected active task : have ${s.dst.wtype!!.tostr()}"
                    }
                    All_assert_tk(s.tk, dst!!.isSupOf(call)) {
                        "invalid `spawn` : ${mismatch(dst!!,call)}"
                    }
                }
            }
            is Stmt.DSpawn -> {
                All_assert_tk(s.dst.tk, s.dst.wtype is Type.Actives) {
                    "invalid `spawn` : type mismatch : expected active tasks : have ${s.dst.wtype!!.tostr()}"
                }
                val call = s.call.unpak() as Expr.Call
                val ftp = call.f.wtype!!
                All_assert_tk(s.call.tk, ftp is Type.Func && ftp.tk.str=="task") {
                    "invalid `spawn` : type mismatch : expected task : have ${ftp.tostr()}"
                }
                val dst = (s.dst.wtype!! as Type.Actives).tsk
                //println("invalid `spawn` : type mismatch : ${dst.str} = ${call.str}")
                val alias = if (call.f !is Expr.Unpak) ftp else call.f.e.wtype!!
                All_assert_tk(s.tk, dst.isSupOf(alias)) {
                    "invalid `spawn` : ${mismatch(dst,alias)}"
                }
            }
            is Stmt.Emit -> {
                All_assert_tk(s.e.tk, s.e.wtype.let { it is Type.Named && it.tk.str=="Event" }) {
                    "invalid `emit` : type mismatch : expected Event : have ${s.e.wtype!!.tostr()}"
                }
            }
            is Stmt.Throw -> {
                All_assert_tk(s.e.tk, s.e.wtype.let { it is Type.Named && it.tk.str=="Error" }) {
                    "invalid `throw` : type mismatch : expected Error : have ${s.e.wtype!!.tostr()}"
                }
            }
            is Stmt.Block -> {
                if (s.catch != null) {
                    All_assert_tk(s.catch.tk, s.catch.wtype.let { it is Type.Nat && it.tk.str == "_int" }) {
                        "invalid `catch` : type mismatch : expected _int : have ${s.catch.wtype!!.tostr()}"
                    }
                }
            }
            is Stmt.If -> {
                All_assert_tk(s.tk, s.tst.wtype is Type.Nat) {
                    "invalid condition : type mismatch : expected _int : have ${s.tst.wtype!!.tostr()}"
                }
            }
            is Stmt.DLoop -> {
                val i    = s.i.wtype!!
                val tsks = s.tsks.wtype!!
                All_assert_tk(s.i.tk, i is Type.Active) {
                    "invalid `loop` : type mismatch : expected task type : have ${i.tostr()}"
                }
                All_assert_tk(s.tsks.tk, tsks is Type.Actives) {
                    "invalid `loop` : type mismatch : expected tasks type : have ${tsks.tostr()}"
                }
                All_assert_tk(s.tk, i.isSupOf(tsks)) {
                    "invalid `loop` : ${mismatch(i,tsks)}"
                }
            }
            is Stmt.Set -> {
                val dst = s.dst.wtype!!
                val src = s.src.wtype!!
                //println(">>> NEW") ;
                //println(s.dst) ; println(s.dst.dump()) ; println(dst.dump()) ; println(dst.tostr())
                //println(s.src) ; println(s.src.dump()) ; println(src.dump()) ; println(src.tostr())
                All_assert_tk(s.tk, dst.isSupOf(src)) {
                    val str = if (s.dst is Expr.Var && s.dst.tk.str == "ret") "return" else "assignment"
                    "invalid $str : ${mismatch(dst,src)}"
                }
            }
        }
    }
    s.visit(::fs, ::fe, ::ft, null)
}
