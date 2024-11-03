# KConst
A proof-of-concept compiler plugin for Kotlin, designed to improve constant folding and constant propagation.

### I want to see it run!
Run `gradle test -i` to run the tests, displaying output. The plugin outputs the IR both before and after running upon it.
It also outputs, for the internship application, any calls that start with "eval" that have been simplified, and their output values.  
To modify the input code being tested, edit `plugin/src/test/kotlin/org/eu/aschwz/kconst/KConstTest.kt` and change the embedded Kotlin code.

### What can we currently optimize on?
We can optimize on any _deterministic_ function operating only on, and returning only _constant types_.  
We define a constant type as either a String, Int or Boolean (other number types are omitted for simplicity)  
We define a deterministic function either as a "simple" operation on constant types (e.g. equality, basic arithmetic...), 
or as a function that calls/uses only other deterministic functions/expressions, 
and does _not_ read from or write to global or static state, and does not recurse. 
Additionally, as we only consider functions written by the user, or pre-defined (in `interpret.kt`) as possibly deterministic. 
Notably, deterministic functions/expressions are absolutely allowed to use local variables, 
and in the case of expressions are allowed to use variables above the scope they are defined in. 
For simplicity, we assume lambdas are not used, and functions don't modify their arguments.  
Here's an example of some such functions:
```kotlin
var someGlobalState : Boolean = false

fun nonDeterministicFunctionDueToGlobals() {
    // This function will possibly (in this case always)
    // read some global state, so is _not_ deterministic
    if (someGlobalState) {
        return 2 + 2
    } else {
        return 1 + 1
    }
}

fun nonDetSideEffect() {
    // println is not a simple operation/user-defined, so is not counted as deterministic,
    // so this function is non-deterministic
    println("hii!!")
}

fun nonDetFib(n: Int) {
    if (n == 0 || n == 1) {
        return 1
    } else {
        // it's hard to reason about termination,
        // so we err on the side of caution and assume any function that recurses,
        // either directly or mutually,
        // to be non-deterministic
        return nonDetFib(n-1) + nonDetFib(n-2)
    }
}

// nullability has no effect on determinism
// TODO: check the above
fun deterministicFunction() : Int? {
    if (false) {
        // because `false` and `if` are both deterministic,
        // we can reason that this non-deterministic function will not be called,
        // so the overall function remains deterministic
        return nonDetFib(-4)
    } else {
        return 3*3
    }
}

fun otherDetFunction() {
    var i: Int? = 0
    if (i != 1) {
        // setting a variable is fine, provided all sets are a deterministic value
        // in a deterministic context
        i = deterministicFunction()
    }
    return i
}
```
Basically, all deterministic things can be evaluated at compile-time.

### Optimizations performed
- if-flattening (also when): For some deterministic boolean X, we replace `if (X) {Y} else {Z}` with either `Y` or `Z`. 
It's analogous for `when`, provided all the checks before the correct one are deterministic 
(any after don't matter, as they won't evaluate).
- call-flattening: For a deterministic function `F` with deterministic parameters `N0, N1, N2 ...` we replace 
`F(N0, N1, ...)` with the actual result.
- while-flattening: For a while loop `while (X) {Y}` we replace it with its collated side effects 
(mostly variables set to their results). This makes the loop deterministic.  
We can do this only if:
  - `X` and `Y` _remain_ deterministic throughout all iterations
  - The loop terminates within a fixed number of iterations (currently 64),
    to stop the compiler from looping infinitely on things like `while (true) {}`

### Things that don't work (probably)
Due to the Transformer design pattern, the evaluation of functions is generally rather optimistic,
in that if it encounters an IR construct it can't explicitly handle, it tends to ignore it,
which is good for functionality but rather bad for correctness.
- Complex types (anything not Int, String or Bool really)
- Lambdas and labelled returns
- Pass-by-reference and modifying arguments (hence the simple types)
- Breaks/Continues _might_ break, while loops in general are a little odd but mostly correct