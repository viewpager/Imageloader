package com.jimmy.loader.bean;

public class FolderBean {
	private String dir;
	private String firstImgPath;
	private String name;
	private int count;
	public String getDir() {
		return dir;
	}
	public void setDir(String dir) {
		this.dir = dir;
		int lastIndexOf = this.dir.lastIndexOf("/");
		this.name = this.dir.substring(lastIndexOf);
	}
	
	public String getFirstImgPath() {
		return firstImgPath;
	}
	public void setFirstImgPath(String firstImgPath) {
		this.firstImgPath = firstImgPath;
	}
	public String getName() {
		return name;
	}
	
	public int getCount() {
		return count;
	}
	public void setCount(int count) {
		this.count = count;
	}
	
	
}
