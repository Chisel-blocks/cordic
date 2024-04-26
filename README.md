# Pipelined generic CORDIC

Generic pipelined CORDIC that supports rotation/vectoring and circular/hyperbolic modes. Due to the pipelined nature, it will generate quite a lot of hardware, but will have a throughput of one operation per clock cycle.
## Example
```
./configure && make config=basic CordicTop
```
generates cordic.v to verilog/ folder.
## Configurability
CORDIC is configurable by a YAML file. They are located under `configs` folder. Next, we explain what different options are available.
### Mode and rotation type
This CORDIC has been developed to support both static and dynamic selecetion between circular/hyperbolic rotation types and rotation/vectoring modes. The options for selecting these are self-explanatory:
- `enable-circular` (default: `false`)
- `enable-hyperbolic` (default: `false`)
- `enable-rotational` (default: `false`)
- `enable-vectoring` (default: `false`)

At least one mode and rotation type must be enabled.

### Resolution
The data type is signed fixed-point. You can generate the CORDIC with varying bits of resolution.

- `mantissa-bits` - number of mantissa bits used
- `fraction-bits` - number of fraction bits used
### Number of iterations
- `iterations` - number of CORDIC iterations generated

CORDIC calculates approximately one bit of precision per clock cycle. Total number of clock cycles taken is `iterations+2`, as one clock cycle is added for both pre- and postprocessing steps. Furthermore, in case hyperbolic mode is used, repeat iterations are introduced: 1 for `iterations=4..12`, 2 for `iterations=13..39`, 3 for `iterations >=40`.
### Pre- and postprocessing
- `preprocessor-class` - which preprocessor class is used (default: `"Basic"`)
- `postprocessor-class` - which postprocessor class is used (default: `"Basic"`)

You can configure the CORDIC by selecting which pre- and postprocessor is used. Different pre- and postprocessors are documented in another section. The default class simply feeds data through to CordicCore.

* "TrigFunc" - Adds pre- and postprocessing to calculate Sine, Cosine, Arctan, Sinh, Cosh, Arctanh, Exponential, and natural logarithm. Input data should be fed to rs1. The rest can be undefined. Output of the function will be in dOut, but the result of Cordic rotation is also provided in cordic.x/y/z. Control bits determine the operation, being indexed from 0-7 in the aforementioned order.
### Number representation
- `number-repr` - which number representation is used (default: `"fixed-point"`)

This determines what the binary numbers represent:
- `"fixed-point"` - standard fixed-point representation
- `"pi"` - values range from -pi to +pi. Here, mantissa-bits and fraction-bits separation loses importance, what matters is their sum.

The provided representation will affect constants, such as the arctan(h) values for angle computation and inverse CORDIC gain. Typically, `fixed-point` makes most sense, but `pi` allows very efficient comparisons with fractions of pi (needed in e.g. phase accumulator).

### Used inputs
You can omit unused inputs from the CORDIC. For example, the upconvert config does not utilize `rs3` input, because it comes from the phase accumulator. In order to omit the rs3 input from IO list, you can use:
- `"used-inputs"` - list of used inputs (1, 2, or 3)

For example: `used-inputs: [1, 2]`. By default, it is `[1, 2, 3]`.

## Available pre- and postprocessors

You can create your own under preprocessor/ and postprocessor/. Then, you need to add a checker in CordicTop.scala that uses your own pre- and postprocessor with a suitable input parameter.

### Basic
No pre- or postprocessing. Input data in `io_in_bits_rs1/rs2/rs3` represent `x_0/y_0/ang_0`. All modes and rotation types are enabled, and controlled as follows:

- `io_in_bits_control(0)` - 0 -> circular, 1 -> hyperbolic
- `io_in_bits_control(1)` - 0 -> rotation, 1 -> vectoring

Output data is provided in `io_out_bits_cordic_x/y/z`. `dOut` output holds no purpose.
### TrigFunc
Processing for calculating various trigonometric functions: Sine, Cosine, Arctan, Sinh, Cosh, Arctanh, Exponential, Log. In this mode, all inputs are fed into `io_in_bits_rs1` - `rs2 and rs3` have no purpose! Output is driven to `io_out_bits_dOut`.

These processors handle the generation and distribution of initial values. The result is always scaling-free. However, note that, due to CORDIC limitations, there is a specific range of input values that can be calculated.

The calculated function is selected as follows:
- `io_in_bits_control <= 0` = Sine
- `io_in_bits_control <= 1` = Cosine
- `io_in_bits_control <= 2` = Arctan
- `io_in_bits_control <= 3` = Sinh
- `io_in_bits_control <= 4` = Cosh
- `io_in_bits_control <= 5` = Arctanh
- `io_in_bits_control <= 6` = Exponential
- `io_in_bits_control <= 7` = Log

TODO: add input range values here

### UpConvert
This processor is meant for using CORDIC for up- or downconversion purpose. It instantiates a phase accumulator, which is used as an input for `io_in_bits_rs3`. This is a counter which increments after each valid sample. The purpose is to add rotation to I/Q pairs, effectively mixing them up to higher frequencies. I and Q should be provided to `io_in_bits_rs1/rs2`. The output, `io_out_bits_cordic_x/y`, hold the mixed I and Q values.

This processor requires the number representation `"pi"` as it needs to sample between -pi and +pi. Thus, this can be achieved by letting the counter overflow at +pi, wrapping to -pi. 


