import java.io.PushbackReader
import java.io.StringReader
import java.lang.AssertionError

val THROW = false
var LINES = false

// search in tests output for
//  definitely|Invalid read|uninitialized
//  - definitely lost
//  - Invalid read of size
//  - uninitialised value
val VALGRIND = ""
//val VALGRIND = "valgrind "

data class Alls (
    val stack: ArrayDeque<All> = ArrayDeque(),
    var tk0:   Tk = Tk.Key(TK.ERR,1,1,""),
    var tk1:   Tk = Tk.Err(TK.ERR,1,1,"")
)

var alls = Alls()

fun all (): All {
    return alls.stack.first()
}

data class All (
    val file:  String?,
    val inp:   PushbackReader,
    var lin:   Int = 1,
    var col:   Int = 1,
    val stack: ArrayDeque<Pair<Int,Int>> = ArrayDeque()
)

fun All_restart (file: String?, inp: PushbackReader) {
    alls = Alls()
    alls.stack.addFirst(All(file, inp))
}

fun All_nest (src: String, f: ()->Any): Any {
    val old = alls.stack.removeFirst()
    val (tk0,tk1) = Pair(alls.tk0,alls.tk1)
    alls.stack.addFirst(All(old.file,PushbackReader(StringReader(src),2)))
    all().lin = old.lin
    all().col = 1
    Lexer.lex()
    val ret = f()
    alls.stack.removeFirst()
    alls.stack.addFirst(old)
    alls.tk0 = tk0
    alls.tk1 = tk1
    return ret
}

fun All.read (): Pair<Int,Char> {
    val i = this.inp.read().let { if (it == 65535) -1 else it }  // TODO: 65535??
    val c = i.toChar()
    when {
        (c == '\n') -> {
            this.lin += 1
            this.col = 1
        }
        (i != -1) -> {
            this.col += 1
        }
    }
    return Pair(i,c)
}

fun All.unread (i: Int) {
    this.inp.unread(i)
    if (i != -1) {
        this.col -= 1
    }
    if (i.toChar() == '\n') {
        this.lin -= 1
        //this.col = ?
    }
}

fun Alls.accept (enu: TK, chr: Char? = null): Boolean {
    val ret = this.check(enu, chr)
    if (ret) {
        Lexer.lex()
    }
    return ret
}

fun Alls.accept_err (enu: TK, chr: Char? = null): Boolean {
    val ret = this.accept(enu,chr)
    if (!ret) {
        this.err_expected(enu.toErr(chr))
    }
    return true
}

fun Alls.check (enu: TK, chr: Char? = null): Boolean {
    return when {
        (this.tk1.enu != enu) -> false
        (chr == null)         -> true
        else -> (this.tk1 as Tk.Chr).chr == chr
    }
}

fun Alls.check_err (enu: TK, chr: Char? = null): Boolean {
    val ret = this.check(enu,chr)
    if (!ret) {
        this.err_expected(enu.toErr(chr))
    }
    return ret
}

fun Alls.err_expected (str: String) {
    fun Tk.toPay (): String {
        return when {
            (this.enu == TK.EOF) -> "end of file"
            (this is Tk.Err)     -> '"' + this.err + '"'
            (this is Tk.Chr)     -> "`" + this.chr + "´"
            (this is Tk.Sym)     -> '`' + this.sym + '´'
            (this is Tk.Id)      -> '"' + this.id + '"'
            (this is Tk.Num)     -> "" + this.num
            (this is Tk.Clk)     -> "time constant"
            (this is Tk.Key)     -> '`' + this.key + '`'
            (this is Tk.Nat)     -> '"' + this.src + '"'
            else -> TODO(this.toString())
        }
    }
    val file = all().file.let { if (it==null) "" else it+" : " }
    error(file + "(ln ${this.tk1.lin}, col ${this.tk1.col}): expected $str : have ${this.tk1.toPay()}")
}

fun All_err_tk (tk: Tk, str: String): String {
    val file = all().file.let { if (it==null) "" else it+" : " }
    error(file + "(ln ${tk.lin}, col ${tk.col}): $str")
}

inline fun All_assert_tk (tk: Tk, value: Boolean, lazyMessage: () -> String = {"Assertion failed"}) {
    if (!value) {
        val m1 = lazyMessage()
        val m2 = All_err_tk(tk, m1)
        throw AssertionError(m2)
    }
}
inline fun All.assert_tk (tk: Tk, value: Boolean, lazyMessage: () -> String = {"Assertion failed"}) {
    if (!value) {
        val m1 = lazyMessage()
        val m2 = All_err_tk(tk, m1)
        throw AssertionError(m2)
    }
}

fun Alls.checkExpr (): Boolean {
    return this.check(TK.CHAR, '(') || this.check(TK.UNIT) || this.check(TK.XID) || this.check(TK.XNAT)
            || this.check(TK.CHAR, '[') || this.check(TK.CHAR, '<') || this.check(TK.NEW)
            || this.check(TK.CHAR, '/') || this.check(TK.FUNC) || this.check(TK.TASK)
}

fun exec (cmds: List<String>): Pair<Boolean,String> {
    //System.err.println(cmds.joinToString(" "))
    val p = ProcessBuilder(cmds)
        //.redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectErrorStream(true)
        .start()
    val ret = p.waitFor()
    val str = p.inputStream.bufferedReader().readText()
    return Pair(ret==0, str)
}

fun exec (cmd: String): Pair<Boolean,String> {
    return exec(cmd.split(' '))
}
