import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.MethodOrderer.Alphanumeric
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder

@TestMethodOrder(Alphanumeric::class)
class TCode {

    val tp_unit = Type.Unit(Tk.Fix(TK.FIX, "()", 1, 1))
    // TYPE

    companion object {
        @BeforeAll
        @JvmStatic
        internal fun setup () {
            LINES = false
        }
    }

    @Test
    fun a01_type_unit () {
        assert(tp_unit.toce() == "Unit")
    }
    @Test
    fun a02_type_tuple () {
        val tp = Type.Tuple(Tk.Fix(TK.FIX, "[", 1, 1), listOf(tp_unit, tp_unit), null)
        assert(tp.toce() == "T_Unit_Unit_T") { tp.toce() }
    }

    // EXPR

    @Test
    fun b01_expr_unit () {
        val e = Expr.Unit(Tk.Fix(TK.FIX, "()", 1, 1))
        e.wtype = tp_unit
        code_fe(e)
        assert(CODE.removeFirst().expr == "0")
    }
    @Test
    fun b02_expr_var () {
        val e = Expr.Var(Tk.id(TK.id, "xxx", 1, 1))
        e.wenv =
            Stmt.Var(
                Tk.id(TK.id, "xxx", 1, 1),
                Type.Nat(Tk.Nat(TK.NAT, "int", 1, 1, null))
            )
        e.wtype = Type.Nat(Tk.Nat(TK.NAT, "int", 1, 1, null))
        code_fe(e)
        CODE.removeFirst().let { assert(it.expr == "(global.xxx)") { it.expr } }
    }
    @Test
    fun b03_expr_nat () {
        val e = Expr.Var(Tk.id(TK.NAT, "xxx", 1, 1))
        e.wenv =
            Stmt.Var(
                Tk.id(TK.id, "xxx", 1, 1),
                Type.Nat(Tk.Nat(TK.NAT, "int", 1, 1, null))
            )
        e.wtype = Type.Nat(Tk.Nat(TK.NAT, "int", 1, 1, null))
        code_fe(e)
        assert(CODE.removeFirst().expr == "(global.xxx)")
    }
    @Test
    fun b04_expr_tuple () {
        val e = Expr.TCons(
            Tk.Fix(TK.FIX, "(", 0, 0),
            listOf(
                Expr.Unit(Tk.Fix(TK.FIX, "()", 1, 1)),
                Expr.Unit(Tk.Fix(TK.FIX, "()", 1, 1)),
            ),
            null
        )
        e.wtype = Type.Tuple(Tk.Fix(TK.FIX, "[", 1, 1), listOf(tp_unit, tp_unit), null)
        e.arg[0].wtype = tp_unit
        e.arg[1].wtype = tp_unit
        e.visit(null, ::code_fe, null, null)
        CODE.removeFirst().expr.let {
            assert(it == "((struct T_Unit_Unit_T) { 0, 0 })")
        }
    }
    @Test
    fun b05_expr_index () {
        val e = Expr.TDisc(
            Tk.Num(TK.NUM, "1", 1, 1, 1),
            Expr.Var(Tk.id(TK.id, "x", 1, 1))
        )
        e.tup.wenv =
            Stmt.Var(
                Tk.id(TK.id, "x", 1, 1),
                Type.Tuple(Tk.Fix(TK.FIX, "(", 1, 1), listOf(Type.Nat(Tk.Nat(TK.NAT, "int", 1, 1, null))), null)
            )
        e.wtype = Type.Nat(Tk.Nat(TK.NAT, "int", 1, 1, null))
        e.tup.wtype = Type.Tuple(Tk.Fix(TK.FIX, "(", 1, 1), listOf(Type.Nat(Tk.Nat(TK.NAT, "int", 1, 1, null))), null)
        e.visit(null, ::code_fe, null, null)
        CODE.removeFirst().expr.let {
            assert(it == "(global.x)._1")
        }
    }

    // STMT

    @Test
    fun c01_stmt_pass () {
        val s = Stmt.Nop(Tk.Err(TK.ERR, "", 1, 1))
        s.visit(::code_fs, null, null, null)
        assert(CODE.removeFirst().stmt == "")
        assert(CODE.size == 0)
    }

    // CODE

    @Disabled
    @Test
    fun d01 () {
        val s = Stmt.Nop(Tk.Err(TK.ERR, "", 1, 1))
        val out = s.code()
        assert(out == """
            #include <assert.h>
            #include <stdio.h>
            #include <stdlib.h>
            
            #define input_std_int(x)     ({ int _x ; scanf("%d",&_x) ; _x ; })
            #define output_std_Unit_(x)  printf("()")
            #define output_std_Unit(x)   (output_std_Unit_(x), puts(""))
            #define output_std_int_(x)   printf("%d",x)
            #define output_std_int(x)    (output_std_int_(x), puts(""))
            #define output_std_char__(x) printf("\"%s\"",x)
            #define output_std_char_(x)  (output_std_char__(x), puts(""))
            #define output_std_Ptr_(x)   printf("%p",x)
            #define output_std_Ptr(x)    (output_std_Ptr_(x), puts(""))

            typedef struct Pool {
                void* val;
                struct Pool* nxt;
            } Pool;
            
            void pool_free (Pool** pool) {
                while (*pool != NULL) {
                    Pool* cur = *pool;
                    *pool = cur->nxt;
                    free(cur->val);
                    free(cur);
                }
                *pool = NULL;
            }

            void pool_push (Pool** root, void* val) {
                Pool* pool = malloc(sizeof(Pool));
                assert(pool!=NULL && "not enough memory");
                pool->val = val;
                pool->nxt = *root;
                *root = pool;
            }

            Pool** pool_GLOBAL;
            int evt;





            int main (void) {
                Pool* pool  __attribute__((__cleanup__(pool_free))) = NULL;
                pool_GLOBAL = &pool;
                Pool** pool_LOCAL  = &pool;
                
            }

        """.trimIndent()) { out }
    }
}