package net.sourceforge.kolmafia.ui;

import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/**
 * Holds the user interface the engine talks to.
 *
 * <p>Unless one has been set, the first user interface found by {@link ServiceLoader} is used. When
 * none can be loaded, the engine uses the defaults in {@link UserInterface}.
 */
public class UserInterfaceManager {
  private static final UserInterface DEFAULT = new UserInterface() {};

  private static volatile UserInterface userInterface = null;

  private UserInterfaceManager() {}

  public static UserInterface getUserInterface() {
    UserInterface current = UserInterfaceManager.userInterface;
    if (current == null) {
      synchronized (UserInterfaceManager.class) {
        current = UserInterfaceManager.userInterface;
        if (current == null) {
          current = UserInterfaceManager.findUserInterface();
          UserInterfaceManager.userInterface = current;
        }
      }
    }
    return current;
  }

  public static void setUserInterface(final UserInterface userInterface) {
    UserInterfaceManager.userInterface = userInterface;
  }

  private static UserInterface findUserInterface() {
    try {
      return ServiceLoader.load(UserInterface.class)
          .findFirst()
          .orElse(UserInterfaceManager.DEFAULT);
    } catch (ServiceConfigurationError | LinkageError e) {
      // A user interface is listed but cannot run here, for example without Swing
      return UserInterfaceManager.DEFAULT;
    }
  }
}
