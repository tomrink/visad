//
// JPythonEditor.java
//

/*
VisAD system for interactive analysis and visualization of numerical
data.  Copyright (C) 1996 - 2000 Bill Hibbard, Curtis Rueden, Tom
Rink, Dave Glowacki, Steve Emmerson, Tom Whittaker, Don Murray, and
Tommy Jasmin.

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Library General Public
License as published by the Free Software Foundation; either
version 2 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Library General Public License for more details.

You should have received a copy of the GNU Library General Public
License along with this library; if not, write to the Free
Software Foundation, Inc., 59 Temple Place - Suite 330, Boston,
MA 02111-1307, USA
*/

package visad.python;

import java.io.*;
import java.lang.reflect.*;
import visad.VisADException;
import visad.formula.FormulaUtil;
import visad.util.*;

/** An editor for writing and executing JPython code in Java runtime. */
public class JPythonEditor extends TextEditor {

  /** name of JPython interpreter class */
  private static final String interp = "org.python.util.PythonInterpreter";

  /** JPython interpreter class */
  private static final Class interpClass = constructInterpClass();

  /** obtains the JPython interpreter class */
  private static Class constructInterpClass() {
    Class c = null;
    try {
      c = Class.forName(interp);
    }
    catch (ClassNotFoundException exc) {
      if (DEBUG) exc.printStackTrace();
    }
    return c;
  }

  /** names of useful methods from JPython interpreter class */
  private static final String[] methodNames = {
    interp + ".exec(java.lang.String)",
    interp + ".execfile(java.lang.String)",
    interp + ".set(java.lang.String, org.python.core.PyObject)",
    interp + ".get(java.lang.String)"
  };

  /** useful methods from JPython interpreter class */
  private static final Method[] methods =
    FormulaUtil.stringsToMethods(methodNames);

  /** method for executing a line of JPython code */
  private static final Method exec = methods[0];

  /** method for executing a document containing JPython code */
  private static final Method execfile = methods[1];

  /** method for setting a JPython variable's value */
  private static final Method set = methods[2];

  /** method for getting a JPython variable's value */
  private static final Method get = methods[3];

  /** PythonInterpreter object */
  protected Object python = null;


  /** constructs a JPythonEditor */
  public JPythonEditor() throws VisADException {
    this(null);
  }

  /** constructs a JPythonEditor containing text from the given filename */
  public JPythonEditor(String filename) throws VisADException {
    super(filename);

    // construct a JPython interpreter
    try {
      python = interpClass.newInstance();
    }
    catch (NullPointerException exc) {
      if (DEBUG) exc.printStackTrace();
    }
    catch (IllegalAccessException exc) {
      if (DEBUG) exc.printStackTrace();
    }
    catch (InstantiationException exc) {
      if (DEBUG) exc.printStackTrace();
    }
    if (python == null) throw new VisADException("JPython library not found");

    // add JPython files to file chooser dialog box
    fileChooser.addChoosableFileFilter(
      new ExtensionFileFilter("py", "JPython source code"));
  }

  /** executes a line of JPython code */
  public void exec(String line) throws VisADException {
    try {
      exec.invoke(python, new Object[] {line});
    }
    catch (IllegalAccessException exc) {
      throw new VisADException(exc.toString());
    }
    catch (IllegalArgumentException exc) {
      throw new VisADException(exc.toString());
    }
    catch (InvocationTargetException exc) {
      throw new VisADException(exc.getTargetException().getMessage());
    }
  }

  /** executes the entire document */
  public void execfile(String filename) throws VisADException {
    try {
      execfile.invoke(python, new Object[] {filename});
    }
    catch (IllegalAccessException exc) {
      throw new VisADException(exc.toString());
    }
    catch (IllegalArgumentException exc) {
      throw new VisADException(exc.toString());
    }
    catch (InvocationTargetException exc) {
      throw new VisADException(exc.getTargetException().getMessage());
    }
  }

  /** sets a JPython variable's value */
  public void set(String name, Object value) throws VisADException {
    try {
      set.invoke(python, new Object[] {name, value});
    }
    catch (IllegalAccessException exc) {
      throw new VisADException(exc.toString());
    }
    catch (IllegalArgumentException exc) {
      throw new VisADException(exc.toString());
    }
    catch (InvocationTargetException exc) {
      throw new VisADException(exc.getTargetException().getMessage());
    }
  }

  /** gets a JPython variable's value */
  public Object get(String name) throws VisADException {
    try {
      return get.invoke(python, new Object[] {name});
    }
    catch (IllegalAccessException exc) {
      throw new VisADException(exc.toString());
    }
    catch (IllegalArgumentException exc) {
      throw new VisADException(exc.toString());
    }
    catch (InvocationTargetException exc) {
      throw new VisADException(exc.getTargetException().getMessage());
    }
  }

  /** executes the JPython script */
  public void run() throws VisADException {
    final String filename = getFilename();
    if (filename == null || hasChanged()) {
      throw new VisADException("A save is required before execution");
    }
    execfile(filename);
  }

  /** compiles the JPython script to a Java class */
  public void compile() throws VisADException {
    throw new VisADException("Not yet implemented!");
  }
  
}
