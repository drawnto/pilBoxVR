// 13jan26abu
// (c) Software Lab. Alexander Burger

package de.software_lab.pilbox;

import java.io.*;
import java.util.*;
import java.net.URI;
import java.net.URLConnection;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.ConsoleMessage;
import android.webkit.ConsoleMessage.MessageLevel;
import android.webkit.ValueCallback;
import android.webkit.PermissionRequest;
import android.webkit.WebResourceRequest;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import androidx.core.content.FileProvider;
import android.graphics.Bitmap;

public class PilBoxActivity extends Activity {
   Bundle State;
   WebView PilView;
   WebSettings Config;
   ResultProxy Result;
   ValueCallback<Uri[]> Vcb;
   boolean Navi, ClearHistory;
   String Home, Url, Back, Fore, Exit, Ex;
   String[][] Auto;
   static PicoLisp SRV;
   FrameLayout Layout;

   @Override protected void onCreate(Bundle state) {
      int resId;

      super.onCreate(State = state);
      setContentView(R.layout.activity_pil_box);
      if ((resId = getResources().getIdentifier("status_bar_height", "dimen", "android")) > 0)
         (Layout = ((FrameLayout)findViewById(R.id.webview_container))).setPadding(0, getResources().getDimensionPixelSize(resId), 0, 0);
      PilView = (WebView)findViewById(R.id.webview);
      PilView.setOnLongClickListener(new View.OnLongClickListener() {
         @Override public boolean onLongClick(View v) {
            if (!Navi)
               naviVis();
            else {
               findViewById(R.id.back).setVisibility(View.INVISIBLE);
               findViewById(R.id.fore).setVisibility(View.INVISIBLE);
               findViewById(R.id.exit).setVisibility(View.INVISIBLE);
               Navi = false;
            }
            return false;
         }
      } );
      Config = PilView.getSettings();
      Config.setBuiltInZoomControls(true);
      Config.setDisplayZoomControls(false);
      Config.setJavaScriptEnabled(true);
      Config.setDomStorageEnabled(true);
      Config.setUserAgentString("PilBox (Linux; Android)");
      PilView.setWebViewClient(new WebViewClient() {
         @Override public boolean shouldOverrideUrlLoading(WebView view,
                                          final WebResourceRequest request) {
            Uri uri = request.getUrl();
            String schm = uri.getScheme();
            if ("mailto".equals(schm) || "tel".equals(schm)) {
               try {
                  startActivity(new Intent(Intent.ACTION_VIEW, uri));
               }
               catch (Exception e) {
                  PicoLisp.err("Scheme " + schm, e);
               }
               return true;
            }
            if (uri.isHierarchical()) {
               final String type = uri.getQueryParameter("*pbType");
               if (type != null && !type.isEmpty()) {
                  (new Thread() {
                     public void run() {
                        try {
                           File f = new File(Home + ".pil/tmp/" + request.getUrl().getQueryParameter("*pbName"));
                           InputStream in = (URI.create(request.getUrl().toString()).toURL()).openConnection().getInputStream();
                           OutputStream out = new FileOutputStream(f);
                           byte[] buf = new byte[4096];
                           int n;
                           while ((n = in.read(buf)) > 0)
                              out.write(buf, 0, n);
                           out.close();
                           in.close();
                           Uri pub = FileProvider.getUriForFile(PicoLisp.GUI, "de.software_lab.pilbox.fileprovider", f);
                           Intent i = new Intent(Intent.ACTION_VIEW);
                           i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                           i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                           i.setDataAndType(pub, type);
                           startActivity(i);
                        }
                        catch (Exception e) {
                           PicoLisp.err("FileProvider", e);
                        }
                     }
                  } ).start();
                  return true;
               }
            }
            return false;
         }
         @Override public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            Url = url;
            Back = Fore = null;
         }
         @Override public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            if (ClearHistory) {
               ClearHistory = false;
               view.clearHistory();
               Back = Fore = null;
            }
            if (SRV != null  &&  url.startsWith(SRV.Start))
               ClearHistory = true;
            if (Auto != null)
               for (String[] a: Auto)
                  if (url.indexOf(a[0]) >= 0)
                     view.evaluateJavascript(
                           "(isNaN('" + a[1] +
                           "')?document.getElementById('" + a[1] +
                           "'):document.getElementsByTagName('input')[" + a[1] +
                           "]).value='" + a[2] + "'",
                        null );
            naviVis();
         }
      } );
      PilView.setWebChromeClient(new WebChromeClient() {
         @Override public void onPermissionRequest(PermissionRequest req) {
            req.grant(req.getResources());
         }
         @Override public boolean onConsoleMessage(ConsoleMessage msg) {
            if (PicoLisp.Terminal != null  &&  msg.messageLevel() == ConsoleMessage.MessageLevel.ERROR) {
               try {
                  InOut rpc = SRV.Rfl.Rpc;

                  rpc.print(msg.lineNumber());
                  rpc.print(msg.sourceId());
                  rpc.print(msg.message());
                  rpc.flush();
               } catch (Exception e) {}
            }
            return true;
         }
         @Override public boolean onShowFileChooser(WebView view,
               ValueCallback<Uri[]> vcb, WebChromeClient.FileChooserParams fcp ) {
            Vcb = vcb;
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("*/*");
            startActivityForResult(Intent.createChooser(i,"File"), 1);
            return true;
         }
      } );
      Home = getFilesDir().getPath() + "/";
      try {
         if (state == null) {
            File uuid = new File(Home + "UUID");
            if (!uuid.exists()) {
               PrintWriter out = new PrintWriter(uuid);
               out.println(UUID.randomUUID().toString());
               out.close();
            }
            if (!(new File(Home + "Version")).exists() ||
                  !PicoLisp.line1(Home + "Version").equals(PicoLisp.line1(getAssets().open("run/Version"))) ) {
               copyAssets("run/", getAssets().list("run"), "");
               mkDir(Home + "bin");
            }
            binary("libpicolisp.so", Home + "bin/picolisp");
            binary("libssl.so", Home + "bin/ssl");
            binary("libandroid-support.so", Home + "lib/libandroid-support.so");
            binary("libffi.so", Home + "lib/libffi.so");
            binary("libreadline.8.3.so", Home + "lib/libreadline.so.8");
            binary("libncursesw.6.5.so", Home + "lib/libncursesw.so.6");
            binary("libssl.3.so", Home + "lib/libssl.so.3");
            binary("libcrypto.3.so", Home + "lib/libcrypto.so.3");
            binary("libext.so", Home + "lib/ext.so");
            binary("libht.so", Home + "lib/ht.so");
            binary("libfb.so", Home + "lib/fb.so");
            Uri uri = null;
            if (getIntent().getAction() == Intent.ACTION_SEND)
               uri = getIntent().getParcelableExtra(Intent.EXTRA_STREAM, Uri.class);
            else if (getIntent().getAction() == Intent.ACTION_VIEW)
               uri = getIntent().getData();
            if (uri != null  &&  uri.getLastPathSegment() != null) {
               PicoLisp.install(Home,
                  getContentResolver().openInputStream(uri),
                  uri.getLastPathSegment().toLowerCase() );
               if (SRV != null) {
                  SRV.stopProc();
                  SRV = null;
               }
               PicoLisp.Loaded = false;
            }
         }
         PicoLisp.GUI = this;
         Intent intent = new Intent(this, PicoLisp.class);
         if ("RPC".equals(getIntent().getAction()))
            intent.setAction("RPC").putExtras(getIntent());
         startForegroundService(intent);
      }
      catch (Exception e) {
         PilView.loadData("<html><body><h3>" + e.toString() + "</h3></body></html>", "text/html; charset=utf-8", null);
      }
   }

   @Override protected void onDestroy() {
      SRV.stopProc();
      SRV.stopSelf();
      PicoLisp.GUI = null;
      PicoLisp.Loaded = false;
      PilView.destroy();
      super.onDestroy();
   }

   @Override protected void onSaveInstanceState(Bundle state) {
      super.onSaveInstanceState(state);
      state.putString("url", Url);
   }

   @Override protected void onNewIntent(Intent intent) {
      super.onNewIntent(intent);
      if (Result != null)
         Result.intent(intent);
      else if (SRV != null)
         SRV.intent(intent);
   }

   @Override protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
      super.onActivityResult(requestCode, resultCode, intent);
      if (Result != null) {
         if (resultCode == RESULT_OK)
            Result.good(requestCode, intent);
         else
            Result.bad(requestCode, resultCode);
         Result = null;
      }
      else if (Vcb != null) {
         Uri[] uris = null;
         if (resultCode == RESULT_OK  &&  intent != null) {
            String str = intent.getDataString();
            if (str != null)
               uris = new Uri[]{Uri.parse(str)};
         }
         Vcb.onReceiveValue(uris);
         Vcb = null;
      }
   }

   void copyAssets(String src, String[] lst, String dst) throws IOException {
      for (int i = 0; i < lst.length; ++i) {
         String s = src + lst[i];
         String d = dst + lst[i];
         String[] x = getAssets().list(s);
         if (x.length != 0) {
            mkDir(Home + d);
            copyAssets(s + "/", x, d + "/");
         }
         else {
            InputStream in = getAssets().open(s);
            OutputStream out = new FileOutputStream(new File(Home + d));
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0)
               out.write(buf, 0, n);
            out.close();
            in.close();
         }
      }
   }

   static void err(String msg, Exception e) {
        Log.e("PilBox", msg + " " + e.toString());
    }
   void binary(String src, String dst) throws android.system.ErrnoException {
      try {
         android.system.Os.remove(dst);
      } catch (android.system.ErrnoException e) {}
      try {
          android.system.Os.symlink(getApplicationInfo().nativeLibraryDir + "/" + src, dst);
      } catch (android.system.ErrnoException e) {
          err("Symlinking failed", e);
          throw e;
      }
   }

   void mkDir(String nm) {
      File dir = new File(nm);
      if (!dir.exists())
         dir.mkdir();
   }

   void naviVis() {
      findViewById(R.id.back).setVisibility(
         Back != null || PilView.canGoBack()? View.VISIBLE : View.INVISIBLE );
      findViewById(R.id.fore).setVisibility(
         Fore != null || PilView.canGoForward()? View.VISIBLE : View.INVISIBLE );
      findViewById(R.id.exit).setVisibility(
         Exit != null? View.VISIBLE : View.INVISIBLE );
      Navi = true;
   }

   public void goBack(View view) {
      if (Back != null)
         PilView.loadUrl(Back);
      else if (PilView.canGoBack()) {
         PilView.goBack();
         Back = Fore = null;
      }
   }

   public void goFore(View view) {
      if (Fore != null)
         PilView.loadUrl(Fore);
      else if (PilView.canGoForward()) {
         PilView.goForward();
         Back = Fore = null;
      }
   }

   public void goExit(View view) {
      if (Exit != null) {
         Ex = Url;
         PilView.loadUrl(Exit);
      }
   }

   public void restart() {
      if (SRV != null) {
         SRV.stopProc();
         SRV = null;
      }
      PicoLisp.Loaded = false;
      Intent intent = new Intent(this, PicoLisp.class);
      startForegroundService(intent);
   }
}
