package org.roda.rodain.inspection;

import java.io.IOException;

import javafx.event.EventHandler;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseEvent;

import org.slf4j.LoggerFactory;

/**
 * @author Andre Pereira apereira@keep.pt
 * @since 21-09-2015.
 */
public class ContentClickedEventHandler implements EventHandler<MouseEvent> {
  private static final org.slf4j.Logger log = LoggerFactory.getLogger(ContentClickedEventHandler.class.getName());
  private static String OS = System.getProperty("os.name").toLowerCase();
  private TreeView<Object> treeView;
  private InspectionPane ipane;

  public ContentClickedEventHandler(TreeView<Object> tree, InspectionPane pane) {
    this.treeView = tree;
    ipane = pane;
  }

  @Override
  public void handle(MouseEvent mouseEvent) {
    if (mouseEvent.getClickCount() == 1) {
      TreeItem<Object> item = treeView.getSelectionModel().getSelectedItem();
      if (item instanceof SipContentDirectory) {
        ipane.setStateContentButtons(false);
      } else if (item instanceof SipContentFile) {
        ipane.setStateContentButtons(true);
      }
    } else if (mouseEvent.getClickCount() == 2) {
      Object source = treeView.getSelectionModel().getSelectedItem();
      if (source instanceof SipContentFile) {
        SipContentFile sipFile = (SipContentFile) source;
        String command;
        // Different commands for different operating systems
        if (isWindows()) {
          command = "explorer";
        } else if (isMac()) {
          command = "open";
        } else if (isUnix()) {
          command = "gnome-open";
        } else {
          return;
        }
        // Execute the command
        try {
          ProcessBuilder pb = new ProcessBuilder(command, sipFile.getPath().toString());
          pb.start();
        } catch (IOException e) {
          log.info("Error opening file from SIP content", e);
        }
      }
    }
  }

  public static boolean isWindows() {
    return OS.contains("win");
  }

  public static boolean isMac() {
    return OS.contains("mac");
  }

  public static boolean isUnix() {
    return OS.contains("nix") || OS.contains("nux") || OS.contains("aix");
  }
}
