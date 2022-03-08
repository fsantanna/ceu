import org.junit.jupiter.api.MethodOrderer.Alphanumeric
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import java.io.PushbackReader
import java.io.StringReader

@TestMethodOrder(Alphanumeric::class)
class TXParser {

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
        val s = Parser().stmt()
        assert(s is Stmt.Seq && s.s1 is Stmt.Var && s.s2 is Stmt.Set)
        assert(s.tostr() == "var x: ()\nset x = ()\n") { s.tostr() }
    }
    @Test
    fun c02_parser_var () {
        All_restart(null, PushbackReader(StringReader("var x: ()"), 2))
        Lexer.lex()
        val s = Parser().stmt()
        assert(s is Stmt.Var)
        assert(s.tostr() == "var x: ()\n") { s.tostr() }
    }
    @Test
    fun c03_parser_var () {
        All_restart(null, PushbackReader(StringReader("var x = ()"), 2))
        Lexer.lex()
        val s = Parser().stmt()
        assert(s is Stmt.Seq && s.s1 is Stmt.Var && s.s2 is Stmt.Set)
    }
    @Test
    fun c04_parser_var () {
        All_restart(null, PushbackReader(StringReader("var x"), 2))
        Lexer.lex()
        try {
            Parser().stmt()
            error("impossible case")
        } catch (e: Throwable) {
            assert(e.message == "(ln 1, col 6): expected type declaration : have end of file")
        }
    }

    // EXPR / PAK / CONS

    @Test
    fun d01_pak_ucons () {
        All_restart(null, PushbackReader(StringReader("List.1 ()"), 2))
        Lexer.lex()
        val e = Parser().expr()
        assert(e is Expr.Pak && e.xtype is Type.Alias && e.e is Expr.UCons && !e.isact!!)
    }
    @Test
    fun d02_pak () {
        All_restart(null, PushbackReader(StringReader("Unit ()"), 2))
        Lexer.lex()
        val e = Parser().expr()
        //println(e.dump())
        assert(e is Expr.Pak && e.xtype is Type.Alias && e.e is Expr.Unit)
    }
    @Test
    fun d03_typedef () {
        All_restart(null, PushbackReader(StringReader("<Cons=/List,Unit=()>"), 2))
        Lexer.lex()
        val tp = Parser().type()
        assert(tp is Type.Union && tp.vec[1] is Type.Unit && tp.ids!![0].id=="Cons")
    }
    @Test
    fun d04_typedef () {
        All_restart(null, PushbackReader(StringReader("<xxx=/List,Unit=()>"), 2))
        Lexer.lex()
        try {
            Parser().type()
            error("impossible case")
        } catch (e: Throwable) {
            assert(e.message == "(ln 1, col 2): invalid type identifier")
        }
    }
    @Test
    fun d05_typedef () {
        All_restart(null, PushbackReader(StringReader("[xxx:/List,yyy:()]"), 2))
        Lexer.lex()
        val tp = Parser().type()
        assert(tp is Type.Tuple && tp.vec[1] is Type.Unit && tp.ids!![0].id=="xxx")
    }
    @Test
    fun d06_typedef () {
        All_restart(null, PushbackReader(StringReader("[xxx:/List,Yyy:()]"), 2))
        Lexer.lex()
        try {
            val tp = Parser().type()
            println(tp)
            error("impossible case")
        } catch (e: Throwable) {
            assert(e.message == "(ln 1, col 12): invalid variable identifier") { e.message!! }
        }
    }
    @Test
    fun d07_typedef () {
        All_restart(null, PushbackReader(StringReader("[xxx:/List,yyy=()]"), 2))
        Lexer.lex()
        try {
            Parser().type()
            error("impossible case")
        } catch (e: Throwable) {
            assert(e.message == "(ln 1, col 15): expected `:´ : have `=´") { e.message!! }
        }
    }
}