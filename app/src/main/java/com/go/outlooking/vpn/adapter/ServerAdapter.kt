package com.go.outlooking.vpn.adapter


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.go.outlooking.vpn.bean.ServerBean
import com.go.outlooking.vpn.R

class ServerAdapter : RecyclerView.Adapter<ServerAdapter.ServerViewHolder>() {


    lateinit var onItemClickListener: OnItemClickListener



    private val serverList by lazy { mutableListOf<ServerBean>() }


    fun refreshData(list: MutableList<ServerBean>) {
        serverList.clear()
        if (list != null) {
            serverList.addAll(list)
        }
        notifyDataSetChanged()
    }


    class ServerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val ivFlag: ImageView = itemView.findViewById(R.id.iv_flag)
        val tvVpnTitle: TextView = itemView.findViewById(R.id.tv_vpn_title)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_server, parent, false)
        return ServerViewHolder(view)
    }

    override fun onBindViewHolder(holder: ServerViewHolder, position: Int) {

        val vpnInfoEntity = serverList[position]

        holder.tvVpnTitle.text = vpnInfoEntity.name

        if (vpnInfoEntity.selected) {
            holder.tvVpnTitle.setTextColor(ContextCompat.getColor(holder.tvVpnTitle.context, R.color.blue))
        } else {
            holder.tvVpnTitle.setTextColor(ContextCompat.getColor(holder.tvVpnTitle.context, android.R.color.black))
        }

//        if (vpnInfoEntity.shareCode == selectedVpn) {
//            holder.tvVpnTitle.setTextColor(holder.tvVpnTitle.context.getColor(R.color.blue))
//        } else {
//            holder.tvVpnTitle.setTextColor(holder.tvVpnTitle.context.getColor(android.R.color.black))
//        }



        when (vpnInfoEntity.countryCode) {
            "US" -> {
                holder.ivFlag.setImageResource(R.drawable.flag_unitedstates)
            }

//            "GB" -> {
//                holder.ivFlag.setImageResource(R.drawable.flag_unitedkingdom)
//            }
//
//            "DE" -> {
//                holder.ivFlag.setImageResource(R.drawable.flag_germany)
//            }
//
//            "FR" -> {
//                holder.ivFlag.setImageResource(R.drawable.flag_france)
//            }
//
//            "JP" -> {
//                holder.ivFlag.setImageResource(R.drawable.flag_japan)
//            }
//
//            "AU" -> {
//                holder.ivFlag.setImageResource(R.drawable.flag_australia)
//            }
//
//            "SG" -> {
//                holder.ivFlag.setImageResource(R.drawable.flag_singapore)
//            }
        }




        holder.itemView.setOnClickListener {

            onItemClickListener?.onItemClick(vpnInfoEntity, position)
        }

    }

    override fun getItemCount(): Int {
       return serverList.size
    }


}