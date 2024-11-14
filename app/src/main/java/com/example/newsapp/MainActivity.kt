package com.example.newsapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.squareup.picasso.Picasso
import org.chromium.net.CronetEngine
import org.chromium.net.UrlRequest
import org.chromium.net.UrlResponseInfo
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.nio.ByteBuffer
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NewsAdapter
    private lateinit var cronetEngine:CronetEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        // Initialize the adapter with an empty list
        adapter = NewsAdapter(emptyList()) // Start with empty list

        // Cronet Engine Initialization
        cronetEngine = CronetEngine.Builder(this).build()


        // Fetch news headlines
        fetchNewsHeadlines()
    }

    private fun fetchNewsHeadlines() {
        val url = "https://newsapi.org/v2/everything?q=NBA-AND-NFL-AND-WNBA&apiKey=7ef64e41e1bf4199907937caf03db3f3" // Replace with your API key

        // Use Cronet to make a request
        // Build and start a Cronet request
        val requestBuilder = cronetEngine.newUrlRequestBuilder(
            url,
            MyUrlRequestCallback { response ->
                response?.let {
                    processResponse(it)
                }
            },
            Executors.newSingleThreadExecutor()
        )
        requestBuilder.build().start()
    }

    private fun processResponse(response: String) {
        // Clean non-printable characters
        val nonPrintableRegex = "[^\\x20-\\x7E]".toRegex()
        val cleanedResponse = response.replace(nonPrintableRegex, "")

        // Parse JSON response
        val responseType = object : TypeToken<NewsApiResponse>() {}.type
        val apiResponse: NewsApiResponse = Gson().fromJson(cleanedResponse, responseType)

        // Filter articles with images and pick a random one
        val sportArticles = apiResponse.articles.filter { it.urlToImage != null }
        if (sportArticles.isNotEmpty()) {
            val randomArticle = sportArticles.random()

            // Save image and content for caching
            saveArticleData(randomArticle)
            displayArticle(randomArticle)
        }
    }

    private fun saveArticleData(article: NewsItem) {
        val contentFile = File(filesDir, "cached_article.txt")
        val imageFile = File(filesDir, "cached_image.jpg")
        val timestampFile = File(filesDir, "last_retrieval_timestamp.txt")

        // Save article content
        contentFile.writeText(article.content ?: "")

        // Save image from URL
        article.urlToImage?.let { urlToImage ->
            Thread {
                try {
                    val bitmap = BitmapFactory.decodeStream(URL(urlToImage).openStream())
                    FileOutputStream(imageFile).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()
        }

        // Save current timestamp
        timestampFile.writeText(System.currentTimeMillis().toString())
    }

    private fun displayCachedArticleIfValid() {
        val timestampFile = File(filesDir, "last_retrieval_timestamp.txt")
        if (timestampFile.exists()) {
            val lastRetrievedTime = timestampFile.readText().toLongOrNull() ?: 0L
            val currentTime = System.currentTimeMillis()

            if (currentTime - lastRetrievedTime < 10 * 1000) { // 10 seconds expiration
                val contentFile = File(filesDir, "cached_article.txt")
                val imageFile = File(filesDir, "cached_image.jpg")

                if (contentFile.exists() && imageFile.exists()) {
                    val articleContent = contentFile.readText()
                    val articleImage = BitmapFactory.decodeFile(imageFile.path)
                    displayArticleContent(articleContent, articleImage)
                    return
                }
            }
        }
        fetchNewsHeadlines()
    }

    private fun displayArticleContent(content: String, image: Bitmap?) {
        findViewById<TextView>(R.id.textView).text = content
        findViewById<ImageView>(R.id.imageView).setImageBitmap(image)
    }

    private fun displayArticle(article: NewsItem) {
        val titleTextView = findViewById<TextView>(R.id.textView)
        val imageView = findViewById<ImageView>(R.id.imageView)

        if (titleTextView != null && imageView != null) {
            titleTextView.text = article.content
            article.urlToImage?.let { Picasso.get().load(article.urlToImage).into(imageView)      }
        } else {
            Log.e("MainActivity", "Title text view or image view giving null")
        }
    }

    data class NewsApiResponse(val status: String, val totalResults: Int, val articles: List<NewsItem>)
    data class NewsItem(val source: Source, val author: String?, val title: String, val description: String?,
                        val url: String, val urlToImage: String?, val publishedAt: String?, val content: String?) {
        data class Source(val id: String?, val name: String)
    }

    inner class MyUrlRequestCallback(private val callback: (String?) -> Unit) : UrlRequest.Callback() {
        private val myBuffer: ByteBuffer = ByteBuffer.allocateDirect(102400)
        private val responseBuilder = StringBuilder()

        override fun onRedirectReceived(request: UrlRequest?, info: UrlResponseInfo?, newLocationUrl: String?) {
            request?.followRedirect()
        }

        override fun onResponseStarted(request: UrlRequest?, info: UrlResponseInfo?) {
            request?.read(myBuffer)
        }

        override fun onReadCompleted(request: UrlRequest?, info: UrlResponseInfo?, byteBuffer: ByteBuffer?) {
            byteBuffer?.flip()
            val response = byteBuffer?.let {
                val byteArray = ByteArray(it.remaining())
                it.get(byteArray)
                String(byteArray)
            }
            responseBuilder.append(response)
            myBuffer.clear()
            request?.read(myBuffer)
//            callback(response)
        }

        override fun onSucceeded(request: UrlRequest?, info: UrlResponseInfo?) {
            callback(responseBuilder.toString())
            Log.d("MyUrlRequestCallback", "Request succeeded")
        }

        override fun onFailed(request: UrlRequest?, info: UrlResponseInfo?, error: org.chromium.net.CronetException?) {
            Log.e("MyUrlRequestCallback", "Request failed: ${error?.message}")
            callback(null)
        }
    }
}