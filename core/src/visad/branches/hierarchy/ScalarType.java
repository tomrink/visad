//
// ScalarType.java
//

/*
VisAD system for interactive analysis and visualization of numerical
data.  Copyright (C) 1996 - 2001 Bill Hibbard, Curtis Rueden, Tom
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

package visad;

import java.io.InvalidObjectException;

import java.lang.ref.ReferenceQueue;

import java.util.HashMap;
import java.util.Map;

import visad.util.WeakMapValue;

/**
 * ScalarType is the superclass of the VisAD hierarchy of scalar data types.
 */
public abstract class ScalarType extends MathType implements Comparable {

  // name of scalar type - enforce uniqueness locally
  // but do not rely on it - names may be duplicated on remote systems
  String Name;

  /**
   * Hashtable of scalar names used to make sure scalar names are unique
   * (within local VM).  Because the values in the hashtable are actually {@link
   * WeakMapValue}s, the existance of a {@link ScalarType} in the hashtable will
   * not prevent it from being garbage-collected when it is no longer strongly
   * referenced.
   */
  private static Map ScalarHash = new HashMap();

  // Aliases for scalar names
  private static Map Translations = new HashMap();
  private static Map ReverseTranslations = new HashMap();

  // Queue that receives garbage-collected ScalarType instances.
  private static final ReferenceQueue queue = new ReferenceQueue();

  /**
   * Constructs an instance with a specified name.
   *
   * @param name           The name for this instance.
   * @throws TypeException if the name is invalid.
   * @see #validateName(String)
   */
  public ScalarType(String name) throws VisADException {
    super();
    Name = name;
    synchronized(getClass()) {
	checkQueue();
	validateName(name, "name");
	ScalarHash.put(name, new WeakMapValue(Name, this, queue));
    }
  }

  /**
   * Trusted constructor used to create standard VisAD <CODE>RealType</CODE>s
   * without all the name-checking overhead.
   *
   * @param name Trusted name.
   * @param b Dummy value used to indicate that this is a trusted constructor.
   */
  ScalarType(String name, boolean b) {
    super(b);
    Name = name;
    synchronized(getClass()) {
	checkQueue();
	ScalarHash.put(name, new WeakMapValue(Name, this, queue));
    }
  }

  /**
   * Compares this object with another of the same type.  The comparison is
   * on the names.
   *
   * @param obj		The other object of the same type.
   * @return		A value less than zero, zero, or greater than zero
   *			depending on whether this object is considered less
   *			than, equal to, or greater than the other object,
   *			respectively.
   */
  public int compareTo(Object obj) {
    return getName().compareTo(((ScalarType)obj).getName());
  }

  /**
   * Indicates if this ScalarType is the same as another object.
   *
   * @param obj		The other object.
   * @return		<code>true</code> if and only if the other object is a
   *			ScalarType and compares equal to this ScalarType.
   */
  public boolean equals(Object obj) {
    return obj instanceof ScalarType && compareTo(obj) == 0;
  }

  /**
   * Obtains the hash code for this object.  If
   * <code>scalarType1.equals(scalarType2)</code>, then
   * <code>scalarType1.hashCode() == scalarType2.hashCode()</code>.
   *
   * @return		The hash code for this object.
   */
  public int hashCode() {
    return getName().hashCode();
  }

  /**
   * Change the primary name for this <CODE>ScalarType</CODE>.
   * The original name can still be used.<P>
   * If multiple aliases are created, the last one is dominant.<P>
   * This is handy for translating standard VisAD <CODE>RealType</CODE>
   * names to a language other than English.
   *
   * @param alias The new name.
   *
   * @exception TypeException If the new name is not valid.
   */
  public void alias(String alias)
    throws TypeException
  {
    synchronized(getClass()) {
      if (!Name.equals(Translations.get(alias))) {
	validateName(alias, "alias");
	Translations.put(alias, Name);
      }
      ReverseTranslations.put(Name, alias);
    }
  }

  /**
   * Returns this <CODE>ScalarType</CODE>'s name.
   *
   * @return The name of this <CODE>ScalarType</CODE>.
   */
  public String getName() {
    synchronized(getClass()) {
      String alias = (String )ReverseTranslations.get(Name);
      if (alias != null) {
	return alias;
      }
    }
    return Name;
  }

  public String getNameWithBlanks() {
    return getName().replace('_', ' ');
  }

  /**
   * Get the <CODE>ScalarType</CODE> which has the specified name.
   *
   * @param name Name of <CODE>ScalarType</CODE>.
   * @return Either the <CODE>ScalarType</CODE> if found,
   *          or <CODE>null</CODE>.
   */
  public static synchronized ScalarType getScalarTypeByName(String name) {
    if (name == null) {
      return null;
    }
    checkQueue();
    if (Translations.containsKey(name)) {
      name = (String )Translations.get(name);
    }
    ScalarType st;
    Object obj = ScalarHash.get(name);
    st =
      obj == null
	? null
	: (ScalarType)((WeakMapValue)obj).getValue();
    return st;
  }

  /**
   * Throws a {@link TypeException} if the name is invalid.  Valid names may not
   * contain the characters period, space, left or right parenthesis or already
   * be associated with a previously-created instance (either as a name or
   * alias).
   *
   * @param name           Name to check.
   * @param type           Type used in exception message (e.g. "name", 
   *                       "alias").
   * @throws TypeException if the name is <code>null</code> or, otherwise,
   *                       illegal.
   * @see #alias(String)
   */
  public static synchronized void validateName(String name, String type)
    throws TypeException
  {
    if (name == null) {
      throw new TypeException("ScalarType: " + type + " cannot be null");
    }
    if (name.indexOf(".") > -1 ||
        name.indexOf(" ") > -1 ||
        name.indexOf("(") > -1 ||
        name.indexOf(")") > -1) {
      throw new TypeException("ScalarType: " + type + " cannot contain " +
                              "space . ( or ) " + name);
    }
    if (getScalarTypeByName(name) != null) {
      throw new TypeException("ScalarType: " + type + " already used");
    }
    if (Translations.containsKey(name)) {
      throw new TypeException("ScalarType: " + type + " already used" +
			      " as an alias");
    }
  }

/*
  public static void dumpAliases()
  {
    java.util.Enumeration enum;

    boolean needHead = true;
    enum = Translations.keys();
    while (enum.hasMoreElements()) {
      Object key = enum.nextElement();
      if (needHead) {
        System.err.println("== Translation table");
        needHead = false;
      }
      System.err.println("   \"" + key + "\" => \"" +
                         Translations.get(key) + "\"");
    }

    boolean needMid = true;
    enum = ReverseTranslations.keys();
    while (enum.hasMoreElements()) {
      Object key = enum.nextElement();
      if (needMid) {
        if (needHead) {
          System.err.println("== Reverse Translation table");
          needHead = false;
        } else {
          System.err.println("-- Reverse Translation table");
        }
        needMid = false;
      }
      System.err.println("   \"" + key + "\" => \"" +
                         ReverseTranslations.get(key) + "\"");
    }
    if (!needHead) {
      System.err.println("==");
    }
  }
*/

  /**
   * <p>Returns the instance corresponding to this newly deserialized instance.
   * If a ScalarType with the same name as this instance already exists and
   * is compatible with this instance, then it is returned.  Otherwise, this
   * instance is returned. </p>
   *
   * <p>This method is protected so that it is always invoked during
   * deserialization and final to prevent subclasses from evading it.</p>
   *
   * @return                        the unique ScalarType object corresponding
   *                                to this object's name.
   * @throws InvalidObjectException if an incompatible ScalarType with the same
   *                                name as this instance already exists.
   */
  protected final Object readResolve()
    throws InvalidObjectException
  {
    ScalarType st;
    synchronized(getClass()) {
	st = getScalarTypeByName(Name);
	if (st == null) {
	  ScalarHash.put(Name, new WeakMapValue(Name, this, queue));
	  st = this;
	}
	else if (!equals(st)) {
	  throw new InvalidObjectException(toString());
	}
    }
    return st;
  }

  /**
   * Checks the queue for garbage-collected instances and removes them from the
   * hash table and translation tables.
   */
  private static synchronized void checkQueue() {
    for (WeakMapValue ref; (ref = (WeakMapValue)queue.poll()) != null; ) {
      Object name = ref.getKey();
      ScalarHash.remove(name);
      Object alias = ReverseTranslations.remove(name);
      if (alias != null)
	Translations.remove(alias);
    }
  }
}

