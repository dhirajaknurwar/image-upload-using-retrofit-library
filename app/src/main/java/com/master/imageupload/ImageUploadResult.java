package com.master.imageupload;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class ImageUploadResult {

    @SerializedName("result")
    @Expose
    private Result result;

    public String getImage() {
        return result.getFiles().getPic().get(0).getName();
    }

    private class Result {

        @SerializedName("files")
        @Expose
        private Files files;

        /**
         * @return The files
         */
        private Files getFiles() {
            return files;
        }

    }

    private class Files {

        @SerializedName("images")
        @Expose
        private List<Pic> images = new ArrayList<Pic>();


        /**
         * @return The pic
         */
        public List<Pic> getPic() {
            return images;
        }

        /**
         * @param pic The pic
         */
        public void setPic(List<Pic> pic) {
            this.images = pic;
        }

    }


    class Pic {

        @SerializedName("name")
        @Expose
        private String name;

        /**
         * @return The name
         */
        public String getName() {
            return name;
        }

        /**
         * @param name The name
         */
        public void setName(String name) {
            this.name = name;
        }

    }
}
