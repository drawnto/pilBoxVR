// 23jul24abu
// (c) Software Lab. Alexander Burger

package de.software_lab.pilbox;

import java.io.*;
import java.util.*;
import java.math.*;
import java.lang.reflect.*;
import android.os.Looper;

// java Reflector
public class Reflector extends Thread {
   PicoLisp Context;
   String InJava, OutLisp, OutRqst, InRply;
   Thread Main;
   InOut Io, Rpc;

   Reflector(PicoLisp context, String java, String lisp, String rqst, String rply) {
      Context = context;
      InJava = java;
      OutLisp = lisp;
      OutRqst = rqst;
      InRply = rply;
      Main = this;
   }

   public void run() {
      Looper.prepare();  // For InvocationHandler
      try {
         Io = new InOut(Context, new FileInputStream(InJava), new FileOutputStream(OutLisp));
         Rpc = new InOut(Context, new FileInputStream(InRply), new FileOutputStream(OutRqst));
         try {
            for (;;)
               reflect((Object[])Io.read());
         }
         catch (EOFException e) {}
         Io.close();
         Rpc.close();
      }
      catch (IOException e) {
         PicoLisp.err("InOut", e);
      }
   }

   // (java "cls" 'T ['any ..]) -> obj         New object
   // (java 'obj ['n] 'msg ['any ..]) -> any   Send message to object
   // (java 'obj "fld" ['any]) -> any          Value of object field
   // (java "cls" ['n] 'msg ['any ..]) -> any  Call method in class
   // (java "cls" "fld" ['any]) -> any         Value of class field
   // (java T "cls" ["cls" ..]) -> obj         Define interface
   // (java 'obj) -> [lst ..]                  Reflect object
   // (java "cls") -> cls                      Get class
   // (java NIL 'obj) -> NIL                   Release reference to object
   void reflect(Object lst[]) {
      int i;
      Object x, y, z;

      try {
         if (lst == null) {
            Io.Out.write(InOut.NIX);
            Io.Out.write(InOut.NIX);
            PicoLisp.log("reflect null");
         }
         else {
            y = lst[0];
            if (lst.length == 1) {                       // Reflect object or class
               if (y instanceof String) {
                  x = Class.forName((String)y);
                  Io.Out.write(InOut.NIX);
                  Io.print(x);
               }
               else {
                  Class cls = y instanceof Class? ((Class)y).getSuperclass() : y.getClass();
                  if (cls == null) {
                     Io.Out.write(InOut.NIX);
                     Io.Out.write(InOut.NIX);
                  }
                  else {
                     Field[] fld = cls.getDeclaredFields();
                     Io.Out.write(InOut.NIX);
                     Io.Out.write(InOut.BEG);
                     Io.Out.write(InOut.BEG);
                     Io.print(cls);
                     Io.Out.write(InOut.DOT);
                     Io.print(cls.getName());
                     if (y instanceof Class) {
                        Class[] cl = ((Class)y).getDeclaredClasses();
                        if (cl.length == 0)
                           Io.Out.write(InOut.NIX);
                        else {
                           Io.Out.write(InOut.BEG);
                           for (i = 0; i < cl.length; ++i) {
                              Io.Out.write(InOut.BEG);
                              Io.print(cl[i]);
                              Io.Out.write(InOut.DOT);
                              Io.print(cl[i].getName());
                           }
                           Io.Out.write(InOut.END);
                        }
                     }
                     for (i = 0; i < fld.length; ++i) {
                        if (!(y instanceof Class)) {
                           Io.Out.write(InOut.BEG);
                           fld[i].setAccessible(true);  // -> fld[i].trySetAccessible();
                           Io.print(fld[i].get(y));
                           Io.Out.write(InOut.DOT);
                        }
                        Io.prSym(fld[i].getName());
                     }
                     Io.Out.write(InOut.END);
                  }
               }
            }
            else {
               if (y == InOut.Nil) {                     // Release reference to object
                  InOut.Obj.remove(lst[1].hashCode());
                  x = null;
               }
               else if (y == InOut.T) {                  // Define interface
                  Class[] c = new Class[lst.length - 1];
                  for (i = 0; i < c.length; ++i)
                     c[i] = Class.forName((lst[i+1]).toString());
                  InvocationHandler h = new InvocationHandler() {
                     public Object invoke(Object x, Method m, Object[] lst) {
                        String nm = m.getName();
                        switch (nm) {
                        case "equals":
                           return x.equals(lst[0]);
                        case "hashCode":
                           return System.identityHashCode(x);
                        case "toString":
                           return x.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(x));
                        default:
                           try {
                              if (Thread.currentThread() == Main) {
                                 Io.prSym(nm);
                                 Io.print(x);
                                 Io.print(lst);
                                 Io.flush();
                                 return Io.read();
                              }
                              else if (Rpc.Out == null)
                                 PicoLisp.log("RPC closed");
                              else {
                                 synchronized (Rpc) {
                                    Rpc.prSym(nm);
                                    Rpc.print(x);
                                    Rpc.print(lst);
                                    Rpc.flush();
                                    return Rpc.read();
                                 }
                              }
                           }
                           catch (Exception e) {
                              PicoLisp.err("Reflector interface", e);
                           }
                           return null;
                        }
                     }
                  };
                  x = Proxy.newProxyInstance(c[0].getClassLoader(), c, h);
               }
               else if ((z = lst[1]) instanceof String) {
                  if (y instanceof String) {             // Value of class field
                     Class cls = Class.forName((String)y);
                     z = cls.getField(z.toString());
                     if (lst.length <= 2)
                        x = ((Field)z).get(cls);
                     else
                        ((Field)z).set(cls, x = lst[2]);
                  }
                  else                                   // Value of object field
                     z = y.getClass().getField(z.toString());
                     if (lst.length <= 2)
                        x = ((Field)z).get(y);
                     else
                        ((Field)z).set(y, x = lst[2]);
               }
               else {
                  int ofs = 2;
                  i = lst.length-2;
                  final Integer cb;
                  if (!(z instanceof Integer))
                     cb = null;
                  else {
                     cb = (Integer)z;
                     z = lst[2];
                     ++ofs;
                     --i;
                  }
                  Object[] arg = new Object[i];
                  Class[] par = new Class[i];
                  while (--i >= 0) {
                     Object v = lst[i+ofs];
                     if (v == InOut.T || v == InOut.Nil) {
                        arg[i] = v == InOut.T;
                        par[i] = Boolean.TYPE;
                     }
                     else if ((arg[i] = v) == null)
                        par[i] = null;
                     else {
                        if (v instanceof Byte)
                           par[i] = Byte.TYPE;
                        else if (v instanceof Character)
                           par[i] = Character.TYPE;
                        else if (v instanceof Short)
                           par[i] = Short.TYPE;
                        else if (v instanceof Integer)
                           par[i] = Integer.TYPE;
                        else if (v instanceof Long)
                           par[i] = Long.TYPE;
                        else if (v instanceof Float)
                           par[i] = Float.TYPE;
                        else if (v instanceof Double)
                           par[i] = Double.TYPE;
                        else
                           par[i] = v.getClass();
                     }
                  }
                  if (z == InOut.T)                      // New object
                     x = javaConstructor(Class.forName(y.toString()), par).newInstance(arg);
                  else {
                     if (cb != null) {
                        final Object y2 = y;
                        final Object z2 = z;
                        final Object[] arg2 = arg;
                        final Class[] par2 = par;
                        x = new Thread() {
                           public void run() {
                              try {
                                 Object val;
                                 do {
                                    if (y2 instanceof String)
                                       val = javaMethod(Class.forName((String)y2), z2.toString(), par2).invoke(null, arg2);
                                    else
                                       val = javaMethod(y2.getClass(), z2.toString(), par2).invoke(y2, arg2);
                                    if (cb.intValue() != 0) {
                                       synchronized (Rpc) {
                                          Rpc.print(z2);
                                          Rpc.print(cb);
                                          Rpc.Out.write(InOut.BEG);
                                          Rpc.print(val);
                                          Rpc.print(y2);
                                          Rpc.Out.write(InOut.END);
                                          Rpc.flush();
                                          if (cb.intValue() > 0)
                                             Rpc.read();
                                       }
                                    }
                                 } while (cb.intValue() < 0  &&  Rpc.read() != InOut.Nil  &&  !Thread.currentThread().isInterrupted());
                              }
                              catch (Throwable err) {
                                 String s = err.toString();
                                 while ((err = err.getCause()) != null)
                                    s += " / " + err.toString();
                                 PicoLisp.log(s);
                                 try {
                                    Rpc.print(cb);
                                    Rpc.print(z2);
                                    Rpc.Out.write(InOut.BEG);
                                    Rpc.print(s);
                                    Rpc.print(y2);
                                    Rpc.Out.write(InOut.END);
                                    Rpc.flush();
                                 }
                                 catch (IOException e) {
                                    PicoLisp.err("Reflector thread", e);
                                 }
                              }
                           }
                        };
                     }
                     else {
                        Method m;
                        if (y instanceof String) {       // Call method in class
                           m = javaMethod(Class.forName((String)y), z.toString(), par);
                           x = m.invoke(null, arg);
                        }
                        else {                           // Send message to object
                           m = javaMethod(y.getClass(), z.toString(), par);
                           x = m.invoke(y, arg);
                        }
                        if (m.getReturnType() == Void.TYPE)
                           x = null;
                     }
                  }
               }
               Io.Out.write(InOut.NIX);
               Io.print(x);
            }
         }
         Io.flush();
      }
      catch (Throwable err) {
         String s = err.toString();
         while ((err = err.getCause()) != null)
            s += " / " + err.toString();
         PicoLisp.log(s);
         try {
            Io.print(0);
            Io.print(s);
            Io.flush();
         }
         catch (IOException e) {
            PicoLisp.err("Reflector cause", e);
         }
      }
   }

   final static Constructor javaConstructor(Class cls, Class[] par) throws NoSuchMethodException {
   looking:
      for (Constructor m : cls.getConstructors()) {
         Class<?>[] types = m.getParameterTypes();
         if (types.length == par.length) {
            for (int i = 0; i < types.length; ++i)
               if (par[i] != null  &&  !(types[i].isAssignableFrom(par[i])))
                  continue looking;
            return m;
         }
      }
      throw new NoSuchMethodException();
   }

   final static Method javaMethod(Class cls, String nm, Class[] par)  throws NoSuchMethodException {
   looking:
      for (Method m : cls.getMethods()) {
         if (m.getName().equals(nm)) {
            Class<?>[] types = m.getParameterTypes();
            if (types.length == par.length) {
               for (int i = 0; i < types.length; ++i)
                  if (par[i] != null  &&  !(types[i].isAssignableFrom(par[i])))
                     continue looking;
               return m;
            }
         }
      }
      throw new NoSuchMethodException(nm + "(" + par + ")");
   }
}
