package edu.ece.ufl.cps;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import net.xeomax.FBRocket.FBRocket;
import net.xeomax.FBRocket.Facebook;
import net.xeomax.FBRocket.LoginListener;
import net.xeomax.FBRocket.ServerErrorException;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public class NaviRangerActivity extends MapActivity implements LocationListener, OnClickListener, LoginListener
{
	/* View for showing map*/
	MapView map;
	MyLocationOverlay compass;
	MapController controller;
	LocationManager locationManager;
	
	/* variables to detect when user touches the screen*/
	long start; 					
	long stop; 						
	
	/*x, y  coordinates of screen where user touched the screen*/
	int x, y;
	int lat = 0, lon = 0;

	GeoPoint touchedPoint;
	GeoPoint ourLocation;

	List<Overlay> overlayList;
	Drawable d;
	
	String towers;
	
	// code for voice recognition
	static final int check = 1111;
	ArrayList<String> voiceResults;
	
	//TextToSpeech tts;
	
	//booleans to remember state of the system
	boolean isWeatherOnFlag = false;
	boolean isTrafficOnFlag = false;
	
	String source, destination;
	String userLocality;
	Address userAddress;
	
	/* facebook instance variables*/
	public String APP_ID = "373032099415719";
	private FBRocket fbRocket;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		map = (MapView) findViewById(R.id.mvMain);
		map.setBuiltInZoomControls(true);

		Button b = (Button) findViewById(R.id.bVoice);
		b.setOnClickListener(this);

		// Initialize TextToSpeech to a listener
		/*tts = new TextToSpeech(NaviRangerActivity.this,	new TextToSpeech.OnInitListener() 
		{
			@Override
			public void onInit(int status) 
			{
				// TODO Auto-generated method stub
				if (status != TextToSpeech.ERROR) 
				{
					tts.setLanguage(Locale.US);
				}
			}
		});*/

		Touch t = new Touch();
		overlayList = map.getOverlays();
		overlayList.add(t);

		compass = new MyLocationOverlay(NaviRangerActivity.this, map);
		overlayList.add(compass);

		controller = map.getController();

		d = getResources().getDrawable(R.drawable.point);

        GeoPoint point = new GeoPoint(51643234, 7848593);
        controller.animateTo(point);
        controller.setZoom(10);
		
		// placing pinpoint at current location
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		Criteria crit = new Criteria();

		// provides name of provider that gives best signals
		towers = locationManager.getBestProvider(crit, true);

		Location location = locationManager.getLastKnownLocation(towers);
		//locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 1, locationListener);
		
		if (location != null) 
		{
			lat = (int) (location.getLatitude() * 1E6);
			lon = (int) (location.getLongitude() * 1E6);
			ourLocation = new GeoPoint(lat, lon);
			OverlayItem overlayItem = new OverlayItem(ourLocation, "String1","String2");
			CustomPinpoint custom = new CustomPinpoint(d, NaviRangerActivity.this);
			custom.insertPinpoint(overlayItem);
			overlayList.add(custom);
			controller.animateTo(ourLocation);
			controller.setZoom(12);
		}
		else 
		{
			Toast.makeText(getBaseContext(), "Cannot get provider",Toast.LENGTH_LONG).show();
		}
	}

	@Override
	protected void onPause() 
	{
		// TODO Auto-generated method stub
		super.onPause();
		compass.disableCompass();

		locationManager.removeUpdates(this);
	}

	@Override
	protected void onResume() 
	{
		// TODO Auto-generated method stub
		super.onResume();
		compass.enableCompass();

		locationManager.requestLocationUpdates(towers, 500, 1, this);
	}

	@Override
	protected boolean isRouteDisplayed() 
	{
		// TODO Auto-generated method stub
		return true;
	}
	
	@Override
	protected boolean isLocationDisplayed() 
	{
		// TODO Auto-generated method stub
		return true;
	}

	class Touch extends Overlay 
	{
		public boolean onTouchEvent(MotionEvent e, MapView m) 
		{
			if (e.getAction() == MotionEvent.ACTION_DOWN) 
			{
				start = e.getEventTime();
				x = (int) e.getX();
				y = (int) e.getY();
				touchedPoint = map.getProjection().fromPixels(x, y);
			}
			if (e.getAction() == MotionEvent.ACTION_UP) 
			{
				stop = e.getEventTime();
				x = (int) e.getX();
				y = (int) e.getY();
			}
			if (stop - start > 1500) 
			{
				// perform an action
				AlertDialog alert = new AlertDialog.Builder(NaviRangerActivity.this).create();
				alert.setTitle("Pick an Option");
				alert.setMessage("Choose what You want to do");
				alert.setButton("Place a Pinpoint",new DialogInterface.OnClickListener() 
				{
					@Override
					public void onClick(DialogInterface dialog,int which) 
					{
						// TODO Auto-generated method stub
						OverlayItem overlayItem = new OverlayItem(touchedPoint, "String1", "String2");
						CustomPinpoint custom = new CustomPinpoint(d,NaviRangerActivity.this);
						custom.insertPinpoint(overlayItem);
						overlayList.add(custom);

					}
				});
				alert.setButton2("Get Address",new DialogInterface.OnClickListener() 
				{
					@Override
					public void onClick(DialogInterface dialog,int which) 
					{
						// TODO Auto-generated method stub
						// get information about specific latitude and longitude
						Geocoder geocoder = new Geocoder(getBaseContext(), Locale.getDefault());
						try 
						{
							List<Address> address = geocoder.getFromLocation(
							touchedPoint.getLatitudeE6() / 1E6,touchedPoint.getLongitudeE6() / 1E6,1);
							if (address.size() > 0) 
							{
								String display = "";
								for (int i = 0; i < address.get(0).getMaxAddressLineIndex(); i++) 
								{
									display += address.get(0).getAddressLine(i) + "\n";
								}
								Toast t = Toast.makeText(getBaseContext(), display,Toast.LENGTH_LONG);
								t.show();
							}
						} 
						catch (IOException e) 
						{
							e.printStackTrace();
						}
						finally 
						{
						}
					}
				});

				alert.setButton3("Toggle View",new DialogInterface.OnClickListener() 
				{
					@Override
					public void onClick(DialogInterface dialog,int which) 
					{
						// TODO Auto-generated method stub
						if (map.isSatellite()) 
						{
							map.setSatellite(false);
							map.setStreetView(true);
						}
						else
						{
							map.setSatellite(true);
							map.setStreetView(false);
						}
					}
				});

				alert.show();

				return true;
			}
			return false;
		}
	}

	@Override
	public void onLocationChanged(Location location) 
	{
		// TODO Auto-generated method stub
		lat = (int) (location.getLatitude() * 1E6);
		lon = (int) (location.getLongitude() * 1E6);

		// Create a pin point at updated location as provided by
		// locationListener
		ourLocation = new GeoPoint(lat, lon);
		OverlayItem overlayItem = new OverlayItem(ourLocation, "String1",
				"String2");
		CustomPinpoint custom = new CustomPinpoint(d, NaviRangerActivity.this);
		custom.insertPinpoint(overlayItem);
		overlayList.add(custom);
	}

	@Override
	public void onProviderDisabled(String provider) 
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderEnabled(String provider) 
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) 
	{
		// TODO Auto-generated method stub

	}

	// onClick method for voice recognition
	@Override
	public void onClick(View v) 
	{
		// TODO Auto-generated method stub
		Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		i.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak up");
		startActivityForResult(i, check);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) 
	{
		//super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == check && resultCode == RESULT_OK) 
		{
			//Toast.makeText(getBaseContext(), requestCode + ", " + resultCode, Toast.LENGTH_LONG).show();
			voiceResults = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
			if (voiceResults != null) 
			{
				Toast.makeText(getBaseContext(), voiceResults.get(0).toString(),Toast.LENGTH_LONG).show();
				NLResult result = NLParser.parser(voiceResults.get(0).toString());
				Toast.makeText(getBaseContext(), result.functor + " " + result.arg1, Toast.LENGTH_LONG).show();
				performVoiceCmd(result);
			}
			else 
			{
				Toast.makeText(getBaseContext(), "Returned null results",Toast.LENGTH_LONG).show();
			}
			
		}
		else if(requestCode == 1 && resultCode == RESULT_OK)
		{
			source = data.getExtras().getString("src");
			destination = data.getExtras().getString("dest");
			//Toast.makeText(getBaseContext(), source + " " +  destination, Toast.LENGTH_SHORT).show();
			drawRoute(source,destination);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		// TODO Auto-generated method stub
		super.onCreateOptionsMenu(menu);
		MenuInflater blowUp = getMenuInflater();
		blowUp.inflate(R.menu.navi_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		switch (item.getItemId()) 
		{
			case R.id.showMyLocation:
				showCurrentLocation();
				break;
			case R.id.showRoute:
				Intent getLocationsIntent = new Intent(NaviRangerActivity.this, LocNames.class);
				startActivityForResult(getLocationsIntent, 1);
				break;
			case R.id.showWeather:
				if(isWeatherOnFlag == false)
				{
					isWeatherOnFlag = true;
					showWeather();
				}
				else
				{
					isWeatherOnFlag = false;
					// remove weather overlays from map		
				}
				break;
			case R.id.shareLocation:
				fbShareLocation();
				break;
			/*case R.id.remoteWeather:
				Intent remoteWeatherIntent = new Intent(NaviRangerActivity.this, LocNames.class);
				startActivityForResult(remoteWeatherIntent, 2);
				break;*/
			case R.id.traffic:
				handleTraffic();
				break;
		}
		return false;
	}

	public void showCurrentLocation() 
	{
		// placing pinpoint at current location
		
		if(ourLocation!= null)
		{
			controller.animateTo(ourLocation);
			controller.setZoom(14);
		}
		else
		{
			Toast.makeText(getBaseContext(), "Cannot get Location",Toast.LENGTH_LONG).show();
		}
	}

	public void handleTraffic()
	{
		if(isTrafficOnFlag == false)
		{
			isTrafficOnFlag = true;
			map.setTraffic(true);
		}
		else if(isTrafficOnFlag == true)
		{
			isTrafficOnFlag = false;
			map.setTraffic(false);
		}
		
	}
	
	public void removeTraffic()
	{
		if(isTrafficOnFlag == true)
		{
			isTrafficOnFlag = false;
			map.setTraffic(false);
		}
	}
	
	/*public void speakUp(String text) 
	{
		tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
	}*/
	
	public void currentTemp()
	{
		
	}
		
	public String getCurrentAddress()
	{
		Geocoder geocoder = new Geocoder(getBaseContext(), Locale.getDefault());
		String reply = "";
		String display = "";
		String locality = "";
		
		try 
		{
			List<Address> address = geocoder.getFromLocation(ourLocation.getLatitudeE6() / 1E6,ourLocation.getLongitudeE6() / 1E6,1);
			if (address.size() > 0) 
			{
				for (int i = 0; i < address.get(0).getMaxAddressLineIndex(); i++) 
				{
					display += address.get(0).getAddressLine(i) + "\n";
				}
				//Toast.makeText(getBaseContext(), locality + display,Toast.LENGTH_LONG).show();
				
				if((locality = address.get(0).getLocality()) != null)
				{
					reply = locality + display;
				}
				else
				{
					reply = display;
				}
			}
		} catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		finally 
		{

		}
		return locality + display;
	}
	
	public String getCurrentAddress(GeoPoint geoPoint)
	{
		String result = " Sorry cannot get current Address";
		Geocoder geocoder = new Geocoder(getBaseContext(), Locale.getDefault());
		try 
		{
			List<Address> address = geocoder.getFromLocation(ourLocation.getLatitudeE6() / 1E6,ourLocation.getLongitudeE6() / 1E6,1);
			if (address.size() > 0) 
			{
				for (int i = 0; i < address.get(0).getMaxAddressLineIndex(); i++) 
				{
					userAddress = address.get(0);
				}
			}
			/*if(userAddress.getAddressLine(0)!=null)
			{
				result  = userAddress.getAddressLine(0) + ",";
			}
			if(userAddress.getLocality()!=null)
			{
				result  = userAddress.getLocality() + ",";
			}
			if(userAddress.getAdminArea()!=null)
			{
				result  = userAddress.getAdminArea();
			}		*/

			result = userAddress.getAddressLine(0) + "," + userAddress.getLocality() + "," + userAddress.getAdminArea();
			return result;
		} catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		finally 
		{}
		
		return result;
	}
	
	public String getUserAddress()
	{
		String strAddress = "";
		Geocoder geocoder = new Geocoder(getBaseContext(), Locale.getDefault());
		try 
		{
			List<Address> address = geocoder.getFromLocation(ourLocation.getLatitudeE6() / 1E6,ourLocation.getLongitudeE6() / 1E6,1);
			if (address.size() > 0) 
			{
				for (int i = 0; i < address.get(0).getMaxAddressLineIndex(); i++) 
				{
					userAddress = address.get(0);
				}
			}
			
			if(userAddress.getAddressLine(0)!=null)
			{
				strAddress  = userAddress.getAddressLine(0) + ",";
			}
			if(userAddress.getLocality()!=null)
			{
				strAddress  = userAddress.getLocality() + ",";
			}
			if(userAddress.getAdminArea()!=null)
			{
				strAddress  = userAddress.getAdminArea() + ",";
			}			
			Log.i("getUserAddress", strAddress);
			return strAddress;
		} catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		finally 
		{}
		
		return strAddress;
	}

	public void showWeather()
	{
		String locality = "";
		Geocoder geocoder = new Geocoder(getBaseContext(), Locale.getDefault());
		try 
		{
			List<Address> address = geocoder.getFromLocation(ourLocation.getLatitudeE6() / 1E6, ourLocation.getLongitudeE6() / 1E6, 1);
			if (address.size() > 0) 
			{
				locality = address.get(0).getLocality();
			}
		}
		catch(Exception ex){
			Log.i("NaviException", "Cannot get locality");
		}
		//Toast.makeText(getApplicationContext(), locality, Toast.LENGTH_LONG).show();
		
		//locality = "Gainesville";
		URL url;
		String cityParamString = locality;
		String queryString = "http://www.google.com/ig/api?weather="+cityParamString;
		try 
		{
			url = new URL(queryString.replace(" ", "%20"));
			
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser sp = spf.newSAXParser();
			XMLReader xr = sp.getXMLReader();
			
			/*
			 * Create a new ContentHandler and apply it to the
			 * XML-Reader
			 */
			GoogleWeatherHandler gwh = new GoogleWeatherHandler();
			xr.setContentHandler(gwh);
			
			/* Parse the xml-data our URL-call returned. */
			xr.parse(new InputSource(url.openStream()));

			/* Our Handler now provides the parsed weather-data to us. */
			WeatherSet ws = gwh.getWeatherSet();
			
			//Toast.makeText(getApplicationContext(), ws.getWeatherCurrentCondition().getHumidity() 
			//+ ws.getWeatherCurrentCondition().getTempCelcius(), Toast.LENGTH_LONG).show();
			
			GeoPoint weatherIconLoc = ourLocation;
			weatherIconLoc = new GeoPoint(weatherIconLoc.getLatitudeE6() -500, weatherIconLoc.getLongitudeE6() - 500);
						
			Drawable currentWeatherIcon = (Drawable)getWeatherIcon(ws.getWeatherCurrentCondition());	
			WeatherOverlay weatherOverlay = new WeatherOverlay(currentWeatherIcon, this);
			OverlayItem overlayItem = new OverlayItem(weatherIconLoc, " Temp: " + ws.getWeatherCurrentCondition().getTempCelcius() ,"Humidity " + 
					ws.getWeatherCurrentCondition().getHumidity() );
			weatherOverlay.addOverlay(overlayItem);
			overlayList.add(weatherOverlay);
		}
		catch (MalformedURLException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void showWeather(String remote)
	{
		GeoPoint remotePoint = searchLocationByName(getApplicationContext(), remote);
		
		if(remotePoint == null)
		{
			Toast.makeText(getBaseContext(), "Sorry, I did not understand ",	Toast.LENGTH_LONG).show();
			return;
		}
		
		URL url;
		String queryString = "http://www.google.com/ig/api?weather="+remote;
		try 
		{
			url = new URL(queryString.replace(" ", "%20"));
			
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser sp = spf.newSAXParser();
			XMLReader xr = sp.getXMLReader();
			
			GoogleWeatherHandler gwh = new GoogleWeatherHandler();
			xr.setContentHandler(gwh);
			
			/* Parse the xml-data our URL-call returned. */
			xr.parse(new InputSource(url.openStream()));

			/* Our Handler now provides the parsed weather-data to us. */
			WeatherSet ws = gwh.getWeatherSet();
			
			/* Get geopoint for remote location */
			GeoPoint weatherIconLoc = remotePoint;
			weatherIconLoc = new GeoPoint(weatherIconLoc.getLatitudeE6() -500, weatherIconLoc.getLongitudeE6() -500);
						
			Drawable currentWeatherIcon = (Drawable)getWeatherIcon(ws.getWeatherCurrentCondition());			
			WeatherOverlay weatherOverlay = new WeatherOverlay(currentWeatherIcon, this);
			OverlayItem overlayItem = new OverlayItem(weatherIconLoc, " Temp: " + ws.getWeatherCurrentCondition().getTempCelcius() ,"Humidity " + 
					ws.getWeatherCurrentCondition().getHumidity() );
			weatherOverlay.addOverlay(overlayItem);
			overlayList.add(weatherOverlay);
		}
		catch (MalformedURLException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public Drawable getWeatherIcon(WeatherCurrentCondition aWCIS) throws MalformedURLException
	{
		Drawable answer;
		/* Construct the Image-URL. */
		URL imgURL = new URL("http://www.google.com" + aWCIS.getIconURL());
		try 
		{
			URLConnection conn = imgURL.openConnection();
			conn.connect();
			InputStream is = conn.getInputStream();
			BufferedInputStream bis = new BufferedInputStream(is);
			Bitmap bm = BitmapFactory.decodeStream(bis);
			bis.close();
			is.close();
			answer = new BitmapDrawable(bm);
			return answer;
		}
		catch (IOException e) 
		{
			/* Reset to 'Dunno' on any error. */
			Bitmap dunnoBm = BitmapFactory.decodeResource(getResources(), R.drawable.dunno);
			answer = new BitmapDrawable(dunnoBm);
		}
		return answer;
	}
		
	private List<GeoPoint> decodePoly(String encoded) 
	{

	    List<GeoPoint> poly = new ArrayList<GeoPoint>();
	    int index = 0, len = encoded.length();
	    int lat = 0, lng = 0;

	    while (index < len) {
	        int b, shift = 0, result = 0;
	        do {
	            b = encoded.charAt(index++) - 63;
	            result |= (b & 0x1f) << shift;
	            shift += 5;
	        } while (b >= 0x20);
	        int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
	        lat += dlat;

	        shift = 0;
	        result = 0;
	        do {
	            b = encoded.charAt(index++) - 63;
	            result |= (b & 0x1f) << shift;
	            shift += 5;
	        } while (b >= 0x20);
	        int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
	        lng += dlng;

	        GeoPoint p = new GeoPoint((int) (((double) lat / 1E5) * 1E6),
	             (int) (((double) lng / 1E5) * 1E6));
	        poly.add(p);
	    }

	    return poly;
	}

	public void drawRoute(String source, String destination)
	{
		GeoPoint srcGeoPoint = null;
		GeoPoint destGeoPoint = null;
		String strURL =null ;	
		String strAddress = null;
		String destAddress = null;
		//srcGP = searchLocationByName(getApplicationContext(), source);
		//destGP = searchLocationByName(getApplicationContext(), destination);
		
		if(source.equalsIgnoreCase("current location"))
		{
			source = getCurrentAddress(ourLocation);
			
			/*Geocoder geocoder = new Geocoder(getBaseContext(), Locale.getDefault());
			srcGeoPoint = new GeoPoint((int) (ourLocation.getLatitudeE6() * 1E6), (int) (ourLocation.getLongitudeE6() * 1E6));
			destGeoPoint = searchLocationByName(getApplicationContext(),destination);
			try{
				List<Address> address = geocoder.getFromLocation(ourLocation.getLatitudeE6()/1E6,ourLocation.getLongitudeE6()/1E6,1);
				if (address.size() > 0) 
				{					
					userAddress = address.get(0);
					if(address.get(0).getLocality() != null)
					{
						if(userAddress.getAddressLine(0)!=null)
						{
							strAddress  = userAddress.getAddressLine(0) + ",";
						}
						if(userAddress.getLocality()!=null)
						{
							strAddress  = userAddress.getLocality() + ",";
						}
						if(userAddress.getAdminArea()!=null)
						{
							strAddress  = userAddress.getAdminArea();
						}		
						
						//source = userLocality;
					}
					else
					{
						Toast.makeText(getApplicationContext(), "Cannot get User's current location", Toast.LENGTH_SHORT).show();
					}
				}
				//source = getUserAddress();
			}catch (IOException e) 
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}finally{ }*/
		}
		else if(destination.equalsIgnoreCase("current location"))
		{
			destination = getCurrentAddress(ourLocation);
			/*Geocoder geocoder = new Geocoder(getBaseContext(), Locale.getDefault());
			destGeoPoint = new GeoPoint((int) (ourLocation.getLatitudeE6() * 1E6), (int) (ourLocation.getLongitudeE6() * 1E6));
			srcGeoPoint = searchLocationByName(getApplicationContext(),source);
			try{
				List<Address> address = geocoder.getFromLocation(ourLocation.getLatitudeE6() / 1E6,ourLocation.getLongitudeE6() / 1E6,1);
				if (address.size() > 0) 
				{					
					if((userLocality = address.get(0).getLocality()) != null){
						destination = userLocality;
					}
					else{
						Toast.makeText(getApplicationContext(), "Cannot get User's current location", Toast.LENGTH_SHORT).show();
					}
				}
				destination = getUserAddress();
			}catch (IOException e) {
				e.printStackTrace();
			} 
			finally { }*/
		}
			strURL = "http://maps.google.com/maps/api/directions/xml?origin=" + source + 
				"&destination=" + destination + "&sensor=false&mode=driving";
		
		Log.i("RouteStr", strURL);
		String url = strURL.replace(" ", "%20");
	
		HttpGet get = new HttpGet(url);
		
		String strResult = "";
		try 
		{
			HttpParams httpParameters = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(httpParameters, 3000);
			HttpClient httpClient = new DefaultHttpClient(httpParameters); 
			
			HttpResponse httpResponse = null;
			httpResponse = httpClient.execute(get);
			
			if (httpResponse.getStatusLine().getStatusCode() == 200){
				strResult = EntityUtils.toString(httpResponse.getEntity());
			}
			

			if (-1 == strResult.indexOf("<status>OK</status>")){
				this.finish();
				return;
			}
			
			int pos = strResult.indexOf("<overview_polyline>");
			pos = strResult.indexOf("<points>", pos + 1);
			int pos2 = strResult.indexOf("</points>", pos);
			strResult = strResult.substring(pos + 8, pos2);
			
			List<GeoPoint> points = decodePoly(strResult);
			
			RouteOverlay mOverlay = new RouteOverlay(points);
			overlayList.add(mOverlay);
			
			if (points.size() >= 2){
				controller.animateTo(points.get(0));
			}
			 
			map.invalidate();
		}
		catch (Exception e) 
		{
			Toast.makeText(getApplicationContext(), "Could not find the route, please make sure Source and Destination are valid cities", Toast.LENGTH_LONG).show();
			return;
		}
		
	}	
	
	public void performVoiceCmd(NLResult command)
	{
		try{
			if(command.functor.equalsIgnoreCase("showWeather"))
			{
				if(command.arg1.equalsIgnoreCase("current"))
				{
					if(isWeatherOnFlag == false)
					{
						showWeather();
					}
				}
				else
				{
					showWeather(command.arg1);
				}
			}
			
			if(command.functor.equalsIgnoreCase("showTraffic"))
			{
				handleTraffic();
			}
			
			if(command.functor.equalsIgnoreCase("removeTraffic"))
			{
				removeTraffic();
			}
			
			if(command.functor.equalsIgnoreCase("drawRoute") && (!command.arg1.equals(null)))
			{
				drawRoute(command.arg1, command.arg2);
			}
			if(command.functor.equalsIgnoreCase("fbShare"))
			{
				fbShareLocation();
			}
			
		}
		catch(Exception ex)
		{
			Toast.makeText(getBaseContext(), "Sorry I did not understand what you mean" ,	Toast.LENGTH_LONG).show();
		}
	}
	
	public GeoPoint searchLocationByName(Context context, String locationName){
	    Geocoder geoCoder = new Geocoder(context, Locale.getDefault());
	    GeoPoint gp = null;
	    Address ad = null;
	    try {
	        List<Address> addresses = geoCoder.getFromLocationName(locationName, 1);
	        for(Address address : addresses){
	            gp = new GeoPoint((int)(address.getLatitude() * 1E6), (int)(address.getLongitude() * 1E6));
	            address.getAddressLine(1);
	            ad = address;
	        }       
	        Toast.makeText(getBaseContext(), "Remote loc" + ad.getLocality(),	Toast.LENGTH_LONG).show();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	    return gp;
	}
	
	public void fbCheckin()
	{
		
		fbRocket = new FBRocket(this, "PitchFork TRY",
				"c3968bbdd9dc7f44a8e05b7346f46673");

		if (fbRocket.existsSavedFacebook()) {
			fbRocket.loadFacebook();
		} else {
			fbRocket.login(R.layout.fbmain);
		}
	}
	
	@Override
	public void onLoginFail() {
		fbRocket.displayToast("Login failed!");
		fbRocket.login(R.layout.fbmain);
	}
	
	@Override
	public void onLoginSuccess(Facebook facebook) {
		// TODO Auto-generated method stub
	    SimpleDateFormat sdf = new SimpleDateFormat("dd:MM:yyyy");
	    SimpleDateFormat sdf1 = new SimpleDateFormat("HH:mm:ss");
	    String data = sdf.format(new Date());
	    String time = sdf1.format(new Date());

	    try {

	        /*facebook.setStatus("Currently at: " +  mAddress.getAddressLine(0) + "\nfeature " + mAddress.getFeatureName() 
	        		+ "\nlocaclity " +  mAddress.getLocality() + "\ncountry name " + mAddress.getCountryName() + ", ");*/
	    	facebook.setStatus("Currently at: " + getCurrentAddress());
	        fbRocket.displayToast("Status Posted Successfully!! ");
	        return ;
	    } catch (ServerErrorException e) {
	        if (e.notLoggedIn()) {
	            fbRocket.login(R.layout.main);
	        } else {
	            System.out.println(e);
	        }

	    }
	}

	public String checkingMessage()
	{
		//String strURL = "https://maps.googleapis.com/maps/api/place/check-in/xml?sensor=true&key=" + GOOGLE_API_KEY;
		URL url;
		String strURL = "https://maps.googleapis.com/maps/api/place/search/xml?location=-33.8670522,151.1957362&radius=500&types=food&name=harbour&sensor=false&key=AIzaSyApaR6YsK60Ru5Uh1djC0BCPBCJ0OXDGT0";
		try 
		{
			url = new URL(strURL.replace(" ", "%20"));

			// Standard of reading a XML file
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			DocumentBuilder builder;
			Document doc = null;
			XPathExpression expr = null;
			builder = factory.newDocumentBuilder();
			
			doc = builder.parse(new InputSource( url.openStream()));
			
			// Create a XPathFactory
			XPathFactory xFactory = XPathFactory.newInstance();

			// Create a XPath object
			XPath xpath = xFactory.newXPath();

			// Compile the XPath expression
			expr = xpath.compile("//result/name/text()");
			// Run the query and get a nodeset
			Object result = expr.evaluate(doc, XPathConstants.NODESET);
			NodeList nodes = (NodeList) result;
			
			String answer = nodes.item(0).getNodeValue();
			Toast.makeText(getApplicationContext(), answer, Toast.LENGTH_LONG);
			
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
			return null;
		
	}
	
	
	public void fbShareLocation()
	{
		Intent mIntent = new Intent(this, FBShare.class);   
		Bundle extras = new Bundle();
		String currentAddress = getCurrentAddress(ourLocation);
		//Toast.makeText(getBaseContext(), getCurrentAddress(ourLocation), Toast.LENGTH_SHORT).show();
		extras.putString("userAddress", "Currently at: " + currentAddress);
		mIntent.putExtras(extras);
		startActivity(mIntent);
	}
	
}