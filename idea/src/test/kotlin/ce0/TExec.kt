import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.MethodOrderer.Alphanumeric
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import java.io.File

val Output0 = "type Output $D{} @{} = <_>"
fun output0 (v: String, tp: String): String {
    return "output Output $D{} @{} <.1 $v>: <$tp>"
}

val func0 = "call func $D{} @{} -> () -> ()"
fun catch0 (v: Int): String {
    return "catch _(task1->err.tag==1 && task1->err._1==$v)"
}
fun throw0 (v: Int): String {
    return "throw Error.1 $D{}@{} _$v:_int"
}

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
        val out = all("""
            type Output $D{} @{} = <_>
            output Output $D{} @{} <.1 ()>: <()>
        """.trimIndent())
        assert(out == "()\n") { out }
    }
    @Test
    fun a01_output2 () {
        val out = all("""
            type Output $D{} @{} = <_>
            output Output $D{} @{} <.1 _10:_int>: <_int>
        """.trimIndent())
        assert(out == "10\n") { out }
    }
    @Test
    fun a01_output3 () {
        val out = all("""
            $Output0
            var x: _int
            set x = ((_abs: _) $D{} @{} (_(-1): _))
            ${output0("x","_int")}            
        """.trimIndent())
        assert(out == "1\n") { out }
    }
    @Test
    fun a01_output4 () {
        val out = all("""
            native _{
                void output_2 (CEU_Output v) {
                    assert(v.tag == 2);
                    switch (v._2.tag) {
                        case 1: printf("()\n"); break;
                        case 2: printf("%d\n", v._2._2); break;
                    }
                }
            }
            type Output $D{} @{} = <_,<(),_int>>
            output Output $D{} @{} <.2 <.1>:<(),_int>>: <_,<(),_int>>
            output Output $D{} @{} <.2 <.2 _10:_int>:<(),_int>>: <_,<(),_int>>
        """.trimIndent())
        assert(out == "()\n10\n") { out }
    }
    @Test
    fun a02_var () {
        val out = all("""
            $Output0
            var x: ()
            set x = ()
            ${output0("x","()")}
        """.trimIndent())
        assert(out == "()\n") { out }
    }
    @Test
    fun a03_error () {
        val out = all("//output std ()")
        assert(out == "(ln 1, col 1): expected statement : have \"/\"") { out }
    }
    @Test
    fun a05_int () {
        val out = all("""
            $Output0
            var x: _int
            set x = _10: _int
            ${output0("x","_int")}
        """.trimIndent())
        assert(out == "10\n") { out }
    }
    @Test
    fun a06_int () {
        val out = all("""
            $Output0
            var x: _int
            ${output0("_10:_int","_int")}
        """.trimIndent())
        assert(out == "10\n") { out }
    }
    @Test
    fun a07_output_string () {
        val out = all("""
            type Output $D{} @{} = <_>
            output Output $D{} @{} <.1 _("hello"):_(char*)>: <_(char*)>
        """.trimIndent())
        assert(out == "\"hello\"\n") { out }
    }
    @Test
    fun a07_syntax_error () {
        val out = all("""
            native {
                putchar('A');
            }
        """.trimIndent())
        assert(out == "(ln 1, col 8): expected \"_\" : have \"{\"") { out }
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
            $Output0
            var x: _int
            set x = _abs:_ $D{} @{} _(-1): _int
            ${output0("x","_int")}
        """.trimIndent())
        assert(out == "1\n") { out }
    }
    @Test
    fun a10_int_set () {
        val out = all("""
            $Output0
            var x: _int
            set x = _10: _int
            set x = _(-20): _int
            ${output0("x","_int")}
        """.trimIndent())
        assert(out == "-20\n") { out }
    }
    @Test
    fun a11_unk () {
        val out = all("""
            $Output0
            var x: _int
            set x = _(-20): _int
            ${output0("x","_int")}
        """.trimIndent())
        assert(out == "-20\n") { out }
    }
    @Test
    fun a12_set () {
        val out = all("""
            $Output0
            var x: ()
            set x = ()
            set x = ()
            ${output0("x","()")}
        """.trimIndent())
        assert(out == "()\n")
    }

    // TUPLES

    @Test
    fun b01_tuple_units () {
        val out = all("""
            $Output0
            var x : [(),()]
            set x = [(),()]
            var y: ()
            set y = x.1
            native _{ output_Std_Unit(${D}y); }
        """.trimIndent())
        assert(out == "()\n") { out }
    }
    @Test
    fun todo_b02_tuple_idx () {
        val out = all("""
            $Output0
            ${output0("([(),()].1)","()")}  -- TODO: generate (struct T_Unit_Unit_T)
        """.trimIndent())
        //assert(out == "(ln 1, col 21): invalid discriminator : unexpected constructor") { out }
        assert(out == "()\n") { out }
    }
    @Test
    fun b03_union_idx () {
        val out = all("""
            $Output0
            ${output0("(<.1>:<(),()>)!1","()")}
        """.trimIndent())
        //assert(out == "(ln 1, col 21): invalid discriminator : unexpected constructor") { out }
        assert(out == "()\n") { out }
    }
    @Test
    fun b03_tuple_tuples () {
        val out = all("""
            $Output0
            var v: [(),()]
            set v = [(),()]
            var x: [(),[(),()]]
            set x = [(),v]
            var y: [(),()]
            set y = x.2
            var z: ()
            set z = y.2
            ${output0("z","_int")}
        """.trimIndent())
        assert(out == "()\n") { out }
    }
    @Test
    fun b04_tuple_pp () {
        val out = all("""
            type Output $D{} @{} = <_>
            var n: _int ; set n = _1: _int
            var x: [[_int,_int],[_int,_int]] ; set x = [[n,n],[n,n]]
            output Output $D{} @{} <.1 /x>: </[[_int,_int],[_int,_int]]@LOCAL>
        """.trimIndent())
        assert(out == "[[1,1],[1,1]]\n") { out }
    }
    @Test
    fun b05_tuple_pp () {
        val out = all("""
            $Output0
            var n: _int ; set n = _1: _int
            var x: [[_int,_int],[_int,_int]] ; set x = [[n,n],[n,n]]
            var y: [_int,[_int,_int]] ; set y = [n,[n,n]]
            ${output0("/x","/[[_int,_int],[_int,_int]]@LOCAL")}
            ${output0("/y","/[_int,[_int,_int]]@LOCAL")}
        """.trimIndent())
        assert(out == "[[1,1],[1,1]]\n[1,[1,1]]\n") { out }
    }

    // NATIVE

    @Test
    fun c01_nat () {
        val out = all("""
            native _{
                void f (void) { putchar('a'); }
            }
            call _f:_ $D{} @{} ()
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
            call _puts:_ $D{} @{} x.2
        """.trimIndent())
        assert(out == "hello\n") { out }
    }
    @Test
    fun c03_nat () {
        val out = all("""
            $Output0
            var y: _(char*); set y = _("hello"): _(char*)
            var n: _int; set n = _10: _int
            var x: [_int,_(char*)]; set x = [n,y]
            ${output0("/x","/[_int,_(char*)]@LOCAL")}
        """.trimIndent())
        assert(out == "[10,\"hello\"]\n") { out }
    }

    // FUNC / CALL

    @Test
    fun d01_f_int () {
        val out = all("""
            $Output0
            type Error $D{} @{} = <_int>
            var isEscRet : func $D{} @{} -> [Error$D{}@{},_int] -> _int
            set isEscRet = func $D{} @{} -> [Error$D{}@{},_int] -> _int {
                if arg.1~?1 {
                    var v1: _int
                    set v1 = arg.1~!1
                    var v2: _int
                    set v2 = arg.2
                    set ret = _(${D}v1 == ${D}v2)
                } else {
                    set ret = _0:_int
                }
            }
            var f: (func $D{} @{}-> _int -> _int)
            set f = func$D{} @{}-> _int -> _int {
                catch isEscRet $D{} @{} [err,_1:_int] {
                    set ret = arg
                    throw Error.1 $D{}@{} _1:_int
                }
            }
            var x: _int
            set x = f $D{} @{} _10: _int
            ${output0("x","_int")}
        """.trimIndent())
        assert(out == "10\n") { out }
    }
    @Test
    fun d02_f_unit () {
        val out = all("""
            $Output0
            type Error $D{} @{}= <_int>
            var f: (func $D{} @{}-> () -> ())
            set f = func$D{} @{}-> ()->() { ${catch0(1)} { ${throw0(1)} } }
            var x: ()
            set x = f $D{} @{} ()
            ${output0("x","()")}
        """.trimIndent())
        assert(out == "()\n") { out }
    }
    @Test
    fun d03_global () {
        val out = all("""
            $Output0
            var xxx: _int
            set xxx = _10:_int
            var g: (func$D{} @{}-> () -> ())
            set g = func$D{} @{}-> ()->() {
                ${output0("xxx","_int")}
            }
            call g $D{} @{} ()
        """.trimIndent())
        assert(out == "10\n") { out }
    }
    @Test
    fun d03_global2 () {
        val out = all("""
            $Output0
            var xxx: _int
            set xxx = _10:_int
            call (func $D{} @{}->()->() {
                ${output0("xxx","_int")}
            }) $D{} @{} ()
        """.trimIndent())
        assert(out == "10\n") { out }
    }
    @Test
    fun d03_fg () {
        val out = all("""
            $Output0
            var f: (func$D{} @{}-> () -> ())
            set f = func$D{} @{}-> ()->() { var x: _int; set x = _10: _int ; ${output0("x","_int")} }
            var g: (func$D{} @{}-> () -> ())
            set g = func$D{} @{}-> ()->() { call f $D{} @{} () }
            call g $D{} @{} ()
        """.trimIndent())
        assert(out == "10\n") { out }
    }
    @Test
    fun d04_arg () {
        val out = all("""
            $Output0
        var f : (func$D{} @{}-> _int -> _int)
        set f = func$D{} @{}-> _int->_int {
           set arg = _(${D}arg+1): _int
           set ret = arg
        }
        ${output0("f $D{} @{} _1","_int")}
        """.trimIndent())
        assert(out == "2\n") { out }
    }
    @Test
    fun d04_arg_err1 () {
        val out = all("""
            set _:_ = _(${D}{arg)
        """.trimIndent())
        assert(out == "(ln 1, col 11): invalid `\$´") { out }
    }
    @Test
    fun d04_arg_err2 () {
        val out = all("""
            set _:_ = _(${D})
        """.trimIndent())
        assert(out == "(ln 1, col 11): invalid `\$´") { out }
    }
    @Test
    fun d04_arg_glb () {
        val out = all("""
            $Output0
            var x: _int
            set x = _10:_int
            var f : (func$D{} @{}-> () -> _int)
            set f = func$D{} @{}-> ()->_int {
               set arg = _(${D}x+1): _int
               set ret = arg
            }
            ${output0("f $D{} @{} ()","_int")}
        """.trimIndent())
        assert(out == "11\n") { out }
    }
    @Test
    fun d04_arg_glb2 () {
        val out = all("""
            $Output0
            var f : func $D{} @{} -> () -> ()
            set f = func $D{} @{} -> () -> () {
                var xxx: _int
                set xxx = _10:_int
                var g : func $D{} @{} -> () -> ()
                set g = func $D{} @{} -> () -> () {
                    ${output0("xxx","_int")}
                }
                call g $D{} @{} ()
            }
            call f $D{} @{} ()
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
            $Output0
            var f: (func$D{} @{}-> _int->_int)
            set f = func$D{} @{}-> _int->_int { set ret=arg }
            var p: (func$D{} @{}-> _int->_int)
            set p = f
            ${output0("p $D{} @{} _10: _int","_int")}
        """.trimIndent())
        assert(out == "10\n") { out }
    }
    @Test
    fun d06_func_fg () {
        val out = all("""
            $Output0
            var f: (func$D{} @{}-> _int->_int)
            set f = func$D{} @{}-> _int->_int { set ret=arg }
            var g: (func$D{} @{}-> [func$D{} @{}-> _int->_int, _int] -> _int)
            set g = func$D{} @{}-> [func$D{} @{}-> _int->_int, _int] -> _int {
               var f: (func$D{} @{}-> _int->_int)
               set f = arg.1
               var v: _int
               set v = f $D{} @{} arg.2
               set ret = v
            }
            ${output0("g $D{} @{} [f,_10: _int]","_int")}
        """.trimIndent())
        assert(out == "(ln 6, col 8): invalid declaration : \"f\" is already declared (ln 2)") { out }
    }
    @Test
    fun d07_func_fg () {
        val out = all("""
            $Output0
            var f:(func$D{} @{}->  _int->_int)
            set f = func$D{} @{}-> _int->_int { set ret=arg }
            var g:  (func$D{} @{i1}-> [(func$D{} @{}-> _int->_int), _int] -> _int)
            set g = func$D{} @{i1}-> [(func$D{} @{}-> _int->_int), _int]-> _int {
               var fx: (func$D{} @{}-> _int->_int)
               set fx = arg.1
               var v: _int
               set v = fx $D{} @{} arg.2
               set ret = v
            }
            --var n: _int = _10: _int
            ${output0("g $D{} @{LOCAL} [f,_10:_int]","_int")}
        """.trimIndent())
        assert(out == "10\n") { out }
    }
    @Test
    fun d08_func_unit () {
        val out = all("""
            $Output0
            var f: (func$D{} @{}->  ()->() )
            set f = func$D{} @{}-> ()->() { var x:() ; set x = arg ; set ret=arg }
            var x:() ; set x = f $D{} @{} ()
            ${output0("x","()")}
        """.trimIndent())
        assert(out == "()\n") { out }
    }
    @Test
    fun d09_func_unit () {
        val out = all("""
            $Output0
            var f: (func$D{} @{}-> ()->())
            set f = func$D{} @{}-> ()->() { var x:() ; set x = arg ; set ret=arg }
            ${output0("f $D{} @{} ()","()")}
        """.trimIndent())
        assert(out == "()\n") { out }
    }
    @Test
    fun d10_func_rec () {
        val out = all("""
            type Num $D{} @{} = </Num$D{}@{}@LOCAL>    
            var clone : func $D{} @{} -> /Num$D{}@{}@LOCAL -> /Num$D{}@{}@LOCAL
            set clone = func $D{} @{} -> /Num$D{}@{}@LOCAL -> /Num$D{}@{}@LOCAL {
                set ret = new <.1 clone $D{} @{} arg\~!1>:</Num$D{}@{}@LOCAL>
            }
            call clone $D{} @{} Null
        """.trimIndent())
        assert(out == "(ln 4, col 15): invalid `new` : expected named type") { out }
    }
    @Test
    fun d11_func_rec () {
        val out = all("""
            type Num $D{} @{} = </Num$D{}@{}@GLOBAL>    
            var clone : func $D{} @{} -> /Num$D{}@{}@GLOBAL -> /Num$D{}@{}@GLOBAL
            set clone = func $D{} @{} -> /Num$D{}@{}@GLOBAL -> /Num$D{}@{}@GLOBAL {
                set ret = new Num $D{} @{} <.1 clone $D{} @{} arg\~!1>:</Num$D{}@{}@GLOBAL>
            }
            call clone $D{} @{} Null
        """.trimIndent())
        assert(out.contains("Assertion `&(*(task1->arg)) != NULL' failed.")) { out }
    }

    // INPUT / OUTPUT

    @Disabled   // no more lib
    @Test
    fun e01_out () {
        val out = all("""
            output () ()
        """.trimIndent())
        //assert(out == "(ln 1, col 8): invalid `output` : expected identifier") { out }
        assert(out == "(ln 1, col 8): expected variable identifier : have \"()\"") { out }
    }
    @Test
    fun e02_out () {
        val out = all("""
            $Output0
            var x: [(),()]; set x = [(),()]
            ${output0("/x","/[(),()]@LOCAL")}
        """.trimIndent())
        assert(out == "[(),()]\n")
    }
    @Disabled // TODO: changed to <.2 ...>
    @Test
    fun e03_out () {
        val out = all("""
            $Output0
            native _{
                void output_f (int x) {
                    output_Std_int(x);
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
            type Output $D{} @{} = <_>
            type Input  $D{} @{} = <_>
            var x: _int
            set x = input Input $D{} @{} <.1>:<_>: _int
            output Output $D{} @{} <.1 x>:<_>
        """.trimIndent())
        assert(out == "10\n") { out }
    }
    @Disabled   // needs user input
    @Test
    fun e05_inp () {
        val out = all("""
            $Output0
            var x: _int
            var y: </_int@LOCAL>
            set x = input Std $D{} @{} y: _int
            ${output0("x","_int")}
        """.trimIndent())
        assert(out == "10\n") { out }
    }
    @Test
    fun e06_inp () {
        val out = all("""
            $Output0
            type Input  $D{} @{} = <_>
            type Int $D{} @{} = _int
            var x: Int $D{} @{}
            set x = input Input $D{} @{} <.1>:<_>: Int $D{} @{}
            ${output0("x","_int")}
        """.trimIndent())
        assert(out.contains("implicit declaration of function ‘input_1_CEU_Int’")) { out }
    }
    @Disabled   // needs user input
    @Test
    fun e07_inp () {
        val out = all("""
            $Output0
            type Int = <_int,()>
            var x: Int
            set x = input Std $D{} @{} <.1 _10:_int>: Int: Int
            output std x
        """.trimIndent())
        assert(out.contains("XXX")) { out }
    }

    // USER

    @Test
    fun f01_bool () {
        val out = all("""
            $Output0
            var b : <(),()>
            set b = <.1()>:<(),()>
            ${output0("/b","/<(),()>@LOCAL")}
        """.trimIndent())
        assert(out == "<.1>\n") { out }
    }
    @Test
    fun f02_xyz () {
        val out = all("""
            $Output0
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
            ${output0("/zz","/<()>@LOCAL")}
        """.trimIndent())
        assert(out == "<.1>\n") { out }
    }
    @Test
    fun f05_user_big () {
        val out = all("""
            $Output0
            var s: <[_int,_int,_int,_int],_int,_int>
            set s = <.1 [_1:_int,_2:_int,_3:_int,_4:_int]>:<[_int,_int,_int,_int],_int,_int>
            ${output0("/s","/<[_int,_int,_int,_int],_int,_int>@LOCAL")}
        """.trimIndent())
        assert(out == "<.1 [1,2,3,4]>\n") { out }
    }
    @Test
    fun f06_user_big () {
        val out = all("""
            $Output0
            var x: <[<()>,<()>]>
            set x = <.1 [<.1()>:<()>,<.1()>:<()>]>:<[<()>,<()>]>
            ${output0("/x","/<[<()>,<()>]>@LOCAL")}
        """.trimIndent())
        assert(out == "<.1 [<.1>,<.1>]>\n") { out }
    }
    @Test
    fun f07_user_pred () {
        val out = all("""
            $Output0
            var z: <()>
            set z = <.1()>: <()>
            ${output0("z?1","_int")}
        """.trimIndent())
        assert(out == "1\n") { out }
    }
    @Test
    fun f10_user_disc () {
        val out = all("""
            $Output0
            var z: <(),()>
            set z = <.2 ()>:<(),()>
            ${output0("z!2","()")}
        """.trimIndent())
        assert(out == "()\n") { out }
    }
    @Test
    fun f11_user_disc_err () {
        val out = all("""
            $Output0
            var z: <(),()>
            set z = <.2()>: <(),()>
            ${output0("z!1","()")}
        """.trimIndent())
        assert(out.endsWith("main: Assertion `(global.z).tag == 1' failed.\n")) { out }
    }
    @Test
    fun f12_user_disc_pred_idx () {
        val out = all("""
            $Output0
            var v: <[<()>,()]>
            set v = <.1 [<.1()>:<()>,()]>: <[<()>,()]>
            ${output0("v!1.1?1","_int")}
        """.trimIndent())
        assert(out == "1\n") { out }
    }
    @Test
    fun f13_user_disc_pred_err () {
        val out = all("""
            set _:_ = ()?1
        """.trimIndent())
        assert(out == "(ln 1, col 14): invalid predicate : not an union") { out }
    }
    @Test
    fun f14_user_dots_err () {
        val out = all("""
            var x: <<<()>>>; set x = <.1 <.1 <.1()>:<()>>:<<()>>>:<<<()>>>
            set _:_ = x!1!2
        """.trimIndent())
        assert(out == "(ln 2, col 15): invalid discriminator : out of bounds") { out }
    }
    @Test
    fun f15_user_dots () {
        val out = all("""
            $Output0
            var x: <<<()>>>
            set x = <.1 <.1 <.1()>:<()>>:<<()>>>:<<<()>>>
            ${output0("x!1!1!1","()")}
        """.trimIndent())
        assert(out == "()\n") { out }
    }

    // IF

    @Test
    fun g01_if () {
        val out = all("""
            $Output0
            var x: <(),()>
            set x = <.2()>: <(),()>
            if x?1 { } else { ${output0("()","()")} }            
        """.trimIndent())
        assert(out == "()\n") { out }
    }
    @Test
    fun g02_if_pred () {
        val out = all("""
            $Output0
            var x: <(),()>
            set x = <.2()>: <(),()>
            if x?2 { ${output0("()","()")} } else { }
        """.trimIndent())
        assert(out == "()\n") { out }
    }
    @Test
    fun g03_if_expr () {
        val out = all("""
            $Output0
            var x: <_int,()>
            set x = <.2 ()>: <_int,()>
            ${output0("(if x?1 { x!1 } else { _999:_int })","_int")}
        """.trimIndent())
        assert(out == "999\n") { out }
    }

    // LOOP

    @Test
    fun h01_loop () {
        val out = all("""
            $Output0
        type Error $D{} @{} = <_int>
        call func $D{} @{}->()->() {
            ${catch0(1)} {
                loop {
                   ${throw0(1)}
                }
            }
            ${output0("()","()")}
        } $D{} @{} ()
        """.trimIndent())
        assert(out == "()\n") { out }
    }

    // PTR

    @Test
    fun i01_ptr () {
        val out = all("""
            $Output0
            var y: _int
            set y = _10: _int
            var x: /_int@LOCAL
            set x = /y
            ${output0("x\\","_int")}
        """.trimIndent())
        assert(out == "10\n") { out }
    }
    @Test
    fun i02_ptr_func () {
        val out = all("""
            $Output0
            var f : func$D{} @{i1}-> /_int@i1 -> ()
            set f = func$D{} @{i1}-> /_int@i1 -> () {
               set arg\ = _(*${D}arg+1): _int
            }
            var x: _int
            set x = _1: _int
            call f $D{} @{LOCAL} /x
            ${output0("x","_int")}
        """.trimIndent())
        assert(out == "2\n") { out }
    }
    @Test
    fun i03_ptr_func () {
        val out = all("""
            $Output0
            var f: func$D{} @{i1}-> /_int@i1->_int
            set f = func $D{} @{i1}->/_int@i1->_int { set ret = arg\ }
            var g: func$D{} @{i1}-> /_int@i1->_int
            set g = f
            var x: _int
            set x = _10: _int
            ${output0("g $D{} @{LOCAL}(/x)","_int")}
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
            $Output0
            var f : func$D{} @{i1}-> /_int@i1 -> /_int@i1
            set f = func$D{} @{i1}-> /_int@i1 -> /_int@i1 {
                set ret = arg
            }
            var v: _int
            set v = _10: _int
            var p: /_int@LOCAL
            set p = f$D{} @{LOCAL} /v
            ${output0("p\\","_int")}
        """.trimIndent())
        assert(out == "10\n") { out }
    }
    @Test
    fun i08_ptr_func_ok () {
        val out = all("""
            $Output0
            var v: _int
            set v = _10: _int
            var f : func$D{} @{i1}-> () -> /_int@i1
            set f = func$D{} @{i1}-> () -> /_int@i1 {
                set ret = /v
            }
            var p: /_int @LOCAL
            set p = f $D{} @{LOCAL} ()
            ${output0("p\\","_int")}
        """.trimIndent())
        assert(out == "10\n") { out }
    }
    @Test
    fun i09_ptr_func_err () {
        val out = all("""
            var f : func$D{} @{i1}-> () -> /_int@i1
            set f = func$D{} @{i1}-> () -> /_int@i1 {
                var v: _int; set v = _10: _int
                set ret = /v   -- err
            }
        """.trimIndent())
        assert(out.startsWith("(ln 4, col 13): invalid return : type mismatch")) { out }
    }
    @Test
    fun i10_ptr_func_err () {
        val out = all("""
            var f : func$D{} @{i1}-> /_int@i1 -> /_int@i1
            set f = func$D{} @{i1}-> /_int@i1 -> /_int@i1 {
                var ptr: /_int@LOCAL
                set ptr = arg
                set ret = ptr  -- err
            }
        """.trimIndent())
        assert(out.startsWith("(ln 5, col 13): invalid return : type mismatch")) { out }
    }
    @Test
    fun i11_ptr_func_ok () {
        val out = all("""
            $Output0
            var f : func$D{} @{i1}-> /_int@i1 -> /_int@i1
            set f = func$D{} @{i1}-> /_int@i1 -> /_int@i1 {
                var ptr: /_int@i1
                set ptr = arg
                set ret = ptr
            }
            var v: _int
            set v = _10: _int
            var p: /_int@LOCAL
            set p = f $D{} @{LOCAL} /v
            ${output0("p\\","_int")}
        """.trimIndent())
        assert(out == "10\n") { out }
    }
    @Test
    fun i12_ptr_ptr_ok () {
        val out = all("""
            $Output0
            var p: //_int @LOCAL @LOCAL
            var z: _int; set z = _10: _int
            var y: /_int@LOCAL; set y = /z
            set p = /y
            ${output0("p\\\\","_int")}
        """.trimIndent())
        assert(out == "10\n") { out }
    }
    @Test
    fun i13_ptr_tup () {
        val out = all("""
            $Output0
            var v: [_int,_int]
            set v = [_10:_int,_20:_int]
            var p: /_int @LOCAL
            set p = /v.1
            set p\ = _20: _int
            ${output0("/v","/ [_int,_int]@LOCAL")}
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
            $Output0
            var v: [_int]; set v = [_10:_int]
            var p: /_int@LOCAL; set p = /v.1
            set p\ = _20: _int
            ${output0("/v","/[_int]@LOCAL")}
        """.trimIndent())
        assert(out == "[20]\n") { out }
    }
    @Test
    fun i15_ptr_tup () {
        val out = all("""
            $Output0
            var x: _int; set x = _10: _int
            var p: [_int,/_int @LOCAL]; set p = [_10:_int,/x]
            var v: _int; set v = _20: _int
            set p.2 = /v
            ${output0("p.2\\","_int")}
        """.trimIndent())
        assert(out == "20\n") { out }
    }
    @Test
    fun i16_ptr_tup () {
        val out = all("""
            $Output0
            var x: _int; set x = _10: _int
            var p: [_int,/_int @LOCAL]; set p = [_10:_int,/x]
            var v: _int; set v = _20: _int
            set p = [_10:_int,/v]
            ${output0("p.2\\","_int")}
        """.trimIndent())
        assert(out == "20\n") { out }
    }
    @Test
    fun i17_ptr_type () {
        val out = all("""
            $Output0
            var x: _int
            set x = _10: _int
            var p: </_int@LOCAL>
            set p = <.1 /x>: </_int @LOCAL>
            var v: _int
            set v = _20: _int
            set p!1 = /v
            ${output0("p!1\\","_int")}
        """.trimIndent())
        assert(out == "20\n") { out }
    }
    @Test
    fun i18_ptr_type () {
        val out = all("""
            $Output0
            var x: _int
            set x = _10: _int
            var p: </_int @LOCAL>
            set p = <.1 /x>: </_int@LOCAL>
            var v: _int
            set v = _20: _int
            set p = <.1 /v>: </_int @LOCAL>
            ${output0("p!1\\","_int")}
        """.trimIndent())
        assert(out == "20\n") { out }
    }
    @Test
    fun i19_ptr_tup () {
        val out = all("""
            $Output0
            var x1: [_int,/_int @LOCAL]
            var v: _int; set v = _20: _int
            var x2: [_int,/_int@LOCAL]; set x2 = [_10:_int,/v]
            set x1 = x2
            ${output0("x1.2\\","_int")}
        """.trimIndent())
        assert(out == "20\n") { out }
    }
    @Test
    fun f09_ptr_type_err () {
        val out = all("""
            $Output0
            var x1: </_int @LOCAL>
            var v: _int
            set v = _20: _int
            var x2: </_int @LOCAL>
            set x2 = <.1 /v>:</_int@LOCAL>
            set x1 = x2
            ${output0("x1!1\\","_int")}
        """.trimIndent())
        assert(out == "20\n") { out }
    }
    @Test
    fun f10_ptr_func () {
        val out = all("""
            $Output0
            var v: _int
            set v = _10: _int
            var f : func$D{} @{i1}-> /_int@i1 -> /_int@i1
            set f = func$D{} @{i1}-> /_int@i1 -> /_int@i1 {            
                set ret = /v
            }
            --{
                var p: /_int @LOCAL
                set p = f $D{} @{GLOBAL} /v: @GLOBAL
                ${output0("p\\","_int")}
            --}
        """.trimIndent())
        assert(out == "10\n") { out }
    }
    @Test
    fun f11_ptr_func () {
        val out = all("""
            $Output0
            var f: (func$D{} @{i1}-> /_int@i1 -> /_int@i1)
            set f = func$D{} @{i1}-> /_int@i1 -> /_int@i1 {
                set ret = arg
            }
            var v: _int
            set v = _10: _int
            var p: /_int @LOCAL
            set p = f $D{} @{LOCAL} /v: @LOCAL
            ${output0("p\\","_int")}
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
            $Output0
            type List $D{} @{} = </List $D{} @{} @LOCAL>
            var l: /List $D{} @{} @LOCAL
            set l = Null: /List$D{} @{}@LOCAL
            ${output0("l","/List $D{} @{} @LOCAL")}
        """.trimIndent())
        //assert(out == "(ln 1, col 5): invalid assignment : type mismatch") { out }
        assert(out == "Null\n") { out }
    }
    @Test
    fun j02_list_new_err_dst () {
        val out = all("""
            $Output0
            type List $D{} @{} = </List $D{} @{} @LOCAL>
            var l: /List $D{} @{} @LOCAL
            set l = new List $D{} @{} <.1 Null: /List$D{} @{}@LOCAL>:</List$D{} @{} @LOCAL>: @LOCAL
            ${output0("l","/List $D{} @{} @LOCAL")}
        """.trimIndent())
        //assert(out == "(ln 1, col 21): invalid assignment : type mismatch") { out }
        assert(out == "<.1 Null>\n") { out }
    }
    @Test
    fun j02_list_new_err_dst2 () {
        val out = all("""
            $Output0
            type List $D{} @{} = </List $D{} @{} @GLOBAL>
            var l: /List $D{} @{} @GLOBAL
            set l = new List $D{} @{} <.1 Null: /List $D{} @{} @GLOBAL>:</List $D{} @{} @GLOBAL>: @GLOBAL
            ${output0("l","/List $D{} @{} @LOCAL")}
        """.trimIndent())
        //assert(out == "(ln 1, col 21): invalid assignment : type mismatch") { out }
        assert(out == "<.1 Null>\n") { out }
    }
    @Test
    fun j02_list_new_err_src () {
        val out = all("""
            type List $D{} @{} = </List $D{} @{} @LOCAL>
            var l: /List $D{} @{} @LOCAL
            set l = new _1: _int @LOCAL
        """.trimIndent())
        assert(out == "(ln 3, col 9): invalid `new` : expected constructor") { out }
        //assert(out == "(ln 3, col 9): invalid `new` : expected alias constructor") { out }
    }
    @Test
    fun j02_list_new_ok () {
        val out = all("""
            $Output0
            type List $D{} @{} = </List $D{} @{} @LOCAL>
            var l: /List $D{} @{} @LOCAL
            set l = new List $D{} @{} <.1 Null: /List$D{} @{}@LOCAL>:</List $D{} @{} @LOCAL>: @LOCAL
            ${output0("l","/List $D{} @{} @LOCAL")}
        """.trimIndent())
        assert(out == "<.1 Null>\n") { out }
    }
    @Test
    fun j02_list_pln () {
        val out = all("""
            $Output0
            type List $D{} @{} = </List $D{} @{} @LOCAL>
            var l: List $D{} @{}
            set l = List $D{} @{} <.1 Null:/List$D{} @{}@LOCAL>:</List $D{} @{} @LOCAL>
            ${output0("/l","/List $D{} @{} @LOCAL")}
        """.trimIndent())
        //assert(out == "(ln 1, col 25): invalid union constructor : expected `new`") { out }
        assert(out == "<.1 Null>\n") { out }
    }
    @Test
    fun j03_list () {
        val out = all("""
            $Output0
            type List $D{} @{} = </List $D{} @{} @LOCAL>
            var l: /List $D{} @{} @LOCAL
            var z: /List $D{} @{} @LOCAL
            set z = Null: /List $D{} @{} @LOCAL
            var one: /List $D{} @{} @LOCAL
            set one = new List$D{} @{}<.1 z>:</List$D{} @{} @LOCAL>: @LOCAL
            set l = new List$D{} @{}<.1 one>:</List $D{} @{}@LOCAL>: @LOCAL
            ${output0("l\\~!1","/List $D{} @{} @LOCAL")}
        """.trimIndent())
        assert(out == "<.1 Null>\n") { out }
    }
    @Test
    fun j05_list_disc_null_err () {
        val out = all("""
            $Output0
            type List $D{} @{} = </List $D{} @{}@LOCAL>
            var l: /List $D{} @{} @LOCAL
            set l = Null: /List $D{} @{} @LOCAL
            ${output0("l\\ ~ !1","/List $D{} @{} @LOCAL")}
        """.trimIndent())
        assert(out.endsWith("(global.l)) != NULL' failed.\n")) { out }
    }
    @Test
    fun j06_list_1 () {
        val out = all("""
            $Output0
            type List $D{} @{} = </List $D{} @{} @LOCAL>
            var l: /List $D{} @{} @LOCAL
            var z: /List $D{} @{} @LOCAL
            set z = Null: /List $D{} @{} @LOCAL
            var one: /List $D{} @{} @LOCAL
            set one = new List $D{} @{} <.1 z>:</List $D{} @{} @LOCAL>: @LOCAL
            set l = new List $D{} @{} <.1 one>:</List $D{} @{} @LOCAL>: @LOCAL
            var p: //List $D{} @{} @LOCAL @LOCAL
            {
                set p = /l --!1
            }
            ${output0("p\\","/List $D{} @{} @LOCAL")}
        """.trimIndent())
        assert(out == "<.1 <.1 Null>>\n") { out }
    }
    @Test
    fun j06_list_2 () {
        val out = all("""
            $Output0
            type List $D{} @{} = </List $D{} @{} @LOCAL>
            var l: /List $D{} @{} @LOCAL
            var z: /List $D{} @{} @LOCAL
            set z = Null: /List $D{} @{} @LOCAL
            var one: /List $D{} @{} @LOCAL
            set one = new List $D{} @{} <.1 z>:</List $D{} @{} @LOCAL>: @LOCAL
            set l = new List $D{} @{} <.1 one>:</List $D{} @{} @LOCAL>: @LOCAL
            var p: /List $D{} @{} @LOCAL
            {
                set p = l --!1
            }
            ${output0("p","/List $D{} @{} @LOCAL")}
        """.trimIndent())
        assert(out == "<.1 <.1 Null>>\n") { out }
    }
    @Test
    fun j06_list_ptr () {
        val out = all("""
            $Output0
            type List $D{} @{} = </List $D{} @{} @LOCAL>
            var l: /List $D{} @{} @LOCAL
            var z: /List $D{} @{} @LOCAL
            set z = Null: /List $D{} @{} @LOCAL
            var one: /List $D{} @{} @LOCAL
            set one = new List $D{} @{}<.1 z>:</List $D{} @{} @LOCAL>: @LOCAL
            set l = new List $D{} @{}<.1 one>:</List $D{} @{} @LOCAL>: @LOCAL
            var p: //List $D{} @{} @LOCAL @LOCAL
            {
                set p = /l --!1
            }
            ${output0("p","//List $D{} @{} @LOCAL @LOCAL")}
        """.trimIndent())
        assert(out == "_\n") { out }
    }
    @Test
    fun j07_list_move () {
        val out = all("""
            $Output0
            type List $D{} @{} = </List $D{} @{} @LOCAL>
            var l1: /List $D{} @{} @LOCAL
            set l1 = new List $D{} @{} <.1 Null: /List $D{} @{} @LOCAL>:</List $D{} @{} @LOCAL>: @LOCAL
            var l2: /List $D{} @{} @LOCAL
            set l2 = l1
            ${output0("/l1","//List $D{} @{} @LOCAL @LOCAL")}
            ${output0("/l2","//List $D{} @{} @LOCAL @LOCAL")}
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
        assert(out == "(ln 1, col 11): expected type : have \"?\"") { out }
    }
    @Test
    fun j08_list_move () {
        val out = all("""
            $Output0
            type List $D{} @{} = </List $D{} @{} @LOCAL>
            var l1: /List $D{} @{} @LOCAL
            set l1 = new List $D{} @{} <.1 Null: /List $D{} @{} @LOCAL>:</List $D{} @{} @LOCAL>: @LOCAL
            var l2: /List $D{} @{} @LOCAL
            set l2 = Null: /List $D{} @{} @LOCAL
            set l2 = new List $D{} @{} <.1 l1>:</List $D{} @{} @LOCAL>: @LOCAL
            ${output0("l2","/List $D{} @{} @LOCAL")}
        """.trimIndent())
        assert(out == "<.1 <.1 Null>>\n") { out }
    }
    @Test
    fun j09_list_move () {
        val out = all("""
            $Output0
            type List $D{} @{} = </List $D{} @{} @LOCAL>
            var l1: /List $D{} @{} @LOCAL
            set l1 = new List $D{} @{} <.1 Null: /List $D{} @{} @LOCAL>:</List $D{} @{} @LOCAL>: @LOCAL
            var l2: [_int,/List $D{} @{} @LOCAL]
            set l2 = [_10:_int, l1]
            ${output0("l1","/List $D{} @{} @LOCAL")}
            ${output0("l2.2","/List $D{} @{} @LOCAL")}
        """.trimIndent())
        //assert(out == "Null\n<.1 Null>\n") { out }
        assert(out == "<.1 Null>\n<.1 Null>\n") { out }
    }
    @Test
    fun j10_rec () {
        val out = all("""
            $Output0
            type List $D{} @{} = </List $D{} @{} @LOCAL>
            var z: /List $D{} @{} @LOCAL
            set z = Null: /List $D{} @{} @LOCAL
            var one: /List $D{} @{} @LOCAL
            set one = new List $D{} @{} <.1 z>:</List $D{} @{} @LOCAL>: @LOCAL

            var n: </List $D{} @{} @LOCAL>
            set n = <.1 one>:</List $D{} @{} @LOCAL>
            ${output0("/n","/</List $D{} @{} @LOCAL>@LOCAL")}
        """.trimIndent())
        assert(out == "<.1 <.1 Null>>\n") { out }
    }
    @Test
    fun j11_borrow_1 () {
        val out = all("""
            $Output0
            type List $D{} @{} = </List $D{} @{} @LOCAL>
            var z: /List $D{} @{} @LOCAL
            set z = Null: /List $D{} @{} @LOCAL
            var one: /List $D{} @{} @LOCAL
            set one = new List $D{} @{} <.1 z>:</List $D{} @{} @LOCAL>: @LOCAL

            var x: /List $D{} @{} @LOCAL
            set x = new List $D{} @{} <.1 one>:</List $D{} @{} @LOCAL>: @LOCAL
            var y: //List $D{} @{} @LOCAL @LOCAL
            set y = /x
            ${output0("y\\","/List $D{} @{} @LOCAL")}
        """.trimIndent())
        assert(out == "<.1 <.1 Null>>\n") { out }
    }
    @Test
    fun j11_borrow_2 () {
        val out = all("""
            $Output0
            type List $D{} @{} = </List $D{} @{} @LOCAL>
            var z: /List $D{} @{} @LOCAL
            set z = Null: /List $D{} @{} @LOCAL
            var one: /List $D{} @{} @LOCAL
            set one = new List $D{} @{} <.1 z>:</List $D{} @{} @LOCAL>: @LOCAL

            var x: /List $D{} @{} @LOCAL
            set x = new List $D{} @{} <.1 one>:</List $D{} @{} @LOCAL>: @LOCAL
            var y: //List $D{} @{} @LOCAL @LOCAL
            set y = /x --!1
            ${output0("y\\","/List $D{} @{} @LOCAL")}
        """.trimIndent())
        assert(out == "<.1 <.1 Null>>\n") { out }
    }
    @Test
    fun j09_tup_list_err () {
        val out = all("""
            $Output0
            type List $D{} @{} = </List $D{} @{} @LOCAL>
            var t: [_int,/List $D{} @{} @LOCAL]
            set t = [_10:_int, new List $D{} @{} <.1 Null:/List $D{} @{} @LOCAL>:</List $D{} @{} @LOCAL>: @LOCAL]
            ${output0("/t","/[_int,/List $D{} @{} @LOCAL]@LOCAL")}
        """.trimIndent())
        assert(out == "[10,<.1 Null>]\n") { out }
    }
    @Test
    fun j10_tup_copy_ok () {
        val out = all("""
            $Output0
            type List $D{} @{} = </List $D{} @{} @LOCAL>
            var l: /List $D{} @{} @LOCAL
            set l = Null: /List $D{} @{} @LOCAL
            var t1: [/List $D{} @{} @LOCAL]
            set t1 = [l]
            var t2: [/List $D{} @{} @LOCAL]
            set t2 = [t1.1]
            ${output0("/t2","/[/List $D{} @{} @LOCAL]@LOCAL")}
        """.trimIndent())
        assert(out == "[Null]\n") { out }
    }
    @Test
    fun j11_tup_move_ok () {
        val out = all("""
            $Output0
            type List $D{} @{} = </List $D{} @{} @LOCAL>
            var l: /List $D{} @{} @LOCAL
            set l = Null: /List $D{} @{} @LOCAL
            var t1: [/List $D{} @{} @LOCAL]
            set t1 = [l]
            var t2: [/List $D{} @{} @LOCAL]
            set t2 = [t1.1]
            ${output0("/t2","/[/List $D{} @{} @LOCAL]@LOCAL")}
        """.trimIndent())
        assert(out == "[Null]\n") { out }
    }
    @Test
    fun j11_tup_copy_ok () {
        val out = all("""
            $Output0
            type List $D{} @{} = </List $D{} @{} @LOCAL>
            var l: /List $D{} @{} @LOCAL
            set l = new List $D{} @{} <.1 Null: /List $D{} @{} @LOCAL>:</List $D{} @{} @LOCAL>: @LOCAL
            var t1: [/List $D{} @{} @LOCAL]
            set t1 = [l]
            var t2: [/List $D{} @{} @LOCAL]
            set t2 = [t1.1]
            ${output0("/t2","/[/List $D{} @{} @LOCAL]@LOCAL")}
        """.trimIndent())
        assert(out == "[<.1 Null>]\n") { out }
    }
    @Test
    fun j14_tup_copy_ok () {
        val out = all("""
            $Output0
            type List $D{} @{} = </List $D{} @{} @LOCAL>
            var l1: /List $D{} @{} @LOCAL
            set l1 = Null: /List $D{} @{} @LOCAL
            var l2: /List $D{} @{} @LOCAL
            set l2 = l1
            ${output0("l2","/List $D{} @{} @LOCAL")}
        """.trimIndent())
        assert(out == "Null\n") { out }
    }
    @Test
    fun j15_tup_copy_ok () {
        val out = all("""
            $Output0
            type List $D{} @{} = </List $D{} @{} @LOCAL>
            var l1: /List $D{} @{} @LOCAL
            set l1 = new List $D{} @{} <.1 Null: /List $D{} @{} @LOCAL>:</List $D{} @{} @LOCAL>: @LOCAL
            var l2: /List $D{} @{} @LOCAL
            set l2 = l1
            ${output0("l2","/List $D{} @{} @LOCAL")}
        """.trimIndent())
        assert(out == "<.1 Null>\n") { out }
    }
    @Test
    fun j16_tup_move_copy_ok () {
        val out = all("""
            $Output0
            type List $D{} @{} = </List $D{} @{} @LOCAL>
            var l1: /List $D{} @{} @LOCAL
            set l1 = new List $D{} @{} <.1 Null: /List $D{} @{} @LOCAL>:</List $D{} @{} @LOCAL>: @LOCAL
            var l2: /List $D{} @{} @LOCAL
            set l2 = new List $D{} @{} <.1 l1>:</List $D{} @{} @LOCAL>: @LOCAL
            var t3: [(),/List $D{} @{} @LOCAL]
            set t3 = [(), new List $D{} @{} <.1 l2\ ~ !1>:</List $D{} @{} @LOCAL>: @LOCAL]
            ${output0("l1","/List $D{} @{} @LOCAL")}
            ${output0("/t3","/[(),/List $D{} @{} @LOCAL]@LOCAL")}
        """.trimIndent())
        assert(out == "<.1 Null>\n[(),<.1 <.1 Null>>]\n") { out }
    }
    @Test
    fun j18_tup_copy_rec_ok () {
        val out = all("""
            $Output0
            type List $D{} @{} = </List $D{} @{} @LOCAL>
            var l1: [/List $D{} @{} @LOCAL]
            set l1 = [new List $D{} @{} <.1 Null:/List $D{} @{} @LOCAL>:</List $D{} @{} @LOCAL>: @LOCAL]
            var l2: [/List $D{} @{} @LOCAL]
            set l2 = l1
            ${output0("/l2","/[/List $D{} @{} @LOCAL]@LOCAL")}
        """.trimIndent())
        assert(out == "[<.1 Null>]\n") { out }
    }
    @Test
    fun j19_consume_ok () {
        val out = all("""
            $Output0
            type List $D{} @{} = </List $D{} @{} @LOCAL>
            var x: /List $D{} @{} @LOCAL
            set x = new List $D{} @{} <.1 Null: /List $D{} @{} @LOCAL>:</List $D{} @{} @LOCAL>: @LOCAL
            set x = x
            ${output0("x","/List $D{} @{} @LOCAL")}
        """.trimIndent())
        assert(out == "<.1 Null>\n") { out }
    }
    @Test
    fun j20_consume_ok () {
        val out = all("""
            $Output0
            type List $D{} @{} = </List $D{} @{} @LOCAL>
            var x: /List $D{} @{} @LOCAL
            set x = new List $D{} @{} <.1 Null: /List $D{} @{} @LOCAL>:</List $D{} @{} @LOCAL>: @LOCAL
            var y: /List $D{} @{} @LOCAL
            if _1: _int {
                set y = x
            } else {
                set y = x
            }
            ${output0("y","/List $D{} @{} @LOCAL")}
        """.trimIndent())
        assert(out == "<.1 Null>\n") { out }
    }

    // SET - TUPLE - UNION

    @Test
    fun k01_set_tuple () {
        val out = all("""
            $Output0
            var xy: [_int,_int]; set xy = [_10:_int,_10:_int]
            set xy.1 = _20: _int
            var x: _int; set x = xy.1
            var y: _int; set y = xy.2
            var v: _int; set v = _(global.x+global.y): _int
            ${output0("v","_int")}
        """.trimIndent())
        assert(out == "30\n") { out }
    }

    // UNION SELF POINTER / HOLD

    @Test
    fun l04_ptr_null () {
        val out = all("""
            $Output0
            var n: _int
            set n = _10: _int
            var x: <(),/_int @LOCAL>
            set x = <.2 /n>: <(),/_int @LOCAL>
            ${output0("x!2\\","_int")}
        """.trimIndent())
        assert(out == "10\n") { out }
    }

    // ALLOC / SCOPE / NEWS

    @Test
    fun noclo_m01_scope_a () {
        val out = all("""
            $Output0
            type List $D{} @{a} = </List $D{} @{a} @a>
            { @A
                var pa: /List $D{} @{LOCAL} @LOCAL
                set pa = new List $D{} @{A} <.1 Null: /List $D{} @{A} @A>:</List $D{} @{A}@LOCAL>: @A
                var f: func $D{} @{}->()->()
                set f = func $D{} @{}-> ()->() {
                    var pf: /List $D{} @{A} @A
                    set pf = new List $D{} @{A} <.1 Null: /List $D{} @{A} @A>:</List $D{} @{A} @A>: @A
                    set pa\ ~ !1 = pf
                    --output std pa
                }
                call f $D{} @{} ()
                ${output0("pa","/List $D{} @{LOCAL} @LOCAL")}
            }
        """.trimIndent())
        assert(out == "<.1 <.1 Null>>\n") { out }
    }
    @Test
    fun m01_scope_f () {
        val out = all("""
            $Output0
            type List $D{} @{a} = </List $D{} @{a} @a>
            var f: func$D{} @{i1}-> /(List $D{} @{i1})@i1->()
            set f = func$D{} @{i1}-> /(List $D{} @{i1})@i1->() {
                var pf: /(List $D{} @{i1}) @i1
                set pf = arg
                ${output0("pf","/(List $D{} @{i1}) @i1")}
            }
            {
                var x: /(List $D{} @{LOCAL}) @LOCAL
                set x = new List $D{} @{LOCAL}<.1 Null: /(List $D{} @{LOCAL}) @LOCAL>:</List $D{} @{LOCAL} @LOCAL>: @LOCAL
                call f $D{} @{LOCAL} x
            }
        """.trimIndent())
        assert(out == "<.1 Null>\n") { out }
    }
    @Test
    fun m02_scope_f () {
        val out = all("""
            $Output0
            type List $D{} @{a} = </List $D{} @{a} @a>
            var f: func $D{} @{i1}->/(List $D{} @{i1})@i1->()
            set f = func$D{} @{i1}-> /(List $D{} @{i1})@i1->() {
                set arg\ ~  !1 = new List $D{} @{i1} <.1 Null:/(List $D{} @{i1})@i1>:</List $D{} @{i1} @i1>: @i1
            }
            {
                var x: /(List $D{} @{LOCAL}) @LOCAL
                set x = new List $D{} @{LOCAL} <.1 Null: /(List $D{} @{LOCAL}) @LOCAL>:</List $D{} @{LOCAL} @LOCAL>: @LOCAL
                call f $D{} @{LOCAL} x
                ${output0("x","/(List $D{} @{LOCAL}) @LOCAL")}
            }
        """.trimIndent())
        assert(out == "<.1 <.1 Null>>\n") { out }
    }
    @Test
    fun m03_scope_f () {
        val out = all("""
            $Output0
            type List $D{} @{a} = </List $D{} @{a} @a>
            var f: func $D{} @{a1,a2} -> [/(List $D{} @{a1})@a1,/(List $D{} @{a2})@a2]->()
            set f = func $D{} @{a1,a2}->[/(List $D{} @{a1})@a1,/(List $D{} @{a2})@a2]->() {
                set arg.1\ ~ !1 = new List $D{} @{a1} <.1 Null:/(List $D{} @{a1})@a1>:</List $D{} @{a1} @a1>: @a1
                set arg.2\ ~ !1 = new List $D{} @{a2}<.1 Null:/(List $D{} @{a2})@a2>:</List $D{} @{a2} @a2>: @a2
            }
            {
                var x: /(List $D{} @{LOCAL}) @LOCAL
                set x = new List $D{} @{LOCAL} <.1 Null: /(List $D{} @{LOCAL}) @LOCAL>:</List $D{} @{LOCAL} @LOCAL>: @LOCAL
                call f $D{} @{LOCAL,LOCAL} [x,x]
                ${output0("x","/(List $D{} @{LOCAL}) @LOCAL")}
            }
        """.trimIndent())
        assert(out == "<.1 <.1 Null>>\n") { out }
    }
    @Test
    fun m04_scope_f () {
        val out = all("""
            $Output0
            type List $D{} @{a} = </List $D{} @{a} @a>
            var f: func $D{} @{i1}->/(List $D{} @{i1})@i1->()
            set f = func $D{} @{i1}->/(List $D{} @{i1})@i1->() {
                set arg\ ~ !1 = new List $D{} @{i1} <.1 Null:/(List $D{} @{i1})@i1>:</List $D{} @{i1} @i1>: @i1
            }
            {
                var x: /(List $D{} @{LOCAL}) @LOCAL
                set x = new List $D{} @{LOCAL} <.1 Null: /(List $D{} @{LOCAL}) @LOCAL>:</List $D{} @{LOCAL} @LOCAL>: @LOCAL
                call f $D{} @{LOCAL} x
                ${output0("x","/(List $D{} @{LOCAL}) @LOCAL")}
            }
        """.trimIndent())
        assert(out == "<.1 <.1 Null>>\n") { out }
    }

    // FUNC / POOL

    @Test
    fun n01_pool () {
        val out = all("""
            $Output0
            var f : func $D{} @{a1} -> /()@a1 -> /()@a1
            ${output0("()","()")}
        """.trimIndent()
        )
        assert(out == "()\n") { out }
    }
    @Test
    fun n02_pool () {
        val out = all("""
            $Output0
            var f : func $D{} @{a1}->/()@a1 -> /()@a1
            set f = func $D{} @{a1}->/()@a1 -> /()@a1 {
                set ret = arg
            }
            ${output0("()","()")}
        """.trimIndent()
        )
        assert(out == "()\n") { out }
    }
    @Test
    fun n03_pool () {
        val out = all("""
            $Output0
            var f :func$D{} @{i1}-> /_int@i1 -> /()@i1
            set f = func$D{} @{i1}-> /_int@i1 -> /()@i1 {
                set ret = arg
            }
            var x: _int
            call f $D{} @{LOCAL} /x
            ${output0("()","()")}
        """.trimIndent()
        )
        assert(out == "()\n") { out }
    }
    @Test
    fun n04_pool () {
        val out = all("""
            $Output0
            var f : func$D{} @{i1}-> /_int@i1 -> /()@i1
            set f = func$D{} @{i1}-> /_int@i1 -> /()@i1 {
                set ret = arg
            }
            var g : func$D{} @{i1}-> /_int@i1 -> /()@i1
            set g = func$D{} @{i1}-> /_int@i1 -> /()@i1 {
                set ret = f $D{} @{i1} arg: @i1
            }
            var x: _int
            var px: /_int@LOCAL
            set px = f $D{} @{LOCAL} /x: @LOCAL
            ${output0("_(global.px == &global.x):_int","_int")}
        """.trimIndent()
        )
        assert(out == "1\n") { out }
    }
    @Test
    fun n05_pool_ff() {
        val out = all(
            """
            $Output0
            var f: ( func$D{} @{}-> () -> () )
            set f = func $D{} @{}->() -> () {
                set ret = arg
            }
            var g: func$D{} @{i1}-> [func$D{} @{}-> ()->(), ()] -> ()
            set g = func$D{} @{i1}-> [(func $D{} @{}->()->()), ()] -> () {
                set ret = arg.1 $D{} @{} arg.2
            }
            ${output0("g $D{} @{LOCAL} [f,()]","()")}
        """.trimIndent()
        )
        assert(out == "()\n") { out }
    }
    @Test
    fun n06_pool_ff() {
        val out = all(
            """
            $Output0
            var f: func$D{} @{i1}->/()@i1 -> /()@i1
            set f = func$D{} @{i1}-> /()@i1 -> /()@i1 {
                set ret = arg
            }
            var g: func$D{} @{i1}-> [ func$D{} @{i1}-> /()@i1->/()@i1, /()@i1] -> /()@i1
            set g = func $D{} @{i1}->[func$D{} @{i1}-> /()@i1->/()@i1, /()@i1] -> /()@i1 {
                set ret = arg.1 $D{} @{i1} arg.2: @i1
            }
            var x: ()
            ${output0("g $D{} @{LOCAL} [f,/x]: @LOCAL","/()@LOCAL")}
        """.trimIndent()
        )
        assert(out == "_\n") { out }
    }
    @Test
    fun noclo_n08_clo_int () {
        val out = all("""
            $Output0
            { @A
                var xxx: _int
                set xxx = _10:_int
                var f: (func $D{} @{}->()->_int)
                set f = func $D{} @{}->()->_int {
                    set ret = xxx
                }
                set xxx = _20:_int
                ${output0("xxx","_int")}
                set xxx = f $D{} @{} ()
                ${output0("xxx","_int")}
            }
        """.trimIndent())
        assert(out == "20\n20\n") { out }
    }
    @Test
    fun noclo_n13_pool_ups1 () {
        val out = all(
            """
            $Output0
            var f : func $D{} @{} -> _int -> ()
            set f = func $D{} @{} -> _int -> () { @A
                var x: _int
                set x = arg
                var g : func $D{} @{} -> () -> _int
                set g = func $D{} @{} -> () -> _int {
                    set ret = x
                }
                ${output0("g $D{} @{} ()","_int")}
            }
            call f $D{} @{} _10:_int
        """.trimIndent()
        )
        assert(out == "10\n") { out }
    }
    @Test
    fun noclo_n14_pool_ups2 () {
        val out = all(
            """
            $Output0
            var f : func $D{} @{} -> _int -> ()
            set f = func $D{} @{} -> _int -> () { @A
                var x: _int
                set x = arg
                var g : func$D{} @{} -> () -> _int
                set g = func $D{} @{} -> () -> _int {
                    var h : func $D{} @{} -> () -> _int
                    set h = func $D{} @{} -> () -> _int {
                        set ret = x
                    }
                    ${output0("h $D{} @{} ()","_int")}
                }
                call g $D{} @{} ()
            }
            call f $D{} @{} _10:_int
        """.trimIndent()
        )
        assert(out == "10\n") { out }
    }

    // TYPEDEF / ALIAS

    @Test
    fun o01_alias () {
        val out = all("""
            $Output0
        type Uxit $D{} @{} = ()
        var x: Uxit $D{} @{}
        set x = Uxit $D{} @{}() 
        ${output0("x","Uxit $D{} @{}")}
        """.trimIndent())
        assert(out == "()\n") { out }
    }
    @Test
    fun o02_alias () {
        val out = all("""
            $Output0
        type Pair $D{} @{}= [_int,_int]
        var x: Pair $D{} @{}
        set x = Pair $D{} @{} [_1:_int,_2:_int] 
        ${output0("/x","/Pair $D{} @{}@LOCAL")}
        """.trimIndent())
        assert(out == "[1,2]\n") { out }
    }
    @Test
    fun o03_alias () {
        val out = all("""
            $Output0
            type List $D{} @{} = </List $D{} @{} @LOCAL>
            var l: /List $D{} @{} @LOCAL
            set l = new List $D{} @{} <.1 Null:/List $D{} @{}@LOCAL>:</List $D{} @{} @LOCAL>: @LOCAL
            ${output0("l","/List $D{} @{} @LOCAL")}
        """.trimIndent())
        assert(out == "<.1 Null>\n") { out }
    }
    @Test
    fun o04_type_ptr () {
        val out = all("""
            $Output0
            type Num $D{} @{s} = </Num $D{} @{s} @s>
            type Ptr $D{} @{s} = /Num $D{} @{s} @s
            var n1: /Num $D{} @{LOCAL} @LOCAL
            set n1 = new Num $D{} @{LOCAL} <.1 Null:/Num $D{} @{LOCAL} @LOCAL>:</Num $D{} @{LOCAL} @LOCAL>
            var n2: Ptr $D{} @{LOCAL}
            set n2 = Ptr $D{} @{LOCAL} new Num $D{} @{LOCAL} <.1 Null:/Num $D{} @{LOCAL} @LOCAL>:</Num $D{} @{LOCAL} @LOCAL>
            ${output0("n1","/Num $D{} @{LOCAL} @LOCAL")}
            ${output0("n2","Ptr $D{} @{LOCAL}")}
        """.trimIndent())
        assert(out == "<.1 Null>\n<.1 Null>\n") { out }
    }
    @Test
    fun o05_type_ptr () {
        val out = all("""
            $Output0
            type List $D{} @{s} = /<List $D{} @{s}> @s
            var l: List $D{} @{LOCAL}
            set l = new <.1 Null:List $D{} @{LOCAL}>:<List $D{} @{LOCAL}>
            ${output0("l","List $D{} @{LOCAL}")}
        """.trimIndent())
        //assert(out == "<.1 Null>\n") { out }
        assert(out == "(ln 4, col 22): invalid type : expected pointer to alias type") { out }
    }

    @Test
    fun o07_type_pln () {
        val out = all("""
            $Output0
            type Int  $D{} @{} = _int
            type Pair $D{} @{} = [Int$D{} @{},Int$D{} @{}]
            var x: Int $D{} @{}
            set x = _10:_int
            var xs: Pair $D{} @{}
            set xs = Pair $D{} @{} [x,x]
            ${output0("/xs","/Pair $D{} @{} @LOCAL")}
        """.trimIndent())
        assert(out == "[10,10]\n") { out }
    }
    @Test
    fun o08_type_pln () {
        val out = all("""
            $Output0
            type Int  $D{} @{} = _int
            type Pair $D{} @{} = <Int$D{} @{},Int$D{} @{}>
            var x: Int $D{} @{}
            set x = _10:_int
            var xs: Pair $D{} @{}
            set xs = Pair$D{} @{}<.1 x>:<Int$D{} @{},Int$D{} @{}>
            ${output0("/xs","/Pair $D{} @{} @LOCAL")}
        """.trimIndent())
        assert(out == "<.1 10>\n") { out }
    }
    @Test
    fun o09_type_pln () {
        val out = all("""
            $Output0
            type T1 @{} = [()]
            type T2 @{} = [T1]
            ${output0("()","()")}
        """.trimIndent())
        //assert(out == "(ln 1, col 6): invalid type identifier") { out }
        assert(out == "(ln 2, col 6): expected type identifier : have \"T1\"") { out }
    }
    @Test
    fun o10_type_pln () {
        val out = all("""
            $Output0
            type Tx1 $D{} @{} = [()]
            type Tx2 $D{} @{} = [Tx1 $D{} @{}]
            ${output0("()","()")}
        """.trimIndent())
        assert(out == "()\n") { out }
    }
    @Test
    fun o12_rect () {
        val out = all("""
            $Output0
            type Point $D{} @{} = [_int,_int]
            type Dims $D{} @{} = [_int,_int]
            type Rect $D{} @{} = [Point$D{} @{},Dims$D{} @{}]
            var rect: Rect $D{} @{}
            set rect = Rect$D{} @{}[Point $D{} @{}[(_10: _int),(_10: _int)],Dims$D{} @{}[(_5: _int),(_5: _int)]]
            ${output0("/rect","/Rect $D{} @{} @LOCAL")}
        """.trimIndent())
        assert(out == "[[10,10],[5,5]]\n") { out }
    }
    @Test
    fun o13_rect_dot () {
        val out = all("""
            $Output0
            type Point $D{} @{} = [_int,_int]
            type Dims $D{} @{} = [_int,_int]
            type Rect $D{}@{} = [Point$D{} @{},Dims$D{} @{}]
            var rect: Rect $D{} @{}
            set rect = Rect $D{} @{}[Point$D{} @{}[(_10: _int),(_10: _int)],Dims$D{} @{}[(_5: _int),(_5: _int)]]
            ${output0("rect  ~ .2 ~  .1","_int")}
        """.trimIndent())
        assert(out == "5\n") { out }
    }
    @Test
    fun o13_func_alias () {
        val out = all("""
            $Output0
            type Int2Int $D{}@{} = func $D{} @{} -> _int -> _int
            
            var f: Int2Int $D{}@{}
            set f = Int2Int $D{}@{} func $D{} @{} -> _int -> _int {
                set ret = arg
            }
            
            var x: _int
            set x = f~ $D{} @{} _10:_int
            
            ${output0("x","_int")}
       """.trimIndent())
        assert(out == "10\n") { out }
    }
    @Test
    fun o14_func_alias () {
        val out = all("""
            $Output0
            type Xask $D{} @{} = task $D{} @{} -> () -> () -> ()
            var t: Xask $D{} @{}
            set t = Xask$D{} @{}(task $D{} @{} -> () -> () -> () {
            })
            ${output0("()","()")}
       """.trimIndent())
        assert(out == "()\n") { out }
    }
    @Test
    fun o15_func_alias () {
        val out = all("""
            $Output0
            type Xask $D{} @{} = task $D{} @{} -> () -> _int -> ()
            var t: Xask $D{} @{}
            set t = Xask $D{} @{}(task $D{} @{} -> () -> _int -> () {
                ${output0("_2:_int","_int")}
                set pub = _10:_int
            }
            )
            ${output0("(_1: _int)","_int")}
            var x: active Xask $D{} @{}
            set x = spawn active Xask $D{} @{}((t ~ ) $D{} @{} ())
            var y: active task $D{} @{} -> () -> _int -> ()
            set y = spawn ((t ~ ) $D{} @{} ())
            ${output0("((x ~ ).pub)","_int")}
            ${output0("(_3: _int)","_int")}
       """.trimIndent())
        assert(out == "1\n2\n2\n10\n3\n") { out }
    }
    @Test
    fun o16_unit_alias () {
        val out = all("""
            $Output0
            type Unit $D{} @{} = ()
            var u: Unit $D{} @{}
            set u = Unit $D{} @{} ()
            ${output0("u~","()")}
       """.trimIndent())
        assert(out == "()\n") { out }
    }

    // TYPE / HIER

    @Test
    fun p01_type_hier () {
        val out = all("""
            $Output0
        type Point $D{} @{}= [_int,_int]
        type Event $D{} @{}= <
            _int,
            (),
            <_int,_int>,    -- Key.Up/Down
            <               -- Mouse
                [Point$D{} @{}],        -- Motion
                <               -- Button 
                    [Point$D{} @{},_int],   -- Up
                    [Point$D{} @{},_int]    -- Down
                >
            >
        >

        var e: Event $D{} @{}
        set e = Event$D{} @{} <.4 <.2 <.1 [Point$D{} @{} [_10:_int,_10:_int],_1:_int]>:<[Point$D{} @{},_int],[Point$D{} @{},_int]>>:<[Point$D{} @{}],<[Point$D{} @{},_int],[Point$D{} @{},_int]>>>:<_int,(),<_int,_int>,<[Point$D{} @{}],<[Point$D{} @{},_int],[Point$D{} @{},_int]>>>
        ${output0("/e","/Event $D{} @{} @LOCAL")}
       """.trimIndent())
        assert(out == "<.4 <.2 <.1 [[10,10],1]>>>\n") { out }
    }
    @Test
    fun p03_type_hier_sub_ok () {
        val out = test(false, """
            $Output0
            type Hier $D{} @{} = <[_int],<<[_int],[_int]>,[_int]>>
            var h: Hier $D{} @{}
            set h = Hier.2.1.2 $D{} @{} [_10:_int]
            ${output0("h~?2","_int")}
            ${output0("h~?2?1","_int")}
            ${output0("h~?2?1?1","_int")}
            ${output0("h~?2?1?2","_int")}
            ${output0("h~!2!1!2.1","_int")}
           """.trimIndent()
        )
        assert(out == "1\n1\n0\n1\n10\n") { out }
    }
    @Test
    fun p04_type_hier () {
        val out = test(false, """
            $Output0
            type Button $D{} @{} = <<[_int,_int,(),_int]>, [_int]> -- Up/Down
            var e: Button $D{} @{}
            set e = Button $D{} @{}
                <.1
                    <.1
                        [_1:_int,_2:_int,(),_4:_int]
                    >: <[_int,_int,(),_int]>
                >: <<[_int,_int,(),_int]>,[_int]>
            ${output0("/e","/Button $D{} @{} @LOCAL")}
           """.trimIndent()
        )
        assert(out == "<.1 <.1 [1,2,(),4]>>\n") { out }
    }

    @Test
    fun q01_type_hier () {
        val out = test(false, """
            $Output0
            type Button $D{} @{} = <_int,_int> -- Up/Down
            var e: Button $D{} @{}
            set e = Button $D{} @{} <.2 _10:_int>:<_int,_int>
            ${output0("/e","/Button $D{} @{} @LOCAL")}
           """.trimIndent()
        )
        assert(out == "<.2 10>\n") { out }
    }
    @Test
    fun q07_type_err () {
        val out = test(false, """
            $Output0
            type Button $D{} @{} = <(),()> -- Up/Down
            var e: Button $D{} @{}
            set e = Button $D{} @{} <.2 ()>:<(),()>
            ${output0("/e","/Button $D{} @{} @LOCAL")}
            ${output0("e~!0","_int")}
           """.trimIndent()
        )
        assert(out == "(ln 6, col 30): invalid discriminator : out of bounds") { out }
    }
    @Test
    fun q09_type_hier_sub () {
        val out = test(false, """
            $Output0
            type Button $D{} @{} = <(),()> -- Up/Down
            var dn: Button.2 $D{} @{}
            set dn = Button.2 $D{} @{} ()
            ${output0("/dn","/Button.2 $D{} @{}@LOCAL")}
           """.trimIndent()
        )
        assert(out == "<.2>\n") { out }
    }
    @Test
    fun q10_type_hier_sub_err () {
        val out = test(false, """
            type Button $D{} @{} = <(),()> -- Up/Down
            var dn: Button.2 $D{} @{}
            set dn = Button $D{} @{} <.2 ()>:<(),()>
           """.trimIndent()
        )
        assert(out == "(ln 3, col 8): invalid assignment : type mismatch :\n    Button.2 $D{} @{}\n    Button $D{} @{}") { out }
    }
    @Test
    fun q11_type_hier_sub_err () {
        val out = test(false, """
            type Button $D{} @{} = <(),()> -- Up/Down
            var dn: Button.2 $D{} @{}
            set dn = Button.1 $D{} @{} ()
           """.trimIndent()
        )
        assert(out == "(ln 3, col 8): invalid assignment : type mismatch :\n    Button.2 $D{} @{}\n    Button.1 $D{} @{}") { out }
    }

    // IF / EXPR

    @Test
    fun r01_if_ok () {
        val out = test(false, """
            $Output0
            ${output0("if _0 {_999:_int} else {_1:_int}","_int")}
           """.trimIndent()
        )
        //assert(out == "(ln 4, col 16): expected \"?\" : have end of file") { out }
        assert(out == "1\n") { out }
    }

    @Test
    fun r01_if_err () {
        val out = test(false, """
            var x: ()
            set x = if _0 {()} else {[()]}
           """.trimIndent()
        )
        //assert(out == "(ln 4, col 16): expected \"?\" : have end of file") { out }
        assert(out == "(ln 1, col 5): invalid \"if\" : type mismatch :\n    ()\n    [()]") { out }
    }

    // PARAMETRIC TYPES / GENERICS

    @Test
    fun s01_maybe () {
        val out = test(false, """
            $Output0
            type Maybe $D{a} @{} = <(), ${D}a>
            var x: Maybe $D{_int} @{}
            set x = Maybe $D{_int} @{} <.2 _10:_int>: <(),_int>
            ${output0("x~!2","_int")}
           """.trimIndent()
        )
        assert(out == "10\n") { out }
    }
    @Test
    fun s02_maybe_err () {
        val out = test(false, """
            type Maybe $D{a} @{} = <(), ${D}a>
            var x: Maybe $D{_int} @{}
            set x = Maybe $D{} @{} <.2 _10:_int>: <(),_int>  -- ERR: missing instance
           """.trimIndent()
        )
        assert(out == "(ln 3, col 9): invalid type instantiation : parameters mismatch") { out }
    }
    @Test
    fun s03_maybe_err () {
        val out = test(false, """
            type Maybe $D{a} @{} = <(), ${D}a>
            var x: Maybe $D{()} @{}
            set x = Maybe $D{[()]} @{} <.2 [()]>: <(),[()]>  -- ERR: incompatible instance
           """.trimIndent()
        )
        assert(out == "(ln 3, col 7): invalid assignment : type mismatch :\n    Maybe $D{()} @{}\n    Maybe $D{[()]} @{}") { out }
    }
    @Test
    fun s04_maybe_err () {
        val out = test(false, """
            type Maybe $D{a} @{} = <(), ${D}a>
            var x: Maybe $D{()} @{}
            set x = Maybe $D{()} @{} <.2 [()]>: <(),[()]>   -- ERR: incompatible cons
           """.trimIndent()
        )
        assert(out == "(ln 3, col 9): invalid type pack : type mismatch :\n    <(),()>\n    <(),[()]>") { out }
    }
    @Test
    fun s05_maybe_err () {
        val out = test(false, """
            type Maybe $D{a} @{} = <(), ${D}a>
            var x: Maybe $D{[()]} @{}
            var y: Maybe $D{()} @{}
            set x = y   -- ERR: incompatible instances
           """.trimIndent()
        )
        assert(out == "(ln 4, col 7): invalid assignment : type mismatch :\n    Maybe $D{[()]} @{}\n    Maybe $D{()} @{}") { out }
    }
    @Test
    fun s06_maybe_concrete () {
        val out = test(false, """
            $Output0
            type Maybe1 $D{} @{} = <(), _int>
            type Maybe2 $D{} @{} = <(), _int>
            var x: Maybe1 $D{} @{}
            set x = Maybe1 $D{} @{} <.2 _10:_int>: <(),_int>
            ${output0("x~!2","_int")}
            var y: Maybe2 $D{} @{}
            set y = Maybe2 $D{} @{} <.2 _10:_int>: <(),_int>
            ${output0("y~!2","_int")}
           """.trimIndent()
        )
        assert(out == "10\n10\n") { out }
    }
    @Test
    fun s06_maybe_twice_dif () {
        val out = test(false, """
            $Output0
            type Maybe $D{a} @{} = <(), ${D}a>
            var x : Maybe $D{_int} @{}
            set x = Maybe $D{_int} @{} <.2 _10:_int>: <(),_int>
            var y : Maybe $D{()} @{}
            set y = Maybe $D{()} @{} <.2 ()>: <(),()>
            ${output0("x~!2","_int")}
            ${output0("y~!2","()")}
           """.trimIndent()
        )
        assert(out == "10\n()\n") { out }
    }
    @Test
    fun s07_maybe_twice_equ () {
        val out = test(false, """
            $Output0
            type Maybe $D{a} @{} = <(), ${D}a>
            var x: Maybe $D{()} @{}
            set x = Maybe $D{()} @{} <.2 ()>: <(),()>
            var y: Maybe $D{()} @{}
            set y = Maybe $D{()} @{} <.2 ()>: <(),()>
            ${output0("x~!2","()")}
            ${output0("y~!2","()")}
           """.trimIndent()
        )
        assert(out == "()\n()\n") { out }
    }

    // PARAMETRIC VALS / FUNCS / GENERICS

    @Test
    fun t01_err () {
        val out = test(false, """
            var min : ${D}a
            var min : ${D}a
           """.trimIndent()
        )
        assert(out == "(ln 2, col 5): invalid declaration : \"min\" is already declared (ln 1)") { out }
    }
    @Test
    fun t021_ok () {
        val out = test(false, """
            $Output0
            var min : ${D}a
            var min : ()
            var min : [()]
            var x: [()]
            set x = min::[()]
            ${output0("x.1","()")}
           """.trimIndent()
        )
        assert(out == "()\n") { out }
    }
    @Test
    fun t022_ok () {
        val out = test(false, """
            $Output0
            var min : ${D}a
            var min : ()
            var min : [()]
            var x: ()
            set x = min::[()].1
            ${output0("x","()")}
           """.trimIndent()
        )
        assert(out == "()\n") { out }
    }
    @Test
    fun t03_err () {
        val out = test(false, """
            var min : ()
            var min : [()]
           """.trimIndent()
        )
        assert(out == "(ln 2, col 5): invalid declaration : \"min\" is already declared (ln 1)") { out }
    }
    @Test
    fun t04_err () {
        val out = test(false, """
            var min : ${D}a
            var min : ()
            var min : ()
           """.trimIndent()
        )
        assert(out == "(ln 3, col 5): invalid declaration : \"min\" is already declared (ln 2)") { out }
    }
    @Test
    fun t05_ok () {
        val out = test(false, """
            $Output0
            var min : ${D}a
            var min : ()
            set min = ()
            var x: ()
            set x = min::()
            ${output0("x","()")}
           """.trimIndent()
        )
        assert(out == "()\n") { out }
    }
    @Test
    fun t06_no () {
        val out = test(false, """
            var min : ${D}a
            var min : ()
            var x: [()]
            set x = min::[()]
           """.trimIndent()
        )
        assert(out == "(ln 4, col 9): undeclared variable \"min\"") { out }
    }
    @Test
    fun t02_f () {
        val out = test(false, """
            $Output0
            var f : func $D{a}  @{} -> ${D}a -> ()
            var f : func $D{()} @{} ->   ()  -> ()
            set f = func $D{()} @{} ->   ()  -> () {
                set ret = ()
            }
            ${output0("f $D{} @{} ()","()")}
           """.trimIndent()
        )
        assert(out == "()\n") { out }
    }
    @Test
    fun t03_f () {
        val out = test(false, """
            $Output0
            var f : func $D{a}  @{} -> ${D}a -> ()
            var f : func $D{()} @{} ->  [()] -> ()
            set f = func $D{()} @{} ->  [()] -> () {
                set ret = arg.1
            }
            var f : func $D{()} @{} ->   ()  -> ()
            set f = func $D{()} @{} ->   ()  -> () {
                set ret = arg
            }
            ${output0("f $D{} @{} [()]","()")}
            ${output0("f $D{} @{} ()","()")}
           """.trimIndent()
        )
        assert(out == "()\n()\n") { out }
    }

    // ALL

    @Test
    fun z01 () {
        val out = all("""
            $Output0
            var inv : (func $D{} @{}-> <(),()> -> <(),()>)
            set inv = func$D{} @{}-> <(),()> -> <(),()> {
                if arg?1 {
                    set ret = <.2()>:<(),()>
                } else {
                    set ret = <.1()>:<(),()>
                }
            }
            var a: <(),()>
            set a = <.2()>: <(),()>
            var b: <(),()>
            set b = inv $D{} @{} a
            ${output0("/b","/<(),()>@LOCAL")}
        """.trimIndent())
        assert(out == "<.1>\n") { out }
    }
    @Test
    fun z02 () {
        val out = all("""
            $Output0
        type Error $D{} @{} = <_int>
        var i: _int; set i = _1: _int
        var n: _int; set n = _0: _int
        $func0 {
            ${catch0(1)} {
                loop {
                    set n = _(global.n + global.i): _int
                    set i = _(global.i + 1): _int
                    if _(global.i > 5): _int {
                        ${throw0(1)}
                    } else {}
                }
            }
        } $D{} @{} ()
        ${output0("n","_int")}
        """.trimIndent())
        assert(out == "15\n") { out }
    }
    @Test
    fun z03 () {
        val out = all("""
            $Output0
        native _{}
        ${output0("()","()")}
        """.trimIndent())
        assert(out == "()\n")
    }
    @Test
    fun z04_if_bool () {
        val out = all("""
            $Output0
        if _0: _int {
        } else {
            ${output0("()","()")}
        }
        if _1: _int {
            ${output0("()","()")}
        } else {
        }
        """.trimIndent())
        assert(out == "()\n()\n")
    }
    @Test
    fun z05_func_rec () {
        val out = all("""
            $Output0
        var i: _int; set i = _0: _int
        var f: func$D{} @{}-> ()->()
        set f = func $D{} @{}->()->() {
            if _(global.i == 10): _int {
                
            } else {
                set i = _(global.i + 1): _int
                set ret = f $D{} @{} ()
            }
        }
        call f $D{} @{} ()
        ${output0("i","_int")}
        """.trimIndent())
        assert(out == "10\n") { out }
    }
    @Test
    fun z06_type_complex () {
        val out = all("""
            $Output0
            type List $D{}@{a} = <[(),/List$D{}@{a}@a]>
            var x: /(List $D{}@{LOCAL}) @LOCAL
            set x = new List $D{}@{LOCAL} <.1 [(),Null: /(List$D{} @{LOCAL})@LOCAL]>:<[(),/List$D{}@{LOCAL}@LOCAL]>: @LOCAL
            var y: [(),/(List $D{}@{LOCAL}) @LOCAL]
            set y = [(), new List $D{}@{LOCAL} <.1 [(),Null: /(List $D{}@{LOCAL})@LOCAL]>:<[(),/List$D{}@{LOCAL}@LOCAL]>: @LOCAL]
            var z: [(),//(List $D{}@{LOCAL}) @LOCAL @LOCAL]
            set z = [(), /x]
            ${output0("z.2\\\\ ~ !1.2","/(List $D{}@{LOCAL}) @LOCAL")}
        """.trimIndent())
        assert(out == "Null\n") { out }
    }
    @Test
    fun z07_type_complex_bug () {
        val out = all("""
            $Output0
            type List $D{}@{a} = <[(),/List$D{}@{a}@a]>
            var x: /(List $D{}@{LOCAL}) @LOCAL
            set x = Null: /(List $D{}@{LOCAL}) @LOCAL
            var z: [(),//(List $D{}@{LOCAL}) @LOCAL @LOCAL]
            set z = [(), /x]
            ${output0("z.2\\","/(List $D{}@{LOCAL}) @LOCAL")}
        """.trimIndent())
        assert(out == "Null\n") { out }
    }
    @Test
    fun z07_type_complex () {
        val out = all("""
            $Output0
            type List $D{}@{a} = <[(),/List$D{}@{a}@a]>
            var x: /(List $D{}@{LOCAL}) @LOCAL
            set x = Null: /(List $D{}@{LOCAL}) @LOCAL
            var z: [(),//(List $D{}@{LOCAL}) @LOCAL @LOCAL]
            set z = [(), /x]
            ${output0("z.2\\","/(List $D{}@{LOCAL}) @LOCAL")}
        """.trimIndent())
        assert(out == "Null\n") { out }
    }
    @Test
    fun zxx_type_complex () {
        val out = all("""
            $Output0
            type List $D{} @{} = </List $D{} @{} @LOCAL>
            var z: /List $D{} @{} @LOCAL
            set z = Null: /List $D{} @{} @LOCAL
            ${output0("z","/List $D{} @{} @LOCAL")}
        """.trimIndent())
        assert(out == "Null\n") { out }
    }
    @Test
    fun z08_type_complex () {
        val out = all("""
            $Output0
            type List $D{}@{a} = <[(),/List$D{}@{a}@a]>
            var y : [(),/(List $D{}@{LOCAL}) @LOCAL]
            set y = [(), new List$D{} @{LOCAL}<.1 [(),Null:/(List $D{}@{LOCAL}) @LOCAL]>:<[(),/List$D{}@{LOCAL}@LOCAL]>: @LOCAL]
            ${output0("/y","/[(),/(List $D{}@{LOCAL}) @LOCAL]@LOCAL")}
        """.trimIndent())
        assert(out == "[(),<.1 [(),Null]>]\n") { out }
    }
    @Test
    fun z08_func_arg () {
        val out = all("""
            $Output0
            type List $D{}@{a} = </List $D{}@{a} @a>
            var x1: /(List $D{}@{LOCAL}) @LOCAL
            set x1 = Null: /(List $D{}@{LOCAL}) @LOCAL
            var z1: _int
            set z1 = x1\~?Null
            var x2: //(List $D{}@{LOCAL}) @LOCAL @LOCAL
            set x2 = /x1
            var z2: _int
            set z2 = x2\\~?1
            set x2\ = new List$D{} @{LOCAL}<.1 Null: /(List $D{}@{LOCAL}) @LOCAL>:</List $D{}@{LOCAL} @LOCAL>: @LOCAL
            var f: func$D{} @{i1}-> //(List $D{}@{i1})@i1@i1->_int
            set f = func$D{} @{i1}-> //(List $D{}@{i1})@i1@i1->_int {
                set ret = arg\\~?1
            }
            var z3: _int
            set z3 = f $D{} @{LOCAL} x2
            var xxx: _int
            set xxx = _(global.z1 + global.z2 + global.z3): _int
            ${output0("xxx","_int")}
        """.trimIndent())
        assert(out == "2\n") { out }
    }
    @Test
    fun z08_func_alt () {
        val out = all("""
            $Output0
            type List $D{} @{} = </List $D{} @{} @LOCAL>
            var x1: /List $D{} @{} @LOCAL
            set x1 = Null: /List $D{} @{} @LOCAL
            var x2: //List $D{} @{} @LOCAL @LOCAL
            set x2 = /x1
            var y2: _int
            set y2 = x2\\~?1
            ${output0("y2","_int")}
        """.trimIndent())
        assert(out == "0\n") { out }
    }
    @Test
    fun z09_output_string () {
        val out = all("""
            $Output0
            type List $D{}@{a} = <[_int,/List $D{}@{a} @a]>
            var f: (func $D{} @{}->  ()->() )
            set f = func $D{} @{}->()->() {
                var s1: /(List $D{}@{LOCAL}) @LOCAL
                set s1 = new List $D{}@{LOCAL} <.1 [_1:_int,Null: /(List $D{}@{LOCAL}) @LOCAL]>:<[_int,/List $D{}@{LOCAL} @LOCAL]>: @LOCAL
                ${output0("s1","/(List $D{}@{LOCAL}) @LOCAL")}
            }
            call f $D{} @{}()
        """.trimIndent())
        assert(out == "<.1 [1,Null]>\n") { out }
    }
    @Test
    fun z10_output_string () {
        val out = all("""
            $Output0
            type List $D{} @{a} = <[_int,/List $D{} @{a} @a]>
            var f: func$D{} @{}-> ()->()
            set f = func $D{} @{}->()->() {
                var s1: /(List $D{} @{LOCAL}) @LOCAL
                set s1 = new List $D{} @{LOCAL}<.1 [_1:_int,Null: /(List $D{} @{LOCAL}) @LOCAL]>:<[_int,/List $D{} @{LOCAL} @LOCAL]>: @LOCAL
                ${output0("s1","/(List $D{}@{LOCAL}) @LOCAL")}
            }
            call f $D{} @{}()
        """.trimIndent())
        assert(out == "<.1 [1,Null]>\n") { out }
    }
    @Test
    fun z10_return_move () {
        val out = all("""
            $Output0
            type List $D{} @{a} = <[_int,/List $D{} @{a} @a]>
            var f: func$D{} @{i}-> ()-><(),_int,/(List $D{} @{i})@i>
            set f = func$D{} @{i}-> ()-><(),_int,/(List $D{} @{i})@i> {
                var str: /(List $D{} @{i}) @i
                set str = Null: /(List $D{} @{i}) @i
                set ret = <.3 str>:<(),_int,/(List $D{} @{i})@i>
            }
            var x: <(),_int,/(List $D{} @{LOCAL})@LOCAL>
            set x = f $D{} @{LOCAL} ()
            ${output0("x!3","/(List $D{}@{LOCAL}) @LOCAL")}
        """.trimIndent())
        assert(out == "Null\n") { out }
    }
    @Test
    fun z11_func_err () {
        val out = all("""
            var f: func $D{} @{}->()->[()]
            set f = func $D{} @{}->()->() {
            }
        """.trimIndent())
        assert(out.startsWith("(ln 2, col 7): invalid assignment : type mismatch")) { out }
    }
    @Test
    fun z12_union_tuple () {
        val out = all("""
            $Output0
            type List $D{} @{a} = <[_int,/List $D{} @{a} @a]>
            var tk2: <(),_int,/(List $D{} @{LOCAL}) @LOCAL>
            set tk2 = <.3 Null:/(List $D{} @{LOCAL}) @LOCAL>: <(),_int,/(List $D{} @{LOCAL}) @LOCAL>
            var s21: /<(),_int,/(List $D{} @{LOCAL}) @LOCAL> @LOCAL
            set s21 = /tk2
            ${output0("s21\\!3","/(List $D{}@{LOCAL}) @LOCAL")}
        """.trimIndent())
        assert(out == "Null\n") { out }
    }
    @Test
    fun z15_acc_move_sub () {
        val out = all("""
            $Output0
            type List $D{} @{} = <(),/List $D{} @{}@LOCAL>
            var x: /List $D{} @{} @LOCAL
            set x = new List $D{} @{} <.2 new List $D{} @{} <.1()>:<(),/List $D{} @{}@LOCAL>: @LOCAL>:<(),/List $D{} @{}@LOCAL>: @LOCAL
            var y: /List $D{} @{} @LOCAL
            set y = x\~!2
            ${output0("x","/List $D{} @{} @LOCAL")}
            ${output0("y","/List $D{} @{} @LOCAL")}
        """.trimIndent())
        assert(out == "<.2 <.1>>\n<.1>\n") { out }
    }
    @Test
    fun z16_rec () {
        val out = all(
            """
            $Output0
            var frec : func $D{} @{}->_int->_int
            set frec = func $D{} @{}->_int->_int {
                --output std arg
                if _(${D}arg == 1):_int {
                    set ret = _1:_int
                } else {
                    var tmp: _int
                    set tmp = frec $D{} @{} _(${D}arg-1):_int
                    --output std arg
                    --output std tmp
                    set ret = _(${D}arg + ${D}tmp):_int
                }
            }
            ${output0("(frec $D{} @{} _5:_int)","_int")}
        """.trimIndent()
        )
        assert(out == "15\n") { out }
    }
    @Test
    fun z17_include () {
        val out = test(false, """
            $Output0
            type Error $D{} @{} = <_int>
            ^"test-func-0.ceu"
            call g $D{} @{} ()
            """.trimIndent()
        )
        assert(out == "()\n") { out }
    }
    @Test
    fun z18_throw () {
        val out = all(
            """
            $Output0
            type Error $D{} @{} = <_int>
            var g : func $D{} @{}-> () -> ()
            set g = func $D{} @{}-> () -> () {
            }
            var f: func $D{} @{a1,b1} -> [()] -> ()
            set f = func $D{} @{a1,b1} -> [()] -> () {
                ${catch0(1)} {
                    call g $D{} @{} ()
                    ${throw0(1)}
                }
            }
            call f $D{} @{LOCAL,LOCAL} [()]
            ${output0("()","()")}
        """.trimIndent()
        )
        assert(out == "()\n") { out }
    }
}
