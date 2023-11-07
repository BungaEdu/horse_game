package com.example.horse_game

import android.content.ContextParams
import android.graphics.Point
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    //Las celdas tienes que estar siempre trakeadas
    private var cellSelectedX = 0
    private var cellSelectedY = 0

    private var options = 0

    //Hago un array de arrays para simular el tablero
    private lateinit var board: Array<IntArray>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initScreenGame()
        resetBoard()
        setFirstPosition()
    }

    fun checkCellClicked(v: View) {
        var name = v.tag.toString()
        var x = name.subSequence(1, 2).toString().toInt()
        var y = name.subSequence(2, 3).toString().toInt()

        checkCell(x, y)
    }

    private fun checkCell(x: Int, y: Int) {
        var difX = x - cellSelectedX
        var difY = y - cellSelectedY

        var checkTrue = false

        if (difX == 1 && difY == 2) checkTrue = true // right - top long
        if (difX == 1 && difY == -2) checkTrue = true // right - bottom long
        if (difX == 2 && difY == 1) checkTrue = true // right long - top
        if (difX == 2 && difY == -1) checkTrue = true // right long - bottom
        if (difX == -1 && difY == 2) checkTrue = true // left - top long
        if (difX == -1 && difY == -2) checkTrue = true // left - bottom long
        if (difX == -2 && difY == 1) checkTrue = true // left long - top
        if (difX == -2 && difY == -1) checkTrue = true // left long - bottom

        //Si ya hay otro caballo ahí
        if (board[x][y] == 1) checkTrue = false

        if (checkTrue) selectCell(x, y)
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

    private fun selectCell(x: Int, y: Int) {
        board[x][y] = 1
        paintHorseCell(cellSelectedX, cellSelectedY, "previus_cell")

        cellSelectedX = x
        cellSelectedY = y

        clearOptions()
        paintHorseCell(x, y, "selected_cell")
        checkOptions(x, y)
    }

    private fun clearOptions () {
        for (i in 0..7) {
            for (j in 0..7) {
                if (board[i][y])
            }
        }
    }

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
    }

    private fun checkMove(x: Int, y: Int, movX: Int, movY: Int) {
        var optionX = x + movX
        var optionY = y + movY

        //Vamos a ver si estamos dentro del tablero
        if (optionX < 8 && optionY < 8 && optionX >= 0 && optionY >= 0) {
            //Vamos a comprobar si esa posición está libre o es un bonus
            if (board[optionX][optionY] == 0 || board[optionX][optionY] == 2) {
                options++
                paintOptions(optionX, optionY)
                board[optionX][optionY] = 9

            }
        }

    }

    private fun paintOptions(x: Int, y: Int) {
        var iv: ImageView = findViewById(resources.getIdentifier("c$x$y", "id", packageName))
        if (checkColorCell(x, y) == "black") iv.setBackgroundResource(R.drawable.option_black)
        else iv.setBackgroundResource(R.drawable.option_white)
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
}