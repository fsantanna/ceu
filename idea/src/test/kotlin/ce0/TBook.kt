import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.MethodOrderer.Alphanumeric
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import java.io.File

fun output0num (v: String): String {
    return output0(v, "/(Num $D{}@{LOCAL})@LOCAL")
}
private val nums = """
    $Output0
    type Num $D{}@{s} = </Num $D{}@{s} @s>
    var zero: /(Num $D{}@{LOCAL})@LOCAL
    set zero = Null: /(Num $D{}@{LOCAL}) @LOCAL
    var one: /(Num $D{}@{LOCAL})@LOCAL
    set one = new Num $D{}@{LOCAL} <.1 zero>:</Num $D{}@{LOCAL} @LOCAL>: @LOCAL
    var two: /(Num $D{}@{LOCAL})@LOCAL
    set two = new Num $D{}@{LOCAL} <.1 one>:</Num $D{}@{LOCAL} @LOCAL>: @LOCAL
    var three: /(Num $D{}@{LOCAL})@LOCAL
    set three = new Num $D{}@{LOCAL} <.1 two>:</Num $D{}@{LOCAL} @LOCAL>: @LOCAL
    var four: /(Num $D{}@{LOCAL})@LOCAL
    set four = new Num $D{}@{LOCAL} <.1 three>:</Num $D{}@{LOCAL} @LOCAL>: @LOCAL
    var five: /(Num $D{}@{LOCAL})@LOCAL
    set five = new Num $D{}@{LOCAL} <.1 four>:</Num $D{}@{LOCAL} @LOCAL>: @LOCAL
""".trimIndent()

fun Num (ptr: Boolean, scope: String): String {
    val ret = "Num $D{} @{$scope}"
    return if (!ptr) ret else "/"+ret+"@"+scope
}
val NumTL  = Num(true,  "LOCAL")
val NumA1  = Num(true,  "a1")
val NumA2  = Num(true,  "a2")
val NumB1  = Num(true,  "b1")
val NumC1  = Num(true,  "c1")
val NumR1  = Num(true,  "r1")
val _NumR1 = Num(false, "r1")

private val clone = """
    var clone : func $D{} @{r1,a1}-> $NumA1 -> $NumR1
    set clone = func $D{} @{r1,a1}-> $NumA1 -> $NumR1 {
        if arg\~?Null {
            set ret = Null:$NumR1
        } else {
            set ret = new $_NumR1 <.1 clone $D{} @{r1,a1} arg\~!1: @r1>:</Num $D{} @{r1} @r1>: @r1
        }
    }
""".trimIndent()

private val add = """
    var add : func $D{} @{r1,a1,b1}-> [$NumA1,$NumB1] -> $NumR1
    set add = func $D{} @{r1,a1,b1}-> [$NumA1,$NumB1] -> $NumR1 {
        var x: $NumA1
        set x = arg.1
        var y: $NumB1
        set y = arg.2
        if y\~?Null {
            set ret = clone $D{} @{r1,a1} x: @r1
        } else {
            set ret = new $_NumR1 <.1 add $D{} @{r1,a1,b1} [x,y\~!1]: @r1>:</Num $D{} @{r1} @r1>: @r1
        }
    }
""".trimIndent()

private val mul = """
    var mul : func $D{} @{r1,a1,b1}-> [$NumA1,$NumB1] -> $NumR1
    set mul = func $D{} @{r1,a1,b1}-> [$NumA1,$NumB1] -> $NumR1 {
        var x: $NumA1
        set x = arg.1
        var y: $NumB1
        set y = arg.2
        if y\~?Null {
            set ret = Null: $NumR1
        } else {
            var z: $NumTL
            set z = mul $D{} @{r1,a1,b1} [x, y\~!1]
            set ret = add $D{} @{r1,a1,LOCAL} [x,z]: @r1
        }
    }
""".trimIndent()

private val lt = """
    var lt : func $D{} @{a1,b1}-> [$NumA1,$NumB1] -> _int
    set lt = func $D{} @{a1,b1}-> [$NumA1,$NumB1] -> _int {
        if arg.2\~?Null {
            set ret = _0:_int
        } else {
            if arg.1\~?Null {
                set ret = _1:_int
            } else {
                set ret = lt $D{} @{a1,b1} [arg.1\~!1,arg.2\~!1]
            }
        }
    }
""".trimIndent()

private val sub = """
    var sub : func $D{} @{r1,a1,b1}-> [$NumA1,$NumB1] -> $NumR1
    set sub = func $D{} @{r1,a1,b1}-> [$NumA1,$NumB1] -> $NumR1 {
        var x: $NumA1
        set x = arg.1
        var y: $NumB1
        set y = arg.2
        if x\~?Null {
            set ret = Null: $NumR1
        } else {
            if y\~?Null {
                set ret = clone $D{} @{r1,a1} x
            } else {
                set ret = sub $D{} @{r1,a1,b1} [x\~!1,y\~!1]: @r1
            }
        }
    }
""".trimIndent()

private val mod = """
    var mod : func $D{} @{r1,a1,b1} -> [$NumA1,$NumB1] -> $NumR1
    set mod = func $D{} @{r1,a1,b1} -> [$NumA1,$NumB1] -> $NumR1 {
        if lt $D{} @{a1,b1} arg {
            set ret = clone $D{} @{r1,a1} arg.1: @r1
        } else {
            var v: $NumTL
            set v = sub $D{} @{LOCAL,a1,b1} arg
            set ret = mod $D{} @{r1,LOCAL,b1} [v,arg.2]: @r1
        }
    }    
""".trimIndent()

private val eq = """
    var eq : func $D{} @{a1,b1}-> [$NumA1,$NumB1] -> _int
    set eq = func $D{} @{a1,b1}-> [$NumA1,$NumB1] -> _int {
        var x: $NumA1
        set x = arg.1
        var y: $NumB1
        set y = arg.2
        if x\~?Null {
            set ret = y\~?Null
        } else {
            if y\~?Null {
                set ret = _0:_int
            } else {
                set ret = eq $D{} @{a1,b1} [x\~!1,y\~!1]
            }
        }
    }
""".trimIndent()

private val lte = """
    var lte : func $D{} @{a1,b1}-> [$NumA1,$NumB1] -> _int
    set lte = func $D{} @{a1,b1}-> [$NumA1,$NumB1] -> _int {
        var islt: _int
        set islt = lt $D{} @{a1,b1} [arg.1\~!1,arg.2\~!1]
        var iseq: _int
        set iseq = eq $D{} @{a1,b1} [arg.1\~!1,arg.2\~!1]
        set ret = _(${D}islt || ${D}iseq): _int
    }
""".trimIndent()

@TestMethodOrder(Alphanumeric::class)
class TBook {

    fun all(inp: String): String {
        println("nums:  ${nums.count  { it == '\n' }}")
        println("clone: ${clone.count { it == '\n' }}")
        println("add:   ${add.count   { it == '\n' }}")
        println("mul:   ${mul.count   { it == '\n' }}")
        println("lt:    ${lt.count    { it == '\n' }}")
        println("sub:   ${sub.count   { it == '\n' }}")
        println("mod:   ${mod.count   { it == '\n' }}")
        println("eq:    ${eq.count    { it == '\n' }}")
        println("lte:   ${lte.count   { it == '\n' }}")
        println("bton:  ${bton.count  { it == '\n' }}")
        println("ntob:  ${ntob.count  { it == '\n' }}")
        println("or:    ${or.count    { it == '\n' }}")
        println("and:   ${and.count   { it == '\n' }}")

        CE1 = false

        val (ok1, out1) = ce2c(null, inp)
        if (!ok1) {
            return out1
        }
        File("out.c").writeText(out1)
        val (ok2, out2) = exec("gcc -Werror out.c -o out.exe")
        if (!ok2) {
            return out2
        }
        val (_,out3) = exec("$VALGRIND./out.exe")
        //println(out3)
        return out3
    }

    @Test
    fun pre_01_nums() {
        val out = all("""
            $Output0
            type Num $D{}@{s} = </Num $D{}@{s} @s>
            var zero: /(Num $D{}@{LOCAL})@LOCAL
            set zero = Null: /(Num $D{}@{LOCAL}) @LOCAL
            var one: (Num $D{}@{LOCAL})
            set one = Num $D{}@{LOCAL} <.1 zero>:</Num $D{}@{LOCAL} @LOCAL>
            var two: (Num $D{}@{LOCAL})
            set two = Num $D{}@{LOCAL} <.1 /one>:</Num $D{}@{LOCAL} @LOCAL>
            ${output0num("/two")}
        """.trimIndent())
        println(output0num("/two"))
        assert(out == "<.1 <.1 Null>>\n") { out }
    }
    @Test
    fun pre_02_add() {
        val out = all(
            """
            $nums
            $clone
            $add
            ${output0num("add $D{} @{LOCAL,LOCAL,LOCAL} [two,one]: @LOCAL")}
        """.trimIndent()
        )
        assert(out == "<.1 <.1 <.1 Null>>>\n") { out }
    }
    @Test
    fun pre_03_clone() {
        val out = all(
            """
            $nums
            $clone
            ${output0num("clone $D{} @{LOCAL,LOCAL} two: @LOCAL")}
        """.trimIndent()
        )
        assert(out == "<.1 <.1 Null>>\n") { out }
    }
    @Test
    fun pre_04_mul() {
        val out = all(
            """
            $nums
            $clone
            $add
            $mul
            ${output0num("mul $D{} @{LOCAL,LOCAL,LOCAL} [two, add $D{} @{LOCAL,LOCAL,LOCAL} [two,one]]")}
        """.trimIndent()
        )
        assert(out == "<.1 <.1 <.1 <.1 <.1 <.1 Null>>>>>>\n") { out }
    }
    @Test
    fun pre_05_lt() {
        val out = all(
            """
            $nums
            $lt
            ${output0("lt $D{} @{LOCAL,LOCAL} [two, one]","_int")}
            ${output0("lt $D{} @{LOCAL,LOCAL} [one, two]","_int")}
        """.trimIndent()
        )
        assert(out == "0\n1\n") { out }
    }
    @Test
    fun pre_06_sub() {
        val out = all(
            """
            $nums
            $clone
            $add
            $sub
            ${output0num("sub $D{} @{LOCAL,LOCAL,LOCAL} [three, two]")}
        """.trimIndent()
        )
        assert(out == "<.1 Null>\n") { out }
    }
    @Test
    fun pre_07_eq() {
        val out = all(
            """
            $nums
            $eq
            ${output0("eq $D{} @{LOCAL,LOCAL} [three, two]","_int")}
            ${output0("eq $D{} @{LOCAL,LOCAL} [one, one]","_int")}
        """.trimIndent()
        )
        assert(out == "0\n1\n") { out }
    }

    // CHAPTER 1.1

    @Test
    fun ch_01_01_square_pg02() {
        val out = all(
            """
            $nums
            $clone
            $add
            $mul
            var square: func $D{} @{r1,a1}-> $NumA1 -> $NumR1
            set square = func $D{} @{r1,a1}-> $NumA1 -> $NumR1 {
                set ret = mul $D{} @{r1,a1,a1} [arg,arg]: @r1
            }
            ${output0num("square $D{} @{LOCAL,LOCAL} two")}
        """.trimIndent()
        )
        assert(out == "<.1 <.1 <.1 <.1 Null>>>>\n") { out }
    }

    @Test
    fun ch_01_01_smaller_pg02() {
        val out = all(
            """
            $nums
            $lt
            var smaller: func $D{} @{a1,a2: a2>a1}-> [$NumA1,$NumA2] -> $NumA2
            set smaller = func $D{} @{a1,a2: a2>a1}-> [$NumA1,$NumA2] -> $NumA2 {
                if lt $D{} @{a1,a2} arg {
                    set ret = arg.1
                } else {
                    set ret = arg.2
                }
            }
            ${output0num("smaller $D{} @{LOCAL,LOCAL} [one,two]: @LOCAL")}
            ${output0num("smaller $D{} @{LOCAL,LOCAL} [two,one]: @LOCAL")}
        """.trimIndent()
        )
        assert(out == "<.1 Null>\n<.1 Null>\n") { out }
    }

    @Test
    fun ch_01_01_delta_pg03() {
        println("TODO")
    }

    // CHAPTER 1.2

    @Test
    fun ch_01_02_three_pg05() {
        val out = all(
            """
            $nums
            var f_three: func $D{} @{r1}-> $NumR1 -> $NumR1
            set f_three = func $D{} @{r1}-> $NumR1 -> $NumR1 {
                set ret = three
            }
            ${output0num("f_three $D{} @{LOCAL} one")}
        """.trimIndent()
        )
        assert(out == "<.1 <.1 <.1 Null>>>\n") { out }
    }
    @Disabled // TODO: infinite loop
    @Test
    fun ch_01_02_infinity_pg05() {
        val out = all(
            """
            var infinity: func $D{} @{r1}-> () -> $NumR1
            set infinity = func $D{} @{r1}-> () -> $NumR1 {
                ${output0num("_10:_int")}
                set ret = new $_NumR1 @r1 <.1 infinity() @r1>:</Num @{r1} @r1>
            }
            ${output0num("infinity $D{} @{LOCAL} ()")}
        """.trimIndent()
        )
        assert(out == "<.1 <.1 <.1 Null>>>\n") { out }
    }

    // CHAPTER 1.3

    @Test
    fun ch_01_03_multiply_pg09() {
        val out = all(
            """
            $nums
            $clone
            $add
            $mul
            var multiply: func $D{} @{r1,a1,b1}-> [$NumA1,$NumB1] -> $NumR1
            set multiply = func $D{} @{r1,a1,b1}-> [$NumA1,$NumB1] -> $NumR1 {
                if arg.1\~?Null {
                    set ret = Null:$NumR1
                } else {
                    set ret = mul $D{} @{r1,a1,b1} [arg.1,arg.2]: @r1
                }
            }
            ${output0num("multiply $D{} @{LOCAL,LOCAL,LOCAL} [two,three]")}
        """.trimIndent()
        )
        assert(out == "<.1 <.1 <.1 <.1 <.1 <.1 Null>>>>>>\n") { out }
    }

    // CHAPTER 1.4

    @Test
    fun ch_01_04_twice_pg11() {
        val out = all(
            """
            $nums
            $clone
            $add
            $mul
            var square: func $D{} @{r1,a1}-> $NumA1 -> $NumR1
            set square = func $D{} @{r1,a1}-> $NumA1 -> $NumR1 {
                set ret = mul $D{} @{r1,a1,a1} [arg,arg]: @r1
            }
            var twice: func $D{} @{r1,a1}-> [func $D{} @{r1,a1}-> $NumA1->$NumR1, $NumA1] -> $NumR1
            set twice = func $D{} @{r1,a1}-> [func $D{} @{r1,a1}-> $NumA1->$NumR1, $NumA1] -> $NumR1 {
                set ret = arg.1 $D{} @{r1,r1} (arg.1 $D{} @{r1,a1} arg.2: @r1): @r1
            }
            ${output0num("twice $D{} @{LOCAL,LOCAL} [square,two]: @LOCAL")}
        """.trimIndent()
        )
        assert(out == "<.1 <.1 <.1 <.1 <.1 <.1 <.1 <.1 <.1 <.1 <.1 <.1 <.1 <.1 <.1 <.1 Null>>>>>>>>>>>>>>>>\n") { out }
    }

    // CHAPTER 1.5

    @Test
    fun ch_01_05_fact_pg23 () {
        val out = all(
            """
            $nums
            $clone
            $add
            $mul
            
            var fact: func $D{} @{r1,a1}->$NumA1->$NumR1
            set fact = func $D{} @{r1,a1}->$NumA1->$NumR1 {
                if arg\~?Null {
                    set ret = new $_NumR1 <.1 Null:$NumR1>:</Num $D{} @{r1} @r1>: @r1
                } else {
                    var x: $NumTL
                    set x = fact $D{} @{LOCAL,a1} arg\~!1
                    set ret = mul $D{} @{r1,a1,LOCAL} [arg,x]: @r1
                }
            }
            
            ${output0num("fact $D{} @{LOCAL,LOCAL} three")}
        """.trimIndent()
        )
        assert(out == "<.1 <.1 <.1 <.1 <.1 <.1 Null>>>>>>\n") { out }
    }

    // CHAPTER 1.6
    // CHAPTER 1.7

    // CHAPTER 2.1

    val B = "<(),()>"
    val and = """
        var and: func $D{} @{} -> [$B,$B] -> $B
        set and = func $D{} @{} -> [$B,$B] -> $B {
            if arg.1?1 {
                set ret = <.1()>:<(),()>
            } else {
                set ret = arg.2
            }
        }        
    """.trimIndent()
    val or = """
        var or: func $D{} @{} -> [$B,$B] -> $B
        set or = func $D{} @{} -> [$B,$B] -> $B {
            if arg.1?2 {
                set ret = <.2()>:<(),()>
            } else {
                set ret = arg.2
            }
        }        
    """.trimIndent()
    val not = """
        var not: func $D{} @{} -> <(),()> -> <(),()>
        set not = func $D{} @{} -> <(),()> -> <(),()> {
            if arg?1 {
                set ret = <.2()>:<(),()>
            } else {
                set ret = <.1()>:<(),()>
            }
        }        
    """.trimIndent()

    val beq = """
        var beq: func $D{} @{} -> [$B,$B] -> $B
        set beq = func $D{} @{} -> [$B,$B] -> $B {
            set ret = or $D{} @{} [and $D{} @{} arg, and $D{} @{} [not $D{} @{} arg.1, not $D{} @{} arg.2]] 
        }
        var bneq: func $D{} @{} -> [$B,$B] -> $B
        set bneq = func $D{} @{} -> [$B,$B] -> $B {
            set ret = not $D{} @{} beq $D{} @{} arg 
        }        
    """.trimIndent()

    val ntob = """
        var ntob: func $D{} @{} -> _int -> $B
        set ntob = func $D{} @{} -> _int -> $B {
            if arg {
                set ret = <.2()>:$B
            } else {
                set ret = <.1()>:$B
            } 
        }
    """.trimIndent()

    val bton = """
        var bton: func $D{} @{} -> $B -> _int
        set bton = func $D{} @{} -> $B -> _int {
            if arg?2 {
                set ret = _1: _int
            } else {
                set ret = _0: _int
            } 
        }
    """.trimIndent()

    @Test
    fun ch_02_01_not_pg30 () {
        val out = all(
            """
            $Output0
            var not: func $D{} @{} -> <(),()> -> <(),()>
            set not = func $D{} @{} -> <(),()> -> <(),()> {
                if arg?1 {
                    set ret = <.2()>:<(),()>
                } else {
                    set ret = <.1()>:<(),()>
                }
            }
            var xxx: <(),()>
            set xxx = not $D{} @{} <.1()>:<(),()>
            ${output0("/xxx","/<(),()>@LOCAL")}
        """.trimIndent()
        )
        assert(out == "<.2>\n") { out }
    }

    @Test
    fun ch_02_01_and_pg30 () {
        val out = all("""
            $Output0
            var and: func $D{} @{} -> [$B,$B] -> $B
            set and = func $D{} @{} -> [$B,$B] -> $B {
                if arg.1?1 {
                    set ret = <.1()>:<(),()>
                } else {
                    set ret = arg.2
                }
            }
            var xxx: <(),()>
            set xxx = and $D{} @{} [<.1()>:<(),()>,<.2()>:<(),()>]
            ${output0("/xxx","/<(),()>@LOCAL")}
            set xxx = and $D{} @{} [<.2()>:<(),()>,<.2()>:<(),()>]
            ${output0("/xxx","/<(),()>@LOCAL")}
        """.trimIndent())
        assert(out == "<.1>\n<.2>\n") { out }
    }
    @Test
    fun ch_02_01_or_pg30 () {
        val out = all("""
            $Output0
            var or: func $D{} @{} -> [$B,$B] -> $B
            set or = func $D{} @{} -> [$B,$B] -> $B {
                if arg.1?2 {
                    set ret = <.2()>:<(),()>
                } else {
                    set ret = arg.2
                }
            }
            var xxx: <(),()>
            set xxx = or $D{} @{} [<.1()>:<(),()>,<.2()>:<(),()>]
            ${output0("/xxx","/<(),()>@LOCAL")}
            set xxx = or $D{} @{} [<.2()>:<(),()>,<.1()>:<(),()>]
            ${output0("/xxx","/<(),()>@LOCAL")}
            set xxx = or $D{} @{} [<.1()>:<(),()>,<.1()>:<(),()>]
            ${output0("/xxx","/<(),()>@LOCAL")}
        """.trimIndent()
        )
        assert(out == "<.2>\n<.2>\n<.1>\n") { out }
    }
    @Test
    fun ch_02_01_eq_neq_pg31 () {
        val out = all("""
            $Output0
            $not
            $and
            $or
            var eq: func $D{} @{} -> [$B,$B] -> $B
            set eq = func $D{} @{} -> [$B,$B] -> $B {
                set ret = or $D{} @{} [and $D{} @{} arg, and $D{} @{} [not $D{} @{} arg.1, not $D{} @{} arg.2]] 
            }
            var neq: func $D{} @{} -> [$B,$B] -> $B
            set neq = func $D{} @{} -> [$B,$B] -> $B {
                set ret = not $D{} @{} eq $D{} @{} arg 
            }
            var xxx: <(),()>
            set xxx = eq $D{} @{} [<.1()>:<(),()>,<.2()>:<(),()>]
            ${output0("/xxx","/<(),()>@LOCAL")}
            set xxx = neq $D{} @{} [<.2()>:<(),()>,<.1()>:<(),()>]
            ${output0("/xxx","/<(),()>@LOCAL")}
            set xxx = eq $D{} @{} [<.2()>:<(),()>,<.1()>:<(),()>]
            ${output0("/xxx","/<(),()>@LOCAL")}
            set xxx = eq $D{} @{} [<.1()>:<(),()>,<.1()>:<(),()>]
            ${output0("/xxx","/<(),()>@LOCAL")}
        """.trimIndent())
        assert(out == "<.1>\n<.2>\n<.1>\n<.2>\n") { out }
    }

    @Test
    fun ch_02_01_mod_pg33 () {
        val out = all("""
            $nums
            $clone
            $add
            $lt
            $sub
            var mod: func $D{} @{r1,a1,b1} -> [$NumA1,$NumB1] -> $NumR1
            set mod = func $D{} @{r1,a1,b1} -> [$NumA1,$NumB1] -> $NumR1 {
                if lt $D{} @{a1,b1} arg {
                    set ret = clone $D{} @{r1,a1} arg.1: @r1
                } else {
                    var v: $NumTL
                    set v = sub $D{} @{LOCAL,a1,b1} arg
                    set ret = mod $D{} @{r1,LOCAL,b1} [v,arg.2]: @r1
                }
            }
            var v: $NumTL
            set v = mod $D{} @{LOCAL,LOCAL,LOCAL} [three,two]
            ${output0num("v")}
        """.trimIndent()
        )
        assert(out == "<.1 Null>\n") { out }
    }

    @Disabled
    @Test
    fun ch_02_01_leap_pg33 () {
        if (VALGRIND != "") return
        val out = all(
            """
            $nums
            $clone
            $add
            $mul
            $lt
            $sub
            $mod
            $eq
            $or
            $and
            $ntob

            var n10: $NumTL
            set n10 = mul $D{} @{LOCAL,LOCAL,LOCAL} [five,two]
            var n100: $NumTL
            set n100 = mul $D{} @{LOCAL,LOCAL,LOCAL} [n10,n10]
            var n400: $NumTL
            set n400 = mul $D{} @{LOCAL,LOCAL,LOCAL} [four,n100]
            
            var leap: func $D{} @{a1} -> $NumA1 -> $B
            set leap = func $D{} @{a1} -> $NumA1 -> $B {
                var mod4: $NumTL
                set mod4 = mod $D{} @{LOCAL,a1,GLOBAL} [arg,four]
                var mod100: $NumTL
                set mod100 = mod $D{} @{LOCAL,a1,GLOBAL} [arg,n100]
                var mod400: $NumTL
                set mod400 = mod $D{} @{LOCAL,a1,GLOBAL} [arg,n400]
                set ret = or [ntob mod4\?Null, and [ntob mod100\?1, ntob mod400\?Null]]
            }
            
            var n2000: $NumTL
            set n2000 = mul $D{} @{LOCAL,LOCAL,LOCAL} [n400,five]
            var n20: $NumTL
            set n20 = add $D{} @{LOCAL,LOCAL,LOCAL} [n10,n10]
            var n1980: $NumTL
            set n1980 = sub $D{} @{LOCAL,LOCAL,LOCAL} [n2000,n20]
            var n1979: $NumTL
            set n1979 = sub $D{} @{LOCAL,LOCAL,LOCAL} [n1980,one]
            var x: $B
            set x = leap $D{} @{LOCAL} n1980
            ${output0num("/x")}
            set x = leap $D{} @{LOCAL} n1979
            ${output0num("/x")}
        """.trimIndent()
        )
        assert(out == "<.2>\n<.1>\n") { out }
    }

    @Test
    fun ch_02_01_triangles_pg33 () {
        val Tri = "<(),(),(),()>"
        val out = all(
            """
            type Error $D{} @{} = <_int>
            $nums
            $clone
            $add
            $mul
            $lt
            $sub
            $eq
            $lte
            $bton
            $ntob
            $or
            -- 125
            var analyse: func $D{} @{a1,b1,c1} -> [$NumA1,$NumB1,$NumC1] -> $Tri
            set analyse = func $D{} @{a1,b1,c1} -> [$NumA1,$NumB1,$NumC1] -> $Tri {
                ${catch0(1)} {
                    var xy: $NumTL
                    set xy = add $D{} @{LOCAL,a1,b1} [arg.1,arg.2]
                    if lte $D{} @{LOCAL,c1} [xy,arg.3] {
                        set ret = <.1()>:$Tri
                        ${throw0(1)}
                    } else {}
                    if eq $D{} @{a1,c1} [arg.1,arg.3] {
                        set ret = <.2()>:$Tri
                        ${throw0(1)}
                    } else {}
                    if bton $D{} @{} (or $D{} @{} [
                        ntob $D{} @{} (eq $D{} @{a1,b1} [arg.1,arg.2]),
                        ntob $D{} @{} (eq $D{} @{b1,c1} [arg.2,arg.3])
                    ]) {
                        set ret = <.3()>:$Tri
                        ${throw0(1)}
                    } else {}
                    set ret = <.4()>:$Tri
                }
            }
            var n10: $NumTL
            set n10 = mul $D{} @{LOCAL,LOCAL,LOCAL} [five,two]
            var v: $Tri
            set v = analyse $D{} @{LOCAL,LOCAL,LOCAL} [n10,n10,n10]
            ${output0("/v",'/'+Tri+"@LOCAL")}
            set v = analyse $D{} @{LOCAL,LOCAL,LOCAL} [one,five,five]
            ${output0("/v",'/'+Tri+"@LOCAL")}
            set v = analyse $D{} @{LOCAL,LOCAL,LOCAL} [one,one,five]
            ${output0("/v",'/'+Tri+"@LOCAL")}
            set v = analyse $D{} @{LOCAL,LOCAL,LOCAL} [two,four,five]
            ${output0("/v",'/'+Tri+"@LOCAL")}
        """.trimIndent()
        )
        assert(out == "<.2>\n<.3>\n<.1>\n<.4>\n") { out }
    }
}
