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
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

const val doubleTapActionId = 0x69420321
class SelfBraillingWidget(context: Context, val service: BrailleDisplayService): View(context) {
  var server: BrlapiServer? = null
  init {
    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    isFocusable = true
    isClickable = true
  }
  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    val tablesPath = File(context.cacheDir, "tables").absolutePath
    server = BrlapiServer(tablesPath, { matrix ->
      if (service.isReady())
        service.display(matrix)
    })
    server?.start(4101, null)
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
    server?.sendKeys(keycode-256)
    return true
  }
  override fun onPopulateAccessibilityEvent(event: AccessibilityEvent?) {
    super.onPopulateAccessibilityEvent(event)
    event?.text?.add("Braille View")
  }
  override fun performAccessibilityAction(action: Int, args: Bundle?): Boolean {
    if (action==doubleTapActionId) {
      val x = args?.getInt("X_POINT")
      val y = args?.getInt("Y_POINT")
      server?.routeCursor(x!!.toByte(), y!!.toByte())
      return true
    }
    else {
      return super.performAccessibilityAction(action, args)
    }
  }
}
class ServerDisplayActivity: Activity() {
  var view: SelfBraillingWidget? = null
  private fun extractTables() {
    val outputDirectory = cacheDir
    val tablesDirectory = File(outputDirectory, "tables")
    if (tablesDirectory.exists()) {
      if (tablesDirectory.isDirectory()) {
        val files = tablesDirectory.listFiles()
        if (files!=null) {
          for (file in files) {
            file.delete()
          }
        }
      }
      tablesDirectory.delete()
    }
    val assetInputStream = assets.open("tables.zip")
    val zipInputStream = ZipInputStream(BufferedInputStream(assetInputStream))
    var entry = zipInputStream.nextEntry
    while (entry!=null) {
      val file = File(outputDirectory, entry.name)
      if (entry.isDirectory) {
        file.mkdirs()
      }
      else {
        file.parentFile?.mkdirs()
        val outputStream = FileOutputStream(file)
        zipInputStream.copyTo(outputStream)
        outputStream.close()
      }
      zipInputStream.closeEntry()
      entry = zipInputStream.nextEntry
    }
    zipInputStream.close()
    assetInputStream.close()
  }
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    requestWindowFeature(Window.FEATURE_NO_TITLE)
    val service = BrailleDisplayService(getApplication())
    extractTables()
    view = SelfBraillingWidget(this, service)
    setContentView(view)
    view?.requestFocus()
  }
}
