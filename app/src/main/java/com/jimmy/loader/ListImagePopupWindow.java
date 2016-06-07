package com.jimmy.loader;


import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.jimmy.loader.bean.FolderBean;
import com.jimmy.loader.util.ImageLoader;

import java.util.List;


public class ListImagePopupWindow extends PopupWindow {
	
	private int mWidth;
	private int mHeight;
	private View mConvertView;
	private ListView mListview;
	
	private List<FolderBean> mDatas;
	private OnDirSeletedListener mListener;
	
	public void setOnDirSeletedListener(OnDirSeletedListener mListener){
		this.mListener = mListener;
	}
	
	public interface OnDirSeletedListener{
		void onSeleted(FolderBean folderBean);
	}
	
	public ListImagePopupWindow(Context context,List<FolderBean> datas){
		calWidthAndHeight(context);
		mConvertView = LayoutInflater.from(context).inflate(R.layout.popup_main,null);
		mDatas = datas;
		//设置基本属性
		setContentView(mConvertView);
		setWidth(mWidth);
		setHeight(mHeight);
		//以下为popupwindow常用属性
		setFocusable(true);
		setTouchable(true);
		setOutsideTouchable(true);
		setBackgroundDrawable(new BitmapDrawable());
		setTouchInterceptor(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
					dismiss();
					return true;
				}
				return false;
			}
		});
		initViews(context);
		initEvent();
	}

	/**
	 * 计算宽高
	 * @param context
	 */
	private void calWidthAndHeight(Context context) {
		WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		DisplayMetrics outMetrics = new DisplayMetrics();
		windowManager.getDefaultDisplay().getMetrics(outMetrics);
		mWidth = outMetrics.widthPixels;
		mHeight = (int) (outMetrics.heightPixels * 0.7);
	}

	private void initViews(Context context) {
		mListview = (ListView) mConvertView.findViewById(R.id.id_list_dir);
		mListview.setAdapter(new ListDirAdapter(context, mDatas));
	}

	private void initEvent() {
		mListview.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				if (mListener != null) {
					mListener.onSeleted(mDatas.get(arg2));
				}
			}
		});
	}
	
	private class ListDirAdapter extends ArrayAdapter<FolderBean>{
		
		private LayoutInflater mInflater;
		private List<FolderBean> mDatas;
		
		public ListDirAdapter(Context context,
				List<FolderBean> objects) {
			super(context, 0, objects);
			mInflater = LayoutInflater.from(context);
		}
			
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if (convertView == null) {
				holder = new ViewHolder();
				convertView = mInflater.inflate(R.layout.popup_item,parent,false);
				holder.mImg = (ImageView) convertView.findViewById(R.id.id_dir_item_img);
				holder.mDirName = (TextView) convertView.findViewById(R.id.id_dir_item_name);
				holder.mDirCount = (TextView) convertView.findViewById(R.id.id_dir_item_count);
				convertView.setTag(holder);
			}else{
				holder = (ViewHolder) convertView.getTag();
			}

			FolderBean folderBean = getItem(position);
			//重置显示的默认图片
			holder.mImg.setImageResource(R.drawable.pictures_no);
			//加载需要显示的图片
			ImageLoader.getInstance().loadImage(folderBean.getFirstImgPath(),holder.mImg);
			holder.mDirName.setText(folderBean.getName());
			holder.mDirCount.setText(folderBean.getCount() + "");
			return convertView;
		}
		
		private class ViewHolder{
			ImageView mImg;
			TextView mDirName,mDirCount;
		}
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
