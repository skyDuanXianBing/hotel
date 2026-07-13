package server.demo.dto;

public class SuMessagingAttachmentDTO {
    private Long id;
    private String mimeType;
    private String fileName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
}
