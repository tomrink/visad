
C      subroutine GET_PROFIL
      subroutine GET_PROFIL(rlat, imon, tpro, wpro, opro, pref_copy)

c * Obtain climatological temperature, water-vapor mixing-ratio,
c       and ozone mixing-ratio profiles by interpolating in
c       month and latitude amongst the FASCODE model atmospheres.

c * Pressure coordinate is 40-level "satellite-standard" (0.1 to 1000 mb).

c   Input:
c         imon = month    (1,...,12)
c         rlat = latitude (degrees)

c   Output:
c         tpro = temperature profile (deg K)
c         wpro = water-vapor mixing ratio profile (g/kg)
c         opro = ozone mixing ratio profile (ppmv)

c   NOTE: For abs(latitude) .le. 15 degrees, the (annual mean) tropical
c           atmosphere is returned, regardless of month.

c .... no. levels, no. seasons, no. zones
        parameter (nl=40,ns=2,nz=3)
        dimension tfas(nl,nz,ns),tmr(nl,ns),tpro(nl)
        dimension wfas(nl,nz,ns),wmr(nl,ns),wpro(nl)
        dimension ofas(nl,nz,ns),omr(nl,ns),opro(nl)
        dimension pref(nl),pref_copy(nl)
        real*4 tlat(nz)/15.,40.,65./

c * PRESSURE LEVELS
        data pref/ .1,.2,.5,1.,1.5,2.,3.,4.,5.,7.,10.,15.,20.,25.,30.,
     +  50.,60.,70.,85.,100.,115.,135.,150.,200.,250.,300.,350.,400.,
     +  430.,475.,500.,570.,620.,670.,700.,780.,850.,920.,950.,1000./
c * TROPICAL
        data (tfas(i,1,1),i=1,nl)/
     +  231.57, 248.62, 264.40, 269.89, 265.69, 260.98, 254.27, 249.54,
     +  245.97, 240.73, 235.29, 229.33, 225.13, 221.81, 219.20, 209.70,
     +  205.24, 201.55, 197.07, 195.64, 198.37, 204.59, 208.75, 220.96,
     +  230.67, 239.25, 246.59, 253.14, 256.77, 261.82, 264.45, 271.35,
     +  275.88, 280.12, 282.53, 286.64, 290.51, 294.62, 296.32, 299.02/
        data (wfas(i,1,1),i=1,nl)/
     +   0.003,  0.004,  0.004,  0.004,  0.004,  0.004,  0.003,  0.003,
     +   0.003,  0.003,  0.003,  0.003,  0.003,  0.003,  0.003,  0.003,
     +   0.003,  0.003,  0.003,  0.003,  0.003,  0.003,  0.004,  0.013,
     +   0.052,  0.165,  0.353,  0.613,  0.790,  1.169,  1.403,  2.186,
     +   2.646,  3.965,  4.895,  8.419, 10.742, 12.729, 13.857, 15.660/
        data (ofas(i,1,1),i=1,nl)/
     + 0.55927,0.98223,1.94681,3.13498,4.30596,5.48908,7.41904,8.55498,
     + 9.22089,9.76594,9.60463,8.45820,6.99682,5.57585,4.30000,1.69985,
     + 1.23555,0.81780,0.39171,0.20944,0.14056,0.12283,0.10984,0.08405,
     + 0.06529,0.05392,0.04764,0.04366,0.04232,0.04052,0.03961,0.03735,
     + 0.03595,0.03534,0.03514,0.03385,0.03252,0.03107,0.03027,0.02901/
c * MIDLATITUDE WINTER
        data (tfas(i,2,1),i=1,nl)/
     +  241.64, 251.73, 263.19, 263.81, 255.01, 248.45, 239.48, 233.30,
     +  228.64, 221.93, 218.22, 215.95, 215.35, 215.20, 215.20, 215.20,
     +  215.20, 215.54, 216.16, 216.68, 217.12, 217.63, 217.97, 218.90,
     +  219.61, 225.79, 232.02, 237.54, 240.59, 244.84, 247.06, 252.82,
     +  256.58, 260.11, 261.94, 264.87, 267.22, 269.39, 270.28, 271.71/
        data (wfas(i,2,1),i=1,nl)/
     +   0.003,  0.003,  0.003,  0.003,  0.003,  0.003,  0.003,  0.003,
     +   0.003,  0.003,  0.003,  0.003,  0.003,  0.003,  0.003,  0.003,
     +   0.003,  0.003,  0.003,  0.003,  0.003,  0.003,  0.003,  0.005,
     +   0.016,  0.035,  0.071,  0.142,  0.227,  0.354,  0.426,  0.660,
     +   0.869,  1.165,  1.327,  1.691,  1.971,  2.253,  2.389,  2.606/
        data (ofas(i,2,1),i=1,nl)/
     + 0.58382,1.06610,2.23416,3.87594,5.18854,6.20949,7.04493,7.17104,
     + 7.10972,6.86107,6.29020,5.71788,5.35257,5.03882,4.57679,3.16918,
     + 2.47482,1.95801,1.43279,1.11336,0.93068,0.81309,0.74765,0.46038,
     + 0.25869,0.15587,0.10257,0.07960,0.06900,0.05625,0.05211,0.04120,
     + 0.03513,0.03297,0.03176,0.02883,0.02821,0.02796,0.02790,0.02781/
c * SUBARCTIC WINTER
        data (tfas(i,3,1),i=1,nl)/
     +  249.27, 254.08, 259.26, 248.94, 241.62, 236.66, 229.76, 224.97,
     +  221.69, 218.42, 216.13, 213.65, 211.92, 211.58, 212.27, 214.19,
     +  214.88, 215.46, 216.20, 216.82, 217.20, 217.20, 217.20, 217.20,
     +  217.20, 218.48, 223.08, 229.02, 232.35, 237.00, 239.43, 245.76,
     +  249.32, 252.17, 253.40, 255.98, 258.05, 258.59, 258.12, 257.39/
        data (wfas(i,3,1),i=1,nl)/
     +   0.003,  0.003,  0.003,  0.003,  0.003,  0.003,  0.003,  0.003,
     +   0.003,  0.003,  0.003,  0.003,  0.003,  0.003,  0.003,  0.003,
     +   0.003,  0.003,  0.003,  0.003,  0.003,  0.003,  0.003,  0.006,
     +   0.014,  0.019,  0.047,  0.106,  0.133,  0.199,  0.242,  0.427,
     +   0.567,  0.700,  0.760,  0.890,  0.965,  0.968,  0.937,  0.886/
        data (ofas(i,3,1),i=1,nl)/
     + 0.75492,1.20217,2.39283,3.75644,4.96742,5.64285,6.17910,6.22151,
     + 6.15197,5.88338,5.42542,4.91094,4.76030,4.63605,4.52229,3.70528,
     + 3.01350,2.39073,1.76424,1.38778,1.12046,0.82870,0.66060,0.36047,
     + 0.28088,0.17023,0.09235,0.06541,0.05169,0.04171,0.03941,0.03409,
     + 0.03095,0.02819,0.02673,0.02330,0.02159,0.01999,0.01933,0.01828/
c * TROPICAL (repeated for symmetry)
        data (tfas(i,1,2),i=1,nl)/
     +  231.57, 248.62, 264.40, 269.89, 265.69, 260.98, 254.27, 249.54,
     +  245.97, 240.73, 235.29, 229.33, 225.13, 221.81, 219.20, 209.70,
     +  205.24, 201.55, 197.07, 195.64, 198.37, 204.59, 208.75, 220.96,
     +  230.67, 239.25, 246.59, 253.14, 256.77, 261.82, 264.45, 271.35,
     +  275.88, 280.12, 282.53, 286.64, 290.51, 294.62, 296.32, 299.02/
        data (wfas(i,1,2),i=1,nl)/
     +   0.003,  0.004,  0.004,  0.004,  0.004,  0.004,  0.003,  0.003,
     +   0.003,  0.003,  0.003,  0.003,  0.003,  0.003,  0.003,  0.003,
     +   0.003,  0.003,  0.003,  0.003,  0.003,  0.003,  0.004,  0.013,
     +   0.052,  0.165,  0.353,  0.613,  0.790,  1.169,  1.403,  2.186,
     +   2.646,  3.965,  4.895,  8.419, 10.742, 12.729, 13.857, 15.660/
        data (ofas(i,1,2),i=1,nl)/
     + 0.55927,0.98223,1.94681,3.13498,4.30596,5.48908,7.41904,8.55498,
     + 9.22089,9.76594,9.60463,8.45820,6.99682,5.57585,4.30000,1.69985,
     + 1.23555,0.81780,0.39171,0.20944,0.14056,0.12283,0.10984,0.08405,
     + 0.06529,0.05392,0.04764,0.04366,0.04232,0.04052,0.03961,0.03735,
     + 0.03595,0.03534,0.03514,0.03385,0.03252,0.03107,0.03027,0.02901/
c * MIDLATITUDE SUMMER
        data (tfas(i,2,2),i=1,nl)/
     +  230.17, 249.31, 268.74, 275.62, 272.63, 267.38, 259.50, 254.07,
     +  249.96, 243.96, 237.90, 231.88, 228.02, 226.02, 224.46, 220.55,
     +  219.13, 217.85, 216.48, 215.70, 215.70, 215.70, 215.70, 220.45,
     +  230.07, 238.24, 245.33, 251.68, 255.15, 259.99, 262.43, 268.56,
     +  272.59, 276.36, 278.51, 283.83, 287.43, 290.47, 291.71, 293.70/
        data (wfas(i,2,2),i=1,nl)/
     +   0.003,  0.003,  0.003,  0.003,  0.003,  0.003,  0.003,  0.003,
     +   0.003,  0.003,  0.003,  0.003,  0.003,  0.003,  0.003,  0.003,
     +   0.003,  0.003,  0.003,  0.003,  0.003,  0.003,  0.003,  0.014,
     +   0.078,  0.201,  0.337,  0.526,  0.655,  0.882,  1.029,  1.607,
     +   2.269,  3.081,  3.563,  5.491,  7.276,  9.091,  9.946, 11.314/
        data (ofas(i,2,2),i=1,nl)/
     + 0.61951,1.07099,1.77685,2.91535,3.98547,5.06939,7.01746,8.18549,
     + 8.74393,8.73998,7.87205,6.65253,5.84694,5.12966,4.37609,2.46410,
     + 1.97307,1.47696,0.91259,0.66705,0.57759,0.48610,0.44729,0.24487,
     + 0.16974,0.12153,0.10001,0.08397,0.07669,0.06661,0.06225,0.05355,
     + 0.04892,0.04505,0.04291,0.03815,0.03517,0.03283,0.03194,0.03053/
c * SUBARCTIC SUMMER
        data (tfas(i,3,2),i=1,nl)/
     +  227.47, 250.23, 272.71, 277.16, 275.24, 272.35, 265.04, 258.38,
     +  253.33, 246.03, 239.14, 233.82, 230.69, 228.94, 227.34, 225.20,
     +  225.20, 225.20, 225.20, 225.20, 225.20, 225.20, 225.20, 225.20,
     +  225.20, 230.54, 237.97, 244.53, 248.15, 253.21, 255.93, 262.27,
     +  265.77, 269.05, 270.90, 275.59, 279.37, 282.91, 284.39, 286.74/
        data (wfas(i,3,2),i=1,nl)/
     +   0.003,  0.003,  0.003,  0.003,  0.003,  0.003,  0.003,  0.003,
     +   0.003,  0.003,  0.003,  0.003,  0.003,  0.003,  0.003,  0.003,
     +   0.003,  0.003,  0.003,  0.003,  0.003,  0.003,  0.003,  0.004,
     +   0.018,  0.068,  0.219,  0.439,  0.592,  0.835,  1.050,  1.669,
     +   2.146,  2.689,  2.995,  4.037,  4.885,  5.852,  6.391,  7.253/
        data (ofas(i,3,2),i=1,nl)/
     + 0.58825,0.98312,1.64271,2.52996,3.56841,4.56575,6.36529,7.39635,
     + 7.78289,7.56976,6.69058,5.57509,5.21478,4.73043,4.34708,2.75529,
     + 2.05541,1.64657,1.17452,0.92611,0.78889,0.65317,0.58224,0.40198,
     + 0.24128,0.15314,0.10010,0.08052,0.07394,0.06544,0.06065,0.04986,
     + 0.04448,0.04090,0.03887,0.03446,0.03129,0.02823,0.02682,0.02456/

C      rlat=ADREAL(1)
C      imon=INT(ADREAL(2))

        alat=abs(rlat)
      if(alat.gt.15.) go to 110
      jl=1
      kk=1
      go to 120
  110 if(alat.lt.65.) go to 130
      jl=3
      kk=2
  120 do k=1,kk
         do i=1,nl
            tmr(i,k)=tfas(i,jl,k)
            wmr(i,k)=wfas(i,jl,k)
            omr(i,k)=ofas(i,jl,k)
         enddo
      enddo
      if(kk.ne.1) go to 140
      do i=1,nl
         tpro(i)=tmr(i,1)
         wpro(i)=wmr(i,1)
         opro(i)=omr(i,1)
      enddo
      go to 150

c * interpolate in latitude

  130 jl1=1
      if(alat.gt.40.) jl1=2
      jl2=jl1+1
      wt1=(tlat(jl2)-alat)/25.
      wt2=1.-wt1
      do k=1,ns
         do i=1,nl
            t1=tfas(i,jl1,k)
            t2=tfas(i,jl2,k)
            tmr(i,k)=wt1*t1+wt2*t2
            w1=wfas(i,jl1,k)
            w2=wfas(i,jl2,k)
            wmr(i,k)=wt1*w1+wt2*w2
            o1=ofas(i,jl1,k)
            o2=ofas(i,jl2,k)
            omr(i,k)=wt1*o1+wt2*o2
         enddo
      enddo

c * interpolate in month

  140 nmon=imon
      if(rlat.lt.0.) nmon=nmon+6
      if(nmon.gt.12) nmon=nmon-12
      kmon=iabs(nmon-7)
      wt1=float(kmon)/6.
      wt2=1.-wt1
      do i=1,nl
         tpro(i)=wt1*tmr(i,1)+wt2*tmr(i,2)
         wpro(i)=wt1*wmr(i,1)+wt2*wmr(i,2)
         opro(i)=wt1*omr(i,1)+wt2*omr(i,2)
C added by WLH
         pref_copy(i)=pref(i)
C end of added by WLH
      enddo

C      CALL ADRM1D(3, 1, tpro, nl)
C      CALL ADRM1D(3, 2, wpro, nl)
C      CALL ADRM1D(3, 3, opro, nl)
C      CALL ADRM1D(3, 4, pref, nl)

  150 return
      end

