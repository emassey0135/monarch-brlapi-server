package dev.emassey0135.monarchBrlapiServer

import android.system.Os

class BrlapiServer(tablesPath: String, val matrixCallback: (Array<ByteArray>) -> Unit) {
  var keycodeTx: Long = 0
  init {
    Os.setenv("LOUIS_TABLEPATH", tablesPath, true)
  }
  external fun start(port: Short, authKey: String?)
  fun displayMatrix(matrix: Array<ByteArray>) {
    matrixCallback(matrix)
  }
  external fun sendKeys(keys: Int)
  init {
    System.loadLibrary("monarch_brlapi_server")
  }
}
