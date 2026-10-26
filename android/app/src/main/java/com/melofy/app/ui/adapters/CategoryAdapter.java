package com.melofy.app.ui.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.melofy.app.databinding.ItemCategoryCardBinding;

import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    public interface OnCategoryClickListener {
        void onCategoryClick(String categoryName);
    }

    public static class CategoryItem {
        public String name;
        public int colorRes;

        public CategoryItem(String name, int colorRes) {
            this.name = name;
            this.colorRes = colorRes;
        }
    }

    private final List<CategoryItem> categories = new ArrayList<>();
    private final OnCategoryClickListener listener;

    public CategoryAdapter(OnCategoryClickListener listener) {
        this.listener = listener;
    }

    public void setCategories(List<CategoryItem> items) {
        this.categories.clear();
        if (items != null) {
            this.categories.addAll(items);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCategoryCardBinding binding = ItemCategoryCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new CategoryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        CategoryItem item = categories.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        private final ItemCategoryCardBinding binding;

        public CategoryViewHolder(ItemCategoryCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(CategoryItem item, OnCategoryClickListener listener) {
            binding.textCategoryName.setText(item.name);
            binding.cardCategory.setBackgroundResource(item.colorRes);

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onCategoryClick(item.name);
            });
        }
    }
}
