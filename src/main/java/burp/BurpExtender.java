/*
 * Copyright (c) 2021 Eddy Vanlerberghe.  All rights reserved.
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

package burp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import evilwan.nakedbody.*;

/**
 * Burp extension to save request and/or response bodies to separate files.
 */
public class BurpExtender implements IBurpExtender, IContextMenuFactory, BodySaver, StdioEncapsulation {
    /**
     * Start of title of main Burpsuite frame
     * <p>
     *     Used for locating the main Burpsuite frame: that frame is necessary for showing this
     *     extension save dialog.
     * </p>
     */
    private final static String BURP_MAIN_FRAME_TITLE = "Burp Suite Professional";
    /**
     * Name for this extension
     */
    private final static String EXTENSION_NAME = "Naked Body";
    /**
     * Label for custom menu item
     */
    private final static String MENUITEM_LABEL = "Naked Body";
    /**
     * Title for save body dialog
     */
    private final static String SAVE_DIALOG_TITLE = "Save naked body";
    /**
     * Common mime-types and their corresponding file extension
     * <p>
     * Note that the mime type strings below are patterns to be searched for
     * in the actual content-type headers. E.g. "/xml" covers several variations
     * for XML that all include this substring.
     */
    private final static Map<String, String> MIME2TYPE =
        Map.ofEntries(
                      new AbstractMap.SimpleEntry<String, String>("text/plain", ".txt"),
                      new AbstractMap.SimpleEntry<String, String>("text/html", ".html"),
                      new AbstractMap.SimpleEntry<String, String>("text/css", ".css"),
                      new AbstractMap.SimpleEntry<String, String>("javascript", ".js"),
                      new AbstractMap.SimpleEntry<String, String>("woff2", ".woff2"),
                      new AbstractMap.SimpleEntry<String, String>("font/ttf", ".ttf"),
                      new AbstractMap.SimpleEntry<String, String>("image/gif", ".gif"),
                      new AbstractMap.SimpleEntry<String, String>("image/png", ".png"),
                      new AbstractMap.SimpleEntry<String, String>("image/jpeg", ".jpg"),
                      new AbstractMap.SimpleEntry<String, String>("image/x-icon", ".ico"),
                      new AbstractMap.SimpleEntry<String, String>("image/svg", ".svg"),
                      new AbstractMap.SimpleEntry<String, String>("application/zip", ".zip"),
                      new AbstractMap.SimpleEntry<String, String>("font-woff", ".woff"),
                      new AbstractMap.SimpleEntry<String, String>("application/pdf", ".pdf"),
                      new AbstractMap.SimpleEntry<String, String>("image/webp", ".webp"),
                      new AbstractMap.SimpleEntry<String, String>("application/x-pem-file", ".pem"),
                      new AbstractMap.SimpleEntry<String, String>("video/mp4", ".mp4"),
                      new AbstractMap.SimpleEntry<String, String>("video/mpeg", ".mpg"),
                      new AbstractMap.SimpleEntry<String, String>("video/webm", ".webm"),
                      new AbstractMap.SimpleEntry<String, String>("video/x-msvideo", ".avi"),
                      new AbstractMap.SimpleEntry<String, String>("image/bmp", ".bmp"),
                      new AbstractMap.SimpleEntry<String, String>("application/x-bzip", ".bz"),
                      new AbstractMap.SimpleEntry<String, String>("application/x-bzip2", ".bz2"),
                      new AbstractMap.SimpleEntry<String, String>("text/csv", ".csv"),
                      new AbstractMap.SimpleEntry<String, String>("application/epub+zip", ".epub"),
                      new AbstractMap.SimpleEntry<String, String>("application/gzip", ".gz"),
                      new AbstractMap.SimpleEntry<String, String>("audio/mpeg", ".mp3"),
                      new AbstractMap.SimpleEntry<String, String>("/xml", ".xml"),
                      new AbstractMap.SimpleEntry<String, String>("json", ".json")
                      );
    /**
     * Prefix to use for output files.
     */
    private final static String OUTFILE_PREFIX = (new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS")).format(new Date());;
    /**
     * Count to use for output files.
     */
    private static int _outcount = 0;
    /**
     * Main set of Burp callback methods
     */
    private IBurpExtenderCallbacks _callbacks;
    /**
     * Additional helper methods made available by Burp.
     */
    private IExtensionHelpers _helpers;
    /**
     * Save body dialog
     */
    private BodySaveDialog _save_dialog;
    /**
     * List of selected items in proxy history when right click was done
     */
    private IHttpRequestResponse[] _them_proxy_lines = null;
    /**
     * IO stream do dump regular output on
     */
    private PrintStream _stdout = null;
    /**
     * IO stream do dump error messages on
     */
    private PrintStream _stderr = null;
    
    /**
     * Main entry point for Burp extension.
     * <p>
     *     This method is required by the IBurpExtender interface.
     * </p>
     * @param callbacks to interact with BurpSuite
     */
    @Override
    public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks) {
        //
        // Save callback and other helper methods
        //
        _callbacks = callbacks;
        //
        // Not strictly necessary but saves some method calls down the line
        //
        _helpers = _callbacks.getHelpers();
        _callbacks.setExtensionName(EXTENSION_NAME);
        _callbacks.registerContextMenuFactory(this);
        _stdout = new PrintStream(_callbacks.getStdout(), true);
        _stderr = new PrintStream(_callbacks.getStderr(), true);
        //_stdout.println("~~~~~ Consider extension loaded and initialized...");
    }

    /**
     * Called from various places in Burp to query the extension if one or more additional
     * context menu items need to be added to the list.
     * <p>This particular instance will only return a new <code>JMenuItem</code> if called for the
     * list of intercepted messages by the proxy</p>
     * @param iContextMenuInvocation An object that implements the IMessageEditorTabFactory interface, which the extension can
     *                               query to obtain details of the context menu invocation.
     * @return A list of custom menu items (which may include sub-menus, checkbox menu items, etc.) that should be displayed.
     */
    @Override
    public List<JMenuItem> createMenuItems(IContextMenuInvocation iContextMenuInvocation) {
        if(iContextMenuInvocation.getInvocationContext() == IContextMenuInvocation.CONTEXT_PROXY_HISTORY) {
            //
            // Save selected proxy items
            //
            _them_proxy_lines = iContextMenuInvocation.getSelectedMessages();
            //_stdout.println("Number of selected items:" + _them_proxy_lines.length);
            //
            // Return one new menu item that will call back into this extension
            //
            BodySaver self = this;
            ArrayList<JMenuItem> lst = new ArrayList<JMenuItem>();
            lst.add(new JMenuItem(new AbstractAction(MENUITEM_LABEL) {
                public void actionPerformed(ActionEvent e) {
                    // Button pressed logic goes here
                    //_stdout.println("~~~~~ Selected my menuitem...");
                    _save_dialog = new BodySaveDialog(findTopFrame(), SAVE_DIALOG_TITLE, self, (StdioEncapsulation) self);
                }
            }));
            return lst;
        } else {
            return null;
        }
    }


    /**
     * Save selected message bodies
     * @param directory location where to save the bodies
     * @param save_req <code>true</code> if request bodies have to be saved
     * @param save_resp <code>true</code> if response bodies have to be saved
     */
    public void saveBodies(String directory, boolean save_req, boolean save_resp) {
        //
        // Loop over all selected items in the proxy request/response list
        //
        for(IHttpRequestResponse reqresp : _them_proxy_lines) {
            _outcount += 1;
            if(save_req) {
                //_stdout.println("---- saving request for item: " + i);
                //
                // Get request info object
                //
                byte[] bar = reqresp.getRequest();
                IRequestInfo info = _helpers.analyzeRequest(bar);
                int body_ofs = info.getBodyOffset();
               if (body_ofs < bar.length) {
                   //
                   // We need to save the request: provide some halfway useful name for
                   // the output file (if possible)
                   //
                   String filnamreq =  String.format("%s/%s-req-%06d%s",
                                                     directory, OUTFILE_PREFIX, _outcount,
                                                     guessFileType(info.getHeaders(), bar, body_ofs));
                   if(info.getContentType() == IRequestInfo.CONTENT_TYPE_URL_ENCODED) {
                       byte[] tmpbar = new byte[bar.length - body_ofs];
                       System.arraycopy(bar, body_ofs, tmpbar, 0, tmpbar.length);
                       byte[] plain = _helpers.urlDecode(tmpbar);
                       dumpFile(filnamreq, plain, 0, plain.length);
                   } else {
                       dumpFile(filnamreq, bar, body_ofs, bar.length - body_ofs);
                   }
               }
            }
            if(save_resp) {
                //
                // We need to save the response
                //_stdout.println("---- saving response for item: " + i);
                //
                // Get response info object
                //
                byte[] bar = reqresp.getResponse();
                IResponseInfo info = _helpers.analyzeResponse(bar);
                //
                // Try to provide semi-intelligent guess as to which filetype to use
                //
                // Note that Burp does not give the actual content-type header (if present)
                //
                // The inferred mimetype is not of much use, unfortunately (that is: not more
                // than the stated mimetype)
                //
                //_stdout.println("---- response: mimetype=" + mimetype + ", filename=" + filnamresp);
                int body_ofs = info.getBodyOffset();
                if (body_ofs < bar.length) {
                    String filnamresp =  String.format("%s/%s-resp-%06d%s",
                                                     directory, OUTFILE_PREFIX, _outcount,
                                                     guessFileType(info.getHeaders(), bar, body_ofs));
                    dumpFile(filnamresp, bar, body_ofs, bar.length - body_ofs);
                }
            }
        }
    }
    /**
     * Analyze set of request/response headers and try to determine the
     * nature of the data being sent/received.
     *
     * @param hdrs set of request/response headers as retrieved from Burp
     * @return file extension that best matches the available information from the headers
     */
    private String guessFileType(List<String> hdrs, byte[] body, int ofs) {
        //say("guessFileType() -- entered.");
        String out = null;
        //
        // Plan A: try analysis of actual body bytes
        //
        if((body.length > ofs + 4) &&
           (body[ofs] == 0x00) &&
           (body[ofs + 1] == (byte) 'a') &&
           (body[ofs + 2] == (byte) 's') &&
           (body[ofs + 3] == (byte) 'm')) {
            //say("guessFileType() -- wasm detected.");
            return ".wasm";
        }
        //
        // Plan B: no type found based on body content, try mime type header
        //
        for(String line: hdrs) {
            String hdr = line.trim().toLowerCase();
            if(hdr.startsWith("content-type")) {
                //
                // Gotcha: provide reasonable set of checks
                //
                //say("guessFileType() -- content-type header: " + hdr);
                for(Map.Entry<String,String> e: MIME2TYPE.entrySet()) {
                    if(hdr.contains(e.getKey())) {
                        //say("guessFileType() -- returning: " + e.getValue());
                        return e.getValue();
                    }
                }
            }
        }
        //
        // Last chance: provide default ".dat" extension
        //
        //say("guessFileType() -- nothing found. returning: .dat");
        return ".dat";
    }

    /**
     * Dump byte array into file with specified name
     * @param filnam is the name of the output file to create
     * @param bar contains the bytes to dump into the file
     * @param ofs start dumping bytes from this zero based offset
     * @param len is the number of bytes to dump
     */
    private void dumpFile(String filnam, byte[] bar, int ofs, int len) {
        try {
            FileOutputStream fos = new FileOutputStream(filnam);
                fos.write(bar, ofs, len);
            fos.close();
        } catch (IOException ioe) {
            //
            // Ignore for now
            //
            _stderr.println("~~~~~~ caught: " + ioe);
            ioe.printStackTrace();
        }
    }

    /**
     * Find top level frame under which to hook the extensions save dialog
     * @return the top level Burp frame
     */
    private JFrame findTopFrame() {
        Frame[] frames = Frame.getFrames();
        for(Frame f : frames) {
            String title = f.getTitle();
            if((title != null) && (title.startsWith(BURP_MAIN_FRAME_TITLE))) {
                return (JFrame) f;
            }
        }
        return null;
    }

    /**
     * Returns the Burp extension standard output PrintStream
     * @return the PrintStream onto which to write output messages
     */
    @Override
    public PrintStream getStdout() {
        return _stdout;
    }

    /**
     * Returns the Burp extension standard error PrintStream
     * @return the PrintStream onto which to write error messages
     */
    @Override
    public PrintStream getStderr() {
        return _stderr;
    }
    /**
     * Helper method for printing messages.
     * <p>
     * The printed message is written to <code>System.out</code> and starts with the current
     * class name, followed by two dashes, followed by the specified text.
     * @param s contains the <code>String</code> value to print.
     */
    private void say(String s) {
        _stdout.println("BurpExtender -- " + s);
    }
}
