package com.example.horse_game

import android.graphics.Point
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.core.content.ContextCompat
import org.w3c.dom.Text
import java.sql.Time
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private var mHandler: Handler?= null
    private var timeInSeconds = 0L

    private var widthBonus = 0

    //Las celdas tienes que estar siempre trakeadas
    private var cellSelectedX = 0
    private var cellSelectedY = 0

    private var levelMoves = 64
    private var movesRequired = 4
    private var moves = 64
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

    }

    ////////////// INICIO JUEGO ///////////////
    private fun initScreenGame() {
        setSizeBoard()
        hideMessage()
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
    private fun hideMessage() {
        var lyMessage = findViewById<LinearLayout>(R.id.lyMessage)
        lyMessage.visibility = View.INVISIBLE
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
            Log.d("BONUS - PRE", bonus.toString())
            bonus++
            Log.d("BONUS - POST", bonus.toString())
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

        checkOption(x, y)

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
        coordX = (0..7).random()
        coordY = (0..7).random()
        //Estas dos var son generales
        cellSelectedX = coordX
        cellSelectedY = coordY
        selectCell(coordX, coordY)
    }

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
        var lyMessage = findViewById<LinearLayout>(R.id.lyMessage)
        lyMessage.visibility = View.VISIBLE

        var tvTitleMessage = findViewById<TextView>(R.id.tvTitleMessage)
        tvTitleMessage.text = title

        var tvTimeData = findViewById<TextView>(R.id.tvTimeData)
        var score: String = ""
        if (gameOver) {
            score = "Score " + (levelMoves - moves) + "/" + levelMoves
        } else {
            score = tvTimeData.text.toString()
        }

        var tvScoreMessage = findViewById<TextView>(R.id.tvScoreMessage)
        tvScoreMessage.text = score
        var tvAction = findViewById<TextView>(R.id.tvAction)
        tvAction.text = action

    }

    ////////////// CHECKS ///////////////
    private fun checkOption(x: Int, y: Int) {
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

    //Voy a hacer mi cronómetro, es un ejecutable
    private var chronometer: Runnable = object: Runnable {
        override fun run() {
            //El try lo que hace es una ecución y el finally lo que hace
            //es que se vuelva a repetir el bucle
            try {
                timeInSeconds++
                updateStopWatchView(timeInSeconds)
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

    private fun startGame () {
        resetBoard()
        clearBoard()
        setFirstPosition()

        resetTime()
        startTime()
    }
}