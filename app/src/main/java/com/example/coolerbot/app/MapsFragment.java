package com.example.coolerbot.app;

import android.app.Activity;
import android.app.Fragment;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MapsFragment extends Fragment implements GoogleMap.OnMapClickListener,
        GoogleMap.OnMarkerDragListener{

    private MapView mapView;
    private GoogleMap mMap;
    private Polyline polyline;
    private List<Marker> waypointList = new ArrayList<Marker>();
    private Marker homeMarker;
    private Marker losMarker;

    private MapUpdateListener mapUpdateListener;

    private static final String ARG_SECTION_NUMBER = "section_number";

    //Create new instance for fragment.
    public static MapsFragment newInstance(int sectionNumber) {
        MapsFragment fragment = new MapsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public MapsFragment() {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        //Register map update listener to pass data back to parent activity
        mapUpdateListener = (MapUpdateListener) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (container == null) { return null; }
        //Inflate map layout
        View rootView = inflater.inflate(R.layout.map_fragment, container, false);

        //Add google map to map layout
        mapView = (MapView) rootView.findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);

        //Initialize map required by maps
        if (mapView == null) { return null; }
        MapsInitializer.initialize(getActivity());

        //Setup map options
        mMap = mapView.getMap();
        mMap.setMyLocationEnabled(true);
        mMap.setOnMapClickListener(this);
        mMap.setOnMarkerDragListener(this);
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        //Instantiate polyline to draw lines between waypoints
        PolylineOptions polylineOptions = new PolylineOptions();
        polyline = mMap.addPolyline(polylineOptions);

        return rootView;
    }

    @Override
    public void onResume() {
        mapView.onResume();
        super.onResume();
    }

    @Override
    public void onPause() {
        mapView.onPause();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mapView.onDestroy();
    }

    @Override
    public void onMapClick(LatLng latLng) {
        //Do not allow map clicks until home location is set. Home location is set when first GPS
        //fix is made.
        if(homeMarker == null) {return;}
        //Add marker for each map click. Waypoint to follow.
        waypointList.add(mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .draggable(true)
                .title("Waypoint")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))));
        //Send waypoint back up to parent activity
        mapUpdateListener.onWaypointUpdate(latLng);

        //Iterate through stored waypoint lists, and create points list for polyline to connect
        //waypoint with line.
        int numWaypoint = waypointList.size();
        if (numWaypoint >= 2) {
            List<LatLng> points = new ArrayList<LatLng>();

            for (Marker marker: waypointList) {
                points.add(marker.getPosition());
            }
            //Create lines
            polyline.setPoints(points);
        }
    }

    @Override
    public void onMarkerDragStart(Marker marker) {}

    @Override
    public void onMarkerDrag(Marker marker) {}

    @Override
    public void onMarkerDragEnd(Marker marker) {}

    //Sets home location. This function is called from parent activity. Cannot add waypoint until
    //home is set.
    public void setHomeLocation(LatLng home) {
        //Auto zoom into home location.
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(home, 19));
        //Add marker for home location.
        homeMarker = mMap.addMarker(new MarkerOptions()
                .position(home)
                .draggable(true)
                .title("Home")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
    }

    //Set LOS marker for debugging. Function is called for parent activity.
    public void setLOSLoction(LatLng los) {
        if(losMarker != null) {losMarker.remove();}
        losMarker = mMap.addMarker(new MarkerOptions()
                .position(los)
                .draggable(true)
                .title("LOS")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
    }

    //Interface class for map update callback.
    public interface MapUpdateListener {
        public void onWaypointUpdate(LatLng waypoint);
    }
}