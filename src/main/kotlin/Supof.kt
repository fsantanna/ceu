// Convert function signatures to increasing scopes for comparison
// var g: func @[] -> {@i,@j,@k} -> [/</^@i>@i,/</^@j>@j] -> /</^@k>@k
//      becomes
// var g: func @[] -> {@a,@b,@c} -> [/</^@a>@a,/</^@b>@b] -> /</^@c>@c
fun Type.Func.mapLabels (up: Any): Type.Func {
    val fst = this.xscps.first.let { if (it==null) emptyList() else listOf(it) }
    val snd = this.xscps.second!!.map { it }
    val scps: List<String> = (fst + snd).map { it.scp1.id }
    val MAP: Map<String, String> = scps.zip((1..scps.size).map { 'a'+it-1+"" }).toMap()
    fun Type.aux (): Type {
        return when (this) {
            is Type.Active, is Type.Actives -> TODO()
            is Type.Unit, is Type.Nat, is Type.Alias -> this
            is Type.Tuple   -> Type.Tuple(this.tk_, this.vec.map { it.aux() }, this.ids)
            is Type.Union   -> Type.Union(this.tk_, this.vec.map { it.aux() }, this.ids)
            is Type.Func    -> this
            is Type.Pointer -> this.xscp.let {
                val id = MAP[it!!.scp1.id]
                if (id == null) {
                    this
                } else {
                    // TODO: scp2 = null
                    val scp = Scope(Tk.Id(TK.XID, this.tk.lin, this.tk.col, id), null)
                    Type.Pointer(this.tk_, scp, this.pln.aux())
                }
            }
        }
    }
    return Type.Func (
        this.tk_,
        this.xscps,
        this.inp.aux(),
        this.pub?.aux(),
        this.out.aux()
    ).clone(up,this.tk.lin,this.tk.col) as Type.Func
}

fun Scope.isNestIn (sub: Scope, up: Any): Boolean {
    val bothcst = (this.scp2!!.second==null && sub.scp2!!.second==null)
    val bothpar = (this.scp2!!.second!=null && sub.scp2!!.second!=null)

    //println(this.xscp1)
    //println(sub.xscp1)
    //println(bothcst)
    //println(bothpar)
    //println(this)
    //println(sub)
    //println(this.ups_tolist())

    return when {
        (sub.scp2!!.second==null && sub.scp2!!.third==0) -> true           // global as source is always ok
        bothcst -> (this.scp2!!.third!! >= sub.scp2!!.third!!)
        bothpar -> this.scp2!!.second!! == sub.scp2!!.second!! || (up.ups_first { it is Expr.Func } as Expr.Func).let {
            // look for (this.id > sub.id) in constraints
            it.ftp()!!.xscps.third!!.any { it.first==this.scp2!!.second!! && it.second==sub.scp2!!.second!! }
        }
        else -> (sub.scp2!!.second!=null && this.scp2!!.first==sub.scp2!!.first)
        // diff abs/rel -> this must be par and bot must be at the same lvl
    }
}

fun Type.isSupOf (sub: Type, isproto: Boolean=false): Boolean {
    return when {
        (this is Type.Nat  || sub is Type.Nat) -> true
        (this is Type.Active && sub is Type.Actives) -> this.tsk.isSupOf(sub.tsk)
        (this is Type.Active && sub is Type.Active)  -> this.tsk.isSupOf(sub.tsk)
        (this is Type.Alias && sub is Type.Alias) -> (this.tk_.id == sub.tk_.id)    // TODO: check scopes
        (this::class != sub::class) -> false
        (this is Type.Unit && sub is Type.Unit) -> true
        (this is Type.Func && sub is Type.Func) -> {
            val sup2 = this.mapLabels(this.wup!!)
            val sub2 = sub.mapLabels(sub.wup!!)
            (
                sup2.xscps.first.scp2!!.third!! >= sub2.xscps.first.scp2!!.third!! &&
                sup2.inp.isSupOf(sub2.inp,true) &&
                sub2.inp.isSupOf(sup2.inp,true) &&
                sup2.out.isSupOf(sub2.out,true) &&
                sub2.out.isSupOf(sup2.out,true) &&
                (
                    (sup2.pub==null && sub2.pub==null) ||
                    ( sup2.pub!=null && sub2.pub!=null &&
                      sup2.pub.isSupOf(sub2.pub,true) &&
                      sub2.pub.isSupOf(sup2.pub,true) )
                )
            )
        }
        (this is Type.Pointer && sub is Type.Pointer) -> {
            /*
            println("===")
            println(this)
            println(sub)
            println(this.tostr())
            println(sub.tostr())
            //println("SUPOF [$isproto] ${this.tk.lin}: ${this.scope()} = ${sub.scope()} /// ${this.scope}")
            */
            val ok = if (isproto) { // comparing func prototypes does not depend on scope calculation
                (this.xscp!!.scp1.id == sub.xscp!!.scp1.id)
            } else {
                this.xscp!!.isNestIn(sub.xscp!!, this)
            }
            ok && this.pln.isSupOf(sub.pln,isproto)
        }
        (this is Type.Tuple && sub is Type.Tuple) ->
            (this.vec.size==sub.vec.size) && this.vec.zip(sub.vec).all { (x,y) -> x.isSupOf(y,isproto) }
        (this is Type.Union && sub is Type.Union) -> {
            if ((this.vec.size == sub.vec.size)) {
                // ok
            } else {
                return false
            }
            return this.vec.zip(sub.vec).all { (x,y) -> x.isSupOf(y,isproto) }
        }
        else -> false
    }
}
