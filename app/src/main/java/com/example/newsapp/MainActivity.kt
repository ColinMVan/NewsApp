package com.example.newsapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.Strictness
import com.google.gson.reflect.TypeToken
import org.chromium.net.CronetEngine
import org.chromium.net.CronetException
import org.chromium.net.UrlRequest
import org.chromium.net.UrlResponseInfo
import java.nio.ByteBuffer
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NewsAdapter
    private lateinit var imageView: ImageView
    private lateinit var cronetEngine: CronetEngine
    private val executor: Executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = NewsAdapter(emptyList()) // Start with an empty list
        recyclerView.adapter = adapter

        // Initialize Cronet engine
        cronetEngine = CronetEngine.Builder(this).build()

        // Fetch news headlines
        fetchNewsHeadlines()
    }

    private fun fetchNewsHeadlines() {
        val url =
            "https://newsapi.org/v2/top-headlines?country=us&apiKey=7ef64e41e1bf4199907937caf03db3f3" // API Key goes in this url
        val requestBuilder = cronetEngine.newUrlRequestBuilder(
            url,
            object : UrlRequest.Callback() {
                override fun onSucceeded(request: UrlRequest, info: UrlResponseInfo) {
                    Log.i("Cronet", "Request succeeded")
                }

                override fun onFailed(
                    request: UrlRequest,
                    info: UrlResponseInfo,
                    error: CronetException
                ) {
                    Log.e("Cronet", "Request failed: ${error.message}")
                }

                override fun onResponseStarted(request: UrlRequest, info: UrlResponseInfo) {
                    Log.i("Cronet", "Response started")
                    val buffer = ByteBuffer.allocateDirect(1024)
                    request.read(buffer)
                }

                override fun onRedirectReceived(
                    request: UrlRequest,
                    info: UrlResponseInfo,
                    newLocationUrl: String
                ) {
                    Log.i("Cronet", "Redirect received to: $newLocationUrl")
                    request.followRedirect()
                }

                override fun onReadCompleted(
                    request: UrlRequest,
                    info: UrlResponseInfo,
                    buffer: ByteBuffer
                ) {
                    Log.i("Cronet", "Read completed")
                    buffer.flip()
                    val responseBody = ByteArray(buffer.remaining())
                    buffer.get(responseBody)
                    val responseString = String(responseBody)

                    // Clean non-printable characters
                    val nonPrintableRegex = "[^\\x20-\\x7E]".toRegex()
                    val cleanedString = responseString.replace(nonPrintableRegex, "")

                    // Deserialize JSON response
                    val gson =
                        GsonBuilder().setStrictness(Strictness.LENIENT).create()
                    val apiResponseType = object : TypeToken<NewsApiResponse>() {}.type
                    val apiResponse: NewsApiResponse = gson.fromJson(cleanedString, apiResponseType)

                    // Select a random article from the response
                    val article = apiResponse.articles.randomOrNull()

                    // Limit  the list of articles to the  first 6
                    val newsList = apiResponse.articles.take(6)
                    article?.let {
                        runOnUiThread { displayArticle(it) }
                    }
                }
            },
            executor
        )

        // Start the request
        requestBuilder.build().start()
    }

    private fun displayArticle(article: NewsItem) {
        // Assuming Article has a `title` and `urlToImage` properties
        findViewById<TextView>(R.id.titleTextView).text = article.title

        // Load image from URL
        loadImageFromUrl(article.urlToImage)
    }

    private fun loadImageFromUrl(imageUrl: String?) {
        // Logic to load and cache the image from URL
        imageUrl?.let {
            // Example code that loads the image using an async method (e.g., using Glide or Picasso)
            // This is a placeholder for actual image loading logic
            Log.i("Image", "Loading image from URL: $imageUrl")
        }
    }

    // Data classes for parsing the API response
    data class NewsApiResponse(
        val status: String,
        val totalResults: Int,
        val articles: List<NewsItem>
    )

    data class NewsItem(
        val source: Source, val author: String?, val title: String, val description: String?,
        val url: String, val urlToImage: String?, val publishedAt: String?, val content: String?
    ) {
        data class Source(val id: String?, val name: String)

    }
}