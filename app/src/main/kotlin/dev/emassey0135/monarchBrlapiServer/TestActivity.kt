package dev.emassey0135.monarchBrlapiServer

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.Window
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import dev.emassey0135.monarchBrlapiServer.brailleDisplayService.BrailleDisplayService

const val doubleTapActionId = 0x69420321
class SelfBraillingWidget(context: Context, val service: BrailleDisplayService): View(context) {
  var firstPress = true
  init {
    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    isFocusable = true
    isClickable = true
  }
  override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
    super.onInitializeAccessibilityNodeInfo(info)
    info.setClassName(SelfBraillingWidget::class.java.name)
    info.setViewIdResourceName("SelfBraillingWidget")
    info.text = "Braille View"
    val doubleTapAction = AccessibilityNodeInfo.AccessibilityAction(doubleTapActionId, "Double tap at dot position")
    info.addAction(doubleTapAction)
  }
  override fun onKeyDown(keycode: Int, event: KeyEvent): Boolean {
    if (keycode<=256)
      return false
    if (firstPress) {
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
      firstPress = false
    }
    else {
      service.speak("Key pressed: $keycode")
    }
    return true
  }
  override fun onPopulateAccessibilityEvent(event: AccessibilityEvent?) {
    event?.text?.add("Braille View")
  }
  override fun performAccessibilityAction(action: Int, args: Bundle?): Boolean {
    if (action==doubleTapActionId) {
      val x = args?.getInt("X_POINT")
      val y = args?.getInt("Y_POINT")
      service.speak("Double tap at ($x, $y)")
      return true
    }
    else {
      return super.performAccessibilityAction(action, args)
    }
  }
}
class TestActivity: Activity() {
  var view: SelfBraillingWidget? = null
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    val service = BrailleDisplayService(getApplication())
    view = SelfBraillingWidget(this, service)
    setContentView(view)
    view?.requestFocus()
  }
}
