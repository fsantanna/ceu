import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.MethodOrderer.Alphanumeric
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder

@TestMethodOrder(Alphanumeric::class)
class TXTask {
    @Test
    fun a01_output () {
        val out = test(
            true, """
                $prelude1
                var f = task ()->()->() {
                    output std _1:_int
                }
                var x = spawn f ()
                output std _2:_int
            """.trimIndent()
        )
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun a02_await_err2 () {
        val out = test(
            true, """
                await ()
            """.trimIndent()
        )
        assert(out.startsWith("(ln 1, col 1): invalid condition : type mismatch")) { out }
    }
    @Test
    fun a02_await () {
        val out = test(
            true, """
                type Event = <(),_int>
                var f = task ()->()->() {
                    output std _1:_int
                    await evt?2
                    output std _3:_int
                }
                var x = spawn f ()
                output std _2:_int
                --awake x _1:_int
                emit @GLOBAL Event.2 _1:_int
            """.trimIndent()
        )
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun a02_await_err () {
        val out = test(
            true, """
                type Event = <(),_int>
                var f = task ()->()->() {
                    output std _1:_int
                    await evt?2
                    output std _3:_int
                }
                var x = spawn f ()
                output std _2:_int
                emit @GLOBAL <.2 _1>
                emit @GLOBAL <.2 _1>
            """.trimIndent()
        )
        //assert(out.endsWith("Assertion `(global.x)->task0.status == TASK_AWAITING' failed.\n")) { out }
        assert(out.endsWith("1\n2\n3\n")) { out }
    }
    @Test
    fun a03_var () {
        val out = test(
            true, """
                type Event = <(),_int>
                var f = task ()->()->() {
                    var x = _10:_int
                    await evt?2
                    output std x
                }
                var x = spawn f ()
                emit @GLOBAL <.2 _1>
            """.trimIndent()
        )
        assert(out == "10\n") { out }
    }
    @Test
    fun a04_vars () {
        val out = test(
            true, """
                type Event = <(),_int>
                var f = task @[]->()->()->() {
                    {
                        var x = _10:_int
                        await evt?2
                        output std x
                    }
                    {
                        var y = _20:_int
                        await evt?2
                        output std y
                    }
                }
                var x = spawn f ()
                emit @GLOBAL <.2 _1>
                emit @GLOBAL <.2 _1>
            """.trimIndent()
        )
        assert(out == "10\n20\n") { out }
    }
    @Test
    fun todo_a05_args_err () {
        val out = test(
            true, """
                var f : task ()->()->()
                var x : active task [()]->()->() = spawn f ()
            """.trimIndent()
        )
        assert(out == "(ln 2, col 36): invalid `spawn` : type mismatch :\n    active task @[] -> [()] -> () -> ()\n    active task @[] -> () -> () -> ()") { out }
    }
    @Test
    fun a05_args () {
        val out = test(
            true, """
                type Event = <(),_int>
                var f = task @[]->_(char*)->()->() {
                    output std arg
                    await evt?2
                    output std evt!2
                    await evt?2
                    output std evt!2
                }
                var x = spawn f _("hello")
                emit @GLOBAL <.2 _10>
                emit @GLOBAL <.2 _20>
            """.trimIndent()
        )
        assert(out == "\"hello\"\n10\n20\n") { out }
    }
    @Test
    fun a06_par_err () {
        val out = test(
            true, """
                type Event = <(),_int>
                var build = func () -> task ()->()->() {
                    set ret = task ()->()->() {    -- ERR: not the same @LOCAL
                        output std _1:_int
                        await evt?2
                        output std _2:_int
                    }
                }
            """.trimIndent()
        )
        assert(out.startsWith("(ln 3, col 13): invalid return : type mismatch")) { out }
    }
    @Test
    fun a06_par1 () {
        val out = test(
            true, """
                type Event = <(),_int>
                var build = task ()->()->() {
                    output std _1:_int
                    await evt?2
                    output std _2:_int
                }
                output std _10:_int
                var f = spawn build ()
                output std _11:_int
                var g = spawn build ()
                emit @GLOBAL <.2 _1>
                output std _12:_int
            """.trimIndent()
        )
        assert(out == "10\n1\n11\n1\n2\n2\n12\n") { out }
    }
    @Test
    fun a07_emit () {
        val out = test(
            true, """
                type Event = <(),_int,_int>
                var f = task ()->()->() {
                    await evt?3
                    var e = evt!3
                    output std _(${D}e+0):_int
                }
                var x = spawn f ()
                
                var g = task @[]->()->()->() {
                    await evt?3
                    var e1 = evt!3
                    output std _(${D}e1+10):_int
                    await evt?3
                    var e2 = evt!3
                    output std _(${D}e2+10):_int
                }
                var y = spawn g ()
                
                emit @GLOBAL Event.3 _1
                emit @GLOBAL Event.3 _2
            """.trimIndent()
        )
        assert(out == "1\n11\n12\n") { out }
    }
    @Test
    fun a08_emit_block () {
        val out = test(
            true, """
                type Event = <(),_uint64_t,_int>
                var f = task ()->()->() {
                    await evt?1
                    var e = evt!1
                    output std _0:_int    -- only on kill
                }
                var x = spawn f ()
                
                {
                    var g = task ()->()->() {
                        await evt?3
                        var e1 = evt!3
                        output std _(${D}e1+10):_int
                        await evt?3
                        var e2 = evt!3
                        output std _(${D}e2+10):_int
                    }
                    var y = spawn g ()
                    emit @LOCAL <.3 _1>
                    emit @LOCAL <.3 _2>
                }            
            """.trimIndent()
        )
        assert(out == "11\n12\n0\n") { out }
    }
    @Test
    fun a09_nest () {
        val out = test(
            true, """
                type Event = <(),_int>
                var f = task ()->()->() {
                    output std _1:_int
                    await evt?2
                    var g = task ()->()->() {
                        output std _2:_int
                        await evt?2
                        output std _3:_int
                    }
                    var xg = spawn g ()
                    await evt?2
                    output std _4:_int
                }
                var x = spawn f ()
                output std _10:_int
                emit @GLOBAL <.2 _1>
                output std _11:_int
                emit @GLOBAL <.2 _1>
                output std _12:_int
            """.trimIndent()
        )
        assert(out == "1\n10\n2\n11\n3\n4\n12\n") { out }
    }
    @Test
    fun a10_block_out () {
        val out = test(
            true, """
                type Event = <(),_int>
                var f = task ()->()->() {
                    output std _10:_int
                    {
                        var g = task ()->()->() {
                            output std _20:_int
                            await _1
                            output std _21:_int
                            await _1
                            if evt?1 {
                                output std _0:_int      -- only on kill
                            } else {
                                output std _22:_int     -- can't execute this one
                            }
                        }
                        var y = spawn g ()
                        await evt?2
                    }
                    output std _11:_int
                    var h = task ()->()->() {
                        output std _30:_int
                        await evt?2
                        output std _31:_int
                    }
                    var z = spawn h ()
                    await evt?2
                    output std _12:_int
                }
                var x = spawn f ()
                emit @GLOBAL <.2 _1>
                emit @GLOBAL <.2 _1>
            """.trimIndent()
        )
        assert(out == "10\n20\n21\n0\n11\n30\n31\n12\n") { out }
    }
    @Test
    fun a11_self_kill () {
        val out = test(
            true, """
                type Event = <(),_int>
                var g = task ()->()->() {
                    var f = task ()->()->() {
                        output std _1:_int
                        await evt?2
                        output std _4:_int
                        emit @GLOBAL <.2 _1>
                        output std _999:_int
                    }
                    var x = spawn f ()
                    output std _2:_int
                    await evt?2
                    output std _5:_int
                }
                output std _0:_int
                var y = spawn g ()
                output std _3:_int
                emit @GLOBAL <.2 _1>
                output std _6:_int
           """.trimIndent()
        )
        assert(out == "0\n1\n2\n3\n4\n5\n6\n") { out }
    }
    @Test
    fun a12_self_kill () {
        val out = test(
            true, """
                type Event = <(),_int>
                var g = task ()->()->() {
                    var f = task ()->()->() {
                        output std _1:_int
                        await evt?2
                        output std _4:_int
                        var kkk = func ()->() {
                            emit @GLOBAL <.2 _1>
                        }
                        call kkk ()
                        output std _999:_int
                    }
                    var x = spawn f ()
                    output std _2:_int
                    await evt?2
                    output std _5:_int
                }
                output std _0:_int
                var y = spawn g ()
                output std _3:_int
                emit @GLOBAL <.2 _1>
                output std _6:_int
           """.trimIndent()
        )
        assert(out == "0\n1\n2\n3\n4\n5\n6\n") { out }
    }

    // DEFER

    @Test
    fun b01_defer () {
        val out = test(
            true, """
                type Event = <(),_int>
                var f = task ()->()->() {
                    var defer_ = task ()->()->() {
                        await evt?1
                        output std _2:_int
                    }
                    var xdefer = spawn defer_ ()
                    output std _0:_int
                    await evt?2
                    output std _1:_int
                }
                var x = spawn f ()
                emit @GLOBAL <.2 _1>
            """.trimIndent()
        )
        assert(out == "0\n1\n2\n") { out }
    }
    @Test
    fun b02_defer_block () {
        val out = test(
            true, """
                type Event = <(),_int>
                var f = task ()->()->() {
                    {
                        var defer_ = task ()->()->() {
                            await evt?1
                            output std _2:_int
                        }
                        var xdefer = spawn defer_ ()
                        output std _0:_int
                        await evt?2
                    }
                    output std _1:_int
                }
                var x = spawn f ()
                emit @GLOBAL <.2 _1>
            """.trimIndent()
        )
        assert(out == "0\n2\n1\n") { out }
    }

    // THROW / CATCH

    @Test
    fun c00_err () {
        val out = test(
            true, """
                var f : task ()->()->()
                var x : task ()->()->()
                set x = spawn f ()
            """.trimIndent()
        )
        assert(out.startsWith("(ln 3, col 9): invalid `spawn` : type mismatch : expected active task")) { out }
    }
    @Test
    fun c00_throw () {
        val out = test(
            true, """
                type Error = <()>
                type Event = <(),_int>
                var h = task ()->()->() {
                   catch _1 {
                        var f = task ()->()->() {
                            await _1
                            output std _1:_int
                        }
                        var x = spawn f ()
                        throw Error.1
                   }
                   output std _2:_int
               }
               var z = spawn h ()
               output std _3:_int
            """.trimIndent()
        )
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun c01_throw () {
        val out = test(
            true, """
                type Error = <()>
                type Event = <(),_int,_int>
                var h = task ()->()->() {
                    catch _1 {
                        var f = task ()->()->() {
                            await evt?3
                            output std _999:_int
                        }
                        var g = task ()->()->() {
                            await _1
                            output std _1:_int
                        }
                        var x = spawn f ()
                        var y = spawn g ()
                        output std _0:_int
                        throw Error.1
                        output std _999:_int
                    }
                    output std _2:_int
               }
               var z = spawn h ()
               output std _3:_int
            """.trimIndent()
        )
        assert(out == "0\n1\n2\n3\n") { out }
    }
    @Test
    fun c02_throw_par2 () {
        val out = test(
            true, """
                type Error = <()>
                type Event = <(),_int,_int>
                var main = task ()->()->() {
                    var fg = task ()->()->() {
                        var f = task ()->()->() {
                            await evt?3
                            output std _999:_int
                        }
                        var g = task ()->()->() {
                            await evt?1
                            output std _2:_int
                        }
                        await evt?3
                        var xf = spawn f ()
                        var xg = spawn g ()
                        throw Error.1
                    }
                    var h = task ()->()->() {
                        await evt?1
                        output std _1:_int
                    }
                    var xfg : active task @[]->()->()->()
                    var xh : active task @[]->()->()->()
                    catch _1 {
                        set xfg = spawn fg ()
                        set xh = spawn h ()
                        emit @GLOBAL <.3 _5>
                        output std _999:_int
                    }
                }
                var xmain = spawn main ()
                output std _3:_int
            """.trimIndent()
        )
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun c03_throw_func () {
        val out = test(
            true, """
                type Error = <()>
                type Event = <(),_int>
                var xxx = func ()->() {
                    throw Error.1
                }
                var h = task ()->()->() {
                   catch _1 {
                        var f = task ()->()->() {
                            await _1
                            output std _1:_int
                        }
                        var xf: active task ()->()->()
                        set xf = spawn f ()
                        call xxx ()
                        output std _999:_int
                   }
                   output std _2:_int
               }
               var xh = spawn h ()
               output std _3:_int
            """.trimIndent()
        )
        assert(out == "1\n2\n3\n") { out }
    }

    // FIELDS

    @Test
    fun d01_field () {
        val out = test(
            true, """
                var f = task ()->_int->() {
                    set pub = _3
                    output std _1:_int
                }
                var xf = spawn f ()
                output std _2:_int
                output std xf.pub
                set xf.pub = _4
                output std xf.pub
            """.trimIndent()
        )
        assert(out == "1\n2\n3\n4\n") { out }
    }

    // SPAWN / DYNAMIC

    @Test
    fun e01_spawn () {
        val out = test(
            true, """
                spawn task @[]->()->()->() {
                    output std ()
                } ()
            """.trimIndent()
        )
        //assert(out == "(ln 2, col 5): expected `in` : have end of file") { out }
        assert(out == "()\n") { out }
    }
    @Test
    fun e01_spawn_err2 () {
        val out = test(
            true, """
                var f : func ()->()
                var fs : active {} task ()->()->()
                spawn f () in fs
            """.trimIndent()
        )
        //assert(out == "(ln 3, col 7): invalid `spawn` : type mismatch : expected task : have func @[] -> () -> ()") { out }
        assert(out == "(ln 3, col 7): invalid spawn : expected task") { out }
    }
    @Test
    fun e01_spawn_err3 () {
        val out = test(
            true, """
                var f : task ()->()->()
                spawn f () in ()
            """.trimIndent()
        )
        assert(out == "(ln 2, col 15): invalid `spawn` : type mismatch : expected active tasks : have ()") { out }
    }
    @Test
    fun e01_spawn_err4 () {
        val out = test(
            true, """
                var f : task ()->()->()
                var fs : active {} task [()]->()->()
                spawn f () in fs
            """.trimIndent()
        )
        assert(out == "(ln 3, col 1): invalid `spawn` : type mismatch :\n    task @[] -> [()] -> () -> ()\n    task @[] -> () -> () -> ()") { out }
    }
    @Test
    fun e02_spawn_free () {
        val out = test(
            true, """
                type Event = <(),_int>
                var f = task ()->()->() {
                    output std _1:_int
                    await evt?2
                    output std _3:_int
                }
                var fs : active {} task ()->()->()
                spawn f () in fs
                output std _2:_int
                emit @GLOBAL <.2 _1>
                output std _4:_int
            """.trimIndent()
        )
        assert(out == "1\n2\n3\n4\n") { out }
    }

    // POOL / TASKS / LOOPT

    @Test
    fun f01_err () {
        val out = test(
            true, """
                var xs: active {} task ()->_int->()
                var x:  task ()->_int->()
                loop x in xs {
                }
            """.trimIndent()
        )
        assert(out == "(ln 3, col 6): invalid `loop` : type mismatch : expected task type : have task @[] -> () -> _int -> ()") { out }

    }
    @Test
    fun f02_err () {
        val out = test(
            true, """
                var xs: active {} task [()]->_int->()
                var x:  active task ()->_int->()
                loop x in xs {
                }
            """.trimIndent()
        )
        assert(out == "(ln 3, col 1): invalid `loop` : type mismatch :\n    active task @[] -> () -> _int -> ()\n    active {} task @[] -> [()] -> _int -> ()") { out }

    }
    @Test
    fun f03_err () {
        val out = test(
            true, """
                var x: ()
                loop x in () {
                }
            """.trimIndent()
        )
        assert(out == "(ln 2, col 6): invalid `loop` : type mismatch : expected task type : have ()") { out }
    }
    @Test
    fun f04_err () {
        val out = test(
            true, """
                var x: active task ()->_int->()
                loop x in () {
                }
            """.trimIndent()
        )
        assert(out == "(ln 2, col 11): invalid `loop` : type mismatch : expected tasks type : have ()") { out }
    }

    @Test
    fun f05_loop () {
        val out = test(
            true, """
                var fs: active {} task ()->_int->()
                var f: active task ()->_int->()
                loop f in fs {
                }
                output std ()
            """.trimIndent()
        )
        assert(out == "()\n") { out }
    }

    @Test
    fun f06_pub () {
        val out = test(
            true, """
                type Event = <(),_int,_int>
                var f = task ()->_int->() {
                    set pub = _3
                    output std _1:_int
                    await evt?3
                }
                var fs: active {} task ()->_int->()
                spawn f () in fs
                var x: active task ()->_int->()
                loop x in fs {
                    output std x.pub
                }
            """.trimIndent()
        )
        assert(out == "1\n3\n") { out }
    }

    @Test
    fun f07_kill () {
        val out = test(
            true, """
                var f : task ()->_int->()
                set f = task ()->_int->() {
                    set pub = _3
                    output std _1:_int
                }
                var fs: active {} task ()->_int->()
                spawn f () in fs
                var x: active task ()->_int->()
                loop x in fs {
                    output std x.pub
                }
            """.trimIndent()
        )
        assert(out == "1\n") { out }
    }

    @Test
    fun f08_natural () {
        val out = test(
            true, """
                type Event = <(),_int,_int>
                var f = task _int->_int->() {
                    set pub = arg
                    output std pub
                    await evt?3
                }
                var g = task _int->_int->() {
                    set pub = arg
                    output std pub
                    await evt?3
                    await evt?3
                }
    
                var xs: active {} task _int->_int->()
                spawn f _1 in xs
                spawn g _2 in xs
    
                var x: active task _int->_int->()
                loop x in xs {
                    output std x.pub
                }
                
                emit @GLOBAL <.3 _10>
                
                loop x in xs {
                    output std x.pub
                }
                
                output std ()
            """.trimIndent()
        )
        assert(out == "1\n2\n1\n2\n2\n()\n") { out }
    }

    @Disabled
    @Test
    fun c03_try_catch () {
        val out = test(
            true, """
                catch (file not found) {
                    var f = open ()
                    defer {
                        call close f
                    }
                    loop {
                        var c = read f
                        ... throw err ...
                    }
                }
            """.trimIndent()
        )
        assert(out == "0\n1\n2\n") { out }
    }

    @Test
    fun f09_dloop_kill () {
        val out = test(
            true, """
                type Event = <(),_int,_int>
                var f = task ()->_int->() {
                    set pub = _10
                    output std _1:_int
                    await _1
                }
                var fs: active {} task ()->_int->()
                spawn f () in fs
                var x: active task ()->_int->()
                loop x in fs {
                    emit @GLOBAL <.2 _5>
                    --emit @GLOBAL <.2 _5>
                    output std x.pub
                }
            """.trimIndent()
        )
        assert(out == "1\n10\n") { out }
    }
    @Test
    fun f09_dloop_kill2 () {
        val out = test(
            true, """
                type Event = <(),_int,_int>
                var f = task ()->_int->() {
                    set pub = _10
                    output std _1:_int
                    await _1
                }
                var fs: active {} task ()->_int->()
                spawn f () in fs
                var x: active task ()->_int->()
                loop x in fs {
                    var y: active task ()->_int->()
                    loop y in fs {
                        emit @GLOBAL <.2 _5>
                        output std x.pub
                    }
                    emit @GLOBAL <.2 _5>
                }
            """.trimIndent()
        )
        assert(out == "1\n10\n") { out }
    }

    @Test
    fun f10_task_type () {
        val out = test(
            true, """
                type Xask = task ()->_int->()
                var t : Xask
                set t = Xask {
                    set pub = _10:_int
                    output std _2:_int
                }
                output std _1:_int
                var x : active Xask
                set x = spawn t ()
                var y = spawn t ()
                output std x.pub
                output std _3:_int
            """.trimIndent()
        )
        assert(out == "1\n2\n2\n10\n3\n") { out }
    }
    @Test
    fun fxx_task_type () {
        val out = test(
            true, """
                type Xask = task ()->()->()
                var t = Xask {}
                output std ()
            """.trimIndent()
        )
        assert(out == "()\n") { out }
    }
    @Test
    fun f11_task_type () {
        val out = test(
            true, """
                type Xask = task ()->()->()
                var t : Xask
                set t = Xask {
                    output std _2:_int
                }
                output std _1:_int
                var xs : active {} Xask
                spawn t () in xs
                spawn t () in xs
                output std _3:_int
            """.trimIndent()
        )
        assert(out == "1\n2\n2\n3\n") { out }
    }
    @Test
    fun f12_task_type () {
        val out = test(
            true, """
                type Event = <()>
                type Pair = [_int,_int]
                type Xask = task @[] -> () -> Pair -> ()
                var t = Xask {
                    set pub = [_10,_20]
                    await _0
                }
                var xs: active {} Xask
                spawn t () in xs
                var i: active Xask
                loop i in xs {
                    output std i.pub.1
                }
            """.trimIndent()
        )
        assert(out == "10\n") { out }
    }
    @Test
    fun f13_task_type () {
        val out = test(
            true, """
                type Xunc = func ()->()
                var f = Xunc {
                    output std _1:_int
                }
                call f () 
                type Xask = task ()->()->()
                var t = Xask {
                    output std _2:_int
                }
                var x = spawn t ()
                output std _3:_int
            """.trimIndent()
        )
        assert(out == "1\n2\n3\n") { out }
    }

    @Test
    fun f14_task_type () {
        val out = test(
            true, """
                type Event = <()>
                var n = _0:_int
                type Xask = task () -> () -> ()
                var t = Xask {
                    set n = _(${D}n+1)
                    await _0:_int
                }
                
                var xs: active {2} Xask
                spawn t () in xs
                spawn t () in xs
                spawn t () in xs
                spawn t () in xs
                output std n
            """.trimIndent()
        )
        assert(out == "2\n") { out }
    }
    @Test
    fun f15_task_type () {
        val out = test(
            true, """
                type Event = <()>
                type Pair = [_int,_int]
                type Xask = task @[] -> () -> _int -> ()
                var t = Xask {
                    set pub = _10
                    await _0
                }
                var xs: active {} Xask
                spawn t () in xs
                spawn {
                    var i: active Xask
                    loop i in xs {
                        output std i.pub
                    }
                }
            """.trimIndent()
        )
        assert(out == "10\n") { out }
    }

    // EMIT LOCAL

    @Test
    fun g01_local () {
        val out = test(
            true, """
                type Event = <(),_uint64_t,_int>
                var f = task @[]->()->()->() {
                    output std _1:_int
                    await evt?3
                    output std _2:_int
                }
                var x1 = spawn f ()
                var x2 = spawn f ()
                emit x1 Event.3 _1
                output std _3:_int
            """.trimIndent()
        )
        assert(out == "1\n1\n2\n3\n") { out }
    }

    @Test
    fun g02_spawn_abort () {
        val out = test(
            true, """
                type Event = <(),_uint64_t>
                type Event += <(),()>
                var t: task @[] -> () -> () -> ()
                set t = task @[] -> () -> () -> () {
                    var v: _int
                    set v = _1:_int
                    loop {
                        output std v
                        await evt~?3
                        set v = _(${D}v+1):_int
                    }
                }
                
                var l: active task  @[] -> () -> () -> ()
                set l = spawn (task  @[] -> () -> () -> () {
                    loop {
                        var x: active task  @[] -> () -> () -> ()
                        set x = spawn t _1:_int
                        await evt~?4
                    }
                }) ()
                
                emit @GLOBAL Event <.3 ()>: <(),_uint64_t,(),()>
                emit @GLOBAL Event <.4 ()>: <(),_uint64_t,(),()>
                emit @GLOBAL Event <.3 ()>: <(),_uint64_t,(),()>
                
           """.trimIndent()
        )
        assert(out == "1\n2\n1\n2\n") { out }
    }
    // AWAIT / RETURN / SPAWN

    @Test
    fun h01_ret () {
        val out = test(
            true, """
                type Event = <(),_uint64_t,_int>
                var f = task @[]->_int->()->_int {
                    return arg
                }
                spawn {
                    var x = await spawn f _10
                    output std x
                }
            """.trimIndent()
        )
        assert(out == "10\n") { out }
    }
    @Test
    fun h02_ret () {
        val out = test(
            true, """
                type Event = <(),_uint64_t,_int>
                var f = task @[]->_int->()->_int {
                    --var v = arg
                    await evt?3
                    return arg
                }
                var x1: _int
                var x2: _int
                spawn {
                    set x1 = await spawn f _10
                    set x2 = await spawn f _20
                }
                emit @GLOBAL Event.3 _1
                emit @GLOBAL Event.3 _1
                output std x1
                output std x2
            """.trimIndent()
        )
        assert(out == "10\n20\n") { out }
    }
    @Test
    fun h02_ret_one () {
        val out = test(
            true, """
                type Event = <(),_uint64_t,_int>
                var f = task @[]->_int->()->_int {
                    --var v = arg
                    await evt?3
                    return arg
                }
                --var x1: _int
                var x2: _int
                spawn {
                    await spawn f _10
                    set x2 = await spawn f _20
                }
                emit @GLOBAL Event.3 _1
                emit @GLOBAL Event.3 _1
                --output std x1
                output std x2
            """.trimIndent()
        )
        assert(out == "20\n") { out }
    }
    @Test
    fun h03_ret () {
        val out = test(
            true, """
                type Event = <(),_uint64_t,_int>
                var f = task @[]->_int->()->_int {
                    var v = arg
                    await evt?3
                    return v
                }
                spawn {
                    var v = await spawn f _10
                    output std v
                }
                spawn {
                    var v = await spawn f _20
                    output std v
                }
                emit @GLOBAL Event.3 _1
            """.trimIndent()
        )
        assert(out == "10\n20\n") { out }
    }
    @Test
    fun h04_ret () {
        val out = test(
            true, """
                type Pico = <<()>>
                var x = Pico.1.1
                output std x!1!1
                spawn {
                    var y = Pico.1.1
                    output std y!1!1
                }
            """.trimIndent()
        )
        assert(out == "()\n()\n") { out }
    }
}
