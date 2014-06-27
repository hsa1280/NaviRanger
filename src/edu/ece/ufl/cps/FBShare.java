package edu.ece.ufl.cps;

import net.xeomax.FBRocket.FBRocket;
import net.xeomax.FBRocket.Facebook;
import net.xeomax.FBRocket.LoginListener;
import net.xeomax.FBRocket.ServerErrorException;
import android.app.Activity;
import android.content.Intent;
import android.location.Address;
import android.os.Bundle;

public class FBShare extends Activity implements LoginListener {

	private FBRocket fbRocket;
	public static String currentFileName ;
    private String strAddress;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		shareFacebook();          
		Intent myIntent = getIntent(); // this getter is just for example purpose, can differ
		if (myIntent !=null && myIntent.getExtras()!=null){
			strAddress = getIntent().getExtras().getString("userAddress");  
		}
		//finish();
	}

	public void shareFacebook() {
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
		fbRocket.login(R.layout.main);
	}
	
	

	@Override
	public void onLoginSuccess(Facebook facebook) {
		// TODO Auto-generated method stub
	    try {
	        facebook.setStatus(strAddress);
	        fbRocket.displayToast("Status Posted Successfully!! ");
	        finish();
	        return ;
	    }
	    catch (ServerErrorException e) 
	    {
	        if (e.notLoggedIn()) 
	        {
	            fbRocket.login(R.layout.main);
	        } else {
	            System.out.println(e);
	            finish();
	        }
	    }
	}
}