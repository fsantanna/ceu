import java.io.File
import java.io.PushbackReader
import java.io.StringReader

fun ce2c (file: String?, ce: String): Pair<Boolean,String> {
    //TYPEX.clear()
    EXPR_WTYPE = true
    //N = 1
    All_restart(file, PushbackReader(StringReader(ce), 2))
    Lexer.lex()
    try {
        val s = Parser().stmts()
        s.setUps(null)
        s.setScp1s()
        s.setEnvs(null)
        check_00_after_envs(s)
        check_01_before_tps(s)
        s.setTypes()
        s.setScp2s()
        check_02_after_tps(s)
        return Pair(true, s.code())
    } catch (e: Throwable) {
        CODE.clear()
        if (THROW) {
            throw e
        }
        return Pair(false, e.message!!)
    }
}

fun exe2run (): Pair<Boolean,String> {
    return exec("./out.exe")
    //return exec("valgrind ./out.exe")
}

fun main (args: Array<String>) {
    var xinp: String? = null
    var xccs = emptyList<String>()
    var i = 0
    while (i < args.size) {
        when {
            (args[i] == "-cc") -> { i++ ; xccs=args[i].split(" ") }
            else               -> xinp = args[i]
        }
        i++
    }

    val inp = File(xinp).readText()
    val (ok1,out1) = ce2c(xinp, inp)
    if (!ok1) {
        println(out1)
        return
    }

    File("out.c").writeText(out1)
    val (ok2,out2) = exec(listOf("gcc", "out.c", "-o", "out.exe") + xccs)
    if (!ok2) {
        println(out2)
        return
    }
    val (_,out3) = exe2run()
    print(out3)
}

