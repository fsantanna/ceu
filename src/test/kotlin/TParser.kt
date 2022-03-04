import org.junit.jupiter.api.MethodOrderer.Alphanumeric
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import java.io.PushbackReader
import java.io.StringReader

@TestMethodOrder(Alphanumeric::class)
class TParser {

    // TYPE

    @Test
    fun a01_parser_type () {
        All_restart(null, PushbackReader(StringReader("xxx"), 2))
        Lexer.lex()
        try {
            Parser().type()
            error("impossible case")
        } catch (e: Throwable) {
            //assert(e.message == "(ln 1, col 1): expected type : have \"xxx\"") { e.message!! }
            assert(e.message == "(ln 1, col 1): invalid type identifier") { e.message!! }
        }
    }
    @Test
    fun a02_parser_type () {
        All_restart(null, PushbackReader(StringReader("()"), 2))
        Lexer.lex()
        val tp = Parser().type()
        assert(tp is Type.Unit)
    }
    @Test
    fun a03_parser_type () {
        All_restart(null, PushbackReader(StringReader("_char"), 2))
        Lexer.lex()
        val tp = Parser().type()
        assert(tp is Type.Nat && tp.tk_.src=="char")
    }
    @Test
    fun a05_parser_type () {
        All_restart(null, PushbackReader(StringReader("[(),_char]"), 2))
        Lexer.lex()
        val tp = Parser().type()
        assert(tp is Type.Tuple && tp.vec.size==2 && tp.vec[0] is Type.Unit && tp.vec[1] is Type.Nat && (tp.vec[1].tk as Tk.Nat).src=="char")
    }
    @Test
    fun a05_parser_type_tuple_err () {
        All_restart(null, PushbackReader(StringReader("[(),(),"), 2))
        Lexer.lex()
        try {
            Parser().type()
            error("impossible case")
        } catch (e: Throwable) {
            assert(e.message == "(ln 1, col 8): expected type : have end of file")
        }
    }
    @Test
    fun a06_parser_type_func () {
        All_restart(null, PushbackReader(StringReader("func @[] -> () -> ()"), 2))
        Lexer.lex()
        val tp = Parser().type()
        assert(tp is Type.Func && tp.inp is Type.Unit && tp.out is Type.Unit)
    }
    @Test
    fun a07_parser_type_err () {
        All_restart(null, PushbackReader(StringReader("("), 2))
        Lexer.lex()
        try {
            Parser().type()
            error("impossible case")
        } catch (e: Throwable) {
            assert(e.message == "(ln 1, col 2): expected type : have end of file")
        }
    }
    @Test
    fun a08_parser_type_err () {
        All_restart(null, PushbackReader(StringReader("[()"), 2))
        Lexer.lex()
        try {
            Parser().type()
            error("impossible case")
        } catch (e: Throwable) {
            assert(e.message == "(ln 1, col 4): expected `]´ : have end of file")
        }
    }
    @Test
    fun a08_parser_type_tuple () {
        All_restart(null, PushbackReader(StringReader("[(),()]"), 2))
        Lexer.lex()
        val tp = Parser().type()
        assert(tp is Type.Tuple && tp.vec.size==2 && tp.vec[0] is Type.Unit && tp.vec[1] is Type.Unit)
    }
    @Test
    fun a09_parser_type_ptr () {
        All_restart(null, PushbackReader(StringReader("/()@a"), 2))
        Lexer.lex()
        val tp = Parser().type()
        assert(tp is Type.Pointer && tp.pln is Type.Unit)
    }
    @Test
    fun a09_parser_type_ptr_err () {
        All_restart(null, PushbackReader(StringReader("/()"), 2))
        Lexer.lex()
        val t = Parser().type()
        //assert(t is Type.Pointer && t.xscp!!.scp1.id=="LOCAL")
        assert(t is Type.Pointer && t.xscp==null)
        //assert(e.message == "(ln 1, col 4): expected `@´ : have end of file") { e.message!! }
    }
    @Test
    fun a09_parser_type_ptr_ok () {
        All_restart(null, PushbackReader(StringReader("/()@x"), 2))
        Lexer.lex()
        val tp = Parser().type()
        assert(tp is Type.Pointer && tp.xscp!!.scp1.id=="x")
    }
    @Test
    fun a09_parser_type_ptr_err2 () {
        All_restart(null, PushbackReader(StringReader("/()@LOCAL"), 2))
        Lexer.lex()
        val tp = Parser().type()
        assert(tp is Type.Pointer && tp.xscp!!.scp1.id=="LOCAL")
        /*
        try {
            val s = Parser().type()
            println(s)
            error("impossible case")
        } catch (e: Throwable) {
            assert(e.message == "(ln 1, col 3): expected type : have `?´") { e.message!! }
        }
         */
    }
    @Test
    fun a10_parser_type_ptr0 () {
        All_restart(null, PushbackReader(StringReader("/<?[^]>"), 2))
        Lexer.lex()
        //val tp = Parser().type()
        try {
            Parser().type()
            error("impossible case")
        } catch (e: Throwable) {
            //assert(e.message == "(ln 1, col 2): invalid type declaration : unexpected `?´") { e.message!! }
            assert(e.message == "(ln 1, col 3): expected type : have `?´") { e.message!! }
        }
    }
    @Test
    fun a10_parser_type_ptr1 () {
        All_restart(null, PushbackReader(StringReader("/List@GLOBAL"), 2))
        Lexer.lex()
        val tp = Parser().type()
        assert(tp is Type.Pointer)      // error on check
    }
    @Test
    fun a10_parser_type_ptr2 () {
        All_restart(null, PushbackReader(StringReader("/<[/List@GLOBAL]>@LOCAL"), 2))
        Lexer.lex()
        val tp = Parser().type()
        assert(tp is Type.Pointer)
    }
    @Test
    fun a12_parser_type_ptr_null () {
        All_restart(null, PushbackReader(StringReader("<? /()>"), 2))
        Lexer.lex()
        //val tp = Parser().type()
        //assert(tp is Type.Union && tp.isnull)
        try {
            Parser().type()
            error("impossible case")
        } catch (e: Throwable) {
            //assert(e.message == "(ln 1, col 1): invalid type declaration : unexpected `?´") { e.message!! }
            assert(e.message == "(ln 1, col 2): expected type : have `?´") { e.message!! }
        }
    }
    @Test
    fun a13_parser_type_ptr_null () {
        All_restart(null, PushbackReader(StringReader("<? /(), ()>"), 2))
        Lexer.lex()
        try {
            Parser().type()
            error("impossible case")
        } catch (e: Throwable) {
            //assert(e.message == "(ln 1, col 1): invalid type declaration : unexpected `?´") { e.message!! }
            assert(e.message == "(ln 1, col 2): expected type : have `?´") { e.message!! }
        }
    }
    @Test
    fun a13_parser_type_pointer_scope () {
        All_restart(null, PushbackReader(StringReader("//() @a @b"), 2))
        Lexer.lex()
        val tp = Parser().type()
        assert(tp is Type.Pointer && tp.xscp!!.scp1.id=="b" && tp.pln is Type.Pointer && (tp.pln as Type.Pointer).xscp!!.scp1.id=="a")
    }

    // EXPR

    @Test
    fun b01_parser_expr_unit () {
        All_restart(null, PushbackReader(StringReader("()"), 2))
        Lexer.lex()
        val e = Parser().expr()
        assert(e is Expr.Unit)
    }
    @Test
    fun b02_parser_expr_var () {
        All_restart(null, PushbackReader(StringReader("x"), 2))
        Lexer.lex()
        val e = Parser().expr()
        assert(e is Expr.Var && e.tk_.id=="x")
    }
    @Test
    fun b08_parser_expr_nat () {
        All_restart(null, PushbackReader(StringReader("_x:_int"), 2))
        Lexer.lex()
        val e = Parser().expr()
        assert(e is Expr.Nat && e.tk_.src=="x" && e.wtype is Type.Nat)
    }
    @Test
    fun b09_parser_expr_nat () {
        All_restart(null, PushbackReader(StringReader("_x:_int"), 2))
        Lexer.lex()
        val e = Parser().expr()
        assert(e is Expr.Nat && e.tk_.src=="x" && e.wtype is Type.Nat)
    }
    @Test
    fun b10_parser_var () {
        All_restart(null, PushbackReader(StringReader("Point"), 2))
        Lexer.lex()
        try {
            Parser().expr()
            error("impossible case")
        } catch (e: Throwable) {
            //assert(e.message == "(ln 1, col 1): expected expression : have \"Point\"") { e.message!! }
            assert(e.message == "(ln 1, col 6): expected `{´ : have end of file") { e.message!! }
        }
    }

    // PARENS, TUPLE

    @Test
    fun b03_parser_expr_parens () {
        All_restart(null, PushbackReader(StringReader("( () )"), 2))
        Lexer.lex()
        val e = Parser().expr()
        assert(e is Expr.Unit)
    }
    @Test
    fun b04_parser_expr_parens () {
        All_restart(null, PushbackReader(StringReader("("), 2))
        Lexer.lex()
        try {
            Parser().expr()
            error("impossible case")
        } catch (e: Throwable) {
            assert(e.message == "(ln 1, col 2): expected expression : have end of file")
        }
    }
    @Test
    fun b05_parser_expr_parens () {
        All_restart(null, PushbackReader(StringReader("(x"), 2))
        Lexer.lex()
        try {
            Parser().expr()
            error("impossible case")
        } catch (e: Throwable) {
            assert(e.message == "(ln 1, col 3): expected `)´ : have end of file")
        }
    }
    @Test
    fun b06_parser_expr_tuple () {
        All_restart(null, PushbackReader(StringReader("[(),x,()]"), 2))
        Lexer.lex()
        val e = (Parser().expr() as Expr.As).e
        assert(e is Expr.TCons && e.arg.size==3 && e.arg[0] is Expr.Unit && e.arg[1] is Expr.Var && (e.arg[1].tk as Tk.Id).id=="x")
    }
    @Test
    fun b07_parser_expr_tuple_err () {
        All_restart(null, PushbackReader(StringReader("[(),x,"), 2))
        Lexer.lex()
        try {
            Parser().expr()
            error("impossible case")
        } catch (e: Throwable) {
            assert(e.message == "(ln 1, col 7): expected expression : have end of file")
        }
    }
    @Test
    fun b08_parser_expr_disc_err () {
        All_restart(null, PushbackReader(StringReader("l!0"), 2))
        Lexer.lex()
        try {
            Parser().expr()
            error("impossible case")
        } catch (e: Throwable) {
            assert(e.message == "(ln 1, col 3): invalid discriminator : union cannot be <.0>") { e.message!! }
        }
    }
    @Test
    fun b08_parser_expr_pred_err () {
        All_restart(null, PushbackReader(StringReader("l?0"), 2))
        Lexer.lex()
        try {
            Parser().expr()
            error("impossible case")
        } catch (e: Throwable) {
            assert(e.message == "(ln 1, col 3): invalid discriminator : union cannot be <.0>") { e.message!! }
        }
    }

    // CALL

    @Test
    fun b09_parser_expr_call () {
        All_restart(null, PushbackReader(StringReader("xxx ()"), 2))
        Lexer.lex()
        val e = Parser().expr()
        assert(e is Expr.Call && e.f.noas() is Expr.Var && (e.f.noas().tk as Tk.Id).id=="xxx" && e.arg is Expr.Unit)
    }
    @Test
    fun b10_parser_expr_call () {
        All_restart(null, PushbackReader(StringReader("xxx ()"), 2))
        Lexer.lex()
        val e = Parser().expr()
        assert(e is Expr.Call && e.f.noas() is Expr.Var && (e.f.noas().tk as Tk.Id).id=="xxx" && e.arg is Expr.Unit)
    }
    @Test
    fun b10_parser_expr_call_err () {
        All_restart(null, PushbackReader(StringReader("() ()"), 2))
        Lexer.lex()
        val e = Parser().expr()
        assert(e is Expr.Call && e.f.noas() is Expr.Unit && e.arg is Expr.Unit)
    }
    @Test
    fun b11_parser_expr_call () {
        All_restart(null, PushbackReader(StringReader("f ()\n()\n()"), 2))
        Lexer.lex()
        val e = Parser().expr()
        assert(e is Expr.Call && e.f.noas() is Expr.Var && e.arg is Expr.Call && (e.arg as Expr.Call).f.noas() is Expr.Unit)
    }
    @Test
    fun b12_parser_expr_call () {
        All_restart(null, PushbackReader(StringReader("f1 f2\nf3\n()"), 2)) // f1 (f2 (f3 ()))
        Lexer.lex()
        val e = Parser().expr()
        assert(e is Expr.Call && e.arg is Expr.Call && (e.arg as Expr.Call).arg is Expr.Call && ((e.arg as Expr.Call).arg as Expr.Call).arg is Expr.Unit)
    }
    @Test
    fun b13_parser_expr_call () {
        All_restart(null, PushbackReader(StringReader("xxx ("), 2))
        Lexer.lex()
        try {
            Parser().expr()
            error("impossible case")
        } catch (e: Throwable) {
            assert(e.message == "(ln 1, col 6): expected expression : have end of file")
        }
    }

    // CONS

    @Test
    fun b14_parser_expr_cons_err_1 () {
        All_restart(null, PushbackReader(StringReader("<.1 ("), 2))
        Lexer.lex()
        try {
            Parser().expr()
            error("impossible case")
        } catch (e: Throwable) {
            assert(e.message == "(ln 1, col 6): expected expression : have end of file")
        }
    }
    @Test
    fun b15_parser_expr_cons_err () {
        All_restart(null, PushbackReader(StringReader("<.0>"), 2))
        Lexer.lex()
        val e = Parser().expr()
        assert(e is Expr.UNull && e.xtype==null)
        /*
        try {
            Parser().expr()
            error("impossible case")
        } catch (e: Throwable) {
            assert(e.message == "(ln 1, col 5): expected `:´ : have end of file") { e.message!! }
        }
         */
    }
    @Test
    fun b15_parser_expr_cons_err2 () {
        All_restart(null, PushbackReader(StringReader("<.1 ()>:()"), 2))
        Lexer.lex()
        try {
            Parser().expr()
            //error("impossible case")
        } catch (e: Throwable) {
            assert(e.message == "(ln 1, col 9): invalid type : expected union type") { e.message!! }
        }
    }
    @Test
    fun b16_parser_expr_cons () {
        All_restart(null, PushbackReader(StringReader("<.1 ()>:<()>"), 2))
        Lexer.lex()
        val e = Parser().expr()
        assert(e is Expr.UCons && e.tk_.num==1 && e.arg is Expr.Unit)
    }
    @Test
    fun b17_parser_expr_cons () {
        All_restart(null, PushbackReader(StringReader("<.2 <.1 [(),()]>:<()>>:<()>"), 2))
        Lexer.lex()
        val e = Parser().expr()
        assert(e is Expr.UCons && e.tk_.num==2 && e.arg is Expr.UCons && (e.arg as Expr.UCons).arg is Expr.As)
    }

    // INDEX

    fun Expr.noas (): Expr {
        return (this as Expr.As).e
    }

    @Test
    fun b18_parser_expr_index () {
        All_restart(null, PushbackReader(StringReader("x.1"), 2))
        Lexer.lex()
        val e = Parser().expr()
        assert(e is Expr.TDisc && e.tk_.num==1 && e.tup.noas() is Expr.Var)
    }
    @Test
    fun b19_parser_expr_index () {
        All_restart(null, PushbackReader(StringReader("x () .10"), 2))
        Lexer.lex()  // x [() .10]
        val e = Parser().expr()
        assert(e is Expr.Call && e.arg is Expr.TDisc)
    }

    // UPREF, DNREF

    @Test
    fun b21_parser_expr_upref () {
        All_restart(null, PushbackReader(StringReader("/x.1"), 2))
        Lexer.lex()
        val e = Parser().expr()
        assert(e is Expr.Upref && e.pln is Expr.TDisc)
    }
    @Test
    fun b22_parser_expr_upref () {
        All_restart(null, PushbackReader(StringReader("/()"), 2))
        Lexer.lex()
        try {
            Parser().expr()
            error("impossible case")
        } catch (e: Throwable) {
            assert(e.message == "(ln 1, col 2): unexpected operand to `/´") { e.message!! }
        }
    }
    @Test
    fun b23_parser_expr_dnref () {
        All_restart(null, PushbackReader(StringReader("x\\.1"), 2))
        Lexer.lex()
        val e = Parser().expr()
        assert(e is Expr.TDisc && e.tup.noas() is Expr.Dnref && (e.tup.noas() as Expr.Dnref).ptr is Expr.Var)
    }
    @Test
    fun b24_parser_expr_dnref () {
        All_restart(null, PushbackReader(StringReader("()\\"), 2))
        Lexer.lex()
        try {
            Parser().expr()
            error("impossible case")
        } catch (e: Throwable) {
            assert(e.message == "(ln 1, col 3): unexpected operand to `\\´") { e.message!! }
        }
    }
    @Test
    fun b25_parser_expr_dnref () {
        All_restart(null, PushbackReader(StringReader("x\\\\"), 2))
        Lexer.lex()
        val e = Parser().expr()
        assert(e is Expr.Dnref && e.ptr is Expr.Dnref)
    }

    // PRED, DISC

    @Test
    fun b25_parser_expr_disc () {
        All_restart(null, PushbackReader(StringReader("x!1"), 2))
        Lexer.lex()
        val e = Parser().expr()
        assert(e is Expr.UDisc && e.tk_.num==1 && e.uni is Expr.Var)
    }
    @Test
    fun b26_parser_expr_pred () {
        All_restart(null, PushbackReader(StringReader("x?1"), 2))
        Lexer.lex()
        val e = Parser().expr()
        assert(e is Expr.UPred && e.tk_.num==1 && e.uni is Expr.Var)
    }
    @Test
    fun b27_parser_expr_idx () {
        All_restart(null, PushbackReader(StringReader("x.10"), 2))
        Lexer.lex()
        val e = Parser().expr()
        assert(e is Expr.TDisc && e.tk_.num==10 && e.tup.noas() is Expr.Var)
    }
    @Test
    fun b28_parser_expr_disc () {
        All_restart(null, PushbackReader(StringReader("arg.1\\!1.1"), 2))
        Lexer.lex()
        val e = Parser().expr()
        assert(e is Expr.TDisc && e.tk_.num==1 && e.tup.noas() is Expr.UDisc)
    }

    // ATTR

    @Test
    fun b29 () {
        All_restart(null, PushbackReader(StringReader("y\\!1.2\\!1.1"), 2))
        Lexer.lex()
        val e = Parser().attr()
        //println(e)
        assert(e is Attr.TDisc && e.tk_.num==1)
    }
    @Test
    fun b30 () {
        All_restart(null, PushbackReader(StringReader("((((((y\\)!1).2)\\)!1).1)"), 2))
        Lexer.lex()
        val e = Parser().attr()
        //println(e)
        assert(e is Attr.TDisc && e.tk_.num==1)
    }

    // STMT

    @Test
    fun c01_parser_stmt_var () {
        All_restart(null, PushbackReader(StringReader("var x: ()"), 2))
        Lexer.lex()
        val s = Parser().stmt()
        assert(s is Stmt.Var && s.xtype is Type.Unit)
    }
    @Test
    fun c03_parser_stmt_var_tuple () {
        All_restart(null, PushbackReader(StringReader("var x: [(),()]"), 2))
        Lexer.lex()
        val s = Parser().stmt()
        assert(s is Stmt.Var && s.xtype is Type.Tuple)
    }
    @Test
    fun c04_parser_stmt_var_caret () {
        All_restart(null, PushbackReader(StringReader("var x: () @a"), 2))
        Lexer.lex()
        try {
            Parser().stmts()
            error("impossible case")
        } catch (e: Throwable) {
            assert(e.message == "(ln 1, col 11): expected statement : have `@´") { e.message!! }
        }
    }
    @Test
    fun c05_parser_stmt_var_global () {
        All_restart(null, PushbackReader(StringReader("var x: /()@1"), 2))
        Lexer.lex()
        try {
            Parser().stmts()
            error("impossible case")
        } catch (e: Throwable) {
            //assert(e.message == "(ln 1, col 11): expected `@´ : have \"@\"") { e.message!! }
            assert(e.message == "(ln 1, col 12): expected identifier : have 1") { e.message!! }
        }
    }
    @Test
    fun c06_parser_stmt_block_scope () {
        All_restart(null, PushbackReader(StringReader("{ @A }"), 2))
        Lexer.lex()
        val s = Parser().stmt()
        assert(s is Stmt.Block && s.scp1!!.id=="A")
    }

    // STMT_CALL

    @Test
    fun c03_parser_stmt_call () {
        All_restart(null, PushbackReader(StringReader("call () ()"), 2))
        Lexer.lex()
        val s = Parser().stmt()
        assert(s is Stmt.SCall)
    }
    @Test
    fun c04_parser_stmt_call () {
        All_restart(null, PushbackReader(StringReader("call () ()"), 2))
        Lexer.lex()
        val s = Parser().stmt()
        assert(s is Stmt.SCall)
    }
    @Test
    fun c05_parser_stmt_call () {
        All_restart(null, PushbackReader(StringReader("call f ()"), 2))
        Lexer.lex()
        val s = Parser().stmt()
        assert(s is Stmt.SCall && s.e.f.noas() is Expr.Var && s.e.arg is Expr.Unit)
    }
    @Test
    fun c06_parser_stmt_call () {
        All_restart(null, PushbackReader(StringReader("call f ()"), 2))
        Lexer.lex()
        val s = Parser().stmt()
        assert(s is Stmt.SCall && s.e.f.noas() is Expr.Var && s.e.arg is Expr.Unit)
    }
    @Test
    fun c07_parser_stmt_call () {
        All_restart(null, PushbackReader(StringReader("call _printf:func@[]->()->() ()"), 2))
        Lexer.lex()
        val s = Parser().stmt()
        assert(s is Stmt.SCall && s.e.f.noas() is Expr.Nat && s.e.arg is Expr.Unit)
    }
    @Test
    fun c07_parser_stmt_output () {
        All_restart(null, PushbackReader(StringReader("output std ()"), 2))
        Lexer.lex()
        val s = Parser().stmt()
        //assert(s is Stmt.Call && s.call.f is Expr.Dnref && ((s.call.f as Expr.Dnref).ptr is Expr.Var) && ((s.call.f as Expr.Dnref).ptr as Expr.Var).tk_.str=="output_std")
        assert(s is Stmt.Output && s.lib.id=="std")
    }
    @Test
    fun c08_parser_stmt_input () {
        All_restart(null, PushbackReader(StringReader("set x = input std (): _int"), 2))
        Lexer.lex()
        val s = Parser().stmt()
        //assert(s is Stmt.Call && s.call.f is Expr.Dnref && ((s.call.f as Expr.Dnref).ptr is Expr.Var) && ((s.call.f as Expr.Dnref).ptr as Expr.Var).tk_.str=="output_std")
        assert(s is Stmt.Input && s.lib.id=="std" && s.xtype is Type.Nat && s.dst!! is Expr.Var && s.arg is Expr.Unit)
    }
    @Test
    fun c09_parser_stmt_input () {
        All_restart(null, PushbackReader(StringReader("input std (): _int"), 2))
        Lexer.lex()
        val s = Parser().stmt()
        //assert(s is Stmt.Call && s.call.f is Expr.Dnref && ((s.call.f as Expr.Dnref).ptr is Expr.Var) && ((s.call.f as Expr.Dnref).ptr as Expr.Var).tk_.str=="output_std")
        assert(s is Stmt.Input && s.lib.id=="std" && s.xtype is Type.Nat && s.dst==null && s.arg is Expr.Unit)
    }

    // STMT_SEQ

    @Test
    fun c08_parser_stmt_seq () {
        All_restart(null, PushbackReader(StringReader("call f() ; call _printf:func@[]->()->() () call g()"), 2))
        Lexer.lex()
        val s = Parser().stmts()
        assert (
            s is Stmt.Seq && s.s1 is Stmt.Seq && s.s2 is Stmt.SCall && ((s.s2 as Stmt.SCall).e.f.noas().tk as Tk.Id).id=="g" &&
            (s.s1 as Stmt.Seq).let {
                it.s1 is Stmt.SCall && (it.s1 as Stmt.SCall).let {
                    it.e.f.noas() is Expr.Var && (it.e.f.noas().tk as Tk.Id).id=="f"
                }
            }
        )
    }

    // STMT_BLOCK

    @Test
    fun c09_parser_stmt_block () {
        All_restart(null, PushbackReader(StringReader("{ call f() }"), 2))
        Lexer.lex()
        val s = Parser().stmt()
        assert(s is Stmt.Block && s.body is Stmt.SCall)
    }

    // STMT_IF

    @Test
    fun c10_parser_stmt_if () {
        All_restart(null, PushbackReader(StringReader("if () {} else { call f() }"), 2))
        Lexer.lex()
        val s = Parser().stmt()
        assert (
            s is Stmt.If && s.tst is Expr.Unit &&
            s.true_.body is Stmt.Nop && s.false_.body is Stmt.SCall
        )
    }
    @Test
    fun c11_parser_stmt_if_err () {
        All_restart(null, PushbackReader(StringReader("if <.2()>:<()> {}"), 2))
        Lexer.lex()
        val s = Parser().stmts()
        assert(s is Stmt.If && s.false_.body is Stmt.Nop)
        /*
        try {
            Parser().stmts()
            error("impossible case")
        } catch (e: Throwable) {
            assert(e.message == "(ln 1, col 18): expected `else` : have end of file") { e.message!! }
        }
         */
    }
    @Test
    fun c11_parser_stmt_if () {
        All_restart(null, PushbackReader(StringReader("if <.2()>:<()> {} else {}"), 2))
        Lexer.lex()
        val s = Parser().stmt()
        assert (
            s is Stmt.If && s.tst is Expr.UCons &&
                    s.true_.body is Stmt.Nop && s.false_.body is Stmt.Nop
        )
    }

    // STMT_FUNC, STMT_RET

    @Test
    fun c12_parser_func () {
        All_restart(null, PushbackReader(StringReader("set f = func () { }"), 2))
        Lexer.lex()
        try {
            Parser().stmt()
            error("impossible case")
        } catch (e: Throwable) {
            assert(e.message == "(ln 1, col 17): expected `->´ : have `{´") { e.message!! }
        }
    }
    @Test
    fun c13_parser_func () {
        All_restart(null, PushbackReader(StringReader("set f = func @[] -> () -> () { return }"), 2))
        Lexer.lex()
        val s = Parser().stmt()
        assert (
            (s is Stmt.Set) && ((s.dst as Expr.Var).tk_.id=="f") &&
            s.src.let {
                (it is Expr.Func) && (it.xtype!!.inp is Type.Unit) && it.block.body is Stmt.Return
            }
        )
    }
    @Test
    fun c14_parser_ret () {
        All_restart(null, PushbackReader(StringReader("return"), 2))
        Lexer.lex()
        val s = Parser().stmt()
        assert(s is Stmt.Return)
    }
    @Test
    fun c15_parser_func () {
        All_restart(null, PushbackReader(StringReader("set f = func @[] -> () -> () { return }"), 2))
        Lexer.lex()
        val s = Parser().stmt()
        assert (
            (s is Stmt.Set) && ((s.dst as Expr.Var).tk_.id=="f") //&& s.src.let { (it is Expr.Func) && (it.ups.size==2) }
        )
    }

    // STMT_NAT

    @Test
    fun c14_parser_nat () {
        All_restart(null, PushbackReader(StringReader("native _{xxx}"), 2))
        Lexer.lex()
        val s = Parser().stmt()
        assert(s is Stmt.Native && s.tk_.src=="xxx" && !s.istype)
    }
    @Test
    fun c14_parser_nat_type () {
        All_restart(null, PushbackReader(StringReader("native type _{xxx}"), 2))
        Lexer.lex()
        val s = Parser().stmt()
        assert(s is Stmt.Native && s.tk_.src=="xxx" && s.istype)
    }
    @Test
    fun c15_parser_nat () {
        All_restart(null, PushbackReader(StringReader("_("), 2))
        Lexer.lex()
        try {
            Parser().stmt()
            error("impossible case")
        } catch (e: Throwable) {
            assert(e.message == "(ln 1, col 1): expected statement : have \"unterminated token\"")
        }
    }
    @Test
    fun c16_parser_nat () {
        All_restart(null, PushbackReader(StringReader("native _{${D}xxx}"), 2))
        Lexer.lex()
        val s = Parser().stmt()
        assert(s is Stmt.Native && s.tk_.src=="${D}xxx")
    }

    // STMT_LOOP

    @Test
    fun c15_parser_loop () {
        All_restart(null, PushbackReader(StringReader("loop { break }"), 2))
        Lexer.lex()
        val s = Parser().stmt()
        assert(s is Stmt.Block && s.body is Stmt.Loop && (s.body as Stmt.Loop).block.body is Stmt.Break)
    }

    // STMT_SET / STMT_VAR

    @Test
    fun c16_parser_set () {
        All_restart(null, PushbackReader(StringReader("set s = ()"), 2))
        Lexer.lex()
        val s = Parser().stmt()
        assert(s is Stmt.Set && s.dst is Expr.Var && s.src is Expr.Unit)
    }
    @Test
    fun c17_parser_var () {
        All_restart(null, PushbackReader(StringReader("set c = arg.1\\!1.1"), 2))
        Lexer.lex()
        val s = Parser().stmt()
        assert(s is Stmt.Set && s.src is Expr.TDisc)
    }
    @Test
    fun c18_parser_var () {
        All_restart(null, PushbackReader(StringReader("arg.1\\!1.1"), 2))
        Lexer.lex()
        Parser().expr()
        //assert(s is Stmt.Var && s.src.e is Expr.TDisc)
    }

    // TASKS / PUB / POOL

    @Test
    fun d02_stmt_spawn () {
        All_restart(null, PushbackReader(StringReader("set x = spawn f ()"), 2))
        Lexer.lex()
        val s = Parser().stmt()
        assert(s is Stmt.SSpawn && s.call.f.noas() is Expr.Var && s.dst is Expr.Var)
    }

    // LOOP

    @Test
    fun d03_stmt_loop () {
        All_restart(null, PushbackReader(StringReader("loop tk in () {}"), 2))
        Lexer.lex()
        val s = Parser().stmt()
        assert(s is Stmt.Block && s.body.let { it is Stmt.DLoop && it.i.tk_.id=="tk" && it.tsks is Expr.Unit })
    }
    @Test
    fun d04_loop () {
        All_restart(null, PushbackReader(StringReader("loop () in @LOCAL {}"), 2))
        Lexer.lex()
        try {
            Parser().stmt()
            error("impossible case")
        } catch (e: Throwable) {
            assert(e.message == "(ln 1, col 6): expected variable expression") { e.message!! }
        }
    }
    @Test
    fun d05_loop () {
        All_restart(null, PushbackReader(StringReader("loop e {}"), 2))
        Lexer.lex()
        try {
            Parser().stmt()
            error("impossible case")
        } catch (e: Throwable) {
            assert(e.message == "(ln 1, col 8): expected `in` : have `{´") { e.message!! }
        }
    }
    @Test
    fun d06_loop () {
        All_restart(null, PushbackReader(StringReader("loop e in {}"), 2))
        Lexer.lex()
        try {
            Parser().stmt()
            error("impossible case")
        } catch (e: Throwable) {
            assert(e.message == "(ln 1, col 11): expected expression : have `{´") { e.message!! }
        }
    }

    // PUB

    @Test
    fun d07_error () {
        All_restart(null, PushbackReader(StringReader("x.y"), 2))
        Lexer.lex()
        try {
            Parser().expr()
            error("impossible case")
        } catch (e: Throwable) {
            assert(e.message == "(ln 1, col 3): unexpected \"y\"") { e.message!! }
        }
    }
    @Test
    fun d08_ok () {
        All_restart(null, PushbackReader(StringReader("set x.pub = ()"), 2))
        Lexer.lex()
        val s = Parser().stmt()
        assert(s is Stmt.Set && s.dst is Expr.Field)
    }

    // TASKS TYPE

    @Test
    fun noclo_d10_tassk () {
        All_restart(null, PushbackReader(StringReader("active {} task @[]->()->()->()"), 2))
        Lexer.lex()
        val tp = Parser().type()
        assert(tp is Type.Actives && tp.tsk.tk.enu==TK.TASK && (tp.tsk as Type.Func).xscps.first.scp1.id=="LOCAL")
    }

    // TYPEDEF

    @Test
    fun e01_typedef () {
        All_restart(null, PushbackReader(StringReader("type Unit = ()"), 2))
        Lexer.lex()
        val s = Parser().stmt()
        assert(s is Stmt.Typedef && s.tk_.id=="Unit" && s.type is Type.Unit)
    }
    @Test
    fun e02_typedef () {
        All_restart(null, PushbackReader(StringReader("var x: Unit"), 2))
        Lexer.lex()
        val s = Parser().stmt()
        assert(s is Stmt.Var && s.xtype is Type.Alias && (s.xtype as Type.Alias).tk_.id=="Unit")
    }
}
