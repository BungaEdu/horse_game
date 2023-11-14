package com.example.horse_game

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Point
import android.media.MediaScannerConnection
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.test.runner.screenshot.ScreenCapture
import androidx.test.runner.screenshot.Screenshot.capture
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.mediation.MediationBannerAd
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private var mInterstitialAd: InterstitialAd? = null
    private var unloadedAd = true

    private var bitmap: Bitmap? = null

    private var mHandler: Handler?= null
    private var timeInSeconds = 0L
    private var gaming = true
    private var stringShare = ""
    private var widthBonus = 0

    //Las celdas tienes que estar siempre trakeadas
    private var cellSelectedX = 0
    private var cellSelectedY = 0

    //Nos avisa si tenemos que avanzar de nivel o no, en este
    //caso inicializaremos con false
    private var nextLevel = false


    //Siempre se va a empezar por el nivel 1
    //cada nivel va a tener un número de movimientos
    //en cada nivel vamos a poner que se requieran X movimientos
    //para conseguir el bonus
    private var level = 1
    private var levelMoves = 0
    private var movesRequired = 0
    private var moves = 0
    private var lives = 0
    //Inicializamos a 1 porque nunca va a empezar a cero
    private var scoreLives = 1
    private var scoreLevel = 1

    private var options = 0
    private var bonus = 0


    //Cuando hay bonus tenemos que dejar hacer movimientos adicionales
    private var checkMovement = true

    private var nameColorBlack = "black_cell"
    private var nameColorWhite = "white_cell"

    //Hago un array de arrays para simular el tablero
    private lateinit var board: Array<IntArray>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initScreenGame()
        startGame()
        initAds()

    }

    //Anuncios, está to+do en la web de Admob
    //BANNER
    private fun initAds() {
        MobileAds.initialize(this) {}

        val adView = AdView(this)
        adView.adSize = AdSize.BANNER
        adView.adUnitId = "ca-app-pub-3940256099942544/6300978111"
        // TODO: Add adView to your view hierarchy.

        var lyAdsBanner = findViewById<LinearLayout>(R.id.lyAdsBanner)
        lyAdsBanner.addView(adView)

        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)

    }

    private fun showInterstitial() {
        if (mInterstitialAd != null) {
            unloadedAd = true
            /*mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    // Called when ad is dismissed.
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError?) {
                    // Called when ad fails to show.
                }

                override fun onAdShowedFullScreenContent() {
                    // Called when ad is shown.
                    mInterstitialAd = null
                }
            }*/
            mInterstitialAd?.show(this)
        }
    }

    private fun getReadyAds () {
        var adRequest = AdRequest.Builder().build()
        unloadedAd = false

        InterstitialAd.load(this,"ca-app-pub-3940256099942544/1033173712", adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                mInterstitialAd = null
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                mInterstitialAd = interstitialAd
            }
        })

    }

    ////////////// INICIO JUEGO ///////////////
    private fun initScreenGame() {
        setSizeBoard()
        hideMessage(false)
    }
    private fun setSizeBoard() {
        var iv: ImageView

        //Para coger el tamaño de pantalla
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        val width = size.x

        //Calculo la densidad de mis cuadros del tablero
        var widthDp = (width / resources.displayMetrics.density)
        var lateralMarginsDp = 0
        val widthCell = (widthDp - lateralMarginsDp) / 8
        //Al ser cuadrados pongo el mismo ancho que largo
        val heightCell = widthCell

        widthBonus = widthCell.toInt() * 2

        for (i in 0..7)
            for (j in 0..7) {
                iv = findViewById(resources.getIdentifier("c$i$j", "id", packageName))

                var height = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    heightCell,
                    resources.displayMetrics
                )
                var width = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    widthCell,
                    resources.displayMetrics
                )
            }
    }
    private fun hideMessage(start: Boolean) {
        var lyMessage = findViewById<LinearLayout>(R.id.lyMessage)
        lyMessage.visibility = View.INVISIBLE

        if (start) startGame()
    }

    fun launchAction(v:View) {
        //Vamos a hacer que el mensaje se quite, o salga de nuevo dependiendo de los niveles
        hideMessage(true)

    }

    //Esta función sólo hace una llamada
    fun launchShareGame(v: View) {
        //Lo dejo en una función para que el código sea más reutilizable
        shareGame()
    }

    private fun shareGame() {
        //Vamos a pedir al usuario permisos de lectura y escritura para guardar la foto
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            1
        )
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            1
        )

        //Sacamos la captura de pantalla
        var ssc: ScreenCapture = capture(this)
        bitmap = ssc.bitmap

        //Si hay captura de pantalla, entonces procedemos
        if (bitmap!=null) {
            //Buscamos un nombre para el archivo que no se va a repetir nunca
            //el tiempo de la partida es bueno, porque en una partida sólo
            //va a haber un tiempo
            var idGame = SimpleDateFormat("yyyy/MM/dd").format(Date())
            idGame = idGame.replace(":", "")
            idGame = idGame.replace("/", "")

            //Creamos una ruta de donde se va a guardar la imagen
            val path = saveImage(bitmap!!, "${idGame}.jpg")

            //Como queremos compartir la foto, tenemos que tenerla referenciada
            val bmUri = Uri.parse(path)

            //Para compartir la foto vamos a hacer:
            //Para enviar creamos el intent
            val shareIntent = Intent(Intent.ACTION_SEND)
            //Creamos una nueva tarea, porque cuando hagamos el envío a otra
            //app será una nueva tarea
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            //Indicamos lo que estamos enviando, que referencia a la imagen
            shareIntent.putExtra(Intent.EXTRA_STREAM, bmUri)
            //Con qué texto se va a enviar esa imagen (Si es WA un texto, si es TW otro)
            //Dependiendo de lo que se comparta lo vamos a hacer en el showMessage()
            shareIntent.putExtra(Intent.EXTRA_TEXT, stringShare)
            shareIntent.type = "image/png"

            //Intent donde se le permita al US elegir dónde compartir la captura
            val finalShareIntent = Intent.createChooser(shareIntent, "Select the app you want to share the game to")
            finalShareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            this.startActivity(finalShareIntent)
        }
    }

    private fun saveImage(bitmap: Bitmap?, fileName: String): String? {
        if (bitmap == null) return null

        //Comprobamos el sdk
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            //Decidimos cómo se va a guardar
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/Screenshots"
                )
            }

            val uri = this.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            if (uri != null) {
                this.contentResolver.openOutputStream(uri).use {
                    if (it == null) return@use
                    bitmap.compress(Bitmap.CompressFormat.PNG, 85, it)
                    it.flush()
                    it.close()

                    // add pic to gallery
                    MediaScannerConnection.scanFile(this, arrayOf(uri.toString()), null, null)
                }
            }
            return uri.toString()
        }

        val filePath = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES + "/Screenshots"
        ).absolutePath

        //Toast.maketext(this, filePath, Toast.LENGTH_LONG),show()
        val dir = File(filePath)
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, fileName)
        val fOut = FileOutputStream(file)

        bitmap.compress(Bitmap.CompressFormat.PNG, 85, fOut)
        fOut.flush()
        fOut.close()

        //add pic to gallery
        MediaScannerConnection.scanFile(this, arrayOf(file.toString()), null, null)
        return filePath
    }

    ////////////// CELDAS ///////////////
    fun checkCellClicked(v: View) {
        var name = v.tag.toString()
        var x = name.subSequence(1, 2).toString().toInt()
        var y = name.subSequence(2, 3).toString().toInt()

        checkCell(x, y)
    }
    private fun checkCell(x: Int, y: Int) {
        var checkTrue = true
        if (checkMovement) {
            var difX = x - cellSelectedX
            var difY = y - cellSelectedY

            checkTrue = false

            if (difX == 1 && difY == 2) checkTrue = true // right - top long
            if (difX == 1 && difY == -2) checkTrue = true // right - bottom long
            if (difX == 2 && difY == 1) checkTrue = true // right long - top
            if (difX == 2 && difY == -1) checkTrue = true // right long - bottom
            if (difX == -1 && difY == 2) checkTrue = true // left - top long
            if (difX == -1 && difY == -2) checkTrue = true // left - bottom long
            if (difX == -2 && difY == 1) checkTrue = true // left long - top
            if (difX == -2 && difY == -1) checkTrue = true // left long - bottom
        } else {
            if (board[x][y] != 1) {
                bonus--
                var tvBonusData = findViewById<TextView>(R.id.tvBonusData)
                tvBonusData.text = " + $bonus"

            }
        }

        //Si ya hay otro caballo ahí
        if (board[x][y] == 1) checkTrue = false

        if (checkTrue) selectCell(x, y)
    }
    private fun selectCell(x: Int, y: Int) {
        moves--

        var tvMovesData = findViewById<TextView>(R.id.tvMovesData)
        tvMovesData.text = moves.toString()

        growProgressBonus()

        //Una vez hacemos el movimiento, sumamos los bonus:
        if (board[x][y] == 2) {
            bonus++
            var tvBonus = findViewById<TextView>(R.id.tvBonusData)
            tvBonus.text = " + $bonus"
        }

        board[x][y] = 1
        paintHorseCell(cellSelectedX, cellSelectedY, "previus_cell")

        cellSelectedX = x
        cellSelectedY = y

        clearOptions()
        paintHorseCell(x, y, "selected_cell")
        checkMovement = true

        checkOptions(x, y)

        if (moves > 0) {
            checkNewBonus()
            checkGameOver(x, y)
        } else showMessage("You win!", "Next Level!", false)
    }

    private fun resetBoard() {
        //0 está libre
        //1 está el caballo
        //2 es un bonus
        //9 es una opción del movimiento actual
        board = arrayOf(
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
        )
    }
    private fun clearBoard() {
        var iv: ImageView
        var colorBlack = ContextCompat.getColor(
            this,
            resources.getIdentifier(nameColorBlack, "color", packageName)
        )

        var colorWhite = ContextCompat.getColor(
            this,
            resources.getIdentifier(nameColorWhite, "color", packageName)
        )

        for (i in 0..7) {
            for (j in 0..7) {
                iv = findViewById(resources.getIdentifier("c$i$j", "id", packageName))
                iv.setImageResource(0)

                if (checkColorCell(i, j) == "black") iv.setBackgroundColor(colorBlack)
                else iv.setBackgroundColor(colorWhite)
            }
        }
    }
    private fun setFirstPosition() {
        var coordX = 0
        var coordY = 0

        var firstPosition = false

        while (firstPosition == false) {
            coordX = (0..7).random()
            coordY = (0..7).random()
            if (board[coordX][coordY]==0) firstPosition = true
            checkOptions(coordX, coordY)
            if (options==0) firstPosition = false
        }



        //Estas dos var son generales
        cellSelectedX = coordX
        cellSelectedY = coordY
        selectCell(coordX, coordY)
    }

    private fun setLevel() {
        if (nextLevel) {
            //Cuando subo el nivel, sumo las vidas
            level++
            setLives()
        }
        else {
            lives--
            if (lives<1) {
                level=1
                lives=1
            }
        }
    }

    private fun setLives() {
        when (level) {
            1-> lives = 1
            2-> lives = 2
            3-> lives = 3
            4-> lives = 4
            5-> lives = 5
        }
    }

    private fun setLevelParameters() {
        var tvLiveData = findViewById<TextView>(R.id.tvLiveData)
        tvLiveData.text = lives.toString()

        scoreLives = lives

        var tvLevelNumber = findViewById<TextView>(R.id.tvLevelNumber)
        tvLevelNumber.text = level.toString()
        scoreLevel = level

        bonus = 0
        var tvBonusData = findViewById<TextView>(R.id.tvBonusData)
        tvBonusData.text = ""

        setLevelMoves()
        moves = levelMoves

        movesRequired = setMovesRequired()
    }
    private fun setLevelMoves() {
        when (level) {
            1 -> levelMoves = 64
            2 -> levelMoves = 56
            3 -> levelMoves = 32
            4 -> levelMoves = 16
            5 -> levelMoves = 48
            6 -> levelMoves = 36
            7 -> levelMoves = 48
            8 -> levelMoves = 49
            9 -> levelMoves = 59
            10 -> levelMoves = 48
            11 -> levelMoves = 64
            12 -> levelMoves = 48
            13 -> levelMoves = 48
        }
    }
    private fun setMovesRequired(): Int {
        var movesRequired = 0

        when (level) {
            1 -> movesRequired = 8
            2 -> movesRequired = 10
            3 -> movesRequired = 12
            4 -> movesRequired = 10
            5 -> movesRequired = 10
            6 -> movesRequired = 12
            7 -> movesRequired = 5
            8 -> movesRequired = 7
            9 -> movesRequired = 9
            10 -> movesRequired = 8
            11 -> movesRequired = 1000
            12 -> movesRequired = 5
            13 -> movesRequired = 5
        }

        return movesRequired
    }

    private fun setBoardLevel () {
        when (level) {
            2 -> paintLevel_2()
            3 -> paintLevel_3()
            4 -> paintLevel_4()
            5 -> paintLevel_5()
            /*
            * TODO hacer más niveles XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
            6 -> paintLevel_6()
            7 -> paintLevel_7()
            8 -> paintLevel_8()
            9 -> paintLevel_9()
            10 -> paintLevel_10()
            11 -> paintLevel_11()
            12 -> paintLevel_12()
            13 -> paintLevel_13()
             */
        }
    }


    /////////////// UTILS para los tableros/////////////////////////////////////////
    private fun paintColumn(column:Int){
        for (i in 0..7) {
            board[column][i] = 1
            paintHorseCell(column, i, "previus_cell")

        }
    }
    /*
    * TODO hacer mas funciones XXXXXXXXXXXXXXXXXXX
    * TODO private fun paintRow(row:Int) {}
    * TODO private fun paintDiagonal(diagonal:Int) {}
    * TODO private fun paintDiagonalInverse(diagonal:Int) {}
    * TODO private fun paintRow(row:Int) {}
     */
    /////////////// NIVELES TABLEROS ///////////////////////////////////////////////
    //Pinta la columna 6
    private fun paintLevel_2(){
        paintColumn(6)
    }
    //Pinta un cuadrado
    private fun paintLevel_3(){
        paintColumn(4)
        paintColumn(5)
        paintColumn(6)
        paintColumn(7)
    }
    private fun paintLevel_4() {
        paintLevel_3()
        paintLevel_5()
    }
    private fun paintLevel_5(){
        for (i in 0..7) {
            for (j in 4..7) {
                board[i][j] = 1
                paintHorseCell(j, i, "previus_cell")
            }
        }
    }
    /*
    * TODO private fun paintLevel_6(){}
    * TODO private fun paintLevel_7(){}
    * TODO private fun paintLevel_8(){}
    * TODO private fun paintLevel_9(){}
    * TODO private fun paintLevel_10(){}
    * TODO private fun paintLevel_11(){}
    * TODO private fun paintLevel_12(){}
    * TODO private fun paintLevel_13(){}
     */

    ////////////// BONUS ///////////////
    private fun checkNewBonus() {
        if (moves % movesRequired == 0) {
            var bonusCellX = 0
            var bonusCellY = 0

            var bonusCell = false
            while (bonusCell == false) {
                bonusCellX = (0..7).random()
                bonusCellY = (0..7).random()
                if (board[bonusCellX][bonusCellY] == 0) bonusCell = true
            }
            board[bonusCellX][bonusCellY] = 2
            paintBonusCell(bonusCellX, bonusCellY)
        }
    }
    private fun paintBonusCell(x: Int, y: Int) {
        var iv: ImageView = findViewById(resources.getIdentifier("c$x$y", "id", packageName))
        iv.setImageResource(R.drawable.bonus)
    }
    private fun growProgressBonus() {
        var movesDone = levelMoves - moves
        var bonusDone = movesDone / movesRequired
        var movesRest = movesRequired * (bonusDone)
        var bonusGrow = movesDone - movesRest

        var v = findViewById<View>(R.id.vNewBonus)
        var widthBonus = ((widthBonus / movesRequired) * bonusGrow).toFloat()

        var height =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f, resources.displayMetrics)
                .toInt()
        var width = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            widthBonus,
            resources.displayMetrics
        ).toInt()
        v.layoutParams = TableRow.LayoutParams(width, height)
    }

    ////////////// OPTIONS ///////////////
    private fun clearOptions() {
        for (i in 0..7) {
            for (j in 0..7) {
                if (board[i][j] == 9 || board[i][j] == 2) {
                    if (board[i][j] == 9) board[i][j] = 0
                }
                clearOption(i, j)
            }
        }
    }
    private fun clearOption(x: Int, y: Int) {
        var iv: ImageView = findViewById(resources.getIdentifier("c$x$y", "id", packageName))
        if (checkColorCell(x, y) == "black")
            iv.setBackgroundColor(
                ContextCompat.getColor(
                    this,
                    resources.getIdentifier(nameColorBlack, "color", packageName)
                )
            )
        else
            iv.setBackgroundColor(
                ContextCompat.getColor(
                    this,
                    resources.getIdentifier(nameColorWhite, "color", packageName)
                )
            )
        if (board[x][y] == 1)
            iv.setBackgroundColor(
                ContextCompat.getColor(
                    this,
                    resources.getIdentifier("previus_cell", "color", packageName)
                )
            )

    }
    private fun paintOption(x: Int, y: Int) {
        var iv: ImageView = findViewById(resources.getIdentifier("c$x$y", "id", packageName))
        if (checkColorCell(x, y) == "black") iv.setBackgroundResource(R.drawable.option_black)
        else iv.setBackgroundResource(R.drawable.option_white)
    }
    private fun paintAllOptions() {
        for (i in 0..7) {
            for (j in 0..7) {
                if (board[i][j] != 1) paintOption(i, j)
                if (board[i][j] == 0) board[i][j] = 9
            }
        }
    }

    ////////////// GAME OVER ///////////////
    private fun checkGameOver(x: Int, y: Int) {
        if (options == 0) {
            if (bonus > 0) {
                checkMovement = false
                paintAllOptions()
            } else showMessage("Game Over", "Try Again!", true)
        }
    }
    private fun showMessage(title: String, action: String, gameOver: Boolean) {
        //Esto significa que no estamos jugando, porque ya ha salido el mensaje
        gaming = false

        //Tengo que verificar que si no he perdido, significa que he ganado, por lo que avanzo de nivel
        //if(gameOver == false) nextLevel == true
        gameOver != nextLevel

        var lyMessage = findViewById<LinearLayout>(R.id.lyMessage)
        lyMessage.visibility = View.VISIBLE

        var tvTitleMessage = findViewById<TextView>(R.id.tvTitleMessage)
        tvTitleMessage.text = title

        var tvTimeData = findViewById<TextView>(R.id.tvTimeData)
        var score: String = ""
        if (gameOver) {
            showInterstitial()
            score = "Score " + (levelMoves - moves) + "/" + levelMoves
            stringShare = "This game make me sick!!! " + score + ") jotajotavm.com/retocaballo"
        } else {
            score = tvTimeData.text.toString()
            stringShare = "Let's go!! New challenge completed. Level: $level (" + score + ") jotajotavm.com/retocaballo"
        }

        var tvScoreMessage = findViewById<TextView>(R.id.tvScoreMessage)
        tvScoreMessage.text = score
        var tvAction = findViewById<TextView>(R.id.tvAction)
        tvAction.text = action

    }

    ////////////// CHECKS ///////////////
    private fun checkOptions(x: Int, y: Int) {
        //acuerdate que esta es "global"
        options = 0
        checkMove(x, y, 1, 2) // Check move right - top long
        checkMove(x, y, 2, 1) // Check move right long - top
        checkMove(x, y, 1, -2) // Check move right - bottom long
        checkMove(x, y, 2, -1) // Check move right long - bottom
        checkMove(x, y, -1, 2) // Check move left - top long
        checkMove(x, y, -2, 1) // Check move left long - top
        checkMove(x, y, -1, -2) // Check move left - bottom long
        checkMove(x, y, -2, -1) // Check move left long - bottom

        //Aquí dejo a cero el textview que muestra las opciones
        var tvOptionsData = findViewById<TextView>(R.id.tvOptionsData)
        tvOptionsData.text = options.toString()
    }
    private fun checkMove(x: Int, y: Int, movX: Int, movY: Int) {
        var optionX = x + movX
        var optionY = y + movY

        //Vamos a ver si estamos dentro del tablero
        if (optionX < 8 && optionY < 8 && optionX >= 0 && optionY >= 0) {
            //Vamos a comprobar si esa posición está libre o es un bonus
            if (board[optionX][optionY] == 0 || board[optionX][optionY] == 2) {
                options++
                paintOption(optionX, optionY)
                if (board[optionX][optionY] == 0) board[optionX][optionY] = 9
            }
        }

    }
    private fun checkColorCell(x: Int, y: Int): String {
        var color = ""
        var blackColumnX = arrayOf(0, 2, 4, 6)
        var blackRowX = arrayOf(1, 3, 5, 7)
        if ((blackColumnX.contains(x) && blackColumnX.contains(y))
            || (blackRowX.contains(x) && blackRowX.contains(y))
        )
            color = "black"
        else color = "white"

        return color
    }

    private fun paintHorseCell(x: Int, y: Int, color: String) {
        //packageName dice donde se buscará la info solicitada
        var iv: ImageView = findViewById(resources.getIdentifier("c$x$y", "id", packageName))
        iv.setBackgroundColor(
            ContextCompat.getColor(
                this,
                resources.getIdentifier(color, "color", packageName)
            )
        )
        iv.setImageResource(R.drawable.horse)
    }
    private fun resetTime() {
        mHandler?.removeCallbacks(chronometer)
        timeInSeconds = 0

        var tvTimeData = findViewById<TextView>(R.id.tvTimeData)
        tvTimeData.text = "00:00"
    }
    private fun startTime() {
        mHandler = Handler(Looper.getMainLooper())
        chronometer.run()
    }

    //Voy a hacer mi cronómetro, es un ejecutable, nunca se detiene,
    //por lo que le voy a decir que sólo funcione mientras estamos jugando
    private var chronometer: Runnable = object: Runnable {
        override fun run() {
            //El try lo que hace es una ecución y el finally lo que hace
            //es que se vuelva a repetir el bucle
            try {
                if (gaming) {
                    //En showmessage ponemos que que gaming es false,
                    //Porque cuando se muestra el mensaje significa que no estamos jugamos
                    timeInSeconds++
                    Log.d("CHRONOMETER", timeInSeconds.toString())
                    updateStopWatchView(timeInSeconds)
                }
            } finally {
                //el Handler es para manejar algo
                //El postdelayed es para repetir algo (this) por cada tiempo(1000L)
                //Creando un bucle
                mHandler!!.postDelayed(this, 1000L)
            }
        }
    }

    private fun updateStopWatchView(timeInSeconds: Long) {
        val formattedTime = getFormattedStopWatch((timeInSeconds * 1000))
        var tvTimeData = findViewById<TextView>(R.id.tvTimeData)
        tvTimeData.text = formattedTime
    }
    private fun getFormattedStopWatch(ms:Long): String {
        //Los miliss se pasan a minutos y los miliss restantes se pasan a segundos
        var milliseconds = ms
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds)
        milliseconds -= TimeUnit.MINUTES.toMillis(minutes)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds)

        //Si munutos o segundos es menor que 10, le añadirá un cero delante
        return "${if (minutes <10) "0" else ""}$minutes:" +
            "${if (seconds<10) "0" else ""}$seconds"
    }

    //Esta función se llama varias veces, tanto en el inicio, como después de que se muestre el mensaje
    private fun startGame() {
        //Siempre que empecemos partida cargamos los datos
        if (unloadedAd) getReadyAds()

        //El gaming se queda en false cuando se termina la partida
        //por lo que hay que dejarlo en true siempre que empiece
        gaming = true

        setLevel()

        //Para probar los niveles puedo poner el nivel aquí directamente
        //level = 2

        setLevelParameters()

        resetBoard()
        clearBoard()

        setBoardLevel()
        setFirstPosition()

        resetTime()
        startTime()
    }
}