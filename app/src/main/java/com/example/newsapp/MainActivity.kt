package com.example.newsapp

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NewsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize the adapter with an empty list
        adapter = NewsAdapter(emptyList()) // Start with empty list
        recyclerView.adapter = adapter

        // Fetch news headlines
        fetchNewsHeadlines()
    }

    private fun fetchNewsHeadlines() {
        val url = "https://newsapi.org/v2/top-headlines?country=us&apiKey=7ef64e41e1bf4199907937caf03db3f3" // Replace with your API key

        // Fetching the news in a background thread
        Thread {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.88 Safari/537.36")
                connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5")
                connection.setRequestProperty("Referer", "https://newsapi.org/")
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val responseCode = connection.responseCode
                Log.d("API Response Code", responseCode.toString()) // Log the response code

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d("API Response", response) // Log the response

                    // Parse JSON response
                    val responseType = object : TypeToken<NewsApiResponse>() {}.type
                    val apiResponse: NewsApiResponse = Gson().fromJson(response, responseType)

                    // Get the list of articles
                    val newsList = apiResponse.articles.take(6) // Limit to first 4 items

                    runOnUiThread {
                        adapter.updateData(newsList.map { it.title })
                    }
                } else {
                    val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() }
                    Log.e("API Error", "Response code: $responseCode, Error message: $errorResponse")
                }
            } catch (e: Exception) {
                Log.e("Network Error", "Error fetching news: ${e.message}")
                e.printStackTrace()
            }
        }.start()
    }

    // Data classes for parsing the API response
    data class NewsApiResponse(val status: String, val totalResults: Int, val articles: List<NewsItem>)
    data class NewsItem(val source: Source, val author: String?, val title: String, val description: String?,
                        val url: String, val urlToImage: String?, val publishedAt: String?, val content: String?) {
        data class Source(val id: String?, val name: String)
    }
}
