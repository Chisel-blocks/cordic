# Pipelined generic CORDIC

Generic pipelined CORDIC that supports rotation/vectoring and circular/hyperbolic modes.
## Example
```
./configure && make mantissa_bits=2 fraction_bits=14 iterations=16 cordic_func=Basic
```
generates CordicTop.v to verilog/ folder.
## Configurability
### Resolution
The data type is signed fixed point. You can generate the CORDIC with varying bits of resolution by providing `mantissa_bits` and `fraction_bits`
### Number of iterations
Number of CORDIC iterations can be selected with `iterations`. Total number of clock cycles taken is `iterations+2+(1..3)`, as one clock cycle is added for both pre- and postprocessing steps, and hyperbolic mode introduces repeat iterations (1 for iterations=4..12, 2 for iterations=13..39, 3 for iterations >=40)
### Pre- and postprocessing
You can configure the CORDIC by selecting which pre- and postprocessor is used. Currently, the following ones have been implemented:
* "Basic" - No pre- or postprocessing. Data in rs1,rs2,rs3 is directly fed to x_0,y_0,z_0. Control(0) selects between Circular (0) and Hyperbolic (1) modes. Control(1) selects between Rotation (0) and Vectoring(0) modes
* "TrigFunc" - Adds pre- and postprocessing to calculate Sine, Cosine, Arctan, Sinh, Cosh, Arctanh, Exponential, and natural logarithm. Input data should be fed to rs1. The rest can be undefined. Output of the function will be in dOut, but the result of Cordic rotation is also provided in cordic.x/y/z. Control bits determine the operation, being indexed from 0-7 in the aforementioned order.
### Number representation
There are two supported number representations: "fixed-point" and "pi". This determines what the binary numbers represent:
- "fixed-point" - standard fixed-point representation
- "pi" - values range from -pi to +pi.
The provided `repr` will mostly affect the result of functions that calculate constants, i.e. functions in `CordicConstants`, `CordicMethods`, and `CordicLut`.

You can create your own under preprocessor/ and postprocessor/. Then, you need to add a checker in CordicTop.scala that uses your own pre- and postprocessor with a suitable input parameter.
