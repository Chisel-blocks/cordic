# Makefile

# defaults
SIM ?= icarus
TOPLEVEL_LANG ?= verilog


# TOPLEVEL is the name of the toplevel module in your Verilog or VHDL file
TOPLEVEL = CordicCore

VERILOG_SOURCES += $(PWD)/../../../../verilog/$(TOPLEVEL).sv
# use VHDL_SOURCES for VHDL files

# MODULE is the basename of the Python test file
MODULE = test_$(TOPLEVEL)

# include cocotb's make rules to take care of the simulator setup
include $(shell cocotb-config --makefiles)/Makefile.sim

%.sv:
	cd $(PWD)/../../../.. && ./configure && $(MAKE) $(TOPLEVEL)

clean_vlog:
	rm $(VERILOG_SOURCES)
