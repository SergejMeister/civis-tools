/*
 * Copyright 2015 Sergej Meister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.civis.utils.html.parser;


import com.civis.utils.html.models.HtmlLink;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Test util html link parser.
 */
public class HtmlParserTest {

    public static final String MONSTER_MAIL_PATH = "htmls/monsterjobs.html";
    public static final String STEPSTONE_MAIL_PATH = "htmls/stepstonejobs.html";
    public static final String STEPSTONE_JOB_FACILITY = "htmls/stepStoneFacility.html";
    public static final String T_SYSTEM_JOB_PATH = "htmls/tsystem.html";
    public static final String FUTURICE_JOB_PATH = "htmls/futurice.html";
    public static final String HAPEKO_JOB_PATH = "htmls/hapeko.html";

    @Test
    public void testFindFirstFrame() {
        String stepStoneMailContent = getMailContent(STEPSTONE_JOB_FACILITY);
        String plainText = new HtmlParser(stepStoneMailContent).toPlainText().getContent();
        Assert.assertTrue(plainText.contains("Ähnliche Stellenangebote"));
        plainText = new HtmlParser(stepStoneMailContent).findFirstFrame().toPlainText().getContent();
        Assert.assertFalse(plainText.contains("Ähnliche Stellenangebote"));
    }

    @Test
    public void testFindAllHtmlLinksInMonsterMail() {
        String monsterMailContent = getMailContent(MONSTER_MAIL_PATH);
        List<HtmlLink> links = new HtmlParser(monsterMailContent).parse().getLinks();
        Assert.assertEquals("This mail content include only 20 links!", 20, links.size());
    }

    @Test
    public void testFindHtmlLinksInMonsterMailWithFilter() {
        String monsterMailContent = getMailContent(MONSTER_MAIL_PATH);
        List<String> linkMatcher = Collections.singletonList("stellenanzeige.monster.de");
        HtmlParseFilter htmlParseFilter = getHtmlParseFilter(linkMatcher);
        List<HtmlLink> links = new HtmlParser(monsterMailContent, htmlParseFilter).parse().getLinks();
        Assert.assertEquals("This mail content include only 4 job links!", 4, links.size());
    }

    @Test
    public void testFindAllHtmlLinksInStepStoneMail() {
        String stepStoneMailContent = getMailContent(STEPSTONE_MAIL_PATH);
        List<HtmlLink> links = new HtmlParser(stepStoneMailContent).parse().getLinks();
        Assert.assertEquals("This mail content include only 32 links!", 32, links.size());
    }

    @Test
    public void testFindHtmlLinksInStepStoneMailWithFilter() {
        String stepStoneMailContent = getMailContent(STEPSTONE_MAIL_PATH);
        //ja.cfm in url means in stepstone mail job agent!
        List<String> linkMatcherList = Collections.singletonList("www.stepstone.de/ja.cfm");
        HtmlParseFilter htmlParseFilter = getHtmlParseFilter(linkMatcherList);
        List<HtmlLink> links = new HtmlParser(stepStoneMailContent, htmlParseFilter).parse().getLinks();
        Assert.assertEquals("This mail content include only 16 job links!", 16, links.size());
    }

    @Test
    public void testParseUrl() {
        String url1 = "Dat ist ein Text mit einem Url http://www.test.de Punkt!";
        String url2 = "Dat ist ein Text mit einem Url www.test.de Punkt!";
        String url3 =
                "http://www.ubiteck.com/test/mypage.jsf?param1=ok http://simpleFileUrl.txt https://backslashUrl.txt";
        String url4 = "Dat ist ein Text mit einem Url www.test1.de Punkt!";
        String url5 = "Dat ist ein Text mit einem Url www.test.de/bewerbung Punkt!";
        String url6 = "Dat ist ein Text mit einem Url https://www.test.de/bewerbung Punkt!";

        List<String> test1 = new HtmlParser(url1).parse().getUrls();
        Assert.assertEquals("Should have only one url!", 1, test1.size());
        Assert.assertEquals("http://www.test.de", test1.get(0));

        List<String> test2 = new HtmlParser(url2).parse().getUrls();
        Assert.assertEquals("Should have only one url!", 1, test2.size());
        Assert.assertEquals("www.test.de", test2.get(0));

        List<String> test3 = new HtmlParser(url3).parse().getUrls();
        Assert.assertEquals("Should have only one url!", 3, test3.size());

        List<String> test4 = new HtmlParser(url4).parse().getUrls();
        Assert.assertEquals("Should have only one url!", 1, test4.size());
        Assert.assertEquals("www.test1.de", test4.get(0));

        List<String> test5 = new HtmlParser(url5).parse().getUrls();
        Assert.assertEquals("Should have only one url!", 1, test5.size());
        Assert.assertEquals("www.test.de/bewerbung", test5.get(0));

        List<String> test6 = new HtmlParser(url6).parse().getUrls();
        Assert.assertEquals("Should have only one url!", 1, test6.size());
        Assert.assertEquals("https://www.test.de/bewerbung", test6.get(0));
    }

    @Test
    public void testEmail() {
        String monsterMailContent = getMailContent(T_SYSTEM_JOB_PATH);
        String monsterEmail = new HtmlParser(monsterMailContent).parse().getMail();
        Assert.assertEquals("jobs@t-systems-mms.com", monsterEmail);

        String testContent = "hiermit moechte ich mien E-Mail job@test testen, das nicht gefudnden werden muss!";
        String mail = new HtmlParser(testContent).parse().getMail();
        Assert.assertNull(mail);

        testContent = "hiermit moechte ich mien E-Mail job@test. testen, das nicht gefudnden werden muss!";
        mail = new HtmlParser(testContent).parse().getMail();
        Assert.assertNull(mail);

        testContent = "hiermit moechte ich mien E-Mail job@test.de testen, das nicht gefudnden werden muss!";
        mail = new HtmlParser(testContent).parse().getMail();
        Assert.assertNotNull(mail);

        testContent = "hiermit moechte ich mien E-Mail job@test.de.de testen, das nicht gefudnden werden muss!";
        mail = new HtmlParser(testContent).parse().getMail();
        Assert.assertNotNull(mail);
    }

    @Test
    public void testFuturice() {
        String futuricerMailContent = getMailContent(FUTURICE_JOB_PATH);

        HtmlParseFilter htmlParseFilter = getHtmlParseFilter(Collections.emptyList());
        htmlParseFilter.setIgnore(
                Arrays.asList("www.tecoloco.com", "www.irishjobs.ie", "www.estascontratado.com", "www.myjob.mu",
                        "www.caribbeanjobs.com", "www.nijobs.com", "www.jobs.lu", "www.pnet.co.za"));

        List<String> urls = new HtmlParser(futuricerMailContent, htmlParseFilter).toPlainText().parse().getUrls();
        Assert.assertEquals("Should have only one url!", 1, urls.size());
        Assert.assertEquals("www.futurice.com", urls.get(0));
    }

    @Test
    public void testHapeko() {
        String hapekoJobContent = getMailContent(HAPEKO_JOB_PATH);
        List<String> urls = new HtmlParser(hapekoJobContent).toPlainText().parse().getUrls();
        Assert.assertEquals("Should have only one url!", 1, urls.size());
        Assert.assertEquals("www.hapeko.de", urls.get(0));
    }

    @Test
    public void testContent() {
        String futuricerMailContent = getMailContent(FUTURICE_JOB_PATH);
        String plainText = new HtmlParser(futuricerMailContent).toPlainText().getContent();
        Assert.assertNotNull(plainText);
        Assert.assertEquals(4190, plainText.length());
    }

    private String getMailContent(String fileName) {
        String mailContent = "";
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName)) {
            mailContent = IOUtils.toString(inputStream, "UTF-8");
            inputStream.close();
        } catch (IOException e) {
            System.out.println(e.getLocalizedMessage());
        }

        return mailContent;
    }

    private HtmlParseFilter getHtmlParseFilter(List<String> linkMatcherList) {
        HtmlParseFilter htmlLinkParseFilter = new HtmlParseFilter();
        htmlLinkParseFilter.setNullableText(Boolean.FALSE);
        htmlLinkParseFilter.setLinkMatcherList(linkMatcherList);
        return htmlLinkParseFilter;
    }
}