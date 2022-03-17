import java.io.File
import java.io.PushbackReader
import java.io.StringReader
import java.lang.AssertionError

var CE1 = true
val THROW = false
var LINES = false

// search in tests output for
//  definitely|Invalid read|uninitialized
//  - definitely lost
//  - Invalid read of size
//  - uninitialised value
val VALGRIND = ""
//val VALGRIND = "valgrind "

val D = "\$"

data class Alls (
    val stack: ArrayDeque<All> = ArrayDeque(),
    var tk0:   Tk = Tk.Err("",1,1),
    var tk1:   Tk = Tk.Err("",1,1)
)

var alls = Alls()

fun all (): All {
    return alls.stack.first()
}

data class All (
    val file:  String?,
    val inp:   PushbackReader,
    val isinc: Boolean,
    var lin:   Int = 1,
    var col:   Int = 1,
    val stack: ArrayDeque<Pair<Int,Int>> = ArrayDeque()
)

fun All_restart (file: String?, inp: PushbackReader) {
    alls = Alls()
    alls.stack.addFirst(All(file, inp, false))
}

fun All_nest (src: String, f: ()->Any): Any {
    //println(src)
    val old = alls.stack.removeFirst()
    val (tk0,tk1) = Pair(alls.tk0,alls.tk1)
    alls.tk0 = Tk.Err("",1,1)
    alls.tk1 = Tk.Err("",1,1)
    alls.stack.addFirst(All(old.file,PushbackReader(StringReader(src),2),false))
    all().lin = old.lin
    all().col = 1
    Lexer.lex()
    val ret = f()
    //println(ret)
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

fun Alls.accept (enu: TK): Boolean {
    val ret = this.check(enu)
    if (ret) {
        Lexer.lex()
    }
    return ret
}

fun Alls.acceptX (str: String): Boolean {
    val ret = this.checkX(str)
    if (ret) {
        Lexer.lex()
    }
    return ret
}

fun Alls.accept_err (enu: TK): Boolean {
    this.check_err(enu)
    this.accept(enu)
    return true
}

fun Alls.acceptX_err (str: String): Boolean {
    this.checkX_err(str)
    this.acceptX(str)
    return true
}

fun Alls.check (enu: TK): Boolean {
    return when (enu) {
        TK.id  -> this.tk1 is Tk.id
        TK.Id  -> this.tk1 is Tk.Id
        TK.ID  -> this.tk1 is Tk.ID
        TK.SCP -> this.tk1 is Tk.Scp
        TK.NAT -> this.tk1 is Tk.Nat
        TK.NUM -> this.tk1 is Tk.Num
        TK.CLK -> this.tk1 is Tk.Clk
        TK.EOF -> this.tk1 is Tk.Eof
        else   -> error("bug found")
    }
}

fun Alls.checkX (str: String): Boolean {
    return (alls.tk1.str == str)
}

fun Alls.check_err (enu: TK): Boolean {
    val ret = this.check(enu)
    if (!ret) {
        this.err_expected(enu.toErr())
    }
    return ret
}

fun Alls.checkX_err (str: String): Boolean {
    val ret = this.checkX(str)
    if (!ret) {
        this.err_expected('"'+str+'"')
    }
    return ret
}

fun Alls.err_expected (str: String): Boolean {
    val file = all().file.let { if (it==null) "" else it+" : " }
    error(file + "(ln ${this.tk1.lin}, col ${this.tk1.col}): expected $str : have ${this.tk1.toPay()}")
}

fun Alls.err_tk0_unexpected (): Boolean {
    val file = all().file.let { if (it==null) "" else it+" : " }
    error(file + "(ln ${this.tk0.lin}, col ${this.tk0.col}): unexpected ${this.tk0.toPay()}")
}

fun Alls.err_tk1_unexpected (): Boolean {
    val file = all().file.let { if (it==null) "" else it+" : " }
    error(file + "(ln ${this.tk1.lin}, col ${this.tk1.col}): unexpected ${this.tk1.toPay()}")
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

fun Alls.checkExpr (): Boolean {
    return this.checkX("(") || this.checkX("()") || this.check(TK.id) || this.check(TK.NAT)
        || this.checkX("[") || this.checkX("<") || this.checkX("new")
        || this.checkX("/") || this.checkX("func") || this.checkX("task")
        || alls.tk1 is Tk.Id || this.checkX("Null")
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

fun test (infer: Boolean, inp: String): String {
    CE1 = infer
    val (ok1,out1) = ce2c(null, inp)
    if (!ok1) {
        return out1
    }
    File("out.c").writeText(out1)
    val (ok2,out2) = exec("gcc -Werror out.c -o out.exe")
    if (!ok2) {
        return out2
    }
    val (_,out3) = exec("$VALGRIND./out.exe")
    //println(out3)
    return out3
}
