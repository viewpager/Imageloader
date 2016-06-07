package com.jimmy.loader.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.util.LruCache;
import android.util.DisplayMetrics;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * 图片加载类
 * @author asus
 */
public class ImageLoader {
	
	private static ImageLoader mInstance;
	private LruCache<String,Bitmap> mLruCache;
	/**
	 * 线程池
	 */
	private ExecutorService mThreadPool;
	private static final int DEFULT_THREAD_COUNT = 1;
	/**
	 * 队列调度方式
	 */
	private Type mType = Type.LIFO;
	/**
	 * 任务队列
	 */
	private LinkedList<Runnable> mTaskQueue;
	/**
	 * 后台轮循线程
	 */
	private Thread mPoolThread;
	/**
	 * 轮循线程的handler
	 */
	private Handler mPoolThreadHandler;
	/**
	 * UI线程的handler
	 */
	private Handler mUIHandler;
	
	private Semaphore mSemaphorePoolThreadHandler = new Semaphore(0);
	/**
	 * 信号量控制任务队列
	 */
	private Semaphore mSemaphoreThreadPool;
	
	
	
	private ImageLoader(int threadCount,Type type){
		init(threadCount,type);
	}
	
	public static ImageLoader getInstance(){
		if(mInstance == null){
			synchronized (ImageLoader.class) {
				if(mInstance == null){
					mInstance = new ImageLoader(DEFULT_THREAD_COUNT,Type.LIFO);
				}
			}
		}
		return mInstance;
	}
	
	public static ImageLoader getInstance(int ThreadCount,Type type){
		if(mInstance == null){
			synchronized (ImageLoader.class) {
				if(mInstance == null){
					mInstance = new ImageLoader(ThreadCount,type);
				}
			}
		}
		return mInstance;
	}

	/**
	 * 初始化操作
	 * @param threadCount
	 * @param type
	 */
	private void init(int threadCount, Type type) {
		//后台轮循线程
		mPoolThread = new Thread(){
			@Override
			public void run() {
				Looper.prepare();
				mPoolThreadHandler = new Handler(){
					@Override
					public void handleMessage(Message msg) {
						//从线程池中取出一个任务进行执行
						mThreadPool.execute(getTask());
						try {
							mSemaphoreThreadPool.acquire();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				};
				//确认threadhandler不为空,释放一个信号量
				mSemaphorePoolThreadHandler.release();
				Looper.loop();
			};
		};
		mPoolThread.start();
		//获取应用最大应用内存
		int mMaxMemory = (int) Runtime.getRuntime().maxMemory();
		int mCacheMemory = mMaxMemory / 8;
		mLruCache = new LruCache<String, Bitmap>(mCacheMemory){
			//用来计算每个Bitmap所占据的内存
			@Override
			protected int sizeOf(String key, Bitmap value) {
				return value.getRowBytes() * value.getHeight();
			}
		};
		//初始化线程池
		mThreadPool = Executors.newFixedThreadPool(threadCount);
		//初始化队列
		mTaskQueue = new LinkedList<Runnable>();
		mType = type;
		mSemaphoreThreadPool = new Semaphore(threadCount);
	}

	/**
	 * 根据路径给imagerView设置图片
	 */
	public void loadImage(final String path,final ImageView imageView){
		//将路径和imageView进行绑定
		imageView.setTag(path);
		if(mUIHandler == null){
			mUIHandler = new Handler(){
				//获取得到的图片，回调设置给ImageView
				public void handleMessage(Message msg) {
					ImgBeanHolder holder = (ImgBeanHolder)msg.obj;
					Bitmap bm = holder.bitmap;
					ImageView imageView = holder.imageView;
					String path = holder.path;
					//将path与getTag存储的路径进行比较，以免错乱
					if (imageView.getTag().toString().equals(path)) {
						imageView.setImageBitmap(bm);
					}
				};
			};
		}
		//根据path在缓存中获取bitmap
		Bitmap bitmap = getBitmapFromLruCache(path);
		if(bitmap != null){
			refreashBitmap(path,imageView,bitmap);
		}else{
			addTask(new Runnable() {
				//加载图片，进行压缩
				@Override
				public void run() {
					//1：获得图片需要显示的大小
					ImageSize imageSize = getImagerSize(imageView);
					//2:压缩图片
					Bitmap bmp = decodeSampledBitmapFromPath(imageSize.width,imageSize.height,path);
					//3：把图片加入到缓存
					addBitmapToLruCache(path,bmp);
					refreashBitmap(path,imageView,bmp);
					mSemaphoreThreadPool.release();//释放信号量
				}
			});
		}
	}

	private void refreashBitmap(String path,ImageView imageView,Bitmap bm){
		Message message = Message.obtain();
		//得到ImgBeanHolder，并给其持有的对象赋值。
		ImgBeanHolder holder = new ImgBeanHolder();
		holder.bitmap = bm;
		holder.path = path;
		holder.imageView = imageView;
		message.obj = holder;
		mUIHandler.sendMessage(message);
	}

	/**
	 * 将图片加入lruCache
	 * @param path
	 * @param bmp
	 */
	protected void addBitmapToLruCache(String path, Bitmap bmp) {
		if(getBitmapFromLruCache(path) == null){
			if (bmp != null) {
				mLruCache.put(path, bmp);
			}
		}
	}

	/**
	 * 根据图片需要显示的宽和高对图片进行压缩
	 * @param width
	 * @param height
	 * @param path
	 * @return
	 */
	protected Bitmap decodeSampledBitmapFromPath(int width, int height,
			String path) {
		//获取图片的宽和高，并不加载图片进内存
		Options options = new Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(path,options);
		options.inSampleSize = caculateInSampleSize(options,width,height);
		//使用获取到的inSampleSize，再次解析时间
		options.inJustDecodeBounds = false;
		Bitmap bmp = BitmapFactory.decodeFile(path,options);
		return bmp;
	}

	/**
	 * 根据需求得宽和高，以及图片实际的宽和高计算SampleSize
	 * @param options
	 * @param reqWidth	需求宽度
	 * @param reqHeight	需求高度
	 * @return
	 */
	private int caculateInSampleSize(Options options, int reqWidth, int reqHeight) {
		int width = options.outWidth;
		int height = options.outHeight;
		
		int inSampleSize = 1;
		if (width > reqWidth || height > reqHeight) {
			int WidthRadio = Math.round(width*1.0f/reqWidth);
			int HeightRadio = Math.round(height*1.0f/reqHeight);
			
			inSampleSize = Math.max(WidthRadio,HeightRadio);
		}
		return inSampleSize;
	}

	/**
	 * 根据imageView获得适当的压缩的宽和高
	 * @param imageView
	 */
	//@SuppressLint("NewApi")
	protected ImageSize getImagerSize(ImageView imageView) {
		ImageSize imageSize = new ImageSize();
		LayoutParams lp = imageView.getLayoutParams();
		
		DisplayMetrics displayMetrics = imageView.getContext().getResources().getDisplayMetrics();
		int width = imageView.getWidth();//获取imagerView的实际宽度
		//有可能没有设置宽度所以可能小于等于0
		if(width <= 0){
			width = lp.width;
		}
		//有可能是包裹内容，所以也可能获取不到宽度
		if(width <= 0){
			//width = imageView.getMaxWidth();//检查最大值
			width = getImageViewFieldValue(imageView,"mMaxWidth");
		}
		//还有可能小于等于0时间，我们就让其等于屏幕宽度
		if(width <= 0){
			width = displayMetrics.widthPixels;
		}
		
		int height = imageView.getHeight();///获取imagerView的实际高度
		//有可能没有设置高度所有可能小于等于0
		if(height <= 0){
			height = lp.height;
		}
		//有可能是包裹内容，所以也可能获取不到高度
		if(height <= 0){
			height = getImageViewFieldValue(imageView,"mMaxHeight");//检查最大值
		}
		//还有可能小于等于0时间，我们就让其等于屏幕高度
		if(height <= 0){
			height = displayMetrics.heightPixels;
		}
		imageSize.width = width;
		imageSize.height = height;
		return imageSize;
	}

	/**
	 * 反射获取imageview的某个属性值(任何对象的任何属性值都能获取到)
	 */
	private static int getImageViewFieldValue(Object obj,String fieldName){
		int value = 0;
		try {
			Field field = ImageView.class.getDeclaredField(fieldName);
			field.setAccessible(true);
			int fieldValue = field.getInt(obj);
			if (fieldValue > 0 && fieldValue < Integer.MAX_VALUE) {
				value = fieldValue;
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		}
		return value;
	}

	/**
	 * 添加到队列
	 */
	private synchronized void addTask(Runnable runnable) {
		mTaskQueue.add(runnable);
		try {
			if (mPoolThreadHandler == null) {
				mSemaphorePoolThreadHandler.acquire();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} 
		mPoolThreadHandler.sendEmptyMessage(0x110);
	}

	/**
	 * 从任务队列取出一个方法
	 * @return
	 */
	private Runnable getTask() {
		if(mType == Type.FIFO){
			return mTaskQueue.removeFirst();
		}else if(mType == Type.LIFO){
			return mTaskQueue.removeLast();
		}
		return null;
	}

	/**
	 * 根据路径从缓存中获取Bitmap
	 * @param
	 * @return
	 */
	private Bitmap getBitmapFromLruCache(String key) {
		return mLruCache.get(key);
	}

	/**
	 * 持有对象，避免造成错乱
	 */
	private class ImgBeanHolder{
		Bitmap bitmap;
		ImageView imageView;
		String path;
	}

	/**
	 * 保存图片的宽 高
	 */
	private class ImageSize{
		int width;
		int height;
	}

	/**
	 * 线程调度方式
	 */
	public enum Type{
		FIFO,LIFO;
	}
}
