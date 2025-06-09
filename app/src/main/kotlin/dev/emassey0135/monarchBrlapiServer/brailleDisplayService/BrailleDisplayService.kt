package dev.emassey0135.monarchBrlapiServer.brailleDisplayService

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.humanware.keysoftsdk.selfbrailling.aidl.ISelfBrailling
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable

class BrailleDisplayService(val context: Context) {
  private var brailleDisplayInterface: ISelfBrailling? = null
  class BrailleDisplayServiceConnection(private val handler: (ISelfBrailling?) -> Unit): ServiceConnection {
    override fun onServiceConnected(name: ComponentName, service: IBinder) {
      handler(ISelfBrailling.Stub.asInterface(service))
    }
    override fun onServiceDisconnected(name: ComponentName) {
      handler(null)
    }
  }
  init {
    val connection = BrailleDisplayServiceConnection { value -> brailleDisplayInterface = value }
    context.bindService(
      Intent().setClassName("com.humanware.keysoft", "com.humanware.keysoft.display.selfbrailling.SelfBraillingService"),
      connection,
      Context.BIND_AUTO_CREATE)
  }
  fun isReady(): Boolean {
    return brailleDisplayInterface==null
  }
  fun getDisplayWidth(): Int? {
    return brailleDisplayInterface?.getDisplayWidth()
  }
  fun getDisplayHeight(): Int? {
    return brailleDisplayInterface?.getDisplayHeight()
  }
  @Serializable
  private data class DotMatrix(val a: List<List<Byte>>, val b: Int, val c: Int) {
    constructor(dots: List<List<Byte>>): this(dots, dots.first().count(), dots.count())
  }
  fun display(dots: List<List<Byte>>) {
    brailleDisplayInterface?.display(Json.encodeToString(DotMatrix(dots)))
  }
  fun speak(text: String) {
    brailleDisplayInterface?.speak(text)
  }
}
