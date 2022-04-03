import org.junit.jupiter.api.MethodOrderer.Alphanumeric
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder

@TestMethodOrder(Alphanumeric::class)
class TXPar {

    // SPAWN

    @Test
    fun a01_spawn () {
        val out = test(true, """
            spawn {
                output Std ()
            }
            spawn {
                output Std ()
            }
        """.trimIndent())
        //assert(out == "(ln 2, col 5): expected `in` : have end of file") { out }
        assert(out == "()\n()\n") { out }
    }
    @Test
    fun a02_spawn_var () {
        val out = test(true, """
            var x = ()
            spawn {
                output Std x
            }
            spawn {
                output Std x
            }
        """.trimIndent())
        //assert(out == "(ln 2, col 5): expected `in` : have end of file") { out }
        assert(out == "()\n()\n") { out }
    }
    @Test
    fun a03_spawn_spawn_var () {
        val out = test(true, """
            spawn {
                var x = ()
                spawn {
                    output Std x
                }
                spawn {
                    output Std x
                }
            }
        """.trimIndent())
        //assert(out == "(ln 2, col 5): expected `in` : have end of file") { out }
        assert(out == "()\n()\n") { out }
    }
    @Test
    fun a04_spawn_spawn_spawn_var () {
        val out = test(true, """
            spawn {
                var x = ()
                spawn {
                    spawn {
                        output Std x
                    }
                }
                spawn {
                    output Std x
                }
            }
        """.trimIndent())
        //assert(out == "(ln 2, col 5): expected `in` : have end of file") { out }
        assert(out == "()\n()\n") { out }
    }
    @Test
    fun a05_spawn_task () {
        val out = test(true, """
            var t = spawn {
                output Std ()
            }
        """.trimIndent())
        //assert(out == "(ln 2, col 5): expected `in` : have end of file") { out }
        assert(out == "()\n") { out }
    }
    @Test
    fun a6_dollar () {
        val out = test(true, """
            spawn {
                var x: _int
                set x = _10:_int
                spawn {
                    output Std _(${D}x): _int
                }
            }
        """.trimIndent())
        assert(out == "10\n") { out }
    }
    @Test
    fun a7_anon () {
        val out = test(true, """
            var t = task () -> _int -> () {
                spawn {
                    set pub = _10
                }
            }
            var xt = spawn t ()
            output Std xt.pub
        """.trimIndent())
        assert(out == "10\n") { out }
    }

    // PAR

    @Test
    fun b01_par () {
        val out = test(true, """
            --type Event = <(),_int>
            spawn {
                par {
                    output Std ()
                } with {
                    output Std ()
                }
                output Std ()   -- never printed
            }
        """.trimIndent())
        //assert(out == "(ln 2, col 5): expected `in` : have end of file") { out }
        assert(out == "()\n()\n") { out }
    }
    @Test
    fun b02_parand () {
        val out = test(true, """
            --type Event = <(),_uint64_t,()>
            spawn {
                parand {
                    await evt?3
                    output Std _1:_int
                } with {
                    output Std _2:_int
                    await evt?3
                }
                output Std _3:_int
            }
            emit @GLOBAL <.3 ()>
            output Std _4:_int
            
        """.trimIndent())
        assert(out == "2\n1\n3\n4\n") { out }
    }
    @Test
    fun b03_paror () {
        val out = test(true, """
            --type Event = <(),_uint64_t,()>
            spawn {
                paror {
                    await evt?3
                    await evt?3
                    output Std _1:_int
                } with {
                    await evt?3
                    output Std _2:_int
                }
                output Std _3:_int
            }
            emit @GLOBAL <.3 ()>
            output Std _4:_int
            
        """.trimIndent())
        assert(out == "2\n3\n4\n") { out }
    }
    @Test
    fun b04_watching () {
        val out = test(true, """
            --type Event = <(),_uint64_t,()>
            spawn {
                watching evt?3 {
                    await _0
                }
                output Std ()
            }
            emit @GLOBAL <.3 ()>
            
        """.trimIndent())
        assert(out == "()\n") { out }
    }
    @Test
    fun b05_spawn_every () {
        val out = test(true, """
            --type Event = <(),_uint64_t,_int>
            spawn {
                every evt?3 {
                    output Std ()
                }
            }
            emit @GLOBAL <.3 _10>
            emit @GLOBAL <.3 _10>
        """.trimIndent())
        assert(out == "()\n()\n") { out }
    }

    // WCLOCK

    @Test
    fun c01_clk () {
        val out = test(true, """
            --type Event = <(),_int,_int>
            var sub = func [_int,_int] -> _int {
                return _(${D}arg._1 - ${D}arg._2)
            }
            var lte = func [_int,_int] -> _int {
                return _(${D}arg._1 <= ${D}arg._2)
            }
            spawn {
                output Std _1:_int
                await 1s
                output Std _4:_int
            }
            output Std _2:_int
            emit @GLOBAL Event.3 _999
            output Std _3:_int
            emit @GLOBAL Event.3 _1
            output Std _5:_int
        """.trimIndent())
        assert(out == "1\n2\n3\n4\n5\n") { out }
    }
    @Test
    fun c02_clk () {
        val out = test(true, """
            --type Event = <(),_int,_int>
            var sub = func [_int,_int] -> _int {
                return _(${D}arg._1 - ${D}arg._2)
            }
            var lte = func [_int,_int] -> _int {
                return _(${D}arg._1 <= ${D}arg._2)
            }
            spawn {
                every 1s {
                    output Std _1:_int
                }
            }
            emit @GLOBAL Event.3 _1000
            emit @GLOBAL Event.3 _1000
            emit @GLOBAL Event.3 _1000
        """.trimIndent())
        assert(out == "1\n1\n1\n") { out }
    }

    // PAUSE

    @Test
    fun d01_pause () {
        val out = test(true, """
            type Event += <()>
            spawn {
                pauseon evt?3 {
                    output Std _1:_int
                    await evt?4
                    output Std _5:_int
                }
            }
            output Std _2:_int
            emit @GLOBAL Event.3 _1
            output Std _3:_int
            emit @GLOBAL Event.4
            emit @GLOBAL Event.3 _0
            output Std _4:_int
            emit @GLOBAL Event.4
        """.trimIndent())
        assert(out == "1\n2\n3\n4\n5\n") { out }
    }

    // DEFER

    @Test
    fun e01_defer () {
        val out = test(true, """
            --type Event = <(),_uint64_t,_int>
            spawn {
                defer {
                    output Std _2:_int
                }
                output Std _0:_int
                await evt?3
                output Std _1:_int
            }
            emit @GLOBAL Event.3 _1
        """.trimIndent())
        assert(out == "0\n1\n2\n") { out }
    }
    @Test
    fun e02_defer_block () {
        val out = test(true, """
            --type Event = <(),_uint64_t,_int>
            spawn {
                {
                    defer {
                        output Std _2:_int
                    }
                    output Std _0:_int
                    await evt?3
                }
                output Std _1:_int
            }
            emit @GLOBAL Event.3 _1
        """.trimIndent())
        assert(out == "0\n2\n1\n") { out }
    }
    @Test
    fun e03_defer_err () {
        val out = test(false, """
            defer {
                output Std _2:_int
            }
        """.trimIndent())
        assert(out == "(ln 11, col 31): undeclared type \"Event\"") { out }
    }

    //

    @Test
    fun f01_multi () {
        val out = test(true, """
            --type Event = <(),_uint64_t>
            task bbb: () -> () -> _int {
                output Std _111:_int
                return _3
            }            
            task aaa: () -> () -> _int {
                output Std _222:_int
                set ret = await spawn bbb ()
            }            
            spawn {
                var opt = await spawn aaa ()
                output Std opt
            }
        """.trimIndent())
        assert(out == "222\n111\n3\n") { out }
    }
    @Test
    fun f02_multi () {
        val out = test(true, """
            --type Event = <(),_uint64_t,()>
            task fff: () -> () -> () {
                output Std _222:_int
                await _0
            }
            spawn {
                output Std _111:_int
                await spawn fff ()
            }
            emit @GLOBAL Event.3
            emit @GLOBAL Event.3
            emit @GLOBAL Event.3
        """.trimIndent())
        assert(out == "111\n222\n") { out }
    }
    @Test
    fun f03_multi_err () {
        val out = test(
            true, """
                --type Event = <(),_uint64_t,()>
                task fff: () -> () -> () {
                }
                await fff ()
            """.trimIndent()
        )
        assert(out == "(ln 12, col 7): invalid call : unexpected task") { out }
    }
    @Test
    fun f04_multi_err () {
        val out = test(
            true, """
                --type Event = <(),_uint64_t,()>
                task fff: () -> () -> () {
                    output Std _222:_int
                    await _0
                }
                call fff ()
            """.trimIndent()
        )
        assert(out == "(ln 14, col 6): invalid call : unexpected task") { out }
    }
    @Test
    fun f05_multi_err () {
        val out = test(
            true, """
                --type Event = <(),_uint64_t,()>
                func fff: () -> () {
                    output Std _222:_int
                }
                spawn fff ()
            """.trimIndent()
        )
        assert(out == "(ln 13, col 7): invalid spawn : expected task") { out }
    }
    @Test
    fun f06_multi () {
        val out = test(true, """
            --type Event = <(),_uint64_t>
            task aaa: () -> () -> _int {
                spawn {
                    return _1
                }
                return _999
            }
            spawn {
                var opt = await spawn aaa ()
                output Std opt
            }
        """.trimIndent())
        assert(out == "1\n") { out }
    }
    @Test
    fun f07_multi () {
        val out = test(true, """
            --type Event = <(),_uint64_t>
            task aaa: () -> () -> _int {
                par {
                    await _0
                    return _999
                } with {
                    return _2
                }
                return _999
            }
            spawn {
                var opt = await spawn aaa ()
                output Std opt
            }
        """.trimIndent())
        assert(out == "2\n") { out }
    }
}