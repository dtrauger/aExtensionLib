package com.derektrauger.library;

import android.graphics.PointF;

import com.google.android.gms.maps.model.LatLng;

public class MapUtils {

	public static final int TILE_SIZE = 256;

	private static double BASE_X_PIXEL_SIZE = 156543.033928041;
	private static double BASE_Y_PIXEL_SIZE = -156543.033928041;
	private static double BASE_X_ORIGIN = -20037508.3427892;
	private static double BASE_Y_ORIGIN = 20037508.3427892;

	//*********************************************************************************************************
	// Convert northing easting to lat lon
	public static LatLng NorthingEastingToLatLon(PointF ne) {
		int zoom = 1;
		
		// Convert northing/easting to xy
		double xRes = BASE_X_PIXEL_SIZE / Math.pow(2, zoom);
		double yRes = BASE_Y_PIXEL_SIZE / Math.pow(2, zoom);

		double x = (ne.x - BASE_X_ORIGIN) / xRes;
		double y = (ne.y - BASE_Y_ORIGIN) / yRes;

		return XYToLatLon(x, y, zoom);
	}

	// Convert lat lon to northing easting
	public static PointF LatLonToNorthingEasting(LatLng ll) {
		return LatLonToNorthingEasting(ll.latitude, ll.longitude);
	}

	// Convert lat lon to northing easting
	public static PointF LatLonToNorthingEasting(double lat, double lon) {
		int zoom = 1;
		PointF xy = LatLonToXY(lat, lon, zoom);
		
		double xRes = BASE_X_PIXEL_SIZE / Math.pow(2, zoom);
		double yRes = BASE_Y_PIXEL_SIZE / Math.pow(2, zoom);

		double northing = BASE_Y_ORIGIN + xy.y * yRes;
		double easting = BASE_X_ORIGIN + xy.x * xRes;
		
		return new PointF((float)(easting), (float)(northing));
	}

	// Convert northing easting to xy
	public static PointF NorthingEastingToXY(PointF ne, double zoom) {
		// Convert northing/easting to xy
		double xRes = BASE_X_PIXEL_SIZE / Math.pow(2, zoom);
		double yRes = BASE_Y_PIXEL_SIZE / Math.pow(2, zoom);

		double x = Math.round((ne.x - BASE_X_ORIGIN) / xRes);
		double y = Math.round((ne.y - BASE_Y_ORIGIN) / yRes);

		return new PointF((float)(x), (float)(y));
	}

	// Convert XY to northing easting
	public static PointF XYToNorthingEasting(PointF xy, double zoom) {
		double xRes = BASE_X_PIXEL_SIZE / Math.pow(2, zoom);
		double yRes = BASE_Y_PIXEL_SIZE / Math.pow(2, zoom);

		double northing = BASE_Y_ORIGIN + xy.y * yRes;
		double easting = BASE_X_ORIGIN + xy.x * xRes;
		
		return new PointF((float)(easting), (float)(northing));
	}

	//*********************************************************************************************************
	// Converts lat lon to world x y coordinates
	public static PointF LatLonToXY(double lat, double lon, int zoom) {

		lat = Math.toRadians(lat);
		lon = Math.toRadians(lon);

		double circumference = TILE_SIZE * Math.pow(2, zoom);
		double falseEasting = circumference / 2.0;
		double falseNorthing = circumference / 2.0;
		double radius = circumference / (2 * Math.PI);
		
		double y = radius / 2.0 * Math.log( (1.0 + Math.sin(lat)) / (1.0 - Math.sin(lat)));
		double x = radius * lon;  
		
		return new PointF((float)(falseEasting + x), (float)(falseNorthing - y)); 
	}
	
	public static PointF LatLonToXY(double lat, double lon, double zoom) {

		lat = Math.toRadians(lat);
		lon = Math.toRadians(lon);

		double circumference = TILE_SIZE * Math.pow(2, zoom);
		double falseEasting = circumference / 2.0;
		double falseNorthing = circumference / 2.0;
		double radius = circumference / (2 * Math.PI);
		
		double y = radius / 2.0 * Math.log( (1.0 + Math.sin(lat)) / (1.0 - Math.sin(lat)));
		double x = radius * lon;  
		
		return new PointF((float)(falseEasting + x), (float)(falseNorthing - y)); 
	}

	// Converts the world x y coordinates to lat lon
	public static LatLng XYToLatLon(double x, double y, int zoom) {
		double circumference = TILE_SIZE * Math.pow(2, zoom);
		double falseEasting = circumference / 2.0;
		double falseNorthing = circumference / 2.0;
		double radius = circumference / (2 * Math.PI);
		
		x -= falseEasting;
		y -= falseNorthing;

		double lon = Math.toDegrees(x / radius);
		double lat = Math.toDegrees((Math.PI/2) - (2 * Math.atan(Math.exp(-1.0 * y / radius)))) * -1;
		
		if (lon < -180) {
			lon = (lon % 180) + 180;
		}
		if (lon > 180) {
			lon = (lon % 180) - 180;
		}
		
		return new LatLng(lat, lon);
	}
	
	// Returns the tile xy name based on the lat lon
	public static PointF LatLonToTileXY(double lat, double lon, int zoom) {
		PointF xy = LatLonToXY(lat, lon, zoom);		
		return new PointF((float)(Math.floor(xy.y / TILE_SIZE)),  (float)(Math.floor(xy.x / TILE_SIZE)));
	}

	// Returns the offset into the tile for the given lat lon
	public static PointF LatLonToTileXYOffset(double lat, double lon, int tileX, int tileY, int zoom) {
		PointF xy = LatLonToXY(lat, lon, zoom);
		return new PointF((float)(tileY * TILE_SIZE + Math.floor(xy.y)), (float)(tileX * TILE_SIZE + Math.floor(xy.x)));
	}
	
	
	//*********************************************************************************************************
	/*public static Rectangle2D.Double getTileExtents(int xTile, int yTile, int zoomLevel) {
		// Convert tile coordinates and zoomlevel to northing/easting values
		double xRes = BASE_X_PIXEL_SIZE / Math.pow(2, zoomLevel);
		double yRes = BASE_Y_PIXEL_SIZE / Math.pow(2, zoomLevel);

		// ul = upperLeft, lr = lowerRight of tile extents
		double ulX = BASE_X_ORIGIN + (xTile * TILE_SIZE) * xRes;
		double ulY = BASE_Y_ORIGIN + (yTile * TILE_SIZE) * yRes;
		double width = xRes * TILE_SIZE;
		double height = yRes * TILE_SIZE;

		return new Rectangle2D.Double(ulX, ulY, width, height);
	}*/
	
}
