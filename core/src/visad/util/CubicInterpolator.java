package visad.util;

import Jama.Matrix;
import Jama.LUDecomposition;
import java.util.Arrays;

public class CubicInterpolator {

      LUDecomposition solver;

      double[][] solution = null;

      double x0 = 0;
      double x1 = 0;
      double x2 = 0;
      double x0_last = 0;
      double x0_save;
      
      float[] values0 = null;
      float[] values1 = null;
      float[] values2 = null;
      float[] values0_last = null;
      float[] values0_save = null;

      int numSpatialPts = 1;

      boolean doIntrp = true;
      
      boolean[] needed = null;
      boolean[] computed = null;
      
      public CubicInterpolator(boolean doIntrp, int numSpatialPts) {
         this.doIntrp = doIntrp;
         this.numSpatialPts = numSpatialPts;
         this.solution = new double[4][numSpatialPts];
         this.needed = new boolean[numSpatialPts];
         this.computed = new boolean[numSpatialPts];
         Arrays.fill(needed, false);
         Arrays.fill(computed, false);
      }

      private void buildSolver() {
         double x0_p3 = x0*x0*x0;
         double x1_p3 = x1*x1*x1;

         double x0_p2 = x0*x0;
         double x1_p2 = x1*x1;

         Matrix coeffs = new Matrix(new double[][]
              { {x0_p3, x0_p2, x0, 1},
                {x1_p3, x1_p2, x1, 1},
                {3*x0_p2, 2*x0, 1, 0},
                {3*x1_p2, 2*x1, 1, 0}}, 4, 4);

         solver = new LUDecomposition(coeffs);         
      }

      public void interpolate(double xt, float[] interpValues) {
         if (!doIntrp) {
            if (xt == x0) {
               System.arraycopy(values0, 0, interpValues, 0, numSpatialPts);
            }
            else if (xt == x1) {
               System.arraycopy(values1, 0, interpValues, 0, numSpatialPts);
            }
            return;
         }
         java.util.Arrays.fill(interpValues, Float.NaN);
         
         for (int k=0; k<numSpatialPts; k++) {
            if (!computed[k]) { // don't need to interp at these locations, at this time
                continue;
            }
            interpValues[k] = (float) cubic_poly(xt, solution[0][k], solution[1][k], solution[2][k], solution[3][k]);
         }
      }

      public void next(double x0, double x1, double x2, float[] values0, float[] values1, float[] values2) {
         this.x0 = x0;
         this.x1 = x1;
         this.x2 = x2;
         this.values0 = values0;
         this.values1 = values1;
         this.values2 = values2;
         
         this.x0_last = x0_save;
         this.x0_save = x0;
         this.values0_last = values0_save;
         this.values0_save = values0;
         Arrays.fill(computed, false);
         
         if (!doIntrp) {
           return;
         }
         
         buildSolver();
      }
 
      public void update(boolean[] needed) {
          java.util.Arrays.fill(this.needed, false);
          for (int k=0; k<numSpatialPts; k++) {
              if (needed[k]) {
                  if (!computed[k]) {
                      this.needed[k] = true;
                  }
              }
          }
          if (doIntrp) {
                getSolution();
          }
      }
      
      private void getSolution() {
         for (int k=0; k<numSpatialPts; k++) {
            if (!this.needed[k]) {
                continue;
            }
            
            double D1_1 = Double.NaN;
            double D1_0 = Double.NaN;
            double y0 = values0[k];
            double y1 = values1[k];
            
            if (values0_last == null) {
               D1_0 = (values1[k] - values0[k])/(x1 - x0);
            }
            else {
               D1_0 = (values1[k] - values0_last[k])/(x1 - x0_last);
            }
            D1_1 = (values2[k] - values0[k])/(x2 - x0);
            
            double[] sol = getSolution(y0, y1, D1_0, D1_1);
            solution[0][k] = sol[0];
            solution[1][k] = sol[1];
            solution[2][k] = sol[2];
            solution[3][k] = sol[3];
            
            computed[k] = true;
         }
      }
      
      private double[] getSolution(double y0, double y1, double D1_0, double D1_1) {
        Matrix constants = new Matrix(new double[][]
             { {y0}, {y1}, {D1_0}, {D1_1} }, 4, 1);

        double[][] solution = (solver.solve(constants)).getArray();

        return new double[] {solution[0][0], solution[1][0], solution[2][0], solution[3][0]};
      }

      public static double cubic_poly_D1(double x, double a, double b, double c) {
         return 3*a*x*x + 2*b*x + c;
      }

      public static double cubic_poly(double x, double a, double b, double c, double d) {
         return a*x*x*x + b*x*x + c*x + d;
      }
  }
