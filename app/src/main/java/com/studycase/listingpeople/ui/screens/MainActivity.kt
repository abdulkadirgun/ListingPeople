package com.studycase.listingpeople.ui.screens

import DataSource
import FetchResponse
import Person
import android.os.Bundle
import android.util.Log
import android.widget.AbsListView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.studycase.listingpeople.R
import com.studycase.listingpeople.databinding.ActivityMainBinding
import com.studycase.listingpeople.ui.common.HomeAdapter
import com.studycase.listingpeople.util.Resource
import com.studycase.listingpeople.util.gone
import com.studycase.listingpeople.util.hide
import com.studycase.listingpeople.util.show
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var mainActivityViewModel: MainActivityViewModel
    private val dataSource =  DataSource()
    private var isScrolling = false
    private var isLoading = false
    private var isLastPage = false
    private var theNext :String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val viewModelFactory = MainActivityViewModelFactory(dataSource)
        mainActivityViewModel = ViewModelProvider(this, viewModelFactory)[MainActivityViewModel::class.java]

        collectData()

        handleSwipeGesture()
    }

    private fun handleSwipeGesture() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            mainActivityViewModel.fetchPeople(null)
        }
    }

    private fun collectData() {
        CoroutineScope(Dispatchers.Main).launch {
            mainActivityViewModel.pageState.collect{ result->
                binding.swipeRefreshLayout.isRefreshing = false
                isLoading = false
                result?.let {
                    when(result){
                        is Resource.Error -> {
                            binding.mainRcv.hide()
                            binding.progressBar.hide()
                            binding.errorText.show()
                            binding.errorText.text = result.message
                        }
                        is Resource.Loading -> {
                            binding.mainRcv.hide()
                            binding.progressBar.show()
                            binding.errorText.hide()
                        }
                        is Resource.Success -> {

                            binding.progressBar.hide()
                            binding.errorText.hide()
                            if(result.data is FetchResponse) {
                                Log.d("burdayım", "peopleCount:${result.data.people.count()} ")
                                setData(result.data)
                                result.data.next?.let {
                                    isLastPage = result.data.next == theNext
                                    theNext = result.data.next
                                }
                            }
                        }
                    }
                }

            }
        }

        /**
         * to handle pagination progress bar visibility
         * */
        CoroutineScope(Dispatchers.Main).launch {
            mainActivityViewModel.paginationProgressState.collect{
               if (it)
                   binding.paginationProgressBar.show()
               else
                   binding.paginationProgressBar.gone()
            }
        }
    }



    private fun setData(result: FetchResponse ) {

        /**
         * to handle empty list
         * */
        if(result.people.isEmpty())
        {
            binding.errorText.show()
            binding.errorText.text = getString(R.string.no_data_to_show)
        }
        else{
            binding.mainRcv.show()
            binding.mainRcv.apply {
                val uniqueIDs = result.people.distinctBy { it.id }
                adapter = HomeAdapter((uniqueIDs as ArrayList<Person>), this@MainActivity)
                layoutManager = LinearLayoutManager(this@MainActivity)
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)

                        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                        val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                        val visibleItemCount = layoutManager.childCount
                        val totalItemCount = layoutManager.itemCount

                        val isNotLoadingAndNotLastPage = !isLoading && !isLastPage
                        val isAtLastItem = firstVisibleItemPosition + visibleItemCount >= totalItemCount
                        val isNotAtBeginning = firstVisibleItemPosition >= 0
                        val isTotalMoreThanVisible = totalItemCount >= 20


                        val shouldPaginate = isNotLoadingAndNotLastPage && isAtLastItem && isNotAtBeginning &&
                                isTotalMoreThanVisible && isScrolling
                        if(shouldPaginate) {
                            if (theNext != null)
                                mainActivityViewModel.fetchPeople(next = theNext, isPaginating = true)
                            Log.d("burdayım", "onScrolled: son geldin ")
                            isScrolling = false
                        }
                    }

                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        super.onScrollStateChanged(recyclerView, newState)
                        if(newState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                            isScrolling = true
                        }
                    }
                })
            }

            /**
             *  to handle current index practically
             * */
            CoroutineScope(Dispatchers.Main).launch {
                mainActivityViewModel.currentIndex.collect{ index->
                    Log.d("burdayım", "index: $index ")
                    binding.mainRcv.smoothScrollToPosition(index)
                }
            }
        }

    }
}