package com.derektrauger.library;

import java.net.MalformedURLException;
import java.net.URL;

import com.google.android.gms.maps.model.UrlTileProvider;

public class XYZUrlTileProvider extends UrlTileProvider {

private String baseUrl;

public XYZUrlTileProvider(int width, int height, String url) {
    super(width, height);
    this.baseUrl = url;
}

@Override
public URL getTileUrl(int x, int y, int zoom) {
    try {
        return new URL(baseUrl.replace("{z}", ""+zoom).replace("{x}",""+x).replace("{y}",""+y));
    } catch (MalformedURLException e) {
        e.printStackTrace();
    }
    return null;
}
}
