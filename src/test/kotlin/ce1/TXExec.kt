import org.junit.jupiter.api.MethodOrderer.Alphanumeric
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import java.io.File

@TestMethodOrder(Alphanumeric::class)
class TXExec {
    @Test
    fun a01_output () {
        val out = test(true, "output std ()")
        assert(out == "()\n") { out }
    }
    @Test
    fun a02_int_abs () {
        val out = test(true, """
            var x: _int
            set x = _abs _(-1)
            output std x
        """.trimIndent())
        assert(out == "1\n") { out }
    }
    @Test
    fun a03_tuple () {
        val out = test(true, """
            var v = [(),()]
            output std /v
        """.trimIndent())
        assert(out == "[(),()]\n") { out }
    }
    @Test
    fun a04_tuples () {
        val out = test(true, """
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
        val out = test(true, """
            var y: _(char*) = _{"hello"}
            var n: _{int} = _10
            var x = [n,y]
            output std /x
        """.trimIndent())
        assert(out == "[10,\"hello\"]\n") { out }
    }
    @Test
    fun a06_call () {
        val out = test(true, """
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
        val out = test(true, """
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
        val out = test(true, """
            var a = <.1>:<(),()>
            var b : <(),()> = <.2>
            output std /a
            output std /b
        """.trimIndent())
        assert(out == "<.1>\n<.2>\n") { out }
    }
    @Test
    fun a09_func_if () {
        val out = test(true, """
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
        val out = test(true, """
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
        val out = test(true, """
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
        val out = test(true, """
            var s: [_int,_int,_int,_int] = [_1,_2,_3,_4]
            output std /s
        """.trimIndent())
        assert(out == "[1,2,3,4]\n") { out }
    }
    @Test
    fun a13_union_nat () {
        val out = test(true, """
            var s: <[_int,_int,_int,_int],_int,_int> = <.1 [_1,_2,_3,_4]>
            output std /s
        """.trimIndent())
        assert(out == "<.1 [1,2,3,4]>\n") { out }
    }
    @Test
    fun a14_pred () {
        val out = test(true, """
            var z = <.1>: <()>
            output std z?1
        """.trimIndent())
        assert(out == "1\n") { out }
    }
    @Test
    fun a15_disc () {
        val out = test(true, """
            var z: <(),()> = <.2>
            output std z!2
        """.trimIndent())
        assert(out == "()\n") { out }
    }
    @Test
    fun a16_dots () {
        val out = test(true, """
            var x: <<<()>>> = <.1 <.1 <.1>>>
            output std x!1!1!1
        """.trimIndent())
        assert(out == "()\n") { out }
    }
    @Test
    fun a17_if () {
        val out = test(true, """
            var x: <(),()> = <.2>
            if x?2 { output std () } else { }
        """.trimIndent())
        assert(out == "()\n") { out }
    }
    @Test
    fun a18_loop () {
        val out = test(true, """
        loop {
           break
        }
        output std ()
        """.trimIndent())
        assert(out == "()\n")
    }
    @Test
    fun a19_ptr () {
        val out = test(true, """
            var y: _int = _10
            var x = /y
            output std x\
        """.trimIndent())
        assert(out == "10\n") { out }
    }
    @Test
    fun i05_ptr_block_err () {
        val out = test(true, """
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
        val out = test(true, """
            type List = </List>
            var x: /List = Null
            var y: <//List> = <.1 /x>
            output std /y
            output std y!1\
        """.trimIndent())
        //assert(out == "(ln 3, col 7): invalid assignment of \"x\" : borrowed in line 2") { out }
        assert(out == "<.1 _>\nNull\n") { out }
    }
    @Test
    fun b12_new_self () {
        val out = test(true, """
            type List = <[(),/List]>
            var x: /List = new <.1 [(),Null]>
            var y: [(),/List] = [(), new <.1 [(),Null]>]
            var z = [(), /x]
            output std z.2\\!1.2
        """.trimIndent())
        assert(out == "Null\n") { out }
    }
    @Test
    fun todo_b13_new_self () {
        val out = test(true, """
            type List = <[//List,/List]>
            var x: /List = new <.1 [_(&printf),Null]>
            set x\!1.1 = /x
            output std x
            output std x\!1.1\
        """.trimIndent())
        assert(out == "<.1 [_,Null]>\n<.1 [_,Null]>\n") { out }
    }
    @Test
    fun b16_new () {
        val out = test(true, """
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
        val out = test(true, """
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
        val out = test(true, """
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
        val out = test(true, """
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
        val out = test(true, """
            type List = </List>
            var z: /List = Null
            var one: /List = new <.1 z>
            var l: /List = new <.1 one>
            var p: //List
            {
                set p = /l --!1
            }
            output std p\
        """.trimIndent())
        assert(out == "<.1 <.1 Null>>\n") { out }
    }
    @Test
    fun b25_new () {
        val out = test(true, """
            type List = </List>
            var l1: /List = new <.1 Null>
            var l2 = new List.1 l1
            var t3 = [(), new List.1 l2\!1]
            output std l1
            output std /t3
        """.trimIndent())
        assert(out == "<.1 Null>\n[(),<.1 <.1 Null>>]\n") { out }
    }

    // FUNC / CALL

    @Test
    fun c01 () {
        val out = test(true, """
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
        val out = test(true, 
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
        val out = test(true, """
            type List = </List>
            var f = func /List->() {
                var pf = arg
                output std pf
            }
            {
                var x: /List
                set x = new <.1 Null>
                call f x
            }
        """.trimIndent())
        assert(out == "<.1 Null>\n") { out }
    }
    @Test
    fun c04_ptr_arg () {
        val out = test(true, """
            type List = </List>
            var f = func /List->() {
                set arg\!1 = new <.1 Null>
            }
            {
                var x: /List
                set x = new <.1 Null>
                call f x
                output std x
            }
        """.trimIndent())
        assert(out == "<.1 <.1 Null>>\n") { out }
    }
    @Test
    fun c05_ptr_arg_two () {
        val out = test(true, """
            type List = </List>
            var f = func [/List,/List]->() {
                set arg.1\!1 = new <.1 Null>
                set arg.2\!1 = new <.1 Null>
            }
            {
                var x: /List = new <.1 Null>
                call f [x,x]
                output std x
            }
        """.trimIndent())
        assert(out == "<.1 <.1 Null>>\n") { out }
    }
    @Test
    fun c06_ptr_call_err () {
        val out = test(true, """
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
        val out = test(true, """
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
        val out = test(true, """
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
        val out = test(true, 
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
    fun d01 () {
        val out = test(true, """
            type List = </List>
            { @A
                var pa: /List @[LOCAL] @LOCAL
                set pa = new List.1@[A] Null: /(List @[A]) @A: @A
                var f: func ()->()
                set f = func @[]-> ()->() {
                    var pf: /List @[A] @A
                    set pf = new List.1 @[A] Null: /List @[A] @A: @A
                    set pa\!1 = pf
                    --output std pa
                }
                call f ()
                output std pa
            }
        """.trimIndent())
        assert(out == "<.1 <.1 Null>>\n") { out }
    }
    @Test
    fun todo_d02 () {
        val out = test(true, """
            type List = </List>
            { @A
                var pa: /List @[LOCAL] @LOCAL
                set pa = new <.1 Null>
                var f: func ()->()
                set f = func @[]-> ()->() {
                    var pf: /List @[A] @A
                    set pf = new List.1 @[A] Null: /List @[A] @A: @A
                    set pa\!1 = pf
                    --output std pa
                }
                call f ()
                output std pa
            }
        """.trimIndent())
        assert(out == "<.1 <.1 Null>>\n") { out }
    }
    @Test
    fun d03_err () {
        val out = test(true, """
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
        val out = test(true, """
            type List = </List @LOCAL>
            var l: /List = Null
            output std l
        """.trimIndent())
        assert(out == "Null\n") { out }
    }
    @Test
    fun e02_type () {
        val out = test(true, """
            type List = </List @LOCAL>
            var l: /List = new <.1 Null>
            output std l
        """.trimIndent())
        //assert(out == "(ln 1, col 21): invalid assignment : type mismatch") { out }
        assert(out == "<.1 Null>\n") { out }
    }
    @Test
    fun e03_type () {
        val out = test(true, """
            type List = </List @LOCAL>
            var l: /List
            var z: /List = Null
            var one: /List = new <.1 z>
            set l = new <.1 one>
            output std l\!1
        """.trimIndent())
        assert(out == "<.1 Null>\n") { out }
    }
    @Test
    fun e04_type () {
        val out = test(true, """
            type List = </List>
            var l: /List = new <.1 Null>
            output std l
        """.trimIndent())
        //assert(out == "(ln 1, col 21): invalid assignment : type mismatch") { out }
        assert(out == "<.1 Null>\n") { out }
    }
    @Test
    fun e07_type () {
        val out = test(true, """
            native _{
                void output_pico (CEU_Pico arg) {}
            }
            type HAnchor = <(),(),()>
            type VAnchor = <(),(),()>
            type Pico = <
                [HAnchor,VAnchor]
            >
            var x = Pico.1 [<.1>,<.1>] 
            output std /x
            output pico x
        """.trimIndent())
        assert(out == "<.1 [<.1>,<.1>]>\n") { out }
    }
    @Test
    fun e08_ptr_num() {
        val out = test(true, """
            type Num = /<Num>    
            var zero:  Num = Null
            var one:   Num = new <.1 zero>
            output std one
        """.trimIndent())
        //assert(out == "<.1 Null>\n") { out }
        //assert(out == "(ln 3, col 18): invalid type : expected pointer to alias type\n") { out }
        assert(out == "(ln 1, col 6): invalid recursive type : cannot be a pointer") { out }
    }
    @Test
    fun e09_bool() {
        val out = test(true, """
            type Bool = <(),()>
            var v: Bool = <.1>
            output std /v
        """.trimIndent())
        assert(out == "<.1>\n") { out }
    }
    @Test
    fun todo_e10_rect() {
        val out = test(true, """
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
        val out = test(true, """
            type Int   = _int
            type Point = [Int,Int]
            type Rect  = [Point,Point]
            var r: Rect  = [[_1,_2],[_3,_4]]
            output std r.2.1
        """.trimIndent())
        assert(out == "3\n") { out }
    }
    @Test
    fun e11_rect_dot_output() {
        val out = test(true, """
            type Int   = _int
            type Point = [Int,Int]
            type Rect  = [Point,Point]
            type Point_Rect = <Point,Rect>
            var r: Rect  = [[_1,_2],[_3,_4]]
            var pr = Point_Rect.2 r
            output std /pr
        """.trimIndent())
        assert(out == "<.2 [[1,2],[3,4]]>\n") { out }
    }
    @Test
    fun e12_ucons_type () {
        val out = test(true, """
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
    fun exx_func_alias () {
        val out = test(true, """
            type Int2Int = func @[] -> () -> ()
            var f: Int2Int
            set f = Int2Int {} 
            output std ()
       """.trimIndent())
        assert(out == "()\n") { out }
    }
    @Test
    fun eyy_func_alias () {
        val out = test(true, """
            type Int2Int = func @[] -> () -> ()
            var f: func @[] -> () -> ()
            set f = func @[] -> () -> () {}
            output std ()
       """.trimIndent())
        assert(out == "()\n") { out }
    }
    @Test
    fun e13_func_alias () {
        val out = test(true, """
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
    @Test
    fun e14_yids () {
        val out = test(true, """
            type Bool = <False=(), True=()>
            var x = Bool.False
            native _{
                printf("False = %d\n", CEU_BOOL_FALSE);
                printf("True = %d\n", CEU_BOOL_TRUE);
                printf("x = %d\n", global.x.False);
            }
        """.trimIndent())
        assert(out == "False = 1\nTrue = 2\nx = 0\n") { out }
    }
    @Test
    fun e15_yids () {
        val out = test(true, """
            type Point = [x:_int,y:_int]
            var pt = Point [_10,_20]
            output std pt.x
            output std pt.y
        """.trimIndent())
        assert(out == "10\n20\n") { out }
    }
    @Test
    fun e16_yids () {
        val out = test(true, """
            type Bool = <False=(), True=()>
            var x = Bool.True
            output std x?False
            output std x!True
        """.trimIndent())
        assert(out == "0\n()\n") { out }
    }
    @Test
    fun e17_yids () {
        val out = test(true, """
            type Rect  = [()]
            var r: Rect
            var x = r.pos
        """.trimIndent())
        assert(out == "(ln 3, col 11): invalid discriminator : unknown \"pos\"") { out }
    }
    @Test
    fun e18_types_yids () {
        val out = test(true, """
            type False = ()
            type Bool = <False=(), True=()>
            type Xxx = <True=(), False=()>
            type Event = <False=False,Bool=Bool,Xxx=Xxx>
            var b = Bool.False
            var x = Xxx.False
            var f = False ()
            output std _CEU_EVENT_FALSE:_int
            output std _CEU_XXX_FALSE:_int
            output std _CEU_BOOL_FALSE:_int
        """.trimIndent())
        assert(out == "1\n2\n1\n") { out }
    }
    @Test
    fun todo_e19_yids () {
        val out = test(true, """
            type Bool = <False=(), True=()>
            var x: Bool = False
            var y: Bool = True ()
            output std x
            output std y
        """.trimIndent())
        assert(out == "False = 1\nTrue = 2\nx = 0\n") { out }
    }

    // WHERE / UNTIL

    @Test
    fun f01_where () {
        val out = test(true, """
            output std x where { var x = ()  }
        """.trimIndent())
        //assert(out == "(ln 2, col 5): expected `in` : have end of file") { out }
        assert(out == "()\n") { out }
    }
    @Test
    fun f02_until () {
        val out = test(true, """
            output std () until _1
        """.trimIndent())
        //assert(out == "(ln 2, col 5): expected `in` : have end of file") { out }
        assert(out == "()\n") { out }
    }
    @Test
    fun f03_err () {
        val out = test(true, """
            output std () until ()
        """.trimIndent())
        assert(out == "(ln 1, col 21): invalid condition : type mismatch : expected _int : have ()") { out }
    }
    @Test
    fun f05_err () {
        val out = test(true, """
            output std v where {
                var v = ()
            } until z where {
                var z = _1:_int
            }
        """.trimIndent())
        assert(out == "()\n") { out }
    }

    // INCLUDE

    @Test
    fun g01_include () {
        val out = test(true, """
            ^"test-func.ceu"
            output std f _10
        """.trimIndent())
        assert(out == "10\n") { out }
    }
    @Test
    fun g02_include () {
        val out = test(true, """
            var f = func _int -> _int {
                return arg
            }
            output std f _10
        """.trimIndent())
        assert(out == "10\n") { out }
    }
    @Test
    fun g03_include_err () {
        val out = test(true, """
            ^"test-func.ceu"
            output () f _10
        """.trimIndent())
        assert(out == "(ln 2, col 8): expected variable identifier : have `()´") { out }
    }
    @Test
    fun g04_include_err () {
        val out = test(true, """
            ^"test-lincol.ceu"
            output () f _10
        """.trimIndent())
        assert(out == "test-lincol.ceu : (ln 1, col 1): expected statement : have \"inside\"") { out }
    }

    // CAST

    @Test
    fun h01_cast_err () {
        val out = test(true, """
            var x: [_int,_int]
            var ptr: _(char*)
            set x = [ptr,ptr]
        """.trimIndent())
        assert(out.contains("error: excess elements in struct initializer")) { out }
    }
    @Test
    fun h02_cast_ok () {
        val out = test(true, """
            var x: [_long,_long]
            var ptr: _(char*)
            set x = [ptr::_long,ptr::_long]
            output std ()
        """.trimIndent())
        assert(out.contains("()\n")) { out }
    }

    // TYPE / HIER

    @Test
    fun p01_type_hier () {
        val out = test(true, """
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
        var e = Event <.4 <.2 <.1 [Point [_10:_int,_10:_int],_1:_int]>:<[Point,_int],[Point,_int]>>:<[Point],<[Point,_int],[Point,_int]>>>:<_int,(),<_int,_int>,<[Point],<[Point,_int],[Point,_int]>>>
        output std /e
       """.trimIndent())
        assert(out == "<.4 <.2 <.1 [[10,10],1]>>>\n") { out }
    }
    @Test
    fun p02_hier_name_err () {
        val out = test(true, """
        type Button = [_int] + <(),()>
        var e = Button <.2 _10:_int>:<_int,_int>
        output std e!Common
       """.trimIndent())
        assert(out == "(ln 3, col 14): invalid discriminator : unknown discriminator \"Common\"") { out }
    }
    @Test
    fun p03_hier_name () {
        val out = test(true, """
        type Button = [b:_int] + <(),()>
        var e = Button <.2 _10:_int>:<_int,_int>
        output std e!Common
       """.trimIndent())
        assert(out == "(ln 1, col 26): missing subtype or field identifiers") { out }
    }
    @Test
    fun p04_hier_name () {
        val out = test(true, """
        type Button = [b:_int] + <Up=(),Down=()>
        var e = Button <.2 _10:_int>:<[_int],[_int]>
        output std /e
        output std e!Common.b
       """.trimIndent())
        assert(out == "<.2 [10]>\n10\n") { out }
    }
    @Test
    fun p05_type_hier () {
        val out = test(true, """
        type Button = [b:_int] + <Up=(),Down=()>
        var e: Button
        set e = Button <.Down [_10:_int]>
        output std /e
        output std e!0.1
       """.trimIndent())
        assert(out == "<.2 [10]>\n10\n") { out }
    }

    @Test
    fun pxx_type_hier () {
        val out = test(true, """
        type Event = <
            Frame = _int,
            Draw  = (),
            Key   = [_int] + <Up=(),Down=()>,
            Mouse = [Point] + <
                Motion = (),
                Button = [Int] + <Up=(),Down=()>
            >
        >
        type Event = <
            _int,
            (),
            [_int] + <(),()>,
            [Point] + <
                (),
                [_int] + <(),()>
            >
        >
        type Event = <
            _int,
            (),
            <_int,_int>,
            <
                [Point],
                <[Point,_int],[Point,_int]>
            >
        >
        type Event = <
            _int,
            (),
            [_int, <(),()>],
            [Point, <
                (),
                [_int, <(),()>]
            >]
        >
        var e: Event
        set e = Event.Mouse.Button.Up [[10,10],1]
        set e = <.4.2.1 [[10,10],1]>
        set e = <.4 <.2 <.1 [[10,10],1]>>>
        set e = <.4 [[10,10], <.2 [1,<.1>]>>
        
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

}
