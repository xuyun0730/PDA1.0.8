package cn.starhelix.material.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import cn.starhelix.material.R;
import cn.starhelix.material.entity.FlowDetail;
import cn.starhelix.material.entity.MaterialItem;
import cn.starhelix.material.util.StrUtil;

public class MaterialListAdapter extends RecyclerView.Adapter<MaterialListAdapter.VH> {
    private static final String TAG = "MaterialListAdapter";
    private List<MaterialItem> itemList;

    private Context context;

    private OnItemClickListener listener;

    public MaterialListAdapter(List<MaterialItem> itemList, Context context) {
        this.itemList = itemList;
        this.context = context;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.adapter_material_list_item, viewGroup, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        MaterialItem itemInfo = itemList.get(position);

        holder.materialIdView.setText(String.format(Locale.CHINA, "物料ID：%s", itemInfo.id));
        holder.materialNameView.setText(String.format(Locale.CHINA, "物料名称：%s", itemInfo.name));

        holder.inputAmountView.setText(String.format(Locale.CHINA, "已投重量：%s", itemInfo.inputAmount));
        holder.totalMinAmtView.setText(String.format(Locale.CHINA, "最小重量：%s", itemInfo.netContentAllowGt));
        holder.totalMaxAmtView.setText(String.format(Locale.CHINA, "最大重量：%s", itemInfo.netContentAllowLt));
        holder.unitNameView.setText(String.format(Locale.CHINA, "单位名称：%s", itemInfo.unitName));

        String minStr = StrUtil.isEmpty(itemInfo.netContentAllowGt) ? "0" : itemInfo.netContentAllowGt;
        String maxStr = StrUtil.isEmpty(itemInfo.netContentAllowLt) ? "0" : itemInfo.netContentAllowLt;
        if (itemInfo.inputAmount.compareTo(new BigDecimal(minStr)) >= 0 &&
                itemInfo.inputAmount.compareTo(new BigDecimal(maxStr)) <= 0) {
            holder.statusView.setVisibility(View.VISIBLE);
        } else {
            holder.statusView.setVisibility(View.INVISIBLE);
        }

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
        public TextView materialIdView;
        public TextView materialNameView;
        public ImageView statusView;
        public TextView inputAmountView;
        public TextView totalMinAmtView;
        public TextView totalMaxAmtView;
        public TextView unitNameView;

        public VH(View v) {
            super(v);
            topPanel = v.findViewById(R.id.topPanel);
            materialIdView = v.findViewById(R.id.materialIdView);
            materialNameView = v.findViewById(R.id.materialNameView);
            statusView = v.findViewById(R.id.statusView);
            inputAmountView = v.findViewById(R.id.inputAmountView);
            totalMinAmtView = v.findViewById(R.id.totalMinAmtView);
            totalMaxAmtView = v.findViewById(R.id.totalMaxAmtView);
            unitNameView = v.findViewById(R.id.unitNameView);
        }
    }
}
