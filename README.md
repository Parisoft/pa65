# pa65
Pseudo Static Heap for ca65<br>
or<br>
Parisoft Allocator for ca65<br>

## Quick Start
1. Download the latest version at [releases](https://github.com/Parisoft/pa65/releases) page.
1. Declare all functions and variables using [pa65 directives](#directives).
1. Choose a file name like `pa65.inc` and include it in all your source files:
   - ```assembly
      .include "pa65.inc"
      ```
1. Call pa65 with your source files to generate the `.inc` file above:
   - ```bash
      java -jar pa65-xy.jar -o pa65.inc file1.s ... fileN.s
      ```
1. Just assembly/link your project as you've used to do

## Directives

### .func(name)
Declare a function with the given name.<br>
The function must terminate with `.endfunc`.
#### Parameters
* **name** - The name of the function
#### Example
```s
.func foo
rts
.endfunc
```
___

### .palloc(seg,var,size)
Allocate some bytes of a variable into a segment.
#### Parameters
* **seg** - The name of a segment to allocate into. The directives `.zeropage` and `.bss` can also be used. 
* **var** - The name of the variable
* **size** - The number of bytes to allocate
#### Example
```s
.func foo
.palloc .zeropage, acme, 1 ; allocate 1 byte in zp for acme
; some code here
rts
.endfunc

.palloc bar
; call foo passing the value 1 to the acme parameter
lda #1
sta foo::acme
jmp foo
.endfunc
```
___

### .pfree(var1 ... varN)
Free the memory space allocated by some variables.
#### Parameters
* **var1...varN** - The name of the variable(s) to free
#### Example
```s
.func foo
.palloc "SEG1", tmp1, 1
.palloc "SEG2", tmp2, 1
; some code here
.pfree tmp1, tmp2 ; free the space allocated by tmp1 and tmp2
.palloc "SEG1", acme, 1 ; reuse the space freed by tmp1
; some code here
rts
.endfunc
```
___

### .pref(v1,v2)
Create a reference of a variable defined on another function.<br>
Useful to preset the parameters of a function.
#### Parameters
* **v1** - The name of the referer variable
* **v2** - The name of the referee variable
#### Example
```s
.func foo
.palloc .zeropage, acme, 1
; some code here
.enfunc

.func bar
.pref acme, foo::acme ; make a reference of foo.acme
jsr foo ; just call foo as the parameter is alread set by the callee (baz)
; some code here
.endfunc

.func baz
; call bar passing 1 to the acme parameter
lda #1
sta bar::acme
jmp bar
.endfunc
```
___

### .ftable(name,funcs)
Declare a table of functions. See [jtx](###jtx) and [###jty](jty).
#### Parameters
* **name** - The name of the table
* **funcs** - Array of functions
#### Example
```s
.linecont+

.func foo
; some code here
.endfunc

.func bar
; some code here
.endfunc

.ftable choose_foo_or_bar, {\
   foo-1,\
   bar-1\
}
```
___

### jtx _table_
Jump to a function referenced by a `.ftable` at index `X` using RTS trick.
#### Parameters
* **table** - The name of function table declared as `.ftable`
#### Example
```s
.linecont+

.func switch
.palloc .zeropage, idx, 1
ldx idx
jtx choose_foo_or_bar ; jump to foo if idx is 0; jump to bar if idx is 1
.endfunc

.func foo
; some code here
.endfunc

.func bar
; some code here
.endfunc

.ftable choose_foo_or_bar, {\
   foo-1,\
   bar-1\
}
```
___

### jty
Jump to a function referenced by a `.ftable` at index `Y` using RTS trick.
#### Parameters
* **table** - The name of function table declared as `.ftable`
#### Example
```s
.linecont+

.func switch
.palloc .zeropage, idx, 1
ldy idx
jty choose_foo_or_bar ; jump to foo if idx is 0; jump to bar if idx is 1
.endfunc

.func foo
; some code here
.endfunc

.func bar
; some code here
.endfunc

.ftable choose_foo_or_bar, {\
   foo-1,\
   bar-1\
}
```
___
