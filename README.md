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
      java -jar pa65-xy.jar -o pa65.inc file1.s file2.s ... fileN.s
      ```
1. Just assembly/link your project as you've used to do

## Directives
### func(name)
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
### palloc(seg,var,size)
Allocate some bytes of a variable into a segment.
#### Parameters
* **seg** - The name of a segment to allocate into
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
```
