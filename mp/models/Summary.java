package edu.illinois.cs.cs124.ay2023.mp.models;

import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Model holding the course summary information shown in the summary list.
 *
 * @noinspection unused
 */
public class Summary implements Comparable<Summary> {
  private String subject;

  /**
   * Get the subject for this Summary.
   *
   * @return the subject for this Summary
   */
  @NotNull
  public final String getSubject() {
    return subject;
  }

  private String number;

  /**
   * Get the number for this Summary.
   *
   * @return the number for this Summary
   */
  @NotNull
  public final String getNumber() {
    return number;
  }

  private String label;

  /**
   * Get the label for this Summary.
   *
   * @return the label for this Summary
   */
  @NotNull
  public final String getLabel() {
    return label;
  }

  /** Create an empty Summary. */
  public Summary() {}

  /**
   * Create a Summary with the provided fields.
   *
   * @param setSubject the department for this Summary
   * @param setNumber the number for this Summary
   * @param setLabel the label for this Summary
   */
  public Summary(@NonNull String setSubject, @NonNull String setNumber, @NotNull String setLabel) {
    subject = setSubject;
    number = setNumber;
    label = setLabel;
  }

  /** {@inheritDoc} */
  @NonNull
  @Override
  public String toString() {
    return subject + " " + number + ": " + label;
  }

  @Override
  public int compareTo(Summary summary) {
    // sort first by number, then by subject
    int comparedValues = this.getNumber().compareTo(summary.getNumber());

    // If the numbers are the same, compare by subject
    if (comparedValues == 0) {
      return this.getSubject().compareTo(summary.getSubject());
    }

    return comparedValues;
  }

  public static List<Summary> filter(List<Summary> summaries, String filter) {
    String less = filter.trim().toLowerCase();
    List<Summary> filteredSummaries = new ArrayList<>();

    for (Summary summary : summaries) {
      String allSummaries = summary.toString();
      if (allSummaries.toLowerCase().contains(less)) {
        filteredSummaries.add(summary);
      }
    }

    Collections.sort(filteredSummaries);

    filteredSummaries.sort(
        (one, two) -> {
          String first = one.toString().toLowerCase();
          String second = two.toString().toLowerCase();
          return first.indexOf(less) - second.indexOf(less);
        });

    return filteredSummaries;
  }

  public String getCourseId() {
    return null;
  }
}
