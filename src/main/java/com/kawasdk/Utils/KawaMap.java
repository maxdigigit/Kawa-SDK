package com.kawasdk.Utils;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

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
}
