package com.humanware.keysoftsdk.selfbrailling.aidl;

interface ISelfBrailling {
  int getDisplayWidth();
  int getDisplayHeight();
  void display(in String dots);
  void speak(in String text);
}
