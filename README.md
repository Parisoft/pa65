# pa65
Pseudo Static Heap for ca65<br>
or<br>
Parisoft Allocator for ca65<br>

## Quick Start
1. Download the latest version at https://github.com/Parisoft/pa65/releases.
1. Declare all functions and variables using [pa65 directives](#directives).
1. Choose a file name like `pa65.inc` and include it in all your source files:
   1. ```assembly_x86
      .include "pa65.inc"
      ```
1. Call pa65 with your source files to generate the `.inc` file above:
   1. ```bash
      java -jar pa65-xy.jar -o pa65.inc file1.s file2.s ... fileN.s
      ```
1. Just assembly/link your project as you've used to do

## Directives
