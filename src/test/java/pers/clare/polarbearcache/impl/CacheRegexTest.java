package pers.clare.polarbearcache.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import pers.clare.polarbeartest.ApplicationTest2;
import pers.clare.polarbeartest.service.regex.RegexService;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

@DisplayName("PolarBearCacheRegexTest")
@TestInstance(PER_CLASS)
@SpringBootTest(classes = ApplicationTest2.class)
class CacheRegexTest {

    @Autowired
    private RegexService regexService;

    @Test
    void update() {
        String a = "a";
        String b = "b";
        Object aOne = regexService.find(a, a, a);
        Object aMap = regexService.findMap(a, a);
        Object aGroup = regexService.findGroup(a);

        Object bOne = regexService.find(b, b, b);
        Object bMap = regexService.findMap(b, b);
        Object bGroup = regexService.findGroup(b);

        regexService.update(a, a, a);

        assertNotSame(aOne, regexService.find(a, a, a));
        assertNotSame(aMap, regexService.findMap(a, a));
        assertNotSame(aGroup, regexService.findGroup(a));

        assertSame(bOne, regexService.find(b, b, b));
        assertSame(bMap, regexService.findMap(b, b));
        assertSame(bGroup, regexService.findGroup(b));
    }


    @Test
    void batch() {
        String a = "a";
        String b = "b";
        Object aOne = regexService.find(a, a, a);
        Object aMap = regexService.findMap(a, a);
        Object aGroup = regexService.findGroup(a);

        Object bOne = regexService.find(b, b, b);
        Object bMap = regexService.findMap(b, b);
        Object bGroup = regexService.findGroup(b);

        regexService.batch(a, a);

        assertNotSame(aOne, regexService.find(a, a, a));
        assertNotSame(aMap, regexService.findMap(a, a));
        assertNotSame(aGroup, regexService.findGroup(a));

        assertSame(bOne, regexService.find(b, b, b));
        assertSame(bMap, regexService.findMap(b, b));
        assertSame(bGroup, regexService.findGroup(b));
    }
    @Test
    void batch2() {
        String a = "a";
        String b = "b";
        Object aOne = regexService.find(a, a, a);
        Object aMap = regexService.findMap(a, a);
        Object aGroup = regexService.findGroup(a);

        Object bOne = regexService.find(b, b, b);
        Object bMap = regexService.findMap(b, b);
        Object bGroup = regexService.findGroup(b);

        regexService.batch2(a);

        assertNotSame(aOne, regexService.find(a, a, a));
        assertNotSame(aMap, regexService.findMap(a, a));
        assertNotSame(aGroup, regexService.findGroup(a));

        assertSame(bOne, regexService.find(b, b, b));
        assertSame(bMap, regexService.findMap(b, b));
        assertNotSame(bGroup, regexService.findGroup(b));
    }
}
