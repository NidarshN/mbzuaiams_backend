package com.mbzuai.ams.repository;

import com.mbzuai.ams.model.FileDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface FileRepository extends MongoRepository<FileDocument, String> {
}
