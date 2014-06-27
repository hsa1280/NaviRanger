package edu.ece.ufl.cps;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

public class WeatherOverlay extends ItemizedOverlay{
	
	ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
	private Context mContext;
	private int mTextSize = 22;
	
	public WeatherOverlay(Drawable defaultMarker) {
		super(boundCenterBottom(defaultMarker));
		// TODO Auto-generated constructor stub
	}
	
	public WeatherOverlay(Drawable defaultMarker, Context context) {
		  super(boundCenterBottom(defaultMarker));
		  mContext = context;
		}

	@Override
	protected OverlayItem createItem(int i) {
		// TODO Auto-generated method stub
		return mOverlays.get(i);
	}

	@Override
	public int size() {
		// TODO Auto-generated method stub
		return mOverlays.size();
	}

	public void addOverlay(OverlayItem overlay){
		mOverlays.add(overlay);
		populate();
	}
	
	@Override
	protected boolean onTap(int index) {
	  OverlayItem item = mOverlays.get(index);
	  AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
	  dialog.setTitle(item.getTitle());
	  dialog.setMessage(item.getSnippet());
	  dialog.show();
	  return true;
	}
	
	/*@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) 
	{
		Paint textPaint = new Paint();
	    textPaint.setARGB(255, 255, 255, 255);
	    textPaint.setTextAlign(Paint.Align.CENTER);
	    textPaint.setTextSize(16);
	    textPaint.setTypeface(Typeface.DEFAULT_BOLD);

	    canvas.drawText("Some Text", 100, 100, textPaint);
        super.draw(canvas, mapView, shadow);
				
	}*/
	
	 @Override
	    public void draw(android.graphics.Canvas canvas, MapView mapView, boolean shadow)
	    {
	        super.draw(canvas, mapView, shadow);

	        if (shadow == false)
	        {
	            //cycle through all overlays
	            for (int index = 0; index < mOverlays.size(); index++)
	            {
	                OverlayItem item = mOverlays.get(index);

	                // Converts lat/lng-Point to coordinates on the screen
	                GeoPoint point = item.getPoint();
	                Point ptScreenCoord = new Point() ;
	                mapView.getProjection().toPixels(point, ptScreenCoord);

	                //Paint
	                Paint paint = new Paint();
	                paint.setTextAlign(Paint.Align.CENTER);
	                paint.setTextSize(mTextSize);
	                paint.setARGB(150, 0, 0, 0); // alpha, r, g, b (Black, semi see-through)

	                //show text to the right of the icon
	                canvas.drawText(item.getTitle(), ptScreenCoord.x, ptScreenCoord.y+mTextSize, paint);
	            }
	        }
	    }

}
