/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2013-19 The Processing Foundation

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package processing.app.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import processing.app.Base;
import processing.app.Language;
import processing.app.Library;
import processing.app.Mode;
import processing.app.Platform;
import processing.app.Preferences;
import processing.app.SketchReference;
import processing.app.contrib.Contribution;
import processing.app.contrib.ContributionManager;
import processing.app.contrib.ContributionType;
import processing.app.contrib.ExamplesContribution;

import processing.core.PApplet;
import processing.data.StringDict;


public class ExamplesFrame extends JFrame {
  protected Base base;
  protected Mode mode;

  protected File examplesContribFolder;


  public ExamplesFrame(final Base base, final Mode mode) {
    super(Language.interpolate("examples.title", mode.getTitle()));
    this.base = base;
    this.mode = mode;

    // Get path to the contributed examples compatible with this mode
    examplesContribFolder = Base.getSketchbookExamplesFolder();

    Toolkit.setIcon(this);
    Toolkit.registerWindowCloseKeys(getRootPane(), e -> setVisible(false));

    JPanel examplesPanel = new JPanel();
    examplesPanel.setLayout(new BorderLayout());
    examplesPanel.setBackground(Color.WHITE);

    final JTree tree = new JTree(buildTree());

    tree.setOpaque(true);
    tree.setAlignmentX(Component.LEFT_ALIGNMENT);

    tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    tree.setShowsRootHandles(true);
    // expand the root
    tree.expandRow(0);
    // now hide the root
    tree.setRootVisible(false);

    // After 2.0a7, no longer expanding each of the categories at Casey's
    // request. He felt that the window was too complicated, too quickly.
//      for (int row = tree.getRowCount()-1; row >= 0; --row) {
//        tree.expandRow(row);
//      }

    tree.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
          DefaultMutableTreeNode node =
            (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();

          int selRow = tree.getRowForLocation(e.getX(), e.getY());
          //TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
          //if (node != null && node.isLeaf() && node.getPath().equals(selPath)) {
          if (node != null && node.isLeaf() && selRow != -1) {
            SketchReference sketch = (SketchReference) node.getUserObject();
            base.handleOpenExample(sketch.getPath(), mode);
          }
        }
      }
    });
    tree.addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {  // doesn't fire keyTyped()
          setVisible(false);
        }
      }
      public void keyTyped(KeyEvent e) {
        if (e.getKeyChar() == KeyEvent.VK_ENTER) {
          DefaultMutableTreeNode node =
            (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
          if (node != null && node.isLeaf()) {
            SketchReference sketch = (SketchReference) node.getUserObject();
            base.handleOpenExample(sketch.getPath(), mode);
          }
        }
      }
    });

    tree.addTreeExpansionListener(new TreeExpansionListener() {
      @Override
      public void treeExpanded(TreeExpansionEvent event) {
        updateExpanded(tree);
      }

      @Override
      public void treeCollapsed(TreeExpansionEvent event) {
        updateExpanded(tree);
      }
    });

    tree.setBorder(new EmptyBorder(0, 5, 5, 5));
    tree.setToggleClickCount(Platform.isMacOS() ? 2 : 1);

    // Special cell renderer that takes the Toolkit zoom setting into account
    tree.setCellRenderer(new ZoomTreeCellRenderer());

    JScrollPane treePane = new JScrollPane(tree);
    treePane.setPreferredSize(Toolkit.zoom(250, 300));
    treePane.setBorder(new EmptyBorder(Toolkit.zoom(2), 0, 0, 0));
    treePane.setOpaque(true);
    treePane.setBackground(Color.WHITE);
    treePane.setAlignmentX(Component.LEFT_ALIGNMENT);

    //examplesPanel.add(openExamplesManagerPanel,BorderLayout.PAGE_START);
    examplesPanel.add(treePane, BorderLayout.CENTER);

    Container buttons = Box.createHorizontalBox();
    JButton addButton = new JButton(Language.text("examples.add_examples"));
    addButton.addActionListener(e -> ContributionManager.openExamples());
    buttons.add(Box.createHorizontalGlue());
    buttons.add(addButton);
    buttons.add(Box.createHorizontalGlue());

    JPanel buttonPanel = new JPanel();  // adds extra border
    buttonPanel.add(buttons);
    examplesPanel.add(buttonPanel, BorderLayout.SOUTH);

    getContentPane().add(examplesPanel);
    pack();
    restoreExpanded(tree);
  }


  public void setVisible() {
    // Space for the editor plus a li'l gap
    int roughWidth = getWidth() + 20;
    // If no window open, or the editor is at the edge of the screen
    Editor editor = base.getActiveEditor();
    if (editor == null) {
      setLocationRelativeTo(null);
    } else {
      Point p = editor.getLocation();
      if (p.x < roughWidth) {
        // Center the window on the screen
        setLocationRelativeTo(null);
      } else {
        // Open the window relative to the editor
        setLocation(p.x - roughWidth, p.y);
      }
    }
    setVisible(true);
  }


  protected void updateExpanded(JTree tree) {
    Enumeration<TreePath> en =
      tree.getExpandedDescendants(new TreePath(tree.getModel().getRoot()));
    //en.nextElement();  // skip the root "Examples" node

    StringBuilder s = new StringBuilder();
    while (en.hasMoreElements()) {
      TreePath tp = en.nextElement();
      Object[] path = tp.getPath();
      for (Object o : path) {
        DefaultMutableTreeNode p = (DefaultMutableTreeNode) o;
        String name = (String) p.getUserObject();
        //System.out.print(p.getUserObject().getClass().getName() + ":" + p.getUserObject() + " -> ");
        //System.out.print(name + " -> ");
        s.append(name);
        s.append(File.separatorChar);
      }
      //System.out.println();
      s.setCharAt(s.length() - 1, File.pathSeparatorChar);
    }
    s.setLength(s.length() - 1);  // nix that last separator
    String pref = "examples." + getClass().getName() + ".visible";
    Preferences.set(pref, s.toString());
    Preferences.save();
//    System.out.println(s);
//    System.out.println();
  }


  protected void restoreExpanded(JTree tree) {
    String pref = "examples." + getClass().getName() + ".visible";
    String value = Preferences.get(pref);
    if (value != null) {
      String[] paths = PApplet.split(value, File.pathSeparator);
      for (String path : paths) {
//        System.out.println("trying to expand " + path);
        String[] items = PApplet.split(path, File.separator);
        DefaultMutableTreeNode[] nodes = new DefaultMutableTreeNode[items.length];
        expandTree(tree, null, items, nodes, 0);
      }
    }
  }


  void expandTree(JTree tree, Object object, String[] items,
                  DefaultMutableTreeNode[] nodes, int index) {
//    if (object == null) {
//      object = model.getRoot();
//    }
    TreeModel model = tree.getModel();

    if (index == 0) {
      nodes[0] = (DefaultMutableTreeNode) model.getRoot();
      expandTree(tree, nodes[0], items, nodes, 1);

    } else if (index < items.length) {
//    String item = items[0];
//    TreeModel model = object.getModel();
//    System.out.println(object.getClass().getName());
      DefaultMutableTreeNode node = (DefaultMutableTreeNode) object;
      int count = model.getChildCount(node);
//    System.out.println("child count is " + count);
      for (int i = 0; i < count; i++) {
        DefaultMutableTreeNode child = (DefaultMutableTreeNode) model.getChild(node, i);
        if (items[index].equals(child.getUserObject())) {
          nodes[index] = child;
          expandTree(tree, child, items, nodes, index+1);
        }
      }
    } else {  // last one
//      PApplet.println(nodes);
      tree.expandPath(new TreePath(nodes));
    }
  }


  protected DefaultMutableTreeNode buildTree() {
    DefaultMutableTreeNode root = new DefaultMutableTreeNode(); //"Examples");

    try {
      // Get the list of Mode-specific examples, in the order the Mode wants
      // to present them (i.e. Basics, then Topics, then Demos...)
      File[] examples = mode.getExampleCategoryFolders();

      for (File subFolder : examples) {
        DefaultMutableTreeNode subNode = new DefaultMutableTreeNode(subFolder.getName());
        if (base.addSketches(subNode, subFolder, true)) {
          root.add(subNode);
        }
      }

      DefaultMutableTreeNode foundationLibraries =
        new DefaultMutableTreeNode(Language.text("examples.core_libraries"));

      // Get examples for core libraries
      for (Library lib : mode.coreLibraries) {
        if (lib.hasExamples()) {
          DefaultMutableTreeNode libNode = new DefaultMutableTreeNode(lib.getName());
          if (base.addSketches(libNode, lib.getExamplesFolder(), true)) {
            foundationLibraries.add(libNode);
          }
        }
      }
      if (foundationLibraries.getChildCount() > 0) {
        root.add(foundationLibraries);
      }

      // Get examples for third party libraries
      DefaultMutableTreeNode contributedLibExamples = new DefaultMutableTreeNode(Language.text("examples.libraries"));
      var sortedContribLibs = new ArrayList<>(mode.contribLibraries);
      // Sort the libraries by actual name (not the name of the folder)
      sortedContribLibs.sort(Comparator.comparing(Contribution::getName));
      for (Library lib : sortedContribLibs) {
        if (lib.hasExamples()) {
            DefaultMutableTreeNode libNode =
              new DefaultMutableTreeNode(lib.getName());
            base.addSketches(libNode, lib.getExamplesFolder(), true);
          contributedLibExamples.add(libNode);
        }
      }
      if (contributedLibExamples.getChildCount() > 0) {
        root.add(contributedLibExamples);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    DefaultMutableTreeNode contributedExamplesNode = buildContribTree();
    if (contributedExamplesNode.getChildCount() > 0) {
      root.add(contributedExamplesNode);
    }

    return root;
  }


  protected DefaultMutableTreeNode buildContribTree() {
    DefaultMutableTreeNode contribExamplesNode =
      new DefaultMutableTreeNode(Language.text("examples.contributed"));

    try {
      File[] folders =
        ContributionType.EXAMPLES.listCandidates(examplesContribFolder);
      if (folders != null) {
        for (File sub : folders) {
          StringDict props =
            //Contribution.loadProperties(sub, ContributionType.EXAMPLES);
            ContributionType.EXAMPLES.loadProperties(sub);
          if (props != null) {
            if (ExamplesContribution.isModeCompatible(base, props)) {
              DefaultMutableTreeNode subNode =
                new DefaultMutableTreeNode(props.get("name"));
              if (base.addSketches(subNode, sub, true)) {
                contribExamplesNode.add(subNode);

                // TODO there has to be a simpler way of handling this
                //      along with addSketches() as well [fry 150811]
                int exampleNodeNumber = -1;
                // The contrib may have other items besides the examples folder
                for (int i = 0; i < subNode.getChildCount(); i++) {
                  if (subNode.getChildAt(i).toString().equals("examples")) {
                    exampleNodeNumber = i;
                  }
                }
                if (exampleNodeNumber != -1) {
                  TreeNode exampleNode = subNode.getChildAt(exampleNodeNumber);
                  subNode.remove(exampleNodeNumber);
                  int count = exampleNode.getChildCount();
                  for (int j = 0; j < count; j++) {
                    subNode.add((DefaultMutableTreeNode) exampleNode.getChildAt(0));
                  }
                }
              }
            }
          }
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return contribExamplesNode;
  }



}