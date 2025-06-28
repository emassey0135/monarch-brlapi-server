package dev.emassey0135.monarchBrlapiServer

class BrlapiServer(val matrixCallback: (Array<Array<Byte>>) -> Unit) {
  external fun start(port: Short, authKey: String?)
  fun displayMatrix(matrix: Array<Array<Byte>>) {
    matrixCallback(matrix)
  }
//  external fun sendKeys(keys: Int)
}
