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

    //Hago un array de arrays para simular el tablero
    private lateinit var board: Array<IntArray>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initScreenGame()
        resetBoard()
        setFirstPosition()
    }

    fun checkCellClicked (v:View) {
        var name = v.tag.toString()
        var x = name.subSequence(1, 2).toString().toInt()
        var y = name.subSequence(2, 3).toString().toInt()
        selectCell(x, y)
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
        paintHorseCell(x, y, "selected_cell")
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