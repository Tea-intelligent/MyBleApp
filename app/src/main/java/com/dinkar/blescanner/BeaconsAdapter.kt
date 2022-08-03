package com.dinkar.blescanner

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.pow


class BeaconsAdapter(beacons: List<Beacon>) :

    RecyclerView.Adapter<BeaconsAdapter.BeaconHolder>(), Filterable {
    var beaconList: MutableList<Beacon> = beacons.toMutableList()
    var beaconListFiltered: MutableList<Beacon>? = beacons.toMutableList()
    /************Anchor list*********************/

    private val Anchor = anchors()


    private var Xcoor: Double = 0.0
    private var Ycoor: Double = 0.0
    /*******************************************/

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BeaconHolder {
        val inflater = LayoutInflater.from(parent.context)
        return BeaconHolder(inflater, parent)
    }

    override fun onBindViewHolder(holder: BeaconHolder, position: Int) {
        val beacon: Beacon = beaconListFiltered!![position]
        holder.bind(beacon)
    }

    // TODO consider adding on resume function

    override fun getItemCount() = beaconListFiltered?.size ?:-1

    /***********************************************/
    // ignore: non_constant_identifier_names
    fun WeightedTrilaterationPosition(): List<Double>{ // trilateration algorithm to calculate the current coordinate that we need
        if(itemCount < 3){
            Log.e("OOPS", "Not enough beacon")
            return listOf(-1.0, -1.0)
        }else{
            val distance1 = Anchor.distance1
            val distance2 = Anchor.distance2
            val distance3 = Anchor.distance3

            val X1: Double = Anchor.xList[0]
            val X2: Double = Anchor.xList[1]
            val X3: Double = Anchor.xList[2]

            val Y1: Double = Anchor.yList[0]
            val Y2: Double = Anchor.yList[1]
            val Y3: Double = Anchor.yList[2]

            val a: Double = (-2 * X1) + (2 * X2)
            val b: Double = (-2 * Y1) + (2 * Y2)
            val c: Double = Math.pow(distance1, 2.0) -
                    distance2.pow(2.0) -
                    X1.pow(2.0) +
                    X2.pow(2.0) -
                    Y1.pow(2.0) +
                    Y2.pow(2.0)
            val d: Double = (-2 * X2) + (2 * X3)
            val e: Double = (-2 * Y2) + (2 * Y3)
            val f: Double = distance2.pow(2.0) -
                    distance3.pow(2.0) -
                    X2.pow(2.0) +
                    X3.pow(2.0) -
                    Y2.pow(2.0) +
                    Y3.pow(2.0)

            Xcoor = (c * e - f * b)
            Xcoor /= (e * a - b * d)
            //

            Ycoor = (c * d - a * f)
            Ycoor /= (b * d - a * e)

            return listOf(Xcoor, Ycoor)
        }
    }
    fun MinMax():List<Double>{
        if(itemCount < 3){
            Log.e("OOPS", "Not enough beacon")
            return listOf(-1.0, -1.0)
        }else{

            val distance1 = Anchor.distance1
            val distance2 = Anchor.distance2
            val distance3 = Anchor.distance3

            val X1: Double = Anchor.xList[0]
            val X2: Double = Anchor.xList[1]
            val X3: Double = Anchor.xList[2]

            val Y1: Double = Anchor.yList[0]
            val Y2: Double = Anchor.yList[1]
            val Y3: Double = Anchor.yList[2]

            val xMin = listOf<Double>(X1-distance1, X2-distance2, X3-distance3).max()
            val xMax = listOf<Double>(X1+distance1, X2+distance2, X3+distance3).min()
            val yMin = listOf<Double>(Y1-distance1, Y2-distance2, Y3-distance3).max()
            val yMax = listOf<Double>(Y1+distance1, Y2+distance2, Y3+distance3).min()

            if (xMin != null && xMax !=null) {
                Xcoor = (xMin + xMax) / 2
            }

            if(yMin != null && yMax != null){
                Ycoor = (yMin + yMax) / 2
            }
            return listOf(Xcoor, Ycoor)
        }
    }

    fun updateData(data: List<Beacon>) {
        beaconList.clear()
        this.notifyDataSetChanged()
        beaconList.addAll(data)
        setBeaconFilter()
        notifyDataSetChanged()
    }

    private fun setBeaconFilter() {
        filter.filter(Utils.IBEACON)
    }

    private fun anchorSetup(beacon: Beacon){
        for(uuid in Anchor.uuidList){
            if(beacon.uuid == Anchor.uuidList[0]){
                Anchor.anchor1Rssi = beacon.rssi
                Anchor.distance1 = beacon.getCalculatedDistance()
            }
            if(beacon.uuid == Anchor.uuidList[1]){
                Anchor.anchor2Rssi = beacon.rssi
                Anchor.distance2 = beacon.getCalculatedDistance()
            }
            /*
            if(beacon.uuid == Anchor.uuidList[0]){
                Anchor.anchor1Rssi = beacon.rssi
                Anchor.distance1 = beacon.getCalculatedDistance()
            }
            */
            else{
                Anchor.anchor3Rssi = beacon.rssi
                Anchor.distance3 = beacon.getCalculatedDistance()
            }
        }
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence): FilterResults {
                val charString = charSequence.toString()
                val filteredList: MutableList<Beacon> = ArrayList()
                for (beacon in beaconList) {
                    if (beacon.type == Utils.getBeaconFilterFromString(charString) && Anchor.uuidList.contains(beacon.uuid)){ // (beacon.uuid == rbd1 || beacon.uuid == rbd2 || beacon.uuid == rbd3))  Anchor.uuidList.contains(beacon.uuid)
                        filteredList.add(beacon)
                        anchorSetup(beacon)
                    }
                }
                beaconListFiltered = filteredList
                val filterResults = FilterResults()
                filterResults.values = beaconListFiltered
                return filterResults
            }

            override fun publishResults(
                charSequence: CharSequence?,
                filterResults: FilterResults
            ) {
                beaconListFiltered = filterResults.values as? ArrayList<Beacon>
                notifyDataSetChanged()
            }
        }
    }

    /********************************************************************/
    class BeaconHolder(inflater: LayoutInflater, parent: ViewGroup) :
        RecyclerView.ViewHolder(inflater.inflate(R.layout.scan_result_items, parent, false)) {
        private var image: ImageView? = null
        private var mac: TextView? = null
        private var namespaceUUID: TextView? = null
        private var instanceMajorMinor: TextView? = null
        private var rssi: TextView? = null
        private var distance: TextView? = null
        private val context = parent.context

        init {
            image = itemView.findViewById(R.id.beacon_image)
            mac = itemView.findViewById(R.id.beacon_mac)
            namespaceUUID = itemView.findViewById(R.id.beacon_namespcae_uuid)
            instanceMajorMinor = itemView.findViewById(R.id.beacon_instance_major_minor)
            rssi = itemView.findViewById(R.id.beacon_rssi)
            distance = itemView.findViewById(R.id.beacon_distance)

        }

        fun bind(beacon: Beacon) {
            mac?.text = String.format(
                context.getString(R.string.mac),
                beacon.macAddress
            )
            namespaceUUID?.text = String.format(context.getString(R.string.uuid), beacon.uuid)
            instanceMajorMinor?.text = String.format(
                context.getString(R.string.major_minor),
                beacon.major,
                beacon.minor
            )
            image?.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ibeacon))
            instanceMajorMinor?.visibility = View.VISIBLE
            namespaceUUID?.visibility = View.VISIBLE


            rssi?.text = String.format(
                context.getString(R.string.rssi),
                beacon.rssi // beacon.rssi beacon.getCalculatedDistance()
            )

            distance?.text = String.format(
                context.getString(R.string.distance),
                beacon.getCalculatedDistance() // beacon.rssi
            )

            val loguuid = beacon.uuid
            val logrssi = beacon.rssi
            Log.i("THIS IS RSSI:", "beacon uuid:$loguuid $logrssi")

        }

    }
}


