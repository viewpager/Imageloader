package com.jimmy.loader;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.GridView;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.jimmy.loader.bean.FolderBean;


public class MainActivity extends Activity {
	
	private RelativeLayout mBottomLy;
	private TextView mDirName,mDirCount;
	
	private GridView mGridView;
	private List<String> mImgs;
	private ImageAdapter mImgAdapter;
	
	private File mCurrentDir;
	private int mMaxCount;
	private List<FolderBean> mFolderBeans = new ArrayList<FolderBean>();
	private ProgressDialog mProgressDialog;
	private static final int DATA_LOADED = 0X110;
	private ListImagePopupWindow mDirPopupWindow;
	
	private Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			if (msg.what == DATA_LOADED) {
				mProgressDialog.dismiss();
				data2View();
				initDirPopupWindow();
			}
		};
	};

	/**
	 * 初始化popupWindow
	 */
	protected void initDirPopupWindow() {
		mDirPopupWindow = new ListImagePopupWindow(this,mFolderBeans);
		mDirPopupWindow.setOnDismissListener(new OnDismissListener() {
			@Override
			public void onDismiss() {
				lightOn();
			}
		});
		
		mDirPopupWindow.setOnDirSeletedListener(new ListImagePopupWindow.OnDirSeletedListener() {
			@Override
			public void onSeleted(FolderBean folderBean) {
				mCurrentDir = new File(folderBean.getDir());
				mImgs = Arrays.asList(mCurrentDir.list(new FilenameFilter() {
					@Override
					public boolean accept(File dir, String filename) {
						if (filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".png")) {
							return true;
						}
						return false;
					}
				}));
				mImgAdapter = new ImageAdapter(MainActivity.this,mImgs,mCurrentDir.getAbsolutePath());
				mGridView.setAdapter(mImgAdapter);
				mDirCount.setText(mImgs.size() + "");
				mDirName.setText(folderBean.getName());
				mDirPopupWindow.dismiss();
			}
		});
	}

	/**
	 * 内容区域变亮
	 */
	protected void lightOn() {
		WindowManager.LayoutParams lp = getWindow().getAttributes();
		lp.alpha = 1.0f;
		getWindow().setAttributes(lp);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		inintView();
		initDatas();
		initEvent();
	}
	
	private void inintView() {
		mGridView = (GridView) findViewById(R.id.id_gv);
		mBottomLy = (RelativeLayout) findViewById(R.id.id_rl);
		mDirName = (TextView) findViewById(R.id.id_dir_name);
		mDirCount = (TextView) findViewById(R.id.id_dir_count);
	}

	/**
	 * 使用ContentProvider扫描手机中的所有图片
	 */
	private void initDatas() {
		if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			Toast.makeText(this, "当前存储卡不可用",Toast.LENGTH_SHORT).show();
			return;
		}
		mProgressDialog = ProgressDialog.show(this,null,"正在加载");
		new Thread(){
			public void run() {
				Uri mImgUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
				ContentResolver cr = MainActivity.this.getContentResolver();
				
				Cursor mCursor = cr.query(mImgUri, null, MediaStore.Images.Media.MIME_TYPE + "=? or " 
				+ MediaStore.Images.Media.MIME_TYPE + "=?", 
				new String[]{"image/jpeg","image/png"},
				MediaStore.Images.Media.DATE_MODIFIED);
				
				Set<String> mDirPaths = new HashSet<String>();
				while(mCursor.moveToNext()){
				String path = mCursor.getString(mCursor.getColumnIndex(MediaStore.Images.Media.DATA));
				File parentFile = new File(path).getParentFile();
				if (parentFile == null) {
					continue;
				}
				String dirPath = parentFile.getAbsolutePath();
				
				FolderBean folderBean = null;
				if (mDirPaths.contains(dirPath)) {
					continue;
				}else{
					mDirPaths.add(dirPath); //將当前文件夹存储起来
					folderBean = new FolderBean();
					folderBean.setDir(dirPath);
					folderBean.setFirstImgPath(path);
				}
				if (parentFile.list() == null) {
					continue;
				}
				int picSize = parentFile.list(new FilenameFilter() {
					@Override
					public boolean accept(File dir, String filename) {
						if (filename.endsWith(".jpg") || filename.endsWith(".png") || filename.endsWith(".jpeg")) {
							return true;
						}
						return false;
					}
				}).length;
				folderBean.setCount(picSize);
				mFolderBeans.add(folderBean);
				
				if (picSize > mMaxCount) {
					mMaxCount = picSize;
					mCurrentDir = parentFile;
				}
			}
				mCursor.close();
				//通知handler扫描完成
				mHandler.sendEmptyMessage(DATA_LOADED);
			};
		}.start();
	}

	/**
	 * 点击出现popupwindow
	 */
	private void initEvent() {
		mBottomLy.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mDirPopupWindow.setAnimationStyle(R.style.dir_popupwindow);
				mDirPopupWindow.showAsDropDown(mBottomLy,0,0);
				lightOff();
			}
		});
	}

	/**
	 * 内容区域变暗
	 */
	protected void lightOff() {
		WindowManager.LayoutParams lp = getWindow().getAttributes();
		lp.alpha = 0.3f;
		getWindow().setAttributes(lp);
	}

	/**
	 * 绑定数据到view中(为GridView设置数据)
	 */
	protected void data2View() {
		if (mCurrentDir == null) {
			Toast.makeText(this,"未扫描到任何图片",Toast.LENGTH_SHORT).show();
			mDirCount.setText("暂无图片");
			return;
		}
		
		mImgs = Arrays.asList(mCurrentDir.list());//将数组包装成一个list返回
		mImgAdapter = new ImageAdapter(this,mImgs,mCurrentDir.getAbsolutePath());
		mGridView.setAdapter(mImgAdapter);
		
		mDirCount.setText(mMaxCount+"");
		mDirName.setText(mCurrentDir.getName());
	}

}
