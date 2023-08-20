package com.studycase.listingpeople.ui.screens

import DataSource
import FetchCompletionHandler
import FetchError
import FetchResponse
import android.util.Log
import androidx.lifecycle.ViewModel
import com.studycase.listingpeople.util.Constants
import com.studycase.listingpeople.util.Constants.RESPONSE
import com.studycase.listingpeople.util.Constants.RETRY_DELAY
import com.studycase.listingpeople.util.Resource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainActivityViewModel(
    private val dataSource: DataSource
) : ViewModel(){

    private val _pageState = MutableStateFlow<Resource<Any>?>(null)
    var pageState = _pageState.asStateFlow()

    private val _paginationProgressState = MutableStateFlow<Boolean?>(null)
    var paginationProgressState = _paginationProgressState.asStateFlow()

    private val _isSwiping = MutableStateFlow<Boolean?>(null)
    var isSwiping = _isSwiping.asStateFlow()


    var requestJob :Job? = null
    var retryJob :Job? = null
    private var currentRetry = 0
    private var currentPage :String? = null

    init {
        fetchPeople()
    }

    fun fetchPeople(next : String? = null, isPaginating :Boolean = false, isSwiping: Boolean = false){
        Log.d(RESPONSE, "fetchPeople called")
        requestJob?.cancel()
        requestJob = CoroutineScope(Dispatchers.IO).launch {

            if (isPaginating)
                _paginationProgressState.update { true }
            else
                _pageState.update { Resource.Loading() }

            dataSource.fetch(next = next, object : FetchCompletionHandler {
                override fun invoke(response: FetchResponse?, error: FetchError?) {
                    if (isPaginating)
                        _paginationProgressState.update { false }

                    if (response != null) {
                        currentPage = response.next
                        _pageState.update { Resource.Success(response) }
                        _isSwiping.update { isSwiping }
                    }
                    else
                        retryRequest(error)
                }
            })
        }
    }


    fun retryRequest(error: FetchError?) {
        /**
         * retry logic (time-frequency)
         * */
        retryJob = CoroutineScope(Dispatchers.IO).launch {
            delay(RETRY_DELAY)
            if(currentRetry < Constants.RETRY_LIMIT) {
                currentRetry++
                fetchPeople(currentPage)
            }
            else
                _pageState.update {  Resource.Error(error?.errorDescription) }
        }
    }


    override fun onCleared() {
        super.onCleared()
        requestJob?.cancel()
        retryJob?.cancel()
    }

}