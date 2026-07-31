package com.bookshelves.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import java.io.IOException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FirebaseProperties.class)
@ConditionalOnProperty(prefix = "external.firebase", name = "enabled", havingValue = "true")
public class FirebaseConfig {

  @Bean
  public FirebaseApp firebaseApp(FirebaseProperties properties) throws IOException {
    FirebaseOptions.Builder options =
        FirebaseOptions.builder().setCredentials(GoogleCredentials.getApplicationDefault());
    if (properties.projectId() != null && !properties.projectId().isBlank()) {
      options.setProjectId(properties.projectId());
    }
    return FirebaseApp.initializeApp(options.build());
  }

  @Bean
  public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
    return FirebaseMessaging.getInstance(firebaseApp);
  }
}
