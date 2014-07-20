package com.ustwo.glass.cookbook;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.FileObserver;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.google.android.glass.app.Card;
import com.google.android.glass.media.CameraManager;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;
import com.ustwo.util.Slog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * An {@link Activity} showing a tuggable "Hello World!" card.
 * <p>
 * The main content view is composed of a one-card {@link CardScrollView} that provides tugging
 * feedback to the user when swipe gestures are detected.
 * If your Glassware intends to intercept swipe gestures, you should set the content view directly
 * and use a {@link com.google.android.glass.touchpad.GestureDetector}.
 * @see <a href="https://developers.google.com/glass/develop/gdk/touch">GDK Developer Guide</a>
 */
public class MainActivity extends Activity {

    private static final int RECORD_VIDEO_REQUEST = 1;
    private static final int RECORD_SPEECH_REQUEST = 2;

    /** {@link CardScrollView} to use as the main content view. */
    private CardScrollView mCardScroller;

    private List<View> mViews = new ArrayList<View>();

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        Slog.init(getApplicationContext());

        mViews.add(createCard("Name recipe"));
        mViews.add(createCard("Create step"));
        mViews.add(createCard("Publish"));

        mCardScroller = new CardScrollView(this);
        mCardScroller.setAdapter(new CardScrollAdapter() {
            @Override
            public int getCount() {
                return mViews.size();
            }

            @Override
            public Object getItem(int position) {
                return mViews.get(position);
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                return mViews.get(position);
            }

            @Override
            public int getPosition(Object item) {
                return 0;
            }
        });
        // Handle the TAP event.
        mCardScroller.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        // Record speech.
                        recordSpeech();
                        break;
                    case 1:
                        // Record a step.
                        recordVideo();
                        break;
                    case 2:
                        // Publish.
                        break;
                }
            }
        });
        setContentView(mCardScroller);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCardScroller.activate();
    }

    @Override
    protected void onPause() {
        mCardScroller.deactivate();
        super.onPause();
    }

    /**
     * Builds a Glass styled "Hello World!" view using the {@link Card} class.
     */
    private View createCard(String title) {
        Card card = new Card(this);
        card.setText(title);
        return card.getView();
    }

    private void recordSpeech() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        startActivityForResult(intent, RECORD_SPEECH_REQUEST);
    }

    private void recordVideo() {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        startActivityForResult(intent, RECORD_VIDEO_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RECORD_VIDEO_REQUEST) {
            if (resultCode == RESULT_OK) {
                String picturePath = data.getStringExtra(
                        CameraManager.EXTRA_VIDEO_FILE_PATH);
                processPictureWhenReady(picturePath);
            } else {
                // Video record cancelled.
            }
        } else if (requestCode == RECORD_SPEECH_REQUEST) {
            if (resultCode == RESULT_OK) {
                List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                for (String result : results) {
                    Slog.d("Speech result: " + result);
                }
            } else {
                // Record speech cancelled.
            }
        }
    }

    private void processPictureWhenReady(final String filePath) {
        final File videoFile = new File(filePath);

        if (videoFile.exists()) {
            // The picture is ready; process it.
            Slog.d("I TOOK A VID: " + videoFile.getAbsolutePath());
        } else {
            // The file does not exist yet. Before starting the file observer, you
            // can update your UI to let the user know that the application is
            // waiting for the picture (for example, by displaying the thumbnail
            // image and a progress indicator).

            final File parentDirectory = videoFile.getParentFile();
            FileObserver observer = new FileObserver(parentDirectory.getPath(),
                    FileObserver.CLOSE_WRITE | FileObserver.MOVED_TO) {
                // Protect against additional pending events after CLOSE_WRITE
                // or MOVED_TO is handled.
                private boolean isFileWritten;

                @Override
                public void onEvent(int event, String path) {
                    if (!isFileWritten) {
                        // For safety, make sure that the file that was created in
                        // the directory is actually the one that we're expecting.
                        File affectedFile = new File(parentDirectory, path);
                        isFileWritten = affectedFile.equals(videoFile);

                        if (isFileWritten) {
                            stopWatching();

                            // Now that the file is ready, recursively call
                            // processPictureWhenReady again (on the UI thread).
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    processPictureWhenReady(filePath);
                                }
                            });
                        }
                    }
                }
            };
            observer.startWatching();
        }
    }
}
