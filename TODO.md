- Lines=4879, LoC=2945 (2022-03-10)
- emit EVENT_TASK to correct scope (not GLOBAL)
  - can infer from func signature
- Main.kt: EXPR_WTYPE
- bugs
  - Expr.pre stmt
    - Call, Func, UCons, New, UDisc, New
    - they all may have stmt called before actual expr
  - Type.Alias.supOf (check scopes)
  - :: check isSupOf
  - atcive currently w/o check scopes (they should be trated as pointers)
  - access to Dyn out of DLoop should require Watching
  - check if recursive type is plain union (not pointer to it)
- List @[...].1 doesnt work
- command to kill task
- check pause/resume types
- abstract types -> cannot be in UCons
  - Event, Error
  - otherwise, problem with ?
- ce1:
  - func f <-- var f = func
    - () -> [() ->] ()
  - if/until condition
    - _int -> Bool:<(),()>
  - loop 1..5
  - defer
  - var [x,y] = arg   (arg is tuple)
  - option, subtyping, generics, typeclass
  - return -> escape?
    - cross task
    - SetBlock
      - var x: () = { ... escape () }
  - collections:
    - tuples
      x = [1,2,3]
      x.1
    - lists
      xs = *[1,2,3]
      xs?Null, xs!Cons
      a * xs
    - vector
      x = #[1,2,3,...]
      x?1, x!1
      x?[v], x![v]
    - dicts
      x = %[x=1,y=2,...]
      x?x, x!x
      x?[v], x![v]
- isSupOf for Nat, make it false against others
- XLexer para ce1
  - only in ce0: :-/:+
  - only in ce1: 1s
- Never type
- pico-ce
  - image scale (birds)
  - zoom x2+1, /2+1
- change @[@a1,@a2] -> @[a,b: a>b]
  - check constraint in func comparison
- accept @A vs var a
- var z = spawn h ()
  - optional in ce0?
  - optional in ce1?
- optimizations
  - use C stack for normal func
  - static alloc for clo/task that do not escape
    - should them even have a @clo annotation?
- catch N, throw N (N = argument)
- explicit `free`
- test block pass w/o data (e.g., internal `new` call)
- reserve `arg`,`pub`,`evt`,`ret`
- DLoop:
  - cannot await inside dloop, would loose the stack with TASK_POOL at top
  - is it possible to create new state=TASK_POOL_LOOP
    and not free while in it?
  1. reject await/emit/etc inside DLoop
  2. runtime error when self kill
  3. escape loop on error (but, multi-level escape)
  - if DLoop has await, it needs to recreate the i stack
  - throw inside DLoop
    - loop tk in @block { stack tk }
    - break if NULL?
  4. keep freelist cleared on finish
    {
      defer{free()}
      loop { ... }
    }
- output
  - std ? --> toString ?
    - output std toString /x
  - should print alias?
    - List <...>

# Subtyping
    - structural:   [(),()] <: [()]
    - nominal:      Player.Warrior <: Warrior
    - both:         T [(),()] <:  [()]
      T [(),()] xx S[()]  -- structural but not nominal

```
type Bool = <(),()>
```

```
type Bool = <   // Q. use `=´ or `:´ as below? R. `=´ is symmetric to `type`
  False = (),
  True  = ()
>
val b1 = Bool True

var b1:<(),()>            = <.1>:<(),()>
var b1:<False=(),True=()> = <.False>:<False=(),True=()>
var b1:<False=(),True=()> = False:<False=(),True=()>

var b1: Bool = False:<False=(),True=()> :+ Bool
var b1: Bool = Bool.False:<False=(),True=()>

```

```
type Player = [
    name: _(char*),
    age:  _int,
    sub:  <
        Warrior = [mana:_int, guild:_(char*)],
        Mage = [...],
    >
]
var x = Player.Warrior [name="Arthur", age=32, [10,"arcane"]]
var x: Player = [name="Arthur", age=32, sub=Warrior[10,"arcane"]]

type Player = [...] + <
    Warrior = [mana:_int, guild:_(char*)],
    Mage    = [...] + <
      Dark = ...
      Arcane = ...
    >
>

var m = Player <...>
var m = 

var m = Player.Warrior <.Arcane ...>
var m = Player [] + Mage [] + Dark []

```

```
type Event = <
    UKill    = (),
    UTask    = _uint64_t,
    UAny     = (),
    UTimeout = (),
    UFrame   = _int,
    UDraw    = (),
    UKeyDown = _int,
    UMouse   = Point + <
        Button = _int + <
          Up   = (),
          Down = ()
        >,
        Motion = ()
    >,
    UCollided        = (),
    UPause           = _int,
    UHide            = _int
>

var e = Event.Mouse.Button.Down [ [x,y], _10 ]
```

```
type List [a] @[s] = <[a, /(List [a] @[s]) @s]>
var str: List [_char] = new <.1 ['a', Null]>
var str: *[_char] = *['a']
var str: String = *"a"

type Either [a,b] = <Left=a, Right=b>
var xy: Either [Int,Int] = Either.Left _10

type Maybe a = <None, Some=a>
var x = Some a
x?? -> x?Some
x!! -> x!Some
var x: _int?

@[a1,a2: a2>a1] -> [/Num@[a1],/Num@a2]

```


# Tasks

- either assignable or anonymous
    - assignable `var`: remains in scope memory even after termination
    - anonymous `pool`: reclaimed on termination
- state:
    - unborn:   expecting spawn & arguments
    - running:  running up to await
    - awaiting: reached await
    - paused:   from tk.pause, do not resume
    - dead:     finished execution, no resume, holds return
- broadcast
    - all & first
    - up & down
    - receives scope
    - resumes task hierarchy, skips paused
    - passes event value

```
var  tk:  task () -> ()     -- assignable
pool tks: task () -> ()     -- anonymous

set tk = task () -> () {
    ...
    e = await e / ()
    ...
}
spawn  tk (...) 
kill   tk
status tk
pause  tk
awake [tk,e]
broadcast up tk e
broadcast down scp e
await _int  // condition using evt

task f {
    defer {
        ...
    }
    ...
    spawn h ()
    ...
    catch cnd {
        defer {
            ...
        }
        ...
        spawn g()       <--- 1. g awakes from emit and throws cond
        await ...
    }
                        <--- 2. catch/defer awakes
                        <--- 3. defer awakes
}
```

finalize/every/pool

par/or do
    ...
with
    ...
end

spawn tk1
spawn tk2
await_all [tk1,tk2]
await_any [tk1,tk2]

