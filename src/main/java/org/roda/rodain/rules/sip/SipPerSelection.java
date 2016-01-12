package org.roda.rodain.rules.sip;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.roda.rodain.rules.MetadataTypes;
import org.roda.rodain.rules.TreeNode;
import org.roda.rodain.rules.filters.ContentFilter;
import org.roda.rodain.utils.TreeVisitor;

/**
 * @author Andre Pereira apereira@keep.pt
 * @since 10-11-2015.
 */
public class SipPerSelection extends Observable implements TreeVisitor, SipPreviewCreator {
  private static final int UPDATEFREQUENCY = 500; // in milliseconds
  private long lastUIUpdate = 0;
  // This map is returned, in full, to the SipPreviewNode when there's an update
  private Map<String, SipPreview> sipsMap;
  // This ArrayList is used to keep the SIPs ordered.
  // We need them ordered because we have to keep track of which SIPs have
  // already been loaded
  private List<SipPreview> sips;
  private int added = 0, returned = 0;
  private Deque<TreeNode> nodes;

  private String id;
  private Set<String> selectedPaths;
  private Set<ContentFilter> filters;
  private MetadataTypes metaType;
  private Path metadataPath;
  private String templateType;

  private boolean cancelled = false;

  /**
   * Creates a new SipPreviewCreator where there's a new SIP created for each selected path.
   *
   * @param id            The id of the SipPreviewCreator.
   * @param selectedPaths The set of selected paths for the SIPs creation
   * @param filters       The set of content filters
   * @param metaType      The type of metadata to be applied to each SIP
   * @param metadataPath  The path of the metadata
   * @param templateType  The type of the metadata template
   */
  public SipPerSelection(String id, Set<String> selectedPaths, Set<ContentFilter> filters, MetadataTypes metaType,
    Path metadataPath, String templateType) {
    this.selectedPaths = selectedPaths;
    this.filters = filters;
    this.metaType = metaType;
    this.metadataPath = metadataPath;
    this.templateType = templateType;
    sips = new ArrayList<>();
    sipsMap = new HashMap<>();
    nodes = new ArrayDeque<>();
    this.id = id;
  }

  /**
   * @return The Map with the SIPs created by the SipPreviewCreator.
   */
  @Override
  public Map<String, SipPreview> getSips() {
    return sipsMap;
  }

  /**
   * @return The count of the SIPs already created.
   */
  @Override
  public int getCount() {
    return added;
  }

  /**
   * The object keeps a list with the created SIPs and this method returns them
   * one at a time.
   *
   * @return The next SIP in the list.
   */
  @Override
  public SipPreview getNext() {
    return sips.get(returned++);
  }

  /**
   * @return True if the number of SIPs returned is smaller than the count of
   * added SIPs.
   */
  @Override
  public boolean hasNext() {
    return returned < added;
  }

  private boolean filter(Path path) {
    String pathString = path.toString();
    for (ContentFilter cf : filters) {
      if (cf.filter(pathString))
        return true;
    }
    Pattern p = Pattern.compile("metadata\\..*");
    Matcher m = p.matcher(path.getFileName().toString());
    return m.matches();
  }

  /**
   * Sets the starting path of this TreeVisitor. This method is empty in this
   * class, but it's defined because of the SipPreviewCreator interface.
   *
   * @param st The starting path of the TreeVisitor.
   */
  @Override
  public void setStartPath(String st) {

  }

  /**
   * Creates a new TreeNode and adds it to the nodes Deque if the path isn't
   * mapped or ignored.
   *
   * @param path  The path of the directory.
   * @param attrs The attributes of the directory.
   */
  @Override
  public void preVisitDirectory(Path path, BasicFileAttributes attrs) {
    if (filter(path) || cancelled)
      return;
    TreeNode newNode = new TreeNode(path);
    nodes.add(newNode);
  }

  /**
   * Adds the current directory to its parent's node. Creates a SIP if this path
   * is in the selected set of paths.
   *
   * @param path The path of the directory.
   */
  @Override
  public void postVisitDirectory(Path path) {
    if (filter(path) || cancelled)
      return;

    // pop the node of this directory and add it to its parent (if it exists)
    TreeNode node = nodes.removeLast();
    if (!nodes.isEmpty())
      nodes.peekLast().add(node);

    // Check if we create a new SIP using this node
    if (selectedPaths.contains(path.toString())) {
      createSip(path, node);
    }

    long now = System.currentTimeMillis();
    if (now - lastUIUpdate > UPDATEFREQUENCY) {
      setChanged();
      notifyObservers();
      lastUIUpdate = now;
    }
  }

  private void createSip(Path path, TreeNode node) {
    Path metaPath = getMetadataPath(path);
    // create a new Sip
    Set<TreeNode> files = new HashSet<>();
    files.add(node);

    SipPreview sipPreview = new SipPreview(path.getFileName().toString(), files, metaType, metaPath, templateType);
    node.addObserver(sipPreview);

    sips.add(sipPreview);
    sipsMap.put(sipPreview.getId(), sipPreview);
    added++;
  }

  /**
   * If the path is in the selected set of paths creates a new SIP using the
   * file, otherwise, adds the visited file to its parent.
   *
   * @param path  The path of the visited file
   * @param attrs The attributes of the visited file
   */
  @Override
  public void visitFile(Path path, BasicFileAttributes attrs) {
    if (filter(path) || cancelled)
      return;
    if (selectedPaths.contains(path.toString())) {
      createSip(path, new TreeNode(path));
    } else {
      if (nodes.isEmpty()) {
        nodes.add(new TreeNode(path.getParent()));
      }
      nodes.peekLast().add(path);
    }
  }

  /**
   * This method is empty in this class, but it's defined because of the
   * TreeVisitor interface.
   *
   * @param path The path of the file.
   */
  @Override
  public void visitFileFailed(Path path) {
  }

  private Path getMetadataPath(Path sipPath) {
    Path result = null;
    if (metaType == MetadataTypes.SINGLE_FILE) {
      result = metadataPath;
    } else if (metaType == MetadataTypes.SAME_DIRECTORY) {
      result = searchMetadata(sipPath);
    }
    return result;
  }

  private Path searchMetadata(Path sipPath) {
    File dir = new File(sipPath.toString());
    File[] foundFiles = dir.listFiles(new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return name.startsWith("metadata.");
      }
    });

    if (foundFiles.length > 0) {
      return foundFiles[0].toPath();
    }
    return null;
  }

  /**
   * Ends the tree visit, notifying the observers of modifications.
   */
  @Override
  public void end() {
    setChanged();
    notifyObservers();
  }

  /**
   * @return The id of the visitor.
   */
  @Override
  public String getId() {
    return id;
  }

  /**
   * Cancels the execution of the SipPreviewCreator
   */
  @Override
  public void cancel() {
    cancelled = true;
  }
}
