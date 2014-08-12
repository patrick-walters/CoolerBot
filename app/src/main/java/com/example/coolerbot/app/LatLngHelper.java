package com.example.coolerbot.app;

import com.google.android.gms.maps.model.LatLng;

public class LatLngHelper {
    // WGS84 ellipsoid constants
    private static final double a = 6378137;
    private static final double e = 8.1819190842622e-2;

    private static final double a_sq = Math.pow(a,2);
    private static final double e_sq = Math.pow(e,2);

    public static LatLng ecef2ll(double[] ecef){
        //Assuming zero altitude.
        double x = ecef[0];
        double y = ecef[1];
        double z = ecef[2];

        double b = Math.sqrt( a_sq * (1-e_sq) );
        double b_sq = Math.pow(b,2);
        double e_p = Math.sqrt( (a_sq - b_sq)/b_sq);
        double p = Math.sqrt( Math.pow(x,2) + Math.pow(y,2) );
        double theta = Math.atan2(a*z, b*p);

        double lng = Math.atan2(y,x);
        double lat = Math.atan2( (z + Math.pow(e_p,2)*b*Math.pow(Math.sin(theta),3) ), 
                (p - e_sq*a*Math.pow(Math.cos(theta),3)) );

        return new LatLng(lat*(180/Math.PI),lng*(180/Math.PI));
    }

    public static double[] ll2ecef(LatLng latLng){
        //Assuming zero altitude.
        double lat = latLng.latitude*(Math.PI/180);
        double lng = latLng.longitude*(Math.PI/180);

        double N = a / Math.sqrt(1 - e_sq * Math.pow(Math.sin(lat), 2));

        double x = N * Math.cos(lat) * Math.cos(lng);
        double y = N * Math.cos(lat) * Math.sin(lng);
        double z = ((1- e_sq) * N) * Math.sin(lat);

        return new double[]{x, y, z};
    }

    public static double[] ll2ltp(LatLng origin, LatLng destination) {
        //Assuming zero altitude.
        double[] originECEF = ll2ecef(origin);
        double[] destinationECEF = ll2ecef(destination);
        double lat = origin.latitude*(Math.PI/180);
        double lng = origin.longitude*(Math.PI/180);

        double deltaX = (destinationECEF[0]-originECEF[0]);
        double deltaY = (destinationECEF[1]-originECEF[1]);
        double deltaZ = (destinationECEF[2]-originECEF[2]);

        double x = - Math.sin(lng)*deltaX
                + Math.cos(lng)*deltaY;
        double y = - Math.cos(lng)*Math.sin(lat)*deltaX
                - Math.sin(lng)*Math.sin(lat)*deltaY
                + Math.cos(lat)*deltaZ;
        double z = Math.cos(lng)*Math.cos(lat)*deltaX
                + Math.sin(lng)*Math.cos(lat)*deltaY
                + Math.sin(lat)*deltaZ;

        return new double[]{x, y, z};
    }

    public static LatLng ltp2lla(LatLng origin, double[] destinationLTP) {
        //Assuming zero altitude.
        double[] originECEF = ll2ecef(origin);
        double lat = origin.latitude*(Math.PI/180);
        double lng = origin.longitude*(Math.PI/180);

        double x = destinationLTP[0];
        double y = destinationLTP[1];
        double z = destinationLTP[2];

        double deltaX = - Math.sin(lng)*x
                - Math.cos(lng)*Math.sin(lat)*y
                + Math.cos(lng)*Math.cos(lat)*z;
        double deltaY = Math.cos(lng)*x
                - Math.sin(lng)*Math.sin(lat)*y
                + Math.sin(lng)*Math.cos(lat)*z;
        double deltaZ = Math.cos(lat)*y
                + Math.sin(lat)*z;


        double[] destinationECEF = {originECEF[0] + deltaX,
                originECEF[1] + deltaY,
                originECEF[2] + deltaZ};

        return ecef2ll(destinationECEF);
    }

}
