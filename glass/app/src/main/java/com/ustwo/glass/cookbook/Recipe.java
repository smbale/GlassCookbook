package com.ustwo.glass.cookbook;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Recipe {
    @Expose @SerializedName("title")
    private String mTitle;
    @Expose @SerializedName("steps")
    private List<Step> mSteps = new ArrayList<Step>();

    public Recipe(String title) {
        mTitle = title;
    }

    public int getNumSteps() {
        return mSteps.size();
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public static class Step {
        @Expose @SerializedName("title")
        private String mTitle;
        @Expose @SerializedName("video")
        private String mVideo;

        public Step(String title, String video) {
            mTitle = title;
            // We only want the name for our demo.
            mVideo = new File(video).getName();
        }
    }

    public void addStep(Step step) {
        mSteps.add(step);
    }
}
