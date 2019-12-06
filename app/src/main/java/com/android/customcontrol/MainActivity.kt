package com.android.customcontrol

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.CompoundButton
import android.widget.RadioGroup
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        switcher.setOnCheckedChangeListener {
                _: CompoundButton?, isChecked: Boolean ->
            customController.isRunningBar = isChecked
        }
    }




}
