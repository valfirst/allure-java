package io.qameta.allure.testng.samples;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Make sure this class can run without causing a ConcurrentModificationException.
 */
public class ParallelDataProviderSample {

  @DataProvider(name = "provideData", parallel = true)
  Iterator<Object[]> provide() {
    List<Object[]> ret = new ArrayList<>();
    for (int i = 0; i < 1000; i++) {
      ret.add(new Integer[]{i});
    }
    return ret.iterator();
  }

  @Test(dataProvider = "provideData", invocationCount = 2, threadPoolSize = 2)
  public void checkCME(Integer i) {
    Assert.assertNotNull(i);
  }
}