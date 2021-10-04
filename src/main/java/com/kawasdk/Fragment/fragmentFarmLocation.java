package com.kawasdk.Fragment;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kawasdk.Model.DeviveBounderyModel;
import com.kawasdk.R;
import com.kawasdk.Utils.Common;
import com.kawasdk.Utils.InterfaceKawaEvents;
import com.kawasdk.Utils.KawaMap;
import com.kawasdk.Utils.ServiceManager;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.VisibleRegion;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete;
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.mapbox.mapboxsdk.Mapbox.getApplicationContext;

;

public class fragmentFarmLocation extends Fragment implements OnMapReadyCallback {

    Intent intent;
    private Common COMACT;

    private MapboxMap MAPBOXMAP;
    private MapView MAPVIEW;
    Button getFarmBtn, zoomOutBtn, zoomInBtn, dropPinFab;
    TextView searchTxt;
    TextView messageBox;
    ActivityResultLauncher<Intent> SEARCHRESULT;
    InterfaceKawaEvents interfaceKawaEvents;
    int firstTimecnt = 0;
    private ActivityResultLauncher<String> MPERMISSIONRESULT;
   // Boolean firstTime = true;

    @Override
    public void onAttach(@NonNull @NotNull Context context) {
        super.onAttach(context);
        interfaceKawaEvents = (InterfaceKawaEvents) context;
        interfaceKawaEvents.initKawaMap(KawaMap.isValidKawaAPiKey);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(getActivity(), Common.MAPBOX_ACCESS_TOKEN);
        COMACT = new Common(getActivity());
        COMACT.showLoader("isScanner");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.select_farm_location, container, false);
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        super.onCreate(savedInstanceState);

        final LocationManager manager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();
        }
        // X_API_KEY = getResources().getString(R.string.kawa_api_key);

        MAPVIEW = view.findViewById(R.id.mapView);
        MAPVIEW.onCreate(savedInstanceState);
        MAPVIEW.getMapAsync(this);

        zoomInBtn = view.findViewById(R.id.zoomInBtn);
        zoomOutBtn = view.findViewById(R.id.zoomOutBtn);
        getFarmBtn = view.findViewById(R.id.getFarmBtn);
        dropPinFab = view.findViewById(R.id.goCurrentLocBtn);
        searchTxt = view.findViewById(R.id.searchTxt);
        messageBox = view.findViewById(R.id.messageBox);
        messageBox.setBackgroundColor(KawaMap.headerBgColor);
        messageBox.setTextColor(KawaMap.headerTextColor);
        getFarmBtn.setTextColor(KawaMap.footerBtnTextColor);
        getFarmBtn.setBackgroundColor(KawaMap.footerBtnBgColor);

        searchTxt.setOnClickListener(viewV -> searchRegion());
        getFarmBtn.setOnClickListener(viewV -> getAllFarms());
        zoomInBtn.setOnClickListener(viewV -> COMACT.setZoomLevel(1, MAPBOXMAP));
        zoomOutBtn.setOnClickListener(viewV -> COMACT.setZoomLevel(-1, MAPBOXMAP));

        SEARCHRESULT = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                    COMACT.MAPZOOM=17.0;
                    firstTimecnt = 1;
                    if (result.getResultCode() == AppCompatActivity.RESULT_OK) {
                        displaySerachRegion(result.getData());
                    }
                }
        );

        MPERMISSIONRESULT = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                new ActivityResultCallback<Boolean>() {
                    @Override
                    public void onActivityResult(Boolean result) {
                        Log.e("PERMISSIONSTATUS", String.valueOf(result));
                        if (result) {
                            Log.e("TAG", "onActivityResult: PERMISSION GRANTED");
                            Style loadedMapStyle = MAPBOXMAP.getStyle();
                            //MAPBOXMAP.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder().zoom(Common.MAPZOOM).build()), 100);
                            LocationComponent locationComponent = MAPBOXMAP.getLocationComponent();
                            locationComponent.activateLocationComponent(LocationComponentActivationOptions.builder(getActivity(), loadedMapStyle).build());
                            if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                Log.e("TAG", "onActivityResult: PERMISSION IF");
                                Toast.makeText(getActivity(), R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show();
                                return;
                            }
                            locationComponent.setLocationComponentEnabled(true);
                           // locationComponent.setCameraMode(CameraMode.TRACKING);
                            Log.e("COMACTZOOM", String.valueOf(COMACT.MAPZOOM));
                            locationComponent.setCameraMode(
                                    CameraMode.TRACKING_GPS,
                                    750L ,
                                    16.0 ,
                                    null ,
                                    null,
                                    null);

                            //locationComponent.getLastKnownLocation().getLongitude()

                        } else {
                            Log.e("TAG", "onActivityResult: PERMISSION DENIED");
                            Toast.makeText(getActivity(), R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        MAPBOXMAP = mapboxMap;
        MAPBOXMAP.getUiSettings().setCompassEnabled(false);
        MAPBOXMAP.getUiSettings().setLogoEnabled(false);
        MAPBOXMAP.getUiSettings().setFlingVelocityAnimationEnabled(false);
        MAPBOXMAP.setStyle(Style.SATELLITE_STREETS, style -> {
            COMACT.hideLoader();
            COMACT.initMarker(style, MAPBOXMAP, MAPVIEW);
            dropPinFab.setOnClickListener(viewV -> MPERMISSIONRESULT.launch(Manifest.permission.ACCESS_FINE_LOCATION));
            MPERMISSIONRESULT.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        });
    }

    public void getAllFarms() {
        COMACT.showLoader("isScanner");
        ServiceManager.getInstance().getKawaService().getFarms(KawaMap.KAWA_API_KEY, Common.SDK_VERSION, getCornerLatLng())
                .enqueue(new Callback<DeviveBounderyModel>() {
                    @Override
                    public void onResponse(@NonNull Call<DeviveBounderyModel> call, @NonNull Response<DeviveBounderyModel> response) {
                        COMACT.hideLoader();
                        try {
                            if (response.isSuccessful() && response.body() != null) {
                                CameraPosition cameraPosition = MAPBOXMAP.getCameraPosition();
                                COMACT.MAPZOOM = cameraPosition.zoom;

                                Log.e("getAllFarms MAPZOOM", String.valueOf(COMACT.MAPZOOM));

                                Bundle farms_bundle = new Bundle();
                                farms_bundle.putString("id", response.body().getId());
                                farms_bundle.putDouble("lat", COMACT.CAMERALAT);
                                farms_bundle.putDouble("lng", COMACT.CAMERALNG);
                                farms_bundle.putDouble("zoom", COMACT.MAPZOOM);

                                fragmentShowAllFarms fragmentFarmLocation = new fragmentShowAllFarms();
                                fragmentFarmLocation.setArguments(farms_bundle);
                                FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                                fragmentTransaction.replace(R.id.kawaMapView, fragmentFarmLocation);
                                fragmentTransaction.addToBackStack(null);
                                fragmentTransaction.commit();

                            } else {
                                if (response.errorBody() != null) {
                                    JSONObject jsonObj = new JSONObject(response.errorBody().string());
                                    Log.e("RESP", jsonObj.getString("error"));
                                    Toast.makeText(getApplicationContext(), jsonObj.getString("error"), Toast.LENGTH_LONG).show();
                                }

                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(Call<DeviveBounderyModel> call, Throwable t) {
                        COMACT.hideLoader();
                        //String errorBody = t.getMessage();
                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.Error_General), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private JsonObject getCornerLatLng() {
        VisibleRegion vRegion = MAPBOXMAP.getProjection().getVisibleRegion();
        String jString = "{ \"recipe_id\": \"farm_boundaries\", \"aoi\": { \"type\": \"Feature\", \"geometry\": { \"type\": \"Polygon\", \"coordinates\": " +
                "[[[" + vRegion.farLeft.getLongitude() + ", " + vRegion.farLeft.getLatitude() + "], " +
                "[" + vRegion.nearLeft.getLongitude() + ", " + vRegion.nearLeft.getLatitude() + "], " +
                "[" + vRegion.nearRight.getLongitude() + ", " + vRegion.nearRight.getLatitude() + "], " +
                "[" + vRegion.farRight.getLongitude() + ", " + vRegion.farRight.getLatitude() + "], " +
                "[" + vRegion.farLeft.getLongitude() + "," + vRegion.farLeft.getLatitude() + "]]] } } }";

        JsonObject jsonObject = JsonParser.parseString(jString).getAsJsonObject();
        Log.e("jsonObject:", String.valueOf(jsonObject));
        return jsonObject;
    }

    private void searchRegion() {
        intent = new PlaceAutocomplete.IntentBuilder()
                .accessToken(Mapbox.getAccessToken() != null ? Mapbox.getAccessToken() : Common.MAPBOX_ACCESS_TOKEN)
                .placeOptions(PlaceOptions.builder()
                        .backgroundColor(Color.parseColor("#EEEEEE"))
                        .limit(10)
                        .build(PlaceOptions.MODE_CARDS))
                .build(getActivity());
        SEARCHRESULT.launch(intent);
    }

    private void displaySerachRegion(Intent data) {
        getFarmBtn.setVisibility(View.VISIBLE);
        searchTxt.setText(String.valueOf(MAPBOXMAP.getProjection().getVisibleRegion().latLngBounds));
        searchTxt.setTextColor(Color.BLACK);
        if (MAPBOXMAP != null) {
            Style style = MAPBOXMAP.getStyle();
            if (style != null) {
                CarmenFeature selectedCarmenFeature = PlaceAutocomplete.getPlace(data);
                double lat = ((Point) selectedCarmenFeature.geometry()).latitude();
                double lng = ((Point) selectedCarmenFeature.geometry()).longitude();
                LatLng latLng = new LatLng(lat, lng);

                Common.CAMERALAT = lat;
                Common.CAMERALNG = lng;

                GeoJsonSource markerSorceID = style.getSourceAs("markerSorceID");
                markerSorceID.setGeoJson(Point.fromLngLat(lat, lng));

                MAPBOXMAP.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder().target(latLng).zoom(COMACT.MAPZOOM).build()), 1000);
            }
        }
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", (dialog, id) -> startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                .setNegativeButton("No", (dialog, id) -> dialog.cancel());
        final AlertDialog alert = builder.create();
        alert.show();
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
        if (firstTimecnt == 0){
            // do something
            Log.e("onStart_if", String.valueOf(firstTimecnt));
            firstTimecnt = 2;
        }
        else if (firstTimecnt == 2){
            Log.e("onStart_else", String.valueOf(firstTimecnt));
            //MPERMISSIONRESULT.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            new android.os.Handler(Looper.getMainLooper()).postDelayed(
                    new Runnable() {
                        public void run() {
                            Fragment frag = new fragmentFarmLocation();
                            FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                            fragmentManager.beginTransaction().replace(R.id.kawaMapView, frag).commit();
                        }
                    },
                    1000);


        }

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
    public void onSaveInstanceState(Bundle outState) {
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