package com.advmeds.cardreaderdemo

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    public fun onButtonClicked(sender: View) {
        when (sender.id) {
            R.id.ble_button -> {
                val intent = Intent(this, BluetoothActivity::class.java)
                startActivity(intent)
            }
            R.id.usb_button -> {
                val intent = Intent(this, USBActivity::class.java)
                startActivity(intent)
            }
        }
    }
}