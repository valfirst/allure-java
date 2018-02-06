package io.qameta.allure.testng.samples;

import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class TestsWithFixtures {

    @BeforeSuite
    public void beforeSuiteOne() {

    }

    @BeforeSuite
    public void beforeSuiteTwo() {

    }

    @BeforeTest
    public void beforeTestOne() {

    }

    @BeforeTest
    public void beforeTestTwo() {

    }

    @BeforeClass
    public void beforeClassOne() {

    }

    @BeforeClass
    public void beforeClassTwo() {

    }

    @BeforeGroups(groups = "first")
    public void beforeGroupOne() {

    }

    @BeforeGroups(groups = "first")
    public void beforeGroupTwo() {

    }

    @BeforeGroups("first")
    public void beforeGroupThree() {

    }

    @BeforeGroups("first")
    public void beforeGroupFour() {

    }

    @BeforeMethod
    public void beforeMethodOne() {

    }

    @BeforeMethod
    public void beforeMethodTwo() {

    }

    @AfterSuite
    public void afterSuiteOne() {

    }

    @AfterSuite
    public void afterSuiteTwo() {

    }

    @AfterTest
    public void afterTestOne() {

    }

    @AfterTest
    public void afterTestTwo() {

    }

    @AfterClass
    public void afterClassOne() {

    }

    @AfterClass
    public void afterClassTwo() {

    }

    @AfterGroups(groups = "first")
    public void afterGroupOne() {

    }

    @AfterGroups(groups = "first")
    public void afterGroupTwo() {

    }

    @AfterGroups("first")
    public void afterGroupThree() {

    }

    @AfterGroups("first")
    public void afterGroupFour() {

    }

    @AfterMethod
    public void afterMethodOne() {

    }

    @AfterMethod
    public void afterMethodTwo() {

    }

    @Test(groups = "first")
    public void testOne() {

    }

    @Test(groups = "first")
    public void testTwo() {

    }
}
