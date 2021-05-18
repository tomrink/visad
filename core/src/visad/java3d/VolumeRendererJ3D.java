//
// ImageRendererJ3D.java
//

/*
VisAD system for interactive analysis and visualization of numerical
data.  Copyright (C) 1996 - 2019 Bill Hibbard, Curtis Rueden, Tom
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

package visad.java3d;

import java.awt.image.BufferedImage;
import java.rmi.RemoteException;

import javax.media.j3d.BranchGroup;
import visad.BadMappingException;
import visad.CoordinateSystem;
import visad.CachingCoordinateSystem;
import visad.ColorAlphaControl;
import visad.ConstantMap;
import visad.InverseLinearScaledCS;
import visad.Data;
import visad.DataDisplayLink;
import visad.DataReferenceImpl;
import visad.Display;
import visad.DisplayException;
import visad.DisplayImpl;
import visad.DisplayRealType;
import visad.DisplayTupleType;
import visad.FieldImpl;
import visad.FlatField;
import visad.FunctionType;
import visad.GraphicsModeControl;
import visad.Gridded2DSet;
import visad.Integer3DSet;
import visad.Linear2DSet;
import visad.MathType;
import visad.RealTupleType;
import visad.RealType;
import visad.ScalarMap;
import visad.ScalarType;
import visad.ShadowType;
import visad.ShadowRealType;
import visad.ShadowRealTupleType;
import visad.ShadowFunctionOrSetType;
import visad.VisADError;
import visad.VisADException;
import visad.util.Util;

/**
   ImageRendererJ3D is the VisAD class for fast loading of images
   and image sequences under Java3D.

   WARNING - when reUseFrames is true during doTransform()
   ImageRendererJ3D makes these assumptions:
   1. That the images in a new time sequence are identical to
   any images at the same time in a previous sequence.
   2. That the image sequence defines the entire animation
   sampling.<P>
*/
public class VolumeRendererJ3D extends DefaultRendererJ3D {

  // FOR DEVELOPMENT PURPOSES //////////////////////////////////
  private static final int DEF_IMG_TYPE;

  //GEOMETRY/COLORBYTE REUSE LOGIC VARIABLES (STARTS HERE)
  private int last_curve_size = -1;
  private float last_zaxis_value = Float.NaN;
  private float last_alpha_value = Float.NaN;
  private long last_data_hash_code = -1;
  private boolean adjust_projection_seam = false; //27FEB2012: Projection Seam Change Bug Fix
  //GEOMETRY/COLORBYTE REUSE LOGIC VARIABLES (ENDS HERE)



  static {
    String val = System.getProperty("visad.java3d.8bit", "false");
    if (Boolean.parseBoolean(val)) {
      DEF_IMG_TYPE = BufferedImage.TYPE_BYTE_GRAY;
      System.err.println("WARN: 8bit enabled via system property");
    } else {
      DEF_IMG_TYPE = BufferedImage.TYPE_4BYTE_ABGR;
    }
  }
  //////////////////////////////////////////////////////////////
  
  // MathTypes that data must equalsExceptNames()
  private static MathType image_sequence_type, image_type;
  private static MathType image_sequence_type2, image_type2;
  private static MathType image_sequence_type3, image_type3;

  // initialize above MathTypes
  static {
    try {
      image_type = MathType.stringToType(
        "((ImageElement, ImageLine) -> ImageValue)");
      image_sequence_type = new FunctionType(RealType.Time, image_type);
      image_type2 = MathType.stringToType(
        "((ImageElement, ImageLine) -> (ImageValue))");
      image_sequence_type2 = new FunctionType(RealType.Time, image_type2);
      image_type3 = MathType.stringToType(
        "((ImageElement, ImageLine) -> (Red, Green, Blue))");
      image_sequence_type3 = new FunctionType(RealType.Time, image_type3);
    }
    catch (VisADException e) {
      throw new VisADError(e.getMessage());
    }
  }

  /** determine whether the given MathType is usable with ImageRendererJ3D */
  public static boolean isImageType(MathType type) {
    return (image_sequence_type.equalsExceptName(type) ||
            image_sequence_type2.equalsExceptName(type) ||
            image_sequence_type3.equalsExceptName(type) ||
            image_type.equalsExceptName(type) ||
            image_type2.equalsExceptName(type) ||
            image_type3.equalsExceptName(type));
  }

  /** @deprecated Use isRendererUsable(MathType, ScalarMap[]) instead. */
  public static void verifyImageRendererUsable(MathType type, ScalarMap[] maps)
    throws VisADException
  {
    isRendererUsable(type, maps);
  }

  /** determine whether the given MathType and collection of ScalarMaps
      meets the criteria to use ImageRendererJ3D. Throw a VisADException
      if ImageRenderer cannot be used, otherwise return true. */
  public static boolean isRendererUsable(MathType type, ScalarMap[] maps)
    throws VisADException
  {
    RealType time = null;
    RealTupleType domain = null;
    RealTupleType range = null;
    RealType x = null, y = null;
    RealType rx = null, ry = null; // WLH 19 July 2000
    RealType r = null, g = null, b = null;
    RealType rgb = null;

    // must be a function
    if (!(type instanceof FunctionType)) {
      throw new VisADException("Not a FunctionType");
    }
    FunctionType function = (FunctionType) type;
    RealTupleType functionD = function.getDomain();
    MathType functionR = function.getRange();

    // time function
    if (function.equalsExceptName(image_sequence_type) ||
      function.equalsExceptName(image_sequence_type2) ||
      function.equalsExceptName(image_sequence_type3))
    {
      // strip off time RealType
      time = (RealType) functionD.getComponent(0);
      function = (FunctionType) functionR;
      functionD = function.getDomain();
      functionR = function.getRange();
    }

    // ((ImageLine, ImageElement) -> ImageValue)
    // ((ImageLine, ImageElement) -> (ImageValue))
    // ((ImageLine, ImageElement) -> (Red, Green, Blue))
    if (function.equalsExceptName(image_type) ||
        function.equalsExceptName(image_type2) ||
        function.equalsExceptName(image_type3)) {
      domain = function.getDomain();
      MathType rt = function.getRange();
      if (rt instanceof RealType) {
        range = new RealTupleType((RealType) rt);
      }
      else if (rt instanceof RealTupleType) {
        range = (RealTupleType) rt;
      } 
      else {
        // illegal MathType
        throw new VisADException("Illegal RangeType");
      }
    }
    else {
      // illegal MathType
      throw new VisADException("Illegal MathType");
    }

    // extract x and y from domain
    x = (RealType) domain.getComponent(0);
    y = (RealType) domain.getComponent(1);

    // WLH 19 July 2000
    CoordinateSystem cs = domain.getCoordinateSystem();
    if (cs != null) {
      RealTupleType rxy = cs.getReference();
      rx = (RealType) rxy.getComponent(0);
      ry = (RealType) rxy.getComponent(1);
    }

    // extract colors from range
    int dim = range.getDimension();
    if (dim == 1) rgb = (RealType) range.getComponent(0);
    else { // dim == 3
      r = (RealType) range.getComponent(0);
      g = (RealType) range.getComponent(1);
      b = (RealType) range.getComponent(2);
    }

    // verify that collection of ScalarMaps is legal
    boolean btime = (time == null);
    boolean bx = false, by = false;
    boolean brx = false, bry = false; // WLH 19 July 2000
    boolean br = false, bg = false, bb = false;
    boolean dbr = false, dbg = false, dbb = false;
    Boolean latlon = null;
    DisplayRealType spatial = null;

    for (int i=0; i<maps.length; i++) {
      ScalarMap m = maps[i];
      ScalarType md = m.getScalar();
      DisplayRealType mr = m.getDisplayScalar();
      boolean ddt = md.equals(time);
      boolean ddx = md.equals(x);
      boolean ddy = md.equals(y);
      boolean ddrx = md.equals(rx);
      boolean ddry = md.equals(ry);
      boolean ddr = md.equals(r);
      boolean ddg = md.equals(g);
      boolean ddb = md.equals(b);
      boolean ddrgb = md.equals(rgb);

      // animation mapping
      if (ddt) {
        if (btime) throw new VisADException("Multiple Time mappings");
        if (!mr.equals(Display.Animation)) {
          throw new VisADException(
            "Time mapped to something other than Animation");
        }
        btime = true;
      }

      // spatial mapping
      else if (ddx || ddy || ddrx || ddry) {
        if (ddx && bx || ddy && by || ddrx && brx || ddry && bry) {
          throw new VisADException("Duplicate spatial mappings");
        }
        if (((ddx || ddy) && (brx || bry)) ||
            ((ddrx || ddry) && (bx || by))) {
          throw new VisADException("reference and non-reference spatial mappings");
        }
        RealType q = (ddx ? x : null);
        if (ddy) q = y;
        if (ddrx) q = rx;
        if (ddry) q = ry;

        boolean ll;
        if (mr.equals(Display.XAxis) || mr.equals(Display.YAxis) ||
          mr.equals(Display.ZAxis))
        {
          ll = false;
        }
        else if (mr.equals(Display.Latitude) || mr.equals(Display.Longitude) ||
          mr.equals(Display.Radius))
        {
          ll = true;
        }
        else throw new VisADException("Illegal domain mapping");

        if (latlon == null) {
          latlon = new Boolean(ll);
          spatial = mr;
        }
        else if (latlon.booleanValue() != ll) {
          throw new VisADException("Multiple spatial coordinate systems");
        }
        // two mappings to the same spatial DisplayRealType are not allowed
        else if (spatial == mr) {
          throw new VisADException(
            "Multiple mappings to the same spatial DisplayRealType");
        }

        if (ddx) bx = true;
        else if (ddy) by = true;
        else if (ddrx) brx = true;
        else if (ddry) bry = true;
      }

      // rgb mapping
      else if (ddrgb) {
        if (br || bg || bb) {
          throw new VisADException("Duplicate color mappings");
        }
        if (rgb == null ||
            !(mr.equals(Display.RGB) || mr.equals(Display.RGBA))) {
          throw new VisADException("Illegal RGB/RGBA mapping");
        }
        dbr = dbg = dbb = true;
        br = bg = bb = true;
      }

      // color mapping
      else if (ddr || ddg || ddb) {
        if (rgb != null) throw new VisADException("Illegal RGB mapping");
        RealType q = (ddr ? r : (ddg ? g : b));
        if (mr.equals(Display.Red)) dbr = true;
        else if (mr.equals(Display.Green)) dbg = true;
        else if (mr.equals(Display.Blue)) dbb = true;
        else throw new VisADException("Illegal color mapping");

        if (ddr) br = true;
        else if (ddg) bg = true;
        else bb = true;
      }

      // illegal ScalarMap involving this MathType
      else if (ddt || ddx || ddy || ddrx || ddry ||
        ddr || ddg || ddb || ddrgb)
      {
        throw new VisADException("Illegal mapping: " + m);
      }
    }

    // return true if all conditions for ImageRendererJ3D are met
    if (!(btime && ((bx && by) || (brx && bry)) &&
          br && bg && bb && dbr && dbg && dbb)) {
      throw new VisADException("Insufficient mappings");
    }
    return true;
  }

  // flag to indicate:
  // 1. That the images in a new time sequence are identical to
  //    any images at the same time in a previous sequence.
  // 2. That the image sequence defines the entire animation
  //    sampling.<P>
  private boolean reUseFrames = false;

  private int suggestedBufImgType = DEF_IMG_TYPE;
  
  private boolean setSetOnReUseFrames = true;

  private VisADImageNode imagesNode = null;

  private boolean lastByRef = false;
  
  private boolean forceUseByRef = false;
  
  public void forceUseByRef(boolean yesno) {
     forceUseByRef = yesno;
  }

  public static boolean isVolByRefUsable(DataDisplayLink link, ShadowType shadow) throws VisADException, RemoteException {
        ShadowFunctionOrSetType shadowType = (ShadowFunctionOrSetType) shadow.getAdaptedShadowType();
        CoordinateSystem dataCoordinateSystem = null;
        FlatField fltField = null;
        int num_images = 1;
        FieldImpl field = (FieldImpl) link.getData();
        if (!(field instanceof FlatField)) {
                num_images = field.getDomainSet().getLength();
                if (1 == num_images) { //If there is a single image in the animation dont do anything, simply return true
                        return true;
                }
                shadowType = (ShadowFunctionOrSetType) shadowType.getRange();
                fltField = (FlatField) field.getSample(0);
                dataCoordinateSystem = fltField.getDomainCoordinateSystem();
        } else {
                dataCoordinateSystem = ((FlatField)field).getDomainCoordinateSystem();
                return true;
        }

        // cs might be cached
        if (dataCoordinateSystem instanceof CachingCoordinateSystem) {
                dataCoordinateSystem = ((CachingCoordinateSystem) dataCoordinateSystem).getCachedCoordinateSystem();
        }

        ShadowRealType[] DomainComponents = shadowType.getDomainComponents();
        ShadowRealTupleType Domain = shadowType.getDomain();
        ShadowRealTupleType domain_reference = Domain.getReference();
        ShadowRealType[] DC = DomainComponents;

        if (domain_reference != null &&
                domain_reference.getMappedDisplayScalar()) {
                DC = shadowType.getDomainReferenceComponents();
        }

        DisplayTupleType spatial_tuple = null;
        for (int i=0; i<DC.length; i++) {
                java.util.Enumeration maps = DC[i].getSelectedMapVector().elements();
                ScalarMap map = (ScalarMap) maps.nextElement();
                DisplayRealType real = map.getDisplayScalar();
                spatial_tuple = real.getTuple();
        }

        CoordinateSystem coord = spatial_tuple.getCoordinateSystem();

        if (coord instanceof CachingCoordinateSystem) {
                coord = ((CachingCoordinateSystem)coord).getCachedCoordinateSystem();
        }

        boolean useLinearTexture = false;
        if (coord instanceof InverseLinearScaledCS) {
                InverseLinearScaledCS invCS = (InverseLinearScaledCS)coord;
                useLinearTexture = (invCS.getInvertedCoordinateSystem()).equals(dataCoordinateSystem);
        }

        /* If useLinearTexture is true at this point, it's true for the first image of a sequence with numimages > 1. However, the transformations
           from Reference to Display must be identical to one another for all images in the sequence for byRef to be applicable.
           There's no guarantee that equal CoordinateSystems describe the same transformation. Additionally, such transforms may not be analytically
           specified, but only sampled at the domain points. For high resolution imagery, at test of equality for each point could incur a 
           substantial performance hit (one of the main motivations of this custom renderer). 
           So below, we test 5 points: the four corners and center.
           Further consideration: how much slop to allow? For example GOES-16/17 are reference on a fixed grid, but earlier series were not.
           Should we allow a few image pixels offset from image to image?
         */

        if (useLinearTexture) { //If DisplayCoordinateSystem != DataCoordinateSystem
//                RealTupleType dataCoordSysRef = dataCoordinateSystem.getReference();
//                boolean isEarthRef = (dataCoordSysRef.equals(RealTupleType.LatitudeLongitudeTuple) || dataCoordSysRef.equals(RealTupleType.SpatialEarth2DTuple));
                
                if (num_images > 1) { //Its an animation
                        Gridded2DSet domSet = (Gridded2DSet) fltField.getDomainSet();
                        int lengths[] = ((visad.GriddedSet) fltField.getDomainSet()).getLengths();
                        int data_width = lengths[0];
                        int data_height = lengths[1];
                        
                        // Determine reference coordinate (likely Earth) system locations of the four corners and center of domain
                        float[][] grid = new float[][] {{0,data_width-1,data_width-1,0,data_width/2},{0,0,data_height-1,data_height-1,data_height/2}};
                        float[][] gridVal = domSet.gridToValue(grid);
                        float[][] lonlat0 = dataCoordinateSystem.toReference(gridVal);
                        
                        
                        for (int i = 1; i < num_images; i++) {
                                FlatField ff = (FlatField) field.getSample(i);
                                CoordinateSystem dcs = ff.getDomainCoordinateSystem();
                                // dcs might be cached
                                if (dcs instanceof CachingCoordinateSystem) {
                                        dcs = ((CachingCoordinateSystem) dcs).getCachedCoordinateSystem();
                                }
                                domSet = (Gridded2DSet) ff.getDomainSet();
                                int[] lens = domSet.getLengths();

                               
                                if (lens[0] != data_width || lens[1] != data_height) {
                                        useLinearTexture = false;
                                        break;
                                }
                                else {
                                        gridVal = domSet.gridToValue(grid);
                                        float[][] lonlat = dcs.toReference(gridVal);
                                        for (int k=0; k<grid[0].length;k++) {
                                            float lon0 = lonlat0[0][k];
                                            float lat0 = lonlat0[1][k];
                                            float lon = lonlat[0][k];
                                            float lat = lonlat[1][k];
                                            if (Float.isNaN(lon0) && Float.isNaN(lon) && Float.isNaN(lat0) && Float.isNaN(lat)) {
                                               continue;
                                            }
                                            if (!Util.isApproximatelyEqual(lon0, lon) || !Util.isApproximatelyEqual(lat0, lat)) {
                                               useLinearTexture = false;
                                               break;
                                            }  
                                        }
                                }
                        }
                }
        }

        return useLinearTexture;
  }

  // factory for ShadowFunctionType that defines unique behavior
  // for ImageRendererJ3D
  public ShadowType makeShadowFunctionType(
         FunctionType type, DataDisplayLink link, ShadowType parent)
         throws VisADException, RemoteException {
    return new ShadowVolByRefFunctionTypeJ3D(type, link, parent);
  }

  /**
   * Toggle the re-using of frames when a new image or set of images
   * is set in the datareference.<P>
   * <B>WARNING</B> - when reUseFrames is true during doTransform()
   * ImageRendererJ3D makes these assumptions:
   * <OL>
   * <LI>That the images in a new time sequence are identical to
   *     any images at the same time in a previous sequence.
   * <LI>That the image sequence defines the entire animation
   *     sampling.<P>
   * </OL>
   */
  public void setReUseFrames(boolean reuse) {
    reUseFrames = reuse;
  }

  /**
   * Suggest to the underlying shadow type the buffered image type
   * to use.
   * 
   * <b>Experimental</b>: This may changed or removed in future releases.
   */
  public void suggestBufImageType(int imageType) {
    switch (imageType) {
    case BufferedImage.TYPE_3BYTE_BGR:
    case BufferedImage.TYPE_4BYTE_ABGR:
    case BufferedImage.TYPE_BYTE_GRAY:
//    case BufferedImage.TYPE_USHORT_GRAY:
      break;
    default:
      throw new IllegalArgumentException("unsupported image type");
    }
    this.suggestedBufImgType = imageType;
  }
  
  /**
   * Get the image type. 
   * @return The buffered image type used to render the image.
   */
  int getSuggestedBufImageType() {
    return suggestedBufImgType;
  }

  public void setImageNode(VisADImageNode node) {
    this.imagesNode = node;
  }

  public VisADImageNode getImageNode() {
    return this.imagesNode;
  }
  
  /**
   * Turn on the reusing of frames
   * @deprecated - use setReUseFrames(boolean reuse)
   */
  public void setReUseFrames() {
    setReUseFrames(true);
  }

  public boolean getReUseFrames() {
    return reUseFrames;
  }

  public void setSetSetOnReUseFrames(boolean ss) {
    setSetOnReUseFrames = ss;
  }

  public boolean getSetSetOnReUseFrames() {
    return setSetOnReUseFrames;
  }

  // logic to allow ShadowImageFunctionTypeJ3D to 'mark' missing frames
  private VisADBranchGroup vbranch = null;

  public void clearScene() {
    vbranch = null;
    super.clearScene();
  }

  void setVisADBranch(VisADBranchGroup branch) {
    vbranch = branch;
  }

  void markMissingVisADBranch() {
    if (vbranch != null) vbranch.scratchTime();
  }
  // end of logic to allow ShadowImageFunctionTypeJ3D to 'mark' missing frames

  public BranchGroup doTransform() throws VisADException, RemoteException {

    DataDisplayLink[] Links = getLinks();
    if (Links == null || Links.length == 0) {
      return null;
    }

    DataDisplayLink link = Links[0];
    ShadowTypeJ3D type = (ShadowTypeJ3D) link.getShadow();
    boolean doByRef = false;
    if ((isVolByRefUsable(link, type) && ShadowType.byReference) || forceUseByRef) {
      doByRef = true;
      type = new ShadowVolByRefFunctionTypeJ3D(link.getData().getType(), link, null, 
                     ((ShadowFunctionOrSetType)type.getAdaptedShadowType()).getInheritedValues(),
                          (ShadowFunctionOrSetType)type.getAdaptedShadowType(), type.getLevelOfDifficulty());
    }

    BranchGroup branch = null;
    if ((lastByRef && doByRef) || (!lastByRef && !doByRef)) { 
      branch = getBranch();
    }
    lastByRef = doByRef;

    if (branch == null) {
      branch = new BranchGroup();
      branch.setCapability(BranchGroup.ALLOW_DETACH);
      branch.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
      branch.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
      branch.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
      last_curve_size = -1;
      last_zaxis_value = Float.NaN;
      last_alpha_value = Float.NaN;
      last_data_hash_code = -1; 
      adjust_projection_seam = false; //27FEB2012: Projection Seam Change Bug Fix
    }


    // initialize valueArray to missing
    int valueArrayLength = getDisplay().getValueArrayLength();
    float[] valueArray = new float[valueArrayLength];
    for (int i=0; i<valueArrayLength; i++) {
      valueArray[i] = Float.NaN;
    }

    Data data;
    try {
      data = link.getData();
    } catch (RemoteException re) {
      if (visad.collab.CollabUtil.isDisconnectException(re)) {
        getDisplay().connectionFailed(this, link);
        removeLink(link);
        return null;
      }
      throw re;
    }

    if (data == null) {
      branch = null;
      addException(
        new DisplayException("Data is null: DefaultRendererJ3D.doTransform"));
    }
    else {
      // check MathType of non-null data, to make sure it is a single-band
      // image or a sequence of single-band images
      MathType mtype = link.getType();
      if (!isImageType(mtype)) {
        throw new BadMappingException("must be image or image sequence");
      }
      link.start_time = System.currentTimeMillis();
      link.time_flag = false;
      vbranch = null;
      // transform data into a depiction under branch
	long t1 = System.currentTimeMillis();
      try {
	if (type instanceof ShadowVolByRefFunctionTypeJ3D) { //GEOMETRY/COLORBYTE REUSE LOGIC Only for ByRef for Time being
		if (checkAction()) { //This generally decides whether at all retransformation is required or not.
	        	type.doTransform(branch, data, valueArray,
                         	link.getDefaultValues(), this);
		}
	} else {	//Not byRef (ShadowImageFunctionTypeJ3D)
		type.doTransform(branch, data, valueArray,
                         link.getDefaultValues(), this);
	}
      } catch (RemoteException re) {
        if (visad.collab.CollabUtil.isDisconnectException(re)) {
          getDisplay().connectionFailed(this, link);
          removeLink(link);
          return null;
        }
        throw re;
      }
	long t2 = System.currentTimeMillis();
	//System.err.println("Time taken:" + (t2-t1));
    }
    link.clearData();

    return branch;
  }

  public Object clone() {
    return new VolumeRendererJ3D();
  }

//GEOMETRY/COLORBYTE REUSE UTILITY METHODS (STARTS HERE)
  public int getLastCurveSize() {
	return last_curve_size;
  }

  public void setLastCurveSize(int csize) {
	last_curve_size = csize;
  }

  public float getLastZAxisValue() {
	return last_zaxis_value;
  }
  public void setLastZAxisValue(float zaxis_value) {
	  last_zaxis_value = zaxis_value;
  }

  public float getLastAlphaValue() {
	return last_alpha_value;
  }

  public void setLastAlphaValue(float alpha) {
        last_alpha_value = alpha;
  }

  public long getLastDataHashCode() {
	return last_data_hash_code;
  } 

  public void setLastDataHashCode(long lastdata_hashcode) {
	last_data_hash_code = lastdata_hashcode;
  }

  //27FEB2012: Projection Seam Change Bug Fix (starts here)
  public boolean getLastAdjustProjectionSeam() {
        return adjust_projection_seam;
  }

  public void setLastAdjustProjectionSeam(boolean adjust) {
         adjust_projection_seam = adjust;
  }
  //27FEB2012: Projection Seam Change Bug Fix (ends here)

  public static float[][] buildTable(float[][] table)
  {
    int length = table[0].length;
    for (int i=0; i<length; i++) {
      float a = ((float) i) / ((float) (table[3].length - 1));
      table[3][i] = a;
    }
    return table;
  }

  public static void main(String[] args) throws VisADException, RemoteException {
    DisplayImpl display = new DisplayImplJ3D("vol rend");
    RealType xr = RealType.getRealType("xr");
    RealType yr = RealType.getRealType("yr");
    RealType zr = RealType.getRealType("zr");
    RealType wr = RealType.getRealType("wr");
    RealType[] types3d = {xr, yr, zr};
    RealTupleType earth_location3d = new RealTupleType(types3d);
    FunctionType grid_tuple = new FunctionType(earth_location3d, wr);

    // int NX = 32;
    // int NY = 32;
    // int NZ = 32;
    int NX = 35;
    int NY = 35;
    int NZ = 35;
    Integer3DSet set = new Integer3DSet(NX, NY, NZ);
    FlatField grid3d = new FlatField(grid_tuple, set);

    float[][] values = new float[1][NX * NY * NZ];
    int k = 0;
    for (int iz=0; iz<NZ; iz++) {
      // double z = Math.PI * (-1.0 + 2.0 * iz / (NZ - 1.0));
      double z = Math.PI * (-1.0 + 2.0 * iz * iz / ((NZ - 1.0)*(NZ - 1.0)) );
      for (int iy=0; iy<NY; iy++) {
        double y = -1.0 + 2.0 * iy / (NY - 1.0);
        for (int ix=0; ix<NX; ix++) {
          double x = -1.0 + 2.0 * ix / (NX - 1.0);
          double r = x - 0.5 * Math.cos(z);
          double s = y - 0.5 * Math.sin(z);
          double dist = Math.sqrt(r * r + s * s);
          values[0][k] = (float) ((dist < 0.1) ? 10.0 : 1.0 / dist);
          k++;
        }
      }
    }
    grid3d.setSamples(values);

    display.addMap(new ScalarMap(xr, Display.XAxis));
    display.addMap(new ScalarMap(yr, Display.YAxis));
    display.addMap(new ScalarMap(zr, Display.ZAxis));

    ScalarMap xrange = new ScalarMap(xr, Display.SelectRange);
    ScalarMap yrange = new ScalarMap(yr, Display.SelectRange);
    ScalarMap zrange = new ScalarMap(zr, Display.SelectRange);
    display.addMap(xrange);
    display.addMap(yrange);
    display.addMap(zrange);

    GraphicsModeControl mode = display.getGraphicsModeControl();
    mode.setScaleEnable(true);

    mode.setTransparencyMode(DisplayImplJ3D.NICEST);
    mode.setTexture3DMode(GraphicsModeControl.STACK2D);

    // new
    RealType duh = RealType.getRealType("duh");
    int NT = 32;
    Linear2DSet set2 = new Linear2DSet(0.0, (double) NX, NT,
                                       0.0, (double) NY, NT);
    RealType[] types2d = {xr, yr};
    RealTupleType domain2 = new RealTupleType(types2d);
    FunctionType ftype2 = new FunctionType(domain2, duh);
    float[][] v2 = new float[1][NT * NT];
    for (int i=0; i<NT*NT; i++) {
      v2[0][i] = (i * i) % (NT/2 +3);
    }
    // float[][] v2 = {{1.0f,2.0f,3.0f,4.0f}};
    FlatField field2 = new FlatField(ftype2,set2);
    field2.setSamples(v2);
    display.addMap(new ScalarMap(duh, Display.RGB));

    ScalarMap map1color = new ScalarMap(wr, Display.RGBA);
    display.addMap(map1color);

    ColorAlphaControl control = (ColorAlphaControl) map1color.getControl();
    control.setTable(buildTable(control.getTable()));

    DataReferenceImpl ref_grid3d = new DataReferenceImpl("ref_grid3d");
    ref_grid3d.setData(grid3d);

    DataReferenceImpl ref2 = new DataReferenceImpl("ref2");
    ref2.setData(field2);

    ConstantMap[] cmaps = {new ConstantMap(0.0, Display.TextureEnable)};
    display.addReference(ref2, cmaps);

    display.addReference(ref_grid3d, null);   
  }

}
