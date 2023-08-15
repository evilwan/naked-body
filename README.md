# How to use Burpsuite extension "Naked Body"

## Notice

Moved to [Codeberg](https://codeberg.org/evilwan/naked-body) because of policy changes at Github (see
[Github notice](https://github.blog/2023-03-09-raising-the-bar-for-software-security-github-2fa-begins-march-13) )

## Installation
In Burp go to the "Extender" tab, then select the "Extensions" sub-tab and click "Add"

This is a Java extension, so select extension type "Java" and for extension file, select the Jar file that comes with
this extension (e.g. `NakedBody-2.0.jar`) Accept all defaults for the extension and you are good to go.

## Usage
Go to the "HTTP history" sub-tab in the "Proxy" tab.

Select one or more lines in the history and click the right mouse button.

In the pop-up, select "Extensions" and then "Naked Body" to display the save dialog of this extension.

Use the "Browse..." button to select the output directory (where to bury^H save the request/response bodies.

Check or uncheck the options to save request bodies and/or response bodies.

Note that if a body is empty, there will be no empty file created. Most of the times a request without body will result in a response with non-empty body. For those situations, it is safe to have both request and response checkboxes selected.

Finally, click "OK" to actually save the selected bodies.

The output filenames are built like so:

* name of the selected output directory
* "`/`"
* timestamp of time when the extension was loaded in the format "`yyyy-MM-dd_HH-mm-ss-SSS`" (the final "SSS" stands for milliseconds)
* "`-req-`" for request bodies, "`-resp-`" for response bodies
* sequence number (starting from 1)
* file extension (default is "`.dat`")

The extension will attempt to guess the correct file extension based on either the body content (for WASM files) or the mime-type as defined in the "`Content-Type`" header.
However, this can be completely off if the server does not set the mime-type correct (or even omits it completely)

Examples:

    2021-12-17_13-29-03-611-resp-000001.html
    2021-12-17_13-29-03-611-resp-000002.jpg
    2021-12-17_13-29-03-611-resp-000009.dat

## Build from source code

The first step would be to download the source code from [here](https://github.com/evilwan/naked-body) and extract the
archive in a convenient place.

If you have Maven installed, then go to the top level directory of the extracted archive and type:

`mvn package`

At the end, you will find the Jar file in subdirectory `target/`

If you do not have Maven at hand, the process is slightly more complicated. Your best bet would be to compile the java code in this extension against
a Burpsuite Jar file (that is: put the Burpsuite Jar in your classpath) After compilation, package the classfiles for this extension in a separate Jar file
(there is no need to include the Burpsuite Jar in the extension Jar)

From the subdirectory `src/main/java` of the extracted sources, compile the code by hand with something like this:

`javac -cp <your-burpsuite.jar> burp/*.java evilwan/nakedbody/*.java`

followed by a packaging like this:

`jar cvf your-naked-body.jar burp/*.class evilwan/nakedbody/*.class`

## License

This extension is distributed under a 3 clause BSD license:


    Copyright (c) 2021 Eddy Vanlerberghe.  All rights reserved.
    
    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions
    are met:
    1. Redistributions of source code must retain the above copyright
       notice, this list of conditions and the following disclaimer.
    2. Redistributions in binary form must reproduce the above copyright
       notice, this list of conditions and the following disclaimer in the
       documentation and/or other materials provided with the distribution.
    3. The name of Eddy Vanlerberghe shall not be used to endorse or promote
       products derived from this software without specific prior written
       permission.
    
    THIS SOFTWARE IS PROVIDED BY EDDY VANLERBERGHE ''AS IS'' AND
    ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
    IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
    ARE DISCLAIMED.  IN NO EVENT SHALL EDDY VANLERBERGHE BE LIABLE
    FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
    DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
    OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
    HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
    LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
    OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
    SUCH DAMAGE.


