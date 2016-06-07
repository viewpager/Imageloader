package com.jimmy.loader;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.graphics.Color;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.jimmy.loader.util.ImageLoader;



class ImageAdapter extends BaseAdapter{
		
		private static Set<String> mSeletedImg = new HashSet<String>();
		private String mDirPath;
		private List<String> mImgPaths;
		private LayoutInflater mInflater;

		//获取屏幕宽度
		private int mScreenWidth;
		
		public ImageAdapter(Context context,List<String> mDatas,String dirPath){
			this.mDirPath = dirPath;
			this.mImgPaths = mDatas;
			this.mInflater = LayoutInflater.from(context);
			
			WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
			DisplayMetrics outMetrics = new DisplayMetrics();
			wm.getDefaultDisplay().getMetrics(outMetrics);
			mScreenWidth = outMetrics.widthPixels;
		}
		
		
		@Override
		public int getCount() {
			return mImgPaths.size();
		}

		@Override
		public Object getItem(int position) {
			return mImgPaths.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			final ViewHolder viewHolder;
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.item_gridview,parent,false);
				viewHolder = new ViewHolder();
				viewHolder.mImg = (ImageView) convertView.findViewById(R.id.item_img);
				viewHolder.mSelect = (ImageButton) convertView.findViewById(R.id.id_item_select);
				convertView.setTag(viewHolder);
			}else{
				viewHolder = (ViewHolder) convertView.getTag();
			}
			//重置狀態
			viewHolder.mImg.setImageResource(R.drawable.pictures_no);
			viewHolder.mSelect.setImageResource(R.drawable.picture_unselected);
			viewHolder.mImg.setColorFilter(null);

			//收手动设置（优化）
			viewHolder.mImg.setMaxWidth(mScreenWidth / 3);
			
			ImageLoader.getInstance(3, ImageLoader.Type.LIFO).loadImage(mDirPath+"/"+mImgPaths.get(position),viewHolder.mImg);
			final String filePath = mDirPath+"/"+mImgPaths.get(position);
			viewHolder.mImg.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					//使用完整路徑防止不同文件下圖片重名
					if (mSeletedImg.contains(filePath)) {
						//已經被選中
						mSeletedImg.remove(filePath);
						viewHolder.mImg.setColorFilter(null);	
						viewHolder.mSelect.setImageResource(R.drawable.picture_unselected);
					}else{
						//未被選中
						mSeletedImg.add(filePath);
						viewHolder.mImg.setColorFilter(Color.parseColor("#77000000"));
						viewHolder.mSelect.setImageResource(R.drawable.pictures_selected);
					}
					//notifyDataSetChanged();
				}
			});
			if (mSeletedImg.contains(filePath)) {
				viewHolder.mImg.setColorFilter(Color.parseColor("#77000000"));
				viewHolder.mSelect.setImageResource(R.drawable.pictures_selected);
			}
			return convertView;
		}
	}
	
	
	class ViewHolder{
		 ImageView mImg;
		 ImageButton mSelect;
	}
	
	
	
	
	