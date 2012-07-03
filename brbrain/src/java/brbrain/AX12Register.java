/**
 * <p>Represents the Dynamixel AX-12 registers in a high-level way.</p>
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

import java.util.*;
import java.lang.reflect.*;

/**
 * <p>Represents the Dynamixel AX-12 registers in a high-level way.</p>
 *
 * <p>Copyright (C) 2008 Marsette A. Vona, III</p>
 *
 * @author Marsette (Marty) A. Vona, III
 **/
public class AX12Register extends AXRegister {
  
  /** the Dynamixel type identifier **/
  public static final String DYNAMIXEL_TYPE = "AX12";

  /** nominal battery voltage in Volts (8xNiMH) = (8x1.2V) = 9.6V **/
  public static final float NOMINAL_BATTERY_VOLTAGE = 9.6f;

  /**
   * <p>Multiplication factor taking {@link #AX12_MOVING_SPEED} raw value to
   * position counts per millisecond.</p>
   *
   * <pre>
   * Tested both @12.4V and @9.8V
   * 500-&gt;400 @ 20: 3s  (f = 100/(3000*20) = 100/60000 = 1/600)
   * 500-&gt;400 @ 10: 6s  (f = 100/(6000*10) = 100/60000 = 1/600)
   * 500-&gt;400 @  5: 11s (f = 100/(11000*5) = 100/55000 = 1/550)
   * 500-&gt;400 @  2: 25s (f = 100/(25000*2) = 100/50000 = 1/500)
   * 500-&gt;400 @  1: 49s (f = 100/(49000*1) = 100/49000 = 1/490)
   * </pre>
   **/
  public static final double MOVING_SPEED_TO_COUNTS_PER_MS =
//  AX12_MOVING_SPEED.naturalUnitsPerCount* //rev/min
//  (1.0/60000.0)*                        //min/ms
//  (1.0/AX12_GOAL_POSITION.naturalUnitsPerCount)* //count/deg
//  360.0; //deg/rev
  1.0/550.0;

  /** next register ordinal during class init **/
  protected static int nextOrdinal = 0;

  /** dynamixel model number (RO) **/
  public static final AX12Register AX12_MODEL_NUMBER =
    new AX12Register(nextOrdinal++, "model number", 0, 2);

  /** dynamixel firmware version (RO) **/
  public static final AX12Register AX12_FIRMWARE_VERSION =
    new AX12Register(nextOrdinal++, "firmware version", 2);

  /** dynamixel ID (RW) **/
  public static final AX12Register AX12_ID =
    new AX12Register(nextOrdinal++, "id", 3, 0, MAX_DYNAMIXEL_ID);
  
  /**
   * <p>Dynamixel Baud Rate (RW), natural units [kbits/sec].</p>
   *
   * <p>See table in dynamixel docs for standard rates.</p>
   *
   * <p>Changing this will probably make the CM-5 firmware unhappy.</p>
   **/
  public static final AX12Register AX12_BAUD_RATE =
    new AX12Register(nextOrdinal++, "baud rate", 4, 0, 254, "kbps", true) {
    
    public float toNaturalUnits(int value) {
      return (float) (2.0e3/(value+1));
    }
    
    public int fromNaturalUnits(float value) {
      return (int) Math.round((2.0e3/value)-1.0);
    }
  };

  /**
   * <p>Dynamixel return delay time (RW), natural units [usec].</p>
   *
   * <p>Changing this will probably make the CM-5 firmware unhappy.</p>
   **/
  public static final AX12Register AX12_RETURN_DELAY_TIME =
    new AX12Register(nextOrdinal++, "return delay time",
                     5, 0, 254, 2.0f, "us", true);
  
  /** clockwise angle limit (RW), natural units [deg] **/
  public static final AX12Register AX12_CW_ANGLE_LIMIT =
    new AX12Register(nextOrdinal++, "cw angle limit",
                     6, 2, 0, 1023, 300.0f/1023.0f, "deg", false);
  
  /** counter-clockwise angle limit (RW), natural units [deg] **/
  public static final AX12Register AX12_CCW_ANGLE_LIMIT =
    new AX12Register(nextOrdinal++, "ccw angle limit",
                     8, 2, 0, 1023, 300.0f/1023.0f, "deg", false);
  
  /** limit temp (RW), natural units [deg Celcius] **/
  public static final AX12Register AX12_HIGHEST_LIMIT_TEMPERATURE =
    new AX12Register(nextOrdinal++, "highest limit temperature",
                     11, 0, 150, 1.0f, "C", true);
  
  /** low limit voltage (RW), natural units [Volts] **/
  public static final AX12Register AX12_LOWEST_LIMIT_VOLTAGE =
    new AX12Register(nextOrdinal++, "lowest limit voltage",
                     12, 50, 250, 0.1f, "V", true);
  
  /** high limit voltage (RW), natural units [Volts] **/
  public static final AX12Register AX12_HIGHEST_LIMIT_VOLTAGE =
    new AX12Register(nextOrdinal++, "highest limit voltage",
                     13, 50, 250, 0.1f, "V", true);
  
  /**
   * <p>Maximum torque (RW), natural units normalized to [0.0, 1.0].</p>
   *
   * <p>According to dynamixel manual, max torque 0 enables "Free Run" mode,
   * whatever that is.  Also note that the dynamixel appears to copy this
   * value to {@link #AX12_TORQUE_LIMIT} at boot and may not respect changes to
   * it (or to {@link #AX12_TORQUE_LIMIT}?) until the next boot.</p>
   *
   * <p>It is unclear how the dynamixel measures "torque" and "load" (current
   * sensing? or based only on servo error?), and calibration relating to
   * physical units is TBD.</p>
   **/
  public static final AX12Register AX12_MAX_TORQUE =
    new AX12Register(nextOrdinal++, "max torque",
                     14, 2, 0, 1023, 1.0f/1023.0f, "", false);
  
  /**
   * <p>Dynamixel status return level (RW).</p>
   *
   * <p>0 means no return packets, 1 means return packets only for
   * READ_DATA, 2 means return packets always.</p>
   *
   * <p>Changing this will probably make the CM-5 firmware unhappy.</p>
   **/
  public static final AX12Register AX12_STATUS_RETURN_LEVEL =
    new AX12Register(nextOrdinal++, "status return level", 16, 1, 0, 2);
  
  /** bitmask of <code>E_*</code> constants to trigger the LED (RW) **/
  public static final AX12Register AX12_ALARM_LED =
    new AX12Register(nextOrdinal++, "alarm led", 17, 0, 127);
  
  /** bitmask of <code>E_*</code> constants to trigger torque off (RW) **/
  public static final AX12Register AX12_ALARM_SHUTDOWN =
    new AX12Register(nextOrdinal++, "alarm shutdown", 18, 0, 127);
  
  /** undocumented pot calibration (RO) **/
  public static final AX12Register AX12_DOWN_CALIBRATION =
    new AX12Register(nextOrdinal++, "down calibration", 20, 2);
  
  /** undocumented pot calibration (RO) **/
  public static final AX12Register AX12_UP_CALIBRATION =
    new AX12Register(nextOrdinal++, "up calibration", 22, 2);
  
  /** enables torque generation (RW), boolean, positive logic **/
  public static final AX12Register AX12_TORQUE_ENABLE =
    new AX12Register(nextOrdinal++, "torque enable", 24, 0, 1);
  
  /** direct LED control (RW), boolean, positive logic **/
  public static final AX12Register AX12_LED =
    new AX12Register(nextOrdinal++, "led", 25, 0, 1);
  
  /**
   * <p>Clockwise compliance margin (RW), natural units normalized to [0.0,
   * 1.0].</p>
   *
   * <p>This is the angular slop allowed before current is applied.</p>
   *
   * <p>Calibration relating to physical units is TBD.</p>
   **/
  public static final AX12Register AX12_CW_COMPLIANCE_MARGIN =
    new AX12Register(nextOrdinal++, "cw compliance margin",
                     26, 0, 254, 1.0f/254.0f, "", false);
  
  /**
   * <p>Counter-clockwise compliance margin (RW), natural units normalized to
   * [0.0, 1.0].</p>
   *
   * <p>This is the angular slop allowed before current is applied.</p>
   *
   * <p>Calibration relating to physical units is TBD.</p>
   **/
  public static final AX12Register AX12_CCW_COMPLIANCE_MARGIN =
    new AX12Register(nextOrdinal++, "ccw compliance margin",
                     27, 0, 254, 1.0f/254.0f, "", false);
  
  /**
   * <p>Clockwise compliance slope (RW), natural units normalized to [0.0,
   * 1.0].</p>
   *
   * <p>This appears to be 1.0 minus the P-gain.</p>
   *
   * <p>Actual calibration relating to physical units of rotational stiffness
   * depends on operating voltage.</p>
   *
   * <p>In an example, the dynamixel documentation states that only the
   * integer part of the base-2 log of this value is significant.</p>
   **/
  public static final AX12Register AX12_CW_COMPLIANCE_SLOPE =
    new AX12Register(nextOrdinal++, "cw compliance slope",
                     28, 1, 254, "", false) {
    
    public float toNaturalUnits(int value) {
      return ((float) (value-1))*1.0f/253.0f;
    }
    
    public int fromNaturalUnits(float value) {
      return (int) Math.round(value*253.0f+1.0f);
    }
  };
  
  /**
   * <p>Counter-clockwise compliance slope (RW), natural units normalized to
   * [0.0, 1.0].</p>
   *
   * <p>This appears to be 1.0 minus the P-gain.</p>
   *
   * <p>Actual calibration relating to physical units of rotational stiffness
   * depends on operating voltage.</p>
   *
   * <p>In an example, the dynamixel documentation states that only the
   * integer part of the base-2 log of this value is significant.</p>
   **/
  public static final AX12Register AX12_CCW_COMPLIANCE_SLOPE =
    new AX12Register(nextOrdinal++, "ccw compliance slope",
                     29, 1, 254, "", false) {

    public float toNaturalUnits(int value) {
      return ((float) (value-1))*1.0f/253.0f;
    }

    public int fromNaturalUnits(float value) {
      return (int) Math.round(value*253.0f+1.0f);
    }
  };

  /**
   * <p>Goal position (RW), natural units [deg].</p>
   *
   * <p>Servo is centered when goal position is at center of range.</p>
   **/
  public static final AX12Register AX12_GOAL_POSITION =
    new AX12Register(nextOrdinal++, "goal position",
                     30, 2, 0, 1023, 300.0f/1023.0f, "deg", false);

  /**
   * <p>Moving speed (RW), natural units [rev/min].</p>
   *
   * <p>It appears that in position control mode this is effectively a(n
   * unsigned) speed <i>limit</i>, with the limit disabled when this is set
   * to zero.</p>
   *
   * <p>Setting both {@link #AX12_CW_ANGLE_LIMIT} and {@link
   * #AX12_CCW_ANGLE_LIMIT} to zero appears to switch the servo to velocity
   * control mode, where the value of this register is the <i>signed</i> goal
   * velocity.</p>
   **/
  public static final AX12Register AX12_MOVING_SPEED =
    new AX12Register(nextOrdinal++, "moving speed",
                     32, 2, -1023, 1023, true, 114.0f/1023.0f, "rpm", false);

  /** See {@link #AX12_MAX_TORQUE} **/
  public static final AX12Register AX12_TORQUE_LIMIT =
    new AX12Register(nextOrdinal++, "torque limit",
                     34, 2, 0, 1023, 1.0f/1023.0f, "", false);

  /** Current position (RO), natural units [deg] **/
  public static final AX12Register AX12_PRESENT_POSITION =
    new AX12Register(nextOrdinal++, "present position",
                     36, 2, 300.0f/1023.0f, "deg", false);

  /** Current speed (RO), natural units [rev/min] **/
  public static final AX12Register AX12_PRESENT_SPEED =
    new AX12Register(nextOrdinal++, "present speed",
                     38, 2, true, 114.0f/1023.0f, "rpm", false);

  /**
   * <p>Current "load" (RO), natural units normalized to [0.0, 1.0].</p>
   *
   * <p>It is unclear whether "load" is the same as "torque" here.</p>
   *
   * <p>Calibration to physical units is TBD.</p>
   **/
  public static final AX12Register AX12_PRESENT_LOAD =
    new AX12Register(nextOrdinal++, "present load",
                     40, 2, true, 1.0f/1023.0f, "", false);

  /** Current voltage (RO), natural units [Volts] **/
  public static final AX12Register AX12_PRESENT_VOLTAGE =
    new AX12Register(nextOrdinal++, "present voltage", 42, 0.1f, "V", true);

  /** Current temperature (RO), natural units [degrees Celcius] **/
  public static final AX12Register AX12_PRESENT_TEMPERATURE =
    new AX12Register(nextOrdinal++, "present temperature",
                     43, 1.0f, "C", true);

  /** Whether there is a registered instruction pending (RW), boolean **/
  public static final AX12Register AX12_REGISTERED_INSTRUCTION =
    new AX12Register(nextOrdinal++, "registered instruction", 44, 0, 1);

  /** Whether the servo is currentl moving (RO), boolean **/
  public static final AX12Register AX12_MOVING =
    new AX12Register(nextOrdinal++, "moving", 46);

  /**
   * <p>Whether to restrict writing to registers {@link #AX12_TORQUE_ENABLE}
   * through {@link #AX12_TORQUE_LIMIT} (RW), boolean.</p>
   *
   * <p>Once this is set it becomes immutable until power cycle.</p>
   **/
  public static final AX12Register AX12_LOCK =
    new AX12Register(nextOrdinal++, "lock", 47, 0, 1);

  /**
   * <p>Initial current to apply after the position error has exceeded the
   * compliance margin (RW), natural units normalized to [0.0, 1.0].</p>
   *
   * <p>Calibration to physical units TBD.</p>
   **/
  public static final AX12Register AX12_PUNCH =
    new AX12Register(nextOrdinal++, "punch",
                     48, 2, 0, 1023, 1.0f/1023.0f, "", false);

  /**
   * <p><i>Virtual</i> register containing the error status of the dynamixel
   * (RO).</p>
   *
   * <p>The error status is a bitfield of the <code>E_*</code> bits.</p> 
   **/
  public static final AX12Register AX12_ERROR =
    new AX12Register(nextOrdinal++, "error", 54);

  private static final String svnid =
  "$Id: AX12Register.java 24 2008-05-02 22:08:41Z vona $";

  /** the array of {@link AX12Register}s, indexed by ordinal **/
  protected static AX12Register[] registers;

  /** total number of registers **/
  public static final int NUM_REGISTERS;

  /** first register in RAM **/
  public static final AXRegister FIRST_REGISTER;

  /** first register in RAM **/
  public static final AXRegister FIRST_RAM_REGISTER;

  static {

    ArrayList<AX12Register> regs = new ArrayList<AX12Register>();

    Field[] fields = AX12Register.class.getDeclaredFields();

    String prefix = DYNAMIXEL_TYPE.toUpperCase();

    try {
      for (Field f : fields)
        if ((f.getType() == AX12Register.class) &&
            (f.getName().startsWith(prefix)))
          regs.add((AX12Register) (f.get(null)));
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }

    Collections.sort(regs, new Comparator<AX12Register>() {
        public int compare(AX12Register r1, AX12Register r2) {

          //negative integer, zero, or a positive integer as the first argument
          //is less than, equal to, or greater than the second.

          if (r1.ordinal < r2.ordinal)
            return -1;
          else if (r1.ordinal > r2.ordinal)
            return +1;
          else
            return 0;
        }
      });

    NUM_REGISTERS = regs.size();
    registers = regs.toArray(new AX12Register[NUM_REGISTERS]);
    
    FIRST_REGISTER = registers[0];

    int ri = 0;
    for (; ri < registers.length; ri++) {
      if (registers[ri].startAddr >= RAM_START_ADDRESS) {
        break;
      }
    }
    
    FIRST_RAM_REGISTER = registers[ri];
  }

  /**
   * <p>Get the stiffness of an AX-12 actuator in units of kg*cm/deg.</p>
   *
   * <p>Calibrated by physical experiment.</p>
   *
   * @param complianceSlope the compliance slope register value which
   * determines the stiffness
   * @param voltage the operating voltage of the servo or NaN to use {@link
   * #NOMINAL_BATTERY_VOLTAGE}
   **/
  public static float getCalibratedStiffness(int complianceSlope,
                                             float voltage) {

    if (Float.isNaN(voltage))
      voltage = NOMINAL_BATTERY_VOLTAGE;

    float stiffnessAt1 = interp(voltage, 4.3f, 10.0f, 8.2f, 12.2f);
    float stiffnessAt254 = 0.3f;

    return interp(complianceSlope, stiffnessAt1, 1.0f, stiffnessAt254, 254.0f);
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
   * <p>Covers {@link #getCalibratedStiffness(int, float)}, uses {@link
   * #NOMINAL_BATTERY_VOLTAGE}.</p>
   **/
  public static float getCalibratedStiffness(int complianceSlope) {
    return getCalibratedStiffness(complianceSlope, Float.NaN);
  }

  /** writeable reg with all options **/
  protected AX12Register(int ordinal, String prettyName,
                         int startAddr, int width,
                         int min, int max,
                         boolean signMagnitude11Bit,
                         float naturalUnitsPerCount,
                         String naturalUnitsLabel,
                         boolean useNaturalUnitsByDefault) {
  super(ordinal, prettyName,
        startAddr, width,
        min, max,
        signMagnitude11Bit, naturalUnitsPerCount, naturalUnitsLabel,
        useNaturalUnitsByDefault);
  }

  /** read-only reg with all options **/
  protected AX12Register(int ordinal, String prettyName,
                         int startAddr, int width,
                         boolean signMagnitude11Bit,
                         float naturalUnitsPerCount,
                         String naturalUnitsLabel,
                         boolean useNaturalUnitsByDefault) {
  super(ordinal, prettyName,
        startAddr, width,
        signMagnitude11Bit, naturalUnitsPerCount, naturalUnitsLabel,
        useNaturalUnitsByDefault);
  }

  /** writeable unsigned reg **/
  protected AX12Register(int ordinal, String prettyName,
                         int startAddr, int width,
                         int min, int max,
                         float naturalUnitsPerCount,
                         String naturalUnitsLabel,
                         boolean useNaturalUnitsByDefault) {
    this(ordinal, prettyName,
         startAddr, width,
         min, max,
         false,
         naturalUnitsPerCount, naturalUnitsLabel, useNaturalUnitsByDefault);
  }

  /** writeable unsigned reg, natural units are counts **/
  protected AX12Register(int ordinal, String prettyName,
                         int startAddr, int width, int min, int max) {
    this(ordinal, prettyName, startAddr, width, min, max, 1.0f, "", false); 
  }

  /** writeable unsigned 1-byte reg, natural units are counts **/
  protected AX12Register(int ordinal, String prettyName,
                         int startAddr, int min, int max,
                         String naturalUnitsLabel,
                         boolean useNaturalUnitsByDefault) {
    this(ordinal, prettyName,
         startAddr, 1, min, max, 1.0f,
         naturalUnitsLabel, useNaturalUnitsByDefault);
  }

  /**
   * <p>Read-only unsigned reg (be careful of confusion with {@link
   * #AX12Register(int, String, int, int, int, String, boolean)}).</p>
   **/
  protected AX12Register(int ordinal, String prettyName,
                         int startAddr, int width,
                         float naturalUnitsPerCount, String naturalUnitsLabel,
                         boolean useNaturalUnitsByDefault) {
    this(ordinal, prettyName,
         startAddr, width, false,
         naturalUnitsPerCount, naturalUnitsLabel, useNaturalUnitsByDefault);
  }

  /** read-only unsigned reg, natural units are counts **/
  protected AX12Register(int ordinal, String prettyName,
                         int startAddr, int width) {
    this(ordinal, prettyName, startAddr, width, 1.0f, "", false);
  }

  /** writeable unsigned 1-byte reg **/
  protected AX12Register(int ordinal, String prettyName,
                         int startAddr,
                         int min, int max,
                         float naturalUnitsPerCount, String naturalUnitsLabel,
                         boolean useNaturalUnitsByDefault) {
    this(ordinal, prettyName,
         startAddr, 1, min, max,
         naturalUnitsPerCount, naturalUnitsLabel, useNaturalUnitsByDefault);
  }

  /** writeable unsigned 1-byte reg, natural units are coutns **/
  protected AX12Register(int ordinal, String prettyName,
                         int startAddr, int min, int max) {
    this(ordinal, prettyName, startAddr, min, max, 1.0f, "", false);
  }

  /** read-only unsigned 1-byte reg **/
  protected AX12Register(int ordinal, String prettyName,
                         int startAddr,
                         float naturalUnitsPerCount, String naturalUnitsLabel,
                         boolean useNaturalUnitsByDefault) {
    this(ordinal, prettyName,
         startAddr, 1,
         naturalUnitsPerCount, naturalUnitsLabel, useNaturalUnitsByDefault);
  }
  
  /** read-only unsigned 1-byte reg, natural units are counts **/
  protected AX12Register(int ordinal, String prettyName, int startAddr) {
    this(ordinal, prettyName, startAddr, 1.0f, "", false);
  }
  
  /** get the register with the given relative ordinal **/
  public AXRegister getRelativeRegister(int offset) {
    return registers[ordinal+offset];
  }

  /** get the total number of AX12Registers **/
  public int getNumRegisters() {
    return NUM_REGISTERS;
  }

  /** returns {@link #DYNAMIXEL_TYPE} **/
  protected String getDynamixelType() {
    return DYNAMIXEL_TYPE;
  }

  /** get the {@link #FIRST_REGISTER} **/
  public static AXRegister getFirstRegister() {
    return FIRST_REGISTER;
  }

  /** get the {@link #FIRST_RAM_REGISTER} **/
  public static AXRegister getFirstRAMRegister() {
    return FIRST_RAM_REGISTER;
  }

  /** get the AX12Register at the specified ordinal **/
  public static AXRegister getRegister(int ordinal) {
    return registers[ordinal];
  }

  /** get a copy of the ordered array of AX12Registers **/
  public static AX12Register[] getAllRegisters(AX12Register[] regs,
                                               int start) {
    if ((regs == null) || (regs.length < (start+NUM_REGISTERS)))
      regs = new AX12Register[start+NUM_REGISTERS];
    System.arraycopy(registers, 0, regs, start, NUM_REGISTERS);
    return regs;
  }

  /** covers {@link #getAllRegisters(AX12Register[], int)}, starts at 0 **/
  public static AX12Register[] getAllRegisters(AX12Register[] regs) {
    return getAllRegisters(regs, 0);
  }

  /** covers {@link #getAllRegisters(AX12Register[], int)}, always conses **/
  public static AX12Register[] getAllRegisters() {
    return getAllRegisters(null, 0);
  }
}
