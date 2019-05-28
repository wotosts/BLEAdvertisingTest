package com.wotosts.bleadvertisingtest.ble.client;

import android.bluetooth.le.ScanResult;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.wotosts.bleadvertisingtest.ble.BLEUtils;
import com.wotosts.bleadvertisingtest.databinding.ListitemBluetoothBinding;

import java.util.ArrayList;
import java.util.List;

public class LeResultListAdapter extends RecyclerView.Adapter<LeResultListAdapter.ItemViewHolder> {

    List<ScanResult> resultList;
    ItemClickListener listener;
    ScanResult connected;

    public interface ItemClickListener {
        void onItemClicked(ScanResult result);
    }

    public LeResultListAdapter(ItemClickListener listener) {
        resultList = new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
        ListitemBluetoothBinding binding = ListitemBluetoothBinding.inflate(inflater, viewGroup, false);

        return new ItemViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder itemViewHolder, int i) {
        if (resultList.get(i).equals(connected)) {
            itemViewHolder.onBind(resultList.get(i), listener, true);
        } else {
            itemViewHolder.onBind(resultList.get(i), listener, false);
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return resultList.size();
    }

    public void setConnected(ScanResult scanResult) {
        connected = scanResult;
    }

    public void addResult(ScanResult scanResult) {
        boolean contain = false;
        List<ScanResult> removeList = new ArrayList<>();

        if (resultList.contains(scanResult))
            contain = true;
        else {
            for (ScanResult result : resultList) {
                if (result.getDevice().getAddress().equals(scanResult.getDevice().getAddress())) {
                    contain = true;

                    if (result.getRssi() < scanResult.getRssi()) {
                        contain = false;
                        removeList.add(result);
                    }
                }
            }
        }

        if (!contain) {
            resultList.removeAll(removeList);
            resultList.add(scanResult);
        }
    }

    public List<ScanResult> getResultList() {
        return resultList;
    }

    public void clear() {
        resultList.clear();
        notifyDataSetChanged();
    }

    class ItemViewHolder extends ViewHolder {
        ListitemBluetoothBinding binding;

        ItemViewHolder(ListitemBluetoothBinding binding) {
            super(binding.getRoot());

            this.binding = binding;
        }

        void onBind(ScanResult result, ItemClickListener listener, boolean connected) {
            binding.setResult(result);
            binding.setListener(listener);
            binding.setConnected(connected);

            binding.setDistance(BLEUtils.PROXIMITY_UNKNOWN);

            float accuracy = BLEUtils.calculateAccuracy(-70, result.getRssi());
            binding.setDistance(BLEUtils.calculateProximity(accuracy));
        }

    }
}
