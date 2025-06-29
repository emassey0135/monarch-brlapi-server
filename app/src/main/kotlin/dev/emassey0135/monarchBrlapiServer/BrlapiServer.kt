package dev.emassey0135.monarchBrlapiServer

class BrlapiServer(val matrixCallback: (Array<ByteArray>) -> Unit) {
  external fun start(port: Short, authKey: String?)
  fun displayMatrix(matrix: Array<ByteArray>) {
    matrixCallback(matrix)
  }
//  external fun sendKeys(keys: Int)
  init {
    System.loadLibrary("monarch_brlapi_server")
  }
}
