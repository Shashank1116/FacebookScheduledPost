package com.example.facebookschedulepost

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap

class MyPostAlarm:BroadcastReceiver(){
    override fun onReceive(context: Context?, intent: Intent?) {
        RxBus.getInstance().triggerEvent(TimeToPostEvent())
    }

}