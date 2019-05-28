package com.wotosts.bleadvertisingtest.ble.server;

import android.bluetooth.BluetoothDevice;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.ViewGroup;


import com.wotosts.bleadvertisingtest.databinding.ListitemBluetoothDeviceBinding;

import java.util.ArrayList;
import java.util.List;

public class LeDeviceListAdapter extends RecyclerView.Adapter<LeDeviceListAdapter.ItemViewHolder> {

    List<BluetoothDevice> resultList;
    ItemClickListener listener;

    public interface ItemClickListener {
        void onItemClicked(BluetoothDevice device);
    }

    public LeDeviceListAdapter(ItemClickListener listener) {
        resultList = new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
        ListitemBluetoothDeviceBinding binding = ListitemBluetoothDeviceBinding.inflate(inflater, viewGroup, false);

        return new ItemViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder itemViewHolder, int i) {
        itemViewHolder.onBind(resultList.get(i), listener);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return resultList.size();
    }

    public void addDevice(BluetoothDevice device) {
        boolean contain = false;

        if(resultList.contains(device))
            contain = true;
        else {
            for(BluetoothDevice device1 : resultList)
                if(device1.getAddress().equals(device.getAddress()))
                    contain = true;
        }

        if(!contain)
            resultList.add(device);
    }

    public void removeDevice(BluetoothDevice device) {
        resultList.remove(device);
    }

    public void clear() {
        resultList.clear();
    }

    class ItemViewHolder extends ViewHolder{
        ListitemBluetoothDeviceBinding binding;

        ItemViewHolder(ListitemBluetoothDeviceBinding binding) {
            super(binding.getRoot());

            this.binding = binding;
        }

        void onBind(BluetoothDevice device, ItemClickListener listener) {
            binding.setDevice(device);
            binding.setListener(listener);
            /*try {
                binding.setData(new String(result.getScanRecord().getServiceData(BLEUtils.Service_UUID), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }*/
        }
    }
}
