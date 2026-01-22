// 07jan26abu
// (c) Software Lab. Alexander Burger

package de.software_lab.pilbox;

import android.view.Surface;
import android.view.SurfaceView;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;

public class FbView extends SurfaceView implements SurfaceHolder.Callback {
   public static native void fbWindow(Surface surface);

   public FbView(PilBoxActivity context) {
      super(context);
      getHolder().addCallback(this);
   }

   @Override public void surfaceCreated(SurfaceHolder holder) {
      fbWindow(holder.getSurface());
   }

   @Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
   }

   @Override public void surfaceDestroyed(SurfaceHolder holder) {
      fbWindow(null);
   }
}
