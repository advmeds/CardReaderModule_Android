package com.advmeds.cardreadermodule.acs.ble

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.acs.bluetooth.*
import com.advmeds.cardreadermodule.acs.ACSUtils
import com.advmeds.cardreadermodule.acs.NullResponseException
import com.advmeds.cardreadermodule.acs.ble.decoder.AcsBleBaseDecoder
import java.lang.ref.WeakReference

public class AcsBaseDevice(_decoder: Array<AcsBleBaseDecoder>) {
    /** 設備狀態 */
    public enum class AcsBleDeviceStatus(rawValue: Int) {
        /** 未知 */
        UNKNOWN(0),

        /** 連線中 */
        CONNECTING(1),

        /** 已連線，並且設置好所有設定 */
        CONNECTED(2),

        /** 斷線中 */
        DISCONNECTING(3),

        /** 已斷線 */
        DISCONNECTED(4);
    }

    private val bleDecoder: Array<AcsBleBaseDecoder> = _decoder

    public var callback: AcsBaseCallback? = null
        set(value) { field = WeakReference<AcsBaseCallback>(value).get() }

    /** Bluetooth GATT client. */
    private var mBluetoothGatt: BluetoothGatt? = null

    private val mGattCallback by lazy {
        val gattCallback = BluetoothReaderGattCallback()
        gattCallback.setOnConnectionStateChangeListener { gatt, _, newState ->
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    if (mBluetoothGatt === gatt) {
                        mBluetoothReaderManager.detectReader(gatt, gattCallback)
                    }
                }
                BluetoothProfile.STATE_CONNECTING -> {

                }
                BluetoothProfile.STATE_DISCONNECTING -> {

                }
                else -> {
                    gatt.close()

                    if (mBluetoothGatt === gatt) {
                        mBluetoothGatt = null
                        mBluetoothReader = null
                        mDeviceStatus = AcsBleDeviceStatus.DISCONNECTED
                    }
                }
            }
        }
        gattCallback
    }

    /** detects ACS Bluetooth */
    private val mBluetoothReaderManager by lazy {
        val bluetoothReaderManager = BluetoothReaderManager()
        bluetoothReaderManager.setOnReaderDetectionListener {
            mBluetoothReader = it

            setupReader(it)
            activateReader(it)
        }
        bluetoothReaderManager
    }

    /** ACS Bluetooth readers */
    private var mBluetoothReader: BluetoothReader? = null

    private var nowDecoderIndex = 0

    public var mDeviceStatus: AcsBleDeviceStatus = AcsBleDeviceStatus.DISCONNECTED
        private set(value) {
            field = value
            runOnMainThread { callback?.onDeviceStatusChanged(value) }
        }

    /**
     * Create a GATT connection with the reader. And detect the connected reader
     * once service list is available.
     */
    public fun connect(context: Context, mDeviceAddress: String) {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        if (bluetoothManager == null) { // Unable to initialize BluetoothManager
            mDeviceStatus = AcsBleDeviceStatus.DISCONNECTED
            return
        }

        val bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter == null) { // Unable to obtain a BluetoothAdapter
            mDeviceStatus = AcsBleDeviceStatus.DISCONNECTED
            return
        }

        val device = bluetoothAdapter.getRemoteDevice(mDeviceAddress)
        if (device == null) { // Device not found. Unable to connect.
            mDeviceStatus = AcsBleDeviceStatus.DISCONNECTED
            return
        }

        // Clear old GATT connection
        disconnect()

        // Connect to GATT server.
        mBluetoothGatt = device.connectGatt(context, false, mGattCallback)

        mDeviceStatus = AcsBleDeviceStatus.CONNECTING
    }

    /** 與已連線的設備斷線 */
    public fun disconnect() = mBluetoothGatt?.disconnect()

    /** 設置BluetoothReader的Listener */
    private fun setupReader(reader: BluetoothReader) {
        // Invoked when the Bluetooth reader changes the card status.
        reader.setOnCardStatusChangeListener { _, cardStatus ->
            when (cardStatus) {
                BluetoothReader.CARD_STATUS_PRESENT -> {
                    nowDecoderIndex = 0

                    onAuth()

                    runOnMainThread { callback?.onCardPresent() }
                }
                BluetoothReader.CARD_STATUS_ABSENT -> {
                    runOnMainThread { callback?.onCardAbsent() }
                }
            }
        }

        // Invoked when the Bluetooth reader is authenticated.
        reader.setOnAuthenticationCompleteListener { _, errorCode ->
            when (errorCode) {
                BluetoothReader.ERROR_SUCCESS -> {
                    onPowerOn()
                }
            }
        }

        // Invoked when the Bluetooth reader returns the ATR string after powering on the card.
        reader.setOnAtrAvailableListener { bluetoothReader, atr, errorCode ->
            when (errorCode) {
                BluetoothReader.ERROR_SUCCESS -> {
                    bleDecoder[nowDecoderIndex].start(bluetoothReader)
                }
            }
        }

        // Invoked when the Bluetooth reader returns the response APDU.
        reader.setOnResponseApduAvailableListener { bluetoothReader, apdu, _ ->
            val response = bleDecoder[nowDecoderIndex].decode(bluetoothReader, apdu)

            if (response == null) {
                nowDecoderIndex = if (nowDecoderIndex + 1 >= bleDecoder.size) 0 else nowDecoderIndex + 1

                if (nowDecoderIndex != 0) {
                    onPowerOn()
                } else {
                    callback?.onReceiveResult(Result.failure(NullResponseException()))
                }
            } else if (response.isEmpty()) { // Next step

            } else {
                Handler(Looper.getMainLooper()).post {
                    callback?.onReceiveResult(Result.success(response))
                }
            }
        }

        reader.setOnEnableNotificationCompleteListener { _, result ->
            Handler(Looper.getMainLooper()).post {
                when (result) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        mDeviceStatus = AcsBleDeviceStatus.CONNECTED
                    }
                }
            }
        }
    }

    /** Start the process to enable the BluetoothReader's notifications */
    private fun activateReader(reader: BluetoothReader) {
        when (reader) {
            is Acr3901us1Reader -> reader.startBonding()
            is Acr1255uj1Reader -> reader.enableNotification(true)
        }
    }

    /** Authenticate the connected card reader. */
    private fun onAuth() {
        val masterKey = ACSUtils.getStringinHexBytes(
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF"
        )

        if (masterKey != null && masterKey.isNotEmpty()) {
            // Start authentication
            mBluetoothReader?.authenticate(masterKey)
        }
    }

    /** Power on the card. */
    private fun onPowerOn() {
        mBluetoothReader?.powerOnCard()
    }

    /** 在主執行緒執行點什麼 */
    private fun runOnMainThread(r: Runnable) = Handler(Looper.getMainLooper()).post(r)
}