/*
 * Copyright 2018 Bence Varga
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 * OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.czentral.incubator.streamm.web;

import java.util.Map;
import java.util.Properties;
import org.czentral.event.EventDispatcher;
import org.czentral.incubator.streamm.ControlledStream;
import org.czentral.incubator.streamm.EventAnalizer;
import org.czentral.minihttp.HTTPException;
import org.czentral.minihttp.HTTPRequest;
import org.czentral.minihttp.HTTPResource;
import org.czentral.minihttp.HTTPResponse;

/**
 *
 * @author Varga Bence
 */
public class InfoResource implements HTTPResource {
    
    private final String STR_CONTENT_TYPE = "Content-type";
    
    protected Properties props;

    protected Map<String, ControlledStream> streams;

    public InfoResource(Properties props, Map<String, ControlledStream> streams) {
        this.props = props;
        this.streams = streams;
    }

    public void serve(HTTPRequest request, HTTPResponse response) throws HTTPException {
        // the part of the path after the resource's path
        int resLength = request.getResourcePath().length();
        String requestPath = request.getPathName();
        if (requestPath.length() - resLength <= 1) {
            throw new HTTPException(400, "No Stream ID Specified");
        }
        // stream ID
        String streamID = requestPath.substring(resLength + 1);
        // is a stream with that name defined?
        if (props.getProperty("streams." + streamID) == null) {
            throw new HTTPException(404, "Stream Not Registered");
        }
        // check password
        String requestPassword = request.getParameter("password");
        if (requestPassword == null) {
            throw new HTTPException(403, "Authentication failed: No password");
        }
        if (!requestPassword.equals(props.getProperty("streams." + streamID + ".password"))) {
            throw new HTTPException(403, "Authentication failed: Wrong password");
        }
        // getting stream
        ControlledStream stream = (ControlledStream) streams.get(streamID);
        if (stream == null || !stream.isRunning()) {
            throw new HTTPException(503, "Stream Not Running");
        }
        // setting rsponse content-type
        response.setParameter(STR_CONTENT_TYPE, "text/plain");
        // get evet dispatcher (if no current one then it is created)
        EventDispatcher dispatcher = stream.getEventDispatcher();
        // creating analizer
        EventAnalizer analizer = new EventAnalizer(stream, dispatcher, response.getOutputStream());
        // serving the request (from this thread)
        analizer.run();
    }
    
}
