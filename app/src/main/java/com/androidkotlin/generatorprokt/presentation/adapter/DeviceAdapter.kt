package com.androidkotlin.generatorprokt.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.androidkotlin.generatorprokt.databinding.ItemDeviceBinding
import com.androidkotlin.generatorprokt.domain.model.Device

class DeviceAdapter(
    private val onConnectClick: (String) -> Unit
) : ListAdapter<Device, DeviceAdapter.DeviceViewHolder>(DeviceDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val binding = ItemDeviceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DeviceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DeviceViewHolder(
        private val binding: ItemDeviceBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(device: Device) {
            binding.apply {
                tvDeviceName.text = device.name
                tvDeviceStatus.text = if (device.isConnected) "연결됨" else "연결 안됨"

                btnConnect.isEnabled = !device.isConnected
                btnConnect.text = if (device.isConnected) "연결됨" else "연결"

                btnConnect.setOnClickListener {
                    if (!device.isConnected) {
                        onConnectClick(device.id)
                    }
                }
            }
        }
    }

    private class DeviceDiffCallback : DiffUtil.ItemCallback<Device>() {
        override fun areItemsTheSame(oldItem: Device, newItem: Device): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Device, newItem: Device): Boolean {
            return oldItem == newItem
        }
    }
}