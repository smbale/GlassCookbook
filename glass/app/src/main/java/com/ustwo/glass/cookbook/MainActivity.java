package com.ustwo.glass.cookbook;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

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

    // /storage/emulated/0/DCIM/Camera
    private static final String JSON_OUTPUT_PATH = "/storage/emulated/0/DCIM/Camera/recipe.json";
    private static final String HTML_OUTPUT_PATH = "/storage/emulated/0/DCIM/Camera/index.html";

    private static final int RECORD_VIDEO_REQUEST = 1;
    private static final int RECORD_RECIPE_TITLE_REQUEST = 2;
    private static final int RECORD_STEP_TITLE_REQUEST = 3;

    /** {@link CardScrollView} to use as the main content view. */
    private CardScrollView mCardScroller;
    private List<View> mViews = new ArrayList<View>();

    private Recipe mRecipe;
    private String mStepTitle;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        Slog.init(getApplicationContext());

        mRecipe = new Recipe("Untitled");
//        mRecipe.addStep(new Recipe.Step("Test", "1.mp4"));

        mViews.add(createCard("Name your recipe", mRecipe.getTitle()));
        mViews.add(createCard("Create step 1", null));
        mViews.add(createCard("Publish", null));

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
            } else {
                // Video record cancelled.
            }
        } else if (requestCode == RECORD_RECIPE_TITLE_REQUEST) {
            if (resultCode == RESULT_OK) {
                List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                mRecipe.setTitle(parseVoiceResults(results));
                updateRecipeTitleCard();
            } else {
                // Record speech cancelled.
            }
        } else if (requestCode == RECORD_STEP_TITLE_REQUEST) {
            if (resultCode == RESULT_OK) {
                List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                mStepTitle = "Step " + (mRecipe.getNumSteps() + 1) + ": " + parseVoiceResults(results);
                recordVideo();
            } else {
                // Record speech cancelled.
            }
        }
    }

    /**
     * Builds a Glass styled "Hello World!" view using the {@link Card} class.
     */
    private View createCard(String title, String footer) {
        Card card = new Card(this);
        card.setText(title);
        if (footer != null) {
            card.setFootnote(footer);
        }
        return card.getView();
    }

    private void updateStepCard() {
        mViews.remove(1);
        mViews.add(1, createCard("Create step " + (1 + mRecipe.getNumSteps()), null));
        mCardScroller.getAdapter().notifyDataSetInvalidated();
    }

    private void updateRecipeTitleCard() {
        mViews.remove(0);
        mViews.add(0, createCard("Name your recipe", mRecipe.getTitle()));
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

    private String parseVoiceResults(List<String> results) {
        StringBuffer buffer = new StringBuffer();
        for (int i=0; i<results.size(); i++) {
            buffer.append(results.get(i));
            if (i < results.size() - 1) {
                buffer.append(" ");
            }
        }

        buffer.replace(0, 1, String.valueOf(buffer.charAt(0)).toUpperCase());

        return buffer.toString();
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
}
