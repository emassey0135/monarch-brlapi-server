package dev.emassey0135.monarchBrlapiServer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import dev.emassey0135.monarchBrlapiServer.brailleDisplayService.BrailleDisplayService

class MainActivity: ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val service = BrailleDisplayService(getApplication())
    setContent {
      Button(onClick = {
        val width = service.getDisplayWidth()!!
        val height = service.getDisplayHeight()!!
        service.speak("Display: ${width} by ${height}.")
        val rows: MutableList<List<Byte>> = mutableListOf()
        (1..height).forEach {
          val row: MutableList<Byte> = mutableListOf()
          val y = it
          (1..width).forEach {
            row.add(if (it % 2 == 0 && y % 2 == 0) 1 else 0)
          }
          rows.add(row)
        }
        service.display(rows)
      }) {
        Text("Run Test")
      }
    }
  }
}
