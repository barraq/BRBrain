# Hey Emacs, this is a -*- makefile -*-

# common definition
TARGET = brbrain
SRC = $(wildcard *.c)
ASRC = 
OPT = s

# CM dependent definition
ifeq ($(cm),cm510)
	MCU = atmega2561
	CPU_HZ = 16E6
	CM = -DCM510
else
	MCU = atmega128
	CPU_HZ = 16E6
	CM = -DCM5
endif

# set this nonzero to define the cpp symbol EN_DBG at compile time (this may be
# used to enable runtime debug code, and is orthogonal to compiling with debug
# info).
#EN_DBG = 1

# PRINTF_LIB_MIN - Minimalistic printf version
# PRINTF_LIB_FLOAT - Floating point printf version (requires MATH_LIB = -lm)
#PRINTF_LIB = $(PRINTF_LIB_MIN)

# SCANF_LIB_MIN - Minimalistic scanf version
# SCANF_LIB_FLOAT - Floating point + %[ scanf version (requires MATH_LIB = -lm)
#SCANF_LIB = $(SCANF_LIB_MIN)

#MATH_LIB = -lm

# AVRDUDE_PROGRAMMER = stk500v2
# AVRDUDE_PORT = /dev/ttyS0

# Use these values for the new avr programmers which have a usb interface.
# They are mkii programmers instead of stk500, although this is mostly
# irrelevant.
# -B command sets sck in microseconds, default may be very slow, see
# http://arcknowledge.com/hardware.avr.avrdude.devel/2006-11/msg00004.html
AVRDUDE_PROGRAMMER = avrisp2 -B 5
AVRDUDE_PORT = usb

# note: you need privileged access to the usb port to do this so you either
# have to run the program command as root or you can remount the usb file
# system to give yourself permission:
#
# sudo mount -t usbfs none /proc/bus/usb -o remount,devgid=`id -g`,devmode=664
#
# A similar line could be put in your /etc/fstab to make this happen
# automatically at boot

# Uncomment the following if you do /not/ wish a verification to be
# performed after programming the device.
# AVRDUDE_NO_VERIFY = -V

# Define [FUSE_BYTE | (LFUSE_BYTE HFUSE_BYTE [EFUSE_BYTE])] and [LOCK_BYTE]
# in hex to have them programmed by the program-fuses and program-lock
# targets
# FUSE_BYTE =
# LFUSE_BYTE =
# HFUSE_BYTE =
# EFUSE_BYTE =
# LOCK_BYTE =

# Compiler flag to set the C Standard level.
# c89   - "ANSI" C
# gnu89 - c89 plus GCC extensions
# c99   - ISO C99 standard (not yet fully implemented)
# gnu99 - c99 plus GCC extensions
CSTANDARD = -std=gnu99
 
# Place -D or -U options here
CDEFS = -DINITIAL_RXD_BLUETOOTH $(CM)

# Place -I options here
CINCS = -I/usr/local/avr/include

# choose one for dead-code elimination (or choose none for no DCE)
#DCE = compile-time
# 
# broken until binutils-avr-2.17? http://lists.gnu.org/archive/html/avr-gcc-list/2006-08/msg00055.html
# DCE = link-time 

default:
	@echo "building BrBrain firmware for $(MCU)..."
	$(MAKE) clean
	$(MAKE) all TARGET=$(TARGET)-initial-rs232
	$(MAKE) clean
	$(MAKE) all TARGET=$(TARGET)-initial-bt
	@echo "done"

realclean-all:
	@echo "cleaning..."
	$(MAKE) realclean TARGET=$(TARGET)-initial-rs232
	$(MAKE) realclean TARGET=$(TARGET)-initial-bt
	@echo "done"

-include ./makefile_AVR.inc

