import cocotb
from cocotb.triggers import RisingEdge, ClockCycles
from cocotb.clock import Clock


@cocotb.test()
async def test_operation(dut):
    """Run 1000 random additions"""

    cocotb.start_soon(Clock(dut.clock, 1, units="ns").start())

    dut.io_in_bits_cordic_x.value = 0
    dut.io_in_bits_cordic_y.value = 0
    dut.io_in_bits_cordic_z.value = 0
    dut.io_in_bits_control_mode.value = 0
    dut.io_in_bits_control_rotType.value = 0
    dut.reset.value = 0
    await RisingEdge(dut.clock)
    dut.reset.value = 1
    await RisingEdge(dut.clock)
    dut.reset.value = 0

    dut.io_in_bits_cordic_x.value = 0b0000100110110010
    dut.io_in_bits_cordic_y.value = 0
    dut.io_in_bits_cordic_z.value = 0
    dut.io_in_bits_control_mode.value = 0
    dut.io_in_bits_control_rotType.value = 0
    dut.io_in_valid.value = 1
    await RisingEdge(dut.clock)
    dut.io_in_valid.value = 0

    await ClockCycles(dut.clock, 20)
