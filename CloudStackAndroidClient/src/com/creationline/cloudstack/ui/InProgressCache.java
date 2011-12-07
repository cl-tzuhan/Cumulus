/*******************************************************************************
 * Copyright 2011 Creationline,Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
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
