package cn.pylin.xycjd.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import cn.pylin.xycjd.R;

public class LogAdapter extends RecyclerView.Adapter<LogAdapter.LogViewHolder> {
    protected List<String> logs;

    public LogAdapter() {
        this.logs = new ArrayList<>();
    }

    public void setLogs(List<String> logs) {
        this.logs.clear();
        this.logs.addAll(logs);
        notifyDataSetChanged();
    }

    public void addLog(String log) {
        logs.add(log);
        notifyItemInserted(logs.size() - 1);
        // 滚动到最后一个位置的逻辑应该在Activity中处理
    }

    public void clearLogs() {
        logs.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_log, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        String log = logs.get(position);
        holder.bind(log);
    }

    @Override
    public int getItemCount() {
        return logs.size();
    }

    public String getLogAt(int position) {
        if (position >= 0 && position < logs.size()) {
            return logs.get(position);
        }
        return null;
    }

    static class LogViewHolder extends RecyclerView.ViewHolder {
        private TextView tvLogContent;

        public LogViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLogContent = itemView.findViewById(R.id.tv_log_content);
        }

        public void bind(String log) {
            tvLogContent.setText(log);
        }
    }
}
