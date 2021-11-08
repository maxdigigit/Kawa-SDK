package com.kawasdk.Fragment;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kawasdk.Model.DeviveBounderyModel;
import com.kawasdk.R;
import com.kawasdk.Utils.Common;
import com.kawasdk.Utils.InterfaceKawaEvents;
import com.kawasdk.Utils.KawaMap;
import com.kawasdk.Utils.LocationSearchAdapter;
import com.kawasdk.Utils.ServiceManager;
import com.mapbox.api.geocoding.v5.GeocodingCriteria;
import com.mapbox.api.geocoding.v5.MapboxGeocoding;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.api.geocoding.v5.models.GeocodingResponse;
import com.mapbox.core.exceptions.ServicesException;
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
import com.smartlook.sdk.smartlook.Smartlook;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.mapbox.mapboxsdk.Mapbox.getApplicationContext;

public class fragmentFarmLocation extends Fragment implements OnMapReadyCallback, LocationSearchAdapter.ItemClickListener {

    Intent intent;
    private Common COMACT;
    private MapboxMap MAPBOXMAP;
    private MapView MAPVIEW;
    Button GET_FARMSBtn, zoomOutBtn, zoomInBtn, dropPinFab;
    EditText searchTxt;
    TextView messageBox, searchResultTxt;
    ActivityResultLauncher<Intent> SEARCHRESULT;
    InterfaceKawaEvents interfaceKawaEvents;
    int firstTimecnt = 0;
    private ActivityResultLauncher<String> MPERMISSIONRESULT;

    RecyclerView place_recyclerView;
    LocationSearchAdapter locationSearchAdapter;
    ArrayList<String> PLACEARRAY;
    ArrayList<LatLng> PLACELATLNGARRAY;
    ArrayList<String> LATLNGARRAY;
    LatLng PLACELATLNG;
    LatLng PLACELNGLAT;

    // 434573882bf2d7079548eeb5344cd61e82131e76 kawa smartlook
    // 81ab38327bb3cbabb3f67fca628c0849d034aec0 maxdigi smartlook

    @Override
    public void onAttach(@NonNull @NotNull Context context) {
        super.onAttach(context);
        interfaceKawaEvents = (InterfaceKawaEvents) context;
        interfaceKawaEvents.initKawaMap(KawaMap.isValidKawaAPiKey);
        Smartlook.setUserIdentifier("KAWA SDK");
        Smartlook.SetupOptionsBuilder builder = new Smartlook.SetupOptionsBuilder("434573882bf2d7079548eeb5344cd61e82131e76")
                .setFps(2)
                .setExperimental(true)
                .setActivity(null);
        Smartlook.setupAndStartRecording(builder.build());
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
        COMACT.setLocale((Activity) getContext()); // Change Langurage
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
        GET_FARMSBtn = view.findViewById(R.id.GET_FARMSBtn);
        dropPinFab = view.findViewById(R.id.goCurrentLocBtn);
        searchTxt = view.findViewById(R.id.searchTxt);
        searchResultTxt = view.findViewById(R.id.searchResultTxt);
        messageBox = view.findViewById(R.id.messageBox);
        messageBox.setBackgroundColor(KawaMap.headerBgColor);
        messageBox.setTextColor(KawaMap.headerTextColor);
        GET_FARMSBtn.setTextColor(KawaMap.footerBtnTextColor);
        GET_FARMSBtn.setBackgroundColor(KawaMap.footerBtnBgColor);

        PLACEARRAY = new ArrayList<>();
        LATLNGARRAY = new ArrayList<>();
        PLACELATLNGARRAY = new ArrayList<>();
        place_recyclerView = view.findViewById(R.id.place_recyclerView);
        place_recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        locationSearchAdapter = new LocationSearchAdapter(getActivity(), PLACEARRAY);
        locationSearchAdapter.setClickListener(this);
        place_recyclerView.setAdapter(locationSearchAdapter);

        searchResultTxt.setOnClickListener(viewV -> searchRegion());
        searchTxt.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
                try {
                    makeGeocodeSearch(searchTxt.getText().toString());
                    place_recyclerView.setVisibility(View.VISIBLE);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                // startGeocodeButton.setVisibility(View.GONE);
                try {
                    makeGeocodeSearch(searchTxt.getText().toString());
                    place_recyclerView.setVisibility(View.VISIBLE);
                } catch (ParseException e) {
                    e.printStackTrace();
                }


            }
        });

        GET_FARMSBtn.setOnClickListener(viewV -> getAllFarms());
        zoomInBtn.setOnClickListener(viewV -> COMACT.setZoomLevel(1, MAPBOXMAP));
        zoomOutBtn.setOnClickListener(viewV -> COMACT.setZoomLevel(-1, MAPBOXMAP));

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
                                    750L,
                                    16.0,
                                    null,
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
            dropPinFab.setOnClickListener(viewV -> getCurrentLocation());
            MPERMISSIONRESULT.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        });
    }

    private void getCurrentLocation() {
        MPERMISSIONRESULT.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        COMACT.segmentEvents(getActivity(), "Fetching the current location", "GPS location saved", MAPBOXMAP, "", "CURRENT_LOC");
    }

    public void getAllFarms() {
        COMACT.showLoader("isScanner");
        ServiceManager.getInstance().getKawaService().GET_FARMSs(KawaMap.KAWA_API_KEY, Common.SDK_VERSION, getCornerLatLng())
                .enqueue(new Callback<DeviveBounderyModel>() {
                    @Override
                    public void onResponse(@NonNull Call<DeviveBounderyModel> call, @NonNull Response<DeviveBounderyModel> response) {
                        COMACT.hideLoader();
                        try {
                            if (response.isSuccessful() && response.body() != null) {
                                CameraPosition cameraPosition = MAPBOXMAP.getCameraPosition();
                                COMACT.MAPZOOM = cameraPosition.zoom;

                                COMACT.segmentEvents(getActivity(), "Get farm boundaries", "Response saved on successfully getting farm boundaries", MAPBOXMAP, response.body().getId(), "GET_FARMS");
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
                                    String errorMsg = jsonObj.getString("error");
                                    Log.e("getAllFarms", errorMsg);
                                    COMACT.segmentEvents(getActivity(), "Get farm boundaries", "Response saved on failing to get farm boundaries", MAPBOXMAP, errorMsg, "GET_FARMS");

                                    if (errorMsg.equals("Could not validate request: the area chosen is too large, please choose coordinates enclosing a smaller area"))
                                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.error_large_area), Toast.LENGTH_LONG).show();
                                    else if (errorMsg.equals("Could not validate request: The coordinates given are out of bounds for the recipe farm_boundaries, please check developers.kawa.space for the coordinate bounds for the recipe"))
                                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.error_farmnot_available), Toast.LENGTH_LONG).show();
                                    else
                                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.Error_General), Toast.LENGTH_LONG).show();
                                }

                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(Call<DeviveBounderyModel> call, Throwable t) {
                        COMACT.hideLoader();
                        String errorBody = t.getMessage();
                        Log.e("TAG", "onFailure: " + errorBody);
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

        searchResultTxt.setVisibility(View.GONE);
        searchTxt.setText("");
        searchTxt.setVisibility(View.VISIBLE);

    }

    /*private void displaySerachRegion(Intent data) {
        GET_FARMSBtn.setVisibility(View.VISIBLE);
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
                Log.e("TAG", lat + " : displaySerachRegion: " + lng);

                GeoJsonSource markerSorceID = style.getSourceAs("markerSorceID");
                markerSorceID.setGeoJson(Point.fromLngLat(lat, lng));

                MAPBOXMAP.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder().target(latLng).zoom(COMACT.MAPZOOM).build()), 1000);
            }
        }
    }*/

    @Override
    public void onItemClick(View view, int position) {
        //Toast.makeText(this, "You clicked " + adapter.getItem(position) + " on row number " + position, Toast.LENGTH_SHORT).show();
        if (PLACELATLNGARRAY.size() > 0) {
            animateCameraToNewPosition(PLACELATLNGARRAY.get(position));
            searchResultTxt.setText(String.valueOf(MAPBOXMAP.getProjection().getVisibleRegion().latLngBounds));
            searchTxt.setVisibility(View.GONE);
            searchResultTxt.setVisibility(View.VISIBLE);
        } else {
            Toast.makeText(getContext(), "Invalid Latitude or Longitude", Toast.LENGTH_SHORT).show();
            searchTxt.setText("");
        }
        COMACT.segmentEvents(getActivity(), "Fetching a desired location", PLACEARRAY.get(position), MAPBOXMAP, searchTxt.getText().toString(), "SEARCH");
        place_recyclerView.setVisibility(View.GONE);
        PLACEARRAY.clear(); // clear list
        PLACELATLNGARRAY.clear(); // clear list
        locationSearchAdapter.setItems(PLACEARRAY);
        hideKeyboard(getActivity(), view);

    }

    private boolean latCoordinateIsValid(double value) {
        return value >= -90 && value <= 90;
    }

    private boolean longCoordinateIsValid(double value) {
        return value >= -180 && value <= 180;
    }

    private void makeGeocodeSearch(String searchStr) throws ParseException {
        if (searchStr.length() >= 3) {
            List<String> placeList = Arrays.asList(searchStr.split(","));
            // if (placeList.get(0).matches("-?\\d+")) {
            if (placeList.get(0).matches("\\d+(?:\\.\\d+)?") || placeList.get(0).matches("-?\\d+")) {
                if (placeList.size() == 2) {
                    PLACEARRAY.clear(); // clear list
                    PLACELATLNGARRAY.clear(); // clear list
                    locationSearchAdapter.setItems(PLACEARRAY);
                    PLACEARRAY.add("Lat : " + placeList.get(0) + " Long : " + placeList.get(1));
                    PLACEARRAY.add("Long : " + placeList.get(0) + " Lat : " + placeList.get(1));
                    if (placeList.get(1).equals(" ")) {
                    } else {
                        NumberFormat numberFormat = NumberFormat.getInstance();
                        //double lat = Double.parseDouble(placeList.get(0));
                        //double lng = Double.parseDouble(placeList.get(1));
                        double lat = numberFormat.parse(placeList.get(0)).doubleValue();
                        double lng = numberFormat.parse(placeList.get(1)).doubleValue();

                        try {
                            LatLng latLng = null;
                            if (latCoordinateIsValid(Double.valueOf(placeList.get(0)))
                                    && longCoordinateIsValid(Double.valueOf(placeList.get(1)))) {
                                // Make a geocoding search with the values inputted into the EditTexts
                                PLACELATLNG = (new LatLng(Double.valueOf(lat),
                                        Double.valueOf(lng)));
                                PLACELNGLAT = (new LatLng(Double.valueOf(lng),
                                        Double.valueOf(lat)));
                                PLACELATLNGARRAY.add(PLACELATLNG);
                                PLACELATLNGARRAY.add(PLACELNGLAT);
                                locationSearchAdapter.setItems(PLACEARRAY);
                                latLng = (new LatLng(Double.valueOf(placeList.get(0)), Double.valueOf(placeList.get(1))));

                                MapboxGeocoding client = MapboxGeocoding.builder()
                                        .accessToken("pk.eyJ1Ijoia2F3YS1hZG1pbiIsImEiOiJja3RqcmN3N2kwNWEyMzJueWQzd2J0Znk1In0.WK1trBUr51BifsBNRX5ekw")
                                        .query(Point.fromLngLat(latLng.getLongitude(), latLng.getLatitude()))
                                        .geocodingTypes(GeocodingCriteria.TYPE_PLACE)
                                        .mode(GeocodingCriteria.MODE_PLACES)
                                        .build();
                                LatLng finalLatLng = latLng;
                                client.enqueueCall(new Callback<GeocodingResponse>() {
                                    @Override
                                    public void onResponse(Call<GeocodingResponse> call,
                                                           Response<GeocodingResponse> response) {
                                        if (response.body() != null) {
                                            List<CarmenFeature> results = response.body().features();
                                            if (results.size() > 0) {
                                                CarmenFeature feature = results.get(0);
                                                //  animateCameraToNewPosition(finalLatLng);
                                            } else {
                                                Toast.makeText(getContext(), "Not Found", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    }

                                    @Override
                                    public void onFailure(Call<GeocodingResponse> call, Throwable throwable) {
                                        Log.e("Geocoding Failure: ", throwable.getMessage());
                                    }
                                });
                            } else {
                                // Toast.makeText(getContext(), "Invalid Mark", Toast.LENGTH_LONG).show();
                            }
                        } catch (ServicesException servicesException) {
                            Log.e("Error geocoding: ", servicesException.toString());
                            servicesException.printStackTrace();
                        }
                    }
                }

                Log.e("TAG", "makeGeocodeearch: " + PLACEARRAY);
            } else {
                PLACEARRAY.clear(); // clear list
                PLACELATLNGARRAY.clear(); // clear list
                locationSearchAdapter.setItems(PLACEARRAY);
                MapboxGeocoding mapboxGeocoding = MapboxGeocoding.builder()
                        .accessToken("pk.eyJ1Ijoia2F3YS1hZG1pbiIsImEiOiJja3RqcmN3N2kwNWEyMzJueWQzd2J0Znk1In0.WK1trBUr51BifsBNRX5ekw")
                        .query(searchStr)
                        .build();

                mapboxGeocoding.enqueueCall(new Callback<GeocodingResponse>() {
                    @Override
                    public void onResponse(Call<GeocodingResponse> call, Response<GeocodingResponse> response) {
                        List<CarmenFeature> results = response.body().features();
                        if (results.size() > 0) {
                            PLACEARRAY.clear(); // clear list
                            PLACELATLNGARRAY.clear(); // clear list
                            for (int i = 0; i < results.size(); i++) {
                                Log.d("TAG", "onResponse: " + results.get(0).placeName());
                                PLACEARRAY.add(results.get(i).placeName());
                                double lat = results.get(i).center().coordinates().get(1);
                                double lng = results.get(i).center().coordinates().get(0);
                                PLACELATLNG = (new LatLng(Double.valueOf(lat),
                                        Double.valueOf(lng)));
                                PLACELATLNGARRAY.add(PLACELATLNG);
                                locationSearchAdapter.setItems(PLACEARRAY);
                            }
                        } else {
                            Log.d("TAG", "onResponse: No result found");
                        }
                    }

                    @Override
                    public void onFailure(Call<GeocodingResponse> call, Throwable throwable) {
                        throwable.printStackTrace();
                    }
                });
            }
        }
    }

    private void animateCameraToNewPosition(LatLng latLng) {
        COMACT.MAPZOOM = 17.0;
        MAPBOXMAP.animateCamera(CameraUpdateFactory
                .newCameraPosition(new CameraPosition.Builder()
                        .target(latLng)
                        .zoom(COMACT.MAPZOOM)
                        .build()), 1000);
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
        if (firstTimecnt == 0) {
            // do something
            Log.e("onStart_if", String.valueOf(firstTimecnt));
            firstTimecnt = 2;
        } else if (firstTimecnt == 2) {
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

    public static void hideKeyboard(Activity activity, View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        Log.e("TAG", "hideKeyboard: ");
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