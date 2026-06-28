package com.azeem.blueprint.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "martin-knowledge")
public class MartinKnowledgeS3Config {
  private String folderPath;
  private String bucketName;
  private List<String> keys;

  public String getFolderPath() {
    return folderPath;
  }

  public void setFolderPath(String folderPath) {
    this.folderPath = folderPath;
  }

  public String getBucketName() {
    return bucketName;
  }

  public void setBucketName(String bucketName) {
    this.bucketName = bucketName;
  }

  public List<String> getKeys() {
    return keys;
  }

  public void setKeys(List<String> keys) {
    this.keys = keys;
  }
}
