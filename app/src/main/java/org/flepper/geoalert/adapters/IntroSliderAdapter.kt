package org.flepper.geoalert.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.flepper.geoalert.R
import org.flepper.geoalert.models.IntroSlide


class IntroSliderAdapter(private val  introSlides: List<IntroSlide>):
    RecyclerView.Adapter<IntroSliderAdapter.IntroSlideViewHolder>(){


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IntroSlideViewHolder {

        return IntroSlideViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.slide_item_container,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {

        return introSlides.size
    }

    override fun onBindViewHolder(holder: IntroSlideViewHolder, position: Int) {

        holder.bind(introSlides[position])
    }

    inner class IntroSlideViewHolder(view:View): RecyclerView.ViewHolder(view){


        private val imageIcon = view.findViewById<ImageView>(R.id.ImageSlideIcon)

        fun bind(introSlide: IntroSlide){

            imageIcon.setImageResource(introSlide.icon)
        }


    }



}