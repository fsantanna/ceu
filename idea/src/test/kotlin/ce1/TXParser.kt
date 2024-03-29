import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer.Alphanumeric
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import java.io.PushbackReader
import java.io.StringReader

@TestMethodOrder(Alphanumeric::class)
class TXParser {

    companion object {
        @BeforeAll
        @JvmStatic
        internal fun setup() {
            CE1 = true
        }
    }
    // TYPE

    /*
    @Test
    fun a01_parser_type () {
        val all = All_new(PushbackReader(StringReader("xxx"), 2))
        lexer(all)
        try {
            parser_type(all)
            error("impossible case")
        } catch (e: Throwable) {
            assert(e.message == "(ln 1, col 1): expected type : have \"xxx\"")
        }
    }

    // EXPR

    @Test
    fun b01_parser_expr_unit () {
        val all = All_new(PushbackReader(StringReader("()"), 2))
        lexer(all)
        val e = parser_expr(all)
        assert(e is Expr.Unit)
    }
     */

    // STMT

    @Test
    fun c01_parser_var () {
        All_restart(null, PushbackReader(StringReader("var x: () = ()"), 2))
        Lexer.lex()
        val s = Parser.stmt()
        assert(s is Stmt.Seq && s.s1 is Stmt.Var && s.s2 is Stmt.Set)
        assert(s.tostr() == "var x: ()\nset x = ()\n") { s.tostr() }
    }
    @Test
    fun c02_parser_var () {
        All_restart(null, PushbackReader(StringReader("var x: ()"), 2))
        Lexer.lex()
        val s = Parser.stmt()
        assert(s is Stmt.Var)
        assert(s.tostr() == "var x: ()\n") { s.tostr() }
    }
    @Test
    fun c03_parser_var () {
        All_restart(null, PushbackReader(StringReader("var x = ()"), 2))
        Lexer.lex()
        val s = Parser.stmt()
        assert(s is Stmt.Seq && s.s1 is Stmt.Var && s.s2 is Stmt.Set)
    }
    @Test
    fun c04_parser_var () {
        All_restart(null, PushbackReader(StringReader("var x"), 2))
        Lexer.lex()
        try {
            Parser.stmt()
            error("impossible case")
        } catch (e: Throwable) {
            assert(e.message == "(ln 1, col 6): expected type declaration : have end of file")
        }
    }
    @Test
    fun c05_parser_stmt_output() {
        All_restart(null, PushbackReader(StringReader("output Std ()"), 2))
        Lexer.lex()
        val s = Parser.stmt()
        //println(s.dump())
        //assert(s is Stmt.Call && s.call.f is Expr.Dnref && ((s.call.f as Expr.Dnref).ptr is Expr.Var) && ((s.call.f as Expr.Dnref).ptr as Expr.Var).tk_.str=="output_Std")
        assert(s is Stmt.Output && s.arg.tk.str == "Output")
    }

    // EXPR / PAK / CONS

    @Test
    fun d01_pak_ucons () {
        All_restart(null, PushbackReader(StringReader("List.1 ()"), 2))
        Lexer.lex()
        val e = Parser.expr()
        assert(e is Expr.Named && e.xtype!!.second is Type.Named && e.e is Expr.UCons && !e.xtype!!.first)
    }
    @Test
    fun d02_pak () {
        All_restart(null, PushbackReader(StringReader("Unit ()"), 2))
        Lexer.lex()
        val e = Parser.expr()
        //println(e.dump())
        assert(e is Expr.Named && e.xtype!!.second is Type.Named && e.e is Expr.Unit)
    }
    @Test
    fun d03_typedef () {
        All_restart(null, PushbackReader(StringReader("<Cons=/List,Unit=()>"), 2))
        Lexer.lex()
        val tp = Parser.type()
        assert(tp is Type.Union && tp.vec[1] is Type.Unit && tp.yids!![0].str=="Cons")
    }
    @Test
    fun d04_typedef () {
        All_restart(null, PushbackReader(StringReader("<xxx=/List,Unit=()>"), 2))
        Lexer.lex()
        try {
            Parser.type()
            error("impossible case")
        } catch (e: Throwable) {
            //assert(e.message == "(ln 1, col 2): invalid type identifier") { e.message!! }
            assert(e.message == "(ln 1, col 2): expected type : have \"xxx\"") { e.message!! }
            //assert(e.message == "(ln 1, col 5): expected \">\" : have \"=\"") { e.message!! }
        }
    }
    @Test
    fun d05_typedef () {
        All_restart(null, PushbackReader(StringReader("[xxx:/List,yyy:()]"), 2))
        Lexer.lex()
        val tp = Parser.type()
        assert(tp is Type.Tuple && tp.vec[1] is Type.Unit && tp.yids!![0].str=="xxx")
    }
    @Test
    fun d06_typedef () {
        All_restart(null, PushbackReader(StringReader("[xxx:/List,Yyy:()]"), 2))
        Lexer.lex()
        try {
            val tp = Parser.type()
            //println(tp)
            error("impossible case")
        } catch (e: Throwable) {
            //assert(e.message == "(ln 1, col 12): invalid variable identifier") { e.message!! }
            assert(e.message == "(ln 1, col 12): expected variable identifier : have \"Yyy\"") { e.message!! }
        }
    }
    @Test
    fun d07_typedef () {
        All_restart(null, PushbackReader(StringReader("[xxx:/List,yyy=()]"), 2))
        Lexer.lex()
        try {
            Parser.type()
            error("impossible case")
        } catch (e: Throwable) {
            assert(e.message == "(ln 1, col 15): expected \":\" : have \"=\"") { e.message!! }
        }
    }

    // HIER

    @Test
    fun e01_type_hier () {
        val src = """
            type Button = [_int] + <(),()> -- Up/Down
       """.trimIndent()
        All_restart(null, PushbackReader(StringReader(src), 2))
        Lexer.lex()
        val s = Parser.stmt()
        //println(s.dump())
        assert(s is Stmt.Typedef && s.type.let { it is Type.Union && it.vec.size==2 && (it.vec[0] as Type.Tuple).vec[0] is Type.Nat})
    }

    @Test
    fun e02_type_hier () {
        val src = """
            type Button = [_int] + <
                [_int,()] + <
                    _int
                >,
                ()
            >
       """.trimIndent()
        All_restart(null, PushbackReader(StringReader(src), 2))
        Lexer.lex()
        try {
            Parser.stmt()
            error("impossible case")
        } catch (e: Throwable) {
            assert(e.message == "(ln 3, col 9): expected tuple type") { e.message!! }
        }
    }
    @Test
    fun e03_type_hier () {
        val src = """
            type Button = [_int] + <
                [_int,()] + <
                    [_int]
                >,
                ()
            >
       """.trimIndent()
        All_restart(null, PushbackReader(StringReader(src), 2))
        Lexer.lex()
        val s = Parser.stmt()
        //println(s.dump())
        assert(s is Stmt.Typedef && s.type.let {
            it is Type.Union && it.vec.size==2 && it.vec[1] is Type.Tuple && it.vec[0].let {
                it is Type.Union && it.vec.size==1  && it.vec[0].let {
                    it is Type.Tuple && it.vec.size==4 && it.vec[2] is Type.Unit
                }
            }
        })
    }
    @Test
    fun e04_nocomma () {
        val src = """
            type Button = [_int] + <
                [_int,()] + <
                    [_int]
                >
                ()
            >
       """.trimIndent()
        All_restart(null, PushbackReader(StringReader(src), 2))
        Lexer.lex()
        val s = Parser.stmt()
        //println(s.dump())
        assert(s is Stmt.Typedef && s.type.let {
            it is Type.Union && it.vec.size==2 && it.vec[1] is Type.Tuple && it.vec[0].let {
                it is Type.Union && it.vec.size==1  && it.vec[0].let {
                    it is Type.Tuple && it.vec.size==4 && it.vec[2] is Type.Unit
                }
            }
        })
    }

    // BREAK / RETURN

    @Test
    fun c13_parser_func() {
        All_restart(null, PushbackReader(StringReader("set f = func @{} -> () -> () { return }"), 2))
        Lexer.lex()
        val s = Parser.stmt()
        //println(s.dump())
        assert(
            (s is Stmt.Set) && ((s.dst as Expr.Var).tk.str == "f") && s.src.let {
                        (it is Expr.Func) && (it.xtype!!.inp is Type.Unit) && it.block.body is Stmt.XReturn
                    }
        )
    }

    @Test
    fun c15_parser_func() {
        All_restart(null, PushbackReader(StringReader("set f = func @{} -> () -> () { return }"), 2))
        Lexer.lex()
        val s = Parser.stmt()
        assert(
            (s is Stmt.Set) && ((s.dst as Expr.Var).tk.str == "f") //&& s.src.let { (it is Expr.Func) && (it.ups.size==2) }
        )
    }

    @Test
    fun c15_parser_loop() {
        All_restart(null, PushbackReader(StringReader("loop { break }"), 2))
        Lexer.lex()
        val s = Parser.stmt()
        assert(s is Stmt.Block && s.body is Stmt.Loop && (s.body as Stmt.Loop).block.body is Stmt.XBreak)
    }

}
