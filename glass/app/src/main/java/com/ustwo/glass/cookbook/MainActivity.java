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
    private static final String OUTPUT_PATH = "/storage/emulated/0/DCIM/Camera/recipe.json";

    private static final int RECORD_VIDEO_REQUEST = 1;
    private static final int RECORD_RECIPE_TITLE_REQUEST = 2;
    private static final int RECORD_STEP_TITLE_REQUEST = 2;

    /** {@link CardScrollView} to use as the main content view. */
    private CardScrollView mCardScroller;
    private List<View> mViews = new ArrayList<View>();

    private Recipe mRecipe;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        Slog.init(getApplicationContext());

        mRecipe = new Recipe("Untitled");
//        mRecipe.addStep(new Recipe.Step("Test", "1.mp4"));

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
                        // Record recipe title.
                        recordSpeech(RECORD_RECIPE_TITLE_REQUEST);
                        break;
                    case 1:
                        // Record a step.
                        recordVideo();
                        break;
                    case 2:
                        // Publish.
                        if (mRecipe.getNumSteps() > 0) {
                            publish(mRecipe);
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
                int stepCount = mRecipe.getNumSteps() + 1;
                mRecipe.addStep(new Recipe.Step("Step " + stepCount, videoPath));
            } else {
                // Video record cancelled.
            }
        } else if (requestCode == RECORD_RECIPE_TITLE_REQUEST) {
            if (resultCode == RESULT_OK) {
                List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                setRecipeTitle(results);
            } else {
                // Record speech cancelled.
            }
        }
    }

    /**
     * Builds a Glass styled "Hello World!" view using the {@link Card} class.
     */
    private View createCard(String title) {
        Card card = new Card(this);
        card.setText(title);
        return card.getView();
    }

    private void recordSpeech(int type) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        startActivityForResult(intent, type);
    }

    private void recordVideo() {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        startActivityForResult(intent, RECORD_VIDEO_REQUEST);
    }

    private void setRecipeTitle(List<String> recipeTitleResults) {
        StringBuffer buffer = new StringBuffer();
        for (int i=0; i<recipeTitleResults.size(); i++) {
            buffer.append(recipeTitleResults.get(i));
            if (i < recipeTitleResults.size() - 1) {
                buffer.append(" ");
            }
        }
        mRecipe.setTitle(buffer.toString());
    }

    private boolean publish(Recipe recipe) {
        boolean result = true;
        OutputStreamWriter writer = null;
        try {
            Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
            writer = new OutputStreamWriter(new FileOutputStream(OUTPUT_PATH));
            writer.write(gson.toJson(mRecipe));
        }
        catch (Exception e) {
            result = false;
            Slog.e("Failed to publish to: " + OUTPUT_PATH, e);
        }
        finally {
            try {
                writer.close();
            } catch (Exception e) {}
        }
        return result;
    }
}
