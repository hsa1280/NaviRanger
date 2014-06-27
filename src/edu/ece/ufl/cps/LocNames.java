package edu.ece.ufl.cps;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class LocNames extends Activity implements OnClickListener
{
	EditText sourceText;
	EditText destinationText;
	
	String source = "";
	String destination = "";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.loc_names);		
		
		sourceText = (EditText)findViewById(R.id.sourceText);
		destinationText = (EditText)findViewById(R.id.destinationText);
		Button bLocations = (Button) findViewById(R.id.routeButton);
		bLocations.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) 
	{
		// TODO Auto-generated method stub
		source = sourceText.getText().toString();
		destination = destinationText.getText().toString();		
		//Toast.makeText(getBaseContext(), source + " " +  destination, Toast.LENGTH_SHORT).show();
		Intent result = new Intent();
		result.putExtra("src", source);
		result.putExtra("dest", destination);
		setResult(RESULT_OK, result);
		finish();
	}
	
	

}
