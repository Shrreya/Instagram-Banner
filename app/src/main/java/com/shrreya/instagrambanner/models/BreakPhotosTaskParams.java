package com.shrreya.instagrambanner.models;

public class BreakPhotosTaskParams {

    private byte[] photo;
    private int panels, panelLength;

    public BreakPhotosTaskParams(byte[] photo, int panels, int panelLength) {
        this.photo = photo;
        this.panels = panels;
        this.panelLength = panelLength;
    }

    public byte[] getPhoto() {
        return photo;
    }

    public int getPanels() {
        return panels;
    }

    public int getPanelLength() {
        return panelLength;
    }
}

