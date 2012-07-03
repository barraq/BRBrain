/**
 * <p>Bioloid Remote Brain library.</p>
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

import static brbrain.AX12Register.*;

import gnu.io.*;
import java.util.*;
import java.io.*;

/**
 * <p>Bioloid Remote Brain library.</p>
 *
 * <p>This is a Java host library, which, combined with custom C firmware,
 * implements <i>remote brain</i> functionality for the <a
 * href="http://www.robotis.com/html/main.php">Robotis</a> <a
 * href="http://www.robotis.com/html/sub.php?sub=2&menu=1">Bioloid</a>.
 *
 * <p>High level documentation and instructions for installing the custom <a
 * href="http://www.robotis.com/html/sub.php?sub=2&menu=3">CM-5</a> firmware
 * are available from the <a
 * href="http://www.mit.edu/~vona/BRBrain/BRBrain-info.html">BRBrain
 * homepage</a>.</p>
 *
 * <h2>Java Code Runs Off-Board</h2>
 *
 * <p>This system allows you to write Java code to control Bioloid
 * constructions.  The Java code always runs on a host PC and communicates with
 * the CM-5 on your Bioloid hardware via a serial link.  Sensor and actuator
 * data is exchanged in <i>near</i> real time between the CM-5 and a host
 * workstation, allowing high-level control code to run (and be debugged)
 * directly on the workstation.</p>
 *
 * <p>Again, BRBrain does not allow you to run Java code directly on the
 * CM-5. You run Java code on a host PC that communicates with the CM-5 over
 * either the standard RS232 serial cable or an optional non-standard bluetooth
 * link that you can add to your CM-5 <a
 * href="http://www.mit.edu/~vona/BRBrain/BRBrain-info.html#BlueSMiRF">as
 * described here</a>.</p>
 *
 * <h2>Establishing Communications</h2>
 *
 * <p>Several constructors are provided to allow the actual communication link
 * between the host PC and the CM-5 to be implemented in different ways:<ul>
 *
 * <li>over a serial port accessed as a regular file</li>
 * <li>over a serial port accessed with the RXTX library</li>
 *
 * </ul>The RXTX codepath has the advantage of internally setting the
 * communications parameters (115.2kbps, 8N1, no flow control).  The regular
 * file codepath will require the serial port to be externally configured
 * (e.g. using <tt>stty</tt> on Linux).  The serial port must be 8-bit clean.
 * This is automatically guaranteed when using RXTX.  For the regular file
 * codepath it is part of the external port configuration. For example, on
 * Linux the terminal driver associated with the port device needs to be taken
 * out of "cooked" mode (the default) and placed in "raw" mode (again using
 * <tt>stty</tt>).</p>
 *
 * <p>{@link #listPorts} may be invoked to get a list of the port names that
 * the RXTX library believes to be available.</p>
 *
 * <h2>Talking to the Bioloid</h2>
 *
 * <p>The CM-5 running the custom firmware acts as a relay between the register
 * banks of the connected network of Dynamixel AX modules and the host PC.  The
 * {@link AX12Register} and {@link AXS1Register} classes enumerate the
 * Dynamixel registers that you can access.  Refer to the <a
 * href="http://www.robotis.com/hb/hisboard.php?id=bbs1_1_eng&group_no=1&category=&bd_no=27&bd_step=0&bd_group=19&bd_float=1900&mode=view&position=3&search=&find=">respective</a>
 * <a
 * href="http://www.robotis.com/hb/hisboard.php?id=bbs1_1_eng&group_no=1&category=&bd_no=31&bd_step=0&bd_group=23&bd_float=2300&mode=view&position=3&search=&find=">manuals</a>
 * for Dynamixel register descriptions.</p>
 *
 * <p>For performance, you first inform BRBrain of the set of particular
 * registers on particular Dynamixels in which you are interested.  Such a
 * specificiation is called a <i>format</i>; you specify separate formats for
 * data that you are writing from the host PC to the Dynamixels and data that
 * you are reading from the Dynamixels to the host PC with the {@link
 * #setWriteFormat} and {@link #setReadFormat} APIs, respectively.</p>
 *
 * <p>Once you have specified your read and write formats, you read and write
 * blocks of data from and to the Dynamixels by calling the {@link
 * #read(int[])}, {@link #read(float[])}, and {@link #write(int[])} and {@link
 * #write(float[])} APIs.  The integer versions of these APIs read and write
 * raw integer values from/to the Dynamixels; the float versions automatically
 * convert <i>natural units</i> for the register from/to the raw register
 * values.  For example, the natural units for the {@link
 * AX12Register#AX12_PRESENT_POSITION} register are degrees.</p>
 *
 * <p>To keep format specification simple, you can only specify one contiguous
 * block of registers to read and a separate contiguous block of registers to
 * write per Dynamixel.  The blocks can be different on different Dynamixels,
 * and you can specify an arbitrary set of Dynamixel IDs to which you will be
 * communicating; this set need not include all of the Dynamixels that are
 * actually connected to your CM-5.  On a write, all registers are written in
 * synchrony with the REG_WRITE/ACTION instructions supported by the
 * Dynamixels.</p>
 *
 * <p>A cache of most-recently read data is maintained and may be queried with
 * {@link #getCachedValue}.</p>
 *
 * <p>Every method which involves actual communication with the CM-5 is
 * synchronized.  Synchronize on the BRBrain object to perform multiple
 * communications in a single uninterrupted transaction.</p>
 *
 * <h2>Usage Example</h2>
 *
 * <p>First, get a list of the available serial ports:
 *
 * <code><pre>
 * BRBrain.listPorts();
 * </code></pre>
 *
 * The output will depend on your operating system and the serial port hardware
 * actually available on your computer.  You'll need to interpret it and figure
 * out the port to which your CM-5 is actually connected.  For example, on
 * Linux this might be {@code /dev/ttyS0} or {@code /dev/ttyUSB0} if the CM-5
 * is connected to the first built-in serial port or to the first USB-to-serial
 * adapter, respectively.  On windows it may be something like {@code
 * COM1}.</p>
 *
 * <p>Next, create a BRBrain object.  This establishes communications:
 *
 * <code><pre>
 * BRBrain b = new BRBrain("/dev/ttyS0"); //or whatever port you selected
 * </pre></code></p>
 *
 * <p>Now set read and write formats.  Let's say you want to read the five
 * AX-12 registers starting at {@link AX12Register#AX12_PRESENT_POSITION}
 * (these also include {@link AX12Register#AX12_PRESENT_SPEED}, {@link
 * AX12Register#AX12_PRESENT_LOAD}, {@link AX12Register#AX12_PRESENT_VOLTAGE},
 * and {@link AX12Register#AX12_PRESENT_TEMPERATURE}), write only the {@link
 * AX12Register#AX12_GOAL_POSITION}, and that you want to do this on two AX-12s
 * with IDs 3 and 7:
 *
 * <code><pre>
 * int status =
 *   b.setReadFormat(new int[] {3, 7}, //id
 *                   new AXRegister{AX12Register.AX12_PRESENT_POSITION, //start
 *                                  AX12Register.AX12_PRESENT_POSITION},
 *                   new int{5, 5}); //num
 * b.verifyStatus(status, "set read format");
 *
 * status = 
 *   b.setWriteFormat(new int[] {3, 7}, //id
 *                   new AXRegister{AX12Register.AX12_GOAL_POSITION,  //start
 *                                  AX12Register.AX12_GOAL_POSITION},
 *                   new int{1, 1}); //num
 * b.verifyStatus(status, "set write format");
 * </pre></code></p>
 *
 * <p>Finally, you can set some goal positions and then monitor the state of
 * your two servos:
 *
 * <code><pre>
 * //sets servo 3 to goal position 100 degrees, servo 7 to 200 degrees
 * status = b.write(new float[] {100.0f, 200.0f});
 * b.verifyStatus(status, "set goal positions");
 *
 * float[] state = new float[2*5];
 * status = b.read(state);
 * b.verifyStatus(status, "read state");
 * </pre></code></p>
 *
 * <h2>Extra Utilities</h2>
 *
 * <p>You can use the BRBrain class alone (well, {@link AXRegister} and its
 * subclasses are also required) in your programs.  Some additional utilities
 * are also included in this package which can help you implement recording and
 * playing back motion sequences, or just displaying a GUI with little
 * read/write boxes for all the Dynamixel register values:<ul>
 *
 * <li>a {@link Pose} represents the values of a set of registers for a set of
 * Dynamixels, including provisions to read/write the data both from/to the
 * hardware and from/to output/input streams</li>
 *
 * <li>a {@link PoseSequence} is an ordered sequence of {@link Pose}s</li>
 *
 * <li>a {@link PoseGUI} displays an array of read/write boxes that corresponds
 * to a {@link Pose}</li>
 *
 * <li>a {@link PoseSequenceGUI} similarly allows you to edit a {@link
 * PoseSequence}</li>
 *
 * <li>a {@link SequencePlaybackController} allows you to play back a {@link
 * PoseSequence} on the Bioloid hardware, possibly making arbitrary control
 * flow decisions</li>
 *
 * <li>{@link BRBrainShell} implements a REPL-style shell from which you can
 * access most of the above functionality from a higher-level and more dynamic
 * Scheme-language environment</li>
 *
 * </ul></p>
 *
 * <h2>Under the Hood</h2>
 *
 * <p>This section documents the variable-length packet communication protocol
 * that BRBrain uses to communicate with the custom firmware.  If you are just
 * using BRBrain to write Java control programs for your Bioloid, and you are
 * not intending to modify the firmware, you can skip this section.</p>
 *
 * <p>All packets sent in either direction follow the basic format<pre>
 *
 * instruction
 * [data...]
 * checksum
 * 
 * </pre>where "instruction" is one byte long, data is zero or more bytes, and
 * checksum is computed as the bitwise inverse of the unsigned byte sum of the
 * data and the instruction.</p>
 *
 * <p>The following instructions are used for packets from the host to the
 * CM-5:<ul>
 *
 * <li>0xF0: {@link Instruction#I_PING} either the CM-5 itself or one of the
 * connected dynamixels.  Data is one byte with either the address of the
 * dynaxmixel to ping in the range [0, {@link AXRegister#MAX_DYNAMIXEL_ID}] or
 * 255 to ping the CM-5.  The CM-5 responds with a {@link Instruction#I_STATUS}
 * packet.</li>
 *
 * <li>0xF1: {@link Instruction#I_SET_READ_FORMAT}.  Data is in the following
 * form:<pre>
 *
 * num dynamixels in the range [0, {@link #MAX_DYNAMIXELS}]
 * dynamixel 0 id
 * dynamixel 0 start byte
 * dynamixel 0 num bytes
 * dynamixel 1 id
 * dynamixel 1 start byte
 * dynamixel 1 num bytes
 * ...
 * dynamixel (n-1) id
 * dynamixel (n-1) start byte
 * dynamixel (n-1) num bytes
 * 
 * </pre>The CM-5 responds with a {@link Instruction#I_STATUS} packet. If any
 * dynamixel id is outside the closed interval [0, {@link
 * AXRegister#MAX_DYNAMIXEL_ID}], or if the span of registers for any given
 * dynamixel extends beyond the last register, the CM-5 will respond with an
 * error code and the read format will be indeterminate until it is
 * successfully re-set.</li>
 *
 * <li>0xF2: {@link Instruction#I_SET_WRITE_FORMAT}.  Data is in the following
 * form:<pre>
 *
 * num dynamixels in the range [0, {@link #MAX_DYNAMIXELS}]
 * dynamixel 0 id
 * dynamixel 0 start byte
 * dynamixel 0 num bytes
 * ...
 * dynamixel (<i>n</i>-1) id
 * dynamixel (<i>n</i>-1) start byte
 * dynamixel (<i>n</i>-1) num bytes
 * 
 * </pre>The CM-5 responds with a {@link Instruction#I_STATUS} packet. If any
 * dynamixel id is outside the closed interval [0, {@link
 * AXRegister#MAX_DYNAMIXEL_ID}], or if the span of registers for any given
 * dynamixel extends beyond the last register or includes any which are
 * read-only, the CM-5 will respond with an error code and the write format
 * will be indeterminate until it is successfully re-set.</li>
 *
 * <li>0xF3: {@link Instruction#I_READ_DATA}.  No data is sent.  The CM-5
 * responds with a {@link Instruction#I_DATA} packet.</li>
 *
 * <li>0xF4: {@link Instruction#I_WRITE_DATA}.  Data is in the following
 * form:<pre>
 *
 * dynamixel 0 data bytes
 * ...
 * dynamixel (<i>n</i>-1) data bytes
 *
 * </pre>Where <i>n</i> is the number of dynamixels specified in the most
 * recent {@link Instruction#I_SET_WRITE_FORMAT}, and for each dynamixel the
 * number of data bytes sent is again specified by the most recent {@link
 * Instruction#I_SET_WRITE_FORMAT}.  The CM-5 responds with a {@link
 * Instruction#I_STATUS} packet.</li>
 *
 * </ul></p>
 *
 * <p>The following instructions are used for packets from the CM-5 to the
 * host:<ul>
 *
 * <li>0xFA: {@link Instruction#I_STATUS}.  Data is five bytes.  The first byte
 * is zero if the previous operation succeeded, or a nonzero errorcode if there
 * was a failure.  The second byte is the total number of dynamixel bus packet
 * retries that were incurred during execution of the previous operation.  The
 * remaining three bytes are the ADC channel raw values as of the last read
 * (reads occur about every 5s) in the order pos, neg, therm.</li>
 *
 * <li>0xFB: {@link Instruction#I_DATA}.  Data is in the following form:<pre>
 *
 * dynamixel 0 data bytes
 * ...
 * dynamixel (<i>n</i>-1) data bytes
 * status byte
 * retry count byte
 * ADC pos channel
 * ADC neg channel
 * ADC therm channel
 *
 * </pre>Where <i>n</i> is the number of dynamixels specified in the most
 * recent {@link Instruction#I_SET_READ_FORMAT}, and for each dynamixel the
 * number of data bytes sent is again specified by the most recent {@link
 * Instruction#I_SET_READ_FORMAT}.  If there are errors acquiring any or all
 * data bytes, the bytes are still sent by the CM-5 to the host but with value
 * 0xFF.  The final status, retry count, and ADC bytes are always sent and have
 * the same semantics as the payload of an {@link Instruction#I_STATUS}
 * packet.</li>
 *
 * </ul></p>
 *
 * <p>Copyright (C) 2008 Marsette A. Vona, III</p>
 *
 * @author Marsette (Marty) A. Vona, III
 **/
public class BRBrain {

  private static final String svnid =
  "$Id: BRBrain.java 28 2008-09-29 16:24:46Z vona $";

  /** whether to show protocol debug messages **/
  public boolean debug = false;

  /** whether to enable debug log of failed recv packets **/
  public boolean enableRecvPacketDebug = true;

  /** whether to log to {@link #recvPacketDebugBuffer} **/
  protected boolean recvPacketDebug = enableRecvPacketDebug;

  /** debug log for recv packets **/
  protected List<Byte> recvPacketDebugBuffer = new ArrayList<Byte>();

  /** a cached register value **/
  public class CachedValue {

    /** the cached value **/
    protected int value;

    /** the last update nanotime **/
    protected long lastUpdateNS;

    /** get the cached value **/
    public int getValue() {
      return value;
    }

    /** get the last update nanotime **/
    public long getLastUpdateNS() {
      return lastUpdateNS;
    }
  }

  /** cache of most recently read data **/
  protected Map<AXRegister, Map<Integer, CachedValue>> cache =
    new HashMap<AXRegister, Map<Integer, CachedValue>>();

  /** RXTX port owner name **/
  public static final String RXTX_PORT_OWNER_NAME = "BRBrain";

  /** timeout in ms to wait to open a port with RXTX **/
  public static final int RXTX_OPEN_TIMEOUT_MS = 1000;

  /** default receive timout in ms **/
  public static final int DEF_TIMEOUT_MS = 1000;

  /** receive poll time in ms **/
  public static final int RECV_POLL_MS = 1;

  /** ms to wait before draining recv buffer in {@link #recover} **/
  public static final int RECOVER_MS = 500;

  /** default baudrate for RXTX **/
  public static final int RXTX_DEF_BAUD_RATE = 115200;

  /**
   * NOTE: do not reference any RXTX classes in static initializers to avoid
   * runtime NoClassDefFoundError when using the ADMIN jar without RXTX on
   * systems with no RXTX jar.
   **/

  /** CM-5 status bit **/
  public static final int S_PC_TIMEOUT =                  (1<<0);

  /** CM-5 status bit **/
  public static final int S_DYNAMIXEL_TIMEOUT =           (1<<1);

  /** CM-5 status bit **/
  public static final int S_INVALID_PC_COMMAND =          (1<<2);

  /** CM-5 status bit **/
  public static final int S_INVALID_DYNAMIXEL_RESPONSE =  (1<<3);

  /** CM-5 status bit **/
  public static final int S_PC_RX_OVERFLOW =              (1<<4);

  /** CM-5 status bit **/
  public static final int S_DYNAMIXEL_RX_OVERFLOW =       (1<<5);

  /** CM-5 status bit **/
  public static final int S_PC_CHECKSUM_ERROR =           (1<<6);

  /** CM-5 status bit **/
  public static final int S_DYNAMIXEL_CHECKSUM_ERROR =    (1<<7);

  /** maximum number of dynamixels in a format **/
  public static final int MAX_DYNAMIXELS = 32;

  /** read format **/
  protected static final int F_READ = 0;

  /** write format **/
  protected static final int F_WRITE = 1;

  /** set format instructions **/
  protected static final Instruction[] FMT_INSTRUCTION =
    new Instruction[] {
    Instruction.I_SET_READ_FORMAT,
    Instruction.I_SET_WRITE_FORMAT
  };

  /** baudrate at which the CM-5 bootloader likes to talk **/
  public static final int CM5_BOOTLOADER_BAUDRATE = 57600;

  /** timeout for response from the CM-5 during {@link #flashCM5} **/
  public static final double FLASH_TIMEOUT_MS = 10e3;

  /** delay between stages of the {@link #flashCM5} procedure **/
  public static final int FLASH_DELAY_MS = 1000;

  /** CM-5 bootloader welcome message **/
  public static final String CM5_BOOTLOADER_MSG =
    "SYSTEM O.K. (CM5 Boot loader";

  /** ADC channel connected to the positive battery terminal **/
  public static final int CHANNEL_POS = 0;

  /** ADC channel connected to the negative battery terminal **/
  public static final int CHANNEL_NEG = 1;

  /** ADC channel connected to the thermistor **/
  public static final int CHANNEL_THERM = 2;
 
  /** most recently read raw 8-bit ADC values **/
  protected int adcValue[] = new int[3];

  /**
   * <p>Compose a human-readable string of CM-5 status from a CM-5 status
   * byte.</p>
   *
   * @param status the bitpacked status value
   * @param buf the buffer to append, or null to make one
   *
   * @return the buffer to which the string was appended
   **/
  public static final StringBuffer statusToString(int status,
                                                  StringBuffer buf) {
    if (buf == null)
      buf = new StringBuffer();

    int n = 0;

    if ((status & S_PC_TIMEOUT) != 0) {
      if (n++ > 0)
        buf.append(", ");
      buf.append("PC_TIMEOUT");
    }

    if ((status & S_DYNAMIXEL_TIMEOUT) != 0) {
      if (n++ > 0)
        buf.append(", ");
      buf.append("DYNAMIXEL_TIMEOUT");
    }

    if ((status & S_INVALID_PC_COMMAND) != 0) {
      if (n++ > 0)
        buf.append(", ");
      buf.append("INVALID_PC_COMMAND");
    }
    
    if ((status & S_INVALID_DYNAMIXEL_RESPONSE) != 0) {
      if (n++ > 0)
        buf.append(", ");
      buf.append("INVALID_DYNAMIXEL_RESPONSE");
    }
    
    if ((status & S_PC_RX_OVERFLOW) != 0) {
      if (n++ > 0)
        buf.append(", ");
      buf.append("PC_RX_OVERFLOW");
    }
    
    if ((status & S_DYNAMIXEL_RX_OVERFLOW) != 0) {
      if (n++ > 0)
        buf.append(", ");
      buf.append("DYNAMIXEL_RX_OVERFLOW");
    }

    if ((status & S_PC_CHECKSUM_ERROR) != 0) {
      if (n++ > 0)
        buf.append(", ");
      buf.append("PC_CHECKSUM_ERROR");
    }

    if ((status & S_DYNAMIXEL_CHECKSUM_ERROR) != 0) {
      if (n++ > 0)
        buf.append(", ");
      buf.append("DYNAMIXEL_CHECKSUM_ERROR");
    }

    if ((status&0xff00) != 0) {
      buf.append(" (");
      buf.append(Integer.toString((status&0xff00)>>8));
      buf.append(" dynamixel retries)");
    }

    return buf;
  }

  /** Covers {@link #statusToString(int, StringBuffer)} **/
  public static final String statusToString(int status) {
    return statusToString(status, null).toString();
  }

  /**
   * <p>Convenience method to query the list of available ports according to
   * RXTX.</p>
   *
   * @param printStream the stream on which to print the list,
   * e.g. <code>System.out</code>
   *
   * @return printStream
   **/
  public static PrintStream listPorts(PrintStream printStream) {
   
    //do this here as it prints stuff
    Enumeration e = CommPortIdentifier.getPortIdentifiers();

    printStream.println("\nAvailable Ports:\n");

    while(e.hasMoreElements()) {
      
      CommPortIdentifier id = (CommPortIdentifier) (e.nextElement());

      printStream.println("name: " + id.getName());
      printStream.println("  type: " +
                          ((id.getPortType()==CommPortIdentifier.PORT_SERIAL) ?
                           "serial" : "parallel"));
      printStream.println("  current owner: " + id.getCurrentOwner());
    }

    return printStream;
  }

  /** covers {@link #listPorts(PrintStream)}, uses System.out **/
  public static PrintStream listPorts() {
    return listPorts(System.out);
  }

  /**
   * <p>{@link BRBrain} protocol packet instructions, see {@link BRBrain} class
   * header doc for details.</p>
   **/
  public static enum Instruction {

    I_PING(0xf0),
    I_SET_READ_FORMAT(0xf1),
    I_SET_WRITE_FORMAT(0xf2),
    I_READ_DATA(0xf3),
    I_WRITE_DATA(0xf4),
    I_STATUS(0xfa),
    I_DATA(0xfb);

    public final int code;

    Instruction(int code) {
      this.code = code;
    }
  }

  /**
   * <p>Setup an BRBrain talking to a CM-5 on the specified serial port at the
   * specified baud rate.</p>
   *
   * <p>Note that this method does no transmission on the port, and does not
   * verify the presence of a CM-5 running the correct firmware.</p>
   *
   * <p>A {@link #recover} is performed.</p>
   *
   * @param portName an RXTX serial port name, see {@link #listPorts} 
   * @param baudRate the baud rate in bits per second
   *
   * @exception IOException if there was a problem opening the port
   * @exception InterruptedException if interrupted during {@link #recover}
   * @exception IllegalStateException if the specified port is not recognized
   * by RXTX as a serial port, or if RXTX silently failed to open the port 
   **/
  public BRBrain(String portName, int baudRate)
    throws IOException, InterruptedException {

    if (portName == null)
      throw new IllegalArgumentException("null port name");

    try {

      CommPortIdentifier id = CommPortIdentifier.getPortIdentifier(portName);
      CommPort port = id.open(RXTX_PORT_OWNER_NAME, RXTX_OPEN_TIMEOUT_MS);

      if (!(port instanceof SerialPort))
        throw new IllegalStateException("RXTX port \"" + portName +
                                        "\" is not a SerialPort");

      serialPort = (SerialPort) port;

      serialPort.disableReceiveFraming();
      serialPort.disableReceiveThreshold();
      serialPort.disableReceiveTimeout();

      setBaudRate(baudRate);

      serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);

      toCM5 = serialPort.getOutputStream();
      fromCM5 = serialPort.getInputStream();

      Runtime.getRuntime().addShutdownHook(new Thread() {
          public void run() {
            close();
          } });
      
/*

  NOTE: Do not directly reference RXTX exceptions, by doing so you would
  require the RXTX jar to be present at runtime or a NoClassDefFoundError is
  thrown.  Instead a generic Exception clause handles these exceptions below.

    } catch (NoSuchPortException e) {

      IOException ioe = new IOException("no such port "+portName);
      ioe.initCause(e);
      throw ioe;

    } catch (PortInUseException e) {

      IOException ioe = new IOException("port "+portName+" in use");
      ioe.initCause(e);
      throw ioe;

    } catch (UnsupportedCommOperationException e) {

      IOException ioe = new IOException("unsupported comm operation");
      ioe.initCause(e);
      throw ioe;

    } 
*/

    } catch (Exception e) {

      IOException ioe = new IOException("error opening RXTX port");
      ioe.initCause(e);

      throw ioe;
    }

    if (fromCM5 == null)
      throw new IllegalStateException("RXTX failed to provide input stream");

    if (toCM5 == null)
      throw new IllegalStateException("RXTX failed to provide output stream");

    recover();
  }

  /** covers {@link #BRBrain(String, int)} uses {@link #RXTX_DEF_BAUD_RATE} **/
  public BRBrain(String portName)
    throws IOException, InterruptedException {
    this(portName, RXTX_DEF_BAUD_RATE);
  }

  /**
   * <p>Same as {@link #BRBrain(String)} but connect to CM-5 via a file instead
   * of with RXTX.</p>
   **/
  public BRBrain(File port) throws IOException, InterruptedException {

    if (port == null)
      throw new IllegalArgumentException("null port");
    
    toCM5 = new FileOutputStream(port);
    fromCM5 = new FileInputStream(port);
  }
 
  /** waits {@link #RECOVER_MS} and then drains recv buf **/
  public synchronized void recover() throws IOException, InterruptedException {
    Thread.sleep(RECOVER_MS);
    drainFromCM5();
    checksum = 0;
  }

  /** drain {@link #fromCM5} **/
  protected synchronized void drainFromCM5()
    throws IOException, InterruptedException {
    while (fromCM5.available() != 0)
      recvByte(false);
  }

  /** attempt to set the baud rate, works only if using RXTX **/
  protected synchronized void setBaudRate(int baudRate) throws IOException {

    if (serialPort == null)
      throw new IllegalStateException("no serial port -- not using RXTX?");

    try {
      //NOTE: Do not make the databits, stopbits, parity, or flowcontrol
      //settings static class constants.  By doing so you would require the
      //RXTX jar to be present at runtime or a NoClassDefFoundError is thrown.
      serialPort.setSerialPortParams(baudRate,
                                     SerialPort.DATABITS_8,
                                     SerialPort.STOPBITS_1,
                                     SerialPort.PARITY_NONE);
    } catch (UnsupportedCommOperationException e) {
      IOException ioe = new IOException("unsupported comm operation");
      ioe.initCause(e);
      throw ioe;
    }
  }

  /** 
   * <p>Interact with the user and the CM-5 bootloader to flash new firmware to
   * the CM-5.</p>
   *
   * @param binary an InputStream from which the new firmware binary is read
   * @param log an output stream to which progress and prompt messages are
   * displayed
   *
   * @return the number of bytes flashed, negative of that if verify failed
   *
   * @exception IOException if there was an input our output error
   * @exception InterruptedException if there was a timeout
   **/
  public synchronized int flashCM5(InputStream binary, PrintStream log)
    throws IOException, InterruptedException {
    
    int bytesSent = 0;
    int b;
 
    if (binary == null)
      throw new IllegalArgumentException("null binary");

    if (log != null) {
      log.println("waiting for CM5 reset, press MODE");
      log.flush();
    }

    drainFromCM5();

    int baudRateWas = -1;

    if (serialPort != null) {

      baudRateWas = serialPort.getBaudRate();
      
      if (baudRateWas != CM5_BOOTLOADER_BAUDRATE) {
        
        if (log != null) {
          log.println("switching from "+baudRateWas+
                      "bps to "+CM5_BOOTLOADER_BAUDRATE+"bps");
          log.flush();
        }
        
        setBaudRate(CM5_BOOTLOADER_BAUDRATE);
      }
    } else {
      if (log != null) {
        log.println(
          "not using RXTX so no control of baudrate, please ensure it is "+
          CM5_BOOTLOADER_BAUDRATE+"bps");
        log.flush();
      }
    }

    double deadline = System.nanoTime() + FLASH_TIMEOUT_MS*1e6;

    int nextMsgChar = 0;
    while (nextMsgChar < CM5_BOOTLOADER_MSG.length()) {
      
      if (System.nanoTime() > deadline)
        throw new IOException("timout waiting for CM-5 reset");
      
      if (nextMsgChar == 0) {
        sendByte('#', false);
        Thread.sleep(50); //important
      }
      
      while ((fromCM5.available() != 0) &&
             (nextMsgChar < CM5_BOOTLOADER_MSG.length())) {
        if (recvByte(false) == CM5_BOOTLOADER_MSG.charAt(nextMsgChar))
          nextMsgChar++;
        else
          nextMsgChar = 0;
      }
    }

    if (log != null) {
      log.println("detected reset to CM-5 bootloader");
      log.println("initiating binary upload to address 0");
      log.flush();
    }

    Thread.sleep(FLASH_DELAY_MS);

    drainFromCM5();

    sendByte('\n', false);
    sendByte('l', false);
    sendByte('\n', false);

    Thread.sleep(FLASH_DELAY_MS);

    drainFromCM5();

    if (log != null) {
      log.print("uploading binary");
      log.flush();
    }

    ByteArrayOutputStream verifyStream = new ByteArrayOutputStream(128*1024);

    for (b = binary.read(); b >= 0; b = binary.read(), bytesSent++) {

      sendByte(b, false);

      verifyStream.write(b);

      if ((log != null) && (bytesSent > 0) && (bytesSent%100 == 0)) {
        log.print(".");
        log.flush();
      }
    }

    if (log != null) {
      log.println("\nflashed "+bytesSent+" bytes");
      log.println("upload complete");
      log.flush();
    }

    if (log != null) {
      log.println("verifying");
      log.flush();
    }

    Thread.sleep(FLASH_DELAY_MS);

    drainFromCM5();

    sendByte('\n', false);
    sendByte('u', false);
    sendByte('p', false);
    sendByte(' ', false);
    sendByte('0', false);
    sendByte(',', false);
    sendByte(' ', false);
    String hexBytesSent = Integer.toHexString(bytesSent);
    for (int i = 0; i < hexBytesSent.length(); i++)
      sendByte(hexBytesSent.charAt(i), false);
    sendByte('\n', false);

    Thread.sleep(FLASH_DELAY_MS);

    drainFromCM5();

    if (log != null) {
      log.println("press START to initiate verify");
      log.flush();
    }

    byte[] verifyArray = verifyStream.toByteArray();

    int verifyFailedAt = -1;

    for (int i = 0; i < bytesSent; i++) {
      
      deadline = System.nanoTime() + FLASH_TIMEOUT_MS*1e6;
      while (fromCM5.available() == 0) 
        if (System.nanoTime() > deadline)
          throw new IOException("timout waiting for verify data");

      if ((log != null) && (i == 0)) {
        log.print("verifying");
        log.flush();
      }

      if ((log != null) && (i > 0) && (i%100 == 0)) {
        log.print(".");
        log.flush();
      }
        
      b = recvByte(false);

      if ((verifyFailedAt < 0) && ((b&0xff) != (verifyArray[i]&0xff)))
        verifyFailedAt = i;
    }

    if (log != null) {
      log.print("\n");
      log.flush();
    }

    if (log != null) {

      if (verifyFailedAt >= 0) {
        log.println("warning, verify failed at byte "+verifyFailedAt);
        log.println("value should have been "+
                    Integer.toHexString(verifyArray[verifyFailedAt]&0xff));
      } else {
        log.println("verified "+bytesSent+" bytes");
      }
      
      log.flush();
    }

    Thread.sleep(FLASH_DELAY_MS);

    drainFromCM5();

    if (serialPort != null) {
      if (baudRateWas != CM5_BOOTLOADER_BAUDRATE) {
        
        if (log != null) {
          log.println("returning to "+baudRateWas+"bps");
          log.flush();
        }
        
        setBaudRate(baudRateWas);
      }
    }

    if (log != null) {
      log.println("waiting for CM5 reset, press MODE");
      log.flush();
    }

    deadline = System.nanoTime() + FLASH_TIMEOUT_MS*1e6;

    while (fromCM5.available() == 0)
      if (System.nanoTime() > deadline)
        throw new IOException("timout waiting for CM-5 reset");

    b = recvByte(false);

    if (b != 0xff) {

      if (log != null) {
        log.println(
          "warning reset byte was 0x"+Integer.toHexString(b)+" not 0xff");
        log.flush();
      }

    } else {

      if (log != null) {
        log.println("detected CM-5 reset to user code");
        log.flush();
      }
    }

    if (verifyFailedAt >= 0)
      bytesSent = -bytesSent;

    return bytesSent;
  }

  /** covers {@link #flashCM5(InputStream, PrintStream)} **/
  public synchronized int flashCM5(File binary, PrintStream log)
    throws IOException, InterruptedException {
    return flashCM5(new FileInputStream(binary), System.out);
  }

  /** covers {@link #flashCM5(File, PrintStream)} **/
  public synchronized int flashCM5(String binary, PrintStream log)
    throws IOException, InterruptedException {
    return flashCM5(new File(binary), System.out);
  }

  /** covers {@link #flashCM5(String, PrintStream)}, uses System.out **/
  public synchronized int flashCM5(String binary)
    throws IOException, InterruptedException {
    return flashCM5(binary, System.out);
  }

  /**
   * <p>Issue {@link Instruction#I_PING} packet requesting ping of the
   * indicated dynamixel.</p>
   *
   * @param id the id of the dynamixel to ping in the closed interval [0,
   * {@link AXRegister#MAX_DYNAMIXEL_ID}] or 255 to ping the CM-5 itself
   *
   * @return the CM-5 status and retry bytes as the 0th and 1st byte of the
   * returned int
   *
   * @exception IOException if there was a communication error
   * @exception InterruptedException if the calling thread was interrupted
   * while waiting for response bytes from the CM-5
   **/
  public synchronized int pingDynamixel(int id)
    throws IOException, InterruptedException {

    if ((AX12_ID.check(id) != 0) && (id != 255))
      throw new IllegalArgumentException("invalid id "+id);

    startSendPacket(Instruction.I_PING);
    sendByte(id);
    endSendPacket();

    return recvStatus();
  }

  /** covers {@link #pingDynamixel}, uses id 255 to ping the CM-5 itself **/
  public synchronized int pingCM5()
    throws IOException, InterruptedException {
    return pingDynamixel(255);
  }

  /**
   * <p>Scan for presence of dynamixels with IDs in the closed interval [0,
   * <code>maxID</code>].</p>
   *
   * @param dynamixels presence written here, (re)consed if null or too short 
   *
   * @return the array of dynamixel presence
   **/
  public synchronized boolean[] scan(boolean[] dynamixels, int maxID)
    throws IOException, InterruptedException {

    if (maxID > MAX_DYNAMIXEL_ID)
      throw new IllegalArgumentException("maxID can be at most "+
                                         MAX_DYNAMIXEL_ID);

    if ((dynamixels == null) || (dynamixels.length < (maxID+1)))
      dynamixels = new boolean[maxID+1];

    for (int i = 0; i <= maxID; i++)
      dynamixels[i] = (pingDynamixel(i) == 0);

    return dynamixels;
  }

  /** covers {@link #scan(boolean[], int)}, always conses **/
  public synchronized boolean[] scan(int maxID)
    throws IOException, InterruptedException {
    return scan(null, maxID);
  }
  
  /**
   * <p>Set the read format as described in the class header doc.</p>
   *
   * <p>The format is both cached for further use on the host and is
   * transmitted to the CM-5.</p>
   *
   * @param id the id of each dynamixel to read, in order.  The number of
   * dynamixels in the read format is considered to be the number of contiguous
   * valid dynamixel IDs (i.e. ids in the interval [0, {@link
   * AXRegister#MAX_DYNAMIXEL_ID}]) starting with the zeroth entry in the id
   * array.  A copy is made.
   * @param start the start register on each dynamixel in the read format, must
   * either be length 1, implying same start reg for all dynamixels, or have at
   * least as many non-null initial entries as the number of read dynamixels.
   * A copy is made.
   * @param num the number of registers to read on each dynamixel in the read
   * format, must either be length 1, implying same num regs for all
   * dynamixels, or have at least as many valid initial entries as the number
   * of read dynamixels.  A copy is made.
   *
   * @return the returned {@link Instruction#I_STATUS} byte from the CM-5
   *
   * @exception IllegalArgumentException if the number of dynamixels in the
   * format exceeds {@link #MAX_DYNAMIXELS}, if a dynamixel ID is used more
   * than once in the format, if the latter args are too short, if the number
   * of registers is negative, or if the span of registers extends beyond the
   * last register
   * @exception IOException if there was a communication error
   * @exception InterruptedException if the calling thread was interrupted
   * while waiting for response bytes from the CM-5
   **/
  public synchronized int setReadFormat(int[] id,
                                        AXRegister[] start, int[] num) 
    throws IOException, InterruptedException {
    return setFormat(F_READ, id, start, num);
  }

  /** 
   * <p>Similar to {@link #setReadFormat}.</p>
   *
   * @exception IllegalArgumentException if any register block {@link
   * AXRegister#containsReadOnlyRegs}
   **/
  public synchronized int setWriteFormat(int[] id,
                                         AXRegister[] start, int[] num)
    throws IOException, InterruptedException {
    return setFormat(F_WRITE, id, start, num);
  }
   
  /** verify the first <i>n</i> dynamixel ids in the given array are unique **/
  public static void checkAXIDs(int[] id, int n) {

    Set<Integer> axIDSet = new LinkedHashSet<Integer>();

    for (int i = 0; i < n; i++) {
      if (axIDSet.contains(id[i]))
        throw new IllegalArgumentException(
          "dynamixel ID "+id[i]+" used more than once");
      else
        axIDSet.add(id[i]);
    }
  }

  /** covers {@link #checkAXIDs(int[], int)}, checks all **/
  public static void checkAXIDs(int[] id) {
    checkAXIDs(id, id.length);
  }

  /**
   * <p>Common impl of {@link #setReadFormat} and {@link #setWriteFormat}.</p>
   *
   * @param f the format
   * @param id the dynamixel ids to set
   * @param start the start regs to set, either length 1 and all start regs set
   * the same or at least as long as the number of dynamixels in the format
   * @param num the reg numbers to set, either length 1 and all start regs set
   * the same or at least as long as the number of dynamixels in the format
   *
   * @return the CM-5 status
   **/
  protected synchronized int setFormat(int f,
                                       int[] id,
                                       AXRegister[] start, int[] num)
    throws IOException, InterruptedException {

    int n;
    for (n = 0; (n < MAX_DYNAMIXELS) && (id != null) && (n < id.length); n++)
      if (AX12_ID.check(id[n]) != 0)
        break;

    checkAXIDs(id, n);

    if (((start.length > 1) && (start.length < n)) || 
        ((num.length > 1) && (num.length < n)))
      throw new IllegalArgumentException("latter args insufficient length");

    for (int i = 0; i < n; i++) {

      AXRegister s = (start.length > 1) ? start[i] : start[0];
      int u = (num.length > 1) ? num[i] : num[0];

      if (((s == null) && (u > 0)) ||
          (u < 0) ||
          ((s.ordinal+u) > s.getNumRegisters()) ||
          ((f == F_WRITE) && containsReadOnlyRegs(s, u)))
        throw new IllegalArgumentException("latter args invalid at index "+i);
    }

    axID[f] = dup(id, axID[f], n);

    if (start.length > 1) {
      startReg[f] = AXRegister.dup(start, startReg[f], n);
    } else {
      if (startReg[f].length < n)
        startReg[f] = new AXRegister[n];
      for (int i = 0; i < n; i++)
        startReg[f][i] = start[0];
    }

    if (num.length > 1) {
      numReg[f] = dup(num, numReg[f], n);
    } else {
      if (numReg[f].length < n)
        numReg[f] = new int[n];
      for (int i = 0; i < n; i++)
        numReg[f][i] = num[0];
    }

    numDynamixels[f] = n;

    startSendPacket(FMT_INSTRUCTION[f]);

    sendByte(n);

    totalNumRegs[f] = 0;

    for (int i = 0; i < n; i++) {

      sendByte(id[i]);

      if (numReg[f][i] > 0) {

        sendByte(startReg[f][i].startAddr);

        AXRegister lastReg =
          startReg[f][i].getRelativeRegister(numReg[f][i]-1);

        int nb = (lastReg.startAddr+lastReg.width)-startReg[f][i].startAddr;

//        System.err.println("nb: "+nb);

        sendByte(nb);

      } else {
        sendByte(0);
        sendByte(0);
      }

      totalNumRegs[f] += numReg[f][i];
    }

    endSendPacket();

    return recvStatus();
  }

  /**
   * <p>Get a copy of the most recently {@link #setReadFormat}, see which.</p>
   *
   * <p>Arg <i>id</i> must either be null or at least as long as the number of
   * dynamixels in the format.</p>
   *
   * <p>Args <i>star</i> and <i>num</i> must either be null, length 1 and all
   * data in format equal, or at least as long as the number of dynamixels in
   * the format.</p>
   *
   * @return the number of dynamixels in the current read format
   **/
  public synchronized int getReadFormat(int[] id,
                                        AXRegister[] start, int[] num) {
    return getFormat(F_READ, id, start, num);
  }

  /** get the number of dynamixels in the current read format **/
  public synchronized int getNumReadDynamixels() {
    return numDynamixels[F_READ];
  }

  /** get the total number of registers in the current read format **/
  public synchronized int getTotalNumReadRegs() {
    return totalNumRegs[F_READ];
  }

  /**
   * <p>Get a copy of the most recently {@link #setWriteFormat}, see which.</p>
   *
   * <p>Args handled similar to {@link #getReadFormat}.</p>
   *
   * @return the number of dynamixels in the current write format
   **/
  public synchronized int getWriteFormat(int[] id,
                                         AXRegister[] start, int[] num)  {
    return getFormat(F_WRITE, id, start, num);
  }

  /** get the number of dynamixels in the current write format **/
  public synchronized int getNumWriteDynamixels() {
    return numDynamixels[F_WRITE];
  }

  /** get the total number of registers in the current write format **/
  public synchronized int getTotalNumWriteRegs() {
    return totalNumRegs[F_WRITE];
  }

  /**
   * <p>Common impl of {@link #getReadFormat} and {@link #getWriteFormat}.</p>
   *
   * @param f the format
   * @param id the dynamixel ids to get
   * @param start the start regs to get, length 1 and all start regs same
   * through format or length geq number of dynamixels in format
   * @param num the reg numbers to get, length 1 and all start regs same
   * through format or length geq number of dynamixels in format
   *
   * @return the number of dynamixels in the format
   **/
  protected synchronized int getFormat(int f,
                                       int[] id,
                                       AXRegister[] start, int[] num) {
    int n = numDynamixels[f];

    if (id != null)
      System.arraycopy(axID[f], 0, id, 0, n);
    
    if (start != null) {
      if (start.length == 1) {
        checkFmtSame(startReg[f], numDynamixels[f]);
        start[0] = startReg[f][0];
      } else {
        System.arraycopy(startReg[f], 0, start, 0, n);
      }
    }

    if (num != null) {
      if (num.length == 1) {
        checkFmtSame(numReg[f], numDynamixels[f]);
        num[0] = numReg[f][0];
      } else {
        System.arraycopy(numReg[f], 0, num, 0, n);
      }
    }
    
    return n;
  }

  /** ensure that all initial <i>n</i> elements of <i>start</i> are equal **/
  protected void checkFmtSame(AXRegister[] start, int n) {
    AXRegister v = start[0];
    for (int i = 0; i < n; i++)
      if (start[i] != v)
        throw new IllegalStateException(
          "start registers not the same across all dynamixels");
  }

  /** ensure that all initial <i>n</i> elements of <i>num</i> are equal **/
  protected void checkFmtSame(int[] num, int n) {
    int v = num[0];
    for (int i = 0; i < n; i++)
      if (num[i] != v)
        throw new IllegalStateException(
          "num registers not the same across all dynamixels");
  }

  /**
   * <p>Covers {@link #setReadFormat}, uses {@link #parseFormatArgs}, for
   * jscheme API convenience.</p>
   **/
  public int setReadFormat(Object[] args)
    throws IOException, InterruptedException {
    args = parseFormatArgs(args);
    return setReadFormat((int[]) args[0],
                         (AXRegister[]) args[1],
                         (int[]) args[2]);
  }

  /**
   * <p>Covers {@link #setWriteFormat}, uses {@link #parseFormatArgs}, for
   * jscheme API convenience.</p>
   **/
  public int setWriteFormat(Object[] args)
    throws IOException, InterruptedException {
    args = parseFormatArgs(args);
    return setWriteFormat((int[]) args[0],
                          (AXRegister[]) args[1],
                          (int[]) args[2]);
  }

  /** 
   * <p>Parse read or write format from a contiguous array, for jscheme API
   * convienience.</p>
   *
   * @param args an integer multiple of &lt;Integer, {@link AXRegister},
   * Integer&gt; triples
   *
   * @return a three-element array consisting of an array of int, an array of
   * {@link AXRegister}, and an array of int, giving the dynamixel id, start
   * register and number of registers respectively in the format
   **/
  protected Object[] parseFormatArgs(Object[] args) {

    if ((args != null) && ((args.length%3) != 0))
      throw new IllegalArgumentException(
        "args must be an integer multiple of <int, AXRegister, int> triples");

    int n = (args != null) ? args.length/3 : 0;

    int[] id = new int[n];
    AXRegister[] start = new AXRegister[n];
    int[] num = new int[n];

    for (int i = 0; i < n; i++) {
      id[i] = ((Integer) (args[3*i])).intValue();
      start[i] = (AXRegister) (args[3*i+1]);
      num[i] = ((Integer) (args[3*i+2])).intValue();
    }
    
    return new Object[] {id, start, num};
  }

  /**
   * <p>Covers {@link #getReadFormat}, uses {@link #packFormat}, for jscheme
   * API convenience.</p>
   **/
  public Object[] getReadFormat() {

    int n = getNumReadDynamixels();

    int[] id = new int[n];
    AXRegister[] start = new AXRegister[n];
    int[] num = new int[n];

    getReadFormat(id, start, num);

    return packFormat(n, id, start, num);
  }

  /**
   * <p>Covers {@link #getWriteFormat}, uses {@link #packFormat}, for jscheme
   * API convenience.</p>
   **/
  public Object[] getWriteFormat() {

    int n = getNumWriteDynamixels();

    int[] id = new int[n];
    AXRegister[] start = new AXRegister[n];
    int[] num = new int[n];

    getWriteFormat(id, start, num);

    return packFormat(n, id, start, num);
  }

  /** 
   * <p>Pack read or write format from a contiguous array, for convienience of
   * the scheme API.</p>
   *
   * @param n the number of dynamixels in the format
   * @param id the dynamixel ids
   * @param start the start registers
   * @param num the number of registers for each dynamixel
   *
   * @return a flat array containing <code>n</code> &lt;Integer, {@link
   * AXRegister}, Integer&gt; triples giving the dynamixel id, start register,
   * and number of registers for each dynamixel in the format
   **/
  protected Object[] packFormat(int n,
                                int[] id,
                                AXRegister[] start, int[] num) {
    
    Object[] ret = new Object[3*n];
    
    for (int i = 0; i < n; i++) {
      ret[3*i] = new Integer(id[i]);
      ret[3*i+1] = start[i];
      ret[3*i+2] = new Integer(num[i]);
    }

    return ret;
  }

  /**
   * <p>Read int register values and CM-5 status into an array, for jscheme API
   * convenience.</p>
   *
   * @return an array of {@link #totalNumRegs}[F_READ]+1 values, with the last
   * set to the CM-5 status/retries
   **/
  public synchronized int[] read() throws IOException, InterruptedException {
    int n = totalNumRegs[F_READ];
    int[] ret = new int[n+1];
    ret[n] = read(ret);
    return ret;
  }

  /**
   * <p>Read natural register values and CM-5 status into an array, for jscheme
   * API convenience.</p>
   *
   * @return an array of {@link #totalNumRegs}[F_READ]+1 values, with the last
   * set to the CM-5 status/retries
   **/
  public synchronized float[] readNatural()
    throws IOException, InterruptedException {
    int n = totalNumRegs[F_READ];
    float[] ret = new float[n+1];
    ret[n] = read(ret);
    return ret;
  }

  /**
   * <p>Read data from dynamixels in natural units according to the current
   * read format.</p>
   *
   * @param data the read data is stored here, must have at least as many
   * entries as the total number of registers in the current read format.  If
   * there were problems reading any particular bytes they will be returned as
   * 0xff.
   *
   * @return the CM-5 status and retry bytes as the 0th and 1st byte of the
   * returned int
   *
   * @exception IOException if there was a communication error
   * @exception InterruptedException if the calling thread was interrupted
   * while waiting for response bytes from the CM-5
   **/
  public synchronized int read(float[] data)
    throws IOException, InterruptedException {
    return read((Object) data);
  }

  /** same as {@link #read(float[])} but reads register ints directly **/
  public synchronized int read(int[] data)
    throws IOException, InterruptedException {
    return read((Object) data);
  }

  /** common impl of {@link #read(float[])} and {@link #read(int[])} **/
  protected synchronized int read(Object data)
    throws IOException, InterruptedException {

    int[] intData = null;
    float[] naturalData = null;

    if (data instanceof int[]) {
      intData = (int[]) data;
      if (intData.length < totalNumRegs[F_READ])
        throw new IllegalArgumentException(
          "must pass an array of at least length "+totalNumRegs[F_READ]);
    } else if (data instanceof float[]) {
      naturalData = (float[]) data;
      if (naturalData.length < totalNumRegs[F_READ])
        throw new IllegalArgumentException(
          "must pass an array of at least length "+totalNumRegs[F_READ]);
    } else {
      throw new IllegalArgumentException("unsupported data type");
    }

    startSendPacket(Instruction.I_READ_DATA);
    endSendPacket();

    startRecvPacket(Instruction.I_DATA);

    int k = 0;

    for (int i = 0; i < numDynamixels[F_READ]; i++) {
      for (int j = 0; j < numReg[F_READ][i]; j++) {

        int value = 0;

        AXRegister reg = startReg[F_READ][i].getRelativeRegister(j);

//        System.err.println("reading "+reg+" ("+reg.width+" bytes)");

        for (int b = 0; b < reg.width; b++)
          value |= recvByte()<<(8*b);

        value = reg.decode(value);

        if (intData != null)
          intData[k++] = value;
        else
          naturalData[k++] = reg.toNaturalUnits(value);

        updateCachedValue(axID[F_READ][i], reg, value);
      }
    }

    int status = recvByte();

    status |= recvByte()<<8;

    recvADCs();

    endRecvPacket();

    return status;
  }

  /** update {@link #cache} **/
  protected synchronized void updateCachedValue(int axID,
                                                AXRegister register,
                                                int value) {

    Map<Integer, CachedValue> cvs = cache.get(register);

    if (cvs == null) {
      cvs = new LinkedHashMap<Integer, CachedValue>();
      cache.put(register, cvs);
    }

    CachedValue cv = cvs.get(axID);

    if (cv == null) {
      cv = new CachedValue();
      cvs.put(axID, cv);
    }

    cv.value = value;
    cv.lastUpdateNS = System.nanoTime();
  }

  /**
   * <p>Look up the most recent {@link CachedValue} of of the specified reg,
   * null if none.</p>
   **/
  public synchronized CachedValue getCachedValue(int axID,
                                                 AXRegister register) {
    Map<Integer, CachedValue> cvs = cache.get(register);
    return (cvs == null) ? null : cvs.get(axID);
  }

  /**
   * <p>Write data to dynamixels in natural units according to the current
   * write format.</p>
   *
   * @param data the data to write, must have at least as many entries as the
   * total number of registers in the current write format 
   *
   * @return the CM-5 status and retry bytes as the 0th and 1st byte of the
   * returned int
   *
   * @exception IOException if there was a communication error
   * @exception InterruptedException if the calling thread was interrupted
   * while waiting for response bytes from the CM-5
   **/
  public synchronized int write(float[] data)
    throws IOException, InterruptedException {
    return write((Object) data);
  }

  /** same as {@link #write(float[])} but writes register ints directly **/
  public synchronized int write(int[] data)
    throws IOException, InterruptedException {
    return write((Object) data);
  }

  /** common impl of {@link #write(float[])} and {@link #write(int[])} **/
  public synchronized int write(Object data)
    throws IOException, InterruptedException {

    int[] intData = null;
    float[] naturalData = null;

    if (data instanceof int[]) {
      intData = (int[]) data;
      if (intData.length < totalNumRegs[F_WRITE])
        throw new IllegalArgumentException(
          "must pass an array of at least length "+totalNumRegs[F_WRITE]);
    } else if (data instanceof float[]) {
      naturalData = (float[]) data;
      if (naturalData.length < totalNumRegs[F_WRITE])
        throw new IllegalArgumentException(
          "must pass an array of at least length "+totalNumRegs[F_WRITE]);
    } else {
      throw new IllegalArgumentException("unsupported data type");
    }

    startSendPacket(Instruction.I_WRITE_DATA);

    int k = 0;

    for (int i = 0; i < numDynamixels[F_WRITE]; i++) {
      for (int j = 0; j < numReg[F_WRITE][i]; j++) {

        int value = 0;

        AXRegister reg = startReg[F_WRITE][i].getRelativeRegister(j);

        if (intData != null)
          value = intData[k++];
        else
          value = reg.fromNaturalUnits(naturalData[k++]);

        value = reg.encode(value);

        for (int b = 0; b < reg.width; b++)
          sendByte(value>>(8*b));
      }
    }

    endSendPacket();

    return recvStatus();
  }

  /** set the timeout for a response from the CM-5 in ms, returns old value **/
  public synchronized double setTimeoutMS(double timeoutMS) {
    double timeoutMSWas = this.timeoutMS;
    this.timeoutMS = timeoutMS;
    return timeoutMSWas;
  } 

  /** get the current timeout for a response from the CM-5 in ms **/
  public synchronized double getTimeoutMS() {
    return timeoutMS;
  }

  /**
   * <p>Receive a byte from the CM-5.</p>
   *
   * @param addToChecksum whether to add the value of the received byte to the
   * current {@link #checksum} in progress
   *
   * @return the received byte
   *
   * @exception IOException if there was a communication error
   * @exception InterruptedException if the calling thread was interrupted
   * while waiting for response bytes from the CM-5
   **/
  protected synchronized int recvByte(boolean addToChecksum)
    throws IOException, InterruptedException {

    double deadline = System.nanoTime() + timeoutMS*1e6;

    while (fromCM5.available() == 0) {
      
      if (System.nanoTime() > deadline) {

        if (recvPacketDebug) {
          int i = 0;
          for (byte b : recvPacketDebugBuffer)
            dbg("RP "+(i++), b);
        }

        recvPacketDebug = false;

        throw new IOException("timeout waiting for response from CM-5");
      }
      
      Thread.sleep(RECV_POLL_MS);
    }

    int b = fromCM5.read();

    if (addToChecksum)
      checksum += b;

    if (debug)
      dbg("R", b);
    
    if (recvPacketDebug)
      recvPacketDebugBuffer.add((byte) (b&0xff));
    
    return b;
  }

  /** print a debug message for a byte **/
  protected void dbg(String msg, int b) {
    b &= 0xff;
    System.err.println(
      msg+": 0x"+Integer.toHexString(b)+" = \'"+((char) b)+"\' = "+b);
  }

  /** covers {@link #recvByte(boolean)}, always adds to checksum **/
  protected synchronized int recvByte()
    throws IOException, InterruptedException {
    return recvByte(true);
  }

  /**
   * <p>Send a byte to the CM-5.</p>
   *
   * @param b the byte to send
   * @param addToChecksum whether to add the value of the sent byte to the
   * current {@link #checksum} in progress, after sending the byte
   *
   * @exception IOException if there was a communication error
   **/
  protected synchronized void sendByte(int b, boolean addToChecksum)
    throws IOException {

    b = b&0xff;

    toCM5.write(b);

    if (addToChecksum)
      checksum += b;

    if (debug)
      dbg("W", b);
  }

  /** covers {@link #sendByte(int, boolean)}, always adds to checksum **/
  protected synchronized void sendByte(int b)
    throws IOException {
    sendByte(b, true);
  }

  /** start an outgoing packet with the given instruction **/
  protected synchronized void startSendPacket(Instruction instruction) 
    throws IOException {
    checksum = 0;
    sendByte(instruction.code);
  }

  /** end an outgoing packet, sending checksum **/
  protected synchronized void endSendPacket() throws IOException {
    sendByte((~checksum)&0xff);
    toCM5.flush();
  }

  /** start an incoming packet expecting the given instruction **/
  protected synchronized void startRecvPacket(Instruction instruction) 
    throws IOException, InterruptedException {

    recvPacketDebugBuffer.clear();
    recvPacketDebug = enableRecvPacketDebug;

    checksum = 0;
    int b = recvByte();
    if (b != instruction.code)
      throw new IOException(
        "expected "+instruction+" return packet, got 0x"+
        Integer.toHexString(b));
  }

  /** end an incoming packet, validating checksum **/
  protected synchronized void endRecvPacket() 
    throws IOException, InterruptedException {

    int b = recvByte(false);

    recvPacketDebug = false;

    if (b != ((~checksum)&0xff))
      throw new IOException(
        "invalid checksum 0x"+Integer.toHexString(b)+
        ", should be 0x"+Integer.toHexString((~checksum)&0xff));
  }

  /** receive a {@link Instruction#I_STATUS} packet, return payload **/
  protected synchronized int recvStatus() 
    throws IOException, InterruptedException {
    startRecvPacket(Instruction.I_STATUS);
    int status = recvByte();
    status |= recvByte()<<8;
    recvADCs();
    endRecvPacket();
    return status;
  }

  /** receive and store the ADC channel readings in {@link #adcValue} **/
  protected synchronized void recvADCs()
    throws IOException, InterruptedException {
    adcValue[CHANNEL_POS] = recvByte();
    adcValue[CHANNEL_NEG] = recvByte();
    adcValue[CHANNEL_THERM] = recvByte();
  }

  /** convert a raw ADC reading to V at the input of the 3.3k/10k divider **/
  public static float adcToVolts(int value) {
    return (((float) value)/255.0f)*5.0f*((10.0f+3.3f)/3.3f);
  }

  /** get the most recent ADC reading for the given channel **/
  public synchronized int getADC(int channel) {

    if ((channel < 0) || (channel > adcValue.length))
      throw new IllegalArgumentException("unknown channel "+channel);

    return adcValue[channel];
  }

  /** close the serial port, no further comms possible **/
  public synchronized void close() {
    serialPort.close();
  }

  /** {@link #close}s **/
  protected void finalize() {
    close();
  }

  /**
   * <p>Verify a CM-5 return status code/retry count.</p>
   *
   * <p>Emits a warning message on stderr if the retry count is non-zero, but
   * only throws an exception when the status code is non-zero.</p>
   *
   * @param status the CM-5 return status code/retry count
   * @param operation a string describing the operation for which the status
   * applies
   * @param warnOnly if set then a warning msg is printed instead of any
   * exception
   *
   * @return true if status was ok
   *
   * @exception IOException if not warnOnly and the status code, but not
   * necessarily the retry count, is non-zero
   **/
  public static boolean verifyStatus(int status, String operation,
                                     boolean warnOnly)
    throws IOException {

    int numRetries = (status&0xff00)>>8;

    if (numRetries > 0)
      System.err.println(
        "W: "+operation+" required "+numRetries+" dynamixel retries");

    if ((status&0xff) != 0) {

      String msg = operation+" failed with status "+statusToString(status);

      if (warnOnly)
        System.err.println("W: "+msg);
      else
        throw new IOException(msg);

      return false;
    }

    return true;
  }

  /** covers {@link #verifyStatus(int, String, boolean)}, not warn **/
  public static void verifyStatus(int status, String operation) 
    throws IOException {
    verifyStatus(status, operation, false);
  }

  /** duplicate first n element of from into to, reallocating as necessary **/
  public static int[] dup(int[] from, int[] to, int n) {
    to = ensureCapacity(to, n);
    System.arraycopy(from, 0, to, 0, n);
    return to;
  }

  /** covers {@link #dup(int[], int[], int)}, copies all **/
  public static int[] dup(int[] from, int[] to) {
    return dup(from, to, from.length);
  }

  /** duplicate first n element of from into to, reallocating as necessary **/
  public static boolean[] dup(boolean[] from, boolean[] to, int n) {
    to = ensureCapacity(to, n);
    System.arraycopy(from, 0, to, 0, n);
    return to;
  }

  /** covers {@link #dup(boolean[], boolean[], int)}, copies all **/
  public static boolean[] dup(boolean[] from, boolean[] to) {
    return dup(from, to, from.length);
  }

  /** duplicate first n element of from into to, reallocating as necessary **/
  public static String[] dup(String[] from, String[] to, int n) {
    to = ensureCapacity(to, n);
    System.arraycopy(from, 0, to, 0, n);
    return to;
  }

  /** covers {@link #dup(String[], String[], int)}, copies all **/
  public static String[] dup(String[] from, String[] to) {
    return dup(from, to, from.length);
  }

  /** make sure <i>a</i> is at least length <i>n</i> **/
  public static int[] ensureCapacity(int[] a, int n) {
    if ((a == null) || (a.length < n))
      a = new int[n];
    return a;
  }

  /** make sure <i>a</i> is at least length <i>n</i> **/
  public static boolean[] ensureCapacity(boolean[] a, int n) {
    if ((a == null) || (a.length < n))
      a = new boolean[n];
    return a;
  }

  /** make sure <i>a</i> is at least length <i>n</i> **/
  public static String[] ensureCapacity(String[] a, int n) {
    if ((a == null) || (a.length < n))
      a = new String[n];
    return a;
  }

  /** serial port talking to the CM-5 **/
  protected SerialPort serialPort;

  /** output stream to CM-5 **/
  protected OutputStream toCM5; 

  /** input stream from CM-5 **/
  protected InputStream fromCM5;

  /** timout in ms to wait for a response byte from the CM-5 **/
  protected double timeoutMS = DEF_TIMEOUT_MS;

  /** checksum in progress **/
  protected int checksum = 0;

  /** num dynamixels in current {@link #F_READ} and {@link #F_WRITE} **/
  protected int[] numDynamixels = new int[] {0, 0};

  /** total num regs in current {@link #F_READ} and {@link #F_WRITE} **/
  protected int[] totalNumRegs = new int[] {0, 0};

  /** dynamixel ids in current {@link #F_READ} and {@link #F_WRITE} **/
  protected int[][] axID = 
    new int[][] {
    new int[numDynamixels[0]],
    new int[numDynamixels[1]]
  };

  /** start regs in current {@link #F_READ} and {@link #F_WRITE} **/
  protected AXRegister[][] startReg =
    new AXRegister[][] {
    new AXRegister[numDynamixels[0]],
    new AXRegister[numDynamixels[1]]
  };

  /** num regs in current {@link #F_READ} and {@link #F_WRITE} **/
  protected int[][] numReg = 
    new int[][] {
    new int[numDynamixels[0]],
    new int[numDynamixels[1]]
  };
}

