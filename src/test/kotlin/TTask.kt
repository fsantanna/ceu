import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.MethodOrderer.Alphanumeric
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import java.io.File

@TestMethodOrder(Alphanumeric::class)
class TTask {

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
        val out = all("""
            var f: task @[]->()->()->()
            set f = task @[]->()->()->() {
                output std _1:_int
            }
            var x : active task @[]->()->()->()
            set x = spawn f ()
            output std _2:_int
        """.trimIndent())
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun a01_output_anon () {
        val out = all("""
            var f: task @[]->()->()->()
            set f = task @[]->()->()->() {
                output std _1:_int
            }
            spawn f ()
            output std _2:_int
        """.trimIndent())
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun a02_await_err2 () {
        val out = all("""
            await ()
        """.trimIndent())
        assert(out.startsWith("(ln 1, col 1): invalid condition : type mismatch")) { out }
    }
    @Test
    fun a02_emit_err () {
        val out = all("""
            emit @GLOBAL _1:_int
        """.trimIndent())
        //assert(out == "(ln 1, col 1): invalid `emit` : type mismatch : expected Event : have _int") { out }
        assert(out == "(ln 1, col 1): invalid `emit` : undeclared type \"Event\"") { out }
    }
    @Test
    fun a02_await () {
        val out = all("""
            type Event = <(),_uint64_t,_int>
            var f : task @[]->()->()->()
            set f = task @[]->()->()->() {
                output std _1:_int
                await evt?3
                output std _3:_int
            }
            var x : active task @[]->()->()->()
            set x = spawn f ()
            output std _2:_int
            --awake x _1:_int
            emit @GLOBAL <.3 _1:_int>:<(),_uint64_t,_int>:+Event
        """.trimIndent())
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun a02_await_err () {
        val out = all("""
            type Event = <(),_uint64_t,_int>
            var f: task @[]->()->()->()
            set f = task @[]->()->()->() {
                output std _1:_int
                await evt?3
                output std _3:_int
            }
            var x : active task @[]->()->()->()
            set x = spawn f ()
            output std _2:_int
            --awake x _1:_int
            --awake x _1:_int
            emit @GLOBAL <.3 _1:_int>:<(),_uint64_t,_int>:+ Event
            emit @GLOBAL <.3 _1:_int>:<(),_uint64_t,_int>:+ Event
        """.trimIndent())
        //assert(out.endsWith("Assertion `(global.x)->task0.state == TASK_AWAITING' failed.\n")) { out }
        assert(out.endsWith("1\n2\n3\n")) { out }
    }
    @Test
    fun a03_var () {
        val out = all("""
            type Event = <(),_uint64_t,_int>
            var f: task @[]->()->()->()
            set f = task @[]->()->()->() {
                var x: _int
                set x = _10:_int
                await evt?3
                output std x
            }
            var x : active task @[]->()->()->()
            set x = spawn f ()
            --awake x _1:_int
            emit @GLOBAL <.3 _1:_int>:<(),_uint64_t,_int>:+ Event
        """.trimIndent())
        assert(out == "10\n") { out }
    }
    @Test
    fun a04_vars () {
        val out = all("""
            type Event = <(),_uint64_t,_int>
            var f: task @[]->()->()->()
            set f = task @[]->()->()->() {
                {
                    var x: _int
                    set x = _10:_int
                    await evt?3
                    output std x
                }
                {
                    var y: _int
                    set y = _20:_int
                    await evt?3
                    output std y
                }
            }
            var x : active task @[]->()->()->()
            set x = spawn f ()
            --awake x _1:_int
            --awake x _1:_int
            emit @GLOBAL <.3 _1:_int>:<(),_uint64_t,_int>:+ Event
            emit @GLOBAL <.3 _1:_int>:<(),_uint64_t,_int>:+ Event
        """.trimIndent())
        assert(out == "10\n20\n") { out }
    }
    @Test
    fun a05_args_err () {
        val out = all("""
            var f : task @[]->()->()->()
            var x : active task @[]->[()]->()->()
            set x = spawn f ()
        """.trimIndent())
        assert(out == "(ln 3, col 9): invalid `spawn` : type mismatch :\n    task @[] -> [()] -> () -> ()\n    task @[] -> () -> () -> ()") { out }
    }
    @Test
    fun a05_args () {
        val out = all("""
            type Event = <(),_uint64_t,_int>
            var f : task @[]->_(char*)->()->()
            set f = task @[]->_(char*)->()->() {
                output std arg
                await evt?3
                output std evt!3
                await evt?3
                output std evt!3
            }
            var x : active task @[]->_(char*)->()->()
            set x = spawn f _("hello"):_(char*)
            --awake x _10:_int
            --awake x _20:_int
            emit @GLOBAL <.3 _10:_int>:<(),_uint64_t,_int>:+ Event
            emit @GLOBAL <.3 _20:_int>:<(),_uint64_t,_int>:+ Event
        """.trimIndent())
        assert(out == "\"hello\"\n10\n20\n") { out }
    }
    @Test
    fun a05_args2 () {
        val out = all("""
            type Event = <(),_uint64_t,_int>
            var f : task @[]->_int->()->_int
            set f = task @[]->_int->()->_int {
                await evt?3
                output std arg
            }
            var x : active task @[]->_int->()->_int
            set x = spawn f _10:_int
            emit @GLOBAL <.3 _20:_int>:<(),_uint64_t,_int>:+ Event
        """.trimIndent())
        assert(out == "10\n") { out }
    }
    @Test
    fun a06_par_err () {
        val out = all("""
            var build : func @[] -> () -> task @[]->()->()->()
            set build = func @[] -> () -> task @[]->()->()->() {
                set ret = task @[]->()->()->() {    -- ERR: not the same @LOCAL
                    output std _1:_int
                    await _(${D}evt != 0):_int
                    output std _2:_int
                }
            }
        """.trimIndent())
        assert(out.startsWith("(ln 3, col 13): invalid return : type mismatch")) { out }
    }
    @Test
    fun a06_par1 () {
        val out = all("""
            type Event = <(),_uint64_t,_int>
            var build : task @[]->()->()->()
            set build = task @[]->()->()->() {
                output std _1:_int
                await evt?3
                output std _2:_int
            }
            output std _10:_int
            var f : active task @[]->()->()->()
            set f = spawn build ()
            output std _11:_int
            var g : active task @[]->()->()->()
            set g = spawn build ()
            output std _12:_int
            --awake f _1:_int
            --awake g _1:_int
            emit @GLOBAL <.3 _1:_int>:<(),_uint64_t,_int>:+ Event
            output std _13:_int
        """.trimIndent())
        assert(out == "10\n1\n11\n1\n12\n2\n2\n13\n") { out }
    }
    @Test
    fun a07_bcast () {
        val out = all("""
            type Event = <(),_uint64_t,_int>
            var f : task @[]->()->()->()
            set f = task @[]->()->()->() {
                await evt?3
                output std evt!3
            }
            var x : active task @[]->()->()->()
            set x = spawn f ()
            
            var g : task @[]->()->()->()
            set g = task @[]->()->()->() {
                await evt?3
                var e: _int
                set e = evt!3
                output std _(${D}e+10):_int
                await evt?3
                set e = evt!3
                output std _(${D}e+10):_int
            }
            var y : active task @[]->()->()->()
            set y = spawn g ()
            
            emit @GLOBAL <.3 _1:_int>:<(),_uint64_t,_int>:+ Event
            emit @GLOBAL <.3 _2:_int>:<(),_uint64_t,_int>:+ Event
        """.trimIndent())
        assert(out == "1\n11\n12\n") { out }
    }
    @Test
    fun a08_bcast_block () {
        val out = all("""
            type Event = <(),_uint64_t,_int>
            var f : task @[]->()->()->()
            set f = task @[]->()->()->() {
                await _1:_int
                var iskill: _int
                var istask: _int
                set iskill = evt?1
                set istask = evt?2
                native _(assert(${D}iskill || ${D}istask);)
                output std _0:_int    -- only on kill
            }
            var x : active task @[]->()->()->()
            set x = spawn f ()
            
            {
                var g : task @[]->()->()->()
                set g = task @[]->()->()->() {
                    await evt?3
                    var e: _int
                    set e = evt!3
                    output std _(${D}e+10):_int
                    await evt?3
                    set e = evt!3
                    output std _(${D}e+10):_int
                }
                var y : active task @[]->()->()->()
                set y = spawn g ()
                emit @LOCAL <.3 _1:_int>:<(),_uint64_t,_int>:+ Event
                emit @LOCAL <.3 _2:_int>:<(),_uint64_t,_int>:+ Event
            }            
        """.trimIndent())
        assert(out == "11\n12\n0\n") { out }
    }
    @Test
    fun a08_bcast_block2 () {
        val out = all("""
            type Event = <(),_uint64_t,_int>
            {
                var f : task @[]->()->()->()
                set f = task @[]->()->()->() {
                    await _1:_int
                    var iskill: _int
                    set iskill = evt?1
                    native _(assert(${D}iskill);)
                    output std _0:_int    -- only on kill
                }
                var x : active task @[]->()->()->()
                set x = spawn f ()
                
                {
                    var g : task @[]->()->()->()
                    set g = task @[]->()->()->() {
                        var e: _int
                        await evt?3
                        set e = evt!3
                        output std _(${D}e+10):_int
                        await evt?3
                        set e = evt!3
                        output std _(${D}e+10):_int
                    }
                    var y : active task @[]->()->()->()
                    set y = spawn g ()
                    emit @LOCAL <.3 _1:_int>:<(),_uint64_t,_int>:+ Event
                    emit @LOCAL <.3 _2:_int>:<(),_uint64_t,_int>:+ Event
                }
            }
        """.trimIndent())
        assert(out == "11\n12\n0\n") { out }
    }
    @Test
    fun a09_nest () {
        val out = all("""
            type Event = <(),_uint64_t,_int>
            var f : task @[]->()->()->()
            set f = task @[]->()->()->() {
                output std _1:_int
                await evt?3
                var g : task @[]->()->()->()
                set g = task @[]->()->()->() {
                    output std _2:_int
                    await evt?3
                    output std _3:_int
                }
                var xg : active task @[]->()->()->()
                set xg = spawn g ()
                await evt?3
                output std _4:_int
            }
            var x : active task @[]->()->()->()
            set x = spawn f ()
            output std _10:_int
            emit @GLOBAL <.3 _1:_int>:<(),_uint64_t,_int>:+ Event
            output std _11:_int
            emit @GLOBAL <.3 _1:_int>:<(),_uint64_t,_int>:+ Event
            output std _12:_int
        """.trimIndent())
        assert(out == "1\n10\n2\n11\n3\n4\n12\n") { out }
    }
    @Test
    fun a10_block_out () {
        val out = all("""
            type Event = <(),_uint64_t,_int>
            var f : task @[]->()->()->()
            set f = task @[]->()->()->() {
                output std _10:_int
                {
                    var g : task @[]->()->()->()
                    set g = task @[]->()->()->() {
                        output std _20:_int
                        await _1:_int
                        output std _21:_int
                        await _1:_int
                        if evt?1 {
                            output std _0:_int      -- only on kill
                        } else {
                            output std _22:_int     -- can't execute this one
                        }
                    }
                    var y : active task @[]->()->()->()
                    set y = spawn g ()
                    await evt?3
                }
                output std _11:_int
                var h : task @[]->()->()->()
                set h = task @[]->()->()->() {
                    output std _30:_int
                    await evt?3
                    output std _31:_int
                }
                var z : active task @[]->()->()->()
                set z = spawn h ()
                await evt?3
                output std _12:_int
            }
            var x : active task @[]->()->()->()
            set x = spawn f ()
            emit @GLOBAL <.3 _1:_int>:<(),_uint64_t,_int>:+ Event
            emit @GLOBAL <.3 _1:_int>:<(),_uint64_t,_int>:+ Event
        """.trimIndent())
        assert(out == "10\n20\n21\n0\n11\n30\n31\n12\n") { out }
    }
    @Test
    fun a11_self_kill () {
        val out = all("""
            type Event = <(),_uint64_t,_int>
            var g : task @[]->()->()->()
            set g = task @[]->()->()->() {
                var f : task @[]->()->()->()
                set f = task @[]->()->()->() {
                    output std _1:_int
                    await evt?3
                    output std _4:_int
                    emit @GLOBAL <.3 _1:_int>:<(),_uint64_t,_int>:+ Event
                    output std _999:_int
                }
                var x : active task @[]->()->()->()
                set x = spawn f ()
                output std _2:_int
                await evt?3
                output std _5:_int
            }
            output std _0:_int
            var y : active task @[]->()->()->()
            set y = spawn g ()
            output std _3:_int
            emit @GLOBAL <.3 _1:_int>:<(),_uint64_t,_int>:+ Event
            output std _6:_int
       """.trimIndent())
        assert(out == "0\n1\n2\n3\n4\n5\n6\n") { out }
    }
    @Test
    fun a12_self_kill () {
        val out = all("""
            type Event = <(),_uint64_t,_int>
            var g : task @[]->()->()->()
            set g = task @[]->()->()->() {
                var f : task @[]->()->()->()
                set f = task @[]->()->()->() {
                    output std _1:_int
                    await evt?3
                    output std _4:_int
                    var kkk : func @[]->()->()
                    set kkk = func @[]->()->() {
                        emit @GLOBAL <.3 _1:_int>:<(),_uint64_t,_int>:+ Event
                    }
                    call kkk ()
                    output std _999:_int
                }
                var x : active task @[]->()->()->()
                set x = spawn f ()
                output std _2:_int
                await evt?3
                output std _5:_int
            }
            output std _0:_int
            var y : active task @[]->()->()->()
            set y = spawn g ()
            output std _3:_int
            emit @GLOBAL <.3 _1:_int>:<(),_uint64_t,_int>:+ Event
            output std _6:_int
       """.trimIndent())
        assert(out == "0\n1\n2\n3\n4\n5\n6\n") { out }
    }

    // DEFER

    @Test
    fun b01_defer () {
        val out = all("""
            type Event = <(),_uint64_t,_int>
            var f : task @[]->()->()->()
            set f = task @[]->()->()->() {
                var defer : task @[]->()->()->()
                set defer = task @[]->()->()->() {
                    await evt?1
                    output std _2:_int
                }
                var xdefer : active task @[]->()->()->()
                set xdefer = spawn defer ()
                output std _0:_int
                await evt?3
                output std _1:_int
            }
            var x : active task @[]->()->()->()
            set x = spawn f ()
            --awake x _1:_int
            emit @GLOBAL <.3 _1:_int>:<(),_uint64_t,_int>:+ Event
        """.trimIndent())
        assert(out == "0\n1\n2\n") { out }
    }
    @Test
    fun b02_defer_block () {
        val out = all("""
            type Event = <(),_uint64_t,_int>
            var f : task @[]->()->()->()
            set f = task @[]->()->()->() {
                {
                    var defer : task @[]->()->()->()
                    set defer = task @[]->()->()->() {
                        await evt?1
                        output std _2:_int
                    }
                    var xdefer : active task @[]->()->()->()
                    set xdefer = spawn defer ()
                    output std _0:_int
                    await evt?3
                }
                output std _1:_int
            }
            var x : active task @[]->()->()->()
            set x = spawn f ()
            --awake x _1:_int
            emit @GLOBAL <.3 _1:_int>:<(),_uint64_t,_int>:+ Event
        """.trimIndent())
        assert(out == "0\n2\n1\n") { out }
    }

    // THROW / CATCH

    @Test
    fun c00_catch () {
        val out = all("""
           catch {
           }
        """.trimIndent())
        assert(out == "(ln 1, col 7): invalid `catch` : requires enclosing task") { out }
    }

    @Test
    fun c00_err () {
        val out = all("""
            var f : task @[]->()->()->()
            var x : task @[]->()->()->()
            set x = spawn f ()
        """.trimIndent())
        assert(out.startsWith("(ln 3, col 5): invalid `spawn` : type mismatch : expected active task")) { out }
    }
    @Test
    fun c00_throw () {
        val out = all("""
            type Event = <(),_uint64_t,_int>
            var h : task @[]->()->()->()
            set h = task @[]->()->()->() {
               catch {
                    var f : task @[]->()->()->()
                    set f = task @[]->()->()->() {
                        await evt?1
                        output std _1:_int
                    }
                    var x : active task @[]->()->()->()
                    set x = spawn f ()
                    throw
               }
               output std _2:_int
           }
           var z : active task @[]->()->()->()
           set z = spawn h ()
           output std _3:_int
        """.trimIndent())
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun c01_throw () {
        val out = all("""
            type Event = <(),_uint64_t,_int>
            var h : task @[]->()->()->()
            set h = task @[]->()->()->() {
                catch {
                    var f : task @[]->()->()->()
                    set f = task @[]->()->()->() {
                        await evt?3
                        output std _999:_int
                    }
                    var g : task @[]->()->()->()
                    set g = task @[]->()->()->() {
                        await evt?1
                        output std _1:_int
                    }
                    var x : active task @[]->()->()->()
                    set x = spawn f ()
                    var y : active task @[]->()->()->()
                    set y = spawn g ()
                    output std _0:_int
                    throw
                    output std _999:_int
                }
                output std _2:_int
           }
           var z : active task @[]->()->()->()
           set z = spawn h ()
           output std _3:_int
        """.trimIndent())
        assert(out == "0\n1\n2\n3\n") { out }
    }
    @Test
    fun c02_throw_par2 () {
        val out = all("""
            type Event = <(),_uint64_t,_int>
            var main : task @[]->()->()->()
            set main = task @[]->()->()->() {
                var fg : task @[]->()->()->()
                set fg = task @[]->()->()->() {
                    var f : task @[]->()->()->()
                    set f = task @[]->()->()->() {
                        await evt?3
                        output std _999:_int
                    }
                    var g: task @[]->()->()->()
                    set g = task @[]->()->()->() {
                        await evt?1
                        output std _2:_int
                    }
                    await evt?3
                    var xf : active task @[]->()->()->()
                    set xf = spawn f ()
                    var xg : active task @[]->()->()->()
                    set xg = spawn g ()
                    throw
                }
                var h : task @[]->()->()->()
                set h = task @[]->()->()->() {
                    await evt?1
                    output std _1:_int
                }
                var xfg : active task @[]->()->()->()
                var xh : active task @[]->()->()->()
                catch {
                    set xfg = spawn fg ()
                    set xh = spawn h ()
                    emit @GLOBAL <.3 _1:_int>:<(),_uint64_t,_int>:+ Event
                    output std _999:_int
                }
            }
            var xmain : active task @[]->()->()->()
            set xmain = spawn main ()
            output std _3:_int
        """.trimIndent())
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun c03_throw_func () {
        val out = all("""
            type Event = <(),_uint64_t,_int>
            var err : func @[]->()->()
            set err = func @[]->()->() {
                throw
            }
            var h : task @[]->()->()->()
            set h = task @[]->()->()->() {
               catch {
                    var f: task @[]->()->()->()
                    set f = task @[]->()->()->() {
                        await _1:_int
                        output std _1:_int
                    }
                    var xf: active task @[]->()->()->()
                    set xf = spawn f ()
                    call err ()
                    output std _999:_int
               }
               output std _2:_int
           }
           var xh : active task @[]->()->()->()
           set xh = spawn h ()
           output std _3:_int
        """.trimIndent())
        assert(out == "1\n2\n3\n") { out }
    }

    // FIELDS

    @Test
    fun d00_err () {
        val out = all("""
            var f : task @[]->()->_int->()
            set f.pub = _4:_int
        """.trimIndent())
        assert(out.startsWith("(ln 2, col 7): invalid \"pub\" : type mismatch : expected active task")) { out }
    }
    @Test
    fun d01_field () {
        val out = all("""
            var f : task @[]->()->_int->()
            set f = task @[]->()->_int->() {
                set pub = _3:_int
                output std _1:_int
            }
            var xf: active task @[]->()->_int->()
            set xf = spawn f ()
            output std _2:_int
            output std xf.pub
            set xf.pub = _4:_int
            output std xf.pub
        """.trimIndent())
        assert(out == "1\n2\n3\n4\n") { out }
    }

    // SPAWN / DYNAMIC

    @Test
    fun e01_spawn () {
        val out = all("""
            spawn task @[]->()->()->() {
                output std ()
            } ()
        """.trimIndent())
        //assert(out == "(ln 2, col 5): expected `in` : have end of file") { out }
        assert(out == "()\n") { out }
    }
    @Test
    fun e01_spawn_err2 () {
        val out = all("""
            var f : func @[]->()->()
            var fs : active {} task @[]->()->()->()
            spawn f () in fs
        """.trimIndent())
        assert(out.startsWith("(ln 3, col 7): invalid `spawn` : type mismatch : expected task")) { out }
    }
    @Test
    fun e01_spawn_err3 () {
        val out = all("""
            var f : task @[]->()->()->()
            spawn f () in ()
        """.trimIndent())
        assert(out.startsWith("(ln 2, col 15): invalid `spawn` : type mismatch : expected active tasks")) { out }
    }
    @Test
    fun e01_spawn_err4 () {
        val out = all("""
            var f : task @[]->()->()->()
            var fs : active {} task @[]->[()]->()->()
            spawn f () in fs
        """.trimIndent())
        assert(out == "(ln 3, col 1): invalid `spawn` : type mismatch :\n    task @[] -> [()] -> () -> ()\n    task @[] -> () -> () -> ()") { out }
    }
    @Test
    fun e02_spawn_free () {
        val out = all("""
            type Event = <(),_uint64_t,_int>
            var f : task @[]->()->()->()
            set f = task @[]->()->()->() {
                output std _1:_int
                await evt?3
                output std _3:_int
            }
            var fs : active {} task @[]->()->()->()
            spawn f () in fs
            output std _2:_int
            emit @GLOBAL <.3 _1:_int>:<(),_uint64_t,_int>:+ Event
            output std _4:_int
        """.trimIndent())
        assert(out == "1\n2\n3\n4\n") { out }
    }
    @Test
    fun e03_spawn_anon () {
        val out = all("""
            var t: task @[] -> () -> [_int] -> ()
            set t = task @[] -> () -> [_int] -> () {
                var xxx: _int
                spawn (task _ -> _ -> _ {
                    set pub = [_10:_int]
                    set xxx = _10:_int
                } @[] ())
            }
            var xt: active task @[] -> () -> [_int] -> ()
            set xt = spawn (t @[] ())
            output std xt.pub.1
        """.trimIndent())
        assert(out == "10\n") { out }
    }

    // POOL / TASKS / LOOPT

    @Test
    fun f01_err () {
        val out = all("""
            var xs: active {} task @[]->()->_int->()
            var x:  task @[]->()->_int->()
            loop x in xs {
            }
        """.trimIndent())
        assert(out.startsWith("(ln 3, col 6): invalid `loop` : type mismatch : expected task type")) { out }

    }
    @Test
    fun f02_err () {
        val out = all("""
            var xs: active {} task @[]->[()]->_int->()
            var x:  active task  @[]->()->_int->()
            loop x in xs {
            }
        """.trimIndent())
        assert(out == "(ln 3, col 1): invalid `loop` : type mismatch :\n    active task @[] -> () -> _int -> ()\n    active {} task @[] -> [()] -> _int -> ()") { out }

    }
    @Test
    fun f03_err () {
        val out = all("""
            var x: ()
            loop x in () {
            }
        """.trimIndent())
        assert(out.startsWith("(ln 2, col 6): invalid `loop` : type mismatch : expected task type")) { out }
    }
    @Test
    fun f04_err () {
        val out = all("""
            var x: active task @[]->()->_int->()
            loop x in () {
            }
        """.trimIndent())
        assert(out.startsWith("(ln 2, col 11): invalid `loop` : type mismatch : expected tasks type")) { out }
    }

    @Test
    fun f05_loop () {
        val out = all("""
            var fs: active {} task @[]->()->_int->()
            var f: active task @[]->()->_int->()
            loop f in fs {
            }
            output std ()
        """.trimIndent())
        assert(out == "()\n") { out }
    }

    @Test
    fun f06_pub () {
        val out = all("""
            type Event = <(),_uint64_t,_int>
            var f : task @[]->()->_int->()
            set f = task @[]->()->_int->() {
                set pub = _3:_int
                output std _1:_int
                await evt?3
            }
            var fs: active {} task @[]->()->_int->()
            spawn f () in fs
            var x: active task @[]->()->_int->()
            loop x in fs {
                output std x.pub
            }
        """.trimIndent())
        assert(out == "1\n3\n") { out }
    }

    @Test
    fun f07_kill () {
        val out = all("""
            var f : task @[]->()->_int->()
            set f = task @[]->()->_int->() {
                set pub = _3:_int
                output std _1:_int
            }
            var fs: active {} task @[]->()->_int->()
            spawn f () in fs
            var x: active task @[]->()->_int->()
            loop x in fs {
                output std x.pub
            }
        """.trimIndent())
        assert(out == "1\n") { out }
    }

    @Test
    fun f07_valgrind () {
        val out = all("""
            type Event = <(),_uint64_t,_int>
            var f : task @[]->()->()->()
            set f = task @[]->()->()->() {
                await evt?3
            }
            var xs: active {} task @[]->_int->_int->()
            spawn f () in xs
            emit @GLOBAL <.3 _10:_int>:<(),_uint64_t,_int>:+ Event
            output std ()
        """.trimIndent())
        assert(out == "()\n") { out }
    }

    @Test
    fun f08_natural () {
        val out = all("""
            type Event = <(),_uint64_t,_int>
            var f : task @[]->_int->_int->()
            set f = task @[]->_int->_int->() {
                set pub = arg
                output std pub
                await evt?3
            }
            var g : task @[]->_int->_int->()
            set g = task @[]->_int->_int->() {
                set pub = arg
                output std pub
                await evt?3
                await evt?3
            }

            var xs: active {} task @[]->_int->_int->()
            spawn f _1:_int in xs
            spawn g _2:_int in xs

            var x: active task @[]->_int->_int->()
            loop x in xs {
                output std x.pub
            }
            
            emit @GLOBAL <.3 _10:_int>:<(),_uint64_t,_int>:+ Event
            
            loop x in xs {
                output std x.pub
            }
            
            output std ()
        """.trimIndent())
        assert(out == "1\n2\n1\n2\n2\n()\n") { out }
    }

    @Test
    fun f09_dloop_kill () {
        val out = all("""
            type Event = <(),_uint64_t,_int>
            var f : task @[]->()->_int->()
            set f = task @[]->()->_int->() {
                set pub = _10:_int
                output std _1:_int
                await evt?3
            }
            var fs: active {} task @[]->()->_int->()
            spawn f () in fs
            var x: active task @[]->()->_int->()
            loop x in fs {
                emit @GLOBAL <.3 _10:_int>:<(),_uint64_t,_int>:+ Event
                output std x.pub
            }
        """.trimIndent())
        assert(out == "1\n10\n") { out }
    }

    @Test
    fun f10_track () {
        val out = all("""
            type Event = <(),_uint64_t,_int>
            var f : task @[]->()->_int->()
            set f = task @[]->()->_int->() {
                set pub = _3:_int
                output std _1:_int
                await evt?3
            }
            var fs: active {} task @[]->()->_int->()
            spawn f () in fs
            var y: active task @[]->()->_int->()
            var x: active task @[]->()->_int->()
            loop x in fs {
                set y = x
            }
            output std y.pub
        """.trimIndent())
        assert(out == "1\n3\n") { out }
    }

    // AWAIT TASK

    @Test
    fun g01_state () {
        val out = all("""
            type Event = <(),_uint64_t,_int>
            var f: task @[]->()->()->()
            set f = task @[]->()->()->() {
                await _1:_int
            }
            var x : active task @[]->()->()->()
            set x = spawn f ()
            spawn (task @[]->()->()->() {
                loop {
                    await evt?2
                    var t: _uint64_t
                    set t = evt!2
                    if _(${D}t == ((uint64_t)${D}x)):_int {
                        break
                    } else {}
                }
                output std _2:_int
            }) ()
            output std _1:_int
            emit @GLOBAL <.3 _1:_int>:<(),_uint64_t,_int>:+Event
            output std _3:_int
       """.trimIndent())
        assert(out == "1\n2\n3\n") { out }
    }

    @Test
    fun g02_kill () {
        val out = all("""
            type Event = <(),_uint64_t,_int>
            spawn (task @[]->()->()->() {
                loop {
--native _{puts("loop");}
                    var f: task @[]->()->()->()
                    set f = task @[]->()->()->() {
--native _{printf(">>> task %p\n", task0);}
                        await evt?3
                        output std _2:_int
--native _{printf("<<< task %p\n", task0);}
                    }
                    var x : active task @[]->()->()->()
                    set x = spawn f ()
                    loop {
                        await evt?2
                        var t: _uint64_t
                        set t = evt!2
                        if _(${D}t == ((uint64_t)${D}x)):_int {
                            break
                        } else {}
                    }
                    output std _3:_int
                }
            }) ()
            output std _1:_int
            emit @GLOBAL <.3 _1:_int>:<(),_uint64_t,_int>:+Event
            output std _4:_int
       """.trimIndent())
        assert(out == "1\n2\n3\n4\n") { out }
    }

    @Test
    fun g03_kill () {
        val out = all("""
            type Event = <(),_uint64_t,_int>
            spawn (task @[]->()->()->() {
                loop {
                    var f: task @[]->()->()->()
                    set f = task @[]->()->()->() {
                        await evt?3
                        output std _2:_int
                    }
                    var x : active task @[]->()->()->()
                    set x = spawn f ()
                    await x
                    output std _3:_int
                }
            }) ()
            output std _1:_int
            emit @GLOBAL <.3 _1:_int>:<(),_uint64_t,_int>:+Event
            output std _4:_int
       """.trimIndent())
        assert(out == "1\n2\n3\n4\n") { out }
    }
    @Test
    fun g03_kill_return () {
        val out = all("""
            type Event = <(),_uint64_t,_int>
            spawn (task @[]->()->()->() {
                loop {
                    var f: task @[]->()->()->_int
                    set f = task @[]->()->()->_int {
                        await evt?3
                        set ret = _10:_int
                    }
                    var x : active task @[]->()->()->_int
                    set x = spawn f ()
                    await x
                    output std x.ret
                }
            }) ()
            emit @GLOBAL <.3 _1:_int>:<(),_uint64_t,_int>:+Event
       """.trimIndent())
        assert(out == "10\n") { out }
    }


    @Test
    fun g03_f_kill () {
        val out = all("""
            var fff: func () -> () {}
            set fff = func () -> () {}
            spawn (task @[]->()->()->() {
                output std _111:_int
                {
                    call fff ()
                }
                output std _222:_int
            }) ()
       """.trimIndent())
        assert(out == "111\n222\n") { out }
    }

    @Test
    fun g04_err () {
        val out = all("""
            type Event = <(),_uint64_t,()>
            
            spawn (task  @[] -> () -> () -> () {
                output std (_1: _int)                                         
            
                var t1: active task @[] -> () -> () -> ()             
                set t1 = spawn (task @[] -> () -> () -> () {              
            
                    var t2: active task @[] -> () -> () -> ()        
                    set t2 = spawn (task @[] -> () -> () -> () {
                        output std _2:_int
                        await (_1: _int)                               
                        output std (_4: _int)                             
                    } @[] ())                          
            
                    await ((evt:- Event)?2)     
                    output std (_5: _int)                  
                } @[] ())                   
            
                await ((evt:- Event)?2)            
                output std (_6: _int)                                  
            } @[] ())
            
            output std _3:_int
            emit @GLOBAL (<.3 ()>: <(),_uint64_t,()>:+ Event)
            output std (_7: _int)
       """.trimIndent())
        assert(out == "1\n2\n3\n4\n5\n6\n7\n") { out }
    }

    @Test
    fun g05_spawn_abort () {
        val out = all("""
            type Event = <(),_uint64_t,(),()>
            var t: task @[] -> () -> () -> ()
            set t = task @[] -> () -> () -> () {
                var v: _int
                set v = _1:_int
                loop {
                    output std v
                    await evt?3
                    set v = _(${D}v+1):_int
                }
            }
            
            var l: active task  @[] -> () -> () -> ()
            set l = spawn (task  @[] -> () -> () -> () {
                loop {
                    var x: active task  @[] -> () -> () -> ()
                    set x = spawn t _1:_int
                    await evt?4
                }
            }) ()
            
            emit @GLOBAL <.3 ()>: <(),_uint64_t,(),()>:+Event
            emit @GLOBAL <.4 ()>: <(),_uint64_t,(),()>:+Event
            emit @GLOBAL <.3 ()>: <(),_uint64_t,(),()>:+Event
            
       """.trimIndent())
        assert(out == "1\n2\n1\n2\n") { out }
    }
    // TYPE TASK

    @Test
    fun h01_type_task () {
        val out = all("""
            type Event = <(),_uint64_t,()>
            type Bird = task  @[] -> () -> () -> ()
            
            var t1: Bird
            set t1 = task  @[] -> () -> () -> () {
                 output std _2:_int
            } :+ Bird
            
            var x1: active Bird
            output std _1:_int
            set x1 = spawn t1:-Bird ()
             output std _3:_int
       """.trimIndent())
        assert(out == "1\n2\n3\n") { out }
    }

    @Test
    fun h02_task_type () {
        val out = all("""
            type Xask = task ()->_int->()
            var t : Xask
            set t = task ()->_int->() {
                set pub = _10:_int
                output std _2:_int
            } :+ Xask
            var x : active Xask
            output std _1:_int
            set x = spawn (t:-Xask) ()
            output std _3:_int
            output std (x:-Xask).pub
        """.trimIndent())
        assert(out == "1\n2\n3\n10\n") { out }
    }

    @Test
    fun h03_task_type () {
        val out = all("""
            type Event = <()>
            type Xask @[] = task @[] -> () -> _int -> ()
            var t: Xask
            set t = task ()->_int->() {
                set pub = _10:_int
                await _0:_int
            } :+ Xask
            var xs: active {} Xask
            spawn ((t:- Xask) @[] ()) in xs
            var i: active Xask
            loop i in xs {
                output std (i:-Xask).pub
            }
        """.trimIndent())
        assert(out == "10\n") { out }
    }
    @Test
    fun h04_task_type () {
        val out = all("""
            type Event = <()>

            var n: _int
            set n = _0:_int

            type Xask @[] = task @[] -> () -> () -> ()
            var t: Xask
            set t = task ()->()->() {
                set n = _(${D}n+1):_int
                await _0:_int
            } :+ Xask
            
            var xs: active {2} Xask
            spawn ((t:- Xask) @[] ()) in xs
            spawn ((t:- Xask) @[] ()) in xs
            spawn ((t:- Xask) @[] ()) in xs
            spawn ((t:- Xask) @[] ()) in xs
            output std n
        """.trimIndent())
        assert(out == "2\n") { out }
    }

    @Test
    fun h05_task_type () {
        val out = all("""
            type Event = <()>

            var n: _int
            set n = _0:_int

            type Xask @[] = task @[] -> () -> () -> ()
            var t: Xask
            set t = task ()->()->() {
                set n = _(${D}n+1):_int
                --await _0:_int
            } :+ Xask
            
            var xs: active {2} Xask
            spawn ((t:- Xask) @[] ()) in xs
            spawn ((t:- Xask) @[] ()) in xs
            spawn ((t:- Xask) @[] ()) in xs
            spawn ((t:- Xask) @[] ()) in xs
            output std n
        """.trimIndent())
        assert(out == "4\n") { out }
    }

    // EMIT LOCAL

    @Test
    fun i01_local () {
        val out = all("""
            type Event = <(),_uint64_t,_int>
            var f : task @[]->()->()->()
            set f = task @[]->()->()->() {
                output std _1:_int
                await evt?3
                output std _2:_int
            }
            var x1 : active task @[]->()->()->()
            set x1 = spawn f ()
            var x2 : active task @[]->()->()->()
            set x2 = spawn f ()
            emit x1 <.3 _1:_int>:<(),_uint64_t,_int>:+Event
            output std _3:_int
        """.trimIndent())
        assert(out == "1\n1\n2\n3\n") { out }
    }

    @Test
    fun i02_err () {
        val out = all("""
            type Event = <(),_uint64_t,_int>
            var x1 : active task @[]->()->()->()
            emit @GLOBAL x1 ()
        """.trimIndent())
        assert(out == "(ln 3, col 14): invalid call : not a function") { out }
    }

    // PAUSE

    @Test
    fun j01_pause () {
        val out = all("""
            type Event = <(),_uint64_t,_int>
            var f : task @[]->()->()->()
            set f = task @[]->()->()->() {
                output std _1:_int
                await evt?3
                output std _5:_int
            }
            var x : active task @[]->()->()->()
            set x = spawn f ()
            output std _2:_int
            pause x
            output std _3:_int
            emit @GLOBAL <.3 _1:_int>:<(),_uint64_t,_int>:+Event
            resume x
            output std _4:_int
            emit @GLOBAL <.3 _1:_int>:<(),_uint64_t,_int>:+Event
        """.trimIndent())
        assert(out == "1\n2\n3\n4\n5\n") { out }
    }
}