import org.junit.jupiter.api.MethodOrderer.Alphanumeric
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import java.io.PushbackReader
import java.io.StringReader

@TestMethodOrder(Alphanumeric::class)
class TEnv {

    fun inp2env (inp: String): String {
        CE1 = false
        All_restart(null, PushbackReader(StringReader(inp), 2))
        Lexer.lex()
        try {
            val s = Parser.stmts()
            s.setUps(null)
            s.setScp1s()
            s.setEnvs(null)
            check_00_after_envs(s)
            s.xinfScp1s()
            s.setXargs()
            check_01_before_tps(s)
            s.xinfTypes(null)
            //s.setTypes()
            s.setScp2s()
            check_02_after_tps(s)
            return "OK"
        } catch (e: Throwable) {
            if (THROW) {
                throw e
            }
            return e.message!!
        }
    }

    val F = "func $D{}@{}->()->()"

    // UNDECLARED

    @Test
    fun a01_undeclared_var () {
        val out = inp2env("call x $D{} @{} ()")
        assert(out == "(ln 1, col 6): undeclared variable \"x\"") { out }
    }
    @Test
    fun a02_undeclared_func () {
        val out = inp2env("call f $D{} @{} ()")
        assert(out == "(ln 1, col 6): undeclared variable \"f\"") { out }
    }
    @Test
    fun a03_redeclared_var () {
        val out = inp2env("var x:() ; { var x:() }")
        assert(out == "(ln 1, col 18): invalid declaration : \"x\" is already declared (ln 1)") { out }
    }
    @Test
    fun a04_redeclared_func () {
        val out = inp2env("var x:() ; var x:func$D{}@{}->()->()")
        assert(out == "(ln 1, col 16): invalid declaration : \"x\" is already declared (ln 1)") { out }
    }
    @Test
    fun a05_return_err () {
        val out = inp2env("return")
        //assert(out == "(ln 1, col 1): invalid return : no enclosing function") { out }
        assert(out == "(ln 1, col 1): unexpected \"return\"") { out }
        //assert(out == "(ln 1, col 1): undeclared variable \"_ret_\"") { out }
    }
    @Test
    fun a04_err_func () {
        val out = inp2env("var x:() ; var x: func $D{}@{}()->()")
        assert(out == "(ln 1, col 30): expected \"->\" : have \"()\"") { out }
    }

    // CONS

    @Test
    fun b01_user_tuple_out () {
        val out = inp2env("""
            var x: [(),()]
            set x.3 = _
        """.trimIndent())
        assert(out == "(ln 2, col 7): invalid discriminator : out of bounds") { out }
    }
    @Test
    fun b02_user_sub_undeclared () {
        val out = inp2env("""
            var x: <(),()>
            set x.0 = _
        """.trimIndent())
        assert(out == "(ln 2, col 7): invalid discriminator : type mismatch : expected tuple : have <(),()>") { out }
    }
    @Test
    fun b04_user_disc_cons_err () {
        val out = inp2env("""
            call ()!1 $D{} @{} ()
        """.trimIndent())
        assert(out == "(ln 1, col 9): invalid discriminator : not an union") { out }
    }
    @Test
    fun b07_user_out_err1 () {
        val out = inp2env("""
            var x: _int @a
        """.trimIndent())
        assert(out == "(ln 1, col 13): expected statement : have \"@a\"") { out }
    }
    @Test
    fun b07_user_rec_up () {
        val out = inp2env("""
            var x: /_int @a
        """.trimIndent())
        assert(out == "(ln 1, col 14): undeclared scope \"@a\"") { out }
    }
    @Test
    fun todo_b09_user_err () {
        val out = inp2env("""
            type List $D{} @{} = List $D{} @{}
        """.trimIndent())
        assert(out == "ERR: recursive must be pointer") { out }
    }
    @Test
    fun b10_user_err () {
        val out = inp2env("""
            var x: <()>
            var y: </_int @LOCAL>
            set y = x
        """.trimIndent())
        assert(out.startsWith("(ln 3, col 7): invalid assignment : type mismatch")) { out }
    }
    @Test
    fun b10_user_empty_err () {
        val out = inp2env("""
            var l: </_int @GLOBAL>
            set l = <.1 ()>:<()>
        """.trimIndent())
        assert(out.startsWith("(ln 2, col 7): invalid assignment : type mismatch")) { out }
        //assert(out == "(ln 2, col 11): invalid union constructor : expected `new`") { out }
    }
    @Test
    fun b10_user_empty_err2 () {
        val out = inp2env("""
            var l: </_int @GLOBAL>
            set l = <.1 ()>:</_int @GLOBAL>
        """.trimIndent())
        //assert(out == "(ln 2, col 7): invalid assignment : type mismatch") { out }
        assert(out.startsWith("(ln 2, col 11): invalid constructor : type mismatch")) { out }
        //assert(out == "(ln 2, col 11): invalid union constructor : expected `new`") { out }
    }
    @Test
    fun b11_user_empty_err () {
        val out = inp2env("""
            var l: <()>
            set l = <.1 ()>:<()>
            set l!2 = ()
        """.trimIndent())
        assert(out == "(ln 3, col 7): invalid discriminator : out of bounds") { out }
    }
    @Test
    fun b15_pool_err () {
        val out = inp2env("""
            var l: @aaa
        """.trimIndent())
        //assert(out == "(ln 1, col 8): undeclared scope \"@aaa\"") { out }
        assert(out == "(ln 1, col 8): expected type : have \"@aaa\"") { out }
    }
    @Test
    fun b16_pool_err () {
        val out = inp2env("""
            call _f: func$D{} @{}->()->() @aaa
        """.trimIndent())
        //assert(out == "(ln 1, col 9): expected `[´ : have `@aaa´") { out }
        //assert(out == "(ln 1, col 28): expected expression : have `@´") { out }
        assert(out == "(ln 1, col 1): expected call expression") { out }
    }
    @Test
    fun b16_pool_err2 () {
        val out = inp2env("""
            call _f:func$D{} @{a}->()->() $D{} @{aaa} ()
        """.trimIndent())
        assert(out == "(ln 1, col 36): undeclared scope \"@aaa\"") { out }
    }
    @Test
    fun b17_pool_err () {
        val out = inp2env("""
            var f: func $D{} @{} -> () -> ()
            call f $D{} @{} (): @aaa
        """.trimIndent())
        assert(out == "(ln 2, col 20): undeclared scope \"@aaa\"") { out }
    }
    @Test
    fun b18_pool_err () {
        val out = inp2env("""
            var g:   func $D{} @{} -> () -> /_int@a1   -- pointer in func proto must have @x
        """.trimIndent())
        //assert(out == "(ln 1, col 19): invalid pointer : missing pool label") { out }
        assert(out == "(ln 1, col 10): invalid function type : missing scope argument") { out }
    }
    @Test
    fun b19_scp_ok () {
        val out = inp2env("""
            var clone: func $D{} @{a} -> () -> /_int@a
            call clone $D{} @{LOCAL} ()
        """.trimIndent())
        assert(out == "OK") { out }
    }

    // TYPE

    @Test
    fun c01_type_var () {
        val out = inp2env("""
            var x: [()]
            set x = ()
        """.trimIndent())
        assert(out.startsWith("(ln 2, col 7): invalid assignment : type mismatch")) { out }
    }
    @Test
    fun c02_type_set () {
        val out = inp2env("""
            var x: ()
            set x = [()]
        """.trimIndent())
        assert(out.startsWith("(ln 2, col 7): invalid assignment : type mismatch")) { out }
    }
    @Test
    fun c03_type_func_ret () {
        val out = inp2env("""
            var f : func $D{} @{} -> () -> ()
            set f =
                func $D{} @{} -> () -> () {
                    set ret = [()]
                }
        """.trimIndent())
        assert(out.startsWith("(ln 4, col 17): invalid return : type mismatch")) { out }
    }
    @Test
    fun c04_type_func_arg () {
        val out = inp2env("""
            var f : func$D{} @{}->[(),()] -> ()
            set f = func$D{} @{}->[(),()] -> () { }
            call f $D{} @{} ()
        """.trimIndent())
        assert(out.startsWith("(ln 3, col 6): invalid call : type mismatch")) { out }
    }
    @Test
    fun c05_type_idx () {
        val out = inp2env("""
            var x: ()
            set x = [[()],[()]].1
        """.trimIndent())
        assert(out.startsWith("(ln 2, col 7): invalid assignment : type mismatch :")) { out }
        //assert(out == "(ln 1, col 32): invalid discriminator : unexpected constructor") { out }
    }
    @Test
    fun c06_type_idx () {
        val out = inp2env("""
            var x: [(),()]
            set x.1 = [()]
        """.trimIndent())
        assert(out.startsWith("(ln 2, col 9): invalid assignment : type mismatch")) { out }
    }
    @Test
    fun c07_type_upref () {
        val out = inp2env("""
            var x: /() @LOCAL
            set x = ()
        """.trimIndent())
        assert(out.startsWith("(ln 2, col 7): invalid assignment : type mismatch")) { out }
    }
    @Test
    fun c08_type_upref () {
        val out = inp2env("""
            var y: ()
            var x: ()
            set x = /y
        """.trimIndent())
        assert(out.startsWith("(ln 3, col 7): invalid assignment : type mismatch"))
    }
    @Test
    fun c09_type_upref () {
        val out = inp2env("""
            var y: ()
            var x: /() @LOCAL
            set x = /y
        """.trimIndent())
        assert(out == "OK") { out }
    }
    @Test
    fun c10_type_upref () {
        val out = inp2env("""
            var y: [()]
            var x: /()@LOCAL
            set x = /y
        """.trimIndent())
        assert(out.startsWith("(ln 3, col 7): invalid assignment : type mismatch")) { out }
    }
    @Test
    fun c11_type_upref () {
        val out = inp2env("""
            var y: ()
            var x: /() @LOCAL
            set x = /y
            var z: _x
            set z = /x
        """.trimIndent())
        assert(out == "OK")
    }
    @Test
    fun c12_type_dnref () {
        val out = inp2env("""
            var x: ()
            set x\ = ()
        """.trimIndent())
        assert(out == "(ln 2, col 6): invalid operand to `\\´ : not a pointer") { out }
    }
    @Test
    fun c12_type_dnref2 () {
        val out = inp2env("""
            var x: ()
            set x = x\
        """.trimIndent())
        assert(out == "(ln 2, col 10): invalid operand to `\\´ : not a pointer") { out }
    }
    @Test
    fun c13_type_dnref () {
        val out = inp2env("""
            var x: ()
            var y: /()@LOCAL
            set y = /x
            var z: /() @LOCAL
            set z = y\
        """.trimIndent())
        assert(out.startsWith("(ln 5, col 7): invalid assignment : type mismatch")) { out }
    }
    @Test
    fun c14_type_func_err () {
        val out = inp2env("""
            var x: func$D{} @{}->()->[(func$D{} @{}->()->())]
        """.trimIndent())
        //assert(out == "(ln 1, col 12): invalid type : cannot return function type : currently not supported")
        assert(out == "OK")
    }
    @Test
    fun c15_type_func_tup () {
        val out = inp2env("""
            var f: [func$D{} @{}->()->()]
            call f.1 $D{} @{} ()
        """.trimIndent())
        assert(out == "OK") { out }
    }
    @Test
    fun c16_type_func_unit () {
        val out = inp2env("""
            call () $D{} @{} ()
        """.trimIndent())
        assert(out == "(ln 1, col 6): invalid call : not a function") { out }
    }
    @Test
    fun c17_type_func_err () {
        val out = inp2env("""
        var f: func$D{} @{}->()->(); set f = func$D{} @{}->()->() {
            call arg.2 $D{} @{} ()
        }
        """.trimIndent())
        assert(out.startsWith("(ln 2, col 14): invalid discriminator : type mismatch : expected tuple")) { out }
    }
    @Test
    fun c18_type_func_err () {
        val out = inp2env("""
        var f: func$D{} @{}->[(),<(),func$D{} @{}->()->()>]->()
        set f = func$D{} @{}-><(),func$D{} @{}->()->()>->() {
        }
        """.trimIndent())
        assert(out.startsWith("(ln 2, col 7): invalid assignment : type mismatch")) { out }
    }
    @Test
    fun c19_arg_err () {
        val out = inp2env("""
            var c: _int; set c = arg.1\!1.1
        """.trimIndent())
        assert(out == "(ln 1, col 22): undeclared variable \"arg\"") { out }
    }

    // TUPLE / UNION DISCRIMINATOR

    @Test
    fun c14_tup_disc_err () {
        val out = inp2env("""
            var x: [()]
            set x!2 = ()
        """.trimIndent())
        assert(out == "(ln 2, col 7): invalid discriminator : not an union") { out }
    }
    @Test
    fun c15_tup_disc_err () {
        val out = inp2env("""
            var x: [()]
            set x.2 = ()
        """.trimIndent())
        assert(out == "(ln 2, col 7): invalid discriminator : out of bounds") { out }
    }
    @Test
    fun c16_tup_disc_err () {
        val out = inp2env("""
            call [()].2 $D{} @{} ()
        """.trimIndent())
        //assert(out == "(ln 1, col 17): invalid discriminator : unexpected constructor") { out }
        assert(out == "(ln 1, col 11): invalid discriminator : out of bounds") { out }
    }
    @Test
    fun c17_uni_disc_err () {
        val out = inp2env("""
            var x: <()>
            set x.2 = ()
        """.trimIndent())
        assert(out.startsWith("(ln 2, col 7): invalid discriminator : type mismatch : expected tuple"))
    }
    @Test
    fun c18_uni_disc_err () {
        val out = inp2env("""
            var x: <()>
            set x!2 = ()
        """.trimIndent())
        assert(out == "(ln 2, col 7): invalid discriminator : out of bounds")
    }
    @Test
    fun c19_uni_disc_err () {
        val out = inp2env("""
            set _:_int = <.1()>:<()>!2
        """.trimIndent())
        //assert(out == "(ln 1, col 24): invalid discriminator : unexpected constructor") { out }
        assert(out == "(ln 1, col 26): invalid discriminator : out of bounds") { out }
    }
    @Test
    fun c19_uni_pred_err () {
        val out = inp2env("""
            set _:_int = <.1()>:<()>?1
        """.trimIndent())
        //assert(out == "(ln 1, col 24): invalid discriminator : unexpected constructor") { out }
        assert(out == "OK") { out }
    }
    @Test
    fun c20_uni_disc_err () {
        val out = inp2env("""
            set _:_ = <.2()>:<(),()>!2
        """.trimIndent())
        //assert(out == "(ln 1, col 27): invalid discriminator : unexpected constructor") { out }
        assert(out == "OK") { out }
    }
    @Test
    fun c21_uni_disc_err () {
        val out = inp2env("""
            var x: <()>
            set x = <.2()>:<(),()>
        """.trimIndent())
        assert(out.startsWith("(ln 2, col 7): invalid assignment : type mismatch")) { out }
    }
    @Test
    fun c22_tup_cons_err () {
        val out = inp2env("""
            var t: [(),()]
            set t = [(),(),()]
        """.trimIndent())
        //assert(out.startsWith("(ln 2, col 7): invalid assignment : type mismatch")) { out }
        assert(out.startsWith("(ln 2, col 9): invalid constructor : out of bounds")) { out }
    }
    @Test
    fun c23_list_zero_err () {
        val out = inp2env("""
            var x: _int
            set x = <.0 [()]>:<()>
        """.trimIndent())
        //assert(out == "(ln 2, col 7): invalid assignment : type mismatch") { out }
        //assert(out == "(ln 2, col 11): invalid union constructor : type mismatch") { out }
        //assert(out == "(ln 2, col 13): expected `>´ : have `[´") { out }
        assert(out == "(ln 2, col 11): invalid constructor : out of bounds") { out }
    }
    @Test
    fun c23_list_zero_err2 () {
        val out = inp2env("""
            var x: < /_int@LOCAL>
            set x = <.0 [()]>:<()>
        """.trimIndent())
        //assert(out == "(ln 2, col 7): invalid assignment : type mismatch") { out }
        //assert(out == "(ln 2, col 11): invalid union constructor : type mismatch") { out }
        //assert(out == "(ln 2, col 13): expected `>´ : have `[´") { out }
        assert(out == "(ln 2, col 11): invalid constructor : out of bounds") { out }
    }
    @Test
    fun c24_ucons () {
        val out = inp2env("""
            set _:_ = <.2 ()>: <()>
        """.trimIndent())
        assert(out == "(ln 1, col 13): invalid constructor : out of bounds") { out }
    }

    // POINTERS / SCOPE / @GLOBAL

    @Test
    fun e01_ptr_block_err () {
        val out = inp2env("""
            var p1: /() @LOCAL
            var p2: /()@LOCAL
            {
                var v: ()
                set p1 = /v -- ERRO p1=0 < v=1
            }
            {
                var v: ()
                --set p2 = /v
            }
        """.trimIndent())
        assert(out.startsWith("(ln 5, col 12): invalid assignment : type mismatch")) { out }
    }
    @Test
    fun e02_ptr_block_err () {
        val out = inp2env("""
            var x: ()
            var p: /() @LOCAL
            {
                var y: ()
                set p = /x   -- ok
                set p = /y   -- no
            }
        """.trimIndent())
        assert(out.startsWith("(ln 6, col 11): invalid assignment : type mismatch")) { out }
    }
    @Test
    fun e03_ptr_err () {
        val out = inp2env("""
            var pout: /_int@LOCAL
            {
                var pin: /_int @LOCAL
                set pout = pin  -- no
            }
        """.trimIndent())
        assert(out.startsWith("(ln 4, col 14): invalid assignment : type mismatch")) { out }
    }
    @Test
    fun e03_ptr_ok () {
        val out = inp2env("""
            var pout: /_int@LOCAL
            {
                var pin: /_int @GLOBAL
                set pout = pin
            }
        """.trimIndent())
        assert(out == "OK") { out }
    }
    @Test
    fun e04_ptr_ok () {
        val out = inp2env("""
            var pout: /_int @LOCAL
            {
                var pin: /_int@LOCAL
                set pin = pout
            }
        """.trimIndent())
        assert(out == "OK")
    }
    @Test
    fun e05_block_err1 () {
        val out = inp2env("""
            { @A
                var a: ()
            }
        """.trimIndent())
        //assert(out == "(ln 2, col 9): invalid declaration : \"a\" is already declared (ln 1)") { out }
        assert(out == "OK") { out }
    }
    @Test
    fun e05_block_err2 () {
        val out = inp2env("""
            var a: ()
            { @A }
        """.trimIndent())
        //assert(out == "(ln 2, col 3): invalid scope : \"@A\" is already declared (ln 1)") { out }
        assert(out == "OK") { out }
    }
    @Test
    fun e05_block_err3 () {
        val out = inp2env("""
            var f: func $D{} @{a} -> /()@a -> ()
            set f = func $D{} @{a} -> /()@a -> () {
                var a: ()
            }
        """.trimIndent())
        //assert(out == "(ln 2, col 9): invalid declaration : \"a\" is already declared (ln 1)") { out }
        assert(out == "OK") { out }
    }
    @Test
    fun e05_ptr_ok () {
        val out = inp2env("""
            { @A
                var pout: /_int @LOCAL
                {
                    var pin: /_int @A
                    set pout = pin  -- ok
                }
            }
        """.trimIndent())
        assert(out == "OK") { out }
    }
    @Test
    fun e06_ptr_scope_err () {
        val out = inp2env("""
            var pin: /_int @A
        """.trimIndent())
        assert(out == "(ln 1, col 16): undeclared scope \"@A\"") { out }
    }
    @Test
    fun noclo_e07_ptr_err () {
        val out = inp2env("""
            { @A
                var pa: /_int @LOCAL
                var f: (func$D{} @{}->()->())
                set f = func$D{} @{}->()->() {
                    var pf: /_int @A
                    set pa = pf
                }
            }
        """.trimIndent())
        //assert(out == "(ln 5, col 24): undeclared scope \"A\"") { out }
        //assert(out == "(ln 6, col 13): invalid access to \"pa\" : invalid closure declaration (ln 4)") { out }
        //assert(out == "(ln 6, col 13): undeclared variable \"pa\"") { out }
        assert(out == "OK") { out }
    }
    @Test
    fun noclo_e07_ptr_ok () {
        val out = inp2env("""
            { @A
                var pa: /_int @LOCAL
                var f: (func$D{} @{}->()->())
                set f = func$D{} @{}->()->() {
                    var pf: /_int @A
                    set pa = pf
                }
            }
        """.trimIndent())
        //assert(out == "(ln 5, col 24): undeclared scope \"A\"") { out }
        //assert(out == "(ln 6, col 13): undeclared variable \"pa\"") { out }
        assert(out == "OK") { out }
    }
    @Test
    fun noclo_e07_ptr_err2 () {
        val out = inp2env("""
            var f: func$D{} @{}->()->()
            { @A
                var pa: ()
                set pa = ()
                set f = func $D{} @{}->()->() {  -- set [] vs [@A]
                    set _:_ = pa
                }
            }
            call f $D{} @{} ()
        """.trimIndent())
        assert(out.startsWith("(ln 5, col 11): invalid assignment : type mismatch :"))
        //assert(out == "(ln 6, col 20): invalid access to \"pa\" : invalid closure declaration (ln 5)") { out }
    }
    @Test
    fun noclo_e07_ptr_err4 () {
        val out = inp2env("""
            var f: func @a->$D{} @{}->()->()
        """.trimIndent())
        //assert(out == "(ln 1, col 14): undeclared scope \"a\"") { out }
        assert(out == "(ln 1, col 13): expected \"\${\" : have \"@a\"") { out }
    }
    @Test
    fun e08_ptr_ok () {
        val out = inp2env("""
            var f: (func $D{} @{k}->/()@k->())
            set f = func $D{} @{k}->/()@k->() {
                var pf: /_int @k
                set pf = arg
            }
            {
                var x: ()
                call f $D{} @{LOCAL} /x
            }
        """.trimIndent())
        assert(out == "OK") { out }
    }
    @Test
    fun e10_func_err () {
        val out = inp2env("""
            var f: func $D{} @{a}->[/()@a]->()
            set f = func $D{} @{b}->[/()@b]->() {}
        """.trimIndent())
        //assert(out == "(ln 2, col 7): invalid assignment : type mismatch") { out }
        assert(out == "OK") { out }
    }
    @Test
    fun e11_func_err () {
        val out = inp2env("""
            var f: func $D{} @{a1}->[/()@a1]->()
            set f = func $D{} @{a2}->[/()@a2]->() {}
        """.trimIndent())
        //assert(out == "(ln 2, col 7): invalid assignment : type mismatch") { out }
        //assert(out == "(ln 2, col 9): invalid function type : pool arguments are not continuous") { out }
        assert(out == "OK") { out }
    }
    @Test
    fun e12_func_ok () {
        val out = inp2env("""
            var f: (func $D{} @{a1,a2}->[/()@a1,/()@a2]->())
            set f = func $D{} @{a1,a2}->[/()@a1,/()@a2]->() {}
        """.trimIndent())
        assert(out == "OK") { out }
    }
    @Test
    fun e12_call_err () {
        val out = inp2env("""
            var f: (func $D{} @{a1,a2: a2>a1}->[/()@a1,/()@a2]->())
            set f = func $D{} @{a1,a2: a2>a1}->[/()@a1,/()@a2]->() {}
            { @A
                var x: ()
                {
                    var y: ()
                    call f $D{} @{LOCAL,A} [/y,/x]  -- err
                }
            }
        """.trimIndent())
        assert(out.startsWith("(ln 7, col 14): invalid call : scope mismatch : constraint mismatch")) { out }

    }
    @Test
    fun e13_tuple_ok () {
        val out = inp2env("""
            { @A
                var x: ()
                { @B
                    var y: ()
                    var ps: [/()@B,/()@B]
                    set ps = [/x,/y]
                }
            }
        """.trimIndent())
        assert(out == "OK") { out }
    }
    @Test
    fun e13_tuple_err () {
        val out = inp2env("""
            { @A
                var x: ()
                { @B
                    var y: ()
                    var ps: [/()@A,/()@B]
                    set ps = [/y,/x]
                }
            }
        """.trimIndent())
        assert(out.startsWith("(ln 6, col 16): invalid assignment : type mismatch")) { out }
    }
    @Test
    fun e13_call_ok () {
        val out = inp2env("""
            var f: (func$D{} @{i1}->/()@i1->/()@i1)
            set f = func$D{} @{i1}->/()@i1->/()@i1 {}
            { @AAA
                var x: /()@LOCAL
                {
                    --var y: /() @LOCAL
                    set x = f $D{} @{X} x: @AAA -- ok
                }
            }
        """.trimIndent())
        assert(out == "OK") { out }
    }
    @Test
    fun e14_call_err () {
        val out = inp2env("""
            var f: (func$D{} @{i1}->/()@i1->/()@i1)
            set f = func$D{} @{i1}->/()@i1->/()@i1 {}
            { @AAA
                var x: /() @LOCAL
                {
                    var y: /()@LOCAL
                    set x = f $D{} @{LOCAL} y: @AAA -- err
                }
            }
        """.trimIndent())
        assert(out.startsWith("(ln 7, col 15): invalid assignment : type mismatch")) { out }
    }
    @Test
    fun e15_call_err () {
        val out = inp2env("""
            var f: (func$D{} @{a1,a2: a2>a1}->/()@a1->/()@a2)
            set f = func$D{} @{a1,a2: a2>a1}->/()@a1->/()@a2 {}
            { @AAA
                var x: /() @LOCAL
                {
                    var y: /()@LOCAL
                    set x = f $D{} @{LOCAL,AAA} y: @AAA  -- err
                }
            }
        """.trimIndent())
        //assert(out == "(ln 7, col 15): invalid assignment : type mismatch") { out }
        //assert(out == "(ln 7, col 22): invalid call : type mismatch") { out }
        //assert(out.startsWith("(ln 7, col 28): invalid call : scope mismatch")) { out }
        assert(out == "(ln 7, col 17): invalid call : scope mismatch : constraint mismatch") { out }
    }
    @Test
    fun e16_call_ok () {
        val out = inp2env("""
            var f:     (func$D{} @{i2,i1}->/()@i2->/()@i1)
            set f = func$D{} @{i2,i1}->/()@i2->/()@i1 {}
            { @AAA
                var x: /() @LOCAL
                {
                    var y: /() @LOCAL
                    set x = f $D{} @{LOCAL,AAA} y: @AAA  -- ok
                }
            }
        """.trimIndent())
        assert(out == "OK") { out }
    }
    @Test
    fun e17_call_ok () {
        val out = inp2env("""
            var f: (func$D{} @{a1,a2: a1>a2}->[/()@a1,/()@a2]->() )
            set f = func$D{} @{a1,a2: a1>a2}->[/()@a1,/()@a2]->() { }
        """.trimIndent())
        assert(out == "OK") { out }
    }
    @Test
    fun e18_call_err () {
        val out = inp2env("""
            var f : func$D{} @{a1,a2: a1>a2}->[/()@a2,/()@a1]->()
            set f = func$D{} @{a1,a2: a1>a2}->[/()@a1,/()@a2]->() { }   -- err
        """.trimIndent())
        assert(out.startsWith("(ln 2, col 7): invalid assignment : type mismatch")) { out }
    }
    @Test
    fun e19_call_ok () {
        val out = inp2env("""
            var f:  (func$D{} @{a2,a1}->/()@a2->/()@a1)
            set f = func$D{} @{a2,a1}->/()@a2->/()@a1 {}
            { @A
                var x: /()@LOCAL
                {
                    var y: /() @LOCAL
                    set y = f $D{} @{LOCAL,A} y: @A  -- ok
                }
            }
        """.trimIndent())
        assert(out == "OK") { out }
    }
    @Test
    fun e19_call_err () {
        val out = inp2env("""
            var f: (func$D{} @{a2,a1}->/()@a2->/()@a1)
            set f = func$D{} @{a2,a1}->/()@a2->/()@a1 {}
            { @B
                var x: /() @LOCAL
                {
                    var y: /()@LOCAL
                    set y = f $D{} @{LOCAL,B} y: @LOCAL -- ok: can return @b into @LOCAL
                }
            }
        """.trimIndent())
        //assert(out == "(ln 7, col 17): invalid call : scope mismatch") { out }
        assert(out == "OK") { out }
    }
    @Test
    fun e19_call_err2 () {
        val out = inp2env("""
            var f: (func$D{} @{a2,a1}->/()@a2->/()@a1)
            set f = func$D{} @{a2,a1}->/()@a2->/()@a1 {}
            {
                var x: /() @LOCAL
                {
                    var y: /() @LOCAL
                    set y = f $D{} @{LOCAL,LOCAL} y  -- no
                }
            }
        """.trimIndent())
        //assert(out == "(ln 7, col 22): invalid call : type mismatch") { out }
        assert(out == "OK") { out }
    }
    @Test
    fun e19_call_ok2 () {
        val out = inp2env("""
            var f: (func$D{} @{a1}->[/()@a1,()]->/()@a1)
            set f = func$D{} @{a1}->[/()@a1,()]->/()@a1 {}
            var y: /()@LOCAL
            set y = f $D{} @{LOCAL} [y,()]: @LOCAL  -- ok
        """.trimIndent())
        assert(out == "OK") { out }
    }
    @Test
    fun e20_arg_ok () {
        val out = inp2env("""
            var f: (func$D{} @{i1}->/()@i1->/()@i1)
            set f = func$D{} @{i1}->/()@i1->/()@i1 {
                set ret = arg
            }
        """.trimIndent())
        assert(out == "OK") { out }
    }
    @Test
    fun e20_arg_ok2 () {
        val out = inp2env("""
            var f: (func$D{} @{x,y: y>x}->/()@x->/()@y)
            set f = func$D{} @{x,y: y>x}->/()@x->/()@y {
                set ret = arg
            }
        """.trimIndent())
        assert(out == "OK") { out }
    }
    @Test
    fun e20_arg_err () {
        val out = inp2env("""
            var f : /(func$D{} @{a1,a2}->/()@a2->/()@a1)@LOCAL
            set f =   func$D{} @{a1,a2}->/()@a2->/()@a1 {
                set ret = arg
            }
        """.trimIndent())
        assert(out.startsWith("(ln 3, col 13): invalid return : type mismatch")) { out }
    }
    @Test
    fun e21_local_err () {
        val out = inp2env("""
            var f: /(func$D{} @{i1}->/()@i1->/()@i1)@LOCAL
            set f = func$D{} @{i1}->/()@i1->/()@i1 {
                {
                    var x: /() @LOCAL
                    set x = arg
                    set ret = x     -- err
                }
            }
        """.trimIndent())
        assert(out.startsWith("(ln 6, col 17): invalid return : type mismatch")) { out }
    }
    @Test
    fun e22_local_ok () {
        val out = inp2env("""
            var f:( func$D{} @{i1}->/()@i1->/()@i1)
            set f = func$D{} @{i1}->/()@i1->/()@i1 {
                {
                    var x: /() @LOCAL
                    set x = arg
                    set ret = arg
                }
            }
        """.trimIndent())
        assert(out == "OK") { out }
    }
    @Test
    fun e23_union_ok () {
        val out = inp2env("""
            {
                var x: /</_int @LOCAL>@LOCAL
                var y: /</_int@LOCAL> @LOCAL
                set x\!1 = y
            }
        """.trimIndent())
        assert(out == "OK") { out }
    }
    @Test
    fun e24_err () {
        val out = inp2env("""
            var x: /</_int @LOCAL> @LOCAL
            set _:_ = [x,x]
        """.trimIndent())
        assert(out == "OK") { out }
    }
    @Test
    fun e25_err () {
        val out = inp2env("""
            var x: @LOCAL
            var y: @GLOBAL
            set x = y
            set y = x
        """.trimIndent())
        //assert(out == "OK") { out }
        assert(out == "(ln 1, col 8): expected type : have \"@LOCAL\"") { out }
    }

    // POINTERS - DOUBLE

    @Test
    fun f01_ptr_ptr_err () {
        val out = inp2env("""
            var p: //_int@LOCAL@LOCAL
            {
                var y: /_int @LOCAL
                set p = /y
            }
        """.trimIndent())
        assert(out.startsWith("(ln 4, col 11): invalid assignment : type mismatch")) { out }
    }
    @Test
    fun f02_ptr_ptr_ok () {
        val out = inp2env("""
            var p: //_int @LOCAL @LOCAL
            var z: _int
            set z = _10: _int
            var y: /_int @LOCAL
            set y = /z
            set p = /y
            set _:_ = p\\
        """.trimIndent())
        assert(out == "OK") { out }
    }
    @Test
    fun f03_ptr_ptr_err () {
        val out = inp2env("""
            var p: //_int @LOCAL @LOCAL
            {
                var z: _int
                set z = _10: _int
                var y: /_int @LOCAL
                set y = /z
                set p = /y      -- ERR here
            }
        """.trimIndent())
        assert(out.startsWith("(ln 7, col 11): invalid assignment : type mismatch :")) { out }
    }
    @Test
    fun f04_ptr_ptr_err () {
        val out = inp2env("""
            var p: /_int @LOCAL
            {
                var x: _int
                set x = _10: _int
                var y: /_int @LOCAL
                set y = /x
                var z: //_int @LOCAL @LOCAL
                set z = /y
                set p = z\      -- ERR here
            }
        """.trimIndent())
        assert(out.startsWith("(ln 9, col 11): invalid assignment : type mismatch :")) { out }
    }

    // POINTERS - FUNC - CALL

    @Test
    fun g01_ptr_func_ok () {
        val out = inp2env("""
            var f : ((func$D{} @{i1}->/_int@i1 -> /_int@i1))
            set f = func$D{} @{i1}->/_int@i1 -> /_int@i1 {
                set ret = arg
            }
            var v: _int
            set v = _10: _int
            var p: /_int @LOCAL
            set p = f $D{} @{LOCAL} /v
            set _:_ = p\
        """.trimIndent())
        assert(out == "OK") { out }
    }
    @Test
    fun g02_ptr_func_err () {
        val out = inp2env("""
            var v: _int
            set v = _10: _int
            var f :  (func$D{} @{i1}->() -> /_int@i1)
            set f = func$D{} @{i1}->() -> /_int@i1 {
                set ret = /v
            }
            var p: /_int @LOCAL
            set p = f $D{} @{LOCAL} (): @LOCAL
            set _:_ = p\
        """.trimIndent())
        //assert(out == "(ln 3, col 13): undeclared variable \"v\"") { out }
        assert(out == "OK") { out }
    }
    @Test
    fun g02_ptr_func_ok () {
        val out = inp2env("""
            var f : (func $D{} @{i1} -> () -> /_int@i1)
            set f = func $D{} @{i1} -> () -> /_int@i1 {
            }
            var p: /_int @LOCAL
            set p = f $D{} @{LOCAL} (): @LOCAL
        """.trimIndent())
        assert(out == "OK") { out }
    }
    @Test
    fun g02_ptr_func_ok2 () {
        val out = inp2env("""
            var v: _int
            set v = _10: _int
            var f : (func$D{} @{i1}-> () -> /_int@i1 )
            set f = func$D{} @{i1}->() -> /_int@i1 {
                set ret = /v
            }
            var p: /_int @LOCAL
            set p = f $D{} @{LOCAL} ()
            set _:_ = p\
        """.trimIndent())
        assert(out == "OK") { out }
    }
    @Test
    fun g03_ptr_func_err () {
        val out = inp2env("""
            var f : func$D{} @{i1}->() -> /_int@i1
            set f = func$D{} @{i1}->() -> /_int@i1 {
                var v: _int; set v = _10: _int
                set ret = /v
            }
        """.trimIndent())
        assert(out.startsWith("(ln 4, col 13): invalid return : type mismatch")) { out }
    }
    @Test
    fun g04_ptr_func_err () {
        val out = inp2env("""
            var f : func$D{} @{i1}->/_int@i1 -> /_int@i1
            set f = func$D{} @{i1}->/_int@i1 -> /_int@i1 {
                var ptr: /_int@LOCAL
                set ptr = arg
                set ret = ptr
            }
        """.trimIndent())
        assert(out.startsWith("(ln 5, col 13): invalid return : type mismatch")) { out }
    }
    @Test
    fun g05_ptr_caret_ok () {
        val out = inp2env("""
            var f : func$D{} @{i1}->/_int@i1 -> /_int@i1
            set f = func$D{} @{i1}->/_int@i1 -> /_int@i1 {
                var ptr: /_int@i1
                set ptr = arg
                set ret = ptr
            }
            var v: _int
            set v = _10: _int
            var p: /_int @LOCAL
            set p = f $D{} @{LOCAL} /v
        """.trimIndent())
        assert(out == "OK") { out }
    }
    @Test
    fun g06_ptr_caret_err () {
        val out = inp2env("""
            var f : func$D{} @{i1}->/_int@i1 -> /_int@i1
            set f = func$D{} @{i1}->/_int@i1 -> /_int@i1 {
                var x: _int
                set x = _10: _int
                var ptr: /_int @i1
                set ptr = /x
                set ret = ptr
            }
        """.trimIndent())
        assert(out.startsWith("(ln 6, col 13): invalid assignment : type mismatch")) { out }
    }

    @Test
    fun g08_ptr_arg_err () {
        val out = inp2env("""
            var f : func $D{} @{i1} -> _int -> /_int@i1
            set f = func $D{} @{i1} -> _int -> /_int@i1
            {
                set ret = /arg
            }
        """.trimIndent())
        assert(out.startsWith("(ln 4, col 13): invalid return : type mismatch")) { out }
    }
    @Test
    fun g09_ptr_arg_err () {
        val out = inp2env("""
            var f: func$D{} @{i1}->() -> /() @i1
            set f = func$D{} @{i1}->() -> /() @i1 {
                var ptr: /() @i1
                set ptr = arg   -- err: type mismatch
                set ret = ptr
            }
        """.trimIndent())
        assert(out.startsWith("(ln 4, col 13): invalid assignment : type mismatch")) { out }
    }
    @Test
    fun g10_ptr_out_err () {
        val out = inp2env("""
            var f: func$D{} @{i1}->/_int@i1 -> //_int@i1@i1
            set f = func$D{} @{i1}->/_int@i1 -> _int {
                var ptr: /_int@GLOBAL
                set ptr = arg
                set ret = /ptr
            }
        """.trimIndent())
        assert(out.startsWith("(ln 4, col 13): invalid assignment : type mismatch")) { out }
    }
    @Test
    fun g13_ptr_func () {
        val out = inp2env("""
            var v: /_int @LOCAL
            var f : func $D{} @{i1} -> /_int@i1 -> ()
            set f = func $D{} @{i1} ->/_int@i1 -> () {
                set v = arg
            }
        """.trimIndent())
        assert(out.startsWith("(ln 4, col 11): invalid assignment : type mismatch")) { out }
    }
    @Test
    fun g14_ptr_func_err () {
        val out = inp2env("""
            var f : func$D{} @{i1}->/()@i1 -> /()@i1
            set f = func$D{} @{i1}->/()@i1 -> /()@i1 {
                set ret = arg
            }
            var p: /() @LOCAL
            {
                var x: /() @LOCAL
                set p = f $D{} @{LOCAL} x: @GLOBAL    -- err: call p/x have diff scopes (@ will be x which is greater)
            }
        """.trimIndent())
        assert(out.startsWith("(ln 8, col 11): invalid assignment : type mismatch")) { out }
        //assert(out == "(ln 8, col 13): invalid call : type mismatch") { out }
        //assert(out == "(ln 8, col 11): invalid assignment : type mismatch") { out }
    }
    @Test
    fun g15_ptr_func_ok () {
        val out = inp2env("""
            var f : (func$D{} @{i1}->/()@i1 -> /()@i1)
            set f = func$D{} @{i1}->/()@i1 -> /()@i1 {
                set ret = arg
            }
            var p: /() @LOCAL
            {
                var x: /() @LOCAL
                set x = f $D{} @{GLOBAL} p: @GLOBAL -- ok: call p/x have diff scopes (@ will be x which is greater)
            }
        """.trimIndent())
        assert(out == "OK") { out }
    }
    @Test
    fun g16_ptr_func_ok () {
        val out = inp2env("""
            var f : func$D{} @{a1,a2: a2>a1}->/()@a1 -> /()@a2
            set f = func$D{} @{a1,a2: a2>a1}->/()@a1 -> /()@a2 {
                set ret = arg
            }
            var p: /() @LOCAL
            {
                var x: /() @LOCAL
                set x = f $D{} @{GLOBAL,LOCAL} p: @LOCAL -- ok: call p/x have diff scopes (@1 will be x which is greater)
            }
        """.trimIndent())
        assert(out == "OK") { out }
    }
    @Test
    fun g17_ptr_func_err () {
        val out = inp2env("""
            var f : func$D{} @{a1,a2: a2>a1}-> /()@a2 -> /()@a1
            set f = func$D{} @{a1,a2: a2>a1}-> /()@a2 -> /()@a1 {
                set ret = arg     -- err
            }
            var p: /() @LOCAL
            {
                var x: /() @LOCAL
                set x = f $D{} @{LOCAL,GLOBAL} p
            }
        """.trimIndent())
        assert(out.startsWith("(ln 3, col 13): invalid return : type mismatch")) { out }
    }
    @Test
    fun g18_ptr_func_err () {
        val out = inp2env("""
            var f : func$D{} @{a1,a2: a2>a1} -> /()@a1 -> /()@a2
            set f = func$D{} @{a1,a2: a2>a1} -> /()@a1 -> /()@a2 {
                set ret = arg
            }
            var p: /() @LOCAL
            {
                var x: /() @LOCAL
                set p = f $D{} @{LOCAL,GLOBAL} x  -- err: @2=p <= @1=x (false) 
            }
        """.trimIndent())
        //assert(out == "(ln 8, col 24): invalid call : scope mismatch") { out }
        //assert(out == "(ln 8, col 13): invalid call : type mismatch") { out }
        //assert(out == "(ln 8, col 11): invalid assignment : type mismatch") { out }
        assert(out == "(ln 8, col 13): invalid call : scope mismatch : constraint mismatch") { out }
    }

    // POINTERS - TUPLE - TYPE

    @Test
    fun h01_ptr_tuple_err () {
        val out = inp2env("""
            var p: /[_int,_int] @LOCAL
            {
                var y: [_int,_int]; set y = [_10: _int,_20: _int]
                set p = /y
            }
        """.trimIndent())
        assert(out.startsWith("(ln 4, col 11): invalid assignment : type mismatch")) { out }
    }
    @Test
    fun h02_ptr_user_err1 () {
        val out = inp2env("""
            var p: /() @LOCAL
            {
                var y: [()]
                set p = /y.1
            }
        """.trimIndent())
        assert(out.startsWith("(ln 4, col 11): invalid assignment : type mismatch")) { out }
    }
    @Test
    fun h02_ptr_user_err2 () {
        val out = inp2env("""
            var p: /() @LOCAL
            {
                var y: [()]
                set p = /y.1
            }
        """.trimIndent())
        assert(out.startsWith("(ln 4, col 11): invalid assignment : type mismatch")) { out }
    }
    @Test
    fun h03_ptr_tup () {
        val out = inp2env("""
            var v: [_int,_int]; set v = [_10: _int,_20: _int]
            var p: /_int @LOCAL
            set p = /v.1
            set p\ = _20: _int
            set _:_ = v
        """.trimIndent())
        assert(out == "OK") { out }
    }
    @Test
    fun h04_ptr_tup_err () {
        val out = inp2env("""
            var p: /_int @LOCAL
            {
                var v: [_int,_int]; set v = [_10: _int,_20: _int]
                set p = /v.1
            }
        """.trimIndent())
        assert(out.startsWith("(ln 4, col 11): invalid assignment : type mismatch")) { out }
    }
    @Test
    fun h05_ptr_type_err () {
        val out = inp2env("""
            var p: /() @LOCAL
            {
                var v: [()]
                set p = /v.1
            }
        """.trimIndent())
        assert(out.startsWith("(ln 4, col 11): invalid assignment : type mismatch")) { out }
    }
    @Test
    fun h06_ptr_tup_err () {
        val out = inp2env("""
            var p: [_int,/_int @LOCAL]
            {
                var v: _int
                set p.2 = /v     -- err
            }
        """.trimIndent())
        assert(out.startsWith("(ln 4, col 13): invalid assignment : type mismatch")) { out }
    }
    @Test
    fun h07_ptr_tup_err () {
        val out = inp2env("""
            var p: [_int,/_int @LOCAL]
            {
                var v: _int
                set p = [_10: _int,/v]
            }
        """.trimIndent())
        assert(out.startsWith("(ln 4, col 11): invalid assignment : type mismatch")) { out }
    }
    @Test
    fun h08_ptr_type_err () {
        val out = inp2env("""
            var p: </_int @LOCAL>
            {
                var v: _int
                set p!1 = /v
            }
        """.trimIndent())
        assert(out.startsWith("(ln 4, col 13): invalid assignment : type mismatch")) { out }
    }
    @Test
    fun h09_ptr_type_err1 () {
        val out = inp2env("""
            var p: </_int @LOCAL>
            {
                var v: _int
                set p = <.1 /v>: </_int @LOCAL>
            }
        """.trimIndent())
        assert(out.startsWith("(ln 4, col 11): invalid assignment : type mismatch")) { out }
    }
    @Test
    fun h09_ptr_type_err2 () {
        val out = inp2env("""
            var p: </_int @LOCAL>
            {
                var v: _int
                set p = <.1 /v>: </_int @GLOBAL>
            }
        """.trimIndent())
        assert(out.startsWith("(ln 4, col 15): invalid constructor : type mismatch")) { out }
    }
    @Test
    fun h09_ptr_type_err3 () {
        val out = inp2env("""
            var p: </_int @LOCAL>
            {
                var v: _int
                { @AAA
                    set p = <.1 /v>: </_int @AAA>
                }
            }
        """.trimIndent())
        assert(out.startsWith("(ln 5, col 15): invalid assignment : type mismatch")) { out }
    }
    @Test
    fun h10_ptr_tup_err () {
        val out = inp2env("""
            var x1: [_int,/_int @LOCAL]
            {
                var v: _int
                var x2: [_int,/_int @LOCAL]
                set x1 = x2
            }
        """.trimIndent())
        assert(out.startsWith("(ln 5, col 12): invalid assignment : type mismatch")) { out }
    }
    @Test
    fun h11_ptr_type_err () {
        val out = inp2env("""
            var x1: </_int @LOCAL>
            {
                var v: _int
                var x2: </_int @LOCAL>
                set x1 = x2
            }
        """.trimIndent())
        assert(out.startsWith("(ln 5, col 12): invalid assignment : type mismatch")) { out }
    }

    // TYPE - REC - REPLACE - CLONE - BORROW

    @Test
    fun i03_list () {
        val out = inp2env("""
            var xxx: <[_int,</_int @LOCAL>]>
            set xxx = <.1()>:<()>
        """.trimIndent())
        assert(out.startsWith("(ln 2, col 9): invalid assignment : type mismatch")) { out }
    }
    // XEXPR

    @Test
    fun j01_rec_xepr_null_err () {
        val out = inp2env("""
            var x: </_int @LOCAL>
            var y: </_int @LOCAL>; set y = x
        """.trimIndent())
        //assert(out == "(ln 2, col 14): invalid expression : expected operation modifier") { out }
        assert(out == "OK") { out }
    }
    @Test
    fun j02_rec_xepr_move_ok () {
        val out = inp2env("""
            var x: </_int @LOCAL>
            var y: </_int @LOCAL>; set y = x
        """.trimIndent())
        assert(out == "OK") { out }
    }
    @Test
    fun j02_rec_xepr_move_err () {
        val out = inp2env("""
            var x: </_int @LOCAL>
            var y: /</_int @LOCAL> @LOCAL
            set y = /x
        """.trimIndent())
        //assert(out == "(ln 2, col 23): expected expression : have `\\´") { out }
        assert(out == "OK") { out }
    }
    @Test
    fun j03_rec_xepr_borrow_err () {
        val out = inp2env("""
            var x: </_int @LOCAL>
            var y: </_int @LOCAL>
            set y = x
        """.trimIndent())
        //assert(out == "(ln 2, col 21): invalid `borrow` : expected pointer to recursive variable") { out }
        assert(out == "OK") { out }
    }
    @Test
    fun j04_rec_xepr_copy_err () {
        val out = inp2env("""
            var x: _int; set x = _10: _int
        """.trimIndent())
        assert(out == "OK") { out }
    }
    @Test
    fun j05_rec_xepr_borrow_ok () {
        val out = inp2env("""
            var x: </_int @LOCAL>
            var y: /</_int @LOCAL> @LOCAL
            set y = /x
        """.trimIndent())
        assert(out == "OK") { out }
    }
    @Test
    fun j06_rec_xepr_borrow_err () {
        val out = inp2env("""
            var x: _int
            var y: /_int @LOCAL
            set y = /x
        """.trimIndent())
        //assert(out == "(ln 2, col 23): invalid `borrow` : expected pointer to recursive variable") { out }
        assert(out == "OK") { out }
    }
    @Test
    fun j15_rec_xepr_borrow_err () {
        val out = inp2env("""
            var x: [()]
            var y: /_int @LOCAL
            set y = /x
        """.trimIndent())
        //assert(out == "(ln 2, col 23): invalid `borrow` : expected pointer to recursive variable") { out }
        assert(out == "OK") { out }
    }
    @Test
    fun j16_rec_xepr_borrow_err () {
        val out = inp2env("""
            var x: <()>
            var y: /_int @LOCAL
            set y = /x
        """.trimIndent())
        //assert(out == "(ln 2, col 16): invalid `borrow` : expected pointer to recursive variable")
        assert(out == "OK") { out }
    }
    @Test
    fun j17_rec_xepr_borrow_ok () {
        val out = inp2env("""
            var x: /<(),/_int @LOCAL> @LOCAL
            var y: /_int @LOCAL
            set y = /x
        """.trimIndent())
        assert(out == "OK") { out }
    }
    @Test
    fun j18_rec_xepr_borrow_ok () {
        val out = inp2env("""
            var x: [/</_int @LOCAL> @LOCAL]
            var y: /_int @LOCAL
            set y = /x
        """.trimIndent())
        assert(out == "OK") { out }
    }
    @Test
    fun j19_rec_xepr_borrow_ok () {
        val out = inp2env("""
            var x: [/</_int @LOCAL> @LOCAL]
            var y: /_int @LOCAL
            set y = /x
        """.trimIndent())
        assert(out == "OK") { out }
    }
    @Test
    fun j20_rec_xepr_borrow_err () {
        val out = inp2env("""
            var x: [/</_int @LOCAL> @LOCAL]
            var f: func$D{} @{i1}-> //</_int@i1>@i1@i1 -> ()
            set f = func$D{} @{i1}->//</_int@i1>@i1@i1 -> ()
            {
                set _:_ = arg
            }
            call f $D{} @{LOCAL} /x.1
        """.trimIndent())
        assert(out == "OK") { out }
    }
    @Test
    fun j21_rec_xepr_borrow_err () {
        val out = inp2env("""
            var x: [/</_int @LOCAL> @LOCAL]
            var y: //</_int @LOCAL> @LOCAL @LOCAL
            set y = /x.1
        """.trimIndent())
        //assert(out == "(ln 2, col 15): invalid expression : expected `borrow` operation modifier") { out }
        assert(out == "OK") { out }
    }
    @Test
    fun j23_rec_xexpr_move_err () {
        val out = inp2env("""
            var x: /</_int @LOCAL> @LOCAL
            var y: //</_int @LOCAL> @LOCAL @LOCAL
            set y = /x --!1
            var z: /</_int @LOCAL> @LOCAL
            set z = y\
        """.trimIndent())
        assert(out == "OK") { out }
        //assert(out == "(ln 3, col 22): invalid `consume` : expected recursive variable")
    }
    @Test
    fun j23_rec_xexpr_move_err2 () {
        val out = inp2env("""
            var x: /</_int @LOCAL> @LOCAL
            var y: //</_int @LOCAL> @LOCAL @LOCAL
            set y = /x --!1
            var z: /</_int @LOCAL> @LOCAL
            set z = y\
        """.trimIndent())
        //assert(out == "(ln 3, col 25): expected expression : have end of file") { out }
        assert(out == "OK") { out }
    }
    @Test
    fun j23_rec_xexpr_move_err3 () {
        val out = inp2env("""
            var x: /</_int @LOCAL> @LOCAL
            var y: //</_int @LOCAL> @LOCAL @LOCAL
            set y = /x --!1
            var z: /</_int @LOCAL> @LOCAL
            set z = y\
        """.trimIndent())
        //assert(out == "(ln 3, col 24): expected `=´ : have end of file") { out }
        assert(out == "OK") { out }
    }
    @Test
    fun j24_rec_xepr_borrow_ok () {
        val out = inp2env("""
            var x: [/</_int @LOCAL> @LOCAL]
            var f: func$D{} @{i1}->//</_int@i1>@i1@i1 -> ()
            set f = func$D{} @{i1}->//</_int@i1>@i1@i1 -> ()
            {
                set _:_ = arg
            }
            call f $D{} @{LOCAL} /x.1
        """.trimIndent())
        assert(out == "OK") { out }
    }
    @Test
    fun j28_nonrec_nullable () {
        val out = inp2env("""
            var l: <()>
        """.trimIndent())
        //assert(out == "(ln 1, col 8): invalid type declaration : unexpected `?´") { out }
        assert(out == "OK") { out }
    }

    // IF / FUNC

    @Test
    fun k01_if () {
        val out = inp2env("""
            if () {} else {}
        """.trimIndent())
        assert(out.startsWith("(ln 1, col 1): invalid condition : type mismatch")) { out }
    }
    @Test
    fun k02_func_arg () {
        val out = inp2env("""
            var f1: (func$D{} @{}->()->())
            set f1 = func$D{} @{}->()->() {
                var f2: (func$D{} @{}->()->())
                set f2 = func$D{} @{}->()->() {
                }
            }
        """.trimIndent())
        assert(out == "OK") { out }
    }

    // BORROW / CONSUME

    @Test
    fun l02_borrow_err () {
        val out = inp2env("""
            var x: /</_int @LOCAL> @LOCAL
            var y: //</_int @LOCAL> @LOCAL @LOCAL
            set y = /x --!1
            var z: /</_int @LOCAL> @LOCAL
            set z = x
        """.trimIndent())
        //assert(out == "(ln 3, col 22): invalid operation on \"x\" : borrowed in line 2") { out }
        assert(out == "OK") { out }
    }
    @Test
    fun l02_borrow_func_err () {
        val out = inp2env("""
            var f: (func$D{} @{}->()->())
            set f = func$D{} @{}->()->() {
                var x: /</_int @LOCAL> @LOCAL
                var y: //</_int @LOCAL> @LOCAL @LOCAL
                --set y = /x --!1
                --var z: <_int>
                --set z = x
            }
        """.trimIndent())
        //assert(out == "(ln 4, col 26): invalid operation on \"x\" : borrowed in line 3") { out }
        assert(out == "OK") { out }
    }
    @Test
    fun l02_borrow_ok () {
        val out = inp2env("""
            var x: /</_int @LOCAL> @LOCAL
            {
                var y: //</_int @LOCAL> @LOCAL @LOCAL
                set y = /x --!1
            }
            var z: /</_int @LOCAL> @LOCAL
            set z = x
        """.trimIndent())
        assert(out == "OK") { out }
    }
    @Test
    fun l15_err () {
        val out = inp2env("""
            var f: / (func$D{} @{i1}->()->())@LOCAL
            call f\ $D{} @{} ()
        """.trimIndent())
        assert(out == "(ln 2, col 7): invalid call : scope mismatch : expecting 1, have 0 argument(s)") { out }
    }

    // UNION SELF POINTER

    @Test
    fun m02_borrow_err () {
        val out = inp2env("""
            var x: <()>
            set x = <.1()>:<()>
            var y: /<()> @LOCAL
            set y = /x --!1
            set x = <.1()>:<()>
        """.trimIndent())
        assert(out == "OK") { out }
        //assert(out == "(ln 3, col 7): invalid assignment of \"x\" : borrowed in line 2") { out }
    }

    @Test
    fun m06_pred_notunion () {
        val out = inp2env("""
            var l: ()
            call _f:$F $D{} @{} l?1
        """.trimIndent())
        assert(out == "(ln 2, col 39): invalid predicate : not an union") { out }
    }
    @Test
    fun m06_disc_notunion () {
        val out = inp2env("""
            var l: ()
            call _f:$F $D{} @{} l!1
        """.trimIndent())
        assert(out == "(ln 2, col 39): invalid discriminator : not an union") { out }
    }

    // UNION SELF POINTER / HOLD

    @Test
    fun n08_hold_ok2 () {
        val out = inp2env("""
            var x: <?/_int>
            set x = <.0>
        """.trimIndent())
        //assert(out == "OK") { out }
        //assert(out == "(ln 2, col 11): unexpected <.0> : not a pointer") { out }
        //assert(out == "(ln 1, col 8): invalid type declaration : unexpected `?´") { out }
        assert(out == "(ln 1, col 9): expected type : have \"?\"") { out }
    }

    // POINTER CROSSING UNION

    @Test
    fun o01_pointer_union_err () {
        val out = inp2env("""
            var x: <[()]>
            var y: /() @LOCAL
            set y = /x!1.1  -- can't point to .1 inside union (union might change)
            --output std y
        """.trimIndent())
        assert(out == "(ln 3, col 9): invalid operand to `/´ : union discriminator") { out }
    }
    @Test
    fun o04_pointer_union_err () {
        val out = inp2env("""
            var x: <(),[()]>
            set x = <.2 [()]>:<(),[()]>
            var y: /() @LOCAL
            set y = /x!2.1  -- crossing udisc
        """.trimIndent())
        assert(out == "(ln 4, col 9): invalid operand to `/´ : union discriminator") { out }
    }
    @Test
    fun o05_pointer_union_ok () {
        val out = inp2env("""
            var x: </() @LOCAL>
            var y: /() @LOCAL
            set y = /x!1\   -- ok: crosses udisc but dnrefs a pointer before the upref
        """.trimIndent())
        assert(out == "OK") { out }
    }
    @Test
    fun o06_pointer_union_err () {
        val out = inp2env("""
            var x: </() @LOCAL>
            var y: //() @LOCAL @LOCAL
            set y = //x!1\   -- no: upref after dnref fires the problem again
        """.trimIndent())
        assert(out == "(ln 3, col 9): invalid operand to `/´ : union discriminator") { out }
    }
    @Test
    fun o07_bug () {
        val out = inp2env("""
            var z: //<()> @LOCAL @LOCAL
            set _:_ = z\!1
        """.trimIndent())
        assert(out == "(ln 2, col 14): invalid discriminator : not an union") { out }
    }

    // FUNC / POOL

    @Test
    fun p01_pool_err () {
        val out = inp2env("""
            var f : func$D{} @{}->/()@i1 -> /()@i1
        """.trimIndent()
        )
        assert(out == "(ln 1, col 9): invalid function type : missing scope argument") { out }
    }
    @Test
    fun p02_pool_ok () {
        val out = inp2env("""
            var f : func$D{} @{a1}->/()@a1 -> /()@a1
        """.trimIndent()
        )
        assert(out == "OK") { out }
    }
    @Test
    fun p03_pool_err () {
        val out = inp2env("""
            var f : func$D{} @{b1}->[/()@a1] -> ()
        """.trimIndent()
        )
        assert(out == "(ln 1, col 9): invalid function type : missing scope argument") { out }
    }
    @Test
    fun p04_pool_err () {
        val out = inp2env("""
            var f :func$D{} @{a}-> /()@a -> ()
        """.trimIndent()
        )
        //assert(out == "(ln 1, col 22): invalid function type : missing scope argument") { out }
        //assert(out == "(ln 1, col 18): invalid pool : expected `_N´ depth") { out }
        assert(out == "OK") { out }
    }
    @Test
    fun p06_pool_ok () {
        val out = inp2env("""
            var f :func $D{} @{a1,a2}-> /()@a1 -> /()@a2
        """.trimIndent()
        )
        assert(out == "OK") { out }
    }
    @Test
    fun p07_pool_err () {
        val out = inp2env("""
            var f : func$D{} @{a1,a3}->/()@a1 -> /()@a3
        """.trimIndent()
        )
        //assert(out == "(ln 1, col 9): invalid function type : pool arguments are not continuous") { out }
        assert(out == "OK") { out }
    }
    @Test
    fun p08_pool_err () {
        val out = inp2env("""
            var f : func$D{} @{a}->/()@a -> /()@a
        """.trimIndent()
        )
        //assert(out == "(ln 1, col 19): invalid pool : expected `_N´ depth") { out }
        assert(out == "OK") { out }
    }
    @Test
    fun p09_pool_err () {
        val out = inp2env("""
            var x : /() @LOCAL
        """.trimIndent()
        )
        //assert(out == "(ln 1, col 10): invalid pool : expected `_N´ depth") { out }
        assert(out == "OK") { out }
    }
    @Test
    fun p10_pool_err () {
        val out = inp2env("""
            { @i1 }
        """.trimIndent()
        )
        //assert(out == "(ln 1, col 10): invalid pool : expected `_N´ depth") { out }
        //assert(out == "(ln 1, col 3): invalid pool : unexpected `_1´ depth") { out }
        //assert(out == "(ln 1, col 3): expected statement : have `@i´") { out }
        assert(out == "(ln 1, col 3): invalid scope constant identifier") { out }
    }
    @Test
    fun p11_pool_err () {
        val out = inp2env("""
            { @A
                var x: @a1
            }
        """.trimIndent()
        )
        //assert(out == "(ln 1, col 8): invalid pool : unexpected `_1´ depth") { out }
        //assert(out == "(ln 2, col 12): undeclared scope \"@a1\"") { out }
        assert(out == "(ln 2, col 12): expected type : have \"@a1\"") { out }
    }
    @Test
    fun p11_pool_err2 () {
        val out = inp2env("""
            { @A
                var x: @A
            }
        """.trimIndent()
        )
        //assert(out == "(ln 1, col 3): invalid pool : unexpected `_1´ depth") { out }
        //assert(out == "(ln 2, col 12): undeclared scope \"@a1\"") { out }
        assert(out == "(ln 2, col 12): expected type : have \"@A\"") { out }
    }
    @Test
    fun p12_pool_ff() {
        val out = inp2env(
            """
            var f: (func$D{} @{i1}->() -> ())
            set f = func$D{} @{i1}->() -> () {}
            var g: (func$D{} @{i1}-> func$D{} @{i1}->()->() -> ())
            set g = func$D{} @{i1}-> func$D{} @{i1}->()->() -> () {}
        """.trimIndent()
        )
        assert(out == "OK") { out }
    }
    @Test
    fun p13_pool_ff() {
        val out = inp2env(
            """
            var f: (func$D{} @{i1}->() -> /()@i1)
            set f = func$D{} @{i1}->() -> /()@i1 {}
            var g: (func$D{} @{i1}-> func$D{} @{i1}->()->/()@i1 -> ())
            set g = func$D{} @{i1}-> func$D{} @{i1}->()->/()@i1 -> () {}
        """.trimIndent()
        )
        assert(out == "OK") { out }
    }
    @Test
    fun p14_pool_ff() {
        val out = inp2env(
            """
            var f : func$D{} @{i1} -> () -> ()
            set f = func$D{} @{i1} -> () -> () {}
            var g : func$D{} @{i1} -> (func$D{} @{i1}->()->()) -> ()
            set g = func$D{} @{i1} -> (func$D{} @{i1}->()->()) -> () {}
            call g $D{} @{LOCAL} f
        """.trimIndent()
        )
        assert(out == "OK") { out }
    }
    @Test
    fun p15_pool_ff() {
        val out = inp2env(
            """
            var f:(func$D{} @{i1}-> () -> /()@i1)
            set f = func$D{} @{i1}-> () -> /()@i1 {}
            var g: (func$D{} @{i1}-> (func$D{} @{i1}->()->/()@i1) -> ())
            set g = func$D{} @{i1}-> (func$D{} @{i1}->()->/()@i1) -> () {}
            call g $D{} @{LOCAL} f
        """.trimIndent()
        )
        assert(out == "OK") { out }
    }
    @Test
    fun p16_strcpy() {
        val out = inp2env(
            """
            var f:      (func$D{} @{a1,b1}->[/()@a1,/()@b1] -> /()@a1)
            set f = func$D{} @{a1,b1}->[/()@a1,/()@b1] -> /()@a1 {}
            var s1: ()
            call f $D{} @{LOCAL,LOCAL} [/s1,/s1]
        """.trimIndent()
        )
        assert(out == "OK") { out }
    }
    @Test
    fun noclo_p17_pool_closure_() {
        val out = inp2env(
            """
            var g: (func$D{} @{a1}->() -> (func$D{} @{a1}->()->()))
            set g = func$D{} @{a1}-> () -> (func$D{} @{a1}->()->()) {
                var f:(func$D{} @{b1}->() -> ())     -- this is @LOCAL, cant return it
                set f = func$D{} @{b1}->() -> () {

                }           
                set ret = f                 -- can't return pointer @LOCAL
            }
            var f: (func$D{} @{a1}->() -> ())
            set f = g $D{} @{LOCAL} ()
            call f $D{} @{LOCAL} ()
        """.trimIndent()
        )
        assert(out.startsWith("(ln 7, col 13): invalid return : type mismatch")) { out }
        //assert(out == "OK") { out }
    }
    @Test
    fun p23_pool_closure_err2() {
        val out = inp2env(
            """
            var f: func $D{} @{_a1}->$D{} @{b1}->() -> ()
        """.trimIndent()
        )
        //assert(out == "(ln 1, col 14): expected `}´ : have \"@_a\"") { out }
        assert(out == "(ln 1, col 19): expected \"}\" : have \"_a1\"") { out }
    }
    @Test
    fun p25_pool_closure_err() {
        val out = inp2env(
            """
            var f:func $D{} @{}->() -> /()@GLOBAL
        """.trimIndent()
        )
        assert(out == "OK") { out }
    }
    @Test
    fun p26_pool_err() {
        val out = inp2env(
            """
            { @A
                var x:()
                { @A
                }
            }
        """.trimIndent()
        )
        assert(out == "(ln 3, col 7): invalid scope : \"@A\" is already declared (ln 1)") { out }
    }
    @Test
    fun p27_pool_err() {
        val out = inp2env(
            """
            var f: /(func$D{} @{a1}-> ()->())@LOCAL
            set f = func$D{} @{a1}-> () -> () {
                var g: /(func$D{} @{a1}-> ()->())@LOCAL
                set g = func$D{} @{a1}-> () -> () {
                }
            }
        """.trimIndent()
        )
        assert(out == "(ln 4, col 13): invalid scope : \"@a1\" is already declared (ln 2)") { out }
    }
    @Test
    fun p28_pool_ff1() {
        val out = inp2env(
            """
            var f:                  /(func $D{} @{i1} -> () -> ())@LOCAL
            var g: /(func $D{} @{i1} -> /(func $D{} @{i1} -> () -> ())@i1 -> ())@LOCAL
            set _:_ = g\ $D{} @{LOCAL} f
        """.trimIndent()
        )
        assert(out == "OK") { out }
    }
    @Test
    fun p28_pool_ff2() {
        val out = inp2env(
            """
            var f:                  /(func $D{} @{i1} -> /()@i1 -> ())@LOCAL
            var g: /(func $D{} @{i1} -> /(func $D{} @{i1} -> /()@i1 -> ())@i1 -> ())@LOCAL
            set _:_ = g\ $D{} @{LOCAL} f
        """.trimIndent()
        )
        assert(out == "OK") { out }
    }
    @Test
    fun noclo_p29_upwrite_err () {
        val out = inp2env("""
            { @A
                var pa: /</_int @LOCAL> @LOCAL
                var f: func$D{} @{}-> ()->()
                set f = func $D{} @{}-> ()->() {
                    var pf: /</_int @A> @A
                    --set pf = new <.1 <.0>: /</_int @A> @A>:</_int @A>: @A
                    set pa = pf
                }
                call f $D{} @{} ()
                --output std pa
            }
        """.trimIndent())
        //assert(out == "(ln 7, col 16): invalid assignment : cannot modify an upalue") { out }
        assert(out == "OK") { out }
    }
    @Test
    fun noclo_p30_closure_ok0 () {
        val out = inp2env("""
            {
                var f: func $D{} @{} -> () -> ()
                var g: func $D{} @{} -> () -> ()
                set f = g
            }
        """.trimIndent())
        assert(out == "OK") { out }
    }
    @Test
    fun p32_test () {
        val out = inp2env(
            """
            var fact: (func$D{} @{}->() -> ())
            set fact = func$D{} @{} ->() -> () { @F
                var x: _int
                set x = _1: _int
                var p2: /_int@LOCAL
                set p2 = /x
                var p1: /_int@F
                set p1 = /x
            }
        """.trimIndent()
        )
        assert(out == "OK") { out }
    }
    @Test
    fun p33_no_pool () {
        val out = inp2env(
            """
            var f: func $D{} @{a1,b1} -> /()@a1 -> /()@b1
            call f $D{} @{} ()
        """.trimIndent()
        )
        assert(out == "(ln 2, col 6): invalid call : scope mismatch : expecting 2, have 0 argument(s)") { out }
    }
    @Test
    fun p34_diff_args () {
        val out = inp2env(
            """
            var f: func$D{} @{a1,i1,j1} -> [/</_int@a1>@a1,/</_int@i1>@i1] -> /</_int@j1>@j1
            var g: func  $D{} @{i1,j1,k1} -> [/</_int@i1>@i1,/</_int@j1>@j1] -> /</_int@k1>@k1
            set f = g
        """.trimIndent()
        )
        assert(out == "OK") { out }
    }
    @Test
    fun p35_clo () {
        val out = inp2env(
            """
            var f: func $D{} @{GLOBAL} -> func $D{} @{} -> () -> () -> func @GLOBAL -> $D{} @{} -> () -> ()
        """.trimIndent()
        )
        assert(out == "(ln 1, col 19): invalid scope parameter identifier") { out }
    }

    // TYPEDEF / ALIAS

    @Test
    fun q00 () {
        val out = inp2env(
            """
            var x: Unit $D{} @{}
        """.trimIndent()
        )
        assert(out == "(ln 1, col 8): undeclared type \"Unit\"") { out }
    }
    @Test
    fun q01 () {
        val out = inp2env(
            """
            type Unit $D{} @{} = ()
            var x: Unit $D{} @{}
            set x = Unit $D{} @{} ()
            --output std x
        """.trimIndent()
        )
        assert(out == "OK") { out }
    }
    @Test
    fun q02 () {
        val out = inp2env(
            """
            type Unit $D{} @{} = ()
            var x: Unit $D{} @{}
            var y: Unit $D{} @{}
            set x = y
        """.trimIndent()
        )
        assert(out == "OK") { out }
    }
    @Test
    fun q03 () {
        val out = inp2env(
            """
            type Unit $D{} @{} = ()
            var x: Unit $D{} @{}
            var y: ()
            set y = x~
        """.trimIndent()
        )
        assert(out == "OK") { out }
    }
    @Test
    fun q04 () {
        val out = inp2env(
            """
            type Unit $D{} @{} = ()
            type Uxit $D{} @{} = ()
            var x: Unit $D{} @{}
            var y: Uxit $D{} @{}
            set y = x
        """.trimIndent()
        )
        assert(out.startsWith("(ln 5, col 7): invalid assignment : type mismatch")) { out }
    }
    @Test
    fun q05 () {
        val out = inp2env(
            """
            type List $D{} @{} = </List $D{} @{} @LOCAL>
            var x: /List $D{} @{} @LOCAL
            var y: /List $D{} @{} @LOCAL
            set y = x
        """.trimIndent()
        )
        assert(out == "OK") { out }
    }
    @Test
    fun q06 () {
        val out = inp2env(
            """
            type List $D{} @{a} = </List $D{} @{a} @a>
            var x: /List $D{} @{LOCAL} @LOCAL
            var y: /List $D{} @{LOCAL} @LOCAL
            set y = x
        """.trimIndent()
        )
        assert(out == "OK") { out }
    }
    @Test
    fun q07 () {
        val out = inp2env(
            """
            type List $D{} @{a} = </List $D{} @{a} @a>
            type PList $D{} @{} = /<List $D{} @{LOCAL}> @LOCAL
            var x: PList $D{} @{}
            var y: PList $D{} @{}
            set y = x
        """.trimIndent()
        )
        assert(out == "OK") { out }
    }
    @Test
    fun q08 () {
        val out = inp2env(
            """
            type List $D{} @{a} = </List $D{} @{a} @a>
            var x: /List $D{} @{LOCAL} @LOCAL
            {
                var y: /List $D{} @{LOCAL} @LOCAL
                set y = x
            }
        """.trimIndent()
        )
        assert(out == "OK") { out }
    }
    @Test
    fun q09 () {
        val out = inp2env(
            """
            type List $D{} @{a} = </List $D{} @{a} @a>
            var x: /List $D{} @{LOCAL} @LOCAL
            {
                var y: /List $D{} @{LOCAL} @LOCAL
                set x = y
            }
        """.trimIndent()
        )
        assert(out.startsWith("(ln 5, col 11): invalid assignment : type mismatch")) { out }
    }
    @Test
    fun q10 () {
        val out = inp2env(
            """
            type List $D{} @{a} = </List $D{} @{a} @a>
            var x: /List $D{} @{LOCAL} @LOCAL
            {
                var y: /List $D{} @{GLOBAL} @GLOBAL
                set x = y
            }
        """.trimIndent()
        )
        assert(out == "OK") { out }
    }
    @Test
    fun q11 () {
        val out = inp2env(
            """
            type List $D{} @{a} = </List $D{} @{a} @a>
            type PList $D{} @{} = /<List $D{} @{LOCAL}> @LOCAL
            var x: PList $D{} @{}
            {
                var y: PList $D{} @{}
                set x = y
            }
        """.trimIndent()
        )
        assert(out == "OK") { out }
    }
    @Test
    fun q12 () {
        val out = inp2env(
            """
            type PList $D{} @{a} = /<PList $D{} @{a}> @a
            var x: PList $D{} @{LOCAL}
            {
                var y: PList $D{} @{GLOBAL}
                set x = y
            }
        """.trimIndent()
        )
        //assert(out == "OK") { out }
        assert(out == "(ln 1, col 6): invalid recursive type : cannot be a pointer") { out }
    }
    @Test
    fun q13 () {
        val out = inp2env(
            """
            type Xxx $D{} @{a} = _int
            var x: Xxx $D{} @{}
        """.trimIndent()
        )
        assert(out == "(ln 2, col 8): invalid type : scope mismatch : expecting 1, have 0 argument(s)") { out }
    }
    @Test
    fun q14 () {
        val out = inp2env("""
            type List $D{} @{a} = </List $D{} @{a} @a>
            { @A
                var p1: /List $D{} @{LOCAL} @LOCAL
                var p2: /List $D{} @{A} @A
                set p1 = Null: /List $D{} @{LOCAL} @LOCAL
                set p2 = Null: /List $D{} @{A} @A
                set p1 = new List $D{} @{LOCAL} <.1 Null: /List $D{} @{LOCAL} @LOCAL>: </List $D{} @{LOCAL} @LOCAL>: @LOCAL
                set p2 = new List $D{} @{A}     <.1 Null: /List $D{} @{A}     @A>:     </List $D{} @{A}     @A>:     @A
                set p1 = p2
                set p2 = p1
            }
        """.trimIndent())
        assert(out == "OK") { out }
    }
    @Test
    fun q15 () {
        val out = inp2env(
            """
            type Pair $D{} @{a,b: b>a} = [/_int@a, /_int@b]
            {
                var x: _int
                {
                    var y: _int
                    var xy: Pair $D{} @{x,y}     -- OK
                    var yx: Pair $D{} @{y,x}     -- NO
                }
            }
        """.trimIndent()
        )
        assert(out == "(ln 7, col 17): invalid type : scope mismatch : constraint mismatch") { out }
    }
    @Test
    fun q16 () {
        val out = inp2env("""
            type Tx $D{} @{a} = [/_int@a]
            { @A
                var x: _int
                var t1: Tx $D{} @{LOCAL}
                set t1 = Tx $D{} @{LOCAL} [/x]
            }
        """.trimIndent())
        assert(out == "OK") { out }
    }
    @Test
    fun q17 () {
        val out = inp2env("""
            type Tx $D{} @{a} = </_int@a>
            { @A
                var x: _int
                var t1: Tx $D{} @{LOCAL}
                set t1 = Tx $D{} @{LOCAL} <.1 /x>: </_int>
            }
        """.trimIndent())
        assert(out == "(ln 5, col 45): expected scope identifier : have \">\"") { out }
    }
    @Test
    fun q18 () {
        val out = inp2env("""
            type Tx $D{} @{a} = </_int@a>
            { @A
                var x: _int
                var t1: Tx $D{} @{LOCAL}
                set t1 = Tx $D{} @{LOCAL} <.1 /x>: </_int@LOCAL>
            }
        """.trimIndent())
        assert(out == "OK") { out }
    }

    // CLOSURE

    @Test
    fun noclo_r01 () {
        val out = inp2env("""
            { @A
                var pa: /_int@LOCAL
                var f: (func $D{} @{}->()->())
                set f = func $D{} @{}->()->() {     -- OK: pa lives while @A lives
                    var pf: /_int @A
                    set pf = pa
                }
            }
        """.trimIndent())
        assert(out == "OK") { out }
    }
    @Test
    fun noclo_r02 () {
        val out = inp2env("""
            var f: func$D{} @{}->()->()
            {
                var pa: ()
                set pa = ()
                set f = func $D{} @{}->()->() {  -- set [] vs [@A]
                    set _:_ = pa
                }
            }
            call f $D{} @{} ()
        """.trimIndent())
        assert(out.startsWith("(ln 5, col 11): invalid assignment : type mismatch")) { out }
        //assert(out == "OK") { out }
    }
    @Test
    fun noclo_r04 () {
        val out = inp2env("""
            {
                var pa: /_int @LOCAL
                var f: func $D{} @{a1}->[/()@a1]->()
                set f = func $D{} @{a1}->[/()@a1]->() {
                    var pf: /_int @a1
                    set pa = arg
                }
                {
                    var x: ()
                    call f $D{} @{LOCAL} [/x]
                }
            }
        """.trimIndent())
        assert(out.startsWith("(ln 6, col 16): invalid assignment : type mismatch")) { out }
    }
    @Test
    fun noclo_r05 () {
        val out = inp2env("""
            var p: /() @GLOBAL
            {
                var v: ()
                var f : func $D{} @{i1} -> () -> /()@i1
                set f = func $D{} @{i1} -> () -> /()@i1 {
                    set ret = /v      -- err: /v may not be at expected @
                }
                {
                    --set p = f ()    -- p=@ is smaller than /v
                }
            }
        """.trimIndent())
        assert(out.startsWith("(ln 6, col 17): invalid return : type mismatch")) { out }
    }
    @Test
    fun noclo_r06 () {
        val out = inp2env("""
            { @A
                var v: _int
                set v = _10: _int
                var f : func $D{} @{a} -> () -> /_int@a
                set f = func $D{} @{a} -> () -> /_int@a {
                    set ret = /v    -- ERR: scope is outside func
                }                   -- although f lives as much as v, f might escape?
                var p: /_int @LOCAL
                {
                    set p = f $D{} @{A} (): @A
                }
            }
        """.trimIndent())
        //assert(out == "OK") { out }
        assert(out == "(ln 6, col 17): invalid return : type mismatch :\n    /_int @a\n    /_int @V") { out }
    }
    @Test
    fun noclo_r07() {
        val out = inp2env(
            """
            var f: func $D{} @{}->() -> ()
            {
                var x: /</_int@LOCAL>@LOCAL
                set f = func $D{} @{}->() -> () {
                    set _:_ = x
                }
            }
        """.trimIndent()
        )
        assert(out.startsWith("(ln 4, col 11): invalid assignment : type mismatch")) { out }
    }
    @Test
    fun noclo_r08 () {
        val out = inp2env("""
            {
                var x: ()
                var f : func $D{} @{} -> () -> ()
                set f = func $D{} @{} -> () -> () { set ret = x }
            }
        """.trimIndent())
        assert(out == "OK") { out }
    }
    @Test
    fun noclo_r12_err () {
        val out = inp2env(
            """
            var f : func $D{} @{} -> _int -> ()
            set f = func $D{} @{} -> _int -> () { --@A
                var x: _int
                set x = arg
                var g : func $D{} @{} -> () -> _int      -- ERR: needs @A->
                set g = func $D{} @{} -> () -> _int {
                    var h : func $D{} @{} -> () -> _int
                    set h = func $D{} @{} -> () -> _int {
                        set ret = x
                    }
                    set _:_ = h $D{} @{} ()
                }
                call g $D{} @{} ()
            }
            call f $D{} @{} _10:_int
        """.trimIndent()
        )
        assert(out == "OK") { out }
    }

    // AS

    @Test
    fun s01_err () {
        val out = inp2env("""
            set _:_ = ():+()
        """.trimIndent())
        //assert(out == "(ln 1, col 16): expected alias type") { out }
        //assert(out == "(ln 1, col 14): expected statement : have `:+´") { out }
        assert(out == "(ln 1, col 13): expected statement : have \":\"") { out }
    }

    @Test
    fun s02_err () {
        val out = inp2env("""
            type Tx $D{} @{} = [()]
            set _:_ = Tx $D{} @{} ()
        """.trimIndent())
        assert(out.contains("(ln 2, col 11): invalid type pack : type mismatch :")) { out }
    }
    @Test
    fun s03_err () {
        val out = inp2env("""
            set _:_ = ()~
        """.trimIndent())
        assert(out == "(ln 1, col 13): invalid type unpack : expected type alias : found ()") { out }
    }
    @Test
    fun s04_err () {
        val out = inp2env("""
            var x: ()
            set x = ()
            set _:_ = x~
        """.trimIndent())
        assert(out == "(ln 3, col 12): invalid type unpack : expected type alias : found ()") { out }
    }
    @Test
    fun s04_union () {
        val out = inp2env("""
            type Tx $D{} @{}= <()>
            var t: Tx $D{} @{}
            set t = Tx $D{} @{}<.1 ()>:<()>
            var u: <()>
            set u = t~
        """.trimIndent())
        assert(out == "OK") { out }
    }

    @Test
    fun s05_event () {
        val out = inp2env("""
            type Event $D{} @{} = <(),_uint64_t,()>
            emit <.3 ()>: Event $D{} @{}
       """.trimIndent())
        assert(out == "(ln 2, col 15): invalid type : expected union type") { out }
    }

    @Test
    fun s06 () {
        val out = inp2env("""
            type Int2Int $D{} @{} = func $D{} @{} -> _int -> _int
            
            var f: Int2Int $D{} @{}
            set f = Int2Int $D{} @{} func $D{} @{} -> _int -> _int {
                set ret = arg
            }
            
            var x: _int
            set x = f~ $D{} @{} _10:_int
            
            --output std x
       """.trimIndent())
        assert(out == "OK") { out }
    }

    @Test
    fun s07_task_type () {
        val out = inp2env("""
            type Xask $D{} @{}= task $D{} @{}->()->()->()
            var t: Xask $D{} @{}
            spawn t $D{} @{} ()
        """.trimIndent())
        assert(out == "(ln 3, col 7): invalid call : not a function") { out }
    }
    @Test
    fun s08_task_type () {
        val out = inp2env("""
            type Xask $D{} @{} = task $D{} @{}->()->()->()
            var t: Xask $D{} @{}
            spawn (t~) $D{} @{} ()
        """.trimIndent())
        assert(out == "OK") { out }
    }
}
