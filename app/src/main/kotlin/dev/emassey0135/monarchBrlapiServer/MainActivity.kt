package dev.emassey0135.monarchBrlapiServer

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.focusable
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import dev.emassey0135.monarchBrlapiServer.brailleDisplayService.BrailleDisplayService

class MainActivity: ComponentActivity() {
  var service: BrailleDisplayService? = null
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    service = BrailleDisplayService(getApplication())
    setContent {
      val packageName = LocalContext.current.packageName
      Button(modifier = Modifier.semantics { testTagsAsResourceId = true }.testTag("$packageName:id/mainView")
        .focusable().onKeyEvent {
          service?.speak("Key pressed: ${it.key.nativeKeyCode}")
          true
        },
        onClick = {
          val width = service?.getDisplayWidth()!!
          val height = service?.getDisplayHeight()!!
          service?.speak("Display: ${width} by ${height}.")
          val rows: MutableList<List<Byte>> = mutableListOf()
          (1..height).forEach {
            val row: MutableList<Byte> = mutableListOf()
            val y = it
            (1..width).forEach {
              row.add(if (it % 2 == 0 && y % 2 == 0) 1 else 0)
            }
            rows.add(row)
          }
          service?.display(rows)
        }
      ) {
        Text("Run Test")
      }
    }
  }
}
