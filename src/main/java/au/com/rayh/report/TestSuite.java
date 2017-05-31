/*
 * The MIT License
 *
 * Copyright (c) 2011 Ray Yamamoto Hilton
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package au.com.rayh.report;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

@XmlRootElement(name="testsuite")
@XmlAccessorType(XmlAccessType.NONE)
public class TestSuite {
    @XmlAttribute
    int failures;
    
    @XmlAttribute
    int errors;
    
    @XmlAttribute
    String hostname;
    
    @XmlAttribute
    String name;
    
    @XmlAttribute
    int tests;
    
    @XmlAttribute
    float time;
    
    @XmlAttribute(name="timestamp")
    Date endTime;
    
    @XmlElement(name="testcase")
    List<TestCase> testcases = new ArrayList<>();

    @XmlTransient
    Date startTime;

    public TestSuite() {
    }
    
    @SuppressFBWarnings({"EI_EXPOSE_REP2", "URF_UNREAD_FIELD"})
    public TestSuite(String hostname, String name, Date startTime) {
        this.hostname = hostname;
        this.name = name;
        this.startTime = startTime;
    }

    public int getFailures() {
        return failures;
    }
    
    public int getErrors() {
        return errors;
    }

    public int getTests() {
        return tests;
    }

    public void addFailure() {
        failures+=1;
    }
    
    public void addError() {
        errors+=1;
    }

    public void addTest() {
        tests+=1;
    }
    
    public String getName() {
        return name;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    public Date getStartTime() {
        return startTime;
    }

    @SuppressFBWarnings({"EI_EXPOSE_REP2", "URF_UNREAD_FIELD"})
    public void setEndTime(Date endTime) {
        this.endTime = endTime;
        this.time = (endTime.getTime() - startTime.getTime())/1000f;
    }

    public List<TestCase> getTestCases() {
        return testcases;
    }

}
