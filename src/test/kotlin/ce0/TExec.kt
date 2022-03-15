import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.MethodOrderer.Alphanumeric
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import java.io.File

@TestMethodOrder(Alphanumeric::class)
class TExec {

    fun all (inp: String): String {
        CE1 = false
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

    @Test
    fun a01_output () {
        val out = all("output std ()")
        assert(out == "()\n") { out }
    }
    @Test
    fun a01_output2 () {
        val out = all("output std _10:_int")
        assert(out == "10\n") { out }
    }
    @Test
    fun a01_output3 () {
        val out = all("""
            var x: _int
            set x = ((_abs: _) @[] (_(-1): _))
            output std x            
        """.trimIndent())
        assert(out == "1\n") { out }
    }
    @Test
    fun a02_var () {
        val out = all("""
            var x: ()
            set x = ()
            output std x
        """.trimIndent())
        assert(out == "()\n") { out }
    }
    @Test
    fun a03_error () {
        val out = all("//output std ()")
        assert(out == "(ln 1, col 1): expected statement : have `/´")
    }
    @Test
    fun a05_int () {
        val out = all("""
            var x: _int
            set x = _10: _int
            output std x
        """.trimIndent())
        assert(out == "10\n") { out }
    }
    @Test
    fun a06_int () {
        val out = all("""
            var x: _int
            output std _10:_int
        """.trimIndent())
        assert(out == "10\n") { out }
    }
    @Test
    fun a07_output_string () {
        val out = all("output std _(\"hello\"):_(char*)")
        assert(out == "\"hello\"\n") { out }
    }
    @Test
    fun a07_syntax_error () {
        val out = all("""
            native {
                putchar('A');
            }
        """.trimIndent())
        assert(out == "(ln 1, col 8): expected `_´ : have `{´")
    }
    @Test
    fun a08_int () {
        val out = all("""
            native _{
                putchar('A');
            }
        """.trimIndent())
        assert(out == "A") { out }
    }
    @Test
    fun a09_int_abs () {
        val out = all("""
            var x: _int
            set x = _abs:_ _(-1): _int
            output std x
        """.trimIndent())
        assert(out == "1\n") { out }
    }
    @Test
    fun a10_int_set () {
        val out = all("""
            var x: _int
            set x = _10: _int
            set x = _(-20): _int
            output std x
        """.trimIndent())
        assert(out == "-20\n") { out }
    }
    @Test
    fun a11_unk () {
        val out = all("""
            var x: _int
            set x = _(-20): _int
            output std x
        """.trimIndent())
        assert(out == "-20\n")
    }
    @Test
    fun a12_set () {
        val out = all("""
            var x: ()
            set x = ()
            set x = ()
            output std x
        """.trimIndent())
        assert(out == "()\n")
    }

    // TUPLES

    @Test
    fun b01_tuple_units () {
        val out = all("""
            var x : [(),()]
            set x = [(),()]
            var y: ()
            set y = x.1
            native _{ output_std_Unit(y); }
        """.trimIndent())
        assert(out == "()\n") { out }
    }
    @Test
    fun b02_tuple_idx () {
        val out = all("""
            output std ([(),()].1)
        """.trimIndent())
        //assert(out == "(ln 1, col 21): invalid discriminator : unexpected constructor") { out }
        assert(out == "()\n") { out }
    }
    @Test
    fun b03_tuple_tuples () {
        val out = all("""
            var v: [(),()]
            set v = [(),()]
            var x: [(),[(),()]]
            set x = [(),v]
            var y: [(),()]
            set y = x.2
            var z: ()
            set z = y.2
            output std z
        """.trimIndent())
        assert(out == "()\n") { out }
    }
    @Test
    fun b04_tuple_pp () {
        val out = all("""
            var n: _int ; set n = _1: _int
            var x: [[_int,_int],[_int,_int]] ; set x = [[n,n],[n,n]]
            output std /x
        """.trimIndent())
        assert(out == "[[1,1],[1,1]]\n") { out }
    }

    // NATIVE

    @Test
    fun c01_nat () {
        val out = all("""
            native _{
                void f (void) { putchar('a'); }
            }
            call _f:_ ()
        """.trimIndent())
        assert(out == "a") { out }
    }
    @Test
    fun c02_nat () {
        val out = all("""
            var y: _(char*); set y = _("hello"): _(char*)
            var n: _int
            set n = _10: _int
            var x: [_int,_(char*)]
            set x = [n,y]
            call _puts:_ x.2
        """.trimIndent())
        assert(out == "hello\n") { out }
    }
    @Test
    fun c03_nat () {
        val out = all("""
            var y: _(char*); set y = _("hello"): _(char*)
            var n: _int; set n = _10: _int
            var x: [_int,_(char*)]; set x = [n,y]
            output std /x
        """.trimIndent())
        assert(out == "[10,\"hello\"]\n") { out }
    }

    // FUNC / CALL

    @Test
    fun d01_f_int () {
        val out = all("""
            var f: (func @[]-> _int -> _int)
            set f = func@[]-> _int -> _int {
                set ret = arg
                return
            }
            var x: _int
            set x = f _10: _int
            output std x
        """.trimIndent())
        assert(out == "10\n") { out }
    }
    @Test
    fun d02_f_unit () {
        val out = all("""
            var f: (func @[]-> () -> ())
            set f = func@[]-> ()->() { return }
            var x: ()
            set x = f ()
            output std x
        """.trimIndent())
        assert(out == "()\n") { out }
    }
    @Test
    fun d03_global () {
        val out = all("""
            var xxx: _int
            set xxx = _10:_int
            var g: (func@[]-> () -> ())
            set g = func@[]-> ()->() { output std xxx }
            call g ()
        """.trimIndent())
        assert(out == "10\n") { out }
    }
    @Test
    fun d03_global2 () {
        val out = all("""
            var xxx: _int
            set xxx = _10:_int
            call (func @[]->()->() { output std xxx }) ()
        """.trimIndent())
        assert(out == "10\n") { out }
    }
    @Test
    fun d03_fg () {
        val out = all("""
            var f: (func@[]-> () -> ())
            set f = func@[]-> ()->() { var x: _int; set x = _10: _int ; output std x }
            var g: (func@[]-> () -> ())
            set g = func@[]-> ()->() { call f () ; return }
            call g ()
        """.trimIndent())
        assert(out == "10\n") { out }
    }
    @Test
    fun d04_arg () {
        val out = all("""
        var f : (func@[]-> _int -> _int)
        set f = func@[]-> _int->_int {
           set arg = _(${D}arg+1): _int
           set ret = arg
           return
        }
        output std f _1: _int
        """.trimIndent())
        assert(out == "2\n") { out }
    }
    @Test
    fun d04_arg_err1 () {
        val out = all("""
            output std _(${D}{arg): _int
        """.trimIndent())
        assert(out == "(ln 1, col 12): invalid `\$´") { out }
    }
    @Test
    fun d04_arg_err2 () {
        val out = all("""
            output std _(${D}): _int
        """.trimIndent())
        assert(out == "(ln 1, col 12): invalid `\$´") { out }
    }
    @Test
    fun d04_arg_glb () {
        val out = all("""
        var x: _int
        set x = _10:_int
        var f : (func@[]-> () -> _int)
        set f = func@[]-> ()->_int {
           set arg = _(${D}x+1): _int
           set ret = arg
           return
        }
        output std f ()
        """.trimIndent())
        assert(out == "11\n") { out }
    }
    @Test
    fun d04_arg_glb2 () {
        val out = all("""
        var f : func @[] -> () -> ()
        set f = func @[] -> () -> () {
            var xxx: _int
            set xxx = _10:_int
            var g : func @[] -> () -> ()
            set g = func @[] -> () -> () {
               output std _(${D}xxx): _int
            }
            call g ()
        }
        call f ()
        """.trimIndent())
        assert(out == "10\n") { out }
    }
    @Test
    fun d04_tup () {
        val out = all("""
            var win: _int
            var rct: [_int]
            set rct = [(_($D{win.1}): _int)]

        """.trimIndent())
        assert(out == "(ln 3, col 13): invalid variable \"win.1\"") { out }
    }
    @Test
    fun d05_func_var () {
        val out = all("""
        var f: (func@[]-> _int->_int)
        set f = func@[]-> _int->_int { set ret=arg ; return }
        var p: (func@[]-> _int->_int)
        set p = f
        output std p _10: _int
        """.trimIndent())
        assert(out == "10\n") { out }
    }
    @Test
    fun d06_func_fg () {
        val out = all("""
            var f: (func@[]-> _int->_int)
            set f = func@[]-> _int->_int { set ret=arg ; return }
            var g: (func@[]-> [func@[]-> _int->_int, _int] -> _int)
            set g = func@[]-> [func@[]-> _int->_int, _int] -> _int {
               var f: (func@[]-> _int->_int)
               set f = arg.1
               var v: _int
               set v = f arg.2
               set ret = v
               return
            }
            output std g [f,_10: _int]
        """.trimIndent())
        assert(out == "(ln 5, col 8): invalid declaration : \"f\" is already declared (ln 1)") { out }
    }
    @Test
    fun d07_func_fg () {
        val out = all("""
            var f:(func@[]->  _int->_int)
            set f = func@[]-> _int->_int { set ret=arg return }
            var g:  (func@[i1]-> [(func@[]-> _int->_int), _int] -> _int)
            set g = func@[i1]-> [(func@[]-> _int->_int), _int]-> _int {
               var fx: (func@[]-> _int->_int)
               set fx = arg.1
               var v: _int
               set v = fx arg.2
               set ret = v
               return
            }
            --var n: _int = _10: _int
            output std g @[LOCAL] [f,_10:_int]
        """.trimIndent())
        assert(out == "10\n") { out }
    }
    @Test
    fun d08_func_unit () {
        val out = all("""
            var f: (func@[]->  ()->() )
            set f = func@[]-> ()->() { var x:() ; set x = arg ; set ret=arg ; return }
            var x:() ; set x = f ()
            output std x
        """.trimIndent())
        assert(out == "()\n") { out }
    }
    @Test
    fun d09_func_unit () {
        val out = all("""
            var f: (func@[]-> ()->())
            set f = func@[]-> ()->() { var x:() ; set x = arg ; set ret=arg ; return }
            output std f ()
        """.trimIndent())
        assert(out == "()\n") { out }
    }

    // INPUT / OUTPUT

    @Test
    fun e01_out () {
        val out = all("""
            output () ()
        """.trimIndent())
        //assert(out == "(ln 1, col 8): invalid `output` : expected identifier") { out }
        assert(out == "(ln 1, col 8): expected variable identifier : have `()´") { out }
    }
    @Test
    fun e02_out () {
        val out = all("""
            var x: [(),()]; set x = [(),()]
            output std /x
        """.trimIndent())
        assert(out == "[(),()]\n")
    }
    @Test
    fun e03_out () {
        val out = all("""
            native _{
                void output_f (int x) {
                    output_std_int(x);
                }
            }
            output f _10: _int
        """.trimIndent())
        assert(out == "10\n") { out }
    }
    @Disabled   // needs user input
    @Test
    fun e04_inp () {
        val out = all("""
            var x: _int
            set x = input std (): _int
            output std x
        """.trimIndent())
        assert(out == "10\n") { out }
    }
    @Disabled   // needs user input
    @Test
    fun e05_inp () {
        val out = all("""
            var x: _int
            var y: </_int@LOCAL>
            set x = input std y: _int
            output std x
        """.trimIndent())
        assert(out == "10\n") { out }
    }
    @Test
    fun e06_inp () {
        val out = all("""
            type Int = _int
            var x: Int
            set x = input std (): Int
            output std x
        """.trimIndent())
        assert(out.contains("implicit declaration of function ‘input_std_Int’")) { out }
    }
    @Disabled   // needs user input
    @Test
    fun e07_inp () {
        val out = all("""
            type Int = <_int,()>
            var x: Int
            set x = input std <.1 _10:_int>: Int: Int
            output std x
        """.trimIndent())
        assert(out.contains("XXX")) { out }
    }

    // USER

    @Test
    fun f01_bool () {
        val out = all("""
            var b : <(),()>
            set b = <.1()>:<(),()>
            output std /b
        """.trimIndent())
        assert(out == "<.1>\n") { out }
    }
    @Test
    fun f02_xyz () {
        val out = all("""
            var z : <()>
            set z = <.1()>:<()>
            var y : <<()>>
            set y = <.1 z>:<<()>>
            var x : <<<()>>>
            set x = <.1 y>:<<<()>>>
            var yy: <<()>>
            set yy = x!1
            var zz: <()>
            set zz = yy!1
            output std /zz
        """.trimIndent())
        assert(out == "<.1>\n") { out }
    }
    @Test
    fun f05_user_big () {
        val out = all("""
            var s: <[_int,_int,_int,_int],_int,_int>
            set s = <.1 [_1:_int,_2:_int,_3:_int,_4:_int]>:<[_int,_int,_int,_int],_int,_int>
            output std /s
        """.trimIndent())
        assert(out == "<.1 [1,2,3,4]>\n") { out }
    }
    @Test
    fun f06_user_big () {
        val out = all("""
            var x: <[<()>,<()>]>
            set x = <.1 [<.1()>:<()>,<.1()>:<()>]>:<[<()>,<()>]>
            output std /x
        """.trimIndent())
        assert(out == "<.1 [<.1>,<.1>]>\n") { out }
    }
    @Test
    fun f07_user_pred () {
        val out = all("""
            var z: <()>
            set z = <.1()>: <()>
            output std z?1
        """.trimIndent())
        assert(out == "1\n") { out }
    }
    @Test
    fun f10_user_disc () {
        val out = all("""
            var z: <(),()>
            set z = <.2 ()>:<(),()>
            output std z!2
        """.trimIndent())
        assert(out == "()\n") { out }
    }
    @Test
    fun f11_user_disc_err () {
        val out = all("""
            var z: <(),()>
            set z = <.2()>: <(),()>
            output std z!1
        """.trimIndent())
        assert(out.endsWith("main: Assertion `(global.z).tag == 1' failed.\n")) { out }
    }
    @Test
    fun f12_user_disc_pred_idx () {
        val out = all("""
            var v: <[<()>,()]>
            set v = <.1 [<.1()>:<()>,()]>: <[<()>,()]>
            output std v!1.1?1
        """.trimIndent())
        assert(out == "1\n") { out }
    }
    @Test
    fun f13_user_disc_pred_err () {
        val out = all("""
            output std ()?1
        """.trimIndent())
        assert(out == "(ln 1, col 15): invalid predicate : not an union") { out }
    }
    @Test
    fun f14_user_dots_err () {
        val out = all("""
            var x: <<<()>>>; set x = <.1 <.1 <.1()>:<()>>:<<()>>>:<<<()>>>
            output std x!1!2
        """.trimIndent())
        assert(out == "(ln 2, col 16): invalid discriminator : out of bounds")
    }
    @Test
    fun f15_user_dots () {
        val out = all("""
            var x: <<<()>>>
            set x = <.1 <.1 <.1()>:<()>>:<<()>>>:<<<()>>>
            output std x!1!1!1
        """.trimIndent())
        assert(out == "()\n") { out }
    }

    // IF

    @Test
    fun g01_if () {
        val out = all("""
            var x: <(),()>
            set x = <.2()>: <(),()>
            if x?1 { } else { output std () }
        """.trimIndent())
        assert(out == "()\n") { out }
    }
    @Test
    fun g02_if_pred () {
        val out = all("""
            var x: <(),()>
            set x = <.2()>: <(),()>
            if x?2 { output std () } else { }
        """.trimIndent())
        assert(out == "()\n") { out }
    }

    // LOOP

    @Test
    fun h01_loop () {
        val out = all("""
        loop {
           break
        }
        output std ()
        """.trimIndent())
        assert(out == "()\n")
    }

    // PTR

    @Test
    fun i01_ptr () {
        val out = all("""
            var y: _int
            set y = _10: _int
            var x: /_int@LOCAL
            set x = /y
            output std x\
        """.trimIndent())
        assert(out == "10\n") { out }
    }
    @Test
    fun i02_ptr_func () {
        val out = all("""
        var f : func@[i1]-> /_int@i1 -> ()
        set f = func@[i1]-> /_int@i1 -> () {
           set arg\ = _(*${D}arg+1): _int
           return
        }
        var x: _int
        set x = _1: _int
        call f @[LOCAL] /x
        output std x
        """.trimIndent())
        assert(out == "2\n") { out }
    }
    @Test
    fun i03_ptr_func () {
        val out = all("""
            var f: func@[i1]-> /_int@i1->_int
            set f = func @[i1]->/_int@i1->_int { set ret = arg\ }
            var g: func@[i1]-> /_int@i1->_int
            set g = f
            var x: _int
            set x = _10: _int
            output std g @[LOCAL](/x)
        """.trimIndent())
        assert(out == "10\n") { out }
    }
    @Test
    fun i05_ptr_block_err () {
        val out = all("""
            var p1: /_int @LOCAL
            var p2: /_int @LOCAL
            {
                var v: _int
                set v = _10: _int
                set p1 = /v  -- no
            }
            {
                var v: _int; set v = _20: _int
                set p2 = /v
            }
            output std p1\
        """.trimIndent())
        assert(out.startsWith("(ln 6, col 12): invalid assignment : type mismatch")) { out }
    }
    @Test
    fun i06_ptr_block_err () {
        val out = all("""
            var x: _int; set x = _10: _int
            var p: /_int@LOCAL
            {
                var y: _int; set y = _10: _int
                set p = /x
                set p = /y  -- no
            }
        """.trimIndent())
        assert(out.startsWith("(ln 6, col 11): invalid assignment : type mismatch")) { out }
    }
    @Test
    fun i07_ptr_func_ok () {
        val out = all("""
            var f : func@[i1]-> /_int@i1 -> /_int@i1
            set f = func@[i1]-> /_int@i1 -> /_int@i1 {
                set ret = arg
            }
            var v: _int
            set v = _10: _int
            var p: /_int@LOCAL
            set p = f@[LOCAL] /v
            output std p\
        """.trimIndent())
        assert(out == "10\n") { out }
    }
    @Test
    fun i08_ptr_func_ok () {
        val out = all("""
            var v: _int
            set v = _10: _int
            var f : func@[i1]-> () -> /_int@i1
            set f = func@[i1]-> () -> /_int@i1 {
                set ret = /v
            }
            var p: /_int @LOCAL
            set p = f @[LOCAL] ()
            output std p\
        """.trimIndent())
        assert(out == "10\n") { out }
    }
    @Test
    fun i09_ptr_func_err () {
        val out = all("""
            var f : func@[i1]-> () -> /_int@i1
            set f = func@[i1]-> () -> /_int@i1 {
                var v: _int; set v = _10: _int
                set ret = /v   -- err
            }
            output std ()
        """.trimIndent())
        assert(out.startsWith("(ln 4, col 13): invalid return : type mismatch")) { out }
    }
    @Test
    fun i10_ptr_func_err () {
        val out = all("""
            var f : func@[i1]-> /_int@i1 -> /_int@i1
            set f = func@[i1]-> /_int@i1 -> /_int@i1 {
                var ptr: /_int@LOCAL
                set ptr = arg
                set ret = ptr  -- err
            }
            output std ()
        """.trimIndent())
        assert(out.startsWith("(ln 5, col 13): invalid return : type mismatch")) { out }
    }
    @Test
    fun i11_ptr_func_ok () {
        val out = all("""
            var f : func@[i1]-> /_int@i1 -> /_int@i1
            set f = func@[i1]-> /_int@i1 -> /_int@i1 {
                var ptr: /_int@i1
                set ptr = arg
                set ret = ptr
            }
            var v: _int
            set v = _10: _int
            var p: /_int@LOCAL
            set p = f @[LOCAL] /v
            output std p\
        """.trimIndent())
        assert(out == "10\n") { out }
    }
    @Test
    fun i12_ptr_ptr_ok () {
        val out = all("""
            var p: //_int @LOCAL @LOCAL
            var z: _int; set z = _10: _int
            var y: /_int@LOCAL; set y = /z
            set p = /y
            output std p\\
        """.trimIndent())
        assert(out == "10\n") { out }
    }
    @Test
    fun i13_ptr_tup () {
        val out = all("""
            var v: [_int,_int]
            set v = [_10:_int,_20:_int]
            var p: /_int @LOCAL
            set p = /v.1
            set p\ = _20: _int
            output std /v
        """.trimIndent())
        assert(out == "[20,20]\n") { out }
    }
    @Test
    fun i14_ptr_type_err () {
        val out = all("""
            var v: <_int>
            set v = <.1 _10: _int>: <_int>
            var p: /_int @LOCAL
            set p = /v!1
            output std ()
        """.trimIndent())
        //assert(out == "(ln 2, col 16): invalid expression : expected `borrow` operation modifier")
        assert(out == "(ln 4, col 12): unexpected operand to `/´") { out }
    }
    @Test
    fun i14_ptr_type () {
        val out = all("""
            var v: [_int]; set v = [_10:_int]
            var p: /_int@LOCAL; set p = /v.1
            set p\ = _20: _int
            output std /v
        """.trimIndent())
        assert(out == "[20]\n") { out }
    }
    @Test
    fun i15_ptr_tup () {
        val out = all("""
            var x: _int; set x = _10: _int
            var p: [_int,/_int @LOCAL]; set p = [_10:_int,/x]
            var v: _int; set v = _20: _int
            set p.2 = /v
            output std p.2\
        """.trimIndent())
        assert(out == "20\n") { out }
    }
    @Test
    fun i16_ptr_tup () {
        val out = all("""
            var x: _int; set x = _10: _int
            var p: [_int,/_int @LOCAL]; set p = [_10:_int,/x]
            var v: _int; set v = _20: _int
            set p = [_10:_int,/v]
            output std p.2\
        """.trimIndent())
        assert(out == "20\n") { out }
    }
    @Test
    fun i17_ptr_type () {
        val out = all("""
            var x: _int
            set x = _10: _int
            var p: </_int@LOCAL>
            set p = <.1 /x>: </_int @LOCAL>
            var v: _int
            set v = _20: _int
            set p!1 = /v
            output std p!1\
        """.trimIndent())
        assert(out == "20\n") { out }
    }
    @Test
    fun i18_ptr_type () {
        val out = all("""
            var x: _int
            set x = _10: _int
            var p: </_int @LOCAL>
            set p = <.1 /x>: </_int@LOCAL>
            var v: _int
            set v = _20: _int
            set p = <.1 /v>: </_int @LOCAL>
            output std p!1\
        """.trimIndent())
        assert(out == "20\n") { out }
    }
    @Test
    fun i19_ptr_tup () {
        val out = all("""
            var x1: [_int,/_int @LOCAL]
            var v: _int; set v = _20: _int
            var x2: [_int,/_int@LOCAL]; set x2 = [_10:_int,/v]
            set x1 = x2
            output std x1.2\
        """.trimIndent())
        assert(out == "20\n") { out }
    }
    @Test
    fun f09_ptr_type_err () {
        val out = all("""
            var x1: </_int @LOCAL>
            var v: _int
            set v = _20: _int
            var x2: </_int @LOCAL>
            set x2 = <.1 /v>:</_int@LOCAL>
            set x1 = x2
            output std x1!1\
        """.trimIndent())
        assert(out == "20\n") { out }
    }
    @Test
    fun f10_ptr_func () {
        val out = all("""
            var v: _int
            set v = _10: _int
            var f : func@[i1]-> /_int@i1 -> /_int@i1
            set f = func@[i1]-> /_int@i1 -> /_int@i1 {            
                set ret = /v
                return
            }
            --{
                var p: /_int @LOCAL
                set p = f @[GLOBAL] /v: @GLOBAL
                output std p\
            --}
        """.trimIndent())
        assert(out == "10\n") { out }
    }
    @Test
    fun f11_ptr_func () {
        val out = all("""
            var f: (func@[i1]-> /_int@i1 -> /_int@i1)
            set f = func@[i1]-> /_int@i1 -> /_int@i1 {
                set ret = arg
            }
            var v: _int
            set v = _10: _int
            var p: /_int @LOCAL
            set p = f @[LOCAL] /v: @LOCAL
            output std p\
        """.trimIndent())
        assert(out == "10\n") { out }
    }
    @Test
    fun i20_ptr_uni_ok () {
        val out = all("""
            var uni: <_(char*),_int>
                set uni = <.1 _("oi"): _(char*)>: <_(char*),_int>
            var ptr: /_(char*)@LOCAL
                set ptr = /uni!1
            call _puts ptr\
        """.trimIndent())
        assert(out == "(ln 4, col 20): unexpected operand to `/´") { out }
    }
    @Test
    fun i21_ptr_uni_err () {
        val out = all("""
            var uni: <_(char*),_int>
                set uni = <.1 _("oi"): _(char*)>: <_(char*),_int>
            var ptr: /_(char*) @LOCAL
                set ptr = /uni!1
            set uni = <.2 _65:_int>
            call _puts ptr\
        """.trimIndent())
        //assert(out == "(ln 5, col 9): invalid assignment of \"uni\" : borrowed in line 4")
        assert(out == "(ln 4, col 20): unexpected operand to `/´") { out }
    }

    // REC

    @Test
    fun j01_list () {
        val out = all("""
            type List = </List @LOCAL>
            var l: /List @LOCAL
            set l = Null: /List@LOCAL
            output std l
        """.trimIndent())
        //assert(out == "(ln 1, col 5): invalid assignment : type mismatch") { out }
        assert(out == "Null\n") { out }
    }
    @Test
    fun j02_list_new_err_dst () {
        val out = all("""
            type List = </List @LOCAL>
            var l: /List @LOCAL
            set l = new List <.1 Null: /List@LOCAL>:</List @LOCAL>: @LOCAL
            output std l
        """.trimIndent())
        //assert(out == "(ln 1, col 21): invalid assignment : type mismatch") { out }
        assert(out == "<.1 Null>\n") { out }
    }
    @Test
    fun j02_list_new_err_dst2 () {
        val out = all("""
            type List = </List @GLOBAL>
            var l: /List @GLOBAL
            set l = new List <.1 Null: /List @GLOBAL>:</List @GLOBAL>: @GLOBAL
            output std l
        """.trimIndent())
        //assert(out == "(ln 1, col 21): invalid assignment : type mismatch") { out }
        assert(out == "<.1 Null>\n") { out }
    }
    @Test
    fun j02_list_new_err_src () {
        val out = all("""
            type List = </List @LOCAL>
            var l: /List @LOCAL
            set l = new _1: _int @LOCAL
            output std l
        """.trimIndent())
        assert(out == "(ln 3, col 9): invalid `new` : expected constructor") { out }
        //assert(out == "(ln 3, col 9): invalid `new` : expected alias constructor") { out }
    }
    @Test
    fun j02_list_new_ok () {
        val out = all("""
            type List = </List @LOCAL>
            var l: /List @LOCAL
            set l = new List <.1 Null: /List@LOCAL>:</List @LOCAL>: @LOCAL
            output std l
        """.trimIndent())
        assert(out == "<.1 Null>\n") { out }
    }
    @Test
    fun j02_list_pln () {
        val out = all("""
            type List = </List @LOCAL>
            var l: List
            set l = List <.1 Null:/List@LOCAL>:</List @LOCAL>
            output std /l
        """.trimIndent())
        //assert(out == "(ln 1, col 25): invalid union constructor : expected `new`") { out }
        assert(out == "<.1 Null>\n") { out }
    }
    @Test
    fun j03_list () {
        val out = all("""
            type List = </List @LOCAL>
            var l: /List @LOCAL
            var z: /List @LOCAL
            set z = Null: /List @LOCAL
            var one: /List @LOCAL
            set one = new List<.1 z>:</List @LOCAL>: @LOCAL
            set l = new List<.1 one>:</List @LOCAL>: @LOCAL
            output std l\~!1
        """.trimIndent())
        assert(out == "<.1 Null>\n") { out }
    }
    @Test
    fun j05_list_disc_null_err () {
        val out = all("""
            type List = </List @LOCAL>
            var l: /List @LOCAL
            set l = Null: /List @LOCAL
            output std l\ ~ !1
        """.trimIndent())
        assert(out.endsWith("(global.l)) != NULL' failed.\n")) { out }
    }
    @Test
    fun j06_list_1 () {
        val out = all("""
            type List = </List @LOCAL>
            var l: /List @LOCAL
            var z: /List @LOCAL
            set z = Null: /List @LOCAL
            var one: /List @LOCAL
            set one = new List <.1 z>:</List @LOCAL>: @LOCAL
            set l = new List <.1 one>:</List @LOCAL>: @LOCAL
            var p: //List @LOCAL @LOCAL
            {
                set p = /l --!1
            }
            output std p\
        """.trimIndent())
        assert(out == "<.1 <.1 Null>>\n") { out }
    }
    @Test
    fun j06_list_2 () {
        val out = all("""
            type List = </List @LOCAL>
            var l: /List @LOCAL
            var z: /List @LOCAL
            set z = Null: /List @LOCAL
            var one: /List @LOCAL
            set one = new List <.1 z>:</List @LOCAL>: @LOCAL
            set l = new List <.1 one>:</List @LOCAL>: @LOCAL
            var p: /List @LOCAL
            {
                set p = l --!1
            }
            output std p
        """.trimIndent())
        assert(out == "<.1 <.1 Null>>\n") { out }
    }
    @Test
    fun j06_list_ptr () {
        val out = all("""
            type List = </List @LOCAL>
            var l: /List @LOCAL
            var z: /List @LOCAL
            set z = Null: /List @LOCAL
            var one: /List @LOCAL
            set one = new List<.1 z>:</List @LOCAL>: @LOCAL
            set l = new List<.1 one>:</List @LOCAL>: @LOCAL
            var p: //List @LOCAL @LOCAL
            {
                set p = /l --!1
            }
            output std p
        """.trimIndent())
        assert(out == "_\n") { out }
    }
    @Test
    fun j07_list_move () {
        val out = all("""
            type List = </List @LOCAL>
            var l1: /List @LOCAL
            set l1 = new List <.1 Null: /List @LOCAL>:</List @LOCAL>: @LOCAL
            var l2: /List @LOCAL
            set l2 = l1
            output std /l1
            output std /l2
        """.trimIndent())
        //assert(out == "(ln 3, col 13): invalid access to \"l1\" : consumed in line 2") { out }
        assert(out == "_\n_\n") { out }
    }
    @Test
    fun j07_list_move_err () {
        val out = all("""
            var l1: /<?/^>
            set l1 = new <.1 Null>: @LOCAL
            var l2: /</^>
            set l2 = l1
        """.trimIndent())
        //assert(out == "(ln 4, col 8): invalid assignment : type mismatch") { out }
        assert(out == "(ln 1, col 11): expected type : have `?´") { out }
    }
    @Test
    fun j08_list_move () {
        val out = all("""
            type List = </List @LOCAL>
            var l1: /List @LOCAL
            set l1 = new List <.1 Null: /List @LOCAL>:</List @LOCAL>: @LOCAL
            var l2: /List @LOCAL
            set l2 = Null: /List @LOCAL
            set l2 = new List <.1 l1>:</List @LOCAL>: @LOCAL
            output std l2
        """.trimIndent())
        assert(out == "<.1 <.1 Null>>\n") { out }
    }
    @Test
    fun j09_list_move () {
        val out = all("""
            type List = </List @LOCAL>
            var l1: /List @LOCAL
            set l1 = new List <.1 Null: /List @LOCAL>:</List @LOCAL>: @LOCAL
            var l2: [_int,/List @LOCAL]
            set l2 = [_10:_int, l1]
            output std l1
            output std l2.2
        """.trimIndent())
        //assert(out == "Null\n<.1 Null>\n") { out }
        assert(out == "<.1 Null>\n<.1 Null>\n") { out }
    }
    @Test
    fun j10_rec () {
        val out = all("""
            type List = </List @LOCAL>
            var z: /List @LOCAL
            set z = Null: /List @LOCAL
            var one: /List @LOCAL
            set one = new List <.1 z>:</List @LOCAL>: @LOCAL

            var n: </List @LOCAL>
            set n = <.1 one>:</List @LOCAL>
            output std /n
        """.trimIndent())
        assert(out == "<.1 <.1 Null>>\n") { out }
    }
    @Test
    fun j11_borrow_1 () {
        val out = all("""
            type List = </List @LOCAL>
            var z: /List @LOCAL
            set z = Null: /List @LOCAL
            var one: /List @LOCAL
            set one = new List <.1 z>:</List @LOCAL>: @LOCAL

            var x: /List @LOCAL
            set x = new List <.1 one>:</List @LOCAL>: @LOCAL
            var y: //List @LOCAL @LOCAL
            set y = /x
            output std y\
        """.trimIndent())
        assert(out == "<.1 <.1 Null>>\n") { out }
    }
    @Test
    fun j11_borrow_2 () {
        val out = all("""
            type List = </List @LOCAL>
            var z: /List @LOCAL
            set z = Null: /List @LOCAL
            var one: /List @LOCAL
            set one = new List <.1 z>:</List @LOCAL>: @LOCAL

            var x: /List @LOCAL
            set x = new List <.1 one>:</List @LOCAL>: @LOCAL
            var y: //List @LOCAL @LOCAL
            set y = /x --!1
            output std y\
        """.trimIndent())
        assert(out == "<.1 <.1 Null>>\n") { out }
    }
    @Test
    fun j09_tup_list_err () {
        val out = all("""
            type List = </List @LOCAL>
            var t: [_int,/List @LOCAL]
            set t = [_10:_int, new List <.1 Null:/List @LOCAL>:</List @LOCAL>: @LOCAL]
            output std /t
        """.trimIndent())
        assert(out == "[10,<.1 Null>]\n") { out }
    }
    @Test
    fun j10_tup_copy_ok () {
        val out = all("""
            type List = </List @LOCAL>
            var l: /List @LOCAL
            set l = Null: /List @LOCAL
            var t1: [/List @LOCAL]
            set t1 = [l]
            var t2: [/List @LOCAL]
            set t2 = [t1.1]
            output std /t2
        """.trimIndent())
        assert(out == "[Null]\n") { out }
    }
    @Test
    fun j11_tup_move_ok () {
        val out = all("""
            type List = </List @LOCAL>
            var l: /List @LOCAL
            set l = Null: /List @LOCAL
            var t1: [/List @LOCAL]
            set t1 = [l]
            var t2: [/List @LOCAL]
            set t2 = [t1.1]
            output std /t2
        """.trimIndent())
        assert(out == "[Null]\n") { out }
    }
    @Test
    fun j11_tup_copy_ok () {
        val out = all("""
            type List = </List @LOCAL>
            var l: /List @LOCAL
            set l = new List <.1 Null: /List @LOCAL>:</List @LOCAL>: @LOCAL
            var t1: [/List @LOCAL]
            set t1 = [l]
            var t2: [/List @LOCAL]
            set t2 = [t1.1]
            output std /t2
        """.trimIndent())
        assert(out == "[<.1 Null>]\n") { out }
    }
    @Test
    fun j14_tup_copy_ok () {
        val out = all("""
            type List = </List @LOCAL>
            var l1: /List @LOCAL
            set l1 = Null: /List @LOCAL
            var l2: /List @LOCAL
            set l2 = l1
            output std l2
        """.trimIndent())
        assert(out == "Null\n") { out }
    }
    @Test
    fun j15_tup_copy_ok () {
        val out = all("""
            type List = </List @LOCAL>
            var l1: /List @LOCAL
            set l1 = new List <.1 Null: /List @LOCAL>:</List @LOCAL>: @LOCAL
            var l2: /List @LOCAL
            set l2 = l1
            output std l2
        """.trimIndent())
        assert(out == "<.1 Null>\n") { out }
    }
    @Test
    fun j16_tup_move_copy_ok () {
        val out = all("""
            type List = </List @LOCAL>
            var l1: /List @LOCAL
            set l1 = new List <.1 Null: /List @LOCAL>:</List @LOCAL>: @LOCAL
            var l2: /List @LOCAL
            set l2 = new List <.1 l1>:</List @LOCAL>: @LOCAL
            var t3: [(),/List @LOCAL]
            set t3 = [(), new List <.1 l2\ ~ !1>:</List @LOCAL>: @LOCAL]
            output std l1
            output std /t3
        """.trimIndent())
        assert(out == "<.1 Null>\n[(),<.1 <.1 Null>>]\n") { out }
    }
    @Test
    fun j18_tup_copy_rec_ok () {
        val out = all("""
            type List = </List @LOCAL>
            var l1: [/List @LOCAL]
            set l1 = [new List <.1 Null:/List @LOCAL>:</List @LOCAL>: @LOCAL]
            var l2: [/List @LOCAL]
            set l2 = l1
            output std /l2
        """.trimIndent())
        assert(out == "[<.1 Null>]\n") { out }
    }
    @Test
    fun j19_consume_ok () {
        val out = all("""
            type List = </List @LOCAL>
            var x: /List @LOCAL
            set x = new List <.1 Null: /List @LOCAL>:</List @LOCAL>: @LOCAL
            set x = x
            output std x
        """.trimIndent())
        assert(out == "<.1 Null>\n") { out }
    }
    @Test
    fun j20_consume_ok () {
        val out = all("""
            type List = </List @LOCAL>
            var x: /List @LOCAL
            set x = new List <.1 Null: /List @LOCAL>:</List @LOCAL>: @LOCAL
            var y: /List @LOCAL
            if _1: _int {
                set y = x
            } else {
                set y = x
            }
            output std y
        """.trimIndent())
        assert(out == "<.1 Null>\n") { out }
    }

    // SET - TUPLE - UNION

    @Test
    fun k01_set_tuple () {
        val out = all("""
            var xy: [_int,_int]; set xy = [_10:_int,_10:_int]
            set xy.1 = _20: _int
            var x: _int; set x = xy.1
            var y: _int; set y = xy.2
            var v: _int; set v = _(global.x+global.y): _int
            output std v
        """.trimIndent())
        assert(out == "30\n") { out }
    }

    // UNION SELF POINTER / HOLD

    @Test
    fun l04_ptr_null () {
        val out = all("""
            var n: _int
            set n = _10: _int
            var x: <(),/_int @LOCAL>
            set x = <.2 /n>: <(),/_int @LOCAL>
            output std x!2\
        """.trimIndent())
        assert(out == "10\n") { out }
    }

    // ALLOC / SCOPE / NEWS

    @Test
    fun noclo_m01_scope_a () {
        val out = all("""
            type List @[a] = </List @[a] @a>
            { @A
                var pa: /List @[LOCAL] @LOCAL
                set pa = new List @[A] <.1 Null: /List @[A] @A>:</List @[A]>: @A
                var f: func ()->()
                set f = func @[]-> ()->() {
                    var pf: /List @[A] @A
                    set pf = new List  @[A] <.1 Null: /List @[A] @A>:</List @[A] @A>: @A
                    set pa\ ~ !1 = pf
                    --output std pa
                }
                call f ()
                output std pa
            }
        """.trimIndent())
        assert(out == "<.1 <.1 Null>>\n") { out }
    }
    @Test
    fun m01_scope_f () {
        val out = all("""
            type List @[a] = </List @[a] @a>
            var f: func@[i1]-> /(List @[i1])@i1->()
            set f = func@[i1]-> /(List @[i1])@i1->() {
                var pf: /(List @[i1]) @i1
                set pf = arg
                output std pf
            }
            {
                var x: /(List @[LOCAL]) @LOCAL
                set x = new List @[LOCAL]<.1 Null: /(List @[LOCAL]) @LOCAL>:</List @[LOCAL] @LOCAL>: @LOCAL
                call f @[LOCAL] x
            }
        """.trimIndent())
        assert(out == "<.1 Null>\n") { out }
    }
    @Test
    fun m02_scope_f () {
        val out = all("""
            type List @[a] = </List @[a] @a>
            var f: func @[i1]->/(List @[i1])@i1->()
            set f = func@[i1]-> /(List @[i1])@i1->() {
                set arg\ ~  !1 = new List @[i1] <.1 Null:/(List @[i1])@i1>:</List @[i1] @i1>: @i1
            }
            {
                var x: /(List @[LOCAL]) @LOCAL
                set x = new List @[LOCAL] <.1 Null: /(List @[LOCAL]) @LOCAL>:</List @[LOCAL] @LOCAL>: @LOCAL
                call f @[LOCAL] x
                output std x
            }
        """.trimIndent())
        assert(out == "<.1 <.1 Null>>\n") { out }
    }
    @Test
    fun m03_scope_f () {
        val out = all("""
            type List @[a] = </List @[a] @a>
            var f: func @[a1,a2] -> [/(List @[a1])@a1,/(List @[a2])@a2]->()
            set f = func @[a1,a2]->[/(List @[a1])@a1,/(List @[a2])@a2]->() {
                set arg.1\ ~ !1 = new List @[a1] <.1 Null:/(List @[a1])@a1>:</List @[a1] @a1>: @a1
                set arg.2\ ~ !1 = new List @[a2]<.1 Null:/(List @[a2])@a2>:</List @[a2] @a2>: @a2
            }
            {
                var x: /(List @[LOCAL]) @LOCAL
                set x = new List @[LOCAL] <.1 Null: /(List @[LOCAL]) @LOCAL>:</List @[LOCAL] @LOCAL>: @LOCAL
                call f @[LOCAL,LOCAL] [x,x]
                output std x
            }
        """.trimIndent())
        assert(out == "<.1 <.1 Null>>\n") { out }
    }
    @Test
    fun m04_scope_f () {
        val out = all("""
            type List @[a] = </List @[a] @a>
            var f: func @[i1]->/(List @[i1])@i1->()
            set f = func @[i1]->/(List @[i1])@i1->() {
                set arg\ ~ !1 = new List @[i1] <.1 Null:/(List @[i1])@i1>:</List @[i1] @i1>: @i1
            }
            {
                var x: /(List @[LOCAL]) @LOCAL
                set x = new List @[LOCAL] <.1 Null: /(List @[LOCAL]) @LOCAL>:</List @[LOCAL] @LOCAL>: @LOCAL
                call f @[LOCAL] x
                output std x
            }
        """.trimIndent())
        assert(out == "<.1 <.1 Null>>\n") { out }
    }

    // FUNC / POOL

    @Test
    fun n01_pool () {
        val out = all("""
            var f : func @[a1] -> /()@a1 -> /()@a1
            output std ()
        """.trimIndent()
        )
        assert(out == "()\n") { out }
    }
    @Test
    fun n02_pool () {
        val out = all("""
            var f : func @[a1]->/()@a1 -> /()@a1
            set f = func @[a1]->/()@a1 -> /()@a1 {
                set ret = arg
            }
            output std ()
        """.trimIndent()
        )
        assert(out == "()\n") { out }
    }
    @Test
    fun n03_pool () {
        val out = all("""
            var f :func@[i1]-> /_int@i1 -> /()@i1
            set f = func@[i1]-> /_int@i1 -> /()@i1 {
                set ret = arg
            }
            var x: _int
            call f @[LOCAL] /x
            output std ()
        """.trimIndent()
        )
        assert(out == "()\n") { out }
    }
    @Test
    fun n04_pool () {
        val out = all("""
            var f : func@[i1]-> /_int@i1 -> /()@i1
            set f = func@[i1]-> /_int@i1 -> /()@i1 {
                set ret = arg
            }
            var g : func@[i1]-> /_int@i1 -> /()@i1
            set g = func@[i1]-> /_int@i1 -> /()@i1 {
                set ret = f @[i1] arg: @i1
            }
            var x: _int
            var px: /_int@LOCAL
            set px = f @[LOCAL] /x: @LOCAL
            output std _(global.px == &global.x):_int
        """.trimIndent()
        )
        assert(out == "1\n") { out }
    }
    @Test
    fun n05_pool_ff() {
        val out = all(
            """
            var f: ( func@[]-> () -> () )
            set f = func () -> () {
                set ret = arg
            }
            var g: func@[i1]-> [func@[]-> ()->(), ()] -> ()
            set g = func@[i1]-> [(func ()->()), ()] -> () {
                set ret = arg.1 arg.2
            }
            output std g @[LOCAL] [f,()]
        """.trimIndent()
        )
        assert(out == "()\n") { out }
    }
    @Test
    fun n06_pool_ff() {
        val out = all(
            """
            var f: func@[i1]->/()@i1 -> /()@i1
            set f = func@[i1]-> /()@i1 -> /()@i1 {
                set ret = arg
            }
            var g: func@[i1]-> [ func@[i1]-> /()@i1->/()@i1, /()@i1] -> /()@i1
            set g = func @[i1]->[func@[i1]-> /()@i1->/()@i1, /()@i1] -> /()@i1 {
                set ret = arg.1 @[i1] arg.2: @i1
            }
            var x: ()
            output std g @[LOCAL] [f,/x]: @LOCAL
        """.trimIndent()
        )
        assert(out == "_\n") { out }
    }
    @Test
    fun noclo_n08_clo_int () {
        val out = all("""
            { @A
                var xxx: _int
                set xxx = _10:_int
                var f: (func ()->_int)
                set f = func ()->_int {
                    set ret = xxx
                }
                set xxx = _20:_int
                output std xxx
                set xxx = f ()
                output std xxx
            }
        """.trimIndent())
        assert(out == "20\n20\n") { out }
    }
    @Test
    fun noclo_n13_pool_ups1 () {
        val out = all(
            """
            var f : func @[] -> _int -> ()
            set f = func @[] -> _int -> () { @A
                var x: _int
                set x = arg
                var g : func @[] -> () -> _int
                set g = func @[] -> () -> _int {
                    set ret = x
                    return
                }
                output std g ()
            }
            call f _10:_int
        """.trimIndent()
        )
        assert(out == "10\n") { out }
    }
    @Test
    fun noclo_n14_pool_ups2 () {
        val out = all(
            """
            var f : func @[] -> _int -> ()
            set f = func @[] -> _int -> () { @A
                var x: _int
                set x = arg
                var g : func@[] -> () -> _int
                set g = func @[] -> () -> _int {
                    var h : func @[] -> () -> _int
                    set h = func @[] -> () -> _int {
                        set ret = x
                        return
                    }
                    output std h ()
                }
                call g ()
            }
            call f _10:_int
        """.trimIndent()
        )
        assert(out == "10\n") { out }
    }

    // TYPEDEF / ALIAS

    @Test
    fun o01_alias () {
        val out = all("""
        type Uxit = ()
        var x: Uxit
        set x = Uxit () 
        output std x
        """.trimIndent())
        assert(out == "()\n") { out }
    }
    @Test
    fun o02_alias () {
        val out = all("""
        type Pair = [_int,_int]
        var x: Pair
        set x = Pair [_1:_int,_2:_int] 
        output std /x
        """.trimIndent())
        assert(out == "[1,2]\n") { out }
    }
    @Test
    fun o03_alias () {
        val out = all("""
        type List = </List @LOCAL>
        var l: /List @LOCAL
        set l = new List <.1 Null:/List@LOCAL>:</List @LOCAL>: @LOCAL
        output std l
        """.trimIndent())
        assert(out == "<.1 Null>\n") { out }
    }
    @Test
    fun o04_type_ptr () {
        val out = all("""
            type Num @[s] = </Num @[s] @s>
            type Ptr @[s] = /Num @[s] @s
            var n1: /Num @[LOCAL]
            set n1 = new Num @[LOCAL] <.1 Null:/Num @[LOCAL] @LOCAL>:</Num @[LOCAL] @LOCAL>
            var n2: Ptr @[LOCAL]
            set n2 = Ptr @[LOCAL] new Num @[LOCAL] <.1 Null:/Num @[LOCAL]>:</Num @[LOCAL] @LOCAL>
            output std n1
            output std n2
        """.trimIndent())
        assert(out == "<.1 Null>\n<.1 Null>\n") { out }
    }
    @Test
    fun o05_type_ptr () {
        val out = all("""
            type List @[s] = /<List @[s]> @s
            var l: List @[LOCAL]
            set l = new <.1 Null:List @[LOCAL]>:<List @[LOCAL]>
            output std l
        """.trimIndent())
        //assert(out == "<.1 Null>\n") { out }
        assert(out == "(ln 3, col 22): invalid type : expected pointer to alias type") { out }
    }

    @Test
    fun o07_type_pln () {
        val out = all("""
            type Int  @[] = _int
            type Pair @[] = [Int,Int]
            var x: Int
            set x = _10:_int
            var xs: Pair
            set xs = Pair [x,x]
            output std /xs
        """.trimIndent())
        assert(out == "[10,10]\n") { out }
    }
    @Test
    fun o08_type_pln () {
        val out = all("""
            type Int  @[] = _int
            type Pair @[] = <Int,Int>
            var x: Int
            set x = _10:_int
            var xs: Pair
            set xs = Pair<.1 x>:<Int,Int>
            output std /xs
        """.trimIndent())
        assert(out == "<.1 10>\n") { out }
    }
    @Test
    fun o09_type_pln () {
        val out = all("""
            type T1 @[] = [()]
            type T2 @[] = [T1]
            output std ()
        """.trimIndent())
        //assert(out == "(ln 1, col 6): invalid type identifier") { out }
        assert(out == "(ln 1, col 6): expected type identifier : have \"T1\"") { out }
    }
    @Test
    fun o10_type_pln () {
        val out = all("""
            type Tx1 @[] = [()]
            type Tx2 @[] = [Tx1]
            output std ()
        """.trimIndent())
        assert(out == "()\n") { out }
    }
    @Test
    fun o12_rect () {
        val out = all("""
            type Point @[] = [_int,_int]
            type Dims @[] = [_int,_int]
            type Rect @[] = [Point,Dims]
            var rect: Rect
            set rect = Rect[Point [(_10: _int),(_10: _int)],Dims[(_5: _int),(_5: _int)]]
            output std /rect
        """.trimIndent())
        assert(out == "[[10,10],[5,5]]\n") { out }
    }
    @Test
    fun o13_rect_dot () {
        val out = all("""
            type Point @[] = [_int,_int]
            type Dims @[] = [_int,_int]
            type Rect @[] = [Point,Dims]
            var rect: Rect
            set rect = Rect [Point[(_10: _int),(_10: _int)],Dims[(_5: _int),(_5: _int)]]
            output std rect  ~ .2 ~  .1
        """.trimIndent())
        assert(out == "5\n") { out }
    }
    @Test
    fun o13_func_alias () {
        val out = all("""
            type Int2Int = func @[] -> _int -> _int
            
            var f: Int2Int
            set f = Int2Int func @[] -> _int -> _int {
                set ret = arg
            }
            
            var x: _int
            set x = f~ _10:_int
            
            output std x
       """.trimIndent())
        assert(out == "10\n") { out }
    }
    @Test
    fun o14_func_alias () {
        val out = all("""
            type Xask @[] = task @[] -> () -> () -> ()
            var t: Xask
            set t = Xask(task @[] -> () -> () -> () {
            })
            output std ()
       """.trimIndent())
        assert(out == "()\n") { out }
    }
    @Test
    fun o15_func_alias () {
        val out = all("""
            type Xask @[] = task @[] -> () -> _int -> ()
            var t: Xask
                set t = Xask (task @[] -> () -> _int -> () {
                output std (_2: _int)
                set pub = _10:_int
            }
            )
            output std (_1: _int)
            var x: active Xask
            set x = spawn active Xask ((t ~ ) @[] ())
            var y: active task @[] -> () -> _int -> ()
            set y = spawn ((t ~ ) @[] ())
            output std ((x ~ ).pub)
            output std (_3: _int)
       """.trimIndent())
        assert(out == "1\n2\n2\n10\n3\n") { out }
    }

    // TYPE / HIER

    @Test
    fun p01_type_hier () {
        val out = all("""
        type Point = [_int,_int]
        type Event = <
            _int,
            (),
            <_int,_int>,    -- Key.Up/Down
            <               -- Mouse
                [Point],        -- Motion
                <               -- Button 
                    [Point,_int],   -- Up
                    [Point,_int]    -- Down
                >
            >
        >

        var e: Event
        set e = Event <.4 <.2 <.1 [Point [_10:_int,_10:_int],_1:_int]>:<[Point,_int],[Point,_int]>>:<[Point],<[Point,_int],[Point,_int]>>>:<_int,(),<_int,_int>,<[Point],<[Point,_int],[Point,_int]>>>
        output std /e
       """.trimIndent())
        assert(out == "<.4 <.2 <.1 [[10,10],1]>>>\n") { out }
    }
    @Test
    fun p02_type_hier () {
        val out = all("""
        type Button = <_int,_int> -- Up/Down
        var e: Button
        set e = Button <.2 _10:_int>:<_int,_int>
        output std /e
       """.trimIndent())
        assert(out == "<.2 10>\n") { out }
    }
    @Test
    fun p03_type_hier_err () {
        val out = all("""
        type Button = _int + <(),()> -- ERR: [_int] + ...
       """.trimIndent())
        assert(out == "(ln 1, col 20): expected statement : have `+´") { out }
    }
    @Test
    fun p04_type_hier_err () {
        val out = all("""
        type Button = [_int] + () -- ERR: ... + <...>
       """.trimIndent())
        assert(out == "(ln 1, col 24): expected `<´ : have `()´") { out }
    }
    @Test
    fun p05_type_hier () {
        val out = all("""
        type Button = [_int] + <(),()> -- Up/Down
        var e: Button
        set e = Button <.2 _10:_int>:<_int,_int>
        output std /e
       """.trimIndent())
        assert(out == "<.2 10>\n") { out }
    }
    @Test
    fun p06_type_hier () {
        val out = all("""
        type Button = [_int] + <[_int,()]+<_int>, ()> -- Up/Down
        var e: Button
        set e = Button
            <.1
                <.1
                    [_1:_int,_2:_int,(),_4:_int]
                >: <[_int,_int,(),_int]>
            >: <<[_int,_int,(),_int]>,_int>
        output std /e
       """.trimIndent())
        assert(out == "<.1 <.1 [1,2,(),4]>>\n") { out }
    }
    @Test
    fun p07_type_hier () {
        val out = all("""
        type Button = [_int] + <(),()> -- Up/Down
        var e: Button
        set e = Button <.2 _10:_int>:<_int,_int>
        output std /e
        output std e!0
       """.trimIndent())
        assert(out == "<.2 10>\n10\n") { out }
    }
    @Test
    fun p08_type_err () {
        val out = all("""
        type Button = <(),()> -- Up/Down
        var e: Button
        set e = Button <.2 ()>:<(),()>
        output std /e
        output std e!0
       """.trimIndent())
        assert(out == "(ln 5, col 14): invalid discriminator : out of bounds") { out }
    }

    // ALL

    @Test
    fun z01 () {
        val out = all("""
        var inv : (func <(),()> -> <(),()>)
        set inv = func@[]-> <(),()> -> <(),()> {
            if arg?1 {
                set ret = <.2()>:<(),()>
            } else {
                set ret = <.1()>:<(),()>
            }
        }
        var a: <(),()>
        set a = <.2()>: <(),()>
        var b: <(),()>
        set b = inv a
        output std /b
        """.trimIndent())
        assert(out == "<.1>\n") { out }
    }
    @Test
    fun z02 () {
        val out = all("""
        var i: _int; set i = _1: _int
        var n: _int; set n = _0: _int
        loop {
            set n = _(global.n + global.i): _int
            set i = _(global.i + 1): _int
            if _(global.i > 5): _int {
                break
            } else {}
        }
        output std n
        """.trimIndent())
        assert(out == "15\n") { out }
    }
    @Test
    fun z03 () {
        val out = all("""
        native _{}
        output std ()
        """.trimIndent())
        assert(out == "()\n")
    }
    @Test
    fun z04_if_bool () {
        val out = all("""
        if _0: _int {
        } else {
            output std ()
        }
        if _1: _int {
            output std ()
        } else {
        }
        """.trimIndent())
        assert(out == "()\n()\n")
    }
    @Test
    fun z05_func_rec () {
        val out = all("""
        var i: _int; set i = _0: _int
        var f: func@[]-> ()->()
        set f = func ()->() {
            if _(global.i == 10): _int {
                
            } else {
                set i = _(global.i + 1): _int
                set ret = f ()
            }
        }
        call f ()
        output std i
        """.trimIndent())
        assert(out == "10\n") { out }
    }
    @Test
    fun z06_type_complex () {
        val out = all("""
            type List @[a] = <[(),/List@[a]@a]>
            var x: /(List @[LOCAL]) @LOCAL
            set x = new List @[LOCAL] <.1 [(),Null: /(List @[LOCAL])@LOCAL]>:<[(),/List@[LOCAL]@LOCAL]>: @LOCAL
            var y: [(),/(List @[LOCAL]) @LOCAL]
            set y = [(), new List @[LOCAL] <.1 [(),Null: /(List @[LOCAL])@LOCAL]>:<[(),/List@[LOCAL]@LOCAL]>: @LOCAL]
            var z: [(),//(List @[LOCAL]) @LOCAL @LOCAL]
            set z = [(), /x]
            output std z.2\\ ~ !1.2
        """.trimIndent())
        assert(out == "Null\n") { out }
    }
    @Test
    fun z07_type_complex_bug () {
        val out = all("""
            type List @[a] = <[(),/List@[a]@a]>
            var x: /(List @[LOCAL]) @LOCAL
            set x = Null: /(List @[LOCAL]) @LOCAL
            var z: [(),//(List @[LOCAL]) @LOCAL @LOCAL]
            set z = [(), /x]
            output std z.2\
        """.trimIndent())
        assert(out == "Null\n") { out }
    }
    @Test
    fun z07_type_complex () {
        val out = all("""
            type List @[a] = <[(),/List@[a]@a]>
            var x: /(List @[LOCAL]) @LOCAL
            set x = Null: /(List @[LOCAL]) @LOCAL
            var z: [(),//(List @[LOCAL]) @LOCAL @LOCAL]
            set z = [(), /x]
            output std z.2\
        """.trimIndent())
        assert(out == "Null\n") { out }
    }
    @Test
    fun zxx_type_complex () {
        val out = all("""
            type List = </List @LOCAL>
            var z: /List @LOCAL
            set z = Null: /List @LOCAL
            output std z
        """.trimIndent())
        assert(out == "Null\n") { out }
    }
    @Test
    fun z08_type_complex () {
        val out = all("""
            type List @[a] = <[(),/List@[a]@a]>
            var y : [(),/(List @[LOCAL]) @LOCAL]
            set y = [(), new List @[LOCAL]<.1 [(),Null:/(List @[LOCAL]) @LOCAL]>:<[(),/List@[LOCAL]@LOCAL]>: @LOCAL]
            output std /y
        """.trimIndent())
        assert(out == "[(),<.1 [(),Null]>]\n") { out }
    }
    @Test
    fun z08_func_arg () {
        val out = all("""
            type List @[a] = </List @[a] @a>
            var x1: /(List @[LOCAL]) @LOCAL
            set x1 = Null: /(List @[LOCAL]) @LOCAL
            var z1: _int
            set z1 = x1\?Null
            var x2: //(List @[LOCAL]) @LOCAL @LOCAL
            set x2 = /x1
            var z2: _int
            set z2 = x2\\?1
            set x2\ = new List @[LOCAL]<.1 Null: /(List @[LOCAL]) @LOCAL>:</List @[LOCAL] @LOCAL>: @LOCAL
            var f: func@[i1]-> //(List @[i1])@i1@i1->_int
            set f = func@[i1]-> //(List @[i1])@i1@i1->_int {
                set ret = arg\\?1
            }
            var z3: _int
            set z3 = f @[LOCAL] x2
            var xxx: _int
            set xxx = _(global.z1 + global.z2 + global.z3): _int
            output std xxx
        """.trimIndent())
        assert(out == "2\n") { out }
    }
    @Test
    fun z08_func_alt () {
        val out = all("""
            type List = </List @LOCAL>
            var x1: /List @LOCAL
            set x1 = Null: /List @LOCAL
            var x2: //List @LOCAL @LOCAL
            set x2 = /x1
            var y2: _int
            set y2 = x2\\?1
            output std y2
        """.trimIndent())
        assert(out == "0\n") { out }
    }
    @Test
    fun z09_output_string () {
        val out = all("""
            type List @[a] = <[_int,/List @[a] @a]>
            var f: (func   ()->() )
            set f = func ()->() {
                var s1: /(List @[LOCAL]) @LOCAL
                set s1 = new List @[LOCAL] <.1 [_1:_int,Null: /(List @[LOCAL]) @LOCAL]>:<[_int,/List @[LOCAL] @LOCAL]>: @LOCAL
                output std s1
            }
            call f ()
        """.trimIndent())
        assert(out == "<.1 [1,Null]>\n") { out }
    }
    @Test
    fun z10_output_string () {
        val out = all("""
            type List @[a] = <[_int,/List @[a] @a]>
            var f: func@[]-> ()->()
            set f = func ()->() {
                var s1: /(List @[LOCAL]) @LOCAL
                set s1 = new List @[LOCAL]<.1 [_1:_int,Null: /(List @[LOCAL]) @LOCAL]>:<[_int,/List @[LOCAL] @LOCAL]>: @LOCAL
                output std s1
            }
            call f ()
        """.trimIndent())
        assert(out == "<.1 [1,Null]>\n") { out }
    }
    @Test
    fun z10_return_move () {
        val out = all("""
            type List @[a] = <[_int,/List @[a] @a]>
            var f: func@[i]-> ()-><(),_int,/(List @[i])@i>
            set f = func@[i]-> ()-><(),_int,/(List @[i])@i> {
                var str: /(List @[i]) @i
                set str = Null: /(List @[i]) @i
                set ret = <.3 str>:<(),_int,/(List @[i])@i>
            }
            var x: <(),_int,/(List @[LOCAL])@LOCAL>
            set x = f @[LOCAL] ()
            output std x!3
        """.trimIndent())
        assert(out == "Null\n") { out }
    }
    @Test
    fun z11_func_err () {
        val out = all("""
            var f: func @[]->()->[()]
            set f = func @[]->()->() {
            }
        """.trimIndent())
        assert(out.startsWith("(ln 2, col 7): invalid assignment : type mismatch")) { out }
    }
    @Test
    fun z12_union_tuple () {
        val out = all("""
            type List @[a] = <[_int,/List @[a] @a]>
            var tk2: <(),_int,/(List @[LOCAL]) @LOCAL>
            set tk2 = <.3 Null:/(List @[LOCAL]) @LOCAL>: <(),_int,/(List @[LOCAL]) @LOCAL>
            var s21: /<(),_int,/(List @[LOCAL]) @LOCAL> @LOCAL
            set s21 = /tk2
            output std s21\!3
        """.trimIndent())
        assert(out == "Null\n") { out }
    }
    @Test
    fun z15_acc_move_sub () {
        val out = all("""
            type List = <(),/List@LOCAL>
            var x: /List @LOCAL
            set x = new List <.2 new List <.1()>:<(),/List@LOCAL>: @LOCAL>:<(),/List@LOCAL>: @LOCAL
            var y: /List @LOCAL
            set y = x\!2
            output std x
            output std y
        """.trimIndent())
        assert(out == "<.2 <.1>>\n<.1>\n") { out }
    }
    @Test
    fun z16_rec () {
        val out = all(
            """
            var frec : func _int->_int
            set frec = func _int->_int {
                --output std arg
                if _(${D}arg == 1):_int {
                    set ret = _1:_int
                } else {
                    var tmp: _int
                    set tmp = frec _(${D}arg-1):_int
                    --output std arg
                    --output std tmp
                    set ret = _(${D}arg + ${D}tmp):_int
                }
            }
            output std (frec _5:_int)
        """.trimIndent()
        )
        assert(out == "15\n") { out }
    }
    @Test
    fun z17_include () {
        val out = test(true, """
            ^"test-func.ceu"
            call g ()
        """.trimIndent())
        assert(out == "()\n") { out }
    }
}
