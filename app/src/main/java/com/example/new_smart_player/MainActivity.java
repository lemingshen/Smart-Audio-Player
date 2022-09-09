package com.example.new_smart_player;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.SoundPool;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity
{
	private ArrayList<String> time_list;
	private ListView listView;
	private SoundPool soundPool;
	private AudioPlayer player;
	private String normal_path, chirp_path;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		boolean result = false;

		String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};

		File file1 = new File(getExternalCacheDir(), "normal.txt");
		File file2 = new File(getExternalCacheDir(), "chirp.txt");
		normal_path = file1.getAbsolutePath();
		chirp_path = file2.getAbsolutePath();

		try
		{
			if (!file1.exists())
			{
				file1.createNewFile();
			}

			if (!file2.exists())
			{
				file2.createNewFile();
			}

			result = true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		if (result)
		{
			Toast toast = Toast.makeText(getApplicationContext(), "files created successfully", Toast.LENGTH_LONG);
			toast.show();
		}

		ActivityCompat.requestPermissions(this, permissions, 10000);

		player = new AudioPlayer();
		listView = (ListView) findViewById(R.id.time_list);
		time_list = new ArrayList<>();
	}

	/* mode = true for playing specified sound, false for playing chirp */
	public void log_time(String content, boolean mode)
	{
		try
		{
			File file = null;

			if (mode)
			{
				file = new File(normal_path);

			}
			else
			{
				file = new File(chirp_path);
			}

			FileOutputStream outputStream = new FileOutputStream(file, true);

			OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
			writer.write(content + "\n\n\n");
			writer.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void play_sound(View v)
	{
		/* get the current timestamp */
		long current_timestamp = System.currentTimeMillis();
		SimpleDateFormat date_format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss SSS");

		log_time(date_format.format(current_timestamp), true);

		/* update the time list when click the play button */
		time_list.add("file " + date_format.format(current_timestamp));
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
		if (soundPool != null)
		{
			soundPool.stop(1);
			soundPool.release();
			soundPool = null;
		}

		player.stop_sound();
	}

	public void play_chirp(View v)
	{
		double start_frequency = Double.parseDouble(((EditText) findViewById(R.id.start_frequency)).getText().toString());
		double end_frequency = Double.parseDouble(((EditText) findViewById(R.id.end_frequency)).getText().toString());
		double duration = Double.parseDouble(((EditText) findViewById(R.id.duration)).getText().toString());

		player.play_sound(start_frequency, end_frequency, 50, duration);
		/* get the current timestamp */
		long current_timestamp = System.currentTimeMillis();
		SimpleDateFormat date_format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss SSS");

		log_time(date_format.format(current_timestamp), false);

		/* update the time list when click the play button */
		time_list.add("chirp " + date_format.format(current_timestamp));
		ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, time_list);
		listView.setAdapter(adapter);

//		Toast.makeText(MainActivity.this, "playing chirp sound", Toast.LENGTH_SHORT).show();
	}
}

class AudioPlayer
{
	/* type of sound stream */
	private static final int steam_type = AudioManager.STREAM_MUSIC;

	/* specify the sample rate */
	private static final int sample_rate = 44100;

	/* double audio channel */
	private static final int channel = AudioFormat.CHANNEL_OUT_STEREO;

	/* specify digitalizing bit */
	private static final int audio_format = AudioFormat.ENCODING_PCM_16BIT;

	/* specify the buffer size */
	private int mMinBufferSize;

	/* stream mode */
	private static int MODE = AudioTrack.MODE_STREAM;

	/* using multi-thread */
	private boolean isPlaying = false;
	private thread_play playing_thread;

	private AudioTrack track;
	private int buffer_size;

	/* initialization */
	public AudioPlayer()
	{
		buffer_size = AudioTrack.getMinBufferSize(sample_rate, channel, audio_format);
		this.track = new AudioTrack(steam_type, sample_rate, channel, audio_format, buffer_size, MODE);
	}

	/* playing chirp sound */
	public void play_sound(double start_rate, double end_rate, int db, double duration)
	{
		this.playing_thread = new thread_play(start_rate, end_rate, db, duration);
		playing_thread.start();
	}

	public void play(double start_rate, double end_rate, int db, double duration)
	{
		start_rate *= 1000;
		end_rate *= 1000;

		System.out.println(start_rate);
		System.out.println(end_rate);

		/* chirp generation */
		int length = (int) (end_rate - start_rate) + 1;
		int sample_per_frequency = (int) (sample_rate * duration / length);
		int size = sample_per_frequency * length;
		short amplitude = Short.MAX_VALUE;
		short[] stream = new short[size];

		amplitude = (short) ((Math.pow(10.0, db / 20.0) * Math.sqrt(2)) * amplitude);

		for (int i = 0; i < length; i++)
		{
			for (int j = 0; j < sample_per_frequency; j++)
			{
				double x = 2.0 * Math.PI * i / 44100.0;
				stream[i * sample_per_frequency + j] = (short) (amplitude * Math.sin(x * j));
			}
		}

		this.track.play();
		this.track.write(stream, 0, size);
	}

	/* using thread */
	class thread_play extends Thread
	{
		private double start_rate, end_rate, duration;
		private int db;

		public thread_play(double start_rate, double end_rate, int db, double duration)
		{
			this.start_rate = start_rate;
			this.end_rate = end_rate;
			this.db = db;
			this.duration = duration;
		}

		@Override
		public void run()
		{
			try
			{
				if (!isPlaying)
				{
					isPlaying = true;

					while (isPlaying)
					{
						play(this.start_rate, this.end_rate, this.db, duration);
					}
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	public void stop_sound()
	{
		this.isPlaying = false;
		this.track.stop();
	}

	public void pause_sound()
	{
		this.track.pause();
	}
}