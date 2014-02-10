package com.derektrauger.library.imaging;

import android.content.Context;
import android.os.AsyncTask;

public class InitDiskCacheTask extends AsyncTask<Context, Void, DiskLruImageCache> {

    private Context c;

    public InitDiskCacheTask() {
    }

    public InitDiskCacheTask(Context context){
        this.c = context;
    }

    @Override
    protected DiskLruImageCache doInBackground(Context... context) {
        if (c == null){
            return new DiskLruImageCache(context[0]);
        } else {
            return new DiskLruImageCache(c);
        }
    }
}