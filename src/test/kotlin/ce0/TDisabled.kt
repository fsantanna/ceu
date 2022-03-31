import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.MethodOrderer.Alphanumeric
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import java.io.PushbackReader
import java.io.StringReader

@Disabled
@TestMethodOrder(Alphanumeric::class)
class TDisabled {

    fun inp2env (inp: String): String {
        All_restart(null, PushbackReader(StringReader(inp), 2))
        Lexer.lex()
        try {
            val s = Parser.stmts()
            s.setUps(null)
            s.setScp1s()
            s.setEnvs(null)
            check_00_after_envs(s)
            check_01_before_tps(s)
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

    // MUTUTAL RECURSION

    @Test
    fun a11_parser_type_issupof () {
        All_restart(null, PushbackReader(StringReader("<(),<(),^^>>"), 2))
        Lexer.lex()
        val tp1 = Parser.type()
        tp1.visit({ it.wup = Any() }, null)
        val tp2 = (tp1 as Type.Union).vec[1]
        // <(),<(),^^>> = <(),<(),<(),^^>>>
        val ok1 = tp1.isSupOf(tp2)
        val ok2 = tp2.isSupOf(tp1)
        assert(ok1 && ok2)
    }
    @Test
    fun i04_uni_rec_err () {
        val out = inp2env("""
            var xxx: <(),</^^ @LOCAL,/^ @LOCAL>>
            set xxx  = <.1()>:<(),</^^ @LOCAL,/^ @LOCAL>>
        """.trimIndent())
        //assert(out == "(ln 2, col 14): invalid union constructor : expected `new`") { out }
        assert(out == "OK") { out }
    }
    @Test
    fun i05_uni_rec_ok () {
        val out = inp2env("""
            var xxx: /<(),</^^ @LOCAL,/^ @LOCAL>> @LOCAL
            set xxx = new <.1()>:<(),</^^ @LOCAL,/^ @LOCAL>>: @LOCAL
        """.trimIndent())
        assert(out == "OK") { out }
    }
    @Test
    fun i06_uni_rec_ok1 () {
        val out = inp2env("""
            var xxx: /<(),/</^^ @LOCAL,/^ @LOCAL> @LOCAL> @LOCAL
            set xxx = new <.1()>:<(),/</^^ @LOCAL,/^ @LOCAL> @LOCAL>: @LOCAL
        """.trimIndent())
        assert(out == "OK") { out }
    }
    @Test
    fun i07_list_err () {
        val out = inp2env("""
            var xxx: <[_int,</_int @LOCAL>]>
            set xxx = <.0>:</_int @LOCAL>
        """.trimIndent())
        //assert(out == "(ln 2, col 9): invalid assignment : type mismatch") { out }
        //assert(out == "(ln 2, col 13): invalid union constructor : type mismatch") { out }
        assert(out == "(ln 2, col 16): invalid type : expected pointer to union") { out }
    }
    @Test
    fun i08_mutual () {
        val out = inp2env("""
            var e: /<(), <(),/^^ @LOCAL>> @LOCAL
            set e = new <.2 <.1()>:<(),/<(), <(),/^^ @LOCAL>> @LOCAL>>:<(), <(),/^^ @LOCAL>>: @LOCAL
            var s: /<(), <(),/^^ @LOCAL>> @LOCAL
            set s = e\!2!2
        """.trimIndent())
        assert(out == "OK") { out }
    }
    @Test
    fun i09_mutual () {
        val out = inp2env("""
            var e: /<</^^ @LOCAL,()>, ()> @LOCAL
            set e = new <.1 <.2()>:</<</^^ @LOCAL,()>, ()> @LOCAL,()>>:<</^^ @LOCAL,()>, ()>: @LOCAL
            var s: /<</^^ @LOCAL,()>, ()> @LOCAL
            set s = e\!1!1
        """.trimIndent())
        assert(out == "OK") { out }
    }
    @Test
    fun i10_mutual () {
        val out = inp2env("""
            var e: /<</^^ @LOCAL,()>, ()> @LOCAL
            set e = new <.1 new <.2()>:</<</^^ @LOCAL,()>, ()> @LOCAL,()>: @LOCAL>:<</^^ @LOCAL,()>, ()>: @LOCAL
               -- err: ~new~ <.2>
        """.trimIndent())
        //assert(out == "(ln 2, col 7): invalid assignment : type mismatch") { out }
        assert(out.startsWith("(ln 2, col 15): invalid union constructor : type mismatch")) { out }
    }
    @Test
    fun j08_rec_xepr_double_rec_err () {
        val out = inp2env("""
            var x: <</^^^ @LOCAL>>
            --set x = <.1 <.1 <.0>>>
        """.trimIndent())
        assert(out == "(ln 1, col 11): invalid `^^^´ : missing enclosing recursive type") { out }
    }
    @Test
    fun j11_rec_xepr_double_rec () {
        val out = inp2env("""
            var x: /</<[/^^ @LOCAL,/^ @LOCAL]> @LOCAL> @LOCAL
            var z1: /</<[/^^ @LOCAL,/^ @LOCAL]> @LOCAL> @LOCAL
            var z2: /<[/</<[/^^ @LOCAL,/^ @LOCAL]> @LOCAL> @LOCAL,/^ @LOCAL]> @LOCAL
            set z1 = <.0>: /</<[/^^ @LOCAL,/^ @LOCAL]> @LOCAL> @LOCAL
            set z2 = <.0>: /<[/</<[/^^ @LOCAL,/^ @LOCAL]> @LOCAL> @LOCAL,/^ @LOCAL]> @LOCAL
            set x = new <.1 new <.1 [z1,z2]>:<[/</<[/^^ @LOCAL,/^ @LOCAL]> @LOCAL> @LOCAL,/^ @LOCAL]>: @LOCAL>:</<[/^^ @LOCAL,/^ @LOCAL]> @LOCAL>: @LOCAL
        """.trimIndent())
        assert(out == "OK") { out }
    }
    @Test
    fun j29_rec_mutual () {
        val out = inp2env("""
            var e: /<(),<(),/^^ @LOCAL>> @LOCAL
            set e = new <.2 <.1()>:<(),/<(),<(),/^^ @LOCAL>> @LOCAL>>:<(),<(),/^^ @LOCAL>>: @LOCAL
        """.trimIndent())
        assert(out == "OK") { out }
    }
    @Test
    fun n08_hold_err () {
        val out = inp2env("""
            var x: /<? <?//^^>>  -- err: <? ...>
        """.trimIndent())
        //assert(out == "(ln 1, col 12): invalid type declaration : unexpected `?´") { out }
        //assert(out == "(ln 1, col 14): invalid type declaration : unexpected `^´") { out }
        assert(out == "(ln 1, col 10): expected type : have `?´") { out }
    }
    @Test
    fun n08_hold_ok3 () {
        val out = inp2env("""
            var x: /<? [<?//^^>,/^]>
            set x = new <.1 [<.0>,<.0>]>
            set x\!1.1 = <.1 /x>  -- ok
        """.trimIndent())
        //assert(out == "OK") { out }
        //assert(out == "(ln 2, col 20): unexpected <.0> : not a pointer") { out }
        //assert(out == "(ln 1, col 14): invalid type declaration : unexpected `^´") { out }
        //assert(out == "(ln 1, col 13): invalid type declaration : unexpected `?´") { out }
        assert(out == "(ln 1, col 10): expected type : have `?´") { out }
    }
    @Test
    fun n09_hold_err () {
        val out = inp2env("""
            var x: /<? [<?//^^>,/^]>
            set x = new <.1 [<.0>,<.0>]>
            var y: /<? [<?//^^>,/^]>
            set y = x
            output std y
        """.trimIndent())
        //assert(out == "(ln 2, col 35): invalid `consume` : expected recursive variable") { out }
        //assert(out == "(ln 1, col 14): invalid type declaration : unexpected `^´") { out }
        //assert(out == "OK") { out }
        //assert(out == "(ln 2, col 20): unexpected <.0> : not a pointer") { out }
        //assert(out == "(ln 1, col 13): invalid type declaration : unexpected `?´") { out }
        assert(out == "(ln 1, col 10): expected type : have `?´") { out }
    }
    /*
    @Test
    fun j11_rec_double2 () {
        val out = all("""
            var n: /<</^^ @LOCAL>> @LOCAL
            set n = <.0>: /<</^^ @LOCAL>> @LOCAL
            output std n
        """.trimIndent())
        assert(out == "<.0>\n") { out }
    }
    @Test
    fun l02_hold_ok () {
        val out = all("""
            var x: /< [<(),//^^ @LOCAL @LOCAL>,_int,/^ @LOCAL]> @LOCAL
            var z: /< [<(),//^^ @LOCAL @LOCAL>,_int,/^ @LOCAL]> @LOCAL
            set z = <.0>: /< [<(),//^^ @LOCAL @LOCAL>,_int,/^ @LOCAL]> @LOCAL
            set x = new <.1 [z,_1: _int,new <.1 [z,_2: _int,z]>:< [<(),//^^ @LOCAL @LOCAL>,_int,/^ @LOCAL]>: @LOCAL]>:< [<(),//^^ @LOCAL @LOCAL>,_int,/^ @LOCAL]>: @LOCAL
            set x!1.3!1.1 = <.1 /x>: <(),//< [<(),//^^ @LOCAL @LOCAL>,_int,/^ @LOCAL]> @LOCAL @LOCAL>
            set x!1.1 = <.1 /x!1.3>: <(),//< [<(),//^^ @LOCAL @LOCAL>,_int,/^ @LOCAL]> @LOCAL @LOCAL> -- err: address of field inside union
            output std x!1.3!1.2
            output std x!1.1!1\!1.1!1\!1.2
        """.trimIndent())
        //assert(out == "2\n1\n") { out }
        //assert(out == "(ln 1, col 14): invalid type declaration : unexpected `^´") { out }
        assert(out == "(ln 6, col 17): invalid operand to `/´ : union discriminator") { out }
    }
    @Test
    fun l02_hold_ok1 () {
        val out = all("""
            var x: /< [<(),/^^ @LOCAL>,_int,/^ @LOCAL]> @LOCAL
            var z: /< [<(),/^^ @LOCAL>,_int,/^ @LOCAL]> @LOCAL
            set z = <.0>: /< [<(),/^^ @LOCAL>,_int,/^ @LOCAL]> @LOCAL
            var o: <(),/< [<(),/^^ @LOCAL>,_int,/^ @LOCAL]> @LOCAL>
            set o = <.1()>: <(),/< [<(),/^^ @LOCAL>,_int,/^ @LOCAL]> @LOCAL>
            set x = new <.1 [o,_1:_int,new <.1 [o,_2:_int,z]>:< [<(),/^^ @LOCAL>,_int,/^ @LOCAL]>: @LOCAL]>:< [<(),/^^ @LOCAL>,_int,/^ @LOCAL]>: @LOCAL
            set x\!1.3\!1.1 = <.2 x>: <(),/< [<(),/^^ @LOCAL>,_int,/^ @LOCAL]> @LOCAL>
            set x\!1.1 = <.2 x\!1.3>: <(),/< [<(),/^^ @LOCAL>,_int,/^ @LOCAL]> @LOCAL>
            output std x\!1.3\!1.2
            output std x\!1.2
        """.trimIndent())
        assert(out == "2\n1\n") { out }
        //assert(out == "(ln 1, col 14): invalid type declaration : unexpected `^´") { out }
        //assert(out == "(ln 4, col 17): invalid operand to `/´ : union discriminator") { out }
    }
    @Test
    fun l02_hold_ok2 () {
        val out = all("""
            var z: /< [<(),//^^ @LOCAL @LOCAL>,_int,/^ @LOCAL]> @LOCAL
            set z = <.0>: /< [<(),//^^ @LOCAL @LOCAL>,_int,/^ @LOCAL]> @LOCAL
            var o: <(),//< [<(),//^^ @LOCAL @LOCAL>,_int,/^ @LOCAL]> @LOCAL @LOCAL>
            set o = <.1()>: <(),//< [<(),//^^ @LOCAL @LOCAL>,_int,/^ @LOCAL]> @LOCAL @LOCAL>

            var x: /< [<(),//^^ @LOCAL @LOCAL>,_int,/^ @LOCAL]> @LOCAL
            set x = new <.1 [o,_2:_int,z]>:< [<(),//^^ @LOCAL @LOCAL>,_int,/^ @LOCAL]>: @LOCAL
            output std x
        """.trimIndent())
        assert(out == "<.1 [<.1>,2,<.0>]>\n") { out }
        //assert(out == "(ln 1, col 14): invalid type declaration : unexpected `^´") { out }
    }
*/

    // NULL w/ ALIAS

    @Test
    fun b12_user_empty_err () {
        val out = inp2env("""
            var l: </_int@LOCAL>
            set l = <.1 <.0>:/</_int@LOCAL>@LOCAL>:</_int@LOCAL>
            --output std l!0
        """.trimIndent())
        //assert(out == "(ln 2, col 11): invalid expression : expected `new` operation modifier") { out }
        //assert(out == "(ln 2, col 11): invalid union constructor : expected `new`") { out }
        assert(out == "OK") { out }
    }
    @Test
    fun b13_user_empty_ok () {
        val out = inp2env("""
            var l: </_int @LOCAL>
            set l = new <.1 <.0>:/</_int@LOCAL>@LOCAL>:</_int@LOCAL>: @LOCAL
        """.trimIndent())
        //assert(out.startsWith("(ln 2, col 7): invalid assignment : type mismatch")) { out }
        assert(out.startsWith("(ln 2, col 9): invalid `new` : expected alias constructor")) { out }
    }
    @Test
    fun j07_rec_xepr_copy_err () {
        val out = inp2env("""
            var x: </_int @LOCAL>
            set x = <.1 <.0>:/</_int @LOCAL>@LOCAL>:</_int @LOCAL>
        """.trimIndent())
        //assert(out == "(ln 1, col 20): expected expression : have `<´") { out }
        //assert(out == "(ln 2, col 11): invalid union constructor : expected `new`") { out }
        //assert(out == "(ln 1, col 20): invalid `copy` : expected recursive variable")
        //assert(out == "(ln 1, col 5): invalid assignment : expected `new` operation modifier")
        assert(out == "OK") { out }
    }
    @Test
    fun j13_tup_move_no () {
        val out = inp2env("""
            var t1: [/</_int @LOCAL> @LOCAL]
            set t1 = [<.0>:/</_int @LOCAL> @LOCAL]
            var t2: [/</_int @LOCAL> @LOCAL]
            set t2 = t1
        """.trimIndent())
        //assert(out == "(ln 2, col 26): invalid `replace` : expected recursive variable") { out }
        assert(out == "OK") { out }
    }
    @Test
    fun j13_tup_move_ok () {
        val out = inp2env("""
            var l: /</_int @LOCAL> @LOCAL
            set l = <.0>:/</_int @LOCAL> @LOCAL
            var t1: [/</_int @LOCAL> @LOCAL]
            set t1 = [l]
            var t2: [/</_int @LOCAL> @LOCAL]
            set t2 = [t1.1]
        """.trimIndent())
        //assert(out == "(ln 3, col 17): invalid `replace` : expected recursive variable")
        assert(out == "OK") { out }
    }
    @Test
    fun j14_new_no () {
        val out = inp2env("""
            var l: /</_int @LOCAL> @LOCAL
            set l = new <.0>:/</_int @LOCAL> @LOCAL
        """.trimIndent())
        //assert(out == "(ln 2, col 9): invalid `new` : expected constructor") { out }
        //assert(out == "(ln 2, col 9): invalid `new` : unexpected <.0>") { out }
        //assert(out == "(ln 2, col 7): invalid assignment : type mismatch") { out }
        assert(out == "(ln 2, col 9): invalid `new` : expected alias constructor") { out }
    }
    @Test
    fun l01_borrow_err () {
        val out = inp2env("""
            var x: /</_int @LOCAL> @LOCAL
            var y: //</_int @LOCAL> @LOCAL @LOCAL
            set y = /x --!1
            set x = <.0>:/</_int @LOCAL> @LOCAL
        """.trimIndent())
        //assert(out == "(ln 3, col 7): invalid assignment of \"x\" : borrowed in line 2") { out }
        assert(out == "OK") { out }
    }
    @Test
    fun l03_borrow_err () {
        val out = inp2env("""
            var x: /</_int @LOCAL> @LOCAL
            var f: func@{i1}->//</_int@i1>@i1@i1 -> ()
            set f = func@{i1}->//</_int@i1>@i1@i1 -> ()
            {
                set x = <.0>:/</_int@GLOBAL>@GLOBAL
            }
            call f @{LOCAL} /x --!1
        """.trimIndent())
        //assert(out == "(ln 4, col 9): undeclared variable \"x\"") { out }
        //assert(out == "(ln 4, col 11): invalid assignment of \"x\" : borrowed in line 3") { out }
        assert(out == "OK") { out }
    }
    @Test
    fun l03_borrow_err2 () {
        val out = inp2env("""
            var x: /</_int @LOCAL> @LOCAL
            var f: func@{i1}->//</_int@i1>@i1@i1 -> ()
            set f = func@{i1}->//</_int@i1>@i1@i1 -> ()
            {
                set x = <.0>:/</_int@GLOBAL>@GLOBAL
            }
            var y: //</_int @LOCAL> @LOCAL @LOCAL
            set y = /x --!1
            call f  @{LOCAL} y
        """.trimIndent())
        assert(out == "OK") { out }
        //assert(out == "(ln 4, col 9): undeclared variable \"x\"") { out }
        //assert(out == "(ln 4, col 11): invalid assignment of \"x\" : borrowed in line 6") { out }
    }
    @Test
    fun l04_borrow_err () {
        val out = inp2env("""
            var x: /</_int @LOCAL> @LOCAL
            var f: (func@{}->() -> ())
            set f = func@{}->() -> () {
                set x = <.0>:/</_int@GLOBAL>@GLOBAL
            }
            var y: //</_int @LOCAL> @LOCAL @LOCAL
            set y = /x --!1
            var g: func@{}->() -> ()
            set g = f
            call g  ()
        """.trimIndent())
        assert(out == "OK") { out }
        //assert(out == "(ln 3, col 9): undeclared variable \"x\"") { out }
        //assert(out == "(ln 3, col 11): invalid assignment of \"x\" : borrowed in line 5") { out }
    }
    @Test
    fun l05_borrow_err () {
        val out = inp2env("""
            var x: /</_int @LOCAL> @LOCAL
            var y: //</_int @LOCAL> @LOCAL @LOCAL
            set y = /x --!1
            set x = <.0>:/</_int @LOCAL> @LOCAL
        """.trimIndent())
        //assert(out == "(ln 3, col 7): invalid assignment of \"x\" : borrowed in line 2") { out }
        assert(out == "OK") { out }
    }
    @Test
    fun l05_borrow_err2 () {
        val out = inp2env("""
            var x: /</_int @LOCAL> @LOCAL
            var y: /</_int @LOCAL> @LOCAL
            set y = x --!1
            set x = <.0>:/</_int @LOCAL> @LOCAL
        """.trimIndent())
        //assert(out == "(ln 3, col 7): invalid assignment of \"x\" : borrowed in line 2") { out }
        assert(out == "OK") { out }
    }
    @Test
    fun l06_borrow_err () {
        val out = inp2env("""
            var x: /</_int @LOCAL> @LOCAL
            var y: [(),//</_int @LOCAL> @LOCAL @LOCAL]
            set y = [(), /x] --/x!1]
            set x = <.0>: /</_int @LOCAL> @LOCAL
        """.trimIndent())
        //assert(out == "(ln 3, col 7): invalid assignment of \"x\" : borrowed in line 2") { out }
        assert(out == "OK") { out }
    }
    @Test
    fun l06_borrow_err2 () {
        val out = inp2env("""
            var x: /</_int @LOCAL> @LOCAL
            var y: [(),/</_int @LOCAL> @LOCAL]
            set y = [(), x] --/x!1]
            set x = <.0>: /</_int @LOCAL> @LOCAL
        """.trimIndent())
        //assert(out == "(ln 3, col 7): invalid assignment of \"x\" : borrowed in line 2") { out }
        assert(out == "OK") { out }
    }
    @Test
    fun l08_borrow_err () {
        val out = inp2env("""
            var x: [//</_int @LOCAL> @LOCAL @LOCAL,/</_int @LOCAL> @LOCAL]
            set x = [_://</_int @LOCAL> @LOCAL @LOCAL,<.0>:/</_int @LOCAL> @LOCAL]
            set x.1 = /x.2 --!1
            var y: /</_int @LOCAL> @LOCAL
            set y = x.2
        """.trimIndent())
        assert(out == "OK") { out }
        //assert(out == "(ln 2, col 9): invalid assignment of \"x\" : borrowed in line 2") { out }
    }
    @Test
    fun l08_borrow_rec_err () {
        val out = inp2env("""
        var x: /</_int @LOCAL> @LOCAL
        var f: ( func@{}->()->())
        set f = func@{}->()->() {
            set x = <.0>:/</_int @GLOBAL> @GLOBAL
            var y: //</_int @LOCAL> @LOCAL @LOCAL
            set y = /x
            set ret = f ()
        }
        call f ()
        """.trimIndent())
        assert(out == "OK") { out }
        //assert(out == "(ln 3, col 9): undeclared variable \"x\"") { out }
        //assert(out == "(ln 3, col 11): invalid assignment of \"x\" : borrowed in line 4") { out }
    }
    @Test
    fun l09_consume_err () {
        val out = inp2env("""
            var x: /</_int @LOCAL> @LOCAL
            set x = <.0>:/</_int @LOCAL> @LOCAL
            var y: /</_int @LOCAL> @LOCAL
            set y = x
            set x = <.0>:/</_int @LOCAL> @LOCAL
        """.trimIndent())
        assert(out == "OK") { out }
        // TODO: could accept b/c x is being reassigned
        //assert(out == "(ln 3, col 5): invalid access to \"x\" : consumed in line 2") { out }
    }
    @Test
    fun l10_consume_err () {
        val out = inp2env("""
            var x: /</_int @LOCAL> @LOCAL
            set x = <.0>: /</_int @LOCAL> @LOCAL
            var y: /</_int @LOCAL> @LOCAL
            set y = x
            output std /x
        """.trimIndent())
        assert(out == "OK") { out }
        //assert(out == "(ln 3, col 13): invalid access to \"x\" : consumed in line 2") { out }
    }
    @Test
    fun l11_consume_err () {
        val out = inp2env("""
            var x: /</_int @LOCAL> @LOCAL
            set x = <.0>: /</_int @LOCAL> @LOCAL
            var y: //</_int @LOCAL> @LOCAL @LOCAL
            set y = /x
            var z: /</_int @LOCAL> @LOCAL
            set z = y\
            output std /x
        """.trimIndent())
        assert(out == "OK") { out }
        //assert(out == "(ln 4, col 13): invalid access to \"x\" : consumed in line 3") { out }
    }
    @Test
    fun l12_replace_ok () {
        val out = inp2env("""
            var x: /</_int @LOCAL> @LOCAL
            set x = <.0>: /</_int @LOCAL> @LOCAL
            var y: //</_int @LOCAL> @LOCAL @LOCAL
            set y = /x
            var z: /</_int @LOCAL> @LOCAL
            set z = y\
            output std /x
        """.trimIndent())
        assert(out == "OK") { out }
    }
    @Test
    fun l13_consume_okr () {
        val out = inp2env("""
            var x: /</_int @LOCAL> @LOCAL
            set x = <.0>: /</_int @LOCAL> @LOCAL
            set x = x
            output std /x
        """.trimIndent())
        assert(out == "OK") { out }
    }
    @Test
    fun l13_consume_ok2 () {
        val out = inp2env("""
            var f: func@{i1}->()->/</_int @i1> @i1
            set f = func@{i1}->()->/</_int @i1> @i1 {
                var x: /</_int @LOCAL> @LOCAL
                set x = <.0>: /</_int @LOCAL> @LOCAL
                set ret = x    -- err
            }
        """.trimIndent())
        assert(out.startsWith("(ln 5, col 13): invalid return : type mismatch")) { out }
    }
    @Test
    fun l13_consume_err () {
        val out = inp2env("""
            var x: /</_int @LOCAL> @LOCAL
            set x = <.0>: /</_int @LOCAL> @LOCAL
            set x\!1 = x
        """.trimIndent())
        assert(out == "OK") { out }
        //assert(out == "(ln 2, col 9): invalid assignment of \"x\" : consumed in line 2") { out }
    }
    @Test
    fun l14_consume_ok () {
        val out = inp2env("""
            var string_c2ce: (func@{i1}->_(char*)->/<[_int,/_int @i1]> @i1)
            set string_c2ce = func@{i1}->_(char*)->/<[_int,/_int @i1]> @i1 {
                var xxx: /</_int @LOCAL> @LOCAL
                set xxx = <.0>:/</_int @LOCAL> @LOCAL
                loop {
                    set xxx = xxx
                }
                var zzz: /</_int @LOCAL> @LOCAL
                set zzz = xxx
                --return ret
            }
            call string_c2ce @{LOCAL} _x:_(char*)
        """.trimIndent())
        assert(out == "OK") { out }
    }
    @Test
    fun m03 () {
        val out = inp2env("""
            var l: </_int @LOCAL>
            set l = <.0>:</_int @LOCAL>    -- ERR: l is not a pointer, cannot accept NULL
            output std /l
        """.trimIndent())
        //assert(out == "(ln 2, col 11): unexpected <.0> : not a pointer") { out }
        //assert(out == "(ln 2, col 11): invalid union constructor : type mismatch") { out }
        assert(out == "(ln 2, col 14): invalid type : expected pointer to union") { out }
    }

    // <.0> must be pointer to alias (not alias as pointer)

    /*
    @Test
    fun nullptr_o06_type_ptr () {
        val out = all("""
            type List @{s} = /<List @{s}> @s
            var l: List @{LOCAL}
            set l = new <.1 <.0>:List @{LOCAL}>:<List @{LOCAL}>
            output std l\!1\!0
        """.trimIndent())
        assert(out == "()\n") { out }
    }
    @Test
    fun o11_type_ptr () {
        val out = all("""
            type Num @{i} = /<Num @{i}> @i
            var zero: Num @{GLOBAL}
            set zero = <.0>: Num @{GLOBAL}
            var one: Num @{GLOBAL}
            set one = (new <.1 zero>: <Num @{GLOBAL}>:+ ???: @GLOBAL)
            output std one
        """.trimIndent())
        assert(out == "()\n") { out }
    }
     */

    // TYPE / ALIAS / RECURSIVE / CANNOT BE POINTER

    /*
    @Test
    fun c09_null () {
        val out = all("""
            type List @{i} = /</List @{i} @i> @i
            var f : func List -> ()
            call f <.0>
        """.trimIndent())
        assert(out == """
            type List @{i} = /</List @{i} @i> @i
            var f: func @{i} -> List @{i} -> ()
            call (f @{GLOBAL} <.0>: List @{GLOBAL})

        """.trimIndent()) { out }
    }
    @Test
    fun c09_null2 () {
        val out = all("""
            type List = /</List>
            var f : func List -> ()
            call f <.0>
        """.trimIndent())
        assert(out == """
            type List @{i} = /</List @{i} @i> @i
            var f: func @{i} -> List @{i} -> ()
            call (f @{GLOBAL} <.0>: List @{GLOBAL})

        """.trimIndent()) { out }
    }
    @Test
    fun e07_ptr_num() {
        val out = all("""
            type Num = /<Num>
            var zero:  Num = <.0>
            var one:   Num = new <.1 zero>
        """.trimIndent())
        assert(out == """
            type Num @{i} = /<Num @{i}> @i
            var zero: Num @{GLOBAL}
            set zero = <.0>: Num @{GLOBAL}
            var one: Num @{GLOBAL}
            set one = (new <.1 zero>: <Num @{GLOBAL}>: @GLOBAL)

        """.trimIndent()) { out }
    }

    @Test
    fun e06_type() {
        val out = all("""
            type List = /<List>
            var l1: List = <.0>
            var l2: List = new <.1 <.0>>
            var l3: List = new <.1 l2>
        """.trimIndent())
        assert(out == """
            type List @{i} = /<List @{i}> @i
            var l1: List @{GLOBAL}
            set l1 = <.0>: List @{GLOBAL}
            var l2: List @{GLOBAL}
            set l2 = (new <.1 <.0>: List @{GLOBAL}>: <List @{GLOBAL}>: @GLOBAL)
            var l3: List @{GLOBAL}
            set l3 = (new <.1 l2>: <List @{GLOBAL}>: @GLOBAL)

        """.trimIndent()) { out }
    }
     */

    // CLOSURES

    @Test
    fun noclo_d01_type_task () {
        All_restart(null, PushbackReader(StringReader("task @LOCAL->@{}->()->()->() {}"), 2))
        Lexer.lex()
        val tp = Parser.type()
        assert(tp is Type.Func && tp.tk.str=="task")
    }
    @Test
    fun noclo_d09_tasks () { // task @LOCAL->@{}->()->()->() {}
        All_restart(null, PushbackReader(StringReader("active tasks @{}->()->()->()"), 2))
        Lexer.lex()
        try {
            Parser.type()
            error("impossible case")
        } catch (e: Throwable) {
            assert(e.message == "(ln 1, col 14): expected `@´ : have `@[´") { e.message!! }
        }
    }

    @Test
    fun noclo_p17_pool_closure_ok() {
        val out = inp2env(
            """
            var g: (func@{a1}->() -> (func@{a1}->()->()))
            set g = func@{a1}->() -> (func@{a1}->()->()) {
                var f:(func@{b1}->() -> ())
                set f = func@{b1}->() -> () {
                    output std ()
                }           
               set ret = f
            }
            var f: (func@{a1}->() -> ())
            set f = g @{LOCAL} ()
            call f @{LOCAL} ()
        """.trimIndent()
        )
        assert(out == "OK") { out }
    }
    @Test // passou a falhar qd mudei env p/ upvals
    fun noclo_p22_pool_closure_err() {
        val out = inp2env(
            """
            var g : func @{a1}->() -> (func @{a1}->()->())
            set g = func @{a1}->() -> (func @{a1}->()->()) {
                var f: func @{b1}->() -> ()
                var x: /</_int@a1>@a1
                --set x = new <.1 <.0>:/</_int@a1>@a1>: </_int@a1>: @a1
                set f = func @{b1}->()  -> () {
                    output std x
                }
                set ret = f
            }
            var f : func @{a1}->() -> ()
            set f = g @{LOCAL} ()
            call f @{LOCAL} ()
        """.trimIndent()
        )
        //assert(out == "(ln 7, col 20): invalid access to \"x\" : invalid closure declaration (ln 6)") { out }
        assert(out == "(ln 7, col 20): undeclared variable \"x\"") { out }
    }
    @Test
    fun noclo_p30_closure_ok () {
        val out = inp2env("""
            var g: func @{a1}->() -> (func @a1->@{b1}->()->/</_int@b1>@b1)
        """.trimIndent())
        assert(out == "OK") { out }
    }
    @Test
    fun noclo_p31_closure_err () {
        val out = inp2env("""
            var g: func @{a1} -> () -> (func @a1->@{}->()->/</_int@b1>@b1)
        """.trimIndent())
        assert(out == "(ln 1, col 29): invalid function type : missing scope argument") { out }
    }
    @Test
    fun noclo_q18_curry () {
        val out = inp2env("""
            type Num @{s} = /<Num @{s}> @s
            var add: func @{a,b,r} -> [Num @{a},Num @{b]} -> Num @{r}
            var curry: func @{} -> func @{a,b,r} -> [Num @{a},Num @{b]} -> Num @{r} -> func @GLOBAL -> @{a} -> Num @{a} -> func @a -> @{b,r} -> Num @{b} -> Num @{r}
            var addc: func @GLOBAL -> @{a} -> Num @{a} -> func @a -> @{b,r} -> Num @{b} -> Num @{r}
            set addc = (curry @{} add: @GLOBAL)
        """.trimIndent())
        assert(out == "OK") { out }
    }

    @Test
    fun noclo_r03 () {
        val out = inp2env("""
            var f: func@{}->()->()
            { @A
                var pa: ()
                set pa = ()
                set f = func @A->@{}->()->() {
                    output std pa
                }
            }
            call f()
        """.trimIndent())
        //assert(out == "(ln 5, col 13): undeclared variable \"xxx\"") { out }
        assert(out.startsWith("(ln 5, col 11): invalid assignment : type mismatch :")) { out }
    }
    @Test
    fun noclo_r09 () {
        val out = inp2env("""
            var f: func () -> _int          -- 1. `f` is a reference to a function
            {
                var x: _int
                set x = _10: _int
                set f = func () -> _int {   -- 2. `f` is created
                    set ret = x             -- 3. `f` needs access to `x`
                    return
                }
            }                               -- 4. `x` goes out of scope
            call f ()                       -- 5. `f` still wants to access `x`
        """.trimIndent()
        )
        //assert(out == "(ln 6, col 19): invalid access to \"x\" : invalid closure declaration (ln 5)") { out }
        assert(out == "(ln 6, col 19): undeclared variable \"x\"") { out }
    }
    @Test
    fun noclo_r11 () {
        val out = inp2env(
            """
            var g: func @{a1}->() -> func @a1->@{}-> ()->()
            set g = func@{a1}->() -> func @a1->@{}-> ()->() {
                var x: ()
                var f: func @a1->@{}-> () -> ()
                set f = func @a1 ->() -> () {   -- ERR: x is between f and a1, so it will leak
                    output std x
                }
            }
        """.trimIndent()
        )
        assert(out == "(ln 6, col 20): undeclared variable \"x\"") { out }
    }
    @Test
    fun noclo_s06_pool_closure () {
        val out = inp2env(
            """
            var f: func@{}-> (func@{}->()->()) -> func @GLOBAL->()->()
            set f = func@{}-> func@{}->()->() -> (func @GLOBAL->()->()) {
                var ff: func@{}->()->()
                set ff = arg
                set ret = func @GLOBAL->@{}->()->() {   -- ERR: ff is between f and a1, so it will leak
                    call ff ()
                }
            }
            var u: func()->()
            set u = func()->() {
                output std ()
            }
            var ff: (func @GLOBAL->@{}->()->())
            set ff = f u
            call ff ()
        """.trimIndent()
        )
        //assert(out == "()\n") { out }
        assert(out == "(ln 6, col 14): undeclared variable \"ff\"") { out }
    }

    /*
    @Test
    fun noclo_n07_pool_closure() {
        val out = all(
            """
            var g: func@{a1}-> () -> func @{a1}->()->()
            set g = func @{a1}->() -> func@{a1}->()->() {
                var f: func@{b1}-> () -> ()
                set f = func@{b1}-> () -> () {
                    output std ()
                }
               set ret = f
            }
            var f: func@{a1}->() -> ()
            set f = g @{LOCAL} ()
            call f @{LOCAL} ()
        """.trimIndent()
        )
        assert(out == "()\n") { out }
    }
    @Test
    fun noclo_n08_pool_closure() {
        val out = all(
            """
            type List @{a} = </List @{a} @a>
            var g: func @{a1}->() -> func @a1->@{}-> ()->()
            set g = func@{a1}->() -> func @a1->@{}-> ()->() {
                var x: /(List @{a1})@a1
                set x = new <.1 <.0>:/(List @{a1})@a1>:</List @{a1} @a1>:+ (List @{a1}): @a1
                var f: func @a1->@{}-> () -> ()
                set f = func @a1 ->() -> () {   -- ERR: x is between f and a1, so it will leak
                    output std x
                }
               set ret = f
            }
            var f: func @LOCAL -> @{} -> () -> ()
            set f = g @{LOCAL} ()
            call f @{}()
        """.trimIndent()
        )
        //assert(out == "<.1 <.0>>\n") { out }
        assert(out == "(ln 8, col 20): undeclared variable \"x\"") { out }
    }
    @Test
    fun noclo_n10_pool_closure () {
        val out = all(
            """
            var cnst: func@{i1}-> /_int@i1 -> (func @i1-> () -> /_int@i1)
            set cnst = func@{i1}-> /_int@i1 -> (func @i1-> () -> /_int@i1) {
                var x: /_int@i1
                set x = arg
                set ret = func@i1->@{}-> () -> /_int@i1 {   -- ERR: x is between f and i1, so it will leak
                    set ret = x
                }
            }
            { @AAA
            var five: _int
            set five = _5: _int
            var f: func @LOCAL -> @{} -> () -> /_int@LOCAL
            set f = cnst @{LOCAL} /five
            var v: /_int@LOCAL
            set v = f ()
            output std v\ --@LOCAL
            }
        """.trimIndent()
        )
        //assert(out == "5\n") { out }
        assert(out == "(ln 6, col 19): undeclared variable \"x\"") { out }
    }
    @Test
    fun noclo_n11_pool_closure () {
        val out = all(
            """
            var cnst:  func@{i1}-> /_int@i1 -> (func @i1-> () -> /_int@i1)
            set cnst = func@{i1}-> /_int@i1 -> (func @i1-> () -> /_int@i1) {
                var x: /_int@i1
                set x = arg
                set ret = func @i1->@{}-> () -> /_int@i1 {
                    set ret = x
                }
            }
            var five: _int
            set five = _5: _int
            var f: func @LOCAL -> @{} -> () -> /_int@LOCAL
            set f = cnst @{LOCAL} /five
            var v: /_int@LOCAL
            set v = f ()
            output std v\ --@LOCAL
        """.trimIndent()
        )
        //assert(out == "5\n") { out }
        assert(out == "(ln 6, col 19): undeclared variable \"x\"") { out }
    }
    @Disabled
    @Test
    fun noclo_n09_pool_closure_err() {
        val out = all(
            """
            type List @{a} = </List @{a} @a>
            var g: /func @{i1}->() -> func@{i1}-> ()->()@LOCAL
            set g = func@{i1}-> () -> func@{i1}-> ()->() {
                var f: /func@{a1}-> () -> ()@LOCAL
                var x: /(List @{LOCAL})@LOCAL
                set x = new <.1 <.0>:/(List @{LOCAL})@LOCAL>:</List @{LOCAL} @LOCAL>:+ (List @{LOCAL}): @LOCAL
                set f = func@{a1}-> () -> () {
                    output std x    -- f uses x in @LOCAL
                }
               set ret = f             -- cannot return f which uses x in @LOCAL
            }
            var f:/(func  @{i1}-> () -> () )@LOCAL
            set f = g\ @{LOCAL} ()
            call f\ @{LOCAL} ()
        """.trimIndent()
        )
        //assert(out == "(ln 8, col 20): invalid access to \"x\" : invalid closure declaration (ln 7)") { out }
        assert(out == "(ln 8, col 20): undeclared variable \"x\"") { out }
    }
    @Disabled
    @Test
    fun noclo_z17_curry () {
        val out = all("""
            type Num @{s} = </Num @{s} @s>
            var add: func @{a,b,r} -> [/Num @{a} @a,/Num @{b] @b} -> /Num @{r} @r
            var curry: func @{} -> func @{a,b,r} -> [/Num @{a} @a,/Num @{b] @b} -> /Num @{r} @r -> func @GLOBAL -> @{a} -> /Num @{a} @a -> func @a -> @{b,r} -> /Num @{b} @b -> /Num @{r} @r
            var addc: func @GLOBAL -> @{a} -> /Num @{a} @a -> func @a -> @{b,r} -> /Num @{b} @b -> /Num @{r} @r
            --set addc = (curry @{} add: @GLOBAL)
            output std ()
        """.trimIndent())
        assert(out == "()\n") { out }
    }

    @Disabled   // no more full closures
    @Test
    fun ch_01_04_addc_pg12() {
        val out = all(
            """
            $X.nums
            $X.clone
            $add
            -- 34
            var plusc : func @{a1}-> $getNumA1 -> (func @a1->@{r1,b1}->$getNumB1->$getNumR1)
            set plusc = func @{a1}-> $getNumA1 -> (func @a1->@{r1,b1}->$getNumB1->$getNumR1) {
                var x: $getNumA1
                set x = arg
                set ret = func @a1->@{r1,b1}->$getNumB1->$getNumR1 [x] {
                    set ret = add @{r1,a1,b1} [x,arg]: @r1
                }
            }
            var f: func @LOCAL->@{r1,b1}->$getNumB1->$getNumR1
            set f = plusc @{LOCAL} one
            output std f @{LOCAL,LOCAL} two: @LOCAL
            output std f @{LOCAL,LOCAL} one: @LOCAL
            output std (plusc @{LOCAL} one) @{LOCAL,LOCAL} zero: @LOCAL
        """.trimIndent()
        )
        assert(out == "<.1 <.1 <.1 <.0>>>>\n<.1 <.1 <.0>>>\n<.1 <.0>>\n") { out }
    }
    @Disabled   // no more full closures
    @Test
    fun ch_01_04_quad_pg12() {
        val out = all(
            """
            $X.nums
            $X.clone
            $add
            $X.mul
            var square: func @{r1,a1}-> $getNumA1 -> $getNumR1
            set square = func @{r1,a1}-> $getNumA1 -> $getNumR1 {
                set ret = mul @{r1,a1,a1} [arg,arg]: @r1
            }
            var twicec:  func @{} -> (func @{r1,a1}->$getNumA1->$getNumR1) -> (func @GLOBAL->@{s1,b1}->$getNumB1->$getNumS1)
            set twicec = func @{} -> (func @{r1,a1}->$getNumA1->$getNumR1) -> (func @GLOBAL->@{s1,b1}->$getNumB1->$getNumS1) {
                var f: func @{r1,a1}->$getNumA1->$getNumR1
                set f = arg
                set ret = func @GLOBAL->@{s1,b1}->$getNumB1->$getNumS1 [f] {
                    set ret = f @{s1,s1} (f @{s1,b1} arg)
                }
            }
            var quad: func @GLOBAL->@{s1,b1}->$getNumB1->$getNumS1
            set quad = twicec square
            output std quad @{LOCAL,LOCAL} two
        """.trimIndent()
        )
        assert(out == "<.1 <.1 <.1 <.1 <.1 <.1 <.1 <.1 <.1 <.1 <.1 <.1 <.1 <.1 <.1 <.1 <.0>>>>>>>>>>>>>>>>>\n") { out }
    }
    @Disabled   // no more full closures
    @Test
    fun ch_01_04_curry_pg13() {
        val fadd = "func @{r1,a1,b1} -> [$getNumA1,$getNumB1] -> $getNumR1"
        val ret2 = "func @a1->@{r1,b1}->$getNumB1->$getNumR1"
        val ret1 = "func @GLOBAL -> @{a1} -> $getNumA1 -> $ret2"
        val out = all(
            """
            $X.nums
            $X.clone
            $add
            var curry: func @{} -> $fadd -> $ret1
            set curry = func @{} -> $fadd -> $ret1 {
                var f: $fadd
                set f = arg
                set ret = $ret1 [f] {
                    var x: $getNumA1
                    set x = arg
                    var ff: $fadd
                    set ff = f
                    set ret = $ret2 [ff,x] {
                        var y: $getNumB1
                        set y = arg
                        set ret = ff @{r1,a1,b1} [x,y]: @r1
                    }
                }
            }
            var addc: $ret1
            set addc = curry add
            output std (addc @{LOCAL} one) @{LOCAL,LOCAL} two
        """.trimIndent()
        )
        assert(out == "<.1 <.1 <.1 <.0>>>>\n") { out }
    }
    @Disabled   // no more full closures
    @Test
    fun ch_01_04_uncurry_pg13() {
        val fadd  = "func @{r1,a1,b1} -> [$getNumA1,$getNumB1] -> $getNumR1"
        val fadd2 = "func @GLOBAL -> @{r1,a1,b1} -> [$getNumA1,$getNumB1] -> $getNumR1"
        val ret2  = "func @a1->@{r1,b1}->$getNumB1->$getNumR1"
        val ret1  = "func @GLOBAL -> @{a1} -> $getNumA1 -> $ret2"
        val out = all(
            """
            $X.nums
            $X.clone
            $add

            var curry: func @{} -> $fadd -> $ret1
            set curry = func @{} -> $fadd -> $ret1 {
                var f: $fadd
                set f = arg
                set ret = $ret1 [f] {
                    var x: $getNumA1
                    set x = arg
                    var ff: $fadd
                    set ff = f
                    set ret = $ret2 [ff,x] {
                        var y: $getNumB1
                        set y = arg
                        set ret = ff @{r1,a1,b1} [x,y]: @r1
                    }
                }
            }

            var uncurry: func @{} -> $ret1 -> $fadd2
            set uncurry = func @{} -> $ret1 -> $fadd2 {
                var f: $ret1
                set f = arg
                set ret = $fadd2 [f] {
                    set ret = (f @{a1} arg.1) @{r1,b1} arg.2
                }
            }

            var addc: $ret1
            set addc = curry add

            var addu: $fadd2
            set addu = uncurry addc

            output std addu @{LOCAL,LOCAL,LOCAL} [one,two]
        """.trimIndent()
        )
        assert(out == "<.1 <.1 <.1 <.0>>>>\n") { out }
    }
    @Disabled   // no more full closures
    @Test
    fun ch_01_04_composition_pg15() {
        val T = "func @{r1,a1} -> $getNumA1 -> $getNumR1"
        val S = "func @GLOBAL -> @{r1,a1} -> $getNumA1 -> $getNumR1"
        val out = all(
            """
            $X.nums
            $X.clone
            $add

            var inc: $T
            set inc = func @{r1,a1}-> $getNumA1 -> $getNumR1 {
                set ret = add @{r1,GLOBAL,a1} [one,arg]: @r1
            }
            output std inc @{LOCAL,LOCAL} two

            var compose: func @{}->[$T,$T]->$S
            set compose = func @{}->[$T,$T]->$S {
                var f: $T
                set f = arg.1
                var g: $T
                set g = arg.2
                set ret = $S [f,g] {
                    var v: $getNumTL
                    set v = f @{LOCAL,a1} arg: @LOCAL
                    set ret = g @{r1,LOCAL} v: @r1
                }
            }
            output std (compose [inc,inc]) @{LOCAL,LOCAL} one
        """.trimIndent()
        )
        assert(out == "<.1 <.1 <.1 <.0>>>>\n<.1 <.1 <.1 <.0>>>>\n") { out }
    }
    @Disabled   // no more full closures
    @Test
    fun ch_01_04_currying_pg11() {
        val out = all(
            """
            $X.nums
            $X.lt
            var smallerc:  func @{r1}-> $getNumR1 -> (func @r1->@{}-> $getNumR1->$getNumR1)
            set smallerc = func @{r1}-> $getNumR1 -> (func @r1->@{}-> $getNumR1->$getNumR1) {
                var x: $getNumR1
                set x = arg
                set ret = func @r1->@{}-> $getNumR1->$getNumR1 [x] {
                    if (lt @{r1,r1} [x,arg]) {
                        set ret = x
                    } else {
                        set ret = arg
                    }
                }
            }
            var f: func @LOCAL->@{}-> $getNumTL -> $getNumTL
            set f = smallerc @{LOCAL} two: @LOCAL   -- smallerc could keep two in memory as long as smallerc does not live longer than two
            output std f one
            output std f three
        """.trimIndent()
        )
        assert(out == "<.1 <.0>>\n<.1 <.1 <.0>>>\n") { out }
    }
    @Disabled
    @Test
    fun noclo_a06_par2 () {
        val out = all("""
            type Event = <(),_uint64_t,_int>
            var build : func @{r1} -> () -> task @r1->@{}->()->()->()
            set build = func @{r1} -> () -> task @r1->@{}->()->()->() {
                set ret = task @r1->@{}->()->()->() {
                    output std _1:_int
                    await evt?3
                    output std _2:_int
                }
            }
            var f: task @{}->()->()->()
            set f = build @{LOCAL} ()
            var g: task @{}->()->()->()
            set g = build @{LOCAL} ()
            output std _10:_int
            var x : active task @{}->()->()->()
            set x = spawn f ()
            output std _11:_int
            var y : active task @{}->()->()->()
            set y = spawn g ()
            --awake x _1:_int
            --awake y _1:_int
            emit @GLOBAL <.3 _1:_int>:<(),_uint64_t,_int>:+ Event
            output std _12:_int
        """.trimIndent())
        assert(out == "10\n1\n11\n1\n2\n2\n12\n") { out }
    }
     */
}
