[![](https://jitpack.io/v/advmeds-service/CardReaderModule_Android.svg)](https://jitpack.io/#advmeds-service/CardReaderModule_Android)

Installation
==========================
1. Add it to your root build.gradle
```
allprojects {
    repositories {
        ...
	maven { url 'https://jitpack.io' }
    }
}
```

2. Add the dependency to your app module's build.gradle
```
dependencies {
    implementation 'com.github.advmeds-service:CardReaderModule_Android:Tag'
}
```
    
3. Download the [.aar](https://github.com/advmeds-service/CardReaderModule_Android/blob/main/CardReaderModule/libs/acsbt-1.0.1.aar) file to /libs folder and add the dependency to your app module's build.gradle
```
dependencies {
    implementation files("libs/acsbt-1.0.1.aar")
}
```

Usage
==========================
## Bluetooth Card Reader

Create an instance from the [`Array<AcsBleBaseDecoder>`](https://github.com/advmeds-service/CardReaderModule_Android/blob/main/CardReaderModule/src/main/java/com/advmeds/cardreadermodule/acs/ble/decoder/AcsBleBaseDecoder.kt)

```Kotlin
val acsBaseDevice = AcsBaseDevice(arrayOf(AcsBleTWDecoder()))
```

Connect the discovered device according to MAC address

```kotlin
acsBaseDevice.connect(your context, your device mac address)
```

Implement the [`AcsBaseCallback`](https://github.com/advmeds-service/CardReaderModule_Android/blob/main/CardReaderModule/src/main/java/com/advmeds/cardreadermodule/acs/ble/AcsBaseCallback.kt)

```Kotlin
public interface AcsBaseCallback {
    /**
     * 設備狀態更新時，會呼叫此方法
     * @param status 設備狀態
     * @see AcsBleDeviceStatus
     */
    fun onDeviceStatusChanged(status: AcsBaseDevice.AcsBleDeviceStatus)

    /** 卡片插入設備 */
    fun onCardPresent()

    /**
     * 回傳插入卡片後的讀取結果
     * @param result 判斷成功或失敗，若成功則回傳AcsResponseModel；若失敗則回傳Exception
     */
    fun onReceiveResult(result: Result<AcsResponseModel>)

    /** 卡片從設備抽離 */
    fun onCardAbsent()
}
```

## USB Card Reader
Create an instance

```Kotlin
val usbManager = getSystemService(Context.USB_SERVICE) as UsbManager

// For ACS USB Card Reader
val acsUsbDevice = AcsUsbDevice(
    usbManager,
    arrayOf(AcsUsbTWDecoder(), AcsUsbJPNDecoder(), AcsUsbThaiDecoder()),
    arrayOf(AcsUsbNfcTWDecoder(), AcsUsbNfcTWMNDDecoder())
)

// For EZ100pu
val ezUsbDevice = CastlesUsbDevice(context)
```
In your activity, you can obtain the UsbDevice that represents the attached device from AcsUsbDevice or CastlesUsbDevice like this:

```Kotlin
val device: UsbDevice? = acsUsbDevice.supportedDevice
val device: UsbDevice? = ezUsbDevice.supportedDevice
```
Before communicating with the USB device, your application must have permission from your users.

```Kotlin
private val usbReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val usbDevice = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE) ?: return

        when (intent.action) {
            USB_PERMISSION -> {
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    // user choose YES for your previously popup window asking for grant perssion for this usb device
                    when (usbDevice.productId) {
                        acsUsbDevice.supportedDevice?.productId -> {
                            acsUsbDevice.connectDevice(usbDevice)
                        }
                        ezUsbDevice.supportedDevice?.productId -> {
                            ezUsbDevice.connectDevice(usbDevice)
                        }
                    }
                } else {
                    // user choose NO for your previously popup window asking for grant perssion for this usb device
                }
            }
            UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
	        // To display the dialog that asks users for permission to connect to the device
                val mPermissionIntent = PendingIntent.getBroadcast(
		    this@YourActivity, 0, Intent(USB_PERMISSION), 0
		)
		
		usbManager.requestPermission(usbDevice, mPermissionIntent)
            }
            UsbManager.ACTION_USB_DEVICE_DETACHED -> {
	    	// call your method that cleans up and closes communication with the device
                when (usbDevice.productId) {
                    acsUsbDevice.connectedDevice?.productId -> {
                        acsUsbDevice.disconnect()
                    }
                    ezUsbDevice.connectedDevice?.productId -> {
                        ezUsbDevice.disconnect()
                    }
                }
            }
        }
    }
}
```

To register the broadcast receiver, add this in your onCreate() method in your activity:

```Kotlin
private const val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"

val filter = IntentFilter(USB_PERMISSION)
filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)

registerReceiver(
    usbReceiver,
    filter
)
```

Implement the [`UsbDeviceCallback`](https://github.com/advmeds-service/CardReaderModule_Android/blob/main/CardReaderModule/src/main/java/com/advmeds/cardreadermodule/UsbDeviceCallback.kt)

```Kotlin
public interface UsbDeviceCallback {
    /** 成功連線上設備 */
    fun onConnectDevice()

    /** 無法與設備連線 */
    fun onFailToConnectDevice()

    /** 卡片插入設備 */
    fun onCardPresent()

    /**
     * 回傳插入卡片後的讀取結果
     * @param result 判斷成功或失敗，若成功則回傳AcsResponseModel；若失敗則回傳Exception
     */
    fun onReceiveResult(result: Result<AcsResponseModel>)

    /** 卡片從設備抽離 */
    fun onCardAbsent()
}
```
