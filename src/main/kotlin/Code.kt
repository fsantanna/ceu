data class Code (val type: String, val struct: String, val func: String, val stmt: String, val expr: String)
val CODE = ArrayDeque<Code>()
var Event = "_Event"

fun Any.self_or_null (): String {
    return if (this.ups_first { it is Expr.Func } == null) "NULL" else "(&task1->task0)"
}

fun Type.pos (): String {
    return when (this) {
        is Type.Alias   -> this.tk_.id
        is Type.Unit    -> "int"
        is Type.Pointer -> this.pln.pos() + "*"
        is Type.Nat     -> this.tk_.src.let { if (it == "") "int" else it }
        is Type.Tuple   -> "struct " + this.toce()
        is Type.Union   -> "struct " + this.toce()
        is Type.Func    -> "struct " + this.toce() + "*"
        is Type.Active   -> this.tsk.pos()
        is Type.Actives  -> "Tasks"
    }
}

fun Type.output_std (c: String, arg: String): String {
    val pln_from_ptr_to_tup_or_uni: Type? = this.noalias().let {
        if (it !is Type.Pointer) null else {
            it.pln.noalias().let {
                if (it is Type.Tuple || it is Type.Union) it else null
            }
        }
    }
    return when {
        (pln_from_ptr_to_tup_or_uni != null) ->"output_std_${pln_from_ptr_to_tup_or_uni.toce()}$c($arg);\n"
        this is Type.Pointer || this is Type.Func -> {
            if (c == "_") "putchar('_');\n" else "puts(\"_\");\n"
        }
        this is Type.Nat -> {
            val out = "output_std_${this.toce()}$c"
            """
                
                #ifdef $out
                    $out($arg);
                #else
                    assert(0 && "$out");
                #endif
                
            """.trimIndent()
        }
        else -> "output_std_${this.noalias().toce()}$c($arg);\n"
    }
}

val TYPEX = mutableSetOf<String>()

fun code_ft (tp: Type) {
    CODE.addFirst(when (tp) {
        is Type.Nat, is Type.Unit, is Type.Alias -> Code("","","","","")
        is Type.Pointer -> CODE.removeFirst()
        is Type.Active  -> CODE.removeFirst()
        is Type.Actives -> CODE.removeFirst()
        is Type.Func    -> {
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
                                it.map { "Block* ${it.scp1.id};\n" }.joinToString("") }
                            }
                        };
                    };
                    struct {
                        ${tp.inp.pos()} arg;
                        $Event evt;
                        ${tp.pub.let { if (it == null) "" else it.pos() + " pub;" }}
                        ${tp.out.pos()} ret;
                    };
                } ${tp.toce()};
                
                typedef union {
                    _Event* evt;
                    struct {
                        Block* blks[${tp.xscps.second!!.size}];
                        ${tp.inp.pos()} arg;
                    } pars;
                } X_${tp.toce()};

                typedef void (*F_${tp.toce()}) (Stack*, ${tp.toce()}*, X_${tp.toce()});
                
            """.trimIndent()

            Code(type+inp.type+pub.type+out.type, inp.struct+pub.struct+out.struct+struct, inp.func+pub.func+out.func, "","")
        }
        is Type.Tuple -> {
            val ce = tp.toce()

            val type = """
                // Type.Tuple.type
                struct $ce;
                void output_std_${ce}_ (${tp.pos()}* v);
                void output_std_${ce}  (${tp.pos()}* v);
                
            """.trimIndent()

            val struct = """
                // Type.Tuple.struct
                struct $ce {
                    ${tp.vec  // do not filter to keep correct i
                        .mapIndexed { i,sub -> (sub.pos() + " _" + (i+1).toString() + ";\n") }
                        .joinToString("")
                    }
                };
                void output_std_${ce}_ (${tp.pos()}* v) {
                    printf("[");
                    ${tp.vec
                        .mapIndexed { i,sub ->
                            val s = when (sub.noalias()) {
                                is Type.Union, is Type.Tuple -> "&v->_${i + 1}"
                                else -> "v->_${i + 1}"
                            }
                            sub.output_std("_", s)
                        }
                        .joinToString("putchar(',');\n")
                    }
                    printf("]");
                }
                void output_std_${ce} (${tp.pos()}* v) {
                    output_std_${ce}_(v);
                    puts("");
                }

            """.trimIndent()

            val codes = tp.vec.map { CODE.removeFirst() }.reversed()
            val types = codes.map { it.type }.joinToString("")
            val structs  = codes.map { it.struct  }.joinToString("")
            Code(types+type,structs+struct, "", "", "")
        }
        is Type.Union -> {
            val ce = tp.toce()

            val type = """
                // Type.Union.type
                struct $ce;
                void output_std_${ce}_ (${tp.pos()}* v);
                void output_std_${ce} (${tp.pos()}* v);

            """.trimIndent()

            val struct = """
                // Type.Union.struct
                struct $ce {
                    int tag;
                    union {
                        ${tp.vec  // do not filter to keep correct i
                            .mapIndexed { i,sub -> "${sub.pos()} _${i+1};\n" }
                            .joinToString("")
                        }
                    };
                };
                void output_std_${ce}_ (${tp.pos()}* v) {
                    // TODO: only if tp.isrec
                    if (v == NULL) {
                        printf("<.0>");
                        return;
                    }
                    printf("<.%d", v->tag);
                    switch (v->tag) {
                        ${tp.vec
                            .mapIndexed { i,sub ->
                                val s = when (sub) {
                                    is Type.Unit -> ""
                                    is Type.Union, is Type.Tuple -> "putchar(' ');\n" + sub.output_std("_", "&v->_${i+1}")
                                    else -> "putchar(' ');\n" + sub.output_std("_", "v->_${i+1}")
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
                void output_std_${ce} (${tp.pos()}* v) {
                    output_std_${ce}_(v);
                    puts("");
                }

            """.trimIndent()

            val codes = tp.vec.map { CODE.removeFirst() }.reversed()
            val types = codes.map { it.type }.joinToString("")
            val structs  = codes.map { it.struct  }.joinToString("")
            Code(types+type,structs+struct, "", "", "")
        }
    }.let {
        val ce = tp.toce()
        if (TYPEX.contains(ce)) {
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
        (this in listOf("arg","pub","evt","ret","state")) -> "(task1->$this)"
        else -> "(task2->$this)"
    }
}

fun String.out_mem (up: Any): String {
    val env = up.env(this)!!
    val str = if (env is Stmt.Block) "B${env.n}" else this

    val upf = env.ups_first(true){ it is Expr.Func }
    if (upf == null) {
        return "(global.$str)"
    }

    val ispar = this in listOf("arg","pub","evt","ret","state")
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
    val id = this.scp1.id
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
        is Stmt.Input, is Stmt.Output, is Stmt.Pause, is Stmt.Return, is Stmt.Break,
        is Stmt.Typedef -> ""

        is Stmt.Var -> "${this.xtype!!.pos()} ${this.tk_.id};\n"
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
        is Expr.Nat   -> CODE.removeFirst().let { Code(it.type, it.struct, it.func, it.stmt, e.tk_.src.native(e, e.tk)) }
        is Expr.Var   -> Code("", "", "", "", e.tk_.id.out_mem(e))
        is Expr.Upref -> CODE.removeFirst().let { Code(it.type, it.struct, it.func, it.stmt, "(&" + it.expr + ")") }
        is Expr.Dnref -> CODE.removeFirst().let { Code(it.type, it.struct, it.func, it.stmt, "(*" + it.expr + ")") }
        is Expr.TDisc -> CODE.removeFirst().let { Code(it.type, it.struct, it.func, it.stmt, it.expr + "._" + e.tk.tostr()) }
        is Expr.Pak   -> {
            val tp = if (e.xtype==null) Code("","","","","") else CODE.removeFirst()
            val e  = CODE.removeFirst()
            Code(tp.type+e.type, tp.struct+e.struct, tp.func+e.func, tp.stmt+e.stmt, tp.expr+e.expr)
        }
        is Expr.Unpak -> {
            val e = CODE.removeFirst()
            Code(e.type, e.struct, e.func, e.stmt, e.expr)
        }
        is Expr.Field -> CODE.removeFirst().let {
            val src = when (e.tk_.id) {
                "state" -> it.expr + "->task0.state"
                "pub"   -> it.expr + "->pub"
                "ret"   -> it.expr + "->ret"
                else    -> error("bug found")
            }
            Code(it.type, it.struct, it.func, it.stmt, src)
        }
        is Expr.UDisc -> CODE.removeFirst().let {
            val ee = it.expr
            val num = e.tk.field2num((e.uni.wtype!!.noalias() as Type.Union).yids)!!
            val pre = if (e.tk.isnull()) {
                """
                assert(&${it.expr} == NULL);

                """.trimIndent()
            } else {
                """
                assert(&${it.expr} != NULL);    // TODO: only if e.uni.wtype!!.isrec()
                assert($ee.tag == $num);

                """.trimIndent()
            }
            Code(it.type, it.struct, it.func, it.stmt+pre, ee+"._"+num)
        }
        is Expr.UPred -> CODE.removeFirst().let {
            val ee = it.expr
            val num = e.tk.field2num((e.uni.wtype!!.noalias() as Type.Union).yids)!!
            val pos = if (e.tk.isnull()) {
                "(&${it.expr} == NULL)"
            } else { // TODO: only if e.uni.wtype!!.isrec()
                "(&${it.expr} != NULL) && ($ee.tag == $num)"
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
            val pre = "$pos $ID = (($pos) { $num , ._$num = ${arg.expr} });\n"
            Code(tp.type+arg.type, tp.struct+arg.struct, tp.func+arg.func, arg.stmt + pre, ID)
        }
        is Expr.UNull -> CODE.removeFirst().let { Code(it.type, it.struct, it.func,"","NULL") }
        is Expr.Call  -> {
            val arg  = CODE.removeFirst()
            val f    = CODE.removeFirst()
            val blks = e.xscps.first!!.map { it.toce(e) }.joinToString(",")
            val tpf  = e.f.wtype!!.noact()
            val upspawn = e.upspawn()
            when {
                (e.f is Expr.Var && e.f.tk_.id=="output_std") -> {
                    Code (
                        f.type + arg.type,
                        f.struct + arg.struct,
                        f.func + arg.func,
                        f.stmt + arg.stmt + e.arg.wtype!!.output_std("", arg.expr),
                        ""
                    )
                }
                (tpf is Type.Func) -> {
                    val block = upspawn.let {
                        if (it is Stmt.DSpawn) {
                            "&" + (it.dst as Expr.Var).tk_.id.out_mem(e) + ".block"
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

                    val pre = """
                        $ret1
                        {
                            ${if (upspawn !is Stmt.DSpawn) "" else {
                                val dst = upspawn.dst as Expr.Var
                                val len = ((dst.env(dst.tk_.id) as Stmt.Var).xtype as Type.Actives).len?.num ?: 0
                                val mem = dst.tk_.id.out_mem(e)
                                "if ($len==0 || pool_size((Task*)&$mem)<$len) {"
                            }}
                            Stack stk_${e.n} = { stack, ${e.self_or_null()}, ${e.localBlockMem()} };
                            ${tpf.toce()}* frame = (${tpf.toce()}*) malloc(${f.expr}->task0.size);
                            assert(frame!=NULL && "not enough memory");
                            memcpy(frame, ${f.expr}, ${f.expr}->task0.size);
                            //${if (upspawn is Stmt.DSpawn) "frame->task0.isauto = 1;" else ""}
                            block_push($block, frame);
                            frame->task0.links.tsk_up = ${if (e.ups_first { it is Expr.Func }==null) "NULL" else "task0"};
                            task_link($block, &frame->task0);
                            frame->task0.state = TASK_UNBORN;
                            ((F_${tpf.toce()})(frame->task0.f)) (
                                &stk_${e.n},
                                frame,
                                (X_${tpf.toce()}) {.pars={{$blks}, ${arg.expr}}}
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
                    Task*             task0 = &task2->task1.task0;
                    ${e.xtype!!.toce()}* task1 = &task2->task1;
                    ${e.xtype!!.xscps.second!!.mapIndexed { i, _ -> "task1->blks[$i] = xxx.pars.blks[$i];\n" }.joinToString("")}
                    assert(task0->state==TASK_UNBORN || task0->state==TASK_AWAITING);
                    switch (task0->pc) {
                        case 0: {                    
                            assert(task0->state == TASK_UNBORN);
                            task2->task1.arg = xxx.pars.arg;
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

            val src = """
                static Func_${e.n} _frame_${e.n};
                _frame_${e.n}.task1.task0 = (Task) {
                    TASK_UNBORN, {}, sizeof(Func_${e.n}), (Task_F)func_${e.n}, 0
                };
                static Func_${e.n}* frame_${e.n} = &_frame_${e.n};
    
            """.trimIndent()

            Code(tp.type+type+block.type, tp.struct+block.struct+struct, tp.func+block.func+func, src, "((${e.xtype!!.pos()}) frame_${e.n})")
        }
    }.let {
        val line = if (!LINES) "" else "\n#line ${e.tk.lin} \"CEU\"\n"
        Code(line+it.type, line+it.struct, line+it.func, line+it.stmt, line+it.expr)
    })
}

fun code_fs (s: Stmt) {
    CODE.addFirst(when (s) {
        is Stmt.Nop -> Code("", "","","", "")
        is Stmt.Typedef -> CODE.removeFirst().let {
            if (s.tk_.id == "Event") {
                Event = "Event"
            }
            val src = """
                //#define output_std_${s.tk_.id}_ output_std_${s.type.toce()}_
                //#define output_std_${s.tk_.id}  output_std_${s.type.toce()}
                typedef ${s.type.pos()} ${s.tk_.id};
                
            """.trimIndent()
            Code(src+it.type, it.struct, it.func, "", "")
        }
        is Stmt.Native -> if (s.istype) {
            Code("", s.tk_.src.native(s, s.tk) + "\n", "", "", "")
        } else {
            Code("", "", "", s.tk_.src.native(s, s.tk) + "\n", "")
        }
        is Stmt.Seq -> {
            val s2 = CODE.removeFirst()
            val s1 = CODE.removeFirst()
            Code(s1.type+s2.type, s1.struct+s2.struct, s1.func+s2.func, s1.stmt+s2.stmt, "")
        }
        is Stmt.Var -> CODE.removeFirst().let {
            val src = if (s.xtype is Type.Actives) {
                s.tk_.id.loc_mem(s).let {
                    """
                        $it = (Tasks) { TASK_POOL, { NULL, NULL }, { NULL, 0, { NULL, NULL, NULL } } };
                        task_link(${s.localBlockMem()}, (Task*) &$it);
                        $it.links.blk_down = &$it.block;
                        
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
            Code(it.type, it.struct, it.func, "while (1) { ${it.stmt} }", "")
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
                    while (${i.expr}!=NULL && ${i.expr}->task0.state!=TASK_DEAD) {
                        ${block.stmt}
                        ${i.expr} = (${s.i.wtype!!.pos()}) ${i.expr}->task0.links.tsk_next;
                    }
                    stack = stk.stk_up;
                }
                
            """.trimIndent()
            Code(tsks.type+i.type+block.type, tsks.struct+i.struct+block.struct, tsks.func+i.func+block.func, tsks.stmt+i.stmt+src, "")
        }
        is Stmt.Break -> {
            val n = ((s.ups_first { it is Stmt.Loop } as Stmt.Loop).wup as Stmt.Block).n
            Code("", "", "", "goto _BLOCK_${n}_;\n", "")
        }
        is Stmt.Return -> {
            val n = (s.ups_first { it is Expr.Func } as Expr.Func).block.n
            Code("", "", "", "goto _BLOCK_${n}_;\n", "")
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
                //assert(${it.expr}->task0.state==TASK_AWAITING && "trying to pause non-awaiting task");
                ${it.expr}->task0.state = TASK_PAUSED;
                
                """.trimIndent()
            } else {
                """
                ${it.expr}->task0.state = TASK_AWAITING;
                
                """.trimIndent()

            }
            Code(it.type, it.struct, it.func, it.stmt+src, "")
        }
        is Stmt.Await -> CODE.removeFirst().let {
            val cnd = if (s.e.wtype is Type.Nat) {
                "${it.expr}"
            } else {
                "(task1->evt.tag == 2) && (((_Event*)(&task1->evt))->payload.Task == ((uint64_t)(${it.expr})))"
            }

            val src = """
                {
                    task0->pc = ${s.n};      // next awake
                    task0->state = TASK_AWAITING;
                    return;                 // await (1 = awake ok)
                case ${s.n}:                // awake here
                    assert(task0->state == TASK_AWAITING);
                    task1->evt = * (Event*) xxx.evt;
                    if (!($cnd)) {
                        return;             // (0 = awake no)
                    }
                    task0->state = TASK_RUNNING;
                }
                
            """.trimIndent()
            Code(it.type, it.struct, it.func, it.stmt+src, "")
        }
        is Stmt.Emit -> {
            val evt = CODE.removeFirst()
            if (s.tgt is Scope) {
                val src = """
                {
                    Stack stk = { stack, ${s.self_or_null()}, ${s.localBlockMem()} };
                    bcast_event_block(&stk, ${s.tgt.toce(s)}, (_Event*) &${evt.expr});
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
                    bcast_event_task(&stk, &${tsk.expr}->task0, (_Event*) &${evt.expr}, 0);
                    if (stk.block == NULL) {
                        return;
                    }
                }
                
                """.trimIndent()
                Code(tsk.type+evt.type, tsk.struct+evt.struct, tsk.func+evt.func, tsk.stmt+evt.stmt+src, "")

            }
        }
        is Stmt.Throw -> {
            val src = """
                {
                    Stack stk = { stack, ${s.self_or_null()}, ${s.localBlockMem()} };
                    block_throw(&stk, &stk);
                    assert(stk.block == NULL);
                    if (stk.block == NULL) {
                        return;
                    }
                }
                
            """.trimIndent()
            Code("", "", "", src, "")
        }
        is Stmt.Input -> {
            val arg = CODE.removeFirst()
            if (s.dst == null) {
                val tp  = CODE.removeFirst()
                val src = "input_${s.lib.id}_${s.xtype!!.toce()}(${arg.expr});\n"
                Code(tp.type+arg.type, tp.struct+arg.struct, tp.func+arg.func, arg.stmt + src, "")
            } else {
                val dst = CODE.removeFirst()
                val tp  = CODE.removeFirst()
                val src = "${dst.expr} = input_${s.lib.id}_${s.xtype!!.toce()}(${arg.expr});\n"
                Code(tp.type+arg.type+dst.type, tp.struct+arg.struct+dst.struct, tp.func+arg.func+dst.func, arg.stmt+dst.stmt+src, "")
            }
        }
        is Stmt.Output -> CODE.removeFirst().let {
            val call = if (s.lib.id == "std") {
                s.arg.wtype!!.output_std("", it.expr)
            } else {
                "output_${s.lib.id}(${it.expr});\n"
            }
            Code(it.type, it.struct, it.func, it.stmt+call, "")
        }
        is Stmt.Block -> CODE.removeFirst().let {
            val up = s.ups_first { it is Expr.Func || it is Stmt.Block }
            val blk = "B${s.n}".loc_mem(s)

            val src = """
            {
                $blk = (Block) { NULL, ${if (s.iscatch) s.n else 0}, {NULL,NULL,NULL} };
                
                // link
                ${if (up is Stmt.Block) {
                    "${s.localBlockMem()}->links.blk_down = &$blk;"
                } else if (up is Expr.Func) {
                    "task0->links.blk_down = &$blk;"
                } else {
                    ""
                }}
                
                ${it.stmt}
                
                // CLEANUP
                
                ${if (!s.iscatch) "" else "case ${s.n}: // catch"}
                
            _BLOCK_${s.n}_:
            
                // task event
                ${if (up !is Expr.Func || up.tk.enu==TK.FUNC) "" else """
                {
                    //task0->state = TASK_DYING;
                    _Event evt = { EVENT_TASK, {.Task=(uint64_t)task0} };
                    Stack stk = { stack, task0, &$blk };
                    bcast_event_block(&stk, GLOBAL, (_Event*) &evt);
                    if (stk.block == NULL) {
                        //task0->state = TASK_DEAD;
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
                    $blk.links.tsk_first = NULL;
                    //$blk.links.tsk_last  = NULL;
                    $blk.links.blk_down  = NULL;
                    """.trimIndent()
                } else if (up is Expr.Func) {
                    """
                    //task0->links.tsk_up   = NULL;
                    //task0->links.tsk_next = NULL;
                    task0->links.blk_down = NULL;
                    task0->state = TASK_DEAD;                        
                    """.trimIndent()
                } else {
                    ""
                }}
            }
            
            """.trimIndent()

            Code(it.type, it.struct, it.func, src, "")
        }
    }.let {
        val line = if (!LINES) "" else "\n#line ${s.tk.lin} \"CEU\"\n"
        assert(it.expr == "")
        Code(line+it.type, line+it.struct, line+it.func, line+it.stmt, "")
    })
}

fun Stmt.code (): String {
    Event = "_Event"
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
        
        #define input_std_int(x)     ({ int _x ; scanf("%d",&_x) ; _x ; })
        #define output_std_Unit_(x)  printf("()")
        #define output_std_Unit(x)   (output_std_Unit_(x), puts(""))
        #define output_std_int_(x)   printf("%d",x)
        #define output_std_int(x)    (output_std_int_(x), puts(""))
        #define output_std_char__(x) printf("\"%s\"",x)
        #define output_std_char_(x)  (output_std_char__(x), puts(""))
        #define output_std_Ptr_(x)   printf("%p",x)
        #define output_std_Ptr(x)    (output_std_Ptr_(x), puts(""))
        
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
        } TASK_STATE;
        
        typedef enum {
            EVENT_KILL=1, EVENT_TASK //, ...
        } EVENT;
        
        typedef struct _Event {
            int tag;
            union {
                //void Kill;
                uint64_t Task;  // cast from Task*
            } payload;
        } _Event;
        
        // stack, task, evt
        typedef void (*Task_F) (Stack*, struct Task*, _Event*);
        
        typedef struct Task {
            TASK_STATE state;
            struct {
                struct Task*  tsk_up;       // first outer task alive (for upvalues)
                struct Task*  tsk_next;     // next Task in the same block (for broadcast)
                struct Block* blk_down;     // nested block inside me
            } links;
            int size;
            Task_F f; // (Stack* stack, Task* task, _Event* evt);
            int pc;
        } Task;
        
        typedef struct Block {
            Pool* pool;                     // allocations in this block
            int catch;                      // label to jump on catch
            struct {
                struct Task*  tsk_first;    // first Task inside me
                struct Task*  tsk_last;     // current last Task inside me
                struct Block* blk_down;     // nested Block inside me 
            } links;
        } Block;
        
        typedef struct Tasks {              // task + block
            TASK_STATE state;
            struct {
                Task*  tsk_up;              // for upvalues
                Task*  tsk_next;            // for broadcast
                Block* blk_down;
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
        
        void task_link (Block* block, Task* task) {
            Task* last = block->links.tsk_last;
            if (last == NULL) {
                assert(block->links.tsk_first == NULL);
                block->links.tsk_first = task;
            } else {
                last->links.tsk_next = task;
            }
            block->links.tsk_last = task;
            task->links.tsk_next  = NULL;
            task->links.blk_down = NULL;
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
            assert(pool->state == TASK_POOL);
            Task* prv = NULL;
            Task* nxt = pool->links.blk_down->links.tsk_first;
            pool->links.blk_down->links.tsk_first = NULL;
            while (nxt != NULL) {
                Task* cur = nxt;
                nxt = cur->links.tsk_next;
                if (cur->state == TASK_DEAD) {
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

        void block_throw (Stack* top, Stack* cur) {
            if (cur == NULL) {
                assert(0 && "throw without catch");
            } if (cur->block->catch == 0) {
                block_throw(top, cur->stk_up);
            } else {
                assert(cur->task!=NULL && "catch outside task");
                Stack stk = { top, cur->task, cur->block };
                cur->task->pc = cur->block->catch;
                cur->task->f(&stk, cur->task, NULL);
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
                if (task->state == TASK_AWAITING) {
                    _Event evt = { EVENT_KILL };            
                    task->f(stack, task, &evt);                     // 1.2
                }
            }
            
            if (block->links.blk_down != NULL) {                    // 2. awake my nested block
                block_bcast_kill(stack, block->links.blk_down);
            }                
            aux(block->links.tsk_first);                            // 1. awake my inner tasks            
            block_free(block);                                      // X. free myself
        }
        
        void bcast_event_task (Stack* stack, Task* task, _Event* evt, int gonxt);

        void bcast_event_block (Stack* stack, Block* block, _Event* evt) {
            
            Stack stk = { stack, stack->task, block };
            
            bcast_event_task(&stk, block->links.tsk_first, evt, 1); // 1. awake my inner tasks
            if (stk.block == NULL) return;  // I died in aux, cannot continue to nested block
            if (block->links.blk_down != NULL) {                    // 2. awake my nested block
                bcast_event_block(stack, block->links.blk_down, evt);
            }
        }

        void bcast_event_task (Stack* stack, Task* task, _Event* evt, int gonxt) {
            if (task == NULL) return;
            if (task->state == TASK_PAUSED) return;
            //assert(task->links.blk_down != NULL);

            if (task->links.blk_down != NULL) {
                // prevents nested pool tasks to free themselves while I'm currently traversing them
                Stack* orig = stack;
                Stack stk = { stack, task, NULL };
                if (task->state==TASK_POOL) stack = &stk;
                bcast_event_block(stack, task->links.blk_down, evt); // 1.1
                if (orig->block == NULL) return;  // outer block died, cannot continue to next task
                if (task->state==TASK_POOL) stack = orig;
            }
            if (task->state == TASK_POOL) {
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
            } else if (task->state == TASK_AWAITING) {
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
