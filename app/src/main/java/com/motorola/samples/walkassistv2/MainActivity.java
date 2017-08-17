/**
 * Copyright (c) 2016 Motorola Mobility, LLC.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holder nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.motorola.samples.modsraw;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toolbar;

import com.motorola.mod.ModDevice;
import com.motorola.mod.ModManager;

import java.nio.ByteBuffer;

import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.LineChartView;

/**
 * A class to represent main activity.
 */
public class MainActivity extends Activity implements View.OnClickListener {
    public static final String MOD_UID = "mod_uid";

    private static final int RAW_PERMISSION_REQUEST_CODE = 100;

    /**
     * Instance of MDK Personality Card interface
     */
    private Personality personality;

    /**
     * Line chart to draw temperature values
     */
    private static int count;
    private static float maxTop = 80f;
    private static float minTop = 70f;
    private LineChartView chart;
    private Viewport viewPort;

    /**
     * Voltmeter Project variables
     */
    boolean flag = true;
    TextView txt;


    /** Handler for events from mod device */
    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Personality.MSG_MOD_DEVICE:
                    /** Mod attach/detach */
                    ModDevice device = personality.getModDevice();
//                    Button scale1 = (Button)findViewById(R.id.scale1);
//                    Button scale2 = (Button)findViewById(R.id.scale2);
//                    Button scale3 = (Button)findViewById(R.id.scale3);
//                    scale1.setBackgroundColor(0xFF5CA0CB);
//                    scale2.setBackgroundColor(0xFF222222);
//                    scale3.setBackgroundColor(0xFF222222);
                    break;
                case Personality.MSG_RAW_DATA:
                    /** Mod raw data */
                    byte[] buff = (byte[]) msg.obj;
                    int length = msg.arg1;
                    //onRawData(buff, length);
                    Log.i(Constants.TAG, "MSG_RAW_DATA  Buff:  " + msg.obj);
                    Log.i(Constants.TAG, "MSG_RAW_DATA  Length " + msg.arg1);
//                    txt.setText(buff[0]);LOG
                    if (buff[0]==1)
                        txt.setText("Scale 1");
                    else if (buff[0]==2)
                        txt.setText("Scale 2");
                    else if (buff[0]==3)
                        txt.setText("Scale 3");
                    if (length == 5){
                        byte[] content = new byte[4];
                        for (int i = 1; i < 5; i++) {
                            content[4 - i] = buff[i];
                        }
                        float adjust = (float) 8.42/2255;
//                        float value = ByteBuffer.wrap(content).getFloat()/2255;
                        float value = ByteBuffer.wrap(content).getFloat()*adjust;
                        String dataStr = String.format("%.3f V", value);
                        txt.setText(dataStr);
                    }
                    break;
                case Personality.MSG_RAW_IO_READY:
                    /** Mod RAW I/O ready to use */
                    onRawInterfaceReady();
                    break;
                case Personality.MSG_RAW_IO_EXCEPTION:
                    /** Mod RAW I/O exception */
                    onIOException();
                    break;
                case Personality.MSG_RAW_REQUEST_PERMISSION:
                    /** Request grant RAW_PROTOCOL permission */
                    onRequestRawPermission();
                default:
                    Log.i(Constants.TAG, "MainActivity - Un-handle events: " + msg.what);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setActionBar(toolbar);

        txt = (TextView)findViewById(R.id.txt);

//        Button bttn = (Button)findViewById(R.id.bttn);
        final Button bttnADC = (Button)findViewById(R.id.bttnADC);
//        Button scale1 = (Button)findViewById(R.id.scale1);
//        Button scale2 = (Button)findViewById(R.id.scale2);
//        Button scale3 = (Button)findViewById(R.id.scale3);

//        bttn.setOnClickListener(this);
        bttnADC.setOnClickListener(this);
//        scale1.setOnClickListener(this);
//        scale2.setOnClickListener(this);
//        scale3.setOnClickListener(this);

        CheckBox autoCollect = (CheckBox)findViewById(R.id.autoCollect);
        autoCollect.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    bttnADC.setEnabled(false);
                    bttnADC.setBackgroundColor(0xFF222222);
                    bttnADC.setTextColor(0xFF666666);
                    personality.getRaw().executeRaw(Constants.RAW_CMD_ADC_ON);
                    //autoCollectVoltage.run();
                } else {
                    bttnADC.setEnabled(true);
                    bttnADC.setBackgroundColor(0xFF5CC0A0);
                    bttnADC.setTextColor(0xFFFFFFFF);
                    //handler.removeCallbacks(autoCollectVoltage);
                    personality.getRaw().executeRaw(Constants.RAW_CMD_ADC_OFF);
                }
            }
        });

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(autoCollectVoltage);
        releasePersonality();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        /** Initial MDK Personality interface */
        initPersonality();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    /** Initial MDK Personality interface */
    private void initPersonality() {
        if (null == personality) {
//            personality = new RawPersonality(this, Constants.VID_MDK, Constants.PID_TEMPERATURE);
            personality = new RawPersonality(this, Constants.VID_DEVELOPER, Constants.PID_DEVELOPER);
            personality.registerListener(handler);
        }
    }

    /** Clean up MDK Personality interface */
    private void releasePersonality() {
        /** Clean up MDK Personality interface */
        if (null != personality) {
            personality.getRaw().executeRaw(Constants.RAW_CMD_STOP);
            personality.onDestroy();
            personality = null;
        }
    }

    /** Button click event from UI */
    @Override
    public void onClick(View v) {
        if (v == null) {
            return;
        }
//        Button scale1 = (Button)findViewById(R.id.scale1);
//        Button scale2 = (Button)findViewById(R.id.scale2);
//        Button scale3 = (Button)findViewById(R.id.scale3);
        switch (v.getId()) {
//            case R.id.bttn:
//                if (flag) {
//                    personality.getRaw().executeRaw(Constants.RAW_CMD_LED_ON);
//                    txt.setText("LED_ON");
//                    flag = false;
//                } else {
//                    personality.getRaw().executeRaw(Constants.RAW_CMD_LED_OFF);
//                    txt.setText("LED_OFF");
//                    flag = true;
//                }
//                break;
            case R.id.bttnADC:
                personality.getRaw().executeRaw(Constants.RAW_CMD_ADC_ON);
                break;
//            case R.id.scale1:
//                scale1.setBackgroundColor(0xFF5CA0CB);
//                scale2.setBackgroundColor(0xFF222222);
//                scale3.setBackgroundColor(0xFF222222);
//                personality.getRaw().executeRaw(Constants.RAW_CMD_SCALE1);
//                break;
//            case R.id.scale2:
//                scale1.setBackgroundColor(0xFF222222);
//                scale2.setBackgroundColor(0xFF5CA0CB);
//                scale3.setBackgroundColor(0xFF222222);
//                personality.getRaw().executeRaw(Constants.RAW_CMD_SCALE2);
//                break;
//            case R.id.scale3:
//                scale1.setBackgroundColor(0xFF222222);
//                scale2.setBackgroundColor(0xFF222222);
//                scale3.setBackgroundColor(0xFF5CA0CB);
//                personality.getRaw().executeRaw(Constants.RAW_CMD_SCALE3);
//                break;
            default:
                Log.i(Constants.TAG, "Alert: Main action not handle.");
                break;
        }
    }


    /** RAW I/O of attached mod device is ready to use */
    public void onRawInterfaceReady() {
        /**
         *  Personality has the RAW interface, query the information data via RAW command, the data
         *  will send back from MDK with flag TEMP_RAW_COMMAND_INFO and TEMP_RAW_COMMAND_CHALLENGE.
         */
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                personality.getRaw().executeRaw(Constants.RAW_CMD_INFO);
            }
        }, 500);
    }

    /** Handle the IO issue when write / read */
    public void onIOException() {
    }

    /*
     * Beginning in Android 6.0 (API level 23), users grant permissions to apps while
     * the app is running, not when they install the app. App need check on and request
     * permission every time perform an operation.
    */
    public void onRequestRawPermission() {
        requestPermissions(new String[]{ModManager.PERMISSION_USE_RAW_PROTOCOL},
                117);
    }

    /** Handle permission request result */
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == RAW_PERMISSION_REQUEST_CODE && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (null != personality) {
                    /** Permission grant, try to check RAW I/O of mod device */
                    personality.getRaw().checkRawInterface();
                }
            } else {
                // TODO: user declined for RAW accessing permission.
                // You may need pop up a description dialog or other prompts to explain
                // the app cannot work without the permission granted.
            }
        }
    }

    /** Runnable */
    private Runnable autoCollectVoltage = new Runnable() {
        @Override
        public void run() {
            int mInterval = 1000; // 1 second interval to refresh Display
            personality.getRaw().executeRaw(Constants.RAW_CMD_ADC_ON);
            handler.postDelayed(autoCollectVoltage, mInterval);
        }
    };
}
