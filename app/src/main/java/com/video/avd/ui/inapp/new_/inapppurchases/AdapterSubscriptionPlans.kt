package com.video.avd.ui.inapp.new_.inapppurchases

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.video.avd.R
import com.video.avd.databinding.ItemSubscriptionPlanBinding


interface SubscriptionPlanCallback {
    fun onPlanClick(position: Int, customInAppModel: CustomInAppModel)
}

class AdapterSubscriptionPlans constructor(
    val context: Context,
    private val subscriptionPlanCallback: SubscriptionPlanCallback
) : ListAdapter<CustomInAppModel, RecyclerView.ViewHolder>(SubscriptionPlanDiffCallback()) {

    private var selectedItemPosition: Int = 1

    fun unselectPlan() {
        selectedItemPosition = 0
        notifyDataSetChanged()
    }

    fun selectPlan(position: Int) {
        selectedItemPosition = position
        notifyDataSetChanged()
    }

    fun getSelectedPosition(): Int {
        return selectedItemPosition
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolder(
            ItemSubscriptionPlanBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ),
            subscriptionPlanCallback,
            context
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        (holder as ViewHolder).bind(item, position, selectedItemPosition)
    }

    class ViewHolder(
        private val binding: ItemSubscriptionPlanBinding,
        subscriptionPlanCallback: SubscriptionPlanCallback,
        val context: Context
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.setClickListener {
                binding.productDetails?.let { plan ->
                    subscriptionPlanCallback.onPlanClick(adapterPosition, plan)
                }
            }
        }

        fun bind(item: CustomInAppModel, position: Int, selectedItemPosition: Int) {
            highlightItemAt(
                binding = binding,
                position = position,
                selectedItemPosition = selectedItemPosition
            )
            binding.apply {
                productDetails = item
            }
        }
        private fun highlightItemAt(
            binding: ItemSubscriptionPlanBinding,
            position: Int,
            selectedItemPosition: Int
        ) {

//            binding.itemLayout.setBackgroundColor(
//                if (selectedItemPosition == position) ContextCompat.getColor(
//                    context,
//                    R.color.colorIAPHighlight
//                ) else ContextCompat.getColor(
//                    context,
//                    R.color.colorIAPLight
//                )
//            )

            binding.itemLayout.background =
                if (selectedItemPosition == position) ContextCompat.getDrawable(
                    context, R.drawable.background_selected_round_btn
                ) else ContextCompat.getDrawable(
                    context, R.drawable.background_un_selected_round_btn
                )
        }
    }
}

class SubscriptionPlanDiffCallback : DiffUtil.ItemCallback<CustomInAppModel>() {

    override fun areItemsTheSame(
        oldItem: CustomInAppModel,
        newItem: CustomInAppModel
    ): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: CustomInAppModel,
        newItem: CustomInAppModel
    ): Boolean {
        return oldItem == newItem
    }
}