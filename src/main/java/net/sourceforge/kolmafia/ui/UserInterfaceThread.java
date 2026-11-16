package net.sourceforge.kolmafia.ui;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.sourceforge.kolmafia.StaticEntity;

/**
 * The thread that runs the default {@link UserInterface#invokeLater} work: one task at a time, in
 * order, as Swing's event thread does. Engine code that hands work to the user interface thread
 * (chat buffers, for example) relies on that, so a user interface without its own thread still gets
 * one.
 */
final class UserInterfaceThread {
  private static volatile Thread thread;

  private static final ExecutorService EXECUTOR =
      Executors.newSingleThreadExecutor(
          r -> {
            Thread t = new Thread(r, "UserInterfaceThread");
            t.setDaemon(true);
            UserInterfaceThread.thread = t;
            return t;
          });

  private UserInterfaceThread() {}

  static boolean isCurrent() {
    return Thread.currentThread() == UserInterfaceThread.thread;
  }

  static void execute(final Runnable runnable) {
    UserInterfaceThread.EXECUTOR.execute(
        () -> {
          // Like Swing's event thread, keep running after a task fails
          try {
            runnable.run();
          } catch (Throwable e) {
            StaticEntity.printStackTrace(e);
          }
        });
  }
}
