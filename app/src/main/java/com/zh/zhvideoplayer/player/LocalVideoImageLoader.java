package com.zh.zhvideoplayer.player;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore.Video.Thumbnails;
import android.support.v4.util.LruCache;
import android.widget.ImageView;

import com.zh.zhvideoplayer.R;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 加载本地视频缩略图的imageLoader,提高效率节省时间
 */
public class LocalVideoImageLoader {
	private Context mContext;
	private String key;
    //创建cache
    private LruCache<String, Bitmap> lruCache;
    
    /** 
     * 图片硬盘缓存类
     */  
    private DiskLruCache mDiskLruCache; 
       
    public LocalVideoImageLoader(Context context){
    	this.mContext = context;
    	 //初始化LruCache内存缓存
        int maxMemory = (int) Runtime.getRuntime().maxMemory();//获取最大的运行内存
        int maxSize = maxMemory / 8; //拿到缓存的内存大小 35     
        lruCache = new LruCache<String, Bitmap>(maxSize){
			@SuppressLint("NewApi")
			@Override
            protected int sizeOf(String key, Bitmap value) {
                //这个方法会在每次存入缓存的时候调用
                if (android.os.Build.VERSION.SDK_INT >= 12) {  
                    return value.getByteCount(); //需要api >=12 ， 总字节数  
                } else {  
                    return value.getRowBytes() * value.getHeight(); //每行字节乘以高(即行)   api1  
                }  
            }
        };
        //初始化DiskCache本地缓存
        try {  
            // 获取图片缓存路径  
            File cacheDir = getDiskCacheDir(mContext, "VideoThumb");  
            if (!cacheDir.exists()) {  
                cacheDir.mkdirs();  
            }  
            // 创建DiskLruCache实例，初始化缓存数据  
            mDiskLruCache = DiskLruCache  
                    .open(cacheDir, getAppVersion(context), 1, 10 * 1024 * 1024);  
        } catch (IOException e) {  
            e.printStackTrace();  
        }  
    }    
   
    public void showThumbByAsynctack(String path,ImageView imgview){
    	key = hashKeyForDisk(path);
        if(getVideoThumbToCache(key) == null){
            //内存加载失败，从本地加载或生成视频截图
            new MyBobAsynctack(imgview, path).execute(path);
        }else{
            imgview.setImageBitmap(getVideoThumbToCache(key));
        }
        
    }
    
    public void addVideoThumbToCache(String key,Bitmap bitmap){
    	
        if(getVideoThumbToCache(key) == null){
            //当前地址没有缓存时，就添加
            lruCache.put(key, bitmap);
        }
    }
    
    public Bitmap getVideoThumbToCache(String key){
        
        return lruCache.get(key);
        
    }
    
    class MyBobAsynctack extends AsyncTask<String, Void, Bitmap> {
        private ImageView imgView;
        private String path;

        public MyBobAsynctack(ImageView imageView,String path) {
            this.imgView = imageView;
            this.path = path;
        }

        @Override
        protected Bitmap doInBackground(String... params) {
                                        //这里的创建缩略图方法是调用VideoUtil类的方法，也是通过 android中提供的 ThumbnailUtils.createVideoThumbnail(vidioPath, kind);
//            Bitmap bitmap = VideoUtil.createVideoThumbnail(params[0], 70, 50, MediaStore.Video.Thumbnails.MICRO_KIND);
//            Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(params[0],  Thumbnails.MINI_KIND);
            //加入缓存中
//            if(bitmap != null){
//            	 if(getVideoThumbToCache(params[0]) == null){
//                     addVideoThumbToCache(path, bitmap);
//                 }
//            }
            
            FileDescriptor fileDescriptor = null;  
            FileInputStream fileInputStream = null;  
            DiskLruCache.Snapshot snapShot = null;
            try {  
                // 生成图片URL对应的key,利用key而不是原来的path是因为防止中文等特殊符号，导致存不进缓存
                final String key = hashKeyForDisk(path);  
                // 查找key对应的缓存  
                snapShot = mDiskLruCache.get(key);  
                if (snapShot == null) {  
                	System.out.println("zh::::::::::::::snapShot == null;");
                    // 如果没有找到对应的本地缓存，则准备从网络上请求数据/或生成视频截图，并写入本地缓存和内存缓存  
                    DiskLruCache.Editor editor = mDiskLruCache.edit(key); 
                    if (editor != null) {  
                        OutputStream outputStream = editor.newOutputStream(0);  
                        if (downloadUrlToStream(path, outputStream)) { 
                        	System.out.println("downloadUrlToStream");
                            editor.commit();  
                        } else {  
                            editor.abort();  
                        }  
                    }  
                    // 缓存被写入后，再次查找key对应的缓存  
                    snapShot = mDiskLruCache.get(key);  
                }
                if (snapShot != null) {  
                    fileInputStream = (FileInputStream) snapShot.getInputStream(0);  
                    fileDescriptor = fileInputStream.getFD();  
                }  
                // 将缓存数据解析成Bitmap对象  
                Bitmap bitmap = null;  
                if (fileDescriptor != null) {  
                    bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);  
                }  
                if (bitmap != null) {  
                    // 将Bitmap对象添加到内存缓存当中  
                	addVideoThumbToCache(key, bitmap);
                }  
                return bitmap;  
            } catch (IOException e) {  
                e.printStackTrace();  
            } finally {  
                if (fileDescriptor == null && fileInputStream != null) {  
                    try {  
                        fileInputStream.close();  
                    } catch (IOException e) {  
                    }  
                }  
            }  
            return null;  
        }
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if(imgView.getTag().equals(hashKeyForDisk(path))){//通过 Tag可以绑定 图片地址和 imageView，这是解决Listview加载图片错位的解决办法之一
            	if(bitmap != null){
            		imgView.setImageBitmap(bitmap);
            	}else{
            		imgView.setImageResource(R.mipmap.video_fail);
            	}
            }
        }
    }
    
    /** 
     * 根据传入的uniqueName获取硬盘缓存的路径地址。 
     */  
    public File getDiskCacheDir(Context context, String uniqueName) {  
        String cachePath;  
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())  
                || !Environment.isExternalStorageRemovable()) {  
            cachePath = context.getExternalCacheDir().getPath();  
        } else {  
            cachePath = context.getCacheDir().getPath();  
        }  
        return new File(cachePath + File.separator + uniqueName);  
    } 
    
    /** 
     * 获取当前应用程序的版本号。 
     */  
    public int getAppVersion(Context context) {  
        try {  
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(),  
                    0);  
            return info.versionCode;  
        } catch (NameNotFoundException e) {  
            e.printStackTrace();  
        }  
        return 1;  
    }
    
    /** 
     * 使用MD5算法对传入的key进行加密并返回。 
     */  
    public String hashKeyForDisk(String key) {  
        String cacheKey;  
        try {  
            final MessageDigest mDigest = MessageDigest.getInstance("MD5");  
            mDigest.update(key.getBytes());  
            cacheKey = bytesToHexString(mDigest.digest());  
        } catch (NoSuchAlgorithmException e) {  
            cacheKey = String.valueOf(key.hashCode());  
        }  
        return cacheKey;  
    }

    private String bytesToHexString(byte[] bytes) {  
        StringBuilder sb = new StringBuilder();  
        for (int i = 0; i < bytes.length; i++) {  
            String hex = Integer.toHexString(0xFF & bytes[i]);  
            if (hex.length() == 1) {  
                sb.append('0');  
            }  
            sb.append(hex);  
        }  
        return sb.toString();  
    }
	
	/** 
     * 建立HTTP请求，并获取Bitmap对象。 
     *  
     *            图片的URL地址
     * @return 解析后的Bitmap对象 
     */  
    private boolean downloadUrlToStream(String urlString, OutputStream outputStream) {  
    	Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(urlString,  Thumbnails.MINI_KIND);
    	if(bitmap != null){
    		ByteArrayOutputStream baos = null;
        	InputStream inputStream = null;
            BufferedOutputStream out = null;  
            BufferedInputStream in = null;  
            try {  
            	baos = new ByteArrayOutputStream();
            	bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            	inputStream = new ByteArrayInputStream(baos.toByteArray());
                in = new BufferedInputStream(inputStream, 8 * 1024);  
                out = new BufferedOutputStream(outputStream, 8 * 1024);  
                int b;  
                while ((b = in.read()) != -1) {  
                    out.write(b);  
                }  
                return true;  
            } catch (final IOException e) {  
                e.printStackTrace();  
            } finally {  
                try {  
                	if (baos != null) {  
                		baos.close();
                    }
                	if (inputStream != null) {  
                		inputStream.close();
                    }
                	 
                    if (out != null) {  
                        out.close();  
                    }  
                    if (in != null) {  
                        in.close();  
                    }  
                } catch (final IOException e) {  
                    e.printStackTrace();  
                }  
            }
            return false;
    	}
        return false;  
    }  

}