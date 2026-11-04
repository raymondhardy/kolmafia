package net.sourceforge.kolmafia.swingui;

import static net.sourceforge.kolmafia.swingui.KoLGUIConstants.FLATMAP_DARK_LOOKS;
import static net.sourceforge.kolmafia.swingui.KoLGUIConstants.FLATMAP_LIGHT_LOOKS;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.awt.Frame;
import java.awt.SystemTray;
import java.awt.Taskbar;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import net.java.dev.spellcast.utilities.ActionPanel;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.LockableListModel.ListElementFilter;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.RequestEditorKit;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.swingui.listener.LicenseDisplayListener;
import net.sourceforge.kolmafia.swingui.panel.ActionPanelRegistry;
import net.sourceforge.kolmafia.swingui.panel.GearChangePanel;
import net.sourceforge.kolmafia.swingui.panel.GenericPanel;
import net.sourceforge.kolmafia.swingui.widget.AutoFilterTextField;
import net.sourceforge.kolmafia.swingui.widget.GenericScrollPane;
import net.sourceforge.kolmafia.swingui.widget.GraphicsUtilities;
import net.sourceforge.kolmafia.swingui.widget.InterruptableDialog;
import net.sourceforge.kolmafia.swingui.widget.ListCellRendererFactory;
import net.sourceforge.kolmafia.swingui.widget.SwingInputFieldUtilities;
import net.sourceforge.kolmafia.ui.UserInterface;
import net.sourceforge.kolmafia.utilities.StringUtilities;

/** The desktop user interface: asks with Swing dialogs and updates the Swing frames. */
public class SwingUserInterface implements UserInterface {
  private static TabbedChatFrame tabbedFrame = null;

  private static final Map<LockableListModel<?>, ContactListFrame> contactFrames =
      new IdentityHashMap<>();

  @Override
  public void alert(final String message) {
    if (StaticEntity.isHeadless()) {
      UserInterface.super.alert(message);
      return;
    }

    JOptionPane.showMessageDialog(
        SwingInputFieldUtilities.getActiveWindow(), StringUtilities.basicTextWrap(message));
  }

  @Override
  public boolean confirm(final String message) {
    if (StaticEntity.isHeadless()) {
      return UserInterface.super.confirm(message);
    }

    return JOptionPane.YES_OPTION
        == JOptionPane.showConfirmDialog(
            SwingInputFieldUtilities.getActiveWindow(),
            StringUtilities.basicTextWrap(message),
            "",
            JOptionPane.YES_NO_OPTION);
  }

  @Override
  public String input(final String message) {
    if (StaticEntity.isHeadless()) {
      return UserInterface.super.input(message);
    }

    return JOptionPane.showInputDialog(
        SwingInputFieldUtilities.getActiveWindow(), StringUtilities.basicTextWrap(message));
  }

  @Override
  public String input(final String message, final String initial) {
    if (StaticEntity.isHeadless()) {
      return UserInterface.super.input(message, initial);
    }

    return JOptionPane.showInputDialog(
        SwingInputFieldUtilities.getActiveWindow(),
        StringUtilities.basicTextWrap(message),
        initial);
  }

  @Override
  public <T> T input(
      final String message,
      final LockableListModel<T> inputs,
      BiFunction<Object, Boolean, String> toHTMLFunction,
      final T initial) {
    if (StaticEntity.isHeadless()) {
      return UserInterface.super.input(message, inputs, toHTMLFunction, initial);
    }

    JList<T> selector = new JList<>(inputs);

    if (toHTMLFunction != null) {
      selector.setCellRenderer(ListCellRendererFactory.getDefaultRenderer(toHTMLFunction));
    }

    JPanel panel = new JPanel(new BorderLayout());
    panel.add(new AutoFilterTextField<>(selector, initial, true), BorderLayout.NORTH);
    panel.add(new GenericScrollPane(selector), BorderLayout.CENTER);

    int option =
        JOptionPane.showConfirmDialog(
            SwingInputFieldUtilities.getActiveWindow(),
            panel,
            StringUtilities.basicTextWrap(message),
            JOptionPane.OK_CANCEL_OPTION);

    return option == JOptionPane.CANCEL_OPTION || option == JOptionPane.CLOSED_OPTION
        ? null
        : selector.getSelectedValue();
  }

  @Override
  public <T> T input(final String message, final T[] inputs, final T initial) {
    return (T)
        JOptionPane.showInputDialog(
            SwingInputFieldUtilities.getActiveWindow(),
            StringUtilities.basicTextWrap(message),
            "",
            JOptionPane.INFORMATION_MESSAGE,
            null,
            inputs,
            initial);
  }

  @Override
  public <T> List<T> multiple(
      final String message, final LockableListModel<T> inputs, final ListElementFilter filter) {
    if (StaticEntity.isHeadless()) {
      return UserInterface.super.multiple(message, inputs, filter);
    }

    JList<T> selector = new JList<>(inputs);
    selector.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

    JPanel panel = new JPanel(new BorderLayout());
    panel.add(
        filter == null
            ? new AutoFilterTextField<>(selector)
            : new AutoFilterTextField<>(selector) {
              @Override
              public boolean isVisible(Object o) {
                return filter.isVisible(o) && super.isVisible(o);
              }
            },
        BorderLayout.NORTH);
    inputs.updateFilter(false);
    panel.add(new GenericScrollPane(selector), BorderLayout.CENTER);

    int option =
        JOptionPane.showConfirmDialog(
            SwingInputFieldUtilities.getActiveWindow(),
            panel,
            StringUtilities.basicTextWrap(message),
            JOptionPane.OK_CANCEL_OPTION);

    return option == JOptionPane.CANCEL_OPTION || option == JOptionPane.CLOSED_OPTION
        ? new ArrayList<>()
        : selector.getSelectedValuesList();
  }

  @Override
  public boolean hasOpenWindows() {
    return GenericFrame.instanceExists();
  }

  @Override
  public void showWindow(final String frameName) {
    KoLmafiaGUI.constructFrame(frameName);
  }

  @Override
  public void showMainWindows() {
    KoLmafiaGUI.intializeMainInterfaces();
  }

  @Override
  public void showLoginWindow() {
    KoLmafiaGUI.initializeLoginInterface();
  }

  @Override
  public void showRequest(final GenericRequest request) {
    RequestSynchFrame.showRequest(request);
  }

  @Override
  public void showDescription(final GenericRequest request) {
    DescriptionFrame.showRequest(request);
  }

  @Override
  public void showHTML(final GenericRequest request) {
    ShowHTMLFrame.showRequest(request);
  }

  @Override
  public boolean isDarkTheme() {
    return KoLmafiaGUI.isDarkTheme();
  }

  @Override
  public void showBalloon(final String message, final boolean onlyShowWhenHidden) {
    SystemTrayFrame.showBalloon(message, onlyShowWhenHidden);
  }

  @Override
  public void meatChanged() {
    MallSearchFrame.updateMeat();
  }

  @Override
  public void familiarsChanged() {
    GearChangePanel.updateFamiliars();
  }

  @Override
  public void familiarListCleared() {
    GearChangePanel.clearFamiliarList();
  }

  @Override
  public void skillsChanged() {
    SkillBuffFrame.update();
  }

  @Override
  public void safetyDetailsChanged() {
    AdventureFrame.updateSafetyDetails();
  }

  @Override
  public void selectedAdventureChanged(final KoLAdventure location) {
    AdventureFrame.updateSelectedAdventure(location);
  }

  @Override
  public void preferencesChanged() {
    AdventureFrame.updateFromPreferences();
  }

  @Override
  public void charPaneRefreshed() {
    RequestFrame.refreshStatus();
  }

  @Override
  public void pullsRemainingChanged(final int pullsRemaining) {
    ItemManageFrame.updatePullsRemaining(pullsRemaining);
  }

  @Override
  public void pullsBudgetedChanged(final int pullsBudgeted) {
    ItemManageFrame.updatePullsBudgeted(pullsBudgeted);
  }

  @Override
  public void storeEditingCancelled() {
    StoreManageFrame.cancelTableEditing();
  }

  @Override
  public void storeEarningsChanged(final long potentialEarnings) {
    StoreManageFrame.updateEarnings(potentialEarnings);
  }

  @Override
  public void equipmentListsCleared() {
    GearChangePanel.clearEquipmentModels();
  }

  @Override
  public void equipmentSlotChanged(final Slot slot) {
    GearChangePanel.updateSlot(slot);
  }

  @Override
  public void stickersChanged(final int st1, final int st2, final int st3) {
    GearChangePanel.updateStickers(st1, st2, st3);
  }

  @Override
  public void deferEquipmentUpdates() {
    GearChangePanel.deferUpdate();
  }

  @Override
  public void resolveDeferredEquipmentUpdates() {
    GearChangePanel.resolveDeferredUpdate();
  }

  @Override
  public boolean confirm(final String message, final long timeOut, final boolean defaultValue) {
    if (StaticEntity.isHeadless()) {
      return UserInterface.super.confirm(message, timeOut, defaultValue);
    }

    return InterruptableDialog.confirm(message, timeOut, defaultValue);
  }

  @Override
  public String input(final String message, final long timeOut, final String defaultValue) {
    if (StaticEntity.isHeadless()) {
      return UserInterface.super.input(message, timeOut, defaultValue);
    }

    return InterruptableDialog.input(message, timeOut, defaultValue);
  }

  @Override
  public boolean isUiThread() {
    return SwingUtilities.isEventDispatchThread();
  }

  @Override
  public void invokeLater(final Runnable runnable) {
    SwingUtilities.invokeLater(runnable);
  }

  @Override
  public void closeSessionWindows() {
    // Only Swing's Event Dispatch Thread can change the GUI
    if (SwingUtilities.isEventDispatchThread()) {
      // If we are within that thread, do it
      SwingUserInterface.resetGUI();
    } else {
      // Otherwise, make a Runnable to do it and hand it to Swing
      ResetGUIRunnable resetGUIRunnable = new ResetGUIRunnable();
      try {
        SwingUtilities.invokeAndWait(resetGUIRunnable);
      } catch (Exception e) {
      }
    }
  }

  private static class ResetGUIRunnable implements Runnable {
    @Override
    public void run() {
      SwingUserInterface.resetGUI();
    }
  }

  private static void resetGUI() {
    if (StaticEntity.isGUIRequired()) {
      KoLmafiaGUI.constructFrame(LoginFrame.class);
    }

    // Shut down main frame

    if (KoLDesktop.instanceExists()) {
      KoLDesktop.getInstance().dispose();
    }

    // Close down any other active frames.	Since
    // there is at least one active, logout will
    // not be called again.

    Frame[] frames = Frame.getFrames();

    for (Frame frame : frames) {
      if (frame.getClass() != LoginFrame.class) {
        frame.dispose();
      }
    }
  }

  @Override
  public void debugLogOpened(final File file) {
    if (KoLDesktop.instanceExists()) {
      SwingUtilities.invokeLater(
          () ->
              KoLDesktop.getInstance()
                  .getRootPane()
                  .putClientProperty("Window.documentFile", file));
    }
  }

  @Override
  public void beep() {
    Toolkit.getDefaultToolkit().beep();
  }

  @Override
  public void highlightChatTab(final String bufferKey) {
    if (SwingUserInterface.tabbedFrame != null) {
      SwingUserInterface.tabbedFrame.highlightTab(bufferKey);
    }
  }

  @Override
  public void openChatWindow(final String bufferKey, final boolean highlightOnOpen) {
    if (Preferences.getBoolean("useTabbedChatFrame")) {
      if (SwingUserInterface.tabbedFrame == null) {
        CreateFrameRunnable creator = new CreateFrameRunnable(TabbedChatFrame.class);
        boolean appearsInTab = GenericFrame.appearsInTab("ChatManager");
        SwingUserInterface.tabbedFrame = (TabbedChatFrame) creator.createFrame(appearsInTab);
      }

      SwingUserInterface.tabbedFrame.addTab(bufferKey);

      if (highlightOnOpen) {
        SwingUserInterface.tabbedFrame.highlightTab(bufferKey);
      }

      return;
    }

    ChatFrame frame = new ChatFrame(bufferKey);
    CreateFrameRunnable.decorate(frame);
    frame.setVisible(true);
  }

  @Override
  public void chatClosed() {
    SwingUserInterface.tabbedFrame = null;
  }

  @Override
  public void showContacts(final String title, final LockableListModel<?> contacts) {
    ContactListFrame frame =
        SwingUserInterface.contactFrames.computeIfAbsent(contacts, ContactListFrame::new);

    frame.setTitle(title);
    frame.setVisible(true);
  }

  @Override
  public void maximizerExpressionChanged(final String expression) {
    MaximizerFrame.expressionSelect.setSelectedItem(expression);
  }

  @Override
  public void showPlayerProfile(final String player) {
    Object[] parameters = new Object[] {player};
    SwingUtilities.invokeLater(new CreateFrameRunnable(ProfileFrame.class, parameters));
  }

  @Override
  public boolean confirmWarning(final String title, final String message) {
    return JOptionPane.showConfirmDialog(
            null, message, title, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE)
        == JOptionPane.YES_OPTION;
  }

  @Override
  public void dumpDisabledSkills() {
    Frame frame = SwingUserInterface.findFrame(SkillBuffFrame.class);
    if (frame != null) {
      SkillBuffFrame sbf = (SkillBuffFrame) frame;
      sbf.dumpDisabledSkills();
    }
  }

  private static Frame findFrame(final Class<?> type) {
    for (Frame frame : Frame.getFrames()) {
      if (frame.getClass() == type) {
        return frame;
      }
    }
    return null;
  }

  @Override
  public boolean canBrowse() {
    return Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Action.BROWSE);
  }

  @Override
  public void browse(final URI uri) throws IOException {
    Desktop.getDesktop().browse(uri);
  }

  @Override
  public String getLookAndFeelColor(final String key) {
    Color color = UIManager.getColor(key);

    if (color == null) {
      return null;
    }

    return "#" + Integer.toHexString(color.getRGB()).substring(2);
  }

  @Override
  public void mergeImages(final List<String> paths, final String outputPath) {
    var images = GraphicsUtilities.readImages(paths);
    var generated = GraphicsUtilities.mergeImages(images);
    GraphicsUtilities.writeImage(generated, outputPath);
  }

  @Override
  public void setIconBadge(final String text) {
    if (Taskbar.isTaskbarSupported()) {
      Taskbar taskbar = Taskbar.getTaskbar();
      if (taskbar.isSupported(Taskbar.Feature.ICON_BADGE_TEXT)
          || taskbar.isSupported(Taskbar.Feature.ICON_BADGE_NUMBER)) {
        taskbar.setIconBadge(text);
      }
    }
  }

  @Override
  public void installEditorKits() {
    JEditorPane.registerEditorKitForContentType("text/html", RequestEditorKit.class.getName());
  }

  @Override
  public void installLookAndFeel() {
    // Change the default look and feel to match the player's
    // preferences. Always do this.

    String defaultLookAndFeel;

    // Tell UIManager about Look and Feel files in external jars ( defined in KoLGUIConstants)
    installLookAndFeel(FLATMAP_LIGHT_LOOKS);
    installLookAndFeel(FLATMAP_DARK_LOOKS);

    if (System.getProperty("os.name").startsWith("Mac")
        || System.getProperty("os.name").startsWith("Win")) {
      defaultLookAndFeel = UIManager.getSystemLookAndFeelClassName();
    } else {
      defaultLookAndFeel = UIManager.getCrossPlatformLookAndFeelClassName();
    }

    String lookAndFeel = Preferences.getString("swingLookAndFeel");

    if (lookAndFeel.isEmpty()) {
      lookAndFeel = defaultLookAndFeel;
    }

    UIManager.LookAndFeelInfo[] installed = UIManager.getInstalledLookAndFeels();
    String[] installedLooks = new String[installed.length];

    for (int i = 0; i < installedLooks.length; ++i) {
      installedLooks[i] = installed[i].getClassName();
    }

    boolean foundLookAndFeel = false;
    for (int i = 0; i < installedLooks.length && !foundLookAndFeel; ++i) {
      foundLookAndFeel = installedLooks[i].equals(lookAndFeel);
    }

    if (!foundLookAndFeel) {
      lookAndFeel = defaultLookAndFeel;
    }

    try {
      UIManager.setLookAndFeel(lookAndFeel);
      KoLmafiaGUI.applyFlatLafMenuBarSettings();
      JFrame.setDefaultLookAndFeelDecorated(System.getProperty("os.name").startsWith("Mac"));
    } catch (Exception e) {
      // Should not happen, as we checked to see if
      // the look and feel was installed first.

      JFrame.setDefaultLookAndFeelDecorated(true);
    }

    if (StaticEntity.usesSystemTray()) {
      SystemTrayFrame.addTrayIcon();
    }

    boolean isWindows = System.getProperty("os.name").startsWith("Win");

    if (Taskbar.isTaskbarSupported()) {
      Taskbar taskbar = Taskbar.getTaskbar();
      if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
        String image = isWindows ? "app_icon256.png" : "limeglass.gif";
        taskbar.setIconImage(JComponentUtilities.getImage(image).getImage());
      }
    }

    if (isWindows || lookAndFeel.equals(UIManager.getCrossPlatformLookAndFeelClassName())) {
      UIManager.put("ProgressBar.foreground", Color.black);
      UIManager.put("ProgressBar.selectionForeground", Color.lightGray);

      UIManager.put("ProgressBar.background", Color.lightGray);
      UIManager.put("ProgressBar.selectionBackground", Color.black);
    }
  }

  private static void installLookAndFeel(Map<String, String> looks) {
    for (Entry<String, String> entry : looks.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();
      try {
        // try to load the class to confirm it exists
        Class.forName(value);
      } catch (ClassNotFoundException e) {
        RequestLogger.printLine("Failed to load class " + value + " for Theme " + key);
        continue;
      }
      UIManager.installLookAndFeel(key, value);
    }
  }

  @Override
  public void initializeGUI() {
    KoLmafiaGUI.initialize();
  }

  @Override
  public void updateToolTip(final String message) {
    SystemTrayFrame.updateToolTip(message);
  }

  @Override
  public void updateDisplayState(final MafiaState state, final String unicodeMessage) {
    ActionPanel[] panels = ActionPanelRegistry.getExistingPanels();

    for (ActionPanel panel : panels) {
      if (panel instanceof GenericPanel) {
        ((GenericPanel) panel).setStatusMessage(unicodeMessage);
      }

      panel.setEnabled(state != MafiaState.CONTINUE);
    }

    Arrays.stream(Frame.getFrames())
        .filter(GenericFrame.class::isInstance)
        .map(GenericFrame.class::cast)
        .forEach(
            f -> {
              f.setStatusMessage(unicodeMessage);
              f.updateDisplayState(state);
            });

    if (KoLDesktop.instanceExists()) {
      KoLDesktop.getInstance().updateDisplayState(state);
    }
  }

  @Override
  public void requestProgress(final int value, final int maximum) {
    AdventureFrame.updateRequestMeter(value, maximum);
  }

  @Override
  public void validateEquipmentSelections() {
    GearChangePanel.validateSelections();
  }

  @Override
  public void showLicenses() {
    new LicenseDisplayListener().run();
  }

  @Override
  public void removeTrayIcon() {
    SystemTrayFrame.removeTrayIcon();
  }

  @Override
  public boolean isSystemTraySupported() {
    return SystemTray.isSupported();
  }
}
