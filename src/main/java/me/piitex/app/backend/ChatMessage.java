package me.piitex.app.backend;

public class ChatMessage {
    private Role sender;
    private String content;
    private String imageUrl;
    private String reasoning;

    public ChatMessage(Role sender, String content, String imageUrl, String reasoning) {
        this.sender = sender;
        this.content = content;
        this.imageUrl = imageUrl;
        this.reasoning = reasoning;
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

    public String getReasoning() {
        if (reasoning == null) return null;
        return reasoning.replace("!@!", "\n");
    }

    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }
}