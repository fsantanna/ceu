import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.MethodOrderer.Alphanumeric
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import java.io.File
import java.io.PushbackReader
import java.io.StringReader

@Disabled
@TestMethodOrder(Alphanumeric::class)
class TXDisabled {

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

    // MUTUTAL RECURSION

    @Disabled
    @Test
    fun b01 () {
        val out = all("""
            var x: /<(),/</^^,/^>>
            set x = <.1>
        """.trimIndent())
        assert(out == "(ln 2, col 11): invalid inference : type mismatch") { out }
    }
    @Disabled
    @Test
    fun b02 () {
        val out = all("""
            var x: /<(),/</^^,/^>>
            set x = new <.2 new <.1 <.1>>>
        """.trimIndent())
        assert(out == "(ln 2, col 27): invalid inference : type mismatch") { out }
    }
    @Disabled
    @Test
    fun b03 () {
        val out = all("""
            var x: /</<[/^^ @GLOBAL,/^ @GLOBAL]> @GLOBAL> @GLOBAL
            set x = new <.1 <.1 [<.0>,<.0>]>>
        """.trimIndent())
        assert(out == "(ln 2, col 19): invalid inference : type mismatch") { out }
    }
    @Disabled
    @Test
    fun b04_self () {
        val out = all("""
            var z: /< [<(),//^^>,_int,/^]> = <.0>
            var x: /< [<(),//^^>,_int,/^]> = new <.1 [z,_1,new <.1 [z,_2,z]>]>
            set x!1.3!1.1 = <.1 /x>
            set x!1.1 = <.1 /x!1.3>
            output std x!1.3!1.2
            output std x!1.1!1\!1.1!1\!1.2
        """.trimIndent())
        assert(out == "(ln 4, col 17): invalid operand to `/´ : union discriminator") { out }
    }

    @Disabled
    @Test
    fun b01_new () {
        val out = all("""
            var xxx: /<(),/</^^,/^>>
            set xxx =
                new <.2
                    new <.1
                        new <.1>
                    >
                >
            output std xxx
        """.trimIndent())
        assert(out == "<.2 <.1 <.1>>>\n") { out }
    }
    @Disabled
    @Test
    fun b02_new () {
        val out = all("""
            var x: /<(),/</^^ @LOCAL,/^ @LOCAL> @LOCAL> @LOCAL
            set x = new <.2 new <.2 new <.2 <.0>>>>
            output std x
        """.trimIndent())
        assert(out == "<.2 <.2 <.2 <.0>>>>\n") { out }
    }
    @Disabled
    @Test
    fun b03_new () {
        val out = all("""
            var x: /<(),/</^^ @LOCAL,/^ @LOCAL> @LOCAL> @LOCAL
            set x = new <.2 new <.1 new <.1>>>
            output std x
        """.trimIndent())
        assert(out == "<.2 <.1 <.1>>>\n") { out }
    }
    @Disabled
    @Test
    fun b04_new () {
        val out = all("""
            var x: /<(),/</^^ @LOCAL,/^ @LOCAL> @LOCAL> @LOCAL
            set x = new <.2 new <.2 new <.1 new <.1>>>>
            output std x
        """.trimIndent())
        assert(out == "<.2 <.2 <.1 <.1>>>>\n") { out }
    }
    @Disabled
    @Test
    fun b05_new () {
        val out = all("""
            var x: /</<[/^^,/^]>>
            set x = new <.1 new <.1 [<.0>,<.0>]>>
            output std x
        """.trimIndent())
        assert(out == "<.1 <.1 [<.0>,<.0>]>>\n") { out }
    }
    @Disabled
    @Test
    fun b06_new () {
        val out = all("""
            var x: /</<[/^^ @LOCAL,/^ @LOCAL]> @LOCAL> @LOCAL
            set x = new <.1 new <.1 [<.0>,new <.1 [<.0>,<.0>]>]>>
            output std x
        """.trimIndent())
        assert(out == "<.1 <.1 [<.0>,<.1 [<.0>,<.0>]>]>>\n") { out }
    }
    @Disabled
    @Test
    fun b08_new () {
        val out = all("""
            var e: /<(),<(),/^^ @LOCAL>>
            set e = new <.2 <.2 new <.1>>>
            var s: <(),/<(),<(),/^^>>>
            set s = <.2 e>
            output std /s
        """.trimIndent())
        assert(out == "<.2 <.2 <.2 <.1>>>>\n") { out }
    }
    @Disabled
    @Test
    fun b10_new () {
        val out = all("""
            var x: /< [<(),//^^ @LOCAL @LOCAL>,/^ @LOCAL]> @LOCAL
            set x = new <.1 [<.1>,<.0>]>
            var y: /< [<(),//^^>,/^]>
            set y = new <.1 [<.1>,x]>
            set y\!1.2\!1.1 = <.1>
            -- <.1 [<.1>,<.1 [<.1>,<.0>]>]>
            output std y
        """.trimIndent())
        assert(out == "<.1 [<.1>,<.1 [<.1>,<.0>]>]>\n") { out }
    }
    @Disabled
    @Test
    fun b11_new_self () {
        val out = all("""
            var x: /< [<(),//^^ @LOCAL @LOCAL>,/^ @LOCAL]> @LOCAL
            set x = new <.1 [<.1>,<.0>]>
            var y: /< [<(),//^^>,/^]>
            set y = new <.1 [<.1>, x]>
            set y\!1.2\!1.1 = <.2 /y>
            output std y
        """.trimIndent())
        assert(out == "<.1 [<.1>,<.1 [<.2 _>,<.0>]>]>\n") { out }
    }
    @Disabled
    @Test
    fun b14_new_self () {
        val out = all("""
            var x: /< [<(),//^^>,/^]>
            set x = new <.1 [<.1>,<.0>]>
            set x\!1.1 = <.2 /x>  -- ok
            output std x
            output std x\!1.1!2\
        """.trimIndent())
        assert(out == "<.1 [<.2 _>,<.0>]>\n<.1 [<.2 _>,<.0>]>\n") { out }
    }
    @Disabled
    @Test
    fun b15_new_self () {
        val out = all("""
            var x: /< <(),//^^ @LOCAL @LOCAL>> @LOCAL
            set x = new <.1 <.1>>
            output std x
        """.trimIndent())
        assert(out == "<.1 <.1>>\n") { out }
        //assert(out == "(ln 1, col 14): invalid type declaration : unexpected `^´") { out }
    }
    @Disabled
    @Test
    fun b18_new () {
        val out = all("""
            var v1: /<(),/<[/^^,/^]>> = new <.2 <.0>>
            var v2: /<(),/<[/^^,/^]>>
            set v2 = new <.2 new <.1 [new <.1>,<.0>]>>
            output std v1
            output std v2
        """.trimIndent())
        assert(out == "<.2 <.0>>\n<.2 <.1 [<.1>,<.0>]>>\n") { out }
    }
    @Disabled
    @Test
    fun b19_new () {
        val out = all("""
            var x: /< [<(),//^^ @LOCAL @LOCAL>,/^ @LOCAL]> @LOCAL = new <.1 [<.1>,<.0>]>
            var y: /< [<(),//^^ @LOCAL @LOCAL>,/^ @LOCAL]> @LOCAL = new <.1 [<.1>,x]>
            set y\!1.2\!1.1 = <.1>
            output std y
        """.trimIndent())
        assert(out == "<.1 [<.1>,<.1 [<.1>,<.0>]>]>\n") { out }
        //assert(out == "(ln 1, col 16): invalid type declaration : unexpected `^´") { out }
    }
    @Disabled
    @Test
    fun b20_new () {
        val out = all("""
            var x: /< [<(),//^^ @LOCAL @LOCAL>,/^ @LOCAL]> @LOCAL = new <.1 [<.1>,new <.1 [<.1>,<.0>]>]>
            set x\!1.2 = <.0>
            output std x
        """.trimIndent())
        //assert(out == "(ln 1, col 14): invalid type declaration : unexpected `^´") { out }
        //assert(out == "out.exe: out.c:133: main: Assertion `(*(x))._1._2 == NULL' failed.\n") { out }
        assert(out == "<.1 [<.1>,<.0>]>\n") { out }
    }
    @Disabled
    @Test
    fun b24_double () {
        val out = all("""
            var n = <.0>: /<</^^>>
            output std n
        """.trimIndent())
        assert(out == "<.0>\n") { out }
    }
    @Disabled
    @Test
    fun b27_self () {
        val out = all("""
            var x: /< [<(),/^^>,_int,/^]>
            var z: /< [<(),/^^>,_int,/^]> = <.0>
            var o: <(),/< [<(),/^^>,_int,/^]>> = <.1>
            set x = new <.1 [o,_1,new <.1 [o,_2,z]>]>
            set x\!1.3\!1.1 = <.2 x>
            set x\!1.1 = <.2 x\!1.3>
            output std x\!1.3\!1.2
            output std x\!1.2
        """.trimIndent())
        assert(out == "2\n1\n") { out }
    }

    // CLOSURES

    @Disabled
    @Test
    fun noclo_d02_clo () {
        val out = all("""
            var f: func () -> (func @a1->()->())
        """.trimIndent()
        )
        assert(out == """
            var f: func @[a1] -> () -> func @a1 -> @[] -> () -> ()
            
        """.trimIndent()) { out }
    }
    @Disabled
    @Test
    fun noclo_d04_clo () {
        val out = all("""
            var g: func () -> (func @a1->()->())
            var f: func @LOCAL -> () -> ()
        """.trimIndent()
        )
        assert(out == """
            var g: func @[a1] -> () -> func @a1 -> @[] -> () -> ()
            var f: func @GLOBAL -> @[] -> () -> ()
            
        """.trimIndent()) { out }
    }
    @Disabled
    @Test
    fun noclo_d05_clo () {
        val out = all("""
            var g: func () -> (func @a1->()->())
            var f: func @LOCAL -> () -> ()
            set f = g ()
        """.trimIndent()
        )
        assert(out == """
            var g: func @[a1] -> () -> func @a1 -> @[] -> () -> ()
            var f: func @GLOBAL -> @[] -> () -> ()
            set f = (g @[GLOBAL] (): @GLOBAL)
            
        """.trimIndent()) { out }
    }
    @Disabled
    @Test
    fun noclo_d06_clo () {
        val out = all("""
            var g: func () -> (func @a1->()->())
            var f: func @LOCAL -> () -> ()
            set f = g ()
            call f ()
        """.trimIndent()
        )
        assert(out == """
            var g: func @[a1] -> () -> func @a1 -> @[] -> () -> ()
            var f: func @GLOBAL -> @[] -> () -> ()
            set f = (g @[GLOBAL] (): @GLOBAL)
            call (f @[] ())
            
        """.trimIndent()) { out }
    }
    @Disabled
    @Test
    fun noclo_d07_clo () {
        val out = all("""
            var cnst = func @[a1]->/_int@a1 -> (func @a1->()->/_int@a1) {
                var x: /_int@a1 = arg
                return func @a1->()->/_int@a1 {
                    return x
                }
            }
        """.trimIndent())
        assert(out == "(ln 7, col 11): undeclared variable \"x\"") { out }
        /*
        assert(out == """
            var cnst: func @[a1] -> /_int @a1 -> func @a1 -> @[] -> () -> /_int @a1
            set cnst = func @[a1] -> /_int @a1 -> func @a1 -> @[] -> () -> /_int @a1 {
            var x: /_int @a1
            set x = arg
            set ret = func @a1 -> @[] -> () -> /_int @a1 {
            set ret = x
            return
            }

            return
            }


        """.trimIndent()) { out }
         */
    }
    @Disabled
    @Test
    fun noclo_d08_clo () {
        val out = all("""
            var g: func @[a]->/_int@a -> (func @a->()->/_int@a)
            var five: _int
            var f: func @LOCAL->()->/_int@LOCAL = g /five
            var v: /_int = f ()
        """.trimIndent())
        assert(out == """
            var g: func @[a] -> /_int @a -> func @a -> @[] -> () -> /_int @a
            var five: _int
            var f: func @GLOBAL -> @[] -> () -> /_int @GLOBAL
            set f = (g @[GLOBAL] (/five): @GLOBAL)
            var v: /_int @GLOBAL
            set v = (f @[] (): @GLOBAL)

        """.trimIndent()) { out }
    }
    @Disabled
    @Test
    fun noclo_d09_clo () {
        val out = all("""
            var g: func @[a]->/_int@a -> (func @a->()->/_int@a)
            {
                var five: _int
                var f: func @LOCAL->()->/_int@LOCAL = g /five
                var v: /_int = f ()
            }
        """.trimIndent())
        assert(out == """
            var g: func @[a] -> /_int @a -> func @a -> @[] -> () -> /_int @a
            { @SSFIVE
            var five: _int
            var f: func @LOCAL -> @[] -> () -> /_int @LOCAL
            set f = (g @[LOCAL] (/five): @LOCAL)
            var v: /_int @LOCAL
            set v = (f @[] (): @LOCAL)
            }

        """.trimIndent()) { out }
    }
    @Disabled
    @Test
    fun noclo_e03_clo () {
        val out = all(
            """
            var f: func (func ()->()) -> (func @GLOBAL->()->())
        """.trimIndent()
        )
        assert(out == "var f: func @[] -> func @[] -> () -> () -> func @GLOBAL -> @[] -> () -> ()\n") { out }
    }
    @Disabled
    @Test
    fun noclo_f02 () {
        val out = all(
            """
            type List = </List>
            var g = func @[a1] -> () -> (func @a1->()->()) {
                var x: /(List @[a1])@a1 = new <.1 <.0>>
                return func @a1->()->() {
                    output std x
                }
            }
            var f: (func @LOCAL->()->()) = g ()
            call f ()
        """.trimIndent()
        )
        //assert(out == "<.1 <.0>>\n") { out }
        assert(out == "(ln 9, col 12): undeclared variable \"x\"") { out }
    }
    @Disabled
    @Test
    fun noclo_f03 () {
        val out = all(
            """
            var cnst = func @[a1]->/_int@a1 -> (func @a1->()->/_int@a1) {
                var x: /_int@a1 = arg
                return func @a1->()->/_int@a1 {
                    return x
                }
            }
            {
                var five = _5: _int
                var f: func @LOCAL -> () -> /_int@LOCAL = cnst /five
                var v: /_int = f ()
                output std v\
            }
        """.trimIndent()
        )
        //assert(out == "5\n") { out }
        assert(out == "(ln 8, col 11): undeclared variable \"x\"") { out }
    }
    @Disabled
    @Test
    fun noclo_f04 () {
        val out = all(
            """
            var f = func (func ()->()) -> (func @GLOBAL->()->()) {
                var ff = arg
                return func @GLOBAL->()->() {
                    call ff ()
                }
            }
            var u = func ()->() {
                output std ()
            }
            var ff = f u
            call ff ()
        """.trimIndent()
        )
        assert(out == "(ln 8, col 7): undeclared variable \"ff\"") { out }
    }

    @Disabled
    @Test
    fun noclo_a06_par2 () {
        val out = all("""
            type Event = <(),_int>
            var build = func @[r1] -> () -> task @r1->()->()->() {
                set ret = task @r1->()->()->() {
                    output std _1:_int
                    await evt?2
                    output std _2:_int
                }
            }
            var f = build ()
            var g = build ()
            output std _10:_int
            var x = spawn f ()
            output std _11:_int
            var y = spawn g ()
            emit <.2 _1>
            output std _12:_int
        """.trimIndent())
        assert(out == "10\n1\n11\n1\n2\n2\n12\n") { out }
    }

    @Disabled
    @Test
    fun noclo_c10_func_ret () {
        val out = all(
            """
            var g = func () -> (func ()->()) {
                var f = func () -> () {
                    output std ()
                }
               return f
            }
            var f = g ()
            call f ()
        """.trimIndent()
        )
        assert(out == "()\n") { out }
    }

    /*
    @Disabled   // no more full closures
    @Test
    fun ch_01_04_addc_pg12() {
        val out = all("""
            $nums
            $clone
            $add
            -- 25
            var plusc = func $NumA1 -> (func @a1->$Num->$Num) {
                var x = arg
                return func @a1->$Num->$Num {
                    return add [x,arg]
                }
            }
            var f = plusc one
            output std f two
            output std f one
            output std (plusc one) zero
        """.trimIndent()
        )
        assert(out == "<.1 <.1 <.1 <.0>>>>\n<.1 <.1 <.0>>>\n<.1 <.0>>\n") { out }
    }
    @Disabled   // no more full closures
    @Test
    fun ch_01_04_quad_pg12() {
        val out = all(
            """
            $nums
            $clone
            $add
            $mul
            var square: func $Num -> $Num
            set square = func $Num -> $Num {
                return mul [arg,arg]
            }
            var twicec = func (func $Num->$Num) -> (func @GLOBAL->$Num->$Num) {
                var f = arg
                return func @GLOBAL->$Num->$Num {
                    return f (f arg)
                }
            }
            var quad = twicec square
            output std quad two
        """.trimIndent()
        )
        assert(out == "<.1 <.1 <.1 <.1 <.1 <.1 <.1 <.1 <.1 <.1 <.1 <.1 <.1 <.1 <.1 <.1 <.0>>>>>>>>>>>>>>>>>\n") { out }
    }
    @Disabled   // no more full closures
    @Test
    fun ch_01_04_curry_pg13() {
        val out = all(
            """
            $nums
            $clone
            $add
            var curry = func (func [$Num,$Num] -> $Num) -> (func @GLOBAL -> $NumA1 -> (func @a1->$Num->$Num)) {
                var f = arg
                return func @GLOBAL -> $NumA1 -> (func @a1->$Num->$Num) {
                    var x = arg
                    var ff = f
                    return func @a1->$Num->$Num {
                        var y = arg
                        return ff [x,y]
                    }
                }
            }
            var addc = curry add
            output std (addc  one) two
        """.trimIndent()
        )
        assert(out == "<.1 <.1 <.1 <.0>>>>\n") { out }
    }
    @Disabled
    @Test
    fun noclo_ch_01_04_curry_pg13_xxx() {
        val out = all(
            """
            type Num @[s] = </Num @[s] @s>
            var add: func [/Num@[a]@a, /Num@[b]@b] -> /Num@[r]@r
            var curry : func (func [/Num@[a]@a,/Num@[b]@b]->/Num@[r]@r) -> (func @GLOBAL -> /Num@[a]@a -> (func @a->/Num@[b]@b->/Num@[r]@r))
            --var addc = curry add
            output std ()
        """.trimIndent()
        )
        assert(out == "()\n") { out }
    }
    @Disabled   // no more full closures
    @Test
    fun ch_01_04_composition_pg15() {
        val out = all(
            """
            $nums
            $clone
            $add
            
            var inc = func $Num -> $Num {
                return add [one,arg]
            }
            output std inc two
            
            var compose = func [func $Num->$Num,func $Num->$Num] -> (func @GLOBAL->$Num->$Num) {
                var f = arg.1
                var g = arg.2
                return func @GLOBAL->$Num->$Num {
                    var v = f arg
                    return g v
                }
            }
            output std (compose [inc,inc]) one
        """.trimIndent()
        )
        assert(out == "<.1 <.1 <.1 <.0>>>>\n<.1 <.1 <.1 <.0>>>>\n") { out }
    }
    @Disabled   // no more full closures
    @Test
    fun ch_01_04_currying_pg11() {
        val out = all(
            """
            $nums
            $clone
            $lt
            -- 19
            var smallerc = func $NumA1 -> (func @a1 -> $Num->$Num) {
                var x = arg
                return func @a1 -> $Num -> $Num {
                    if (lt [x,arg]) {
                        return clone x     -- TODO: remove clone
                    } else {
                        return clone arg   -- TODO: remove clone
                    }
                }
            }
            -- 30
            var f: func @LOCAL -> $Num -> $Num
            set f = smallerc two
            output std f one
            output std f three
        """.trimIndent()
        )
        assert(out == "<.1 <.0>>\n<.1 <.1 <.0>>>\n") { out }
    }
     */
}