package com.kawasdk.Utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.kawasdk.R;
import com.mapbox.android.gestures.MoveGestureDetector;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.geometry.VisibleRegion;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.FillLayer;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillOpacity;

public class Common extends AppCompatActivity {

    private static Animation animVerticala;
    public static ProgressDialog proDialog;
    public static Context context;
    public static ImageView imageline;
    public static double MXCAMERALAT;
    public static double MXCAMERALNG;
    public static double CAMERALAT;
    public static double CAMERALNG;
    public static double MAXZOOM = 22.00;
    public static double MINZOOM = 5.00;
    public static double MAPZOOM = 17.00;
    public static ProgressBar PROGRESSBAR;
    public static final String MAPBOX_ACCESS_TOKEN = "pk.eyJ1Ijoia2F3YS1hZG1pbiIsImEiOiJja3RqcmN3N2kwNWEyMzJueWQzd2J0Znk1In0.WK1trBUr51BifsBNRX5ekw"; // MAPBOX TOKEN
    //public static final String MAPBOX_ACCESS_TOKEN = "pk.eyJ1IjoicnVwZXNoamFpbiIsImEiOiJja3JwdmdneGU1NHlxMnpwODN6bzFpbnkwIn0.UgSIBr9ChJFyrAKxtdNf9w"; // OLd MAPBOX TOKEN
    public static final String BASE_URL = "https://data.kawa.space/"; // live url
    //public static final String BASE_URL = "https://data-staging.kawa.space/"; // test url
    public static final String SDK_VERSION = android.os.Build.VERSION.SDK;

    public static String FARMS_FETCHED_AT = "";
    public static InterfaceKawaEvents interfaceKawaEvents;

    public Common(Context context) {
        this.context = context;
    }

    public static void drawMapLayers(Style style, List<Point> llPts, String id, String type) {
        List<List<Point>> llPtsA = new ArrayList<>();
        llPtsA.add(llPts);
        float opacityP = 0.6f;
        float opacityL = 0.0f;
        Integer color = null;

        if (type.equals("edit")) {
            opacityL = 1.0f;
            color = Color.parseColor("#c9577e");
        } else {
            Random random = new Random();
            int colorR = Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256));
            color = Integer.parseInt(String.valueOf(colorR));
        }

        style.addSource(new GeoJsonSource("polySourceID" + id, Polygon.fromLngLats(llPtsA)));
        style.addLayerBelow(new FillLayer("polyLayerID" + id, "polySourceID" + id).withProperties(fillColor(color), fillOpacity(opacityP)), "settlement-label");

        style.addSource(new GeoJsonSource("lineSourceID" + id, FeatureCollection.fromFeatures(new Feature[]{Feature.fromGeometry(LineString.fromLngLats(llPts))})));
        style.addLayer(new LineLayer("lineLayerID" + id, "lineSourceID" + id).withProperties(
                PropertyFactory.lineWidth(4f),
                PropertyFactory.lineColor(Color.parseColor("#000000")),
                PropertyFactory.lineOpacity(opacityL)
        ));
    }

    public static void initMarker(Style style, MapboxMap MAPBOXMAP, MapView MAPVIEW) {
        interfaceKawaEvents = (InterfaceKawaEvents) context;
        Bitmap marker_image = BitmapFactory.decodeResource(context.getResources(), R.drawable.marker);
        addMarker(style, marker_image);
        MAPBOXMAP.addOnMoveListener(new MapboxMap.OnMoveListener() {
            @Override
            public void onMoveBegin(MoveGestureDetector detector) {
                setMarkerPosition(style, MAPBOXMAP);
            }

            @Override
            public void onMove(@NonNull MoveGestureDetector detector) {
                setMarkerPosition(style, MAPBOXMAP);
            }

            @Override
            public void onMoveEnd(MoveGestureDetector detector) {
                setMarkerPosition(style, MAPBOXMAP);
                onkawaUpdatechange(interfaceKawaEvents, MAPBOXMAP);
            }
        });

        MAPBOXMAP.addOnCameraMoveCancelListener(() -> setMarkerPosition(style, MAPBOXMAP));
        MAPBOXMAP.addOnCameraIdleListener(() -> setMarkerPosition(style, MAPBOXMAP));
        MAPBOXMAP.addOnFlingListener(() -> setMarkerPosition(style, MAPBOXMAP));
        MAPVIEW.addOnDidFinishLoadingStyleListener(() -> setMarkerPosition(style, MAPBOXMAP));
    }

    public static void setMarkerPosition(Style style, MapboxMap MAPBOXMAP) {
        CameraPosition cameraPosition = MAPBOXMAP.getCameraPosition();
        LatLng location = cameraPosition.target;
        //MAPZOOM = cameraPosition.zoom;
        CAMERALAT = location.getLatitude();
        CAMERALNG = location.getLongitude();

        GeoJsonSource markerSorceID = style.getSourceAs("markerSorceID");
        if (markerSorceID != null) {
            markerSorceID.setGeoJson(Point.fromLngLat(CAMERALNG, CAMERALAT));
        }
    }

    public static void addMarker(Style style, Bitmap marker_image) {
        style.addSource(new GeoJsonSource("markerSorceID"));
        style.addImage("marker_image", marker_image);
        style.addLayer(new SymbolLayer("markerID", "markerSorceID").withProperties(
                PropertyFactory.iconImage("marker_image"),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconSize(0.6F)
        ));
    }

    public static void setZoomLevel(float val, MapboxMap MAPBOXMAP) {
        CameraPosition cameraPosition = MAPBOXMAP.getCameraPosition();

        int zoomleval = (int) cameraPosition.zoom;
        Log.e("zoomleval", String.valueOf(zoomleval));
        if (zoomleval < 3) {
            MAPZOOM = 3;
            MAPBOXMAP.animateCamera(CameraUpdateFactory.newLatLngZoom(cameraPosition.target, 3), 1000);
        } else {
            if (zoomleval > 2) {
                MAPZOOM = zoomleval + val;
                MAPBOXMAP.animateCamera(CameraUpdateFactory.newLatLngZoom(cameraPosition.target, zoomleval + val), 1000);
            }
        }
    }

    public static void lockZoom(MapboxMap MAPBOXMAP) {
        new android.os.Handler(Looper.getMainLooper()).postDelayed(
                new Runnable() {
                    public void run() {
                        VisibleRegion vRegion = MAPBOXMAP.getProjection().getVisibleRegion();
                        LatLng BOUND_CORNER_NW = new LatLng(vRegion.nearLeft.getLatitude(), vRegion.nearLeft.getLongitude());
                        LatLng BOUND_CORNER_SE = new LatLng(vRegion.farRight.getLatitude(), vRegion.farRight.getLongitude());
                        LatLngBounds RESTRICTED_BOUNDS_AREA = new LatLngBounds.Builder()
                                .include(BOUND_CORNER_NW)
                                .include(BOUND_CORNER_SE)
                                .build();

                        MAPBOXMAP.setLatLngBoundsForCameraTarget(RESTRICTED_BOUNDS_AREA);
                        MAPBOXMAP.setMinZoomPreference(MAPZOOM);
                    }
                },
                3000);
    }

    public static boolean checkLatLongInPolygon(LatLng coordsOfPoint, List<LatLng> latlngsOfPolygon) {
        int i;
        int j;
        boolean contains = false;
        for (i = 0, j = latlngsOfPolygon.size() - 1; i < latlngsOfPolygon.size(); j = i++) {
            if ((latlngsOfPolygon.get(i).getLongitude() > coordsOfPoint.getLongitude()) != (latlngsOfPolygon.get(j).getLongitude() > coordsOfPoint.getLongitude()) &&
                    (coordsOfPoint.getLatitude() < (latlngsOfPolygon.get(j).getLatitude() - latlngsOfPolygon.get(i).getLatitude()) * (coordsOfPoint.getLongitude() - latlngsOfPolygon.get(i).getLongitude()) / (latlngsOfPolygon.get(j).getLongitude() - latlngsOfPolygon.get(i).getLongitude()) + latlngsOfPolygon.get(i).getLatitude())) {
                contains = !contains;
            }
        }
        return contains;
    }

    public static void showLoader(String loaderType) {
        if (proDialog == null || !proDialog.isShowing()) {
            proDialog = new ProgressDialog(context);
            proDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            proDialog.show();
            View view = LayoutInflater.from(context).inflate(R.layout.loader, null);
            proDialog.setContentView(view);
            imageline = view.findViewById(R.id.imageline);
            PROGRESSBAR = view.findViewById(R.id.progress_circular);
            if (loaderType.equals("isScanner")) {
                PROGRESSBAR.setVisibility(View.GONE);
                imageline.setVisibility(View.VISIBLE);
                animVerticala = AnimationUtils.loadAnimation(context,
                        R.anim.slide_down);
                imageline.startAnimation(animVerticala);
            } else {
                imageline.setVisibility(View.GONE);
                PROGRESSBAR.setVisibility(View.VISIBLE);
            }
        }
    }


    public static void hideLoader() {
        if (proDialog != null && proDialog.isShowing()) {
            imageline.clearAnimation();
            proDialog.dismiss();
        }
    }

    public static void onkawaUpdatechange(InterfaceKawaEvents listner, MapboxMap MAPBOXMAP) {
        VisibleRegion vRegion = MAPBOXMAP.getProjection().getVisibleRegion();
        try {
            JSONObject jsonObject = new JSONObject();
            JSONObject centerPointobject = new JSONObject();

            interfaceKawaEvents = listner;
            if (interfaceKawaEvents != null) {
                centerPointobject.put("latitude", CAMERALAT);
                centerPointobject.put("longitude", CAMERALAT);
                centerPointobject.put("zoomLeval", MAPZOOM);
                jsonObject.put("center_position", centerPointobject);
                jsonObject.put("four_corners", vRegion);
                interfaceKawaEvents.onkawaUpdate(jsonObject);
            }
        } catch (Exception e) {

        }
    }
}
