package com.mbzuai.ams.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "files")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileDocument {
    @Id
    private String id;
    
    private String fileName;
    private String contentType;
    private byte[] content;
}
