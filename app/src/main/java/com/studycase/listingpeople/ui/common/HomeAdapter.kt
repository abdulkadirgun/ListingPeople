package com.studycase.listingpeople.ui.common

import Person
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.studycase.listingpeople.R
import com.studycase.listingpeople.databinding.PersonRowItemBinding

class HomeAdapter(
    private var items: ArrayList<Person>,
    private val mContext: Context
) : RecyclerView.Adapter<HomeAdapter.PersonViewHolder>() {


    fun updateList(newList : ArrayList<Person>){
        val startPos = items.size
        val newIDs = arrayListOf<Person>()

        /**
         * to prevent adding ids that are the same as existing ids
         * */
        newList.distinctBy { it.id }.forEach { new->
            var same = false
            items.forEach { old->
                if (old.id == new.id)
                    same = true
            }
            if (!same)
                newIDs.add(new)
        }

        items.addAll(newIDs)
        notifyItemRangeInserted(startPos, newIDs.size)
    }

    fun refreshList(newList : ArrayList<Person>){
        items.clear()
        /**
         * prevent same ids
         * */
        items.addAll(newList.distinctBy { it.id })
        notifyDataSetChanged()
    }

    inner class PersonViewHolder(val binding: PersonRowItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Person) {
            binding.apply {
                personTv.text = mContext.getString(R.string.name_and_id,item.fullName, item.id)
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