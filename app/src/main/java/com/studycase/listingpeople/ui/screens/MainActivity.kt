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
import com.studycase.listingpeople.util.Constants.PAGINATION
import com.studycase.listingpeople.util.Constants.RESPONSE
import com.studycase.listingpeople.util.Constants.SWIPING
import com.studycase.listingpeople.util.Resource
import com.studycase.listingpeople.util.gone
import com.studycase.listingpeople.util.hide
import com.studycase.listingpeople.util.show
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch



class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var mainActivityViewModel: MainActivityViewModel
    private val dataSource =  DataSource()
    private var isScrolling = false
    private var isLoading = false
    private var isLastPage = false
    private var theNext :String? = null
    private var theAdapter = HomeAdapter(arrayListOf(), this)
    private var isSwipingIsWorking = false

    private var peopleJob :Job? = null
    private var swipingJob :Job? = null
    private var paginationJob :Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val viewModelFactory = MainActivityViewModelFactory(dataSource)
        mainActivityViewModel = ViewModelProvider(this, viewModelFactory)[MainActivityViewModel::class.java]

        setRCV()

        collectData()

        handleSwipeGesture()
    }

    private fun setRCV() {
        binding.mainRcv.apply {
            adapter = theAdapter
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

                    // Calculate the visible item count
                    val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                    val itemCountOnScreen = lastVisibleItemPosition - firstVisibleItemPosition + 1
                    Log.d(PAGINATION, "itemCountOnScreen:$itemCountOnScreen totalItemCount:$totalItemCount ")
                    val isTotalMoreThanVisible = totalItemCount >= itemCountOnScreen


                    val shouldPaginate = isNotLoadingAndNotLastPage && isAtLastItem && isNotAtBeginning &&
                            isTotalMoreThanVisible && isScrolling && !isSwipingIsWorking && dy > 0
                    if(shouldPaginate) {
                        if (theNext != null){
                            mainActivityViewModel.fetchPeople(next = theNext, isPaginating = true)
                        }
                        Log.d(PAGINATION, "fetch new page from server")
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
    }

    private fun handleSwipeGesture() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            isSwipingIsWorking = true
            mainActivityViewModel.fetchPeople(null, isSwiping = true)
        }
    }

    private fun collectData() {
        peopleJob = CoroutineScope(Dispatchers.Main).launch {
            mainActivityViewModel.pageState.collect{ result->
                binding.swipeRefreshLayout.isRefreshing = false
                isSwipingIsWorking = false
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
                                Log.d(RESPONSE, "peopleCount:${result.data.people.count()} ")

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
        paginationJob = CoroutineScope(Dispatchers.Main).launch {
            mainActivityViewModel.paginationProgressState.collect{
                Log.d(PAGINATION, "paginationProgressState: $it")
                it?.let {
                    if (it)
                        binding.paginationProgressBar.show()
                    else
                        binding.paginationProgressBar.gone()
                }
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

            swipingJob = CoroutineScope(Dispatchers.Main).launch {
                mainActivityViewModel.isSwiping.collect{
                    Log.d(SWIPING, "isSwiping: $it")

                    it?.let {
                        if (it)
                            theAdapter.refreshList(result.people as ArrayList<Person>)
                        else
                            theAdapter.updateList(result.people as ArrayList<Person>)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        /**
         * to avoid memory leak. we can use lifecycle library for this. but we did manually in this scenario
         * */
        peopleJob?.cancel()
        swipingJob?.cancel()
        paginationJob?.cancel()
    }
}