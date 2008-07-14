//
// MouseHelper.java
//

/*
VisAD system for interactive analysis and visualization of numerical
data.  Copyright (C) 1996 - 2008 Bill Hibbard, Curtis Rueden, Tom
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

import java.awt.event.*;

import java.rmi.*;
import java.awt.*;

import visad.browser.Convert;

/**
   MouseHelper is the VisAD helper class for MouseBehaviorJ3D
   and MouseBehaviorJ2D.<p>

   MouseHelper is preferred by cats everywhere.<p>
*/
public class MouseHelper
  implements RendererSourceListener
{

  MouseBehavior behavior;

  /** DisplayRenderer for Display */
  DisplayRenderer display_renderer;
  DisplayImpl display;
  /** ProjectionControl for Display */
  private ProjectionControl proj;

  private double xymul;

  DataRenderer direct_renderer = null;

  /** matrix from ProjectionControl when mousePressed1 (left) */
  private double[] tstart;

  /** screen location when mousePressed1 or mousePressed3 */
  private int start_x, start_y;
  private double xmul, ymul;
  private double[] xtrans = new double[3];
  private double[] ytrans = new double[3];

  /** mouse in window */
  private boolean mouseEntered;
  /** ((InputEvent) event).getModifiers() when mouse pressed */
  private int mouseModifiers;

  /** flag for 2-D mode */
  private boolean mode2D;

// start of variables for table-driven mapping from mouse buttons and
//     keys to functons

  // index values for functions
  public static final int NONE = -1, ROTATE = 0, ZOOM = 1, TRANSLATE = 2,
    CURSOR_TRANSLATE = 3, CURSOR_ZOOM = 4, CURSOR_ROTATE = 5, DIRECT = 6,
    NFUNCTIONS = 7;

  // index values for mouse buttons
  public static final int LEFT = 0, CENTER = 1, RIGHT = 2;

  // actual mouse buttons pressed
  boolean[] actual_button = {false, false, false};

  // mouse button pressed accounting for combos
  int virtual_button = -1;

  // array of enables for functions
  boolean[] function = {false, false, false, false, false, false, false};

  // save previous function to compute function change
  boolean[] old_function = {false, false, false, false, false, false, false};

  // enable any two mouse buttons = the third
  boolean enable_combos = true;

  // mapping from buttons/keys to function
  //   function_map[button][CTRL][SHIFT] where
  //   button = 0, 1, 2
  //   CTRL = 0, 1
  //   SHIFT = 0, 1
  // initialize with defaults
  int[][][] function_map =
    {{{ROTATE, ZOOM}, {TRANSLATE, NONE}},
     {{CURSOR_TRANSLATE, CURSOR_ZOOM}, {CURSOR_ROTATE, NONE}},
     {{DIRECT, DIRECT}, {DIRECT, DIRECT}}};

// end of variables for table-driven mapping from mouse buttons
//     and keys to functons

  public MouseHelper(DisplayRenderer r, MouseBehavior b) {
    behavior = b;
    display_renderer = r;
    display = display_renderer.getDisplay();
    proj = display.getProjectionControl();
    mode2D = display_renderer.getMode2D();

    // track Display's DataRenderers in case direct_renderer is removed
    display.addRendererSourceListener(this);

    function =
      new boolean[] {false, false, false, false, false, false, false};
    enable_combos = true;

  }

  /**
   * Process the given event treating it as a local event.
   * @param event event to process.
   */
  public void processEvent(AWTEvent event) {
    processEvent(event, VisADEvent.LOCAL_SOURCE);
  }

  // WLH 17 Aug 2000
  private boolean first = true;

  /** 
   * Process the given event, treating it as coming from a remote source
   *  if remote flag is set.
   * @param event event to process.
   * @param remoteId  id of remote source.
   */
  public void processEvent(AWTEvent event, int remoteId) {

    if (behavior == null) return;

    // WLH 13 May 2003
    // if (first) {
    if (event instanceof MouseEvent &&
        ((MouseEvent) event).getID() == MouseEvent.MOUSE_PRESSED) {
      start_x = 0;
      start_y = 0;
      VisADRay start_ray = behavior.findRay(start_x, start_y);
      VisADRay start_ray_x = behavior.findRay(start_x + 1, start_y);
      VisADRay start_ray_y = behavior.findRay(start_x, start_y + 1);

      if (start_ray != null && start_ray_x != null && start_ray_y != null) {
        double[] tstart = proj.getMatrix();
        double[] rot = new double[3];
        double[] scale = new double[3];
        double[] trans = new double[3];
        behavior.instance_unmake_matrix(rot, scale, trans, tstart);
        double[] trot = behavior.make_matrix(rot[0], rot[1], rot[2],
                                             scale[0], scale[1], scale[2],
                                             0.0, 0.0, 0.0);
        double[] xmat = behavior.make_translate(
                           start_ray_x.position[0] - start_ray.position[0],
                           start_ray_x.position[1] - start_ray.position[1],
                           start_ray_x.position[2] - start_ray.position[2]);
        double[] ymat = behavior.make_translate(
                           start_ray_y.position[0] - start_ray.position[0],
                           start_ray_y.position[1] - start_ray.position[1],
                           start_ray_y.position[2] - start_ray.position[2]);
        double[] xmatmul = behavior.multiply_matrix(trot, xmat);
        double[] ymatmul = behavior.multiply_matrix(trot, ymat);
        behavior.instance_unmake_matrix(rot, scale, trans, xmatmul);
        double xmul = trans[0];
        behavior.instance_unmake_matrix(rot, scale, trans, ymatmul);
        double ymul = trans[1];
        xymul = Math.sqrt(xmul * xmul + ymul * ymul);
        // System.out.println("xymul = " + xymul);
        first = false;
      }
    }

    if (!(event instanceof MouseEvent)) {
      System.out.println("MouseHelper.processStimulus: non-" +
                         "MouseEvent");
      return;
    }
    MouseEvent mouse_event = (MouseEvent) event;

    int event_id = event.getID();

    if (event_id == MouseEvent.MOUSE_ENTERED) {
      mouseEntered = true;
      try {
        DisplayEvent e = new DisplayEvent(display,
          DisplayEvent.MOUSE_ENTERED, mouse_event, remoteId);
        display.notifyListeners(e);
      }
      catch (VisADException e) {
      }
      catch (RemoteException e) {
      }
      return;
    }
    else if (event_id == MouseEvent.MOUSE_EXITED) {
      mouseEntered = false;
      try {
        DisplayEvent e = new DisplayEvent(display,
          DisplayEvent.MOUSE_EXITED, mouse_event, remoteId);
        display.notifyListeners(e);
      }
      catch (VisADException e) {
      }
      catch (RemoteException e) {
      }
      return;
    }
    else if (event_id == MouseEvent.MOUSE_MOVED) {
      try {
        DisplayEvent e = new DisplayEvent(display,
          DisplayEvent.MOUSE_MOVED, mouse_event, remoteId);
        display.notifyListeners(e);
      }
      catch (VisADException e) {
      }
      catch (RemoteException e) {
      }
      return;
    }
    else if (event_id == MouseEvent.MOUSE_PRESSED ||
             event_id == MouseEvent.MOUSE_RELEASED) {
      int m = ((InputEvent) event).getModifiers();
      int m1 = m & InputEvent.BUTTON1_MASK;
      int m2 = m & InputEvent.BUTTON2_MASK;
      int m3 = m & InputEvent.BUTTON3_MASK;
      int mctrl = m & InputEvent.CTRL_MASK;
      int mshift = m & InputEvent.SHIFT_MASK;

      if (event_id == MouseEvent.MOUSE_PRESSED) {
        display.updateBusyStatus();
        if (m1 != 0) {
          actual_button[LEFT] = true;
        }
        if (m2 != 0) {
          actual_button[CENTER] = true;
        }
        if (m3 != 0) {
          actual_button[RIGHT] = true;
        }
        mouseModifiers = m;
      }
      else { // event_id == MouseEvent.MOUSE_RELEASED
        display.updateBusyStatus();
        if (m1 != 0) {
          actual_button[LEFT] = false;
        }
        if (m2 != 0) {
          actual_button[CENTER] = false;
        }
        if (m3 != 0) {
          actual_button[RIGHT] = false;
        }
        mouseModifiers = 0;
      }

      // compute button combos
      int n = 0, sum = 0;
      for (int i=0; i<3; i++) {
        if (actual_button[i]) {
          n++;
          sum += i;
        }
      }
      if (n == 1) {
        virtual_button = sum;
      }
      else if (n == 2 && enable_combos) {
        virtual_button = 3 - sum;
      }
      else { // n == 0 || n == 3 || (n == 2 && !enable_combos)
        virtual_button = -1;
      }
  
      // compute old and new functions
      for (int i=0; i<NFUNCTIONS; i++) {
        old_function[i] = function[i];
        function[i] = false;
      }

      int vctrl = (mctrl == 0) ? 0 : 1;
      int vshift = (mshift == 0) ? 0 : 1;
      int f = (virtual_button < 0) ? -1 :
              function_map[virtual_button][vctrl][vshift];

      if (f >= 0) function[f] = true;

      boolean cursor_off = enableFunctions((MouseEvent) event);

      if (event_id == MouseEvent.MOUSE_PRESSED) {
        try {
          DisplayEvent e = new DisplayEvent(display,
            DisplayEvent.MOUSE_PRESSED, mouse_event, remoteId);
          display.notifyListeners(e);
        }
        catch (VisADException e) {
        }
        catch (RemoteException e) {
        }
        if (m1 != 0) {
          try {
            DisplayEvent e = new DisplayEvent(display,
              DisplayEvent.MOUSE_PRESSED_LEFT, mouse_event, remoteId);
            display.notifyListeners(e);
          }
          catch (VisADException e) {
          }
          catch (RemoteException e) {
          }
        }
        if (m2 != 0) {
          try {
            DisplayEvent e = new DisplayEvent(display,
              DisplayEvent.MOUSE_PRESSED_CENTER, mouse_event, remoteId);
            display.notifyListeners(e);
          }
          catch (VisADException e) {
          }
          catch (RemoteException e) {
          }
        }
        if (m3 != 0) {
          try {
            DisplayEvent e = new DisplayEvent(display,
              DisplayEvent.MOUSE_PRESSED_RIGHT, mouse_event, remoteId);
            display.notifyListeners(e);
          }
          catch (VisADException e) {
          }
          catch (RemoteException e) {
          }
        }
      }
      else { // event_id == MouseEvent.MOUSE_RELEASED
        try {
          DisplayEvent e = new DisplayEvent(display,
            DisplayEvent.MOUSE_RELEASED, mouse_event, remoteId);
          display.notifyListeners(e);
        }
        catch (VisADException e) {
        }
        catch (RemoteException e) {
        }
        if (m1 != 0) {
          try {
            DisplayEvent e = new DisplayEvent(display,
              DisplayEvent.MOUSE_RELEASED_LEFT, mouse_event, remoteId);
            display.notifyListeners(e);
          }
          catch (VisADException e) {
          }
          catch (RemoteException e) {
          }
        }
        if (m2 != 0) {
          try {
            DisplayEvent e = new DisplayEvent(display,
              DisplayEvent.MOUSE_RELEASED_CENTER, mouse_event, remoteId);
            display.notifyListeners(e);
          }
          catch (VisADException e) {
          }
          catch (RemoteException e) {
          }
        }
        if (m3 != 0) {
          try {
            DisplayEvent e = new DisplayEvent(display,
              DisplayEvent.MOUSE_RELEASED_RIGHT, mouse_event, remoteId);
            display.notifyListeners(e);
          }
          catch (VisADException e) {
          }
          catch (RemoteException e) {
          }
        }
      }
      if (cursor_off) display_renderer.setCursorOn(false);
    }
    else if (event_id == MouseEvent.MOUSE_DRAGGED) {
      boolean cursor = function[CURSOR_TRANSLATE] ||
                       function[CURSOR_ZOOM] ||
                       function[CURSOR_ROTATE];

      boolean matrix = function[ROTATE] ||
                       function[ZOOM] ||
                       function[TRANSLATE];

      if (cursor || matrix || function[DIRECT]) {
        display.updateBusyStatus();

        Dimension d = ((MouseEvent) event).getComponent().getSize();
        int current_x = ((MouseEvent) event).getX();
        int current_y = ((MouseEvent) event).getY();

        if (matrix) {
          double[] t1 = null;
          if (function[ZOOM]) {
            // current_y -> scale
            double scale =
              Math.exp((start_y-current_y) / (double) d.height);
            t1 = behavior.make_matrix(0.0, 0.0, 0.0, scale, 0.0, 0.0, 0.0);
          }
          if (function[TRANSLATE]) {
            // current_x, current_y -> translate
            // WLH 9 Aug 2000
            double transx = xmul * (start_x - current_x);
            double transy = ymul * (start_y - current_y);
            // System.out.println("xmul = " + xmul + " ymul = " + ymul);
            // System.out.println("transx = " + transx + " transy = " + transy);
            t1 = behavior.make_translate(-transx, -transy);
          }
          if (function[ROTATE]) {
            if (mode2D) {
              double transx = xmul * (start_x - current_x);
              double transy = ymul * (start_y - current_y);
              t1 = behavior.make_translate(-transx, -transy);
            }
            else {
              // don't do 3-D rotation in 2-D mode
              double angley =
                - (current_x - start_x) * 100.0 / (double) d.width;
              double anglex =
                - (current_y - start_y) * 100.0 / (double) d.height;
              t1 = behavior.make_matrix(anglex, angley,
                0.0, 1.0, 0.0, 0.0, 0.0);
            }
          }
          if (t1 != null) {
            t1 = behavior.multiply_matrix(t1, tstart);
            try {
              proj.setMatrix(t1);
            }
            catch (VisADException e) {
            }
            catch (RemoteException e) {
            }
          }
        } // end if (matrix)


        if (function[CURSOR_ZOOM]) {
          if (!mode2D) {
            // don't do cursor Z in 2-D mode
            // current_y -> 3-D cursor Z
            float diff =
              (start_y - current_y) * 4.0f / (float) d.height;
            display_renderer.drag_depth(diff);
          }
        }
        if (function[CURSOR_ROTATE]) {
          if (!mode2D) {
            // don't do 3-D rotation in 2-D mode
            double angley =
              - (current_x - start_x) * 100.0 / (double) d.width;
            double anglex =
              - (current_y - start_y) * 100.0 / (double) d.height;
            double[] t1 = behavior.make_matrix(anglex, angley,
              0.0, 1.0, 0.0, 0.0, 0.0);
            t1 = behavior.multiply_matrix(t1, tstart);
            try {
              proj.setMatrix(t1);
            }
            catch (VisADException e) {
            }
            catch (RemoteException e) {
            }
          }
        }
        if(function[CURSOR_TRANSLATE]) {
          // current_x, current_y -> 3-D cursor X and Y
          VisADRay cursor_ray = behavior.findRay(current_x, current_y);
          if (cursor_ray != null) {
            display_renderer.drag_cursor(cursor_ray, false);
          }
        }

        if (function[DIRECT]) {
          if (direct_renderer != null) {
            VisADRay direct_ray = behavior.findRay(current_x, current_y);
            if (direct_ray != null) {
              direct_renderer.setLastMouseModifiers(mouseModifiers);
              direct_renderer.drag_direct(direct_ray, false, mouseModifiers);
            }
          }
        }


      }
      try {
        DisplayEvent e = new DisplayEvent(display,
          DisplayEvent.MOUSE_DRAGGED, mouse_event, remoteId);
        display.notifyListeners(e);
      }
      catch (VisADException e) {
      }
      catch (RemoteException e) {
      }
    }

  }

  private boolean enableFunctions(MouseEvent event) {

    boolean cursor_off = false;

    if (event == null) {
      for (int i=0; i<NFUNCTIONS; i++) {
        old_function[i] = function[i];
        function[i] = false;
      }
    }

    // compute old and new cursor and matrix enables
    boolean cursor = function[CURSOR_TRANSLATE] ||
                     function[CURSOR_ZOOM] ||
                     function[CURSOR_ROTATE];
    boolean old_cursor = old_function[CURSOR_TRANSLATE] ||
                         old_function[CURSOR_ZOOM] ||
                         old_function[CURSOR_ROTATE];

    boolean matrix = function[ROTATE] ||
                     function[ZOOM] ||
                     function[TRANSLATE];
    boolean old_matrix = old_function[ROTATE] ||
                         old_function[ZOOM] ||
                         old_function[TRANSLATE];

    // disable functions
    if (old_cursor && !cursor) {
      // display_renderer.setCursorOn(false);
      cursor_off = true;
    }

    if (old_function[DIRECT] && !function[DIRECT]) {
      display_renderer.setDirectOn(false);
      if (direct_renderer != null) {
        direct_renderer.release_direct();
        direct_renderer = null;
      }

    }

    // enable functions
    if (matrix && !old_matrix) {

      start_x = ((MouseEvent) event).getX();
      start_y = ((MouseEvent) event).getY();

      // WLH 9 Aug 2000
      VisADRay start_ray = behavior.findRay(start_x, start_y);
      VisADRay start_ray_x = behavior.findRay(start_x + 1, start_y);
      VisADRay start_ray_y = behavior.findRay(start_x, start_y + 1);

      tstart = proj.getMatrix();
      // print_matrix("tstart", tstart);
      double[] rot = new double[3];
      double[] scale = new double[3];
      double[] trans = new double[3];
      behavior.instance_unmake_matrix(rot, scale, trans, tstart);
      double sts = scale[0];
      double[] trot = behavior.make_matrix(rot[0], rot[1], rot[2],
                                           scale[0], scale[1], scale[2],
                                           0.0, 0.0, 0.0);
      // print_matrix("trot", trot);

      // WLH 17 Aug 2000
      double[] xmat = behavior.make_translate(
                         start_ray_x.position[0] - start_ray.position[0],
                         start_ray_x.position[1] - start_ray.position[1],
                         start_ray_x.position[2] - start_ray.position[2]);
      double[] ymat = behavior.make_translate(
                         start_ray_y.position[0] - start_ray.position[0],
                         start_ray_y.position[1] - start_ray.position[1],
                         start_ray_y.position[2] - start_ray.position[2]);
      double[] xmatmul = behavior.multiply_matrix(trot, xmat);
      double[] ymatmul = behavior.multiply_matrix(trot, ymat);
/*
      print_matrix("xmat", xmat);
      print_matrix("ymat", ymat);
      print_matrix("xmatmul", xmatmul);
      print_matrix("ymatmul", ymatmul);
*/
      behavior.instance_unmake_matrix(rot, scale, trans, xmatmul);
      xmul = trans[0];
      behavior.instance_unmake_matrix(rot, scale, trans, ymatmul);
      ymul = trans[1];

      // horrible hack, WLH 17 Aug 2000
      if (behavior instanceof visad.java2d.MouseBehaviorJ2D) {
        double factor = xymul / Math.sqrt(xmul * xmul + ymul * ymul);
        xmul *= factor;
        ymul *= factor;

        xmul = Math.abs(xmul);
        ymul = -Math.abs(ymul);
      }
/*
      System.out.println("xmul = " + Convert.shortString(xmul) +
                         " ymul = " + Convert.shortString(ymul) +
                         " scale = " + Convert.shortString(sts));
*/

    } // end if (matrix && !old_matrix)


    if (cursor && !old_cursor) {

      // turn cursor on whenever mouse button2 pressed
      display_renderer.setCursorOn(true);

      start_x = ((MouseEvent) event).getX();
      start_y = ((MouseEvent) event).getY();

      tstart = proj.getMatrix();
    }

    if (function[CURSOR_TRANSLATE] && !old_function[CURSOR_TRANSLATE]) {
      VisADRay cursor_ray = behavior.findRay(start_x, start_y);
      if (cursor_ray != null) {
        display_renderer.drag_cursor(cursor_ray, true);
      }
    }

    if (function[CURSOR_ZOOM] && !old_function[CURSOR_ZOOM]) {
      if (!mode2D) {
        // don't do cursor Z in 2-D mode
        // current_y -> 3-D cursor Z
        VisADRay cursor_ray =
          behavior.cursorRay(display_renderer.getCursor());
        display_renderer.depth_cursor(cursor_ray);
      }
    }

    if (function[DIRECT] && !old_function[DIRECT]) {
      if (display_renderer.anyDirects()) {
        int current_x = ((MouseEvent) event).getX();
        int current_y = ((MouseEvent) event).getY();
        VisADRay direct_ray =
          behavior.findRay(current_x, current_y);
        if (direct_ray != null) {
          direct_renderer =
            display_renderer.findDirect(direct_ray, mouseModifiers);
          if (direct_renderer != null) {
            display_renderer.setDirectOn(true);
            direct_renderer.setLastMouseModifiers(mouseModifiers);
            direct_renderer.drag_direct(direct_ray, true,
              mouseModifiers);
          }
        }
      }
    }
    return cursor_off;
  }

  /** 
   * Enable/disable the interpretation of any pair of mouse buttons
   * as the third button.
   * @param  e  enable/disable. If true (default), interpret any pair 
   *            of mouse buttons as the third button.
   */
  public void setEnableCombos(boolean e) {
    enable_combos = e;
    enableFunctions(null);
  }

  /** 
   * Set mapping from (button, ctrl, shift) to function.
   *
   *  <pre>
   *  map[button][ctrl][shift] =
   *    MouseHelper.NONE               for no function
   *    MouseHelper.ROTATE             for box rotate
   *    MouseHelper.ZOOM               for box zoom
   *    MouseHelper.TRANSLATE          for box translate
   *    MouseHelper.CURSOR_TRANSLATE   for cursor translate
   *    MouseHelper.CURSOR_ZOOM        for cursor on Z axis (3-D only)
   *    MouseHelper.CURSOR_ROTATE      for box rotate with cursor
   *    MouseHelper.DIRECT             for direct manipulate
   *  where button = 0 (left), 1 (center), 2 (right)
   *  ctrl = 0 (CTRL key not pressed), 1 (CTRL key pressed)
   *  shift = 0 (SHIFT key not pressed), 1 (SHIFT key pressed)
   *
   *  Note some direct manipulation DataRenderers test the status of
   *  CTRL and SHIFT keys, so it is advisable that the DIRECT function
   *  be invariant to the state of ctrl and shift in the map array.
   *
   *  For example, to set the left mouse button for direct
   *  manipulation, and the center button for box rotation
   *  (only without shift or control):
   *  mouse_helper.setFunctionMap(new int[][][]
   *    {{{MouseHelper.DIRECT, MouseHelper.DIRECT},
   *      {MouseHelper.DIRECT, MouseHelper.DIRECT}},
   *     {{MouseHelper.ROTATE, MouseHelper.NONE},
   *      {MouseHelper.NONE, MouseHelper.NONE}},
   *     {{MouseHelper.NONE, MouseHelper.NONE},
   *      {MouseHelper.NONE, MouseHelper.NONE}}});
   *  </pre>
   * @param  map map of functions.  map must be int[3][2][2]
   * @throws VisADException  bad map
   */
  public void setFunctionMap(int[][][] map) throws VisADException {
    if (map == null || map.length != 3) {
      throw new DisplayException("bad map array");
    }
    for (int i=0; i<3; i++) {
      if (map[i] == null || map[i].length != 2) { 
        throw new DisplayException("bad map array");
      }
      for (int j=0; j<2; j++) {
        if (map[i][j] == null || map[i][j].length != 2) {
          throw new DisplayException("bad map array");
        }
        for (int k=0; k<2; k++) {
          if (map[i][j][k] >= NFUNCTIONS) {
            throw new DisplayException("bad map array value" + map[i][j][k]);
          }
        }
      }
    }
    for (int i=0; i<3; i++) {
      for (int j=0; j<2; j++) {
        for (int k=0; k<2; k++) {
          function_map[i][j][k] = map[i][j][k];
        }
      }
    }
    enableFunctions(null);
  }


  /**
   * Print out a readable form of a matrix.  Useful for
   * debugging.
   * @param title  title to prepend to output.
   * @param m  matrix to print.
   */
  public void print_matrix(String title, double[] m) {
    if (behavior == null) return;
    double[] rot = new double[3];
    double[] scale = new double[3];
    double[] trans = new double[3];
    behavior.instance_unmake_matrix(rot, scale, trans, m);
    StringBuffer buf = new StringBuffer(title);
    buf.append(" = (");
    buf.append(Convert.shortString(rot[0]));
    buf.append(", ");
    buf.append(Convert.shortString(rot[1]));
    buf.append(", ");
    buf.append(Convert.shortString(rot[2]));
    buf.append("), ");
    if (scale[0] == scale[1] && scale[0] == scale[2]) {
      buf.append(Convert.shortString(scale[0]));
      buf.append(", (");
    } else {
      buf.append("(");
      buf.append(Convert.shortString(scale[0]));
      buf.append(", ");
      buf.append(Convert.shortString(scale[1]));
      buf.append(", ");
      buf.append(Convert.shortString(scale[2]));
      buf.append("), (");
    }
    buf.append(Convert.shortString(trans[0]));
    buf.append(", ");
    buf.append(Convert.shortString(trans[1]));
    buf.append(", ");
    buf.append(Convert.shortString(trans[2]));
    buf.append(")");
    System.out.println(buf.toString());
  }

  /**
   * Implementation for RendererSourceListener.  Notifies that the
   * renderer has been deleted.
   * @param renderer DataRenderer that was deleted.
   */
  public void rendererDeleted(DataRenderer renderer)
  {
    if (direct_renderer != null) {
      if (direct_renderer == renderer || direct_renderer.equals(renderer)) {
        direct_renderer = null;
      }
    }
  }
}
