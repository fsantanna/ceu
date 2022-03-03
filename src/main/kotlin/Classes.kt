var N = 1

enum class TK {
    ERR, EOF, CHAR,
    XID, XNAT, XNUM, XAS, XCLK,
    UNIT, ARROW, ATBRACK,
    ACTIVE, AWAIT, BREAK, CALL, CATCH, ELSE, EMIT, EVERY, FUNC, IF, IN, INPUT,
    LOOP, NATIVE, NEW, OUTPUT, PAUSE, PAUSEIF, RESUME, PAR, PARAND, PAROR, RETURN, SET, SPAWN, TASK,
    THROW, TYPE, UNTIL, VAR, WATCHING, WHERE, WITH
}

val key2tk: HashMap<String, TK> = hashMapOf (
    "active" to TK.ACTIVE,
    "await"  to TK.AWAIT,
    "break"  to TK.BREAK,
    "call"   to TK.CALL,
    "catch"  to TK.CATCH,
    "else"   to TK.ELSE,
    "emit"   to TK.EMIT,
    "every"  to TK.EVERY,
    "func"   to TK.FUNC,
    "if"     to TK.IF,
    "in"     to TK.IN,
    "input"  to TK.INPUT,
    "loop"   to TK.LOOP,
    "native" to TK.NATIVE,
    "new"    to TK.NEW,
    "output" to TK.OUTPUT,
    "par"    to TK.PAR,
    "parand" to TK.PARAND,
    "paror"  to TK.PAROR,
    "pause"  to TK.PAUSE,
    "pauseif" to TK.PAUSEIF,
    "resume" to TK.RESUME,
    "return" to TK.RETURN,
    "set"    to TK.SET,
    "spawn"  to TK.SPAWN,
    "task"   to TK.TASK,
    "throw"  to TK.THROW,
    "type"   to TK.TYPE,
    "until"  to TK.UNTIL,
    "var"    to TK.VAR,
    "watching" to TK.WATCHING,
    "where"  to TK.WHERE,
    "with"   to TK.WITH,
)

sealed class Type(val tk: Tk, var wup: Any?, var wenv: Any?) {
    data class Unit    (val tk_: Tk.Sym): Type(tk_, null, null)
    data class Nat     (val tk_: Tk.Nat): Type(tk_, null, null)
    data class Tuple   (val tk_: Tk.Chr, val vec: List<Type>): Type(tk_, null, null)
    data class Union   (val tk_: Tk.Chr, val vec: List<Type>): Type(tk_, null, null)
    data class Pointer (val tk_: Tk.Chr, val xscp: Scope?, val pln: Type): Type(tk_, null, null)
    data class Active  (val tk_: Tk.Key, val tsk: Type): Type(tk_, null, null)
    data class Actives (val tk_: Tk.Key, val len: Tk.Num?, val tsk: Type): Type(tk_, null, null)
    data class Func (
        val tk_: Tk.Key,
        val xscps: Triple<Scope,List<Scope>?,List<Pair<String,String>>?>,   // [closure scope, input scopes, input scopes constraints]
        val inp: Type, val pub: Type?, val out: Type
    ): Type(tk_, null, null)
    data class Alias (
        val tk_: Tk.Id,
        var xisrec: Boolean,
        val xscps: List<Scope>?,
    ): Type(tk_, null, null)
}

sealed class Attr(val tk: Tk) {
    data class Var   (val tk_: Tk.Id): Attr(tk_)
    data class Nat   (val tk_: Tk.Nat, val type: Type): Attr(tk_)
    data class As    (val tk_: Tk.Sym, val e: Attr, val type: Type.Alias): Attr(tk_)
    data class Dnref (val tk_: Tk, val ptr: Attr): Attr(tk_)
    data class TDisc (val tk_: Tk.Num, val tup: Attr): Attr(tk_)
    data class UDisc (val tk_: Tk.Num, val uni: Attr): Attr(tk_)
    data class Field (val tk_: Tk.Id, val tsk: Attr): Attr(tk_)
}

sealed class Expr (val n: Int, val tk: Tk, var wup: Any?, var wenv: Any?, var wtype: Type?) {
    data class Unit  (val tk_: Tk.Sym): Expr(N++, tk_, null, null, Type.Unit(tk_))
    data class Var   (val tk_: Tk.Id): Expr(N++, tk_, null, null, null)
    data class Nat   (val tk_: Tk.Nat, val xtype: Type?): Expr(N++, tk_, null, null, xtype)
    data class As    (val tk_: Tk.Sym, val e: Expr, val type: Type.Alias): Expr(N++, tk_, null, null, type)
    data class TCons (val tk_: Tk.Chr, val arg: List<Expr>): Expr(N++, tk_, null, null, null)
    data class UCons (val tk_: Tk.Num, val xtype: Type.Union?, val arg: Expr): Expr(N++, tk_, null, null, xtype)
    data class UNull (val tk_: Tk.Num, val xtype: Type.Pointer?): Expr(N++, tk_, null, null, xtype)
    data class TDisc (val tk_: Tk.Num, val tup: Expr): Expr(N++, tk_, null, null, null)
    data class UDisc (val tk_: Tk.Num, val uni: Expr): Expr(N++, tk_, null, null, null)
    data class UPred (val tk_: Tk.Num, val uni: Expr): Expr(N++, tk_, null, null, null)
    data class New   (val tk_: Tk.Key, val xscp: Scope?, val arg: Expr): Expr(N++, tk_, null, null, null)
    data class Dnref (val tk_: Tk,     val ptr: Expr): Expr(N++, tk_, null, null, null)
    data class Upref (val tk_: Tk.Chr, val pln: Expr): Expr(N++, tk_, null, null, null)
    data class Call  (val tk_: Tk, val f: Expr, val arg: Expr, val xscps: Pair<List<Scope>?,Scope?>): Expr(N++, tk_, null, null, null)
    data class Func  (val tk_: Tk, val xtype: Type.Func?, val block: Stmt.Block) : Expr(N++, tk_, null, null, xtype)
    data class Field (val tk_: Tk.Id, val tsk: Expr): Expr(N++, tk_, null, null, null)
}

sealed class Stmt (val n: Int, val tk: Tk, var wup: Any?, var wenv: Any?) {
    data class Nop    (val tk_: Tk) : Stmt(N++, tk_, null, null)
    data class Var    (val tk_: Tk.Id, val xtype: Type?, val xinfer: String?) : Stmt(N++, tk_, null, null)
    data class Set    (val tk_: Tk.Chr, val dst: Expr, val src: Expr) : Stmt(N++, tk_, null, null)
    data class Native (val tk_: Tk.Nat, val istype: Boolean) : Stmt(N++, tk_, null, null)
    data class SCall  (val tk_: Tk.Key, val e: Expr.Call): Stmt(N++, tk_, null, null)
    data class SSpawn (val tk_: Tk.Key, val dst: Expr?, val call: Expr.Call): Stmt(N++, tk_, null, null)
    data class DSpawn (val tk_: Tk.Key, val dst: Expr, val call: Expr.Call): Stmt(N++, tk_, null, null)
    data class Pause  (val tk_: Tk.Key, val tsk: Expr, val pause: Boolean): Stmt(N++, tk_, null, null)
    data class Await  (val tk_: Tk.Key, val e: Expr): Stmt(N++, tk_, null, null)
    data class Emit   (val tk_: Tk.Key, val tgt: Any, val e: Expr): Stmt(N++, tk_, null, null)
    data class Throw  (val tk_: Tk.Key): Stmt(N++, tk_, null, null)
    data class Input  (val tk_: Tk.Key, val xtype: Type?, val dst: Expr?, val lib: Tk.Id, val arg: Expr): Stmt(N++, tk_, null, null)
    data class Output (val tk_: Tk.Key, val lib: Tk.Id, val arg: Expr): Stmt(N++, tk_, null, null)
    data class Seq    (val tk_: Tk, val s1: Stmt, val s2: Stmt) : Stmt(N++, tk_, null, null)
    data class If     (val tk_: Tk.Key, val tst: Expr, val true_: Block, val false_: Block) : Stmt(N++, tk_, null, null)
    data class Return (val tk_: Tk.Key) : Stmt(N++, tk_, null, null)
    data class Loop   (val tk_: Tk.Key, val block: Block) : Stmt(N++, tk_, null, null)
    data class DLoop  (val tk_: Tk.Key, val i: Expr.Var, val tsks: Expr, val block: Block) : Stmt(N++, tk_, null, null)
    data class Break  (val tk_: Tk.Key) : Stmt(N++, tk_, null, null)
    data class Block  (val tk_: Tk.Chr, val iscatch: Boolean, var scp1: Tk.Id?, val body: Stmt) : Stmt(N++, tk_, null, null)
    data class Typedef (
        val tk_: Tk.Id,
        val xscp1s: Pair<List<Tk.Id>?,List<Pair<String,String>>?>,
        val type: Type
    ) : Stmt(N++, tk_, null, null)
}
