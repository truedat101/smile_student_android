/**
Copyright 2012-2013 SMILE Consortium, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

	http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
**/

package org.smilec.smile.student;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Formatter;
import java.util.Locale;
import java.util.Vector;
import org.json.JSONArray;
import org.json.JSONException;
import android.widget.RatingBar;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import android.webkit.WebChromeClient;

public class CourseList extends Activity implements OnDismissListener {

	String APP_TAG = "SMILE";
	String server_dir = "/smile/current/";

	WebView webviewQ;
	WebView webviewSR;
	WebView webviewSRD;
	WebView curwebview;
	WebView webviewPR;
	ImageView imageview;
	private RadioGroup rgb02;
	private RadioGroup rgb03;
	private RadioGroup rgb04; // for making question

	Vector<Integer> answer_arr;
	Vector<Integer> rating_arr; // per each question by individual
	Vector<Integer> right_answer;
	Vector<Integer> my_score;
	Vector<String> score_winner_name;
	Vector<String> rating_winner_name;
	Vector<String> final_avg_ratings; // per each question by all users
	Vector<String> r_answer_percents; // per each question by all users

	static int high_score;
	static float high_rating;

	private String question_arr[] = { "", "", "", "", "", "" };
	int LAST_SCENE_NUM = 0;
	//private String category_arr[] = { "Make Your Question", "Solve Questions", "See Results" };
	private String category_arr[];
	private String language_list[];
	String curcategory;

	static int selarridx  = 0; // 0:solve question 1: see results
	static int previewidx = 0; // 0:no preview 1:preview
	static int galleryidx = 0; // 1:been there 2: back no image
	int issolvequestion   = 0; // 0: solve question 1: see results

	static int chkimg = 0; // 0:no image 1: image selected
	static int choice02 = -2;
	static int choice03 = -2;
	static int scene_number;
	static int curusertype = 1;
	static String cururi;
	static String curusername;
	static String curlanguage;

	String myRightan;

	private boolean isImageSelected = false;

	AddPictureDialog add_pic_d = null;
	AndroidCustomGallery add_pic_g = null; // on 3/19/2012

	piechart draw_piechart;

	TextView notemkcontent;
	TextView notesolcontent;
	TextView noteseecontent;

	String stored_file_url;
	HttpMsgForStudent student;

	// variables regarding on quick_action
	ActionItem image_first;
	ActionItem image_second;
	ActionItem image_third;

	QuickAction image_qa;

	Boolean TakenImage = false;

	Boolean SolvingIndex = false; // if it is true, it means a user solved questions

	Uri imageUri;
	Uri ThumUri;
	Intent camera_intent;
	static Cursor mcursor_for_camera;
	int IMAGECAPTURE_OK = 0;
	String media_path;
	private int mNetworkFailureCount = 0;
	Activity activity;

	// ---------------------------------------------------------------------------------------------------------
	// communication channel from JunctionStudent -> main application
	// JunctionStudent -> main.setNewState -> [MessageHandler] -> handleMessage
	// -> main.change_todo_view_state
	// ---------------------------------------------------------------------------------------------------------
	public void setNewStateFromTeacher(int todo_number) { // called by junction object

		messageHandler.sendMessage(Message.obtain(messageHandler, todo_number));
		// below Handler will be called soon enough with 'todo_number' as
		// msg.what
	}

	public void setTranferStatus(boolean is_send, int time) { // called by junction object
		// 0: finished
		// 1~20: how many time has passed
		if (is_send)
			messageHandler.sendMessage(Message.obtain(messageHandler,
					HTTP_SEND_STATUS, time, 0));
		else
			messageHandler.sendMessage(Message.obtain(messageHandler,
					HTTP_PING_STATUS, time, 0));
		// below Handler will be called soon enough with 'todo_number' as
		// msg.what
	}

	void setHttpStatus(int kind, int time) {

		try {
			TextView http = (TextView) findViewById(R.id.HTTPText);
			if (time == 0)
				http.setText("");
			else {

				StringBuffer sb = new StringBuffer("");
				if (kind == HTTP_PING_STATUS){
					sb.append(getString(R.string.ConnectingSever));
				} else {
					sb.append(getString(R.string.Sending));
				}

				for (int i = 0; i < time; i++) {
					sb.append('.');
				}

				http.setText(sb.toString());

			}

		} catch (Exception e) {
		}

	}

	private Handler messageHandler = new Handler() {
		public void handleMessage(Message msg) {
			if ((msg.what == HTTP_PING_STATUS)
					|| (msg.what == HTTP_SEND_STATUS)) {
				int time = msg.arg1;
				setHttpStatus(msg.what, time);
			} else {
				if(msg.what == RE_TAKE)
				{
					last_state_received = WAIT_SOLVE_QUESTION;
					change_todo_view_state(SOLVE_QUESTION);
				}
				else if(msg.what == RE_START)
				{
					last_state_received = BEFORE_CONNECT;
					change_todo_view_state(INIT_WAIT);
				}
				else change_todo_view_state(msg.what);
			}
		};
	};

	public int getCurrentState() {
		return last_state_received;
	}

	public int getLanguageIndex() {

		int return_index = 0; //default (English)

		if(curlanguage.equals(language_list[0]))       return_index = 0;
		else if (curlanguage.equals(language_list[1])) return_index = 1;

		return return_index;

	}

	public String getMyName() {
		return this.curusername;
	}

	// States of junction_quiz
	public final static int HTTP_PING_STATUS = -10;
	public final static int HTTP_SEND_STATUS = -20;
	public final static int CONNECT_FAIL = -2;
	public final static int BEFORE_CONNECT = -1;
	public final static int INIT_WAIT = 0;
	public final static int MAKE_QUESTION = 1;
	public final static int WAIT_SOLVE_QUESTION = 2;
	public final static int SOLVE_QUESTION = 3;
	public final static int WAIT_SEE_RESULT = 4;
	public final static int SEE_RESULT = 5;
	public final static int FINISH = 6;
	public final static int RE_TAKE = 100;
	public final static int RE_START = 200;

	int last_state_received = BEFORE_CONNECT; // MAKE_QUESTION,
	String MY_IP;

	// This part is called when starting this app
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		getWindow().requestFeature(Window.FEATURE_PROGRESS);

		Resources res = getResources(); //get resources
		language_list = res.getStringArray(R.array.language_list);
		category_arr  = res.getStringArray(R.array.category_list);

		// First Extract the bundle from intent
		Bundle bundle = getIntent().getExtras();
		// Next extract the values using the key as
		curusertype = 1; // fixed (student)

		curusername    = bundle.getString("USERNAME");
		cururi         = bundle.getString("URI");
		curlanguage    = bundle.getString("CHOSEN_LANGUAGE");

		draw_piechart = new piechart(this); // to draw the piechart
		draw_piechart.onStart(100,0);

		add_pic_g = new AndroidCustomGallery (this); // on 3/19/2012s

		answer_arr   = new Vector<Integer>();
		rating_arr   = new Vector<Integer>();
		my_score     = new Vector<Integer>();
		right_answer = new Vector<Integer>();

		score_winner_name  = new Vector<String>();
		rating_winner_name = new Vector<String>();
		final_avg_ratings  = new Vector<String>();
		r_answer_percents  = new Vector<String>();

		// connection_created = false;
		_act = this;

		chkimg = 0; // no image
		image_qa = null;

		student = null; // student communicator is created by make_connection()

		// Adding quick action menu
		image_quickaction();

		MY_IP = get_IP();

		getStoredFileURL();
		create_connection();
		show_todo_view();

		// student.send_initial_message();

	}

	private void create_connection() {

		Log.d(APP_TAG, "Creating connection");
		student = null;

		Toast.makeText(this, getString(R.string.creatingconnection), Toast.LENGTH_SHORT).show();

		student = new HttpMsgForStudent(this, curusername, MY_IP);
		student.beginConnection(cururi);
	}

	private String get_IP() {

		try {
			Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
			//
			// Refactor this, this could cause problems if there are multiple NICs that get 
			// picked up
			//
			while (e.hasMoreElements()) {
				NetworkInterface ni = e.nextElement();
				Enumeration<InetAddress> ips = ni.getInetAddresses();
				while (ips.hasMoreElements()) {
					String ipa = ips.nextElement().toString();

					if (ipa.startsWith("/"))
						ipa = ipa.substring(1);

					if (ipa.indexOf(':') >= 0) { // IPv6. Ignore
						continue;
					}

					if (ipa.equals("127.0.0.1")) {
						continue; // loopback MY_IP. Not meaningful for out
									// purpose
					}

					Log.d(APP_TAG, ipa);
					return ipa;
				}
			}

		} catch (SocketException e) {

			e.printStackTrace();
		}

		return null;
	}

	@SuppressWarnings("deprecation")
	private void getStoredFileURL() {

		// save URL for stored file
		File data_dir = getBaseContext().getFilesDir();

		try {

			URL url = data_dir.toURL();
			stored_file_url = url.toString();
		} catch (Exception e) {
			Log.d(APP_TAG, "URL ERROR");
		}
	}

	TextView inform;
	Button exitTodoView;
	Button makeQ;
	Button solveQ;
	Button seeR;

	Boolean enabled_m = false;
	Boolean enabled_s = false;
	Boolean enabled_r = false;
	int status_t = 0;

	private void show_todo_view() {

		setTitle(curusername + "@SMILE Student");
		setContentView(R.layout.category);

		inform = (TextView) findViewById(R.id.ProgressText);

		makeQ        = (Button) findViewById(R.id.MKQbutton);
		solveQ       = (Button) findViewById(R.id.SOLQbutton);
		seeR         = (Button) findViewById(R.id.SEERbutton);
		exitTodoView = (Button) findViewById(R.id.exitbutton);

		makeQ.setText(category_arr[0]);
		solveQ.setText(category_arr[1]);
		seeR.setText(category_arr[2]);
		exitTodoView.setText(R.string.category_exit);

		makeQ.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				change_todo_view_state(WAIT_SOLVE_QUESTION);
				student.can_rest_now();
				MakeQuestion();

			}
		});
		solveQ.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				change_todo_view_state(WAIT_SEE_RESULT);
				student.can_rest_now();
				SolveQuestion();
				selarridx = 0;
			}
		});

		seeR.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				change_todo_view_state(FINISH);
				// comment out for re-take the quiz and start a new session
				// student.can_rest_now();
				seeResults();
				selarridx = 1;
			}
		});

		exitTodoView.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {

				final Builder adb = new AlertDialog.Builder(CourseList.this);

				adb.setTitle(getString(R.string.warn));
				adb.setMessage(getString(R.string.exit_q));
				adb.setPositiveButton(getString(R.string.OK),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface arg0, int arg1) {
								CourseList.this.finish();
							}
						});

				adb.setNegativeButton(getString(R.string.Cancel), null);
				adb.show();

			}
		});

		change_todo_view_state(this.last_state_received); // init_state
	}

	public void onDestroy() {
		if (student != null)
			student.clean_up();
		super.onDestroy();
	}

	public void setStatusText(String msg, boolean showToast) {
		inform.setText(msg);
		if (showToast) {
			Toast.makeText(this, msg, Toast.LENGTH_LONG);
		}
	}
	
	@SuppressLint("ShowToast")
	private void change_todo_view_state(int state) {

		if (state == CONNECT_FAIL) {
			mNetworkFailureCount++;
			Toast.makeText(this, getString(R.string.network_warning), Toast.LENGTH_LONG);
			inform.setText(getString(R.string.network_warning) + ":" + mNetworkFailureCount);
			// Don't kill the activity
			return;
		}

		if (state < last_state_received) { // repeated messag
			Log.d(APP_TAG, "repeated todo status:" + state + ", curr:"
					+ last_state_received);
			return; // cannot go back state.
		}

		Log.d(APP_TAG, "New todo status " + state);
		last_state_received = state;

		switch (state) {

		case BEFORE_CONNECT: // wait
			makeQ.setEnabled(false);
			solveQ.setEnabled(false);
			seeR.setEnabled(false);
			inform.setText(R.string.inform_state_before_connect);
			break;

		case INIT_WAIT: // wait
			makeQ.setEnabled(false);
			solveQ.setEnabled(false);
			seeR.setEnabled(false);
			inform.setText(R.string.inform_state_ini_wait);
			break;

		case MAKE_QUESTION: // press button for making
			makeQ.setEnabled(true);
			solveQ.setEnabled(false);
			seeR.setEnabled(false);
			inform.setText(R.string.inform_state_make_question);
			break;

		case WAIT_SOLVE_QUESTION: // wait solving
			makeQ.setEnabled(false);
			solveQ.setEnabled(false);
			seeR.setEnabled(false);
			inform.setText(R.string.inform_state_wait_solve_question);
			break;

		case SOLVE_QUESTION: // press button for solving
			// clear cache of webview
			makeQ.setEnabled(false);
			solveQ.setEnabled(true);
			seeR.setEnabled(false);
			inform.setText(R.string.inform_state_solve_question);
			break;

		case WAIT_SEE_RESULT: // wait seeing results
			makeQ.setEnabled(false);
			solveQ.setEnabled(false);
			seeR.setEnabled(false);
			inform.setText(R.string.inform_state_wait_see_result);
			break;

		case SEE_RESULT: // see results
			makeQ.setEnabled(false);
			solveQ.setEnabled(false);
			seeR.setEnabled(true);
			inform.setText(R.string.inform_state_see_result);
			break;

		case FINISH: // activity is over but it is possible to see results
			makeQ.setEnabled(false);
			solveQ.setEnabled(false);
			seeR.setEnabled(true);
			exitTodoView.setEnabled(true);
			inform.setText(R.string.inform_state_finish);
		}

	}

	public void setTimer(int _time) {
		Log.d(APP_TAG, "time_limit:" + _time);
		// call timer function
	}

	public void setNumOfScene(int _numscene) {
		LAST_SCENE_NUM = _numscene;
	}

	public void setRightAnswers(JSONArray _array, int _num) {

		Integer temp = 0;

		right_answer = new Vector<Integer>();
		for (int i = 0; i < _array.length(); i++) {
			right_answer.add(i, -1);
		}

		for (int i = 0; i < _num; i++) {
			try {
				temp = _array.getInt(i);
				right_answer.set(i, temp);
			} catch (JSONException e) {
				Log.d(APP_TAG, "Correct answers error: " + e);
			}
		}
	}

	public void restoreSavedAnswers(JSONArray _array, int _num) {
		Integer temp = 0;

		for (int i = 0; i < _num; i++) {
			try {
				temp = _array.getInt(i);
				answer_arr.set(i, temp);
				Log.d(APP_TAG, "Saved Answer:" + temp);
			} catch (JSONException e) {
				Log.d(APP_TAG, "Correct answers error: " + e);
			}
		}
	}

	public void setWinScore(JSONArray _array, int h_score) {
		score_winner_name.clear();

		for (int i = 0; i < _array.length(); i++) {
			try {

				String name = _array.getString(i);
				score_winner_name.add(i, name);

			} catch (JSONException e) {
				Log.d(APP_TAG, "Score Winner Error: " + e);
			}
		}

		high_score = h_score;
	}

	public void setWinRating(JSONArray _array, float h_rating) {
		rating_winner_name.clear();

		for (int i = 0; i < _array.length(); i++) {
			try {

				String name = _array.getString(i);
				rating_winner_name.add(i, name);

			} catch (JSONException e) {
				Log.d(APP_TAG, "Rating Winner Error: " + e);
			}

		}
		high_rating = h_rating;
	}

	// It is added in 6/22
	public void setAvgRating(JSONArray _array) {
		final_avg_ratings.clear();

		for (int i = 0; i < _array.length(); i++) {
			try {
				final_avg_ratings.add(i, _array.getString(i));
			} catch (JSONException e) {
				Log.d(APP_TAG, "Avrage Rating Error: " + e);
			}

		}

	}

	public void setRAPercents(JSONArray _array) {
		r_answer_percents.clear();

		for (int i = 0; i < _array.length(); i++) {
			try {
				r_answer_percents.add(i, _array.getString(i));
			} catch (JSONException e) {
				Log.d(APP_TAG, "Percent of right answer Error: " + e);
			}

		}

	}

	Activity _act;
	static String imgURL;
	Bitmap bmImg = null;
	Bitmap _bmImg;
	String myHTMLfile;
	String _content;
	String _op1;
	String _op2;
	String _op3;
	String _op4;
	String _rightan;

	// 1. Make Questions
	void MakeQuestion() {

		curcategory = category_arr[0];

		setTitle(curcategory);
		setContentView(R.layout.mkquestion);

		imageview = (ImageView) findViewById(R.id.galleryimg01);

		final EditText myContent = (EditText) findViewById(R.id.mkqContent);
		final EditText myOp1     = (EditText) findViewById(R.id.op1);
		final EditText myOp2     = (EditText) findViewById(R.id.op2);
		final EditText myOp3     = (EditText) findViewById(R.id.op3);
		final EditText myOp4     = (EditText) findViewById(R.id.op4);
		rgb04 = (RadioGroup) findViewById(R.id.rgroup04); // for inserting right answer



		// if (galleryidx == 1) SaveImage();

		// retain the previous contents for preview
		if ((previewidx == 1) || (galleryidx == 1)) {

			galleryidx = 0;

			myContent.setText(question_arr[0]);
			myOp1.setText(question_arr[1]);
			myOp2.setText(question_arr[2]);
			myOp3.setText(question_arr[3]);
			myOp4.setText(question_arr[4]);
			rgb04.check(Integer.parseInt(question_arr[5]) + R.id.rightan01 - 1);
			imageview.setImageBitmap(_bmImg);


		}

		// Add Image
		ImageButton addimg = (ImageButton) findViewById(R.id.camera01);
		addimg.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {

				// retain the previous information
				int checked_rid = rgb04.getCheckedRadioButtonId();
				int my_an = checked_rid - R.id.rightan01 + 1;

				myRightan = Integer.toString(my_an);

				String _content = myContent.getText().toString();
				String _op1 = myOp1.getText().toString();
				String _op2 = myOp2.getText().toString();
				String _op3 = myOp3.getText().toString();
				String _op4 = myOp4.getText().toString();
				String _rightan = myRightan;
				question_arr[0] = _content;
				question_arr[1] = _op1;
				question_arr[2] = _op2;
				question_arr[3] = _op3;
				question_arr[4] = _op4;
				question_arr[5] = _rightan;
				//----------------------------------------------------------

				image_qa = new QuickAction(v);

				image_qa.addActionItem(image_first);
				image_qa.addActionItem(image_second);
				image_qa.addActionItem(image_third);

				image_qa.show();
			}

		});

		// save a question made by student (ok = 1, cancel = 2, post+makeQuestion = 3)
		Button post1 = (Button) findViewById(R.id.post01);
		post1.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {

				final Builder adb = new AlertDialog.Builder(CourseList.this);
				adb.setTitle(curcategory);
				adb.setMessage(getString(R.string.post_q));
				adb.setPositiveButton(getString(R.string.OK),
						new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface arg0, int arg1) {

								int checked_rid = rgb04.getCheckedRadioButtonId();
								int my_an = checked_rid - R.id.rightan01 + 1;

								if (my_an < 0) {
									Toast.makeText(CourseList.this,
											getString(R.string.error_noanswer),
											Toast.LENGTH_SHORT).show();
								} else {

									myRightan = Integer.toString(my_an);

									if (bmImg != null) {
										ByteArrayOutputStream jpg = new ByteArrayOutputStream(
												64 * 1024);
										boolean error = bmImg.compress(
												Bitmap.CompressFormat.JPEG,100, jpg);

										if (!error)
											Log.d(APP_TAG, "ERROR JPEG");

										// post with picture
										student.post_question_to_teacher_picture(
												myContent.getText().toString(),
												myOp1.getText().toString(),
												myOp2.getText().toString(),
												myOp3.getText().toString(),
												myOp4.getText().toString(),
												myRightan, jpg.toByteArray());

									} else { // post it without picture
										student.post_question_to_teacher(
												myContent.getText().toString(),
												myOp1.getText().toString(),
												myOp2.getText().toString(),
												myOp3.getText().toString(),
												myOp4.getText().toString(),
												myRightan);
									}

									Log.d(APP_TAG, "Posting:"
											+ myContent.getText().toString());
									// after posting question, return the main
									// screen
									if (previewidx == 1) {
										previewidx = 0;
										_bmImg = null;
									}

									show_todo_view();
									selarridx = 0;

								}
							}
						});

				// Cancel to post the question
				adb.setNegativeButton(getString(R.string.Cancel), null);

				// Post and make the question
				adb.setNeutralButton(getString(R.string.Post_MoreQ),
						new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface arg0, int arg1) {
								int checked_rid = rgb04
										.getCheckedRadioButtonId();
								int my_an = checked_rid - R.id.rightan01 + 1;

								if (my_an < 0) {
									Toast.makeText(CourseList.this,
											getString(R.string.error_noanswer),
											Toast.LENGTH_SHORT).show();
								} else {

									myRightan = Integer.toString(my_an);

									if (bmImg != null) {
										ByteArrayOutputStream jpg = new ByteArrayOutputStream(
												64 * 1024);
										boolean error = bmImg.compress(
												Bitmap.CompressFormat.JPEG,
												100, jpg);

										if (!error)
											Log.d(APP_TAG, "ERROR JPEG");

										// post with picture
										student.post_question_to_teacher_picture(
												myContent.getText().toString(),
												myOp1.getText().toString(),
												myOp2.getText().toString(),
												myOp3.getText().toString(),
												myOp4.getText().toString(),
												myRightan, jpg.toByteArray());

									} else { // post it without picture
										student.post_question_to_teacher(
												myContent.getText().toString(),
												myOp1.getText().toString(),
												myOp2.getText().toString(),
												myOp3.getText().toString(),
												myOp4.getText().toString(),
												myRightan);
									}

									Log.d(APP_TAG, "Posting:"+ myContent.getText().toString());

									if (previewidx == 1) {
										previewidx = 0;
										_bmImg = null;
										chkimg = 0;
										bmImg = null;
									}

									chkimg = 0;
									selarridx = 0;
									bmImg = null;
									MakeQuestion();
								}
							}
						});

				adb.show();

			}
		});

		// Preview the newly created question
		Button preview1 = (Button) findViewById(R.id.preview01);
		preview1.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {

				previewidx = 1;

				int checked_rid = rgb04.getCheckedRadioButtonId();
				int my_an = checked_rid - R.id.rightan01 + 1;

				myRightan = Integer.toString(my_an);

				String _content = myContent.getText().toString();
				String _op1 = myOp1.getText().toString();
				String _op2 = myOp2.getText().toString();
				String _op3 = myOp3.getText().toString();
				String _op4 = myOp4.getText().toString();
				String _rightan = myRightan;

				question_arr[0] = _content;
				question_arr[1] = _op1;
				question_arr[2] = _op2;
				question_arr[3] = _op3;
				question_arr[4] = _op4;
				question_arr[5] = _rightan;

				preview(_content, _op1, _op2, _op3, _op4);

			}
		});

	}

	public void RetainData(String s1, String s2, String s3, String s4, String s5) {

	}

	//This part has a problem with LG phone (3/19/2012)
	public void addimage() {

		//galleryidx = 1; // in order to retain information
		chkimg = 1;

		/****************************************************
		* This is the part which worked as an image dialog  *
		*****************************************************/

		add_pic_d = new AddPictureDialog(CourseList.this);
		add_pic_d.setActivity(_act);

		Window window = add_pic_d.getWindow();
		window.setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND,
				WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
		//add_pic_d.setTitle("Touch the picture for selection");
		add_pic_d.setTitle(R.string.img_dialog_title);
		add_pic_d.setContentView(R.layout.addpicdialog);
		add_pic_d.show();
		add_pic_d.setOnDismissListener((CourseList) _act);


		//add_pic_g.onStart();

	}

	public void image_quickaction() {

		// Adding quick action menu
		image_first = new ActionItem();

		image_first.setTitle(getString(R.string.add_img));
		image_first.setIcon(getResources().getDrawable(R.drawable.plus));
		image_first.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {

				addimage();
				if (image_qa != null)
					image_qa.dismiss();

			}
		});

		image_second = new ActionItem();

		image_second.setTitle(getString(R.string.take_pic));
		image_second.setIcon(getResources()
				.getDrawable(R.drawable.take_picture));
		image_second.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {

				takepicture();
				if (image_qa != null)
					image_qa.dismiss();

			}
		});

		image_third = new ActionItem();

		image_third.setTitle(getString(R.string.rmv_img));
		image_third.setIcon(getResources().getDrawable(R.drawable.minus));
		image_third.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				removeimage_dialog();
				if (image_qa != null)
					image_qa.dismiss();
			}
		});

	}

	private void removeimage_dialog() {

		chkimg = 0;
		activity = this;
		Builder adb = new AlertDialog.Builder(activity);

		adb.setTitle(getString(R.string.rmv_img_dialog_title));
		adb.setMessage(getString(R.string.rmv_img_q));

		adb.setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface arg0, int arg1) {

				bmImg = null;
				imageview.setImageBitmap(bmImg);
				_bmImg = bmImg;
			}

		});

		adb.setNegativeButton(getString(R.string.Cancel), null);
		adb.show();

	}

	private void takepicture() {

		chkimg = 1;
		TakenImage = false;

		// -----------------------------------------------------------------
		// Start Built-in Camera Activity
		// -----------------------------------------------------------------
		// define the file-name to save photo taken by Camera activity
		String img_filename = "new-photo-name.jpg";
		// create parameters for Intent with filename
		ContentValues values = new ContentValues();
		values.put(MediaStore.Images.Media.TITLE, img_filename);
		values.put(MediaStore.Images.Media.DESCRIPTION,
				"Image capture by camera");
		// imageUri is the current activity attribute, define and save it for
		// later usage (also in onSaveInstanceState)
		imageUri = getContentResolver().insert(
				MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
		// imageUri =
		// getContentResolver().insert(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI,
		// values);

		// create new Intent
		camera_intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		camera_intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
		camera_intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);

		startActivityForResult(camera_intent, IMAGECAPTURE_OK);

	}

	@SuppressLint({ "ShowToast", "ShowToast" })
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (requestCode == IMAGECAPTURE_OK) {
			if (resultCode == RESULT_OK) {
				// ----------------------------------------
				// Get Result from Camera
				// ----------------------------------------
				TakenImage = true;

				setImageFromCamera();

			} else if (resultCode == RESULT_CANCELED) {
				Toast.makeText(this, getString(R.string.warn_nopic),Toast.LENGTH_SHORT);
			} else {
				Toast.makeText(this, getString(R.string.warn_nopic),Toast.LENGTH_SHORT);
			}
		}

	}

	private void setImageFromCamera() {

		if (TakenImage) {
			bmImg = getBitmapFromCameraResult();
		}

		// If there was a problem with getBitMap..., bmImg became null again
		try {

			// open file as input stream
			InputStream is = getContentResolver().openInputStream(imageUri);

			// save this output to local file
			OutputStream os = getBaseContext().openFileOutput("test.jpg",
					MODE_PRIVATE);
			byte[] BUF = new byte[1024];

			try {
				while (is.read(BUF) != -1) {
					os.write(BUF);
				}
				;

			} catch (IOException e) {
				Log.d(APP_TAG, "ERROr COPYING DATA");
			}

		} catch (FileNotFoundException e) {
			Log.d(APP_TAG, imageUri.toString());
		}

		imageview.setImageBitmap(bmImg);
		_bmImg = bmImg;

	}

	private Bitmap getBitmapFromCameraResult() {

		long id = -1;
		_act = this;
		// 1. Get Original id using m_cursor
		try {
			String[] proj = { MediaStore.Images.Media._ID };
			mcursor_for_camera = _act.managedQuery(imageUri, proj, // Which
																	// columns
																	// to return
					null, // WHERE clause; which rows to return (all rows)
					null, // WHERE clause selection arguments (none)
					null);// Order-by clause (ascending by name)

			int id_ColumnIndex = mcursor_for_camera
					.getColumnIndexOrThrow(MediaStore.Images.Media._ID);

			if (mcursor_for_camera.moveToFirst()) {
				id = mcursor_for_camera.getLong(id_ColumnIndex);

			} else {
				return null;
			}

		} finally {
			if (mcursor_for_camera != null) {
				mcursor_for_camera.close();
			}
		}

		// 2. get Bitmap
		Bitmap b = null;
		try {
			Bitmap c = MediaStore.Images.Media.getBitmap(_act
					.getContentResolver(), Uri.withAppendedPath(
					MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id + ""));

			Log.d(APP_TAG, "Height" + c.getHeight() + " width= " + c.getWidth());

			// resize bitmap: code copied from
			// thinkandroid.wordpress.com/2009/12/25/resizing-a-bitmap/

			int target_width = 800;
			int target_height = 600;
			int w = c.getWidth();
			int h = c.getHeight();
			if ((w > target_width) || (h > target_height)) {
				b = Bitmap.createScaledBitmap(c, target_width, target_height, false);
			} else {
				b = c;
			}
		} catch (FileNotFoundException e) {
			Log.d(APP_TAG, "ERROR" + e);
		} catch (IOException e) {
			Log.d(APP_TAG, "ERROR" + e);
		}

		return b;

	}

	String image;
	String my_html;

	// Creating HTML file for previewing
	public void preview(String _content, String _op1, String _op2, String _op3, String _op4) {

		setTitle(curcategory);
		setContentView(R.layout.preview);

		Button backpreview = (Button) findViewById(R.id.previewBack);
		backpreview.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				MakeQuestion();
			}
		});

		curwebview = (WebView) findViewById(R.id.webviewPreview);
		curwebview.clearCache(true);

		String header = "<html> <head>"+ getString(R.string.notice_preview)+ "</head>";
		String body1 = " <body>";
		String question = "<P>" + _content + "</P>";

		if (chkimg == 1) {

			image = "<center><img class=\"main\" src=\"test.jpg\" width=250 height=240/></center>";
		}
		String choices = "<P>(1)" + _op1 + "<br>" + "(2)" + _op2 + "<br>"
				+ "(3)" + _op3 + "<br>" + "(4)" + _op4 + "<br>" + "</P>";
		String end = "</body>" + "</html>";

		if (chkimg == 1) {
			my_html = header + body1 + question + image + choices + end;
		} else {
			my_html = header + body1 + question + choices + end;
		}

		boolean from_asset = false;
		readHTMLfromString(my_html, from_asset);
	}

	// for image dialog
	public void onDismiss(DialogInterface dialog) {

		isImageSelected = add_pic_d.isSelectedImg();

		if (isImageSelected) {
			// Quality of image should be improved.
			bmImg = add_pic_d.readThunmbBitmap();

			// save this file to asset
			try {

				// open file as input stream
				InputStream is = getContentResolver().openInputStream(
						add_pic_d.readURI());

				// save this output to local file
				OutputStream os = getBaseContext().openFileOutput("test.jpg",
						MODE_PRIVATE);
				byte[] BUF = new byte[1024];
				try {
					while (is.read(BUF) != -1) {
						os.write(BUF);
					}
					;

				} catch (IOException e) {
					Log.d(APP_TAG, "ERROr COPYING DATA");
				}

			} catch (FileNotFoundException e) {
				Log.d(APP_TAG, add_pic_d.readURI().toString());
			}
		} else {
			// bmImg = null;
		}

		imageview.setImageBitmap(bmImg);
		_bmImg = bmImg;
	}

	public void SaveImage() {

		isImageSelected = add_pic_g.getImageIdx();

		if (isImageSelected) {

			bmImg = add_pic_g.readThunmbBitmap();

			try {

				// open file as input stream
				InputStream is = getContentResolver().openInputStream(add_pic_g.readURI());

				// save this output to local file
				OutputStream os = getBaseContext().openFileOutput("test.jpg", MODE_PRIVATE);
				byte[] BUF = new byte[1024];
				try {
					while (is.read(BUF) != -1) {
						os.write(BUF);
					}


				} catch (IOException e) {
					Log.d(APP_TAG, "ERROR COPYING DATA");
				}

			} catch (FileNotFoundException e) { // If errors happen
				Log.d(APP_TAG, add_pic_g.readURI().toString());
			}
		} else {

			//bmImg = null;
		}

		imageview.setImageBitmap(bmImg);
		_bmImg = bmImg;
	}

	// initialization of arrays
	void initialize_AnswerRatingArray_withDummyValues() {

		answer_arr.clear();
		rating_arr.clear();
		my_score.clear();

		Log.d(APP_TAG, "LAST_SCENE " + LAST_SCENE_NUM);
		for (int i = 0; i < LAST_SCENE_NUM; i++) {
			answer_arr.add(i, -1);
			rating_arr.add(i, -1);
			my_score.add(i, -1);
		}

	}

	private int saveCurrentAnswers() {

		int tot_answer = 0;
		int checked_id = rgb02.getCheckedRadioButtonId();

		if (checked_id > 0) {
			answer_arr.set((scene_number - 1), (checked_id - R.id.op01 + 1));
			tot_answer++;
		} else {
			answer_arr.set((scene_number - 1), -1);
		}

		checked_id = rgb03.getCheckedRadioButtonId();

		if (checked_id > 0) {
			rating_arr.set((scene_number - 1), (checked_id - R.id.rt01 + 1));
			tot_answer++;
		} else {
			rating_arr.set((scene_number - 1), -1);
		}

		return tot_answer;

	}

	private void checkCurrentAnswers() {

		// display answer
		if (answer_arr.get((scene_number - 1)) == -1) // not selected yet
			rgb02.check(-1);
		else
			rgb02.check(answer_arr.get((scene_number - 1)) + R.id.op01 - 1);

		if (rating_arr.get((scene_number - 1)) == -1)
			rgb03.check(-1);
		else
			rgb03.check(rating_arr.get((scene_number - 1)) + R.id.rt01 - 1);

	}

	String webpage;
	Boolean ansewr_chk = false;

	// 2. solve Questions w/solving questions screen
	private void SolveQuestion() {

		SolvingIndex = false;
		issolvequestion = 0;
		// 1st screen
		curwebview = webviewQ;
		curcategory = category_arr[1];
		scene_number = 1;

		setTitle(curcategory + "   1/" + LAST_SCENE_NUM);
		setContentView(R.layout.question);

		curwebview = (WebView) findViewById(R.id.webviewQ);
		rgb02 = (RadioGroup) findViewById(R.id.rgroup02);
		rgb03 = (RadioGroup) findViewById(R.id.rgroup03);
		curwebview.clearCache(true);

		getWindow().setFeatureInt(Window.FEATURE_PROGRESS,
				Window.PROGRESS_VISIBILITY_ON);

		curwebview.setWebChromeClient(new WebChromeClient() {
			public void onProgressChanged(WebView view, int progress) {
				// Make the bar disappear after URL is loaded, and changes
				// string to Loading...
				CourseList.this.setTitle(getString(R.string.loading));
				CourseList.this.setProgress(progress * 100); // Make the bar
																// disappear
																// after URL is
																// loaded
				// Restore the app name after finish loading
				if (progress == 100)
					CourseList.this.setTitle(curcategory + "   " + scene_number
							+ " /" + LAST_SCENE_NUM);
			}
		});

		initialize_AnswerRatingArray_withDummyValues();

		showScene();
		checkCurrentAnswers();

		// reset button (delete previous answer)
		Button resetB = (Button) findViewById(R.id.resetQ);
		resetB.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				rgb02.clearCheck();
				rgb03.clearCheck();
			}
		});

		// previous button
		Button prevB = (Button) findViewById(R.id.prevQ);
		prevB.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				if (scene_number > 1) {

					int temp = saveCurrentAnswers();
					showPreviousScene();
					checkCurrentAnswers();
				}
			}
		});

		// next button
		Button nextB = (Button) findViewById(R.id.nextQ);
		nextB.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				if (scene_number != LAST_SCENE_NUM) {

					int temp1 = saveCurrentAnswers();
					if (temp1 == 2) {
						showNextScene();
						checkCurrentAnswers();
					} else {
						Toast.makeText(CourseList.this,getString(R.string.insert_error), Toast.LENGTH_SHORT).show();
					}

				} else {

					int temp1 = saveCurrentAnswers(); // save the last answer

					if (temp1 == 2) {
						Builder adb = new AlertDialog.Builder(CourseList.this);
						adb.setTitle(curcategory);
						adb.setMessage(getString(R.string.submit_q));
						adb.setPositiveButton(getString(R.string.submit_btn),
								new DialogInterface.OnClickListener() {

									public void onClick(DialogInterface arg0,
											int arg1) {

										student.submit_answer_to_teacher(
												answer_arr, rating_arr);

										// Log.d(APP_TAG,
										// "answer_arr[0]="+answer_arr.get(0));
										// Log.d(APP_TAG,
										// "answer_arr[1]="+answer_arr.get(1));

										show_todo_view();
										selarridx = 0;
										SolvingIndex = true;

									}
								});

						adb.setNegativeButton(getString(R.string.Cancel), null);
						adb.show();
					} else {
						Toast.makeText(CourseList.this,
								getString(R.string.insert_error), Toast.LENGTH_SHORT)
								.show();
					}
				}
			}
		});

	}

	private void showScene() {

		if (issolvequestion == 0) { // related to solving questions
			webpage = "http://" + cururi + server_dir + (scene_number - 1)
					+ ".html";

		} else { // related to seeing results

			webpage = "http://" + cururi + server_dir + (scene_number - 1)
					+ "_result" + ".html";


			if((SolvingIndex == true) ||(answer_arr.isEmpty() == false) ) {
				my_answer_view.setText(answer_arr.get(scene_number - 1).toString());
			} else {
				my_answer_view.setText("N/A");
			}

			// 5-rating star
			Float f = new Float(final_avg_ratings.get(scene_number - 1));
			ratingbar.setRating(f);
			ratingbar.setEnabled(false);

			// percent graph
			System.out.println("Draw Graph: start");
			right_val = Integer.parseInt(r_answer_percents.get(scene_number - 1).trim());
			wrong_val = 100 - right_val;
			draw_piechart.redraw(right_val, wrong_val);
			System.out.println("Draw Graph: end");

		}

		curwebview.clearView();
		curwebview.loadUrl(webpage);
	}

	private void showNextScene() {

		if (scene_number != LAST_SCENE_NUM) {
			scene_number++;
			setTitle(curcategory + "   " + scene_number + "/" + LAST_SCENE_NUM);
			showScene();
		}
	}

	private void showPreviousScene() {
		if (scene_number > 1) {
			scene_number--;
			setTitle(curcategory + "   " + scene_number + "/" + LAST_SCENE_NUM);
			showScene();
		}
	}

	// read questions
	private void readHTMLfromResouceFile(String fileName) {
		InputStream is;
		try {
			is = getAssets().open(fileName);
		} catch (IOException e) {
			return;
		}

		InputStreamReader isr = new InputStreamReader(is);
		StringBuffer builder = new StringBuffer();
		char buffer[] = new char[1024];

		try {
			int chars;

			while ((chars = isr.read(buffer)) >= 0) {
				builder.append(buffer, 0, chars);
			}
		} catch (IOException e) {
			return;
		}

		String htmlstring = builder.toString();
		curwebview.loadDataWithBaseURL("file:///android_asset/", htmlstring,
				"text/html", "utf-8", "");
	}

	private void readHTMLfromString(String htmlStr, boolean from_asset) {
		String url;
		if (from_asset)
			url = "file:///andorid_asset";
		else
			url = stored_file_url;

		curwebview.loadDataWithBaseURL(stored_file_url, htmlStr, "text/html",
				"utf-8", "");
	}

	// 3. See Results
	private void seeResults() {

		issolvequestion = 1; // see results
		curcategory = category_arr[2];
		setTitle(curcategory);
		setContentView(R.layout.seeresults);
		curwebview = (WebView) findViewById(R.id.webviewSeeR);
		curwebview.clearCache(false);

		scorequestion(answer_arr, right_answer); // score my answers

		// 1. show main result
		String received_html = createresulthtml(my_score, curusername);
		curwebview.loadData(received_html, "text/html", "UTF-8");

		Button quitSR = (Button) findViewById(R.id.SeeRQuit);
		quitSR.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				show_todo_view();
				selarridx = 0;

			}
		});

		// who is winner in score and rating
		Button winnerSR = (Button) findViewById(R.id.WinnerResult);
		winnerSR.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				show_winner();
			}
		});

		// show the result of each question
		Button detailSR = (Button) findViewById(R.id.DetailResult);
		detailSR.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				draw_piechart.setIsAdded(false);
				show_detail(my_score);
			}
		});

	}

	@Override
	public boolean onCreateOptionsMenu (Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.simplemenu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
			case R.id.restart:
				startActivity(new Intent(this, intro.class));
				return true;
			default:
			return super.onOptionsItemSelected(item);
		}
	}
	private void show_winner() {

		setContentView(R.layout.winner);
		curwebview = (WebView) findViewById(R.id.webviewWinner);
		String received_html = createwinnerhtml();
		curwebview.loadData(received_html, "text/html", "UTF-8");

		Button returnSW = (Button) findViewById(R.id.returnresult01);
		returnSW.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				seeResults();
			}
		});
	}

	TextView my_answer_view;
	RatingBar ratingbar;
	int right_val;
	int wrong_val;

	private void show_detail(Vector<Integer> _myscore) {

		curcategory = category_arr[2];
		scene_number = 1;
		setContentView(R.layout.detailresult);

		setTitle(curcategory + "     1/" + LAST_SCENE_NUM);
		curwebview = (WebView) findViewById(R.id.webviewdetailresult);
		curwebview.clearCache(false);

		my_answer_view = (TextView) findViewById(R.id.textresult02);

		ratingbar = (RatingBar) findViewById(R.id.ratingbar);

		showScene();

		Button returnMR = (Button) findViewById(R.id.returnresult02);
		returnMR.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				seeResults();
			}
		});

		Button prevDR = (Button) findViewById(R.id.prevResult);
		prevDR.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				if (scene_number > 1) {
					showPreviousScene();
				}
			}
		});

		Button nextDR = (Button) findViewById(R.id.nextResult);
		nextDR.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				if (scene_number != LAST_SCENE_NUM) {
					showNextScene();
				} else {

					Builder adb = new AlertDialog.Builder(CourseList.this);
					adb.setTitle(curcategory);
					adb.setMessage(getString(R.string.notice_last_q));
					adb.setPositiveButton(getString(R.string.OK), null);
					adb.show();
				}
			}
		});

	}

	private void scorequestion(Vector<Integer> _rightanswer,
			Vector<Integer> _myanswer) {

		for (int i = 0; i < _rightanswer.size(); i++) {
			if (_myanswer.get(i) == _rightanswer.get(i))
				my_score.set(i, 1); // right
			else
				my_score.set(i, 0); // wrong
		}
	}

	private int countrightquestion(Vector<Integer> arr) {
		int revalue = 0;

		for (int i = 0; i < arr.size(); i++) {
			if (arr.get(i) == 1)
				revalue++;
		}

		return revalue;
	}

	private String getHTMLTagForUTF8() {
		return new String("\n <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"\n ");
	}

	private String createwinnerhtml() {
		String return_html = "";


		String header1 = "<html><head></head><body><P></P><font face=\"helvetica\">";
		String header2 = "<center><Strong>"+getString(R.string.winner_title)+"</Strong></center><br>";
		String body1 = "<P></P>*** "+getString(R.string.quiz_score)+" ***<br>";
		String body2 = getString(R.string.h_score)+ ":"+ high_score + "<br>";
		String body3 = getString(R.string.name)+":<br>";
		String mid_html = "";
		for (int i = 0; i < score_winner_name.size(); i++) {
			String name = score_winner_name.get(i);
			mid_html = mid_html + name + "<br>";

		}

		String body4 = "<P></P>";
		String body5 = "*** "+getString(R.string.rating_score_title)+ " ***<br>";
		Formatter f = new Formatter();
		String avg = f.format(new String("%4.2f"), high_rating).toString();
		String body6 = getString(R.string.h_avg_rating)+ " :" + avg + "<br>";
		String body7 = getString(R.string.name)+" :<br>";
		String next_html = "";
		for (int i = 0; i < rating_winner_name.size(); i++) {
			String r_name = rating_winner_name.get(i);
			next_html = next_html + r_name + "<br>";

		}
		String end = "</font></body></html>";

		return_html = header1 + getHTMLTagForUTF8() + header2 + body1 + body2 + body3 + mid_html
				+ body4 + body5 + body6 + body7 + next_html + end;

		return return_html;
	}

	String result_html;

	private String createresulthtml(Vector<Integer> _myscore, String username) {

		int num_right = countrightquestion(_myscore);
		int total_question = LAST_SCENE_NUM;

		String header1 = "<html><head></head><body><P></P><font face=\"helvetica\">";
		String header2 = "<center>" + getString(R.string.name)+ ": "+ username + "<br>";
		String header3 = getString(R.string.t_score)+":" + num_right + "/" + total_question  + "<br />&#x2717;=Incorrect, &#x2713;=Correct";
		String body1 = "<P><table border=\"1\">";
		String body2 = "<tr><td><div align=\"center\">" + getString(R.string.q_num) + "</div></td>"
		+ "<td><div align=\"center\">" + getString(R.string.correct_wrong)+ " </div></td></tr>";

		String fore_html = header1 + getHTMLTagForUTF8()+ header2 + header3 + body1 + body2;
		String mid_html = "";
		for (int i = 0; i < _myscore.size(); i++) {
			String mid;
			int number = 0;
			number = i + 1;
			int score = _myscore.get(i);
			if (score == 1) { // right
				mid = "<tr><td><div align=\"center\">" + "(" + number + ")"
						+ "</div></td>"
						+ "<td><div align=\"center\">&#x2713;</div></td></tr>";
			} else { // wrong
				mid = "<tr><td><div align=\"center\">" + "(" + number + ")"
						+ "</div></td>"
						+ "<td><div align=\"center\">&#x2717;</div></td></tr>";
			}

			mid_html = mid_html + mid;
		}
		String end = "</table></p></center></font></body></html>";
		String last_html = end;
		result_html = fore_html + mid_html + last_html;

		return result_html;
	}

	public void onStop() {

		// CourseList.this.finish();
		super.onStop();
	}

	public void onPause() {

		// CourseList.this.finish();
		super.onPause();
	}

} // end of activity