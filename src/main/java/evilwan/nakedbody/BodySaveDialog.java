/*
 * Copyright (c) 2019 Eddy Vanlerberghe.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of Eddy Vanlerberghe shall not be used to endorse or promote
 *    products derived from this software without specific prior written
 *    permission.
 *
 * THIS SOFTWARE IS PROVIDED BY EDDY VANLERBERGHE ''AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL EDDY VANLERBERGHE BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 */

package evilwan.nakedbody;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * This class implements the save messages dialog that is popped up when selecting
 * to save some naked bodies from the proxy history.
 */
public class BodySaveDialog extends JDialog implements ActionListener {
    /**
     * Parent frame for this dialog
     */
    private JFrame _dad;
    /**
     * Object to use for saving the message bodies
     */
    private BodySaver _mom;
    /**
     * Checkbox to tick for saving request bodies
     */
    private JCheckBox _save_req;
    /**
     * Checkbox to tick for saving response bodies
     */
    private JCheckBox _save_resp;
    /**
     * Text field for directory where to save the body files
     */
    private JTextField _directory_txt;
    /**
     * Actual output directory name
     */
    private String _outdir = "";
    /**
     * Encapsulation of standard output and standard error provided by Burp for its extensions
     */
    private StdioEncapsulation _stdio;

    /**
     * Build and show the extension save dialog
     * @param dad parent frame for this dialog
     * @param title for save bodies dialog
     * @param mom object to use for saving the message bodies
     * @param stdio encapsulation of standard output and standard error provided by Burp for its extensions
     */
    public BodySaveDialog(JFrame dad, String title, BodySaver mom, StdioEncapsulation stdio) {
        super(dad, title, true);
        _dad = dad;
        _mom = mom;
        _stdio = stdio;
        if (dad != null) {
            Dimension parentSize = dad.getSize();
            Point p = dad.getLocation();
            setLocation(p.x + parentSize.width / 4, p.y + parentSize.height / 4);
        } else {
            //
            // Typical case of "can never happen"...
            //
            _stdio.getStderr().println("~~~~~~ dad == null !!!!");
        }
        JPanel messagePane = new JPanel();
        messagePane.setLayout(new BoxLayout(messagePane, BoxLayout.Y_AXIS));
        JPanel dirpane = new JPanel();
        _directory_txt = new JTextField("", 40);
        dirpane.add(_directory_txt);
        JButton btn_seldir = new JButton("Browse...");
        dirpane.add(btn_seldir);
        btn_seldir.addActionListener(new ActionListener() {
            /**
             * Called when browse filesystem button has been clicked
             * @param actionEvent is the event that triggers this call
             */
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                chooser.setAcceptAllFileFilterUsed(false);
                int returnVal = chooser.showOpenDialog(_dad);
                if(returnVal == JFileChooser.APPROVE_OPTION) {
                    _directory_txt.setText(chooser.getCurrentDirectory() + "/" + chooser.getSelectedFile().getName());
                    //_stdio.getStdout().println("~~~~~~ _directory_txt=" + _directory_txt.getText());
                }

            }
        });
        messagePane.add(dirpane);
        _save_req = new JCheckBox("Save request bodies", true);
        _save_resp = new JCheckBox("Save response bodies", true);
        messagePane.add(_save_req);
        messagePane.add(_save_resp);
        JButton button = new JButton("OK");
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        messagePane.add(button);
        button.addActionListener(this);
        getContentPane().add(messagePane);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setVisible(true);
    }

    /**
     * Called when user wants to start saving the bodies
     * @param e is the event that triggers this call
     */
    public void actionPerformed(ActionEvent e) {
        setVisible(false);
        dispose();
        _mom.saveBodies(_directory_txt.getText(), _save_req.isSelected(), _save_resp.isSelected());
    }

}
