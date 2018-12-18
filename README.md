# PowerFunctions

PowerFunctions is a scripting language designed to compile to Minecraft's `.mcfunction` file format.


## Syntax

### Commands
The most essential parts of power functions are commands. Commands are preceded with a forward slash (`/`).

#### Example:
```asm
/say Hello World!
/tp Player 100 50 100
```

### Comments
PowerFunctions only supports single-, full-line comments in assembler-typical syntax. A comment starts with `;`.

#### Example:
```asm
; this is a comment
/say this is not a comment
```

### Statements
A statement consists of a left-hand side and a right-hand side with an operator in between. This syntax will be expanded
on in the future.

```
<target_entity> [target_objective] <operator> <source_entity> [source_objective]
```
If no objective was specified, the default objective `_int32` of type `dummy` will be used instead.

#### Operators
* `=` Assignment
* `+=` Addition
* `-=` Subtraction
* `*=` Multiplication
* `/=` Division
* `%=` Modulo Division
* `<` Minimum
* `>` Maximum
* `><` Swap
* `&=` Unboolified AND
* `&&=` Boolified AND
* `|=` Unboolified OR
* `||=` Boolified OR
* `^=` Unboolified XOR
* `^^=` Boolified XOR
* `===` Boolified Equivalence / XNOR
* `<<=` Arithmetic Left-Shift (equivalent to multiplication with a power of 2)
* `>>=` Arithmetic Right-Shift (equivalent to multiplication with a power of 2)

#### Example
```asm
; add 50 to x
#x += 50
; subtract 50 from y
#y -= 50
; swap x, y
#x >< #y
```

#### Variable notes
Variable names are compiled toward player names on the scoreboard.
Minecraft uses the `#` prefix for scoreboard entries which don't correspond to a real player.
Also these names will not be displayed in display slots such as the sidebar.
They should **always** be used in stead of visible variable names.

#### Operator Implementation Notes and Practices
All operators until (and including) `><` are *native*. This means that they compile to a single command:
```asm
/scoreboard players operation <entity> <objective> <operator> <entity> <objective>
```
Other operators may or may not be supported directly by a Minecraft command.

If you're surprised what *boolified* refers to: *boolified* values are either `0` for `false` or `1` for `true`.
Generally, PowerFunctions assumes that inputs are not boolified and compiles operators accordingly.

Consider the following example:
```asm
; equivalent to saying 'result = first_condition AND second_condition AND third_condition
#result = #first_condition
#result &&= #second_condition
#result &&= #third_condition
; do something with the result ...
```
Depending on the context, we might not need to use the boolified AND operator and can get away the unboolified one for
such chains of ANDs, because ANDs assume their inputs are all unboolified anyways.


### Absolute
```asm
abs[olute] <entity> [objective]
```
Turns the positive or negative score of a given entity into an absolute score (positive).

### Boolify
```asm
bool[ify] <entity> [objective]
```
Maps nonzero scores onto `1` and zero scores onto `0`.

### Logical Negation
```asm
neg[ate] <entity> [objective]
```
Maps nonzero scores onto `0` and zero scores onto `1`.

### Chain
```asm
chain <amount>:
    ; code block here
```
Executes multiple chain command blocks in the next ticks containing the specified code block.
This can be used to bypass the limit of commands per function per tick (2^16) by default.
It can also be used to achieve infra-tick execution of a code block in the next tick.

### If
```asm
if <entity> [objective] <logical_operator> <entity> [objective]:
    ; code block here
```
Executes a code block conditionally.

#### Example
```asm
if Player bananas >= #required_bananas:
    /tellraw Player {"text":"Congratulations on having gathered enough bananas!"}
```

### Logical Operators
* `>` Greater than
* `<` Lower than
* `>=` Greater-Equals
* `<=` Lower-Equals
* `==` Equals
* `!=` Not-Equals

#### Implementation notes
Using an if-statement may cause loss of the `@s` selector which refers to the executor of the function.
Refrain from using `/tell` commands and such inside of if-blocks as those may be executed from the perspective of a
temporary entity used exclusively for logic.

#### While
```asm
while <entity> [objective] <logical_operator> <entity> [objective]:
    ; code block here
```
A while statement is similar to an if-statement, except that it will rerun the code-block infra-tick as long as the
specified condition is true.

#### Example
```asm
; print all even numbers from 1 to 100
#i = 1
while #i <= 100:
    #tmp = i
    #tmp %= 2
    if #tmp == 0:
        /tellraw @a {"score":{"name":"#i","objective":"_int32"}}
    #i += 1
```

### Require
```asm
req[uire] <statement>
```
Puts the statement into the setup function.

#### On the Topic of Requirements
Certain statements have requirements that must be met by running the `_setup` function one time.
For example, adding two numbers of one objective requires that objective to exist in the first place.
Or for example, a `chain` statement requires a chain of command blocks to be placed down.
All requirements across all compiled files are initialized via the `_setup.mcfunction` file.

### Require-Objective
```asm
req-obj|require-objective <objective> [type]
```
Initializes an objective in the setup function.

### Require-Score
```asm
req-score|require-score <entity> [objective] <value>
```
Puts the assignment of a score into the setup function.


