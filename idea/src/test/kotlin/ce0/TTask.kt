import org.junit.jupiter.api.MethodOrderer.Alphanumeric
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder

@TestMethodOrder(Alphanumeric::class)
class TTask {

    @Test
    fun a01_output () {
        val out = test(false, """
            $Output0
            var f: task $D{} @{}->()->()->()
            set f = task $D{} @{}->()->()->() {
                ${output0("_1:_int","_int")}
            }
            var x : active task $D{} @{}->()->()->()
            set x = spawn f $D{} @{} ()
            ${output0("_2:_int","_int")}
            """.trimIndent()
        )
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun a01_output_anon () {
        val out = test(false, """
            $Output0
            var f: task $D{} @{}->()->()->()
            set f = task $D{} @{}->()->()->() {
                ${output0("_1:_int","_int")}
            }
            spawn f $D{} @{} ()
            ${output0("_2:_int","_int")}
        """.trimIndent())
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun a02_await_err2 () {
        val out = test(false, """
                await ()
            """.trimIndent()
        )
        assert(out.startsWith("(ln 1, col 1): invalid condition : type mismatch")) { out }
    }
    @Test
    fun a02_await_err3 () {
        val out = test(false, """
                type Event $D{} @{} = ()
                await evt?1
            """.trimIndent()
        )
        assert(out.startsWith("(ln 2, col 7): undeclared variable \"evt\"")) { out }
    }
    @Test
    fun a02_emit_err () {
        val out = test(false, """
                emit @GLOBAL _1:_int
            """.trimIndent()
        )
        //assert(out == "(ln 1, col 1): invalid `emit` : type mismatch : expected Event : have _int") { out }
        assert(out == "(ln 1, col 1): invalid `emit` : undeclared type \"Event\"") { out }
    }
    @Test
    fun a02_await () {
        val out = test(false, """
            $Output0
            type Event $D{} @{} = <(),_uint64_t,_int>
            var f : task $D{} @{}->()->()->()
            set f = task $D{} @{}->()->()->() {
                ${output0("_1:_int","_int")}
                await evt~?3
                ${output0("_3:_int","_int")}
            }
            var x : active task $D{} @{}->()->()->()
            set x = spawn f $D{} @{} ()
            ${output0("_2:_int","_int")}
            --awake x _1:_int
            emit @GLOBAL Event $D{} @{} <.3 _1:_int>:<(),_uint64_t,_int>
        """.trimIndent())
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun a02_await_err () {
        val out = test(false, """
            $Output0
            type Event $D{} @{} = <(),_uint64_t,_int>
            var f: task $D{} @{}->()->()->()
            set f = task $D{} @{}->()->()->() {
                ${output0("_1:_int","_int")}
                await evt~?3
                ${output0("_3:_int","_int")}
            }
            var x : active task $D{} @{}->()->()->()
            set x = spawn f $D{} @{} ()
            ${output0("_2:_int","_int")}
            --awake x _1:_int
            --awake x _1:_int
            emit @GLOBAL Event $D{} @{} <.3 _1:_int>:<(),_uint64_t,_int>
            emit @GLOBAL Event $D{} @{} <.3 _1:_int>:<(),_uint64_t,_int>
        """.trimIndent())
        //assert(out.endsWith("Assertion `(global.x)->task0.status == TASK_AWAITING' failed.\n")) { out }
        assert(out.endsWith("1\n2\n3\n")) { out }
    }
    @Test
    fun a03_var () {
        val out = test(false, """
            $Output0
            type Event $D{} @{} = <(),_uint64_t,_int>
            var f: task $D{} @{}->()->()->()
            set f = task $D{} @{}->()->()->() {
                var x: _int
                set x = _10:_int
                await evt~?3
                ${output0("x","_int")}
            }
            var x : active task $D{} @{}->()->()->()
            set x = spawn f $D{} @{} ()
            --awake x _1:_int
            emit @GLOBAL Event $D{} @{} <.3 _1:_int>:<(),_uint64_t,_int>
        """.trimIndent())
        assert(out == "10\n") { out }
    }
    @Test
    fun a04_vars () {
        val out = test(false, """
            $Output0
            type Event $D{} @{} = <(),_uint64_t,_int>
            var f: task $D{} @{}->()->()->()
            set f = task $D{} @{}->()->()->() {
                {
                    var x: _int
                    set x = _10:_int
                    await evt~?3
                    ${output0("x","_int")}
                }
                {
                    var y: _int
                    set y = _20:_int
                    await evt~?3
                    ${output0("y","_int")}
                }
            }
            var x : active task $D{} @{}->()->()->()
            set x = spawn f $D{} @{} ()
            --awake x _1:_int
            --awake x _1:_int
            emit @GLOBAL Event $D{} @{} <.3 _1:_int>:<(),_uint64_t,_int>
            emit @GLOBAL Event $D{} @{} <.3 _1:_int>:<(),_uint64_t,_int>
        """.trimIndent())
        assert(out == "10\n20\n") { out }
    }
    @Test
    fun a05_args_err () {
        val out = test(false, """
                var f : task $D{} @{}->()->()->()
                var x : active task $D{} @{}->[()]->()->()
                set x = spawn f $D{} @{} ()
            """.trimIndent()
        )
        assert(out == "(ln 3, col 9): invalid `spawn` : type mismatch :\n    active task $D{} @{} -> [()] -> () -> ()\n    active task $D{} @{} -> () -> () -> ()") { out }
    }
    @Test
    fun a05_args () {
        val out = test(false, """
            $Output0
            type Event $D{} @{} = <(),_uint64_t,_int>
            var f : task $D{} @{}->_(char*)->()->()
            set f = task $D{} @{}->_(char*)->()->() {
                ${output0("arg","_(char*)")}
                await evt~?3
                ${output0("evt~!3","_int")}
                await evt~?3
                ${output0("evt~!3","_int")}
            }
            var x : active task $D{} @{}->_(char*)->()->()
            set x = spawn f $D{} @{} _("hello"):_(char*)
            --awake x _10:_int
            --awake x _20:_int
            emit @GLOBAL Event $D{} @{} <.3 _10:_int>:<(),_uint64_t,_int>
            emit @GLOBAL Event $D{} @{} <.3 _20:_int>:<(),_uint64_t,_int>
        """.trimIndent())
        assert(out == "\"hello\"\n10\n20\n") { out }
    }
    @Test
    fun a05_args2 () {
        val out = test(false, """
            $Output0
            type Event $D{} @{} = <(),_uint64_t,_int>
            var f : task $D{} @{}->_int->()->_int
            set f = task $D{} @{}->_int->()->_int {
                await evt~?3
                ${output0("arg","_int")}
            }
            var x : active task $D{} @{}->_int->()->_int
            set x = spawn f $D{} @{} _10:_int
            emit @GLOBAL Event $D{} @{} <.3 _20:_int>:<(),_uint64_t,_int>
        """.trimIndent())
        assert(out == "10\n") { out }
    }
    @Test
    fun a06_par_err () {
        val out = test(false, """
            var build : func $D{} @{} -> () -> task $D{} @{}->()->()->()
            set build = func $D{} @{} -> () -> task $D{} @{}->()->()->() {
                set ret = task $D{} @{}->()->()->() {    -- ERR: not the same @LOCAL
                    --${output0("_1:_int","_int")}
                    await _(${D}evt != 0):_int
                    --${output0("_2:_int","_int")}
                }
            }
        """.trimIndent())
        assert(out.startsWith("(ln 3, col 13): invalid return : type mismatch")) { out }
    }
    @Test
    fun a06_par1 () {
        val out = test(false, """
            $Output0
            type Event $D{} @{} = <(),_uint64_t,_int>
            var build : task $D{} @{}->()->()->()
            set build = task $D{} @{}->()->()->() {
                ${output0("_1:_int","_int")}
                await evt~?3
                ${output0("_2:_int","_int")}
            }
            ${output0("_10:_int","_int")}
            var f : active task $D{} @{}->()->()->()
            set f = spawn build $D{} @{} ()
            ${output0("_11:_int","_int")}
            var g : active task $D{} @{}->()->()->()
            set g = spawn build $D{} @{} ()
            ${output0("_12:_int","_int")}
            --awake f _1:_int
            --awake g _1:_int
            emit @GLOBAL Event $D{} @{} <.3 _1:_int>:<(),_uint64_t,_int>
            ${output0("_13:_int","_int")}
        """.trimIndent())
        assert(out == "10\n1\n11\n1\n12\n2\n2\n13\n") { out }
    }
    @Test
    fun a07_bcast () {
        val out = test(false, """
            $Output0
            type Event $D{} @{} = <(),_uint64_t,_int>
            var f : task $D{} @{}->()->()->()
            set f = task $D{} @{}->()->()->() {
                await evt~?3
                ${output0("evt~!3","_int")}
            }
            var x : active task $D{} @{}->()->()->()
            set x = spawn f $D{} @{} ()
            
            var g : task $D{} @{}->()->()->()
            set g = task $D{} @{}->()->()->() {
                await evt~?3
                var e: _int
                set e = evt~!3
                ${output0("_(\$e+10):_int","_int")}
                await evt~?3
                set e = evt~!3
                ${output0("_(\$e+10):_int","_int")}
            }
            var y : active task $D{} @{}->()->()->()
            set y = spawn g $D{} @{} ()
            
            emit @GLOBAL Event $D{} @{} <.3 _1:_int>:<(),_uint64_t,_int>
            emit @GLOBAL Event $D{} @{} <.3 _2:_int>:<(),_uint64_t,_int>
        """.trimIndent())
        assert(out == "1\n11\n12\n") { out }
    }
    @Test
    fun a08_bcast_block () {
        val out = test(false, """
            $Output0
            type Event $D{} @{} = <(),_uint64_t,_int>
            var f : task $D{} @{}->()->()->()
            set f = task $D{} @{}->()->()->() {
                await _1:_int
                var iskill: _int
                var istask: _int
                set iskill = evt~?1
                set istask = evt~?2
                native _(assert(${D}iskill || ${D}istask);)
                ${output0("_0:_int","_int")}
            }
            var x : active task $D{} @{}->()->()->()
            set x = spawn f $D{} @{} ()
            
            {
                var g : task $D{} @{}->()->()->()
                set g = task $D{} @{}->()->()->() {
                    await evt~?3
                    var e: _int
                    set e = evt~!3
                    ${output0("_(\$e+10):_int","_int")}
                    await evt~?3
                    set e = evt~!3
                    ${output0("_(\$e+10):_int","_int")}
                }
                var y : active task $D{} @{}->()->()->()
                set y = spawn g $D{} @{} ()
                emit @LOCAL Event $D{} @{} <.3 _1:_int>:<(),_uint64_t,_int>
                emit @LOCAL Event $D{} @{} <.3 _2:_int>:<(),_uint64_t,_int>
            }            
        """.trimIndent())
        assert(out == "11\n12\n0\n") { out }
    }
    @Test
    fun a08_bcast_block2 () {
        val out = test(false, """
            $Output0
            type Event $D{} @{} = <(),_uint64_t,_int>
            {
                var f : task $D{} @{}->()->()->()
                set f = task $D{} @{}->()->()->() {
                    await _1:_int
                    var iskill: _int
                    set iskill = evt~?1
                    native _(assert(${D}iskill);)
                    --${output0("_0:_int","_int")}    -- only on kill
                    ${output0("_0:_int","_int")}
                }
                var x : active task $D{} @{}->()->()->()
                set x = spawn f $D{} @{} ()
                
                {
                    var g : task $D{} @{}->()->()->()
                    set g = task $D{} @{}->()->()->() {
                        var e: _int
                        await evt~?3
                        set e = evt~!3
                        ${output0("_(\$e+10):_int","_int")}
                        await evt~?3
                        set e = evt~!3
                        ${output0("_(\$e+10):_int","_int")}
                    }
                    var y : active task $D{} @{}->()->()->()
                    set y = spawn g $D{} @{} ()
                    emit @LOCAL Event $D{} @{} <.3 _1:_int>:<(),_uint64_t,_int>
                    emit @LOCAL Event $D{} @{} <.3 _2:_int>:<(),_uint64_t,_int>
                }
            }
        """.trimIndent())
        assert(out == "11\n12\n0\n") { out }
    }
    @Test
    fun a09_nest () {
        val out = test(false, """
            $Output0
            type Event $D{} @{} = <(),_uint64_t,_int>
            var f : task $D{} @{}->()->()->()
            set f = task $D{} @{}->()->()->() {
                ${output0("_1:_int","_int")}
                await evt~?3
                var g : task $D{} @{}->()->()->()
                set g = task $D{} @{}->()->()->() {
                    ${output0("_2:_int","_int")}
                    await evt~?3
                    ${output0("_3:_int","_int")}
                }
                var xg : active task $D{} @{}->()->()->()
                set xg = spawn g $D{} @{} ()
                await evt~?3
                ${output0("_4:_int","_int")}
            }
            var x : active task $D{} @{}->()->()->()
            set x = spawn f $D{} @{} ()
            ${output0("_10:_int","_int")}
            emit @GLOBAL Event $D{} @{} <.3 _1:_int>:<(),_uint64_t,_int>
            ${output0("_11:_int","_int")}
            emit @GLOBAL Event $D{} @{} <.3 _1:_int>:<(),_uint64_t,_int>
            ${output0("_12:_int","_int")}
        """.trimIndent())
        assert(out == "1\n10\n2\n11\n3\n4\n12\n") { out }
    }
    @Test
    fun a10_block_out () {
        val out = test(false, """
            $Output0
            type Event $D{} @{} = <(),_uint64_t,_int>
            var f : task $D{} @{}->()->()->()
            set f = task $D{} @{}->()->()->() {
                ${output0("_10:_int","_int")}
                {
                    var g : task $D{} @{}->()->()->()
                    set g = task $D{} @{}->()->()->() {
                        ${output0("_20:_int","_int")}
                        await _1:_int
                        ${output0("_21:_int","_int")}
                        await _1:_int
                        if evt~?1 {
                            ${output0("_0:_int","_int")}
                            --${output0("_0:_int","_int")}      -- only on kill
                        } else {
                            ${output0("_22:_int","_int")}
                            --output std _22:_int     -- can't execute this one
                        }
                    }
                    var y : active task $D{} @{}->()->()->()
                    set y = spawn g $D{} @{} ()
                    await evt~?3
                }
                ${output0("_11:_int","_int")}
                var h : task $D{} @{}->()->()->()
                set h = task $D{} @{}->()->()->() {
                    ${output0("_30:_int","_int")}
                    await evt~?3
                    ${output0("_31:_int","_int")}
                }
                var z : active task $D{} @{}->()->()->()
                set z = spawn h $D{} @{} ()
                await evt~?3
                ${output0("_12:_int","_int")}
            }
            var x : active task $D{} @{}->()->()->()
            set x = spawn f $D{} @{} ()
            emit @GLOBAL Event $D{} @{} <.3 _1:_int>:<(),_uint64_t,_int>
            emit @GLOBAL Event $D{} @{} <.3 _1:_int>:<(),_uint64_t,_int>
        """.trimIndent())
        assert(out == "10\n20\n21\n0\n11\n30\n31\n12\n") { out }
    }
    @Test
    fun a11_self_kill () {
        val out = test(false, """
            $Output0
            type Event $D{} @{} = <(),_uint64_t,_int>
            var g : task $D{} @{}->()->()->()
            set g = task $D{} @{}->()->()->() {
                var f : task $D{} @{}->()->()->()
                set f = task $D{} @{}->()->()->() {
                    ${output0("_1:_int","_int")}
                    await evt~?3
                    ${output0("_4:_int","_int")}
                    emit @GLOBAL Event $D{} @{} <.3 _1:_int>:<(),_uint64_t,_int>
                    ${output0("_999:_int","_int")}
                }
                var x : active task $D{} @{}->()->()->()
                set x = spawn f $D{} @{} ()
                ${output0("_2:_int","_int")}
                await evt~?3
                ${output0("_5:_int","_int")}
            }
            ${output0("_0:_int","_int")}
            var y : active task $D{} @{}->()->()->()
            set y = spawn g $D{} @{} ()
            ${output0("_3:_int","_int")}
            emit @GLOBAL Event $D{} @{} <.3 _1:_int>:<(),_uint64_t,_int>
            ${output0("_6:_int","_int")}
       """.trimIndent())
        assert(out == "0\n1\n2\n3\n4\n5\n6\n") { out }
    }
    @Test
    fun a12_self_kill () {
        val out = test(false, """
            $Output0
            type Event $D{} @{} = <(),_uint64_t,_int>
            var g : task $D{} @{}->()->()->()
            set g = task $D{} @{}->()->()->() {
                var f : task $D{} @{}->()->()->()
                set f = task $D{} @{}->()->()->() {
                    ${output0("_1:_int","_int")}
                    await evt~?3
                    ${output0("_4:_int","_int")}
                    var kkk : func $D{} @{}->()->()
                    set kkk = func $D{} @{}->()->() {
                        emit @GLOBAL Event $D{} @{} <.3 _1:_int>:<(),_uint64_t,_int>
                    }
                    call kkk $D{} @{} ()
                    ${output0("_999:_int","_int")}
                }
                var x : active task $D{} @{}->()->()->()
                set x = spawn f $D{} @{} ()
                ${output0("_2:_int","_int")}
                await evt~?3
                ${output0("_5:_int","_int")}
            }
            ${output0("_0:_int","_int")}
            var y : active task $D{} @{}->()->()->()
            set y = spawn g $D{} @{} ()
            ${output0("_3:_int","_int")}
            emit @GLOBAL Event $D{} @{} <.3 _1:_int>:<(),_uint64_t,_int>
            ${output0("_6:_int","_int")}
       """.trimIndent())
        assert(out == "0\n1\n2\n3\n4\n5\n6\n") { out }
    }

    // DEFER

    @Test
    fun b01_defer () {
        val out = test(false, """
            $Output0
            type Event $D{} @{} = <(),_uint64_t,_int>
            var f : task $D{} @{}->()->()->()
            set f = task $D{} @{}->()->()->() {
                var defer_ : task $D{} @{}->()->()->()
                set defer_ = task $D{} @{}->()->()->() {
                    await evt~?1
                    ${output0("_2:_int","_int")}
                }
                var xdefer : active task $D{} @{}->()->()->()
                set xdefer = spawn defer_ $D{} @{} ()
                ${output0("_0:_int","_int")}
                await evt~?3
                ${output0("_1:_int","_int")}
            }
            var x : active task $D{} @{}->()->()->()
            set x = spawn f $D{} @{} ()
            --awake x _1:_int
            emit @GLOBAL Event $D{} @{} <.3 _1:_int>:<(),_uint64_t,_int>
        """.trimIndent())
        assert(out == "0\n1\n2\n") { out }
    }
    @Test
    fun b02_defer_block () {
        val out = test(false, """
            $Output0
            type Event $D{} @{} = <(),_uint64_t,_int>
            var f : task $D{} @{}->()->()->()
            set f = task $D{} @{}->()->()->() {
                {
                    var defer_ : task $D{} @{}->()->()->()
                    set defer_ = task $D{} @{}->()->()->() {
                        await evt~?1
                        ${output0("_2:_int","_int")}
                    }
                    var xdefer : active task $D{} @{}->()->()->()
                    set xdefer = spawn defer_ $D{} @{} ()
                    ${output0("_0:_int","_int")}
                    await evt~?3
                }
                ${output0("_1:_int","_int")}
            }
            var x : active task $D{} @{}->()->()->()
            set x = spawn f $D{} @{} ()
            --awake x _1:_int
            emit @GLOBAL Event $D{} @{} <.3 _1:_int>:<(),_uint64_t,_int>
        """.trimIndent())
        assert(out == "0\n2\n1\n") { out }
    }

    // THROW / CATCH

    @Test
    fun c01_catch_err () {
        val out = test(
            false, """
               catch {
               }
            """.trimIndent()
        )
        assert(out == "(ln 1, col 7): expected expression : have \"{\"") { out }
    }
    @Test
    fun c02_catch_err () {
        val out = test(
            false, """
               catch _0 {
               }
            """.trimIndent()
        )
        assert(out == "(ln 1, col 10): invalid `catch` : requires enclosing task") { out }
    }
    @Test
    fun c03_catch_err () {
        val out = test(
            false, """
                var f : task $D{} @{}->()->()->()
                set f = task $D{} @{}->()->()->() {
                    catch () {
                    }
                }
            """.trimIndent()
        )
        assert(out == "(ln 3, col 11): invalid `catch` : type mismatch : expected _int : have ()") { out }
    }
    @Test
    fun c04_err () {
        val out = test(
            false, """
                var f : task $D{} @{}->()->()->()
                var x : task $D{} @{}->()->()->()
                set x = spawn f $D{} @{} ()
            """.trimIndent()
        )
        assert(out.startsWith("(ln 3, col 9): invalid `spawn` : type mismatch : expected active task")) { out }
    }
    @Test
    fun c05_throw_err () {
        val out = test(
            false, """
                throw
            """.trimIndent()
        )
        assert(out == "(ln 1, col 6): expected expression : have end of file") { out }
    }
    @Test
    fun c06_throw_err () {
        val out = test(
            false, """
                throw _1
            """.trimIndent()
        )
        assert(out == "(ln 1, col 1): invalid `throw` : undeclared type \"Error\"") { out }
    }
    @Test
    fun c07_throw_err () {
        val out = test(
            false, """
                type Error $D{} @{} = ()
                throw ()
            """.trimIndent()
        )
        assert(out == "(ln 2, col 7): invalid `throw` : type mismatch : expected Error : have ()") { out }
    }
    @Test
    fun c08_catch () {
        val out = test(false, """
            $Output0
            type Error $D{} @{} = <()>
            var h : task $D{} @{}->()->()->()
            set h = task $D{} @{}->()->()->() {
                ${output0("_2:_int","_int")}
                catch err~?1 {
                    ${output0("_3:_int","_int")}
                    { throw Error.1  $D{} @{} ()}
                    ${output0("_999:_int","_int")}
                }
                ${output0("_4:_int","_int")}
           }
           var z : active task $D{} @{}->()->()->()
           ${output0("_1:_int","_int")}
           set z = spawn h $D{} @{} ()
            ${output0("_5:_int","_int")}
        """.trimIndent())
        assert(out == "1\n2\n3\n4\n5\n") { out }
    }
    @Test
    fun c09_no_catch () {
        val out = test(false, """
            $Output0
            type Error $D{} @{} = <(),()>
            var h : task $D{} @{}->()->()->()
            set h = task $D{} @{}->()->()->() {
                ${output0("_2:_int","_int")}
                catch err~?2 {
                    ${output0("_3:_int","_int")}
                    throw Error.1 $D{} @{} ()
                    ${output0("_999:_int","_int")}
                }
                ${output0("_4:_int","_int")}
           }
           var z : active task $D{} @{}->()->()->()
            ${output0("_1:_int","_int")}
           set z = spawn h $D{} @{} ()
            ${output0("_5:_int","_int")}
        """.trimIndent())
        assert(out.contains("block_throw: Assertion `0 && \"throw without catch\"' failed.")) { out }
    }
    @Test
    fun c10_up_catch () {
        val out = test(false, """
            $Output0
            type Error $D{} @{} = <(),()>
            var h : task $D{} @{}->()->()->()
            set h = task $D{} @{}->()->()->() {
                ${output0("_2:_int","_int")}
                catch err~?1 {
                    catch err~?2 {
                        ${output0("_3:_int","_int")}
                        throw Error.1 $D{} @{} ()
                        ${output0("_999:_int","_int")}
                    }
                    ${output0("_999:_int","_int")}
                }
                ${output0("_4:_int","_int")}
           }
           var z : active task $D{} @{}->()->()->()
            ${output0("_1:_int","_int")}
           set z = spawn h $D{} @{} ()
            ${output0("_5:_int","_int")}
        """.trimIndent())
        assert(out == "1\n2\n3\n4\n5\n") { out }
    }
    @Test
    fun c11_throw () {
        val out = test(false, """
            $Output0
            type Error $D{} @{} = <()>
            type Event $D{} @{} = <(),_uint64_t,_int>
            var h : task $D{} @{}->()->()->()
            set h = task $D{} @{}->()->()->() {
               catch _1 {
                    var f : task $D{} @{}->()->()->()
                    set f = task $D{} @{}->()->()->() {
                        await evt~?1
                        ${output0("_1:_int","_int")}
                    }
                    var x : active task $D{} @{}->()->()->()
                    set x = spawn f $D{} @{} ()
                    throw Error.1 $D{} @{} ()
               }
               ${output0("_2:_int","_int")}
           }
           var z : active task $D{} @{}->()->()->()
           set z = spawn h $D{} @{} ()
           ${output0("_3:_int","_int")}
        """.trimIndent())
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun c12_throw () {
        val out = test(false, """
            $Output0
            type Error $D{} @{} = <()>
            type Event $D{} @{} = <(),_uint64_t,_int>
            var h : task $D{} @{}->()->()->()
            set h = task $D{} @{}->()->()->() {
                catch _1 {
                    var f : task $D{} @{}->()->()->()
                    set f = task $D{} @{}->()->()->() {
                        await evt~?3
                        ${output0("_999:_int","_int")}
                    }
                    var g : task $D{} @{}->()->()->()
                    set g = task $D{} @{}->()->()->() {
                        await evt~?1
                        ${output0("_1:_int","_int")}
                    }
                    var x : active task $D{} @{}->()->()->()
                    set x = spawn f $D{} @{} ()
                    var y : active task $D{} @{}->()->()->()
                    set y = spawn g $D{} @{} ()
                    ${output0("_0:_int","_int")}
                    throw Error.1 $D{} @{} ()
                    ${output0("_999:_int","_int")}
                }
                ${output0("_2:_int","_int")}
           }
           var z : active task $D{} @{}->()->()->()
           set z = spawn h $D{} @{} ()
           ${output0("_3:_int","_int")}
        """.trimIndent())
        assert(out == "0\n1\n2\n3\n") { out }
    }
    @Test
    fun c13_throw_par2 () {
        val out = test(false, """
            $Output0
            type Error $D{} @{} = <()>
            type Event $D{} @{} = <(),_uint64_t,_int>
            var main : task $D{} @{}->()->()->()
            set main = task $D{} @{}->()->()->() {
                var fg : task $D{} @{}->()->()->()
                set fg = task $D{} @{}->()->()->() {
                    var f : task $D{} @{}->()->()->()
                    set f = task $D{} @{}->()->()->() {
                        await evt~?3
                        ${output0("_999:_int","_int")}
                    }
                    var g: task $D{} @{}->()->()->()
                    set g = task $D{} @{}->()->()->() {
                        await evt~?1
                        ${output0("_2:_int","_int")}
                    }
                    await evt~?3
                    var xf : active task $D{} @{}->()->()->()
                    set xf = spawn f $D{} @{} ()
                    var xg : active task $D{} @{}->()->()->()
                    set xg = spawn g $D{} @{} ()
                    throw Error.1 $D{} @{} ()
                }
                var h : task $D{} @{}->()->()->()
                set h = task $D{} @{}->()->()->() {
                    await evt~?1
                    ${output0("_1:_int","_int")}
                }
                var xfg : active task $D{} @{}->()->()->()
                var xh : active task $D{} @{}->()->()->()
                catch _1 {
                    set xfg = spawn fg $D{} @{} ()
                    set xh = spawn h $D{} @{} ()
                    emit @GLOBAL Event $D{} @{} <.3 _1:_int>:<(),_uint64_t,_int>
                    ${output0("_999:_int","_int")}
                }
            }
            var xmain : active task $D{} @{}->()->()->()
            set xmain = spawn main $D{} @{} ()
            ${output0("_3:_int","_int")}
        """.trimIndent())
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun c14_throw_func () {
        val out = test(false, """
            $Output0
            type Error $D{} @{} = <()>
            type Event $D{} @{} = <(),_uint64_t,_int>
            var xxx : func $D{} @{}->()->()
            set xxx = func $D{} @{}->()->() {
                throw Error.1 $D{} @{} ()
            }
            var h : task $D{} @{}->()->()->()
            set h = task $D{} @{}->()->()->() {
               catch _1 {
                    var f: task $D{} @{}->()->()->()
                    set f = task $D{} @{}->()->()->() {
                        await _1:_int
                        ${output0("_1:_int","_int")}
                    }
                    var xf: active task $D{} @{}->()->()->()
                    set xf = spawn f $D{} @{} ()
                    call xxx $D{} @{} ()
                    ${output0("_999:_int","_int")}
               }
               ${output0("_2:_int","_int")}
           }
           var xh : active task $D{} @{}->()->()->()
           set xh = spawn h $D{} @{} ()
           ${output0("_3:_int","_int")}
        """.trimIndent())
        assert(out == "1\n2\n3\n") { out }
    }

    // FIELDS

    @Test
    fun d00_err () {
        val out = test(
            false, """
                var f : task $D{} @{}->()->_int->()
                set f.pub = _4:_int
            """.trimIndent()
        )
        assert(out.startsWith("(ln 2, col 7): invalid \"pub\" : type mismatch : expected active task")) { out }
    }
    @Test
    fun d01_field () {
        val out = test(false, """
            $Output0
            var f : task $D{} @{}->()->_int->()
            set f = task $D{} @{}->()->_int->() {
                set pub = _3:_int
                ${output0("_1:_int","_int")}
            }
            var xf: active task $D{} @{}->()->_int->()
            set xf = spawn f $D{} @{} ()
            ${output0("_2:_int","_int")}
            ${output0("xf.pub","_int")}
            set xf.pub = _4:_int
            ${output0("xf.pub","_int")}
        """.trimIndent())
        assert(out == "1\n2\n3\n4\n") { out }
    }
    @Test
    fun d02_field () {
        val out = test(false, """
            var f : task $D{} @{}->()->_int->()
            var xf: active task $D{} @{}->()->_int->()
            set xf = spawn f $D{} @{} ()
            set _:_ = xf.pub
        """.trimIndent())
        assert(out.contains("error: expected expression before ‘=’ token")) { out }
    }

    // SPAWN / DYNAMIC

    @Test
    fun e01_spawn () {
        val out = test(false, """
            $Output0
            spawn task $D{} @{}->()->()->() {
                ${output0("()","()")}
            } $D{} @{} ()
        """.trimIndent())
        //assert(out == "(ln 2, col 5): expected `in` : have end of file") { out }
        assert(out == "()\n") { out }
    }
    @Test
    fun e01_spawn_err2 () {
        val out = test(
            false, """
                var f : func $D{} @{}->()->()
                var fs : active {} task $D{} @{}->()->()->()
                spawn f $D{} @{} () in fs
            """.trimIndent()
        )
        //assert(out.startsWith("(ln 3, col 7): invalid `spawn` : type mismatch : expected task")) { out }
        assert(out.startsWith("(ln 3, col 7): invalid spawn : expected task")) { out }
    }
    @Test
    fun e01_spawn_err3 () {
        val out = test(
            false, """
                var f : task $D{} @{}->()->()->()
                spawn f $D{} @{} () in ()
            """.trimIndent()
        )
        assert(out.startsWith("(ln 2, col 23): invalid `spawn` : type mismatch : expected active tasks")) { out }
    }
    @Test
    fun e01_spawn_err4 () {
        val out = test(
            false, """
                var f : task $D{} @{}->()->()->()
                var fs : active {} task $D{} @{}->[()]->()->()
                spawn f $D{} @{} () in fs
            """.trimIndent()
        )
        assert(out == "(ln 3, col 1): invalid `spawn` : type mismatch :\n    task $D{} @{} -> [()] -> () -> ()\n    task $D{} @{} -> () -> () -> ()") { out }
    }
    @Test
    fun e02_spawn_free () {
        val out = test(false, """
            $Output0
            type Event $D{} @{} = <(),_uint64_t,_int>
            var f : task $D{} @{}->()->()->()
            set f = task $D{} @{}->()->()->() {
                ${output0("_1:_int","_int")}
                await evt~?3
                ${output0("_3:_int","_int")}
            }
            var fs : active {} task $D{} @{}->()->()->()
            spawn f $D{} @{} () in fs
            ${output0("_2:_int","_int")}
            emit @GLOBAL Event $D{} @{} <.3 _1:_int>:<(),_uint64_t,_int>
            ${output0("_4:_int","_int")}
        """.trimIndent())
        assert(out == "1\n2\n3\n4\n") { out }
    }
    @Test
    fun e03_spawn_anon () {
        val out = test(false, """
            $Output0
            var t: task $D{} @{} -> () -> [_int] -> ()
            set t = task $D{} @{} -> () -> [_int] -> () {
                var xxx: _int
                spawn (task $D{} @{}->_ -> _ -> _ {
                    set pub = [_10:_int]
                    set xxx = _10:_int
                } $D{} @{} ())
            }
            var xt: active task $D{} @{} -> () -> [_int] -> ()
            set xt = spawn (t $D{} @{} ())
            ${output0("xt.pub.1","_int")}
        """.trimIndent())
        assert(out == "10\n") { out }
    }

    // POOL / TASKS / LOOPT

    @Test
    fun f01_err () {
        val out = test(
            false, """
                var xs: active {} task $D{} @{}->()->_int->()
                var x:  task $D{} @{}->()->_int->()
                loop x in xs {
                }
            """.trimIndent()
        )
        assert(out.startsWith("(ln 3, col 6): invalid `loop` : type mismatch : expected task type")) { out }

    }
    @Test
    fun f02_err () {
        val out = test(
            false, """
                var xs: active {} task $D{} @{}->[()]->_int->()
                var x:  active task  $D{} @{}->()->_int->()
                loop x in xs {
                }
            """.trimIndent()
        )
        assert(out == "(ln 3, col 1): invalid `loop` : type mismatch :\n    active task $D{} @{} -> () -> _int -> ()\n    active {} task $D{} @{} -> [()] -> _int -> ()") { out }

    }
    @Test
    fun f03_err () {
        val out = test(
            false, """
                var x: ()
                loop x in () {
                }
            """.trimIndent()
        )
        assert(out.startsWith("(ln 2, col 6): invalid `loop` : type mismatch : expected task type")) { out }
    }
    @Test
    fun f04_err () {
        val out = test(
            false, """
                var x: active task $D{} @{}->()->_int->()
                loop x in () {
                }
            """.trimIndent()
        )
        assert(out.startsWith("(ln 2, col 11): invalid `loop` : type mismatch : expected tasks type")) { out }
    }
    @Test
    fun f05_loop () {
        val out = test(false, """
            $Output0
            var fs: active {} task $D{} @{}->()->_int->()
            var f: active task $D{} @{}->()->_int->()
            loop f in fs {
            }
            ${output0("()","()")}
        """.trimIndent())
        assert(out == "()\n") { out }
    }
    @Test
    fun f06_pub () {
        val out = test(false, """
            $Output0
            type Event $D{} @{} = <(),_uint64_t,_int>
            var f : task $D{} @{}->()->_int->()
            set f = task $D{} @{}->()->_int->() {
                set pub = _3:_int
                ${output0("_1:_int","_int")}
                await evt~?3
            }
            var fs: active {} task $D{} @{}->()->_int->()
            spawn f $D{} @{} () in fs
            var x: active task $D{} @{}->()->_int->()
            loop x in fs {
                ${output0("x.pub","_int")}
            }
        """.trimIndent())
        assert(out == "1\n3\n") { out }
    }
    @Test
    fun f07_kill () {
        val out = test(false, """
            $Output0
            var f : task $D{} @{}->()->_int->()
            set f = task $D{} @{}->()->_int->() {
                set pub = _3:_int
                ${output0("_1:_int","_int")}
            }
            var fs: active {} task $D{} @{}->()->_int->()
            spawn f $D{} @{} () in fs
            var x: active task $D{} @{}->()->_int->()
            loop x in fs {
                ${output0("x.pub","_int")}
            }
        """.trimIndent())
        assert(out == "1\n") { out }
    }
    @Test
    fun f07_valgrind () {
        val out = test(false, """
            $Output0
            type Event $D{} @{} = <(),_uint64_t,_int>
            var f : task $D{} @{}->()->()->()
            set f = task $D{} @{}->()->()->() {
                await evt~?3
            }
            var xs: active {} task $D{} @{}->_int->_int->()
            spawn f $D{} @{} () in xs
            emit @GLOBAL Event $D{} @{} <.3 _10:_int>:<(),_uint64_t,_int>
            ${output0("()","()")}
        """.trimIndent())
        assert(out == "()\n") { out }
    }
    @Test
    fun f08_natural () {
        val out = test(false, """
            $Output0
            type Event $D{} @{} = <(),_uint64_t,_int>
            var f : task $D{} @{}->_int->_int->()
            set f = task $D{} @{}->_int->_int->() {
                set pub = arg
                ${output0("pub","_int")}
                await evt~?3
            }
            var g : task $D{} @{}->_int->_int->()
            set g = task $D{} @{}->_int->_int->() {
                set pub = arg
                ${output0("pub","_int")}
                await evt~?3
                await evt~?3
            }

            var xs: active {} task $D{} @{}->_int->_int->()
            spawn f $D{} @{} _1:_int in xs
            spawn g $D{} @{} _2:_int in xs

            var x: active task $D{} @{}->_int->_int->()
            loop x in xs {
                ${output0("x.pub","_int")}
            }
            
            emit @GLOBAL Event $D{} @{} <.3 _10:_int>:<(),_uint64_t,_int>
            
            loop x in xs {
                ${output0("x.pub","_int")}
            }
            
            ${output0("()","()")}
        """.trimIndent())
        assert(out == "1\n2\n1\n2\n2\n()\n") { out }
    }
    @Test
    fun f09_dloop_kill () {
        val out = test(false, """
            $Output0
            type Event $D{} @{} = <(),_uint64_t,_int>
            var f : task $D{} @{}->()->_int->()
            set f = task $D{} @{}->()->_int->() {
                set pub = _10:_int
                ${output0("_1:_int","_int")}
                await evt~?3
            }
            var fs: active {} task $D{} @{}->()->_int->()
            spawn f $D{} @{} () in fs
            var x: active task $D{} @{}->()->_int->()
            loop x in fs {
                emit @GLOBAL Event $D{} @{} <.3 _10:_int>:<(),_uint64_t,_int>
                ${output0("x.pub","_int")}
            }
        """.trimIndent())
        assert(out == "1\n10\n") { out }
    }
    @Test
    fun f10_track () {
        val out = test(false, """
            $Output0
            type Event $D{} @{} = <(),_uint64_t,_int>
            var f : task $D{} @{}->()->_int->()
            set f = task $D{} @{}->()->_int->() {
                set pub = _3:_int
                ${output0("_1:_int","_int")}
                await evt~?3
            }
            var fs: active {} task $D{} @{}->()->_int->()
            spawn f $D{} @{} () in fs
            var y: active task $D{} @{}->()->_int->()
            var x: active task $D{} @{}->()->_int->()
            loop x in fs {
                set y = x
            }
            ${output0("y.pub","_int")}
        """.trimIndent())
        assert(out == "1\n3\n") { out }
    }

    // AWAIT TASK

    @Test
    fun g01_state () {
        val out = test(false, """
            $Output0
            type Event $D{} @{} = <(),_uint64_t,_int>
            type Error $D{} @{} = <_int>
            var f: task $D{} @{}->()->()->()
            set f = task $D{} @{}->()->()->() {
                await _1:_int
            }
            var x : active task $D{} @{}->()->()->()
            set x = spawn f $D{} @{} ()
            spawn (task $D{} @{}->()->()->() {
                ${catch0(1)} {
                    loop {
                        await evt~?2
                        var t: _uint64_t
                        set t = evt~!2
                        if _(${D}t == ((uint64_t)${D}x)):_int {
                            ${throw0(1)}
                        } else {}
                    }
                }
                ${output0("_2:_int","_int")}
            }) $D{} @{} ()
            ${output0("_1:_int","_int")}
            emit @GLOBAL Event $D{} @{} <.3 _1:_int>:<(),_uint64_t,_int>
            ${output0("_3:_int","_int")}
       """.trimIndent())
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun g02_kill () {
        val out = test(false, """
            $Output0
            type Event $D{} @{} = <(),_uint64_t,_int>
            type Error $D{} @{} = <_int>
            spawn (task $D{} @{}->()->()->() {
                loop {
                    var f: task $D{} @{}->()->()->()
                    set f = task $D{} @{}->()->()->() {
                        await evt~?3
                        ${output0("_2:_int","_int")}
                    }
                    var x : active task $D{} @{}->()->()->()
                    set x = spawn f $D{} @{} ()
                    ${catch0(1)} {
                        loop {
                            await evt~?2
                            var t: _uint64_t
                            set t = evt~!2
                            if _(${D}t == ((uint64_t)${D}x)):_int {
                                ${throw0(1)}
                            } else {}
                        }
                    }
                    ${output0("_3:_int","_int")}
                }
            }) $D{} @{} ()
            ${output0("_1:_int","_int")}
            emit @GLOBAL Event $D{} @{} <.3 _1:_int>:<(),_uint64_t,_int>
            ${output0("_4:_int","_int")}
       """.trimIndent())
        assert(out == "1\n2\n3\n4\n") { out }
    }
    @Test
    fun g03_kill () {
        val out = test(false, """
            $Output0
            type Event $D{} @{} = <(),_uint64_t,_int>
            spawn (task $D{} @{}->()->()->() {
                loop {
                    var f: task $D{} @{}->()->()->()
                    set f = task $D{} @{}->()->()->() {
                        await evt~?3
                        ${output0("_2:_int","_int")}
                    }
                    var x : active task $D{} @{}->()->()->()
                    set x = spawn f $D{} @{} ()
                    await x
                    ${output0("_3:_int","_int")}
                }
            }) $D{} @{} ()
            ${output0("_1:_int","_int")}
            emit @GLOBAL Event $D{} @{} <.3 _1:_int>:<(),_uint64_t,_int>
            ${output0("_4:_int","_int")}
       """.trimIndent())
        assert(out == "1\n2\n3\n4\n") { out }
    }
    @Test
    fun g03_kill_return () {
        val out = test(false, """
            $Output0
            type Event $D{} @{} = <(),_uint64_t,_int>
            spawn (task $D{} @{}->()->()->() {
                loop {
                    var f: task $D{} @{}->()->()->_int
                    set f = task $D{} @{}->()->()->_int {
                        await evt~?3
                        set ret = _10:_int
                    }
                    var x : active task $D{} @{}->()->()->_int
                    set x = spawn f $D{} @{} ()
                    await x
                    ${output0("x.ret","_int")}
                }
            }) $D{} @{} ()
            emit @GLOBAL Event $D{} @{} <.3 _1:_int>:<(),_uint64_t,_int>
       """.trimIndent())
        assert(out == "10\n") { out }
    }
    @Test
    fun g03_f_kill_err () {
        val out = test(
            false, """
                var fff: func $D{} @{}  -> () -> () {}  -- leading block has no effect 
           """.trimIndent()
        )
        assert(out == "(ln 1, col 36): expected \";\"") { out }
    }
    @Test
    fun g03_f_kill () {
        val out = test(false, """
            $Output0
            var fff: func $D{} @{} -> () -> ()
            set fff = func $D{} @{} -> () -> () {}
            spawn (task $D{} @{}->()->()->() {
                ${output0("_111:_int","_int")}
                {
                    call fff $D{} @{} ()
                }
                ${output0("_222:_int","_int")}
            }) $D{} @{} ()
       """.trimIndent())
        assert(out == "111\n222\n") { out }
    }
    @Test
    fun g04_err () {
        val out = test(false, """
            $Output0
            type Event $D{} @{} = <(),_uint64_t,()>
            
            spawn (task  $D{} @{} -> () -> () -> () {
                ${output0("(_1: _int)","_int")}
                var t1: active task $D{} @{} -> () -> () -> ()             
                set t1 = spawn (task $D{} @{} -> () -> () -> () {              
            
                    var t2: active task $D{} @{} -> () -> () -> ()        
                    set t2 = spawn (task $D{} @{} -> () -> () -> () {
                        ${output0("_2:_int","_int")}
                        await (_1: _int)                               
                        ${output0("_4:_int","_int")}               
                    } $D{} @{} ())                          
            
                    await ((evt~)?2)     
                    ${output0("_5:_int","_int")}    
                } $D{} @{} ())                   
            
                await (evt~?2)            
                ${output0("_6:_int","_int")}                    
            } $D{} @{} ())
            
            ${output0("_3:_int","_int")}
            emit @GLOBAL Event $D{} @{} <.3 ()>: <(),_uint64_t,()>
            ${output0("_7:_int","_int")}
       """.trimIndent())
        assert(out == "1\n2\n3\n4\n5\n6\n7\n") { out }
    }
    @Test
    fun g05_spawn_abort () {
        val out = test(false, """
            $Output0
            type Event $D{} @{} = <(),_uint64_t,(),()>
            var t: task $D{} @{} -> () -> () -> ()
            set t = task $D{} @{} -> () -> () -> () {
                var v: _int
                set v = _1:_int
                loop {
                    ${output0("v","_int")}
                    await evt~?3
                    set v = _(${D}v+1):_int
                }
            }
            
            var l: active task  $D{} @{} -> () -> () -> ()
            set l = spawn (task  $D{} @{} -> () -> () -> () {
                loop {
                    var x: active task  $D{} @{} -> () -> () -> ()
                    set x = spawn t $D{} @{} _1:_int
                    await evt~?4
                }
            }) $D{} @{} ()
            
            emit @GLOBAL Event $D{} @{} <.3 ()>: <(),_uint64_t,(),()>
            emit @GLOBAL Event $D{} @{} <.4 ()>: <(),_uint64_t,(),()>
            emit @GLOBAL Event $D{} @{} <.3 ()>: <(),_uint64_t,(),()>
            
       """.trimIndent())
        assert(out == "1\n2\n1\n2\n") { out }
    }

    // TYPE TASK

    @Test
    fun h01_type_task () {
        val out = test(false, """
            $Output0
            type Event $D{} @{} = <(),_uint64_t,()>
            type Bird $D{} @{} = task  $D{} @{} -> () -> () -> ()
            
            var t1 : Bird $D{} @{}
            set t1 = Bird $D{} @{} task  $D{} @{} -> () -> () -> () {
                 ${output0("_2:_int","_int")}
            }
            
            var x1: active Bird $D{} @{}
            ${output0("_1:_int","_int")}
            set x1 = spawn active Bird $D{} @{} (t1~ $D{} @{} ())
            ${output0("_3:_int","_int")}
       """.trimIndent())
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun h02_task_type () {
        val out = test(false, """
            $Output0
            type Xask  $D{} @{} = task $D{} @{}->()->_int->()
            var t : Xask  $D{} @{}
            set t = Xask $D{} @{} task $D{} @{}->()->_int->() {
                set pub = _10:_int
                ${output0("_2:_int","_int")}
            }
            var x : active Xask $D{} @{}
            ${output0("_1:_int","_int")}
            set x = spawn active Xask $D{} @{} (t~ $D{} @{} ())
            ${output0("_3:_int","_int")}
            ${output0("(x~).pub","_int")}
        """.trimIndent())
        assert(out == "1\n2\n3\n10\n") { out }
    }
    @Test
    fun h03_task_type () {
        val out = test(false, """
            $Output0
            type Event $D{} @{} = <()>
            type Xask $D{} @{} = task $D{} @{} -> () -> _int -> ()
            var t: Xask $D{} @{}
            set t = Xask $D{} @{} task$D{} @{}-> ()->_int->() {
                set pub = _10:_int
                await _0:_int
            }
            var xs: active {} Xask $D{} @{}
            spawn (t~ $D{} @{} ()) in xs
            var i: active Xask $D{} @{}
            loop i in xs {
                ${output0("(i~).pub","_int")}
            }
        """.trimIndent())
        assert(out == "10\n") { out }
    }
    @Test
    fun h04_task_type () {
        val out = test(false, """
            $Output0
            type Event $D{} @{} = <()>

            var n: _int
            set n = _0:_int

            type Xask $D{} @{} = task $D{} @{} -> () -> () -> ()
            var t: Xask $D{} @{}
            set t = Xask $D{} @{} task $D{} @{}->()->()->() {
                set n = _(${D}n+1):_int
                await _0:_int
            }
            
            var xs: active {2} Xask $D{} @{}
            spawn (t~ $D{} @{} ()) in xs
            spawn (t~ $D{} @{} ()) in xs
            spawn (t~ $D{} @{} ()) in xs
            spawn (t~ $D{} @{} ()) in xs
            ${output0("n","_int")}
        """.trimIndent())
        assert(out == "2\n") { out }
    }
    @Test
    fun h05_task_type () {
        val out = test(false, """
            $Output0
            type Event $D{} @{} = <()>

            var n: _int
            set n = _0:_int

            type Xask $D{} @{} = task $D{} @{} -> () -> () -> ()
            var t: Xask $D{} @{}
            set t = Xask $D{} @{} task $D{} @{}->()->()->() {
                set n = _(${D}n+1):_int
                --await _0:_int
            }
            
            var xs: active {2} Xask $D{} @{}
            spawn (t~ $D{} @{} ()) in xs
            spawn (t~ $D{} @{} ()) in xs
            spawn (t~ $D{} @{} ()) in xs
            spawn (t~ $D{} @{} ()) in xs
            ${output0("n","_int")}
        """.trimIndent())
        assert(out == "4\n") { out }
    }

    // EMIT LOCAL

    @Test
    fun i01_local () {
        val out = test(false, """
            $Output0
            type Event $D{} @{} = <(),_uint64_t,_int>
            var f : task $D{} @{}->()->()->()
            set f = task $D{} @{}->()->()->() {
                ${output0("_1:_int","_int")}
                await evt~?3
                ${output0("_2:_int","_int")}
            }
            var x1 : active task $D{} @{}->()->()->()
            set x1 = spawn f $D{} @{} ()
            var x2 : active task $D{} @{}->()->()->()
            set x2 = spawn f $D{} @{} ()
            emit x1 Event $D{} @{}<.3 _1:_int>:<(),_uint64_t,_int>
            ${output0("_3:_int","_int")}
        """.trimIndent())
        assert(out == "1\n1\n2\n3\n") { out }
    }
    @Test
    fun i02_err () {
        val out = test(
            false, """
                type Event $D{} @{} = <(),_uint64_t,_int>
                var x1 : active task $D{} @{}->()->()->()
                emit @GLOBAL x1 $D{} @{}()
            """.trimIndent()
        )
        assert(out == "(ln 3, col 14): invalid call : not a function") { out }
    }
    @Test
    fun i03_err () {
        val out = test(false, """
            type Event $D{} @{} = <(),_uint64_t,_int>
            emit @XXX Event $D{} @{}<.3 _1:_int>:<(),_uint64_t,_int>
        """.trimIndent())
        assert(out == "(ln 2, col 6): undeclared scope \"@XXX\"") { out }
    }
    @Test
    fun i04_err () {
        val out = test(false, """
            type Event $D{} @{} = <(),_uint64_t,_int>
            emit @XXX Event $D{} @{}<.3 _1:_int>:<(),_uint64_t,_int>
        """.trimIndent())
        assert(out == "(ln 2, col 6): undeclared scope \"@XXX\"") { out }
    }

    // PAUSE

    @Test
    fun j01_pause () {
        val out = test(false, """
            $Output0
            type Event $D{} @{} = <(),_uint64_t,_int>
            var f : task $D{} @{}->()->()->()
            set f = task $D{} @{}->()->()->() {
                ${output0("_1:_int","_int")}
                await evt~?3
                ${output0("_5:_int","_int")}
            }
            var x : active task $D{} @{}->()->()->()
            set x = spawn f $D{} @{} ()
            ${output0("_2:_int","_int")}
            pause x
            ${output0("_3:_int","_int")}
            emit @GLOBAL Event $D{} @{}<.3 _1:_int>:<(),_uint64_t,_int>
            resume x
            ${output0("_4:_int","_int")}
            emit @GLOBAL Event $D{} @{}<.3 _1:_int>:<(),_uint64_t,_int>
        """.trimIndent())
        assert(out == "1\n2\n3\n4\n5\n") { out }
    }

    // XXX

    @Test
    fun xxx_01 () {
        val out = test(false, """
            $Output0
            type Error $D{} @{} = <_int>
            spawn ((task $D{} @{} -> _ -> _ -> _ {
                var opt: _int
                var str: ()
                set str = (if (_0: _int) { ()}  else { (if opt { ()}  else { () }) })
                ${output0("()","()")}
            }) $D{} @{} ())
        """.trimIndent())
        assert(out == "()\n") { out }
    }
}
