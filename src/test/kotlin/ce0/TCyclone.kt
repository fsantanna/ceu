package ce0

import D
import VALGRIND
import ce2c
import exec
import org.junit.jupiter.api.MethodOrderer.Alphanumeric
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import java.io.File

@TestMethodOrder(Alphanumeric::class)
class TCyclone {

    fun all(inp: String): String {
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
    fun strcpy_01() {
        val out = all(
            """
            var scpy:     (func @[a1,b1]-> [/_char@a1,/_char@b1] -> /_char@a1)
            set scpy = func @[a1,b1]-> [/_char@a1,/_char@b1] -> /_char@a1 {
                set ret = arg.1
            }
            var s1: /_char@LOCAL
            set s1 = scpy @[LOCAL,LOCAL] [s1,s1]
            {
                var s2: /_char@LOCAL
                set s1 = scpy @[GLOBAL,LOCAL] [s1,s2]: @GLOBAL
                set s2 = scpy @[LOCAL,GLOBAL] [s2,s1]   -- TODO: should be ok @a1/@b1
            }
            output std ()
        """.trimIndent()
        )
        assert(out == "()\n") { out }
    }
    @Test
    fun strdup_02() {
        val out = all(
            """
            var sdup:     (func @[i1]-> /_char@i1 -> /_char@GLOBAL)
            set sdup = func @[i1]-> /_char@i1 -> /_char@GLOBAL {
                var xxx: /_char @GLOBAL -- new ...
                set ret = xxx
            }
            var s1: /_char@LOCAL
            --set s1 = sdup s1 @LOCAL
            {
                var s2: /_char@LOCAL
                --set s1 = sdup @[LOCAL] s2: @GLOBAL
                set s2 = sdup @[GLOBAL] s1: @GLOBAL
            }
            output std ()
        """.trimIndent()
        )
        assert(out == "()\n") { out }
    }
    @Test
    fun fact_03 () {
        val out = all(
            """
            var fact: (func @[i1]->[/_int@i1,_int] -> ())
            set fact = func @[i1] ->[/_int@i1,_int] -> () { @F
                var x: _int
                var n: _int
                set n = arg.2
                if _(${D}n > 1):_int {
                    call fact @[F] [/x,_(${D}n-1):_int]
                } else {
                    set x = _1: _int                
                }
                set arg.1\ = _(${D}x*${D}n):_int
            }
            var x: _int
            call fact @[LOCAL] [/x, _6:_int]
            output std x
        """.trimIndent()
        )
        assert(out == "720\n") { out }
    }
}
