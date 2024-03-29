import java.util.*

var N = 1

val keywords: SortedSet<String> = sortedSetOf (
    "active", "await", "break", "call", "catch", "else", "emit", "func",
    "if", "ifs", "in", "input", "loop", "native", "new", "Null", "output",
    "pause", "resume", "return", "set", "spawn", "task", "throw", "type",
    "var", "defer", "every", "par", "parand", "paror", "pauseon", "until",
    "watching", "where", "with",
)

sealed class Tk (val str: String, val lin: Int, val col: Int) {
    data class Err (val str_: String, val lin_: Int, val col_: Int): Tk(str_, lin_, col_)
    data class Eof (val str_: String, val lin_: Int, val col_: Int): Tk(str_, lin_, col_)
    data class Fix (val str_: String, val lin_: Int, val col_: Int): Tk(str_, lin_, col_)
    data class id  (val str_: String, val lin_: Int, val col_: Int): Tk(str_, lin_, col_)
    data class Id  (val str_: String, val lin_: Int, val col_: Int): Tk(str_, lin_, col_)
    data class ID  (val str_: String, val lin_: Int, val col_: Int): Tk(str_, lin_, col_)
    data class Scp (val str_: String, val lin_: Int, val col_: Int): Tk(str_, lin_, col_)
    data class Par (val str_: String, val lin_: Int, val col_: Int): Tk(str_, lin_, col_)
    data class Nat (val str_: String, val lin_: Int, val col_: Int): Tk(str_, lin_, col_)
    data class Num (val str_: String, val lin_: Int, val col_: Int): Tk(str_, lin_, col_)
    data class Clk (val str_: String, val lin_: Int, val col_: Int): Tk(str_, lin_, col_)
}

sealed class Type(val tk: Tk, var wup: Any?, var wenv: Any?) {
    data class Unit    (val tk_: Tk.Fix): Type(tk_, null, null)
    data class Nat     (val tk_: Tk.Nat): Type(tk_, null, null)
    data class Par     (val tk_: Tk.Par, var xtype: Type?): Type(tk_, null, null)
    data class Tuple   (val tk_: Tk.Fix, val vec: List<Type>, val yids: List<Tk.id>?): Type(tk_, null, null)
    data class Union   (val tk_: Tk.Fix, val common: Type.Tuple?, val vec: List<Type>, val yids: List<Tk.Id>?): Type(tk_, null, null)
    data class Pointer (val tk_: Tk.Fix, var xscp: Scope?, val pln: Type): Type(tk_, null, null)
    data class Active  (val tk_: Tk.Fix, val tsk: Type): Type(tk_, null, null)
    data class Actives (val tk_: Tk.Fix, val len: Tk.Num?, val tsk: Type): Type(tk_, null, null)
    data class Func (
        val tk_: Tk.Fix,
        val pars: Triple<Boolean,List<Tk.id>?,List<Type>?>, // ispar, pars, args
        var xscps: Triple<Scope,List<Scope>?,List<Pair<String,String>>?>,   // [closure scope, input scopes, input scopes constraints]
        val inp: Type, val pub: Type?, val out: Type
    ): Type(tk_, null, null)
    data class Named (
        val tk_:    Tk.Id,
        val subs:   List<Tk>,
        var xisrec: Boolean,
        var xargs:  List<Type>?,    // ${_int},     "?" diffs inference from empty (only CE1)
        var xscps:  List<Scope>?,   // @{a},        "?" diffs inference from empty (only CE1)
        var xdef:   Stmt.Typedef?   // typedef after args instantiation
    ): Type(tk_, null, null)
}

sealed class Attr(val tk: Tk) {
    data class Var   (val tk_: Tk.id): Attr(tk_)
    data class Nat   (val tk_: Tk.Nat, val type: Type): Attr(tk_)
    data class Unpak (val tk_: Tk.Fix, val isinf: Boolean, val e: Attr): Attr(tk_)
    data class Dnref (val tk_: Tk, val ptr: Attr): Attr(tk_)
    data class TDisc (val tk_: Tk, val tup: Attr): Attr(tk_)
    data class UDisc (val tk_: Tk, val uni: Attr): Attr(tk_)
    data class Field (val tk_: Tk.id, val tsk: Attr): Attr(tk_)
}

// Expr.Named.xtype is Type.Named [except for Type.Active(Type.Named)]
// Expr.Named.isact:
// spawn f ()                   results in Type.Active(Type.Func)
// spawn (active Xask) (f ())   results in Type.Active(Type.Named(Type.Func))
// "active" is required here, otherwise result would only be Type.Named(Type.Func)
// to better understand, read as
// (active Xask) spawn (f ())

sealed class Expr (val n: Int, val tk: Tk, var wup: Any?, var wenv: Any?, var wtype: Type?) {
    data class Unit  (val tk_: Tk.Fix): Expr(N++, tk_, null, null, Type.Unit(tk_))
    data class Var   (val tk_: Tk.id): Expr(N++, tk_, null, null, null)
    data class Nat   (val tk_: Tk.Nat, var xtype: Type?): Expr(N++, tk_, null, null, xtype)
    data class Cast  (val tk_: Tk.Fix, val e: Expr, val type: Type): Expr(N++, tk_, null, null, type)
    data class Named (val tk_: Tk, val e: Expr, var xtype: Pair<Boolean,Type>?): Expr(N++, tk_, null, null, xtype?.second)
    data class UNamed (val tk_: Tk.Fix, val isinf: Boolean, val e: Expr): Expr(N++, tk_, null, null, null)
    data class TCons (val tk_: Tk.Fix, val arg: List<Expr>, val yids: List<Tk.id>?): Expr(N++, tk_, null, null, null)
    data class UCons (val tk_: Tk, var xtype: Type.Union?, val arg: Expr): Expr(N++, tk_, null, null, xtype)
    data class UNull (val tk_: Tk.Fix, var xtype: Type.Pointer?): Expr(N++, tk_, null, null, xtype)
    data class TDisc (val tk_: Tk, val tup: Expr): Expr(N++, tk_, null, null, null)
    data class UDisc (val tk_: Tk, val uni: Expr): Expr(N++, tk_, null, null, null)
    data class UPred (val tk_: Tk, val uni: Expr): Expr(N++, tk_, null, null, null)
    data class New   (val tk_: Tk.Fix, var xscp: Scope?, val arg: Expr): Expr(N++, tk_, null, null, null)
    data class Dnref (val tk_: Tk,     val ptr: Expr): Expr(N++, tk_, null, null, null)
    data class Upref (val tk_: Tk.Fix, val pln: Expr): Expr(N++, tk_, null, null, null)
    data class Func  (val tk_: Tk, var xtype: Type.Func?, val block: Stmt.Block) : Expr(N++, tk_, null, null, xtype)
    data class Field (val tk_: Tk.id, val tsk: Expr): Expr(N++, tk_, null, null, null)
    data class If    (val tk_: Tk.Fix, val tst: Expr, val true_: Expr, val false_: Expr) : Expr(N++, tk_, null, null, null)
    data class Call  (
        val tk_:   Tk,
        val f:     Expr,                        // f
        var xargs: List<Type>?,                 // ${_int}
        val arg:   Expr,                        // [_1,_2]
        var xscps: Pair<List<Scope>?,Scope?>    // @{INP1,INP2} @RET
    ): Expr(N++, tk_, null, null, null)
}

sealed class Stmt (val n: Int, val tk: Tk, var wup: Any?, var wenv: Any?) {
    data class Nop    (val tk_: Tk) : Stmt(N++, tk_, null, null)
    data class Var    (val tk_: Tk.id, var xtype: Type?) : Stmt(N++, tk_, null, null)
    data class Set    (val tk_: Tk.Fix, val dst: Expr, val src: Expr) : Stmt(N++, tk_, null, null)
    data class Native (val tk_: Tk.Nat, val istype: Boolean) : Stmt(N++, tk_, null, null)
    data class SCall  (val tk_: Tk.Fix, val e: Expr.Call): Stmt(N++, tk_, null, null)
    data class SSpawn (val tk_: Tk.Fix, val dst: Expr?, val call: Expr): Stmt(N++, tk_, null, null)
    data class DSpawn (val tk_: Tk.Fix, val dst: Expr, val call: Expr): Stmt(N++, tk_, null, null)
    data class Pause  (val tk_: Tk.Fix, val tsk: Expr, val pause: Boolean): Stmt(N++, tk_, null, null)
    data class Await  (val tk_: Tk.Fix, val e: Expr): Stmt(N++, tk_, null, null)
    data class Emit   (val tk_: Tk.Fix, val tgt: Any, val e: Expr): Stmt(N++, tk_, null, null)
    data class Throw  (val tk_: Tk.Fix, val e: Expr): Stmt(N++, tk_, null, null)
    data class Input  (val tk_: Tk.Fix, var xtype: Type?, val dst: Expr?, val arg: Expr.Named): Stmt(N++, tk_, null, null)
    data class Output (val tk_: Tk.Fix, val arg: Expr.Named): Stmt(N++, tk_, null, null)
    data class Seq    (val tk_: Tk, val s1: Stmt, val s2: Stmt) : Stmt(N++, tk_, null, null)
    data class If     (val tk_: Tk.Fix, val tst: Expr, val true_: Block, val false_: Block) : Stmt(N++, tk_, null, null)
    data class Loop   (val tk_: Tk.Fix, val block: Block) : Stmt(N++, tk_, null, null)
    data class DLoop  (val tk_: Tk.Fix, val i: Expr.Var, val tsks: Expr, val block: Block) : Stmt(N++, tk_, null, null)
    data class Block  (val tk_: Tk.Fix, val catch: Expr?, var scp1: Tk.Scp?, val body: Stmt) : Stmt(N++, tk_, null, null)
    data class Typedef (
        val tk_: Tk.Id,         // List
        val isinc: Boolean,
        val pars: List<Tk.id>,  // {a}
        var args: List<Type>?,  // {_int}   (only when instantiated)
        var xscp1s: Pair<List<Tk.Scp>?,List<Pair<String,String>>?>,
        val type: Type,         // = <...>
        var xtype: Type?       // because of +=
    ) : Stmt(N++, tk_, null, null)
    data class XBreak  (val tk_: Tk.Fix) : Stmt(N++, tk_, null, null)
    data class XReturn (val tk_: Tk.Fix) : Stmt(N++, tk_, null, null)
}
