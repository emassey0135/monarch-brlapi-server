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
  init {
    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    isFocusable = true
    isClickable = true
  }
  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    val server = BrlapiServer { matrix ->
      if (service.isReady())
        service.display(matrix)
    }
    server.start(4101, null)
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
    service.speak("Key pressed: $keycode")
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
