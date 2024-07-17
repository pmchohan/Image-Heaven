package com.example.mad_final_prep;

public class ImageModel {
    public String email;
    public String imageName;
    public String imageUrl;
    public boolean broadcasted;

    public ImageModel(String email, String imageName, String imageUrl, boolean broadcasted) {
        this.email = email;
        this.imageName = imageName;
        this.imageUrl = imageUrl;
        this.broadcasted = broadcasted;
    }
    public ImageModel() {}

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isBroadcasted() {
        return broadcasted;
    }

    public void setBroadcasted(boolean broadcasted) {
        this.broadcasted = broadcasted;
    }
}
