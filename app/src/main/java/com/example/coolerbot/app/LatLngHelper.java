package com.example.coolerbot.app;

import com.google.android.gms.maps.model.LatLng;

public class LatLngHelper {
    // WGS84 ellipsoid constants
    private static final double a = 6378137; // radius
    private static final double e = 8.1819190842622e-2;  // eccentricity

    private static final double asq = Math.pow(a,2);
    private static final double esq = Math.pow(e,2);

    public static LatLng ecef2latlng(double[] ecef){
        double x = ecef[0];
        double y = ecef[1];
        double z = ecef[2];

        double b = Math.sqrt( asq * (1-esq) );
        double bsq = Math.pow(b,2);
        double ep = Math.sqrt( (asq - bsq)/bsq);
        double p = Math.sqrt( Math.pow(x,2) + Math.pow(y,2) );
        double th = Math.atan2(a*z, b*p);

        double lon = Math.atan2(y,x);
        double lat = Math.atan2( (z + Math.pow(ep,2)*b*Math.pow(Math.sin(th),3) ),
                (p - esq*a*Math.pow(Math.cos(th),3)) );

        // mod lat to 0-2pi
        lon = lon % (2*Math.PI);

        // correction for altitude near poles left out.

        return new LatLng(lat,lon);
    }

    public double[] latlng2ecef(LatLng latLng){
        double lat = latLng.latitude;
        double lon = latLng.longitude;
        double alt = 0;

        double N = a / Math.sqrt(1 - esq * Math.pow(Math.sin(lat),2) );

        double x = (N+alt) * Math.cos(lat) * Math.cos(lon);
        double y = (N+alt) * Math.cos(lat) * Math.sin(lon);
        double z = ((1-esq) * N + alt) * Math.sin(lat);

        double[] ecef = {x, y, z};
        return ecef;
    }

}
