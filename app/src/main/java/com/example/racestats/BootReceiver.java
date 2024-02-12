package com.example.racestats;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Intent activityIntent = new Intent(context, MainActivity.class);
            activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(activityIntent);
        }
    }
}

    // will launch users saved view still needs tested.
//    @Override
//    public void onReceive(Context context, Intent intent) {
//        // Access the string resource within the onReceive method
//        String viewToLaunch = context.getString(R.string.launchView);
//
//        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
//            // Assuming 'LaunchView' represents a class name, modify this part accordingly
//            try {
//                Class<?> launchViewClass = Class.forName(viewToLaunch);
//                Intent launchIntent = new Intent(context, launchViewClass);
//                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                context.startActivity(launchIntent);
//            } catch (ClassNotFoundException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//}
