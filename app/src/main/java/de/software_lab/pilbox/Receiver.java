// 18oct24abu
// (c) Software Lab. Alexander Burger

package de.software_lab.pilbox;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class Receiver extends BroadcastReceiver {
   @Override public void onReceive(Context context, Intent intent) {
      intent = (new Intent(context, PicoLisp.class)).setAction("RPC").putExtras(intent);
      context.startForegroundService(intent);
   }
}
