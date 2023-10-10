# Makefile

# defaults
SIM ?= icarus
TOPLEVEL_LANG ?= verilog

VERILOG_SOURCES += $(PWD)/../../../../verilog/AdderSubtractor.sv
# use VHDL_SOURCES for VHDL files

# TOPLEVEL is the name of the toplevel module in your Verilog or VHDL file
TOPLEVEL = AdderSubtractor

# MODULE is the basename of the Python test file
MODULE = test_AdderSubtractor

# include cocotb's make rules to take care of the simulator setup
include $(shell cocotb-config --makefiles)/Makefile.sim

%.sv:
	cd $(PWD)/../../../.. && $(MAKE) $(TOPLEVEL)

clean_vlog:
	rm $(VERILOG_SOURCES)
