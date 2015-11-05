/*
 * Copyright (c) 2012 Stefan Wolf
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package hudson.plugins.depgraph_view;

/**
 *  Data Structure to encode the content type and the -T argument for the graphviz tools
 */
public enum SupportedImageType {
	
    PNG("image/png", "png"),
    SVG("image/svg", "svg"),
    MAP("image/cmapx", "cmapx"),
    JSON("text/plain", "json", false),
    GV("text/plain", "gv", false),
    
    ;

    public final String contentType;
    public final String dotType;
    public final boolean requiresProcessing;

    private SupportedImageType(String contentType,
                       String dotType,
                       boolean requiresProcessing) {
        this.contentType = contentType;
        this.dotType = dotType;
        this.requiresProcessing = requiresProcessing;
    }

    private SupportedImageType(String contentType,
                       String dotType) {
        this(contentType, dotType, true);
    }

}
