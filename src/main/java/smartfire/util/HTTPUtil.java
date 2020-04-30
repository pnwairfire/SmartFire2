/*SMARTFIRE: Satellite Mapping Automated Reanalysis Tool for Fire Incident REconciliation
Copyright (C) 2006-Present  USDA Forest Service AirFire Research Team and Sonoma Technology, Inc.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
package smartfire.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smartfire.SmartfireException;

public class HTTPUtil {
    private static final Logger log = LoggerFactory.getLogger(HTTPUtil.class);

    static public String submitGetRequest(String url) {
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet request = new HttpGet(url);

        // Execute HTTP Get request
        HttpResponse response;
        try {
            log.info("Sending GET request to URL \"{}\"", url);
            response = client.execute(request);
        } catch(IOException e) {
            log.error("Error while executing HTTP request", e);
            throw new SmartfireException("Error while executing HTTP request", e);
        }
        log.info("Response code: {}", response.getStatusLine().getStatusCode());

        // Read & return HTTP Get request response
        return HTTPUtil.readResponse(response);
    }

    static public String submitPostRequest(String url, Map<String, String> params) {
        HttpClient client = HttpClientBuilder.create().build();
        HttpPost request = new HttpPost(url);

        // Encode HTTP Post parameters into request
        List<NameValuePair> postParameters = new ArrayList<NameValuePair>();
        try {
            for (Entry<String, String> entry : params.entrySet()) {
                postParameters.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }
            request.setEntity(new UrlEncodedFormEntity(postParameters, "UTF-8"));
        } catch(UnsupportedEncodingException e) {
            log.error("Error while encoding http post parameters", e);
            throw new SmartfireException("Error while encoding http post parameters", e);
        }
        // Execute HTTP Post request
        HttpResponse response;
        try {
            response = client.execute(request);
        } catch(IOException e) {
            log.error("Error while executing HTTP request", e);
            throw new SmartfireException("Error while executing HTTP request", e);
        }

        // Read & return HTTP Post request response
        return HTTPUtil.readResponse(response);
    }

    static private String readResponse(HttpResponse response) {
        StringBuilder result = new StringBuilder();
        BufferedReader rd = null;
        try {
            rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            String line = rd.readLine();
            while (line != null) {
                result.append(line);
                line = rd.readLine();
            }
        } catch(IOException e) {
            log.error("Error while reading HTTP response", e);
            throw new SmartfireException("Error while reading HTTP response", e);
        } catch(IllegalStateException e) {
            log.error("Error while reading HTTP response", e);
            throw new SmartfireException("Error while reading HTTP response", e);
        } finally {
            try {
                rd.close();
            } catch(IOException e) {
                log.error("Error while reading HTTP response", e);
                throw new SmartfireException("Error while reading HTTP response", e);
            }
        }
        return result.toString();
    }
}
