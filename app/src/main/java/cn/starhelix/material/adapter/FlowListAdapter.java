package cn.starhelix.material.adapter;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import cn.starhelix.material.R;
import cn.starhelix.material.entity.FlowDetail;

public class FlowListAdapter extends RecyclerView.Adapter<FlowListAdapter.VH> {
    private List<FlowDetail> itemList;

    private Context context;

    private OnItemClickListener listener;

    public FlowListAdapter(List<FlowDetail> itemList, Context context) {
        this.itemList = itemList;
        this.context = context;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.adapter_flow_list_item, viewGroup, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        FlowDetail itemInfo = itemList.get(position);

        holder.materialView.setText(String.format(Locale.CHINA, "物料：%s", itemInfo.material));

        if (itemInfo.status == 3) {
            holder.statusView.setTextColor(ContextCompat.getColor(context, R.color.success_green));
            holder.statusView.setText("已完成");
        } else if (itemInfo.status == 4) {
            holder.statusView.setTextColor(ContextCompat.getColor(context, R.color.fail_red));
            holder.statusView.setText("异常");
        } else {
            holder.statusView.setTextColor(ContextCompat.getColor(context, R.color.processing_orange));
            holder.statusView.setText("进行中");
        }

        holder.inputAmountView.setText(String.format(Locale.CHINA, "已投数量：%.2f %s", itemInfo.input_amount, itemInfo.unit));
        holder.totalAmountView.setText(String.format(Locale.CHINA, "计划数量：%.2f %s", itemInfo.total, itemInfo.unit));

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
        public TextView materialView;
        public TextView statusView;
        public TextView inputAmountView;
        public TextView totalAmountView;

        public VH(View v) {
            super(v);
            topPanel = v.findViewById(R.id.topPanel);
            materialView = v.findViewById(R.id.materialView);
            statusView = v.findViewById(R.id.statusView);
            inputAmountView = v.findViewById(R.id.inputAmountView);
            totalAmountView = v.findViewById(R.id.totalAmountView);
        }
    }
}
