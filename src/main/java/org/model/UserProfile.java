package org.model;

public record UserProfile(
        int label,
        String name,
        int samples
) {
    @Override
    public String toString() {
        return label + ". " + name + " (" + samples + " фото)";
    }
}