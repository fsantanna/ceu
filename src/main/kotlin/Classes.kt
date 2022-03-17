var N = 1

enum class TK {
    ERR, EOF, CHAR,
    Xide, XIde, XIDE, XSCP, XNAT, XNUM, XCLK,
    CAST, UNIT, ARROW, ATBRACK,
    ACTIVE, AWAIT, BREAK, CALL, CATCH, ELSE, EMIT, FUNC,
    IF, IN, INPUT, LOOP, NATIVE, NEW, NULL, OUTPUT, PAUSE,
    RESUME, RETURN, SET, SPAWN, TASK, THROW, TYPE, VAR,
    XDEFER, XEVERY, XPAUSEIF, XPAR, XPARAND, XPAROR,
    XUNTIL, XWATCHING, XWHERE, XWITH
}

val key2tk: HashMap<String, TK> = hashMapOf (
    "active" to TK.ACTIVE,
    "await"  to TK.AWAIT,
    "break"  to TK.BREAK,
    "call"   to TK.CALL,
    "catch"  to TK.CATCH,
    "else"   to TK.ELSE,
    "emit"   to TK.EMIT,
    "func"   to TK.FUNC,
    "if"     to TK.IF,
    "in"     to TK.IN,
    "input"  to TK.INPUT,
    "loop"   to TK.LOOP,
    "native" to TK.NATIVE,
    "new"    to TK.NEW,
    "Null"   to TK.NULL,
    "output" to TK.OUTPUT,
    "pause"  to TK.PAUSE,
    "resume" to TK.RESUME,
    "return" to TK.RETURN,
    "set"    to TK.SET,
    "spawn"  to TK.SPAWN,
    "task"   to TK.TASK,
    "throw"  to TK.THROW,
    "type"   to TK.TYPE,
    "var"    to TK.VAR,
    "defer"  to TK.XDEFER,
    "every"  to TK.XEVERY,
    "par"    to TK.XPAR,
    "parand" to TK.XPARAND,
    "paror"  to TK.XPAROR,
    "pauseif" to TK.XPAUSEIF,
    "until"  to TK.XUNTIL,
    "watching" to TK.XWATCHING,
    "where"  to TK.XWHERE,
    "with"   to TK.XWITH,
)

sealed class Tk (
    val enu: TK,
    val str: String,
    val lin: Int,
    val col: Int,
) {
    data class Err (val enu_: TK, val str_: String, val lin_: Int, val col_: Int): Tk(enu_,str_,lin_,col_)
    data class Sym (val enu_: TK, val str_: String, val lin_: Int, val col_: Int): Tk(enu_,str_,lin_,col_)
    data class Chr (val enu_: TK, val str_: String, val lin_: Int, val col_: Int):   Tk(enu_,str_,lin_,col_)
    data class Key (val enu_: TK, val str_: String, val lin_: Int, val col_: Int): Tk(enu_,str_,lin_,col_)
    data class ide (val enu_: TK, val str_: String, val lin_: Int, val col_: Int):  Tk(enu_,str_,lin_,col_)
    data class Ide (val enu_: TK, val str_: String, val lin_: Int, val col_: Int):  Tk(enu_,str_,lin_,col_)
    data class IDE (val enu_: TK, val str_: String, val lin_: Int, val col_: Int):  Tk(enu_,str_,lin_,col_)
    data class Scp (val enu_: TK, val str_: String, val lin_: Int, val col_: Int):  Tk(enu_,str_,lin_,col_)
    data class Nat (val enu_: TK, val str_: String, val lin_: Int, val col_: Int, val chr: Char?): Tk(enu_,str_,lin_,col_)
    data class Num (val enu_: TK, val str_: String, val lin_: Int, val col_: Int, val num: Int):    Tk(enu_,str_,lin_,col_)
    data class Clk (val enu_: TK, val str_: String, val lin_: Int, val col_: Int, val ms: Int):     Tk(enu_,str_,lin_,col_)
}

sealed class Type(val tk: Tk, var wup: Any?, var wenv: Any?) {
    data class Unit    (val tk_: Tk.Sym): Type(tk_, null, null)
    data class Nat     (val tk_: Tk.Nat): Type(tk_, null, null)
    data class Tuple   (val tk_: Tk.Chr, val vec: List<Type>, val yids: List<Tk.ide>?): Type(tk_, null, null)
    data class Union   (val tk_: Tk.Chr, val common: Type.Tuple?, val vec: List<Type>, val yids: List<Tk.Ide>?): Type(tk_, null, null)
    data class Pointer (val tk_: Tk.Chr, var xscp: Scope?, val pln: Type): Type(tk_, null, null)
    data class Active  (val tk_: Tk.Key, val tsk: Type): Type(tk_, null, null)
    data class Actives (val tk_: Tk.Key, val len: Tk.Num?, val tsk: Type): Type(tk_, null, null)
    data class Func (
        val tk_: Tk.Key,
        var xscps: Triple<Scope,List<Scope>?,List<Pair<String,String>>?>,   // [closure scope, input scopes, input scopes constraints]
        val inp: Type, val pub: Type?, val out: Type
    ): Type(tk_, null, null)
    data class Named (
        val tk_: Tk.Ide,
        val subs: List<Tk>,
        var xisrec: Boolean,
        var xscps: List<Scope>?,
    ): Type(tk_, null, null)
}

sealed class Attr(val tk: Tk) {
    data class Var   (val tk_: Tk.ide): Attr(tk_)
    data class Nat   (val tk_: Tk.Nat, val type: Type): Attr(tk_)
    data class Unpak (val tk_: Tk.Chr, val isinf: Boolean, val e: Attr): Attr(tk_)
    data class Dnref (val tk_: Tk, val ptr: Attr): Attr(tk_)
    data class TDisc (val tk_: Tk, val tup: Attr): Attr(tk_)
    data class UDisc (val tk_: Tk, val uni: Attr): Attr(tk_)
    data class Field (val tk_: Tk.ide, val tsk: Attr): Attr(tk_)
}

// Expr.Pak.xtype is Type.Named [except for Type.Active(Type.Named)]

sealed class Expr (val n: Int, val tk: Tk, var wup: Any?, var wenv: Any?, var wtype: Type?) {
    data class Unit  (val tk_: Tk.Sym): Expr(N++, tk_, null, null, Type.Unit(tk_))
    data class Var   (val tk_: Tk.ide): Expr(N++, tk_, null, null, null)
    data class Nat   (val tk_: Tk.Nat, var xtype: Type?): Expr(N++, tk_, null, null, xtype)
    data class Cast  (val tk_: Tk.Sym, val e: Expr, val type: Type): Expr(N++, tk_, null, null, type)
    data class Pak   (val tk_: Tk, val e: Expr, var isact: Boolean?, var xtype: Type?): Expr(N++, tk_, null, null, xtype)
    data class Unpak (val tk_: Tk.Chr, val isinf: Boolean, val e: Expr): Expr(N++, tk_, null, null, null)
    data class TCons (val tk_: Tk.Chr, val arg: List<Expr>, val yids: List<Tk.ide>?): Expr(N++, tk_, null, null, null)
    data class UCons (val tk_: Tk, var xtype: Type.Union?, val arg: Expr): Expr(N++, tk_, null, null, xtype)
    data class UNull (val tk_: Tk.Key, var xtype: Type.Pointer?): Expr(N++, tk_, null, null, xtype)
    data class TDisc (val tk_: Tk, val tup: Expr): Expr(N++, tk_, null, null, null)
    data class UDisc (val tk_: Tk, val uni: Expr): Expr(N++, tk_, null, null, null)
    data class UPred (val tk_: Tk, val uni: Expr): Expr(N++, tk_, null, null, null)
    data class New   (val tk_: Tk.Key, var xscp: Scope?, val arg: Expr): Expr(N++, tk_, null, null, null)
    data class Dnref (val tk_: Tk,     val ptr: Expr): Expr(N++, tk_, null, null, null)
    data class Upref (val tk_: Tk.Chr, val pln: Expr): Expr(N++, tk_, null, null, null)
    data class Call  (val tk_: Tk, val f: Expr, val arg: Expr, var xscps: Pair<List<Scope>?,Scope?>): Expr(N++, tk_, null, null, null)
    data class Func  (val tk_: Tk, var xtype: Type.Func?, val block: Stmt.Block) : Expr(N++, tk_, null, null, xtype)
    data class Field (val tk_: Tk.ide, val tsk: Expr): Expr(N++, tk_, null, null, null)
}

sealed class Stmt (val n: Int, val tk: Tk, var wup: Any?, var wenv: Any?) {
    data class Nop    (val tk_: Tk) : Stmt(N++, tk_, null, null)
    data class Var    (val tk_: Tk.ide, var xtype: Type?) : Stmt(N++, tk_, null, null)
    data class Set    (val tk_: Tk.Chr, val dst: Expr, val src: Expr) : Stmt(N++, tk_, null, null)
    data class Native (val tk_: Tk.Nat, val istype: Boolean) : Stmt(N++, tk_, null, null)
    data class SCall  (val tk_: Tk.Key, val e: Expr.Call): Stmt(N++, tk_, null, null)
    data class SSpawn (val tk_: Tk.Key, val dst: Expr?, val call: Expr): Stmt(N++, tk_, null, null)
    data class DSpawn (val tk_: Tk.Key, val dst: Expr, val call: Expr): Stmt(N++, tk_, null, null)
    data class Pause  (val tk_: Tk.Key, val tsk: Expr, val pause: Boolean): Stmt(N++, tk_, null, null)
    data class Await  (val tk_: Tk.Key, val e: Expr): Stmt(N++, tk_, null, null)
    data class Emit   (val tk_: Tk.Key, val tgt: Any, val e: Expr): Stmt(N++, tk_, null, null)
    data class Throw  (val tk_: Tk.Key): Stmt(N++, tk_, null, null)
    data class Input  (val tk_: Tk.Key, var xtype: Type?, val dst: Expr?, val lib: Tk.ide, val arg: Expr): Stmt(N++, tk_, null, null)
    data class Output (val tk_: Tk.Key, val lib: Tk.ide, val arg: Expr): Stmt(N++, tk_, null, null)
    data class Seq    (val tk_: Tk, val s1: Stmt, val s2: Stmt) : Stmt(N++, tk_, null, null)
    data class If     (val tk_: Tk.Key, val tst: Expr, val true_: Block, val false_: Block) : Stmt(N++, tk_, null, null)
    data class Return (val tk_: Tk.Key) : Stmt(N++, tk_, null, null)
    data class Loop   (val tk_: Tk.Key, val block: Block) : Stmt(N++, tk_, null, null)
    data class DLoop  (val tk_: Tk.Key, val i: Expr.Var, val tsks: Expr, val block: Block) : Stmt(N++, tk_, null, null)
    data class Break  (val tk_: Tk.Key) : Stmt(N++, tk_, null, null)
    data class Block  (val tk_: Tk.Chr, val iscatch: Boolean, var scp1: Tk.Scp?, val body: Stmt) : Stmt(N++, tk_, null, null)
    data class Typedef (
        val tk_: Tk.Ide,
        var xscp1s: Pair<List<Tk.Scp>?,List<Pair<String,String>>?>,
        val type: Type
    ) : Stmt(N++, tk_, null, null)
}
