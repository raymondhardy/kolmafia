package net.sourceforge.kolmafia.ui;

import static internal.helpers.Player.withUserInterface;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;

import org.junit.jupiter.api.Test;

class UserInterfaceManagerTest {
  @Test
  void usesTheInstalledUserInterface() {
    var ui = new UserInterface() {};

    try (var cleanups = withUserInterface(ui)) {
      assertThat(UserInterfaceManager.getUserInterface(), sameInstance(ui));
    }
  }

  @Test
  void findsAUserInterfaceWhenNoneIsInstalled() {
    try (var cleanups = withUserInterface(null)) {
      UserInterface found = UserInterfaceManager.getUserInterface();

      assertThat(found, notNullValue());
      assertThat(UserInterfaceManager.getUserInterface(), sameInstance(found));
    }
  }

  @Test
  void defaultsShowNothing() {
    UserInterface defaults = new UserInterface() {};

    assertThat(defaults.hasOpenWindows(), is(false));
    assertThat(defaults.isDarkTheme(), is(false));
  }
}
