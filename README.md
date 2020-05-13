# pa65
Pseudo Static Heap for ca65

## Quick Start
Declare all sub-routines as functions using the directives `.func`/`.endfunc`:
```s
.func foo
rts
.endfunc

.func bar
rts
.endfunc
```
Allocate some variables using the directive `.palloc`:
```s
.func foo
lda #0
sta bar::param0
jsr bar
.palloc .bss, local_var, 1
lda #1
sta local_var
rts
.endfunc

.func bar
.palloc .zeropage, param0, 1
ldx param0
inx
rts
.enfunc
```
