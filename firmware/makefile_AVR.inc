# Hey Emacs, this is a -*- makefile -*-

######################################################################################
# generic parts of AVR-GCC Makefile, derived from the WinAVR template (which
# is public domain), believed to be neutral to any flavor of "make"
# (GNU make, BSD make, SysV make)
######################################################################################
# specific makefiles in each dir should include the following block:

# MCU = atmega8
# TARGET = foo 
# SRC = $(wildcard *.c)
# ASRC = 
# OPT = s
# CPU_HZ = 4E6
# 
# # set this nonzero to define the cpp symbol EN_DBG at compile time (this may
# # be used to enable runtime debug code, and is orthogonal to compiling with
# # debug info).
# #EN_DBG = 1
#
# # PRINTF_LIB_MIN - Minimalistic printf version
# # PRINTF_LIB_FLOAT - Floating point printf version (requires MATH_LIB = -lm)
# PRINTF_LIB = $(PRINTF_LIB_FLOAT)
#
# # SCANF_LIB_MIN - Minimalistic scanf version
# # SCANF_LIB_FLOAT - Floating point + %[ scanf version (requires MATH_LIB = -lm)
# SCANF_LIB = $(SCANF_LIB_FLOAT)
#
# MATH_LIB = -lm
#
# AVRDUDE_PROGRAMMER = stk500v2
# AVRDUDE_PORT = /dev/ttyS0
#
# # Use these values for the new avr programmers which have a usb interface.
# # They are mkii programmers instead of stk500, although this is mostly
# # irrelevant.
# # AVRDUDE_PROGRAMMER = avrisp2
# # AVRDUDE_PORT = usb
#
# # note: you need privileged access to the usb port to do this so you either
# # have to run the program command as root or you can remount the usb file
# # system to give yourself permission:
# #
# # sudo mount -t usbfs none /proc/bus/usb -o remount,devgid=`id -g`,devmode=664
# #
# # A similar line could be put in your /etc/fstab to make this happen
# # automatically at boot
#
# # Uncomment the following if you do /not/ wish a verification to be
# # performed after programming the device.
# AVRDUDE_NO_VERIFY = -V
# 
# # Define [FUSE_BYTE | (LFUSE_BYTE HFUSE_BYTE [EFUSE_BYTE])] and [LOCK_BYTE]
# # in hex to have them programmed by the program-fuses and program-lock
# # targets
# # FUSE_BYTE =
# # LFUSE_BYTE =
# # HFUSE_BYTE =
# # EFUSE_BYTE =
# # LOCK_BYTE =
#
# # Compiler flag to set the C Standard level.
# # c89   - "ANSI" C
# # gnu89 - c89 plus GCC extensions
# # c99   - ISO C99 standard (not yet fully implemented)
# # gnu99 - c99 plus GCC extensions
# CSTANDARD = -std=gnu99
# 
# # Place -D or -U options here
# CDEFS =
# 
# # Place -I options here
# CINCS = 
# 
# # choose one for dead-code elimination (or choose none for no DCE)
# # DCE = compile-time
# 
# # broken until binutils-avr-2.17? http://lists.gnu.org/archive/html/avr-gcc-list/2006-08/msg00055.html
# # DCE = link-time
#
# -include makefile_AVR.inc 

######################################################################################
######################################################################################



# The rest of this file is the generic stuff

SVN_VERSION := $(shell svnversion -n | cut -f2 -d:)
BUILD_DATE := $(shell date +%Y-%m-%d)

# Debugging format.
# Native formats for AVR-GCC's -g are stabs [default], or dwarf-2.
# AVR (extended) COFF requires stabs, plus an avr-objcopy run.
DEBUG = stabs

#CDEBUG = -g$(DEBUG)
CDBG = $(if $(EN_DBG),-DEN_DBG,)
CWARN = -Wall #-Wstrict-prototypes
CTUNING = -funsigned-char -funsigned-bitfields -fpack-struct -fshort-enums
#CEXTRA = -Wa,-adhlns=$(<:.c=.lst)

#compile-time DCE compiler options
CDCEC = -combine -fwhole-program

#link-time DCE compiler options
CDCEL = -ffunction-sections

#link-time DCE linker options
LDCEL = -Wl,--gc-sections

#ASFLAGS = -Wa,-adhlns=$(<:.S=.lst),-gstabs 

CFLAGS = $(CDEBUG) $(CDBG) $(CDEFS) $(CINCS) -O$(OPT) $(CWARN) $(CSTANDARD) -DF_CPU=$(CPU_HZ) -DSVN_VERSION=\"$(SVN_VERSION)\" -DBUILD_DATE=\"$(BUILD_DATE)\" $(CEXTRA)

# used for "make depend"
DEPENDFILE = makefile.depend

#Additional libraries.

# Minimalistic printf version
PRINTF_LIB_MIN = -Wl,-u,vfprintf -lprintf_min

# Floating point printf version (requires MATH_LIB = -lm below)
PRINTF_LIB_FLOAT = -Wl,-u,vfprintf -lprintf_flt


# Minimalistic scanf version
SCANF_LIB_MIN = -Wl,-u,vfscanf -lscanf_min

# Floating point + %[ scanf version (requires MATH_LIB = -lm below)
SCANF_LIB_FLOAT = -Wl,-u,vfscanf -lscanf_flt

# External memory options

# 64 KB of external RAM, starting after internal RAM (ATmega128!),
# used for variables (.data/.bss) and heap (malloc()).
#EXTMEMOPTS = -Wl,-Tdata=0x801100,--defsym=__heap_end=0x80ffff

# 64 KB of external RAM, starting after internal RAM (ATmega128!),
# only used for heap (malloc()).
#EXTMEMOPTS = -Wl,--defsym=__heap_start=0x801100,--defsym=__heap_end=0x80ffff

EXTMEMOPTS =

#LDMAP = $(LDFLAGS) -Wl,-Map=$(TARGET).map,--cref
LDFLAGS = $(EXTMEMOPTS) $(LDMAP) $(PRINTF_LIB) $(SCANF_LIB) $(MATH_LIB)

# Programming support using avrdude.

AVRDUDE_WRITE_FLASH = -U flash:w:$(TARGET).hex
#AVRDUDE_WRITE_EEPROM = -U eeprom:w:$(TARGET).eep
AVRDUDE_WRITE_FUSES = $(if $(FUSE_BYTE),-U fuse:w:0x$(FUSE_BYTE):m,$(if $(LFUSE_BYTE),-U lfuse:w:0x$(LFUSE_BYTE):m) $(if $(HFUSE_BYTE),-U hfuse:w:0x$(HFUSE_BYTE):m) $(if $(EFUSE_BYTE),-U efuse:w:0x$(EFUSE_BYTE):m))
AVRDUDE_WRITE_LOCK = $(if $(LOCK_BYTE),-U lock:w:$(LOCK_BYTE):m)

AVRDUDE_READBACK_FILE = .avrdude-readback

DUMP_AVRDUDE_READBACK_FILE = echo; echo $@; echo 76543210; xxd -b $(AVRDUDE_READBACK_FILE) | cut -d ' ' -f 2; xxd $(AVRDUDE_READBACK_FILE) | cut -d ' ' -f 2; rm $(AVRDUDE_READBACK_FILE)

# Uncomment the following if you want avrdude's erase cycle counter.
# Note that this counter needs to be initialized first using -Yn,
# see avrdude manual.
#AVRDUDE_ERASE_COUNTER = -y

# Increase verbosity level.  Please use this when submitting bug
# reports about avrdude. See <http://savannah.nongnu.org/projects/avrdude> 
# to submit bug reports.
#AVRDUDE_VERBOSE = -v -v

AVRDUDE_BASIC = -p $(MCU) -P $(AVRDUDE_PORT) -c $(AVRDUDE_PROGRAMMER)
AVRDUDE_FLAGS = $(AVRDUDE_BASIC) $(AVRDUDE_NO_VERIFY) $(AVRDUDE_VERBOSE) $(AVRDUDE_ERASE_COUNTER) 

CC = avr-gcc
OBJCOPY = avr-objcopy
OBJDUMP = avr-objdump
SIZE = avr-size
NM = avr-nm
AVRDUDE = avrdude
REMOVE = rm -f
MV = mv -f

# Define all object files.
OBJ = $(SRC:.c=.o) $(ASRC:.S=.o) 

# Define all listing files.
LST = $(ASRC:.S=.lst) $(SRC:.c=.lst)

# Combine all necessary flags and optional flags.
# Add target processor to flags.
ALL_CFLAGS = -mmcu=$(MCU) -I. $(CFLAGS)
ALL_ASFLAGS = -mmcu=$(MCU) -I. -x assembler-with-cpp $(ASFLAGS)


# Default target.
all: build

build: depend elf hex bin eep lss

package: build
package.recursive: build

elf: $(TARGET).elf
hex: $(TARGET).hex
bin: $(TARGET).bin
eep: $(TARGET).eep
lss: $(TARGET).lss 
sym: $(TARGET).sym


# Program the device.  
program: $(TARGET).hex $(TARGET).eep
	$(AVRDUDE) $(AVRDUDE_FLAGS) $(AVRDUDE_WRITE_FLASH) $(AVRDUDE_WRITE_EEPROM)

program-fuses: 
	$(AVRDUDE) $(AVRDUDE_FLAGS) $(AVRDUDE_WRITE_FUSES)

program-lock: 
	$(AVRDUDE) $(AVRDUDE_FLAGS) $(AVRDUDE_WRITE_LOCK)

read-fuse:
	$(AVRDUDE) $(AVRDUDE_FLAGS) -U fuse:r:$(AVRDUDE_READBACK_FILE):r
	$(DUMP_AVRDUDE_READBACK_FILE)

read-lfuse:
	$(AVRDUDE) $(AVRDUDE_FLAGS) -U lfuse:r:$(AVRDUDE_READBACK_FILE):r
	$(DUMP_AVRDUDE_READBACK_FILE)

read-hfuse:
	$(AVRDUDE) $(AVRDUDE_FLAGS) -U hfuse:r:$(AVRDUDE_READBACK_FILE):r
	$(DUMP_AVRDUDE_READBACK_FILE)

read-efuse:
	$(AVRDUDE) $(AVRDUDE_FLAGS) -U efuse:r:$(AVRDUDE_READBACK_FILE):r
	$(DUMP_AVRDUDE_READBACK_FILE)

read-lock:
	$(AVRDUDE) $(AVRDUDE_FLAGS) -U lock:r:$(AVRDUDE_READBACK_FILE):r
	$(DUMP_AVRDUDE_READBACK_FILE)

# Convert ELF to COFF for use in debugging / simulating in AVR Studio or VMLAB.
COFFCONVERT=$(OBJCOPY) --debugging \
--change-section-address .data-0x800000 \
--change-section-address .bss-0x800000 \
--change-section-address .noinit-0x800000 \
--change-section-address .eeprom-0x810000 


coff: $(TARGET).elf
	$(COFFCONVERT) -O coff-avr $(TARGET).elf $(TARGET).cof


extcoff: $(TARGET).elf
	$(COFFCONVERT) -O coff-ext-avr $(TARGET).elf $(TARGET).cof


.SUFFIXES: .elf .hex .eep .lss .sym .cee .bin

.hex.bin:
	$(OBJCOPY) -I ihex -O binary -R .eeprom $< $@

.elf.hex:
	$(OBJCOPY) -O ihex -R .eeprom $< $@

.elf.eep:
	-$(OBJCOPY) -j .eeprom --set-section-flags=.eeprom="alloc,load" \
	--change-section-lma .eeprom=0 -O elf $< $@


# Create extended listing file from ELF output file.
.elf.lss:
	$(OBJDUMP) -h -S $< > $@


# Create a symbol table from ELF output file.
.elf.sym:
	$(NM) -n $< > $@


# Link: create ELF output file from object files.
ifeq ($(DCE),compile-time)
$(TARGET).elf: $(SRC)
	$(CC) $(ALL_CFLAGS) $(CDCEC) $(SRC) --output $@ $(LDFLAGS)
endif
ifeq ($(DCE),link-time)
$(TARGET).elf: $(OBJ)
	$(CC) $(ALL_CFLAGS) --output $@ $(LDFLAGS) $(LDCEL) 
endif
ifeq ($(DCE),)
$(TARGET).elf: $(OBJ)
	$(CC) $(ALL_CFLAGS) $(OBJ) --output $@ $(LDFLAGS)
endif

# macro Expand: run the c preprocessor
.c.cee:
	$(CC) $(ALL_CFLAGS) -E $< > $@


# Compile: create object files from C source files.
ifeq ($(DCE),link-time)
.c.o:
	$(CC) -c $(ALL_CFLAGS) $(CDCEL) $< -o $@ 
else
.c.o:
	$(CC) -c $(ALL_CFLAGS) $< -o $@ 
endif


# Compile: create assembler files from C source files.
.c.s:
	$(CC) -S $(ALL_CFLAGS) $< -o $@


# Assemble: create object files from assembler source files.
.S.o:
	$(CC) -c $(ALL_ASFLAGS) $< -o $@

# Target: clean project.
clean:
	$(REMOVE) \
	$(TARGET).cof $(TARGET).elf $(TARGET).map $(TARGET).sym $(TARGET).lss \
	$(OBJ) $(LST) $(SRC:.c=.s) $(SRC:.c=.d) $(SRC:.c=.cee)

realclean: clean
	$(REMOVE) $(TARGET).hex $(TARGET).bin $(TARGET).eep $(DEPENDFILE)

depend:
	$(CC) -M -mmcu=$(MCU) $(CDEFS) $(CINCS) $(SRC) $(ASRC) > $(DEPENDFILE)

.PHONY:	all build package package.recursive elf hex eep lss sym program coff extcoff clean realclean depend

#use -include so that build does not fail if dependfile is not present
-include $(DEPENDFILE)

