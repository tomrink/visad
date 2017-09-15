//
// DisplayImplA3D.java
//

/*
VisAD system for interactive analysis and visualization of numerical
data.  Copyright (C) 1996 - 2017 Bill Hibbard, Curtis Rueden, Tom
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

package visad.ardor3d;

import com.ardor3d.renderer.Camera;

import visad.*;

import java.rmi.*;

import java.awt.*;

import javax.media.j3d.*;
import java.util.Iterator;
import java.util.Vector;
// import com.sun.j3d.utils.applet.AppletFrame;

/**
   DisplayImplJ3D is the VisAD class for displays that use
   Java 3D.  It is runnable.<P>

   DisplayImplJ3D is not Serializable and should not be copied
   between JVMs.<P>
*/
public class DisplayImplA3D extends DisplayImpl {

  /** distance behind for surfaces in 2-D mode */
  // WLH 25 March 2003 (at BOM)
  // public static final float BACK2D = -2.0f;
  public static final float BACK2D = -0.01f;

  /**
   * Use a parallel projection view
   * @see GraphicsModeControlJ3D#setProjectionPolicy
   */
  public static final int PARALLEL_PROJECTION =
    Camera.ProjectionMode.Parallel.ordinal();

  /**
   * Use a perspective projection view. This is the default.
   * @see GraphicsModeControlJ3D#setProjectionPolicy
   */
  public static final int PERSPECTIVE_PROJECTION =
    Camera.ProjectionMode.Parallel.ordinal();

  /** Render polygonal primitives by filling the interior of the polygon
      @see GraphicsModeControlJ3D#setPolygonMode */
  public static final int POLYGON_FILL = 0;

  /**
   * Render polygonal primitives as lines drawn between consecutive vertices
   * of the polygon.
   * @see GraphicsModeControlJ3D#setPolygonMode
   */
  public static final int POLYGON_LINE = 1;

  /**
   * Render polygonal primitives as points drawn at the vertices of
   * the polygon.
   * @see GraphicsModeControlJ3D#setPolygonMode
   */
  public static final int POLYGON_POINT = 2;
  
  /**
   * Transparency attributes. 
   */
  public static final int SCREEN_DOOR = 0;
  public static final int BLENDED = 1;
  public static final int NONE = 2;
  public static final int FASTEST = 3;
  public static final int NICEST = 4;

  /** Field for specifying unknown API type */
  public static final int UNKNOWN = 0;
  /** Field for specifying that the DisplayImpl be created in a JPanel */
  public static final int JPANEL = 1;
  /** Field for specifying that the DisplayImpl does not have a screen Component */
  public static final int OFFSCREEN = 2;
  /** Field for specifying that the DisplayImpl transforms but does not render */
  public static final int TRANSFORM_ONLY = 4;

  /** 
   * Property name for setting whether to use geometry by reference.
   * @see #GEOMETRY_BY_REF
   */
  public static final String PROP_GEOMETRY_BY_REF = "visad.java3d.geometryByRef";
  /**
   * Indicates whether to use geometry by reference when creating geometry arrays.
   * @see javax.media.j3d.GeometryArray#BY_REFERENCE
   */
  public static final boolean GEOMETRY_BY_REF;
  static {
    GEOMETRY_BY_REF = Boolean.parseBoolean(System.getProperty(PROP_GEOMETRY_BY_REF, "true"));
  }

  /**
   * Property name for enabling the use of non-power of two textures.
   * @see #TEXTURE_NPOT
   */
  public static final String PROP_TEXTURE_NPOT = "visad.java3d.textureNpot";

  /**
   * Indicates whether to allow non-power of two textures. This has been known
   * to cause some issues with Apple 32bit Macs eventhough the Canvas3D
   * properties indicate that NPOT is supported.
   * @see javax.media.j3d.Canvas3D#queryProperties()
   */
  // FIXME:
  // This works with the Java3D 1.5.2 example TextureImageNPOT but does not work
  // with the VisAD library image rednering. On initial testing it behaves as if
  // there may be threading issues.  This requires more investigation before we
  // can enable this based on the Canvas3D properties.
  public static final boolean TEXTURE_NPOT;
  static {
    TEXTURE_NPOT = Boolean.parseBoolean(System.getProperty(PROP_TEXTURE_NPOT, "false"));
    //System.err.println("TEXTURE_NPOT:"+TEXTURE_NPOT);
  }
  

  private ProjectionControlA3D projection = null;
  private GraphicsModeControlA3D mode = null;
  private int apiValue = UNKNOWN;

  /** construct a DisplayImpl for Java3D with the
      default DisplayRenderer, in a JFC JPanel */
  public DisplayImplA3D(String name)
         throws VisADException, RemoteException {
    this(name, null, JPANEL, null);
  }

  /** construct a DisplayImpl for Java3D with a non-default
      DisplayRenderer, in a JFC JPanel */
  public DisplayImplA3D(String name, DisplayRendererA3D renderer)
         throws VisADException, RemoteException {
    this(name, renderer, JPANEL, null);
  }

  /** constructor with default DisplayRenderer */
  public DisplayImplA3D(String name, int api)
         throws VisADException, RemoteException {
    this(name, null, api, null);
  }

  /** construct a DisplayImpl for Java3D with a non-default
      GraphicsConfiguration, in a JFC JPanel */
  public DisplayImplA3D(String name, GraphicsConfiguration config)
         throws VisADException, RemoteException {
    this(name, null, JPANEL, config);
  }

  /** construct a DisplayImpl for Java3D with a non-default
      DisplayRenderer;
      in a JFC JPanel if api == DisplayImplJ3D.JPANEL and
      in an AppletFrame if api == DisplayImplJ3D.APPLETFRAME */
  public DisplayImplA3D(String name, DisplayRendererA3D renderer, int api)
         throws VisADException, RemoteException {
    this(name, renderer, api, null);
  }

  /** construct a DisplayImpl for Java3D with a non-default
      DisplayRenderer and GraphicsConfiguration, in a JFC JPanel */
  public DisplayImplA3D(String name, DisplayRendererA3D renderer,
                        GraphicsConfiguration config)
         throws VisADException, RemoteException {
    this(name, renderer, JPANEL, config);
  }

  /** constructor with default DisplayRenderer and a non-default
      GraphicsConfiguration */
  public DisplayImplA3D(String name, int api, GraphicsConfiguration config)
         throws VisADException, RemoteException {
    this(name, null, api, config);
  }

  public DisplayImplA3D(String name, DisplayRendererA3D renderer, int api,
                        GraphicsConfiguration config)
         throws VisADException, RemoteException {
    this(name, renderer, api, config, null);
  }

  /** the 'c' argument is intended to be an extension class of
      VisADCanvasJ3D (or null); if it is non-null, then api must
      be JPANEL and its super() constructor for VisADCanvasJ3D
      must be 'super(renderer, config)' */
  public DisplayImplA3D(String name, DisplayRendererA3D renderer, int api,
                        GraphicsConfiguration config, VisADCanvasA3D c)
         throws VisADException, RemoteException {
    super(name, renderer);

    initialize(api, config, c);
  }

  /** constructor for off screen */
  public DisplayImplA3D(String name, int width, int height)
         throws VisADException, RemoteException {
    this(name, null, width, height);
  }

  /** constructor for off screen */
  public DisplayImplA3D(String name, DisplayRendererA3D renderer,
                        int width, int height)
         throws VisADException, RemoteException {
    this(name, renderer, width, height, null);
  }

  /** constructor for off screen;
      the 'c' argument is intended to be an extension class of
      VisADCanvasJ3D (or null); if it is non-null, then its super()
      constructor for VisADCanvasJ3D must be
      'super(renderer, width, height)' */
  public DisplayImplA3D(String name, DisplayRendererA3D renderer,
                        int width, int height, VisADCanvasA3D c)
         throws VisADException, RemoteException {
    super(name, renderer);

    initialize(OFFSCREEN, null, width, height, c);
  }

  public DisplayImplA3D(RemoteDisplay rmtDpy)
         throws VisADException, RemoteException {
    this(rmtDpy, null, rmtDpy.getDisplayAPI(), null);
  }

  public DisplayImplA3D(RemoteDisplay rmtDpy, DisplayRendererA3D renderer)
         throws VisADException, RemoteException {
    this(rmtDpy, renderer, rmtDpy.getDisplayAPI(), null);
  }

  public DisplayImplA3D(RemoteDisplay rmtDpy, int api)
         throws VisADException, RemoteException {
    this(rmtDpy, null, api, null);
  }

  public DisplayImplA3D(RemoteDisplay rmtDpy, GraphicsConfiguration config)
         throws VisADException, RemoteException {
    this(rmtDpy, null, rmtDpy.getDisplayAPI(), config);
  }

  public DisplayImplA3D(RemoteDisplay rmtDpy, DisplayRendererA3D renderer,
			int api)
         throws VisADException, RemoteException {
    this(rmtDpy, renderer, api, null);
  }

  public DisplayImplA3D(RemoteDisplay rmtDpy, DisplayRendererA3D renderer,
                        GraphicsConfiguration config)
         throws VisADException, RemoteException {
    this(rmtDpy, renderer, rmtDpy.getDisplayAPI(), config);
  }

  public DisplayImplA3D(RemoteDisplay rmtDpy, int api,
                        GraphicsConfiguration config)
         throws VisADException, RemoteException {
    this(rmtDpy, null, api, config);
  }

  public DisplayImplA3D(RemoteDisplay rmtDpy, DisplayRendererA3D renderer,
                        int api, GraphicsConfiguration config)
         throws VisADException, RemoteException {
    this(rmtDpy, renderer, api, config, null);
  }

  /** the 'c' argument is intended to be an extension class of
      VisADCanvasJ3D (or null); if it is non-null, then api must
      be JPANEL and its super() constructor for VisADCanvasJ3D
      must be 'super(renderer, config)' */
  public DisplayImplA3D(RemoteDisplay rmtDpy, DisplayRendererA3D renderer,
                        int api, GraphicsConfiguration config,
                        VisADCanvasA3D c)
         throws VisADException, RemoteException {
    super(rmtDpy,
          ((renderer == null && api == TRANSFORM_ONLY) ?
             new TransformOnlyDisplayRendererA3D() : renderer));

    // use this for testing cluster = true in ordinary collab programs
    // super(rmtDpy,
    //       ((renderer == null && api == TRANSFORM_ONLY) ?
    //          new TransformOnlyDisplayRendererJ3D() : renderer), true);

    initialize(api, config, c);

    syncRemoteData(rmtDpy);
  }

  private void initialize(int api, GraphicsConfiguration config)
          throws VisADException, RemoteException {
    initialize(api, config, -1, -1, null);
  }

  private void initialize(int api, GraphicsConfiguration config,
                          VisADCanvasA3D c)
          throws VisADException, RemoteException {
    initialize(api, config, -1, -1, c);
  }

  private void initialize(int api, GraphicsConfiguration config,
                          int width, int height)
          throws VisADException, RemoteException {
    initialize(api, config, width, height, null);
  }

  private void initialize(int api, GraphicsConfiguration config,
                          int width, int height, VisADCanvasA3D c)
          throws VisADException, RemoteException {
    // a ProjectionControl always exists
    projection = new ProjectionControlA3D(this);
    addControl(projection);


    if (api == JPANEL) {
      Component component = new DisplayPanelA3D(this, config, c);
      setComponent(component);
      apiValue = api;
    }
    else if (api == TRANSFORM_ONLY) {
      if (!(getDisplayRenderer() instanceof TransformOnlyDisplayRendererA3D)) {
        throw new DisplayException("must be TransformOnlyDisplayRendererA3D " +
                                   "for api = TRANSFORM_ONLY");
      }
      setComponent(null);
      apiValue = api;
    }
    else if (api == OFFSCREEN) {
      DisplayRendererA3D renderer = (DisplayRendererA3D) getDisplayRenderer();
      VisADCanvasA3D canvas = (c != null) ? c :
                              new VisADCanvasA3D(renderer, width, height);
      setComponent(null);
      apiValue = api;
    }
    else {
      throw new DisplayException("DisplayImplA3D: bad graphics API " + api);
    }
    if (api != TRANSFORM_ONLY) {
      // initialize projection and set Display in Canvas
      projection.setAspect(new double[] {1.0, 1.0, 1.0});
      ((DisplayRendererA3D) getDisplayRenderer()).getCanvas().setDisplay();
    }

    // a GraphicsModeControl always exists
    mode = new GraphicsModeControlA3D(this);
    addControl(mode);
  }

  /** return a DefaultDisplayRendererJ3D */
  protected DisplayRenderer getDefaultDisplayRenderer() {
    return new DefaultDisplayRendererA3D();
  }

  public void setScreenAspect(double height, double width) {
    DisplayRendererA3D dr = (DisplayRendererA3D) getDisplayRenderer();
//    Screen3D screen = dr.getCanvas().getScreen3D();
//    screen.setPhysicalScreenHeight(height);
//    screen.setPhysicalScreenWidth(width);
  }

  /**
   * Get the projection control associated with this display
   * @see ProjectionControlJ3D
   *
   * @return  this display's projection control
   */
  public ProjectionControl getProjectionControl() {
    return projection;
  }

  /**
   * Get the graphics mode control associated with this display
   * @see GraphicsModeControlJ3D
   *
   * @return  this display's graphics mode control
   */
  public GraphicsModeControl getGraphicsModeControl() {
    return mode;
  }

  /**
   * Return the API used for this display
   *
   * @return  the mode being used (UNKNOWN, JPANEL, APPLETFRAME,
   *                               OFFSCREEN, TRANSFORM_ONLY)
   * @throws  VisADException
   */
  public int getAPI()
	throws VisADException
  {
    return apiValue;
  }

  private void setGeometryCapabilities(GeometryArray array) {
    array.setCapability(GeometryArray.ALLOW_COLOR_READ);
    array.setCapability(GeometryArray.ALLOW_COORDINATE_READ);
    array.setCapability(GeometryArray.ALLOW_COUNT_READ);
    array.setCapability(GeometryArray.ALLOW_FORMAT_READ);
    array.setCapability(GeometryArray.ALLOW_NORMAL_READ);
    array.setCapability(GeometryArray.ALLOW_TEXCOORD_READ);
    /* TDR (2013-10-12): Should only be used in conjunction with GeometryArray.updateData 
       which is not currently implemented
    array.setCapability(GeometryArray.ALLOW_REF_DATA_WRITE);
    */

    // only used when using BY_REFERENCE, so just set it anyways
    //array.setCapability(GeometryArray.ALLOW_REF_DATA_READ);
  }
  
  public GeometryArray makeGeometry(VisADGeometryArray vga) throws VisADException {
    if (vga == null) return null;
    
    boolean mode2d = getDisplayRenderer().getMode2D();
    int vertexFormat = makeFormat(vga);

    if (vga instanceof VisADIndexedTriangleStripArray) {
      /* this is the 'normal' makeGeometry */
      VisADIndexedTriangleStripArray vgb = (VisADIndexedTriangleStripArray) vga;
      if (vga.vertexCount == 0) return null;
      IndexedTriangleStripArray array =
        new IndexedTriangleStripArray(vga.vertexCount, vertexFormat,
                                      vgb.indexCount, vgb.stripVertexCounts);
     
      setGeometryCapabilities(array);
     
      basicGeometry(vga, array, mode2d);
      if (vga.coordinates != null) {
        array.setCoordinateIndices(0, vgb.indices);
      }
      if (vga.colors != null) {
        array.setColorIndices(0, vgb.indices);
      }
      if (vga.normals != null) {
        array.setNormalIndices(0, vgb.indices);
      }
      if (vga.texCoords != null) {
        array.setTextureCoordinateIndices(0, vgb.indices);
      }
      return array;
    }
    if (vga instanceof VisADTriangleStripArray) {
      VisADTriangleStripArray vgb = (VisADTriangleStripArray) vga;
      if (vga.vertexCount == 0) return null;
      TriangleStripArray array =
        new TriangleStripArray(vga.vertexCount, vertexFormat, vgb.stripVertexCounts);
      setGeometryCapabilities(array);
      basicGeometry(vga, array, mode2d);
      return array;
    }
    else if (vga instanceof VisADLineArray) {
      if (vga.vertexCount == 0) return null;
      LineArray array = new LineArray(vga.vertexCount, vertexFormat);
      setGeometryCapabilities(array);
      basicGeometry(vga, array, false);
      return array;
    }
    else if (vga instanceof VisADLineStripArray) {
      if (vga.vertexCount == 0) return null;
      VisADLineStripArray vgb = (VisADLineStripArray) vga;
      LineStripArray array =
        new LineStripArray(vga.vertexCount, vertexFormat, vgb.stripVertexCounts);
      setGeometryCapabilities(array);
      basicGeometry(vga, array, false);
      return array;
    }
    else if (vga instanceof VisADPointArray) {
      if (vga.vertexCount == 0) return null;
      PointArray array = new PointArray(vga.vertexCount, vertexFormat);
      setGeometryCapabilities(array);
      basicGeometry(vga, array, false);
      return array;
    }
    else if (vga instanceof VisADTriangleArray) {
      if (vga.vertexCount == 0) return null;
      TriangleArray array = new TriangleArray(vga.vertexCount, vertexFormat);
      setGeometryCapabilities(array);
      basicGeometry(vga, array, mode2d);
      return array;
    }
    else if (vga instanceof VisADQuadArray) {
      if (vga.vertexCount == 0) return null;
      QuadArray array = new QuadArray(vga.vertexCount, vertexFormat);
      setGeometryCapabilities(array);
      basicGeometry(vga, array, mode2d);
      return array;
    }
    else {
      throw new DisplayException("DisplayImplJ3D.makeGeometry");
    }
  }
  
  private void basicGeometry(VisADGeometryArray vga, GeometryArray array, boolean mode2d) {
    
    if (mode2d) {
      if (vga.coordinates != null) {
        int len = vga.coordinates.length;
        float[] coords = new float[len];
        System.arraycopy(vga.coordinates, 0, coords, 0, len);
        for (int i=2; i<len; i+=3) coords[i] = BACK2D;
        if (GEOMETRY_BY_REF) array.setCoordRefFloat(coords);
        else array.setCoordinates(0, coords);
      }
    }
    else {
      if (vga.coordinates != null) {
        if (GEOMETRY_BY_REF) array.setCoordRefFloat(vga.coordinates);
        else array.setCoordinates(0, vga.coordinates);
      }
    }
    if (vga.colors != null) {
      if (GEOMETRY_BY_REF) array.setColorRefByte(vga.colors);
      else array.setColors(0, vga.colors);
    }
    if (vga.normals != null) {
      if (GEOMETRY_BY_REF) array.setNormalRefFloat(vga.normals);
      else array.setNormals(0, vga.normals);
    }
    if (vga.texCoords != null) {
      if (GEOMETRY_BY_REF) array.setTexCoordRefFloat(0, vga.texCoords);
      else array.setTextureCoordinates(0, vga.texCoords);
    }
  }
  
  private static int makeFormat(VisADGeometryArray vga) {
    int format = 0;
    if (vga.coordinates != null) format |= GeometryArray.COORDINATES;
    if (vga.colors != null) {
      if (vga.colors.length == 3 * vga.vertexCount) {
        format |= GeometryArray.COLOR_3;
      }
      else {
        format |= GeometryArray.COLOR_4;
      }
    }
    if (vga.normals != null) format |= GeometryArray.NORMALS;
    if (vga.texCoords != null) {
      if (vga.texCoords.length == 2 * vga.vertexCount) {
        format |= GeometryArray.TEXTURE_COORDINATE_2;
      }
      else {
        format |= GeometryArray.TEXTURE_COORDINATE_3;
      }
    }
    if (GEOMETRY_BY_REF) format |= GeometryArray.BY_REFERENCE;
    return format;
  }

//  public void destroyUniverse() {
//    if (universe != null) universe.destroy();
//    universe = null;
//  }


  public void destroy() throws VisADException, RemoteException {
    /** TDR, keep temporarily for reference
    if(isDestroyed())return;

    ((DisplayRendererA3D) getDisplayRenderer()).destroy();
    if (apiValue == OFFSCREEN) {
      destroyUniverse();
    }
    MouseBehavior mouse =  getMouseBehavior();
    if(mouse!=null && mouse instanceof MouseBehaviorJ3D) {
        ((MouseBehaviorJ3D) mouse).destroy();
    }
    super.destroy();
    applet = null;
    projection = null;
    mode = null;
    */
  }
  
  float getOffsetDepthMinimum(float depthOffsetMax) {
    Vector rendVec = getRendererVector();
    Iterator<DataRenderer> iter = rendVec.iterator();
    float offsetMin = depthOffsetMax;
    while (iter.hasNext()) {
      DataRenderer rend = iter.next();
        if (rend.hasPolygonOffset()) {
          if (rend.getPolygonOffset() < offsetMin) {
            offsetMin = rend.getPolygonOffset();  
          }
        }
    }
    return offsetMin;
  }
    
  int getNumRenderersWithZoffset() {
     Vector rendVec = getRendererVector();
     Iterator<DataRenderer> iter = rendVec.iterator();
     int num = 0;
     while (iter.hasNext()) {
       DataRenderer rend = iter.next();
       if (rend.hasPolygonOffset()) {
         num++;
       }
     }
     return num;
   }
   
   /**
    * Sets the depth buffer offset when autoDepthOffset is enabled for this display.
    * @param renderer
    * @param mode 
    */
  public void setDepthBufferOffset(DataRenderer renderer, GraphicsModeControl mode) {
     GraphicsModeControlA3D mode3d = (GraphicsModeControlA3D) mode;
     if (mode3d.getAutoDepthOffsetEnable()) {
       float depthOffsetInc = mode3d.getDepthOffsetIncrement();
       int numLayers = mode3d.getNumRenderersWithDepthOffset();
       float maxDepthOffset = numLayers*(-depthOffsetInc);
       
       if (!renderer.hasPolygonOffset()) {
         int cnt = getNumRenderersWithZoffset();
         if (cnt < numLayers) {
           renderer.setPolygonOffset(getOffsetDepthMinimum(maxDepthOffset) + depthOffsetInc);
           renderer.setPolygonOffsetFactor(0f);
           renderer.setHasPolygonOffset(true);  
         }
         else {
           renderer.setPolygonOffset(0f);  
           renderer.setPolygonOffsetFactor(0f);
           renderer.setHasPolygonOffset(false);
         }
       }
       mode3d.setPolygonOffset(renderer.getPolygonOffset(), false);
       mode3d.setPolygonOffsetFactor(renderer.getPolygonOffsetFactor(), false);
     }
   }
   
   public void resetDepthBufferOffsets() {
     Vector rendVec = getRendererVector();
     Iterator<DataRenderer> iter = rendVec.iterator();
     while (iter.hasNext()) {
       DataRenderer rend = iter.next();
       if (rend.hasPolygonOffset()) {
         rend.setHasPolygonOffset(false);
         rend.setPolygonOffset(0f);
         rend.setPolygonOffsetFactor(0f);
       }
     }
   }
  
}

