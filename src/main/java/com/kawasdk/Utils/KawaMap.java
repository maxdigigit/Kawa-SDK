package com.kawasdk.Utils;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.segment.analytics.Analytics;
import com.segment.analytics.Properties;
import com.smartlook.sdk.smartlook.Smartlook;

public class KawaMap extends AppCompatActivity {

    public static Context context;

    public static InterfaceKawaEvents interfaceKawaEvents;
    public static Integer footerBtnBgColor = Color.BLUE;
    public static Integer footerBtnTextColor = Color.WHITE;

    public static Integer innerBtnBgColor = Color.WHITE;
    public static Integer innerBtnTextColor = Color.BLACK;

    public static Integer headerBgColor = Color.WHITE;
    public static Integer headerTextColor = Color.BLACK;
    public static boolean isValidKawaAPiKey;
    public static String KAWA_API_KEY = "";
    public static String SEGMENT_API_KEY = "";
    public static String SMARTLOOK_API_KEY = "";
    public static boolean isMergeEnable = false;
    public static boolean isEditEnable = false;
    public static boolean isFormEnable = false;
    public static boolean isFarmDetailsEnable = false;
    public static boolean isOtherFarmDetailsEnable = false;
    public static boolean isSaveResultEnable = false;
    public static boolean isBahasaEnable = false;
    public static boolean isFlyToLocationEnable = false;

    public KawaMap(Context context) {
        this.context = context;
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        interfaceKawaEvents = (InterfaceKawaEvents) context;
    }

    public static boolean initKawaMap(String kawa_api_key) {
        isValidKawaAPiKey = false;
        if (kawa_api_key != null && kawa_api_key != "") {
            KAWA_API_KEY = kawa_api_key;
            isValidKawaAPiKey = true;
        }
        return isValidKawaAPiKey;
        //interfaceKawaEvents.initKawaMap(isValid);
    }

    public static void initSmartlook(String smartlook_api_key) {
        if (smartlook_api_key != null || smartlook_api_key != "") {
            SMARTLOOK_API_KEY = smartlook_api_key;
            smartlookIntegration();
        }
    }

    public static void initSegment(Context context, String segment_api_key) {

        if (segment_api_key != null || segment_api_key != "") {
            SEGMENT_API_KEY = segment_api_key;
            segmentIntegration(context);
        }
    }

    public static void setFooterBtnBgColorAndTextColor(int bgColor, int textColor) {
        footerBtnBgColor = bgColor;
        footerBtnTextColor = textColor;
    }

    public static void setInnerBtnBgColorAndTextColor(int bgColor, int textColor) {
        innerBtnBgColor = bgColor;
        innerBtnTextColor = textColor;
    }

    public static void setHeaderBgColorAndTextColor(int bgColor, int textColor) {
        headerBgColor = bgColor;
        headerTextColor = textColor;
    }

    public static void setFooterButtonColor(Button[] footerButtons) {
        for (int i = 0; i < footerButtons.length; i++) {
            footerButtons[i].setTextColor(footerBtnTextColor);
            footerButtons[i].setBackgroundColor(footerBtnBgColor);
        }
    }

    public static void setInnerButtonColor(Button[] innerButtons) {
        for (int i = 0; i < innerButtons.length; i++) {
            innerButtons[i].setTextColor(innerBtnTextColor);
            innerButtons[i].setBackgroundColor(innerBtnBgColor);
        }
    }

    public static void smartlookIntegration() {
        Smartlook.setUserIdentifier("KAWA SDK");
        Smartlook.SetupOptionsBuilder builder = new Smartlook.SetupOptionsBuilder(SMARTLOOK_API_KEY)
                .setFps(2)
                .setExperimental(true)
                .setActivity(null);
        Smartlook.setupAndStartRecording(builder.build());
    }

    public static void segmentIntegration(Context context) {
        // Create an analytics client with the given context and Segment write key.
        String fildsInformation = Common.getFiledsDetails();
        try {
            Analytics analytics = new Analytics.Builder(context, SEGMENT_API_KEY)
                    // Enable this to record certain application events automatically!
                    .trackApplicationLifecycleEvents()
                    // Enable this to record screen views automatically!
                    .recordScreenViews()
                    .build();
            Analytics.setSingletonInstance(analytics);

            Properties properties = new Properties();
            String jString = fildsInformation +",\"metadata\":{\"message\":\"User Details Saved\"}}";
            JsonObject jsonObject = JsonParser.parseString(jString).getAsJsonObject();
            properties.putValue("data", jsonObject);
            Analytics.with(context).track("Map Initialisation", properties);

        } catch (Exception e) {
            Log.e("catch-error", String.valueOf(e.getMessage()));
            Toast.makeText(context, String.valueOf(e.getMessage()), Toast.LENGTH_LONG).show();
        }

    }

}
