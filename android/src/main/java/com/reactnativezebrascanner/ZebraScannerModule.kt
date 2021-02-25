package com.reactnativezebrascanner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.ContactsContract
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule

class ZebraScannerModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext), LifecycleEventListener {

  init {
    reactContext.addLifecycleEventListener(this)
  }

  object ZebraKeys {
    const val INTENT_FILTER_ACTION = "com.reactnativezebrascanner.BARCODE_SCANNED"
    const val DATA = "com.symbol.datawedge.data_string"
    const val PROFILE_NAME = "ReactNativeZebraScannerModule"
  }

  object ZebraActions {
    const val DATA_WEDGE = "com.symbol.datawedge.api.ACTION"
    const val RESULT_NOTIFICATION = "com.symbol.datawedge.api.NOTIFICATION"
    const val RESULT = "RESULT"
  }

  object ZebraExtras {
    const val CREATE_PROFILE = "com.symbol.datawedge.api.CREATE_PROFILE"
    const val SET_CONFIG = "com.symbol.datawedge.api.SET_CONFIG"
    const val SEND_RESULT = "SEND_RESULT"
    const val GET_VERSION_INFO = "com.symbol.datawedge.api.GET_VERSION_INFO"
    const val EMPTY = ""
    const val REGISTER_NOTIFICATION = "com.symbol.datawedge.api.REGISTER_FOR_NOTIFICATION"
    const val APPLICATION_NAME = "com.symbol.datawedge.api.APPLICATION_NAME"
    const val NOTIFICATION_TYPE = "com.symbol.datawedge.api.NOTIFICATION_TYPE"
    const val SCANNER_STATUS = "SCANNER_STATUS"
    const val UNREGISTER_NOTIFICATION = "com.symbol.datawedge.api.UNREGISTER_FOR_NOTIFICATION"
  }

  private var scanOncePromise: Promise? = null
  private var scanMultiplePromise: Promise? = null
  private var isScanningMultiple: Boolean = false

  override fun getName(): String {
    return "ZebraScanner"
  }

  // <editor-fold desc="LifeCycle">
  override fun onHostResume() {
    registerReceivers()
  }

  override fun onHostPause() {
    unregisterReceivers()
  }

  override fun onHostDestroy() {
  }
  // </editor-fold>

  // <editor-fold desc="Broadcast receiver">
  private val broadcastReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
      if (intent == null) {
        return
      }

      val action = intent.action ?: return

      if (ZebraKeys.INTENT_FILTER_ACTION == action) {
        val barcode = intent.getStringExtra(ZebraKeys.DATA) ?: return
        scanOncePromise?.resolve(barcode)
        scanOncePromise = null
        if (isScanningMultiple) {
          scanMultiplePromise?.resolve(barcode)
          scanMultiplePromise = null
          emitBarcode(barcode)
        }
      }
    }

  }
  // </editor-fold>

  // <editor-fold desc="Events">
  private fun emitBarcode(barcode: String) {
    val params = Arguments.createMap()
    params.putString("barcode", barcode)
    reactApplicationContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      .emit("BarcodeScanned", params)
  }
  // </editor-fold>

  // <editor-fold desc="Module interface">
  @ReactMethod
  fun testScan(success: Boolean, promise: Promise) {
    if (success) {
      promise.resolve("TestScan Success")
    } else {
      promise.reject(Exception("TestScan Failure"))
    }
  }

  @ReactMethod
  fun scanOnce(promise: Promise) {
    scanOncePromise = promise
  }

  @ReactMethod
  fun startScanning(promise: Promise) {
    scanMultiplePromise = promise
    isScanningMultiple = true
  }

  @ReactMethod
  fun stopScanning() {
    isScanningMultiple = false
    scanMultiplePromise = null
  }
  // </editor-fold>

  // <editor-fold desc="Helpers">
  private fun registerReceivers() {
    init()
    val intentFilter = IntentFilter().apply {
      addAction(ZebraActions.RESULT_NOTIFICATION)
      addAction(ZebraActions.RESULT)
      addCategory(Intent.CATEGORY_DEFAULT)
      addAction(ZebraKeys.INTENT_FILTER_ACTION)
    }
    reactApplicationContext.registerReceiver(broadcastReceiver, intentFilter)
  }

  private fun unregisterReceivers() {
    reactApplicationContext.unregisterReceiver(broadcastReceiver)
    reactApplicationContext.sendBroadcast(Intent().apply {
      setAction(ContactsContract.Intents.Insert.ACTION)
      putExtra(ZebraExtras.UNREGISTER_NOTIFICATION, Bundle().apply {
        putString(ZebraExtras.APPLICATION_NAME, reactApplicationContext.packageName)
        putString(ZebraExtras.NOTIFICATION_TYPE, ZebraExtras.SCANNER_STATUS)
      })
    })
  }

  private fun init() {
    // Create the profile
    sendDataWedgeIntent(ZebraActions.DATA_WEDGE, ZebraExtras.CREATE_PROFILE, ZebraKeys.PROFILE_NAME)

    val bundle = Bundle().apply {
      putString("PROFILE_NAME", ZebraKeys.PROFILE_NAME)
      putString("PROFILE_ENABLED", "true")
      putString("CONFIG_MODE", "CREATE_IF_NOT_EXIST")
      putBundle("PLUGIN_CONFIG", Bundle().apply {
        putString("PLUGIN_NAME", "BARCODE")
        putString("RESET_CONFIG", "true")
        putBundle("PARAM_LIST", Bundle().apply {
          putString("scanner_selection", "auto")
          putString("scanner_input_enabled", "true")
          putString("decoder_code128", "true")
          putString("decoder_code39", "true")
          putString("decoder_ean13", "true")
          putString("decoder_upca", "true")
        })
      })
      putParcelableArray("APP_LIST", arrayOf(Bundle().apply {
        putString("PACKAGE_NAME", reactApplicationContext.packageName)
        putStringArray("ACTIVITY_LIST", arrayOf("*"))
      }))
    }
    // Configure the profile
    sendDataWedgeIntent(ZebraActions.DATA_WEDGE, ZebraExtras.SET_CONFIG, bundle)

    bundle.remove("PLUGIN_CONFIG");
    val intentBundle = Bundle().apply {
      putString("PLUGIN_NAME", "INTENT")
      putString("RESET_CONFIG", "false")
      putBundle("PARAM_LIST", Bundle().apply {
        putString("intent_output_enabled", "true")
        putString("intent_action", ZebraKeys.INTENT_FILTER_ACTION)
        putString("intent_delivery", "2")
      })
    }
    bundle.apply {
      putBundle("PLUGIN_CONFIG", intentBundle)
    }
    sendDataWedgeIntent(ZebraActions.DATA_WEDGE, ZebraExtras.SET_CONFIG, bundle)

    sendDataWedgeIntent(ZebraActions.DATA_WEDGE, ZebraExtras.REGISTER_NOTIFICATION, Bundle().apply {
      putString(ZebraExtras.APPLICATION_NAME, reactApplicationContext.packageName)
      putString(ZebraExtras.NOTIFICATION_TYPE, "SCANNER_STATUS")
    })
  }

  private fun sendDataWedgeIntent(action: String, extraKey: String, extras: Bundle) {
    reactApplicationContext.sendBroadcast(Intent().apply {
      setAction(action)
      putExtra(extraKey, extras)
      putExtra(ZebraExtras.SEND_RESULT, "true")
    })
  }

  private fun sendDataWedgeIntent(action: String, extraKey: String, extraValue: String) {
    reactApplicationContext.sendBroadcast(Intent().apply {
      setAction(action)
      putExtra(extraKey, extraValue)
      putExtra(ZebraExtras.SEND_RESULT, "true")
    })
  }
  // </editor-fold>

}
