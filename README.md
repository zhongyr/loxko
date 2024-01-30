# Loxko

This is a naive script interpreter based on Kotlin Multiplatform and expect to run on **ALL** platforms which KMP supports.

## Build

Using the basic gradle task to build targets, and run the executable in the ouput directs.

> ./jLox [script_dir]

or in a REPL mode

> ./jLox

The test script is in `test/resoutces/test.lox`

```
var a = 0;
var temp;

for (var b = 1; a < 10000; b = temp + b) {
  print a;
  temp = a;
  a = b;
}
```

## Todo

1. function calls
2. add an easy way to bind builtin functions to target platform related functions

   for example: developer may want to add a lib function __create_element() to create an Android View or UIView on iOS. The functions in side the interpreter environment should
   mapping to the native implementations and should be able to be added dynamically while luanching the Lox env.

Follows the tutorial of 

http://www.craftinginterpreters.com/
