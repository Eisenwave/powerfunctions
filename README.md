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
```
/scoreboard players operation <entity> <objective> <operator> <entity> <objective>
```
Other operators may or may not be supported directly by a Minecraft command.

If you're surprised what *boolified* refers to: *boolified* values are either `0` for `false` or `1` for `true`.
Generally, PowerFunctions assumes that inputs are not boolified and compiles operators accordingly.

Consider the following example:
```
; equivalent to saying 'result = first_condition AND second_condition AND third_condition
#result = #first_condition
#result &&= #second_condition
#result &&= #third_condition
; do something with the result ...
```
Depending on the context, we might not need to use the boolified AND operator and can get away the unboolified one for
such chains of ANDs, because ANDs assume their inputs are all unboolified anyways.


