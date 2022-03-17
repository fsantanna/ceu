import java.io.File
import java.io.PushbackReader
import java.io.StringReader

object Lexer {
    fun blanks() {
        while (true) {
            val (c1, x1) = all().read()
            when (x1) {
                '\n', ' ' -> {
                }                // ignore line/space
                '-' -> {
                    val (c2, x2) = all().read()
                    if (x2 == '-') {            // ignore comments
                        while (true) {
                            val (c3, x3) = all().read()
                            if (c3 == -1) {     // EOF stops comment
                                break
                            }
                            if (x3 == '\n') {   // LN stops comment
                                all().unread(c3)
                                break
                            }
                        }
                    } else {
                        all().unread(c2)
                        all().unread(c1)
                        return
                    }
                }
                else -> {
                    all().unread(c1)
                    return
                }
            }
        }
    }

    fun lincol (): Boolean {
        var (c1,x1) = all().read()
        if (x1 != '^') {
            all().unread(c1)
            return false
        }
        x1 = all().read().second
        when (x1) {
            '[' -> {
                x1 = all().read().second
                if (x1 == ']') {
                    all().stack.removeFirst()
                } else if (x1.isDigit()) {
                    fun digits (): Int {
                        assert(x1.isDigit())
                        var pay = ""
                        do {
                            pay += x1
                            all().read().let { c1 = it.first; x1 = it.second }
                        } while (x1.isDigit())
                        all().unread(c1)
                        return pay.toInt()
                    }

                    val lin = digits()
                    x1 = all().read().second
                    if (x1 != ',')  TODO()
                    x1 = all().read().second
                    if (!x1.isDigit()) TODO()
                    val col = digits()
                    x1 = all().read().second
                    if (x1 != ']') TODO()
                    all().stack.addFirst(Pair(lin, col))
                }
            }
            '"' -> {
                var file = ""
                val (lin,col) = all().let { Pair(it.lin,it.col) }
                while (true) {
                    x1 = all().read().second
                    if (x1 == '"') {
                        break
                    }
                    file += x1
                }
                assert(x1 == '"')
                val f = File(file)
                All_assert_tk(all().let{Tk.Err(TK.ERR,"",lin,col)}, f.exists()) {
                    "file not found : $file"
                }
                alls.stack.addFirst(All(file, PushbackReader(StringReader(f.readText()), 2), true))
            }
            else -> TODO()
        }
        return true
    }

    fun token () {
        val LIN = all().lin
        val COL = all().col

        fun lin (): Int {
            return if (all().stack.isEmpty()) LIN else all().stack.first().first
        }
        fun col (): Int {
            return if (all().stack.isEmpty()) COL else all().stack.first().second
        }

        var (c1, x1) = all().read()

        when {
            (c1 == -1) -> alls.tk1 = Tk.Fix(TK.EOF, "", lin(), col())
            (x1 in listOf(')','{','}','[',']','<','>',';','=',',','\\','/','.','!','~','+')) -> {
                alls.tk1 = Tk.Fix(TK.FIX, x1.toString(), lin(), col())
            }
            (x1 == '?') -> {
                val (c2, x2) = all().read()
                if (x2 == '!') {
                    alls.tk1 = Tk.Fix(TK.FIX, "?!", lin(), col())
                } else {
                    alls.tk1 = Tk.Fix(TK.FIX, x1.toString(), lin(), col())
                    all().unread(c2)
                }
            }
            (x1 == ':') -> {
                val (c2, x2) = all().read()
                if (x2 == ':') {
                    alls.tk1 = Tk.Fix(TK.FIX, "::", lin(), col())
                } else {
                    alls.tk1 = Tk.Fix(TK.FIX, x1.toString(), lin(), col())
                    all().unread(c2)
                }
            }
            (x1 == '(') -> {
                val (c2, x2) = all().read()
                if (x2 == ')') {
                    alls.tk1 = Tk.Fix(TK.FIX, "()", lin(), col())
                } else {
                    alls.tk1 = Tk.Fix(TK.FIX, x1.toString(), lin(), col())
                    all().unread(c2)
                }
            }
            (x1 == '-') -> {
                val (_, x2) = all().read()
                if (x2 == '>') {
                    alls.tk1 = Tk.Fix(TK.FIX, "->", lin(), col())
                } else {
                    alls.tk1 = Tk.Err(TK.ERR, "" + x1 + x2, lin(), col())
                }
            }
            (x1 == '@') -> {
                all().read().let { c1 = it.first; x1 = it.second }
                when {
                    (x1 == '[') -> {
                        alls.tk1 = Tk.Fix(TK.FIX, "@[", lin(), col())
                    }
                    x1.isLetter() -> {
                        var pay = ""
                        do {
                            pay += x1
                            all().read().let { c1 = it.first; x1 = it.second }
                        } while (x1.isLetterOrDigit() || x1 == '_')
                        all().unread(c1)
                        alls.tk1 = Tk.Scp(TK.XSCP, pay, lin(), col())
                    }
                    else -> {
                        alls.tk1 = Tk.Err(TK.ERR, "@", lin(), col())
                    }
                }
            }
            (x1 == '_') -> {
                var (c2, x2) = all().read()
                var pay = ""

                var open: Char? = null
                var close: Char? = null
                var open_close = 0
                if (x2 == '(' || x2 == '{') {
                    open = x2
                    close = if (x2 == '(') ')' else '}'
                    open_close += 1
                    all().read().let { c2 = it.first; x2 = it.second }
                }

                while ((close != null || x2.isLetterOrDigit() || x2 == '_')) {
                    if (c2 == -1) {
                        alls.tk1 = Tk.Err(TK.ERR, "unterminated token", lin(), col())
                        return
                    }
                    if (x2 == open) {
                        open_close += 1
                    } else if (x2 == close) {
                        open_close -= 1
                        if (open_close == 0) {
                            break
                        }
                    }
                    pay += x2
                    all().read().let { c2 = it.first; x2 = it.second }
                }
                if (close == null) {
                    all().unread(c2)
                }
                alls.tk1 = Tk.Nat(TK.XNAT, pay, lin(), col(), open)
            }
            x1.isDigit() -> {
                fun digits (): Int {
                    assert(x1.isDigit())
                    var pay = ""
                    do {
                        pay += x1
                        all().read().let { c1 = it.first; x1 = it.second }
                    } while (x1.isDigit())
                    all().unread(c1)
                    return pay.toInt()
                }

                var num = digits()
                if (!x1.isLetter()) {
                    alls.tk1 = Tk.Num(TK.XNUM, num.toString(), lin(), col(), num)
                } else {
                    fun letters(): String {
                        assert(x1.isLetter())
                        var pay = ""
                        do {
                            pay += x1
                            all().read().let { c1 = it.first; x1 = it.second }
                        } while (x1.isLetter())
                        all().unread(c1)
                        return pay
                    }

                    var ms = 0
                    while (true) {
                        all().read().let { c1 = it.first; x1 = it.second }
                        if (!x1.isLetter()) {
                            all().unread(c1)
                            alls.tk1 = Tk.Err(TK.ERR, "invalid time constant", lin(), col())
                            break
                        }
                        val unit = letters()
                        ms += when (unit) {
                            "ms" -> num
                            "s" -> num * 1000
                            "min" -> num * 1000 * 60
                            "h" -> num * 1000 * 60 * 60
                            else -> {
                                alls.tk1 = Tk.Err(TK.ERR, "invalid time constant", lin(), col())
                                break
                            }
                        }
                        all().read().let { c1 = it.first; x1 = it.second }
                        if (!x1.isDigit()) {
                            all().unread(c1)
                            alls.tk1 = Tk.Clk(TK.XCLK, ms.toString(), lin(), col(), ms)
                            break
                        }
                        num = digits()
                    }
                }
            }
            x1.isLetter() -> {
                var pay = ""
                do {
                    pay += x1
                    all().read().let { c1 = it.first; x1 = it.second }
                } while (x1.isLetterOrDigit() || x1 == '_')
                all().unread(c1)

                alls.tk1 = keywords.contains(pay).let {
                    when {
                        it -> Tk.Fix(TK.FIX, pay, lin(), col())
                        pay[0].isLowerCase() -> Tk.ide(TK.Xide, pay, lin(), col())
                        pay[0].isUpperCase() && pay.any{it.isLowerCase()} -> Tk.Ide(TK.XIde, pay, lin(), col())
                        pay.none { it.isLowerCase() } -> Tk.IDE(TK.XIDE, pay, lin(), col())
                        else -> Tk.Err(TK.ERR, pay, lin(), col())
                    }
                }
            }
            else -> {
                alls.tk1 = Tk.Err(TK.ERR, x1.toString(), lin(), col())
            }
        }
    }

    fun lex () {
        alls.tk0 = alls.tk1
        blanks(); while (lincol()) { blanks() }
        token()
        while (all().isinc && alls.tk1.enu==TK.EOF) {
            alls.stack.removeFirst()
            blanks(); while (lincol()) { blanks() }
            token()
        }
    }
}
