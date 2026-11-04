package net.sourceforge.kolmafia.ui;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import internal.helpers.Cleanups;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class UserInterfaceThreadTest {
  private static final UserInterface DEFAULTS = new UserInterface() {};

  private static Cleanups withDefaultUserInterface() {
    UserInterfaceManager.setUserInterface(DEFAULTS);
    return new Cleanups(() -> UserInterfaceManager.setUserInterface(null));
  }

  /** Waits until the user interface thread has run everything queued so far. */
  private static void drain() throws InterruptedException {
    var done = new CountDownLatch(1);
    DEFAULTS.invokeLater(done::countDown);
    assertThat(done.await(10, TimeUnit.SECONDS), is(true));
  }

  @Test
  void runsWorkOnItsOwnThread() throws Exception {
    var onUiThread = new AtomicBoolean(false);

    try (var cleanups = withDefaultUserInterface()) {
      DEFAULTS.invokeLater(() -> onUiThread.set(DEFAULTS.isUiThread()));
      drain();

      assertThat(onUiThread.get(), is(true));
      assertThat(DEFAULTS.isUiThread(), is(false));
    }
  }

  @Test
  void keepsRunningAfterATaskFails() throws Exception {
    var ran = new AtomicBoolean(false);

    try (var cleanups = withDefaultUserInterface()) {
      DEFAULTS.invokeLater(
          () -> {
            throw new IllegalStateException("expected by the test");
          });
      DEFAULTS.invokeLater(() -> ran.set(true));
      drain();

      assertThat(ran.get(), is(true));
    }
  }
}
