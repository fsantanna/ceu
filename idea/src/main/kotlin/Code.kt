data class Code (val type: String, val struct: String, val func: String, val stmt: String, val expr: String)
val CODE = ArrayDeque<Code>()

var EVENT_SIZE = 32  // 8 bytes (Task: uint64_t) // TODO: change if Event uses more

fun Any.self_or_null (): String {
    return if (this.ups_first { it is Expr.Func } == null) "NULL" else "(&task1->task0)"
}

fun concrete (str:String, args:List<Type>): String {
    return "CEU_"+str+args.map { "_"+it.toce() }.joinToString("")
}

fun Type.pos (): String {
    return when (this) {
        is Type.Named   -> concrete(this.tk.str, this.xargs!!)
        is Type.Unit    -> "int"
        is Type.Pointer -> this.pln.pos() + "*"
        is Type.Nat     -> this.tk_.payload().let { if (it == "") "int" else it }
        is Type.Par     -> this.xtype.let { if (it==null) "TODO1" else it.pos() }
        is Type.Tuple   -> "struct " + this.toce()
        is Type.Union   -> "struct " + this.toce()
        is Type.Func    -> "struct " + this.toce()
        is Type.Active  -> this.tsk.pos() + "*"
        is Type.Actives -> "Tasks"
    }
}

// TODO: remove this funcion. only (flawed) output systems needs it
fun Type.uname (): Type {
    return if (this !is Type.Named) this else this.nm_uname().clone(this.tk,this)
}

fun Type.output_Std (c: String, arg: String): String {
    val pln_from_ptr_to_tup_or_uni: Type? = this.uname().let {
        if (it !is Type.Pointer) null else {
            it.pln.uname().let {
                if (it is Type.Tuple || it is Type.Union) it else null
            }
        }
    }
    return when {
        (pln_from_ptr_to_tup_or_uni != null) ->"output_Std_${pln_from_ptr_to_tup_or_uni.toce()}$c($arg);\n"
        this is Type.Pointer || this is Type.Func -> {
            if (c == "_") "putchar('_');\n" else "puts(\"_\");\n"
        }
        this is Type.Nat -> {
            val out = "output_Std_${this.toce()}$c"
            """
                
                #ifdef $out
                    $out($arg);
                #else
                    //assert(0 && "$out");
                    puts("_");
                #endif
                
            """.trimIndent()
        }
        else -> "output_Std_${this.uname().toce()}$c($arg);\n"
    }
}

val TYPEX = mutableSetOf<String>()

fun code_ft (tp: Type) {
    CODE.addFirst(when (tp) {
        is Type.Nat, is Type.Unit, is Type.Par -> Code("","","","","")
        is Type.Pointer -> CODE.removeFirst()
        is Type.Active  -> CODE.removeFirst()
        is Type.Actives -> CODE.removeFirst()
        is Type.Named   -> if (tp.xargs!!.size == 0) Code("","","","","") else {
            val args = tp.xargs!!.map { CODE.removeFirst() }.reversed()
            val types = args.map { it.type }.joinToString("")
            val structs  = args.map { it.struct  }.joinToString("")
            val def1 = tp.def()!!
            if (def1.isParentOf(tp)) {
                Code("","", "", "", "")     // TODO: will be ignored on TYPEX
            } else {
                def1.visit(::code_fs, ::code_fe, ::code_ft, null)
                val def2 = CODE.removeFirst()
                Code(types+def2.type,structs+def2.struct, def2.func, "", "")
            }
        }
        is Type.Func  -> {
            val out = CODE.removeFirst()
            val pub = if (tp.pub == null) Code("","","","","") else CODE.removeFirst()
            val inp = CODE.removeFirst()

            val type = """
                // Type.Func.type
                struct ${tp.toce()};

            """.trimIndent()

            val struct = """
                // Type.Func.struct
                
                typedef struct ${tp.toce()} {
                    Task task0;
                    union {
                        Block* blks[${tp.xscps.second!!.size}];
                        struct {
                            ${tp.xscps.second!!.let { if (it.size == 0) "" else
                                it.map { "Block* ${it.scp1.str};\n" }.joinToString("") }
                            }
                        };
                    };
                    struct {
                        ${tp.inp.pos()} arg;
                        CEU_Event evt;
                        CEU_Error err;
                        ${tp.pub.let { if (it == null) "" else it.pos() + " pub;" }}
                        ${tp.out.pos()} ret;
                    };
                } ${tp.toce()};

                typedef struct {
                    Block* blks[${tp.xscps.second!!.size}];
                    ${tp.inp.pos()} arg;
                } X_PARS_${tp.toce()};

                typedef union {
                    _CEU_Event* evt;
                    void** err;         // double pointer to return if caught or not
                    X_PARS_${tp.toce()}* pars;
                } X_${tp.toce()};

                typedef void (*F_${tp.toce()}) (Stack*, ${tp.toce()}*, X_${tp.toce()});
                
            """.trimIndent()

            Code(type+inp.type+pub.type+out.type, inp.struct+pub.struct+out.struct+struct, inp.func+pub.func+out.func, "","")
        }
        is Type.Tuple -> {
            val ce = tp.toce()
            val type    = """
                // Type.Tuple.type
                struct $ce;
                void output_Std_${ce}_ (${tp.pos()}* v);
                void output_Std_${ce}  (${tp.pos()}* v);
                
            """.trimIndent()
            val struct  = """
                // Type.Tuple.struct
                struct $ce {
                    ${tp.vec  // do not filter to keep correct i
                        .mapIndexed { i,sub -> (sub.pos() + " _" + (i+1).toString() + ";\n") }
                        .joinToString("")
                    }
                };
                void output_Std_${ce}_ (${tp.pos()}* v) {
                    printf("[");
                    ${tp.vec
                        .mapIndexed { i,sub ->
                            val s = when (sub.uname()) {
                                is Type.Union, is Type.Tuple -> "&v->_${i + 1}"
                                else -> "v->_${i + 1}"
                            }
                            sub.output_Std("_", s)
                        }
                        .joinToString("putchar(',');\n")
                    }
                    printf("]");
                }
                void output_Std_${ce} (${tp.pos()}* v) {
                    output_Std_${ce}_(v);
                    puts("");
                }

            """.trimIndent()
            val codes   = tp.vec.map { CODE.removeFirst() }.reversed()
            val types   = codes.map { it.type }.joinToString("")
            val structs = codes.map { it.struct  }.joinToString("")
            Code(types+type,structs+struct, "", "", "")
        }
        is Type.Union -> {
            val ce = tp.toce()
            val type    = """
                // Type.Union.type
                struct $ce;
                void output_Std_${ce}_ (${tp.pos()}* v);
                void output_Std_${ce} (${tp.pos()}* v);

            """.trimIndent()
            val struct  = """
                // Type.Union.struct
                struct $ce {
                    union {
                        ${if (tp.wup.let { it is Stmt.Typedef && (it.tk.str=="Event" || it.tk.str=="Error") }) "char _ceu[${EVENT_SIZE}];" else "" }
                        ${if (tp.common == null) "" else { """
                            union {
                                ${tp.common.pos()} _0;            
                                ${tp.common.pos()} common;  
                            };
                            
                        """.trimIndent()
                        }}
                        ${tp.vec  // do not filter to keep correct i
                            .mapIndexed { i,sub -> """
                                union {
                                    ${sub.pos()} _${i+1};
                                    ${if (tp.yids == null) "" else "${sub.pos()} ${tp.yids[i].str};"}
                                };
                                
                                """.trimIndent()
                            }
                            .joinToString("")
                        }
                    };
                    int tag;
                };
                void output_Std_${ce}_ (${tp.pos()}* v) {
                    // TODO: only if tp.isrec
                    if (v == NULL) {
                        printf("Null");
                        return;
                    }
                    printf("<.%d", v->tag);
                    switch (v->tag) {
                        ${tp.vec
                            .mapIndexed { i,sub ->
                                val s = when (sub.uname()) {
                                    is Type.Unit -> ""
                                    is Type.Union, is Type.Tuple -> "putchar(' ');\n" + sub.output_Std("_", "&v->_${i+1}")
                                    else -> "putchar(' ');\n" + sub.output_Std("_", "v->_${i+1}")
                                }
                                """
                                case ${i+1}:
                                    $s
                                break;

                                """.trimIndent()
                            }.joinToString("")
                        }
                    }
                    putchar('>');
                }
                void output_Std_${ce} (${tp.pos()}* v) {
                    output_Std_${ce}_(v);
                    puts("");
                }

            """.trimIndent()
            val codes   = tp.vec.map { CODE.removeFirst() }.reversed()
            val common  = if (tp.common == null) Code("","","","","") else CODE.removeFirst()
            val types   = codes.map { it.type }.joinToString("")
            val structs = codes.map { it.struct }.joinToString("")
            Code(common.type+types+type,common.struct+structs+struct, "", "", "")
        }
    }.let {
        val ce = tp.toce()
        //println(">>>")
        //println(ce)
        //println(tp.tostr())
        //println("<<<")
        if (TYPEX.contains(ce) || (it.type+it.struct+it.func+it.stmt+it.expr=="")) {
            Code("","", "", "","")
        } else {
            TYPEX.add(ce)
            it
        }
    }.let {
        val line = if (!LINES) "" else "\n#line ${tp.tk.lin} \"CEU\"\n"
        assert(it.expr == "")
        assert(it.stmt == "")
        Code(line+it.type, line+it.struct, line+it.func, "", "")
    })
}

fun String.loc_mem (up: Any): String {
    val isglb = up.ups_first(true){ it is Expr.Func } == null
    return when {
        isglb -> "(global.$this)"
        (this in listOf("arg","pub","evt","err","ret","status")) -> "(task1->$this)"
        else -> "(task2->$this)"
    }
}

fun String.out_mem (up: Any, tp: String=""): String {
    val env = up.env(this)!!
    val str = if (env is Stmt.Block) "B${env.n}" else this+tp

    val upf = env.ups_first(true){ it is Expr.Func }
    if (upf == null) {
        return "(global.$str)"
    }

    val ispar = this in listOf("arg","pub","evt","err","ret","status")
    val jmps = up.ups_tolist().filter { it is Expr.Func }.takeWhile { it != upf }.size
    if (jmps == 0) {
        val tsk = if (ispar) "task1" else "task2"
        return "($tsk->$str)"
    } else {
        val lnks = ("->links.tsk_up").repeat(jmps)
        val tsk = if (ispar) "task1." else ""
        return "((Func_${(upf as Expr.Func).n}*)(task0" + lnks + "))->${tsk}$str"
    }
}

fun Scope.toce (up: Any): String {
    val id = this.scp1.str
    return when {
        // @GLOBAL is never an argument
        (id == "GLOBAL") -> "GLOBAL"
        // @i_1 is always an argument (must be after closure test)
        this.scp1.isscopepar() -> "(task1->$id)"
        // otherwise depends (calls mem)
        else -> "(&" + id.out_mem(up) + ")"
    }
}

fun Any.localBlockMem (): String {
    return this.localBlockScp1Id().let { if (it == "GLOBAL") "GLOBAL" else "(&" + it.loc_mem(this) + ")" }
}

fun String.native (up: Any, tk: Tk): String {
    var ret = ""
    var i = 0
    while (i < this.length) {
        ret += if (this[i] == '$') {
            i++
            All_assert_tk(tk, i < this.length) { "invalid `\$´" }
            val open = if (this[i] != '{') false else { i++; true }
            var ce = ""
            while (i<this.length && (this[i].isLetterOrDigit() || this[i] == '_' || (open && this[i]!='}'))) {
                ce += this[i]
                i++
            }
            if (open) {
                All_assert_tk(tk, i < this.length) { "invalid `\$´" }
                assert(this[i]=='}') { "bug found" }
                i++
            }
            val env = up.env(ce)
            All_assert_tk(tk, env!=null) {
                "invalid variable \"$ce\""
            }
            ce.out_mem(up)
        } else {
            this[i++]
        }
    }
    return ret
}

fun Stmt.mem_vars (): String {
    return when (this) {
        is Stmt.Nop, is Stmt.Set, is Stmt.Native, is Stmt.SCall, is Stmt.SSpawn,
        is Stmt.DSpawn, is Stmt.Await, is Stmt.Emit, is Stmt.Throw,
        is Stmt.Input, is Stmt.Output, is Stmt.Pause, is Stmt.XReturn, is Stmt.XBreak,
        is Stmt.Typedef -> ""

        is Stmt.Var -> this.xtype!!.let {
            when {
                !it.isConcrete() -> ""
                this.envs(this.tk.str).isEmpty() -> "${this.xtype!!.pos()} ${this.tk.str};\n"
                else -> "${it.pos()} ${this.tk.str}_${it.toce()};\n"
            }
        }
        is Stmt.Loop -> this.block.mem_vars()
        is Stmt.DLoop -> this.block.mem_vars()

        is Stmt.Block -> """
            struct {
                Block B${this.n};
                ${this.body.mem_vars()}
            };
            
        """.trimIndent()

        is Stmt.If -> """
            union {
                ${this.true_.mem_vars()}
                ${this.false_.mem_vars()}
            };
                
        """.trimIndent()

        is Stmt.Seq -> {
            if (this.s1 !is Stmt.Block) {
                this.s1.mem_vars() + this.s2.mem_vars()
            } else {
                """
                    union {
                        ${this.s1.mem_vars()}
                        struct {
                            ${this.s2.mem_vars()}
                        };
                    };
                    
                """.trimIndent()
            }
        }
    }
}

fun code_fe (e: Expr) {
    val xp = e.wtype!!
    CODE.addFirst(when (e) {
        is Expr.Unit  -> Code("", "", "", "", "0")
        is Expr.Nat   -> CODE.removeFirst().let { Code(it.type, it.struct, it.func, it.stmt, e.tk_.payload().native(e, e.tk)) }
        is Expr.Var   -> {
            val dcl = e.env(e.tk.str)!!.getType()!!
            val tp = if (dcl.isConcrete()) "" else "_"+e.wtype!!.toce()
            Code("", "", "", "", e.tk.str.out_mem(e,tp))
        }
        is Expr.Upref -> CODE.removeFirst().let { Code(it.type, it.struct, it.func, it.stmt, "(&" + it.expr + ")") }
        is Expr.Dnref -> CODE.removeFirst().let { Code(it.type, it.struct, it.func, it.stmt, "(*" + it.expr + ")") }
        is Expr.TDisc -> CODE.removeFirst().let { Code(it.type, it.struct, it.func, it.stmt, it.expr + "._" + e.tk.field2num((e.tup.wtype as Type.Tuple).yids)) }
        is Expr.Cast  -> {
            val tp = CODE.removeFirst()
            val ex = CODE.removeFirst()
            val (pre,pos) = when (e.type) {
                is Type.Nat   -> Pair("", "((${e.type.toce()})${ex.expr})")
                is Type.Named -> {
                    var xxx = e.type.nm_uname()
                    var yyy = ex.expr
                    val pre = e.type.subs.map {
                        val uni = xxx as Type.Union
                        val num = it.field2num(uni.yids)!!
                        val pre = """
                            assert(&$yyy != NULL);    // TODO: only if e.uni.wtype!!.isrec()
                            ${if (num == 0) "" else "assert($yyy.tag == $num);"}
            
                        """.trimIndent()
                        xxx = uni.vec[num-1]
                        yyy = yyy + "._$num"
                        pre
                    }.joinToString("")
                    Pair(pre,ex.expr)
                }
                else -> Pair("", ex.expr)
            }
            Code(ex.type+tp.type, ex.struct+tp.struct, ex.func+tp.func, ex.stmt+tp.stmt+pre, pos)
        }
        is Expr.Named   -> {
            val tp = if (e.xtype==null) Code("","","","","") else CODE.removeFirst()
            val ex = CODE.removeFirst()
            val v = if (e.xtype==null || e.e.wtype !is Type.Union) ex.expr else {
                // because of type stretching
                "(*(${e.wtype!!.pos()}*)&${ex.expr})"
            }
            Code(tp.type+ex.type, tp.struct+ex.struct, tp.func+ex.func, tp.stmt+ex.stmt, tp.expr+v)
        }
        is Expr.UNamed -> CODE.removeFirst().let {
            Code(it.type, it.struct, it.func, it.stmt, it.expr)
        }
        is Expr.Field -> CODE.removeFirst().let {
            val src = when (e.tk.str) {
                "status" -> it.expr + "->task0.status"
                "pub"    -> it.expr + "->pub"
                "ret"    -> it.expr + "->ret"
                else     -> error("bug found")
            }
            Code(it.type, it.struct, it.func, it.stmt, src)
        }
        is Expr.UDisc -> CODE.removeFirst().let {
            val ee = it.expr
            val num = e.tk.field2num((e.uni.wtype as Type.Union).yids)!!
            val pre = """
                assert(&$ee != NULL);    // TODO: only if e.uni.wtype!!.isrec()
                ${if (num == 0) "" else "assert($ee.tag == $num);"}

            """.trimIndent()
            Code(it.type, it.struct, it.func, it.stmt+pre, "("+ee+"._"+num+")")
        }
        is Expr.UPred -> CODE.removeFirst().let {
            val ee = it.expr
            val pos = when {
                (e.tk.str == "Null")  -> "(&$ee == NULL)"
                (e.wup is Expr.UPred) -> {
                    val num = e.tk.field2num((e.uni.wtype as Type.Union).yids)!!
                    "($ee.tag == $num) && $ee._$num"
                }
                (e.uni is Expr.UPred) -> {
                    val num = e.tk.field2num((e.uni.wtype as Type.Union).yids)!!
                    "($ee.tag == $num)"
                }
                else -> {
                    val num = e.tk.field2num((e.uni.wtype as Type.Union).yids)!!
                    "((&$ee != NULL) && ($ee.tag == $num))"
                }
            }
            Code(it.type, it.struct, it.func, it.stmt, pos)
        }
        is Expr.New  -> CODE.removeFirst().let {
            val ID  = "__tmp_" + e.n
            val ptr = e.wtype!! as Type.Pointer

            val pre = """
                ${ptr.pos()} $ID = malloc(sizeof(*$ID));
                assert($ID!=NULL && "not enough memory");
                *$ID = ${it.expr};
                block_push(${ptr.xscp!!.toce(ptr)}, $ID);

            """.trimIndent()
            Code(it.type, it.struct, it.func, it.stmt+pre, ID)
        }
        is Expr.TCons -> {
            val args = (1..e.arg.size).map { CODE.removeFirst() }.reversed()
            Code (
                args.map { it.type   }.joinToString(""),
                args.map { it.struct }.joinToString(""),
                args.map { it.func   }.joinToString(""),
                args.map { it.stmt   }.joinToString(""),
                args.map { it.expr   }.filter { it!="" }.joinToString(", ").let { "((${xp.pos()}) { $it })" }
            )
        }
        is Expr.UCons -> {
            val arg = CODE.removeFirst()
            val tp  = CODE.removeFirst()
            val ID  = "_tmp_" + e.n
            val pos = xp.pos()
            val num = e.tk.field2num(e.xtype!!.yids)!!
            val cast = e.xtype!!.vec[num-1].pos()
            val pre = "$pos $ID = (($pos) { .tag=$num , ._$num = /*($cast) (long)*/ ${arg.expr} });\n"
            Code(tp.type+arg.type, tp.struct+arg.struct, tp.func+arg.func, arg.stmt + pre, ID)
        }
        is Expr.UNull -> CODE.removeFirst().let { Code(it.type, it.struct, it.func,"","NULL") }
        is Expr.Call  -> {
            val arg  = CODE.removeFirst()
            val f    = CODE.removeFirst()
            val blks = e.xscps.first!!.map { it.toce(e) }.joinToString(",")
            val tpf  = e.f.wtype!!
            val upspawn = e.upspawn()
            when {
                (e.f is Expr.Var && e.f.tk.str=="output_Std") -> {
                    Code (
                        f.type + arg.type,
                        f.struct + arg.struct,
                        f.func + arg.func,
                        f.stmt + arg.stmt + e.arg.wtype!!.output_Std("", arg.expr),
                        ""
                    )
                }
                (tpf is Type.Func) -> {
                    val block = upspawn.let {
                        if (it is Stmt.DSpawn) {
                            "&" + (it.dst as Expr.Var).tk.str.out_mem(e) + ".block"
                        } else {
                            // always local
                            e.localBlockMem()
                        }
                    }

                    val (ret1,ret2) = when (upspawn) {
                        is Stmt.SSpawn -> Pair("${tpf.toce()}* ret_${e.n};", "ret_${e.n} = frame;")
                        is Stmt.DSpawn -> Pair("", "")
                        else           -> Pair("${tpf.out.pos()} ret_${e.n};", "ret_${e.n} = (((${tpf.toce()}*)frame)->ret);")
                    }

                    val task0 = if (e.ups_first { it is Expr.Func } == null) "NULL" else "task0"
                    val pre = """
                        $ret1
                        {
                            ${if (upspawn !is Stmt.DSpawn) "" else {
                                val dst = upspawn.dst as Expr.Var
                                val len = ((dst.env(dst.tk.str) as Stmt.Var).xtype as Type.Actives).len?.str?.toInt() ?: 0
                                val mem = dst.tk.str.out_mem(e)
                                "if ($len==0 || pool_size((Task*)&$mem)<$len) {"
                            }}
                            Stack stk_${e.n} = { stack, ${e.self_or_null()}, ${e.localBlockMem()} };
                            ${tpf.toce()}* frame = (${tpf.toce()}*) malloc(${f.expr}.task0.size);
                            assert(frame!=NULL && "not enough memory");
                            *frame = *(${tpf.toce()}*) &${f.expr};
                            //${if (upspawn is Stmt.DSpawn) "frame->task0.isauto = 1;" else ""}
                            block_push($block, frame);
                            //frame->task0.links.tsk_up = $task0;
                            task_link(&frame->task0, $task0, $block);
                            frame->task0.status = TASK_UNBORN;
                            X_PARS_${tpf.toce()} _tmp_${e.n} = { {$blks}, ${arg.expr} };
                            ((F_${tpf.toce()})(frame->task0.f)) (
                                &stk_${e.n},
                                frame,
                                (X_${tpf.toce()}) {.pars=&_tmp_${e.n}}
                            );
                            if (stk_${e.n}.block == NULL) {
                                return;
                            }
                            $ret2
                            ${if (upspawn !is Stmt.DSpawn) "" else "}"}
                        }
                        
                        """.trimIndent()
                    Code (
                        f.type + arg.type,
                        f.struct + arg.struct,
                        f.func + arg.func,
                        f.stmt + arg.stmt + pre,
                        "ret_${e.n}"
                    )
                }
                else -> {
                    Code (
                        f.type + arg.type,
                        f.struct + arg.struct,
                        f.func + arg.func,
                        f.stmt + arg.stmt,
                        f.expr + "(" + blks + (if (e.arg is Expr.Unit) "" else arg.expr) + ")"
                    )
                }
            }
        }
        is Expr.Func -> {
            val block = CODE.removeFirst()
            val tp    = CODE.removeFirst()

            val type = """
                // Expr.Func.type
                struct Func_${e.n};
                //void func_${e.n} (Stack* stack, struct Func_${e.n}* task2, X_${e.xtype!!.toce()} xxx);
                
            """.trimIndent()

            val struct = """
                // Expr.Func.struct
                typedef struct Func_${e.n} {
                    ${e.xtype!!.toce()} task1;
                    ${e.block.mem_vars()}
                } Func_${e.n};
                
            """.trimIndent()

            val func = """
                void func_${e.n} (Stack* stack, struct Func_${e.n}* task2, X_${e.xtype!!.toce()} xxx) {
                    Task* task0 = &task2->task1.task0;
                    ${e.xtype!!.toce()}* task1 = &task2->task1;
                    ${e.xtype!!.xscps.second!!.mapIndexed { i, _ -> "task1->blks[$i] = xxx.pars->blks[$i];\n" }.joinToString("")}
                    assert(task0->status==TASK_UNBORN || task0->status==TASK_AWAITING || stack->block->catch!=0);
                    switch (task0->pc) {
                        case 0: {                    
                            assert(task0->status == TASK_UNBORN);
                            task0->status = TASK_RUNNING;
                            task2->task1.arg = xxx.pars->arg;
                            ${block.stmt}
                            break;
                        }
                        default:
                            assert(0 && "invalid PC");
                            break;
                    }
                    return;
                }
                
            """.trimIndent()

            val task0 = if (e.ups_first { it is Expr.Func } == null) "NULL" else "task0"
            val src = """
                Func_${e.n} _tmp_${e.n} = {
                    TASK_UNBORN, {$task0,NULL,NULL,{}}, sizeof(Func_${e.n}), (Task_F)func_${e.n}, 0
                };
                //static Func_${e.n}* frame_${e.n} = &_frame_${e.n};
    
            """.trimIndent()

            Code(tp.type+type+block.type, tp.struct+block.struct+struct, tp.func+block.func+func, src, "(*(${e.xtype!!.toce()}*)&_tmp_${e.n})")
        }
        is Expr.If -> {
            val false_ = CODE.removeFirst()
            val true_  = CODE.removeFirst()
            val tst    = CODE.removeFirst()
            val stmt   = """
                ${e.wtype!!.pos()} _tmp_${e.n};
                if (${tst.expr}) {
                    ${true_.stmt}
                    _tmp_${e.n} = ${true_.expr};
                } else {
                    ${false_.stmt}
                    _tmp_${e.n} = ${false_.expr};
                }
                
            """.trimIndent()
            Code (
                tst.type+true_.type+false_.type,
                tst.struct+true_.struct+false_.struct,
                tst.func+true_.func+false_.func,
                tst.stmt+stmt,
                "_tmp_${e.n}"
            )
        }
    }.let {
        val line = if (!LINES) "" else "\n#line ${e.tk.lin} \"CEU\"\n"
        Code(line+it.type, line+it.struct, line+it.func, line+it.stmt, line+it.expr)
    })
}

fun Stmt.throwExpr (e: String): String {
    return """
    {
        Stack stk = { stack, ${this.self_or_null()}, ${this.localBlockMem()} };
        block_throw(&stk, $e, task0, ${this.localBlockMem()});
        assert(stk.block == NULL);
        if (stk.block == NULL) {
            return;
        }
    }
    
    """.trimIndent()
}

fun code_fs (s: Stmt) {
    CODE.addFirst(when (s) {
        is Stmt.Nop -> Code("", "","","", "")
        is Stmt.Typedef -> {
            val xtype = s.xtype!!.let { CODE.removeFirst() }
            val type  = CODE.removeFirst()
            if (s.pars.size>0 && s.args==null) {
                // generic type: do not generate code
                Code("", "","","", "")
            } else {
                // concrete type
                val name = concrete(s.tk.str, s.args ?: emptyList())
                //if (name == "CEU_Maybe_Unit") TODO()
                //println(name + ": " + s.tostr())
                val unddef = """
                    #undef $name // ${s.tostr()}
                    #define $name ${name}_${s.n}
                    
                """.trimIndent()

                fun Type.defs(pre: String): String {
                    return if (this !is Type.Union || this.yids == null) "" else {
                        this.yids.mapIndexed { i, id -> "#define CEU_${(pre + '_' + id.str).toUpperCase()} ${i + 1}\n" }
                            .joinToString("") +
                                this.vec.mapIndexed { i, sub -> sub.defs(pre + '_' + this.yids[i].str) }
                                    .joinToString("")
                    }
                }

                val src = """
                    //#define output_Std_${s.tk.str}_ output_Std_${s.xtype!!.toce()}_
                    //#define output_Std_${s.tk.str}  output_Std_${s.xtype!!.toce()}
                    typedef ${s.xtype!!.pos()} ${name}_${s.n};
                    
                """.trimIndent()
                Code(
                    unddef + src + type.type + xtype.type + s.xtype!!.defs(s.tk.str),
                    unddef + type.struct + xtype.struct,
                    unddef + type.func + xtype.func,
                    "",
                    ""
                )
            }
        }
        is Stmt.Native -> if (s.istype) {
            Code("", s.tk_.payload().native(s,s.tk) + "\n", "", "", "")
        } else {
            Code("", "", "", s.tk_.payload().native(s,s.tk) + "\n", "")
        }
        is Stmt.Seq -> {
            val s2 = CODE.removeFirst()
            val s1 = CODE.removeFirst()
            Code(s1.type+s2.type, s1.struct+s2.struct, s1.func+s2.func, s1.stmt+s2.stmt, "")
        }
        is Stmt.Var -> CODE.removeFirst().let {
            val src = if (s.xtype is Type.Actives) {
                s.tk.str.loc_mem(s).let {
                    val blk_cur = s.localBlockMem()
                    val task0 = if (s.ups_first { it is Expr.Func } == null) "NULL" else "task0"
                    """
                        $it = (Tasks) { TASK_POOL, { NULL, NULL }, { NULL, 0, { NULL, NULL, NULL, NULL } } };
                        task_link((Task*) &$it, $task0, $blk_cur);
                        $it.links.blk_down = &$it.block;
                        $it.block.links.blk_up = $blk_cur;
                        
                    """.trimIndent()
                }
            } else ""
            Code(it.type, it.struct, it.func, src,"")
        }
        is Stmt.Set -> {
            val src = CODE.removeFirst()
            val dst = CODE.removeFirst()
            Code(dst.type+src.type, dst.struct+src.struct, dst.func+src.func, dst.stmt+src.stmt + dst.expr+" = "+src.expr + ";\n", "")
        }
        is Stmt.If -> {
            val false_ = CODE.removeFirst()
            val true_  = CODE.removeFirst()
            val tst = CODE.removeFirst()
            val src = tst.stmt + """
                if (${tst.expr}) {
                    ${true_.stmt}
                } else {
                    ${false_.stmt}
                }

            """.trimIndent()
            Code(tst.type+true_.type+false_.type, tst.struct+true_.struct+false_.struct, tst.func+true_.func+false_.func, src, "")
        }
        is Stmt.Loop -> CODE.removeFirst().let {
            Code(it.type, it.struct, it.func, "while (1) ${it.stmt}", "")
        }
        is Stmt.DLoop -> {
            val block = CODE.removeFirst()
            val tsks  = CODE.removeFirst()
            val i     = CODE.removeFirst()
            val src   = """
                {   // DLoop
                    Stack stk = { stack, (Task*)&${tsks.expr}, NULL };
                    stack = &stk;
                    ${i.expr} = (${s.i.wtype!!.pos()}) ${tsks.expr}.block.links.tsk_first;
                    while (${i.expr}!=NULL && ${i.expr}->task0.status!=TASK_DEAD) {
                        ${block.stmt}
                        ${i.expr} = (${s.i.wtype!!.pos()}) ${i.expr}->task0.links.tsk_next;
                    }
                    stack = stk.stk_up;
                }
                
            """.trimIndent()
            Code(tsks.type+i.type+block.type, tsks.struct+i.struct+block.struct, tsks.func+i.func+block.func, tsks.stmt+i.stmt+src, "")
        }
        is Stmt.SCall -> CODE.removeFirst().let {
            Code(it.type, it.struct, it.func, it.stmt+it.expr+";\n", "")
        }
        is Stmt.SSpawn -> {
            val call = CODE.removeFirst()
            val (dst,src) = if (s.dst == null) {
                val dst = Code("","","", "","")
                val src = "${call.expr};\n"
                Pair(dst, src)
            } else {
                val dst = CODE.removeFirst()
                val src = "${dst.expr} = ${call.expr};\n"
                Pair(dst, src)
            }
            Code(call.type+dst.type, call.struct+dst.struct, call.func+dst.func, call.stmt+dst.stmt+src, "")
        }
        is Stmt.DSpawn -> { // Expr.Call links call/tsks
            val call = CODE.removeFirst()
            val tsks = CODE.removeFirst()
            Code(tsks.type+call.type, tsks.struct+call.struct, tsks.func+call.func, tsks.stmt+call.stmt, "")
        }
        is Stmt.Pause -> CODE.removeFirst().let {
            val src = if (s.pause) {
                """
                /*assert(${it.expr}->task0.status==TASK_AWAITING && "trying to pause non-awaiting task");*/
                ${it.expr}->task0.status = TASK_PAUSED;
                
                """.trimIndent()
            } else {
                """
                ${it.expr}->task0.status = TASK_AWAITING;
                
                """.trimIndent()

            }
            Code(it.type, it.struct, it.func, it.stmt+src, "")
        }
        is Stmt.Await -> CODE.removeFirst().let {
            val cnd = if (s.e.wtype is Type.Nat) {
                it.expr
            } else {
                "(task1->evt.tag == _CEU_EVENT_TASK) && (((_CEU_Event*)(&task1->evt))->payload.Task == ((uint64_t)(${it.expr})))"
            }

            val src = """
                {
                    task0->pc = ${s.n};     // next awake
                    task0->status = TASK_AWAITING;
                    return;                 // await (1 = awake ok)
                case ${s.n}:                // awake here
                    assert(task0->status == TASK_AWAITING);
                    task0->status = TASK_RUNNING;
                    task1->evt = * (CEU_Event*) xxx.evt;
                    ${it.stmt}
                    if (!($cnd)) {
                        task0->status = TASK_AWAITING;
                        return;             // (0 = awake no)
                    }
                }
                
            """.trimIndent()
            Code(it.type, it.struct, it.func, src, "")
        }
        is Stmt.Emit -> {
            val evt = CODE.removeFirst()
            if (s.tgt is Scope) {
                val src = """
                {
                    Stack stk = { stack, ${s.self_or_null()}, ${s.localBlockMem()} };
                    bcast_event_block(&stk, ${s.tgt.toce(s)}, (_CEU_Event*) &${evt.expr});
                    if (stk.block == NULL) {
                        return;
                    }
                }
                
                """.trimIndent()
                Code(evt.type, evt.struct, evt.func, evt.stmt+src, "")
            } else {
                val tsk = CODE.removeFirst()
                val src = """
                {
                    Stack stk = { stack, ${s.self_or_null()}, ${s.localBlockMem()} };
                    bcast_event_task(&stk, &${tsk.expr}->task0, (_CEU_Event*) &${evt.expr}, 0);
                    if (stk.block == NULL) {
                        return;
                    }
                }
                
                """.trimIndent()
                Code(tsk.type+evt.type, tsk.struct+evt.struct, tsk.func+evt.func, tsk.stmt+evt.stmt+src, "")

            }
        }
        is Stmt.Throw -> CODE.removeFirst().let {
            val src = s.throwExpr('&' + it.expr)
            Code(it.type, it.struct, it.func, it.stmt+src, "")
        }
        is Stmt.XBreak -> {
            val n = (s.ups_first { it is Stmt.Loop } as Stmt.Loop).block.catch!!.n
            val src = """
                CEU_Error _tmp_${s.n} = { .tag=CEU_ERROR_ESCAPE, .Escape=$n };
                ${s.throwExpr("&_tmp_${s.n}")}    
            """.trimIndent()
            Code("", "", "", src, "")
        }
        is Stmt.XReturn -> {
            val n = (s.ups_first {
                it is Expr.Func && !(it.wtype as Type.Func).let { it.inp.nonat_()==null && it.out.nonat_()==null }
            } as Expr.Func).block.catch!!.n
            val src = """
                CEU_Error _tmp_${s.n} = { .tag=CEU_ERROR_ESCAPE, .Escape=$n };
                ${s.throwExpr("&_tmp_${s.n}")}    
            """.trimIndent()
            Code("", "", "", src, "")
        }
        is Stmt.Input -> {
            s.arg.e as Expr.UCons
            val idx = s.arg.e.tk.str
            val arg = CODE.removeFirst()
            if (s.dst == null) {
                val tp  = CODE.removeFirst()
                val src = "input_${idx}_${s.xtype!!.toce()}(${arg.expr});\n"
                Code(tp.type+arg.type, tp.struct+arg.struct, tp.func+arg.func, arg.stmt + src, "")
            } else {
                val dst = CODE.removeFirst()
                val tp  = CODE.removeFirst()
                val src = "${dst.expr} = input_${idx}_${s.xtype!!.toce()}(${arg.expr});\n"
                Code(tp.type+arg.type+dst.type, tp.struct+arg.struct+dst.struct, tp.func+arg.func+dst.func, arg.stmt+dst.stmt+src, "")
            }
        }
        is Stmt.Output -> CODE.removeFirst().let {
            s.arg.e as Expr.UCons
            val idx = s.arg.e.tk.str
            val call = if (idx=="1" || idx=="Std") { // output Output.1 ()
                val v = s.arg.e.arg
                v.visit(::code_fs,::code_fe,::code_ft,null)
                val x = CODE.removeFirst()
                v.wtype!!.output_Std("", x.expr)
            } else {
                //val fld = if (idx.toIntOrNull() == null) idx else "_"+idx
                "output_$idx(${it.expr});\n"
            }
            // TODO: removed it.stmt, only interested in Expr (but may fail for some Exprs)
            Code(it.type, it.struct, it.func, it.stmt+call, "")
        }
        is Stmt.Block -> {
            val body = CODE.removeFirst()
            val catch = if (s.catch==null) Code("","","","","") else CODE.removeFirst()
            val up = s.ups_first { it is Expr.Func || it is Stmt.Block }
            val blk = "B${s.n}".loc_mem(s)

            val src = """
            {
                $blk = (Block) { NULL, ${if (s.catch!=null) s.n else 0}, {NULL,NULL,NULL,NULL} };
                
                // link
                ${if (up is Stmt.Block) {
                    val cur = s.localBlockMem()
                    """
                        $cur->links.blk_down = &$blk;
                        $blk.links.blk_up = $cur;
                        //printf("BLK: %p -> %p\n", &$blk, $cur);
                    """.trimIndent()
                } else if (up is Expr.Func) {
                    // blk_up = NULL
                    "task0->links.blk_down = &$blk;"
                } else { // global
                    // blk_up = NULL
                    ""
                }}
                
                ${body.stmt}
                
                // CLEANUP
                
                ${if (s.catch==null) "" else """
                    if (0) {
                        case ${s.n}: {      // ?? catch
                            int status = task0->status;    
                            task0->status = TASK_RUNNING;
                            task1->err = * (CEU_Error*) *xxx.err;
                            ${catch.stmt}
                            if (!(${catch.expr})) {
                                task0->status = status;
                                return;     // NO catch
                            }
                            *xxx.err = NULL;
                            // OK catch
                            ${if (s.wup is Stmt.Loop) "break;" else ""} 
                        }
                    }
                """.trimIndent()}
                
                // task event
                ${if (up !is Expr.Func || up.tk.str=="func") "" else """
                {
                    //task0->status = TASK_DYING;
                    _CEU_Event evt = { .tag=_CEU_EVENT_TASK, .payload={.Task=(uint64_t)task0} };
                    Stack stk = { stack, task0, &$blk };
                    bcast_event_block(&stk, GLOBAL, (_CEU_Event*) &evt);
                    if (stk.block == NULL) {
                        //task0->status = TASK_DEAD;
//printf("do not continue %p\n", task0);
                        return;
                    }
                }
                """.trimIndent()}
                
                // block kill
                block_bcast_kill(stack, &$blk);
                
                // unlink
                // uplink still points to me, but I will not propagate down
                ${if (up is Stmt.Block) {
                    """
                    $blk.links.tsk_first  = NULL;
                    //$blk.links.tsk_last = NULL;
                    $blk.links.blk_down   = NULL;
                    """.trimIndent()
                } else if (up is Expr.Func) {
                    """
                    //task0->links.tsk_up   = NULL;
                    //task0->links.tsk_next = NULL;
                    task0->links.blk_down   = NULL;
                    task0->status = TASK_DEAD;                        
                    """.trimIndent()
                } else {
                    ""
                }}
            }
            
            """.trimIndent()

            Code(body.type+catch.type, body.struct+catch.struct, body.func+catch.func, src, "")
        }
    }.let {
        val name = "\n// "+ s.javaClass.name + "\n"
        val line = if (!LINES) "" else "\n#line ${s.tk.lin} \"CEU\"\n"
        assert(it.expr == "")
        Code(name+line+it.type, name+line+it.struct, name+line+it.func, name+line+it.stmt, "")
    })
}

fun Stmt.code (): String {
    TYPEX.clear()
    EXPR_WTYPE = false
    this.visit(::code_fs, ::code_fe, ::code_ft, null)
    EXPR_WTYPE = true

    val code = CODE.removeFirst()
    assert(CODE.size == 0)
    assert(code.expr == "")

    return ("""
        #include <stdint.h>
        #include <assert.h>
        #include <stdio.h>
        #include <stdlib.h>
        #include <string.h>
        
        #define input_1_int(x)       ({ int _x ; scanf("%d",&_x) ; _x ; })
        #define input_Std_int(x)     input_1_int(x)
        
        #define output_Std_Unit_(x)  (x, printf("()"))
        #define output_Std_Unit(x)   (output_Std_Unit_(x), puts(""))
        #define output_Std_int_(x)   printf("%d",x)
        #define output_Std_int(x)    (output_Std_int_(x), puts(""))
        #define output_Std_float_(x) printf("%f",x)
        #define output_Std_float(x)  (output_Std_float_(x), puts(""))
        #define output_Std_char__(x) printf("\"%s\"",x)
        #define output_Std_char_(x)  (output_Std_char__(x), puts(""))
        #define output_Std_Ptr_(x)   printf("%p",x)
        #define output_Std_Ptr(x)    (output_Std_Ptr_(x), puts(""))
        
        ///
        
        typedef struct Pool {
            void* val;
            struct Pool* nxt;
        } Pool;
        
        struct Block;
        struct Task;
        
        // When a block K terminates, it traverses the stack and sets to NULL
        // all matching blocks K in the stack.
        // All call/spawn/awake/emit operations need to test if its enclosing
        // block is still alive before continuing.
        typedef struct Stack {
            struct Stack* stk_up;
            struct Task*  task;     // used by throw/catch
            struct Block* block;
        } Stack;
        
        typedef enum {
            TASK_POOL=1, TASK_UNBORN, TASK_RUNNING, TASK_AWAITING, TASK_PAUSED, /*TASK_DYING,*/ TASK_DEAD
        } TASK_STATUS;
        
        typedef enum {
            _CEU_EVENT_KILL=1, _CEU_EVENT_TASK //, ...
        } _CEU_EVENT;
        
        typedef struct _CEU_Event {
            union {
                char _ceu[${EVENT_SIZE}];  // max payload size
                //void Kill;
                uint64_t Task;  // cast from Task*
                //int Timer;
            } payload;
            _CEU_EVENT tag;
        } _CEU_Event;
        
        #define CEU_Event _CEU_Event
        
        typedef enum {
            _CEU_ERROR_RETURN=1 //, ...
        } _CEU_ERROR;
        
        typedef struct _CEU_Error {
            union {
                char _ceu[${EVENT_SIZE}];  // max payload size
                int Escape;
            };
            _CEU_ERROR tag;
        } _CEU_Error;
        
        #define CEU_Error _CEU_Error
        
        // stack, task, evt
        typedef void (*Task_F) (Stack*, struct Task*, void*);
        
        typedef struct Task {
            TASK_STATUS status;
            struct {
                struct Task*  tsk_up;       // first outer task alive (for upvalues)
                struct Task*  tsk_next;     // next Task in the same block (for broadcast)
                struct Block* blk_down;     // nested block inside me
                struct {                    // tsk/blk which called me (for throw/catch)
                    struct Task*  tsk;
                    struct Block* blk;
                } up;
            } links;
            int size;
            Task_F f; // (Stack* stack, Task* task, void* evt);
            int pc;
        } Task;
        
        typedef struct Block {
            Pool* pool;                     // allocations in this block
            int catch;                      // label to jump on catch
            struct {
                struct Task*  tsk_first;    // first Task inside me
                struct Task*  tsk_last;     // current last Task inside me
                struct Block* blk_down;     // nested Block inside me 
                struct Block* blk_up;       // block in which I am nested (for throw/catch)
            } links;
        } Block;
        
        typedef struct Tasks {              // task + block
            TASK_STATUS status;
            struct {
                Task*  tsk_up;              // for upvalues
                Task*  tsk_next;            // for broadcast
                Block* blk_down;
                struct {                    // for throw/catch
                    struct Task*  tsk;
                    struct Block* blk;
                } up;
            } links;
            Block block;
        } Tasks;

        ///
        
        void block_free (Block* block) {
            while (block->pool != NULL) {
                Pool* cur = block->pool;
                block->pool = cur->nxt;
                free(cur->val);
                free(cur);
            }
            block->pool = NULL;
        }
        
        void block_push (Block* block, void* val) {
            Pool* pool = malloc(sizeof(Pool));
            assert(pool!=NULL && "not enough memory");
            pool->val = val;
            pool->nxt = block->pool;
            block->pool = pool;
        }
        
        ///
        
        void task_link (Task* task, Task* up_tsk, Block* up_blk) {
            Task* last = up_blk->links.tsk_last;
            if (last == NULL) {
                assert(up_blk->links.tsk_first == NULL);
                up_blk->links.tsk_first = task;
            } else {
                last->links.tsk_next = task;
            }
            up_blk->links.tsk_last = task;
            task->links.tsk_next  = NULL;
            task->links.blk_down = NULL;
            task->links.up.tsk = up_tsk;
            task->links.up.blk = up_blk;
        }
        
        /// ONLY FOR DYNAMIC POOLS

        void __task_free (Block* block, Task* task) {
            Pool** tonxt = &block->pool;
            Pool*  cur   = block->pool;
            assert(cur != NULL);
            while (cur != NULL) {
                if (cur->val == task) {
                    *tonxt = cur->nxt; 
                    free(cur->val);
                    free(cur);
                    return;
                }
                tonxt = &cur->nxt;
                cur   = cur->nxt;
            }
            assert(0 && "bug found");
        }
        
        void pool_maybe_free (Task* pool) {
            assert(pool->status == TASK_POOL);
            Task* prv = NULL;
            Task* nxt = pool->links.blk_down->links.tsk_first;
            pool->links.blk_down->links.tsk_first = NULL;
            while (nxt != NULL) {
                Task* cur = nxt;
                nxt = cur->links.tsk_next;
                if (cur->status == TASK_DEAD) {
                    __task_free(pool->links.blk_down, cur);
                } else {
                    if (pool->links.blk_down->links.tsk_first == NULL) {
                        pool->links.blk_down->links.tsk_first = cur;    // first to survive
                    }
                    if (prv != NULL) {
                        prv->links.tsk_next = cur;                      // next to survive
                    }
                    cur->links.tsk_next = NULL;
                    prv = cur;
                }
            }
            pool->links.blk_down->links.tsk_last = prv;                 // last to survive
        }
        
        int pool_size (Task* pool) {
            int ret = 0;
            Task* nxt = pool->links.blk_down->links.tsk_first;
            while (nxt != NULL) {
                ret += 1;
                nxt = nxt->links.tsk_next;
            }            
            return ret;
        }
        
        ///

        void block_throw (Stack* top, void* err, Task* cur_tsk, Block* cur_blk) {
            //printf(">>> %p/%p\n", cur_tsk, cur_blk);
            if (cur_blk == NULL) {
                if (cur_tsk == NULL) {
                    assert(0 && "throw without catch");
                } else {
                    block_throw(top, err, cur_tsk->links.up.tsk, cur_tsk->links.up.blk);
                }
            } else {
                //printf("    > %d\n", cur_blk->catch);
                if (cur_blk->catch != 0) {
                    assert(cur_tsk!=NULL && "catch outside task");
                    Stack stk = { top, cur_tsk, cur_blk };
                    cur_tsk->pc = cur_blk->catch;
                    cur_tsk->f(&stk, cur_tsk, &err);
                    if (err == NULL) {  // err caught
                        return;
                    }
                }
                block_throw(top, err, cur_tsk, cur_blk->links.blk_up);
            }            
        }
        
        ///
        
        // 1. awake my inner tasks  (they started before the nested block)
        // 1.1. awake tasks in inner block in current task
        // 1.2. awake current task  (it is blocked after inner block. but before next task)
        // 1.3. awake next task
        // 2. awake my nested block (it started after the inner tasks)            

        void block_bcast_kill (Stack* stack, Block* block) {
            // X. clear stack from myself
            {
                Stack* stk = stack;
                while (stk != NULL) {
                    if (stk->block == block) {
                        stk->block = NULL;
                    }
                    stk = stk->stk_up;
                }
            }
            
            void aux (Task* task) {
                if (task == NULL) return;
                aux(task->links.tsk_next);                          // 1.3
                //assert(task->links.blk_down != NULL);
                if (task->links.blk_down != NULL) {
                    block_bcast_kill(stack, task->links.blk_down);      // 1.1
                }
                if (task->status == TASK_AWAITING) {
                    _CEU_Event evt = { .tag=_CEU_EVENT_KILL };            
                    task->f(stack, task, &evt);                     // 1.2
                }
            }
            
            if (block->links.blk_down != NULL) {                    // 2. awake my nested block
                block_bcast_kill(stack, block->links.blk_down);
            }                
            aux(block->links.tsk_first);                            // 1. awake my inner tasks            
            block_free(block);                                      // X. free myself
        }
        
        void bcast_event_task (Stack* stack, Task* task, _CEU_Event* evt, int gonxt);

        void bcast_event_block (Stack* stack, Block* block, _CEU_Event* evt) {
            
            Stack stk = { stack, stack->task, block };
            
            bcast_event_task(&stk, block->links.tsk_first, evt, 1); // 1. awake my inner tasks
            if (stk.block == NULL) return;  // I died in aux, cannot continue to nested block
            if (block->links.blk_down != NULL) {                    // 2. awake my nested block
                bcast_event_block(stack, block->links.blk_down, evt);
            }
        }

        void bcast_event_task (Stack* stack, Task* task, _CEU_Event* evt, int gonxt) {
            if (task == NULL) return;
            if (task->status == TASK_PAUSED) return;
            //assert(task->links.blk_down != NULL);

            if (task->links.blk_down != NULL) {
                // prevents nested pool tasks to free themselves while I'm currently traversing them
                Stack* orig = stack;
                Stack stk = { stack, task, NULL };
                if (task->status==TASK_POOL) stack = &stk;
                bcast_event_block(stack, task->links.blk_down, evt); // 1.1
                if (orig->block == NULL) return;  // outer block died, cannot continue to next task
                if (task->status==TASK_POOL) stack = orig;
            }
            if (task->status == TASK_POOL) {
                int ok = 1;
                Stack* cur = stack;
                while (cur != NULL) {
                    if (cur->task == task) {
                        ok = 0;
                        break;
                    }
                    cur = cur->stk_up; 
                }
                if (ok) {
                    pool_maybe_free(task);
                }
            } else if (task->status == TASK_AWAITING) {
                task->f(stack, task, evt);                       // 1.2
                if (stack->block == NULL) return;  // outer block died, cannot continue to next task
            }
            if (gonxt) {
                bcast_event_task(stack, task->links.tsk_next, evt, 1); // 1.3
            }
        }

        ///
        
        Block* GLOBAL;

        ${code.type}        
        ${code.struct}
        struct {
            ${this.mem_vars()}
        } global;
        ${code.func}

        void main (void) {
            Block B0 = { NULL, 0, {NULL,NULL,NULL} };
            GLOBAL = &B0;
            Stack _stack_ = { NULL, NULL, &B0 };
            Stack* stack = &_stack_;
            ${code.stmt}
            block_bcast_kill(stack, &B0);
        }

    """).trimIndent()
}
