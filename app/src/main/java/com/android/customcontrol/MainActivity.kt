package com.android.customcontrol

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        customController.onProgressChangedListener = object : CustomController.OnProgressListener {
            override fun onProgressChanged(progress: Float) {

            }

        }
    }




}
