/**
 * <p>Represents the Dynamixel AX-S1 registers in a high-level way.</p>
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
 * <p>Represents the Dynamixel AX-S1 registers in a high-level way.</p>
 *
 * <p>Copyright (C) 2008 Marsette A. Vona, III</p>
 *
 * @author Marsette (Marty) A. Vona, III
 **/
public class AXS1Register extends AXRegister {
  
  /** the Dynamixel type identifier **/
  public static final String DYNAMIXEL_TYPE = "AXS1";

  /** obstacle or luminosity detection flag **/
  public static final int F_RIGHT =  (1<<2);

  /** obstacle or luminosity detection flag **/
  public static final int F_CENTER = (1<<1);

  /** obstacle or luminosity detection flag **/
  public static final int F_LEFT =   (1<<0);

  /** special value for {@link #AXS1_BUZZER_TIME} **/
  public static final int BUZZER_START = 254;

  /** special value for {@link #AXS1_BUZZER_TIME} **/
  public static final int BUZZER_STOP = 0;

  /** special value for {@link #AXS1_BUZZER_TIME} **/
  public static final int BUZZER_MELODY = 255;

  /** min duration for timed {@link #AXS1_BUZZER_TIME} in units of 0.1s **/
  public static final int BUZZER_MIN_DURATION = 3;

  /** max duration for timed {@link #AXS1_BUZZER_TIME} in units of 0.1s **/
  public static final int BUZZER_MAX_DURATION = 50;

  /** next register ordinal during class init **/
  protected static int nextOrdinal = 0;

  /** dynamixel model number (RO) **/
  public static final AXS1Register AXS1_MODEL_NUMBER =
    new AXS1Register(nextOrdinal++, "model number", 0, 2);

  /** dynamixel firmware version (RO) **/
  public static final AXS1Register AXS1_FIRMWARE_VERSION =
    new AXS1Register(nextOrdinal++, "firmware version", 2);

  /** dynamixel ID (RW) **/
  public static final AXS1Register AXS1_ID =
    new AXS1Register(nextOrdinal++, "id", 3, 0, MAX_DYNAMIXEL_ID);
  
  /**
   * <p>Dynamixel Baud Rate (RW), natural units [kbits/sec].</p>
   *
   * <p>See table in dynamixel docs for standard rates.</p>
   *
   * <p>Changing this will probably make the CM-5 firmware unhappy.</p>
   **/
  public static final AXS1Register AXS1_BAUD_RATE =
    new AXS1Register(nextOrdinal++, "baud rate", 4, 0, 254, "kbps", true) {
    
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
  public static final AXS1Register AXS1_RETURN_DELAY_TIME =
    new AXS1Register(nextOrdinal++, "return delay time",
                     5, 0, 254, 2.0f, "us", true);
  
  /** limit temp (RW), natural units [deg Celcius] **/
  public static final AXS1Register AXS1_HIGHEST_LIMIT_TEMPERATURE =
    new AXS1Register(nextOrdinal++, "highest limit temperature",
                     11, 0, 150, 1.0f, "C", true);
  
  /** low limit voltage (RW), natural units [Volts] **/
  public static final AXS1Register AXS1_LOWEST_LIMIT_VOLTAGE =
    new AXS1Register(nextOrdinal++, "lowest limit voltage",
                     12, 50, 250, 0.1f, "V", true);
  
  /** high limit voltage (RW), natural units [Volts] **/
  public static final AXS1Register AXS1_HIGHEST_LIMIT_VOLTAGE =
    new AXS1Register(nextOrdinal++, "highest limit voltage",
                     13, 50, 250, 0.1f, "V", true);
  
  /**
   * <p>Dynamixel status return level (RW).</p>
   *
   * <p>0 means no return packets, 1 means return packets only for
   * READ_DATA, 2 means return packets always.</p>
   *
   * <p>Changing this will probably make the CM-5 firmware unhappy.</p>
   **/
  public static final AXS1Register AXS1_STATUS_RETURN_LEVEL =
    new AXS1Register(nextOrdinal++, "status return level", 16, 1, 0, 2);

  /**
   * <p>Default for {@link #AXS1_OBSTACLE_DETECTED_COMPARE} (RW).</p>
   **/
  public static final AXS1Register AXS1_OBSTACLE_DETECTED_COMPARE_VALUE =
    new AXS1Register(nextOrdinal++, "obstacle detected compare value",
                     20, 0, 255);

  /**
   * <p>Default for {@link #AXS1_LIGHT_DETECTED_COMPARE} (RW).</p>
   **/
  public static final AXS1Register AXS1_LIGHT_DETECTED_COMPARE_VALUE =
    new AXS1Register(nextOrdinal++, "light detected compare value",
                     21, 0, 255);

  /**
   * <p>Left IR distance reading (RO).</p>
   **/
  public static final AXS1Register AXS1_LEFT_IR_SENSOR_DATA =
    new AXS1Register(nextOrdinal++, "left ir sensor data", 26);

  /**
   * <p>Center IR distance reading (RO).</p>
   **/
  public static final AXS1Register AXS1_CENTER_IR_SENSOR_DATA =
    new AXS1Register(nextOrdinal++, "center ir sensor data", 27);

  /**
   * <p>Right IR distance reading (RO).</p>
   **/
  public static final AXS1Register AXS1_RIGHT_IR_SENSOR_DATA =
    new AXS1Register(nextOrdinal++, "right ir sensor data", 28);

  /**
   * <p>Left IR luminosity reading (RO).</p>
   **/
  public static final AXS1Register AXS1_LEFT_LUMINOSITY =
    new AXS1Register(nextOrdinal++, "left luminosity", 29);

  /**
   * <p>Center IR luminosity reading (RO).</p>
   **/
  public static final AXS1Register AXS1_CENTER_LUMINOSITY =
    new AXS1Register(nextOrdinal++, "center luminosity", 30);

  /**
   * <p>Right IR luminosity reading (RO).</p>
   **/
  public static final AXS1Register AXS1_RIGHT_LUMINOSITY =
    new AXS1Register(nextOrdinal++, "right luminosity", 31);

  /**
   * <p>IR obstacle detection bitmask, see {@link #F_RIGHT}, {@link #F_CENTER},
   * {@link #F_LEFT} (RO).</p>
   **/
  public static final AXS1Register AXS1_OBSTACLE_DETECTION_FLAG =
    new AXS1Register(nextOrdinal++, "obstacle detection flag", 32);

  /**
   * <p>IR luminosity detection bitmask, see {@link #F_RIGHT}, {@link
   * #F_CENTER}, {@link #F_LEFT} (RO).</p>
   **/
  public static final AXS1Register AXS1_LUMINOSITY_DETECTION_FLAG =
    new AXS1Register(nextOrdinal++, "luminosity detection flag", 33);

  /**
   * <p>Sound pressure (RW).</p>
   *
   * <p>~127-128 is quiet.</p>
   **/
  public static final AXS1Register AXS1_SOUND_DATA =
    new AXS1Register(nextOrdinal++, "sound data", 35, 0, 255);

  /**
   * <p>Loudest sound pressure heard (RW).</p>
   *
   * <p>Write to 0 to force update.</p>
   **/
  public static final AXS1Register AXS1_SOUND_DATA_MAX_HOLD =
    new AXS1Register(nextOrdinal++, "sound data max hold", 36, 0, 255);

  /**
   * <p>Recent count of loud sounds (RW).</p>
   **/
  public static final AXS1Register AXS1_SOUND_DETECTED_COUNT =
    new AXS1Register(nextOrdinal++, "sound detected count", 37, 0, 255);

  /**
   * <p>Timestamp of most recent loud sound detection (RW), natrual units
   * [ms].</p>
   **/
  public static final AXS1Register AXS1_SOUND_DETECTED_TIME =
    new AXS1Register(nextOrdinal++, "sound detected time",
                     38, 2, 0, 65536, false, 4.096f/65536.0f, "ms", true);

  /**
   * <p>Buzzer note (RW).</p>
   **/
  public static final AXS1Register AXS1_BUZZER_INDEX =
    new AXS1Register(nextOrdinal++, "buzzer index", 40, 0, 51);

  /**
   * <p>Buzzer duration, natural units [s] (RW).</p>
   *
   * <p>{@link #BUZZER_START}, {@link #BUZZER_STOP}, {@link #BUZZER_MELODY},
   * {@link #BUZZER_MIN_DURATION}, {@link #BUZZER_MAX_DURATION} are special
   * values.</p>
   **/
  public static final AXS1Register AXS1_BUZZER_TIME =
    new AXS1Register(nextOrdinal++, "buzzer time",
                     41, 0, 255, 0.1f, "s", true);

  /** Current voltage (RO), natural units [Volts] **/
  public static final AXS1Register AXS1_PRESENT_VOLTAGE =
    new AXS1Register(nextOrdinal++, "present voltage", 42, 0.1f, "V", true);

  /** Current temperature (RO), natural units [degrees Celcius] **/
  public static final AXS1Register AXS1_PRESENT_TEMPERATURE =
    new AXS1Register(nextOrdinal++, "present temperature",
                     43, 1.0f, "C", true);

  /** Whether there is a registered instruction pending (RW), boolean **/
  public static final AXS1Register AXS1_REGISTERED_INSTRUCTION =
    new AXS1Register(nextOrdinal++, "registered instruction", 44, 0, 1);

  /** Remocon data is ready when value is 2 (RO). **/
  public static final AXS1Register AXS1_IR_REMOCON_ARRIVED =
    new AXS1Register(nextOrdinal++, "ir remocon arrived", 46);

  /**
   * <p>This appears to be an AX-12 vestige.</p>
   **/
  public static final AXS1Register AXS1_LOCK =
    new AXS1Register(nextOrdinal++, "lock", 47, 0, 1);

  /**
   * <p>Value of received remocon data (RO).</p>
   *
   * <p>{@link #AXS1_IR_REMOCON_ARRIVED} == 2 indicates that data is ready.</p>
   *
   * <p>This will reset to 0 once read.</p>
   **/
  public static final AXS1Register AXS1_IR_REMOCON_RX_DATA =
    new AXS1Register(nextOrdinal++, "ir remocon rx data", 48, 2);

  /**
   * <p>Remocon data to send (RW).</p>
   *
   * <p>Transmission is triggered on register write.</p>
   **/
  public static final AXS1Register AXS1_IR_REMOCON_TX_DATA =
    new AXS1Register(nextOrdinal++, "ir remocon tx data", 50, 2, 0, 65536);

  /**
   * <p>IR obstacle detection threshold (RW).</p>
   *
   * <p>Initialized from {@link #AXS1_OBSTACLE_DETECTED_COMPARE_VALUE}.</p>
   **/
  public static final AXS1Register AXS1_OBSTACLE_DETECTED_COMPARE =
    new AXS1Register(nextOrdinal++, "obstacle detected compare",
                     52, 0, 255);

  /**
   * <p>IR detection threshold (RW).</p>
   *
   * <p>Initialized from {@link #AXS1_LIGHT_DETECTED_COMPARE_VALUE}.</p>
   **/
  public static final AXS1Register AXS1_LIGHT_DETECTED_COMPARE =
    new AXS1Register(nextOrdinal++, "light detected compare",
                     53, 0, 255);

  /**
   * <p><i>Virtual</i> register containing the error status of the dynamixel
   * (RO).</p>
   *
   * <p>The error status is a bitfield of the <code>E_*</code> bits.</p> 
   **/
  public static final AXS1Register AXS1_ERROR =
    new AXS1Register(nextOrdinal++, "error", 54);

  private static final String svnid =
  "$Id: AXS1Register.java 25 2008-05-02 22:11:43Z vona $";

  /** the array of {@link AXS1Register}s, indexed by ordinal **/
  protected static AXS1Register[] registers;

  /** total number of registers **/
  public static final int NUM_REGISTERS;

  /** first register in RAM **/
  public static final AXRegister FIRST_REGISTER;

  /** first register in RAM **/
  public static final AXRegister FIRST_RAM_REGISTER;

  static {

    ArrayList<AXS1Register> regs = new ArrayList<AXS1Register>();

    Field[] fields = AXS1Register.class.getDeclaredFields();

    String prefix = DYNAMIXEL_TYPE.toUpperCase();

    try {
      for (Field f : fields)
        if ((f.getType() == AXS1Register.class) &&
            (f.getName().startsWith(prefix)))
          regs.add((AXS1Register) (f.get(null)));
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }

    Collections.sort(regs, new Comparator<AXS1Register>() {
        public int compare(AXS1Register r1, AXS1Register r2) {

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
    registers = regs.toArray(new AXS1Register[NUM_REGISTERS]);
    
    FIRST_REGISTER = registers[0];

    int ri = 0;
    for (; ri < registers.length; ri++) {
      if (registers[ri].startAddr >= RAM_START_ADDRESS) {
        break;
      }
    }
    
    FIRST_RAM_REGISTER = registers[ri];
  }

  /** writeable reg with all options **/
  protected AXS1Register(int ordinal, String prettyName,
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
  protected AXS1Register(int ordinal, String prettyName,
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
  protected AXS1Register(int ordinal, String prettyName,
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
  protected AXS1Register(int ordinal, String prettyName,
                         int startAddr, int width, int min, int max) {
    this(ordinal, prettyName, startAddr, width, min, max, 1.0f, "", false); 
  }

  /** writeable unsigned 1-byte reg, natural units are counts **/
  protected AXS1Register(int ordinal, String prettyName,
                         int startAddr, int min, int max,
                         String naturalUnitsLabel,
                         boolean useNaturalUnitsByDefault) {
    this(ordinal, prettyName,
         startAddr, 1, min, max, 1.0f,
         naturalUnitsLabel, useNaturalUnitsByDefault);
  }

  /**
   * <p>Read-only unsigned reg (be careful of confusion with {@link
   * #AXS1Register(int, String, int, int, int, String, boolean)}).</p>
   **/
  protected AXS1Register(int ordinal, String prettyName,
                         int startAddr, int width,
                         float naturalUnitsPerCount, String naturalUnitsLabel,
                         boolean useNaturalUnitsByDefault) {
    this(ordinal, prettyName,
         startAddr, width, false,
         naturalUnitsPerCount, naturalUnitsLabel, useNaturalUnitsByDefault);
  }

  /** read-only unsigned reg, natural units are counts **/
  protected AXS1Register(int ordinal, String prettyName,
                         int startAddr, int width) {
    this(ordinal, prettyName, startAddr, width, 1.0f, "", false);
  }

  /** writeable unsigned 1-byte reg **/
  protected AXS1Register(int ordinal, String prettyName,
                         int startAddr,
                         int min, int max,
                         float naturalUnitsPerCount, String naturalUnitsLabel,
                         boolean useNaturalUnitsByDefault) {
    this(ordinal, prettyName,
         startAddr, 1, min, max,
         naturalUnitsPerCount, naturalUnitsLabel, useNaturalUnitsByDefault);
  }

  /** writeable unsigned 1-byte reg, natural units are coutns **/
  protected AXS1Register(int ordinal, String prettyName,
                         int startAddr, int min, int max) {
    this(ordinal, prettyName, startAddr, min, max, 1.0f, "", false);
  }

  /** read-only unsigned 1-byte reg **/
  protected AXS1Register(int ordinal, String prettyName,
                         int startAddr,
                         float naturalUnitsPerCount, String naturalUnitsLabel,
                         boolean useNaturalUnitsByDefault) {
    this(ordinal, prettyName,
         startAddr, 1,
         naturalUnitsPerCount, naturalUnitsLabel, useNaturalUnitsByDefault);
  }
  
  /** read-only unsigned 1-byte reg, natural units are counts **/
  protected AXS1Register(int ordinal, String prettyName, int startAddr) {
    this(ordinal, prettyName, startAddr, 1.0f, "", false);
  }
  
  /** get the register with the given relative ordinal **/
  public AXRegister getRelativeRegister(int offset) {
    return registers[ordinal+offset];
  }

  /** get the total number of AXS1Registers **/
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

  /** get the AXS1Register at the specified ordinal **/
  public static AXRegister getRegister(int ordinal) {
    return registers[ordinal];
  }

  /** get a copy of the ordered array of AXS1Registers **/
  public static AXS1Register[] getAllRegisters(AXS1Register[] regs,
                                               int start) {
    if ((regs == null) || (regs.length < (start+NUM_REGISTERS)))
      regs = new AXS1Register[start+NUM_REGISTERS];
    System.arraycopy(registers, 0, regs, start, NUM_REGISTERS);
    return regs;
  }

  /** covers {@link #getAllRegisters(AXS1Register[], int)}, starts at 0 **/
  public static AXS1Register[] getAllRegisters(AXS1Register[] regs) {
    return getAllRegisters(regs, 0);
  }

  /** covers {@link #getAllRegisters(AXS1Register[], int)}, always conses **/
  public static AXS1Register[] getAllRegisters() {
    return getAllRegisters(null, 0);
  }
}
