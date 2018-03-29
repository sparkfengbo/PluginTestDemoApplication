package com.sparkfengbo.ng.plugintestdemoapplication;

import android.os.Bundle;
import android.app.Activity;

public class StubEmptyActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /**
         * 空的Activity，用来绕过AMS的鉴定
         */
        setContentView(R.layout.activity_stub_empty);
    }

}
