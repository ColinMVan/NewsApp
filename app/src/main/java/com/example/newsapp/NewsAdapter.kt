package com.example.newsapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NewsAdapter(private var newsTitles: List<String>) : RecyclerView.Adapter<NewsAdapter.NewsViewHolder>() {

    inner class NewsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_news, parent, false)
        return NewsViewHolder(view)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        val title = newsTitles[position]
        holder.titleTextView.text = title
    }

    override fun getItemCount() = newsTitles.size

    // Method to update the data and notify the RecyclerView
    fun updateData(newTitles: List<String>) {
        newsTitles = newTitles
        notifyDataSetChanged() // Notify that data has changed
    }
}

