package dev.emassey0135.monarchBrlapiServer

import android.content.ContentProvider
import android.content.ContentValues
import android.content.res.AssetManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import java.io.File;
import java.io.FileOutputStream;

class XmlResource: ContentProvider() {
  override fun onCreate(): Boolean {
    return true
  }
  override fun insert(uri: Uri, content: ContentValues?): Uri? {
    return null
  }
  override fun update(uri: Uri, content: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int {
    return 0
  }
  override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
    return 0
  }
  override fun getType(uri: Uri): String? {
    return null
  }
  override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor? {
    return null
  }
  override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
    val context = getContext()!!
    val askingForCommands = uri.getPath()?.contains("commands") ?: false
    if (!askingForCommands)
      return null
    val file = File(context.cacheDir, "commands.xml")
    val assets = context.getAssets()
    val assetStream = assets.open("commands.xml")
    val fileOutputStream = FileOutputStream(file)
    val buffer = ByteArray(1024)
    while (true) {
      val size = assetStream.read(buffer)
      if (size==-1)
        break
      fileOutputStream.write(buffer, 0, size)
    }
    assetStream.close()
    fileOutputStream.close()
    return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
  }
}
