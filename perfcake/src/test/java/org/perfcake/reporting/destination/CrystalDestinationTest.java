/*
 * -----------------------------------------------------------------------\
 * PerfCake
 *  
 * Copyright (C) 2010 - 2016 the original author or authors.
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
package org.perfcake.reporting.destination;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.perfcake.TestSetup;
import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.Quantity;
import org.perfcake.reporting.reporter.Reporter;
import org.perfcake.reporting.reporter.ResponseTimeStatsReporter;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

import static org.mockito.Mockito.*;

/**
 * Tests {@link CrystalDestination}.
 *
 * @author <a href="mailto:kurovamartina@gmail.com">Martina Kůrová</a>
 */
@Test(groups = { "unit" })
public class CrystalDestinationTest extends TestSetup {

   private static final Logger log = LogManager.getLogger(CrystalDestinationTest.class);
   private static final long ITERATION = 12345;


   @Test
   public void testReportingToSingleDocument() throws Exception {
      String title1 = "Crystal Destination 1";
      String title2 = "Crystal Destination 2";
      String path = "Results";
      final CrystalDestination cd1 = new CrystalDestination();
      final CrystalDestination cd2 = new CrystalDestination();
      cd1.setTitle(title1);
      cd2.setTitle(title2);
      cd1.setPath(path);
      cd2.setPath(path);
      cd1.open();
      cd2.open();
      Assert.assertTrue(cd1.getSharedResultsMap().containsKey(title1));
      Assert.assertTrue(cd1.getSharedResultsMap().containsKey(title2));
      Assert.assertTrue(cd2.getSharedResultsMap().containsKey(title1));
      Assert.assertTrue(cd2.getSharedResultsMap().containsKey(title2));
   }

   @Test
   public void testOneMasterCrystalDestination() throws Exception {
      String title1 = "Crystal Destination 1";
      String title2 = "Crystal Destination 2";
      String path1 = "Results1";
      String path2 = "Results2";

      final CrystalDestination cd1mock = mock(CrystalDestination.class);
      when(cd1mock.getParentReporterName()).thenReturn("mock");

      final CrystalDestination cd2mock = mock(CrystalDestination.class);
      when(cd2mock.getParentReporterName()).thenReturn("mock");

      final CrystalDestination cd1 = cd1mock;
      final CrystalDestination cd2 = cd2mock;
      cd1.setTitle(title1);
      cd2.setTitle(title2);
      cd1.setPath(path1);
      cd2.setPath(path2);
      cd1.open();
      cd2.open();

      for(int i=0; i<5; i++) {
         final Measurement measurement = new Measurement(42, 123456000, ITERATION - 1); // first iteration index is 0
         measurement.set("Result", new Quantity<>(222.22, "ms"));
         cd1.report(measurement);
         cd2.report(measurement);
      }
      cd1.close();
      cd2.close();

      Assert.assertTrue(!(cd1.isMasterThread() || cd2.isMasterThread()));
   }
}
