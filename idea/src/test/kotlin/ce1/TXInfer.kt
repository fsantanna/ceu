import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.MethodOrderer.Alphanumeric
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import java.io.PushbackReader
import java.io.StringReader

/*
fun rem (out: String): String {
    out.split("\n").let {
        it.drop(1).takeLast(it.size-1)
    }
}
 */

@TestMethodOrder(Alphanumeric::class)
class TXInfer {

    fun all (inp: String): String {
        CE1 = true
        All_restart(null, PushbackReader(StringReader(inp), 2))
        N = 1
        Lexer.lex()
        try {
            val s = Parser.stmts()
            s.setUps(null)
            s.setScp1s()
            s.setEnvs(null)
            check_00_after_envs(s)
            s.setXargs()
            s.xinfScp1s()
            check_01_before_tps(s)
            //println(s.dump())
            //println(s.tostr())
            s.xinfTypes(null)
            s.setScp2s()
            return s.tostr()
        } catch (e: Throwable) {
            if (THROW) {
                throw e
            }
            return e.message!!
        }
    }

    @Test
    fun a01_var () {
        val out = all("var x = ()")
        assert(out == "var x: ()\nset x = ()\n") { out }
    }
    @Test
    fun a01_var_err () {
        val out = all("var x; set x=()")
        assert(out == "(ln 1, col 6): expected type declaration : have \";\"") { out }
    }
    @Test
    fun a02_var () {
        val out = all("var x = <.1>:<(),()>")
        assert(out == "var x: <(),()>\nset x = <.1 ()>: <(),()>\n") { out }
    }
    @Test
    fun a03_var () {
        val out = all("var rct = [_1]")
        assert(out == "(ln 1, col 12): invalid inference : undetermined type") { out }
    }
    @Test   // TODO: Std -> .1
    fun a03_input_output () {
        val out = all(prelude1+"var x: _int = input Std ()\noutput Std ()\n")
        assert(out == """
            type Error $D{} @{} = <Escape=()>
            type Event $D{} @{} = <Kill=(),Task=_uint64_t,Timer=_int>
            type Output $D{} @{} = <Std=_(void*)>
            type Input $D{} @{} = <Std=_>
            var x: _int
            set x = input (Input.Std $D{} @{} ()): _int
            output (Output.Std $D{} @{} ())
            
        """.trimIndent()) { out }
    }
    @Test
    fun todo_h05_ret () {   // TODO: needs tostring(CE0/CE1)
        val out = all("""
            spawn {
                var opt: _int
                var str = if _0 {()} else { if opt {()} else {()} }
            }
        """.trimIndent())
        assert(out == "()\n()\n") { out }
    }
    @Disabled // no more expr
    @Test
    fun a04_input () {
        val out = all("var x: [_int,_int] = [_10,input std ()]")
        assert(out == "var x: [_int,_int]\nset x = [(_10: _int),input std (): _int]\n") { out }
    }
    @Test
    fun a05_upref () {
        val out = all("""
            var y: _int = _10
            var x: /_int = /_y
            set _:_int = x\
        """.trimIndent())
        assert(out == """
            var y: _int
            set y = (_10: _int)
            var x: /_int @GLOBAL
            set x = (/(_y: _int))
            set (_: _int) = (x\)
            
        """.trimIndent()) { out }
    }
    @Test
    fun a06_dnref () {
        val out = all("""
            var y: /_int = _10
            var x: _int = _y\
            set _:_int = x\
        """.trimIndent())
        assert(out == """
            var y: /_int @GLOBAL
            set y = (_10: /_int @GLOBAL)
            var x: _int
            set x = ((_y: /_int @GLOBAL)\)
            set (_: _int) = (x\)
            
        """.trimIndent()) { out }
    }
    @Test
    fun a07_call () {
        val out = all("""
            var v = _f ()
        """.trimIndent())
        //assert(out == "(ln 1, col 9): invalid inference : undetermined type") { out }
        assert(out == """
            var v: _
            set v = ((_f: _) @{} ())

        """.trimIndent()) { out }
    }
    @Test
    fun a08_call () {
        val out = all("""
            var f = func <()>->() { return arg }
            var v = f <.1>
        """.trimIndent())
        assert(out == """
            var f: func @{} -> <()> -> ()
            set f = (func @{} -> <()> -> () {
            set ret = arg
            return
            }
            )
            var v: ()
            set v = (f @{} <.1 ()>: <()>)
            
        """.trimIndent()) { out }
    }
    @Test
    fun a09_union () {
        val out = all("""
            var x : <<()>>
            set x = <.1 <.1>>
        """.trimIndent())
        assert(out == """
            var x: <<()>>
            set x = <.1 <.1 ()>: <()>>: <<()>>
            
        """.trimIndent()) { out }
    }
    @Test
    fun a10_type1 () {
        val out = all("""
            type List = </List @LOCAL>
            var l: /List = Null
            --output std l
        """.trimIndent())
        assert(out == """
            type List $D{} @{} = </List $D{} @{} @GLOBAL>
            var l: /List $D{} @{} @GLOBAL
            set l = Null: /List $D{} @{} @GLOBAL
            
        """.trimIndent()) { out }
    }
    @Test
    fun a10_type2 () {
        val out = all("""
            type List = </List>
            var l: /List = Null
            --output std l
        """.trimIndent())
        assert(out == """
            type List $D{} @{i} = </List $D{} @{i} @i>
            var l: /List $D{} @{GLOBAL} @GLOBAL
            set l = Null: /List $D{} @{GLOBAL} @GLOBAL

        """.trimIndent()) { out }
    }
    @Test
    fun a10_new () {
        val out = all("""
            type List = </List>
            var l: /List = new <.1 Null>
            --output std l
        """.trimIndent())
        assert(out == """
            type List $D{} @{i} = </List $D{} @{i} @i>
            var l: /List $D{} @{GLOBAL} @GLOBAL
            set l = (new (List $D{} @{GLOBAL} <.1 Null: /List $D{} @{GLOBAL} @GLOBAL>: </List $D{} @{GLOBAL} @GLOBAL>): @GLOBAL)

        """.trimIndent()) { out }
    }
    @Test
    fun a11_new () {
        val out = all("""
            type List = </List>
            var l = new List.1 Null
            --var l = new <.1 Null>:List
            --output std l
        """.trimIndent())
        assert(out == """
            type List $D{} @{i} = </List $D{} @{i} @i>
            var l: /List $D{} @{GLOBAL} @GLOBAL
            set l = (new (List.1 $D{} @{GLOBAL} Null: /List $D{} @{GLOBAL} @GLOBAL): @GLOBAL)

        """.trimIndent()) { out }
    }
    @Test
    fun todo_a12_new_ce1_ce0 () {
        val out = all("""
            type List = <Cons=/List>
            var l = new List.Cons Null
            --var l = new <.1 Null>:List
            --output std l
        """.trimIndent())
        assert(out == """
            type Error @{} = <Escape=()>
            type List $D{} @{i} = </List $D$D{} @{i} @i>
            var l: /List $D{} @{GLOBAL} @GLOBAL
            set l = (new (List $D{} @{GLOBAL} <.1 Null: /List $D{} @{GLOBAL} @GLOBAL>: </List $D{} @{GLOBAL} @GLOBAL>): @GLOBAL)
            
        """.trimIndent()) { out }
    }
    @Test
    fun a12_ucons () {
        val out = all("""
            type Xx = <()>
            var y = Xx.2 ()
        """.trimIndent())
        assert(out == "(ln 2, col 12): invalid constructor : out of bounds") { out }
    }
    @Test
    fun a13_input_ptr () {
        val out = all("""
            $prelude1
            type Input += <Pico=/()>
            var input_pico_Unit = func /() -> () {}
            var e: () = ()
            input Pico /e
        """.trimIndent())
        assert(out == """
            type Error $D{} @{} = <Escape=()>
            type Event $D{} @{} = <Kill=(),Task=_uint64_t,Timer=_int>
            type Output $D{} @{} = <Std=_(void*)>
            type Input $D{} @{} = <Std=_>
            type Input $D{} @{i} += <Pico=/() @i>
            var input_pico_Unit: func @{i} -> /() @i -> ()
            set input_pico_Unit = (func @{i} -> /() @i -> () {

            }
            )
            var e: ()
            set e = ()
            input (Input.Pico $D{} @{GLOBAL} (/e)): ()

        """.trimIndent()) { out }
    }
    @Test
    fun a14_nat1 () {
        val out = all("var x: _int = _10")
        assert(out == "var x: _int\nset x = (_10: _int)\n") { out }
    }
    @Test
    fun a15_nat2 () {
        val out = all("var x: _int ; set x = _10")
        assert(out == "var x: _int\nset x = (_10: _int)\n") { out }
    }
    @Test
    fun a16_func () {
        val out = all("""
            call _f ()
            func f: <()> -> () { return arg }
            var v = f <.1>
        """.trimIndent())
        assert(out == """
            call ((_f: _) @{} ())
            var f: func @{} -> <()> -> ()
            set f = (func @{} -> <()> -> () {
            set ret = arg
            return
            }
            )
            var v: ()
            set v = (f @{} <.1 ()>: <()>)
            
        """.trimIndent()) { out }
    }
    @Test
    fun a17_task () {
        val out = all("""
            task f: ()->()->() {
                --output std _1:_int
            }
            var x = spawn f ()
            --output std _2:_int
        """.trimIndent())
        assert(out == """
            var f: task @{} -> () -> () -> ()
            set f = (task @{} -> () -> () -> () {
            
            }
            )
            var x: active task @{} -> () -> () -> ()
            set x = spawn (f @{} ())

        """.trimIndent()) { out }
    }
    @Test
    fun a18_clone_rec () {
        val out = all("""
            type Num = </Num>    
            var clone = func /Num -> /Num {
                return new <.1 clone arg\!1>
            }
            call clone Null
        """.trimIndent())
        assert(out == """
            type Num $D{} @{i} = </Num $D{} @{i} @i>
            var clone: func @{i,j,k,l} -> /Num $D{} @{j} @i -> /Num $D{} @{l} @k
            set clone = (func @{i,j,k,l} -> /Num $D{} @{j} @i -> /Num $D{} @{l} @k {
            set ret = (new (Num $D{} @{l} <.1 (clone @{j,j,l,l} (((arg\)~)!1): @l)>: </Num $D{} @{l} @l>): @k)
            return
            }
            )
            call (clone @{GLOBAL,GLOBAL,GLOBAL,GLOBAL} Null: /Num $D{} @{GLOBAL} @GLOBAL: @GLOBAL)
            
        """.trimIndent()) { out }
    }
    @Test
    fun a19_clone() {
        val out = all("""
            type Num = </Num>    
            var clone = func /Num -> /Num {
            }
        """.trimIndent())
        assert(out == """
            type Num $D{} @{i} = </Num $D{} @{i} @i>
            var clone: func @{i,j,k,l} -> /Num $D{} @{j} @i -> /Num $D{} @{l} @k
            set clone = (func @{i,j,k,l} -> /Num $D{} @{j} @i -> /Num $D{} @{l} @k {
            
            }
            )
            
        """.trimIndent()) { out }
    }
    @Test
    fun a20_clone() {
        val out = all("""
            type Num = </Num>    
            var clone : func /Num -> /Num
            set clone = func /Num -> /Num {
            }
        """.trimIndent())
        assert(out == """
            type Num $D{} @{i} = </Num $D{} @{i} @i>
            var clone: func @{i,j,k,l} -> /Num $D{} @{j} @i -> /Num $D{} @{l} @k
            set clone = (func @{i,j,k,l} -> /Num $D{} @{j} @i -> /Num $D{} @{l} @k {
            
            }
            )
            
        """.trimIndent()) { out }
    }

    // inference error

    @Test
    fun b05_nat () {
        val out = all("""
            var output_pico = func () -> () {
                native _{
                    pico_output(*(Pico_IO*)&arg);
                }
            }
        """.trimIndent())
        assert(out == """
            var output_pico: func @{} -> () -> ()
            set output_pico = (func @{} -> () -> () {
            native _{
                    pico_output(*(Pico_IO*)&arg);
                }
            }
            )
    
        """.trimIndent()) { out }
    }
    @Test
    fun b06_inp_err () {
        val out = all("""
            $prelude1
            type Input += <Pico=()>
            input Pico ()
        """.trimIndent())
        assert(out == """
            type Error $D{} @{} = <Escape=()>
            type Event $D{} @{} = <Kill=(),Task=_uint64_t,Timer=_int>
            type Output $D{} @{} = <Std=_(void*)>
            type Input $D{} @{} = <Std=_>
            type Input $D{} @{} += <Pico=()>
            input (Input.Pico $D{} @{} ()): ()
            
        """.trimIndent()) { out }
    }

    // POINTER ARGUMENTS / SCOPES

    @Test
    fun c01 () {
        val out = all("""
        var f: func /_int -> ()
        """.trimIndent())
        assert(out == "var f: func @{i} -> /_int @i -> ()\n") { out }
    }
    @Test
    fun c02 () {
        val out = all("""
            var f = func /_int -> () {}
        """.trimIndent())
        assert(out == """
            var f: func @{i} -> /_int @i -> ()
            set f = (func @{i} -> /_int @i -> () {
            
            }
            )
            
        """.trimIndent()) { out }
    }
    @Test
    fun c03 () {
        val out = all("""
        var f: func /_int@k1 -> ()
        var x: _int = _1
        call f /x
        """.trimIndent())
        assert(out == """
            var f: func @{k1} -> /_int @k1 -> ()
            var x: _int
            set x = (_1: _int)
            call (f @{GLOBAL} (/x))
            
        """.trimIndent()) { out }
    }
    @Test
    fun c04 () {
        val out = all("""
        var f: func /_int@k -> ()
        {
            var x: _int = _1
            var y: _int = _1
            call f /x
            call f /y
        }
        """.trimIndent())
        assert(out == """
            var f: func @{k} -> /_int @k -> ()
            {
            var x: _int
            set x = (_1: _int)
            var y: _int
            set y = (_1: _int)
            call (f @{X} (/x))
            call (f @{Y} (/y))
            }
            
        """.trimIndent()) { out }
    }
    @Test
    fun c05_fact () {
        val out = all(
            """
            var fact = func [/_int,_int] -> () {
                var x = _1:_int
                call fact [/x,_]
            }
        """.trimIndent()
        )
        //assert(out == "(ln 3, col 10): invalid inference : undetermined type") { out }
        //assert(out == "(ln 3, col 15): invalid inference : type mismatch") { out }
        assert(out == """
            var fact: func @{i} -> [/_int @i,_int] -> ()
            set fact = (func @{i} -> [/_int @i,_int] -> () {
            var x: _int
            set x = (_1: _int)
            call (fact @{X} [(/x),(_: _int)])
            }
            )
            
        """.trimIndent()) { out }
    }
    @Test
    fun c06_new_return0 () {
        val out = all("""
            type List @{x} = </List @{x} @x>
            var f = func /List->() {
                set arg\!1 = Null
            }
        """.trimIndent())
        assert(out == """
            type List $D{} @{x} = </List $D{} @{x} @x>
            var f: func @{i,j} -> /List $D{} @{j} @i -> ()
            set f = (func @{i,j} -> /List $D{} @{j} @i -> () {
            set (((arg\)~)!1) = Null: /List $D{} @{j} @j
            }
            )

        """.trimIndent()) { out }
    }
    @Test
    fun c06_new_return1 () {
        val out = all("""
            type List = </List>
            var f = func /List->() {
                set arg\!1 = Null
            }
        """.trimIndent())
        assert(out == """
            type List $D{} @{i} = </List $D{} @{i} @i>
            var f: func @{i,j} -> /List $D{} @{j} @i -> ()
            set f = (func @{i,j} -> /List $D{} @{j} @i -> () {
            set (((arg\)~)!1) = Null: /List $D{} @{j} @j
            }
            )

        """.trimIndent()) { out }
    }
    @Test
    fun c06_new_return2 () {
        val out = all("""
            type List = </List>
            var f = func /List->() {
                set arg\!1 = new <.1 Null>
            }
        """.trimIndent())
        assert(out == """
            type List $D{} @{i} = </List $D{} @{i} @i>
            var f: func @{i,j} -> /List $D{} @{j} @i -> ()
            set f = (func @{i,j} -> /List $D{} @{j} @i -> () {
            set (((arg\)~)!1) = (new (List $D{} @{j} <.1 Null: /List $D{} @{j} @j>: </List $D{} @{j} @j>): @j)
            }
            )

        """.trimIndent()) { out }
    }
    @Test
    fun c07_null () {
        val out = all("""
            var v = Null
        """.trimIndent())
        assert(out == "(ln 1, col 9): invalid inference : undetermined type") { out }
    }
    @Test
    fun c08_null () {
        val out = all("""
            type List = </List>
            var v: /List = Null
        """.trimIndent())
        assert(out == """
            type List $D{} @{i} = </List $D{} @{i} @i>
            var v: /List $D{} @{GLOBAL} @GLOBAL
            set v = Null: /List $D{} @{GLOBAL} @GLOBAL

        """.trimIndent()) { out }
    }
    @Test
    fun c09_null () {
        val out = all("""
            type List @{i} = /</List @{i} @i> @i
            var f : func List -> ()
            call f Null
        """.trimIndent())
        assert(out == "(ln 1, col 6): invalid recursive type : cannot be a pointer") { out }
    }
    @Test
    fun c10_ff () {
        val out = all("""
            type List = </List>
            var f : func /List -> /List
            var v = f Null
            --output std f v
        """.trimIndent())
        assert(out == """
            type List $D{} @{i} = </List $D{} @{i} @i>
            var f: func @{i,j,k,l} -> /List $D{} @{j} @i -> /List $D{} @{l} @k
            var v: /List $D{} @{GLOBAL} @GLOBAL
            set v = (f @{GLOBAL,GLOBAL,GLOBAL,GLOBAL} Null: /List $D{} @{GLOBAL} @GLOBAL: @GLOBAL)

        """.trimIndent()) { out }
    }
    @Test
    fun c11_rec_ptr () {
        val out = all("""
            type List = </List>
            { @A
                var x: /List @{A}
            }
        """.trimIndent())
        assert(out == """
            type List $D{} @{i} = </List $D{} @{i} @i>
            { @A
            var x: /List $D{} @{A} @A
            }

        """.trimIndent()) { out }
    }
    @Test
    fun c12_rec_ptr () {
        val out = all("""
            type List = </List>
            { @A
                var x: /List @GLOBAL
            }
        """.trimIndent())
        assert(out == """
            type List $D{} @{i} = </List $D{} @{i} @i>
            { @A
            var x: /List $D{} @{GLOBAL} @GLOBAL
            }

        """.trimIndent()) { out }
    }
    @Test
    fun c12_rec_ptr2 () {
        val out = all("""
            type List = </List>
            { @A
                { @B
                    var x: /List @A
                }
            }
        """.trimIndent())
        assert(out == """
            type List $D{} @{i} = </List $D{} @{i} @i>
            { @A
            { @B
            var x: /List $D{} @{A} @A
            }
            }

        """.trimIndent()) { out }
    }
    @Test
    fun c13_rec_ptr () {
        val out = all("""
            type List = </List>
            var f: func @{a} -> /List @{a} -> ()
        """.trimIndent())
        assert(out == """
            type List $D{} @{i} = </List $D{} @{i} @i>
            var f: func @{a} -> /List $D{} @{a} @a -> ()

        """.trimIndent()) { out }
    }
    @Test
    fun c14_rec_ptr () {
        val out = all("""
            type List = </List>
            var f: func @{a} -> /List @a -> ()
        """.trimIndent())
        assert(out == """
            type List $D{} @{i} = </List $D{} @{i} @i>
            var f: func @{a} -> /List $D{} @{a} @a -> ()

        """.trimIndent()) { out }
    }
    @Test
    fun c15_rec_ptr () {
        val out = all("""
            type List = </List>
            var f: func @{i} -> /List @{i} -> ()
            { @A
                var x: /List @{A}
                { @B
                    var g: func /List @{i} -> ()
                    var y: /List @A
                    var z: /List @{A}
                }
            }
        """.trimIndent())
        assert(out == """
            type List $D{} @{i} = </List $D{} @{i} @i>
            var f: func @{i} -> /List $D{} @{i} @i -> ()
            { @A
            var x: /List $D{} @{A} @A
            { @B
            var g: func @{i} -> /List $D{} @{i} @i -> ()
            var y: /List $D{} @{A} @A
            var z: /List $D{} @{A} @A
            }
            }

        """.trimIndent()) { out }
    }

    // CLOSURE

    @Test
    fun noclo_d00_clo () {
        val out = all("""
            {
                var x = ()
                var f = func () -> () {
                    --output std x
                }
                call f ()
            }
        """.trimIndent())
        assert(out == """
            {
            var x: ()
            set x = ()
            var f: func @{} -> () -> ()
            set f = (func @{} -> () -> () {
            
            }
            )
            call (f @{} ())
            }

        """.trimIndent()) { out }
    }
    @Test
    fun d01_clo () {
        val out = all("""
            type List = </List>
            {
                var pa: /List = new <.1 Null>
                var f = func () -> () {
                }
                call f ()
            }
        """.trimIndent())
        assert(out == """
            type List $D{} @{i} = </List $D{} @{i} @i>
            {
            var pa: /List $D{} @{LOCAL} @LOCAL
            set pa = (new (List $D{} @{LOCAL} <.1 Null: /List $D{} @{LOCAL} @LOCAL>: </List $D{} @{LOCAL} @LOCAL>): @LOCAL)
            var f: func @{} -> () -> ()
            set f = (func @{} -> () -> () {

            }
            )
            call (f @{} ())
            }
            
        """.trimIndent()) { out }
    }
    @Test
    fun d03_clo () {
        val out = all("""
            var f: func ()->()
        """.trimIndent()
        )
        assert(out == """
            var f: func @{} -> () -> ()
            
        """.trimIndent()) { out }
    }
    @Test
    fun noclo_d10_clo () {
        val out = all("""
            { --@Y
                var x = ()
                {
                    var f = func () -> () {
                        --output std x
                    }
                    call f ()
                }
            }
        """.trimIndent())
        assert(out == """
            {
            var x: ()
            set x = ()
            {
            var f: func @{} -> () -> ()
            set f = (func @{} -> () -> () {
            
            }
            )
            call (f @{} ())
            }
            }

        """.trimIndent()) { out }
    }

    // NUMS

    @Test
    fun e01_clone () {
        val out = all("""
            type List = </List>
            var clone : func /List -> /List
            set clone = func /List -> /List {
                if arg\?Null {
                    return Null
                } else {
                    return new <.1 clone arg\!1>
                }
            }
        """.trimIndent())
        assert(out == """
            type List $D{} @{i} = </List $D{} @{i} @i>
            var clone: func @{i,j,k,l} -> /List $D{} @{j} @i -> /List $D{} @{l} @k
            set clone = (func @{i,j,k,l} -> /List $D{} @{j} @i -> /List $D{} @{l} @k {
            if (((arg\)~)?Null)
            {
            set ret = Null: /List $D{} @{l} @k
            return
            }
            else
            {
            set ret = (new (List $D{} @{l} <.1 (clone @{j,j,l,l} (((arg\)~)!1): @l)>: </List $D{} @{l} @l>): @k)
            return
            }
            }
            )

        """.trimIndent()) { out }
    }
    @Test
    fun e02_ff () {
        val out = all("""
            type List = </List>
            var f : func /List -> /List
            set _:_int = f (f Null)
        """.trimIndent())
        assert(out == """
            type List $D{} @{i} = </List $D{} @{i} @i>
            var f: func @{i,j,k,l} -> /List $D{} @{j} @i -> /List $D{} @{l} @k
            set (_: _int) = (f @{GLOBAL,GLOBAL,GLOBAL,GLOBAL} (f @{GLOBAL,GLOBAL,GLOBAL,GLOBAL} Null: /List $D{} @{GLOBAL} @GLOBAL: @GLOBAL): @GLOBAL)
            
        """.trimIndent()) { out }
    }
    @Test
    fun e03_scp () {
        val out = all("""
            var clone: func () -> /_int
            call clone ()
        """.trimIndent())
        assert(out == """
            var clone: func @{i} -> () -> /_int @i
            call (clone @{GLOBAL} (): @GLOBAL)
            
        """.trimIndent()) { out }
    }
    @Test
    fun e04_ctrs () {
        val out = all(
            """
            var smaller: func @{a1,a2: a2>a1} -> [/_int@a1,/_int@a2] -> /_int@a2
        """.trimIndent()
        )
        assert(out == "var smaller: func @{a1,a2: a2>a1} -> [/_int @a1,/_int @a2] -> /_int @a2\n") { out }
    }
    @Test
    fun e05_notype() {
        val out = all(
            """
            var zero: /Num = Null
            var one:   Num = <.1 zero>
        """.trimIndent()
        )
        assert(out == "(ln 1, col 12): undeclared type \"Num\"") { out }
    }

    // CLOSURE ERRORS

    @Test
    fun noclo_f01 () {
        val out = all("""
            type List = </List>
            { @A
                var pa: /List = new <.1 Null>
                var f = func ()->() {
                    var pf: /(List @{A})@A = new <.1 Null>
                    set pa\!1 = pf
                }
                call f ()
                --output std pa
            }
        """.trimIndent())
        //assert(out == "<.1 <.1 Null>>\n") { out }
        //assert(out == "(ln 6, col 13): undeclared variable \"pa\"") { out }
        assert(out == """
            type List $D{} @{i} = </List $D{} @{i} @i>
            { @A
            var pa: /List $D{} @{LOCAL} @LOCAL
            set pa = (new (List $D{} @{LOCAL} <.1 Null: /List $D{} @{LOCAL} @LOCAL>: </List $D{} @{LOCAL} @LOCAL>): @LOCAL)
            var f: func @{} -> () -> ()
            set f = (func @{} -> () -> () {
            var pf: /List $D{} @{A} @A
            set pf = (new (List $D{} @{A} <.1 Null: /List $D{} @{A} @A>: </List $D{} @{A} @A>): @A)
            set (((pa\)~)!1) = pf
            }
            )
            call (f @{} ())
            }
           
        """.trimIndent()) { out }
    }
    @Test
    fun noclo_f05 () {
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
        assert(out == """
            var f: func @{} -> () -> _int
            {
            var x: _int
            set x = (_10: _int)
            set f = (func @{} -> () -> _int {
            set ret = x
            return
            }
            )
            }
            call (f @{} ())
            
        """.trimIndent()) { out }
    }

    // PAR

    @Test
    fun noclo_g01_spawn () {
        val out = all("""
            spawn {
                var x = ()
                spawn {
                    --output std x
                }
                spawn {
                    --output std x
                }
            }
        """.trimIndent())
        assert(out == """
            spawn ((task @{} -> _ -> _ -> _ {
            var x: ()
            set x = ()
            spawn ((task @{} -> _ -> _ -> _ {
            
            }
            ) @{} ())
            spawn ((task @{} -> _ -> _ -> _ {
            
            }
            ) @{} ())
            }
            ) @{} ())

        """.trimIndent()) { out }
    }
    @Test
    fun noclo_g02_spawn_spawn () {
        val out = all("""
            spawn {
                var x = ()
                spawn {
                    spawn {
                        --output std x
                    }
                }
                spawn {
                    --output std x
                }
            }
        """.trimIndent())
        assert(out == """
            spawn ((task @{} -> _ -> _ -> _ {
            var x: ()
            set x = ()
            spawn ((task @{} -> _ -> _ -> _ {
            spawn ((task @{} -> _ -> _ -> _ {
            
            }
            ) @{} ())
            }
            ) @{} ())
            spawn ((task @{} -> _ -> _ -> _ {
            
            }
            ) @{} ())
            }
            ) @{} ())

        """.trimIndent()) { out }
    }
    @Test
    fun noclo_g03_spawn_task () {
        val out = all("""
            var t = spawn {
                --output std ()
            }
        """.trimIndent())
        assert(out == """
            var t: active task @{} -> _ -> _ -> _
            set t = spawn ((task @{} -> _ -> _ -> _ {
            
            }
            ) @{} ())
            
        """.trimIndent()) { out }
    }
    @Test
    fun g04_task_type () {
        val out = all("""
            type Xask = task ()->_int->()
            var t : Xask
            set t = Xask {
                --output std _2:_int
            }
            --output std _1:_int
            var x : active Xask
            set x = spawn t ()
            var y = spawn t ()
            --output std x.pub
            set _:_int = x.pub
            --output std _3:_int
        """.trimIndent())
        assert(out == """
            type Xask $D{} @{} = task @{} -> () -> _int -> ()
            var t: Xask $D{} @{}
            set t = (Xask $D{} @{} (task @{} -> () -> _int -> () {
            
            }
            ))
            var x: active Xask $D{} @{}
            set x = spawn (active Xask $D{} @{} ((t~) @{} (): @GLOBAL))
            var y: active task @{} -> () -> _int -> ()
            set y = spawn ((t~) @{} ())
            set (_: _int) = ((x~).pub)
            
        """.trimIndent()) { out }
    }
    @Test
    fun g05_task_type () {
        val out = all("""
            type Xask = task ()->()->()
            var t : Xask
            var xs : active {} Xask
            spawn t () in xs
        """.trimIndent())
        assert(out == """
            type Xask $D{} @{} = task @{} -> () -> () -> ()
            var t: Xask $D{} @{}
            var xs: active {} Xask $D{} @{}
            spawn ((t~) @{} ()) in xs

        """.trimIndent()) { out }
    }

    // WHERE / UNTIL / WCLOCK

    @Test
    fun h01_err () {
        val out = all("""
            var x: ()
            set x = y where {
                var y = ()
            }
            --output std x
        """.trimIndent())
        assert(out == """
            var x: ()
            {
            var y: ()
            set y = ()
            set x = y
            }

        """.trimIndent()) { out }
    }

    @Test
    fun h02_var () {
        val out = all("""
            var x = y where {
                var y = ()
            }
            --output std x
        """.trimIndent())
        assert(out == """
            var x: ()
            {
            var y: ()
            set y = ()
            set x = y
            }
            
        """.trimIndent()) { out }
    }

    @Test
    fun h03_until () {
        val out = all("""
            spawn {
                set _:_ = () until _0
            }
        """.trimIndent())
        assert(out == """
            spawn ((task @{} -> _ -> _ -> _ {
            {
            {
            loop {
            set (_: _) = ()
            if (_0: _int)
            {
            break
            }
            else
            {
            
            }
            }
            }
            }
            }
            ) @{} ())
            
        """.trimIndent()) { out }
    }
    @Test
    fun h04_until_where () {
        val out = all("""
            spawn {
                set (_: _) = () until x where { var x = () }
            }
        """.trimIndent())
        assert(out == """
            spawn ((task @{} -> _ -> _ -> _ {
            {
            {
            loop {
            set (_: _) = ()
            {
            var x: ()
            set x = ()
            if x
            {
            break
            }
            else
            {
            
            }
            }
            }
            }
            }
            }
            ) @{} ())
    
        """.trimIndent()) { out }
    }
    @Test   // TODO: should it give an error?
    fun todo_h05_until_var_err () {
        val out = all("""
            var x = () until _0
        """.trimIndent())
        assert(out == """            
        """.trimIndent()) { out }
    }
    @Test
    fun h06_where_until_where () {
        val out = all("""
            spawn {
                set (_: _) = y where { var y = () } until x where { var x:_int = _1 }
            }
        """.trimIndent())
        assert(out == """
            spawn ((task @{} -> _ -> _ -> _ {
            {
            {
            loop {
            {
            var y: ()
            set y = ()
            set (_: _) = y
            }
            {
            var x: _int
            set x = (_1: _int)
            if x
            {
            break
            }
            else
            {

            }
            }
            }
            }
            }
            }
            ) @{} ())

        """.trimIndent()) { out }
    }
    @Test
    fun h07_err () {
        val out = all("""
            set (_: _) = v until _1 where {
                var v = ()
            }
        """.trimIndent())
        assert(out == "(ln 1, col 14): undeclared variable \"v\"") { out }
    }

    @Test
    fun h08_wclock () {
        val out = all("""
            type Event = <Kill=(),Task=(),Timer=()>
            var sub: func [_imt,_int] -> _int
            var lte: func [_imt,_int] -> _int
            spawn {
                await 1s
            }
        """.trimIndent())
        assert(out == """
            type Event $D{} @{} = <Kill=(),Task=(),Timer=()>
            var sub: func @{} -> [_imt,_int] -> _int
            var lte: func @{} -> [_imt,_int] -> _int
            spawn ((task @{} -> _ -> _ -> _ {
            {
            var ms_8: _int
            set ms_8 = (_1000: _int)
            {
            {
            loop {
            await ((evt~)?Timer)
            set ms_8 = (sub @{} [ms_8,((evt~)!Timer)])
            if (lte @{} [ms_8,(_0: _int)])
            {
            break
            }
            else
            {
            
            }
            }
            }
            }
            }
            }
            ) @{} ())

        """.trimIndent()) { out }
    }
    @Test
    fun h09_wclock () {
        val out = all("""
            type Event = <Kill=(),Task=(),Timer=()>
            var sub: func [_imt,_int] -> _int
            var lte: func [_imt,_int] -> _int
            spawn {
                every 1h5min2s20ms {
                    --output std ()
                }
            }
        """.trimIndent())
        assert(out == """
            type Event $D{} @{} = <Kill=(),Task=(),Timer=()>
            var sub: func @{} -> [_imt,_int] -> _int
            var lte: func @{} -> [_imt,_int] -> _int
            spawn ((task @{} -> _ -> _ -> _ {
            {
            {
            loop {
            {
            var ms_12: _int
            set ms_12 = (_3902020: _int)
            {
            {
            loop {
            await ((evt~)?Timer)
            set ms_12 = (sub @{} [ms_12,((evt~)!Timer)])
            if (lte @{} [ms_12,(_0: _int)])
            {
            break
            }
            else
            {

            }
            }
            }
            }
            }
            {
            
            }
            }
            }
            }
            }
            ) @{} ())

        """.trimIndent()) { out }
    }

    // TUPLES / TYPE

    @Test
    fun j01_point () {
        val out = all("""
            type Point = [_int,_int]
            var xy: Point = [_1,_2]
            var x = xy.1
        """.trimIndent())
        //assert(out == "(ln 2, col 5): expected `in` : have end of file") { out }
        assert(out == """
            type Point $D{} @{} = [_int,_int]
            var xy: Point $D{} @{}
            set xy = (Point $D{} @{} [(_1: _int),(_2: _int)])
            var x: _int
            set x = ((xy~).1)
            
        """.trimIndent()) { out }
    }
    @Test
    fun j02 () {
        val out = all("""
            type Point = [_int,_int]
            type Dims  = [_int,_int]
            type Rect  = [Point,Dims]
            var r: Rect = [[_1,_2],[_1,_2]]
            var h = r.2.2
        """.trimIndent())
        assert(out == """
            type Point $D{} @{} = [_int,_int]
            type Dims $D{} @{} = [_int,_int]
            type Rect $D{} @{} = [Point $D{} @{},Dims $D{} @{}]
            var r: Rect $D{} @{}
            set r = (Rect $D{} @{} [(Point $D{} @{} [(_1: _int),(_2: _int)]),(Dims $D{} @{} [(_1: _int),(_2: _int)])])
            var h: _int
            set h = ((((r~).2)~).2)
            
        """.trimIndent()) { out }
    }
    @Test
    fun j03 () {
        val out = all("""
            type TPico = <()>
            spawn {
                set (_: _) = TPico.1
            }
        """.trimIndent())
        assert(out == """
            type TPico $D{} @{} = <()>
            spawn ((task @{} -> _ -> _ -> _ {
            set (_: _) = (TPico.1 $D{} @{} ())
            }
            ) @{} ())
            
        """.trimIndent()) { out }
    }
    @Test
    fun j04 () {
        val out = all("""
            type TPico = <(),[_int,_int]>
            spawn {
                set (_: _) = TPico.2 [_1,_2]
            }
        """.trimIndent())
        assert(out == """
            type TPico $D{} @{} = <(),[_int,_int]>
            spawn ((task @{} -> _ -> _ -> _ {
            set (_: _) = (TPico.2 $D{} @{} [(_1: _int),(_2: _int)])
            }
            ) @{} ())
            
        """.trimIndent()) { out }
    }
    @Test
    fun f06_tst () {
        val out = all("""
            var isPointInsideRect = func Point -> _int {
                return _1
            }
        """.trimIndent())
        assert(out == "(ln 1, col 30): undeclared type \"Point\"") { out }
    }
    @Test
    fun f07_tst () {
        val out = all("""
            var f = func () -> () {                                                 
                return g ()                                                         
            }                                                                       
        """.trimIndent())
        assert(out == "(ln 2, col 12): undeclared variable \"g\"") { out }
    }
    @Test
    fun f08_err_e_not_declared () {
        val out = all("""
            set _:_int = e?3
        """.trimIndent())
        assert(out == "(ln 1, col 14): undeclared variable \"e\"") { out }
    }
    @Test
    fun f09_func_alias () {
        val out = all("""
            type Int2Int = func @{} -> _int -> _int
            
            var f: Int2Int
            set f = Int2Int {
                set ret = arg
            } 
            
            var x: _int
            set x = f _10:_int
            
            --output std x
       """.trimIndent())
        assert(out == """
            type Int2Int $D{} @{} = func @{} -> _int -> _int
            var f: Int2Int $D{} @{}
            set f = (Int2Int $D{} @{} (func @{} -> _int -> _int {
            set ret = arg
            }
            ))
            var x: _int
            set x = ((f~) @{} (_10: _int))
            
        """.trimIndent()) { out }
    }

    @Test
    fun f10_await_ret_err () {
        val out = all("""
            type Event = <(),_uint64_t,_int>
            var f = task @{}->_int->()->_int {
                return arg
            }
            spawn {
                var x = await f _1
                --output std x
            }
        """.trimIndent())
        assert(out == "(ln 6, col 19): expected \"spawn\" : have \"f\"") { out }
    }

    @Test
    fun f10_await_ret () {
        val out = all("""
            type Event = <(),_uint64_t,_int>
            var f = task @{}->_int->()->_int {
                return arg
            }
            spawn {
                var x = await spawn f _1
                --output std x
            }
        """.trimIndent())
        assert(out == """
            type Event $D{} @{} = <(),_uint64_t,_int>
            var f: task @{} -> _int -> () -> _int
            set f = (task @{} -> _int -> () -> _int {
            set ret = arg
            return
            }
            )
            spawn ((task @{} -> _ -> _ -> _ {
            var x: _int
            {
            var tsk_27: active task @{} -> _int -> () -> _int
            set tsk_27 = spawn (f @{} (_1: _int))
            var st_27: _int
            set st_27 = (tsk_27.status)
            if (_(${D}st_27 == TASK_AWAITING): _int)
            {
            await tsk_27
            }
            else
            {
            
            }
            set x = (tsk_27.ret)
            }
            }
            ) @{} ())
            
        """.trimIndent()) { out }
    }

    @Test
    fun f11_task_type () {
        val out = all("""
            type Xask = task ()->()->()
            var t = Xask {}
            --output std ()
        """.trimIndent())
        assert(out == """
            type Xask $D{} @{} = task @{} -> () -> () -> ()
            var t: Xask $D{} @{}
            set t = (Xask $D{} @{} (task @{} -> () -> () -> () {
            
            }
            ))
            
        """.trimIndent()) { out }
    }

    // FIELDS

    @Test
    fun d01_field () {
        val out = all("""
            var pt: [x:_int, y:_int]
            set pt.x = _10
        """.trimIndent())
        assert(out == """
            var pt: [x:_int,y:_int]
            set (pt.x) = (_10: _int)
            
        """.trimIndent()) { out }
    }
    @Test
    fun d02_field_err () {
        val out = all("""
            var pt: [x:_int, y:_int]
            set pt.z = _10
        """.trimIndent())
        assert(out == "(ln 2, col 8): invalid discriminator : unknown \"z\"") { out }
    }
    @Test
    fun d03_union () {
        val out = all("""
            var b: <False=(), True=()>
            set b = <.False ()>: <False=(),True=()>
        """.trimIndent())
        assert(out == """
            var b: <False=(),True=()>
            set b = <.False ()>: <False=(),True=()>

        """.trimIndent()) { out }
    }
    @Test
    fun d04_union_err () {
        val out = all("""
            var b: <False=(), True=()>
            set b = <.Maybe ()>: <False=(), True=()>
        """.trimIndent())
        assert(out == "(ln 2, col 11): invalid constructor : unknown discriminator \"Maybe\"") { out }
    }
    @Test
    fun d05_union () {
        val out = all("""
            type Bool = <False=(), True=()>
            var b = Bool.False
        """.trimIndent())
        assert(out == """
            type Bool $D{} @{} = <False=(),True=()>
            var b: Bool $D{} @{}
            set b = (Bool.False $D{} @{} ())

        """.trimIndent()) { out }
    }
    @Test
    fun d06_tuple () {
        val out = all("""
            var b: [x:_int,y:_int] = [_10,_10]
            var c = b
            set _:_int = c.x
        """.trimIndent())
        assert(out == """
            var b: [x:_int,y:_int]
            set b = [(_10: _int),(_10: _int)]
            var c: [x:_int,y:_int]
            set c = b
            set (_: _int) = (c.x)

        """.trimIndent()) { out }
    }
    @Test
    fun d07_tuple () {
        val out = all("""
            type Point = [x:_int,y:_int]
            var b: Point = [_10,_10]
            var c = b
            set (_:_int) = (c.x)
        """.trimIndent())
        assert(out == """
            type Point $D{} @{} = [x:_int,y:_int]
            var b: Point $D{} @{}
            set b = (Point $D{} @{} [(_10: _int),(_10: _int)])
            var c: Point $D{} @{}
            set c = b
            set (_: _int) = ((c~).x)

        """.trimIndent()) { out }
    }
    @Test
    fun d08_tuple () {
        val out = all("""
            type Point = [x:_int,y:_int]
            var b: Point = [x=_10,y=_10]
            var c = b
            set (_:_int) = ((c~).x)
        """.trimIndent())
        assert(out == """
            type Point $D{} @{} = [x:_int,y:_int]
            var b: Point $D{} @{}
            set b = (Point $D{} @{} [(_10: _int),(_10: _int)])
            var c: Point $D{} @{}
            set c = b
            set (_: _int) = ((c~).x)
            
        """.trimIndent()) { out }
    }
    @Test
    fun d09_tuple_err () {
        val out = all("""
            type Point = [x:_int,y:_int]
            var b: Point = [x=_10,z=_10]
            var c = b
            set (_:_int) = (c.x)
        """.trimIndent())
        assert(out == "(ln 2, col 23): invalid constructor : unknown discriminator \"z\"") { out }
    }
    @Test
    fun d10_tuple_err () {
        val out = all("""
            type Point = [x:_int,y:_int]
            var b: Point = [x=_10,y=_10]
            var c = b
            set (_:_int) = (c.z)
        """.trimIndent())
        assert(out == "(ln 4, col 19): invalid discriminator : unknown \"z\"") { out }
    }
    @Test
    fun d11_tuple_err () {
        val out = all("""
            type Point = [x:_int,y:_int]
            var b: Point = [y=_10,x=_10]
            var c = b
            --output std c.z
        """.trimIndent())
        assert(out == "(ln 2, col 17): invalid constructor : invalid position for \"y\"") { out }
    }

    // TYPE / STRETCH

    @Test
    fun e01_stretch_err () {
        val out = all("""
            type Point = ()
            type Point += <()>
        """.trimIndent())
        assert(out == "(ln 2, col 6): invalid declaration : \"Point\" must be of union type (ln 1)") { out }
    }
    @Test
    fun e02_stretch_err () {
        val out = all("""
            type Point = <()>
            type Point += ()
        """.trimIndent())
        assert(out == "(ln 2, col 15): expected union type") { out }
    }
    @Test
    fun e03_stretch_ok () {
        val out = all("""
            type Point = <()>
            type Point += <()>
        """.trimIndent())
        assert(out == """
            type Point $D{} @{} = <()>
            type Point $D{} @{} += <()>
            
        """.trimIndent()) { out }
    }
    @Test
    fun e04_stretch_err () {
        val out = all("""
            type Point = ()
            type Point = ()
        """.trimIndent())
        assert(out == "(ln 2, col 6): invalid declaration : \"Point\" is already declared (ln 1)") { out }
    }
    @Test
    fun e05_stretch_err () {
        val out = all("""
            type Point += <()>
        """.trimIndent())
        assert(out == "(ln 1, col 6): invalid declaration : \"Point\" is not yet declared") { out }
    }
    @Test
    fun e06_stretch_ok () {
        val out = all("""
            type Point = <()>
            type Point += <()>
            var pt = Point.2
        """.trimIndent())
        assert(out == """
            type Point $D{} @{} = <()>
            type Point $D{} @{} += <()>
            var pt: Point $D{} @{}
            set pt = (Point.2 $D{} @{} ())
            
        """.trimIndent()) { out }
    }
    @Test
    fun e07_stretch_ok () {
        val out = all("""
            type Point = <Xxx = ()>
            type Point += <Yyy = ()>
            var pt = Point.Xxx
        """.trimIndent())
        assert(out == """
            type Point $D{} @{} = <Xxx=()>
            type Point $D{} @{} += <Yyy=()>
            var pt: Point $D{} @{}
            set pt = (Point.Xxx $D{} @{} ())
            
        """.trimIndent()) { out }
    }
    @Test
    fun e08_stretch_ok () {
        val out = all("""
            type Point = <Xxx = ()>
            type Point += <Yyy = ()>
            var pt = Point.Xxx
            type Point += <Zzz = ()>
        """.trimIndent())
        assert(out == """
            type Point $D{} @{} = <Xxx=()>
            type Point $D{} @{} += <Yyy=()>
            var pt: Point $D{} @{}
            set pt = (Point.Xxx $D{} @{} ())
            type Point $D{} @{} += <Zzz=()>
            
        """.trimIndent()) { out }
    }
    @Test
    fun e09_hier () {
        val out = all("""
        type Point = [_int,_int]
        type Event = [/()] + <()>
        var e = Event <.1 [_]>
       """.trimIndent())
        assert(out == """
            type Point $D{} @{} = [_int,_int]
            type Event $D{} @{i} = [/() @i] <[/() @i]>
            var e: Event $D{} @{GLOBAL}
            set e = (Event $D{} @{GLOBAL} <.1 [(_: /() @GLOBAL)]>: [/() @GLOBAL] <[/() @GLOBAL]>)

        """.trimIndent()) { out }
    }

    // THROW / CATCH

    @Test
    fun f01_return () {
        val out = all("return")
        assert(out == "(ln 1, col 1): invalid \"return\" : no enclosing function") { out }
    }
    @Test
    fun f02_break () {
        val out = all("break")
        assert(out == "(ln 1, col 1): invalid \"break\" : no enclosing loop") { out }
    }

    // IFS

    @Test
    fun g01_ifs_stmt () {
        val out = all("""
            ifs {
                _0 { set _:_ = _999:_int }
                _1 { set _:_ = _1:_int   }
            }
           """.trimIndent()
        )
        //assert(out == "(ln 4, col 16): expected \"?\" : have end of file") { out }
        assert(out == """
            if (_0: _int)
            {
            set (_: _) = (_999: _int)
            }
            else
            {
            if (_1: _int)
            {
            set (_: _) = (_1: _int)
            }
            else
            {
            {
            native _(assert(0 && "runtime error : missing \"ifs\" case");)
            }
            }
            }
            
        """.trimIndent()) { out }
    }
    @Test
    fun g02_ifs_stmt () {
        val out = all("""
            ifs {
            }
       """.trimIndent())
        assert(out == "(ln 2, col 1): expected expression : have \"}\"") { out }
    }
    @Test
    fun g03_ifs_stmt () {
        val out = all("""
            ifs {
                else {}
            }
       """.trimIndent())
        assert(out == "(ln 2, col 5): expected expression : have \"else\"") { out }
    }
    @Test
    fun g04_ifs_expr () {
        val out = all("""
            set _:_ = ifs {
                _0:_int { _999:_int }
                _1:_int { _1:_int   }
            }
           """.trimIndent()
        )
        //assert(out == "(ln 4, col 16): expected \"?\" : have end of file") { out }
        assert(out == "set (_: _) = (if (_0: _int) { (_999: _int)}  else { (if (_1: _int) { (_1: _int)}  else { (_((assert(0 && \"runtime error : missing \\\"ifs\\\" case\"),0);): _int) }) })\n") { out }
    }
    @Test
    fun g05_ifs_expr () {
        val out = all("""
            set _:_ = ifs {
            }
       """.trimIndent())
        assert(out == "(ln 2, col 1): expected expression : have \"}\"") { out }
    }
    @Test
    fun g06_ifs_expr () {
        val out = all("""
            set _:_ = ifs {
                else {}
            }
       """.trimIndent())
        assert(out == "(ln 2, col 5): expected expression : have \"else\"") { out }
    }
}
