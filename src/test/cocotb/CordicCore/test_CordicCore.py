import cocotb
from cocotb.triggers import RisingEdge, ClockCycles
from cocotb.clock import Clock


@cocotb.test()
async def test_circ_rot_operation(dut):
    """Test circular rotation"""

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
    assert dut.io_out_valid == 1, f"Valid should be 1, was {dut.io_out_valid}"
    assert dut.io_out_bits_y.value.signed_integer in [
        -1,
        0,
        1,
    ], f"Y should be zero +- 1, was {dut.io_out_bits_y.value}"


@cocotb.test()
async def test_circ_vec_operation(dut):
    """Test circular vectoring"""

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

    dut.io_in_bits_cordic_x.value = 0b0001000000000000
    dut.io_in_bits_cordic_y.value = 0b1100110110111101
    dut.io_in_bits_cordic_z.value = 0
    dut.io_in_bits_control_mode.value = 1
    dut.io_in_bits_control_rotType.value = 0
    dut.io_in_valid.value = 1
    await RisingEdge(dut.clock)
    dut.io_in_valid.value = 0

    await ClockCycles(dut.clock, 20)
    assert dut.io_out_valid == 1, f"Valid should be 1, was {dut.io_out_valid}"
    assert dut.io_out_bits_y.value.signed_integer in [
        -3, -2, -1, 0, 1, 2, 3
    ], f"Y should be zero +- 3, was {dut.io_out_bits_y.value}"


@cocotb.test()
async def test_hyper_rot_operation(dut):
    """Test hyperbolic rotation"""

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

    dut.io_in_bits_cordic_x.value = 0b0001001101010001
    dut.io_in_bits_cordic_y.value = 0
    dut.io_in_bits_cordic_z.value = 0b1110111001100111
    dut.io_in_bits_control_mode.value = 0
    dut.io_in_bits_control_rotType.value = 1
    dut.io_in_valid.value = 1
    await RisingEdge(dut.clock)
    dut.io_in_valid.value = 0

    await ClockCycles(dut.clock, 20)
    assert dut.io_out_valid == 1, f"Valid should be 1, was {dut.io_out_valid}"
    assert dut.io_out_bits_z.value.signed_integer in [
        -1, 0, 1
    ], f"Z should be zero +- 1, was {dut.io_out_bits_z.value}"


@cocotb.test()
async def test_hyper_vec_operation(dut):
    """Test hyperbolic vectoring"""

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

    dut.io_in_bits_cordic_x.value = 0b0001001001100110
    dut.io_in_bits_cordic_y.value = 0b1111001001100110
    dut.io_in_bits_cordic_z.value = 0
    dut.io_in_bits_control_mode.value = 1
    dut.io_in_bits_control_rotType.value = 1
    dut.io_in_valid.value = 1
    await RisingEdge(dut.clock)
    dut.io_in_valid.value = 0

    await ClockCycles(dut.clock, 20)
    assert dut.io_out_valid == 1, f"Valid should be 1, was {dut.io_out_valid}"
    assert dut.io_out_bits_y.value.signed_integer in [
        -1, 0, 1
    ], f"Y should be zero +- 1, was {dut.io_out_bits_y.value}"
