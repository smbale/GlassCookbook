package com.ustwo.glass.cookbook;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.glass.app.Card;
import com.google.android.glass.media.CameraManager;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ustwo.util.Slog;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

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

    // TODO:
    // CREATE option icons
    // SHOW published + finish

    // /storage/emulated/0/DCIM/Camera
    private static final String JSON_OUTPUT_PATH = "/storage/emulated/0/DCIM/Camera/recipe.json";
    private static final String HTML_OUTPUT_PATH = "/storage/emulated/0/DCIM/Camera/index.html";

    private static final int RECORD_VIDEO_REQUEST = 1;
    private static final int RECORD_RECIPE_TITLE_REQUEST = 2;
    private static final int RECORD_STEP_TITLE_REQUEST = 3;

    private String[] PROGRESS_LABELS = {"       ", "■      ", "■  ■   ", "■  ■  ■"};

    /** {@link CardScrollView} to use as the main content view. */
    private CardScrollView mCardScroller;
    private List<View> mViews = new ArrayList<View>();

    private Recipe mRecipe;
    private String mStepTitle;

    private boolean mPublishing;
    private int mPublishProgress;

    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        Slog.init(getApplicationContext());

        mRecipe = new Recipe("Untitled");

        mViews.add(createCard("Name recipe", mRecipe.getTitle(), R.drawable.icn_pencil));
        mViews.add(createCard("Create step 1", null, R.drawable.icn_capture));

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
                if (mPublishing) {
                    return;
                }

                switch (position) {
                    case 0:
                        // Record recipe title.
                        recordSpeech(RECORD_RECIPE_TITLE_REQUEST, "What is the recipe name?");
                        break;
                    case 1:
                        // Record a step.
                        recordSpeech(RECORD_STEP_TITLE_REQUEST, "What is the step title?");
                        break;
                    case 2:
                        // Publish.
                        if (mRecipe.getNumSteps() > 0) {
                            publishHTML(mRecipe);
                            animateAndExit();
                        }
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RECORD_VIDEO_REQUEST) {
            if (resultCode == RESULT_OK) {
                String videoPath = data.getStringExtra(
                        CameraManager.EXTRA_VIDEO_FILE_PATH);
                mRecipe.addStep(new Recipe.Step(mStepTitle, videoPath));
                updateStepCard();

                if (mRecipe.getNumSteps() == 1) {
                    mViews.add(createCard("Publish", null, R.drawable.icn_publish));
                    mCardScroller.getAdapter().notifyDataSetInvalidated();
                }
            } else {
                // Video record cancelled.
            }
        } else if (requestCode == RECORD_RECIPE_TITLE_REQUEST) {
            if (resultCode == RESULT_OK) {
                List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                mRecipe.setTitle(parseVoiceResults(results, true));
                updateRecipeTitleCard();
            } else {
                // Record speech cancelled.
            }
        } else if (requestCode == RECORD_STEP_TITLE_REQUEST) {
            if (resultCode == RESULT_OK) {
                List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                mStepTitle = "Step " + (mRecipe.getNumSteps() + 1) + ": " + parseVoiceResults(results, false);
                recordVideo();
            } else {
                // Record speech cancelled.
            }
        }
    }

    /**
     * Builds a Glass styled "Hello World!" view using the {@link Card} class.
     */
    private View createCard(String title, String footer, int iconId) {
        View view = LayoutInflater.from(this).inflate(R.layout.card, null);
        ((TextView) view.findViewById(R.id.body)).setText(title);
        ((ImageView) view.findViewById(R.id.icon)).setImageResource(iconId);
        if (footer != null) {
            ((TextView) view.findViewById(R.id.footer)).setText(footer);
        }
        return view;
    }

    private void updateStepCard() {
        ((TextView) mViews.get(1).findViewById(R.id.body)).setText("Create step " + (1 + mRecipe.getNumSteps()));
        mCardScroller.getAdapter().notifyDataSetInvalidated();
    }

    private void updateRecipeTitleCard() {
        ((TextView) mViews.get(0).findViewById(R.id.footer)).setText(mRecipe.getTitle());
        mCardScroller.getAdapter().notifyDataSetInvalidated();
    }

    private void recordSpeech(int type, String prompt) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        if (prompt != null) {
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, prompt);
        }
        startActivityForResult(intent, type);
    }

    private void recordVideo() {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        startActivityForResult(intent, RECORD_VIDEO_REQUEST);
    }

    private String parseVoiceResults(List<String> results, boolean titleCase) {
        StringBuffer buffer = new StringBuffer();
        for (int i=0; i<results.size(); i++) {
            buffer.append(results.get(i));
            if (i < results.size() - 1) {
                buffer.append(" ");
            }
        }

        StringTokenizer tokenizer = new StringTokenizer(buffer.toString(), " ");

        if (titleCase) {
            StringBuffer titleBuffer = new StringBuffer();

            while (tokenizer.hasMoreTokens()) {
                StringBuffer wordBuffer = new StringBuffer(tokenizer.nextToken());
                wordBuffer.replace(0, 1, String.valueOf(wordBuffer.charAt(0)).toUpperCase());
                titleBuffer.append(wordBuffer.toString());
                titleBuffer.append(" ");
            }
            return titleBuffer.toString().trim();
        } else {
            return buffer.replace(0, 1, String.valueOf(buffer.charAt(0)).toUpperCase()).toString();
        }
    }

    private boolean publishJSON(Recipe recipe) {
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        return writeFile(JSON_OUTPUT_PATH, gson.toJson(mRecipe));
    }

    private boolean publishHTML(Recipe recipe) {
        StringBuffer buffer = new StringBuffer();
        for (Recipe.Step step : recipe.getSteps()) {
            buffer.append(getString(R.string.step_template, step.mTitle, step.mVideo));
        }

        String html = getString(R.string.recipe_template, mRecipe.getTitle(), buffer.toString());
        return writeFile(HTML_OUTPUT_PATH, html);
    }

    private boolean writeFile(String path, String data) {
        boolean result = true;
        OutputStreamWriter writer = null;
        try {
            writer = new OutputStreamWriter(new FileOutputStream(path));
            writer.write(data);
        }
        catch (Exception e) {
            result = false;
            Slog.e("Failed to publish to: " + path, e);
        }
        finally {
            try {
                writer.close();
            } catch (Exception e) {}
        }
        return result;
    }

    private void animateAndExit() {
        // Remove the creation views.
        mViews.remove(0);
        mViews.remove(0);
        // Disable input.
        mPublishing = true;
        ((TextView) mViews.get(0).findViewById(R.id.body)).setText("Publishing");
        mCardScroller.getAdapter().notifyDataSetInvalidated();

        mProgressRunnable.run();
    }

    private Runnable mProgressRunnable = new Runnable() {
        @Override
        public void run() {
            TextView footer = (TextView) mViews.get(0).findViewById(R.id.footer);

            if (mPublishProgress <= PROGRESS_LABELS.length) {
                if (mPublishProgress < PROGRESS_LABELS.length) {
                    footer.setText(PROGRESS_LABELS[mPublishProgress]);
                } else {
                    ((TextView) mViews.get(0).findViewById(R.id.body)).setText("Published");
                    footer.setText("");
                }
                mPublishProgress++;
                mHandler.postDelayed(mProgressRunnable, mPublishProgress <= PROGRESS_LABELS.length ? 750 : 1000);
                mCardScroller.getAdapter().notifyDataSetInvalidated();
            } else {
                finish();
            }
        }
    };
}
