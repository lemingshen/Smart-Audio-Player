package com.example.smartaudioplayer;

import android.app.Activity;
import android.media.SoundPool;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.example.smartaudioplayer.databinding.ActivityMainBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class MainActivity extends Activity
{
	private ArrayList<String> time_list;
	private ActivityMainBinding binding;
	private ListView listView;
	private SoundPool soundPool;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		binding = ActivityMainBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		/* bind the listview to local variable */
		listView = binding.timeList;
		time_list = new ArrayList<>();
	}

	public void play_sound(View v)
	{
		/* get the current timestamp */
		long current_timestamp = System.currentTimeMillis();
		SimpleDateFormat date_format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss SSS");

		/* update the time list when click the play button */
		time_list.add(date_format.format(current_timestamp));
		ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, time_list);
		listView.setAdapter(adapter);

		/* audio player initialization */
		soundPool = new SoundPool.Builder().setMaxStreams(1).build();
		/* the parameter 'test' means the 'test.wav' file located in 'res/raw'		*
		 * you can add your sound file in the 'raw' folder and modify the parameter */
		soundPool.load(this, R.raw.test, 1);

		/* after loading file completes, play the sound *
		 * loop = -1 means repeatedly play the sound    */
		soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener()
		{
			@Override
			public void onLoadComplete(SoundPool soundPool, int sampleId, int status)
			{
				soundPool.play(1, 1, 1, 1, -1, 1);
			}
		});
	}

	public void stop_sound(View v)
	{
		soundPool.stop(1);
		soundPool.release();
		soundPool = null;
	}
}