package com.studycase.listingpeople.ui.common

import Person
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.studycase.listingpeople.databinding.PersonRowItemBinding
import com.studycase.listingpeople.databinding.ProgressRowItemBinding

class HomeAdapter(
    private var items: ArrayList<Person>,
    private val mContext: Context
) : RecyclerView.Adapter<HomeAdapter.PersonViewHolder>() {

    inner class PersonViewHolder(val binding: PersonRowItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Person) {
            binding.apply {
                personTv.text = item.fullName + " (" +item.id + ")"
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeAdapter.PersonViewHolder {
        return PersonViewHolder(
                PersonRowItemBinding.inflate(
                    LayoutInflater.from(mContext),
                    parent,
                    false
                )
            )
    }

    override fun onBindViewHolder(holder: HomeAdapter.PersonViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

}