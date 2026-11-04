package net.sourceforge.kolmafia.ui;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;

import internal.helpers.Cleanups;
import java.util.ArrayList;
import java.util.List;
import net.sourceforge.kolmafia.KoLCharacter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UserInterfaceManagerTest {
  @BeforeAll
  static void beforeAll() {
    KoLCharacter.reset("UserInterfaceManagerTest");
  }

  /** Records the notifications the engine sends. */
  private static class RecordingUserInterface implements UserInterface {
    final List<String> notified = new ArrayList<>();

    @Override
    public void meatChanged() {
      this.notified.add("meat");
    }

    @Override
    public void safetyDetailsChanged() {
      this.notified.add("safety");
    }
  }

  private static Cleanups withUserInterface(final UserInterface userInterface) {
    UserInterfaceManager.setUserInterface(userInterface);
    return new Cleanups(() -> UserInterfaceManager.setUserInterface(null));
  }

  @Nested
  class Installing {
    @Test
    void usesTheInstalledUserInterface() {
      var ui = new RecordingUserInterface();

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

  @Nested
  class Notifications {
    @Test
    void reportsMeatChanges() {
      var ui = new RecordingUserInterface();
      KoLCharacter.setAvailableMeat(0);
      KoLCharacter.setStorageMeat(0);

      try (var cleanups = withUserInterface(ui)) {
        KoLCharacter.setAvailableMeat(100);
        KoLCharacter.setAvailableMeat(100);
        KoLCharacter.setStorageMeat(50);

        assertThat(ui.notified, contains("meat", "meat"));
      } finally {
        KoLCharacter.setAvailableMeat(0);
        KoLCharacter.setStorageMeat(0);
      }
    }

    @Test
    void reportsSafetyChangesWhenMindControlChanges() {
      var ui = new RecordingUserInterface();
      KoLCharacter.setMindControlLevel(0);

      try (var cleanups = withUserInterface(ui)) {
        KoLCharacter.setMindControlLevel(0);
        assertThat(ui.notified, empty());

        KoLCharacter.setMindControlLevel(5);
        assertThat(ui.notified, contains("safety"));
      } finally {
        KoLCharacter.setMindControlLevel(0);
      }
    }
  }
}
