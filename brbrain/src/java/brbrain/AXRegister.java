/**
 * <p>Represents the Dynamixel AX registers in a high-level way.</p>
 *
 * <p>Copyright (C) 2007 Marsette A. Vona, III</p>
 *
 * <p>This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.</p>
 *
 * <p>This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.</p>
 *
 * <p>You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc., 59
 * Temple Place - Suite 330, Boston, MA 02111-1307, USA.</p>
 **/

package brbrain;

/**
 * <p>Represents the Dynamixel AX registers in a high-level way.</p>
 *
 * <p>Multi-byte registers are handled as single values, write control and
 * value clamping is provided, encoding and decoding from the 11 bit sign
 * magnitude representation used by some AX-12 registers is handled, and
 * translation to and from natural fractional units is included.</p>
 *
 * <p>Copyright (C) 2008 Marsette A. Vona, III</p>
 *
 * @author Marsette (Marty) A. Vona, III
 **/
public abstract class AXRegister {

  private static final String svnid =
  "$Id: AXRegister.java 24 2008-05-02 22:08:41Z vona $";

  /** maximum Dynamixel ID **/
  public static final int MAX_DYNAMIXEL_ID = 253;

  /** start address of RAM **/
  public static final int RAM_START_ADDRESS = 24;

  /** Dynamixel error bit **/
  public static final int E_INPUT_VOLTAGE = (1<<0);

  /** Dynamixel error bit (goal pos out of range) **/
  public static final int E_ANGLE_LIMIT =    (1<<1);

  /** Dynamixel error bit **/
  public static final int E_OVERHEATING =    (1<<2);

  /** Dynamixel error bit (write parameter out of range) **/
  public static final int E_RANGE =          (1<<3);

  /** Dynamixel error bit (comm failure) **/
  public static final int E_CHECKSUM =       (1<<4);

  /** Dynamixel error bit (insufficient torque) **/
  public static final int E_OVERLOAD =       (1<<5);

  /** Dynamixel error bit (invalid instruction) **/
  public static final int E_INSTRUCTION =    (1<<6);

  /** verify that (start, n) is a valid span of registers **/
  public static void checkSpan(AXRegister start, int n) {

    if (start == null)
      throw new IllegalArgumentException("null start");

    if (n < 0)
      throw new IllegalArgumentException("negative n");

    int startOrdinal = start.ordinal;

    if ((startOrdinal+n) > start.getNumRegisters())
      throw new IllegalArgumentException("span beyond last register");
  }

  /** check whether a span of registers contains any which are read-only **/
  public static boolean containsReadOnlyRegs(AXRegister start, int n) {

    checkSpan(start, n);

    for (int i = 0; i < n; i++)
      if (!(start.getRelativeRegister(i).writeable))
        return true;
    
    return false;
  }

  /** duplicate first n element of from into to, reallocating as necessary **/
  public static AXRegister[] dup(AXRegister[] from, AXRegister[] to, int n) {
    to = ensureCapacity(to, n);
    System.arraycopy(from, 0, to, 0, n);
    return to;
  }

  /**
   * <p>Covers {@link #dup(AXRegister[], AXRegister[], int)}, copies all.</p>
   **/
  public static AXRegister[] dup(AXRegister[] from, AXRegister[] to) {
    return dup(from, to, from.length);
  }

  /** make sure <i>a</i> is at least length <i>n</i> **/
  public static AXRegister[] ensureCapacity(AXRegister[] a, int n) {
    if ((a == null) || (a.length < n))
      a = new AXRegister[n];
    return a;
  }

  /**
   * <p>Return span of <i>n</i> regs from <i>start</i>.</p>
   *
   * <p>Span is returned in <i>span</i> iff it's big enough, else a new
   * array.</p>
   *
   * <p>If <i>n</i> is less than 0 then the span includes all subsequent
   * registers.</p>
   **/
  public static AXRegister[] span(AXRegister start, int n, AXRegister[] span) {
    
    if (n < 0)
      n = start.getNumRegisters() - start.ordinal;

    span = ensureCapacity(span, n);

    for (int i = 0; i < n; i++)
      span[i] = start.getRelativeRegister(i);

    return span;
  }

  /** covers {@link #span(AXRegister, int, AXRegister[])}, always conses **/
  public static AXRegister[] span(AXRegister start, int n) {
    return span(start, n, null);
  }

  /** covers {@link #span(AXRegister, int, AXRegister[])}, spans till end **/
  public static AXRegister[] span(AXRegister start, AXRegister[] span) {
    return span(start, -1, span);
  }

  /** covers {@link #span(AXRegister, int)}, spans till end **/
  public static AXRegister[] span(AXRegister start) {
    return span(start, null);
  }

  /**
   * <p>Linearly interpolate between the points (abscissaAtLowEnd,
   * valueAtLowEnd) and (abscissaAtHighEnd, valueAtHighEnd).</p>
   **/
  public static float interp(float abscissa,
                             float valueAtLowEnd,
                             float abscissaAtLowEnd,
                             float valueAtHighEnd,
                             float abscissaAtHighEnd) {
    return
      valueAtLowEnd+
      (abscissa-abscissaAtLowEnd)*
      (valueAtHighEnd-valueAtLowEnd)/(abscissaAtHighEnd-abscissaAtLowEnd);
  }
                             
  /**
   * <p>Compose a human-readable string of AX-12 errors from a Dynamixel error
   * bitfield.</p>
   *
   * @param errors the bitpacked error value
   * @param buf the buffer to append, or null to make one
   *
   * @return the buffer to which the string was appended
   **/
  public static final StringBuffer errorsToString(int errors,
                                                  StringBuffer buf) {
    if (buf == null)
      buf = new StringBuffer();

    int n = 0;

    if ((errors & E_INPUT_VOLTAGE) != 0) {
      if (n++ > 0)
        buf.append(", ");
      buf.append("INPUT_VOLTAGE");
    }

    if ((errors & E_ANGLE_LIMIT) != 0) {
      if (n++ > 0)
        buf.append(", ");
      buf.append("ANGLE_LIMIT");
    }

    if ((errors & E_OVERHEATING) != 0) {
      if (n++ > 0)
        buf.append(", ");
      buf.append("OVERHEATING");
    }
    
    if ((errors & E_RANGE) != 0) {
      if (n++ > 0)
        buf.append(", ");
      buf.append("RANGE");
    }
    
    if ((errors & E_CHECKSUM) != 0) {
      if (n++ > 0)
        buf.append(", ");
      buf.append("CHECKSUM");
    }
    
    if ((errors & E_OVERLOAD) != 0) {
      if (n++ > 0)
        buf.append(", ");
      buf.append("OVERLOAD");
    }

    if ((errors & E_INSTRUCTION) != 0) {
      if (n++ > 0)
        buf.append(", ");
      buf.append("INSTRUCTION");
    }

    return buf;
  }

  /** Covers {@link #errorsToString(int, StringBuffer)} **/
  public static final String errorsToString(int errors) {
    return errorsToString(errors, null).toString();
  }

  /** zero-based index of this register in the register bank **/
  public final int ordinal;

  /** pretty human-readable register name **/
  public final String prettyName;

  /** start (byte) address of this register in the AX-12 register space **/
  public final int startAddr;

  /** byte width of this register **/
  public final int width;

  /** whether this register can be written **/
  public final boolean writeable;

  /** minimum valid write value of this register, if writeable **/
  public final int min;

  /** maximum valid write value of this register, if writeable **/
  public final int max;

  /** human-readable label for the natural units, if any **/
  public final String naturalUnitsLabel;

  /** whether to prefer natural or raw units by default **/
  public final boolean useNaturalUnitsByDefault;

  /**
   * <p>Whether this is a 2-byte little endian reg with a 10 bit magnitude
   * and the sign in bit 11.</p>
   *
   * <p>When the sign bit is set the value is interpreted as negative or
   * clocwise.</p>
   **/
  public final boolean signMagnitude11Bit;

  /** multiplier taking natural units to int register counts **/
  public final float naturalUnitsPerCount;

  /** writeable reg with all options **/
  protected AXRegister(int ordinal, String prettyName,
                       int startAddr, int width,
                       int min, int max,
                       boolean signMagnitude11Bit,
                       float naturalUnitsPerCount,
                       String naturalUnitsLabel,
                       boolean useNaturalUnitsByDefault) {
    this.ordinal = ordinal;
    this.prettyName = prettyName;
    this.startAddr = startAddr;
    this.width = width;
    this.writeable = true;
    this.min = min;
    this.max = max;
    this.signMagnitude11Bit = signMagnitude11Bit;
    this.naturalUnitsPerCount = naturalUnitsPerCount;
    this.naturalUnitsLabel = naturalUnitsLabel;
    this.useNaturalUnitsByDefault = useNaturalUnitsByDefault;
  }

  /** read-only reg with all options **/
  protected AXRegister(int ordinal, String prettyName,
                       int startAddr, int width,
                       boolean signMagnitude11Bit,
                       float naturalUnitsPerCount,
                       String naturalUnitsLabel,
                       boolean useNaturalUnitsByDefault) {
    this.ordinal = ordinal;
    this.prettyName = prettyName;
    this.startAddr = startAddr;
    this.width = width;
    this.writeable = false;
    this.min = Integer.MIN_VALUE;
    this.max = Integer.MAX_VALUE;
    this.signMagnitude11Bit = signMagnitude11Bit;
    this.naturalUnitsPerCount = naturalUnitsPerCount;
    this.naturalUnitsLabel = naturalUnitsLabel;
    this.useNaturalUnitsByDefault = useNaturalUnitsByDefault;
  }

  /** true iff register is in RAM **/
  public boolean isRAM() {
    return startAddr > RAM_START_ADDRESS;
  }

  /**
   * <p>Check if an integer write value is within the closed interval [{@link
   * #min}, {@link #max}].</p>
   *
   * @param value the value to check
   *
   * @return -1 if value is too low, +1 if it's too high, 0 if it's ok
   **/
  public int check(int value) {
    if (value < min)
      return -1;
    else if (value > max)
      return 1;
    else
      return 0;
  }

  /** covers {@link #check(int)}, converts {@link #fromNaturalUnits} **/
  public int check(float value) {
    return check(fromNaturalUnits(value));
  }

  /**
   * <p>Clamp an integer write value to the closed interval [{@link #min},
   * {@link #max}].</p>
   *
   * @param value the value to clamp
   *
   * @return the clamped value
   **/
  public int clamp(int value) {
    if (value < min)
      return min;
    else if (value > max)
      return max;
    else
      return value;
  }

  /**
   * <p>Covers {@link #clamp(int)}, converts {@link #fromNaturalUnits} and
   * {@link #toNaturalUnits}.</p>
   **/
  public float clamp(float value) {
    return toNaturalUnits(clamp(fromNaturalUnits(value)));
  }

  /**
   * <p>Encode an integer write value into the bits to write to the
   * register.</p>
   *
   * <p>Handles conversion to {@link #signMagnitude11Bit} as appropriate.</p>
   *
   * @param value the value to encode
   *
   * @return the encoded value
   **/
  public int encode(int value) {
    return
      (signMagnitude11Bit && (value < 0)) ? 
      ((1<<10) | (-value)) : value;
  }

  /**
   * <p>Encode an integer read value from the register bits.</p>
   *
   * <p>Handles conversion from {@link #signMagnitude11Bit} as
   * appropriate.</p>
   *
   * @param value the value to decode
   *
   * @return the decoded value
   **/
  public int decode(int value) {
    return
      (signMagnitude11Bit && ((value & (1<<10)) != 0)) ?
      -(value & 0x3ff) : value;
  }
   
  /**
   * <p>Convert a register int value to natural units.</p>
   *
   * <p>Default impl just multiplies by {@link #naturalUnitsPerCount}.</p>
   *
   * @param value the int value to convert
   *
   * @return the value in natural units
   **/ 
  public float toNaturalUnits(int value) {
    return value*naturalUnitsPerCount;
  }

  /**
   * <p>Convert a register int value from natural units.</p>
   *
   * <p>Default impl just divides by {@link #naturalUnitsPerCount}.</p>
   *
   * @param value the natural value to convert
   *
   * @return the value in int counts
   **/ 
  public int fromNaturalUnits(float value) {
    return (int) Math.round(value/naturalUnitsPerCount);
  }

  /** check if this is a boolean valued register **/
  public boolean isBoolean() {
    return (min == 0) && (max == 1);
  }

  /** get the register with the given relative ordinal **/
  public abstract AXRegister getRelativeRegister(int offset);

  /** get the total number of registers in this Dynamixel type **/
  public abstract int getNumRegisters();

  /** get the next register after this one **/
  public AXRegister nextRegister() {
    return getRelativeRegister(+1);
  }

  /** get register before this one **/
  public AXRegister prevRegister() {
    return getRelativeRegister(-1);
  }

  /** returns {@link #prettyName} **/
  public String toString() {
    return prettyName;
  }

  /** get an identifier for registers of this Dynamixel type **/
  protected abstract String getDynamixelType();

  /** returns the Java identifier for this register **/
  public String toIdentifierString() {
    return
      getDynamixelType().toUpperCase()+"_"+
      prettyName.toUpperCase().replace(' ', '_');
  }
}
