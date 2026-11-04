package net.sourceforge.kolmafia.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.LockableListModel.ListElementFilter;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;

/**
 * What the engine needs from whichever user interface is running: asking the user something,
 * showing things, and hearing that something changed. See devdoc/engine-ui-separation.md.
 *
 * <p>Every method has a default that works without a display: prompts are asked on the console, the
 * way KoLmafia has always asked them when running headless, and everything else does nothing. A
 * user interface overrides only what it supports.
 */
public interface UserInterface {
  default void alert(final String message) {
    RequestLogger.printLine(message);
    KoLmafiaCLI.DEFAULT_SHELL.getNextLine("Press enter to continue...");
  }

  default boolean confirm(final String message) {
    RequestLogger.printLine(message);
    RequestLogger.printLine("(Y/N, leave blank to choose N)");

    String reply = KoLmafiaCLI.DEFAULT_SHELL.getNextLine(" > ");

    return reply.equalsIgnoreCase("y");
  }

  default String input(final String message) {
    RequestLogger.printLine(message);

    String reply = KoLmafiaCLI.DEFAULT_SHELL.getNextLine(" > ");

    return reply;
  }

  default String input(final String message, final String initial) {
    RequestLogger.printLine(message);
    RequestLogger.printLine("(leave blank to use " + initial + ")");

    String reply =
        KoLmafiaCLI.DEFAULT_SHELL.getNextLine(
            message + "\n(Leave blank to use " + initial + ")\n > ");

    if (reply.equals("")) {
      reply = initial;
    }

    return reply;
  }

  default <T> T input(
      final String message,
      final LockableListModel<T> inputs,
      BiFunction<Object, Boolean, String> toHTMLFunction,
      final T initial) {
    int initialIndex = 0;
    RequestLogger.printLine(message);

    for (int i = 0; i < inputs.size(); ++i) {
      T o = inputs.get(i);
      RequestLogger.printLine("  " + (i + 1) + ": " + o);

      if (initial != null && initial.equals(o)) {
        initialIndex = i;
      }
    }

    RequestLogger.printLine(
        "(Enter a number from 1 to "
            + inputs.size()
            + ", leave blank to use "
            + (initialIndex + 1)
            + ")");

    String reply = KoLmafiaCLI.DEFAULT_SHELL.getNextLine(" > ");

    if (reply.equals("")) {
      return initial;
    }

    int selectedIndex = StringUtilities.parseInt(reply) - 1;

    if (selectedIndex > -1 && selectedIndex < inputs.size()) {
      return inputs.get(selectedIndex);
    }

    return null;
  }

  /** Chooses from a short list of values (booleans, stats, classes, ...). */
  default <T> T input(final String message, final T[] inputs, final T initial) {
    return this.input(message, new LockableListModel<>(Arrays.asList(inputs)), null, initial);
  }

  /** Asks yes or no, giving up after timeOut milliseconds. */
  default boolean confirm(final String message, final long timeOut, final boolean defaultValue) {
    // this doesn't support headless operation yet, sorry
    RequestLogger.printLine(message);
    RequestLogger.printLine("(Y/N, leave blank to choose N)");

    String reply = KoLmafiaCLI.DEFAULT_SHELL.getNextLine(" > ");

    return reply.equalsIgnoreCase("y");
  }

  /** Asks for text, giving up after timeOut milliseconds. */
  default String input(final String message, final long timeOut, final String defaultValue) {
    // this doesn't support headless operation yet, sorry
    RequestLogger.printLine(message);
    RequestLogger.printLine("(type your response)");

    String reply = KoLmafiaCLI.DEFAULT_SHELL.getNextLine(" > ");

    return reply.toString();
  }

  default <T> List<T> multiple(
      final String message, final LockableListModel<T> inputs, final ListElementFilter filter) {
    RequestLogger.printLine(message);
    List<T> visibleInputs = new ArrayList<>();

    for (T o : inputs) {
      if (filter.isVisible(o)) {
        visibleInputs.add(o);
        RequestLogger.printLine("  " + (visibleInputs.size()) + ": " + o);
      }
    }

    RequestLogger.printLine(
        "(Enter a comma-delimited list of values from 1 to " + visibleInputs.size() + ")");

    String reply = KoLmafiaCLI.DEFAULT_SHELL.getNextLine(" > ");

    String[] replyList = StringUtilities.splitByComma(reply);

    Set<T> selectedValues = new HashSet<>();

    for (int i = 0; i < replyList.length; ++i) {
      int selectedIndex = StringUtilities.parseInt(replyList[i]) - 1;

      if (selectedIndex > -1 && selectedIndex < visibleInputs.size()) {
        selectedValues.add(visibleInputs.get(i));
      }
    }

    return new ArrayList<>(selectedValues);
  }

  // Windows. Frame names are the ids in KoLConstants.FRAME_NAMES, as used by the window command.

  /** Whether any window is open, i.e. whether the user is looking at a graphical interface. */
  default boolean hasOpenWindows() {
    return false;
  }

  default void showWindow(final String frameName) {}

  default void showMainWindows() {}

  default void showLoginWindow() {}

  default void showRequest(final GenericRequest request) {}

  default void showDescription(final GenericRequest request) {}

  default void showHTML(final GenericRequest request) {}

  default boolean isDarkTheme() {
    return false;
  }

  default void showBalloon(final String message, final boolean onlyShowWhenHidden) {}

  // Notifications: the engine's state changed and anything showing it should refresh.

  default void meatChanged() {}

  default void familiarsChanged() {}

  default void familiarListCleared() {}

  default void skillsChanged() {}

  default void safetyDetailsChanged() {}

  default void selectedAdventureChanged(final KoLAdventure location) {}

  default void preferencesChanged() {}

  default void charPaneRefreshed() {}

  default void pullsRemainingChanged(final int pullsRemaining) {}

  default void pullsBudgetedChanged(final int pullsBudgeted) {}

  default void storeEditingCancelled() {}

  default void storeEarningsChanged(final long potentialEarnings) {}

  default void equipmentListsCleared() {}

  default void equipmentSlotChanged(final Slot slot) {}

  default void stickersChanged(final int st1, final int st2, final int st3) {}

  default void deferEquipmentUpdates() {}

  default void resolveDeferredEquipmentUpdates() {}

  // Threads

  /** Whether this is the thread that updates the user interface. */
  default boolean isUiThread() {
    return false;
  }

  // Session

  /** The user is logging out: close the windows that belong to the session. */
  default void closeSessionWindows() {}

  default void debugLogOpened(final File file) {}

  // Chat

  default void beep() {
    // Send alert character
    System.out.write(0x07);
  }

  default void openChatWindow(final String bufferKey, final boolean highlightOnOpen) {}

  default void highlightChatTab(final String bufferKey) {}

  default void chatClosed() {}

  default void showContacts(final String title, final LockableListModel<?> contacts) {}
}
