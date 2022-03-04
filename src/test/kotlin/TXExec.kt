import org.junit.jupiter.api.MethodOrderer.Alphanumeric
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import java.io.File
import java.io.PushbackReader
import java.io.StringReader

@TestMethodOrder(Alphanumeric::class)
class TXExec {

    fun all (inp: String): String {
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
    fun a02_int_abs () {
        val out = all("""
            var x: _int
            set x = _abs _(-1)
            output std x
        """.trimIndent())
        assert(out == "1\n") { out }
    }
    @Test
    fun a03_tuple () {
        val out = all("""
            var v = [(),()]
            output std /v
        """.trimIndent())
        assert(out == "[(),()]\n") { out }
    }
    @Test
    fun a04_tuples () {
        val out = all("""
            var v = [(),()]
            var x = [(),v]
            var y = x.2
            var z = y.2
            output std z
            output std /x
        """.trimIndent())
        assert(out == "()\n[(),[(),()]]\n") { out }
    }
    @Test
    fun a05_nat () {
        val out = all("""
            var y: _(char*) = _{"hello"}
            var n: _{int} = _10
            var x = [n,y]
            output std /x
        """.trimIndent())
        assert(out == "[10,\"hello\"]\n") { out }
    }
    @Test
    fun a06_call () {
        val out = all("""
            var f = func _int -> _int {
                return arg
            }
            var x = f _10
            output std x
        """.trimIndent())
        assert(out == "10\n") { out }
    }
    @Test
    fun a07_call_fg () {
        val out = all("""
            var f = func ()->() {
                var x = _10:_int
                output std x
            }
            var g = func ()->() {
                return f ()
            }
            call g ()
        """.trimIndent())
        assert(out == "10\n") { out }
    }
    @Test
    fun a08_union () {
        val out = all("""
            var a = <.1>:<(),()>
            var b : <(),()> = <.2>
            output std /a
            output std /b
        """.trimIndent())
        assert(out == "<.1>\n<.2>\n") { out }
    }
    @Test
    fun a09_func_if () {
        val out = all("""
        var inv = func <(),()> -> <(),()> {
            if arg?1 {
                return <.2>
            } else {
                return <.1>
            }
        }
        var a: <(),()> = <.2>
        var b = inv a
        output std /b
        """.trimIndent())
        assert(out == "<.1>\n") { out }
    }
    @Test
    fun a10_loop () {
        val out = all("""
        var i: _int = _1
        var n = _0: _int
        loop {
            set n = _(${D}n + ${D}i)
            set i = _(${D}i + 1)
            if _(${D}i > 5) {
                break
            }
        }
        output std n
        """.trimIndent())
        assert(out == "15\n") { out }
    }
    @Test
    fun a11_unions () {
        val out = all("""
            var z = <.1()>:<()>
            var y : <<()>> = <.1 z>
            var x = <.1 y>:<<<()>>>
            var yy: <<()>> = x!1
            var zz = yy!1
            output std /zz
        """.trimIndent())
        assert(out == "<.1>\n") { out }
    }
    @Test
    fun a12_tuple_nat () {
        val out = all("""
            var s: [_int,_int,_int,_int] = [_1,_2,_3,_4]
            output std /s
        """.trimIndent())
        assert(out == "[1,2,3,4]\n") { out }
    }
    @Test
    fun a13_union_nat () {
        val out = all("""
            var s: <[_int,_int,_int,_int],_int,_int> = <.1 [_1,_2,_3,_4]>
            output std /s
        """.trimIndent())
        assert(out == "<.1 [1,2,3,4]>\n") { out }
    }
    @Test
    fun a14_pred () {
        val out = all("""
            var z = <.1>: <()>
            output std z?1
        """.trimIndent())
        assert(out == "1\n") { out }
    }
    @Test
    fun a15_disc () {
        val out = all("""
            var z: <(),()> = <.2>
            output std z!2
        """.trimIndent())
        assert(out == "()\n") { out }
    }
    @Test
    fun a16_dots () {
        val out = all("""
            var x: <<<()>>> = <.1 <.1 <.1>>>
            output std x!1!1!1
        """.trimIndent())
        assert(out == "()\n") { out }
    }
    @Test
    fun a17_if () {
        val out = all("""
            var x: <(),()> = <.2>
            if x?2 { output std () } else { }
        """.trimIndent())
        assert(out == "()\n") { out }
    }
    @Test
    fun a18_loop () {
        val out = all("""
        loop {
           break
        }
        output std ()
        """.trimIndent())
        assert(out == "()\n")
    }
    @Test
    fun a19_ptr () {
        val out = all("""
            var y: _int = _10
            var x = /y
            output std x\
        """.trimIndent())
        assert(out == "10\n") { out }
    }
    @Test
    fun i05_ptr_block_err () {
        val out = all("""
            var p1: /_int
            var p2: /_int
            {
                var v: _int = _10: _int
                set p1 = /v  -- no
            }
            output std p1\
        """.trimIndent())
        assert(out.startsWith("(ln 5, col 12): invalid assignment : type mismatch")) { out }
    }

    // old disabled

    @Test
    fun b09_union () {
        val out = all("""
            type List = </List>
            var x: /List = <.0>
            var y: <//List> = <.1 /x>
            output std /y
            output std y!1\
        """.trimIndent())
        //assert(out == "(ln 3, col 7): invalid assignment of \"x\" : borrowed in line 2") { out }
        assert(out == "<.1 _>\n<.0>\n") { out }
    }
    @Test
    fun b12_new_self () {
        val out = all("""
            type List = <[(),/List]>
            var x: /List = new <.1 [(),<.0>]>
            var y: [(),/List] = [(), new <.1 [(),<.0>]>]
            var z = [(), /x]
            output std z.2\\!1.2\!0
        """.trimIndent())
        assert(out == "()\n") { out }
    }
    @Test
    fun b13_new_self () {
        val out = all("""
            type List = <[//List,/List]>
            var x: /List = new <.1 [_(&printf),<.0>]>
            set x\!1.1 = /x
            output std x
            output std x\!1.1\
        """.trimIndent())
        assert(out == "<.1 [_,<.0>]>\n<.1 [_,<.0>]>\n") { out }
    }
    @Test
    fun b16_new () {
        val out = all("""
            type List = <(),/List>
            var l: /List = new <.2 new <.1>>
            var t1 = [l]
            var t2 = [t1.1]
            output std /t2
        """.trimIndent())
        assert(out == "[<.2 <.1>>]\n") { out }
    }
    @Test
    fun b17_new () {
        val out = all("""
            type List = <(),/List>
            var l: /List = new <.2 new <.1>>
            var t1 = [(), l]
            var t2 = [(), t1.2]
            output std /t2
        """.trimIndent())
        assert(out == "[(),<.2 <.1>>]\n") { out }
    }
    @Test
    fun b21_new () {
        val out = all("""
            type List = <(),/List>
            var x: /List = new <.2 new <.1>>
            var y = x
            output std x
            output std y
        """.trimIndent())
        assert(out == "<.2 <.1>>\n<.2 <.1>>\n") { out }
    }
    @Test
    fun b22_new () {
        val out = all("""
            type List = <(),[(),/List]>
            var x: /List = new <.2 [(),new <.1>]>
            var y = [(), x\!2.2]
            output std x
            output std /y
        """.trimIndent())
        assert(out == "<.2 [(),<.1>]>\n[(),<.1>]\n") { out }
    }
    @Test
    fun b23_new () {
        val out = all("""
            type List = </List>
            var z: /List = <.0>
            var one: /List = new <.1 z>
            var l: /List = new <.1 one>
            var p: //List
            {
                set p = /l --!1
            }
            output std p\
        """.trimIndent())
        assert(out == "<.1 <.1 <.0>>>\n") { out }
    }
    @Test
    fun b25_new () {
        val out = all("""
            type List = </List>
            var l1: /List = new <.1 <.0>>
            var l2 = new List.1 l1
            var t3 = [(), new List.1 l2\!1]
            output std l1
            output std /t3
        """.trimIndent())
        assert(out == "<.1 <.0>>\n[(),<.1 <.1 <.0>>>]\n") { out }
    }

    // FUNC / CALL

    @Test
    fun c01 () {
        val out = all("""
        var f = func /_int@k1 -> () {
           set arg\ = _(*${D}arg+1)
           return
        }
        var x: _int = _1
        call f /x
        output std x
        """.trimIndent())
        assert(out == "2\n") { out }
    }
    @Test
    fun c02_fact () {
        val out = all(
            """
            var fact : func [/_int,_int] -> ()
            set fact = func [/_int,_int] -> () {
                var x = _1: _int
                var n = arg.2
                if _(${D}n > 1) {
                    call fact [/x,_(${D}n-1)]
                }
                set arg.1\ = _(${D}x*${D}n)
            }
            var x = _0: _int
            call fact [/x,_6]
            output std x
        """.trimIndent()
        )
        assert(out == "720\n") { out }
    }
    @Test
    fun c03 () {
        val out = all("""
            type List = </List>
            var f = func /List->() {
                var pf = arg
                output std pf
            }
            {
                var x: /List
                set x = new <.1 <.0>>
                call f x
            }
        """.trimIndent())
        assert(out == "<.1 <.0>>\n") { out }
    }
    @Test
    fun c04_ptr_arg () {
        val out = all("""
            type List = </List>
            var f = func /List->() {
                set arg\!1 = new <.1 <.0>>
            }
            {
                var x: /List
                set x = new <.1 <.0>>
                call f x
                output std x
            }
        """.trimIndent())
        assert(out == "<.1 <.1 <.0>>>\n") { out }
    }
    @Test
    fun c05_ptr_arg_two () {
        val out = all("""
            type List = </List>
            var f = func [/List,/List]->() {
                set arg.1\!1 = new <.1 <.0>>
                set arg.2\!1 = new <.1 <.0>>
            }
            {
                var x: /List = new <.1 <.0>>
                call f [x,x]
                output std x
            }
        """.trimIndent())
        assert(out == "<.1 <.1 <.0>>>\n") { out }
    }
    @Test
    fun c06_ptr_call_err () {
        val out = all("""
            var f = func /() -> /() {
                return arg
            }
            output std (f ())
        """.trimIndent()
        )
        assert(out.startsWith("(ln 2, col 5): invalid return : type mismatch")) { out }
    }
    @Test
    fun c07_ptr_arg_ret () {
        val out = all("""
            var f = func /_int@a1 -> /_int@a1 {
                return arg
            }
            var x: _int = _10
            var y: /_int = f /x
            output std y\
        """.trimIndent()
        )
        assert(out == "10\n") { out }
    }
    @Test
    fun c08_call_call () {
        val out = all("""
            var f = func /_int@k1 -> /()@k1 {
                return arg
            }
            var g = func /_int@k1 -> /()@k1 {
                return f arg
            }
            var x: _int
            var px = f /x
            output std _(${D}px == &${D}x):_int
        """.trimIndent()
        )
        assert(out == "1\n") { out }
    }
    @Test
    fun c09_func_arg () {
        val out = all(
            """
            var f = func () -> () {
                return arg
            }
            var g = func [(func ()->()), ()] -> () {
                return arg.1 arg.2
            }
            output std g [f,()]
        """.trimIndent()
        )
        assert(out == "()\n") { out }
    }

    // CLOSURE

    @Test
    fun todo_d01 () {
        val out = all("""
            type List = </List>
            { @A
                var pa: /List @[LOCAL] @LOCAL
                set pa = new List@[A].1 <.0>: /(List @[A]) @A: @A
                var f: func ()->()
                set f = func @[]-> ()->() {
                    var pf: /List @[A] @A
                    set pf = new List @[A].1 <.0>: /List @[A] @A: @A
                    set pa\!1 = pf
                    --output std pa
                }
                call f ()
                output std pa
            }
        """.trimIndent())
        assert(out == "<.1 <.1 <.0>>>\n") { out }
    }
    @Test
    fun todo_d02 () {
        val out = all("""
            type List = </List>
            { @A
                var pa: /List @[LOCAL] @LOCAL
                set pa = new <.1 <.0>>
                var f: func ()->()
                set f = func @[]-> ()->() {
                    var pf: /List @[A] @A
                    set pf = new List @[A].1 <.0>: /List @[A] @A: @A
                    set pa\!1 = pf
                    --output std pa
                }
                call f ()
                output std pa
            }
        """.trimIndent())
        assert(out == "<.1 <.1 <.0>>>\n") { out }
    }
    @Test
    fun d03_err () {
        val out = all("""
            var f: func () -> _int          -- 1. `f` is a reference to a function
            {
                var x: _int = _10
                set f = func () -> _int {   -- 2. `f` is created
                    return x                -- 3. `f` needs access to `x`
                }
            }                               -- 4. `x` goes out of scope
            call f ()                       -- 5. `f` still wants to access `x`
        """.trimIndent()
        )
        //assert(out == "()\n") { out }
        assert(out.startsWith("(ln 4, col 11): invalid assignment : type mismatch :")) { out }
    }

    // TYPE / ALIAS

    @Test
    fun e01_type () {
        val out = all("""
            type List = </List @LOCAL>
            var l: /List = <.0>
            output std l
        """.trimIndent())
        assert(out == "<.0>\n") { out }
    }
    @Test
    fun e02_type () {
        val out = all("""
            type List = </List @LOCAL>
            var l: /List = new <.1 <.0>>
            output std l
        """.trimIndent())
        //assert(out == "(ln 1, col 21): invalid assignment : type mismatch") { out }
        assert(out == "<.1 <.0>>\n") { out }
    }
    @Test
    fun e03_type () {
        val out = all("""
            type List = </List @LOCAL>
            var l: /List
            var z: /List = <.0>
            var one: /List = new <.1 z>
            set l = new <.1 one>
            output std l\!1
        """.trimIndent())
        assert(out == "<.1 <.0>>\n") { out }
    }
    @Test
    fun e04_type () {
        val out = all("""
            type List = </List>
            var l: /List = new <.1 <.0>>
            output std l
        """.trimIndent())
        //assert(out == "(ln 1, col 21): invalid assignment : type mismatch") { out }
        assert(out == "<.1 <.0>>\n") { out }
    }
    @Test
    fun e07_type () {
        val out = all("""
            native _{
                void output_pico (TPico arg) {}
            }
            type THAnchor = <(),(),()>
            type TVAnchor = <(),(),()>
            type TPico = <
                [THAnchor,TVAnchor]
            >
            var x = TPico.1 [<.1>,<.1>] 
            output std /x
            output pico x
        """.trimIndent())
        assert(out == "<.1 [<.1>,<.1>]>\n") { out }
    }
    @Test
    fun e08_ptr_num() {
        val out = all("""
            type Num = /<Num>    
            var zero:  Num = <.0>
            var one:   Num = new <.1 zero>
            output std one
        """.trimIndent())
        //assert(out == "<.1 <.0>>\n") { out }
        //assert(out == "(ln 3, col 18): invalid type : expected pointer to alias type\n") { out }
        assert(out == "(ln 1, col 6): invalid recursive type : cannot be a pointer") { out }
    }
    @Test
    fun e09_bool() {
        val out = all("""
            type Bool = <(),()>
            var v: Bool = <.1>
            output std /v
        """.trimIndent())
        assert(out == "<.1>\n") { out }
    }
    @Test
    fun e10_rect() {
        val out = all("""
            type Unit  = ()
            type Int   = _int
            type Point = [Int,Int]
            type Rect  = [Point,Point]
            type URect = [Unit,Rect]
            var v:    Int   = _1
            var pt:   Point = [_1,v]
            var rect: Rect  = [pt,[_3,_4]]
            var r2: Rect  = [[_1,_2],[_3,_4]]
            var ur1:  URect = [(),rect]
            var unit: Unit  = ()
            var ur2:  URect = [unit,rect]
            output std /ur2
        """.trimIndent())
        assert(out == "[(),[[1,1],[3,4]]]\n") { out }
    }
    @Test
    fun e11_rect_dot() {
        val out = all("""
            type Int   = _int
            type Point = [Int,Int]
            type Rect  = [Point,Point]
            var r: Rect  = [[_1,_2],[_3,_4]]
            output std r.2.1
        """.trimIndent())
        assert(out == "3\n") { out }
    }
    @Test
    fun e12_ucons_type () {
        val out = all("""
            type TPico = <(),[_int,_int]>
            spawn {
                var t1 = TPico.1
                output std /t1
                var t2 = TPico.2 [_1,_2]
                output std /t2
            }
        """.trimIndent())
        assert(out == "<.1>\n<.2 [1,2]>\n") { out }
    }
    @Test
    fun e13_func_alias () {
        val out = all("""
            type Int2Int = func @[] -> _int -> _int
            
            var f: Int2Int
            set f = Int2Int {
                set ret = arg
            } 
            
            var x: _int
            set x = f _10:_int
            
            output std x
       """.trimIndent())
        assert(out == "10\n") { out }
    }

    // WHERE / UNTIL

    @Test
    fun f01_where () {
        val out = all("""
            output std x where { var x = ()  }
        """.trimIndent())
        //assert(out == "(ln 2, col 5): expected `in` : have end of file") { out }
        assert(out == "()\n") { out }
    }
    @Test
    fun f02_until () {
        val out = all("""
            output std () until _1
        """.trimIndent())
        //assert(out == "(ln 2, col 5): expected `in` : have end of file") { out }
        assert(out == "()\n") { out }
    }
    @Test
    fun f03_err () {
        val out = all("""
            output std () until ()
        """.trimIndent())
        assert(out == "(ln 1, col 21): invalid condition : type mismatch : expected _int : have ()\n") { out }
    }
    @Test
    fun f05_err () {
        val out = all("""
            output std v where {
                var v = ()
            } until z where {
                var z = _1:_int
            }
        """.trimIndent())
        assert(out == "()\n") { out }
    }
}