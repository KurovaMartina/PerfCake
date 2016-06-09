/*
 * -----------------------------------------------------------------------\
 * SilverWare
 *  
 * Copyright (C) 2014 - 2016 the original author or authors.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------------/
 */
package org.perfcake.reporting.reporters;

import org.perfcake.PerfCakeConst;
import org.perfcake.common.PeriodType;
import org.perfcake.reporting.MeasurementUnit;
import org.perfcake.reporting.ReportingException;
import org.perfcake.reporting.destinations.Destination;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Streams all recorded {@link org.perfcake.reporting.MeasurementUnit MesurementUnits} to a file for later replay.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class RawReporter extends AbstractReporter {

   /**
    * The reporter's logger.
    */
   private static final Logger log = LogManager.getLogger(RawReporter.class);

   /**
    * Channel to store data.
    */
   private AsynchronousFileChannel fileChannel;

   /**
    * Keeps the position of the next write to the file.
    */
   private final AtomicLong filePointer = new AtomicLong(0L);

   private List<Future<Integer>> fileOperations = new LinkedList<>();

   private int gcCounter = 0;

   private static Semaphore gcSemaphore = new Semaphore(1);

   /**
    * Output file.
    */
   private String outputFile = "perfcake-measurement-" + System.getProperty(PerfCakeConst.TIMESTAMP_PROPERTY) + ".raw";

   @Override
   public void start() {
      if (getDestinations().size() > 0) {
         log.warn("No destinations are supported with RawReporter. The destinations that wre configured won't be used and no results will be sent to them.");
      }
      reset();
   }

   @Override
   public void stop() {
      if (fileChannel != null && fileChannel.isOpen()) {
         try {
            fileOperations.stream().forEach(fo -> {
               try {
                  fo.get();
               } catch (Exception e) {
                  log.warn("Unable to complete file operation: ", e);
               }
            });
            fileChannel.close();
         } catch (IOException e) {
            log.warn("Unable to close file channel with results: ", e);
         }
      }
   }

   @Override
   protected void doReset() {
      stop();

      try {
         synchronized (filePointer) {
            fileChannel = AsynchronousFileChannel.open(Paths.get(outputFile), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            filePointer.set(0);
         }
      } catch (IOException e) {
         log.error("Unable to open file for writing results. No results will be written: ", e);
      }
   }

   @Override
   protected void doReport(final MeasurementUnit measurementUnit) throws ReportingException {
      try (
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final ObjectOutputStream oos = new ObjectOutputStream(baos);
      ) {
         measurementUnit.streamOut(oos);
         oos.flush();

         final byte[] bytes = baos.toByteArray();
         enqueueFileOperation(fileChannel.write(ByteBuffer.wrap(bytes), filePointer.getAndAdd(bytes.length)));
      } catch (IOException e) {
         throw new ReportingException("Unable to report to raw reporter. Cannot write to the output file: ", e);
      }
   }

   private void enqueueFileOperation(final Future<Integer> fileOperation) {
      fileOperations.add(fileOperation);

      // perform garbage collection of completed file operations
      if (gcCounter++ > 1000) {
         gcCounter = 0;

         try {
            gcSemaphore.acquire();
            List<Future<Integer>> newFileOperations = new LinkedList<>();
            fileOperations.stream().forEach(fo -> {
               if (!fo.isDone()) {
                  newFileOperations.add(fo);
               }
            });
            fileOperations = newFileOperations;
         } catch (InterruptedException ie) {
            // nothing happened, we will give it one more try
         } finally {
            gcSemaphore.release();
         }
      }
   }

   @Override
   public void publishResult(final PeriodType periodType, final Destination destination) throws ReportingException {
      // nothing to do, we ignore all destinations and we warned the user
   }

   /**
    * Gets the name of the output file where the results are written.
    *
    * @return The name of the output file where the results are written.
    */
   public String getOutputFile() {
      return outputFile;
   }

   /**
    * Sets the name of the output file where to write the results.
    *
    * @param outputFile
    *       The name of the output file where to write the results.
    */
   public void setOutputFile(final String outputFile) {
      this.outputFile = outputFile;
   }
}
