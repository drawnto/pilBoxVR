# 08oct25 Software Lab. Alexander Burger

(load "@lib/net.l" "@lib/misc.l" "@lib/btree.l" "@lib/db.l" "@lib/pilog.l" "@lib/android.l")

(msg *Pid " " (stamp))

# Generic PilBox App
(allowed ()
   "!pb0" "@lib.css" "@lib/phone.css" "!pb.css" )

(scl 6)
(load "@lib/http.l" "@lib/xhtml.l" "@lib/form.l")

(on *AlwaysAsk)

(symbols (setq *PbSymbols '(android pico)))

(conc (last repl)
   (quote
      (gui '(+JS +Button) "Clear Cache" '(clearCache))
      (<nbsp> 3)
      (gui '(+Able +Button) '(not *Dbg) "Debug"
         '(prog
            (on *Dbg)
            (symbols '(pico) (load "@lib/lint.l" "@lib/debug.l" "@lib/too.l")) ) )
      (gui '(+Var +Checkbox) '*Repl)
      (<nbsp> 3)
      (gui '(+Var +RgbPicker) '*Color)
      (gui '(+View +TextField) '*Color) ) )

(de *Locales
   ("English" NIL)
   ("English (US)" "US")
   ("English (UK)" "UK")
   ("Deutsch (DE)" "DE" . "de")
   ("Deutsch (CH)" "CH" . "ch")
   ("Español (ES)" "ES" . "es")
   ("Español (AR)" "AR" . "ar")
   ("Català (ES)" "ES" . "ca")
   ("Français (FR)" "FR" . "fr")
   ("λληνικά (GR)" "GR" . "gr")
   ("Norsk" "NO" . "no")
   ("Русский" "RU" . "ru")
   ("Svenska" "SE" . "sv")
   ("کوردیی ناوەندی" "CKB" . "ckb")
   ("日本語" "JP" . "jp") )

(de needs (V)
   (or
      (>= `(in "Version" (line T)) V)
      (let *Reduced T
         (menu "Version"
            (<h2> "center red" "Needs PilBox >= " V) )
         NIL ) ) )

(de readLocale ()
   (unless (info "Locale")
      (let Loc (java "java.util.Locale" 'getDefault)
         (out "Locale"
            (println
               (cons (java Loc 'getCountry) (java Loc 'getLanguage)) ) ) ) )
   (prog1
      (in "Locale" (read))
      (apply locale *Locs (car @) (cdr @)) ) )

(de pilName ()
   (if (rassoc *Pil *Pils)
      (eval (car @))
      "PicoLisp App" ) )

(de status (Msg)
   (notify 1 (pilName) Msg) )

(de hasFile (F)
   (and *PbPath (info (pack @ "/" F)) @@) )

(de menu (Ttl . Prg)
   (cond
      ((= *Url "!pbHome")
         (java (; CONTEXT GUI Config) 'setTextZoom 100)
         (off *PbPath *PbCss) )
      ((file)
         (setq *PbPath
            (flip (stem (flip (chop (car @))) "/")) ) ) )
   (setq *Portrait
      (=1  # ORIENTATION_PORTRAIT
         (; (java (java CONTEXT 'getResources) 'getConfiguration) orientation) ) )
   (action
      (html 0 Ttl
         (if (hasFile "lib.css")
            (cons "@lib.css" "@lib/phone.css" @ *PbCss)
            '("@lib.css" "@lib/phone.css" "!pb.css") )
         NIL
         (<layout> T
            ((0 1))
            ((4 12) NIL
               ((12 12 "display: table-cell; text-align: center")
                  (<div> @
                     (<img>
                        (or (hasFile "logo.png") "@img/logo.png")
                        "Logo"
                        (cond
                           ((= "!pbHome" *Url) "!pbRepl")
                           ((pre? "!pb" *Url) "!pbHome")
                           ((= "App.l" (cadr (file))) "!pbHome")
                           (*Pil (pack @ "App.l"))
                           (T "!pbHome") )
                        "100%" ) )
                  ((68 12 "center")
                     (<div> @
                        (run *PbHeader) )
                     ((12 12 "display: table-cell; text-align: center")
                        (<div> @
                           (unless *Reduced
                              (cond
                                 ((member *Url '("!pbHome" "!pbRepl"))
                                    (<img> "@img/settings.png" ,"Settings" "!pbSettings?*Tab=+1" "100%") )
                                 ((hasFile "settings.l")
                                    (<img> "@img/settings.png" ,"Settings" (pack @ "?*Tab=+1") "100%") ) ) ) ) ) ) ) )
            ((0 0 'main)
               (prog
                  (<div> @
                     (run Prg 1) )
                  (and *Repl (<> *Url "!pbRepl")
                     (<layout> T
                        ((200) NIL
                           ((0)
                              (<div> @
                                 (<style> "repl"
                                    (use *Top (repl)) ) ) ) ) ) ) ) ) ) ) ) )

(de pb.css ()
   (httpHead "text/css" 86400 "!pb.css")
   (ht:Out *Chunked
      (prinl "body {")
      (and
         (info "lib/bg.css")
         (in @@ (line T))
         (prinl "   background-color: " @ ";") )
      (and
         (info "lib/tc.css")
         (in @@ (line T))
         (prinl "   color: " @ ";") )
      (and
         (info "lib/fs.css")
         (in @@ (line T))
         (prinl "   font-size: " @ ";") )
      (prinl "}")
      (and
         (info "lib/lc.css")
         (in @@ (line T))
         (prinl "li a {color: " @ "}") ) ) )

(de pbRepl ()
   (menu "PicoLisp REPL"
      (<h3> "fh" "PicoLisp REPL")
      (<style> "repl" (repl)) ) )

(de pbSettings ()
   (menu ,"Settings"
      (form NIL
         (<h3> "fh" ,"Settings")
         (<h4> "fh" "PilBox Version " (in "Version" (echo)))
         (<tab>
            ("PILs"  # PlugIn-Lisp Apps
               (gui '(+Hint1 +TextField) '(: home pils data) 20)
               (gui '(+Button) "Download"
                  '(let? S (fold (val> (field -1)))
                     (ifn
                        (download
                           (or
                              (and (info "lib/repoHost") (in @@ (line T)))
                              "software-lab.de" )
                           (pack
                              (let? L (and (info "lib/repoPath") (in @@ (line)))
                                 (unless (= "/" (last L))
                                    (conc L '("/")) )
                                 L )
                              S
                              ".zip" )
                           "pil.zip" )
                        (error ,"Data not found")
                        (java CONTEXT 'install S "pil.zip")
                        (%@ "unlink" NIL "pil.zip")
                        (clearCache)
                        (restart) ) ) )
               (<hr>)
               (gui 'pils '(+Upd +Chart1)
                  '(sort
                     (extract
                        '((F)
                           (when (pre? "PIL-" F)
                              (pack (cddddr (chop F))) ) )
                        (dir) ) )
                  2 )
               (<table> 'chart NIL '(NIL (btn))
                  (do 16
                     (<row> (alternating)
                        (gui 1 '(+Url +TextField)
                           '((F) (pack "!pbList?PIL-" F)) )
                        (gui 2 '(+DelRowButton)
                           '(let K
                              (make
                                 (for S (: home pils data)
                                    (unless (= S (curr))
                                       (in (pack "PIL-" S)
                                          (while (line T) (link @)) ) ) )
                                 (link "App.l" "Version") )
                              (in (pack "PIL-" (curr))
                                 (while (line T)
                                    (let F @
                                       (unless (member F K)
                                          (%@ "unlink" NIL F)
                                          (while
                                             (and
                                                (setq F (dirname F))
                                                (=0 (%@ "rmdir" 'I F)) ) ) ) ) ) )
                              (%@ "unlink" NIL (pack "PIL-" (curr))) )
                           '(text ,"Delete @1?" (curr)) ) ) ) ) )
            ("Repo"
               (<grid> 2
                  "Host" (gui '(+FileField) "lib/repoHost" 25)
                  "Path" (gui '(+FileField) "lib/repoPath" 25) ) )
            ("CSS"
               (<grid> 2
                  "Background" (gui '(+FileField) "lib/bg.css" 12)
                  "Text Color" (gui '(+FileField) "lib/tc.css" 12)
                  "Link Color" (gui '(+FileField) "lib/lc.css" 12)
                  "Font Size" (gui '(+FileField) "lib/fs.css" 12)
                  NIL NIL
                  NIL (gui '(+Button) "OK" '(clearCache)) ) )
            (,"Language" (localeSettings)) ) ) ) )

(de localeSettings ()
   (<table> NIL NIL NIL
      (<row> NIL
         (gui '(+Init +Chg +Fmt +Radio)
            (readLocale)
            '((X)
               (apply locale *Locs (car X) (cdr X))
               (out "Locale" (println X)) )
            '((X) (car (rassoc X *Locales)))
            '((R) (cdr (assoc R *Locales)))
            NIL
            (caar *Locales) )
         (ht:Prin (caar *Locales)) )
      (for (I . L) (cdr *Locales)
         (<row> NIL
            (gui '(+Radio) (- I) (car L))
            (ht:Prin (car L)) ) ) ) )

(de pbList (File)
   (menu File
      (<h3> "fh" File)
      (<ol> NIL
         (in File
            (while (line T)
               (<li> NIL (<href> @ (pack "!pbCat?" @))) ) ) ) ) )

(de pbCat (File)
   (menu File
      (<h2> "fh" File)
      (<pre> "tiny"
         (in File
            (until (eof)
               (ht:Prin (line T) "\n") ) )
         (do 3 (prinl)) ) ) )

(de start Prg
   (let Path (car (file))
      (unless (= Path *Pil)
         (when (assoc *Pil *Stop)
            (symbols (cadr @)
               (run (cddr @)) )
            (off *Pil)
            (status) )
         (off *Lock)
         (setq *Pil Path)
         (and
            (info "Locale")
            (info (pack Path "/loc/"))
            (in "Locale" (read))
            (locale (car @) (cdr @) (pack Path "/loc/")) )
         (clearHistory)
         (status)
         (run Prg) ) )
   (setq *PbSymbols (symbols)) )

(de stop Prg
   (when Prg
      (push 'Prg *PbSymbols)
      (let Path (car (file))
         (if (assoc Path *Stop)
            (con @ Prg)
            (push '*Stop (cons Path Prg)) ) ) ) )

(de pilbox Head
   (setq *PbHeader Head) )

(de pbHome (Uuid)
   (readLocale)
   (setq *PbHeader
      (quote
         (<h3> "fh" "PilBox")
         (<h4> "fh" (ht:Prin (pilName))) ) )
   (let
      (Old (and (info "PILs") (in @@ (read)))
         New
         (make
            (for D (dir)
               (when (info (pack D "/App.l"))
                  (link (pack D "/"))
                  (when
                     (and
                        (<> (pack D "/") *Pil)
                        (info (pack D "/pil.zip")) )
                     (java CONTEXT 'install D @@)
                     (%@ "unlink" NIL @@) ) ) ) )
         L (conc (diff New Old) (sect Old New)) )
      (let? N (num? Uuid)
         (unless (=1 N)
            (xchg (nth L (dec N)) (nth L N)) )
         (off Uuid) )
      (unless (= L Old)
         (out "PILs" (println L)) )
      (setq *Pils
         (mapcar
            '((D)
               (cons (in (pack D "/App.l") (read)) D) )
            L ) ) )
   (cond
      (*Alarm
         (loadUrl (baseHRef) *SesId *Alarm)
         (off *Alarm) )
      ((and Uuid *Pils (not (cdr *Pils)))
         (loadUrl (baseHRef) *SesId (pack (cdar *Pils) "App.l")) )
      (T
         (overview NIL)
         (java CONTEXT 'exit null)
         (menu "PilBox"
            (apply <buttons>
               (let N 0
                  (mapcar
                     '(((Nm . D))
                        (cons
                           Nm
                           (pack D "App.l")
                           (if (info (pack D "logo.png")) @@ "@img/logo.png")
                           (inc 'N) ) )
                     *Pils ) )
               12 8 60 10 ) ) ) ) )

(private) (image button *BtnArg)

(de <buttons> (X W DX DY . @)
   (or *Portrait (inc 'DY DY))
   (<layout> T
      ((X NIL)
         (<div> @)
         ((W DY) (image) ((DX DY) (button)))
         ((W DY) (image) ((DX DY) (button)))
         ((W DY) (image) ((DX DY) (button)))
         ((W DY) (image) ((DX DY) (button)))
         ((W DY) (image) ((DX DY) (button)))
         ((W DY) (image) ((DX DY) (button)))
         ((W DY) (image) ((DX DY) (button)))
         ((W DY) (image) ((DX DY) (button)))
         ((W DY) (image) ((DX DY) (button)))
         ((W DY) (image) ((DX DY) (button)))
         ((W DY) (image) ((DX DY) (button)))
         ((W DY) (image) ((DX DY) (button)))
         ((W DY) (image) ((DX DY) (button)))
         ((W DY) (image) ((DX DY) (button)))
         ((W DY) (image) ((DX DY) (button)))
         ((W DY) (image) ((DX DY) (button))) ) ) )

(de image ()
   (<div> @
      (loop
         (T (setq *BtnArg (next))
            (when (caddr *BtnArg)
               (<img> @
                  (car *BtnArg)
                  (if (cdddr *BtnArg)
                     (pack *Url "?+" @)
                     (cadr *BtnArg) )
                  "100%" ) ) )
         (NIL (args)) ) ) )

(de button ()
   (<div> @
      (when *BtnArg
         (<button> (cadr @) (eval (car @))) ) ) )

# Error handler
(de *Err
   (println '==== *Pid (symbols))
   (for ("L" (trail T)  "L")
      (cond
         ((pair (car "L"))
            (let "E" (++ "L")
               (println
                  (if (getd (box? (car "E")))
                     (cons @ (cdr "E"))
                     "E" ) ) ) )
         ((== '"L" (car "L"))
            (setq "L" (cddr "L")) )
         (T
            (space 3)
            (println (++ "L") (++ "L")) ) ) )
   (println '====)
   (flush)
   (wait 420)
   (loadTxt
      "<style type=\"text/css\">body{color:white;background-color:red}</style>"
      "<h1>☹</h1><pre>" (sym ^) "</pre>"
      "<pre>" *Msg "</pre><br/>"
      "<textarea wrap=\"off\" cols=\"40\" rows=\"30\">"
      (in "log" (till NIL T))
      "</textarea>" ) )

# RPC handler
(de rpc (Src Arg Url)
   (and Src (script Src Arg))
   (if *ForeGround
      (and Url (loadUrl (baseHRef) *SesId Url))
      (setq *Alarm Url) ) )

# Downloads
(local) (VISIBILITY_VISIBLE_NOTIFY_COMPLETED onDownloadStart
startDownload parse allowScanningByMediaScanner
setNotificationVisibility setDestinationInExternalPublicDir
guessFileName enqueue setDownloadListener)

(de VISIBILITY_VISIBLE_NOTIFY_COMPLETED . 1)  # DownloadManager.Request

(de startDownload (Url Agent Cont Mime Len)
   (catch '("Exception")
      (let Req
         (java "android.app.DownloadManager$Request" T
            (java "android.net.Uri" 'parse Url) )
         (java Req 'allowScanningByMediaScanner)
         (java Req 'setNotificationVisibility VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
         (java Req 'setDestinationInExternalPublicDir
            (java "android.os.Environment" "DIRECTORY_DOWNLOADS")
            (java "android.webkit.URLUtil" 'guessFileName
               Url
               (or Cont null)
               (or Mime null) ) )
         (java
            (java CONTEXT 'getSystemService "download")  # DownloadManager
            'enqueue
            Req )
         (toast ,"downloading ...") )
      (and @@ (msg *Msg)) ) )

(when
   (make
      (for Dir (dir)
         (let F (pack Dir "/App.l")
            (when (info F)
               (and (info (pack Dir "/css/")) (allow @@ T))
               (and (info (pack Dir "/lib.css")) (allow @@))
               (and (info (pack Dir "/img/")) (allow @@ T))
               (and (info (pack Dir "/logo.png")) (allow @@))
               (in F
                  (read)
                  (while (= "\"" (skip "#"))
                     (let S (read)
                        (if (pre? "@" S)
                           (link S)
                           (push1 '*PbStart S) ) ) ) )
               (when (info (setq F (pack Dir "/loc/")))
                  (push1 '*Locs F) ) ) ) ) )
   (symbols '(pico)
      (mapc load (uniq @)) ) )

# Start application
(de pb0 (Uuid)
   (when (= Uuid *Uuid)
      (when (app)
         (startForeground "PicoLisp App" NIL 1073741824)  # FOREGROUND_SERVICE_TYPE_SPECIAL_USE
         (on *ForeGround)
         (mapc allow
            '("!pbHome" "!pbRepl" "!pbSettings" "!pbList" "!pbCat") )
         (for Dir (dir)
            (when (info (pack Dir "/App.l"))
               (allow (pack Dir "/") T) ) )
         (let Lsn (java T "android.webkit.DownloadListener")
            (def 'onDownloadStart Lsn startDownload)
            (java (; CONTEXT GUI) 'runOnUiThread
               (java (; CONTEXT GUI PilView) 0 'setDownloadListener Lsn) ) ) )
      (loadUrl (baseHRef) *SesId "!pbHome?" Uuid) ) )

(de pico~go ()
   (out "PID" (prinl *Pid))
   (symbols *PbSymbols)
   (daemon 'http (symbols *PbSymbols))
   (finish
      (when (assoc *Pil *Stop)
         (symbols (cadr @)
            (run (cddr @)) ) )
      (or *Err (restart)) )  # PTY: (off *Err)
   (when (info "~/.pty")
      (ctty)
      (task (port T `(inc (format (sys "PORT"))))  # Listen for IPC
         (let? L (udp @)
            (when (= (++ L) (in "~/.pty" (line T)))
               (run L) ) ) ) )
   (off *Timeout)
   (while (info "PID")
      (wait 10) )
   (java1)
   (push '*Run
      '(-1 0
         (++ *Run)
         (out "PID" (prinl *Pid))
         (mapc load *PbStart) ) )
   (server (format (sys "PORT")) "!pb0" T) )

### Debug ###
`(info "~/.pty")

(load "@lib/too.l" "@lib/term.l")

(de _ttyHost Prg
   (let? Sock
      (connect
         (default (NIL)
            (java
               (java (; (wipe CONTEXT) Terminal) 'getInetAddress)
               'getHostAddress ) )
         `(+ (format (sys "PORT")) 2) )
      (finally (close Sock)
         (run Prg 1) ) ) )

(de pbPut @
   (_ttyHost
      (out Sock (pr (in "~/.pty" (line T)) '+ (rest)))
      (out "pty.zip" (in Sock (echo)))
      (java CONTEXT 'install null "pty.zip")
      (%@ "unlink" NIL "pty.zip") ) )

(de pbGet @
   (_ttyHost
      (java CONTEXT 'zip "pty.zip" (rest))
      (out Sock
         (pr (in "~/.pty" (line T)) '-)
         (in "pty.zip" (echo)) )
      (%@ "unlink" NIL "pty.zip") ) )

(noLint 'pbHome "fh")
(mapc noLint '(pbPut pbGet) 'Sock)

# Vip
(symbols '(vip android pico))

(cmd "get" (L Lst Cnt)
   (pbGet (: buffer file)) )
