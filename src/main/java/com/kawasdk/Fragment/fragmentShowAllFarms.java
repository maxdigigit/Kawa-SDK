package com.kawasdk.Fragment;

import android.content.Context;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kawasdk.Model.Boundary;
import com.kawasdk.Model.MergeModel;
import com.kawasdk.Model.PolygonModel;
import com.kawasdk.Model.ResponseKawa;
import com.kawasdk.R;
import com.kawasdk.Utils.Common;
import com.kawasdk.Utils.InterfaceKawaEvents;
import com.kawasdk.Utils.KawaMap;
import com.kawasdk.Utils.ServiceManager;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.mapbox.mapboxsdk.Mapbox.getApplicationContext;

public class fragmentShowAllFarms extends Fragment implements OnMapReadyCallback, MapboxMap.OnMapClickListener {
    private MapboxMap MAPBOXMAP;
    private MapView MAPVIEW;
    String STRID = "";
    private List<Integer> POLYSELECTED = new ArrayList<>();
    private List<List<LatLng>> LNGLAT = new ArrayList<>();
    Button combinePlotBtn, startOverBtn;
    TextView messageBox;
    InterfaceKawaEvents interfaceKawaEvents;

    @Override
    public void onAttach(@NonNull @NotNull Context context) {
        super.onAttach(context);
        interfaceKawaEvents = (InterfaceKawaEvents) context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(getActivity(), Common.MAPBOX_ACCESS_TOKEN);
        Common.showLoader("isScanner");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.show_all_farms, container, false);
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        super.onCreate(savedInstanceState);

        // X_API_KEY = getResources().getString(R.string.kawa_api_key);

        MAPVIEW = view.findViewById(R.id.mapView);
        MAPVIEW.onCreate(savedInstanceState);
        MAPVIEW.getMapAsync(this);

        STRID = getArguments().getString("id");

        Log.e("POLYSELECTED::", String.valueOf(POLYSELECTED));
        Log.e("LNGLAT::", String.valueOf(LNGLAT));

        Common.CAMERALAT = getArguments().getDouble("lat", 0.00);
        Common.CAMERALNG = getArguments().getDouble("lng", 0.00);
        Common.MAPZOOM = getArguments().getDouble("zoom", 16.0);

        Log.e("GET ZOOM", String.valueOf(Common.MAPZOOM));

        messageBox = view.findViewById(R.id.messageBox);
        messageBox.setBackgroundColor(KawaMap.headerBgColor);
        messageBox.setTextColor(KawaMap.headerTextColor);
        combinePlotBtn = view.findViewById(R.id.combinePlotBtn);
        combinePlotBtn.setTextColor(KawaMap.footerBtnTextColor);
        combinePlotBtn.setBackgroundColor(KawaMap.footerBtnBgColor);
        combinePlotBtn.setVisibility(View.GONE);
        startOverBtn = view.findViewById(R.id.startOverBtn);

        Button[] innerButtons = null;
        innerButtons = new Button[]{startOverBtn};
        KawaMap.setInnerButtonColor(innerButtons);

        startOverBtn.setOnClickListener(viewV -> {
            fragmentFarmLocation fragmentFarmLocation = new fragmentFarmLocation();
            FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.kawaMapView, fragmentFarmLocation);
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
        });

        combinePlotBtn.setOnClickListener(viewV -> {
            try {
                getMergedCordinates();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });

    }

    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        MAPBOXMAP = mapboxMap;
        MAPBOXMAP.getUiSettings().setCompassEnabled(false);
        MAPBOXMAP.getUiSettings().setLogoEnabled(false);
        MAPBOXMAP.getUiSettings().setFlingVelocityAnimationEnabled(false);
    //    MAPBOXMAP.getUiSettings().setScrollGesturesEnabled(false); // Disable Scroll
        //MAPBOXMAP.setMinZoomPreference(Common.MAPZOOM);
        MAPBOXMAP.setStyle(Style.SATELLITE_STREETS, style -> {
            MAPBOXMAP.addOnMapClickListener(this);
            LatLng latLng = new LatLng(Common.CAMERALAT, Common.CAMERALNG);
            Log.e("FINA ZOOM", String.valueOf(Common.MAPZOOM));
            MAPBOXMAP.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder().target(latLng).zoom(Common.MAPZOOM).build()), 1000);
            Common.initMarker(style, MAPBOXMAP, MAPVIEW);


            new android.os.Handler(Looper.getMainLooper()).postDelayed(
                    new Runnable() {
                        public void run() {
                            Common.lockZoom(MAPBOXMAP);
                        }
                    },
                    3000);
            getAllCordinates(style);
        });
    }

    @Override
    public boolean onMapClick(@NonNull LatLng coordsOfPoint) {
        if (LNGLAT.size() > 0) {
            MAPBOXMAP.getStyle(style -> {
                for (int i = 0; i < LNGLAT.size(); i++) {
                    boolean contains;
                    contains = Common.checkLatLongInPolygon(coordsOfPoint, LNGLAT.get(i));
                    if (contains) {
                        Layer lineLayer = style.getLayer("lineLayerID" + i);
                        if (lineLayer != null) {
                            int flg = 0;
                            combinePlotBtn.setVisibility(View.VISIBLE);

                            if (POLYSELECTED.size() > 0) {
                                if (POLYSELECTED.contains(i)) {
                                    flg = 1;
                                    POLYSELECTED.remove((Integer) i);
                                }
                            }

                            if (flg == 0) {
                                POLYSELECTED.add(i);
                                lineLayer.setProperties(PropertyFactory.lineOpacity(1f));
                            } else {
                                lineLayer.setProperties(PropertyFactory.lineOpacity(0f));

                            }
                        }
                        break;
                    }
                }
            });
            int selSize = POLYSELECTED.size();
            String msgPre = "s";
            if (selSize == 1) {
                msgPre = "";
            }
            messageBox.setText(selSize + " Farm" + msgPre + " selected");
        }
        return false;
    }

    private void getAllCordinates(Style style) {
        //STRID = "6383c4cd-7889-44ee-8a57-42ff3c01d824";
        ServiceManager.getInstance().getKawaService().status(KawaMap.KAWA_API_KEY, Common.SDK_VERSION, STRID).enqueue(new Callback<PolygonModel>() {
            @Override
            public void onResponse(@NonNull Call<PolygonModel> call, @NonNull Response<PolygonModel> response) {

                try {
                    if (response.isSuccessful()) {
                        Common.hideLoader();
                        if (response.body() != null) {
                            //Log.e("RESPONSE", String.valueOf(response.body()));
                            Common.FARMS_FETCHED_AT = response.body().getData().getFarms_fetched_at();
                            //Log.e("FARMS_FETCHED_AT", Common.FARMS_FETCHED_AT );
                            List<Boundary> newListBoundry = response.body().getData().getBoundaries();
                            if (newListBoundry.size() > 0) {

                                if (POLYSELECTED.size() > 0) {
                                    combinePlotBtn.setVisibility(View.VISIBLE);
                                }

                                for (int i = 0; i < newListBoundry.size(); i++) {
                                    List<List<Double>> cordinates = newListBoundry.get(i).getGeojson().getCoordinates().get(0);
                                    List<Point> llPts = new ArrayList<>();
                                    List<List<Point>> llPtsA = new ArrayList<>();
                                    List<LatLng> ll = new ArrayList<>();

                                    for (int j = 0; j < cordinates.size(); j++) {
                                        llPts.add(Point.fromLngLat(cordinates.get(j).get(0), cordinates.get(j).get(1)));
                                        ll.add(new LatLng(cordinates.get(j).get(1), cordinates.get(j).get(0)));
                                    }

                                    llPtsA.add(llPts);
                                    LNGLAT.add(ll);
                                    Common.drawMapLayers(style, llPts, String.valueOf(i), "list");
                                    if (POLYSELECTED.contains(i)) {
                                        Layer lineLayer = style.getLayer("lineLayerID" + i);
                                        if (lineLayer != null) {
                                            lineLayer.setProperties(PropertyFactory.lineOpacity(1f));
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Common.hideLoader();
                        if(response.errorBody()!=null){
                            JSONObject jsonObj = new JSONObject(response.errorBody().string());
                            Log.e("RESP", jsonObj.getString("error"));
                            Toast.makeText(getApplicationContext(), jsonObj.getString("error"), Toast.LENGTH_LONG).show();// this will tell you why your api doesnt work most of time
                        }
                    }
                } catch (Exception e) {
                    Common.hideLoader();
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(@NonNull Call<PolygonModel> call, @NonNull Throwable t) {
                Common.hideLoader();
                //String errorBody = t.getMessage();
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.Error_General), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void getMergedCordinates() throws JSONException {
        if (POLYSELECTED.size() > 0) {
//            Log.e("LNGLAT", String.valueOf(LNGLAT));
//            List<List<LatLng>> lngLat = new ArrayList<>();
//            for (int i = 0; i < POLYSELECTED.size(); i++) {
//                Log.e("POLYSELECTED:", String.valueOf(POLYSELECTED.get(i)));
//
//                lngLat.add(LNGLAT.get(POLYSELECTED.get(i)));
//            }
//            Log.e("lngLat-b", String.valueOf(lngLat));
//            gotoEditPolygon(lngLat); // Will be removed once service call is used

            List<String> listFeatures = new ArrayList<>();
            for (int i = 0; i < POLYSELECTED.size(); i++) {
                List<List<Point>> llPtsA = new ArrayList<>();
                List<Point> llPts = new ArrayList<>();
                for (int j = 0; j < LNGLAT.get(POLYSELECTED.get(i)).size(); j++) {
                    List<LatLng> ll = LNGLAT.get(POLYSELECTED.get(i));
                    llPts.add(Point.fromLngLat(ll.get(j).getLongitude(), ll.get(j).getLatitude()));

                    // Log.e("POLYSELECTED", ll.toString() );
                }
                llPtsA.add(llPts);
                Feature multiPointFeature = Feature.fromGeometry(Polygon.fromLngLats(llPtsA));
                listFeatures.add(multiPointFeature.toJson());
            }

            String strMerge = "{\"farms_fetched_at\":" + "\"" + Common.FARMS_FETCHED_AT + "\"" + ",\"recipe_id\":\"farm_boundaries\",\"aois\":" + String.valueOf(listFeatures) + "}";
//            "{\"farms_fetched_at\":" + "\"" + Common.FARMS_FETCHED_AT + "\"" + ",\"recipe_id\":\"farm_boundaries\",\"aois\":" + String.valueOf(listFeatures) + "}";
            //Log.e("strMerge:", strMerge);
            JsonObject selectedFarms = JsonParser.parseString(strMerge).getAsJsonObject();
            Log.e("selectedFarms:", String.valueOf(selectedFarms));
            //interfaceKawaEvents.onkawaSelect(selectedFarms);
            //Phase second
           Common.showLoader("isCircle");
            ServiceManager.getInstance().getKawaService().getMergedPoints(KawaMap.KAWA_API_KEY, Common.SDK_VERSION, selectedFarms).enqueue(new Callback<MergeModel>() {
                @Override
                public void onResponse(@NonNull Call<MergeModel> call, @NonNull Response<MergeModel> response) {
                    Common.hideLoader();
                    try {
                        if (response.isSuccessful()) {
                            if (response.body() != null) {
                                List<ResponseKawa> responseKawa = response.body().getResponse();
                                Log.e("resposnebody", String.valueOf(responseKawa.size()));
                                List<List<LatLng>> lngLat = new ArrayList<>();
                                if (responseKawa.size() > 0) {
                                    for (int i = 0; i < responseKawa.size(); i++) {
                                        List<LatLng> ll = new ArrayList<>();
                                        List<List<Float>> cordinates = responseKawa.get(i).getGeometry().getCoordinates().get(0);
                                        if (cordinates.size() > 0) {
                                            for (int j = 0; j < cordinates.size(); j++) {
                                                ll.add(new LatLng(cordinates.get(j).get(1), cordinates.get(j).get(0)));
                                            }
                                            lngLat.add(ll);
                                        }
                                    }
                                    gotoEditPolygon(lngLat);
                                }
                                //Log.e("lngLat-a", String.valueOf(lngLat));
                            }
                        } else {
                            Common.hideLoader();
                            //assert response.errorBody() != null;
                            if(response.errorBody()!=null){
                                JSONObject jsonObj = new JSONObject(response.errorBody().string());
                                Log.e("RESP", jsonObj.getString("error"));
                                Toast.makeText(getApplicationContext(), jsonObj.getString("error"), Toast.LENGTH_LONG).show();
                            }
                        }
                    } catch (Exception e) {
                        Common.hideLoader();
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<MergeModel> call, @NonNull Throwable t) {
                    Common.hideLoader();
                    String errorBody = t.getMessage();
                    Log.e("onResponse:Failure ", errorBody);
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.Error_General), Toast.LENGTH_LONG).show();
                    //Toast.makeText(getActivity(), "onResponse:Failure " + errorBody, Toast.LENGTH_LONG).show(); // this will tell you why your api doesnt work most of time
                }
            });
        } else {
            Toast.makeText(getActivity(), "Please select farms to merge...", Toast.LENGTH_LONG).show();
        }
    }

    private void gotoEditPolygon(List<List<LatLng>> mergedCord) {
        CameraPosition cameraPosition = MAPBOXMAP.getCameraPosition();
        Common.MAPZOOM = cameraPosition.zoom;

        fragmentEditFarmBoundries fragmentEditFarmBoundries = new fragmentEditFarmBoundries();
        Bundle farms_bundle = new Bundle();
        farms_bundle.putString("id", STRID);
        farms_bundle.putDouble("lat", Common.CAMERALAT);
        farms_bundle.putDouble("lng", Common.CAMERALNG);
        farms_bundle.putDouble("zoom", Common.MAPZOOM);
        farms_bundle.putSerializable("data", (Serializable) mergedCord);
        fragmentEditFarmBoundries.setArguments(farms_bundle);

        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.kawaMapView, fragmentEditFarmBoundries);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    @Override
    public void onStart() {
        super.onStart();
        MAPVIEW.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        MAPVIEW.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        MAPVIEW.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        MAPVIEW.onStop();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        MAPVIEW.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        MAPVIEW.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        MAPVIEW.onLowMemory();
    }
}