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
    
3. Download the [.aar](acsbt-1.0.1.aar) file to /libs folder and add the dependency to your app module's build.gradle
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
val acsUsbDevice = AcsUsbDevice(
    usbManager,
    arrayOf(AcsUsbTWDecoder()),
    arrayOf(AcsUsbNfcTWDecoder())
)
```

Connect USB device. If you are so lazy, you can get USB device from the variable `supportedDevice`

```Kotlin
acsUsbDevice.supportedDevice?.also { connectUSBDevice(it) }
```

Implement the [`AcsUsbCallback`](https://github.com/advmeds-service/CardReaderModule_Android/blob/main/CardReaderModule/src/main/java/com/advmeds/cardreadermodule/acs/usb/AcsUsbCallback.kt)

```Kotlin
public interface AcsUsbCallback {
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
