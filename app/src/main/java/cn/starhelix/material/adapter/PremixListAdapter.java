package cn.starhelix.material.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

import cn.starhelix.material.R;
import cn.starhelix.material.entity.MaterialItem;

public class PremixListAdapter extends RecyclerView.Adapter<PremixListAdapter.VH> {
    private List<MaterialItem> itemList;

    private Context context;

    private OnItemClickListener listener;

    public PremixListAdapter(List<MaterialItem> itemList, Context context) {
        this.itemList = itemList;
        this.context = context;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public PremixListAdapter.VH onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.adapter_premix_list_item, viewGroup, false);
        return new PremixListAdapter.VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PremixListAdapter.VH holder, int position) {
        MaterialItem itemInfo = itemList.get(position);

        holder.idView.setText(String.format(Locale.CHINA, "ID：%s", itemInfo.id));
        holder.nameView.setText(String.format(Locale.CHINA, "名称：%s", itemInfo.name));

        if (this.listener != null) {
            holder.topPanel.setOnClickListener(view -> listener.onItemClick(position, holder.topPanel));
        }
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public static class VH extends RecyclerView.ViewHolder {
        public View topPanel;
        public TextView idView;
        public TextView nameView;

        public VH(View v) {
            super(v);
            topPanel = v.findViewById(R.id.topPanel);
            idView = v.findViewById(R.id.idView);
            nameView = v.findViewById(R.id.nameView);
        }
    }
}