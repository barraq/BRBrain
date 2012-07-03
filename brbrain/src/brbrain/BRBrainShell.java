/**
 * <p>Bioloid Remote Brain scheme shell.</p>
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
import java.io.*;

import jscheme.*;

/**
 * <p>Bioloid Remote Brain scheme shell.</p>
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
 *
 * @author Marsette (Marty) A. Vona, III
 **/
public class BRBrainShell {

  private static final String cvsid =
  "$Id: BRBrainShell.java 24 2008-05-02 22:08:41Z vona $";

  /** shell prompt **/
  public static final String PROMPT = "brbrain>";
  
  /** startup banner **/
  public static final String BANNER =
    "\nThis is a JScheme interpreter (http://jscheme.sourceforge.net).\n";

  /** command line usage message **/
  public static final String USAGE = 
    "BRBrainShell -l|<<port name> [-f] [<baudrate>] [-e|i <script> [<script> ...]]>\n"+
    "\n"+
    "-l shows available ports and default baudrate\n"+
    "-f connects via a file, else use RXTX\n"+
    "-e executes one or more scripts and terminates\n"+
    "-i executes one or more scripts and shows prompt";

  /** the scheme bindings for the api **/
  public static final String RBRAIN_SHELL_API = "brbrain-shell-api.scm";

  /** the scheme bindings for the extra stuff **/
  public static final String RBRAIN_SHELL_EXTRA = "brbrain-shell-extra.scm";

  /** the BRBrain **/
  protected BRBrain brBrain;

  /** the JScheme interpreter **/
  protected JScheme js = null;

  /** create a new shell maing a new {@link BRBrain} connected with RXTX **/
  public BRBrainShell(String portName, int baudRate)
    throws IOException, InterruptedException {
    this(new BRBrain(portName, baudRate));
  }

  /** create a new shell maing a new {@link BRBrain} connected via a file **/
  public BRBrainShell(File port) throws IOException, InterruptedException {
    this(new BRBrain(port));
  }

  /** create a new shell on an existing {@link BRBrain} **/
  public BRBrainShell(BRBrain brBrain) throws IOException {
    this.brBrain = brBrain;
    initJScheme();
  }

  /** initialize JScheme **/
  protected void initJScheme() throws IOException {

    ClassLoader classLoader = getClass().getClassLoader();

    //Ensure the classloader that gives us the JScheme class is the same as the
    //classloader that gave us this class.  This is an attempt to fix the
    //issue that the user has their own jscheme.jar in their path which would
    //normally take precedence over the application jar

    Exception ex = null;

    try {

      Class clazz = classLoader.loadClass("jscheme.JScheme");

      if (clazz != null)
        js = (JScheme) (clazz.newInstance());

    } catch (ClassNotFoundException e) {
      ex = e;
    } catch (InstantiationException e) {
      ex = e;
    } catch (IllegalAccessException e) {
      ex = e;
    } 

    if (js == null)
      System.err.println("W: failed to load jscheme: "+ex);
    
    //Important: if jscheme.jar is loaded as an extension then it will come in
    //on a different classloader that does not know how to load normal classes.
    //We fix that by telling jscheme which classloader to use.
    jsint.Import.setClassLoader(classLoader);

    js.setGlobalValue("brbrain", brBrain);
    js.setGlobalValue("brbrain-shell", this);

    for (AXRegister r : AX12Register.getAllRegisters())
      js.setGlobalValue(r.toIdentifierString(), r);

    for (AXRegister r : AXS1Register.getAllRegisters())
      js.setGlobalValue(r.toIdentifierString(), r);

    loadJSchemeFile(RBRAIN_SHELL_API);
    loadJSchemeFile(RBRAIN_SHELL_EXTRA);
  }

  /** load a scheme file in the interpreter **/
  protected void loadJSchemeFile(String fileName) throws IOException {

    if (js == null)
      return;

    //first try finding it in the jar
    InputStream inputStream = getClass().getResourceAsStream(fileName);

    if (inputStream == null) {
     
      //nope, look for it on the filesystem
      File file = new File(fileName);
      
      if (file.exists() && file.canRead()) {
        
        inputStream = new FileInputStream(file);

      } else {

        System.err.println("W: failed to load " + fileName);
      }
    }

    if (inputStream != null)
      js.load(new InputStreamReader(inputStream));
  }

  /** command line driver, see {@link #USAGE} **/
  public static void main(String[] argv)
    throws IOException, InterruptedException {

    int baudRate = BRBrain.RXTX_DEF_BAUD_RATE;

    if (argv.length == 0) {
      System.err.println(USAGE);
      System.exit(-1);
    }
   
    if ("-l".equals(argv[0]) && (argv.length == 1)) {
      BRBrain.listPorts();
      System.out.println("\ndefault baudrate "+baudRate+"\n");
      System.exit(0);
    }
      
    String portName = argv[0];

    String[] scripts = null;
    boolean showPrompt = true;
    boolean directFile = false;

    for (int i = 1; i < argv.length; i++) {

      if ("-e".equals(argv[i])) {
        
        int numScripts = argv.length-(i+1);

        if (numScripts < 1) {
          System.err.println(USAGE);
          System.exit(-1);
        }

        scripts = new String[numScripts];
        System.arraycopy(argv, i+1, scripts, 0, numScripts);

        showPrompt = false;

        break;

      } else if ("-i".equals(argv[i])) {
        
        int numScripts = argv.length-(i+1);

        if (numScripts < 1) {
          System.err.println(USAGE);
          System.exit(-1);
        }

        scripts = new String[numScripts];
        System.arraycopy(argv, i+1, scripts, 0, numScripts);
        
        break;

      } else if ("-f".equals(argv[i])) {
        
        directFile = true;

      } else if ((i == 1) || ((i == 2) && directFile)) {
        
        try {

          baudRate = Integer.parseInt(argv[i]);

        } catch (NumberFormatException e) {

          System.err.println(USAGE);
          System.exit(-1);
        }
        
      } else {
        
        System.err.println(USAGE);
        System.exit(-1);
      }
    }
  
    BRBrainShell shell = null;
    if (!directFile) {

      System.out.println(
        "\nConnecting on "+portName+" at "+baudRate+" bps.\n");

      shell = new BRBrainShell(portName, baudRate);

    } else {

      System.out.println("\nConnecting to file "+portName+".\n");

      shell = new BRBrainShell(new File(portName));
    }

    if (scripts != null)
      for (String script : scripts)
        shell.loadJSchemeFile(script);

    if (showPrompt && (shell.js != null)) {
      System.out.println(BANNER);
      shell.js.readEvalPrintLoop(PROMPT);
    }

    System.exit(0);
  }
}
