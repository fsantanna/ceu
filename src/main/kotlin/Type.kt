/*
fun Type.flattenRight (): List<Type> {
    // TODO: func/union do not make sense?
    return when (this) {
        is Type.Unit, is Type.Nat, is Type.Rec -> listOf(this)
        is Type.Tuple -> this.vec.map { it.flattenRight() }.flatten() + this
        is Type.Union -> this.vec.map { it.flattenRight() }.flatten() + this
        is Type.Func  -> listOf(this) //this.inp.flattenRight() + this.out.flattenRight() + this
        is Type.Ptr   -> this.pln.flattenRight() + this
    }
}
 */

fun Type.flattenLeft (): List<Type> {
    // TODO: func/union do not make sense?
    return when (this) {
        is Type.Unit, is Type.Nat, is Type.Named -> listOf(this)
        is Type.Tuple   -> listOf(this) + this.vec.map { it.flattenLeft() }.flatten()
        is Type.Union   -> listOf(this) + this.common.let { if (it==null) emptyList() else listOf(it) } + this.vec.map { it.flattenLeft() }.flatten()
        is Type.Func    -> listOf(this) //this.inp.flatten() + this.out.flatten()
        is Type.Active  -> listOf(this) + this.tsk.flattenLeft()
        is Type.Actives -> listOf(this) + this.tsk.flattenLeft()
        is Type.Pointer -> listOf(this) + this.pln.flattenLeft()
    }
}

fun Type.setUpEnv (up: Any): Type {
    this.wup  = up
    this.wenv = up.getEnv()
    return this
}

fun Type.clone (up: Any, lin: Int, col: Int): Type {
    fun Tk.clone (): Tk {
        return when (this) {
            is Tk.Err -> this.copy(lin_ = lin, col_ = col)
            is Tk.Eof -> this.copy(lin_ = lin, col_ = col)
            is Tk.Fix -> this.copy(lin_ = lin, col_ = col)
            is Tk.id  -> this.copy(lin_ = lin, col_ = col)
            is Tk.Id  -> this.copy(lin_ = lin, col_ = col)
            is Tk.ID  -> this.copy(lin_ = lin, col_ = col)
            is Tk.Scp -> this.copy(lin_ = lin, col_ = col)
            is Tk.Nat -> this.copy(lin_ = lin, col_ = col)
            is Tk.Num -> this.copy(lin_ = lin, col_ = col)
            is Tk.Clk -> this.copy(lin_ = lin, col_ = col)
        }
    }
    fun Type.aux (lin: Int, col: Int): Type {
        return when (this) {
            is Type.Unit -> Type.Unit(this.tk.clone() as Tk.Fix)
            is Type.Named -> Type.Named(this.tk.clone() as Tk.Id, this.subs.map { it.clone() }, this.xisrec, this.xscps)
            is Type.Nat -> Type.Nat(this.tk.clone() as Tk.Nat)
            is Type.Tuple -> Type.Tuple(
                this.tk.clone() as Tk.Fix,
                this.vec.map { it.aux(lin, col) },
                this.yids?.map { it.clone() as Tk.id }
            )
            is Type.Union -> Type.Union(
                this.tk.clone() as Tk.Fix,
                this.common?.aux(lin, col) as Type.Tuple?,
                this.vec.map { it.aux(lin, col) },
                this.yids?.map { it.clone() as Tk.Id }
            )
            is Type.Func -> Type.Func(
                this.tk.clone() as Tk.Fix,
                this.xscps.let {
                    Triple (
                        Scope(it.first.scp1.clone() as Tk.Scp, it.first.scp2),
                        it.second?.map { Scope(it.scp1.clone() as Tk.Scp, it.scp2) },
                        it.third
                    )
                },
                this.inp.aux(lin, col),
                this.pub?.aux(lin, col),
                this.out.aux(lin, col)
            )
            is Type.Active -> Type.Active (
                this.tk_.copy(lin_ = lin, col_ = col),
                this.tsk.aux(lin, col)
            )
            is Type.Actives -> Type.Actives (
                this.tk_.copy(lin_ = lin, col_ = col),
                this.len,
                this.tsk.aux(lin, col)
            )
            is Type.Pointer -> Type.Pointer(
                this.tk_.copy(lin_ = lin, col_ = col),
                Scope(this.xscp!!.scp1.copy(lin_=lin,col_=col), this.xscp!!.scp2),
                this.pln.aux(lin, col)
            )
        }.let {
            it.setUpEnv(up)
        }
    }
    return this.aux(lin,col)
}

fun Expr.Func.ftp (): Type.Func? {
    return this.wtype as Type.Func?
}

fun Type.isrec (): Boolean {
    return this.flattenLeft().any { it is Type.Named && it.xisrec }
}

fun Type.noact (): Type {
    return when (this) {
        is Type.Active -> this.tsk
        is Type.Actives -> this.tsk
        else -> this
    }
}

fun Type.noactnoalias (): Type {
    return this.noact().unpack()
}

fun Type.react_noalias (up: Expr): Type {
    val noalias = this.noactnoalias().clone(up,up.tk.lin,up.tk.col)
    return when (this) {
        is Type.Active  -> Type.Active(this.tk_, noalias).setUpEnv(this.getUp()!!)
        is Type.Actives -> Type.Actives(this.tk_, this.len, noalias).setUpEnv(this.getUp()!!)
        else            -> noalias
    }
}

fun Type.unpack (): Type {
    return if (this !is Type.Named) this else {
        val def = this.env(this.tk.str)!! as Stmt.Typedef

        // Original constructor:
        //      typedef Pair @[a] = [/_int@a,/_int@a]
        //      var xy: Pair @[LOCAL] = [/x,/y]
        // Transform typedef -> type
        //      typedef Pair @[LOCAL] = [/_int@LOCAL,/_int@LOCAL]
        //      var xy: Pair @[LOCAL] = [/x,/y]

        def.toType().mapScps(false,
            def.xscp1s.first!!.map { it.str }.zip(this.xscps!!).toMap()
        ).clone(this,this.tk.lin,this.tk.col)
    }
}

fun Type.toce (): String {
    return when (this) {
        is Type.Unit    -> "Unit"
        is Type.Pointer -> "P_" + this.pln.toce() + "_P"
        is Type.Named   -> this.tk.str
        is Type.Nat     -> this.tk_.payload().replace('*','_')
        is Type.Tuple   -> "T_" + this.vec.map { it.toce() }.joinToString("_") + "_T"
        is Type.Union   -> "U_" + this.vec.map { it.toce() }.joinToString("_") + "_U"
        is Type.Func    -> "F_" + (if (this.tk.str=="task") "TK_" else "") + this.inp.toce() + "_" + (this.pub?.toce()?:"") + "_" + this.out.toce() + "_F"
        is Type.Active   -> "S_" + this.tsk.toce() + "_S"
        is Type.Actives  -> "SS_" + this.tsk.toce() + "_SS"
    }
}

fun mismatch (sup: Type, sub: Type): String {
    return "type mismatch :\n    ${sup.tostr(ispak=true)}\n    ${sub.tostr(ispak=true)}"
}

// Original call:
//      var f: (func @[a1]->/()@a1->())
//      call f @[LOCAL] /x
// Map from f->call
//      { a1=(scp1(LOCAL),scp2(LOCAL) }
// Transform f->call
//      var f: (func @[LOCAL]->/()@LOCAL -> ())
//      call f @[LOCAL] /x
// Transform typedef -> type
//      typedef Pair @[LOCAL] = [/_int@LOCAL,/_int@LOCAL]
//      var xy: Pair @[LOCAL] = [/x,/y]
//// (comment above/below were from diff funs that were merged)
// Map return scope of "e" call based on "e.arg" applied to "e.f" scopes
// calculates return of "e" call based on "e.f" function type
// "e" passes "e.arg" scopes which may affect "e.f" return scopes
// we want to map these input scopes into "e.f" return scopes
//  var f: func /@a1 -> /@b1
//              /     /---/
//  call f {@scp1,@scp2}  -->  /@scp2
//  f passes two scopes, first goes to @a1, second goes to @b1 which is the return
//  so @scp2 maps to @b1
// zip [[{@scp1a,@scp1b},{@scp2a,@scp2b}],{@a1,@b1}]
fun Type.mapScps (dofunc: Boolean, map: Map<String, Scope>): Type {
    fun Scope.idx(): Scope {
        return map[this.scp1.str] ?: this
    }
    return when (this) {
        is Type.Pointer -> Type.Pointer(this.tk_, this.xscp!!.idx(), this.pln.mapScps(dofunc,map))
        is Type.Tuple   -> Type.Tuple(this.tk_, this.vec.map { it.mapScps(dofunc,map) }, this.yids)
        is Type.Union   -> Type.Union(this.tk_, this.common?.mapScps(dofunc,map) as Type.Tuple?, this.vec.map { it.mapScps(dofunc,map) }, this.yids)
        is Type.Named   -> Type.Named(this.tk_, this.subs, this.xisrec, this.xscps!!.map { it.idx() })
        is Type.Func -> if (!dofunc) this else {
            Type.Func(
                this.tk_,
                Triple (
                    this.xscps.first.idx(),
                    this.xscps.second,
                    this.xscps.third
                ),
                this.inp.mapScps(dofunc,map),
                this.pub?.mapScps(dofunc,map),
                this.out.mapScps(dofunc,map)
            )
        }
        is Type.Unit, is Type.Nat, is Type.Active, is Type.Actives -> this
    }
}
