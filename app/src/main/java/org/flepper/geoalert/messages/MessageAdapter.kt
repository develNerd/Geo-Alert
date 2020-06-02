package org.flepper.geoalert.messages

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.flepper.geoalert.R

class MessageAdapter(private val mCtx: Context, private val MessageList: List<MessageObject>) :
    RecyclerView.Adapter<MessageAdapter.MyViewHolder>() {

    private val inflater: LayoutInflater


    init {

        inflater = LayoutInflater.from(mCtx)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageAdapter.MyViewHolder {

        val view = inflater.inflate(R.layout.message_item, parent, false)

        return MyViewHolder(view)


    }

    override fun onBindViewHolder(holder: MessageAdapter.MyViewHolder, position: Int) {

        val product = MessageList[position]


        holder.message_user.text = product.message_user
        holder.message_text.text = product.message_text
        holder.message_time.text = product.message_time



    }

    override fun getItemCount(): Int {
        return MessageList.size
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var message_text: TextView
        var message_user: TextView
        var message_time: TextView




        init {

            message_text = itemView.findViewById(R.id.message_text) as TextView
            message_user = itemView.findViewById(R.id.message_user) as TextView
            message_time = itemView.findViewById(R.id.message_time) as TextView


        }

    }



}