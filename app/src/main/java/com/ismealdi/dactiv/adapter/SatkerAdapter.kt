package com.ismealdi.dactiv.adapter

import android.annotation.SuppressLint
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import com.ismealdi.dactiv.R
import com.ismealdi.dactiv.components.AmButton
import com.ismealdi.dactiv.components.AmTextView
import com.ismealdi.dactiv.fragment.SatkerFragment
import com.ismealdi.dactiv.model.Satker
import kotlinx.android.synthetic.main.fragment_satker.*
import kotlinx.android.synthetic.main.list_satker.view.*

class SatkerAdapter(private var satkers: MutableList<Satker>, private val context: SatkerFragment) : RecyclerView.Adapter<SatkerAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val description: AmTextView = itemView.textDescription
        val name: AmTextView = itemView.textName
        val frame: LinearLayout = itemView.layoutFrame
        val more: RelativeLayout = itemView.layoutAddMore
        val buttonAdd: AmButton = itemView.buttonAdd
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_satker, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val satker = satkers[holder.adapterPosition]

        if(satker.id != "0") {
            holder.more.visibility = View.GONE
            holder.name.setTextFade(satker.name)
            holder.description.setTextFade(satker.description)

            holder.frame.setOnClickListener {
                // TODO
            }

        }else{
            holder.more.visibility = View.VISIBLE
            holder.buttonAdd.setOnClickListener {
                context.buttonAdd.performClick()
            }
        }

    }

    override fun getItemCount(): Int {
        return satkers.size
    }

    fun updateData(mSatkers: MutableList<Satker>) {
        this.satkers.clear()
        this.satkers.add(Satker("0"))
        this.satkers.addAll(mSatkers)

        notifyDataSetChanged()
    }
}