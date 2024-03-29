import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.MethodOrderer.Alphanumeric
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder

private val nums = """
    type Num = </Num>    
    var zero:  /Num = Null
    var one:   /Num = new <.1 zero>
    var two:   /Num = new <.1 one>
    var three: /Num = new <.1 two>
    var four:  /Num = new <.1 three>
    var five:  /Num = new <.1 four>
""".trimIndent()

private val clone = """
    func clone: /Num -> /Num {
        if arg\?Null {
            return Null
        } else {
            return new <.1 clone arg\!1>
        }
    }
""".trimIndent()

private val add = """
    func add: [/Num,/Num] -> /Num {
        var x = arg.1
        var y = arg.2
        if y\?Null {
            return clone x
        } else {
            return new <.1 add [x,y\!1]>
        }
    }
""".trimIndent()

private val mul = """
    func mul: [/Num,/Num] -> /Num {
        var x = arg.1
        var y = arg.2
        if y\?Null {
            return Null
        } else {
            var z = mul [x, y\!1]
            return add [x,z]
        }
    }
""".trimIndent()

private val lt = """
    func lt: [/Num,/Num] -> _int {
        if arg.2\?Null {
            return _0
        } else {
            if arg.1\?Null {
                return _1
            } else {
                return lt [arg.1\!1,arg.2\!1]
            }
        }
    }
""".trimIndent()

private val sub = """
    func sub: [/Num,/Num] -> /Num {
        var x = arg.1
        var y = arg.2
        if x\?Null {
            return Null
        } else {
            if y\?Null {
                return clone x
            } else {
                return sub [x\!1,y\!1]
            }
        }
    }
""".trimIndent()

private val mod = """
    func mod: [/Num,/Num] -> /Num {
        if lt arg {
            return clone arg.1
        } else {
            var v = sub arg
            return mod [v,arg.2]
        }
    }    
""".trimIndent()

private val eq = """
    func eq: [/Num,/Num] -> _int {
        var x = arg.1
        var y = arg.2
        if x\?Null {
            return y\?Null
        } else {
            if y\?Null {
                return _0
            } else {
                return eq [x\!1,y\!1]
            }
        }
    }
""".trimIndent()

private val lte = """
    func lte: [/Num,/Num] -> _int {
        var islt = lt [arg.1\!1,arg.2\!1]
        var iseq = eq [arg.1\!1,arg.2\!1]
        return _(${D}islt || ${D}iseq)
    }
""".trimIndent()

@TestMethodOrder(Alphanumeric::class)
class TXBook {

    fun all (inp: String) {
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
    }

    @Test
    fun pre_01_nums() {
        val out = test(
            true, """
            type Num = </Num>
            var zero: /Num = Null
            var one:   Num = <.1 zero>
            var two:   Num = <.1 /one>
            output Std /two
        """.trimIndent()
        )
        assert(out == "<.1 <.1 Null>>\n") { out }
    }
    @Test
    fun pre_02_add() {
        val out = test(true, """
            $nums
            $clone
            $add
            var n = add [two,one]
            output Std n
        """.trimIndent())
        assert(out == "<.1 <.1 <.1 Null>>>\n") { out }
    }
    @Test
    fun pre_03_clone() {
        val out = test(true, """
            $nums
            $clone
            var n = clone two
            output Std n
        """.trimIndent())
        assert(out == "<.1 <.1 Null>>\n") { out }
    }
    @Test
    fun pre_04_mul() {
        val out = test(true, """
            $nums
            $clone
            $add
            $mul
            var x = add [two,one]
            var n = mul [two, x]
            output Std n
            --output Std mul [two, add [two,one]]
        """.trimIndent())
        assert(out == "<.1 <.1 <.1 <.1 <.1 <.1 Null>>>>>>\n") { out }
    }
    @Test
    fun pre_05_lt() {
        val out = test(true, """
            $nums
            $lt
            var n1 = lt [two, one]
            var n2 = lt [one, two]
            output Std n1
            output Std n2
        """.trimIndent())
        assert(out == "0\n1\n") { out }
    }
    @Test
    fun pre_06_sub() {
        val out = test(true, """
            $nums
            $clone
            $add
            $sub
            --var zzz = sub [three, two]
            --output Std zzz
            output Std sub [three, two]
        """.trimIndent())
        assert(out == "<.1 Null>\n") { out }
    }
    @Test
    fun pre_07_eq() {
        val out = test(true, """
            $nums
            $eq
            output Std eq [three, two]
            output Std eq [one, one]
        """.trimIndent())
        assert(out == "0\n1\n") { out }
    }

    // CHAPTER 1.1

    @Test
    fun ch_01_01_square_pg02() {
        val out = test(true, """
            $nums
            $clone
            $add
            $mul
            func square: /Num -> /Num {
                return mul [arg,arg]
            }
            output Std square two
        """.trimIndent())
        assert(out == "<.1 <.1 <.1 <.1 Null>>>>\n") { out }
    }

    @Test
    fun ch_01_01_smaller_pg02() {
        val out = test(true, """
            $nums
            $lt
            -- 20
            -- returns narrower scope, guarantees both alive
            func smaller: @{a1,a2: a2>a1} -> [/Num@{a1},/Num@a2] -> /Num@a2 {
                if lt arg {
                    return arg.1
                } else {
                    return arg.2
                }
            }
            output Std smaller [one,two]
            output Std smaller [two,one]
        """.trimIndent())
        assert(out == "<.1 Null>\n<.1 Null>\n") { out }
    }

    @Test
    fun ch_01_01_delta_pg03() {
        println("TODO")
    }

    // CHAPTER 1.2

    @Test
    fun ch_01_02_three_pg05() {
        val out = test(true, """
            $nums
            func f_three: /Num -> /Num {
                return three
            }
            output Std f_three one
        """.trimIndent())
        assert(out == "<.1 <.1 <.1 Null>>>\n") { out }
    }
    @Disabled // TODO: infinite loop
    @Test
    fun ch_01_02_infinity_pg05() {
        val out = test(true, """
            var infinity : func () -> /Num
            set infinity = func () -> /Num {
                output Std _10:_int
                return new <.1 infinity ()>
            }
            output Std infinity ()
        """.trimIndent())
        assert(out == "<.1 <.1 <.1 Null>>>\n") { out }
    }

    // CHAPTER 1.3

    @Test
    fun ch_01_03_multiply_pg09() {
        val out = test(true, """
            $nums
            $clone
            $add
            $mul
            func multiply: [/Num,/Num] -> /Num {
                if arg.1\?Null {
                    return Null
                } else {
                    return mul [arg.1,arg.2]
                }
            }
            output Std multiply [two,three]
        """.trimIndent())
        assert(out == "<.1 <.1 <.1 <.1 <.1 <.1 Null>>>>>>\n") { out }
    }

    // CHAPTER 1.4

    @Test
    fun ch_01_04_twice_pg11() {
        val out = test(true, """
            $nums
            $clone
            $add
            $mul
            func square: /Num -> /Num {
                return mul [arg,arg]
            }
            func twice: [func /Num->/Num, /Num] -> /Num {
                return arg.1 (arg.1 arg.2)
            }
            output Std twice [square,two]
        """.trimIndent())
        assert(out == "<.1 <.1 <.1 <.1 <.1 <.1 <.1 <.1 <.1 <.1 <.1 <.1 <.1 <.1 <.1 <.1 Null>>>>>>>>>>>>>>>>\n") { out }
    }

    // CHAPTER 1.5

    @Test
    fun ch_01_05_fact_pg23 () {
        val out = test(true, """
            $nums
            $clone
            $add
            $mul
            
            func fact: /Num->/Num {
                if arg\?Null {
                    return new <.1 Null>
                } else {
                    var x = fact arg\!1
                    return mul [arg,x]
                }
            }
            
            output Std fact three
        """.trimIndent())
        assert(out == "<.1 <.1 <.1 <.1 <.1 <.1 Null>>>>>>\n") { out }
    }

    // CHAPTER 1.6
    // CHAPTER 1.7

    // CHAPTER 2.1

    val B = "<(),()>"
    val and = """
        func and: [$B,$B] -> $B {
            if arg.1?1 {
                return <.1>:<(),()>
            } else {
                return arg.2
            }
        }        
    """.trimIndent()
    val or = """
        func or: [$B,$B] -> $B {
            if arg.1?2 {
                return <.2>:<(),()>
            } else {
                return arg.2
            }
        }        
    """.trimIndent()
    val not = """
        func not: <(),()> -> <(),()> {
            if arg?1 {
                return <.2>:<(),()>
            } else {
                return <.1>:<(),()>
            }
        }        
    """.trimIndent()

    val beq = """
        func beq: [$B,$B] -> $B {
            return or [and arg, and [not arg.1, not arg.2]] 
        }
        func bneq: [$B,$B] -> $B {
            return not beq arg 
        }        
    """.trimIndent()

    val ntob = """
        func ntob: _int -> $B {
            if arg {
                return <.2>:$B
            } else {
                return <.1>:$B
            } 
        }
    """.trimIndent()

    val bton = """
        func bton: $B -> _int {
            if arg?2 {
                return _1: _int
            } else {
                return _0: _int
            } 
        }
    """.trimIndent()

    @Test
    fun ch_02_01_not_pg30 () {
        val out = test(true, """
            func not: <(),()> -> <(),()> {
                if arg?1 {
                    return <.2>
                } else {
                    return <.1>
                }
            }
            var xxx = not <.1>
            output Std /xxx
        """.trimIndent())
        assert(out == "<.2>\n") { out }
    }

    @Test
    fun ch_02_01_and_pg30 () {
        val out = test(true, """
            func and: [$B,$B] -> $B {
                if arg.1?1 {
                    return <.1>
                } else {
                    return arg.2
                }
            }
            var xxx = and [<.1>,<.2>]
            output Std /xxx
            set xxx = and [<.2>,<.2>]
            output Std /xxx
        """.trimIndent())
        assert(out == "<.1>\n<.2>\n") { out }
    }
    @Test
    fun ch_02_01_or_pg30 () {
        val out = test(true, """
            func or: [$B,$B] -> $B {
                if arg.1?2 {
                    return <.2>
                } else {
                    return arg.2
                }
            }
            var xxx = or [<.1>,<.2>]
            output Std /xxx
            set xxx = or [<.2>,<.1>]
            output Std /xxx
            set xxx = or [<.1>,<.1>]
            output Std /xxx
        """.trimIndent())
        assert(out == "<.2>\n<.2>\n<.1>\n") { out }
    }
    @Test
    fun ch_02_01_eq_neq_pg31 () {
        val out = test(true, """
            $not
            $and
            $or
            func eq: [$B,$B] -> $B {
                return or [and arg, and [not arg.1, not arg.2]]
            }
            func neq: [$B,$B] -> $B {
                return not eq arg 
            }
            var xxx = eq [<.1>,<.2>]
            output Std /xxx
            set xxx = neq [<.2>,<.1>]
            output Std /xxx
            set xxx = eq [<.1>,<.1>]
            output Std /xxx
        """.trimIndent())
        assert(out == "<.1>\n<.2>\n<.2>\n") { out }
    }

    @Test
    fun ch_02_01_mod_pg33 () {
        val out = test(true, """
            $nums
            $clone
            $add
            $lt
            $sub
            -- 51
            func mod: [/Num,/Num] -> /Num {
                if lt arg {
                    return clone arg.1
                } else {
                    var v = sub arg
                    return mod [v,arg.2]
                }
            }
            var v = mod [three,two]
            output Std v
        """.trimIndent())
        assert(out == "<.1 Null>\n") { out }
    }

    @Disabled   // TODO: too slow
    @Test
    fun ch_02_01_leap_pg33 () {
        val out = test(true, """
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

            var n10 = mul [five,two]
            var n100 = mul [n10,n10]
            var n400 = mul [four,n100]
            
            func leap: /Num -> $B {
                var mod4 = mod [arg,four]
                var mod100 = mod [arg,n100]
                var mod400 = mod [arg,n400]
                return or [ntob mod4\?Null, and [ntob mod100\?1, ntob mod400\?Null]]
            }
            
            var n2000 = mul [n400,five]
            var n20 = add [n10,n10]
            var n1980 = sub [n2000,n20]
            var n1979 = sub [n1980,one]
            var x = leap n1980
            output Std /x
            set x = leap n1979
            output Std /x
        """.trimIndent())
        assert(out == "<.2>\n<.1>\n") { out }
    }

    @Test
    fun ch_02_01_triangles_pg33 () {
        val Tri = "<(),(),(),()>"
        val out = test(true, """
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
            -- 119
            func analyse: [/Num,/Num,/Num] -> $Tri {
                var xy = add [arg.1,arg.2]
                if lte[xy,arg.3] {
                    return <.1>
                }
                if eq [arg.1,arg.3] {
                    return <.2>:$Tri
                }
                if bton (or [
                    ntob (eq [arg.1,arg.2]),
                    ntob (eq [arg.2,arg.3])
                ]) {
                    return <.3>
                }
                return <.4>
            }
            var n10 = mul [five,two]
            var v = analyse [n10,n10,n10]
            output Std /v
            set v = analyse [one,five,five]
            output Std /v
            set v = analyse [one,one,five]
            output Std /v
            set v = analyse [two,four,five]
            output Std /v
        """.trimIndent())
        assert(out == "<.2>\n<.3>\n<.1>\n<.4>\n") { out }
    }
}
