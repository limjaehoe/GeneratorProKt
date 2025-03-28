package android_serialport_api

import android.util.Log
import java.io.File
import java.io.FileReader
import java.io.LineNumberReader
import java.util.Vector

class SerialPortFinder {
    private val TAG = "SerialPortFinder"
    private val mDrivers = Vector<Driver>()

    val allDevices: Array<String>
        get() {
            val devices = Vector<String>()
            for (driver in mDrivers) {
                val driverDevices = driver.devices
                for (device in driverDevices) {
                    val deviceName = String.format("%s (%s)", device.name, driver.name)
                    devices.add(deviceName)
                }
            }
            return devices.toTypedArray()
        }

    val allDevicesPath: Array<String>
        get() {
            val devices = Vector<String>()
            for (driver in mDrivers) {
                val driverDevices = driver.devices
                for (device in driverDevices) {
                    devices.add(device.root)
                }
            }
            return devices.toTypedArray()
        }

    init {
        probeSerialPorts()
    }

    private fun probeSerialPorts() {
        mDrivers.clear()
        try {
            // /proc/tty/drivers 파일 읽기
            val r = LineNumberReader(FileReader("/proc/tty/drivers"))
            var line: String?
            while (r.readLine().also { line = it } != null) {
                // Format: /dev/tty 유형
                val driverName = line!!.substring(0, 0x15).trim { it <= ' ' }
                val w = line!!.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (w.size >= 5 && w[w.size - 1] == "serial") {
                    Log.d(TAG, "Found new driver: $driverName on ${w[w.size - 4]}")
                    val d = Driver(driverName, w[w.size - 4])
                    mDrivers.add(d)
                }
            }
            r.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error reading /proc/tty/drivers", e)
        }

        // 각 드라이버에 대해 장치 파일 검색
        for (driver in mDrivers) {
            driver.probeDevices()
        }
    }

    /**
     * 시리얼 드라이버 정보
     */
    inner class Driver(val name: String, val root: String) {
        val devices = Vector<Device>()

//        fun probeDevices() {
//            val dev = File("/dev")
//            val files = dev.listFiles { _, name -> name.startsWith(root.substring("/dev/".length)) }
//
//            // 나머지 코드...
//            if (files != null) {
//                for (file in files) {
//                    Log.d(TAG, "Found new device: ${file.name}")
//                    devices.add(Device(file.name, file.absolutePath))
//                }
//            }
//
//        }


        fun probeDevices() {
            val dev = File("/dev")

            // 안전 확인 추가
            if (root.length <= "/dev/".length) {
                Log.e(TAG, "Invalid root path: $root")
                return
            }

            val prefix = try {
                root.substring("/dev/".length)
            } catch (e: Exception) {
                Log.e(TAG, "Error extracting prefix from $root", e)
                return
            }

            val files = dev.listFiles { _, name -> name.startsWith(prefix) }

            // 나머지 코드...
            if (files != null) {
                for (file in files) {
                    Log.d(TAG, "Found new device: ${file.name}")
                    devices.add(Device(file.name, file.absolutePath))
                }
            }
        }
    }

    /**
     * 시리얼 장치 정보
     */
    inner class Device(val name: String, val root: String)
}