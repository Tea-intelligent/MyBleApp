package com.dinkar.blescanner

import android.Manifest
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_scanner.*
import java.util.*


class ScannerFragment : Fragment() {
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    // localisation button
    private lateinit var localButton: Button
    // private lateinit var spinner: Spinner
    private lateinit var recyclerView: RecyclerView
    private lateinit var linearLayoutManager: LinearLayoutManager

    private var btManager: BluetoothManager? = null
    private var btAdapter: BluetoothAdapter? = null
    private var btScanner: BluetoothLeScanner? = null

    val eddystoneServiceId: ParcelUuid = ParcelUuid.fromString("0000FEAA-0000-1000-8000-00805F9B34FB")
    var beaconSet: MutableList<Beacon> = mutableListOf<Beacon>()
    var beaconTypePositionSelected = 0
    var beaconAdapter: BeaconsAdapter? = null

    private var xcoord: TextView? = null
    private var ycoord: TextView? = null
    private var xcoord2: TextView? = null
    private var ycoord2: TextView? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_scanner, container, false)
        initViews(view)
        setUpBluetoothManager()
        return view
    }

    companion object {
        private const val REQUEST_ENABLE_BT = 1
        private const val PERMISSION_REQUEST_COARSE_LOCATION = 1
    }

    private fun initViews(view: View) {
        startButton = view.findViewById(R.id.startButton)
        stopButton = view.findViewById(R.id.stopButton)
        localButton = view.findViewById(R.id.Localisation)
        // spinner = view.findViewById(R.id.spinner)
        recyclerView = view.findViewById(R.id.recyclerView)
        startButton.setOnClickListener { onStartScannerButtonClick() }
        stopButton.setOnClickListener { onStopScannerButtonClick() }
        localButton.setOnClickListener {onLocaliseButtonClick()}
        linearLayoutManager = LinearLayoutManager(context)
        recyclerView.layoutManager = linearLayoutManager
        beaconAdapter = BeaconsAdapter(beaconSet.toList())
        recyclerView.adapter = beaconAdapter
        beaconAdapter!!.filter.filter(Utils.IBEACON)
        xcoord = view.findViewById(R.id.X_coord)
        ycoord = view.findViewById(R.id.Y_coord)

        xcoord2 = view.findViewById(R.id.X_coord2)
        ycoord2 = view.findViewById(R.id.Y_coord2)

    }

    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()


    private fun buildScanFilters(): List<ScanFilter>? {
        val scanFilters: MutableList<ScanFilter> = ArrayList()
        val builder = ScanFilter.Builder()
        // Comment out the below line to see all BLE devices around you
        builder.setServiceUuid(ParcelUuid.fromString("644F76F7-6A52-42BC-E911-FD902C9BB987"))  // 644F76F76A5242BCE911FD902C9BB987
        scanFilters.add(builder.build())
        return scanFilters
    }
    private fun onStartScannerButtonClick() {
        startButton.visibility = View.GONE
        stopButton.visibility = View.VISIBLE
        btScanner!!.startScan(null, scanSettings, leScanCallback)
    }

    private fun onStopScannerButtonClick() {
        stopButton.visibility = View.GONE
        startButton.visibility = View.VISIBLE
        btScanner!!.stopScan(leScanCallback)
    }

    private fun onLocaliseButtonClick() {

    }

    private fun setUpBluetoothManager() {
        btManager = activity?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        btAdapter = btManager!!.adapter
        btScanner = btAdapter?.bluetoothLeScanner
        if (btAdapter != null && !btAdapter!!.isEnabled) {
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT)
        }
        checkForLocationPermission()
    }

    private fun checkForLocationPermission() {
        // Make sure we have access coarse location enabled, if not, prompt the user to enable it
        if (activity!!.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            val builder = AlertDialog.Builder(activity)
            builder.setTitle("This app needs location access")
            builder.setMessage("Please grant location access so this app can detect  peripherals.")
            builder.setPositiveButton(android.R.string.ok, null)
            builder.setOnDismissListener {
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                    PERMISSION_REQUEST_COARSE_LOCATION
                )
            }
            builder.show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_REQUEST_COARSE_LOCATION -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    println("coarse location permission granted")
                } else {
                    val builder = AlertDialog.Builder(activity)
                    builder.setTitle("Functionality limited")
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover BLE beacons")
                    builder.setPositiveButton(android.R.string.ok, null)
                    builder.setOnDismissListener { }
                    builder.show()
                }
                return
            }
        }
    }

    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val scanRecord = result.scanRecord
            val beacon = Beacon(result.device.address)
            beacon.manufacturer = result.device.name
            beacon.rssi = result.rssi
            // TODO try to become a cow later (i can't become a fish)
            val indexQuery = beaconSet.indexOfFirst { it.macAddress == result.device.address}
            if (indexQuery != -1) { // A scan result already exists with the same address
                beaconSet[indexQuery] = beacon
                beaconAdapter?.notifyItemChanged(indexQuery)
            } else {
                with(result.device) {
                    Log.i(
                        "Scanncallv",
                        "Found BLE device! Name: ${name ?: "Unnamed"}, address: $address"
                    )

                }
                beaconSet.add(beacon)
                beaconAdapter?.notifyItemInserted(beaconSet.size - 1)
                Log.i("THIS IS RSSI:", "Updated value")
            }

            if (scanRecord != null) {
                scanRecord.serviceUuids
                val iBeaconManufactureData = scanRecord.getManufacturerSpecificData(0X004c)

                if (iBeaconManufactureData != null && iBeaconManufactureData.size >= 23) {
                    val iBeaconUUID = Utils.toHexString(iBeaconManufactureData.copyOfRange(2, 18))
                    // val iBeaconUUID = iBeaconManufactureData.copyOfRange(2, 18)
                    val major = Integer.parseInt(
                        Utils.toHexString(
                            iBeaconManufactureData.copyOfRange(
                                18,
                                20
                            )
                        ), 16
                    )
                    val minor = Integer.parseInt(
                        Utils.toHexString(
                            iBeaconManufactureData.copyOfRange(
                                20,
                                22
                            )
                        ), 16
                    )
                    beacon.type = Beacon.beaconType.iBeacon
                    beacon.uuid = iBeaconUUID
                    beacon.major = major
                    beacon.minor = minor
                    Log.i("UUID", "iBeaconUUID:$iBeaconUUID major:$major minor:$minor")
                }
            }

            var coor: List<Double> = beaconAdapter!!.MinMax()
            var coor2: List<Double> = beaconAdapter!!.WeightedTrilaterationPosition()
            X_coord?.text = context?.let {
                if(coor[0] == -1.0){
                    String.format(
                        it.getString(R.string.xNa_coord)
                    )
                }
                else{
                    String.format(
                        it.getString(R.string.X_Coordinate),
                        coor[0]
                    )
                }
            }

            Y_coord?.text = context?.let {
                if(coor[1] == -1.0){
                    String.format(
                        it.getString(R.string.yNa_coord)
                    )
                }
                else{
                    String.format(
                        it.getString(R.string.Y_Coordinate),
                        coor[1]
                    )
                }
            }

            X_coord2?.text = context?.let {
                if(coor[0] == -1.0){
                    String.format(
                        it.getString(R.string.xNa_coord)
                    )
                }
                else{
                    String.format(
                        it.getString(R.string.X_Coordinate2),
                        coor2[0]
                    )
                }
            }

            Y_coord2?.text = context?.let {
                if(coor[1] == -1.0){
                    String.format(
                        it.getString(R.string.yNa_coord)
                    )
                }
                else{
                    String.format(
                        it.getString(R.string.Y_Coordinate2),
                        coor2[1]
                    )
                }
            }

            (recyclerView.adapter as BeaconsAdapter).updateData(beaconSet.toList())
        }
        /****/
        override fun onScanFailed(errorCode: Int) {
            Log.e("OOPS", errorCode.toString())
        }

    }
}

