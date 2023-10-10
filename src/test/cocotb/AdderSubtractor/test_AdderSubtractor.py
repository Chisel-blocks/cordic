import cocotb
from cocotb.triggers import Timer
import random
from bitstring import Bits

@cocotb.test()
async def test_adds(dut):
    """Run 1000 random additions"""

    dut.io_D.value = 0
    for i in range(0,1000):
        a = random.randint(-2**14, 2**14 - 1)
        b = random.randint(-2**14, 2**14)
        dut.io_A.value = a
        dut.io_B.value = b
        ans = a + b
        await Timer(5, units="ns")  # wait a bit
        assert dut.io_S.value.signed_integer == ans, f"io_S was {dut.io_S.value.signed_integer}, not {ans}"

@cocotb.test()
async def test_subs(dut):
    """Run 1000 random subtractions"""

    dut.io_D.value = 1
    for i in range(0,1000):
        a = random.randint(-2**14, 2**14 - 1)
        b = random.randint(-2**14, 2**14)
        dut.io_A.value = a
        dut.io_B.value = b
        ans = a - b
        await Timer(5, units="ns")  # wait a bit
        assert dut.io_S.value.signed_integer == ans, f"io_S was {dut.io_S.value}, not {ans}"