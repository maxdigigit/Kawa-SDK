package com.kawasdk.Fragment;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LongSparseArray;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kawasdk.Model.MergeModel;
import com.kawasdk.R;
import com.kawasdk.Utils.AddressServiceManager;
import com.kawasdk.Utils.Common;
import com.kawasdk.Utils.InterfaceKawaEvents;
import com.kawasdk.Utils.KawaMap;
import com.kawasdk.Utils.ServiceManager;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
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
import com.mapbox.mapboxsdk.plugins.annotation.OnSymbolDragListener;
import com.mapbox.mapboxsdk.plugins.annotation.Symbol;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.mapbox.mapboxsdk.Mapbox.getApplicationContext;

public class fragmentEditFarmBoundries extends Fragment implements OnMapReadyCallback, MapboxMap.OnMapClickListener {
    private View VIEW;
    private MapView MAPVIEW;
    private MapboxMap MAPBOXMAP;
    String TAG = "KAWA";
    String STRID = "";

    private List<List<LatLng>> LNGLAT = new ArrayList<>();
    private List<List<LatLng>> LNGLATEDIT = new ArrayList<>();
    ArrayList POLYGONAREA = new ArrayList<>();

    private static Integer LAYERINDEX;
    private static List<SymbolManager> SYMBOLSET = new ArrayList<>();
    private static Symbol SYMBOLACTIVE;
    private boolean EDITON = false;
    InterfaceKawaEvents interfaceKawaEvents;

    Button correctBoundryBtn, saveEditBtn, completeMarkingBtn, saveDetailBtn, startOverBtn, addMoreBtn, discardEditBtn, zoomOutBtn, zoomInBtn, backBtn, markAnotherBtn;
    ImageButton downBtn, upBtn, leftBtn, rightBtn;
    LinearLayout detailsForm, thankyouLinearLayout, farmDetailsLayout;
    TextView totalAreaTv, totalseedsTv, addressTv;

    JSONArray farm_fields_array;
    EditText[] myTextViews = null;
    TextView messageBox;
    LinearLayout farm_mark_messagebox;
    Handler mHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onAttach(@NonNull @NotNull Context context) {
        super.onAttach(context);
        interfaceKawaEvents = (InterfaceKawaEvents) context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Mapbox.getInstance(getActivity(), Common.MAPBOX_ACCESS_TOKEN);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.edit_farm_boundries, container, false);
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        super.onCreate(savedInstanceState);

        VIEW = view;
        MAPVIEW = view.findViewById(R.id.mapView);
        MAPVIEW.onCreate(savedInstanceState);
        MAPVIEW.getMapAsync(this);

        LNGLATEDIT = (List<List<LatLng>>) getArguments().getSerializable("data");
        POLYGONAREA = (ArrayList) getArguments().getSerializable("polygonarea");
        Log.e(TAG, "POLYGONAREA: " + POLYGONAREA);

        STRID = getArguments().getString("id");
        Common.CAMERALAT = getArguments().getDouble("lat", 0.00);
        Common.CAMERALNG = getArguments().getDouble("lng", 0.00);
        Common.MAPZOOM = getArguments().getDouble("zoom", 17.00);

        messageBox = view.findViewById(R.id.messageBox);
        farm_mark_messagebox = view.findViewById(R.id.farm_mark_messagebox);
        messageBox.setBackgroundColor(KawaMap.headerBgColor);
        messageBox.setTextColor(KawaMap.headerTextColor);

        initButtons();
    }

    @Override
    public void onMapReady(@NonNull final MapboxMap MAPBOXMAP1) {
        onFarmsLoaded();
        SYMBOLSET = new ArrayList<>();
        MAPBOXMAP = MAPBOXMAP1;
        MAPBOXMAP.getUiSettings().setCompassEnabled(false);
        MAPBOXMAP.getUiSettings().setLogoEnabled(false);
        MAPBOXMAP.getUiSettings().setFlingVelocityAnimationEnabled(false);
        //  MAPBOXMAP.getUiSettings().setScrollGesturesEnabled(false); // Disable Scroll
        MAPBOXMAP.getUiSettings().setDoubleTapGesturesEnabled(false);
        MAPBOXMAP.setStyle(Style.SATELLITE_STREETS, style -> {
            Log.e("onMapReady", "CALLED");
            LatLng latLng = new LatLng(Common.CAMERALAT, Common.CAMERALNG);
            MAPBOXMAP.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder().target(latLng).zoom(Common.MAPZOOM).build()), 1000);
            Common.lockZoom(MAPBOXMAP);//----------
            MAPBOXMAP.addOnMapClickListener(this);
            LNGLAT = new ArrayList<>();
            if (LNGLATEDIT.size() > 0) {
                for (int i = 0; i < LNGLATEDIT.size(); i++) {
                    List<LatLng> ll = new ArrayList<>();
                    List<Point> llPts = new ArrayList<>();
                    for (int j = 0; j < LNGLATEDIT.get(i).size(); j++) {
                        double lat = LNGLATEDIT.get(i).get(j).getLatitude();
                        double lng = LNGLATEDIT.get(i).get(j).getLongitude();
                        llPts.add(Point.fromLngLat(lng, lat));
                        ll.add(new LatLng(lat, lng));
                    }
                    LNGLAT.add(ll);
                    Common.drawMapLayers(style, llPts, String.valueOf(i), "edit");
                    drawSymbol(style, i);
                }
            }
            Common.initMarker(style, MAPBOXMAP, MAPVIEW);
        });
    }

    @Override
    public boolean onMapClick(@NonNull LatLng coordsOfPoint) {
        Log.e("onMapClick BEFORE", String.valueOf(EDITON) + " : " + String.valueOf(SYMBOLACTIVE));
        if (EDITON && SYMBOLACTIVE == null) {
            if (LNGLATEDIT.size() > 0) {
                onMaboxMapClick(coordsOfPoint);
            }
        }
        return false;
    }

    private void onMaboxMapClick(LatLng coordsOfPoint) {
        MAPBOXMAP.getStyle(style -> {
            boolean contains = false;
            Integer foundIdx = -1;

            if (coordsOfPoint != null) {
                for (int i = 0; i < LNGLATEDIT.size(); i++) {
                    contains = Common.checkLatLongInPolygon(coordsOfPoint, LNGLATEDIT.get(i));
                    if (contains) {
                        foundIdx = i;
                        break;
                    }
                }
            }

            if (foundIdx >= 0 && LAYERINDEX != foundIdx) {
                for (int i = 0; i < LNGLATEDIT.size(); i++) {
                    float opacityL = 0.3f;
                    float opacityP = 0.3f;
                    if (foundIdx == i) {
                        opacityL = 1.0f;
                        opacityP = 0.6f;
                    }
                    Layer lineLayer = style.getLayer("lineLayerID" + i);
                    Layer polyLayer = style.getLayer("polyLayerID" + i);
                    if (lineLayer != null && polyLayer != null) {
                        lineLayer.setProperties(PropertyFactory.lineOpacity(opacityL));
                        polyLayer.setProperties(PropertyFactory.fillOpacity(opacityP));
                    }
                }
                Integer finalFoundIdx = foundIdx;
                new Handler(Looper.getMainLooper()).postDelayed(
                        new Runnable() {
                            public void run() {
                                LAYERINDEX = finalFoundIdx;
                                onFarmSelected();
                                showHideSymbol("show", false);
                                Log.e("onMapClick AFTER", String.valueOf(EDITON) + " : " + String.valueOf(LAYERINDEX));
                            }
                        },
                        300);
            }
        });
    }

    private void drawSymbol(Style style, int idx) {
        SymbolManager symbolManager = new SymbolManager(MAPVIEW, MAPBOXMAP, style);
        symbolManager.setIconAllowOverlap(true);
        symbolManager.setTextAllowOverlap(true);
        SYMBOLSET.add(symbolManager);

        style.addImage("symbol_blue", BitmapFactory.decodeResource(this.getResources(), R.drawable.symbol_blue));
        style.addImage("symbol_yellow", BitmapFactory.decodeResource(this.getResources(), R.drawable.symbol_yellow));
        if (LNGLATEDIT.get(idx).size() > 0) {
            for (int j = 0; j < LNGLATEDIT.get(idx).size(); j++) {
                JsonObject objD = new JsonObject();
                //objD.addProperty("lIndex", idx);
                objD.addProperty("sIndex", j);
                symbolManager.create(new SymbolOptions()
                        .withLatLng(new LatLng(LNGLATEDIT.get(idx).get(j).getLatitude(), LNGLATEDIT.get(idx).get(j).getLongitude()))
                        .withIconImage("symbol_blue")
                        .withIconSize(0.5f)
                        .withDraggable(false)
                        .withIconOpacity(0.0f)
                        .withData(objD)
                );
            }
        }

        symbolManager.addClickListener(symbol -> {
            Log.e("EDITON - LAYERINDEX", String.valueOf(EDITON) + " - " + LAYERINDEX);
            if (EDITON && LAYERINDEX >= 0) {
                Log.e("getIconOpacity SYMBOL", String.valueOf(symbol.getIconOpacity()));
                if (symbol.getIconOpacity() > 0) {
                    int flg = 0;
                    if (SYMBOLACTIVE != null) {
                        Log.e("PREV SYMBOL", "FOUND");
                        if (!symbol.equals(SYMBOLACTIVE)) {
                            Log.e("NOT EQUAL", "PREV");
                            SYMBOLACTIVE.setDraggable(false);
                            SYMBOLACTIVE.setIconImage("symbol_blue");
                            SYMBOLACTIVE.setIconSize(0.3F);
                            symbolManager.update(SYMBOLACTIVE);
                            flg = 1;
                        }
                    } else {
                        Log.e("PREV SYMBOL", "NOT FOUND");
                        flg = 1;
                    }

                    Log.e("SYBOL FLAG", String.valueOf(flg));

                    if (flg == 1) {
                        SYMBOLACTIVE = symbol;
                        symbol.setDraggable(true);
                        symbol.setIconImage("symbol_yellow");
                        symbol.setIconSize(0.5f);
                        symbolManager.update(symbol);
                        onSymbolSelected();
                    }
                }
            }
            return true;
        });

        symbolManager.addDragListener(new OnSymbolDragListener() {
            @Override
            public void onAnnotationDragStarted(Symbol annotation) {
            }

            @Override
            public void onAnnotationDrag(Symbol symbol) {
                JsonObject objD = (JsonObject) symbol.getData();
                int sIndex = objD.get("sIndex").getAsInt();
                LNGLATEDIT.get(LAYERINDEX).set(sIndex, symbol.getLatLng());
                redrawFarms();
            }

            @Override
            public void onAnnotationDragFinished(Symbol symbol) {
//                JsonObject objD = (JsonObject) symbol.getData();
//                int sIndex = objD.get("sIndex").getAsInt();
//                LNGLATEDIT.get(LAYERINDEX).set(sIndex, symbol.getLatLng());
//                redrawFarms();
            }
        });
    }

    private void showAllLayers() {
        MAPBOXMAP.getStyle(style -> {
            for (int i = 0; i < LNGLATEDIT.size(); i++) {
                Layer lineLayer = style.getLayer("lineLayerID" + i);
                Layer polyLayer = style.getLayer("polyLayerID" + i);
                if (lineLayer != null && polyLayer != null) {
                    lineLayer.setProperties(PropertyFactory.lineOpacity(1.0f));
                    polyLayer.setProperties(PropertyFactory.fillOpacity(0.6f));
                }
            }
        });
    }

    private void showHideSymbol(String type, boolean discard) {
        if (SYMBOLSET.size() > 0) {
            Log.e("SYMBOLSET", String.valueOf(SYMBOLSET.size()));
            for (int i = 0; i < SYMBOLSET.size(); i++) {
                SymbolManager sm = SYMBOLSET.get(i);
                if (sm != null) {
                    Log.e("SymbolManager", "not null");
                    float opacity = 0.0f;
                    if (LAYERINDEX == i && type.equals("show"))
                        opacity = 1.0f;

                    LongSparseArray<Symbol> annotations = sm.getAnnotations();
                    if (!annotations.isEmpty()) {
                        if (annotations.size() > 0) {
                            for (int j = 0; j < annotations.size(); j++) {
                                Symbol symbol = annotations.get(j);

                                if (discard && LAYERINDEX == i) {
                                    JsonObject symbolData = (JsonObject) symbol.getData();
                                    //Integer sIndex = symbolData.get("sIndex").getAsInt();
                                    //Integer lIndex = symbolData.get("lIndex").getAsInt();
                                    LatLng latLng = LNGLATEDIT.get(i).get(j);
                                    if (latLng != null)
                                        setSymbolLL(symbol, latLng);
                                }

                                if (opacity > 0 && symbol.getIconOpacity() <= 0 || opacity <= 0 && symbol.getIconOpacity() > 0) {
                                    symbol.setIconOpacity(opacity);
                                    symbol.setDraggable(false);
                                    symbol.setIconImage("symbol_blue");
                                    symbol.setIconSize(0.3f);
                                    SYMBOLSET.get(i).update(symbol);
                                }
                            }
                        }
                    }

                }
            }
        }
    }

    private void moveSymbol(String direction) {
        if (SYMBOLACTIVE != null) {
            final PointF pointF = MAPBOXMAP.getProjection().toScreenLocation(SYMBOLACTIVE.getLatLng());

            final int moveBy = 5;
            float newX = pointF.x;
            float newY = pointF.y;

            switch (direction) {
                case "U":
                    newY = newY - moveBy;
                    break;
                case "D":
                    newY = newY + moveBy;
                    break;
                case "L":
                    newX = newX - moveBy;
                    break;
                case "R":
                    newX = newX + moveBy;
                    break;
            }

            pointF.set(newX, newY);
            LatLng newLL = MAPBOXMAP.getProjection().fromScreenLocation(pointF);
            setSymbolLL(SYMBOLACTIVE, newLL);
            redrawFarms();
        }
    }

    private void setSymbolLL(Symbol symbol, LatLng latLng) {
        JsonObject objD = (JsonObject) symbol.getData();
        Integer sIndex = objD.get("sIndex").getAsInt();
        LNGLATEDIT.get(LAYERINDEX).set(sIndex, latLng);
        symbol.setLatLng(latLng);
        SYMBOLSET.get(LAYERINDEX).update(symbol);
    }

    private void redrawFarms() {
        if (LNGLATEDIT.get(LAYERINDEX) != null) {
            MAPBOXMAP.getStyle(style -> {
                List<Point> llPts = new ArrayList<>();
                List<List<Point>> llPtsA = new ArrayList<>();
                for (int j = 0; j < LNGLATEDIT.get(LAYERINDEX).size(); j++) {
                    llPts.add(Point.fromLngLat(LNGLATEDIT.get(LAYERINDEX).get(j).getLongitude(), LNGLATEDIT.get(LAYERINDEX).get(j).getLatitude()));
//                    Layer lineLayer = style.getLayer("lineLayerID" + j);
//                    Layer polyLayer = style.getLayer("polyLayerID" + j);
//                    if (lineLayer != null && polyLayer != null) {
//                           lineLayer.setProperties(PropertyFactory.lineOpacity(1.0f));
//                          polyLayer.setProperties(PropertyFactory.fillOpacity(0.6f));
//                    }
                }

                llPtsA.add(llPts);
                GeoJsonSource lineSourceID = style.getSourceAs("lineSourceID" + LAYERINDEX);
                GeoJsonSource polySourceID = style.getSourceAs("polySourceID" + LAYERINDEX);

                if (lineSourceID != null) {
                    lineSourceID.setGeoJson(FeatureCollection.fromJson(""));
                    polySourceID.setGeoJson(FeatureCollection.fromJson(""));
                    lineSourceID.setGeoJson(Feature.fromGeometry(LineString.fromLngLats(llPts)));
                    polySourceID.setGeoJson(Feature.fromGeometry(Polygon.fromLngLats(llPtsA)));
                }
            });
        }
    }

    private void initButtons() {
        correctBoundryBtn = VIEW.findViewById(R.id.correctBoundryBtn);
        addMoreBtn = VIEW.findViewById(R.id.addMoreBtn);
        saveEditBtn = VIEW.findViewById(R.id.saveEditBtn);
        completeMarkingBtn = VIEW.findViewById(R.id.completeMarkingBtn);
        saveDetailBtn = VIEW.findViewById(R.id.saveDetailBtn);
        discardEditBtn = VIEW.findViewById(R.id.discardEditBtn);
        startOverBtn = VIEW.findViewById(R.id.startOverBtn);
        backBtn = VIEW.findViewById(R.id.backBtn);
        upBtn = VIEW.findViewById(R.id.upBtn);
        downBtn = VIEW.findViewById(R.id.downBtn);
        leftBtn = VIEW.findViewById(R.id.leftBtn);
        rightBtn = VIEW.findViewById(R.id.rightBtn);
        zoomInBtn = VIEW.findViewById(R.id.zoomInBtn);
        zoomOutBtn = VIEW.findViewById(R.id.zoomOutBtn);
        markAnotherBtn = VIEW.findViewById(R.id.markAnotherBtn);
        detailsForm = VIEW.findViewById(R.id.detailsForm);
        thankyouLinearLayout = VIEW.findViewById(R.id.thankyouLinearLayout);
        farmDetailsLayout = VIEW.findViewById(R.id.farmDetailsLayout);

        completeMarkingBtn.setOnClickListener(viewV -> completeMarking());
        correctBoundryBtn.setOnClickListener(viewV -> correctBoundry());
        saveEditBtn.setOnClickListener(viewV -> saveEdits());
        saveDetailBtn.setOnClickListener(viewV -> saveDetail());
        upBtn.setOnClickListener(viewV -> moveSymbol("U"));
        downBtn.setOnClickListener(viewV -> moveSymbol("D"));
        leftBtn.setOnClickListener(viewV -> moveSymbol("L"));
        rightBtn.setOnClickListener(viewV -> moveSymbol("R"));
        zoomInBtn.setOnClickListener(viewV -> Common.setZoomLevel(1, MAPBOXMAP));
        zoomOutBtn.setOnClickListener(viewV -> Common.setZoomLevel(-1, MAPBOXMAP));
        addMoreBtn.setOnClickListener(viewV -> onBackPressed());
        backBtn.setOnClickListener(viewV -> onBackPressed());
        discardEditBtn.setOnClickListener(viewV -> discardEdit());
        startOverBtn.setOnClickListener(view1 -> startOver());
        markAnotherBtn.setOnClickListener(view1 -> startOver());

        upBtn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                mHandler.postDelayed(joyesticRunnable, 0); // initial call for our handler.
                return true;
            }
        });
        downBtn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                mHandler.postDelayed(joyesticRunnable, 0); // initial call for our handler.
                return true;
            }
        });
        leftBtn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                mHandler.postDelayed(joyesticRunnable, 0); // initial call for our handler.
                return true;
            }
        });
        rightBtn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                mHandler.postDelayed(joyesticRunnable, 0); // initial call for our handler.
                return true;
            }
        });

        Button[] footerButtons = null;
        footerButtons = new Button[]{
                saveEditBtn,
                completeMarkingBtn,
                saveDetailBtn,markAnotherBtn
        };
        KawaMap.setFooterButtonColor(footerButtons);

        Button[] innerButtons = null;
        innerButtons = new Button[]{
                discardEditBtn,
                startOverBtn,
                backBtn,
                addMoreBtn,
                correctBoundryBtn,
        };
        KawaMap.setInnerButtonColor(innerButtons);
    }

    private Runnable joyesticRunnable = new Runnable() {
        @Override
        public void run() {
            if (upBtn.isPressed()) { // check if the button is still in its pressed state
                moveSymbol("U");
                mHandler.postDelayed(joyesticRunnable, 100); // call for a delayed re-check of the button's state through our handler. The delay of 100ms can be changed as needed.
            }
            if (downBtn.isPressed()) { // check if the button is still in its pressed state
                moveSymbol("D");
                mHandler.postDelayed(joyesticRunnable, 100); // call for a delayed re-check of the button's state through our handler. The delay of 100ms can be changed as needed.
            }
            if (leftBtn.isPressed()) { // check if the button is still in its pressed state
                moveSymbol("L");
                mHandler.postDelayed(joyesticRunnable, 100); // call for a delayed re-check of the button's state through our handler. The delay of 100ms can be changed as needed.
            }
            if (rightBtn.isPressed()) { // check if the button is still in its pressed state
                moveSymbol("R");
                mHandler.postDelayed(joyesticRunnable, 100); // call for a delayed re-check of the button's state through our handler. The delay of 100ms can be changed as needed.
            }

        }
    };

    private void hideAllBtn() {
        Log.e("Called", "hideAllBtn");
        correctBoundryBtn.setVisibility(VIEW.GONE);
        addMoreBtn.setVisibility(VIEW.GONE);
        backBtn.setVisibility(VIEW.GONE);
        discardEditBtn.setVisibility(VIEW.GONE);
        markAnotherBtn.setVisibility(View.GONE);
        upBtn.setVisibility(VIEW.GONE);
        downBtn.setVisibility(VIEW.GONE);
        leftBtn.setVisibility(VIEW.GONE);
        rightBtn.setVisibility(VIEW.GONE);
        zoomInBtn.setVisibility(VIEW.GONE);
        zoomOutBtn.setVisibility(VIEW.GONE);
        completeMarkingBtn.setVisibility(VIEW.GONE);
        saveDetailBtn.setVisibility(VIEW.GONE);
        saveEditBtn.setVisibility(VIEW.GONE);
    }

    private void onFarmsLoaded() {
        Log.e("Called", "onFarmsLoaded");
        hideAllBtn();
        addMoreBtn.setVisibility(VIEW.VISIBLE);
        if (Common.PHASERSTR.equals("1")) {
            correctBoundryBtn.setVisibility(VIEW.VISIBLE);
            completeMarkingBtn.setVisibility(VIEW.VISIBLE);
        } else if (Common.PHASERSTR.equals("2") || Common.PHASERSTR.equals("3")) {
            saveDetailBtn.setVisibility(VIEW.VISIBLE);
        }


        EDITON = false;
        SYMBOLACTIVE = null;
        LAYERINDEX = -1;
      //  messageBox.setText(getResources().getString(R.string.tap_correct_boundery));
        messageBox.setVisibility(View.GONE);
    }

    private void correctBoundry() {
        Log.e("Called", "correctBoundry");
        hideAllBtn();
        backBtn.setVisibility(VIEW.VISIBLE);
        EDITON = true;
        SYMBOLACTIVE = null;
        LAYERINDEX = -1;
        messageBox.setText(getResources().getString(R.string.select_plot_toedit));
    }

    private void onFarmSelected() {
        Log.e("Called", "onFarmSelected");
        hideAllBtn();
        //correctBoundryBtn.setVisibility(VIEW.VISIBLE);
        zoomInBtn.setVisibility(VIEW.VISIBLE);
        zoomOutBtn.setVisibility(VIEW.VISIBLE);
        //saveEditBtn.setVisibility(VIEW.VISIBLE);
        discardEditBtn.setVisibility(VIEW.VISIBLE);
        //saveEditBtn.setEnabled(true);
        //EDITON = true;
        messageBox.setText(getResources().getString(R.string.select_point_toedit));
    }

    private void onSymbolSelected() {
        Log.e("Called", "onSymbolSelected");
        hideAllBtn();
        zoomInBtn.setVisibility(VIEW.VISIBLE);
        zoomOutBtn.setVisibility(VIEW.VISIBLE);
        upBtn.setVisibility(VIEW.VISIBLE);
        downBtn.setVisibility(VIEW.VISIBLE);
        leftBtn.setVisibility(VIEW.VISIBLE);
        rightBtn.setVisibility(VIEW.VISIBLE);
        discardEditBtn.setVisibility(VIEW.VISIBLE);
        saveEditBtn.setVisibility(VIEW.VISIBLE);
        EDITON = true;
        messageBox.setText(getResources().getString(R.string.drag_point_joystic));
    }

    private void discardEdit() {
        LNGLATEDIT.clear();
        LNGLATEDIT = new ArrayList<>();

        for (int i = 0; i < LNGLAT.size(); i++) {
            List<LatLng> ll = new ArrayList<>();
            for (int j = 0; j < LNGLAT.get(i).size(); j++) {
                double lat = LNGLAT.get(i).get(j).getLatitude();
                double lng = LNGLAT.get(i).get(j).getLongitude();
                ll.add(new LatLng(lat, lng));
            }
            LNGLATEDIT.add(ll);
        }

        redrawFarms();
        showHideSymbol("hide", true);
        showAllLayers();
        onFarmsLoaded();
    }

    private void saveEdits() {
        hideAllBtn();
        //addMoreBtn.setVisibility(VIEW.VISIBLE);
        correctBoundryBtn.setVisibility(VIEW.VISIBLE);
        completeMarkingBtn.setVisibility(VIEW.VISIBLE);
        EDITON = false;
        showHideSymbol("hide", false);
        showAllLayers();
        onFarmsLoaded();
        addMoreBtn.setVisibility(VIEW.GONE);
    }

    private void completeMarking() {
        Log.e("Called", "completeMarking");
//        String strMerge = "{\"farms_fetched_at\":" + "\"" + Common.FARMS_FETCHED_AT + "\"" + ",\"recipe_id\":\"farm_boundaries\",\"aois\":" + String.valueOf(LNGLATEDIT) + "}";
//        JsonObject selectedFarms = JsonParser.parseString(strMerge).getAsJsonObject();
//        interfaceKawaEvents.onkawaSelect(selectedFarms);

        hideAllBtn();
        //backBtn.setVisibility(VIEW.VISIBLE);
        messageBox.setVisibility(View.GONE);
        saveDetailBtn.setVisibility(VIEW.VISIBLE);
        EDITON = false;
        createformsfileds();
    }

    private void saveDetail() {
        hideAllBtn();
        // detailsForm.setVisibility(View.GONE);

        if (Common.PHASERSTR.equals("1")) {
            Log.e("Called", "saveDetail");

            List<Point> llPts = new ArrayList<>();

            for (int i = 0; i < LNGLATEDIT.size(); i++) {
                for (int j = 0; j < LNGLATEDIT.get(i).size(); j++) {
                    llPts.add(Point.fromLngLat(LNGLATEDIT.get(i).get(j).getLongitude(), LNGLATEDIT.get(i).get(j).getLatitude()));
                }
            }

            List<List<Point>> llPtsA = new ArrayList<>();
            llPtsA.add(llPts);
            Feature multiPointFeature = Feature.fromGeometry(Polygon.fromLngLats(llPtsA));

            List<String> listFeatures = new ArrayList<>();
            listFeatures.add(multiPointFeature.toJson());

            JSONObject filedsObject = new JSONObject();
            try {
                if (farm_fields_array != null) {
                    for (int i = 0; i < farm_fields_array.length(); i++) {
                        filedsObject.put(String.valueOf(myTextViews[i].getTag()), myTextViews[i].getText().toString());
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            String strSubmit = "{\"farms_fetched_at\":" + "\"" + Common.FARMS_FETCHED_AT + "\"" + ",\"recipe_id\":\"farm_boundaries\",\"aois\":" + String.valueOf(listFeatures) + "}";
            JsonObject submitJsonObject = JsonParser.parseString(strSubmit).getAsJsonObject();

            //String strJsonWrite = "{“farm_boundaries”: "++", “metadata”: { “farm_name”: "++" } }";
            String strJsonWrite = "{\"farm_boundaries\":" + "\"" + listFeatures + "\"" + ",\"metadata\":" + "\"" + filedsObject + "}";
            Log.e("strJsonWrite", strJsonWrite);

            Common.showLoader("isCircle");
            ServiceManager.getInstance().getKawaService().sumbitPoints(KawaMap.KAWA_API_KEY, Common.SDK_VERSION, submitJsonObject).enqueue(new Callback<MergeModel>() {
                @Override
                public void onResponse(@NonNull Call<MergeModel> call, @NonNull Response<MergeModel> response) {
                    Common.hideLoader();
                    try {
                        if (response.isSuccessful()) {
                            if (response.body() != null) {
                                Log.e("TAG", "onResponse: " + response.body().getStatus());
                                startOverBtn.setVisibility(View.GONE);
                                markAnotherBtn.setVisibility(View.VISIBLE);
                                thankyouLinearLayout.setVisibility(VIEW.VISIBLE);
                                //farmDetailsLayout.setVisibility(VIEW.VISIBLE);

                                //interfaceKawaEvents.onkawaSubmit(strJsonWrite);
                            }
                        } else {
                            Common.hideLoader();
                            if (response.errorBody() != null) {
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
                public void onFailure(@NonNull Call<MergeModel> call, @NonNull Throwable t) {
                    Common.hideLoader();
                    //  String errorBody = t.getMessage();
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.Error_General), Toast.LENGTH_LONG).show();
                }
            });
        } else if (Common.PHASERSTR.equals("2")) {
            totalAreaTv = VIEW.findViewById(R.id.totalAreaTv);
            totalseedsTv = VIEW.findViewById(R.id.totalseedsTv);
            addressTv = VIEW.findViewById(R.id.addressTv);
            Float totalArea = 0.0f;
            for (int i = 0; i < POLYGONAREA.size(); i++) {
                totalArea = totalArea + Float.valueOf((Float) POLYGONAREA.get(i));
            }
            float hectares = (float) (totalArea / 2.47105);
            float seedrequired = hectares * 17 ;
            String hectaresStr = String.format("%.2f", hectares);
            String seedrequiredStr = String.format("%.2f", seedrequired);
            totalAreaTv.setText(hectaresStr + " "+getResources().getString(R.string.hectares));
            totalseedsTv.setText(seedrequiredStr + " KG");
            getAddress();
        }
        else if (Common.PHASERSTR.equals("3")) {
            totalAreaTv = VIEW.findViewById(R.id.totalAreaTv);
            totalseedsTv = VIEW.findViewById(R.id.totalseedsTv);
            addressTv = VIEW.findViewById(R.id.addressTv);
            ImageView dootedLineFirst = VIEW.findViewById(R.id.dootedLineFirst);
            ImageView dootedLineSecond = VIEW.findViewById(R.id.dootedLineSecond);
            LinearLayout seedsLayout = VIEW.findViewById(R.id.seedsLayout);
            LinearLayout locationLayout = VIEW.findViewById(R.id.locationLayout);

            Float totalArea = 0.0f;
            for (int i = 0; i < POLYGONAREA.size(); i++) {
                totalArea = totalArea + Float.valueOf((Float) POLYGONAREA.get(i));
            }
            String areaStr = String.format("%.2f", totalArea);
            totalAreaTv.setText(areaStr + " Acres");
            totalseedsTv.setVisibility(View.GONE);
            addressTv.setVisibility(View.GONE);

            farmDetailsLayout.setVisibility(VIEW.VISIBLE);
            markAnotherBtn.setVisibility(View.VISIBLE);
            startOverBtn.setVisibility(View.GONE);
            messageBox.setVisibility(View.GONE);
            dootedLineFirst.setVisibility(View.GONE);
            dootedLineSecond.setVisibility(View.GONE);
            seedsLayout.setVisibility(View.GONE);
            locationLayout.setVisibility(View.GONE);

            farm_mark_messagebox.setVisibility(View.VISIBLE);
        }
    }

    private void startOver() {
        fragmentFarmLocation fragmentFarmLocation = new fragmentFarmLocation();
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.kawaMapView, fragmentFarmLocation);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    private void createformsfileds() {
        try {
            JSONObject obj = new JSONObject(loadJSONFromAsset());
            Log.e("TAG", "onCreate: " + obj);
            if (obj != null) {
                farm_fields_array = obj.getJSONArray("farm_fields");
                Log.e("TAG", "farm_fields_array: " + farm_fields_array);
                if (farm_fields_array != null && farm_fields_array.length() > 0) {
                    detailsForm.setVisibility(VIEW.VISIBLE);
                    ArrayList<HashMap<String, String>> formList = new ArrayList<HashMap<String, String>>();
                    HashMap<String, String> m_li;
                    myTextViews = new EditText[farm_fields_array.length()]; // create an empty array;
                    LinearLayout.LayoutParams editTextLayoutParams = new LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
                    editTextLayoutParams.setMargins(20, 10, 20, 10);
                    for (int i = 0; i < farm_fields_array.length(); i++) {
                        JSONObject jsonObject = farm_fields_array.getJSONObject(i);
                        String filed_placeholder = jsonObject.getString("field_placeholder");
                        int filed_id = Integer.parseInt(jsonObject.getString("field_id"));
                        String filed_tags = jsonObject.getString("field_tag");
                        final EditText rowEditTextView = new EditText(getActivity());
                        rowEditTextView.setHint(filed_placeholder);
                        rowEditTextView.setId(filed_id);
                        rowEditTextView.setBackgroundColor(Color.WHITE);
                        rowEditTextView.setHintTextColor(Color.LTGRAY);
                        rowEditTextView.setTextColor(Color.BLACK);
                        rowEditTextView.setImeOptions(EditorInfo.IME_ACTION_DONE);
                        rowEditTextView.setSingleLine(true);
                        rowEditTextView.setLayoutParams(editTextLayoutParams);
                        rowEditTextView.setPadding(20, 10, 10, 10);
                        rowEditTextView.setTag(filed_tags);
                        detailsForm.addView(rowEditTextView);
                        myTextViews[i] = rowEditTextView;
                    }
                } else {
                    Toast.makeText(getActivity(), "Invalid JSON", Toast.LENGTH_LONG).show();
                    detailsForm.setVisibility(VIEW.GONE);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            detailsForm.setVisibility(VIEW.GONE);
            //Toast.makeText(getActivity(), "Json file not found", Toast.LENGTH_LONG).show();
        }
    }


    public String loadJSONFromAsset() {
        String json = null;
        try {
            InputStream inputStream = getActivity().getAssets().open("kawa_form_fields.json");
            Log.e("TAG", "loadJSONFromAsset: " + inputStream);
            if (inputStream != null) {
                int size = inputStream.available();
                byte[] buffer = new byte[size];
                inputStream.read(buffer);
                inputStream.close();
                json = new String(buffer, "UTF-8");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            return "";
        }
        return json;
    }

    public void getAddress() {
        LatLng mapLatLng;
        double mapLat, mapLong;
        if (POLYGONAREA.size() == 1) {
            Log.e(TAG, "getAddress:IF "+POLYGONAREA );
            mapLat = LNGLATEDIT.get(0).get(0).getLatitude();
            mapLong = LNGLATEDIT.get(0).get(0).getLongitude();
        } else {
            Log.e(TAG, "getAddress:else "+POLYGONAREA );
            mapLatLng = MAPBOXMAP.getCameraPosition().target;
            mapLat = mapLatLng.getLatitude();
            mapLong = mapLatLng.getLongitude();
        }


        Common.showLoader("isCircle");

        AddressServiceManager.getInstance().getKawaService().getAddress("json", String.valueOf(mapLat), String.valueOf(mapLong))
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                        Common.hideLoader();
                        try {
                            if (response.isSuccessful() && response.body() != null) {
                                JSONObject jsonObj = new JSONObject(response.body().toString());
                                addressTv.setText(jsonObj.getString("display_name"));

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
                        farmDetailsLayout.setVisibility(VIEW.VISIBLE);
                        markAnotherBtn.setVisibility(View.VISIBLE);
                        startOverBtn.setVisibility(View.GONE);
                        messageBox.setVisibility(View.GONE);

                        farm_mark_messagebox.setVisibility(View.VISIBLE);

                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        Common.hideLoader();
                        farmDetailsLayout.setVisibility(VIEW.VISIBLE);
                        //String errorBody = t.getMessage();
                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.Error_General), Toast.LENGTH_LONG).show();
                    }
                });


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

    public void onBackPressed() {
        getActivity().getSupportFragmentManager().popBackStack();
    }
}