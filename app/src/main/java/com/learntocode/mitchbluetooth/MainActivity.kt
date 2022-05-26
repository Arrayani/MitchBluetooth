package com.learntocode.mitchbluetooth

import android.Manifest
import android.annotation.SuppressLint
//import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.google.android.material.snackbar.Snackbar
import com.learntocode.mitchbluetooth.databinding.ActivityMainBinding
import com.learntocode.mitchbluetooth.utils.LoadingDialog
import java.io.IOException
import java.lang.Exception
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import android.os.Looper




class MainActivity : AppCompatActivity(),Runnable {

    private lateinit var binding : ActivityMainBinding
    private val applicationUUID = UUID
        .fromString("00001101-0000-1000-8000-00805F9B34FB")
//sudah ada dalam arraylist tipe data bluetoothdevice dari javalib
    var mBTDevices : ArrayList<BluetoothDevice> = ArrayList()
    //var mDeviceListAdapter : DeviceListAdapter?=null
    //var lvNewDevices: ListView?=null
   // private var mBluetoothConnectProgressDialog: ProgressDialog? = null
   // lateinit var mBluetoothDevice: BluetoothDevice //needed
    lateinit var mBluetoothSocket: BluetoothSocket
    lateinit var simpanMac : String

    /* Get time and date */
    var c = Calendar.getInstance()
    var df = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.US)
    val formattedDate = df.format(c.time)

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pm: PackageManager = this.packageManager
        val hasBluetooth: Boolean = pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
        //ini buat cek apakah hp ada bluetooth secara system

        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

        //setContentView(R.layout.activity_main)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
       //val progressBar = binding.progressBar

        val listView =findViewById<ListView>(R.id.lvNewDevices)
        listView.setOnItemClickListener { adapterView, view, i, l ->
            bluetoothAdapter!!.cancelDiscovery()
            Log.d(TAG, "onItemClick: You Clicked on a device.")
            val deviceName: String = mBTDevices[i].getName()
            val deviceAddress: String = mBTDevices[i].getAddress()
            Log.d(TAG, "onItemClick: deviceName = $deviceName")
            Log.d(TAG,  "onItemClick: deviceAddress = $deviceAddress")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
                Log.d(TAG, "Trying to pair with $deviceName")
                mBTDevices[i].createBond()
                //bluetoothAdapter.getRemoteDevice(mBTDevices[i].getAddress())
            }


        }




        binding.btnONOFF.setOnClickListener {
            Log.d(TAG, "onClick: enabling/disabling bluetooth.")
            Log.d(TAG, hasBluetooth.toString())
            isThereBT()
            onOffBT()
        }

        binding.btnDiscoverableOnOff.setOnClickListener {
            Log.d(TAG, "btnEnableDisable_Discoverable: Making device discoverable for 300 seconds.")
            val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            startActivity(discoverableIntent)
            val intentFilter = IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)
            registerReceiver(mBroadcastReceiver3, intentFilter)
        }

        binding.btnFindUnpairedDevices.setOnClickListener{
//            val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
//            val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
//ini untuk menemukan device yg sudah dipaired
            val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
            pairedDevices?.forEach { device ->
                val deviceName = device.name
                val deviceHardwareAddress = device.address // MAC address
                Log.d(TAG, deviceName+deviceHardwareAddress)
            }

            Log.d(TAG, "btnDiscover: Looking for unpaired devices.")
            //ingat ada BluetoothaAdapater ada juga bluetoothAdapter .. CaseSensitive
            mBTDevices.clear() // bersihin kalo ga bakal numpuk nama device yg sama
            if (bluetoothAdapter!!.isDiscovering){
                bluetoothAdapter.cancelDiscovery()
                Log.d(TAG, "btnDiscover: Canceling discovery.")
                checkBTPermissions()                  //disini minta permission
                bluetoothAdapter.startDiscovery()
                val discoverDeviceIntent = IntentFilter(BluetoothDevice.ACTION_FOUND)
                registerReceiver(mBroadcastReceiver4,discoverDeviceIntent)
            }
            if (!bluetoothAdapter.isDiscovering){      //disini if kedua
                checkBTPermissions()      //disini juga bisa minta permission
                bluetoothAdapter.startDiscovery()
                val discoverDeviceIntent = IntentFilter(BluetoothDevice.ACTION_FOUND)
                registerReceiver(mBroadcastReceiver4,discoverDeviceIntent)
            }
        }
        //Broadcasts when bond state changes (ie:pairing)
        val filter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        registerReceiver(mBroadcastReceiver5,filter)

        binding.mScan.setOnClickListener{
            if(binding.mScan.text == "Connect"){
                ListPairedDevices()
                val connectIntent = Intent(this@MainActivity,DeviceListActivity::class.java)//TODO 3
                startActivityForResult(connectIntent,REQUEST_CONNECT_DEVICE) // REQUEST_CONNECT_DEVICE menjadi mRequestCode
            }else if (binding.mScan.text == "Disconnect") {
                val stat = findViewById<TextView>(R.id.bpstatus)
                val mScan = findViewById<Button>(R.id.mScan)
                if (bluetoothAdapter != null) bluetoothAdapter.disable()
                stat.text = ""
                stat.text = "Disconnected"
                stat.setTextColor(Color.rgb(199, 59, 59))
                binding.mPrint.isEnabled = false
                mScan.isEnabled = true
                mScan.text = "Connect"
            }
        }
        binding.mPrint.setOnClickListener{
            p1()
        }
    }

    private fun p1() {
        val t: Thread = object : Thread() {
            override fun run() {
                try {
                    val os = mBluetoothSocket.outputStream
                    var header = ""
                    var he = ""
                    var blank = ""
                    var header2 = ""
                    //var BILL = ""
                    var BILL : String?
                    //var vio = ""
                    var vio : String? = null
                    var header3 = ""
                    var mvdtail = ""
                    var header4 = ""
                    var offname = ""
                    var time = ""
                    var copy = ""
                    val checktop_status = ""
                    blank = "\n\n"
                    //he = "      EFULLTECH NIGERIA\n"
                    he = "1234567890123456789012345678901234567890"
                   // he = "$he********************************\n\n"
                    header = "FULL NAME:\n"
                    BILL =   "\n"   // ini buat ngambil text nama
                    BILL = (BILL
                            + "================================\n")
                    header2 = "COMPANY'S NAME:\n"
                    vio =  "\n"  // ini buat ngambil text nama company
                    vio = (vio
                            + "================================\n")
                    header3 = "AGE:\n"
                    mvdtail =  "\n"   // ini buat ngambil text age
                    mvdtail = ("================================\n")
                    header4 = "AGENT DETAILS:\n"
                    offname = "\n" // ini buat ngambil text agents detail
                    offname = (offname
                            + "--------------------------------\n")
                    time = formattedDate + "\n\n"
                    copy = "-Customer's Copy\n\n\n\n\n\n\n\n\n"
                    os!!.write(blank.toByteArray())
                    //os.write(he.toByteArray())
//                    os.write(header.toByteArray())
//                    os.write(BILL.toByteArray())
//                    os.write(header2.toByteArray())
//                    os.write(vio.toByteArray())
//                    os.write(header3.toByteArray())
//                    os.write(mvdtail.toByteArray())
//                    os.write(header4.toByteArray())
//                    os.write(offname.toByteArray())
//                    os.write(checktop_status.toByteArray())
//                    os.write(time.toByteArray())
//                    os.write(copy.toByteArray())



                    //ini bedanya dengan diatas adalah, variable valuenya sudah di isi dengan angka
                    // sehingga jika menggunakan toByteArray maka yang dibawah ini menggunakan'
                    //intToByteArray
                    // Setting height --- ini buat semacam feed kertas blank. memberi jeda tulisan kosong untuk print yg ke dua
                    val gs = 29
                    os.write(intToByteArray(gs).toInt())
                    val h = 150
                    os.write(intToByteArray(h).toInt())
                    val n = 170
                    os.write(intToByteArray(n).toInt())

                    // Setting Width
                    val gs_width = 29
                    os.write(intToByteArray(gs_width).toInt())
                    val w = 119
                    os.write(intToByteArray(w).toInt())
                    val n_width = 2
                    os.write(intToByteArray(n_width).toInt())

                    var text = "---------------------\n"
                    text += "Nama item  : Tongkat bangku \n"
                    text += "harga : 135000\n"
                    text += "qty   : 10 pccs\n"
                    text += "total Harga : 1.350.000 pccs\n"
                    os.write(text.toByteArray())


                } catch (e: Exception) {
                    Log.e("PrintActivity", "Exe ", e)
                }
            }
        }
        t.start()
    }


    @RequiresApi(Build.VERSION_CODES.M)
    private fun ListPairedDevices() {
        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
        val mPairedDevices = bluetoothAdapter?.bondedDevices

        if (mPairedDevices != null) {
            if (mPairedDevices.size > 0) {
                for (mDevice: BluetoothDevice in mPairedDevices) {
                    Log.v(TAG, "PairedDevices: " + mDevice.name + "  "+ mDevice.address)
                }
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onActivityResult(mRequestCode: Int, mResultCode: Int, mDataIntent: Intent?) {
        super.onActivityResult(mRequestCode, mResultCode, mDataIntent)
        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

        when (mRequestCode) {                                         //mRequestCode itu dapat caller diatas, dan merupakan variable buatan
            REQUEST_CONNECT_DEVICE -> if (mResultCode == RESULT_OK) { //Result_Ok  didapat dari DeviceListActivity mbundle
                val mExtra = mDataIntent!!.extras //ini mengambil data dari mbundle berasal dari devicelistActivity
                val mDeviceAddress = mExtra!!.getString("DeviceAddress")
                Log.v(TAG,"Coming incoming address $mDeviceAddress")
                val bluetoothDevice = bluetoothAdapter!!.getRemoteDevice(mDeviceAddress)
                simpanMac = bluetoothDevice.toString() //ini cara gw menyimpan mac ddressnya.... kerennn boyy kelassss
                                                       //gw buat variable global, biar bisa di pake dimana-mana
                //bluetoothAdapter!!.getRemoteDevice(mDeviceAddress)
//                mBluetoothConnectProgressDialog = ProgressDialog.show(this,
//                    "Connecting...", bluetoothDevice.getName() + " : "
//                            + bluetoothDevice.getAddress(), true, false
//                )
                    binding.progressBar.visibility = View.VISIBLE
//                val loading = LoadingDialog(this)
//                loading.startLoading()

                val mBlutoothConnectThread = Thread(this)
                mBlutoothConnectThread.start()
            }
            REQUEST_ENABLE_BT -> if (mResultCode == RESULT_OK) {
                ListPairedDevices()
                val connectIntent = Intent(this@MainActivity,DeviceListActivity::class.java )
                startActivityForResult(connectIntent, REQUEST_CONNECT_DEVICE)
            }else{
                Toast.makeText(this@MainActivity, "Not connected to any device", Toast.LENGTH_SHORT)
                    .show()
            }

        }

    }
    @RequiresApi(Build.VERSION_CODES.M)
    override fun run() {
        try {

            val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java) //daily dose
            val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter //daily dose
            val mBluetoothDevice = bluetoothAdapter!!.getRemoteDevice(simpanMac)
            //mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(applicationUUID)
            mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(applicationUUID)
            bluetoothAdapter.cancelDiscovery()
            mBluetoothSocket.connect()
            mHandler.sendEmptyMessage(0)
          //mainHandler belum berhasil ganti ini

            Log.d(TAG, "berhasil connect ke socket bluetoothdevice")
        } catch (eConnectException: IOException) {
            Log.d(TAG, "CouldNotConnectToSocket", eConnectException)
            closeSocket(mBluetoothSocket)
            return
        }
    }
    private fun closeSocket(nOpenSocket: BluetoothSocket?) {
        try {
            nOpenSocket!!.close()
            Log.d(TAG, "SocketClosed")
        } catch (ex: IOException) {
            Log.d(TAG, "CouldNotCloseSocket")
        }
    }
    @SuppressLint("HandlerLeak")
    private val mHandler = object : Handler() {
        override fun handleMessage(msg: Message) {
            val stat = findViewById<TextView>(R.id.bpstatus)
           // mBluetoothConnectProgressDialog!!.dismiss()
           // val loading = LoadingDialog(this@MainActivity)
           // loading.isDismiss()
            binding.progressBar.visibility = View.INVISIBLE
            stat.text = ""
            stat!!.text = "Connected"
            stat.setTextColor(Color.rgb(97, 170, 74))
            val mPrint = findViewById<Button>(R.id.mPrint)
            mPrint.isEnabled = true
            val mScan = findViewById<Button>(R.id.mScan)
            mScan.text = "Disconnect"
        }
    }

//       val mainHandler = Handler(Looper.getMainLooper()).post {
//           val stat = binding.bpstatus
//           //mBluetoothConnectProgressDialog!!.dismiss()
//           binding.progressBar.visibility = View.INVISIBLE
//           stat.text = ""
//           stat!!.text = "Connected"
//           stat.setTextColor(Color.rgb(97, 170, 74))
//           binding.mPrint.isEnabled = true
//           binding.mScan.text = "Disconnect"
//       }


    private fun checkBTPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            var permissionCheck: Int = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION")
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION")
            if (permissionCheck != 0) {
                this.requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION),
                    1001) //Any number
            } // minta 2 permission sekaligus
        } else {
            Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.")
        }
    }




    @SuppressLint("MissingPermission") //ini suppress supaya error bilang izin akses hilang
    @RequiresApi(Build.VERSION_CODES.M)
    private fun onOffBT() {
        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
        if (!bluetoothAdapter!!.isEnabled){
            Log.d(TAG, "enableDisableBT: enabling BT.")
            val enableBTIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivity(enableBTIntent)
            //IntentFilter butuh di import
            // tenyata bisa langsung akses class BluetoothAdapter tanpa harus buat instances
            val BTIntent = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
            registerReceiver(mBroadcastReceiver2,BTIntent)
        }
        if (bluetoothAdapter.isEnabled) {
            Log.d(TAG, "enableDisableBT: disabling BT.")
            bluetoothAdapter.disable()
            val BTIntent = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
            registerReceiver(mBroadcastReceiver2, BTIntent)
        }

    }



    //BroadcastReceiver di import dulu
    private val mBroadcastReceiver2 = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            // When discovery finds a device
            if (action == BluetoothAdapter.ACTION_STATE_CHANGED ){
                //ini untuk menangkap kode error yang muncul
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,BluetoothAdapter.ERROR)
                when (state) {
                    BluetoothAdapter.STATE_OFF -> Log.d(
                        TAG,
                        "onReceive: STATE OFF"
                    )
                    BluetoothAdapter.STATE_TURNING_OFF -> Log.d(
                        TAG,
                        "mBroadcastReceiver1: STATE TURNING OFF"
                    )
                    BluetoothAdapter.STATE_ON -> Log.d(
                        TAG,
                        "mBroadcastReceiver1: STATE ON"
                    )
                    BluetoothAdapter.STATE_TURNING_ON -> Log.d(
                        TAG,
                        "mBroadcastReceiver1: STATE TURNING ON"
                    )
                }
            }
        }
    }

    private val mBroadcastReceiver3 =object :BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (action == BluetoothAdapter.ACTION_SCAN_MODE_CHANGED) {
                val mode: Int = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR)
                when (mode) {
                    BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE -> Log.d(
                        TAG,
                        "mBroadcastReceiver2: Discoverability Enabled."
                    )
                    BluetoothAdapter.SCAN_MODE_CONNECTABLE -> Log.d(
                        TAG,
                        "mBroadcastReceiver2: Discoverability Disabled. Able to receive connections."
                    )
                    BluetoothAdapter.SCAN_MODE_NONE -> Log.d(
                        TAG,
                        "mBroadcastReceiver2: Discoverability Disabled. Not able to receive connections."
                    )
                    BluetoothAdapter.STATE_CONNECTING -> Log.d(TAG, "mBroadcastReceiver2: Connecting....")
                    BluetoothAdapter.STATE_CONNECTED -> Log.d(TAG, "mBroadcastReceiver2: Connected.")
                }
            }
        }
    }

    private val mBroadcastReceiver4 = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.M)
        override fun onReceive(context: Context, intent: Intent) {
            val action: String? = intent.action
            Log.d(TAG, "onReceive: ACTION FOUND.")

            if (action ==  BluetoothDevice.ACTION_FOUND){
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                val listView =findViewById<ListView>(R.id.lvNewDevices)
                mBTDevices.add(device!!)
                Log.d(TAG, "onReceive: " + device.name.toString() + ": " + device.address)
                Log.d(TAG,mBTDevices.toString())

                listView.adapter = DeviceListAdapter(this@MainActivity,R.layout.device_adapter_view,mBTDevices)

            }else{
                Log.d(TAG, "failede to discover")
            }
        }

    }

    private val mBroadcastReceiver5= object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.M)
        override fun onReceive(context: Context, intent: Intent) {
            val action: String? = intent.action

            if (action ==  BluetoothDevice.ACTION_BOND_STATE_CHANGED){
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                //3 cases:
                //case1: bonded already
                if (device!!.bondState === BluetoothDevice.BOND_BONDED){
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDED.")
                }
                //case2: creating a bone
                if (device!!.bondState ===  BluetoothDevice.BOND_BONDING){
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDING.")
                }
                if (device!!.bondState ===  BluetoothDevice.BOND_NONE){
                    Log.d(TAG, "BroadcastReceiver: BOND_NONE.")
                }

            }else{
                Log.d(TAG, "failede to Bond")
            }
        }

    }


    override fun onDestroy() {
        Log.d(TAG, "onDestroy: called.")
        super.onDestroy()
        unregisterReceiver(mBroadcastReceiver2)
        unregisterReceiver(mBroadcastReceiver3)
        unregisterReceiver(mBroadcastReceiver4)
        unregisterReceiver(mBroadcastReceiver5)

    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun isThereBT() {
        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

        if (bluetoothAdapter == null) {
            Toast.makeText(this@MainActivity,"Tidak mendukung bluetooth", Toast.LENGTH_SHORT).show()
            binding.btnONOFF.isEnabled=false
            return

        }else
        {
            //Toast.makeText(this@MainActivity,"Dukungan bluetooth tersedia",Toast.LENGTH_SHORT).show()
            cekScanBTPermission()
            cekForBTConPermision()

        }
    }

    private fun cekForBTConPermision() {
        val mLayout = binding.rootMLayout
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            == PackageManager.PERMISSION_GRANTED){
            Snackbar.make(mLayout,"Sudah diberikan izin Bluetooth Connect", Snackbar.LENGTH_LONG).show()
        }
        else{
            Snackbar.make(mLayout,"Belum diberikan izin akses", Snackbar.LENGTH_LONG).show()
            requestBluetoothConnetctPermission()
        }
    }

    private fun requestBluetoothConnetctPermission() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.BLUETOOTH_CONNECT

            )){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(
                    this@MainActivity, arrayOf((Manifest.permission.BLUETOOTH_CONNECT)),
                    PERMISSION_REQUEST_BLUETOOTH_CONNECT
                )
            }
        }else{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(
                    this@MainActivity, arrayOf((Manifest.permission.BLUETOOTH_CONNECT)),
                    PERMISSION_REQUEST_BLUETOOTH_CONNECT
                )
            }

        }
    }

    private fun cekScanBTPermission() {
        val mLayout = binding.rootMLayout
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
            == PackageManager.PERMISSION_GRANTED){
            Toast.makeText(this@MainActivity,"Sudah diberikan izin akses Scan Bluetooth",Toast.LENGTH_SHORT).show()
            //Snackbar.make(mLayout,"Sudah diberikan izin akses Scan Bluetooth", Snackbar.LENGTH_LONG).show()
        }
        else{
            Snackbar.make(mLayout,"Belum diberikan izin akses Scan Bluetooth", Snackbar.LENGTH_LONG).show()
            requestScanBTPermission()
        }
    }

    private fun requestScanBTPermission() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.BLUETOOTH_SCAN

            )){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(
                    this@MainActivity, arrayOf((Manifest.permission.BLUETOOTH_SCAN)),
                    PERMISSION_REQUEST_BLUETOOTH_SCAN
                )
            }
        }else{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(
                    this@MainActivity, arrayOf((Manifest.permission.BLUETOOTH_SCAN)),
                    PERMISSION_REQUEST_BLUETOOTH_SCAN
                )
            }

        }

    }

    companion object{
        private const val TAG = "MainActivity"
        private const val PERMISSION_REQUEST_BLUETOOTH_CONNECT = 20
        private const val PERMISSION_REQUEST_BLUETOOTH_SCAN = 30
        private const val REQUEST_CONNECT_DEVICE = 1
        private const val REQUEST_ENABLE_BT = 2

        fun intToByteArray(value: Int): Byte {
            val b = ByteBuffer.allocate(4).putInt(value).array()
            for (k in b.indices) {
                println(
                    "Selva  [" + k + "] = " + "0x"
                            + UnicodeFormatter.byteToHex(b[k])
                )
            }
            return b[3]
        }

    }




}