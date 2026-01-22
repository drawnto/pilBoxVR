// 15jan26abu
// (c) Software Lab. Alexander Burger

package de.software_lab.pilbox;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;
import android.app.Service;
import android.os.IBinder;
import android.os.SystemClock;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import android.view.View;
import android.view.ViewGroup.MarginLayoutParams;
import android.os.Build;
import android.util.Log;

public class PicoLisp extends Service {
   String Home, Uuid, Port, Start;
   File Pid, Pty;
   Process Proc;
   Reflector Rfl;
   OutputStream StdIn;
   InputStream StdOut;
   ServerSocket Tty;
   static Socket Terminal;
   static OutputStream Screen;
   static PilBoxActivity GUI;
   static boolean Loaded;
   FbView FB;
   public static native int fbBeg(int size);
   public static native void fbDraw();
   public static native void fbEnd();

   @Override public void onCreate() {
      super.onCreate();
      Home = getFilesDir().getPath();
      Pid = new File(Home + "/PID");
      Pty = new File(Home + "/.pty");
      rmDir(new File(Home + "/.pil/tmp"));
   }

   @Override public int onStartCommand(Intent intent, int flags, int startId) {
      try {
         if (PilBoxActivity.SRV != this) {
            log("Service started");
            (new File(Home + "/log-")).renameTo(new File(Home + "/log--"));
            (new File(Home + "/log")).renameTo(new File(Home + "/log-"));
            Uuid = line1(Home + "/UUID");
            Port = line1(Home + "/Port");
            Start = "http://localhost:" + Port;
            if (Pid.exists()) {
               android.os.Process.sendSignal(Integer.parseInt(line1(Pid)), 9);
               if (Pty.exists())
                  toast("Zombie");
               Pid.delete();
            }
            ProcessBuilder pb = new ProcessBuilder("bin/picolisp", "lib.l", "App.l", "-go", Pty.exists()? "+" : "-wait");
            Map<String, String> env = pb.environment();
            env.put("HOME", Home);
            env.put("PORT", Port);
            env.put("LD_LIBRARY_PATH", Home + "/lib");
            env.put("TERMINFO", Home + "/lib/terminfo");
            env.put("SDK_INT", Integer.toString(Build.VERSION.SDK_INT));
            pb.redirectErrorStream(true);
            pb.directory(new File(Home));

             try {
                 Proc = pb.start();
             }
             catch(Exception e) {
                 err("Starting failed", e);
                 throw e;
             }


            StdIn = Proc.getOutputStream();
            StdOut = Proc.getInputStream();
            (new Thread() {
               OutputStream log = new FileOutputStream(Home + "/log");
               public void run() {
                  try {
                     byte[] buf = new byte[1024];
                     int n;
                     while ((n = StdOut.read(buf)) > 0) {
                        if (Terminal == null) {
                           log.write(buf, 0, n);
                           log.flush();
                        }
                        else {
                           Screen.write(buf, 0, n);
                           Screen.flush();
                        }
                     }
                     log.close();
                  }
                  catch (IOException e) {
                     err("STDOUT", e);
                  }
               }
            } ).start();
            if (Pty.exists()) {
               Tty = new ServerSocket(Integer.parseInt(Port) + 1);
               (new Thread() {
                  public void run() {
                     while (Tty != null && !Tty.isClosed()) {
                        try {
                           Terminal = Tty.accept();
                           Screen = Terminal.getOutputStream();
                           InputStream in = Terminal.getInputStream();
                           StringBuffer s = new StringBuffer();
                           int c;
                           while ((c = in.read()) > 0  &&  c != '\n')
                              s.append((char)c);
                           if (s.toString().equals(line1(Pty))) {
                              byte[] buf = new byte[1024];
                              int n;
                              while ((n = in.read(buf)) > 0) {
                                 StdIn.write(buf, 0, n);
                                 StdIn.flush();
                              }
                           }
                           if (Terminal != null) {
                              Terminal.close();
                              Terminal = null;
                           }
                        }
                        catch (IOException e) {
                           err("STDIN", e);
                        }
                     }
                  }
               } ).start();
            }
            do
               SystemClock.sleep(10);
            while (!Pid.exists());
            (Rfl = new Reflector(this, Home + "/JAVA", Home + "/LISP", Home + "/RQST", Home + "/RPLY")).start();
            Pid.delete();
            do
               SystemClock.sleep(10);
            while (!Pid.exists());
            PilBoxActivity.SRV = this;
         }
         if (GUI != null  &&  !Loaded) {
            if (GUI.State == null)
               GUI.PilView.loadUrl(Start + "?" + Uuid);
            else {
               GUI.PilView.loadUrl(GUI.State.getString("url"));
               GUI.State = null;
            }
            Loaded = true;
         }
      }
      catch (Exception e) {
         toast("Cannot start\n" + e.toString());
      }
      intent(intent);
      return START_NOT_STICKY;
   }

   @Override public IBinder onBind(Intent intent) {
      return null;
   }

   void stopProc() {
      if (Proc != null) {
         try {
            android.os.Process.sendSignal(Integer.parseInt(line1(Pid)), 15);
            if (Proc.waitFor(12000, java.util.concurrent.TimeUnit.MILLISECONDS))
               Pid.delete();
            if (Tty != null) {
               if (Terminal != null) {
                  Terminal.close();
                  Terminal = null;
               }
               Tty.close();
               Tty = null;
            }
            Proc = null;
         }
         catch (Exception e) {
            err("Stopping PicoLisp", e);
         }
      }
   }

   static String line1(File file) throws IOException {
      BufferedReader rd = new BufferedReader(new FileReader(file));
      String s = rd.readLine();
      rd.close();
      return s;
   }

   static String line1(InputStream in) throws IOException {
      BufferedReader rd = new BufferedReader(new InputStreamReader(in));
      String s = rd.readLine();
      rd.close();
      return s;
   }

   static String line1(String nm) throws IOException {
      BufferedReader rd = new BufferedReader(new FileReader(nm));
      String s = rd.readLine();
      rd.close();
      return s;
   }

   void rmDir(File dir) {
      if (dir.exists())
         for (File f : dir.listFiles()) {
            if (f.isDirectory())
               rmDir(f);
            f.delete();
         }
   }

   void intent(Intent intent) {
      try {
         if (intent.getAction() == Intent.ACTION_VIEW) {
            Uri uri = intent.getData();
            if (uri != null  &&  uri.getLastPathSegment() != null) {
               install(Home + "/",
                  getContentResolver().openInputStream(uri),
                  uri.getLastPathSegment().toLowerCase() );
               if (GUI != null)
                  GUI.restart();
            }
         }
         else if ("RPC".equals(intent.getAction()) && Uuid.equals(intent.getStringExtra("UUID"))) {
            InOut io = new InOut(this, null, new FileOutputStream(Home + "/BOSS"));

            io.Out.write(InOut.BEG);
            io.prSym("rpc");
            io.print(intent.getStringExtra("SRC"));
            io.print(intent.getIntExtra("ARG", 0));
            String s = intent.getStringExtra("URL");
            if (s != null)
               io.print(s);
            io.Out.write(InOut.END);
            io.close();
         }
      }
      catch (Exception e) {
         err("rpc", e);
      }
   }

   static void install(String dst, InputStream in, String nm) throws Exception {
      String path = new File(dst).getCanonicalPath();
      ZipInputStream zip = new ZipInputStream(in);
      byte[] buf = new byte[4096];
      PrintWriter pil;
      ZipEntry ze;
      int n;

      if (nm == null)
         pil = null;
      else {
         if (nm.endsWith(".zip"))
            nm = nm.substring(0, nm.length()-4);
         pil = new PrintWriter(dst + "PIL-" + nm);
      }
      while ((ze = zip.getNextEntry()) != null) {
         String s = ze.getName();
         File f = new File(dst + s);

         if (!f.getCanonicalPath().startsWith(path))
            throw new SecurityException("Bad ZIP path");
         if (pil != null)
            pil.println(s);
         if (ze.isDirectory())
            f.mkdir();
         else {
            File p = f.getParentFile();
            if (p != null)
               p.mkdirs();
            OutputStream out = new FileOutputStream(f);
            while ((n = zip.read(buf)) > 0)
               out.write(buf, 0, n);
            out.close();
            zip.closeEntry();
            f.setLastModified(ze.getTime());
         }
      }
      if (pil != null)
         pil.close();
      zip.close();
   }

   public void install(String name, String file) throws Exception {
      install(Home + "/", new FileInputStream(Home + "/" + file), name);
   }

   public void zip(String file, String[] lst) throws Exception {
      ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(Home + "/" + file));
      byte[] buf = new byte[4096];
      int n;

      for (int i = 0; i < lst.length; ++i) {
         File f = new File(Home + "/" + lst[i]);
         FileInputStream in = new FileInputStream(f);
         ZipEntry ze = new ZipEntry(lst[i]);
         ze.setTime(f.lastModified());
         zip.putNextEntry(ze);
         while ((n = in.read(buf)) > 0)
            zip.write(buf, 0, n);
         zip.closeEntry();
         in.close();
      }
      zip.close();
   }

   public void setResultProxy(ResultProxy p) {
      if (GUI != null)
         GUI.Result = p;
   }

   public void clearHistory() {
      if (GUI != null)
         GUI.ClearHistory = true;
   }

   public void back(final String s) {
      if (GUI != null)
         GUI.runOnUiThread(new Runnable() {
            public void run() {
               GUI.Back = s;
               GUI.naviVis();
            }
         } );
   }

   public void fore(final String s) {
      if (GUI != null)
         GUI.runOnUiThread(new Runnable() {
            public void run() {
               GUI.Fore = s;
               GUI.naviVis();
            }
         } );
   }

   public void exit(final String s) {
      if (GUI != null)
         GUI.runOnUiThread(new Runnable() {
            public void run() {
               GUI.Exit = s;
               GUI.naviVis();
            }
         } );
   }

   public void auto(String[] lst) {
      if (GUI != null) {
         int i = 0;
         GUI.Auto = new String[lst.length / 3][];
         for (int j = 0; j < GUI.Auto.length; ++j) {
            GUI.Auto[j] = new String[3];
            GUI.Auto[j][0] = lst[i++];
            GUI.Auto[j][1] = lst[i++];
            GUI.Auto[j][2] = lst[i++];
         }
      }
   }

   public void toast(final String s) {
      if (GUI != null)
         GUI.runOnUiThread(new Runnable() {
            public void run() {
               Toast.makeText(GUI, s, Toast.LENGTH_LONG).show();
            }
         } );
   }

   public void showFB(int x, int y, int dx, int dy) {
      if (FB == null) {
         System.loadLibrary("FbView");
         FB = new FbView(GUI);
      }
      GUI.runOnUiThread(new Runnable() {
         public void run() {
            MarginLayoutParams par = new MarginLayoutParams(dx, dy);
            par.leftMargin = x;
            par.topMargin = y;
            GUI.Layout.addView(FB, par);
            FB.setVisibility(View.VISIBLE);
            int res = fbBeg(4 * dx * dy);
            if (res != 0)
               toast("FB Error: " + res);
         }
      } );
   }

   public void drawFB() {fbDraw();}

   public void hideFB() {
      GUI.runOnUiThread(new Runnable() {
         public void run() {
            fbEnd();
            FB.setVisibility(View.GONE);
            GUI.Layout.removeView(FB);
         }
      } );
   }

   static void log(String msg) {
      Log.d("PilBox", msg);
   }

   static void err(String msg, Exception e) {
      Log.e("PilBox", msg + " " + e.toString());
   }
}
