package org.perfcake.reporting.destination.anomalyDetection;

/**
 * Distinguishing performance issues.
 *
 * @author <a href="mailto:kurovamartina@gmail.com">Martina Kůrová</a>
 */
public enum PerformanceIssueType {
   REGULAR_SPIKES,
   DEGRADATION,
   THRESHOLD_EXCEEDED,
   OK;
}