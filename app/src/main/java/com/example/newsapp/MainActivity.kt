package com.example.newsapp

import android.content.Intent
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

//    7ef64e41e1bf4199907937caf03db3f3
private lateinit var recyclerView: RecyclerView
private lateinit var adapter: NewsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Allow network on main thread (for testing only, not recommended for production)
        fetchNewsHeadlines()
    }

    private fun fetchNewsHeadlines() {
        val url = "https://newsapi.org/v2/top-headlines?country=us&apiKey=7ef64e41e1bf4199907937caf03db3f3"

        val newsList: List<NewsItem> = try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()
                Log.d("API Response", response) // Log response to check the data

                // Parse JSON and extract articles
                val responseType = object : TypeToken<NewsApiResponse>() {}.type
                Gson().fromJson<NewsApiResponse>(response, responseType).articles
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }

        // Set adapter with data
        adapter = NewsAdapter(newsList)
        recyclerView.adapter = adapter
    }

    data class NewsApiResponse(val articles: List<NewsItem>)
    data class NewsItem(val title: String)

}