package me.piitex.app.backend;

public class ChatMessage {
    private Role sender;
    private String content;
    private String imageUrl;

    public ChatMessage(Role sender, String content, String imageUrl) {
        this.sender = sender;
        this.content = content;
        this.imageUrl = imageUrl;
    }

    public Role getSender() {
        return sender;
    }

    public void setSender(Role sender) {
        this.sender = sender;
    }

    public String getContentRaw() {
        return content;
    }

    public String getContent() {
        return content.replace("!@!", "\n");
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean hasImage() {
        return imageUrl != null && !imageUrl.isBlank();
    }
}