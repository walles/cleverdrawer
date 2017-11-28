package com.gmail.walles.johan.cleverdrawer;

import static org.hamcrest.CoreMatchers.is;

import org.junit.Assert;
import org.junit.Test;

public class LaunchableTest {
    @Test
    public void testMatches() {
        Launchable launchable = new Launchable("fot", "Gnu");
        Assert.assertThat(launchable.matches("g"), is(true));
        Assert.assertThat(launchable.matches("n"), is(true));
        Assert.assertThat(launchable.matches("u"), is(true));
        Assert.assertThat(launchable.matches("nu"), is(true));
        Assert.assertThat(launchable.matches("gnu"), is(true));

        Assert.assertThat(launchable.matches("f"), is(false));
        Assert.assertThat(launchable.matches("o"), is(false));
        Assert.assertThat(launchable.matches("t"), is(false));
        Assert.assertThat(launchable.matches("fot"), is(false));
    }
}
