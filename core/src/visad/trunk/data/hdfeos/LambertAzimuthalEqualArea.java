//
// LambertAzimuthalEqualArea.java
//

/*
VisAD system for interactive analysis and visualization of numerical
data.  Copyright (C) 1996 - 1998 Bill Hibbard, Curtis Rueden, Tom
Rink and Dave Glowacki.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 1, or (at your option)
any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License in file NOTICE for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
*/

package visad.data.hdfeos;

import visad.*;

/**
   LambertAzimuthalEqualArea is the VisAD class for cordinate
   systems for ( X_map, Y_map ).<P>
*/

public class LambertAzimuthalEqualArea extends CoordinateSystem {

  double R;
  double lon_center;
  double lat_center;
  double false_easting;
  double false_northing;
  double sin_lat_o;
  double cos_lat_o;

  private static Unit[] coordinate_system_units =
    {SI.radian, SI.radian};

  public LambertAzimuthalEqualArea( RealTupleType reference, 
                                    double R, 
                                    double lon_center, 
                                    double lat_center,
                                    double false_easting, 
                                    double false_northing 
                                                              )
  throws VisADException
  {

    super( reference, coordinate_system_units );

    this.R = R;
    this.lon_center = lon_center;
    this.lat_center = lat_center;
    this.false_easting = false_easting;
    this.false_northing = false_northing;
    this.sin_lat_o = Math.sin( lat_center  );
    this.cos_lat_o = Math.cos( lat_center  );


  }

  public double[][] toReference(double[][] tuples) throws VisADException {

     double Rh;
     double x;
     double y;
     double z;               // Great circle dist from proj center to given point
     double sin_z;           // Sine of z
     double cos_z;           // Cosine of z
     double temp;            // Re-used temporary variable
     double lon;
     double lat;
     Double dum_1 = null;
     Double dum_2 = null;

     int n_tuples = tuples[0].length;
     int tuple_dim = tuples.length;

     if ( tuple_dim != 2) {
       throw new VisADException("LambertAzimuthalEqualArea: tuple dim != 2");
     }

     double t_tuples[][] = new double[2][n_tuples];

     for ( int ii = 0; ii < n_tuples; ii++ ) {

       x = tuples[0][ii] - false_easting;
       y = tuples[0][ii] - false_northing;
       Rh = Math.sqrt(x * x + y * y);
       temp = Rh / (2.0 * R);
       if (temp > 1)
       {
         // p_error("Input data error", "lamaz-inverse");
       }
       z = 2.0 * gctpFunction.asinz(temp);
       gctpFunction.sincos(z, dum_1, dum_2);
       sin_z = dum_1.doubleValue();
       cos_z = dum_2.doubleValue();
       lon = lon_center;
       if ( Math.abs(Rh) > gctpFunction.EPSLN )
       {
         lat = gctpFunction.asinz(sin_lat_o * cos_z + cos_lat_o * sin_z * y / Rh);
         temp = Math.abs(lat_center) - gctpFunction.HALF_PI;
         if (Math.abs(temp) > gctpFunction.EPSLN)
         {
           temp = cos_z - sin_lat_o * Math.sin(lat);
           if(temp!=0.0) {
             lon = gctpFunction.adjust_lon(lon_center+Math.atan2(x*sin_z*cos_lat_o,temp*Rh));
           }
         }
         else if (lat_center < 0.0) {
           lon = gctpFunction.adjust_lon(lon_center - Math.atan2(-x, y));
         }
         else {
           lon = gctpFunction.adjust_lon(lon_center + Math.atan2(x, -y));
         }
       }
       else {
         lat = lat_center;
       }

       t_tuples[0][ii] = lat;
       t_tuples[1][ii] = lon;
     }

     return t_tuples;
  }

  public double[][] fromReference(double[][] tuples) throws VisADException {

     int n_tuples = tuples[0].length;
     int tuple_dim = tuples.length;
     double ksp;
     double g;

     if ( tuple_dim != 2) {
        throw new VisADException("LambertAzimuthalEqualArea: tuple dim != 2");
     }

     double t_tuples[][] = new double[2][n_tuples];
     double[] delta_lon = new double[n_tuples];
     double[] sin_lat = new double[n_tuples];
     double[] cos_lat = new double[n_tuples];
     double[] sin_delta_lon = new double[n_tuples];
     double[] cos_delta_lon = new double[n_tuples];

     for ( int ii = 0; ii < n_tuples; ii++ ) {
        delta_lon[ii] = tuples[1][ii] - lon_center; 
     }

     gctpFunction.adjust_lon( delta_lon );

     gctpFunction.sincos( tuples[0], sin_lat, cos_lat );
     gctpFunction.sincos( delta_lon, sin_delta_lon, cos_delta_lon );


     for ( int ii = 0; ii < n_tuples; ii++ ) {

       g = sin_lat_o * sin_lat[ii] + cos_lat_o * cos_lat[ii] * cos_delta_lon[ii];
       if ( g == -1 ) {
          throw new VisADException( "Point projects to a circle of radius = "+(2.*R) );
       }

       ksp = R * Math.sqrt(2.0 / (1.0 + g));

       t_tuples[0][ii] = ksp * cos_lat[ii] * sin_delta_lon[ii] + false_easting;
       t_tuples[1][ii] = ksp * (cos_lat_o * sin_lat[ii] - 
                         sin_lat_o * cos_lat[ii] * cos_delta_lon[ii]) +
                         false_northing;
     } 

     
     delta_lon = null;
     sin_lat = null;
     cos_lat = null;
     sin_delta_lon = null; 
     cos_delta_lon = null;

     return t_tuples;
  }

  public boolean equals(Object cs) {
    return ( cs instanceof LambertAzimuthalEqualArea );
  }


  public static void main(String args[]) throws VisADException {


     CoordinateSystem coord_cs1 = null;
     RealType real1;
     RealType real2;
     double[][] value_in = { {0, .5236, 1.0472, 1.5708}, {1, 1, 1, 1}};
     double[][] value_out = new double[2][4];

     real1 = new RealType("Theta", SI.radian, null);
     real2 = new RealType("radius", SI.meter, null);
     RealType reals[] = {real1, real2};

     RealTupleType Reference = new RealTupleType(reals);

   //  coord_cs1 = new LambertAzimuthalEqualArea( Reference, null );

     RealTupleType tuple1 = new RealTupleType( reals, coord_cs1, null);

     value_out = tuple1.getCoordinateSystem().fromReference( value_in );

     for ( int i=0; i<value_out[0].length; i++) {
        System.out.println(value_out[0][i]+",  "+value_out[1][i]);
     }

     value_in = tuple1.getCoordinateSystem().toReference( value_out );

     for ( int i=0; i<value_in[0].length; i++) {
        System.out.println(value_in[0][i]+",  "+value_in[1][i]);
     }

     Unit kilometer = new ScaledUnit(1000, SI.meter);

  }
}
