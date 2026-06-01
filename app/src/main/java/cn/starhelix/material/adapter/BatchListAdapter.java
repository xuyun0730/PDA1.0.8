package cn.starhelix.material.adapter;

//调度器：
//当前文件作用：数什么、取什么、发送什么


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
import cn.starhelix.material.entity.BatchItem;


/**
 * 物料批次列表适配器 (BatchListAdapter)
 * * 架构设计推导：
 * 1. 继承自 RecyclerView.Adapter，并传入自定义的 ViewHolder (BatchListAdapter.VH) 作为泛型约束。
 * 2. 职责解耦：适配器只负责【数据到视图的渲染】以及【点击事件的分发】，不参与具体业务逻辑处理。
 */
public class BatchListAdapter extends RecyclerView.Adapter<BatchListAdapter.VH> {

    // 1. 【数据源】批次列表数据集合。由外层的 Activity 或 Fragment 传入，是列表展现的物质基础。
    private List<BatchItem> itemList;

    // 2. 【上下文】主要用于在 onCreateViewHolder 中调用 LayoutInflater 获取布局充气器
    private Context context;

    // 3. 【回调接口变量】持有外层页面的点击监听器引用，当列表项被点击时通过它通知外层页面
    private OnItemClickListener listener;

    /**
     * 构造函数：初始化适配器，注入核心数据源与上下文
     * @param itemList 从后台或者本地数据库查出来的批次数据列表
     * @param context 当前页面的上下文环境
     */
    public BatchListAdapter(List<BatchItem> itemList, Context context) {
        this.itemList = itemList;
        this.context = context;
    }

    /**
     * 提供给外层页面（Activity/Fragment）调用的方法，用于设置点击事件的回调
     * @param listener 实现了 OnItemClickListener 接口的具体匿名内部类或 Lambda 表达式
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    /**
     * 核心步骤一：【创建视图壳子】
     * 触发时机：当 RecyclerView 需要展示一个新条目，且缓存池里没有可复用的旧壳子时，此方法会被触发。
     * @param viewGroup 代表整个列表的父容器（RecyclerView 本身）
     * @param viewType 当列表包含多种不同样式的布局时使用，这里只有一种批次样式，可忽略
     * @return 返回一个装载了 XML 视图布局的全新 ViewHolder 对象
     */
    @NonNull
    @Override
    public BatchListAdapter.VH onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        // LayoutInflater.from(...).inflate 的作用是：把一个独立的 XML 布局文件（R.layout.adapter_batch_list_item）
        // 实例化翻译成 Android 运行时的真正 View 对象。
        // attachToRoot 必须传 false，因为条目什么时候挂载到 RecyclerView 上是由 LayoutManager 控制的，不能提早挂载。
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.adapter_batch_list_item, viewGroup, false);

        // 把做好的 View 壳子包进 ViewHolder 里返出去
        return new BatchListAdapter.VH(v);
    }

    /**
     * 核心步骤二：【绑定/填充数据】（此方法调用频率极高，不能做耗时操作）
     * 触发时机：当一个列表项准备滑入屏幕、需要展示真实内容时。
     * 推导：复用机制的关键就在这里！holder（视图壳子）是复用的，但 position（位置）是全新的。
     * @param holder 无论是新创建的还是从缓存池捞出来的 ViewHolder 实例
     * @param position 当前准备渲染的条目在 List 集合中的索引位置（0, 1, 2...）
     */
    @Override
    public void onBindViewHolder(@NonNull BatchListAdapter.VH holder, int position) {
        // 1. 根据当前滑入的行数位置，从数据集合中捞出对应的实体类对象
        BatchItem itemInfo = itemList.get(position);

        // 2. 【数据搬运】还记得你的第一个问题吗？
        // 这里的 holder.idView 就是通过 XML 中的 id 找到的 TextView 控件。
        // 通过 String.format 格式化字符串，把 itemInfo 里的真实数据填入对应的 View 控件中渲染出来。

        holder.idView.setText(String.format(Locale.CHINA, "批次ID：%s", itemInfo.id));
        holder.batchView.setText(String.format(Locale.CHINA, "批次号：%s", itemInfo.batch));
        holder.outProductView.setText(String.format(Locale.CHINA, "产出品：%s", itemInfo.outProductName));

        // 3. 【点击事件分发】如果外层页面设置了点击监听器
        if (this.listener != null) {
            // 当用户在屏幕上点击了这整块条目（topPanel 面板）时
            holder.topPanel.setOnClickListener(view -> {
                // 触发回调，把当前点击的行数（position）和被点击的布局视图（topPanel）回传给外层页面。
                // 这样外层 Activity 就能知道用户点击的是哪一个批次，从而进行详情页跳转。
                listener.onItemClick(position, holder.topPanel);
            });
        }
    }

    /**
     * 核心步骤三：【告诉系统总长度】
     * RecyclerView 需要知道一共有多少条数据，从而计算出手机右侧滚动条应该缩到多短、列表能往下滚多深。
     * @return 数据源集合的长度大小
     */
    @Override
    public int getItemCount() {
        // 健壮性防空保护：如果集合为 null 则返回 0，否则返回真实大小
        return itemList == null ? 0 : itemList.size();
    }

    /**
     * 🛠️ 自定义 ViewHolder (视图持有者)
     * * 理论推导：
     * 传统的 ListView 每次显示条目都要在底层调用 findViewById() 遍历整棵 XML 树，性能极低。
     * ViewHolder 的核心目的就是：在壳子第一次被创建时，就把 XML 里的所有子控件一网打尽、找出来存在自己的公共属性里。
     * 以后复用时，直接拿属性赋值，**实现 findViewById() 的零次重复调用**，极大节省 CPU。
     */
    public static class VH extends RecyclerView.ViewHolder {
        // 声明每一行布局中包含的子控件引用
        public View topPanel;             // 整个列表项的最外层根/大面板布局（用于设置整行点击）
        public TextView idView;           // 显示批次 ID 的文本控件
        public TextView batchView;        // 显示批次号的文本控件
        public TextView outProductView;   // 显示产出品名称的文本控件

        /**
         * 构造方法：在实例化时，顺藤摸瓜将所有的子控件引用死死绑在身上
         * @param v 即 R.layout.adapter_batch_list_item 对应的根视图实例
         */
        public VH(View v) {
            super(v); // 必须调用父类构造，将视图传给底层存储

            // 【前后端桥梁】利用之前讲过的 id 机制，把 XML 布局和 Java 变量进行映射绑定
            topPanel = v.findViewById(R.id.topPanel);
            idView = v.findViewById(R.id.idView);
            batchView = v.findViewById(R.id.batchView);
            outProductView = v.findViewById(R.id.outProductView);
        }
    }

    /**
     * 🔗 【自定义点击事件回调接口】（由于你原本的代码中漏掉了，我帮你补充在这里）
     * 作用：解耦。Adapter 自己不应该决定点击后是“跳页面”还是“弹窗”，它只需负责把点击信号发射给 Activity 即可。
     */
    public interface OnItemClickListener {
        /**
         * 条目点击时触发
         * @param position 被点击的条目在列表中的行数索引（从 0 开始）
         * @param view     当前被点击的视图根布局
         */
        void onItemClick(int position, View view);
    }
}
