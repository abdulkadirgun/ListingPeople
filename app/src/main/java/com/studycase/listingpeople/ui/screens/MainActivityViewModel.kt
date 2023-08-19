package com.studycase.listingpeople.ui.screens

import DataSource
import FetchCompletionHandler
import FetchError
import FetchResponse
import Person
import android.util.Log
import androidx.lifecycle.ViewModel
import com.studycase.listingpeople.util.Constants
import com.studycase.listingpeople.util.Resource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainActivityViewModel(
    private val dataSource: DataSource
) : ViewModel(){

    private val _pageState = MutableStateFlow<Resource<Any>?>(null)
    var pageState = _pageState.asStateFlow()

    private val _paginationProgressState = MutableStateFlow<Boolean>(false)
    var paginationProgressState = _paginationProgressState.asStateFlow()

    private val _currentIndex = MutableStateFlow<Int>(0)
    var currentIndex = _currentIndex.asStateFlow()


    var theJob :Job? = null
    private var currentRetry = 0
    private var currentPage :String? = null

    init {
        fetchPeople()
    }

    fun fetchPeople(next : String? = null, isPaginating :Boolean = false){
        Log.d("burdayım", "fetchPeople called")
        theJob?.cancel()
        theJob = CoroutineScope(Dispatchers.IO).launch {

            if (isPaginating)
                _paginationProgressState.update { true }
            else
                _pageState.update { Resource.Loading() }

            dataSource.fetch(next = next, object : FetchCompletionHandler {
                override fun invoke(response: FetchResponse?, error: FetchError?) {
                    _paginationProgressState.update { false }
                    //Log.d("burdayım", "response: $response - error: $error ")
                    if (response != null) {
                        currentPage = response.next
                        updateState(response)
                    }
                    else
                        retryRequest(error)
                }
            })
        }
    }

    fun updateState(response: FetchResponse) {
        val currentState = _pageState.value?.data ?: FetchResponse(listOf(), null)
        val oldList = (currentState as FetchResponse).people
        val newList = oldList + response.people
        val newState = FetchResponse(newList, response.next)
        Log.d("burdayım", "old:${oldList.count()} new: ${newList.count()} newState:${newState.people.count()}")
        _currentIndex.update { oldList.count() }
        _pageState.update { Resource.Success(newState) }
    }

    fun retryRequest(error: FetchError?) {
        if(currentRetry < Constants.RETRY_LIMIT) {
            currentRetry++
            fetchPeople(currentPage)
        }
        else
            _pageState.update {  Resource.Error(error?.errorDescription) }
    }


    override fun onCleared() {
        super.onCleared()
        theJob?.cancel()
        theJob = null
    }

}