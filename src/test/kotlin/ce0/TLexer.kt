import org.junit.jupiter.api.MethodOrderer.Alphanumeric
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import java.io.PushbackReader
import java.io.StringReader

@TestMethodOrder(Alphanumeric::class)
class TLexer {
    @Test
    fun a00_buffer () {
        val inp = PushbackReader(StringReader("Hello World!"),2)
        assert(inp.read().toChar() == 'H')
        val c = inp.read()
        assert(c.toChar() == 'e')
        inp.unread(c)
        assert(inp.read().toChar() == 'e')
    }
    @Test
    fun a01_buffer () {
        val inp = PushbackReader(StringReader("Hello World!"),2)
        assert(inp.read().toChar() == 'H')
        val c = inp.read()
        assert(c.toChar() == 'e')
        inp.unread('e'.toInt())
        inp.unread('H'.toInt())
        assert(inp.read().toChar() == 'H')
    }
    @Test
    fun a02_buffer () {
        val inp = PushbackReader(StringReader(""),2)
        assert(inp.read().toChar() == (-1).toChar())
    }

    // BLANKS

    @Test
    fun b01_lexer_blanks () {
        val inp = PushbackReader(StringReader("-- foobar"),2)
        alls.stack.addFirst(All(null, inp, false))
        Lexer.blanks()
        assert(inp.read() == 65535)     // for some reason, it returns this value after reading -1
        assert(inp.read() == -1)        // then, it returns -1 correctly
        assert(inp.read() == -1)        // then, it returns -1 correctly
        assert(all().lin == 1)
        assert(all().col == 10)
    }
    @Test
    fun b02_lexer_blanks () {
        val inp = PushbackReader(StringReader("-- c1\n--c2\n\n"), 2)
        alls.stack.addFirst(All(null, inp, false))
        Lexer.blanks()
        assert(all().lin == 4)
        assert(all().col == 1)
    }

    // SYMBOLS

    @Test
    fun b03_lexer_syms () {
        All_restart(null, PushbackReader(StringReader("{ -> , ()"), 2))
        Lexer.lex(); assert(alls.tk1.str=="{")
        Lexer.lex(); assert(alls.tk1.str=="->")
        Lexer.lex(); assert(alls.tk1.str==",")
        Lexer.lex(); assert(alls.tk1.str=="()")
        Lexer.lex(); assert(alls.tk1 is Tk.Eof)
    }
    @Test
    fun b04_lexer_syms () {
        All_restart(null, PushbackReader(StringReader(": }{ :"), 2))
        Lexer.lex(); assert(alls.tk1.str==":")
        Lexer.lex(); assert(alls.tk1.str=="}")
        Lexer.lex(); assert(alls.tk1.str=="{")
        Lexer.lex(); assert(alls.tk1.str==":")
    }

    // KEYWORDS

    @Test
    fun b05_lexer_keys () {
        All_restart(null, PushbackReader(StringReader("xvar var else varx type output //@rec"), 2))
        Lexer.lex(); assert(alls.tk1 is Tk.id && (alls.tk1 as Tk.id).str=="xvar")
        Lexer.lex(); assert(alls.tk1.str=="var")
        Lexer.lex(); assert(alls.tk1.str=="else")
        Lexer.lex(); assert(alls.tk1 is Tk.id && (alls.tk1 as Tk.id).str=="varx")
        Lexer.lex(); assert(alls.tk1.str=="type")
        Lexer.lex(); assert(alls.tk1.str=="output")
        //Lexer.lex() ; assert(alls.tk1.enu==TK.AREC && (alls.tk1 as Tk.Key).key=="@rec")
    }

    // XVAR / XUSER

    @Test
    fun b06_lexer_xs () {
        All_restart(null, PushbackReader(StringReader("c1\nc2 c3  \n    \nc4"), 2))
        Lexer.lex(); assert(alls.tk1.lin==1 && alls.tk1.col==1) ; assert(alls.tk1 is Tk.id && (alls.tk1 as Tk.id).str=="c1")
        Lexer.lex(); assert(alls.tk1.lin==2 && alls.tk1.col==1) ; assert(alls.tk1 is Tk.id && (alls.tk1 as Tk.id).str=="c2")
        Lexer.lex(); assert(alls.tk1.lin==2 && alls.tk1.col==4) ; assert(alls.tk1 is Tk.id && (alls.tk1 as Tk.id).str=="c3")
        Lexer.lex(); assert(alls.tk1.lin==4 && alls.tk1.col==1) ; assert(alls.tk1 is Tk.id && (alls.tk1 as Tk.id).str=="c4")
    }
    @Test
    fun b07_lexer_xs () {
        All_restart(null, PushbackReader(StringReader("c1 a"), 2))
        Lexer.lex(); assert(alls.tk1.lin==1 && alls.tk1.col==1) ; assert(alls.tk1 is Tk.id && (alls.tk1 as Tk.id).str=="c1")
        Lexer.lex(); assert(alls.tk1.lin==1 && alls.tk1.col==4) ; assert(alls.tk1 is Tk.id && (alls.tk1 as Tk.id).str=="a")
    }

    // XNAT

    @Test
    fun b07_lexer_xnat () {
        All_restart(null, PushbackReader(StringReader("_char _Tp"), 2))
        Lexer.lex(); assert(alls.tk1 is Tk.Nat && (alls.tk1 as Tk.Nat).str=="char")
        Lexer.lex(); assert(alls.tk1 is Tk.Nat && (alls.tk1 as Tk.Nat).str=="Tp")
    }
    @Test
    fun b08_lexer_xnat () {
        All_restart(null, PushbackReader(StringReader("_{(1)} _(2+2)"), 2))
        Lexer.lex(); assert(alls.tk1 is Tk.Nat && (alls.tk1 as Tk.Nat).str=="(1)")
        Lexer.lex(); assert(alls.tk1 is Tk.Nat && (alls.tk1 as Tk.Nat).str=="2+2")
    }

    // XNUM

    @Test
    fun b09_lexer_xnum () {
        All_restart(null, PushbackReader(StringReader(".a"), 2))
        Lexer.lex(); assert(alls.tk1.str==".")
        //Lexer.lex() ; assert(alls.tk1.enu==TK.ERR && (alls.tk1 as Tk.Err).err=="a")
    }
    @Test
    fun b10_lexer_xnum () {
        All_restart(null, PushbackReader(StringReader(".10"), 2))
        Lexer.lex(); Lexer.lex()
        assert(alls.tk1 is Tk.Num && alls.tk1.str=="10")
    }

    // XSCOPE

    @Test
    fun c01_scope () {
        All_restart(null, PushbackReader(StringReader("@GLOBAL"), 2))
        Lexer.lex()
        assert((alls.tk1 as Tk.Scp).str=="GLOBAL")
    }
    @Test
    fun c02_scope () {
        All_restart(null, PushbackReader(StringReader("@i1"), 2))
        Lexer.lex()
        assert((alls.tk1 as Tk.Scp).str=="i1")
    }
    @Test
    fun c03_scope () {
        All_restart(null, PushbackReader(StringReader("@x11"), 2))
        Lexer.lex()
        assert((alls.tk1 as Tk.Scp).str=="x11")
    }
    @Test
    fun c04_scope () {
        All_restart(null, PushbackReader(StringReader("@[]"), 2))
        Lexer.lex()
        //println(alls.tk1)
        //assert(alls.tk1.enu==TK.XSCPCST && (alls.tk1 as Tk.Scp1).lbl=="" && (alls.tk1 as Tk.Scp1).num==null)
        //assert(alls.tk1.enu==TK.ERR && (alls.tk1 as Tk.Err).err=="@")
        assert(alls.tk1.str=="@[")
    }

    // WCLOCK

    @Test
    fun d01_clk () {
        All_restart(null, PushbackReader(StringReader("1s"), 2))
        Lexer.lex()
        //println(alls.tk1)
        assert(alls.tk1 is Tk.Clk && alls.tk1.str=="1000")
    }
    @Test
    fun d02_clk () {
        All_restart(null, PushbackReader(StringReader("1ss"), 2))
        Lexer.lex()
        assert(alls.tk1 is Tk.Err && (alls.tk1 as Tk.Err).str=="invalid time constant")
    }
    @Test
    fun d03_clk () {
        All_restart(null, PushbackReader(StringReader("1s1"), 2))
        Lexer.lex()
        assert(alls.tk1 is Tk.Err && (alls.tk1 as Tk.Err).str=="invalid time constant")
    }
    @Test
    fun d04_clk () {
        All_restart(null, PushbackReader(StringReader("1h5min2s20ms"), 2))
        Lexer.lex()
        assert(alls.tk1 is Tk.Clk && alls.tk1.str=="3902020")
    }

    // LIN / COL

    @Test
    fun e01_lincol () {
        All_restart(null, PushbackReader(StringReader("c1 ^[5,10]\na\n^[]\n b"), 2))
        Lexer.lex(); assert(alls.tk1.lin==1 && alls.tk1.col==1)  ; assert(alls.tk1 is Tk.id && (alls.tk1 as Tk.id).str=="c1")
        Lexer.lex(); assert(alls.tk1.lin==5 && alls.tk1.col==10) ; assert(alls.tk1 is Tk.id && (alls.tk1 as Tk.id).str=="a")
        Lexer.lex(); assert(alls.tk1.lin==4 && alls.tk1.col==2)  ; assert(alls.tk1 is Tk.id && (alls.tk1 as Tk.id).str=="b")
    }
    @Test
    fun e02_lincol () {
        try {
            All_restart(null, PushbackReader(StringReader("c1 \na\n ^\"err.ce\"\n^[]\n b"), 2))
            Lexer.lex(); Lexer.lex(); Lexer.lex()
            error("bug found")
        } catch (e: Throwable) {
            assert(e.message!! == "(ln 3, col 4): file not found : err.ce") { e.message!! }
        }
    }
    @Test
    fun e03_lincol () {
        All_restart(null, PushbackReader(StringReader("c1 ^[5,10]\na\n^\"test-lincol.ceu\"\n^[]\n b"), 2))
        Lexer.lex(); assert(alls.tk1.lin==1 && alls.tk1.col==1)  ; assert(alls.tk1 is Tk.id && (alls.tk1 as Tk.id).str=="c1")
        Lexer.lex(); assert(alls.tk1.lin==5 && alls.tk1.col==10) ; assert(alls.tk1 is Tk.id && (alls.tk1 as Tk.id).str=="a")
        Lexer.lex(); assert(all().file=="test-lincol.ceu" && alls.tk1.lin==1 && alls.tk1.col==1) ; assert(alls.tk1 is Tk.id && (alls.tk1 as Tk.id).str=="inside")
        Lexer.lex(); assert(alls.tk1.lin==5 && alls.tk1.col==2)  ; assert(alls.tk1 is Tk.id && (alls.tk1 as Tk.id).str=="b")
    }
}