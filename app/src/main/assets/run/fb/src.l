# 06jan26 Software Lab. Alexander Burger

(symbols '(llvm))

(begin "src" T
   "../src/vers.l" "../src/defs.l" "../src/glob.l" "../src/dec.l" )

# External declarations
(de T NIL stkErr (any))
(de T NIL varErr (any any))
(de T NIL protErr (any any))
(de T i64 evCnt (any any))
(de T i64 xCnt64 (any any))
(de T evSym (any))
(de T xName (any))
(de T boxNum (any))

(de T i8* ssfnInit (i8* i32 i32))
(de T i8* ssfnCtx ())
(de T i8* ssfnFont (i8* i8*))
(de T i8* ssfnSelect (i8* i32))
(de T void ssfnColor(i8* i32 i32))
(de T i64 ssfnRender (i8* i8* i32 i32 i8*))
(de T void ssfnFree (i8*))

# Globals
(var $DX i64 0)
(var $DY i64 0)
(var $Color i32 0)  # RGBA Little endian

###### SSFN ######
# (fb:Draw var 'ptr 'dx 'dy . prg) -> any
(de Draw (Exe)
   (let
      (X (cdr Exe)
         Y (needChkVar Exe (++ X))
         Buf
         (ssfnInit
            (i8* (xCnt64 Exe (eval (++ X))))
            (set $DX (i32 (evCnt Exe X)))
            (set $DY (i32 (evCnt Exe (cdr X)))) )
         P (set $Bind (push (val Y) Y (val $Bind))) )  # [[sym] sym LINK]
      (set Y (box64 (i64 Buf)))
      (stkChk Exe)
      (prog1
         (run X)
         (free Buf)
         (set Y (val P)  $Bind (val 3 P)) ) ) )

# (fb:Ctx) -> ctx
(de Ctx (Exe)
   (box64 (i64 (ssfnCtx))) )

# (fb:Font 'ctx 'font 'size) -> str
(de Font (Exe)
   (let
      (X (cdr Exe)
         Ctx (i8* (xCnt64 Exe (eval (++ X)))) )
      (cond
         ((ssfnFont Ctx (i8* (xCnt64 Exe (eval (++ X)))))
            (mkStr @) )
         ((ssfnSelect Ctx (i32 (evCnt Exe X)))
            (mkStr @) )
         (T $Nil) ) ) )

# (fb:Color 'buf 'fg ['bg]) -> T
(de Color (Exe)
   (let X (cdr Exe)
      (ssfnColor
         (i8* (xCnt64 Exe (eval (++ X))))
         (set $Color (i32 (evCnt Exe X)))
         (i32
            (if (pair (shift X))
               (evCnt Exe X)
               0 ) ) )
      $T ) )

# (fb:Render 'ctx 'buf 'x 'y 'str) -> T
(de Render (Exe)
   (let X (cdr Exe)
      (ssfnRender
         (i8* (xCnt64 Exe (eval (++ X))))
         (i8* (xCnt64 Exe (eval (++ X))))
         (i32 (evCnt Exe X))
         (i32 (evCnt Exe (shift X)))
         (let Nm (xName (evSym (shift X)))
            (bufString Nm (b8 (bufSize Nm))) ) )
      $T ) )

# (fb:Done 'ctx) -> NIL
(de Done (Exe)
   (let X (cdr Exe)
      (ssfnFree
         (i8* (xCnt64 Exe (eval (++ X)))) )
      $Nil ) )

###### Drawwng ######
# (fb:Dot 'ptr 'x 'y ['col]) -> flg
(de Dot (Exe)
   (let
      (X (cdr Exe)
         Ptr (i32* (xCnt64 Exe (eval (++ X))))
         PX (evCnt Exe X)
         PY (evCnt Exe (shift X))
         Col
         (if (pair (shift X))
            (i32 (evCnt Exe X))
            (val $Color) ) )
      (if
         (or
            (lt0 PX)
            (lt0 PY)
            (>= PX (val $DX))
            (>= PY (val $DY)) )
         $Nil
         (set
            (ofs Ptr (+ PX (mul PY (val $DX))))
            Col )
         $T ) ) )

(de void fillRect ((i32* . Ptr) (i64 . X) (i64 . Y) (i64 . DX) (i64 . DY) (i32 . Col))
   (setq Ptr
      (ofs Ptr (+ X (mul Y (val $DX)))) )
   (let (C DX  D (- (val $DX) DX))
      (loop
         (set Ptr Col)
         (if (ge0 (dec 'DX))
            (inc 'Ptr)
            (? (le0 (dec 'DY)))
            (inc 'Ptr D)
            (setq DX C) ) ) ) )

(de void fillQuadrant ((i32* . Ptr) (i64 . X) (i64 . Y) (i64 . R) (i64 . Q) (i32 . Col))
   (let
      (Ri2 (* (dec R) (dec R))
         Ro2 (* R R)
         DY R )
      (loop
         (let DX R
            (loop
               (let
                  (Di2 (+ (* DX DX) (* DY DY))
                     PX X
                     PY Y )
                  (case Q
                     (1 (dec 'PX DX) (dec 'PY DY))
                     (2 (inc 'PX DX) (dec 'PY DY))
                     (3 (inc 'PX DX) (inc 'PY DY))
                     (4 (dec 'PX DX) (inc 'PY DY)) )
                  (cond
                     ((> Ri2 Di2)
                        (set
                           (ofs Ptr (+ PX (mul PY (val $DX))))
                           Col ) )
                     ((> Ro2 Di2)
                        (let A (i32 (/ (* 255 (- Ro2 Di2)) (- Ro2 Ri2)))
                           (set
                              (ofs Ptr (+ PX (mul PY (val $DX))))
                              (|
                                 (shl (/ (* A (shr Col 24)) 256) 24)
                                 (|
                                    (shl
                                       (/ (* A (& (shr Col 16) 255)) 256)
                                       16 )
                                    (|
                                       (shl
                                          (/ (* A (& (shr Col 8) 255)) 256)
                                          8 )
                                       (/ (* A (& Col 255)) 256) ) ) ) ) ) ) ) )
               (? (lt0 DX))
               (dec 'DX) ) )
         (? (lt0 DY))
         (dec 'DY) ) ) )

# (fb:Rect 'ptr 'x 'y 'dx 'dy 'r ['col]) -> T
(de Rect (Exe)
   (let
      (Z (cdr Exe)
         Ptr (i32* (xCnt64 Exe (eval (++ Z))))
         X (evCnt Exe Z)
         Y (evCnt Exe (shift Z))
         DX (evCnt Exe (shift Z))
         DY (evCnt Exe (shift Z))
         R (if (pair (shift Z)) (evCnt Exe Z) 0)
         Col
         (if (pair (shift Z))
            (i32 (evCnt Exe Z))
            (val $Color) ) )
      (if (=0 R)
         (fillRect Ptr X Y DX DY Col)
         (fillRect Ptr (+ X R) Y (- DX R R) DY Col)
         (fillRect Ptr X (+ Y R) R (- DY R R) Col)
         (fillRect Ptr (- (+ X DX) R) (+ Y R) R (- DY R R) Col)
         (fillQuadrant Ptr (+ X R -1) (+ Y R -1) R 1 Col)
         (fillQuadrant Ptr (- (+ X DX) R -1) (+ Y R -1) R 2 Col)
         (fillQuadrant Ptr (- (+ X DX) R -1) (- (+ Y DY) R) R 3 Col)
         (fillQuadrant Ptr (+ X R -1) (- (+ Y DY) R) R 4 Col) ) )
   $T )

(end)
