package com.creationline.cloudstack.ui;

import android.os.Bundle;

public class InProgressCache {

    private static Bundle inProgressCache = new Bundle();

    public static final int IDLE = 0;  //purposely 0 so getProgressForId() returns IDLE (i.e. nothing in progress) if there is no match id in the cache
    public static final int IN_PROGRESS = 1;
    public static final int SHOW_ICON = 2;
    
    public int getProgressForId(final String id) {
    	return inProgressCache.getInt(id);
    }
    
    public void removeId(final String id) {
    	inProgressCache.remove(id);
    }
    
    public void setShowIconForId(final String id) {
    	inProgressCache.putInt(id, InProgressCache.SHOW_ICON);
    }
    
    public void setInProgressForId(final String id) {
    	inProgressCache.putInt(id, InProgressCache.IN_PROGRESS);
    }
    
}
