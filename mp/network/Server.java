package edu.illinois.cs.cs124.ay2023.mp.network;

import static edu.illinois.cs.cs124.ay2023.mp.helpers.Helpers.CHECK_SERVER_RESPONSE;
import static edu.illinois.cs.cs124.ay2023.mp.helpers.Helpers.OBJECT_MAPPER;

import android.util.Log;
import androidx.annotation.NonNull;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import edu.illinois.cs.cs124.ay2023.mp.application.CourseableApplication;
import edu.illinois.cs.cs124.ay2023.mp.models.Course;
import edu.illinois.cs.cs124.ay2023.mp.models.Summary;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * Development course API server.
 *
 * <p>Normally you would run this server on another machine, which the client would connect to over
 * the internet. For the sake of development, we're running the server alongside the app on the same
 * device. However, all communication between the course API client and course API server is still
 * done using the HTTP protocol. Meaning that it would be straightforward to move this code to an
 * actual server, which could provide data for all course API clients.
 */
public final class Server extends Dispatcher {
  /** List of summaries as a JSON string. */
  private final String summariesJSON;

  /** Helper method to create a 200 HTTP response with a body. */
  private MockResponse makeOKJSONResponse(@NonNull String body) {
    return new MockResponse()
        .setResponseCode(HttpURLConnection.HTTP_OK)
        .setBody(body)
        .setHeader("Content-Type", "application/json; charset=utf-8");
  }

  /** Helper value storing a 404 Not Found response. */
  private static final MockResponse HTTP_NOT_FOUND =
      new MockResponse()
          .setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
          .setBody("404: Not Found");

  /** Helper value storing a 400 Bad Request response. */
  private static final MockResponse HTTP_BAD_REQUEST =
      new MockResponse()
          .setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST)
          .setBody("400: Bad Request");

  /** GET the JSON with the list of course summaries. */
  private MockResponse getSummaries() {
    return makeOKJSONResponse(summariesJSON);
  }
  private MockResponse getCourse(String path) {

    return HTTP_BAD_REQUEST;
  }
  /**
   * HTTP request dispatcher.
   *
   * <p>This method receives HTTP requests from clients and determines how to handle them, based on
   * the request path and method.
   */
  @NonNull
  @Override
  public MockResponse dispatch(@NonNull RecordedRequest request) {
    try {
      // Reject requests without a path or method
      if (request.getPath() == null || request.getMethod() == null) {
        return HTTP_BAD_REQUEST;
      }

      // Normalize trailing slashes and method
      String path = request.getPath().replaceAll("/+", "/");
      String method = request.getMethod().toUpperCase();

      // Main dispatcher tree tree
      if (path.equals("/") && method.equals("GET")) {
        // Used by API client to validate server
        return new MockResponse()
            .setBody(CHECK_SERVER_RESPONSE)
            .setResponseCode(HttpURLConnection.HTTP_OK);
      } else if (path.equals("/reset/") && method.equals("GET")) {
        // Used to reset the server during testing
        return new MockResponse().setBody("200: OK").setResponseCode(HttpURLConnection.HTTP_OK);
      } else if (path.equals("/summary/") && method.equals("GET")) {
        Log.d("Server", "getSummaries called");
        return getSummaries();
      } else if (path.startsWith("/course/")) {
        return getCourse(path);
      } else {
        // Default is not found
        return HTTP_NOT_FOUND;
      }
    } catch (Exception e) {
      // Log an error and return 500 if an exception is thrown
      e.printStackTrace();
      return new MockResponse()
          .setResponseCode(HttpURLConnection.HTTP_INTERNAL_ERROR)
          .setBody("500: Internal Error");
    }
  }

  /**
   * Start the server if has not already been started, and wait for startup to finish.
   *
   * <p>Done in a separate thread to avoid blocking the UI.
   */
  public static void start() {
    if (isRunning(false)) {
      return;
    }
    new Server();
    if (!isRunning(true)) {
      throw new IllegalStateException("Server should be running");
    }
  }

  /** Number of times to check the server before failing. */
  private static final int RETRY_COUNT = 8;

  /** Delay between retries. */
  private static final int RETRY_DELAY = 512;

  /**
   * Determine if the server is currently running.
   *
   * @param wait whether to wait or not
   * @return whether the server is running or not
   * @throws IllegalStateException if something else is running on our port
   */
  public static boolean isRunning(boolean wait) {
    return isRunning(wait, RETRY_COUNT, RETRY_DELAY);
  }

  public static boolean isRunning(boolean wait, int retryCount, long retryDelay) {
    for (int i = 0; i < retryCount; i++) {
      OkHttpClient client = new OkHttpClient();
      Request request = new Request.Builder().url(CourseableApplication.SERVER_URL).get().build();
      try (Response response = client.newCall(request).execute()) {
        if (response.isSuccessful()) {
          if (Objects.requireNonNull(response.body()).string().equals(CHECK_SERVER_RESPONSE)) {
            return true;
          } else {
            throw new IllegalStateException(
                "Another server is running on port " + CourseableApplication.DEFAULT_SERVER_PORT);
          }
        }
      } catch (IOException ignored) {
        if (!wait) {
          break;
        }
        try {
          Thread.sleep(retryDelay);
        } catch (InterruptedException ignored1) {
        }
      }
    }
    return false;
  }

  /**
   * Reset the server. Used to reset the server between tests.
   *
   * @noinspection UnusedReturnValue, unused
   */
  public static boolean reset() throws IOException {
    OkHttpClient client = new OkHttpClient();
    Request request =
        new Request.Builder().url(CourseableApplication.SERVER_URL + "/reset/").get().build();
    try (Response response = client.newCall(request).execute()) {
      return response.isSuccessful();
    }
  }

  private Server() {
    // Disable server logging, since this is a bit verbose
    Logger.getLogger(MockWebServer.class.getName()).setLevel(Level.OFF);

    // Load data used by the server
    summariesJSON = loadData();

    try {
      // This resource needs to outlive the try-catch
      //noinspection resource
      MockWebServer server = new MockWebServer();
      server.setDispatcher(this);
      server.start(CourseableApplication.DEFAULT_SERVER_PORT);
    } catch (IOException e) {
      e.printStackTrace();
      throw new IllegalStateException(e.getMessage());
    }
  }

  /**
   * Helper method to load data used by the server.
   *
   * @return the summary JSON string.
   * @noinspection checkstyle:LineLength, checkstyle:LineLength
   */
  private String loadData() {
    // Load the JSON string
    String json =
        new Scanner(Server.class.getResourceAsStream("/courses.json"), "UTF-8")
            .useDelimiter("\\A").next();

    // Build the list of summaries
    List<Summary> summaries = new ArrayList<>();
    try {
      // Iterate through the list of JsonNodes returned by deserialization
      JsonNode nodes = OBJECT_MAPPER.readTree(json);
      for (JsonNode node : nodes) {
        // Deserialize as Summary and add to the list
        Summary summary = OBJECT_MAPPER.readValue(node.toString(), Summary.class);
        Course courses = OBJECT_MAPPER.readValue(node.toString(), Course.class);
        summaries.add(summary);
        mapCourse.put(courses,json);
      }

      // Convert the List<Summary> to a Map<String, Summary>


      // Convert the Map<String, Summary> to a String and return it
      return OBJECT_MAPPER.writerWithDefaultPrettyPrinter()
          .writeValueAsString(mapCourse.values());
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }
  private Map<Course, String> mapCourse = new HashMap<>();

}
